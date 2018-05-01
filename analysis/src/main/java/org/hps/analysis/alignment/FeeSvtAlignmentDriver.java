package org.hps.analysis.alignment;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackData;
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
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class FeeSvtAlignmentDriver extends Driver {

    boolean debug = false;
    private AIDA aida = AIDA.defaultInstance();

    private final String finalStateParticlesColName = "FinalStateParticles";

    private Double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    //Set min seed energy value, default to 2015 run 
    private double seedCut = 0.0; //= 0.4

    //set min cluster energy value, default to 2015 run
    private double clusterCut = 0.2;

    //minimum number of hits per cluster
    private int minHits = 3;

    double ctMin = 40.;
    double ctMax = 49.;

    double thetaXmin = -0.05;
    double thetaXmax = 0.05;

    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    protected void process(EventHeader event) {
        // modify for 2016 run
        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
            seedCut = 0.;
            clusterCut = 0.2;
            ctMin = 55.;
            ctMax = 61.;
            thetaXmin = 0.09;
            thetaXmax = 0.15;
        }
        // only keep singles triggers:
        if (!event.hasCollection(GenericObject.class, "TriggerBank")) {
            return;
        }
        boolean isSingles = false;
        for (GenericObject gob : event.get(GenericObject.class, "TriggerBank")) {
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) {
                continue;
            }
            TIData tid = new TIData(gob);
            if (tid.isSingle0Trigger() || tid.isSingle1Trigger()) {
                isSingles = true;
                break;
            }
        }
//        if (!isSingles) {
//            return;
//        }
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            return;
        }
        // only keep events with one and only one cluster
//        List<Cluster> ecalClusters = event.get(Cluster.class, "EcalClustersCorr");
//        if (ecalClusters.size() != 1) {
//            return;
//        }
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        setupSensors(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        for (ReconstructedParticle rp : rpList) {
            if (!TrackType.isGBL(rp.getType())) {
                continue;
            }
            if (rp.getMomentum().magnitude() > 1.5 * _beamEnergy) {
                continue;
            }
            // require both track and cluster
            if (rp.getClusters().size() != 1) {
                continue;
            }

            if (rp.getTracks().size() != 1) {
                continue;
            }

            Track t = rp.getTracks().get(0);
            double p = rp.getMomentum().magnitude();
            Cluster c = rp.getClusters().get(0);
            // debug diagnostics to set cuts
            if (debug) {
                aida.cloud1D("clusterSeedHit energy").fill(ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                aida.cloud1D("cluster nHits").fill(c.getCalorimeterHits().size());
                aida.cloud2D("clusterSeedHit energy vs p").fill(p, ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                aida.cloud2D("cluster nHits vs p").fill(p, c.getCalorimeterHits().size());
                aida.cloud2D("cluster time vs p").fill(p, ClusterUtilities.getSeedHitTime(c));
            }
            double ct = ClusterUtilities.getSeedHitTime(c);

            if (c.getEnergy() > clusterCut
                    && ClusterUtilities.findSeedHit(c).getCorrectedEnergy() > seedCut
                    && c.getCalorimeterHits().size() >= minHits
                    && ct > ctMin
                    && ct < ctMax) {
                double chiSquared = t.getChi2();
                int ndf = t.getNDF();
                double chi2Ndf = t.getChi2() / t.getNDF();
                double chisqProb = ChisqProb.gammp(ndf, chiSquared);
                int nHits = t.getTrackerHits().size();
                double dEdx = t.getdEdx();
                //rotate into physiscs frame of reference
                Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
                double theta = Math.acos(rprot.z() / rprot.magnitude());

                // debug diagnostics to set cuts
                if (debug) {
                    aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
                    aida.cloud1D("Track chisq prob").fill(chisqProb);
                    aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());
                    aida.cloud1D("Track momentum").fill(p);
                    aida.cloud1D("Track deDx").fill(t.getdEdx());
                    aida.cloud1D("Track theta").fill(theta);
                    aida.cloud2D("Track theta vs p").fill(theta, p);
                    aida.cloud1D("rp x0").fill(TrackUtils.getX0(t));
                    aida.cloud1D("rp y0").fill(TrackUtils.getY0(t));
                    aida.cloud1D("rp z0").fill(TrackUtils.getZ0(t));
                }
                double trackDataTime = TrackData.getTrackTime(TrackData.getTrackData(event, t));
                if (debug) {
                    aida.cloud1D("track data time").fill(trackDataTime);
                }
                if (isTopTrack(t)) {
                    if (nHits == 5) {
                        aida.histogram1D("Fee top 5-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                    } else if (nHits == 6) {
                        aida.histogram1D("Fee top 6-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                    }
                } else {

                    if (nHits == 5) {
                        aida.histogram1D("Fee bottom 5-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                    } else if (nHits == 6) {
                        aida.histogram1D("Fee bottom 6-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                    }
                }
                if (nHits == 6) {
                    alignit(rp);
                }
            }// end of cluster cuts
        }
    }

    private void alignit(ReconstructedParticle rp) {
        Track t1 = rp.getTracks().get(0);
        // only analyze 6-hit tracks (for now)
        int t1Nhits = t1.getTrackerHits().size();
        if (t1Nhits != 6) {
            return;
        }
        // in principle, tracks with multi-strip hits are better measured...
        // 1st axial layer has greatest influence on theta, so require 2 strips in hit
        // TODO should I also require 2 strips in stereo layers?
        int t1L1AxialNstrips = 0;
        int t1L1StereoNstrips = 0;
        int t1L2AxialNstrips = 0;
        int t1L2StereoNstrips = 0;

        int t1L1AxialStripNumber = 0;
        int t1L1StereoStripNumber = 0;
        int t1L2AxialStripNumber = 0;
        int t1L2StereoStripNumber = 0;

        for (TrackerHit hit : TrackUtils.getStripHits(t1, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    t1L1AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L1AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L1StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L1StereoStripNumber);
                    }
                }
            }
            if (moduleName.contains("module_L2")) {
                if (moduleName.contains("axial")) {
                    t1L2AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L2AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L2StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L2StereoStripNumber);
                    }
                }
            }

//            for (Object o : hit.getRawHits()) {
//                RawTrackerHit rth = (RawTrackerHit) o;
//                System.out.printf("name=%s\tside=%d\tstrip=%d\n", rth.getDetectorElement().getName(),
//                        rth.getIdentifierFieldValue("side"), rth.getIdentifierFieldValue("strip"));
//            }
//            System.out.println("Track 1 hit at " + Arrays.toString(hit.getPosition()) + " has " + hit.getRawHits().size() + " strips");
        }

        double e1 = rp.getEnergy();
        double p1 = rp.getMomentum().magnitude();

        Hep3Vector p1mom = rp.getMomentum();

        //rotate into physics frame of reference
        Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        Hep3Vector p1rot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());

        double theta1x = Math.asin(p1rot.x() / p1rot.magnitude());
        double theta1y = Math.asin(p1rot.y() / p1rot.magnitude());

        if (t1L1AxialNstrips < 3) {
            aida.histogram1D("Track thetaY " + t1L1AxialNstrips + " L1 axial strips", 1000, -0.06, 0.06).fill(theta1y);
        }

        if (t1L1AxialNstrips < 3 && t1L2AxialNstrips < 3) {
            aida.histogram1D("Track thetaY " + t1L1AxialNstrips + " L1 " + t1L2AxialNstrips + " L2 axial strips", 1000, -0.06, 0.06).fill(theta1y);
        }

        // look for correlations
        if (t1L1AxialNstrips == 1 && t1L2AxialNstrips == 1) {
            if (theta1y > 0) {
                aida.histogram2D("Top Track L1 axial strip number vs Track thetaY", 100, 0., 100., 500, 0.015, 0.055).fill(t1L1AxialStripNumber, theta1y);
            } else {
                aida.histogram2D("Bottom Track L1 axial strip number vs Track thetaY", 100, 0., 100., 500, 0.015, 0.055).fill(t1L1AxialStripNumber, -theta1y);
            }
            if (t1L1AxialStripNumber > 1 && t1L1AxialStripNumber < 100) // inspect the first few strips more closely...
            {
                if (theta1y > 0) {
                    aida.histogram2D("Top Track thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
                    aida.histogram1D("Top Track L1 axial strip number " + t1L1AxialStripNumber + " thetaY", 400, 0.015, 0.055).fill(theta1y);
                    aida.cloud1D("Top Track thetaX").fill(theta1x);
                    aida.histogram2D("Top Track L1 axial strip number " + t1L1AxialStripNumber + " thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
                } else {
                    aida.histogram2D("Bottom Track thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
                    aida.histogram1D("Bottom Track L1 axial strip number " + t1L1AxialStripNumber + " thetaY", 400, 0.015, 0.055).fill(-theta1y);
                    aida.cloud1D("Bottom Track thetaX").fill(theta1x);
                    aida.histogram2D("Bottom Track L1 axial strip number " + t1L1AxialStripNumber + " thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
                }
            }
        }
        if (t1L1AxialNstrips == 1 && t1L2AxialNstrips == 2) { // should give the bext position resolution for layer 2 wrt 1
            if (theta1y > 0) {
                aida.histogram2D("Top Track 1 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
            } else {
                aida.histogram2D("Bottom Track 1 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
            }
        }
        if (t1L1AxialNstrips == 2 && t1L2AxialNstrips == 2) { // should give the bext position resolution but no structure
            if (theta1y > 0) {
                aida.histogram2D("Top Track 2 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
            } else {
                aida.histogram2D("Bottom Track 2 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
            }
        }

//        double mollerTrackTheta1 = acos(1 - 0.511e-3 * (1 / p1 - 1 / _beamEnergy));
//        double mollerTrackTheta2 = acos(1 - 0.511e-3 * (1 / p2 - 1 / _beamEnergy));
//
//        double phi1 = atan2(p1rot.x(), p1rot.y());
//        double phi2 = atan2(p2rot.x(), -p2rot.y()); //TODO figure out why the -ive sign is needed
//
//        // step in momentum
//        for (int i = 0; i < nSteps; ++i) {
//            double pBin = pMin + i * dP;
//            BigDecimal bd = new BigDecimal(Double.toString(pBin));
//            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
//            double binLabel = bd.doubleValue();
//
//            // System.out.println("i " + i + " pBin " + pBin + " p1 " + p1 + " p2 " + p2);
//            if (abs(p1 - pBin) < dP / 2.) {
//                double dTheta = theta1 - mollerTrackTheta1;
//                if (isTopTrack(t1)) {
//                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p1);
//                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Top Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
//                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
//                    if (t1L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
//                    }
//                } else {
//                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p1);
//                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Bottom Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
//                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
//                    if (t1L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
//                    }
//                }
//            }
//            if (abs(p2 - pBin) < dP / 2.) {
//                double dTheta = theta2 - mollerTrackTheta2;
//                if (isTopTrack(t2)) {
//                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p2);
//                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Top Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
//                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
//                    if (t2L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
//                    }
//                } else {
//                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p2);
//                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Bottom Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
//                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
//                    if (t2L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
//                    }
//                }
//            }
//        }
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
}
