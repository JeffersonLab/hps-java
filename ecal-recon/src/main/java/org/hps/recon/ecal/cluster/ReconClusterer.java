package org.hps.recon.ecal.cluster;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.solids.Trd;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * <p>
 * This class creates clusters from a CalorimeterHit input collection.
 * <p>
 * This clustering logic is based on that from the <a
 * href="https://misportal.jlab.org/ul/Physics/Hall-B/clas/viewFile.cfm/2005-001.pdf?documentId=6"
 * >CLAS-Note-2005-001</a>.
 * <p>
 * The analysis and position corrections are described in <a
 * href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note
 * 2014-001</a>.
 * <p>
 * The algorithm sorts hits from highest to lowest energy and builds clusters around each local
 * maximum/seed hit. Common hits are distributed between clusters when minimum between two clusters.
 * There is a threshold cut for minimum hit energy, minimum cluster energy, and minimum seed hit
 * energy. There is also a timing threshold with respect to the seed hit. All of these parameters
 * are tunable and should be refined with more analysis. Energy corrections are applied separately.
 * 
 * @see AbstractClusterer
 * @see Clusterer
 * 
 * @author Holly Szumila-Vance <hvanc001@odu.edu>
 * @author Kyle McCarty <mccaky@gmail.com>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ReconClusterer extends AbstractClusterer {

    // Minimum energy threshold for hits; lower energy hits will be
    // excluded from clustering. Units in GeV.
    double hitEnergyThreshold = 0.0075;

    // Minimum energy threshold for seed hits; if seed hit is below
    // cluster is excluded from output. Units in GeV.
    double seedEnergyThreshold = 0.05;

    // Minimum energy threshold for cluster hits; if total cluster
    // energy is below, the cluster is excluded. Units in GeV.
    double clusterEnergyThreshold = 0.1;

    // Apply time cut to hits
    boolean useTimeCut = true;

    // Minimum time cut window range. Units in ns.
    double minTime = 0.0;

    // Maximum time cut window range. Units in ns.
    double timeWindow = 6.0;

    // Make a map for quick calculation of the x-y position of crystal face
    Map<Point, double[]> correctedPositionMap = new HashMap<Point, double[]>();

    List<CalorimeterHit> rejectedHitList = new ArrayList<CalorimeterHit>();

    ReconClusterer() {
        super(new String[] { "hitEnergyThreshold", "seedEnergyThreshold", "clusterEnergyThreshold", "minTime", "timeWindow" }, 
                new double[] { 0.0075, 0.05, 0.1, 0.0, 6.0 });
    }

    void setUseTimeCut(boolean useTimeCut) {
        this.useTimeCut = useTimeCut;
    }

    public void initialize() {
        hitEnergyThreshold = getCuts().getValue("hitEnergyThreshold");
        seedEnergyThreshold = getCuts().getValue("seedEnergyThreshold");
        if (hitEnergyThreshold > seedEnergyThreshold) {
            throw new IllegalArgumentException("hitEnergyThreshold must be <= to seedEnergyThreshold");
        }
        clusterEnergyThreshold = getCuts().getValue("clusterEnergyThreshold");
        minTime = getCuts().getValue("minTime");
        timeWindow = getCuts().getValue("timeWindow");
    }

    /**
     * Get the list of rejected hits that was made from processing the last event.
     * @return The list of rejected hit.
     */
    List<CalorimeterHit> getRejectedHitList() {
        return this.rejectedHitList;
    }

    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitList) {
                        
        // Clear the position map.
        correctedPositionMap.clear();
        
        // Clear the rejected hit list.
        rejectedHitList = new ArrayList<CalorimeterHit>();

        // Create a list for the created clusters.
        ArrayList<Cluster> clusterList = new ArrayList<Cluster>();

        // Sort the input hit list by energy.
        ClusterUtilities.sortHitsUniqueEnergy(hitList);

        // Filter the hit list of any hits that fail to pass the
        // designated threshold.
        for (int index = hitList.size() - 1; index >= 0; index--) {
            // If the hit is below threshold or below min time, kill it.
            if ((hitList.get(index).getCorrectedEnergy() < hitEnergyThreshold) || (hitList.get(index).getTime() < minTime)) {
                rejectedHitList.add(hitList.get(index));
                hitList.remove(index);
            }

            // Since the hits are sorted by energy from highest to
            // lowest, any hit that is above threshold means that all
            // subsequent hits will also be above threshold. Continue through
            // list to check in time window.
            else {
                continue;
            }
        }

        // Create a map to connect the cell ID of a calorimeter crystal to the hit which occurred in
        // that crystal.
        
//        Map<Long, CalorimeterHit> hitMap = ClusterUtilities.createHitMap(hitList);
        
        HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        
        //boolean multihit = false;
        //for (int ii = hitList.size() - 1; ii >= 0; ii--) {
        for (int ii = 0; ii <= hitList.size() - 1; ii++){
            CalorimeterHit hit = hitList.get(ii);
            if (hitMap.containsKey(hit.getCellID())) {
                // throw new RuntimeException("Multiple CalorimeterHits found in same crystal.");
                //multihit = true;
                rejectedHitList.add(hit);
                hitList.remove(ii);
                ii--;
            } else {
                hitMap.put(hit.getCellID(), hit);
            }
        }

        // Multiple hits in same channel occurs in the data, so no print out is needed here.  --JM
        //if (multihit == true) {
        //    System.err.println("Multiple CalorimeterHits found in same crystal!");            
        //}

        // Create a map to connect a seed hit to its cluster.
        Map<CalorimeterHit, BaseCluster> seedToCluster = new HashMap<CalorimeterHit, BaseCluster>();

        // Map a crystal to a list of all clusters in which it is a member.
        Map<CalorimeterHit, List<CalorimeterHit>> commonHits = new HashMap<CalorimeterHit, List<CalorimeterHit>>();

        // Map a crystal to the seed of the cluster of which it is a member.
        HashMap<CalorimeterHit, CalorimeterHit> hitToSeed = new HashMap<CalorimeterHit, CalorimeterHit>();

        // Loop through all calorimeter hits to locate seeds and perform
        // first pass calculations for component and common hits.
        for (int ii = 0; ii <= hitList.size() - 1; ii++) {
            CalorimeterHit hit = hitList.get(ii);

            // Get the set of all neighboring crystals to the current hit.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            // Generate a list to store any neighboring hits in.
            ArrayList<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();

            // Sort through the set of neighbors and, if a hit exists
            // which corresponds to a neighbor, add it to the list of
            // neighboring hits.
            for (Long neighbor : neighbors) {
                // Get the neighboring hit.
                CalorimeterHit neighborHit = hitMap.get(neighbor);

                // If it exists, add it to the list.
                if (neighborHit != null && hitList.contains(neighborHit)) {
                    neighborHits.add(neighborHit);
                }
            }

            // Track whether the current hit is a seed hit or not.
            boolean isSeed = true;

            // Loops through all the neighboring hits to determine if
            // the current hit is the local maximum within its set of
            // neighboring hits.
            seedHitLoop: for (CalorimeterHit neighbor : neighborHits) {
                if (!equalEnergies(hit, neighbor)) {
                    isSeed = false;
                    break seedHitLoop;
                }
            }

            // Hit is a local maximum
            if (isSeed) {
                // Seed must pass minimum threshold
                if (hit.getCorrectedEnergy() >= seedEnergyThreshold) {
                    // Create new cluster
                    BaseCluster cluster = createBasicCluster();
                    clusterList.add(cluster);
                    seedToCluster.put(hit, cluster);
                    hitToSeed.put(hit, hit);

                }
                // Seed does not pass minimum threshold
                else {
                    rejectedHitList.add(hit);
                    hitList.remove(ii);
                    ii--;
                }
            }// end if isSeed

            // If this hit is not a seed hit, see if it should be
            // attached to any neighboring seed hits.
            else {
                // Sort through the list of neighboring hits.
                for (CalorimeterHit neighborHit : neighborHits) {
                    // Check whether the neighboring hit is a seed.
                    // if (seedToCluster.containsKey(neighborHit)) {
                    if (hitToSeed.get(neighborHit) == neighborHit) {

                        // If the neighboring hit is a seed hit and the
                        // current hit has been associated with a cluster,
                        // then it is a common hit between its previous
                        // seed and the neighboring seed.
                        if (hitToSeed.containsKey(hit)) {
                            // Check and see if a list of common seeds
                            // for this hit already exists or not.
                            List<CalorimeterHit> commonHitList = commonHits.get(hit);

                            // If it does not, make a new one.
                            if (commonHitList == null) {
                                commonHitList = new ArrayList<CalorimeterHit>();
                            }

                            // Add the neighbors to the seeds to set of
                            // common seeds.
                            commonHitList.add(neighborHit);
                            commonHitList.add(hitToSeed.get(hit));

                            // Put the common seed list back into the set.
                            commonHits.put(hit, commonHitList);
                        }

                        // If the neighboring hit is a seed hit and the
                        // current hit has not been added to a cluster yet
                        // associate it with the neighboring seed and note
                        // that it has been clustered.
                        else {
                            hitToSeed.put(hit, neighborHit);
                        }
                    }
                }
            }
        } // End primary seed loop.

        // Performs second pass calculations for component hits.
        secondaryHitsLoop: for (CalorimeterHit secondaryHit : hitList) {
            // Look for hits that already have an associated seed/clustering.
            if (!hitToSeed.containsKey(secondaryHit)) {
                continue secondaryHitsLoop;
            }

            // Get the secondary hit's neighboring crystals.
            Set<Long> secondaryNeighbors = neighborMap.get(secondaryHit.getCellID());

            // Make a list to store the hits associated with the
            // neighboring crystals.
            List<CalorimeterHit> secondaryNeighborHits = new ArrayList<CalorimeterHit>();

            // Loop through the neighboring crystals.
            for (Long secondaryNeighbor : secondaryNeighbors) {
                // Get the hit associated with the neighboring crystal.
                CalorimeterHit secondaryNeighborHit = hitMap.get(secondaryNeighbor);

                // If the neighboring crystal exists and is not already
                // in a cluster, add it to the list of neighboring hits.
                if (secondaryNeighborHit != null && !hitToSeed.containsKey(secondaryNeighborHit)
                		&& hitList.contains(secondaryNeighborHit)) {
                    secondaryNeighborHits.add(secondaryNeighborHit);
                }
            }

            // Loop over the secondary neighbor hits.
            for (CalorimeterHit secondaryNeighborHit : secondaryNeighborHits) {
                // If the neighboring hit is of lower energy than the
                // current secondary hit, then associate the neighboring
                // hit with the current secondary hit's seed.
                if (!equalEnergies(secondaryNeighborHit, secondaryHit)) {
                    hitToSeed.put(secondaryNeighborHit, hitToSeed.get(secondaryHit));
                } else {
                    continue;
                }
            }
        } // End component hits loop.

        // Performs second pass calculations for common hits.
        commonHitsLoop: for (CalorimeterHit clusteredHit : hitToSeed.keySet()) {

            // Get the current clustered hit's neighboring crystals.
            Set<Long> clusteredNeighbors = neighborMap.get(clusteredHit.getCellID());

            // Store a list of all the clustered hits neighboring
            // crystals which themselves contain hits.
            List<CalorimeterHit> clusteredNeighborHits = new ArrayList<CalorimeterHit>();

            // Loop through the neighbors and see if they have hits.
            for (Long neighbor : clusteredNeighbors) {
                // Get the hit associated with the neighbor.
                CalorimeterHit clusteredNeighborHit = hitMap.get(neighbor);

                // If it exists, add it to the neighboring hit list.

                if (clusteredNeighborHit != null && hitToSeed.get(clusteredNeighborHit) != null
                		&& hitList.contains(clusteredNeighborHit)) {
                    clusteredNeighborHits.add(clusteredNeighborHit);
                }
            }

            // Get the seed hit associated with this clustered hit.
            CalorimeterHit clusteredHitSeed = hitToSeed.get(clusteredHit);

            // Loop over the clustered neighbor hits.
            for (CalorimeterHit clusteredNeighborHit : clusteredNeighborHits) {
                // Check to make sure that the clustered neighbor hit
                // is not already associated with the current clustered
                // hit's seed.

                if ((hitToSeed.get(clusteredNeighborHit) != clusteredHitSeed)) {
                    // Check for lowest energy hit and that comparison hit is not already common.
                    // If already common, this boundary is already accounted for.
                    if (!equalEnergies(clusteredHit, clusteredNeighborHit) && !commonHits.containsKey(clusteredNeighborHit)) {

                        // Check and see if a list of common seeds
                        // for this hit already exists or not.
                        List<CalorimeterHit> commonHitList = commonHits.get(clusteredHit);

                        // If it does not, make a new one.
                        if (commonHitList == null) {
                            commonHitList = new ArrayList<CalorimeterHit>();
                        }

                        // Add the neighbors to the seeds to set of
                        // common seeds.
                        commonHitList.add(clusteredHitSeed);
                        commonHitList.add(hitToSeed.get(clusteredNeighborHit));

                        // Put the common seed list back into the set.
                        commonHits.put(clusteredHit, commonHitList);

                    }
                }
            }
        } // End common hits loop.

        // Remove any common hits from the clustered hits collection.
        for (CalorimeterHit commonHit : commonHits.keySet()) {
            hitToSeed.remove(commonHit);
            hitList.remove(commonHit);
        }

        /*
         * All hits are sorted from above. The next part of the code is for building the output
         * cluster collections.
         */
        // Add all hits except for common hits
        for (CalorimeterHit ihit : hitList) {
            CalorimeterHit iseed = hitToSeed.get(ihit);
            BaseCluster icluster = seedToCluster.get(iseed);
            
            // Consider time cut-is this hit in same time window as seed?
            if (useTimeCut){
            	if(ihit.getCorrectedEnergy() < 0.1 && (Math.abs(ihit.getTime() - iseed.getTime()) < timeWindow))
            	{	
            		icluster.addHit(ihit);
            	}
            
            	else if (ihit.getCorrectedEnergy() > 0.1 && (Math.abs(ihit.getTime() - iseed.getTime()) < 2.0))
            	{
            		icluster.addHit(ihit);
            	}
            	
            } // end of using time cut
            else {icluster.addHit(ihit);}           
        }

        // Add common hits
        for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> commHit : commonHits.entrySet()) {
        	// Check that the common hit is in both time windows to their clusters
        	CalorimeterHit seedA = commHit.getValue().get(0);
            CalorimeterHit seedB = commHit.getValue().get(1);
        	
            boolean inTimeWithA = false;
            boolean inTimeWithB = false;
        	// In time window with seedA?
            if (commHit.getKey().getCorrectedEnergy() < 0.1){
            	if (Math.abs(commHit.getKey().getTime() - seedA.getTime()) < timeWindow){
            		inTimeWithA = true;
            	}	
            
            	// In time window with seedB?
            	if (Math.abs(commHit.getKey().getTime() - seedB.getTime()) < timeWindow){
            		inTimeWithB = true;
            	}
            }
            
            else if (commHit.getKey().getCorrectedEnergy() > 0.1)
            {
            	if (Math.abs(commHit.getKey().getTime() - seedA.getTime()) < 2.0){
            		inTimeWithA = true;
            	}	
            
            	// In time window with seedB?
            	if (Math.abs(commHit.getKey().getTime() - seedB.getTime()) < 2.0){
            		inTimeWithB = true;
            	}	
            }
                         	
            double eclusterA = seedToCluster.get(seedA).getEnergy();
            double eclusterB = seedToCluster.get(seedB).getEnergy();
            double fractionA = eclusterA / (eclusterA + eclusterB);
            double fractionB = eclusterB / (eclusterA + eclusterB);
            double hitcontributionA = commHit.getKey().getCorrectedEnergy() * fractionA;
            double hitcontributionB = commHit.getKey().getCorrectedEnergy() * fractionB;

            BaseCluster clusterA = seedToCluster.get(seedA);
            BaseCluster clusterB = seedToCluster.get(seedB);

            if (useTimeCut){
            	// Do this if the hit is in both cluster's windows
            	if (inTimeWithA && inTimeWithB){
            		clusterA.addHit(commHit.getKey(), hitcontributionA);
            		clusterB.addHit(commHit.getKey(), hitcontributionB);
            	}
            
            	//If the hit is only in 1 cluster's window, add the full contribution
            	else if(inTimeWithA ^ inTimeWithB){
            		if(inTimeWithA){
            			clusterA.addHit(commHit.getKey());
            		}
            		else{
            			clusterB.addHit(commHit.getKey());
            		}
            	}
            } // end of using time cut
            else{
            	clusterA.addHit(commHit.getKey(), hitcontributionA);
        		clusterB.addHit(commHit.getKey(), hitcontributionB);           	
            }
            
        }

        
        // Remove clusters that do not pass cluster threshold and add to rejectedHitList.
        for (int j = 0; j <= clusterList.size() - 1; j++) {
            BaseCluster checkcluster = (BaseCluster) clusterList.get(j);
            if (checkcluster.getEnergy() < clusterEnergyThreshold) {
                List<CalorimeterHit> clusterHits = checkcluster.getCalorimeterHits();
                for (CalorimeterHit nhit : clusterHits) {
                    rejectedHitList.add(nhit);
                }
                clusterList.remove(checkcluster);
                j--;
            } else {
                // Computer the position of the cluster and set it.
                calculatePosition(checkcluster);
                continue;
            }
        }
        //System.out.println("Number of clusters:"+clusterList.size());
        return clusterList;
    }

    /**
     * Handles pathological case where multiple neighboring crystals have EXACTLY the same energy.
     * @param hit
     * @param neighbor Neighbor to hit
     * @return boolean value of if the hit is a seed
     */
    private boolean equalEnergies(CalorimeterHit hit, CalorimeterHit neighbor) {
        boolean isSeed = true;

        int hix = hit.getIdentifierFieldValue("ix");
        int hiy = Math.abs(hit.getIdentifierFieldValue("iy"));
        int nix = neighbor.getIdentifierFieldValue("ix");
        int niy = Math.abs(neighbor.getIdentifierFieldValue("iy"));
        double hE = hit.getCorrectedEnergy();
        double nE = neighbor.getCorrectedEnergy();
        if (hE < nE) {
            isSeed = false;
        } else if ((hE == nE) && (hiy > niy)) {
            isSeed = false;
        } else if ((hE == nE) && (hiy == niy) && (hix < nix)) {
            isSeed = false;
        }
        return isSeed;
    }

    /**
     * Calculates the position of each cluster with no correction for particle type as documented in
     * HPS Note 2014-001.
     * @param cluster
     */
    private void calculatePosition(BaseCluster cluster) {
        final double w0 = 3.1;
        // calculated cluster x position
        double xCl = 0.0;
        // calculated cluster y position
        double yCl = 0.0;
        double eNumX = 0.0;
        double eNumY = 0.0;
        double eDen = 0.0;
        List<CalorimeterHit> clusterHits = cluster.getCalorimeterHits();
        for (CalorimeterHit hit : clusterHits) {
            // This block fills a map with crystal to center of face of crystal
            // Get the hit indices as a Point.
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            Point hitIndex = new Point(ix, iy);

            // Get the corrected position for this index pair.
            double[] position = correctedPositionMap.get(hitIndex);

            if (position == null) {
                addToMap(hit);
                position = correctedPositionMap.get(hitIndex);
            }

            eNumX += Math.max(0.0, (w0 + Math.log(hit.getCorrectedEnergy() / cluster.getEnergy()))) * (correctedPositionMap.get(hitIndex)[0] / 10.0);
            eNumY += Math.max(0.0, (w0 + Math.log(hit.getCorrectedEnergy() / cluster.getEnergy()))) * (correctedPositionMap.get(hitIndex)[1] / 10.0);
            eDen += Math.max(0.0, (w0 + Math.log(hit.getCorrectedEnergy() / cluster.getEnergy())));

        }// end for iteration through clusterHits

        xCl = eNumX / eDen;
        yCl = eNumY / eDen;

        double[] clusterPosition = new double[3];
        clusterPosition[0] = xCl * 10.0;// mm
        clusterPosition[1] = yCl * 10.0;// mm
        int ix = clusterHits.get(0).getIdentifierFieldValue("ix");
        int iy = clusterHits.get(0).getIdentifierFieldValue("iy");
        Point hitIndex = new Point(ix, iy);
        clusterPosition[2] = correctedPositionMap.get(hitIndex)[2];
        
        cluster.setPosition(clusterPosition);
        cluster.setNeedsPropertyCalculation(false);
    }

    /**
     * This constructs a mapping of a crystal to an x,y position at the face of the ecal. This fills
     * the correctedPositionMap with the position of a crystal's face from the index x,y of the
     * crystal.
     * @param hit
     */
    private void addToMap(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        Point hitIndex = new Point(ix, iy);
        // If the result is null, it hasn't been calculated yet.
        // Calculate the corrected position.
        IGeometryInfo geom = hit.getDetectorElement().getGeometry();
        double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()), (Hep3Vector) new BasicHep3Vector(0, 0, -1 * ((Trd) geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();

        // Store the result in the map.
        correctedPositionMap.put(hitIndex, pos);
    }

    public ClusterType getClusterType() {
        return ClusterType.RECON;
    }

}