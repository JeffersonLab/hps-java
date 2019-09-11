package org.hps.analysis.examples;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IProfile1D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class EventAnalysis2019 extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    IProfile1D zProfileBottomMatched;
    IProfile1D zProfileBottomGBL;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        zProfileBottomMatched = aida.profile1D("Bottom Matched Track thetaY vs z0 profile", 25, 0.032, 0.052);
        zProfileBottomGBL = aida.profile1D("Bottom GBLTrack thetaY vs z0 profile", 25, 0.032, 0.052);
    }

    public void process(EventHeader event) {
        //skip "monster" events
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        int nRawTrackerHits = rawTrackerHits.size();
        aida.histogram1D("SVT number of RawTrackerHits", 100, 0., 1000.).fill(nRawTrackerHits);
        if (nRawTrackerHits > 250) {
            return;
        }

        setupSensors(event);
        analyzeRP(event);
        analyzeV0(event);
    }

    private void analyzeRP(EventHeader event) {
        String dir = "FinalState ReconstructedParticle Analysis";
        aida.tree().mkdirs(dir);
        aida.tree().cd(dir);
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        for (ReconstructedParticle rp : rpList) {
            int pdgId = rp.getParticleIDUsed().getPDG();
            if (pdgId == 11) {
                String trackType = TrackType.isGBL(rp.getType()) ? "GBL " : "MatchedTrack ";
//            if (!TrackType.isGBL(rp.getType())) {
//                continue;
//            }
                Track t = rp.getTracks().get(0);
                int nHits = t.getTrackerHits().size();
                String id = pdgId == 11 ? "electron" : "positron";
                Hep3Vector pmom = rp.getMomentum();
                double thetaY = atan2(pmom.y(), pmom.z());//asin(pmom.y() / pmom.magnitude());
                double z0 = t.getTrackStates().get(0).getZ0();
                String torb = isTopTrack(t) ? " top " : "bottom ";
                aida.histogram1D(trackType + torb + id + " track momentum", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                
                aida.cloud1D(trackType + torb + id + " |thetaY|").fill(abs(thetaY));
                aida.histogram1D(trackType + torb + id + " z0", 100, -2., 2.).fill(z0);
                aida.cloud2D(trackType + torb + id + " |thetaY| vs z0").fill(abs(thetaY), z0);
                aida.profile1D(trackType + torb + id + " |thetaY| vs z0 profile", 10, 0.01, 0.1).fill(abs(thetaY), z0);
                if (trackType.equals("MatchedTrack ") && torb.equals("bottom ") && id.equals("electron")) {
                    zProfileBottomMatched.fill(abs(thetaY), z0);
                }
                if (trackType.equals("GBL ") && torb.equals("bottom ") && id.equals("electron")) {
                    zProfileBottomGBL.fill(abs(thetaY), z0);
                }

                List<Cluster> clusters = rp.getClusters();
                if (!clusters.isEmpty()) {
                    Cluster c = clusters.get(0);
                    double[] cPos = c.getPosition();
                    aida.histogram2D(id + " cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cPos[0], cPos[1]);
                    aida.histogram1D(trackType + torb + id + " cluster energy", 100, 0., 10.).fill(c.getEnergy());
                    aida.histogram1D(trackType + torb + id + " cluster energy over track momentum EoverP", 100, 0., 2.).fill(c.getEnergy() / rp.getMomentum().magnitude());
                    aida.histogram1D(trackType + torb + id + " track momentum with cluster", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                    aida.histogram1D(trackType + torb + id + " track momentum with cluster "+nHits+" hits", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                }
            }
            if (pdgId == 22) {
                String id = "photon";
                Cluster c = rp.getClusters().get(0);
                double[] cPos = c.getPosition();
                aida.histogram2D(id + " cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cPos[0], cPos[1]);
                aida.histogram1D(id + " momentum", 100, 0., 10.).fill(rp.getMomentum().magnitude());
            }
        }
        aida.tree().cd("..");
    }

    @Override
    protected void endOfData() {
        IAnalysisFactory af = IAnalysisFactory.create();
        IDataPointSetFactory dpsf = af.createDataPointSetFactory(aida.tree());
        IDataPointSet dps2DFromProf = dpsf.create("dps2DFromProf", zProfileBottomMatched);

    }

    private void analyzeV0(EventHeader event) {
        String dir = "Unconstrained V0 Analysis";
        aida.tree().mkdirs(dir);
        aida.tree().cd(dir);
        List<ReconstructedParticle> v0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        for (ReconstructedParticle v0 : v0List) {
            if (!TrackType.isGBL(v0.getType())) {
                continue;
            }
            Vertex uncVert = v0.getStartVertex();
            Hep3Vector pVtxRot = VecOp.mult(beamAxisRotation, v0.getMomentum());
            Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, uncVert.getPosition());
            double theta = Math.acos(pVtxRot.z() / pVtxRot.magnitude());
            double phi = Math.atan2(pVtxRot.y(), pVtxRot.x());
            aida.histogram1D("v0 x", 50, -5., 5.).fill(vtxPosRot.x());
            aida.histogram1D("v0 y", 50, -5., 5.).fill(vtxPosRot.y());
            aida.histogram1D("v0 z", 50, -25., 0.).fill(vtxPosRot.z());
            aida.histogram1D("v0 energy", 100, 0., 10.).fill(v0.getEnergy());
            aida.histogram1D("v0 mass", 50, 0., 0.5).fill(v0.getMass());
            // this always has 2 tracks.
            List<ReconstructedParticle> trks = v0.getParticles();
            ReconstructedParticle ele = trks.get(0);
            ReconstructedParticle pos = trks.get(1);
            double eMom = ele.getMomentum().magnitude();
            double pMom = pos.getMomentum().magnitude();
            aida.histogram1D("electron momentum", 100, 0., 6.0).fill(eMom);
            aida.histogram1D("positron momentum", 100, 0., 6.0).fill(pMom);
            aida.histogram2D("electron vs positron momentum", 100, 0., 6.0, 100, 0., 6.).fill(eMom, pMom);
        }
        aida.tree().cd("..");
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
