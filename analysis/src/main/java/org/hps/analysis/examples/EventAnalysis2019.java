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
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author ngraf
 */
public class EventAnalysis2019 extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    IProfile1D zProfileBottomMatched;
    IProfile1D zProfileBottomGBL;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    //TODO add in OtherElectrons for high-momentum tracks...
    String[] finalStateParticleCollectionNames = {"FinalStateParticles"};//, "FinalStateParticles_KF"};
    String clusterCollection = "EcalClusters";
    double minSeedEnergy = 2.7;
    boolean correctECalSF = true;
    double ecalFeeSF = 0.91;
    double muMass = 0.10566;
    double mumass2 = muMass * muMass;

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        zProfileBottomMatched = aida.profile1D("Bottom Matched Track thetaY vs z0 profile", 25, 0.032, 0.052);
        zProfileBottomGBL = aida.profile1D("Bottom GBLTrack thetaY vs z0 profile", 25, 0.032, 0.052);
    }

    public void process(EventHeader event) {
        if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
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
//            analyzeV0eemumu(event);
        }
        if (event.hasCollection(Cluster.class, clusterCollection)) {
            analyzeClusters(event);
        }
    }

    private void analyzeRP(EventHeader event) {
        for (String s : finalStateParticleCollectionNames) {
            if (event.hasCollection(ReconstructedParticle.class, s)) {
                String dir = s + " ReconstructedParticle Analysis";
                aida.tree().mkdirs(dir);
                aida.tree().cd(dir);
                List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, s);
                for (ReconstructedParticle rp : rpList) {
                    int pdgId = rp.getParticleIDUsed().getPDG();
                    if (pdgId == 11) {
                        String trackType = "SeedTrack ";
                        if (TrackType.isGBL(rp.getType())) {
                            trackType = "GBL ";
                        }
                        if (rp.getType() == 1) {
                            trackType = "Kalman ";
                        }
//            if (!TrackType.isGBL(rp.getType())) {
//                continue;
//            }
                        Track t = rp.getTracks().get(0);
                        int trackFinderType = t.getType();
                        int nHits = t.getTrackerHits().size();
                        String id = pdgId == 11 ? "electron" : "positron";
                        Hep3Vector pmom = rp.getMomentum();
                        double thetaY = atan2(pmom.y(), pmom.z());//asin(pmom.y() / pmom.magnitude());
                        double z0 = t.getTrackStates().get(0).getZ0();
                        String torb = isTopTrack(t) ? " top " : " bottom ";
                        aida.histogram1D(trackType + torb + id + " track momentum", 100, 0., 10.).fill(rp.getMomentum().magnitude());

                        aida.cloud1D(trackType + torb + id + " |thetaY|").fill(abs(thetaY));
                        aida.histogram1D(trackType + torb + id + " z0", 100, -2., 2.).fill(z0);
                        aida.cloud2D(trackType + torb + id + " |thetaY| vs z0").fill(abs(thetaY), z0);
                        aida.profile1D(trackType + torb + id + " |thetaY| vs z0 profile", 10, 0.01, 0.1).fill(abs(thetaY), z0);
                        if (trackType.equals("MatchedTrack ") && torb.equals(" bottom ") && id.equals("electron")) {
                            zProfileBottomMatched.fill(abs(thetaY), z0);
                        }
                        if (trackType.equals("GBL ") && torb.equals(" bottom ") && id.equals("electron")) {
                            zProfileBottomGBL.fill(abs(thetaY), z0);
                        }

                        List<Cluster> clusters = rp.getClusters();
                        if (!clusters.isEmpty()) {
                            Cluster c = clusters.get(0);
                            analyzeCluster(c, id);
                            double[] cPos = c.getPosition();
                            aida.histogram2D(id + " cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cPos[0], cPos[1]);
                            aida.histogram1D(trackType + torb + id + " cluster energy", 100, 0., 10.).fill(c.getEnergy());
                            aida.histogram1D(trackType + torb + id + " cluster energy over track momentum EoverP", 100, 0., 2.).fill(c.getEnergy() / rp.getMomentum().magnitude());
                            aida.histogram1D(trackType + torb + id + " track momentum with cluster", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                            aida.histogram1D(trackType + torb + id + " track momentum with cluster " + nHits + " hits", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                            aida.histogram1D(trackType + " " + trackFinderType + torb + id + " track momentum with cluster " + nHits + " hits", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                        }
                    }
                    if (pdgId == 22) {
                        String id = "photon";
                        Cluster c = rp.getClusters().get(0);
                        analyzeCluster(c, id);
                        double[] cPos = c.getPosition();
                        aida.histogram2D(id + " cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cPos[0], cPos[1]);
                        aida.histogram1D(id + " momentum", 100, 0., 10.).fill(rp.getMomentum().magnitude());
                    }
                }
                aida.tree().cd("..");
            }
        }
    }

    @Override
    protected void endOfData() {
        IAnalysisFactory af = IAnalysisFactory.create();
        IDataPointSetFactory dpsf = af.createDataPointSetFactory(aida.tree());
        IDataPointSet dps2DFromProf = dpsf.create("dps2DFromProf", zProfileBottomMatched);

    }

    private void analyzeV0(EventHeader event) {
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        String[] v0Dirs = {"UnconstrainedV0Candidates"};//, "UnconstrainedV0Candidates_KF"};
        for (String dir : v0Dirs) {
            if (event.hasCollection(ReconstructedParticle.class, dir)) {
                aida.tree().mkdirs(dir);
                aida.tree().cd(dir);
                List<ReconstructedParticle> v0List = event.get(ReconstructedParticle.class, dir);
                for (ReconstructedParticle v0 : v0List) {
                    String trackType = "SeedTrack ";
                    int minNhits = 5;
                    if (TrackType.isGBL(v0.getType())) {
                        trackType = "GBL ";
                    }
                    if (v0.getType() == 1) {
                        trackType = "Kalman ";
                        minNhits = 10;
                    }
                    Vertex uncVert = v0.getStartVertex();
                    Hep3Vector pVtxRot = VecOp.mult(beamAxisRotation, v0.getMomentum());
                    Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, uncVert.getPosition());
                    double theta = Math.acos(pVtxRot.z() / pVtxRot.magnitude());
                    double phi = Math.atan2(pVtxRot.y(), pVtxRot.x());

                    // this always has 2 tracks.
                    List<ReconstructedParticle> trks = v0.getParticles();
                    ReconstructedParticle ele = trks.get(0);
                    ReconstructedParticle pos = trks.get(1);
                    Track negTrack = ele.getTracks().get(0);
                    Track posTrack = pos.getTracks().get(0);

                    double tNeg = getMyTrackTime(negTrack, hitToStrips, hitToRotated);
                    double tPos = getMyTrackTime(posTrack, hitToStrips, hitToRotated);

                    double trackDeltaT = tNeg - tPos;
                    aida.cloud1D("track deltaT").fill(trackDeltaT);
                    int eNhits = ele.getTracks().get(0).getTrackerHits().size();
                    int pNhits = pos.getTracks().get(0).getTrackerHits().size();
                    double eMom = ele.getMomentum().magnitude();
                    double pMom = pos.getMomentum().magnitude();
                    aida.histogram1D("electron track nHits", 20, 0., 20.).fill(eNhits);
                    aida.histogram1D("positron track nHits", 20, 0., 20.).fill(pNhits);
                    aida.histogram1D("electron momentum", 100, 0., 6.0).fill(eMom);
                    aida.histogram1D("positron momentum", 100, 0., 6.0).fill(pMom);

                    if (eNhits >= minNhits && pNhits >= minNhits) {
                        aida.histogram1D("v0 x", 50, -5., 5.).fill(vtxPosRot.x());
                        aida.histogram1D("v0 y", 50, -2., 2.).fill(vtxPosRot.y());
                        aida.histogram1D("v0 z", 50, -25., 0.).fill(vtxPosRot.z());
                        aida.histogram1D("v0 chisq", 100, 0., 100.).fill(uncVert.getChi2());
                        aida.histogram1D("v0 x ele " + eNhits + " pos " + pNhits + " hits on track", 50, -5., 5.).fill(vtxPosRot.x());
                        aida.histogram1D("v0 y ele " + eNhits + " pos " + pNhits + " hits on track", 50, -2., 2.).fill(vtxPosRot.y());
                        aida.histogram1D("v0 z ele " + eNhits + " pos " + pNhits + " hits on track", 50, -25., 0.).fill(vtxPosRot.z());
                        aida.histogram1D("v0 energy", 100, 0., 10.).fill(v0.getEnergy());
                        aida.histogram1D("v0 mass", 50, 0., 0.5).fill(v0.getMass());
                        aida.histogram2D("v0 mass vs Z vertex", 50, 0., 0.5, 100, -20., 20.).fill(v0.getMass(), vtxPosRot.z());
                        aida.histogram2D("v0 mass vs Z vertex ele " + eNhits + " pos " + pNhits + " hits on track", 50, 0., 0.5, 100, -20., 20.).fill(v0.getMass(), vtxPosRot.z());
                        aida.profile1D("v0 mass vs Z vertex profile", 50, 0.05, 0.25).fill(v0.getMass(), vtxPosRot.z());
                        if (ele.getClusters().isEmpty()) {
                            aida.cloud1D("track deltaT no electron ECal Cluster").fill(trackDeltaT);
                            aida.histogram1D("psum no electron ECal Cluster", 100, 0., 6.0).fill(eMom + pMom);
                            aida.histogram1D("psum no electron ECal Cluster ele " + eNhits + " pos " + pNhits + " hits on track", 100, 0., 6.0).fill(eMom + pMom);
                        }
                        aida.histogram1D("psum", 100, 0., 6.0).fill(eMom + pMom);
                        aida.histogram1D("psum ele " + eNhits + " pos " + pNhits + " hits on track", 100, 0., 6.0).fill(eMom + pMom);
                        aida.histogram2D("electron vs positron momentum", 100, 0., 6.0, 100, 0., 6.).fill(eMom, pMom);
                        if (ele.getClusters().size() > 0 && pos.getClusters().size() > 0) {
                            Cluster negClus = ele.getClusters().get(0);
                            Cluster posClus = pos.getClusters().get(0);
                            // in time
                            double p1Time = ClusterUtilities.getSeedHitTime(negClus);
                            double p2Time = ClusterUtilities.getSeedHitTime(posClus);
                            double deltaTime = p1Time - p2Time;
                            aida.histogram1D("cluster pair delta time", 100, -5., 5.).fill(deltaTime);
                            aida.cloud1D("track deltaT both ECal Clusters").fill(trackDeltaT);
                            aida.cloud2D("track deltaT vs cluster deltaT both ECal Clusters").fill(trackDeltaT, deltaTime);
                            aida.histogram1D("psum both ECal Clusters", 100, 0., 6.0).fill(eMom + pMom);
                            aida.histogram1D("esum both ECal Clusters", 100, 0., 6.0).fill(ele.getClusters().get(0).getEnergy() + pos.getClusters().get(0).getEnergy());
                        }
                    }
                }
                aida.tree().cd("..");
            }
        }
    }

    private void analyzeV0eemumu(EventHeader event) {
        String[] vertexCollectionNames = {"BeamspotConstrainedV0Vertices", "BeamspotConstrainedV0Vertices_KF"};
        for (String vertexCollectionName : vertexCollectionNames) {
            System.out.println(vertexCollectionName);
            if (event.hasCollection(Vertex.class, vertexCollectionName)) {

                System.out.println("found " + vertexCollectionName + " collection");
                List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
                System.out.println("found " + vertices.size() + " vertices");
                aida.tree().mkdirs(vertexCollectionName + "_eemumu");
                aida.tree().cd(vertexCollectionName + "_eemumu");
                for (Vertex v : vertices) {
                    ReconstructedParticle v0 = v.getAssociatedParticle();
//                }
//            }
//        }
//        // analysis concentrating on comparison of V0s with e+e- vs mu+mu- in the 2019 data
//        String[] v0Dirs = {"BeamspotConstrainedV0Candidates", "BeamspotConstrainedV0Candidates_KF"};
//        for (String dir : v0Dirs) {
//            if (event.hasCollection(ReconstructedParticle.class, dir)) {
//                aida.tree().mkdirs(dir + "_eemumu");
//                aida.tree().cd(dir + "_eemumu");
//                List<ReconstructedParticle> v0List = event.get(ReconstructedParticle.class, dir);
//                for (ReconstructedParticle v0 : v0List) {
                    String trackType = "SeedTrack ";
                    int minNhits = 5;
                    if (TrackType.isGBL(v0.getType())) {
                        trackType = "GBL ";
                    }
                    if (v0.getType() == 1) {
                        trackType = "Kalman ";
                        minNhits = 10;
                    }
                    Vertex uncVert = v0.getStartVertex();
                    Hep3Vector pVtxRot = VecOp.mult(beamAxisRotation, v0.getMomentum());
                    Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, uncVert.getPosition());
                    double theta = Math.acos(pVtxRot.z() / pVtxRot.magnitude());
                    double phi = Math.atan2(pVtxRot.y(), pVtxRot.x());

                    // this always has 2 tracks.
                    List<ReconstructedParticle> trks = v0.getParticles();
                    ReconstructedParticle neg = trks.get(0);
                    ReconstructedParticle pos = trks.get(1);
                    // let's also require both to have clusters, since this will distinguish e from mu
                    if (!neg.getClusters().isEmpty() && !pos.getClusters().isEmpty()) {
                        Cluster negClus = neg.getClusters().get(0);
                        Cluster posClus = pos.getClusters().get(0);
                        // in time
                        double p1Time = ClusterUtilities.getSeedHitTime(negClus);
                        double p2Time = ClusterUtilities.getSeedHitTime(posClus);
                        double deltaTime = p1Time - p2Time;
                        aida.histogram1D("cluster pair delta time", 100, -5., 5.).fill(deltaTime);
                        double negE = negClus.getEnergy();
                        double posE = posClus.getEnergy();
                        int negNhits = neg.getTracks().get(0).getTrackerHits().size();
                        int posNhits = pos.getTracks().get(0).getTrackerHits().size();
                        double negMom = neg.getMomentum().magnitude();
                        double posMom = pos.getMomentum().magnitude();
                        double negEoverP = negE / negMom;
                        double posEoverP = posE / posMom;

                        if (negNhits >= minNhits && posNhits >= minNhits && abs(deltaTime) < 5.) {
                            aida.histogram1D("negative track nHits", 20, 0., 20.).fill(negNhits);
                            aida.histogram1D("positive track nHits", 20, 0., 20.).fill(posNhits);
                            aida.histogram1D("negative momentum", 100, 0., 6.0).fill(negMom);
                            aida.histogram1D("positive momentum", 100, 0., 6.0).fill(posMom);
                            aida.histogram1D("v0 x", 50, -5., 5.).fill(vtxPosRot.x());
                            aida.histogram1D("v0 y", 50, -2., 2.).fill(vtxPosRot.y());
                            aida.histogram1D("v0 z", 50, -25., 0.).fill(vtxPosRot.z());
                            aida.histogram1D("v0 x neg " + negNhits + " pos " + posNhits + " hits on track", 50, -5., 5.).fill(vtxPosRot.x());
                            aida.histogram1D("v0 y neg " + negNhits + " pos " + posNhits + " hits on track", 50, -2., 2.).fill(vtxPosRot.y());
                            aida.histogram1D("v0 z neg " + negNhits + " pos " + posNhits + " hits on track", 50, -25., 0.).fill(vtxPosRot.z());
                            aida.histogram1D("v0 energy", 100, 0., 10.).fill(v0.getEnergy());
                            aida.histogram1D("v0 mass", 50, 0., 0.5).fill(v0.getMass());
                            aida.histogram2D("v0 mass vs Z vertex", 50, 0., 0.5, 100, -20., 20.).fill(v0.getMass(), vtxPosRot.z());
                            aida.histogram2D("v0 mass vs Z vertex neg " + negNhits + " pos " + posNhits + " hits on track", 50, 0., 0.5, 100, -20., 20.).fill(v0.getMass(), vtxPosRot.z());
                            aida.profile1D("v0 mass vs Z vertex profile", 50, 0.05, 0.25).fill(v0.getMass(), vtxPosRot.z());
                            aida.histogram1D("psum", 100, 0., 6.0).fill(negMom + posMom);
                            aida.histogram1D("psum neg " + negNhits + " pos " + posNhits + " hits on track", 100, 0., 6.0).fill(negMom + posMom);
                            aida.histogram2D("negative vs positive momentum", 100, 0., 6.0, 100, 0., 6.).fill(negMom, posMom);
                            aida.histogram1D("psum both ECal Clusters", 100, 0., 6.0).fill(negMom + posMom);
                            aida.histogram1D("esum both ECal Clusters", 100, 0., 6.0).fill(neg.getClusters().get(0).getEnergy() + pos.getClusters().get(0).getEnergy());

                            aida.histogram2D("negative E vs positive E all", 100, 0., 5., 100, 0., 5.).fill(negE, posE);
                            aida.histogram2D("negative E vs positive E lowE", 100, 0., 0.5, 100, 0., 0.5).fill(negE, posE);
                            aida.histogram1D("negative E over P", 100, 0., 2.).fill(negEoverP);
                            aida.histogram1D("positive E over P", 100, 0., 2.).fill(posEoverP);
                            aida.histogram2D("negative vs positive E over P", 100, 0., 2., 100, 0., 2.).fill(negEoverP, posEoverP);

                            // define mu+mu- sample...
                            String v0type = "bkgnd";
                            if (negE < .3 && posE < .3) {
                                v0type = "mu+mu-";
                                Map<String, Double> vals = v.getParameters();
                                // System.out.println(vals);
                                double[] p1 = new double[4];
                                double[] p2 = new double[4];
                                double[] pV = new double[3];
                                p1[0] = vals.get("p1X");
                                p1[1] = vals.get("p1Y");
                                p1[2] = vals.get("p1Z");
                                p2[0] = vals.get("p2X");
                                p2[1] = vals.get("p2Y");
                                p2[2] = vals.get("p2Z");
                                double v0p = vals.get("V0P");
                                pV[0] = vals.get("V0Px");
                                pV[1] = vals.get("V0Py");
                                pV[2] = vals.get("V0Pz");
                                double mu1 = 0;
                                double mu2 = 0.;
                                for (int i = 0; i < 3; ++i) {
                                    mu1 += p1[i] * p1[i];
                                    mu2 += p2[i] * p2[i];
                                }
                                mu1 = sqrt(mu1 + mumass2);
                                mu2 = sqrt(mu2 + mumass2);
                                Momentum4Vector kvec1 = new Momentum4Vector(p1[0], p1[1], p1[2], mu1);
                                Momentum4Vector kvec2 = new Momentum4Vector(p2[0], p2[1], p2[2], mu2);
                                Lorentz4Vector mumusum = kvec1.plus(kvec2);
                                double mumumass = mumusum.mass();
                                aida.histogram1D("v0 mu+mu- mass " + v0type, 50, 0., 0.5).fill(mumumass);
                            }
                            // define e+e- sample...
                            if (negE > .3 && posE > .3) {
                                v0type = "e+e-";
                            }
                            aida.histogram1D("v0 mass " + v0type, 50, 0., 0.5).fill(v0.getMass());
                            aida.histogram1D("cluster pair delta time " + v0type, 100, -5., 5.).fill(deltaTime);
                            aida.histogram1D("negative momentum " + v0type, 100, 0., 6.0).fill(negMom);
                            aida.histogram1D("positive momentum " + v0type, 100, 0., 6.0).fill(posMom);
                            aida.histogram2D("negative vs positive momentum " + v0type, 100, 0., 6.0, 100, 0., 6.).fill(negMom, posMom);
                            aida.histogram2D("negative E vs positive E all " + v0type, 100, 0., 5., 100, 0., 5.).fill(negE, posE);
                            aida.histogram1D("negative E over P " + v0type, 100, 0., 2.).fill(negEoverP);
                            aida.histogram1D("positive E over P " + v0type, 100, 0., 2.).fill(posEoverP);
                            aida.histogram2D("negative vs positive E over P " + v0type, 100, 0., 2., 100, 0., 2.).fill(negEoverP, posEoverP);

                        }//end of check on track having greater than minHits
                    }// end of check on both clusters
                }// end of loop over vertices
                aida.tree().cd("..");
            } // end of loop over check on collection
        } // end of loop over vertex collections
    }

    private void analyzeClusters(EventHeader event) {

        List<Cluster> eventClusters = event.get(Cluster.class, clusterCollection);
        aida.tree().mkdirs(clusterCollection + " Analysis");
        aida.tree().cd(clusterCollection + " Analysis");
        int nClusters = eventClusters.size();
        aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(nClusters);

        for (Cluster c : eventClusters) {
            boolean isFiducial = TriggerModule.inFiducialRegion(c);
            String fiducial = isFiducial ? " fiducial " : " ";
            double[] cPos = c.getPosition();
            String topOrBottom = cPos[1] > 0 ? " top " : " bottom ";
            CalorimeterHit seed = c.getCalorimeterHits().get(0);
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            aida.histogram2D("cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
            //           aida.histogram1D(ix + " " + iy + " cluster energy", 100, 0., 5.0).fill(c.getEnergy());
            aida.histogram1D(topOrBottom + " cluster energy", 100, 0., 5.0).fill(c.getEnergy());
            aida.histogram1D(fiducial + topOrBottom + " cluster energy", 100, 0., 5.0).fill(c.getEnergy());
            aida.histogram1D(fiducial + topOrBottom + " cluster energy " + nClusters + " event clusters", 100, 0., 5.0).fill(c.getEnergy());

            aida.histogram1D("event cluster nHits", 20, 0., 20.).fill(c.getCalorimeterHits().size());
            aida.histogram1D("event cluster energy", 100, 0., 5.5).fill(c.getEnergy());
            aida.histogram1D(fiducial + "event cluster energy", 100, 0., 5.5).fill(c.getEnergy());

            aida.histogram2D("event cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
            if (isFiducial) {
                aida.histogram2D("fiducial cluster seed energy vs cluster energy", 100, 3., 5., 100, 0.5, 3.5).fill(c.getEnergy(), seed.getCorrectedEnergy());
            }
            if (seed.getCorrectedEnergy() > minSeedEnergy) {
                aida.histogram1D(topOrBottom + " cluster energy, seed > " + minSeedEnergy, 100, 0., 6.0).fill(c.getEnergy());
                aida.histogram1D(topOrBottom + " cluster energy, seed > " + minSeedEnergy + " " + nClusters + " event clusters", 100, 0., 6.0).fill(c.getEnergy());
                aida.histogram1D(fiducial + topOrBottom + " cluster energy, seed > " + minSeedEnergy, 100, 0., 6.0).fill(c.getEnergy());
                aida.histogram1D(fiducial + topOrBottom + " cluster energy, seed > " + minSeedEnergy + " " + nClusters + " event clusters", 100, 0., 6.0).fill(c.getEnergy());
            }
            List<CalorimeterHit> clusterHits = c.getCalorimeterHits();
            CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
            aida.histogram1D("cluster seed time", 100, 0., 200.).fill(seedHit.getTime());
            if (seed != seedHit) {
                System.out.println("Panic!");
            }
            for (CalorimeterHit hit : clusterHits) {
                aida.histogram1D("cluster hit corrected energy", 200, 0., 5.).fill(hit.getCorrectedEnergy());
                aida.histogram1D("cluster hit corrected energy low end", 200, 0., 0.1).fill(hit.getCorrectedEnergy());
//                aida.histogram1D("cluster hit raw energy", 200, 0., 5.).fill(hit.getRawEnergy());  //!? always zero?
                if (seedHit != hit) {
                    aida.histogram1D("cluster seed hit time - hit time", 100, -20., 20.).fill(seedHit.getTime() - hit.getTime());
                }
            }
        }
        aida.tree().cd("..");
    }

    void analyzeCluster(Cluster cluster, String type) {
        double[] cPos = cluster.getPosition();
        String topOrBottom = cPos[1] > 0 ? " top " : " bottom ";
        aida.tree().mkdirs("clusterAnalysis");
        aida.tree().cd("clusterAnalysis");
        CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
        int ix = seed.getIdentifierFieldValue("ix");
        int iy = seed.getIdentifierFieldValue("iy");
        aida.histogram2D("cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
//        aida.histogram1D(ix + " " + iy + " " + type + " cluster energy", 100, 0., 5.0).fill(cluster.getEnergy());
        aida.histogram1D(topOrBottom + " cluster energy", 100, 0., 5.0).fill(cluster.getEnergy());
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

    public static double getMyTrackTime(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double meanTime = 0;
        List<TrackerHit> stripHits = getMyStripHits(track, hitToStrips, hitToRotated);
        System.out.println("found " + stripHits.size() + " strip Hits for Track " + track);
        for (TrackerHit hit : stripHits) {
            meanTime += hit.getTime();
        }
        meanTime /= stripHits.size();
        return meanTime;
    }

    public static List<TrackerHit> getMyStripHits(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        List<TrackerHit> hits = new ArrayList<TrackerHit>();
        System.out.println("track has " + track.getTrackerHits() + " trackerhits");
        for (TrackerHit hit : track.getTrackerHits()) {
            System.out.println("hit " + hit + " has " + hitToStrips.allFrom(hitToRotated.from(hit)).size() + " hits");
            hits.addAll(hitToStrips.allFrom(hitToRotated.from(hit)));
        }
        System.out.println("found " + hits.size() + " strip hits for this track");
        return hits;
    }
}
