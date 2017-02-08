package org.hps.users.holly;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.hps.users.holly.HPSEcalClusterIC;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.solids.Trd;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;


/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 * 
 * This clustering logic is based on that from the CLAS-Note-2005-001. 
 * This sorts hits from highest to lowest energy and build clusters around 
 * each local maximum/seed hit. Common hits are distributed between clusters 
 * when minimum between two clusters. There is a threshold cut for minimum
 * hit energy, minimum cluster energy, and minimum seed hit energy. There is 
 * also a timing threshold with respect to the seed hit. All of these parameters
 * are tunable and should be refined with more analysis. 
 * 
 * This adapts the current clustering algorithm for use in calculating cluster 
 * centroid energy and position reconstruction.
 */
public class EcalClusterICPosition extends Driver {
    // File writer to output cluster results.
    FileWriter writeHits;
    // LCIO collection name for calorimeter hits.
    String ecalCollectionName="EcalCalHits";
    // Name of the calorimeter detector object.
    String ecalName = "Ecal";
    // LCIO cluster collection name to which to write.
    String clusterCollectionName = "EcalClusters";
    // Collection name for rejected hits
    String rejectedHitName = "RejectedHits";
    // File path to which to write event display output.
    String outfile = "cluster-hit-IC.txt";
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    // Minimum energy threshold for hits; lower energy hits will be
    // excluded from clustering. Units in GeV.
    double hitEnergyThreshold = 0.0075;
    // Minimum energy threshold for seed hits; if seed hit is below
    // cluster is excluded from output. Units in GeV.
    double seedEnergyThreshold = 0.1;
    // Minimum energy threshold for cluster hits; if total cluster
    // energy is below, the cluster is excluded. Units in GeV.
    double clusterEnergyThreshold = 0.3;  
    // A Comparator that sorts CalorimeterHit objects from highest to
    // lowest energy.
    private static final EnergyComparator ENERGY_COMP = new EnergyComparator();
    // Track the event number for the purpose of outputting to event
    // display format.
    private int eventNum = -1;
    // Apply time cut to hits
    boolean timeCut = false;
    // Minimum time cut window range. Units in ns.
    double minTime = 0.0;
    // Maximum time cut window range. Units in ns.
    double timeWindow = 20.0;
    // Collection for ECal scoring plane
    String trackerCollectionName="TrackerHitsECal";
    
    public Random rNum = new Random();

    
    public void setTrackerCollectionName(String trackerCollectionName){
        this.trackerCollectionName = trackerCollectionName;
    }
    
       
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setRejectedHitName(String rejectedHitName){
        this.rejectedHitName = rejectedHitName;
    }
    
    /**
     * Minimum energy for a hit to be used in a cluster. Default of 0.0075 GeV
     *
     * @param hitEnergyThreshold
     */
    public void sethitEnergyThreshold(double hitEnergyThreshold) {
        this.hitEnergyThreshold = hitEnergyThreshold;
    }

    /**
     * Minimum energy for a seed hit. Default of 0.1 GeV
     *
     * @param seedEnergyThreshold
     */
    public void setseedEnergyThreshold(double seedEnergyThreshold) {
        this.seedEnergyThreshold = seedEnergyThreshold;
    }
    
    /**
     * Minimum energy for a cluster. Default of 0.3 GeV
     *
     * @param clusterEnergyThreshold
     */
    public void setclusterEnergyThreshold(double clusterEnergyThreshold) {
        this.clusterEnergyThreshold = clusterEnergyThreshold;
    }
    
    /**
     * Apply time cuts to hits. Defaults to false.
     *
     * @param timeCut
     */
    public void setTimeCut(boolean timeCut) {
        this.timeCut = timeCut;
    }

    /**
     * Minimum hit time, if timeCut is true. Default of 0 ns.
     *
     * @param minTime
     */
    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    /**
     * Width of time window, if timeCut is true. Default of 20 ns.
     *
     * @param timeWindow
     */
    public void setTimeWindow(double timeWindow) {
        this.timeWindow = timeWindow;
    }
    
    
  //trying to get mc particle list
    public ArrayList<MCParticle> mcList = new ArrayList<MCParticle>();
    
    //get the list of Ecal scoring Tracker hits
    public ArrayList<SimTrackerHit> trackHits = new ArrayList<SimTrackerHit>();
    public void addTrackHit(SimTrackerHit trHit){
        trackHits.add(trHit);
    }
    
    // Make a map for quick calculation of the x-y position of crystal face
    public Map<Point, Double[]> correctedPositionMap = new HashMap<Point, Double[]>();
    
    // Make mapping of hit to energy with pre-amplifier noise
    public Map<CalorimeterHit, Double> hitEnergyMap = new HashMap<CalorimeterHit, Double>();
    
    //attempt for mc particle list
    public void addMCGen(MCParticle genMC){
        mcList.add(genMC);
    }
    
    public void startOfData() {
        // Make sure that the calorimeter hit collection name is defined.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // Make sure the name of calorimeter detector is defined.
        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
        
        // Create a file writer and clear the output file, if it exists.
        try {
            writeHits = new FileWriter(outfile);
            writeHits.write("");
        }
        catch(IOException e) { }
    }

    public void detectorChanged(Detector detector) {
        // Get the calorimeter.
        HPSEcal3 ecal = (HPSEcal3) detector.getSubdetector(ecalName);
        
        // Store the map of neighbor crystals for the current calorimeter set-up.
        neighborMap = ecal.getNeighborMap();
    }

    public void process(EventHeader event) {
        // Make sure the current event contains calorimeter hits.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            
            // Get generated hits
            List<MCParticle> genPart = event.getMCParticles();
            for(MCParticle m : genPart){
                mcList.add(m);
            }
            
            List<SimTrackerHit> trHit = event.get(SimTrackerHit.class, trackerCollectionName);
            for (SimTrackerHit t : trHit){
                trackHits.add(t);
            }
            

            // Generate clusters from the calorimeter hits.
            //List<HPSEcalClusterIC> clusterList = null;
            try { createClusters(event); }
            catch(IOException e) { }
        }
    }

    public void createClusters(EventHeader event) throws IOException {
        
        // Create a list to store the event hits in.
        List<CalorimeterHit> hitList = new ArrayList<CalorimeterHit>();
        List<CalorimeterHit> baseList = event.get(CalorimeterHit.class, ecalCollectionName);
        for(CalorimeterHit r : baseList) {
            hitEnergyMap.put(r, (r.getCorrectedEnergy()+rNum.nextGaussian()*0.003));
            hitList.add(r);
        }
        
        // Create a list to store the newly created clusters in.
        ArrayList<HPSEcalClusterIC> clusterList = new ArrayList<HPSEcalClusterIC>();
        
        // Create a list to store the rejected hits in.
        ArrayList<CalorimeterHit> rejectedHitList = new ArrayList<CalorimeterHit>();
        
        // Sort the list of hits by energy.
        Collections.sort(hitList, ENERGY_COMP);
        
        // Filter the hit list of any hits that fail to pass the
        // designated threshold.
        filterLoop:
        for(int index = hitList.size() - 1; index >= 0; index--) {
            // If the hit is below threshold or outside of time window, kill it.
/*          if((hitList.get(index).getCorrectedEnergy() < hitEnergyThreshold)||
                    (timeCut && (hitList.get(index).getTime() < minTime || hitList.get(index).getTime() > (minTime + timeWindow)))) {
                rejectedHitList.add(hitList.get(index));
                hitList.remove(index);
            }*/
            if((hitEnergyMap.get(hitList.get(index))< hitEnergyThreshold)||
                    (timeCut && (hitList.get(index).getTime() < minTime || hitList.get(index).getTime() > (minTime + timeWindow)))) {
                rejectedHitList.add(hitList.get(index));
                hitList.remove(index);
            }
            
            
            
            
            
            // Since the hits are sorted by energy from highest to
            // lowest, any hit that is above threshold means that all
            // subsequent hits will also be above threshold. Continue through
            // list to check in time window. 
            else { continue; }
        }
        
        // Create a map to connect the cell ID of a calorimeter crystal
        // to the hit which occurred in that crystal.
        HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hitList) { hitMap.put(hit.getCellID(), hit); }
        
        // Map a crystal to a list of all clusters in which it is a member.
        Map<CalorimeterHit, List<CalorimeterHit>> commonHits = new HashMap<CalorimeterHit, List<CalorimeterHit>>();
        
        // Map a crystal to the seed of the cluster of which it is a member.
        HashMap<CalorimeterHit, CalorimeterHit> hitSeedMap = new HashMap<CalorimeterHit, CalorimeterHit>();
        
        // Set containing hits immediately around a seed hit.
        HashSet<CalorimeterHit> surrSeedSet = new HashSet<CalorimeterHit>();
        
        // Loop through all calorimeter hits to locate seeds and perform
        // first pass calculations for component and common hits.
        for (CalorimeterHit hit : hitList) {
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
                if(neighborHit != null) { neighborHits.add(neighborHit); }
            }
            
            // Track whether the current hit is a seed hit or not.
            boolean isSeed = true;
            
            // Loops through all the neighboring hits to determine if
            // the current hit is the local maximum within its set of
            // neighboring hits.
            seedHitLoop:
            for(CalorimeterHit neighbor : neighborHits) {
                if(!equalEnergies(hit, neighbor)) {
                    isSeed = false;
                    break seedHitLoop;
                }
           
            }
            
            // If this hit is a seed hit, just map it to itself.
            if (isSeed) { hitSeedMap.put(hit, hit); }
            
            // If this hit is not a seed hit, see if it should be
            // attached to any neighboring seed hits.
            else {
                // Sort through the list of neighboring hits.
                for (CalorimeterHit neighborHit : neighborHits) {
                    // Check whether the neighboring hit is a seed.
                    if(hitSeedMap.get(neighborHit) == neighborHit) {
                        // If the neighboring hit is a seed hit and the
                        // current hit has been associated with a cluster,
                        // then it is a common hit between its previous
                        // seed and the neighboring seed.
                        if (hitSeedMap.containsKey(hit)) {
                            // Check and see if a list of common seeds
                            // for this hit already exists or not.
                            List<CalorimeterHit> commonHitList = commonHits.get(hit);
                            
                            // If it does not, make a new one.
                            if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>(); }
                            
                            // Add the neighbors to the seeds to set of
                            // common seeds.
                            commonHitList.add(neighborHit);
                            commonHitList.add(hitSeedMap.get(hit));

                            
                            // Put the common seed list back into the set.
                            commonHits.put(hit, commonHitList);
                        }
                        
                        // If the neighboring hit is a seed hit and the
                        // current hit has not been added to a cluster yet
                        // associate it with the neighboring seed and note
                        // that it has been clustered.
                        else {
                            hitSeedMap.put(hit, neighborHit);
                            surrSeedSet.add(hit);
                        }
                    }
                }
            }
        } // End primary seed loop.
        
        // Performs second pass calculations for component hits.
        secondaryHitsLoop:
        for (CalorimeterHit secondaryHit : hitList) {
            // If the secondary hit is not associated with a seed, then
            // the rest of there is nothing further to be done.
            if(!hitSeedMap.containsKey(secondaryHit)) { continue secondaryHitsLoop; }
            
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
                if (secondaryNeighborHit != null && !hitSeedMap.containsKey(secondaryNeighborHit)) { //!clusteredHitSet.contains(secondaryNeighborHit)) {
                    secondaryNeighborHits.add(secondaryNeighborHit);
                }
            }
            
            // Loop over the secondary neighbor hits.
            for (CalorimeterHit secondaryNeighborHit : secondaryNeighborHits) {
                // If the neighboring hit is of lower energy than the
                // current secondary hit, then associate the neighboring
                // hit with the current secondary hit's seed.
                
                //  if (secondaryNeighborHit.getCorrectedEnergy() < secondaryHit.getCorrectedEnergy()) {
                if(!equalEnergies(secondaryNeighborHit, secondaryHit)) {
                    hitSeedMap.put(secondaryNeighborHit, hitSeedMap.get(secondaryHit));
                }
                else {continue;}
            }
        } // End component hits loop.

       
        
        
        
        // Performs second pass calculations for common hits.
        commonHitsLoop:
        for (CalorimeterHit clusteredHit : hitSeedMap.keySet()) {
            // Seed hits are never common hits and can be skipped.
            if(hitSeedMap.get(clusteredHit) == clusteredHit || surrSeedSet.contains(clusteredHit)) { continue commonHitsLoop; }
            
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
                if (clusteredNeighborHit != null) {
                    clusteredNeighborHits.add(clusteredNeighborHit);
                }
            }
            
            // Get the seed hit associated with this clustered hit.
            CalorimeterHit clusteredHitSeed = hitSeedMap.get(clusteredHit);
            
            // Loop over the clustered neighbor hits.
            for (CalorimeterHit clusteredNeighborHit : clusteredNeighborHits) {
                // Check to make sure that the clustered neighbor hit
                // is not already associated with the current clustered
                // hit's seed.
                
                if (hitSeedMap.get(clusteredNeighborHit) != clusteredHitSeed){

                    //if (clusteredHit.getCorrectedEnergy() < clusteredNeighborHit.getCorrectedEnergy()) {
                    if(!equalEnergies(clusteredHit, clusteredNeighborHit)){
                    // Check and see if a list of common seeds
                        // for this hit already exists or not.
                        List<CalorimeterHit> commonHitList = commonHits.get(clusteredHit);
                        
                        // If it does not, make a new one.
                        if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>(); }
                        
                        // Add the neighbors to the seeds to set of
                        // common seeds.
                        commonHitList.add(clusteredHitSeed);
                        commonHitList.add(hitSeedMap.get(clusteredNeighborHit));
                        
                        // Put the common seed list back into the set.
                        commonHits.put(clusteredHit, commonHitList);
                    }
                }
                
                
            }
        } // End common hits loop.

        
        // Remove any common hits from the clustered hits collection.
        for(CalorimeterHit commonHit : commonHits.keySet()) {
            hitSeedMap.remove(commonHit);
        }
        
        
        /*
         * All hits are sorted from above. The next part of the code is for calculating energies. Still 
         * needs implementation into new cluster collection so as to preserve shared hit energy 
         * distribution within clusters.
         */
                
        //Create map to contain the total energy of each cluster
        Map<CalorimeterHit, Double> seedEnergy = new HashMap<CalorimeterHit, Double>();
        
        // Get energy of each cluster, excluding common hits
        for (CalorimeterHit iSeed : hitList) {
            if(hitSeedMap.get(iSeed) == iSeed) {
                seedEnergy.put(iSeed, 0.0);
            }
        }
        
        //Putting total cluster energies excluding common hit energies into map with seed keys    
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry : hitSeedMap.entrySet()) {
            CalorimeterHit eSeed = entry.getValue();
            double eEnergy = seedEnergy.get(eSeed);
            eEnergy += hitEnergyMap.get(entry.getKey());
            seedEnergy.put(eSeed, eEnergy);
        }

        //Distribute common hit energies with clusters
        Map<CalorimeterHit, Double> seedEnergyTot = seedEnergy;
        
        for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry1 : commonHits.entrySet()) {
            CalorimeterHit commonCell = entry1.getKey();
            CalorimeterHit seedA = entry1.getValue().get(0);
            CalorimeterHit seedB = entry1.getValue().get(1);        
            double eFractionA = seedEnergy.get(seedA)/(seedEnergy.get(seedA)+seedEnergy.get(seedB));
            double eFractionB = seedEnergy.get(seedB)/(seedEnergy.get(seedA)+seedEnergy.get(seedB));
            double currEnergyA = seedEnergyTot.get(seedA);
            double currEnergyB = seedEnergyTot.get(seedB);
            currEnergyA += eFractionA * (hitEnergyMap.get(commonCell));
            currEnergyB += eFractionB * (hitEnergyMap.get(commonCell));

            seedEnergyTot.put(seedA, currEnergyA);
            seedEnergyTot.put(seedB, currEnergyB);
        }

        // Choose only the highest energy cluster
         List<CalorimeterHit> seedList = new ArrayList<CalorimeterHit>();
        for (Map.Entry<CalorimeterHit, Double> entry1 : seedEnergyTot.entrySet()) {
            seedList.add(entry1.getKey());
        }
        
        Collections.sort(seedList, new EnergyComparator());
        if(seedList.size()!=0){
        
        // This calculates the position of the highest energy cluster
        double xCl = 0.0;
        double yCl = 0.0;
        double eNumX = 0.0;
        double eNumY = 0.0;
        double eDen = 0.0;
        double crystalAngle = 0.0;//given in degrees
        double w0 = 3.1;
        
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry1 : hitSeedMap.entrySet()) {
            CalorimeterHit eSeed1 = entry1.getValue();
            if(seedList.get(0)==eSeed1){// Check for if belonging to highest seed only.

                
                // Method 3 calculation.
                // Calculates x-y centroid for each crystal face
//              IGeometryInfo geom = entry1.getKey().getDetectorElement().getGeometry();
//              double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()),(Hep3Vector)new BasicHep3Vector(0,0,-1*((Trd)geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();
               
               ///////////////////////////////
               // Get the hit indices as a Point.
                int ix = entry1.getKey().getIdentifierFieldValue("ix");
                int iy = entry1.getKey().getIdentifierFieldValue("iy");
                Point hitIndex = new Point(ix, iy);

                // Get the corrected position for this index pair.
                Double[] position = correctedPositionMap.get(hitIndex);

                // If the result is null, it hasn't been calculated yet.
                if(position == null) {
                        // Calculate the corrected position.
                        IGeometryInfo geom = entry1.getKey().getDetectorElement().getGeometry();
                        double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()),(Hep3Vector)new BasicHep3Vector(0,0,-1*((Trd)geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();
                        
                        // Convert the result to  a Double[] array.
                        position = new Double[3];
                        position[0] = pos[0];
                        position[1] = pos[1];
                        position[2] = pos[2];
                        
                        // Store the result in the map.
                        correctedPositionMap.put(hitIndex, position);
//                     writeHits.append("\t"+ix+"\t"+iy+"\t"+position[0]+"\t"+position[1]+"\n"); //write out slic crystal maps
                }
                ///////////////////////////////
                // Method 3:
                eNumX += Math.max(0.0,(w0+Math.log((hitEnergyMap.get(entry1.getKey()))
                        /seedEnergyTot.get(eSeed1))))*(correctedPositionMap.get(hitIndex)[0]/10.0);
                eNumY += Math.max(0.0,(w0+Math.log((hitEnergyMap.get(entry1.getKey()))
                        /seedEnergyTot.get(eSeed1))))*(correctedPositionMap.get(hitIndex)[1]/10.0);
                eDen += Math.max(0.0, w0+Math.log((hitEnergyMap.get(entry1.getKey()))/
                        seedEnergyTot.get(eSeed1)));
        
                // Method 1:
/*              eNumX += (hitEnergyMap.get(entry1.getKey()))*correctedPositionMap.get(hitIndex)[0]/10.0;
                eNumY += (hitEnergyMap.get(entry1.getKey()))*correctedPositionMap.get(hitIndex)[1]/10.0;
                eDen += hitEnergyMap.get(entry1.getKey());
*/              
                
                
                //Method 2:
/*              eNumX += Math.log10(1000*(hitEnergyMap.get(entry1.getKey())))*correctedPositionMap.get(hitIndex)[0]/10.0;
                eNumY += Math.log10(1000*(hitEnergyMap.get(entry1.getKey())))*correctedPositionMap.get(hitIndex)[1]/10.0;
                eDen += Math.log10(1000*(hitEnergyMap.get(entry1.getKey())));
*/          
                crystalAngle = 0.967826*(eSeed1.getIdentifierFieldValue("ix"));

            }

        }
        
       xCl = eNumX/eDen;
       yCl = eNumY/eDen;
        
        double ECl = seedEnergyTot.get(seedList.get(0));
        
        
        
        
        
        if(trackHits.size() != 0 ){
            
            // Calculates the final generated particle position
            double d0 = 139.3 - trackHits.get(0).getPositionVec().z()/10.0;
            double px = trackHits.get(0).getMomentum()[0];
            double py = trackHits.get(0).getMomentum()[1];
            double pz = trackHits.get(0).getMomentum()[2];
            double xpos = trackHits.get(0).getPosition()[0]/10.0;
            double ypos = trackHits.get(0).getPosition()[1]/10.0;
            
            double xGen = xpos + d0*px/pz;
            double yGen = ypos + d0*py/pz;
            
            boolean validNum = false;
            if((Math.abs(xCl)>0)&&(Math.abs(yCl)>0)&&(Math.abs(xGen)>0)&&(Math.abs(yGen)>0)){
                validNum=true;
            }
            
            
           
             //position fitting
//                  writeHits.append("\t"+seedList.get(0).getIdentifierFieldValue("ix")+"\t"+seedList.get(0).getIdentifierFieldValue("iy")+"\t"
//           +xCl+"\t"+yCl+"\t"+xF+"\t"+yF+"\t"+mcList.get(0).getEnergy()+"\t"+crystalAngle+"\t"+ECl+"\n");
              if(validNum==true){      
//                  writeHits.append("\t"+xCl+"\t"+yCl+"\t"+xGen+"\t"+yGen+"\t"+mcList.get(0).getEnergy()+"\t"+crystalAngle+"\t"+ECl+"\n");
                                        
              }    

                    }
                    
            
        }// end seedList.size != 0 
        
        int flag = 1 << LCIOConstants.CLBIT_HITS;
        event.put(clusterCollectionName, clusterList, HPSEcalClusterIC.class, flag);
        trackHits.clear();
        mcList.clear();
        hitEnergyMap.clear();
    }
        
        

    public void endOfData() {
        // Close the event display output writer.
        try { writeHits.close(); }
        catch (IOException e) { }
    }
    
  
    private static class EnergyComparator implements Comparator<CalorimeterHit> {
        /**
         * Compares the first hit with respect to the second. This
         * method will compare hits first by energy, and the spatially.
         * In the case of equal energy hits, the hit closest to the
         * beam gap and closest to the positron side of the detector
         * will be selected. If all of these conditions are true, the
         * hit with the positive y-index will be selected. Hits with
         * all four conditions matching are the same hit.
         * @param hit1 The hit to compare.
         * @param hit2 The hit with respect to which the first should
         * be compared.
         */
    public int compare(CalorimeterHit hit1, CalorimeterHit hit2) {
        // Hits are sorted on a hierarchy by three conditions. First,
        // the hits with the highest energy come first. Next, they
        // are ranked by vertical proximity to the beam gap, and
        // lastly, they are sorted by horizontal proximity to the
        // positron side of the detector.
        
        // Get the hit energies.
        double[] e = { hit1.getCorrectedEnergy(), hit2.getCorrectedEnergy() };
        
        // Perform the energy comparison. The higher energy hit
        // will be ordered first.
        if(e[0] < e[1]) { return 1; }
        else if(e[0] > e[1]) { return -1; }
        
        // If the hits are the same energy, we must perform the
        // spatial comparisons.
        else {
            // Get the position with respect to the beam gap.
            int[] iy = { Math.abs(hit1.getIdentifierFieldValue("iy")), Math.abs(hit2.getIdentifierFieldValue("iy")) };
            
            // The closest hit is first.
            if(iy[0] > iy[1]) { return -1; }
            else if(iy[0] < iy[1]) { return 1; }
            
            // Hits that are identical in vertical distance from
            // beam gap and energy are differentiated with distance
            // horizontally from the positron side of the detector.
            else {
                // Get the position from the positron side.
                int[] ix = { hit1.getIdentifierFieldValue("ix"), hit2.getIdentifierFieldValue("ix") };
                
                // The closest hit is first.
                if(ix[0] > ix[1]) { return 1; }
                else if(ix[0] < ix[1]) { return -1; }
                
                // If all of these checks are the same, compare
                // the raw value for iy. If these are identical,
                // then the two hits are the same. Otherwise, sort
                // the numerical value of iy. (This removes the
                // issue where hits (x, y) and (x, -y) can have
                // the same energy and be otherwise seen as the
                // same hit from the above checks.
                else { return Integer.compare(hit1.getIdentifierFieldValue("iy"), hit2.getIdentifierFieldValue("iy")); }
            }
        }
    }
}
    

    // Handles pathological case where multiple neighboring crystals have EXACTLY the same energy.
    private boolean equalEnergies(CalorimeterHit hit, CalorimeterHit neighbor){
        boolean isSeed = true;
        
        int hix = hit.getIdentifierFieldValue("ix");
        int hiy = Math.abs(hit.getIdentifierFieldValue("iy"));
        int nix = neighbor.getIdentifierFieldValue("ix");
        int niy = Math.abs(neighbor.getIdentifierFieldValue("iy"));
        double hE = hit.getCorrectedEnergy();
        double nE = neighbor.getCorrectedEnergy();
        if(hE < nE) {
            isSeed = false;
        }
        else if((hE == nE) && (hiy > niy)) {
            isSeed = false;
        }
        else if((hE == nE) && (hiy == niy) && (hix > nix)) {
            isSeed = false;
        }
        return isSeed;  
    }
    
    
 
    

    
    
    
    
}    

    
