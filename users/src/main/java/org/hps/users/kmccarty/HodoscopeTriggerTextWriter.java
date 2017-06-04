package org.hps.users.kmccarty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hps.readout.ecal.ClockSingleton;
import org.hps.record.triggerbank.TriggerModule;
import org.hps.users.kmccarty.HodoscopeHitDriver.TempHodoscopeHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class HodoscopeTriggerTextWriter extends Driver {
	// Buffer range around the "current" cluster.
	private int deltaT = 12;    // ns
	
	// Store the collection names for SLCIO data objects.
	private String clusterCollectionName = "EcalClustersGTP";
	private String hodoHitCollectionName = "TempHodoscopeHits";
	
	// Define trigger data objects.
	private Buffer<Cluster> clusterBuffer;
	private Buffer<TempHodoscopeHit> hodoscopeHitBuffer;
	
	// Store whether or not output should be written.
	private static boolean TRIGGERED = false;
	
	// Define the file writer.
	private File outputFile = null;
	private FileWriter writer = null;
	
	@Override
	public void startOfData() {
		// Create a buffer for the hodoscope hits. It must contain
		// enough hodoscope buffer entries to represent Δt ns in each
		// direction. Since each event represents 2 ns, this means
		// that the total number of buffers needed is (2 * Δt) / 2.
		// For instance, for Δt = 12:
		// [-8, -6) [-6, -4) [-4, -2) [-2,  0) [ 0,  2) [2,  4) [ 4,  6) [6,  8) [8, 10) [10, 12)
		hodoscopeHitBuffer = new Buffer<TempHodoscopeHit>(deltaT);
		
		// Create a buffer for calorimeter clusters. Only the buffer
		// entry that corresponds to "t = 0" is needed for clusters.
		// Mechanically, this corresponds to the buffer entry from
		// Δt/2 ns in the past (since these clusters must be compared
		// with "future" hodoscope hits). The total number of cluster
		// buffers needed is Δ t/ 2.
		// For instance, for Δt = 12:
		// [ 0,  2) [2,  4) [ 4,  6) [6,  8) [8, 10) [10, 12)
		clusterBuffer = new Buffer<Cluster>((deltaT / 2));
		
		// Instantiate the file writer.
		try {
			writer = new FileWriter(outputFile);
			writer.write("");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// Get the relevant object collections.
		List<Cluster> clusters = null;
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			clusters = event.get(Cluster.class, clusterCollectionName);
		} else { clusters = new ArrayList<Cluster>(0); }
		
		List<TempHodoscopeHit> hodoHits = null;
		if(event.hasCollection(TempHodoscopeHit.class, hodoHitCollectionName)) {
			hodoHits = event.get(TempHodoscopeHit.class, hodoHitCollectionName);
		} else { hodoHits = new ArrayList<TempHodoscopeHit>(0); }
		
		// Update the buffers.
		clusterBuffer.add(clusters);
		hodoscopeHitBuffer.add(hodoHits);
		
		// If a trigger has occurred, this event should be written.
		if(TRIGGERED) {
			// Write the event to the terminal.
			System.out.println("\n\n");
			System.out.println("Triggered on event " + (event.getEventNumber() - (deltaT / 2) + 1));
			System.out.println("Event Clusters:");
			for(Cluster cluster : clusterBuffer.getOldest()) {
				System.out.println("\t" + toString(cluster));
			}
			System.out.println("Event Hodoscope Hits:");
			for(TempHodoscopeHit hit : hodoscopeHitBuffer) {
				System.out.println("\t" + toString(hit));
			}
			String outputText = getOutputText(event, clusterBuffer.getOldest(), hodoscopeHitBuffer, deltaT);
			try {
				writer.append(outputText + "\n");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println(outputText);
			System.out.println("\n\n");
			
			// Reset the trigger bit.
			TRIGGERED = false;
		}
	}
	
	@Override
	public void endOfData() {
		try { writer.close(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public static final void trigger() {
		TRIGGERED = true;
	}
	
	private static final String getOutputText(Cluster cluster) {
		return String.format("%f %f %f %d %d %d", cluster.getEnergy(), cluster.getPosition()[0], cluster.getPosition()[1],
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"), cluster.getCalorimeterHits().size());
	}
	
	private static final String getOutputText(TempHodoscopeHit hit) {
		return String.format("%f %f %f", hit.position[0], hit.position[1], hit.energyDep);
	}
	
	private static final String getOutputText(EventHeader event, Collection<Cluster> clusters,
			Buffer<TempHodoscopeHit> hodoHits, int deltaT) {
		// Get the triggering event number.
		int eventNumber = event.getEventNumber() - (deltaT / 2) + 1;
		
		// Generate the cluster text.
		StringBuffer clusterBuffer = new StringBuffer(" " + clusters.size());
		for(Cluster cluster : clusters) {
			clusterBuffer.append(" " + getOutputText(cluster));
		}
		
		// Generate the hodoscope hit text.
		StringBuffer hodoscopeBuffer = new StringBuffer();
		Iterator<Collection<TempHodoscopeHit>> hodoIterator = hodoHits.listIterator();
		for(int i = 0; i < hodoHits.getBufferSize(); i++) {
			Collection<TempHodoscopeHit> hits = hodoIterator.next();
			hodoscopeBuffer.append(" " + hits.size());
			for(TempHodoscopeHit hit : hits) {
				hodoscopeBuffer.append(" " + getOutputText(hit));
			}
		}
		
		// Return the result.
		return eventNumber + clusterBuffer.toString() + hodoscopeBuffer.toString();
	}
	
	private final String toString(Cluster cluster) {
		int cix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
		int ciy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
		double cx = cluster.getCalorimeterHits().get(0).getPosition()[0];
		double cy = cluster.getCalorimeterHits().get(0).getPosition()[1];
		double energy = TriggerModule.getValueClusterTotalEnergy(cluster);
		//double t = TriggerModule.getClusterTime(cluster);
		double t = ClockSingleton.getTime() - deltaT;
		return String.format("Cluster at r = (%6.1f, %6.1f) and i = (%3d, %3d) with %5.3f GeV at t = %.0f", cx, cy, cix, ciy, energy, t);
	}
	
	private static final String toString(TempHodoscopeHit hit) {
		return String.format("Hit at r = (%6.1f, %6.1f) and i = (%3d, %3d) with %8.3f MeV at t = %.0f",
				hit.position[0], hit.position[1], hit.index.x, hit.index.y, hit.energyDep * 1000, hit.time);
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
	
	public void setOutputFilePath(String filePath) {
		outputFile = new File(filePath);
		if(!outputFile.exists()) {
			try { outputFile.createNewFile(); }
			catch (IOException e) {
				System.err.println("Error: Could not create specified output file at path \"" + filePath + "\"");
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			if(!outputFile.canWrite()) {
				System.err.println("Error: Can not write to specified output file at path \"" + filePath + "\"");
				throw new RuntimeException();
			}
		}
	}
}