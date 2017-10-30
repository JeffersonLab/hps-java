package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.TempOutputWriter;
import org.hps.recon.ecal.cluster.ClusterType;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;

public class GTPClusterReadoutDriver extends ReadoutDriver {
	private LinkedList<Map<Long, CalorimeterHit>> hitBuffer;
	private final TempOutputWriter writer = new TempOutputWriter("clusters_new.log");
	private final TempOutputWriter seedWriter = new TempOutputWriter("cluster_seeds_new.log");
	
	private NeighborMap neighborMap;
	private String inputCollectionName = "EcalCorrectedHits";
	private String outputCollectionName = "EcalClustersGTP";
	
	private int temporalWindow = 16;
	private double localTime = 2.0;
	private double localTimeDisplacement = 0;
	private double seedEnergyThreshold = 0.050;
	
	private boolean outputClusters = true;
	
	@Override
	public void endOfData() {
		writer.close();
		seedWriter.close();
	}
    
	@Override
	public void detectorChanged(Detector etector) {
		// Get the calorimeter data object.
		HPSEcal3 ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        if(ecal == null) {
            throw new IllegalStateException("Error: Calorimeter geometry data object not defined.");
        }
        
        // Get the calorimeter hit neighbor map.
        neighborMap = ecal.getNeighborMap();
        if(neighborMap == null) {
            throw new IllegalStateException("Error: Calorimeter hit neighbor map is not defined.");
        }
	}
	
	@Override
	public void process(EventHeader event) {
		StringBuffer outputBuffer = new StringBuffer();
		outputBuffer.append("Status for range: [" + (localTime - temporalWindow) + ", " + (localTime + temporalWindow + 4) + ") :: "
				+ ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + temporalWindow + 4.0) + "\n");
		int index = (temporalWindow / 4) * 2 + 1;
		double time = localTime - temporalWindow;
		outputBuffer.append("System Time: " + localTime + "\n");
		while(time < localTime + temporalWindow + 4) {
			Collection<CalorimeterHit> hits = ReadoutDataManager.getData(time, time + 4.0, inputCollectionName, CalorimeterHit.class);
			if(localTime == time) {
				outputBuffer.append("\tSeed Buffer " + index + " (" + time + " --> " + (time + 4.0) + ")\n");
			} else {
				outputBuffer.append("\tBuffer " + index + " (" + time + " --> " + (time + 4.0) + ")\n");
			}
        	for(CalorimeterHit hit : hits) {
        		outputBuffer.append(String.format("\t\t%f;%f;%d%n", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
        	}
        	index--;
        	time += 4.0;
		}
		
		
		
		
		System.out.println("New Clusterer -- Event " + event.getEventNumber() +" -- Current Time is " + localTime);
		
		// Check the data management driver to determine whether the
		// input collection is available or not.
		if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + temporalWindow + 4.0)) {
			return;
		}
		
		// Get the hits that occur during the present clock-cycle, as
		// well as the hits that occur in the verification window
		// both before and after the current clock-cycle.
		Collection<CalorimeterHit> seedCandidates = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, CalorimeterHit.class);
		Collection<CalorimeterHit> foreHits = ReadoutDataManager.getData(localTime - temporalWindow, localTime, inputCollectionName, CalorimeterHit.class);
		Collection<CalorimeterHit> postHits = ReadoutDataManager.getData(localTime + 4.0, localTime + temporalWindow + 4.0, inputCollectionName, CalorimeterHit.class);
		
		// Increment the local time.
		localTime += 4.0;
		
		//writer.write("Fore: [" + (localTime - temporalWindow) + ", " + localTime + ");   Seeds: [" + localTime + ", " + (localTime + 4.0) + ");   Aft: ["
		//		+ (localTime + 4.0) + ", " + (localTime + temporalWindow + 8.0) + ")");
		
		// DEBUG :: Print out the input hits.
		List<CalorimeterHit> allHits = new ArrayList<CalorimeterHit>(seedCandidates.size() + foreHits.size() + postHits.size());
		allHits.addAll(foreHits);
		allHits.addAll(seedCandidates);
		allHits.addAll(postHits);
		System.out.println("\tSaw Hits:");
		if(allHits.isEmpty()) { System.out.println("\t\tNone!"); }
		for(CalorimeterHit hit : allHits) {
			System.out.println("\t\tCalorimeter hit with energy " + hit.getRawEnergy() + " and time " + hit.getTime() + " on channel " + hit.getCellID() + ".");
		}
		
		// DEBUG :: Write the converted hits seen.
		/*
		for(CalorimeterHit hit : allHits) {
			writer.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
		}
		for(CalorimeterHit hit : seedCandidates) {
			seedWriter.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
		}
		*/
		
		System.out.println("\tSaw Seed Candidates:");
		if(seedCandidates.isEmpty()) { System.out.println("\t\tNone!"); }
		for(CalorimeterHit hit : seedCandidates) {
			System.out.println("\t\tCalorimeter hit with energy " + hit.getRawEnergy() + " and time " + hit.getTime() + " on channel " + hit.getCellID() + ".");
		}
		
		// Store newly created clusters.
		List<Cluster> gtpClusters = new ArrayList<Cluster>();
		
		// Iterate over all seed hit candidates.
		seedLoop:
		for(CalorimeterHit seedCandidate : seedCandidates) {
			// A seed candidate must meet a minimum energy cut to be
			// considered for clustering.
			if(seedCandidate.getRawEnergy() < seedEnergyThreshold) {
				continue seedLoop;
			}
			
			// Collect other hits that are adjacent to the seed hit
			// and may be a part of the cluster.
			List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
			
			// Iterate over all other hits in the clustering window
			// and check that the seed conditions are met for the
			// seed candidate. Note that all hits are properly within
			// the clustering time window by definition, so the time
			// condition is not checked explicitly.
			hitLoop:
			for(CalorimeterHit hit : allHits) {
				// If the hit is not adjacent to the seed hit, it can
				// be ignored.
				if(!neighborMap.get(seedCandidate.getCellID()).contains(hit.getCellID())) {
					continue hitLoop;
				}
				
				// A seed hit must have the highest energy in its
				// spatiotemporal window. If it is not, this is not a
				// valid seed hit.
				if(seedCandidate.getRawEnergy() < hit.getRawEnergy()) {
					continue seedLoop;
				}
				
				// Add the hit to the list of cluster hits.
				clusterHits.add(hit);
			}
			
			// If no adjacent hit was found that invalidates the seed
			// condition, then the seed candidate is valid and a
			// cluster should be formed.
			gtpClusters.add(createBasicCluster(seedCandidate, clusterHits));
		}
		
		// DEBUG :: Output the generated clusters.
		System.out.println("\tClusters:");
		if(gtpClusters.isEmpty()) { System.out.println("\t\tNone!"); }
		for(Cluster cluster : gtpClusters) {
			System.out.println("\t\tSaw cluster with energy " + cluster.getEnergy() + " at time " + TriggerModule.getClusterTime(cluster) + " with "
					+ TriggerModule.getClusterHitCount(cluster) + " hit on channel " + TriggerModule.getClusterSeedHit(cluster).getCellID() + ".");
		}
		
		// Pass the clusters to the data management driver.
		ReadoutDataManager.addData(outputCollectionName, gtpClusters, Cluster.class);
		
		if(!gtpClusters.isEmpty()) {
			// DEBUG :: Write the converted hits seen.
			//writer.write("Output");
			writer.write(outputBuffer.toString());
			writer.write("Saw " + gtpClusters.size() + " new clusters.");
			for(Cluster cluster : gtpClusters) {
				writer.write(String.format("%f;%f;%f;%d", cluster.getEnergy(), TriggerModule.getClusterTime(cluster), TriggerModule.getClusterHitCount(cluster),
						TriggerModule.getClusterSeedHit(cluster).getCellID()));
			}
			writer.write("\n\n");
		}
	}
	
	@Override
	public void startOfData() {
		// Instantiate the GTP cluster collection with the readout
		// data manager.
		localTimeDisplacement = temporalWindow + 4.0;
		addDependency(inputCollectionName);
		ReadoutDataManager.registerCollection(outputCollectionName, this, Cluster.class, (1 << LCIOConstants.CLBIT_HITS), outputClusters);
		
		// Initiate the hit buffer.
		hitBuffer = new LinkedList<Map<Long, CalorimeterHit>>();
		
		// Populate the event buffer with (2 * clusterWindow + 1)
		// empty events. These empty events represent the fact that
		// the first few events will not have any events in the past
		// portion of the buffer.
		int bufferSize = (2 * (temporalWindow / 4)) + 1;
		for (int i = 0; i < bufferSize; i++) {
			hitBuffer.add(new HashMap<Long, CalorimeterHit>(0));
		}
	}
	
	@Override
	protected double getTimeDisplacement() {
		return localTimeDisplacement;
	}
	
	public static final Cluster createBasicCluster(CalorimeterHit seedHit, List<CalorimeterHit> hits) {
        BaseCluster cluster = new BaseCluster();
        cluster.setType(ClusterType.GTP.getType());
        cluster.addHit(seedHit);
        cluster.setPosition(seedHit.getDetectorElement().getGeometry().getPosition().v());
        cluster.setNeedsPropertyCalculation(false);
    	cluster.addHits(hits);
        return cluster;
	}
    
    /**
     * Sets the size of the hit verification temporal window. Note
     * that this defines the size of the window in one direction, so
     * the full time window will be <code>(2 * clusterWindow)+
     * 1</code> clock-cycles in length. (i.e., it will be a length of
     * <code>clusterWindow</code> before the seed hit, a length of
     * <code>clusterWindow</code> after the seed hit, plus the cycle
     * that includes the seed hit.) Time length is in clock-cycles.
     * @param value - The number of clock-cycles around the hit in
     * one direction.
     */
    public void setClusterWindow(int value) {
        temporalWindow = value * 4;
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster. This is the seed energy lower bound
     * trigger cut and is in units of GeV.
     * @param value - The minimum cluster seed energy in GeV.
     */
    public void setSeedEnergyThreshold(double value) {
    	seedEnergyThreshold = value;
    }
    
    /**
     * Defines whether the output of this clusterer should be
     * persisted to LCIO or not. By default, this is false.
     * @param state - <code>true</code> indicates that clusters will
     * be persisted, and <code>false</code> that they will not.
     */
    public void setWriteClusterCollection(boolean state) {
    	outputClusters = state;
    }
}
