package org.hps.users.kmccarty;

import java.util.List;

import org.hps.readout.ecal.TriggerDriver;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

public class HodoscopeEcalOnlyTrigger extends TriggerDriver {
	// Declare steering parameters.
	private double clusterMinX = 90;                // mm
	private double clusterEnergyLowerBound = 0.120; // GeV
	private double clusterEnergyUpperBound = 1.995; // GeV
	private String clusterCollectionName = "EcalClustersGTP";
	
	// Track the trigger rate with no deadtime.
	private int events = 0;
	private int triggers = 0;
	
	@Override
	protected boolean triggerDecision(EventHeader event) {
		// Increment the total number of events.
		events++;
		
		// Get calorimeter clusters.
		List<Cluster> clusters = getCollection(event, clusterCollectionName, Cluster.class);
		
		// Iterate over the clusters and determine whether or not a
		for(Cluster cluster : clusters) {
			// Apply the spatial cut.
			if(TriggerModule.getClusterX(cluster) < clusterMinX) {
				continue;
			}
			
			// Apply the energy cuts.
			if(TriggerModule.getValueClusterTotalEnergy(cluster) > clusterEnergyUpperBound
					|| TriggerModule.getValueClusterTotalEnergy(cluster) < clusterEnergyLowerBound) {
				continue;
			}
			
			// If both cuts pass, indicate that a trigger should be
			// called.
			triggers++;
			return true;
		}
		
		// If no cluster meets the conditions, then there should not
		// be a trigger.
		return false;
	}
	
	@Override
	public void endOfData() {
		System.out.println("Total Events :: " + events);
		System.out.println("Triggers     :: " + triggers);
		System.out.printf("Trigger Rate :: %.3f kHz%n", (triggers / (2 * events * Math.pow(10, -6))));
	}
	
	public void setClusterCollectionName(String collection) {
		clusterCollectionName = collection;
	}
	
	public void setClusterEnergyUpperBound(double threshold) {
		clusterEnergyUpperBound = threshold;
	}
	
	public void setClusterEnergyLowerBound(double threshold) {
		clusterEnergyLowerBound = threshold;
	}
	
	public void setClusterMinX(double threshold) {
		clusterMinX = threshold;
	}
	
	private static final <E> List<E> getCollection(EventHeader event, String collectionName, Class<E> type) {
		List<E> collection;
		if(event.hasCollection(type, collectionName)) {
			collection = event.get(type, collectionName);
		} else {
			//System.err.println("Collection \"" + collectionName + "\" does not exist for data type \"" + type.getSimpleName() + "\".");
			collection = new java.util.ArrayList<E>(0);
		}
		return collection;
	}
}