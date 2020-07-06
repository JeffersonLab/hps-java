package org.hps.analysis.ecal;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Select FinalStateParticles which are consistent with muons to determine the
 * ECal single-crystal gain calibration
 *
 * Selecting events with dimuons provides a cleaner sample, albeit with lower
 * statistics
 *
 * @author Norman A. Graf
 */
public class EcalMuonGainCalibrationDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    // skimming
    boolean _skimEvents = true;
    int _numberOfEventsSelected;
    double clusterEcut = 0.3;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        // single muon analysis
        if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
            for (ReconstructedParticle rp : rpList) {
                int pdgId = rp.getParticleIDUsed().getPDG();
                if (abs(pdgId) == 11) {  // require a charged particle...
                    List<Cluster> clusters = rp.getClusters();
                    if (!clusters.isEmpty()) { // has to be associated with a cluster
                        Cluster c = clusters.get(0);
                        if (c.getCalorimeterHits().size() == 1) { // only analyze single-crystal clusters to extract MIP peak
                            String id = pdgId == 11 ? "mu-" : "mu+";
                            aida.tree().mkdirs("single muon");
                            aida.tree().cd("single muon");
                            analyzeCluster(c, id);
                            aida.tree().cd("..");
                            skipEvent = false;
                        }
                    }
                }
            }
        }

        // dimuon analysis
        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        List<Vertex> vertices = event.get(Vertex.class, "UnconstrainedV0Vertices");
        for (Vertex v : vertices) {
            boolean goodMuPair = false;
            ReconstructedParticle muPlus = null;
            ReconstructedParticle muMinus = null;
            ReconstructedParticle V0 = v.getAssociatedParticle();
            boolean isGBL = false;
            Cluster[] clusters = new Cluster[2];
            List<ReconstructedParticle> particles = V0.getParticles();
            isGBL = TrackType.isGBL(particles.get(0).getTracks().get(0).getType());
            if (isGBL) {
                for (int i = 0; i < 2; ++i) {
                    ReconstructedParticle rp = particles.get(i);
                    Cluster clus = null;
                    if (rp.getClusters().size() == 1) {
                        clus = rp.getClusters().get(0);
                        clusters[i] = clus;
                    }
                }

                muMinus = particles.get(0);
                muPlus = particles.get(1);

                boolean noClusters = (muPlus.getClusters().isEmpty() && muMinus.getClusters().isEmpty());
                boolean oneCluster = ((muPlus.getClusters().size() == 1 && muMinus.getClusters().isEmpty()) || (muPlus.getClusters().size() == 0 && muMinus.getClusters().size() == 1));
                boolean twoClusters = (muPlus.getClusters().size() == 1 && muMinus.getClusters().size() == 1);

                if (oneCluster) {
                    aida.tree().mkdirs("dimuon one cluster");
                    aida.tree().cd("dimuon one cluster");
                    if (muPlus.getClusters().size() == 1) {
                        analyzeCluster(muPlus.getClusters().get(0), "mu+");
                    }
                    if (muMinus.getClusters().size() == 1) {
                        analyzeCluster(muMinus.getClusters().get(0), "mu-");
                    }
                    aida.tree().cd("..");
                }
                if (twoClusters) {
                    if (clusters[0].getEnergy() < clusterEcut && clusters[1].getEnergy() < clusterEcut) {
                        double deltaT = ClusterUtilities.findSeedHit(clusters[0]).getTime() - ClusterUtilities.findSeedHit(clusters[1]).getTime();
                        if (abs(deltaT) < 5.) {
                            //two in-time MIP-like clusters: keep
                            skipEvent = false;
                            aida.tree().mkdirs("dimuon");
                            aida.tree().cd("dimuon");
                            analyzeCluster(muPlus.getClusters().get(0), "mu+");
                            analyzeCluster(muMinus.getClusters().get(0), "mu-");
                            aida.tree().cd("..");
                        } // end of deltaT cut
                    }
                } // end of check for clusters on both tracks.
            } // end of check on GBL track type
        } // end of loop over vertices
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

    void analyzeCluster(Cluster cluster, String type) {
        if (cluster.getCalorimeterHits().size() == 1) {
            aida.tree().mkdirs("clusterAnalysis");
            aida.tree().cd("clusterAnalysis");
            CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            String fid = TriggerModule.inFiducialRegion(cluster) ? "fiducial " : "edge ";
            aida.histogram1D(fid + type + " single-crystal cluster energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.histogram2D(fid + "cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
            aida.histogram1D(type + " single-crystal cluster energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.histogram2D("cluster ix vs iy", 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
            aida.histogram1D(ix + " " + iy + " " + type + " crystal energy", 50, 0.1, 0.3).fill(cluster.getEnergy());
            aida.tree().cd("..");
        }
    }
}
