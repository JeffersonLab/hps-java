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
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class FeeAnalysisDriver extends Driver {

    boolean debug = false;
    private AIDA aida = AIDA.defaultInstance();

    private final String finalStateParticlesColName = "FinalStateParticles";

    private Double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    //Set min seed energy value, default to 2015 run 
    private double seedCut = 0.4; //= 0.4

    //set min cluster energy value, default to 2015 run
    private double clusterCut = 0.6;

    //minimum number of hits per cluster
    private int minHits = 3;

    double ctMin = 40.;
    double ctMax = 49.;

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    protected void process(EventHeader event) {
        // modify for 2016 run
        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
            seedCut = 1.2;
            clusterCut = 1.6;
            ctMin = 55.;
            ctMax = 61.;
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
        if (!isSingles) {
            return;
        }
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            return;
        }
        // only keep events with one and only one cluster
        List<Cluster> ecalClusters = event.get(Cluster.class, "EcalClustersCorr");
        if (ecalClusters.size() != 1) {
            return;
        }
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        setupSensors(event);
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
            }// end of cluster cuts
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
