package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.solids.Trd;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>NeutralPionTriggerDriver</code> simulates a pi-0 trigger.
 * It executes four cuts, two of which are single cluster cuts and two
 * of which are cluster pair cuts. The single cluster cuts are on the
 * total energy of the cluster and the energy of the seed hit of the
 * cluster. The first cluster pair cut is on the sum of the energies of
 * both clusters. The second calculates the invariant mass of the
 * particle that produced the clusters, assuming that clusters were
 * created by an electron/positron pair. The pair is then cut if the
 * invariant mass is outside the expected range for a neutral pion decay.
 * <br/><br/>
 * All incoming clusters are passed through the single cluster cuts and
 * those which survive are added to a list of clusters for their event
 * and stored in a buffer. The buffer stores a number of event lists
 * equal to coincidence window parameter. This limits the time frame
 * in which clusters can be used for a trigger. Of the clusters stored
 * in the cluster buffer, the two with the highest energies are chosen
 * and the cluster pair cuts are applied to them. If the highest energy
 * pair survives this process, the event triggers. If it does not,
 * there is no trigger for the event.
 * <br/><br/>
 * All thresholds can be set through a steering file, along with the
 * coincidence window. The driver also supports a verbose mode where
 * it will output more details with every event to help with diagnostics.
 * 
 * @author Kyle McCarty
 * @author Michel Garçon
 */
public class NeutralPionTriggerDriver extends TriggerDriver {
    
    // ==================================================================
    // ==== Trigger Algorithms ==========================================
    // ==================================================================    
    
    @Override
    public void endOfData() {
        // Print out the results of the trigger cuts.
        System.out.printf("Trigger Processing Results%n");
        System.out.printf("\tSingle-Cluster Cuts%n");
        System.out.printf("\t\tTotal Clusters Processed     :: %d%n", allClusters);
        System.out.printf("\t\tPassed Seed Energy Cut       :: %d%n", clusterSeedEnergyCount);
        System.out.printf("\t\tPassed Hit Count Cut         :: %d%n", clusterHitCountCount);
        if(rejectEdgeCrystals) {
            System.out.printf("\t\tPassed Edge Crystal Cut      :: %d%n", clusterEdgeCount);
        }
        System.out.printf("%n");
        System.out.printf("\tCluster Pair Cuts%n");
        System.out.printf("\t\tTotal Pairs Processed        :: %d%n", allPairs);
        System.out.printf("\t\tPassed Energy Sum Cut        :: %d%n", pairEnergySumCount);
        System.out.printf("\t\tPassed Energy Invariant Mass :: %d%n", pairInvariantMassCount);
        System.out.printf("%n");
        System.out.printf("\tTrigger Count :: %d%n", triggers);
        
        // Run the superclass method.
        super.endOfData();
    }
    
    public void process(EventHeader event) {
        // Generate a temporary list to store the good clusters
        // in before they are added to the buffer.
        List<Cluster> tempList = new ArrayList<Cluster>();
        
        // If the current event has a cluster collection, get it.
        if(event.hasCollection(Cluster.class, clusterCollectionName)) {
            // VERBOSE :: Note that a cluster collection exists for
            //            this event.
            if(verbose) { System.out.println("Cluster collection is present for event."); }
            
            // Get the cluster list from the event.
            List<Cluster> eventList = event.get(Cluster.class, clusterCollectionName);
            
            // VERBOSE :: Output the number of extant clusters.
            if(verbose) { System.out.printf("%d clusters in event.%n", eventList.size()); }
            
            // Add the clusters from the event into the cluster list
            // if they pass the minimum total cluster energy and seed
            // energy thresholds.
            for(Cluster cluster : eventList) {
                // Increment the clusters processed count.
                allClusters++;
                
                // Plot the seed energy / cluster energy histogram.
                seedPercent.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy() / cluster.getEnergy(), 1);
                
                // Get the cluster position indices.
                int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                
                // VERBOSE :: Output the current cluster's properties.
                if(verbose) {
                    System.out.printf("\tTesting cluster at (%d, %d) with total energy %f and seed energy %f.%n",
                            ix, iy, cluster.getCalorimeterHits().get(0).getCorrectedEnergy(), cluster.getEnergy());
                }
                
                // Add the clusters to the uncut histograms.
                clusterHitCount.fill(cluster.getCalorimeterHits().size());
                clusterTotalEnergy.fill(cluster.getEnergy());
                clusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
                clusterDistribution.fill(ix, iy, 1);
                
                // VERBOSE :: Output the single cluster trigger thresholds.
                if(verbose) {
                    System.out.printf("\tCluster seed energy threshold  :: [%f, %f]%n", clusterSeedEnergyThresholdLow, clusterSeedEnergyThresholdHigh);
                    System.out.printf("\tCluster total energy threshold :: %f%n%n", clusterTotalEnergyThresholdLow);
                }
                
                // Perform the single cluster cuts.
                boolean totalEnergyCut = clusterTotalEnergyCut(cluster);
                boolean seedEnergyCut = clusterSeedEnergyCut(cluster);
                boolean hitCountCut = clusterHitCountCut(cluster);
                boolean edgeCrystalCut = isEdgeCluster(cluster);
                
                // Increment the single cut counts.
                if(seedEnergyCut) {
                    clusterSeedEnergyCount++;
                    if(hitCountCut) {
                        clusterHitCountCount++;
                        if(rejectEdgeCrystals && edgeCrystalCut) {
                            clusterEdgeCount++;
                        }
                    }
                }
                
                // VERBOSE :: Note whether the cluster passed the single
                //            cluster cuts.
                if(verbose) {
                    System.out.printf("\tPassed seed energy cut    :: %b%n", seedEnergyCut);
                    System.out.printf("\tPassed cluster energy cut :: %b%n%n", totalEnergyCut);
                    System.out.printf("\tPassed hit count cut :: %b%n%n", hitCountCut);
                    System.out.printf("\tIs an edge cluster :: %b%n%n", edgeCrystalCut);
                }
                
                // Determine whether the cluster passes all the single
                // cluster cuts.
                boolean passedCuts = false;
                
                // If edge crystals should be not be used for triggering,
                // require that the cluster not be centered in an edge
                // crystal.
                if(rejectEdgeCrystals) {
                    if(totalEnergyCut && seedEnergyCut && hitCountCut && !edgeCrystalCut) {
                        passedCuts = true;
                    }
                }
                
                // Otherwise, it just needs to pass the standard trigger
                // cuts regardless of where it is located.
                else {
                    if(totalEnergyCut && seedEnergyCut && hitCountCut) {
                        passedCuts = true;
                    }
                }
                
                // If both pass, add the cluster to the list.
                if(passedCuts) {
                    // Add the cluster to the cluster list.
                    tempList.add(cluster);
                    
                    // Add the cluster information to the single cut histograms.
                    pClusterHitCount.fill(cluster.getCalorimeterHits().size());
                    pClusterTotalEnergy.fill(cluster.getEnergy());
                    pClusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
                    pClusterDistribution.fill(ix, iy, 1);
                }
            }
            
            // Remove the oldest cluster buffer element and add the new
            // cluster list to the buffer.
            clusterBuffer.removeFirst();
            clusterBuffer.addLast(tempList);
        }
        
        // Otherwise, clear the cluster list.
        else {
            // VERBOSE :: Note that the event has no clusters.
            if(verbose) { System.out.println("No cluster collection is present for event.\n"); }
        }
        
        // Reset the highest energy pair to null.
        clusterTriplet[0] = null;
        clusterTriplet[1] = null;
        clusterTriplet[2] = null;
        
        // Loop over all of the cluster lists in the cluster buffer.
        double[] energy = { 0.0, 0.0, 0.0 };
        for(List<Cluster> bufferList : clusterBuffer) {
            // Loop over all of the clusters in each buffer list.
            for(Cluster cluster : bufferList) {
                // If the new cluster is higher energy than the first
                // slot cluster, move the subsequent clusters down and
                // insert the new one.
                if(cluster.getEnergy() > energy[0]) {
                    clusterTriplet[2] = clusterTriplet[1];
                    clusterTriplet[1] = clusterTriplet[0];
                    clusterTriplet[0] = cluster;
                    energy[2] = energy[1];
                    energy[1] = energy[0];
                    energy[0] = cluster.getEnergy();
                }
                
                // Otherwise, if the new cluster has more energy than
                // the second slot, it goes there and the second does
                // to the third.
                else if(cluster.getEnergy() > energy[1]) {
                    clusterTriplet[2] = clusterTriplet[1];
                    clusterTriplet[1] = cluster;
                    energy[2] = energy[1];
                    energy[1] = cluster.getEnergy();
                }
                
                // If the new cluster has more energy than the third
                // cluster, it just replaces it.
                else if(cluster.getEnergy() > energy[2]) {
                    clusterTriplet[2] = cluster;
                    energy[2] = cluster.getEnergy();
                }
            }
        }
        
        // The highest energy pair is the same as the first two slots
        // of the highest energy triplet.
        clusterPair[0] = clusterTriplet[0];
        clusterPair[1] = clusterTriplet[1];
        
        // Run the superclass event process.
        super.process(event);
    }
    
    public void startOfData() {
        // Initialize the cluster buffer to the size of the coincidence window.
        clusterBuffer = new LinkedList<List<Cluster>>();
        
        // Populate the buffer with empty lists.
        for(int i = 0; i < coincidenceWindow; i++) {
            clusterBuffer.add(new ArrayList<Cluster>(0));
        }
        
        // Initialize the cluster hit count diagnostic plots.
        clusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution", 9, 1, 10);
        pClusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed Single Cuts)", 9, 1, 10);
        aClusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed All Cuts)", 9, 1, 10);
        
        // Initialize the cluster total energy diagnostic plots.
        clusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution", 176, 0.0, 2.2);
        pClusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
        aClusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
        
        // Initialize the cluster seed energy diagnostic plots.
        clusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution", 176, 0.0, 2.2);
        pClusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
        aClusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed All Cuts)", 176, 0.0, 2.2);
        
        // Initialize the seed distribution diagnostic plots.
        clusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution", 44, -22.0, 22.0, 10, -5, 5);
        pClusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed Single Cuts)", 44, -23, 23, 11, -5.5, 5.5);
        aClusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed All Cuts)", 44, -23, 23, 11, -5.5, 5.5);
        
        // Initialize the cluster pair energy sum diagnostic plots.
        pairEnergySum = aida.histogram1D("Trigger Plots :: Pair Energy Sum Distribution", 176, 0.0, 2.2);
        pPairEnergySum = aida.histogram1D("Trigger Plots :: Pair Energy Sum Distribution (Passed Pair Cuts)", 176, 0.0, 2.2);
        
        // Initialize the cluster pair hypothetical invariant mass diagnostic plots.
        invariantMass = aida.histogram1D("Trigger Plots :: Invariant Mass Distribution", 1500, 0.0, 0.03);
        pInvariantMass = aida.histogram1D("Trigger Plots :: Invariant Mass Distribution (Passed Pair Cuts)", 1500, 0.0, 0.03);
        
        // Initialize the seed percentage of cluster energy.
        seedPercent = aida.histogram1D("Analysis Plots :: Seed Percentage of Total Energy", 400, 0.0, 1.0);
    }
    
    protected boolean triggerDecision(EventHeader event) {
        // If the active cluster pair has a null value, then there were
        // fewer than two clusters in the buffer and we can not trigger.
        if(!useClusterTriplet && (clusterPair[0] == null || clusterPair[1] == null)) {
            // VERBOSE :: Note that triggering failed due to insufficient
            // clusters. in the cluster buffer.
            if(verbose) { System.out.println("Inufficient clusters in buffer -- no trigger."); }
            
            // Return false; we can not trigger without two clusters.
            return false;
        }
        
        // If the active cluster triplet has a null value, then there
        // were fewer than three clusters in the buffer and we can not
        // trigger.
        if(useClusterTriplet && (clusterTriplet[0] == null || clusterTriplet[1] == null || clusterTriplet[2] == null)) {
            // VERBOSE :: Note that triggering failed due to insufficient
            // clusters. in the cluster buffer.
            if(verbose) { System.out.println("Inufficient clusters in buffer -- no trigger."); }
            
            // Return false; we can not trigger without three clusters.
            return false;
        }
        
        // Increment the number of pairs considered.
        allPairs++;
        
        // Get the cluster position indices.
        int[] ix = { clusterPair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix"), clusterPair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix") };
        int[] iy = { clusterPair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("iy"), clusterPair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("iy") };
        
        // VERBOSE :: Output the clusters selected for triggering.
        if(verbose) {
            System.out.printf("\tTesting first cluster at (%d, %d) with total energy %f and seed energy %f.%n",
                    ix[0], iy[0], clusterPair[0].getCalorimeterHits().get(0).getCorrectedEnergy(), clusterPair[0].getEnergy());
            System.out.printf("\tTesting second cluster at (%d, %d) with total energy %f and seed energy %f.%n",
                    ix[1], iy[1], clusterPair[1].getCalorimeterHits().get(0).getCorrectedEnergy(), clusterPair[1].getEnergy());
            if(useClusterTriplet) {
                System.out.printf("\tTesting third cluster at (%d, %d) with total energy %f and seed energy %f.%n",
                        ix[1], iy[1], clusterTriplet[2].getCalorimeterHits().get(0).getCorrectedEnergy(), clusterTriplet[2].getEnergy());
            }
        }
        
        if(!useClusterTriplet) {
            // Fill the uncut histograms.
            pairEnergySum.fill(getEnergySumValue(clusterPair));
            invariantMass.fill(getInvariantMassValue(clusterPair));
            
            // VERBOSE :: Output the cluster pair trigger thresholds.
            if(verbose) {
                System.out.printf("\tCluster pair energy sum threshold     :: %f%n", pairEnergySumThresholdLow);
                System.out.printf("\tHypothetical invariant mass threshold :: [%f, %f]%n%n", invariantMassThresholdLow, invariantMassThresholdHigh);
            }
            
            // Perform the cluster pair checks.
            boolean energySumCut = pairEnergySumCut(clusterPair);
            boolean invariantMassCut = pairInvariantMassCut(clusterPair);
            
            // Increment the pair cut counts.
            if(energySumCut) {
                pairEnergySumCount++;
                if(invariantMassCut) {
                    pairInvariantMassCount++;
                }
            }
            
            // VERBOSE :: Note the outcome of the trigger cuts.
            if(verbose) {
                System.out.printf("\tPassed energy sum cut     :: %b%n", energySumCut);
                System.out.printf("\tPassed invariant mass cut :: %b%n%n", invariantMassCut);
            }
            
            // If the pair passes both cuts, we have a trigger.
            if(energySumCut && invariantMassCut) {
                // Fill the cut histograms.
                pPairEnergySum.fill(getEnergySumValue(clusterPair));
                pInvariantMass.fill(getInvariantMassValue(clusterPair));
                
                // Fill the all cuts histograms.
                aClusterHitCount.fill(clusterPair[0].getCalorimeterHits().size());
                aClusterHitCount.fill(clusterPair[1].getCalorimeterHits().size());
                aClusterTotalEnergy.fill(clusterPair[0].getEnergy());
                aClusterTotalEnergy.fill(clusterPair[1].getEnergy());
                aClusterSeedEnergy.fill(clusterPair[0].getCalorimeterHits().get(0).getCorrectedEnergy());
                aClusterSeedEnergy.fill(clusterPair[1].getCalorimeterHits().get(0).getCorrectedEnergy());
                aClusterDistribution.fill(ix[0], iy[0], 1);
                aClusterDistribution.fill(ix[1], iy[1], 1);
                
                // VERBOSE :: Note that the event has triggered.
                if(verbose) { System.out.println("Event triggers!\n\n"); }
                
                // Increment the number of triggers.
                triggers++;
                
                // Return the trigger.
                return true;
            }
        }
        
        // If we are using a cluster triplet, apply the cluster triplet
        // cuts.
        else {
            // Perform the cluster triplet checks.
            boolean energySumCut = tripletEnergySumCut(clusterTriplet);
            boolean horizontalCut = tripletHorizontalCut(clusterTriplet);
            boolean energySpatialCut = tripletTotalEnergyCut(clusterTriplet);
            
            // Fill the all cuts histograms.
            aClusterHitCount.fill(clusterPair[0].getCalorimeterHits().size());
            aClusterHitCount.fill(clusterPair[1].getCalorimeterHits().size());
            aClusterTotalEnergy.fill(clusterPair[0].getEnergy());
            aClusterTotalEnergy.fill(clusterPair[1].getEnergy());
            aClusterSeedEnergy.fill(clusterPair[0].getCalorimeterHits().get(0).getCorrectedEnergy());
            aClusterSeedEnergy.fill(clusterPair[1].getCalorimeterHits().get(0).getCorrectedEnergy());
            aClusterDistribution.fill(ix[0], iy[0], 1);
            aClusterDistribution.fill(ix[1], iy[1], 1);
            
            if(energySumCut && horizontalCut && energySpatialCut) {
                return true;
            }
        }
        
        // VERBOSE :: Note that the event has failed to trigger.
        if(verbose) { System.out.println("No trigger.\n\n"); }
        
        // If one or more of the pair cuts failed, the we do not trigger.
        return false;
    }
    
    // ==================================================================
    // ==== Trigger Cut Methods =========================================
    // ==================================================================
    
    /**
     * Checks whether the cluster passes the threshold for minimum
     * number of component hits.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterHitCountCut(Cluster cluster) {
        return cluster.getCalorimeterHits().size() >= clusterHitCountThreshold;
    }
    
    /**
     * Checks whether the cluster falls within the allowed range for
     * the seed hit energy cut.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterSeedEnergyCut(Cluster cluster) {
        // Get the seed energy value.
        double seedEnergy = cluster.getCalorimeterHits().get(0).getCorrectedEnergy();
        
        // Perform the seed energy cut.
        return seedEnergy >= clusterSeedEnergyThresholdLow && seedEnergy <= clusterSeedEnergyThresholdHigh;
    }
    
    /**
     * Checks whether the cluster passes the threshold for minimum
     * total cluster energy.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes and <code>
     * false</code> if it does not.
     */
    private boolean clusterTotalEnergyCut(Cluster cluster) {
        // Get the cluster energy.
        double clusterEnergy = cluster.getEnergy();
        
        // Perform the cut.
        return clusterEnergy >= clusterTotalEnergyThresholdLow && clusterEnergy <= clusterTotalEnergyThresholdHigh;
    }
    
    /**
     * Calculates the value used in the pair energy sum cut from a pair
     * of two clusters.
     * @param clusterPair - The cluster pair from which to derive the
     * cut value.
     * @return Returns the cut value as a <code>double</code>.
     */
    private static double getEnergySumValue(Cluster[] clusterGroup) {
        // Track the sum.
        double energySum = 0.0;
        
        // Add the energies of all clusters in the array.
        for(Cluster cluster : clusterGroup) { energySum += cluster.getEnergy(); }
        
        // Return the sum.
        return energySum;
    }
    
    /**
     * Calculates the value used in the invariant mass cut from a pair
     * of two clusters.
     * @param clusterPair - The cluster pair from which to derive the
     * cut value.
     * @return Returns the cut value as a <code>double</code>.
     */
    private double getInvariantMassValue(Cluster[] clusterPair) {
        // Store the x/y positions for the seeds.
        double x[] = new double[2];
        double y[] = new double[2];
        
        // Get the seed hits.
        CalorimeterHit[] seed = { clusterPair[0].getCalorimeterHits().get(0), clusterPair[1].getCalorimeterHits().get(0) };
        
        // Set the positions for each seed.
        for(int index = 0; index < seed.length; index++) {
            // Get the seed position array stored in the position map.
            Double[] seedPos = seedPosMap.get(clusterPair[index].getCalorimeterHits().get(0));
            
            // If there is a position array for the seed, use it.
            if(seedPos != null) {
                x[index] = seedPos[0];
                y[index] = seedPos[1];
            }
            
            // Otherwise, calculate the position at the crystal face.
            else {
                // Get the position and store it in a double array.
                IGeometryInfo geom = clusterPair[index].getCalorimeterHits().get(0).getDetectorElement().getGeometry();
                double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()),
                        (Hep3Vector) new BasicHep3Vector(0, 0, -1 * ((Trd) geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();
                
                // Set the seed location.
                x[index] = pos[0];
                y[index] = pos[1];
                
                // Store the seed location for future use.
                Double[] positionVec = { pos[0], pos[1], pos[2] };
                seedPosMap.put(clusterPair[index].getCalorimeterHits().get(0), positionVec);
            }
        }
        
        // Get the cluster energy for each seed.
        double[] e = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
        
        //Return the invariant mass.
        return (e[0] * e[1] * (Math.pow(x[0] - x[1], 2) + Math.pow(y[0] - y[1], 2)) / D2);
    }
    
    /**
     * Indicates whether a cluster has a seed hit located on the edge
     * of the calorimeter or not.
     * 
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster seed is on the
     * edge of the calorimeter and <code>false</code> otherwise.
     */
    private static boolean isEdgeCluster(Cluster cluster) {
        // Get the x- and y-indices of the cluster seed hit.
        int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
        int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
        
        // Track whether the cluster is an edge cluster or not.
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
        return edge;
    }
    
    /**
     * Checks whether the cluster pair passes the falls within the
     * allowed range for the piar energy sum cut.
     * @param clusterPair - An array of size two containing the cluster
     * pair to check.
     * @return Returns <code>true</code> if the clusters pass and <code>
     * false</code> if they does not.
     */
    private boolean pairEnergySumCut(Cluster[] clusterPair) {
        // Get the energy sum value.
        double energySum = getEnergySumValue(clusterPair);
        
        // Otherwise, get the energy sum and compare it to the threshold.
        return energySum >= pairEnergySumThresholdLow && energySum <= pairEnergySumThresholdHigh;
    }
    
    /**
     * Checks whether the cluster pair passes the threshold for the
     * invariant mass check.
     * @param clusterPair - An array of size two containing the cluster
     * pair to check.
     * @return Returns <code>true</code> if the clusters pass and <code>
     * false</code> if they does not.
     */
    private boolean pairInvariantMassCut(Cluster[] clusterPair) {
        // Calculate the invariant mass.
        double myy2 = getInvariantMassValue(clusterPair);
        
        // Perform the cut.
        return ( (myy2 >= invariantMassThresholdLow) && (myy2 <= invariantMassThresholdHigh));
    }
    
    /**
     * Checks whether the cluster pair passes the threshold for the
     * minimum pair energy sum check.
     * @param clusterTriplet - An array of size three containing the
     * cluster triplet to check.
     * @return Returns <code>true</code> if the clusters pass and <code>
     * false</code> if they does not.
     */
    private boolean tripletEnergySumCut(Cluster[] clusterTriplet) {
        return (getEnergySumValue(clusterTriplet) >= tripletEnergySumThreshold);
    }
    
    /**
     * Checks that there is at least one cluster is located on the right
     * side and at least one cluster on the left side of the calorimeter.
     * @param clusterTriplet - An array of size three containing the
     * cluster triplet to check.
     * @return Returns <code>true</code> if the clusters pass and <code>
     * false</code> if they does not.
     */
    private static boolean tripletHorizontalCut(Cluster[] clusterTriplet) {
        // Track whether a cluster has occurred on each horizontal side
        // of the calorimeter.
        boolean leftCluster = false;
        boolean rightCluster = false;
        
        // Sort through the cluster triplet and check where they occur.
        for(Cluster cluster : clusterTriplet) {
            int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
            if(ix < 0) { leftCluster = true; }
            if(ix > 0) { rightCluster = true; }
        }
        
        // If a cluster fell on both sides, it passes.
        if(leftCluster && rightCluster) { return true; }
        else { return false; }
    }
    
    private boolean tripletTotalEnergyCut(Cluster[] clusterTriplet) {
        // Check to see if each cluster passes the check.
        for(Cluster cluster1 : clusterTriplet) {
            for(Cluster cluster2 : clusterTriplet) {
                // The cluster pair must be two different clusters.
                if(cluster1 == cluster2) { continue; }
                
                // Check to see if the clusters are over threshold.
                boolean over1 = cluster1.getEnergy() >= tripletTotalEnergyThreshold;
                boolean over2 = cluster1.getEnergy() >= tripletTotalEnergyThreshold;
                
                // If both the clusters are over threshold, check that
                // they are sufficiently far apart.
                if(over1 && over2) {
                    // Get the x and y coordinates of the clusters.
                    double x[] = { cluster1.getPosition()[0], cluster2.getPosition()[0] };
                    double y[] = { cluster1.getPosition()[1], cluster2.getPosition()[1] };
                    
                    // Calculate the distance between the clusters.
                    double dr = Math.sqrt(x[0] * x[0] + y[0] * y[0]);
                    
                    // Run the check.
                    if(dr >= tripletPairSeparationThreshold) { return true; }
                }
            }
        }
        
        // If none of the cluster pairs pass all the checks, the
        // triplet fails.
        return false;
    }
    
    // ==================================================================
    // ==== Variables Mutator Methods ===================================
    // ==================================================================
    
    /**
     * Sets the LCIO collection name where <code>Cluster</code>
     * objects are stored for use in the trigger.
     * @param clusterCollectionName - The name of the LCIO collection.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the minimum number of hits required for a cluster to be
     * used in triggering.
     * @param clusterHitCountThreshold - The smallest number of hits
     * in a cluster.
     */
    public void setClusterHitCountThreshold(int clusterHitCountThreshold) {
        this.clusterHitCountThreshold = clusterHitCountThreshold;
    }
    
    /**
     * Sets the threshold for the cluster seed energy of individual
     * clusters above which the cluster will be rejected and not used
     * for triggering.
     * @param clusterSeedEnergyThresholdHigh - The cluster seed energy
     * lower bound.
     */
    public void setClusterSeedEnergyThresholdHigh(double clusterSeedEnergyThresholdHigh) {
        this.clusterSeedEnergyThresholdHigh = clusterSeedEnergyThresholdHigh;
    }
    
    /**
     * Sets the threshold for the cluster seed energy of individual
     * clusters under which the cluster will be rejected and not used
     * for triggering.
     * @param clusterSeedEnergyThresholdLow - The cluster seed energy
     * lower bound.
     */
    public void setClusterSeedEnergyThresholdLow(double clusterSeedEnergyThresholdLow) {
        this.clusterSeedEnergyThresholdLow = clusterSeedEnergyThresholdLow;
    }
    
    /**
     * Sets the threshold for the total cluster energy of individual
     * clusters under which the cluster will be rejected and not used
     * for triggering.
     * @param clusterTotalEnergyThresholdLow - The cluster total energy
     * lower bound.
     */
    public void setClusterTotalEnergyThresholdLow(double clusterTotalEnergyThresholdLow) {
        this.clusterTotalEnergyThresholdLow = clusterTotalEnergyThresholdLow;
    }
    
    /**
     * Sets the threshold for the total cluster energy of individual
     * clusters above which the cluster will be rejected and not used
     * for triggering.
     * @param clusterTotalEnergyThresholdHigh - The cluster total energy
     * upper bound.
     */
    public void setClusterTotalEnergyThresholdHigh(double clusterTotalEnergyThresholdHigh) {
        this.clusterTotalEnergyThresholdHigh = clusterTotalEnergyThresholdHigh;
    }
    
    /**
     * Sets the number of events that clusters will be retained and
     * employed for triggering before they are cleared.
     * @param coincidenceWindow - The number of events that clusters
     * should be retained.
     */
    public void setCoincidenceWindow(int coincidenceWindow) {
        this.coincidenceWindow = coincidenceWindow;
    }
    
    /**
     * Sets the invariant mass threshold to accept only cluster pairs
     * with a reconstructed invariant mass within a certain number of
     * standard deviations of the mean (corrected for sampling fraction).
     * @param invariantMassSigma - The number of standard deviations
     * within which a cluster pair invariant mass is accepted.
     */
    public void setInvariantMassSigma(int invariantMassSigma) {
        this.invariantMassThresholdLow = 0.012499 - (invariantMassSigma * 0.0011095);
        this.invariantMassThresholdHigh = 0.012499 + (invariantMassSigma * 0.0011095);
    }
    
    /**
     * Sets the threshold for the calculated invariant mass of the
     * generating particle (assuming that the clusters are produced
     * by a positron/electron pair) above which the cluster pair will
     * be rejected and not produce a trigger.
     * @param invariantMassThresholdHigh - The invariant mass upper
     * bound.
     */
    public void setInvariantMassThresholdHigh(double invariantMassThresholdHigh) {
        this.invariantMassThresholdHigh = invariantMassThresholdHigh;
    }
    
    /**
     * Sets the threshold for the calculated invariant mass of the
     * generating particle (assuming that the clusters are produced
     * by a positron/electron pair) under which the cluster pair will
     * be rejected and not produce a trigger.
     * @param invariantMassThresholdLow - The invariant mass lower
     * bound.
     */
    public void setInvariantMassThresholdLow(double invariantMassThresholdLow) {
        this.invariantMassThresholdLow = invariantMassThresholdLow;
    }
    
    /**
     * Sets the threshold for the sum of the energies of a cluster pair
     * above which the pair will be rejected and not produce a trigger.
     * @param pairEnergySumThresholdHigh - The cluster pair energy sum
     * upper bound.
     */
    public void setPairEnergySumThresholdHigh(double pairEnergySumThresholdHigh) {
        this.pairEnergySumThresholdHigh = pairEnergySumThresholdHigh;
    }
    
    /**
     * Sets the threshold for the sum of the energies of a cluster pair
     * under which the pair will be rejected and not produce a trigger.
     * @param pairEnergySumThresholdLow - The cluster pair energy sum
     * lower bound.
     */
    public void setPairEnergySumThresholdLow(double pairEnergySumThresholdLow) {
        this.pairEnergySumThresholdLow = pairEnergySumThresholdLow;
    }
    
    /**
     * Sets whether clusters centered on an edge crystal should be
     * used for triggering or not.
     * 
     * @param rejectEdgeCrystals - <code>true</code> means that edge
     * clusters will not be used and <code>false</code> means that they
     * will be used.
     */
    public void setRejectEdgeCrystals(boolean rejectEdgeCrystals) {
        this.rejectEdgeCrystals = rejectEdgeCrystals;
    }
    
    /**
     * Sets the threshold for the sum of the energies of a cluster triplet
     * under which the triplet will be rejected and not produce a trigger.
     * @param tripletEnergySumThreshold - The cluster triplet energy sum
     * lower bound.
     */
    public void setTripletEnergySumThreshold(double tripletEnergySumThreshold) {
        this.tripletEnergySumThreshold = tripletEnergySumThreshold;
    }
    
    /**
     * Sets the minimum distance apart for a cluster pair within a
     * cluster triplet. Clusters that are not sufficiently far apart
     * are rejected and do not trigger. 
     * @param tripletPairSeparationThreshold - The minimum distance in
     * millimeters.
     */
    public void setTripletPairSeparationThreshold(double tripletPairSeparationThreshold) {
        this.tripletPairSeparationThreshold = tripletPairSeparationThreshold;
    }
    
    /**
     * Sets the threshold for which at least two clusters in a cluster
     * triplet will be required to surpass. Cluster triplets with one
     * or fewer clusters above the threshold will be rejected.
     * @param tripletTotalEnergyThreshold - The cluster total energy
     * that two clusters must pass.
     */
    public void setTripletTotalEnergyThreshold(double tripletTotalEnergyThreshold) {
        this.tripletTotalEnergyThreshold = tripletTotalEnergyThreshold;
    }
    
    /**
     * Toggles whether the driver will output its actions to the console
     * during run time or not.
     * @param verbose - <code>true</code> indicates that the console
     * will write its actions and <code>false</code> that it will not.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Toggles whether the driver triggers off of a pair of clusters
     * or a triplet of clusters.
     * @param useClusterTriplet - <code>true</code> indicates that a
     * triplet should be used and <code>false</code> that a pair should
     * be used.
     */
    public void setUseClusterTriplet(boolean useClusterTriplet) {
        this.useClusterTriplet = useClusterTriplet;
    }
    
    // ==================================================================
    // ==== AIDA Plots ==================================================
    // ==================================================================
    IHistogram2D aClusterDistribution;
    IHistogram1D aClusterHitCount;
    IHistogram1D aClusterSeedEnergy;
    IHistogram1D aClusterTotalEnergy;
    IHistogram2D clusterDistribution;
    IHistogram1D clusterHitCount;
    IHistogram1D clusterSeedEnergy;
    IHistogram1D clusterTotalEnergy;
    IHistogram1D invariantMass;
    IHistogram1D pairEnergySum;
    IHistogram1D pClusterHitCount;
    IHistogram2D pClusterDistribution;
    IHistogram1D pClusterSeedEnergy;
    IHistogram1D pClusterTotalEnergy;
    IHistogram1D pPairEnergySum;
    IHistogram1D pInvariantMass;
    IHistogram1D seedPercent;
    
    // ==================================================================
    // ==== Variables ===================================================
    // ==================================================================
    
    /**
     * <b>aida</b><br/><br/>
     * <code>private AIDA <b>aida</b></code><br/><br/>
     * Factory for generating histograms.
     */
    private AIDA aida = AIDA.defaultInstance();
    
    /**
     * <b>clusterBuffer</b><br/><br/>
     * <code>private LinkedList<List<Cluster>> <b>clusterBuffer</b></code><br/><br/>
     * Stores the list of clusters from each event for a finite-sized
     * buffer. The size of the buffer is determined by the coincidence
     * window.
     */
    private LinkedList<List<Cluster>> clusterBuffer;
    
    /**
     * <b>clusterCollectionName</b><br/><br/>
     * <code>private String <b>clusterCollectionName</b></code><br/><br/>
     * The name of the LCIO collection containing <code>Cluster
     * </code> objects.
     */
    private String clusterCollectionName = "EcalClusters";
    
    /**
     * <b>clusterPair</b><br/><br/>
     * <code>private Cluster[] <b>clusterPair</b></code><br/><br/>
     * Stores the two highest energy clusters located in the cluster
     * buffer. These are sorted by energy, with the highest energy
     * cluster first in the array.
     */
    private Cluster[] clusterPair = new Cluster[2];
    
    /**
     * <b>clusterHitCountThreshold</b><br/><br/>
     * <code>private int <b>clusterHitCountThreshold</b></code><br/><br/>
     * Defines the minimum number of hits required for a cluster to
     * be used in triggering.
     */
    private int clusterHitCountThreshold = 5;
    
    /**
     * <b>clusterSeedEnergyThresholdLow</b><br/><br/>
     * <code>private double <b>clusterSeedEnergyThresholdLow</b></code><br/><br/>
     * Defines the threshold for the cluster seed energy under which
     * a cluster will be rejected.
     */
    private double clusterSeedEnergyThresholdLow = 0.15;
    
    /**
     * <b>clusterSeedEnergyThresholdHigh</b><br/><br/>
     * <code>private double <b>clusterSeedEnergyThresholdHigh</b></code><br/><br/>
     * Defines the threshold for the cluster seed energy above which
     * a cluster will be rejected.
     */
    private double clusterSeedEnergyThresholdHigh = 1.00;
    
    /**
     * <b>clusterTotalEnergyThresholdLow</b><br/><br/>
     * <code>private double <b>clusterTotalEnergyThreshold</b></code><br/><br/>
     * Defines the threshold for the total cluster energy under which
     * a cluster will be rejected.
     */
    private double clusterTotalEnergyThresholdLow = 0.0;
    
    /**
     * <b>clusterTotalEnergyThresholdHigh</b><br/><br/>
     * <code>private double <b>clusterTotalEnergyThresholdHigh</b></code><br/><br/>
     * Defines the threshold for the total cluster energy above which
     * a cluster will be rejected.
     */
    private double clusterTotalEnergyThresholdHigh = Double.MAX_VALUE;
    
    /**
     * <b>clusterTriplet</b><br/><br/>
     * <code>private Cluster[] <b>clusterTriplet</b></code><br/><br/>
     * Stores the three highest energy clusters located in the cluster
     * buffer. These are sorted by energy, with the highest energy
     * cluster first in the array.
     */
    private Cluster[] clusterTriplet = new Cluster[3]; 
    
    /**
     * <b>coincidenceWindow</b><br/><br/>
     * <code>private int <b>coincidenceWindow</b></code><br/><br/>
     * The number of events for which clusters will be retained and
     * used in the trigger before they are removed.
     */
    private int coincidenceWindow = 3;
    
    /**
     * <b>D2</b><br/><br/>
     * <code>private static final double <b>D2</b></code><br/><br/>
     * The squared distance of the calorimeter from the target.
     */
    private static final double D2 = 1414 * 1414; // (1414^2 mm^2)
    
    /**
     * <b>invariantMassThresholdHigh</b><br/><br/>
     * <code>private double <b>invariantMassThresholdHigh</b></code><br/><br/>
     * Defines the threshold for the invariant mass of the generating
     * particle above which the cluster pair will be rejected.
     */
    private double invariantMassThresholdHigh = 0.01472;
    
    /**
     * <b>invariantMassThresholdLow</b><br/><br/>
     * <code>private double <b>invariantMassThresholdLow</b></code><br/><br/>
     * Defines the threshold for the invariant mass of the generating
     * particle below which the cluster pair will be rejected.
     */
    private double invariantMassThresholdLow = 0.01028;
    
    /**
     * <b>pairEnergySumThresholdLow</b><br/><br/>
     * <code>private double <b>pairEnergySumThresholdLow</b></code><br/><br/>
     * Defines the threshold for the sum of the energies of a cluster
     * pair below which the pair will be rejected.
     */
    private double pairEnergySumThresholdLow = 1.5;
    
    /**
     * <b>pairEnergySumThresholdHigh</b><br/><br/>
     * <code>private double <b>pairEnergySumThresholdHigh</b></code><br/><br/>
     * Defines the threshold for the sum of the energies of a cluster
     * pair above which the pair will be rejected.
     */
    private double pairEnergySumThresholdHigh = 1.8;
    
    /**
     * <b>rejectEdgeCrystals</b><br/><br/>
     * <code>private boolean <b>rejectEdgeCrystals</b></code><br/><br/>
     * Defines whether edge crystals should be used for triggering.
     */
    private boolean rejectEdgeCrystals = false;
    
    /**
     * <b>tripletEnergySumThreshold</b><br/><br/>
     * <code>private double <b>tripletEnergySumThreshold</b></code><br/><br/>
     * Defines the threshold for the sum of the energies of a cluster
     * triplet below which the pair will be rejected.
     */
    private double tripletEnergySumThreshold = 0.8 / 0.83;
    
    /**
     * <b>tripletPairSeparationThreshold</b><br/><br/>
     * <code>private double <b>tripletPairSeparationThreshold</b></code><br/><br/>
     * Defines the minimum distance apart required for a cluster pair
     * within a cluster triplet.
     */
    private double tripletPairSeparationThreshold = 160; // 160 mm
    
    /**
     * <b>tripletTotalEnergyThreshold</b><br/><br/>
     * <code>private double <b>tripletTotalEnergyThreshold</b></code><br/><br/>
     * Defines the threshold for the total energy of a cluster that is
     * required of at least two clusters in a triplet.
     */
    private double tripletTotalEnergyThreshold = 0.25 / 0.83;
    
    /**
     * <b>useClusterTriplet</b><br/><br/>
     * <code>private boolean <b>useClusterTriplet</b></code><br/><br/>
     */
    private boolean useClusterTriplet = false;
    
    /**
     * <b>verbose</b><br/><br/>
     * <code>private boolean <b>verbose</b></code><br/><br/>
     * Sets whether the driver outputs its clustering decisions to the
     * console or not.
     */
    private boolean verbose = false;
    
    /**
     * <b>seedPosMap</b><br/><br/>
     * <code>private Map<CalorimeterHit, Double[]> <b>seedPosMap</b></code><br/><br/>
     * Stores the positions of the crystal faces to be used in the
     * invariant mass calculations.
     */
    private Map<CalorimeterHit, Double[]> seedPosMap = new HashMap<CalorimeterHit, Double[]>();
    
    private int triggers = 0;                                      // Track the number of triggers.
    private int allClusters = 0;                                   // Track the number of clusters processed.
    private int allPairs = 0;                                      // Track the number of cluster pairs processed.
    private int clusterSeedEnergyCount = 0;                        // Track the clusters which pass the seed energy cut.
    private int clusterHitCountCount = 0;                          // Track the clusters which pass the hit count cut.
    private int clusterEdgeCount = 0;                              // Track the clusters which pass the edge cut.
    private int pairEnergySumCount = 0;                            // Track the pairs which pass the energy sum cut.
    private int pairInvariantMassCount = 0;                        // Track the pairs which pass the invariant mass cut.
}