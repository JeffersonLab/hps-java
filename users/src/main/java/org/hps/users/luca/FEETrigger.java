package org.hps.users.luca;

import java.util.List;

import org.hps.readout.ecal.TriggerDriver;
import org.hps.recon.ecal.HPSEcalCluster;

import org.lcsim.event.EventHeader;

/**
 * Class <code>FEETrigger</code> represents a single-cluster trigger
 * that triggers off of clusters which exceed a certain energy threshold.
 * The trigger considers three regions, each which require a different
 * number of clusters to occur in said region before the trigger occurs.
 * 
 * @author Luca Colaneri
 */
public class FEETrigger extends TriggerDriver {
	// Store the LCIO cluster collection name.
	private String clusterCollectionName = "EcalClusters";
	
	// Store the cluster total energy trigger threshold.
	private double energyThreshold = 1.5;
	
	// Track the number of over-threshold clusters in each region.
	private int zone1Count = 0;
	private int zone2Count = 0;
	private int zone3Count = 0;
	
    // The number of cluster over threshold that must occur in a region
	// before a trigger occurs.
	private int zone1Prescaling = 1000;
	private int zone2Prescaling = 100;
	
	/**
	 * Sets the energy threshold required for a cluster to be counted.
	 * 
	 * @param energyThreshold - The energy threshold in GeV.
	 */
	public void setEnergyThreshold(int energyThreshold) {
		this.energyThreshold = energyThreshold;
	}
	
	/**
	 * Sets the number of events over threshold which must occur in the
	 * first region in order for a trigger to occur.
	 * 
	 * @param zone1Prescaling - The number of over-threshold clusters needed
	 * for a trigger.
	 */
	public void setZone1Prescaling(int zone1Prescaling) {
		this.zone1Prescaling = zone1Prescaling;
	}
	
	/**
	 * Sets the number of events over threshold which must occur in the
	 * second region in order for a trigger to occur.
	 * 
	 * @param zone2Prescaling - The number of over-threshold clusters needed
	 * for a trigger.
	 */
	public void setZone2Prescaling(int zone2Prescaling) {
		this.zone2Prescaling = zone2Prescaling;
	}
	
	/**
	 * Checks if any clusters exist over the set energy threshold and,
	 * if they do, increments the appropriate over-threshold count
	 * variable for the zone in which the cluster resides.
	 * 
	 * @param event - The event from which clusters should be extracted.
	 */
	@Override
	public void process(EventHeader event) {
		if(event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
			// Get the list of clusters from the event.
			List<HPSEcalCluster> clusterList = event.get(HPSEcalCluster.class, clusterCollectionName);
			
			// Loop over the clusters and check for any that pass the threshold.
			for(HPSEcalCluster cluster : clusterList) {
				// Check if the current cluster exceeds the energy
				// threshold. If it does not, continue to the next
				// cluster in the list.
				if(cluster.getEnergy() > energyThreshold) {
					// Get the x-index of the seed hit.
					int ix = cluster.getSeedHit().getIdentifierFieldValue("ix");
					
					// Determine in which region the cluster is located
					// and increment the counter for that region. Zones
					// are defined as:
					// Zone 1 is -13 < ix < -4 and 14 < ix < 21  MISTAKE!!! it's all reversed!! remember!!!
					// Zone 2 is -20 < ix < -14 and ix > 20
					// Zone 3 is -23 <= ix < -18
					if( ix > 18 || ix < -22) { zone3Count++; }
					if(ix < 19 && ix  > 12 )  { zone2Count++; }
					if((ix > 4 && ix < 13) || (ix > -23 && ix < -14)) { zone1Count++; }
				}
			}
		}
		
		// Run the superclass event processing.
		super.process(event);
	}
	
	/**
	 * Checks whether or not a trigger occurred.
	 * 
	 * @param event - The event on which to base the trigger decision.
	 * @return Returns <code>true</code> if a trigger occurred and <code>
	 * false</code> if a trigger did not.
	 */
	@Override
	protected boolean triggerDecision(EventHeader event) {
		// Check if the event has clusters. An event with no clusters
		// should never result in a trigger.
		if(event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
			// Check if any of the zone counts are high enough to trigger.
			return triggerTest();
		}
		
		// Events without clusters can not trigger.
		else { return false; }
	}
	
	/**
	 * Checks if any of the regional counts are sufficiently high to
	 * register a trigger.
	 * 
	 * @return Returns <code>true</code> if a region has enough clusters
	 * to trigger and <code>false</code> otherwise.
	 */
	private boolean triggerTest() {
		// Track whether a trigger occurred.
		boolean trigger = false;
		
		// If any clusters occur in zone 3, reset the count and note
		// that a trigger occurred.
		if(zone3Count > 0) {
			zone3Count = 0;
                         if(zone2Count==zone2Prescaling){zone2Count=0;}
                         if(zone1Count==zone1Prescaling){zone1Count=0;}
			trigger = true;
		}
		
		// If zone 2 has sufficient clusters (100 by default) to
		// trigger, reset its count and note that a trigger occurred.
		else if(zone2Count == zone2Prescaling) {
			zone2Count = 0;
                        if(zone3Count>0){zone3Count=0;}
                        if(zone1Count==zone1Prescaling){zone1Count=0;}
			trigger = true;
		}
		
		// If zone 3 has sufficient clusters (1000 by default) to
		// trigger, reset its count and note that a trigger occurred.
		else if(zone1Count == zone1Prescaling) {
			zone1Count = 0;
                        if(zone3Count>0){zone3Count=0;}
                        if(zone2Count==zone2Prescaling){zone2Count=0;}
			trigger = true;
		}
		
		// Return whether or not a trigger occurred.
		return trigger;
	}
}