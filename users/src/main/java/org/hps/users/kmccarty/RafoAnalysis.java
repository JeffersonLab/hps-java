package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class RafoAnalysis extends Driver {
	private String clusterCollectionName = "EcalClusters";
	private String particleCollectionName = "FinalStateParticles";
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D t0TimeCoincidenceAll          = aida.histogram1D("Tier 0/Time Coincidence",                    45, 0.0, 15.0);
	private IHistogram1D t0TimeCoincidenceFiducial     = aida.histogram1D("Tier 0/Time Coincidence (Fiducial Region)",  45, 0.0, 15.0);
	private IHistogram1D t0EnergySumAll                = aida.histogram1D("Tier 0/Energy Sum",                         110, 0.0,  1.1);
	private IHistogram1D t0EnergySumFiducial           = aida.histogram1D("Tier 0/Energy Sum (Fiducial Region)",       110, 0.0,  1.1);
	private IHistogram2D t0EnergySum2DAll              = aida.histogram2D("Tier 0/Top Cluster Energy vs. Bottom Cluster Energy",                   55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t0EnergySum2DFiducial         = aida.histogram2D("Tier 0/Top Cluster Energy vs. Bottom Cluster Energy (Fiducial Region)", 55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t0SumCoplanarityAll           = aida.histogram2D("Tier 0/Hardware Coplanarity vs. Energy Sum",                            55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t0SumCoplanarityFiducial      = aida.histogram2D("Tier 0/Hardware Coplanarity vs. Energy Sum (Fiducial Region)",          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t0SumCoplanarityCalcAll       = aida.histogram2D("Tier 0/Calculated Coplanarity vs. Energy Sum",                          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t0SumCoplanarityCalcFiducial  = aida.histogram2D("Tier 0/Calculated Coplanarity vs. Energy Sum (Fiducial Region)",        55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t0TimeEnergyAll               = aida.histogram2D("Tier 0/Cluster Time vs. Cluster Energy",                                55, 0, 1.1,  25, 0, 100);
	private IHistogram2D t0TimeEnergyFiducial          = aida.histogram2D("Tier 0/Cluster Time vs. Cluster Energy (Fiducial Region)",              55, 0, 1.1,  25, 0, 100);
	
	private IHistogram1D t1TimeCoincidenceAll          = aida.histogram1D("Tier 1/Time Coincidence",                    45, 0.0, 15.0);
	private IHistogram1D t1TimeCoincidenceFiducial     = aida.histogram1D("Tier 1/Time Coincidence (Fiducial Region)",  45, 0.0, 15.0);
	private IHistogram1D t1EnergySumAll                = aida.histogram1D("Tier 1/Energy Sum",                         110, 0.0,  1.1);
	private IHistogram1D t1EnergySumFiducial           = aida.histogram1D("Tier 1/Energy Sum (Fiducial Region)",       110, 0.0,  1.1);
	private IHistogram2D t1EnergySum2DAll              = aida.histogram2D("Tier 1/Top Cluster Energy vs. Bottom Cluster Energy",                   55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t1EnergySum2DFiducial         = aida.histogram2D("Tier 1/Top Cluster Energy vs. Bottom Cluster Energy (Fiducial Region)", 55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t1SumCoplanarityAll           = aida.histogram2D("Tier 1/Hardware Coplanarity vs. Energy Sum",                            55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t1SumCoplanarityFiducial      = aida.histogram2D("Tier 1/Hardware Coplanarity vs. Energy Sum (Fiducial Region)",          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t1SumCoplanarityCalcAll       = aida.histogram2D("Tier 1/Calculated Coplanarity vs. Energy Sum",                          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t1SumCoplanarityCalcFiducial  = aida.histogram2D("Tier 1/Calculated Coplanarity vs. Energy Sum (Fiducial Region)",        55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t1TimeEnergyAll               = aida.histogram2D("Tier 1/Cluster Time vs. Cluster Energy",                                55, 0, 1.1,  25, 0, 100);
	private IHistogram2D t1TimeEnergyFiducial          = aida.histogram2D("Tier 1/Cluster Time vs. Cluster Energy (Fiducial Region)",              55, 0, 1.1,  25, 0, 100);

	private IHistogram1D t2TimeCoincidenceAll          = aida.histogram1D("Tier 1/Time Coincidence",                    45, 0.0, 15.0);
	private IHistogram1D t2TimeCoincidenceFiducial     = aida.histogram1D("Tier 1/Time Coincidence (Fiducial Region)",  45, 0.0, 15.0);
	private IHistogram1D t2EnergySumAll                = aida.histogram1D("Tier 1/Energy Sum",                         110, 0.0,  1.1);
	private IHistogram1D t2EnergySumFiducial           = aida.histogram1D("Tier 1/Energy Sum (Fiducial Region)",       110, 0.0,  1.1);
	private IHistogram2D t2EnergySum2DAll              = aida.histogram2D("Tier 1/Top Cluster Energy vs. Bottom Cluster Energy",                   55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t2EnergySum2DFiducial         = aida.histogram2D("Tier 1/Top Cluster Energy vs. Bottom Cluster Energy (Fiducial Region)", 55, 0, 1.1, 55, 0, 1.1);
	private IHistogram2D t2SumCoplanarityAll           = aida.histogram2D("Tier 1/Hardware Coplanarity vs. Energy Sum",                            55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t2SumCoplanarityFiducial      = aida.histogram2D("Tier 1/Hardware Coplanarity vs. Energy Sum (Fiducial Region)",          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t2SumCoplanarityCalcAll       = aida.histogram2D("Tier 1/Calculated Coplanarity vs. Energy Sum",                          55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t2SumCoplanarityCalcFiducial  = aida.histogram2D("Tier 1/Calculated Coplanarity vs. Energy Sum (Fiducial Region)",        55, 0, 1.1, 165, 0, 230);
	private IHistogram2D t2TimeEnergyAll               = aida.histogram2D("Tier 1/Cluster Time vs. Cluster Energy",                                55, 0, 1.1,  25, 0, 100);
	private IHistogram2D t2TimeEnergyFiducial          = aida.histogram2D("Tier 1/Cluster Time vs. Cluster Energy (Fiducial Region)",              55, 0, 1.1,  25, 0, 100);
	
	private int t0Events = 0;
	private int t1Events = 0;
	private int t2Events = 0;
	
	@Override
	public void endOfData() {
		System.out.printf("Tier 0 Events: %d%n", t0Events);
		System.out.printf("Tier 1 Events: %d%n", t1Events);
		System.out.printf("Tier 2 Events: %d%n", t2Events);
	}
	
	@Override
	public void process(EventHeader event) {
		// Check whether the SVT was active in this event.
		final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
		boolean svtGood = true;
        for(int i = 0; i < flagNames.length; i++) {
            int[] flag = event.getIntegerParameters().get(flagNames[i]);
            if(flag == null || flag[0] == 0) {
                svtGood = false;
            }
        }
		
        // If the SVT is not properly running, skip the event.
        if(!svtGood) { return; }
        
		// Get the list of particles, if it exists.
		List<ReconstructedParticle> trackList = null;
		if(event.hasCollection(ReconstructedParticle.class, particleCollectionName)) {
			trackList = event.get(ReconstructedParticle.class, particleCollectionName);
		}
		
		// Get the list of clusters, if it exists.
		List<Cluster> clusterList = null;
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			clusterList = event.get(Cluster.class, clusterCollectionName);
		}
		
		// Make sure that the cluster and track lists both exist.
		if(clusterList == null || trackList == null) {
			return;
		}
		
		// Perform tier 1 analysis. This requires that there be at
		// least one top/bottom cluster pair with a time difference
		// of less then 4 ns.
		double t1TimeThreshold = 4;
		
		// Get a list of cluster pairs.
		List<Cluster[]> pairList = getClusterPairs(clusterList);
		
		// Iterate over the cluster pairs.
		boolean t1Passed = false;
		t1ClusterLoop:
		for(Cluster[] pair : pairList) {
			// Check that the time difference for the cluster pair
			// meets the time cut.
			if(TriggerModule.getValueTimeCoincidence(pair) <= t1TimeThreshold) {
				// Note that the tier 1 analysis condition passed.
				t1Passed = true;
				
				// Break from the loop.
				break t1ClusterLoop;
			}
		}
		
		// Perform the additional checks for tier 2 analysis. This
		// requires that there be at least one top/bottom track pair
		// and that one track be positive and the other be negative.
		
		// Get a list of top and bottom track pairs.
		List<ReconstructedParticle[]> trackPairList = getTrackPairs(trackList);
		
		// Check that at least one top/bottom track has one negative and
		// one positive track.
		boolean t2Passed = false;
		t2TrackLoop:
		for(ReconstructedParticle[] pair : trackPairList) {
			if((pair[0].getCharge() > 0 && pair[1].getCharge() < 0)
					|| (pair[0].getCharge() < 0 && pair[1].getCharge() > 0)) {
				t2Passed = true;
				break t2TrackLoop;
			}
		}
		
		// Populate the tier 0 analysis plot.
		if(true) {
			// Increment the number of tier 1 events found.
			t0Events++;
			
			// Track which clusters have already been added to the
			// singles plot so that there are no repeats.
			Set<Cluster> plotSet = new HashSet<Cluster>(clusterList.size());
			Set<Cluster> plotFiducial = new HashSet<Cluster>(clusterList.size());
			
			for(Cluster[] pair : pairList) {
				// Fill the all pairs plots.
				double pairEnergy = pair[0].getEnergy() + pair[1].getEnergy();
				t0EnergySumAll.fill(pairEnergy);
				t0EnergySum2DAll.fill(pair[1].getEnergy(), pair[0].getEnergy());
				t0TimeCoincidenceAll.fill(TriggerModule.getValueTimeCoincidence(pair));
				t0SumCoplanarityCalcAll.fill(pairEnergy, getCalculatedCoplanarity(pair));
				t0SumCoplanarityAll.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				
				// Fill the singles plots.
				if(!plotSet.contains(pair[0])) {
					plotSet.add(pair[0]);
					t0TimeEnergyAll.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotSet.contains(pair[1])) {
					plotSet.add(pair[1]);
					t0TimeEnergyAll.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
				
				// Fill the fiducial plots if appropriate.
				if(inFiducialRegion(pair[0]) && inFiducialRegion(pair[1])) {
					t0EnergySumFiducial.fill(pairEnergy);
					t0EnergySum2DFiducial.fill(pair[1].getEnergy(), pair[0].getEnergy());
					t0TimeCoincidenceFiducial.fill(TriggerModule.getValueTimeCoincidence(pair));
					t0SumCoplanarityCalcFiducial.fill(pairEnergy, getCalculatedCoplanarity(pair));
					t0SumCoplanarityFiducial.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				}
				
				// Fill the singles fiducial plots if appropriate.
				if(!plotFiducial.contains(pair[0]) && inFiducialRegion(pair[0])) {
					plotFiducial.add(pair[0]);
					t0TimeEnergyFiducial.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotFiducial.contains(pair[1]) && inFiducialRegion(pair[1])) {
					plotFiducial.add(pair[1]);
					t0TimeEnergyFiducial.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
			}
		}
		
		// Populate the tier 1 analysis plots, if the conditions were met.
		if(t1Passed) {
			// Increment the number of tier 1 events found.
			t1Events++;
			
			// Track which clusters have already been added to the
			// singles plot so that there are no repeats.
			Set<Cluster> plotSet = new HashSet<Cluster>(clusterList.size());
			Set<Cluster> plotFiducial = new HashSet<Cluster>(clusterList.size());
			
			for(Cluster[] pair : pairList) {
				// Fill the all pairs plots.
				double pairEnergy = pair[0].getEnergy() + pair[1].getEnergy();
				t1EnergySumAll.fill(pairEnergy);
				t1EnergySum2DAll.fill(pair[1].getEnergy(), pair[0].getEnergy());
				t1TimeCoincidenceAll.fill(TriggerModule.getValueTimeCoincidence(pair));
				t1SumCoplanarityCalcAll.fill(pairEnergy, getCalculatedCoplanarity(pair));
				t1SumCoplanarityAll.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				
				// Fill the singles plots.
				if(!plotSet.contains(pair[0])) {
					plotSet.add(pair[0]);
					t1TimeEnergyAll.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotSet.contains(pair[1])) {
					plotSet.add(pair[1]);
					t1TimeEnergyAll.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
				
				// Fill the fiducial plots if appropriate.
				if(inFiducialRegion(pair[0]) && inFiducialRegion(pair[1])) {
					t1EnergySumFiducial.fill(pairEnergy);
					t1EnergySum2DFiducial.fill(pair[1].getEnergy(), pair[0].getEnergy());
					t1TimeCoincidenceFiducial.fill(TriggerModule.getValueTimeCoincidence(pair));
					t1SumCoplanarityCalcFiducial.fill(pairEnergy, getCalculatedCoplanarity(pair));
					t1SumCoplanarityFiducial.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				}
				
				// Fill the singles fiducial plots if appropriate.
				if(!plotFiducial.contains(pair[0]) && inFiducialRegion(pair[0])) {
					plotFiducial.add(pair[0]);
					t1TimeEnergyFiducial.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotFiducial.contains(pair[1]) && inFiducialRegion(pair[1])) {
					plotFiducial.add(pair[1]);
					t1TimeEnergyFiducial.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
			}
		}
		
		// Populate the tier 2 analysis plots, if the conditions were met.
		if(t2Passed) {
			// Increment the number of tier 2 events found.
			t2Events++;
			
			// Track which clusters have already been added to the
			// singles plot so that there are no repeats.
			Set<Cluster> plotSet = new HashSet<Cluster>(clusterList.size());
			Set<Cluster> plotFiducial = new HashSet<Cluster>(clusterList.size());
			
			for(Cluster[] pair : pairList) {
				// Fill the all pairs plots.
				double pairEnergy = pair[0].getEnergy() + pair[1].getEnergy();
				t2EnergySumAll.fill(pairEnergy);
				t2EnergySum2DAll.fill(pair[1].getEnergy(), pair[0].getEnergy());
				t2TimeCoincidenceAll.fill(TriggerModule.getValueTimeCoincidence(pair));
				t2SumCoplanarityCalcAll.fill(pairEnergy, getCalculatedCoplanarity(pair));
				t2SumCoplanarityAll.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				
				// Fill the singles plots.
				if(!plotSet.contains(pair[0])) {
					plotSet.add(pair[0]);
					t2TimeEnergyAll.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotSet.contains(pair[1])) {
					plotSet.add(pair[1]);
					t2TimeEnergyAll.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
				
				// Fill the fiducial plots if appropriate.
				if(inFiducialRegion(pair[0]) && inFiducialRegion(pair[1])) {
					t2EnergySumFiducial.fill(pairEnergy);
					t2EnergySum2DFiducial.fill(pair[1].getEnergy(), pair[0].getEnergy());
					t2TimeCoincidenceFiducial.fill(TriggerModule.getValueTimeCoincidence(pair));
					t2SumCoplanarityCalcFiducial.fill(pairEnergy, getCalculatedCoplanarity(pair));
					t2SumCoplanarityFiducial.fill(pairEnergy, TriggerModule.getValueCoplanarity(pair));
				}
				
				// Fill the singles fiducial plots if appropriate.
				if(!plotFiducial.contains(pair[0]) && inFiducialRegion(pair[0])) {
					plotFiducial.add(pair[0]);
					t2TimeEnergyFiducial.fill(pair[0].getEnergy(), TriggerModule.getClusterTime(pair[0]));
				} if(!plotFiducial.contains(pair[1]) && inFiducialRegion(pair[1])) {
					plotFiducial.add(pair[1]);
					t2TimeEnergyFiducial.fill(pair[1].getEnergy(), TriggerModule.getClusterTime(pair[1]));
				}
			}
		}
	}
	
	private static final double getCalculatedCoplanarity(Cluster[] pair) {
		// Define the x- and y-coordinates of the clusters as well as
		// calorimeter center.
		final double ORIGIN_X = 42.52;
		double x[] = { pair[0].getPosition()[0], pair[1].getPosition()[0] };
		double y[] = { pair[0].getPosition()[1], pair[1].getPosition()[1] };
		
        // Get the cluster angles.
        double[] clusterAngle = new double[2];
        for(int i = 0; i < 2; i++) {
        	clusterAngle[i] = Math.atan2(y[i], x[i] - ORIGIN_X) * 180 / Math.PI;
        	if(clusterAngle[i] <= 0) { clusterAngle[i] += 360; }
        }
        
        // Calculate the coplanarity cut value.
        double clusterDiff = clusterAngle[0] - clusterAngle[1];
        return clusterDiff > 0 ? clusterDiff : clusterDiff + 360;
	}
	
	private static final boolean inFiducialRegion(Cluster cluster) {
		// Get the x and y indices for the cluster.
		int ix   = TriggerModule.getClusterXIndex(cluster);
		int absx = Math.abs(TriggerModule.getClusterXIndex(cluster));
		int absy = Math.abs(TriggerModule.getClusterYIndex(cluster));
		
		// Check if the cluster is on the top or the bottom of the
		// calorimeter, as defined by |y| == 5. This is an edge cluster
		// and is not in the fiducial region.
		if(absy == 5) {
			return false;
		}
		
		// Check if the cluster is on the extreme left or right side
		// of the calorimeter, as defined by |x| == 23. This is also
		// and edge cluster is not in the fiducial region.
		if(absx == 23) {
			return false;
		}
		
		// Check if the cluster is along the beam gap, as defined by
		// |y| == 1. This is an internal edge cluster and is not in the
		// fiducial region.
		if(absy == 1) {
			return false;
		}
		
		// Lastly, check if the cluster falls along the beam hole, as
		// defined by clusters with -11 <= x <= -1 and |y| == 2. This
		// is not the fiducial region.
		if(absy == 2 && ix <= -1 && ix >= -11) {
			return false;
		}
		
		// If all checks fail, the cluster is in the fiducial region.
		return true;
	}
	
	private static final List<ReconstructedParticle[]> getTrackPairs(List<ReconstructedParticle> tracks) {
		// Separate the tracks into top and bottom tracks.
		List<ReconstructedParticle> topList = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> botList = new ArrayList<ReconstructedParticle>();
		for(ReconstructedParticle track : tracks) {
			if(track.getMomentum().y() > 0.0) {
				topList.add(track);
			} else {
				botList.add(track);
			}
		}
		
		// Form all permutations of top and bottom tracks.
		List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
		for(ReconstructedParticle topTrack : topList) {
			for(ReconstructedParticle botTrack : botList) {
				pairList.add(new ReconstructedParticle[] { topTrack, botTrack });
			}
		}
		
		// Return the resulting cluster pairs.
		return pairList;
	}
	
	private static final List<Cluster[]> getClusterPairs(List<Cluster> clusters) {
		// Separate the clusters into top and bottom clusters.
		List<Cluster> topList = new ArrayList<Cluster>();
		List<Cluster> botList = new ArrayList<Cluster>();
		for(Cluster cluster : clusters) {
			if(TriggerModule.getClusterYIndex(cluster) > 0) {
				topList.add(cluster);
			} else {
				botList.add(cluster);
			}
		}
		
		// Form all permutations of top and bottom clusters.
		List<Cluster[]> pairList = new ArrayList<Cluster[]>();
		for(Cluster topCluster : topList) {
			for(Cluster botCluster : botList) {
				pairList.add(new Cluster[] { topCluster, botCluster });
			}
		}
		
		// Return the resulting cluster pairs.
		return pairList;
	}
}
