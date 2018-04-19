package org.hps.analysis.alignment;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan2;
import java.math.BigDecimal;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class MollerSvtAlignmentDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    String vertexCollectionName = "UnconstrainedMollerVertices";
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    private Double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;
    private double _psumDelta = 0.05;
    private double _thetasumCut = 0.07;
    private double _trackChi2NdfCut = 8.; //corresponds to chisquared cut of 40 for 5-hit tracks
    private boolean _requireClusterMatch = false;
    private boolean _dumpRunAndEventNumber = false;

    double pMin = 0.25;
    double dP = .05;
    int nSteps = 11;
    double thetaMax = 0.06;
    double thetaMin = -0.06;

    String _triggerType = "all";//allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1

    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    protected void process(EventHeader event) {
        if (!matchTrigger(event)) {
            return;
        }
        if (event.getRunNumber() < 5620) {
            pMin = .35;
            nSteps = 9;
        }
        if (event.getRunNumber() > 7000) {
            pMin = 0.75;
            dP = .05;
            nSteps = 14;
            thetaMax = 0.035;
            thetaMin = -.035;
            _beamEnergy = 2.306;
            _thetasumCut = 0.0475;
        }
        double psumMin = (1 - _psumDelta) * _beamEnergy;
        double psumMax = (1 + _psumDelta) * _beamEnergy;
        setupSensors(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        for (Vertex v : vertices) {
            aida.tree().cd("/");
            ReconstructedParticle rp = v.getAssociatedParticle();
            int type = rp.getType();
            boolean isGbl = TrackType.isGBL(type);
            // require GBL tracks in vertex
            if (isGbl) {
                List<ReconstructedParticle> parts = rp.getParticles();
                ReconstructedParticle rp1 = parts.get(0);
                ReconstructedParticle rp2 = parts.get(1);
                // basic sanity check here, remove full energy electrons (fee)
                if (rp1.getMomentum().magnitude() > 1.5 * _beamEnergy || rp2.getMomentum().magnitude() > 1.5 * _beamEnergy) {
                    continue;
                }
                // require both reconstructed particles to have a track

                if (rp1.getTracks().size() != 1) {
                    continue;
                }
                if (rp2.getTracks().size() != 1) {
                    continue;
                }
                Track t1 = rp1.getTracks().get(0);
                Track t2 = rp2.getTracks().get(0);
                if (_requireClusterMatch) {
                    // require both reconstructed particles to have a cluster

                    if (rp1.getClusters().size() != 1) {
                        continue;
                    }
                    if (rp2.getClusters().size() != 1) {
                        continue;
                    }
                    Cluster c1 = rp1.getClusters().get(0);
                    Cluster c2 = rp2.getClusters().get(0);
                    double deltaT = ClusterUtilities.getSeedHitTime(c1) - ClusterUtilities.getSeedHitTime(c2);
                    // require cluster times to be coincident within 2 ns
                    if (abs(deltaT) > 2.0) {
                        continue;
                    }
                }
                // require momentum sum to equal beam energy +-
                double psum = rp1.getMomentum().magnitude() + rp2.getMomentum().magnitude();
                if (psum < psumMin || psum > psumMax) {
                    continue;
                }
                //rotate into physics frame of reference
                //TODO double-check this
                Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
                Hep3Vector p1rot = VecOp.mult(beamAxisRotation, rp1.getMomentum());
                Hep3Vector p2rot = VecOp.mult(beamAxisRotation, rp2.getMomentum());
                double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());
                double theta2 = Math.acos(p2rot.z() / p2rot.magnitude());
                double thetasum = theta1 + theta2;
                // cut on thetasum
                if (thetasum > _thetasumCut) {
                    continue;
                }
                // cut on Moller pX
                if (abs(rprot.x()) > 0.02) {
                    continue;
                }
                // cut on Moller pY
                if (abs(rp.getMomentum().y()) > .02) {
                    continue;
                }
                double t1ChisqNdf = t1.getChi2() / t1.getNDF();
                double t2ChisqNdf = t2.getChi2() / t2.getNDF();

                double t1ChisqProb = ChisqProb.gammp(t1.getNDF(), t1.getChi2());
                double t2ChisqProb = ChisqProb.gammp(t2.getNDF(), t2.getChi2());
                // used to cut on prob < 0.995, which corresponds to roughly 3.4
                // change this to a cut on chi-squared/dof which people are more familiar with.
                // Omar currently cuts on chi-squared <40(!), irrespective of 5 or 6 hit tracks
                // let's start at chisq/dof of 8
                if (t1ChisqNdf > _trackChi2NdfCut) {//(t1ChisqProb > 0.995) {
                    continue;
                }
                if (t2ChisqNdf > _trackChi2NdfCut) {//(t2ChisqProb > 0.995) {
                    continue;
                }
                // all cuts passed, let's fill some histograms
                alignit(rp, rp1, rp2);
            }
        }
    }

    private void alignit(ReconstructedParticle rp, ReconstructedParticle rp1, ReconstructedParticle rp2) {
        Track t1 = rp1.getTracks().get(0);
        Track t2 = rp2.getTracks().get(0);
        // only analyze 6-hit tracks (for now)
        int t1Nhits = t1.getTrackerHits().size();
        if (t1Nhits != 6) {
            return;
        }
        int t2Nhits = t2.getTrackerHits().size();
        if (t2Nhits != 6) {
            return;
        }
        // in principle, tracks with multi-strip hits are better measured...
        // 1st axial layer has greatest influence on theta, so require 2 strips in hit
        // TODO should I also require 2 strips in stereo layers?
        int t1L1AxialNstrips = 0;
        int t1L1StereoNstrips = 0;
        int t2L1AxialNstrips = 0;
        int t2L1StereoNstrips = 0;

        for (TrackerHit hit : TrackUtils.getStripHits(t1, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    t1L1AxialNstrips = rthList.size();
                }
                if (moduleName.contains("stereo")) {
                    t1L1StereoNstrips = rthList.size();
                }
            }
//            System.out.println(hit);
//            for (Object o : hit.getRawHits()) {
//                RawTrackerHit rth = (RawTrackerHit) o;
//                System.out.printf("name=%s\tside=%d\tstrip=%d\n", rth.getDetectorElement().getName(),
//                        rth.getIdentifierFieldValue("side"), rth.getIdentifierFieldValue("strip"));
//            }
//            System.out.println("Track 1 hit at " + Arrays.toString(hit.getPosition()) + " has " + hit.getRawHits().size() + " strips");
        }
        for (TrackerHit hit : TrackUtils.getStripHits(t2, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    t2L1AxialNstrips = rthList.size();
                }
                if (moduleName.contains("stereo")) {
                    t2L1StereoNstrips = rthList.size();
                }
            }
        }

        double e1 = rp1.getEnergy();
        double e2 = rp2.getEnergy();
        double p1 = rp1.getMomentum().magnitude();
        double p2 = rp2.getMomentum().magnitude();

        Hep3Vector p1mom = rp1.getMomentum();
        Hep3Vector p2mom = rp2.getMomentum();

        //rotate into physics frame of reference
        Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        Hep3Vector p1rot = VecOp.mult(beamAxisRotation, rp1.getMomentum());
        Hep3Vector p2rot = VecOp.mult(beamAxisRotation, rp2.getMomentum());
        double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());
        double theta2 = Math.acos(p2rot.z() / p2rot.magnitude());

        double theta1x = Math.asin(p1rot.x() / p1rot.magnitude());
        double theta1y = Math.asin(p1rot.y() / p1rot.magnitude());

        double theta2x = Math.asin(p2rot.x() / p2rot.magnitude());
        double theta2y = Math.sin(p2rot.y() / p2rot.magnitude());

        aida.histogram1D("Track thetaY " + t1L1AxialNstrips + " L1 axial strips", 1000, -0.06, 0.06).fill(theta1y);
        aida.histogram1D("Track thetaY " + t2L1AxialNstrips + " L1 axial strips", 1000, -0.06, 0.06).fill(theta2y);

        double mollerTrackTheta1 = acos(1 - 0.511e-3 * (1 / p1 - 1 / _beamEnergy));
        double mollerTrackTheta2 = acos(1 - 0.511e-3 * (1 / p2 - 1 / _beamEnergy));

        double phi1 = atan2(p1rot.x(), p1rot.y());
        double phi2 = atan2(p2rot.x(), -p2rot.y()); //TODO figure out why the -ive sign is needed

        // step in momentum
        for (int i = 0; i < nSteps; ++i) {
            double pBin = pMin + i * dP;
            BigDecimal bd = new BigDecimal(Double.toString(pBin));
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            double binLabel = bd.doubleValue();

            // System.out.println("i " + i + " pBin " + pBin + " p1 " + p1 + " p2 " + p2);
            if (abs(p1 - pBin) < dP / 2.) {
                double dTheta = theta1 - mollerTrackTheta1;
                if (isTopTrack(t1)) {
                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p1);
                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
                    aida.histogram1D(binLabel + " Top Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta1);
                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
                } else {
                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p1);
                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
                    aida.histogram1D(binLabel + " Bottom Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta1);
                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
                }
            }
            if (abs(p2 - pBin) < dP / 2.) {
                double dTheta = theta2 - mollerTrackTheta2;
                if (isTopTrack(t2)) {
                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p2);
                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
                    aida.histogram1D(binLabel + " Top Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta2);
                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
                } else {
                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p2);
                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
                    aida.histogram1D(binLabel + " Bottom Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta2);
                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
                }
            }
        }

    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");
    }

    public boolean matchTriggerType(TIData triggerData) {
        if (_triggerType.contentEquals("") || _triggerType.contentEquals("all")) {
            return true;
        }
        if (triggerData.isSingle0Trigger() && _triggerType.contentEquals("singles0")) {
            return true;
        }
        if (triggerData.isSingle1Trigger() && _triggerType.contentEquals("singles1")) {
            return true;
        }
        if (triggerData.isPair0Trigger() && _triggerType.contentEquals("pairs0")) {
            return true;
        }
        if (triggerData.isPair1Trigger() && _triggerType.contentEquals("pairs1")) {
            return true;
        }
        if (triggerData.isPulserTrigger() && _triggerType.contentEquals("pulser")) {
            return true;
        }
        return false;

    }

    public boolean matchTrigger(EventHeader event) {
        boolean match = true;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList) {
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...
                    {
                        match = false;
                    }
                }
            }
        }
        return match;
    }
}
