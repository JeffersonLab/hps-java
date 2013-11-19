package org.lcsim.hps.analysis.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.List;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Reads clusters and makes trigger decision using opposite quadrant criterion.
 * Prints triggers to file if file path specified.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TestRunRateAnalysis.java,v 1.1 2013/02/25 22:39:26 meeg Exp $
 */
public class TestRunRateAnalysis extends Driver {

    AIDA aida = AIDA.defaultInstance();
    IHistogram2D eClusterVsP, photonEClusterVsP, electronEClusterVsP, positronEClusterVsP;
    IHistogram2D eVsP, photonEVsP, electronEVsP, positronEVsP;
    IHistogram1D eClusterOverP, photonEClusterOverP, electronEClusterOverP, positronEClusterOverP;
    IHistogram1D eOverP, photonEOverP, electronEOverP, positronEOverP;
    private String clusterCollectionName;
    private String hitCollectionName = "EcalHits";
    int nTriggers;
    private double clusterEnergyLow = 10;    //
    int deadtimelessTriggerCount;
    int[] triggersY = new int[5];

    public TestRunRateAnalysis() {
    }

    @Override
    public void startOfData() {
        deadtimelessTriggerCount = 0;

        eClusterVsP = aida.histogram2D("All Cluster E vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        photonEClusterVsP = aida.histogram2D("Photon Cluster E vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        electronEClusterVsP = aida.histogram2D("Electron Cluster E vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        positronEClusterVsP = aida.histogram2D("Positron Cluster E vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);

        eClusterOverP = aida.histogram1D("Cluster E over Pz, Pz > 0.6", 100, 0.0, 2.0);
        photonEClusterOverP = aida.histogram1D("Photon Cluster E over Pz, Pz > 0.6", 100, 0.0, 2.0);
        electronEClusterOverP = aida.histogram1D("Electron Cluster E over Pz, Pz > 0.6", 100, 0.0, 2.0);
        positronEClusterOverP = aida.histogram1D("Positron Cluster E over Pz, Pz > 0.6", 100, 0.0, 2.0);

        eVsP = aida.histogram2D("All Edep vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        photonEVsP = aida.histogram2D("Photon Edep vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        electronEVsP = aida.histogram2D("Electron Edep vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);
        positronEVsP = aida.histogram2D("Positron Edep vs. Pz", 100, 0.0, 2000.0, 100, 0.0, 2000.0);

        eOverP = aida.histogram1D("All Edep over Pz, Pz > 0.6", 100, 0.0, 2.0);
        photonEOverP = aida.histogram1D("Photon Edep over Pz, Pz > 0.6", 100, 0.0, 2.0);
        electronEOverP = aida.histogram1D("Electron Edep over Pz, Pz > 0.6", 100, 0.0, 2.0);
        positronEOverP = aida.histogram1D("Positron Edep over Pz, Pz > 0.6", 100, 0.0, 2.0);
    }

    public void setClusterEnergyLow(double clusterEnergyLow) {
        this.clusterEnergyLow = clusterEnergyLow;
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    @Override
    public void process(EventHeader event) {
        //System.out.println(this.getClass().getCanonicalName() + " - process");

        // MCParticles
        List<MCParticle> mcparticles = event.get(MCParticle.class).get(0);

        if (mcparticles.isEmpty()) {
            return;
        }
//        if (mcparticles.size() != 1) {
//            throw new RuntimeException("expected exactly 1 MCParticle");
//        }
//        MCParticle particle = mcparticles.get(0);

        // Get the list of raw ECal hits.
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
        if (clusters == null) {
            throw new RuntimeException("Event is missing ECal clusters collection!");
        }

        boolean trigger = false;

        for (HPSEcalCluster cluster : clusters) {
            if (cluster.getEnergy() > clusterEnergyLow && cluster.getSeedHit().getIdentifierFieldValue("ix") < 0) {
//            if (cluster.getEnergy() > clusterEnergyLow && cluster.getSeedHit().getIdentifierFieldValue("iy")>0 && cluster.getSeedHit().getIdentifierFieldValue("ix")<0) {
                triggersY[Math.abs(cluster.getSeedHit().getIdentifierFieldValue("iy")) - 1]++;
                if (Math.abs(cluster.getSeedHit().getIdentifierFieldValue("iy")) > 1) {
                    trigger = true;
                }
            }
            if (cluster.getSeedHit().getIdentifierFieldValue("ix") < 0 && Math.abs(cluster.getSeedHit().getIdentifierFieldValue("iy")) > 1) {
                for (MCParticle particle : mcparticles) {
                    if (ParticleTypeClassifier.isElectron(particle.getPDGID())) {
                        electronEClusterVsP.fill(1000.0 * particle.getPZ(), 1000.0 * cluster.getEnergy());
                        if (particle.getPZ() > 0.6) {
                            electronEClusterOverP.fill(cluster.getEnergy() / particle.getPZ());
                        }
                    } else if (ParticleTypeClassifier.isPositron(particle.getPDGID())) {
                        positronEClusterVsP.fill(1000.0 * particle.getPZ(), 1000.0 * cluster.getEnergy());
                        if (particle.getPZ() > 0.6) {
                            positronEClusterOverP.fill(cluster.getEnergy() / particle.getPZ());
                        }
                    } else if (ParticleTypeClassifier.isPhoton(particle.getPDGID())) {
                        photonEClusterVsP.fill(1000.0 * particle.getPZ(), 1000.0 * cluster.getEnergy());
                        if (particle.getPZ() > 0.6) {
                            photonEClusterOverP.fill(cluster.getEnergy() / particle.getPZ());
                        }
                    }
                    eClusterVsP.fill(1000.0 * particle.getPZ(), 1000.0 * cluster.getEnergy());
                    if (particle.getPZ() > 0.6) {
                        eClusterOverP.fill(cluster.getEnergy() / particle.getPZ());
                    }
                }
            }
        }
        if (trigger) {
            deadtimelessTriggerCount++;
        }

        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, hitCollectionName);
        if (hits == null) {
            throw new RuntimeException("Event is missing ECal hits collection!");
        }
        double totalE = 0;
        for (CalorimeterHit hit : hits) {
            totalE += hit.getRawEnergy();
        }

        for (MCParticle particle : mcparticles) {
            if (ParticleTypeClassifier.isElectron(particle.getPDGID())) {
                electronEVsP.fill(1000.0 * particle.getPZ(), 1000.0 * totalE);
                if (particle.getPZ() > 0.6) {
                    electronEOverP.fill(totalE / particle.getPZ());
                }
            } else if (ParticleTypeClassifier.isPositron(particle.getPDGID())) {
                positronEVsP.fill(1000.0 * particle.getPZ(), 1000.0 * totalE);
                if (particle.getPZ() > 0.6) {
                    positronEOverP.fill(totalE / particle.getPZ());
                }
            } else if (ParticleTypeClassifier.isPhoton(particle.getPDGID())) {
                photonEVsP.fill(1000.0 * particle.getPZ(), 1000.0 * totalE);
                if (particle.getPZ() > 0.6) {
                    photonEOverP.fill(totalE / particle.getPZ());
                }
            }
            eVsP.fill(1000.0 * particle.getPZ(), 1000.0 * totalE);
            if (particle.getPZ() > 0.6) {
                eOverP.fill(totalE / particle.getPZ());
            }
        }
    }

    @Override
    public void endOfData() {
        System.out.printf("Trigger count without dead time: %d\n", deadtimelessTriggerCount);
        System.out.format("Triggers vs. Y: %d\t%d\t%d\t%d\t%d, total %d\n", triggersY[0], triggersY[1], triggersY[2], triggersY[3], triggersY[4], deadtimelessTriggerCount);
        super.endOfData();
    }
}