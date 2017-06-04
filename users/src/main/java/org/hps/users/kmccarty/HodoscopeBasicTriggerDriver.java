package org.hps.users.kmccarty;

import java.util.Collection;
import java.util.List;

import org.hps.readout.ecal.TriggerDriver;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;

public class HodoscopeBasicTriggerDriver extends TriggerDriver {
	// Set the trigger conditions for the driver.
	protected double clusterXMin     = 90;    // mm
	protected double clusterEMin     = 0.120; // GeV
	protected double clusterEMax     = 1.995; // GeV
	protected double hodoEMin        = 0.100; // GeV
	protected int deltaT             = 12;    // ns
	
	// Store the collection names for SLCIO data objects.
	protected String clusterCollectionName = "EcalClustersGTP";
	protected String hodoHitCollectionName = "HodoscopeForeHits";
	
	// Define trigger data objects.
	private int events = 0;
	private Buffer<Cluster> clusterBuffer;
	private Buffer<SimTrackerHit> hodoscopeHitBuffer;
	
	private int TEMP_VAR_TRIGGERS = 0;
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	
	@Override
	public void startOfData() {
		// Create a buffer for the hodoscope hits. It must contain
		// enough hodoscope buffer entries to represent Δt ns in each
		// direction. Since each event represents 2 ns, this means
		// that the total number of buffers needed is (2 * Δt) / 2.
		// For instance, for Δt = 8:
		// [-8, -6] [-6, -4] [-4, -2] [-2,  0] [ 0,  2] [2,  4] [ 4,  6] [6,  8]
		hodoscopeHitBuffer = new Buffer<SimTrackerHit>(deltaT);
		
		// Create a buffer for calorimeter clusters. Only the buffer
		// entry that corresponds to "t = 0" is needed for clusters.
		// Mechanically, this corresponds to the buffer entry from
		// Δt/2 ns in the past (since these clusters must be compared
		// with "future" hodoscope hits). The total number of cluster
		// buffers needed is Δt/2.
		// For instance, for Δt = 8:
		// [ 0,  2] [2,  4] [ 4,  6] [6,  8]
		clusterBuffer = new Buffer<Cluster>(deltaT / 2);
	}
	
	@Override
	public void endOfData() {
		System.out.printf("Triggered! [%d / %d events = %.2f Hz]%n",
				TEMP_VAR_TRIGGERS, events, TEMP_VAR_TRIGGERS / (2.0 * events * Math.pow(10, -9)));
	}
	
	int clusterCount = 0;
	int goodClusterCount = 0;
	@Override
	protected boolean triggerDecision(EventHeader event) {
		/*
		clusterCount += getCollection(event, clusterCollectionName, Cluster.class).size();
		
		StringBuffer outputBuffer = new StringBuffer();
		outputBuffer.append("Particles:\n");
		for(SimTrackerHit particle : getCollection(event, "TrackerHitsECal", SimTrackerHit.class)) {
			double px = particle.getPosition()[0];
			double py = particle.getPosition()[1];
			double p = getMagnitude(particle.getMomentum());
			double t = particle.getTime();
			outputBuffer.append(String.format("\tParticle at (%6.1f, %6.1f) with %5.3f GeV at t = %f%n", px, py, p, t));
		}
		*/
		/*
		System.out.println("Ecal Hits:");
		for(CalorimeterHit hit : getCollection(event, "EcalHits", CalorimeterHit.class)) {
			double hx = TriggerModule.getHitX(hit);
			double hy = TriggerModule.getHitY(hit);
			double hE = hit.getRawEnergy();
			double ht = hit.getTime();
			System.out.printf("\tHit at (%6.1f, %6.1f) with %5.3f GeV at t = %f%n", hx, hy, hE, ht);
		}
		
		System.out.println("Raw Hits:");
		for(CalorimeterHit hit : getCollection(event, "EcalRawHits", CalorimeterHit.class)) {
			double hx = TriggerModule.getHitX(hit);
			double hy = TriggerModule.getHitY(hit);
			double hE = hit.getRawEnergy();
			double ht = hit.getTime();
			System.out.printf("\tHit at (%6.1f, %6.1f) with %5.3f GeV at t = %f%n", hx, hy, hE, ht);
		}
		
		System.out.println("Corrected Hits:");
		for(CalorimeterHit hit : getCollection(event, "EcalCorrectedHits", CalorimeterHit.class)) {
			double hx = TriggerModule.getHitX(hit);
			double hy = TriggerModule.getHitY(hit);
			double hE = hit.getRawEnergy();
			double ht = hit.getTime();
			System.out.printf("\tHit at (%6.1f, %6.1f) with %5.3f GeV at t = %f%n", hx, hy, hE, ht);
		}
		*/
		/*
		outputBuffer.append("Clusters:\n");
		for(Cluster cluster : getCollection(event, clusterCollectionName, Cluster.class)) {
			int cix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
			int ciy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
			double cx = cluster.getCalorimeterHits().get(0).getPosition()[0];
			double cy = cluster.getCalorimeterHits().get(0).getPosition()[1];
			double energy = TriggerModule.getValueClusterTotalEnergy(cluster);
			double t = TriggerModule.getClusterTime(cluster);
			outputBuffer.append(String.format("\tCluster at (%6.1f, %6.1f)/(%3d, %3d) with %5.3f GeV at t = %f%n",
					cx, cy, cix, ciy, energy, t));
			for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
				int hix = hit.getIdentifierFieldValue("ix");
				int hiy = hit.getIdentifierFieldValue("iy");
				double hx = hit.getPosition()[0];
				double hy = hit.getPosition()[1];
				double hE = hit.getRawEnergy();
				double ht = hit.getTime();
				outputBuffer.append(String.format("\t\tHit at (%6.1f, %6.1f)/(%3d, %3d) with %5.3f GeV at t = %f%n",
						hx, hy, hix, hiy, hE, ht));
			}
		}
		
		for(Cluster cluster : getCollection(event, clusterCollectionName, Cluster.class)) {
			double cx = cluster.getCalorimeterHits().get(0).getPosition()[0];
			double cy = cluster.getCalorimeterHits().get(0).getPosition()[1];
			for(SimTrackerHit particle : getCollection(event, "TrackerHitsECal", SimTrackerHit.class)) {
				double px = particle.getPosition()[0];
				double py = particle.getPosition()[1];
				double Δr = Math.sqrt(Math.pow(cx - px, 2) + Math.pow(cy - py, 2));
				if(Δr <= 30) {
					System.out.println(outputBuffer.toString());
					goodClusterCount++;
					break;
				}
			}
		}
		if(clusterCount != 0) {
			System.out.printf("%d / %d = %f%n", goodClusterCount, clusterCount, (1.0 * goodClusterCount) / clusterCount);
		}
		System.out.println();
		System.out.println();
		*/
		
		// Update the number of events that have been seen.
		events++;
		
		// Update the hodoscope hit buffer. This should hold all hits
		// on the hodoscope that occurred within ±Δt of the "current"
		// cluster time buffer. Because of the time offset induced by
		// the GTP clustering driver, the current event data goes to
		// a temporary buffer, while then oldest data from the buffer
		// is added to the hodoscope data buffer.
		hodoscopeHitBuffer.add(getCollection(event, hodoHitCollectionName, SimTrackerHit.class));
		
		// The "current" clusters are the clusters which occurred at
		// Δt/2 ns in the past. To ensure that these remain in memory
		// it is necessary to buffer them as well.
		clusterBuffer.add(getCollection(event, clusterCollectionName, Cluster.class));
		
		// Get the "current" clusters. The "current" clusters are the
		// clusters at the start of the cluster buffer.
		Collection<Cluster> clusters = clusterBuffer.getOldest();
		
		// If there are no clusters, a trigger can not occur.
		if(clusters.isEmpty()) {
			return false;
		}
		//System.out.println("\n\n\nEvent:");
		
		// Check that a calorimeter cluster exists that meets the
		// trigger conditions.
		boolean matchingClusterExists = false;
		for(Cluster cluster : clusters) {
			if(isValidCluster(cluster)) {
				matchingClusterExists = true;
				break;
			}
		}
		
		// Check that a hodoscope hit exists that meets the trigger
		// conditions.
		boolean matchingHodoHitExists = false;
		for(SimTrackerHit hodoHit : hodoscopeHitBuffer) {
			if(isValidHodoscopeHit(hodoHit)) {
				matchingHodoHitExists = true;
				break;
			}
		}
		
		// If there is a matching hit from both objects, produce a
		// trigger. Otherwise, continue.
		if(matchingClusterExists && matchingHodoHitExists) {
			TEMP_VAR_TRIGGERS++;
			//System.out.printf("Triggered! [%d / %d events = %.2f Hz]%n",
			//		TEMP_VAR_TRIGGERS, events, TEMP_VAR_TRIGGERS / (2.0 * events * Math.pow(10, -9)));
			HodoscopeTriggerTextWriter.trigger();
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean isValidCluster(Cluster cluster) {
		//System.out.println(getClusterString(cluster));
		
		// Perform the cluster x-position cut.
		//System.out.printf("\t%6.1f > %6.1f          ", getClusterX(cluster), clusterXMin);
		if(getClusterX(cluster) < clusterXMin) {
			//System.out.println("[" + ANSI_RED + "FAIL" + ANSI_RESET + "]");
			return false;
		}
		//System.out.println("[" + ANSI_GREEN + "PASS" + ANSI_RESET + "]");
		
		// Perform the cluster energy cut.
		//System.out.printf("\t%6.3f < %6.3f < %6.3f ", clusterEMin, TriggerModule.getValueClusterTotalEnergy(cluster), clusterEMax);
		if(TriggerModule.getValueClusterTotalEnergy(cluster) < clusterEMin
				|| TriggerModule.getValueClusterTotalEnergy(cluster) > clusterEMax) {
			//System.out.println("[" + ANSI_RED + "FAIL" + ANSI_RESET + "]");
			return false;
		}
		//System.out.println("[" + ANSI_GREEN + "PASS" + ANSI_RESET + "]");
		
		// If both cuts pass, the cluster is valid.
		return true;
	}
	
	protected boolean isValidHodoscopeHit(SimTrackerHit hodoHit) {
		// Perform the hodoscope minimum energy cut.
		if(getMagnitude(hodoHit.getMomentum()) < hodoEMin) {
			return false;
		}
		
		// If the cut passes, the hit is valid.
		return true;
	}
	
	protected static final <E> List<E> getCollection(EventHeader event, String collectionName, Class<E> type) {
		List<E> collection;
		if(event.hasCollection(type, collectionName)) {
			collection = event.get(type, collectionName);
		} else {
			//System.err.println("Collection \"" + collectionName + "\" does not exist for data type \"" + type.getSimpleName() + "\".");
			collection = new java.util.ArrayList<E>(0);
		}
		return collection;
	}
	
	protected static final double getMagnitude(double[] v) {
		double squareSum = 0;
		for(double vi : v) {
			squareSum += Math.pow(vi, 2);
		}
		return Math.sqrt(squareSum);
	}
	
	public void setClusterXMin(double xMin) {
		clusterXMin = xMin;
	}
	
	public void setClusterEMin(double eMin) {
		clusterEMin = eMin;
	}
	
	public void setClusterEMax(double eMax) {
		clusterEMax = eMax;
	}
	
	public void setHodoHitEMin(double eMin) {
		hodoEMin = eMin;
	}
	
	public void setTimeCoincidence(int deltaT) {
		// Δt must be divisible by 4.
		if(deltaT % 4 != 0) {
			throw new IllegalArgumentException("Time coincidence must be divisible by 4 ns!");
		}
		
		// Set the value.
		this.deltaT = deltaT;
	}
	
	public void setClusterCollectionName(String collection) {
		clusterCollectionName = collection;
	}
	
	public void setHodoscopeHitCollectionName(String collection) {
		hodoHitCollectionName = collection;
	}
	
	private static final double getClusterX(Cluster cluster) {
		return cluster.getCalorimeterHits().get(0).getPosition()[0];
	}
	
	private static final String getClusterString(Cluster cluster) {
		int cix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
		int ciy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
		double cx = cluster.getCalorimeterHits().get(0).getPosition()[0];
		double cy = cluster.getCalorimeterHits().get(0).getPosition()[1];
		double energy = TriggerModule.getValueClusterTotalEnergy(cluster);
		double t = TriggerModule.getClusterTime(cluster);
		return String.format("Cluster at (%6.1f, %6.1f)/(%3d, %3d) with %5.3f GeV at t = %f", cx, cy, cix, ciy, energy, t);
	}
}