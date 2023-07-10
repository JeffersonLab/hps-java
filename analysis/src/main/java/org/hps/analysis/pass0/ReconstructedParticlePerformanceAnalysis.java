package org.hps.analysis.pass0;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Catch-all Driver to analyze ReconstructedParticles.
 *
 * Currently targeted towards charged particles, specifically those with a track
 * associated with an Ecal cluster.
 *
 * @author Norman A. Graf
 */
public class ReconstructedParticlePerformanceAnalysis extends Driver {

    String[] reconstructedParticleCollectionNames = {"FinalStateParticles_KF"};//, "FinalStateParticles"};

    private AIDA aida = AIDA.defaultInstance();
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    @Override
    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    @Override
    protected void process(EventHeader event) {
        setupSensors(event);
        for (String reconstructedParticleCollectionName : reconstructedParticleCollectionNames) {
            List<ReconstructedParticle> rps;
            if (event.hasCollection(ReconstructedParticle.class, reconstructedParticleCollectionName)) {
                rps = event.get(ReconstructedParticle.class, reconstructedParticleCollectionName);
                aida.tree().mkdirs(reconstructedParticleCollectionName + " analysis");
                aida.tree().cd(reconstructedParticleCollectionName + " analysis");
                for (ReconstructedParticle rp : rps) {
                    int pdgId = rp.getParticleIDUsed().getPDG();
                    if (abs(pdgId) == 11) {
                        Track track = rp.getTracks().get(0);
                        int trackType = track.getType();
                        boolean isGBL = TrackType.isGBL(trackType);
                        String trackDir = isGBL ? "GBL " : "ST ";
                        if (trackType == 1) {
                            trackDir = "KF ";
                        }
                        aida.tree().mkdirs(trackDir + "track analysis");
                        aida.tree().cd(trackDir + "track analysis");
                        Hep3Vector momentum = rp.getMomentum();
                        Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
                        double theta = Math.acos(rprot.z() / rprot.magnitude());
                        double thetaY = abs(Math.asin(rprot.y() / rprot.magnitude()));
                        double phi = momentum.y() > 0 ? atan2(rprot.x(), rprot.y()) : atan2(rprot.x(), -rprot.y());
                        double p = momentum.magnitude();
                        int nHits = track.getTrackerHits().size();
                        double chisqNdf = track.getChi2() / track.getNDF();
                        String topOrBottom = momentum.y() > 0 ? " top" : " bottom";
                        aida.histogram1D("Track momentum", 100, 0., 7.).fill(p);
                        aida.histogram1D("Track momentum" + topOrBottom, 100, 0., 7.).fill(p);
                        aida.histogram1D("Track nHits" + topOrBottom, 20, -0.5, 19.5).fill(nHits);

//                        aida.tree().mkdirs(trackDir + "alignment analysis");
//                        aida.tree().cd(trackDir + "alignment analysis");
//                        aida.histogram1D("Track thetaY fine" + topOrBottom, 1000, 0.0, 0.1).fill(thetaY);
//                        if (nHits >= 12) {
//                            aida.histogram1D("Track thetaY fine" + topOrBottom + " nHits>11", 1000, 0.0, 0.1).fill(thetaY);
//                        }
//                        aida.histogram1D("phi" + topOrBottom, 100, -1.5, 1.5).fill(phi);
//                        aida.histogram2D("phi vs thetaY" + topOrBottom, 100, -1., 1., 500, 0.01, 0.05).fill(phi, thetaY);
//                        aida.tree().cd("..");
                        if (!rp.getClusters().isEmpty()) {
                            Cluster c = rp.getClusters().get(0);
                            boolean isFiducial = TriggerModule.inFiducialRegion(c);
                            String fid = isFiducial ? " fiducial" : " edge";
                            double e = c.getEnergy();
                            CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
                            int ix = seedHit.getIdentifierFieldValue("ix");
                            int iy = seedHit.getIdentifierFieldValue("iy");
                            aida.histogram2D("Cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
                            aida.histogram2D("Cluster ix vs iy" + " " + nHits + "track hits", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
                            aida.histogram1D("Track with cluster momentum", 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster momentum" + topOrBottom, 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster nHits" + topOrBottom, 20, -0.5, 19.5).fill(nHits);
                            aida.histogram1D("Track with cluster energy" + topOrBottom, 100, 0., 7.).fill(e);
                            aida.histogram1D("Track with cluster eoverp" + topOrBottom, 100, 0., 2.).fill(e / p);
                            aida.histogram2D("Track with cluster e vs eoverp" + topOrBottom, 100, 0., 7., 100, 0., 2.).fill(e, e / p);

                            aida.histogram1D("Track with cluster momentum" + fid, 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster momentum" + topOrBottom + fid, 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster nHits" + topOrBottom + fid, 20, -0.5, 19.5).fill(nHits);
                            aida.histogram1D("Track with cluster energy" + topOrBottom + fid, 100, 0., 7.).fill(e);
                            aida.histogram1D("Track with cluster eoverp" + topOrBottom + fid, 100, 0., 2.).fill(e / p);
                            aida.histogram2D("Track with cluster e vs eoverp" + topOrBottom + fid, 100, 0., 7., 100, 0., 2.).fill(e, e / p);

                            aida.histogram1D("Track with cluster momentum" + fid + " " + nHits, 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster momentum" + topOrBottom + fid + " " + nHits, 100, 0., 7.).fill(p);
                            aida.histogram1D("Track with cluster nHits" + topOrBottom + fid + " " + nHits, 20, -0.5, 19.5).fill(nHits);
                            aida.histogram1D("Track with cluster energy" + topOrBottom + fid + " " + nHits, 100, 0., 7.).fill(e);
                            aida.histogram1D("Track with cluster eoverp" + topOrBottom + fid + " " + nHits, 100, 0., 2.).fill(e / p);
                            aida.histogram2D("Track with cluster e vs eoverp" + topOrBottom + fid + " " + nHits, 100, 0., 7., 100, 0., 2.).fill(e, e / p);

                            aida.histogram1D("Track theta" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.20).fill(theta);
                            aida.histogram1D("Track theta" + topOrBottom, 100, 0.010, 0.20).fill(theta);
                            aida.histogram2D("Track theta vs p" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.20, 100, 0., 7.0).fill(theta, p);
                            aida.histogram2D("Track theta vs eoverp" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.20, 100, 0., 2.0).fill(theta, e / p);
                            aida.histogram2D("Track theta vs eoverp" + topOrBottom + fid, 100, 0.010, 0.20, 100, 0., 2.0).fill(theta, e / p);

                            aida.histogram1D("Track thetaY" + topOrBottom, 100, 0.010, 0.07).fill(thetaY);
                            aida.histogram1D("Track thetaY" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.07).fill(thetaY);
                            aida.histogram2D("Track thetaY vs p" + topOrBottom, 100, 0.010, 0.07, 100, 0., 7.0).fill(thetaY, p);
                            aida.histogram2D("Track thetaY vs p" + topOrBottom + " " + nHits, 100, 0.010, 0.07, 100, 0., 7.0).fill(thetaY, p);
                            aida.histogram2D("Track thetaY vs p" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.07, 100, 0., 7.0).fill(thetaY, p);
                            aida.histogram2D("Track thetaY vs eoverp" + topOrBottom + fid + " " + nHits, 100, 0.010, 0.07, 100, 0., 2.0).fill(thetaY, e / p);
                            aida.histogram2D("Track thetaY vs eoverp" + topOrBottom + fid, 100, 0.010, 0.07, 100, 0., 2.0).fill(thetaY, e / p);

                            aida.histogram1D("Track chisq per NDF " + topOrBottom + " " + nHits, 100, 0., 30.).fill(chisqNdf);
                            aida.histogram1D("Track chisq " + topOrBottom + " " + nHits, 100, 0., 400.).fill(track.getChi2());

                            //let's check out the track-cluster position matching...
                            aida.tree().mkdirs(trackDir + "ECal alignment analysis");
                            aida.tree().cd(trackDir + "ECal alignment analysis");
                            //note that the reference point coordinates are listed as (z,x,y).
                            double[] tpos = track.getTrackStates().get(track.getTrackStates().size() - 1).getReferencePoint();
                            double tx = tpos[1];
                            double ty = tpos[2];
                            double tz = tpos[0];
                            double[] cpos = c.getPosition();
                            double cx = cpos[0];
                            double cy = cpos[1];
                            double cz = cpos[2];

                            double dx = cx - tx;
                            double dy = cy - ty;

                            aida.cloud1D("cluster z").fill(cz);
                            aida.cloud1D("track z").fill(tz);
                            aida.histogram2D("Cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
                            aida.histogram2D("cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cx, cy);
                            aida.histogram2D("track x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(tx, ty);
                            aida.histogram2D("cluster x vs y" + fid, 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cx, cy);
                            aida.histogram2D("track x vs y" + fid, 320, -270.0, 370.0, 90, -90.0, 90.0).fill(tx, ty);

                            aida.histogram1D("cluster x - track x", 100, -15., 15.).fill(dx);
                            aida.histogram2D("cluster x - track x vs cluster x", 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            if (cy > 0) {
                                aida.histogram1D("cluster x - track x top", 100, -15., 15.).fill(dx);
                                aida.histogram2D("cluster x - track x vs cluster x top", 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            } else {
                                aida.histogram1D("cluster x - track x bottom", 100, -15., 15.).fill(dx);
                                aida.histogram2D("cluster x - track x vs cluster x bottom", 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            }

                            aida.histogram2D("cluster x - track x vs cluster y", 90, -90.0, 90.0, 100, -15., 15.).fill(cy, dx);

                            aida.histogram1D("cluster y - track y", 100, -10., 10.).fill(dy);

                            aida.histogram2D("cluster y - track y vs cluster x", 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            aida.histogram2D("cluster y - track y vs cluster y", 90, -90.0, 90.0, 100, -10., 10.).fill(cy, dy);

                            if (cy > 0) {
                                aida.histogram2D("cluster y - track y vs cluster x top", 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            } else {
                                aida.histogram2D("cluster y - track y vs cluster x bottom", 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            }
                            //repeat with an analysis of fiducial regions...
                            aida.histogram1D("cluster x - track x" + fid, 100, -15., 15.).fill(dx);
                            aida.histogram2D("cluster x - track x vs cluster x" + fid, 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            if (cy > 0) {
                                aida.histogram1D("cluster x - track x top" + fid, 100, -15., 15.).fill(dx);
                                aida.histogram2D("cluster x - track x vs cluster x top" + fid, 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            } else {
                                aida.histogram1D("cluster x - track x bottom" + fid, 100, -15., 15.).fill(dx);
                                aida.histogram2D("cluster x - track x vs cluster x bottom" + fid, 320, -270.0, 370.0, 100, -15., 15.).fill(cx, dx);
                            }

                            aida.histogram2D("cluster x - track x vs cluster y" + fid, 90, -90.0, 90.0, 100, -15., 15.).fill(cy, dx);

                            aida.histogram1D("cluster y - track y" + fid, 100, -10., 10.).fill(dy);

                            aida.histogram2D("cluster y - track y vs cluster x" + fid, 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            aida.histogram2D("cluster y - track y vs cluster y" + fid, 90, -90.0, 90.0, 100, -10., 10.).fill(cy, dy);

                            if (cy > 0) {
                                aida.histogram2D("cluster y - track y vs cluster x top" + fid, 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            } else {
                                aida.histogram2D("cluster y - track y vs cluster x bottom" + fid, 320, -270.0, 370.0, 100, -10., 10.).fill(cx, dy);
                            }
                            aida.tree().cd("..");
                        }
                        aida.tree().cd("..");
                    }
                }
                aida.tree().cd("..");
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

}
