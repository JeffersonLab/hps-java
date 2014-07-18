package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>FADCProgrammableTriggerDriver</code> takes a list of
 * clusters and performs both single cluster and cluster pair trigger
 * cuts. Should a given pair pass all cuts, a trigger is reported to
 * the simulation. The driver requires that all cut values be passed
 * to it through a driver. Parameters are defined for each beam energy
 * as<br/><br/>
 * <b>2.2 GeV Beam</b>
 * <ul>
 * <li>&#60;clusterEnergyHighThreshold&#62;1.6&#60;/clusterEnergyHighThreshold&#62;</li>
 * <li>&#60;clusterEnergyLowThreshold&#62;0.1&#60;/clusterEnergyLowThreshold&#62;</li>
 * <li>&#60;hitCountLowThreshold&#62;1&#60;/hitCountLowThreshold&#62;</li>
 * <li>&#60;energyDifferenceHighThreshold>1.5&#60;/energyDifferenceHighThreshold></li>
 * <li>&#60;coplanarityHighCut&#62;45&#60;/coplanarityHighCut&#62;</li>
 * <li>&#60;energySumHighThreshold&#62;1.7&#60;/energySumHighThreshold&#62;</li>
 * <li>&#60;energySlopeParamF&#62;0.0055&#60;/energySlopeParamF&#62;</li>
 * <li>&#60;energySlopeThreshold&#62;1.1&#60;/energySlopeThreshold&#62;</li>
 * <li>&#60;beamEnergy&#62;2.2&#60;/beamEnergy&#62;</li>
 * </ul>
 *
 * @author Kyle McCarty
 * @author Omar Moreno
 * @author Sho Uemura
 */
public class FADCProgrammableTriggerDriver extends TriggerDriver {
    // Histograms!
    IHistogram1D clusterSeedEnergyPlot;
    IHistogram1D clusterTotalEnergyPlot;
    IHistogram1D clusterHitCountPlot;
    IHistogram1D pairEnergySumPlot;
    IHistogram1D pairEnergyDifferencePlot;
    IHistogram1D pairCoplanarityPlot;
    IHistogram1D pairEnergySlopePlot;
    
    IHistogram1D edgeClusterSeedEnergyPlot;
    IHistogram1D edgeClusterTotalEnergyPlot;
    IHistogram1D edgeTClusterSeedEnergyPlot;
    IHistogram1D edgeTClusterTotalEnergyPlot;    
    IHistogram1D nedgeClusterSeedEnergyPlot;
    IHistogram1D nedgeClusterTotalEnergyPlot;
    IHistogram1D nedgeTClusterSeedEnergyPlot;
    IHistogram1D nedgeTClusterTotalEnergyPlot;
    IHistogram2D edgeSeedDistribution;
    IHistogram2D edgeTSeedDistribution;
    
    IHistogram1D singleHitSeedEnergyPlot;
    IHistogram1D particleEnergyPlot;
    IHistogram2D singleHitClusterDistribution;
    IHistogram1D particleMomentumPlot;
    IHistogram2D particleMomentumDistribution;
    IHistogram2D clusterSizeBySeedHit;
    List<MCParticle> particleList;
    
    IHistogram1D tClusterSeedEnergyPlot;
    IHistogram1D tClusterTotalEnergyPlot;
    IHistogram1D tClusterHitCountPlot;
    IHistogram1D tPairEnergySumPlot;
    IHistogram1D tPairEnergyDifferencePlot;
    IHistogram1D tPairCoplanarityPlot;
    IHistogram1D tPairEnergySlopePlot;
    
    // Programmable cut thresholds.
    /**
    private double clusterEnergyLowThreshold = 0.0;
    private double clusterEnergyHighThreshold = Double.MAX_VALUE;
    private double clusterSeedEnergyLowThreshold = 0.0;
    private double clusterSeedEnergyHighThreshold = Double.MAX_VALUE;
    private double energyDifferenceHighThreshold = Double.MAX_VALUE;
    private double coplanarityHighCut = Double.MAX_VALUE;
    private double energySumLowThreshold = 0.0;
    private double energySumHighThreshold = Double.MAX_VALUE;
    private double energySlopeParamF = 0.0;
    private double energySlopeHighThreshold = Double.MIN_VALUE;
    private int hitCountLowThreshold = 1;
    **/
    private double clusterEnergyLowThreshold = 0.1;
    private double clusterEnergyHighThreshold = 1.5;
    private double clusterSeedEnergyLowThreshold = 0.1;
    private double clusterSeedEnergyHighThreshold = Double.MAX_VALUE;
    private double energyDifferenceHighThreshold = 2.2;
    private double coplanarityHighCut = 35;
    private double energySumLowThreshold = 0.0;
    private double energySumHighThreshold = 1.9;
    private double energySlopeParamF = 0.005500;
    private double energySlopeHighThreshold = 1.100000;
    private int hitCountLowThreshold = 1;
    
    // Other programmable values.
    private double originX = 1393.0 * Math.tan(0.03052);
    private double beamEnergy = 2.2 * ECalUtils.GeV;
    private String clusterCollectionName = "EcalClusters";
    private int pairCoincidence = 2;
    
    // Trigger cut tracking.
    private int allPairs = 0;
    private int energySumCount = 0;
    private int energyDifferenceCount = 0;
    private int energyDistanceCount = 0;
    private int coplanarityCount = 0;
    private int allClusters = 0;
    private int clusterTotalEnergyCount = 0;
    private int clusterSeedEnergyCount = 0;
    private int clusterHitCountCount = 0;
    
    // Internal variables.
    AIDA aida = AIDA.defaultInstance();
    private Queue<List<HPSEcalCluster>> topClusterQueue = null;
    private Queue<List<HPSEcalCluster>> botClusterQueue = null;
    
    // Trigger variation studies.
    private FileWriter writer;
    private static final int CLUSTER_ENERGY_LOW = 0;
    private static final int CLUSTER_ENERGY_HIGH = 1;
    private static final int SEED_ENERGY_LOW = 2;
    private static final int SEED_ENERGY_HIGH = 3;
    private static final int ENERGY_SUM_LOW = 4;
    private static final int ENERGY_SUM_HIGH = 5;
    private static final int ENERGY_DIFFERENCE = 6;
    private static final int ENERGY_SLOPE = 7;
    private static final int COPLANARITY = 8;
    private static final int HIT_COUNT = 9;
    private static final int LOW_VALUE = 0;
    private static final int HIGH_VALUE = 1;
    private static final int STEP_SIZE = 2;
    
    // [Cut Start] [Cut End] [Cut Step Size]
    private double cutInfo[][] = {
    		{ 0.0, 0.0, 0.1},		// Cluster total energy lower bound
    		{ 1.6, 1.6, 0.1 },		// Cluster total energy upper bound
    		{ 0.1, 0.1, 0.05},		// Seed energy lower bound
    		{ 1.2, 1.2, 0.1},		// Seed energy upper bound
    		{ 0.2, 0.5, 0.1},		// Pair energy sum lower bound
    		{ 1.98, 1.98, 0.05},	// Pair energy sum upper bound
    		{ 1.4, 1.4, 0.1},		// Pair energy difference
    		{ 0.4, 1.2, 0.05 },		// Pair energy slope
    		{ 35, 80, 1},			// Pair coplanarity
    		{ 1, 3, 1}				// Cluster hit count
    };
    /**
    private double cutInfo[][] = {
    		{ 0.0, 0.1, 0.1},		// Cluster total energy lower bound
    		{ 1.3, 2.0, 0.1 },		// Cluster total energy upper bound
    		{ 0.10, 0.20, 0.05},	// Seed energy lower bound
    		{ 0.9, 1.5, 0.1},		// Seed energy upper bound
    		{ 0.2, 1.0, 0.1},		// Pair energy sum lower bound
    		{ 1.8, 2.1, 0.05},		// Pair energy sum upper bound
    		{ 1.0, 1.8, 0.1},		// Pair energy difference
    		{ 0.4, 1.2, 0.1 },		// Pair energy slope
    		{ 35, 65, 5},			// Pair coplanarity
    		{ 1, 3, 1}				// Cluster hit count
    };
    **/
    /**
    private double cutInfo[][] = {
    		{ 0.0, 0.0, 0.05},		// Cluster total energy lower bound
    		{ 500, 500, 0.1 },		// Cluster total energy upper bound
    		{ 0.00, 0.00, 0.05},	// Seed energy lower bound
    		{ 500, 500, 0.1},		// Seed energy upper bound
    		{ 0.0, 0.0, 0.1},		// Pair energy sum lower bound
    		{ 500, 500, 0.05},		// Pair energy sum upper bound
    		{ 500, 500, 0.1},		// Pair energy difference
    		{ 0.0, 0.0, 0.1 },		// Pair energy slope
    		{ 500, 500, 5},			// Pair coplanarity
    		{ 0, 0, 1}				// Cluster hit count
    };
    **/
    
    private int spatialCut[][][][][][][][][][];
    
    public void setClusterSeedEnergyHighThreshold(double clusterSeedEnergyHighThreshold) {
    	this.clusterSeedEnergyHighThreshold = clusterSeedEnergyHighThreshold;
    }
    
    public void setEnergySumLowThreshold(double energySumLowThreshold) {
    	this.energySumLowThreshold = energySumLowThreshold;
    }
    
    
    
    
    private enum Flag {
        ENERGY_SUM, ENERGY_DIFFERENCE, ENERGY_SLOPE, COPLANARITY, SEED_ENERGY, CLUSTER_ENERGY, HIT_COUNT;
    }
    
    /**
     * <b>endOfData</b><br/><br/>
     * <code>public void <b>endOfData</b>()</code><br/><br/>
     * Prints trigger information and terminates the driver.
     */
    public void endOfData() {
    	try {
    		// Write the trigger results.
    		writer.write("");
    		
    		// Define the starting cut values.
    		double cutValue[] = new double[10];
	    	for(int i = 0; i < 10; i++) { cutValue[i] = cutInfo[i][LOW_VALUE]; }
    		
    		// Write spatial cuts.
	    	for(int i = 0; i < spatialCut.length; i++) {
	    		for(int j = 0; j < spatialCut[i].length; j++) {
	    			for(int k = 0; k < spatialCut[i][j].length; k++) {
	    				for(int l = 0; l < spatialCut[i][j][k].length; l++) {
		    				for(int m = 0; m < spatialCut[i][j][k][l].length; m++) {
			    				for(int n = 0; n < spatialCut[i][j][k][l][m].length; n++) {
				    				for(int o = 0; o < spatialCut[i][j][k][l][m][n].length; o++) {
					    				for(int p = 0; p < spatialCut[i][j][k][l][m][n][o].length; p++) {
						    				for(int q = 0; q < spatialCut[i][j][k][l][m][n][o][p].length; q++) {
							    				for(int r = 0; r < spatialCut[i][j][k][l][m][n][o][p][q].length; r++) {
							    					// Generate the output line.
							    					String s = String.format("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%d%n", cutValue[0], cutValue[1],
							    							cutValue[2], cutValue[3], cutValue[4], cutValue[5], cutValue[6], cutValue[7], cutValue[8],
							    							cutValue[9], spatialCut[i][j][k][l][m][n][o][p][q][r]);
							    					
							    					// Write the output line.
							    					writer.append(s);
							    					
							    					// Increment the cut.
							    					cutValue[9] += cutInfo[9][STEP_SIZE];
							    				}
							    				// Reset the cut value.
							    				cutValue[9] = cutInfo[9][LOW_VALUE];

						    					// Increment the cut.
						    					cutValue[8] += cutInfo[8][STEP_SIZE];
						    				}
						    				// Reset the cut value.
						    				cutValue[8] = cutInfo[8][LOW_VALUE];

					    					// Increment the cut.
					    					cutValue[7] += cutInfo[7][STEP_SIZE];
					    				}
					    				// Reset the cut value.
					    				cutValue[7] = cutInfo[7][LOW_VALUE];

				    					// Increment the cut.
				    					cutValue[6] += cutInfo[6][STEP_SIZE];
				    				}
				    				// Reset the cut value.
				    				cutValue[6] = cutInfo[6][LOW_VALUE];

			    					// Increment the cut.
			    					cutValue[5] += cutInfo[5][STEP_SIZE];
			    				}
			    				// Reset the cut value.
			    				cutValue[5] = cutInfo[5][LOW_VALUE];

		    					// Increment the cut.
		    					cutValue[4] += cutInfo[4][STEP_SIZE];
		    				}
		    				// Reset the cut value.
		    				cutValue[4] = cutInfo[4][LOW_VALUE];

	    					// Increment the cut.
	    					cutValue[3] += cutInfo[3][STEP_SIZE];
	    				}
	    				// Reset the cut value.
	    				cutValue[3] = cutInfo[3][LOW_VALUE];

    					// Increment the cut.
    					cutValue[2] += cutInfo[2][STEP_SIZE];
	    			}
    				// Reset the cut value.
    				cutValue[2] = cutInfo[2][LOW_VALUE];

					// Increment the cut.
					cutValue[1] += cutInfo[1][STEP_SIZE];
	    		}
				// Reset the cut value.
				cutValue[1] = cutInfo[1][LOW_VALUE];

				// Increment the cut.
				cutValue[0] += cutInfo[0][STEP_SIZE];
	    	}
    		
    		// Close the writer.
    		writer.close();
        	
    		// Output file name.
    		int extension = outputFileName.lastIndexOf('.');
    		String filename = outputFileName;
    		if(extension != -1) { filename = outputFileName.substring(0, extension); }
    		System.out.println("Output File: " + filename + "-triggers.txt");
    	}
    	catch(IOException e) { System.exit(1); }
    	
        if (outputStream != null) { printCounts(outputStream); }
        printCounts(new PrintWriter(System.out));
        super.endOfData();
    }
    
    /**
     * <b>process</b><br/><br/>
     * <code>public void <b>process</b>(EventHeader event)</code><br/><br/>
     * Processes the event clusters for the trigger to analyze.
     */
    public void process(EventHeader event) {
    	// If there is a particle list, store it.
    	if(event.hasCollection(MCParticle.class, "MCParticle")) {
    		// Set the particle list for subsequent clusters.
    		particleList = event.get(MCParticle.class, "MCParticle");
    		
    		for(MCParticle particle : event.get(MCParticle.class, "MCParticle")) {
    			// Make sure that this is both a positron and at t = 0.
    			if(particle.getProductionTime() == 0 && particle.getPDGID() == -11) {
		    		// Get momentum values for the momentum plots.
		    		Hep3Vector p = particle.getMomentum();
		    		double totalMomentum = Math.sqrt(p.x() * p.x() + p.y() * p.y() + p.z() * p.z());
		    		double xyMomentum = Math.sqrt(p.x() * p.x() + p.y() * p.y());
		            particleMomentumPlot.fill(totalMomentum);
		            particleMomentumDistribution.fill(xyMomentum, p.z());
    			}
    		}
    	}
    	
    	// If there are clusters to process, do so. Note that this
    	// should run regardless of whether or not the trigger is live.
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
        	// Get the clusters from the current event.
        	List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
        	
        	/**
        	// Store clusters that pass all cluster cuts.
        	ArrayList<HPSEcalCluster> goodClusters = new ArrayList<HPSEcalCluster>();
        	**/
        	
        	// Filter out clusters
        	for(HPSEcalCluster cluster : clusters) {
        		// Check whether the cluster passes the cuts.
        		boolean seedEnergyCut = clusterSeedEnergyCut(cluster, clusterSeedEnergyLowThreshold, clusterSeedEnergyHighThreshold);
        		boolean totalEnergyCut = clusterTotalEnergyCut(cluster, clusterEnergyLowThreshold, clusterEnergyHighThreshold);
        		boolean hitCountCut = clusterHitCountCut(cluster, hitCountLowThreshold);
        		
        		// Determine how many clusters have passed each successive cut.
        		allClusters++;
        		if(seedEnergyCut) {
        			clusterSeedEnergyCount++;
        			if(totalEnergyCut) {
        				clusterTotalEnergyCount++;
        				if(hitCountCut) {
        					clusterHitCountCount++;
        				}
        			}
        		}
    	        
    			// Get the seed x and y indices.
    			int ix = cluster.getSeedHit().getIdentifierFieldValue("ix");
    			int iy = cluster.getSeedHit().getIdentifierFieldValue("iy");
    			double clusterEnergy = cluster.getEnergy();
    			double seedEnergy = cluster.getSeedHit().getRawEnergy();
        		
        		// Populate the all-clusters plots.
    	        clusterHitCountPlot.fill(cluster.getCalorimeterHits().size());
    	        clusterTotalEnergyPlot.fill(clusterEnergy);
    	        clusterSeedEnergyPlot.fill(seedEnergy);
    	        
    			// Fill the edge seed hit plots.
    			if(isEdgeCrystal(ix, iy)) {
	    	        edgeClusterTotalEnergyPlot.fill(clusterEnergy);
	    	        edgeClusterSeedEnergyPlot.fill(cluster.getSeedHit().getRawEnergy());
    			}
    			else {
	    	        nedgeClusterTotalEnergyPlot.fill(clusterEnergy);
	    	        nedgeClusterSeedEnergyPlot.fill(seedEnergy);
    			}
    	        edgeSeedDistribution.fill(ix - 0.5 * Math.signum(ix), iy, 1);
    	        singleHitClusterDistribution.fill(seedEnergy, cluster.getCalorimeterHits().size());
    	        
    	        // Fill single-hit cluster plots.
    	        if(cluster.getCalorimeterHits().size() == 1 && cluster.getSeedHit().getRawEnergy() >= 0.1) {
    	        	// Fill the seed plots.
    	        	singleHitSeedEnergyPlot.fill(seedEnergy);
	    	        singleHitClusterDistribution.fill(ix, iy, 1);
	    	        
	    	        // Fill the particle energy plots.
	    	        for(MCParticle p : particleList) {
	    	        	if(p.getProductionTime() == 0) {
	    	        		particleEnergyPlot.fill(p.getEnergy());
	    	        	}
	    	        }
    	        }
    	        
        		// Populate the passed-cuts cluster plots.
        		if(seedEnergyCut && totalEnergyCut) {
        	        tClusterHitCountPlot.fill(cluster.getCalorimeterHits().size());
        		}
        		if(seedEnergyCut && hitCountCut) {
        	        tClusterTotalEnergyPlot.fill(clusterEnergy);
        		}
        		if(hitCountCut && totalEnergyCut) {
        	        tClusterSeedEnergyPlot.fill(seedEnergy);
        		}
        		
        		// If the cluster passes all of the cuts, add it to
        		// the filtered list.
        		if(seedEnergyCut && totalEnergyCut && hitCountCut) {
        	        edgeTSeedDistribution.fill(ix - 0.5 * Math.signum(ix), iy, 1);
        			if(isEdgeCrystal(ix, iy)) {
        				edgeTClusterTotalEnergyPlot.fill(clusterEnergy);
        				edgeTClusterSeedEnergyPlot.fill(seedEnergy);
        			}
        			else {
    	    	        nedgeTClusterTotalEnergyPlot.fill(clusterEnergy);
        				nedgeTClusterSeedEnergyPlot.fill(seedEnergy);
        			}
        		}
        	}
        	
        	// Place the filtered clusters into the appropriate buffer.
            //updateClusterQueues(event.get(HPSEcalCluster.class, clusterCollectionName));
        	updateClusterQueues(clusters);
        }
        
        // Let the TriggerDriver class perform its processing.
        super.process(event);
    }
    
    /**
     * <b>setBeamEnergy</b><br/><br/>
     * <code>public void <b>setBeamEnergy</b>(double beamEnergy)</code><br/><br/>
     * Sets the beam energy to the indicated value and calculates the
     * energy slope values.
     * @param beamEnergy - The beam energy in GeV.
     */
    public void setBeamEnergy(double beamEnergy) {
        this.beamEnergy = beamEnergy * ECalUtils.GeV;
    }
    
    /**
     * <b>setClusterCollectionName</b><br/><br/>
     * <code>public void <b>setClusterCollectionName</b>(String clusterCollectionName)</code><br/><br/>
     * Sets the LCIO cluster collection name.
     * @param clusterCollectionName - The collection to use.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * <b>setClusterEnergyHighThreshold</b><br/><br/>
     * <code>public void <b>setClusterEnergyHighThreshold</b>(double clusterEnergyHighThreshold)</code><br/><br/>
     * Sets the maximum allowed energy for single clusters.
     * @param clusterEnergyHighThreshold - The threshold value.
     */
    public void setClusterEnergyHighThreshold(double clusterEnergyHighThreshold) {
    	this.clusterEnergyHighThreshold = clusterEnergyHighThreshold;
    }
    
    /**
     * <b>setClusterEnergyLowThreshold</b><br/><br/>
     * <code>public void <b>setClusterEnergyLowThreshold</b>(double clusterEnergyLowThreshold)</code><br/><br/>
     * Sets the minimum allowed energy for single clusters.
     * @param clusterEnergyLowThreshold - The threshold value.
     */
    public void setClusterEnergyLowThreshold(double clusterEnergyLowThreshold) {
    	this.clusterEnergyLowThreshold = clusterEnergyLowThreshold;
    }
    
    /**
     * <b>setClusterSeedEnergyThreshold</b><br/><br/>
     * <code>public void <b>setClusterSeedEnergyThreshold</b>(double clusterSeedEnergyThreshold)</code><br/><br/>
     * Sets the minimum allowed energy for cluster seed hits.
     * @param clusterSeedEnergyThreshold - The threshold value.
     */
    public void setClusterSeedEnergyLowThreshold(double clusterSeedEnergyLowThreshold) {
    	this.clusterSeedEnergyLowThreshold = clusterSeedEnergyLowThreshold;
    }
    
    /**
     * <b>setCoplanarityHighCut</b><br/><br/>
     * <code>public void <b>setCoplanarityHighCut</b>(double coplanarityHighCut)</code><br/><br/>
     * Sets the maximum allowed coplanarity angle between each cluster
     * in a pair.
     * @param coplanarityHighCut - The threshold value.
     */
    public void setCoplanarityHighCut(double coplanarityHighCut) {
    	this.coplanarityHighCut = coplanarityHighCut;
    }
    
    /**
     * <b>setEnergyDifferenceHighThreshold</b><br/><br/>
     * <code>public void <b>setEnergyDifferenceHighThreshold</b>(double energyDifferenceHighThreshold)</code><br/><br/>
     * Sets the maximum allowed difference between the energies of each
     * cluster in the pair.
     * @param energyDifferenceHighThreshold - The threshold value.
     */
    public void setEnergyDifferenceHighThreshold(double energyDifferenceHighThreshold) {
    	this.energyDifferenceHighThreshold = energyDifferenceHighThreshold;
    }
    
    /**
     * <b>setEnergySlopeParamF</b><br/><br/>
     * <code>public void <b>setEnergySlopeParamF</b>(double energySlopeParamF)</code><br/><br/>
     * Sets the factor by which the radial distance from the origin
     * for the cluster center in the energy slope cut is multiplied.
     * @param energySlopeParamF - The threshold value.
     */
    public void setEnergySlopeParamF(double energySlopeParamF) {
    	this.energySlopeParamF = energySlopeParamF;
    }
    
    /**
     * <b>setEnergySlopeThreshold</b><br/><br/>
     * <code>public void <b>setEnergySlopeThreshold</b>(double energySlopeThreshold)</code><br/><br/>
     * Sets the maximum allowed value for the energy slope of the lower
     * energy cluster in the pair. Energy slope is defined as E_cluster +
     * R_seed * F, where F is a programmable parameter.
     * @param energySlopeThreshold - The threshold value.
     */
    public void setEnergySlopeHighThreshold(double energySlopeHighThreshold) {
    	this.energySlopeHighThreshold = energySlopeHighThreshold;
    }
    
    /**
     * <b>setEnergySumHighThreshold</b><br/><br/>
     * <code>public void <b>setEnergySumHighThreshold</b>(double energySumHighThreshold)</code><br/><br/>
     * Sets the maximum allowed value for the sum of the energies of
     * each cluster in a pair.
     * @param energySumHighThreshold - The threshold value.
     */
    public void setEnergySumHighThreshold(double energySumHighThreshold) {
    	this.energySumHighThreshold = energySumHighThreshold;
    }
    
    /**
     * <b>setHitCountLowThreshold</b><br/><br/>
     * <code>public void <b>setHitCountLowThreshold</b>(int hitCountLowThreshold)</code><br/><br/>
     * Sets the minimum number of hits required for a cluster.
     * @param hitCountLowThreshold - The threshold value.
     */
    public void setHitCountLowThreshold(int hitCountLowThreshold) {
    	this.hitCountLowThreshold = hitCountLowThreshold;
    }
    
    /**
     * <b>setOriginX</b><br/><br/>
     * <code>public void <b>setOriginX</b>(double originX)</code><br/><br/>
     * Sets the x-coordinate used as the origin for the cluster
     * coplanarity and energy slope cut calculations. This defaults
     * to the calorimeter mid-plane.
     * @param originX - The x-coordinate to use.
     */
    public void setOriginX(double originX) {
        this.originX = originX;
    }
    
    /**
     * <b>setPairCoincidence</b><br/><br/>
     * <code>public void <b>setPairCoincidence</b>(int pairCoincidence)</code><br/><br/>
     * Sets the size of the time buffer for the trigger. This will
     * create a buffer of size (2 * pairCoincidence) + 1 and should
     * match the time buffer used by the GTP clusterer.
     * @param pairCoincidence - The number of time buffers before and
     * after the "current" time that should be used.
     */
    public void setPairCoincidence(int pairCoincidence) {
        this.pairCoincidence = pairCoincidence;
    }
    
    /**
     * <b>startOfData</b><br/><br/>
     * <code>public void <b>startOfData</b>()</code><br/><br/>
     * Initializes the cluster queues, passed trigger cut trackers, and
     * the trigger cut histograms.
     */
    public void startOfData() {
    	// Create a file writer.
    	try {
    		int extension = outputFileName.lastIndexOf('.');
    		String filename = outputFileName;
    		if(extension != -1) { filename = outputFileName.substring(0, extension); }
    		writer = new FileWriter(filename + "-triggers.txt");
    	}
    	catch(IOException e) { System.exit(1); }
    	
    	// Calculate the number of boxes for the absurdly large trigger array.
    	int boxes[] = new int[10];
    	boxes[CLUSTER_ENERGY_LOW] = (int) Math.ceil((cutInfo[CLUSTER_ENERGY_LOW][HIGH_VALUE] - cutInfo[CLUSTER_ENERGY_LOW][LOW_VALUE]) / cutInfo[CLUSTER_ENERGY_LOW][STEP_SIZE]) + 1;
    	boxes[CLUSTER_ENERGY_HIGH] = (int) Math.ceil((cutInfo[CLUSTER_ENERGY_HIGH][HIGH_VALUE] - cutInfo[CLUSTER_ENERGY_HIGH][LOW_VALUE]) / cutInfo[CLUSTER_ENERGY_HIGH][STEP_SIZE]) + 1;
    	boxes[SEED_ENERGY_LOW] = (int) Math.ceil((cutInfo[SEED_ENERGY_LOW][HIGH_VALUE] - cutInfo[SEED_ENERGY_LOW][LOW_VALUE]) / cutInfo[SEED_ENERGY_LOW][STEP_SIZE]) + 1;
    	boxes[SEED_ENERGY_HIGH] = (int) Math.ceil((cutInfo[SEED_ENERGY_HIGH][HIGH_VALUE] - cutInfo[SEED_ENERGY_HIGH][LOW_VALUE]) / cutInfo[SEED_ENERGY_HIGH][STEP_SIZE]) + 1;
    	boxes[ENERGY_SUM_LOW] = (int) Math.ceil((cutInfo[ENERGY_SUM_LOW][HIGH_VALUE] - cutInfo[ENERGY_SUM_LOW][LOW_VALUE]) / cutInfo[ENERGY_SUM_LOW][STEP_SIZE]) + 1;
    	boxes[ENERGY_SUM_HIGH] = (int) Math.ceil((cutInfo[ENERGY_SUM_HIGH][HIGH_VALUE] - cutInfo[ENERGY_SUM_HIGH][LOW_VALUE]) / cutInfo[ENERGY_SUM_HIGH][STEP_SIZE]) + 1;
    	boxes[ENERGY_DIFFERENCE] = (int) Math.ceil((cutInfo[ENERGY_DIFFERENCE][HIGH_VALUE] - cutInfo[ENERGY_DIFFERENCE][LOW_VALUE]) / cutInfo[ENERGY_DIFFERENCE][STEP_SIZE]) + 1;
    	boxes[ENERGY_SLOPE] = (int) Math.ceil((cutInfo[ENERGY_SLOPE][HIGH_VALUE] - cutInfo[ENERGY_SLOPE][LOW_VALUE]) / cutInfo[ENERGY_SLOPE][STEP_SIZE]) + 1;
    	boxes[COPLANARITY] = (int) Math.ceil((cutInfo[COPLANARITY][HIGH_VALUE] - cutInfo[COPLANARITY][LOW_VALUE]) / cutInfo[COPLANARITY][STEP_SIZE]) + 1;
    	boxes[HIT_COUNT] = (int) Math.ceil((cutInfo[HIT_COUNT][HIGH_VALUE] - cutInfo[HIT_COUNT][LOW_VALUE]) / cutInfo[HIT_COUNT][STEP_SIZE]) + 1;
        
    	// Initialize the absurd trigger array.
        spatialCut = new int[boxes[CLUSTER_ENERGY_LOW]][boxes[CLUSTER_ENERGY_HIGH]][boxes[SEED_ENERGY_LOW]][boxes[SEED_ENERGY_HIGH]][boxes[ENERGY_SUM_LOW]][boxes[ENERGY_SUM_HIGH]][boxes[ENERGY_DIFFERENCE]][boxes[ENERGY_SLOPE]][boxes[COPLANARITY]][boxes[HIT_COUNT]];
    	
        // Initialize the cluster pair queues.
    	topClusterQueue = new LinkedList<List<HPSEcalCluster>>();
        botClusterQueue = new LinkedList<List<HPSEcalCluster>>();
        
        // Populate the cluster pair queues with empty lists for each
        // each time buffer. The time buffer size is determined by
        // (2 * coincidenceWindow) + 1.
        for (int i = 0; i < 2 * pairCoincidence + 1; i++) {
            topClusterQueue.add(new ArrayList<HPSEcalCluster>());
        }
        for (int i = 0; i < pairCoincidence + 1; i++) {
            botClusterQueue.add(new ArrayList<HPSEcalCluster>());
        }
        
        // Initialize the superclass TriggerDriver.
        super.startOfData();
        
        // Make sure that there is a name for the cluster collection
        // from which to trigger.
        if (clusterCollectionName == null) {
            throw new RuntimeException("The parameter clusterCollectionName was not set!");
        }
        
        // Set the histogram sizes based on the beam energy.
        int oneEnergy = (int) Math.ceil(beamEnergy);
        int oneEnergyBoxes = 64 * oneEnergy;
        int twoEnergy = (int) Math.ceil(2 * beamEnergy);
        int twoEnergyBoxes = 32 * twoEnergy;
        
        // Initialize the trigger cut histograms.
        clusterSeedEnergyPlot =  aida.histogram1D("Trigger Cut :: Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        clusterTotalEnergyPlot = aida.histogram1D("Trigger Cut :: Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);
        clusterHitCountPlot = aida.histogram1D("Trigger Cut :: Cluster Hit Count", 9, 1, 10);
        pairEnergySumPlot = aida.histogram1D("Trigger Cut :: Cluster Pair Energy Sum", twoEnergyBoxes, 0.0, twoEnergy);
        pairEnergyDifferencePlot = aida.histogram1D("Trigger Cut :: Pair Energy Difference", oneEnergyBoxes, 0.0, oneEnergy);
        pairCoplanarityPlot = aida.histogram1D("Trigger Cut :: Pair Coplanarity", 180, 0.0, 180.0);
        pairEnergySlopePlot = aida.histogram1D("Trigger Cut :: Pair Energy Slope", 200, 0.0, 10);

        edgeClusterSeedEnergyPlot =  aida.histogram1D("Trigger Cut :: Edge Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        edgeClusterTotalEnergyPlot = aida.histogram1D("Trigger Cut :: Edge Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);        
        edgeTClusterSeedEnergyPlot =  aida.histogram1D("Passed Trigger Cut :: Edge Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        edgeTClusterTotalEnergyPlot = aida.histogram1D("Passed Trigger Cut :: Edge Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);
        nedgeClusterSeedEnergyPlot =  aida.histogram1D("Trigger Cut :: Non-Edge Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        nedgeClusterTotalEnergyPlot = aida.histogram1D("Trigger Cut :: Non-Edge Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);        
        nedgeTClusterSeedEnergyPlot =  aida.histogram1D("Passed Trigger Cut :: Non-Edge Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        nedgeTClusterTotalEnergyPlot = aida.histogram1D("Passed Trigger Cut :: Non-Edge Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);
        edgeSeedDistribution = aida.histogram2D("Trigger Cut:: Seed Hit Distribution", 52, -26, 26, 11, -5.5, 5.5);
        edgeTSeedDistribution = aida.histogram2D("Passed Trigger Cut:: Seed Hit Distribution", 52, -26, 26, 11, -5.5, 5.5);
        
        singleHitSeedEnergyPlot = aida.histogram1D("Cluster Seed Energy", oneEnergyBoxes, 0.0, oneEnergy);
        particleEnergyPlot = aida.histogram1D("Initial Particle Energy", oneEnergyBoxes, 0.0, oneEnergy);
        singleHitClusterDistribution = aida.histogram2D("One-Hit Cluster Seed Hit Energy", 52, -26, 26, 11, -5.5, 5.5);
        particleMomentumPlot = aida.histogram1D("Positron Total Momentum", 4 * 22, 0.0, 2.2);
        particleMomentumDistribution = aida.histogram2D("Positron Momentum Distribution", 100, 0, 0.15, 100, 0, 2.3);
        singleHitClusterDistribution = aida.histogram2D("Cluster Hits by Seed Energy", 22, 0, 2.2, 9, 1, 10);
        
        tClusterSeedEnergyPlot =  aida.histogram1D("Passed Trigger Cut :: Seed Hit Energy", oneEnergyBoxes, 0.0, oneEnergy);
        tClusterTotalEnergyPlot = aida.histogram1D("Passed Trigger Cut :: Cluster Total Energy", twoEnergyBoxes, 0.0, twoEnergy);
        tClusterHitCountPlot = aida.histogram1D("Passed Trigger Cut :: Cluster Hit Count", 9, 1, 10);
        tPairEnergySumPlot = aida.histogram1D("Passed Trigger Cut :: Cluster Pair Energy Sum", twoEnergyBoxes, 0.0, twoEnergy);
        tPairEnergyDifferencePlot = aida.histogram1D("Passed Trigger Cut :: Pair Energy Difference", oneEnergyBoxes, 0.0, oneEnergy);
        tPairCoplanarityPlot = aida.histogram1D("Passed Trigger Cut :: Pair Coplanarity", 180, 0.0, 180.0);
        tPairEnergySlopePlot = aida.histogram1D("Passed Trigger Cut :: Pair Energy Slope", 200, 0.0, 10);
    }
    
    /**
     * <b>testTrigger</b>()<br/><br/>
     * <code>public boolean <b>testTrigger</b></code><br/><br/>
     * Performs trigger cuts on all cluster pairs and indicates if a
     * trigger has occurred. Note that individual cluster cuts are
     * handled in the <code>process()</code> method.
     * @return Returns <code>true</code> if a trigger occurred and
     * </code>false</code> otherwise.
     */
    public boolean testTrigger() {
    	// Track whether or not a trigger occurred.
        boolean trigger = false;
        
        // Get the cluster pairs.
        List<HPSEcalCluster[]> clusterPairs = getClusterPairsTopBot();
        
        // Iterate through all cluster pairs present in the event.  If at least
        // one of the cluster pairs satisfies all of the trigger conditions,
        // a trigger signal is sent to all other detectors.
        for (HPSEcalCluster[] clusterPair : clusterPairs) {
        	
        	// Track which cuts the cluster pair passed.
            EnumSet<Flag> bits = EnumSet.noneOf(Flag.class);
            
            // Note that an additional pair occurred.
            allPairs++;
            
            // Require both clusters to have a seed energy in a range.
            if(clusterSeedEnergyCut(clusterPair[0], clusterSeedEnergyLowThreshold, clusterSeedEnergyHighThreshold) &&
            		clusterSeedEnergyCut(clusterPair[1], clusterSeedEnergyLowThreshold, clusterSeedEnergyHighThreshold)) {
            	bits.add(Flag.SEED_ENERGY);
            }
            
            // Require both clusters to have a total energy in a range.
            if(clusterTotalEnergyCut(clusterPair[0], clusterEnergyLowThreshold, clusterEnergyHighThreshold) &&
            		clusterTotalEnergyCut(clusterPair[1], clusterEnergyLowThreshold, clusterEnergyHighThreshold)) {
            	bits.add(Flag.CLUSTER_ENERGY);
            }
            
            // Require both clusters to have a minimum number of hits.
            if(clusterHitCountCut(clusterPair[0], hitCountLowThreshold) && clusterHitCountCut(clusterPair[1], hitCountLowThreshold)) {
            	bits.add(Flag.HIT_COUNT);
            }
            
            // Require the sum of the energies of the components of the
            // cluster pair to be less than the
            // (Beam Energy)*(Sampling Fraction) ( 2 GeV for the Test Run )
            if (pairEnergySumCut(clusterPair)) {
                bits.add(Flag.ENERGY_SUM);
            }
            
            // Require the difference in energy of the components of the
            // cluster pair to be less than 1.5 GeV
            if (pairEnergyDifferenceCut(clusterPair)) {
                bits.add(Flag.ENERGY_DIFFERENCE);
            }
            
            // Apply a low energy cluster vs. distance cut of the form
            // E_low + .0032 GeV/mm < .8 GeV
            if (pairEnergySlopeCut(clusterPair)) {
                bits.add(Flag.ENERGY_SLOPE);
            }
            
            // Require that the two clusters are coplanar with the beam within
            // 35 degrees
            if (pairCoplanarityCut(clusterPair)) {
                bits.add(Flag.COPLANARITY);
            }
            
            // Note whether a given cluster pair cut was passed.
            if (bits.contains(Flag.ENERGY_SUM)) {
                energySumCount++;
                if (bits.contains(Flag.ENERGY_DIFFERENCE)) {
                    energyDifferenceCount++;
                    if (bits.contains(Flag.ENERGY_SLOPE)) {
                        energyDistanceCount++;
                        if (bits.contains(Flag.COPLANARITY)) {
                            coplanarityCount++;
                        }
                    }
                }
            }
            
            // Check that the pair passed all cluster cuts.
            boolean passedSingleCuts = bits.contains(Flag.SEED_ENERGY) && bits.contains(Flag.CLUSTER_ENERGY) && bits.contains(Flag.HIT_COUNT);
            
            // Plot the values for pairs which pass the other pair cuts.
            if(passedSingleCuts) {
            	// Populate the no-pair-cuts histograms.
                pairEnergySumPlot.fill(pairEnergySumValue(clusterPair));
                pairEnergySlopePlot.fill(pairEnergySlopeValue(clusterPair));
                pairEnergyDifferencePlot.fill(pairEnergyDifferenceValue(clusterPair));
            	pairCoplanarityPlot.fill(pairCoplanarityValue(clusterPair));
            	
            	// Populate the all other pair cuts histograms.
	            if(bits.contains(Flag.ENERGY_DIFFERENCE) && bits.contains(Flag.ENERGY_SLOPE) && bits.contains(Flag.COPLANARITY)) {
	                tPairEnergySumPlot.fill(pairEnergySumValue(clusterPair));
	            }
	            if(bits.contains(Flag.ENERGY_SUM) && bits.contains(Flag.ENERGY_SLOPE) && bits.contains(Flag.COPLANARITY)) {
	            	tPairEnergyDifferencePlot.fill(pairEnergyDifferenceValue(clusterPair));
	            }
	            if(bits.contains(Flag.ENERGY_DIFFERENCE) && bits.contains(Flag.ENERGY_SUM) && bits.contains(Flag.COPLANARITY)) {
	            	tPairEnergySlopePlot.fill(pairEnergySlopeValue(clusterPair));
	            }
	            if(bits.contains(Flag.ENERGY_DIFFERENCE) && bits.contains(Flag.ENERGY_SLOPE) && bits.contains(Flag.ENERGY_SUM)) {
	            	tPairCoplanarityPlot.fill(pairCoplanarityValue(clusterPair));
	            }
            }
            
            // If the cluster pair passes all of the cuts, it is a trigger.
            if (bits.containsAll(EnumSet.allOf(Flag.class))) {
                // If all cuts are passed, we have a trigger
                if (outputStream != null) { outputStream.println("Passed all cuts"); }
                
                trigger = true;
            }
        	
        	
            
        	/** Start :: Multiple Trigger Condition Test **/
	    	// Get the cut values.
	    	double coplanarityValue = pairCoplanarityValue(clusterPair);
	        double energyDifferenceValue = pairEnergyDifferenceValue(clusterPair);
	        double energySlopeValue = pairEnergySlopeValue(clusterPair);
	        double energySumValue = pairEnergySumValue(clusterPair);
	        
	        boolean printData = false;
	        if(printData) {
		        System.out.printf("Single Cluster Values%n");
		        System.out.printf("Cluster Energy: %f\t%f%n", clusterPair[0].getEnergy(), clusterPair[1].getEnergy());
		        System.out.printf("Seed Energy   : %f\t%f%n", clusterPair[0].getSeedHit().getRawEnergy(), clusterPair[1].getSeedHit().getRawEnergy());
		        System.out.printf("Hit Count     : %d\t%d%n", clusterPair[0].getCalorimeterHits().size(), clusterPair[1].getCalorimeterHits().size());
		        
		        System.out.printf("%nCluster Pair Values%n");
		        System.out.printf("Energy Sum       : %f%n", energySumValue);
		        System.out.printf("Energy Difference: %f%n", energyDifferenceValue);
		        System.out.printf("Energy Slope     : %f%n", energySlopeValue);
		        System.out.printf("Coplanarity      : %f%n", coplanarityValue);
		        System.out.printf("%n");
		        System.out.printf("Cl Low\tCl High\tSeed Low\tSeed High\tSum Low\tSum High\tE Diff\tE Slope\tCoplane\t# Hits\tPasses%n");
	        }
	        
	        // Set the cut starting values.
	    	boolean cut[] = new boolean[10];
	    	double cutValue[] = new double[10];
	    	for(int i = 0; i < 10; i++) { cutValue[i] = cutInfo[i][LOW_VALUE]; }
	    	
        	// [i] Cluster total energy lower bound loop.
	    	for(int i = 0; i < spatialCut.length; i++) {
				cut[CLUSTER_ENERGY_LOW] =  clusterPair[0].getEnergy() > cutValue[CLUSTER_ENERGY_LOW] &&
						clusterPair[1].getEnergy() > cutValue[CLUSTER_ENERGY_LOW];
						
	        	// [j] Cluster total energy upper bound loop.
	    		for(int j = 0; j < spatialCut[i].length; j++) {
					cut[CLUSTER_ENERGY_HIGH] = clusterPair[0].getEnergy() < cutValue[CLUSTER_ENERGY_HIGH] &&
							clusterPair[1].getEnergy() < cutValue[CLUSTER_ENERGY_HIGH];
					
		        	// [k] Seed energy lower bound loop.
	    			for(int k = 0; k < spatialCut[i][j].length; k++) {
    					cut[SEED_ENERGY_LOW] = clusterPair[0].getSeedHit().getRawEnergy() >= cutValue[SEED_ENERGY_LOW] &&
    							clusterPair[1].getSeedHit().getRawEnergy() >= cutValue[SEED_ENERGY_LOW];
    							
	    	        	// [l] Seed energy upper bound loop.
	    				for(int l = 0; l < spatialCut[i][j][k].length; l++) {
	    					cut[SEED_ENERGY_HIGH] = clusterPair[0].getSeedHit().getRawEnergy() <= cutValue[SEED_ENERGY_HIGH] &&
	    							clusterPair[1].getSeedHit().getRawEnergy() <= cutValue[SEED_ENERGY_HIGH];
	    					
	    		        	// [m] Energy sum lower bound loop.
		    				for(int m = 0; m < spatialCut[i][j][k][l].length; m++) {
		    					cut[ENERGY_SUM_LOW] = energySumValue > cutValue[ENERGY_SUM_LOW];
		    					
		    		        	// [n] Energy sum upper bound loop.
			    				for(int n = 0; n < spatialCut[i][j][k][l][m].length; n++) {
			    					cut[ENERGY_SUM_HIGH] = energySumValue < cutValue[ENERGY_SUM_HIGH];
			    					
			    		        	// [o] Energy difference loop.
				    				for(int o = 0; o < spatialCut[i][j][k][l][m][n].length; o++) {
				    					cut[ENERGY_DIFFERENCE] = energyDifferenceValue < cutValue[ENERGY_DIFFERENCE];
				    					
				    		        	// [p] Energy slope loop.
					    				for(int p = 0; p < spatialCut[i][j][k][l][m][n][o].length; p++) {
					    					cut[ENERGY_SLOPE] = energySlopeValue > cutValue[ENERGY_SLOPE];
					    					
					    		        	// [q] Coplanarity loop.
						    				for(int q = 0; q < spatialCut[i][j][k][l][m][n][o][p].length; q++) {
						    					cut[COPLANARITY] = coplanarityValue < cutValue[COPLANARITY];
						    					
						    		        	// [r] Cluster hit count loop.
							    				for(int r = 0; r < spatialCut[i][j][k][l][m][n][o][p][q].length; r++) {
							    					cut[HIT_COUNT] = clusterPair[0].getCalorimeterHits().size() >= cutValue[HIT_COUNT] &&
							    							clusterPair[1].getCalorimeterHits().size() >= cutValue[HIT_COUNT];
							    					
							    					// Check if all the cuts are passed.
							    					boolean passedCuts = true;
							    					for(int s = 0; s < 10; s++) {
							    						if(!cut[s]) {
							    							passedCuts = false;
							    							break;
							    						}
							    					}
							    					
							    					if(printData) {
								    					System.out.printf("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%b%n", cutValue[0], cutValue[1],
								    							cutValue[2], cutValue[3], cutValue[4], cutValue[5], cutValue[6], cutValue[7],
								    							cutValue[8], cutValue[9], passedCuts);
								    					System.out.printf("%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b\t\t%b%n", cut[0], cut[1], cut[2],
								    							cut[3], cut[4], cut[5], cut[6], cut[7], cut[8], cut[9], passedCuts);
							    					}
							    					
							    					// Increment the triggers for this set of cuts.
							    					if(passedCuts) { spatialCut[i][j][k][l][m][n][o][p][q][r]++; }
							    					
							    					// Increment the cut.
							    					cutValue[9] += cutInfo[9][STEP_SIZE];
							    				}
							    				// Reset the cut value.
							    				cutValue[9] = cutInfo[9][LOW_VALUE];
							    				
						    					// Increment the cut.
						    					cutValue[8] += cutInfo[8][STEP_SIZE];
						    				}
						    				// Reset the cut value.
						    				cutValue[8] = cutInfo[8][LOW_VALUE];
						    				
					    					// Increment the cut.
					    					cutValue[7] += cutInfo[7][STEP_SIZE];
					    				}
					    				// Reset the cut value.
					    				cutValue[7] = cutInfo[7][LOW_VALUE];
					    				
				    					// Increment the cut.
				    					cutValue[6] += cutInfo[6][STEP_SIZE];
				    				}
				    				// Reset the cut value.
				    				cutValue[6] = cutInfo[6][LOW_VALUE];
				    				
			    					// Increment the cut.
			    					cutValue[5] += cutInfo[5][STEP_SIZE];
			    				}
			    				// Reset the cut value.
			    				cutValue[5] = cutInfo[5][LOW_VALUE];
			    				
		    					// Increment the cut.
		    					cutValue[4] += cutInfo[4][STEP_SIZE];
		    				}
		    				// Reset the cut value.
		    				cutValue[4] = cutInfo[4][LOW_VALUE];
		    				
	    					// Increment the cut.
	    					cutValue[3] += cutInfo[3][STEP_SIZE];
	    				}
	    				// Reset the cut value.
	    				cutValue[3] = cutInfo[3][LOW_VALUE];
	    				
    					// Increment the cut.
    					cutValue[2] += cutInfo[2][STEP_SIZE];
	    			}
    				// Reset the cut value.
    				cutValue[2] = cutInfo[2][LOW_VALUE];
    				
					// Increment the cut.
					cutValue[1] += cutInfo[1][STEP_SIZE];
	    		}
				// Reset the cut value.
				cutValue[1] = cutInfo[1][LOW_VALUE];
				
				// Increment the cut.
				cutValue[0] += cutInfo[0][STEP_SIZE];
	    	}
	    	
	    	if(printData) {
	    		System.out.printf("%n%n");
	    	}
        	/**  End  :: Multiple Trigger Condition Test **/
	    	
	    	
        }
        
        // Return whether a trigger occurred.
        return trigger;
    }
    
    /**
     * <b>triggerDecision</b><br/><br/>
     * <code>protected boolean <b>triggerDecision</b>(EventHeader event)</code><br/><br/>
     * Determines whether the clusters attached the argument event will
     * generate a trigger.
     */
    protected boolean triggerDecision(EventHeader event) {
        // If the event has a collection of clusters, process it.
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
            return testTrigger();
        }
        
        // Otherwise, it does not trigger.
        else { return false; }
    }
    
    private boolean isEdgeCrystal(int ix, int iy) {
    	boolean edge = false;
    	
    	// Get the absolute values of the coordinates.
    	int aix = Math.abs(ix);
    	int aiy = Math.abs(iy);
    	
    	// Check if this an outer edge crystal.
    	if(aix == 23 || aiy == 5) { edge = true; }
    	
    	// Check if this along the central beam gap.
    	if(aiy == 1) { edge = true; }
    	
    	// Check if this is around the beam gap.
    	if(aiy == 2 && (ix >= -11 && ix <= -1)) { edge = true; }
    	
    	// Otherwise, this is not an edge crystal.
    	//System.out.printf("ix :: % 3d   iy :: % 3d  %5b%n", ix, iy, edge);
    	return edge;
    }
    
    /**
     * <b>clusterHitCountCut</b><br/><br/>
     * <code>private boolean <b>clusterHitCountCut</b>(HPSEcalCluster cluster)</code><br/><br/>
     * Cuts clusters where the cluster has more than a certain number
     * of component hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut,
     * and <code>false</code> otherwise.
     */
    private boolean clusterHitCountCut(HPSEcalCluster cluster, double hitCountLowThreshold) {
    	// Get the number of hits in the cluster.
    	int hits = cluster.getCalorimeterHits().size();
    	
    	// Return whether the cluster passed the cut.
        return hits >= hitCountLowThreshold;
    }
    
    /**
     * <b>clusterSeedEnergyCut</b><br/><br/>
     * <code>private boolean <b>clusterSeedEnergyCut</b>(HPSEcalCluster cluster)</code><br/><br/>
     * Cuts clusters where the cluster seed hit energy falls outside of
     * exceeds a given value.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut,
     * and <code>false</code> otherwise.
     */
    private boolean clusterSeedEnergyCut(HPSEcalCluster cluster, double clusterSeedEnergyLowThreshold, double clusterSeedEnergyHighThreshold) {
    	// Get the seed hit's energy.
    	double seedEnergy = cluster.getSeedHit().getRawEnergy();
    	
    	// Check if the cluster passes the thresholds.
    	boolean highCut = (seedEnergy <= clusterSeedEnergyHighThreshold);
    	boolean lowCut = (seedEnergy >= clusterSeedEnergyLowThreshold);
    	
    	// Return whether the cluster passed the cut.
    	return (highCut && lowCut);
    }
    
    /**
     * <b>clusterTotalEnergyCut</b><br/><br/>
     * <code>private boolean <b>clusterTotalEnergyCut</b>(HPSEcalCluster cluster)</code><br/><br/>
     * Cuts clusters where the total cluster energy falls outside of a
     * certain range.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut,
     * and <code>false</code> otherwise.
     */
    private boolean clusterTotalEnergyCut(HPSEcalCluster cluster, double clusterEnergyLowThreshold, double clusterEnergyHighThreshold) {
    	// Get the cluster's total energy.
    	double clusterEnergy = cluster.getEnergy();
    	
    	// Check if the cluster passes the thresholds.
    	boolean highCut = clusterEnergy < clusterEnergyHighThreshold;
    	boolean lowCut = clusterEnergy > clusterEnergyLowThreshold;
    	
    	// Return whether the cluster passed the cuts.
        return highCut && lowCut;
    }
    
    /**
     * <b>getClusterAngle</b><br/><br/>
     * <code>private double <b>getClusterAngle</b>(HPSEcalCluster cluster)</code><br/><br/>
     * Calculates the angle between the x axis and the cluster in
     * degrees within the range of -180 to 180.
     * @param cluster - The cluster for which the angle should be
     * calculated.
     * @return Returns the angle as a <code>double</code> in degrees.
     */
    private double getClusterAngle(HPSEcalCluster cluster) {
    	// Get the cluster's position.
        double position[] = cluster.getSeedHit().getPosition();
        
        // Perform the appropriate trigonometric calculation.
        return Math.toDegrees(Math.atan2(position[1], position[0] - originX));
    }
    
    /**
     * <b>getClusterDistance</b><br/><br/>
     * <code>private double <b>getClusterDistance</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Calculates the radial distance between the given cluster and
     * the origin.
     * @param cluster - The cluster on which to perform the calculation.
     * @return Returns the radial distance as a <code>double</code>.
     */
    private double getClusterDistance(HPSEcalCluster cluster) {
        return Math.hypot(cluster.getSeedHit().getPosition()[0] - originX, cluster.getSeedHit().getPosition()[1]);
    }
    
    /**
     * <b>getClusterPairsTopBot</b><br/><br/>
     * <code>private List<HPSEcalCluster[]> <b>getClusterPairsTopBot</b>()</code><br/><br/>
     * Get a list of all unique cluster pairs in the event
     * @param ecalClusters : List of ECal clusters
     * @return list of cluster pairs
     */
    private List<HPSEcalCluster[]> getClusterPairsTopBot() {
        // Make a list of cluster pairs
        List<HPSEcalCluster[]> clusterPairs = new ArrayList<HPSEcalCluster[]>();
        
        // To apply pair coincidence time, use only bottom clusters from the 
        // readout cycle pairCoincidence readout cycles ago, and top clusters 
        // from all 2*pairCoincidence+1 previous readout cycles
        
        // Loop over all the bottom clusters in the first list in the queue.
        for (HPSEcalCluster botCluster : botClusterQueue.element()) {
        	// Loop over all the top cluster lists.
            for (List<HPSEcalCluster> topClusters : topClusterQueue) {
            	// Create a cluster pair between the current bottom
            	// cluster and the current top cluster. Note that the
            	// higher energy cluster goes first in the pair.
                for (HPSEcalCluster topCluster : topClusters) {
                	// If the top cluster has more energy, it goes first.
                    if (topCluster.getEnergy() > botCluster.getEnergy()) {
                        HPSEcalCluster[] clusterPair = {topCluster, botCluster};
                        clusterPairs.add(clusterPair);
                    }
                    
                    // Otherwise, the bottom cluster goes first.
                    else {
                        HPSEcalCluster[] clusterPair = {botCluster, topCluster};
                        clusterPairs.add(clusterPair);
                    }
                }
            }
        }
        
        // Return the list of cluster pairs.
        return clusterPairs;
    }
    
    /**
     * <b>pairUncoplanarity</b><br/><br/>
     * <code>private double <b>pairUncoplanarity</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Calculates the angle between the clusters given in the argument.
     * @param clusterPair - The clusters between which the angle should
     * be found.
     * @return Returns the cluster angle as a <code>double</code> in degrees.
     */
    private double getPairCoplanarity(HPSEcalCluster[] clusterPair) {
    	// Get the angle of each cluster and shift it such that it
    	// is always positive.
        double cluster1Angle = (getClusterAngle(clusterPair[0]) + 180.0) % 180.0;
        double cluster2Angle = (getClusterAngle(clusterPair[1]) + 180.0) % 180.0;
        
        // Return the difference between the angles.
        return cluster2Angle - cluster1Angle;
    }
    
    /**
     * <b>pairCoplanarityCut</b><br/><br/>
     * <code>private boolean <b>pairCoplanarityCut</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Cuts cluster pairs where the clusters are not sufficiently
     * coplanar with the beam.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if pair passes the cut, and
     * <code>false</code> otherwise.
     */
    private boolean pairCoplanarityCut(HPSEcalCluster[] clusterPair) {
    	// Get the coplanarity value.
    	double coplanarity = pairCoplanarityValue(clusterPair);
    	
    	// Return the coplanarity cut.
        return (coplanarity < coplanarityHighCut);
    }
    
    /**
     * <b>pairEnergyDifferenceCut</b><br/><br/>
     * <code>private boolean <b>pairEnergyDifferenceCut</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Cuts cluster pairs where the difference of the energy of the
     * two clusters exceeds some threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if pair passes the cut, and
     * <code>false</code> otherwise.
     */
    private boolean pairEnergyDifferenceCut(HPSEcalCluster[] clusterPair) {
    	// Get the difference between the high energy cluster energy
    	// and the low energy cluster energy.
        double energyDifference = pairEnergyDifferenceValue(clusterPair);
        
        // If the difference is below the threshold, it passes.
        return (energyDifference < energyDifferenceHighThreshold);
    }
    
    /**
     * <b>pairEnergySlopeCut</b><br/><br/>
     * <code>private boolean <b>pairEnergySlopeCut</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Cuts cluster pairs where the energy slope exceeds a given
     * threshold. Energy slope is defined as E + (R * F), where
     * E is the energy of the lower energy cluster in the pair,
     * R is the same cluster's radial distance from the origin,
     * and F is a programmable parameter.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if pair passes the cut, and
     * <code>false</code> otherwise.
     */
    private boolean pairEnergySlopeCut(HPSEcalCluster[] clusterPair) {
    	// Calculate the energy slope.
    	double energySlope = pairEnergySlopeValue(clusterPair);
    	
    	// If the energy slope exceeds the threshold, it passes the cut.
        return (energySlope > energySlopeHighThreshold);
    }
    
    /**
     * <b>pairEnergySumCut</b><br/><br/>
     * <code>private boolean <b>pairEnergySumCut</b>(HPSEcalCluster[] clusterPair)</code><br/><br/>
     * Cuts cluster pairs where the sum of the energy of the two
     * clusters exceeds some threshold.
     * @param clusterPair - The cluster pair to check.
     * @return Returns <code>true</code> if pair passes the cut, and
     * <code>false</code> otherwise.
     */
    private boolean pairEnergySumCut(HPSEcalCluster[] clusterPair) {
    	// Get the sum of the cluster energies.
        double energySum = pairEnergySumValue(clusterPair);
        
        // If it is below the threshold, it passes.
        return (energySum < energySumHighThreshold) && (energySum > energySumLowThreshold);
    }
    
    /**
     * <b>printCounts</b><br/><br/>
     * <code>private void <b>printCounts</b>(PrintWriter writer)</code><br/><br/>
     * Writes the results of the trigger cuts to the output stream.
     * @param writer - The output stream to which trigger data should
     * be written.
     */
    private void printCounts(PrintWriter writer) {
		// Print Cut Values
    	writer.printf("Cuts:%n\tHit Count: %d%n", hitCountLowThreshold);
    	writer.printf("\tCluster Energy (Low): %f%n", clusterEnergyLowThreshold);
    	writer.printf("\tCluster Energy (High): %f%n", clusterEnergyHighThreshold);
    	writer.printf("\tSeed Energy (Low): %f%n", clusterSeedEnergyLowThreshold);
    	writer.printf("\tSeed Energy (High): %f%n", clusterSeedEnergyHighThreshold);
    	writer.printf("\tEnergy Sum (Low): %f%n", energySumLowThreshold);
    	writer.printf("\tEnergy Sum (Low): %f%n", energySumHighThreshold);
    	writer.printf("\tEnergy Difference: %f%n", energyDifferenceHighThreshold);
    	writer.printf("\tEnergy Slope: %f%n", energySlopeHighThreshold);
      	writer.printf("\tEnergy Slope F: %f%n", energySlopeParamF);
    	writer.printf("\tPair Coplanarity: %f%n", coplanarityHighCut);
    	
    	// Print results.
    	writer.printf("Results\n");
    	writer.printf("Number of Clusters: %d\n", allClusters);
    	writer.printf("\tClusters After Successive Cuts:\n");
        writer.printf("\tSeed Hit Energy Cut  :: %d\n", clusterSeedEnergyCount);
        writer.printf("\tTotal Energy Cut     :: %d\n", clusterTotalEnergyCount);
        writer.printf("\tHit Count Cut        :: %d\n", clusterHitCountCount);
        writer.printf("\n");
        writer.printf("Number of Pairs: %d\n", allPairs);
        writer.printf("\tPairs After Successive Cuts:\n");
        writer.printf("\tEnergy Sum Cut       :: %d\n", energySumCount);
        writer.printf("\tEnergy Difference Cut:: %d\n", energyDifferenceCount);
        writer.printf("\tEnergy Slope Cut     :: %d\n", energyDistanceCount);
        writer.printf("\tPair Coplanarity Cut :: %d\n", coplanarityCount);
        writer.printf("Trigger Count: %d\n", numTriggers);
        writer.printf("Beam Energy: %f\n", beamEnergy);
        writer.close();
    }
    
    /**
     * <b>updateClusterQueues</b><br/><br/>
     * <code>private void <b>updateClusterQueues</b>(List<HPSEcalCluster> ecalClusters)</code><br/><br/>
     * Splits the event clusters into two lists based on whether they
     * are in the top or bottom portion of the calorimeter.
     * @param ecalClusters - The list of clusters.
     */
    private void updateClusterQueues(List<HPSEcalCluster> ecalClusters) {
    	// Make the top and bottom cluster lists.
        ArrayList<HPSEcalCluster> topClusterList = new ArrayList<HPSEcalCluster>();
        ArrayList<HPSEcalCluster> botClusterList = new ArrayList<HPSEcalCluster>();
        
        // Sort through the list of clusters.
        for (HPSEcalCluster ecalCluster : ecalClusters) {
        	// Get the cluster y position.
        	int iy = ecalCluster.getSeedHit().getIdentifierFieldValue("iy");
        	
        	// Clusters with y > 0 are in the top of the calorimeter.
            if (iy > 0) { topClusterList.add(ecalCluster); }
            
            // Otherwise, they are in the bottom.
            else { botClusterList.add(ecalCluster); }
        }
        
        // Add the cluster lists the appropriate queue.
        topClusterQueue.add(topClusterList);
        botClusterQueue.add(botClusterList);
        topClusterQueue.remove();
        botClusterQueue.remove();
    }
    
    
    
    
    
    private double pairCoplanarityValue(HPSEcalCluster[] clusterPair) {
    	// Get the coplanarity value.
    	return Math.abs(getPairCoplanarity(clusterPair));
    }
    
    private double pairEnergyDifferenceValue(HPSEcalCluster[] clusterPair) {
        return clusterPair[0].getEnergy() - clusterPair[1].getEnergy();
    }
    
    private double pairEnergySlopeValue(HPSEcalCluster[] clusterPair) {
    	// E + R*F
    	// Get the low energy cluster energy.
    	double slopeParamE = clusterPair[1].getEnergy();
    	
    	// Get the low energy cluster radial distance.
    	double slopeParamR = getClusterDistance(clusterPair[1]);
    	
    	// Calculate the energy slope.
    	return slopeParamE + slopeParamR * energySlopeParamF;
    }
    
    private double pairEnergySumValue(HPSEcalCluster[] clusterPair) {
    	return clusterPair[0].getEnergy() + clusterPair[1].getEnergy();
    }
}