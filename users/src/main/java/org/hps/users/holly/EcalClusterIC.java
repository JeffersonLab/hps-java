package org.hps.users.holly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 *
 *
 * @author Holly Szumila-Vance <hszumila@jlab.org>
 * @author Kyle McCarty <mccaky@gmail.com>
 *
 * @version $Id: EcalClusterer.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class EcalClusterIC extends Driver {

    HPSEcal3 ecal;
    String ecalCollectionName;
    String ecalName = "Ecal";
    String clusterCollectionName = "EcalClusters";
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    //Minimum energy that counts as hit
    double Emin = 0.0009;

    public EcalClusterIC() {
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

    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
    }

    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);

        // Cache ref to neighbor map.
        neighborMap = ecal.getNeighborMap();
    }

    public void process(EventHeader event) {

        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            
            for (CalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }
            
            System.out.println("Number of ECal hits: "+hitMap.size());
            
            // Put Cluster collection into event.
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            event.put(clusterCollectionName, createClusters(hitMap), HPSEcalCluster.class, flag);
        }
    }

    public List<HPSEcalCluster> createClusters(Map<Long, CalorimeterHit> map) {

        // New Cluster list to be added to event.
        List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();
             
        //Create a Calorimeter hit list in each event, then sort with highest energy first
        ArrayList<CalorimeterHit> chitList = new ArrayList<CalorimeterHit>(map.size());
        for(CalorimeterHit h : map.values()) {
        	if(h.getRawEnergy()>Emin){
        	chitList.add(h);}
        
        	Collections.sort(chitList, new EnergyComparator());}
        
        //New Seed list containing each local maximum energy hit
        List<CalorimeterHit> seedHits = new ArrayList<CalorimeterHit>();

      	//Create map to contain common hits for evaluation later, key is crystal and values are seed
      	Map<CalorimeterHit, CalorimeterHit> commonHits = new HashMap<CalorimeterHit, CalorimeterHit>();
      	
      	//Created map to contain seeds with listed hits, key is crystal, and value is seed
      	Map<CalorimeterHit, CalorimeterHit> clusterHits = new HashMap<CalorimeterHit, CalorimeterHit>();
      	
      	//Create map to contain the total energy of each cluster
      	Map<CalorimeterHit, Double> seedEnergy = new HashMap<CalorimeterHit,Double>();
      	
      	//Quick Map to access hits from cell IDs
      	Map<Long, CalorimeterHit> hitID = new HashMap<Long, CalorimeterHit>();
      	
        //Fill Map with cell ID and hit
      	for (CalorimeterHit hit : chitList){
      		hitID.put(hit.getCellID(), hit);
      	}

        for (CalorimeterHit hit : chitList) { 
            //Check seed? Check common hit? Add to cluster...
        	Set<Long> neighbors = neighborMap.get(hit.getCellID()); //obtains up to 8 neighboring cell ids
        	for(Long neighbor : neighbors){
        		if(hitID.containsKey(neighbor)){//check if this neighbor is in hit list 
        			CalorimeterHit adjacent = hitID.get(neighbor);
        			if (clusterHits.containsKey(adjacent)){//does current hit have neighbors that are cluster hits?
        				CalorimeterHit adjHit = adjacent;////get adjacent hit from neighbors
        				CalorimeterHit seed = clusterHits.get(adjHit);//seed value with adjHit
        				if(adjHit.getRawEnergy()>hit.getRawEnergy())//adjHit energy is > hit energy, add to cluster
        				{
        					clusterHits.put(hit, seed);
        				}
        				else{//hit energy is greater than adjHit energy, add to common hit
        					commonHits.put(hit, seed);
        					if(clusterHits.containsKey(hit)){//is this hit in clusterHits
        						CalorimeterHit prevSeed = clusterHits.get(hit);//get previous seed value from clusterHits map
        						clusterHits.remove(hit);
        						commonHits.put(hit,seed);//previous seed value
        					}
        					else{continue;}
        					}			
        				}
        			else if(!seedHits.contains(hit)){//check if seed already in list
        				seedHits.add(hit);//add to list of seeds
        				clusterHits.put(hit, hit);//add to list of cluster hits
        				continue;}
        			else {continue;}
        		}
        		else {continue;}
        	}
        }
        
        //Get energy of each cluster, excluding common hits
        for(CalorimeterHit iSeed : seedHits){
        seedEnergy.put(iSeed, 0.0);
        }
        //Putting total cluster energies excluding common hit energies into map with seed keys    
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry : clusterHits.entrySet()) {
        	CalorimeterHit eSeed = entry.getValue();
        	double eEnergy = seedEnergy.get(eSeed);
        	eEnergy += entry.getKey().getRawEnergy();
        	seedEnergy.remove(eSeed);
        	seedEnergy.put(eSeed, eEnergy);
        }
                   
        
        //Add back in energy from common hits to each cluster, as well as hit
        //energy fraction looks like common hit energy*(energy of cluster adding into)/(sum of energies of all clusters it belongs to)
        
        //Do some system.out for number of crystals in each cluster, energy of each cluster
        System.out.println("Number of clusters: "+seedHits.size());
        for (Map.Entry<CalorimeterHit, Double> output : seedEnergy.entrySet()) {
        	System.out.println("\t Cluster position = "+output.getKey().getCellID()+"\t Cluster energy = "+output.getValue());}

       
        //Clear all maps for next event iteration
        hitID.clear();
        clusterHits.clear();
        seedHits.clear();
        commonHits.clear();
        chitList.clear();
        seedEnergy.clear();
 
        return clusters;

    }
    

    private class EnergyComparator implements Comparator<CalorimeterHit>{

		@Override
		public int compare(CalorimeterHit o1, CalorimeterHit o2) {
			// TODO Auto-generated method stub
			double diff = o1.getRawEnergy()-o2.getRawEnergy();
			if(diff < 0) { return 1; }
			if(diff > 0) { return -1; } 
			else { return 0; }
		}		
    }   
}
