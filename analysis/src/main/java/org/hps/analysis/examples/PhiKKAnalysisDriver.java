package org.hps.analysis.examples;

import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.List;
import java.util.Map;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 * Simple analysis to search for φ→K⁺K⁻ Loop over existing V0s, recalculate mass
 * using different particle hypotheses for the constituent particles.
 *
 * @author Norman A. Graf
 */
public class PhiKKAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String vertexCollectionName = "UnconstrainedV0Vertices";
    String[] particleType = {"electron", "positron"};
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[4];
    double[] p2 = new double[4];
    double[] pV = new double[3];
    double emass = 0.000511;
    double kmass = 0.493667;
    double[] masses = {0.000511, 0.10566, 0.13957, 0.493667};
    String[] particleNames = {"electron", "muon", "pion", "kaon"};
    double[] minVals = {0.0, 0.15, 0.25, 0.95};
    double[] maxVals = {0.85, 0.85, 0.85, 1.45};
    double phimass = 1.019;
    double emass2 = emass * emass;
    double kmass2 = kmass * kmass;
    boolean debug = false;
    double clusterEcut = 0.3;

    // skimming
    boolean _skimEvents = true;
    int _numberOfEventsSelected;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        // just a few sanity checks
        List<Cluster> eventClusters = event.get(Cluster.class, "EcalClustersCorr");
        aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(eventClusters.size());
        for (Cluster c : eventClusters) {
            aida.histogram1D("event cluster nHits", 20, 0., 20.).fill(c.getCalorimeterHits().size());
            aida.histogram1D("event cluster energy", 100, 0., 1.5).fill(c.getEnergy());
            aida.histogram2D("event cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
        }
        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
//        if (V0List.size() != 2) {
//            return;
//        }
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        for (Vertex v : vertices) {
            ReconstructedParticle V0 = v.getAssociatedParticle();

            Cluster[] clusters = new Cluster[2];
            List<ReconstructedParticle> particles = V0.getParticles();
            for (int i = 0; i < 2; ++i) {
                ReconstructedParticle rp = particles.get(i);
                Cluster clus = null;
                if (rp.getClusters().size() == 1) {
                    clus = rp.getClusters().get(0);
                    clusters[i] = clus;
                }
                boolean isGBL = TrackType.isGBL(rp.getTracks().get(0).getType());
                String trackType = isGBL ? "gbl" : "matchedTracks";
                aida.tree().mkdirs(trackType);
                aida.tree().cd(trackType);
                Track t = rp.getTracks().get(0);
                int nHits = t.getTrackerHits().size();
                double dEdx = t.getdEdx();
                aida.histogram1D(particleType[i] + " track nHits", 20, 0., 20.).fill(nHits);
                aida.histogram1D(particleType[i] + " track dEdx", 200, 0., 0.003).fill(dEdx);
                // cluster plots
                if (clus != null) {
                    aida.histogram1D(particleType[i] + " cluster nHits", 20, 0., 20.).fill(clus.getCalorimeterHits().size());
                    aida.histogram1D(particleType[i] + " cluster energy", 100, 0., 1.5).fill(clus.getEnergy());
                    aida.histogram2D(particleType[i] + " cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(clus.getPosition()[0], clus.getPosition()[1]);
                    if (clus.getEnergy() < clusterEcut) {
                        aida.histogram1D(particleType[i] + " MIP track nHits", 20, 0., 20.).fill(nHits);
                        aida.histogram1D(particleType[i] + " MIP track dEdx", 200, 0., 0.003).fill(dEdx);
                        aida.histogram1D(particleType[i] + " MIP cluster nHits", 20, 0., 20.).fill(clus.getCalorimeterHits().size());
                    }
                }
                aida.tree().cd("..");
            }
            ReconstructedParticle ele = particles.get(0);
            ReconstructedParticle pos = particles.get(1);

            boolean isGBL = TrackType.isGBL(ele.getTracks().get(0).getType());
            String trackType = isGBL ? "gbl" : "matchedTracks";

            boolean noClusters = (ele.getClusters().isEmpty() && pos.getClusters().isEmpty());
            boolean oneCluster = ((ele.getClusters().size() == 1 && pos.getClusters().isEmpty()) || (ele.getClusters().size() == 0 && pos.getClusters().size() == 1));
            boolean twoClusters = (ele.getClusters().size() == 1 && pos.getClusters().size() == 1);

            aida.tree().mkdirs(trackType);
            aida.tree().cd(trackType);

            Hep3Vector vertexPosition = v.getPosition();
            Map<String, Double> vals = v.getParameters();
            // System.out.println(vals);
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
            double k1 = 0;
            double k2 = 0.;
            for (int i = 0; i < 3; ++i) {
                k1 += p1[i] * p1[i];
                k2 += p2[i] * p2[i];
            }
            k1 = sqrt(k1 + kmass2);
            k2 = sqrt(k2 + kmass2);
            Momentum4Vector kvec1 = new Momentum4Vector(p1[0], p1[1], p1[2], k1);
            Momentum4Vector kvec2 = new Momentum4Vector(p2[0], p2[1], p2[2], k2);
            Lorentz4Vector kksum = kvec1.plus(kvec2);
            double kkmass = kksum.mass();
            double invMass = vals.get("invMass");

            aida.histogram1D("vertex invariant mass", 100, 0., 0.4).fill(invMass);
            aida.histogram1D("vertex x", 100, -3., 3.).fill(vertexPosition.x());
            aida.histogram1D("vertex y", 100, -1., 1.).fill(vertexPosition.y());
            aida.histogram1D("vertex z", 200, -25., 0.).fill(vertexPosition.z());
            aida.histogram2D("vertex x vs y", 100, -3., 3., 100, -1., 1.).fill(vertexPosition.x(), vertexPosition.y());
            aida.histogram1D("vertex invariant mass K+K-", 100, 0.95, 1.95).fill(kkmass);
            aida.histogram1D("vertex invariant mass phi search", 100, 0.98, 1.06).fill(kkmass);
            aida.histogram1D("vertex momentum", 100, 0., 5.0).fill(v0p);
            if (noClusters) {
                aida.histogram1D("vertex invariant mass no clusters", 100, 0., 0.4).fill(invMass);
                aida.histogram1D("vertex invariant mass phi search no clusters", 100, 0.98, 1.06).fill(kkmass);
            }
            if (oneCluster) {
                aida.histogram1D("vertex invariant mass one cluster", 100, 0., 0.4).fill(invMass);
                aida.histogram1D("vertex invariant mass phi search one cluster", 100, 0.98, 1.06).fill(kkmass);
            }
            if (twoClusters) {
                aida.histogram1D("vertex invariant mass two clusters", 100, 0., 0.4).fill(invMass);
                aida.histogram1D("vertex invariant mass phi search two clusters", 100, 0.98, 1.06).fill(kkmass);
                aida.histogram2D("electron vs positron cluster energy", 100, 0., 1.5, 100, 0., 1.5).fill(clusters[0].getEnergy(), clusters[1].getEnergy());
                if (clusters[0].getEnergy() < clusterEcut && clusters[1].getEnergy() < clusterEcut) {
                    double deltaT = ClusterUtilities.findSeedHit(clusters[0]).getTime() - ClusterUtilities.findSeedHit(clusters[1]).getTime();
                    if (abs(deltaT) < 5.) {
                        //two in-time MIP-like clusters: keep
                        skipEvent = false;
                        aida.histogram1D(particleType[0] + " track momentum", 200, 0., 4.5).fill(ele.getMomentum().magnitude());
                        aida.histogram1D(particleType[1] + " track momentum", 200, 0., 4.5).fill(pos.getMomentum().magnitude());
                        aida.histogram2D("electron vs positron track momentum", 100, 0., 4.5, 100, 0., 4.5).fill(ele.getMomentum().magnitude(), pos.getMomentum().magnitude());
                        aida.histogram1D(particleType[0] + " two MIP clusters nHits", 20, 0., 20.).fill(clusters[0].getCalorimeterHits().size());
                        aida.histogram1D(particleType[1] + " two MIP clusters nHits", 20, 0., 20.).fill(clusters[1].getCalorimeterHits().size());
                        aida.histogram1D(particleType[0] + " two MIP clusters energy", 100, 0., 0.3).fill(clusters[0].getEnergy());
                        aida.histogram1D(particleType[1] + " two MIP clusters energy", 100, 0., 0.3).fill(clusters[1].getEnergy());

                        aida.histogram1D(particleType[0] + " two MIP clusters track dEdx", 200, 0., 0.003).fill(ele.getTracks().get(0).getdEdx());
                        aida.histogram1D(particleType[1] + " two MIP clusters track dEdx", 200, 0., 0.003).fill(pos.getTracks().get(0).getdEdx());

                        aida.histogram1D("vertex invariant mass phi search two clusters below " + clusterEcut, 100, 0.98, 1.06).fill(kkmass);
                        aida.histogram1D("vertex invariant mass two clusters below " + clusterEcut, 100, 0., 0.4).fill(invMass);
                        aida.histogram1D("two cluster deltaT", 100, -5., 5.).fill(deltaT);
                        aida.histogram2D(particleType[0] + " cluster x vs y two clusters below " + clusterEcut, 320, -270.0, 370.0, 90, -90.0, 90.0).fill(clusters[0].getPosition()[0], clusters[0].getPosition()[1]);
                        aida.histogram2D(particleType[1] + " cluster x vs y two clusters below " + clusterEcut, 320, -270.0, 370.0, 90, -90.0, 90.0).fill(clusters[1].getPosition()[0], clusters[1].getPosition()[1]);
                        aida.histogram1D("vertex x MIP clusters", 100, -3., 3.).fill(vertexPosition.x());
                        aida.histogram1D("vertex y MIP clusters", 100, -1., 1.).fill(vertexPosition.y());
                        aida.histogram1D("vertex z MIP clusters", 200, -25., 0.).fill(vertexPosition.z());
                        aida.histogram2D("vertex x vs y MIP clusters", 100, -3., 3., 100, -1., 1.).fill(vertexPosition.x(), vertexPosition.y());
                        for (int j = 0; j < masses.length; ++j) {
                            double mass2 = masses[j] * masses[j];
                            k1 = 0;
                            k2 = 0.;
                            for (int i = 0; i < 3; ++i) {
                                k1 += p1[i] * p1[i];
                                k2 += p2[i] * p2[i];
                            }
                            k1 = sqrt(k1 + mass2);
                            k2 = sqrt(k2 + mass2);
                            kvec1 = new Momentum4Vector(p1[0], p1[1], p1[2], k1);
                            kvec2 = new Momentum4Vector(p2[0], p2[1], p2[2], k2);
                            kksum = kvec1.plus(kvec2);
                            kkmass = kksum.mass();
//                        aida.histogram1D("vertex invariant mass phi search two clusters below " + clusterEcut + " " + particleNames[j] + " hypothesis", 200, 0., 2.).fill(kkmass);
                            aida.histogram1D("vertex invariant mass phi search two clusters below " + clusterEcut + " " + particleNames[j] + " hypothesis", 200, minVals[j], maxVals[j]).fill(kkmass);
                        }
                    }
                }
            }
            aida.tree().cd("..");
        }
        if (skipEvent && _skimEvents) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    @Override
    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }

    public void setSkimEvents(boolean b) {
        _skimEvents = b;
    }
}
