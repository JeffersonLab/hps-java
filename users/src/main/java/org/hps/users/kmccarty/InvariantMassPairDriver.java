package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.ecal.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class InvariantMassPairDriver extends Driver {
	private int[] events = new int[3];
	private TriggerModule[] trigger = new TriggerModule[2];
	
	private String gtpClusterCollectionName = "EcalClustersGTP";
	private String particleCollectionName = "FinalStateParticles";
	private String reconParticleCollectionName = "UnconstrainedV0Candidates";
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D electronEnergyHist = aida.histogram1D("Trident Analysis/Electron Energy", 150, 0.000, 1.500);
	private IHistogram1D positronEnergyHist = aida.histogram1D("Trident Analysis/Positron Energy", 150, 0.000, 1.500);
	private IHistogram1D pairEnergyHist = aida.histogram1D("Trident Analysis/Energy Sum Distribution", 220, 0.00, 2.200);
	private IHistogram2D pair2DEnergyHist = aida.histogram2D("Trident Analysis/2D Energy Distribution", 55, 0, 1.1, 55, 0, 1.1);
	private IHistogram1D pair1MassHist = aida.histogram1D("Trident Analysis/Particle Invariant Mass (1 Hit)", 240, 0.000, 0.120);
	private IHistogram1D pair1ModMassHist = aida.histogram1D("Trident Analysis/Particle Invariant Mass (2 Hit)", 240, 0.000, 0.120);
	private IHistogram1D elasticElectronEnergyHist = aida.histogram1D("Trident Analysis/Trident Electron Energy", 150, 0.000, 1.500);
	private IHistogram1D elasticPositronEnergyHist = aida.histogram1D("Trident Analysis/Trident Positron Energy", 150, 0.000, 1.500);
	
	@Override
	public void startOfData() {
		// Instantiate the pair 1 trigger.
		trigger[0] = new TriggerModule();
		trigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       1);
		trigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    0.054);
		trigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   0.630);
		trigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       30);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 0.540);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         0.180);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        0.860);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.600);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F,         0.0055);
		trigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE,       12);
		
		// Instantiate the pair 1 trigger with a hit count cut of two.
		trigger[1] = new TriggerModule();
		trigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW,       2);
		trigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW,    0.054);
		trigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH,   0.630);
		trigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH,       30);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 0.540);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW,         0.180);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH,        0.860);
		trigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW,       0.600);
		trigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F,         0.0055);
		trigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE,       12);
	}
	
	@Override
	public void endOfData() {
		System.out.printf("Pair 1     :: %d / %d%n", events[0], events[2]);
		System.out.printf("Pair 1 Mod :: %d / %d%n", events[1], events[2]);
	}
	
	@Override
	public void process(EventHeader event) {
		// Skip the event if there is no reconstructed particle list.
		if(!event.hasCollection(ReconstructedParticle.class, particleCollectionName)) {
			return;
		}
		
		// Get a list of all tracks in the event.
		List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, particleCollectionName);
		
		// Plot the energies of the electrons and positrons.
		for(ReconstructedParticle track : trackList) {
			// Positive tracks are assumed to be positrons.
			if(track.getCharge() > 0) {
				positronEnergyHist.fill(track.getMomentum().magnitude());
			}
			
			// Negative tracks are assumed to be electrons.
			else if(track.getCharge() < 0) {
				electronEnergyHist.fill(track.getMomentum().magnitude());
			}
		}
		
		// Get track pairs.
		List<ReconstructedParticle[]> trackPairList = getTrackPairs(trackList);
		
		// Populate the pair plots.
		trackPairLoop:
		for(ReconstructedParticle[] trackPair : trackPairList) {
			// Note the polarity of the tracks.
			boolean[] trackIsPositive = {
					trackPair[0].getCharge() > 0,
					trackPair[1].getCharge() > 0
			};
			
			// Require that one track be positive and one be negative.
			if(!(trackIsPositive[0] ^ trackIsPositive[1])) {
				continue trackPairLoop;
			}
			
			// Populate the track pair plots.
			pairEnergyHist.fill(VecOp.add(trackPair[0].getMomentum(), trackPair[1].getMomentum()).magnitude());
			if(trackIsPositive[0]) {
				pair2DEnergyHist.fill(trackPair[0].getMomentum().magnitude(), trackPair[1].getMomentum().magnitude());
			} else {
				pair2DEnergyHist.fill(trackPair[1].getMomentum().magnitude(), trackPair[0].getMomentum().magnitude());
			}
		}
		
		// Check that the event has a collection of GTP clusters.
		if(!event.hasCollection(Cluster.class, gtpClusterCollectionName)) {
			return;
		}
		
		// Increment the total event count.
		events[2]++;
		
		// Get the GTP clusters.
		List<Cluster> clusters = event.get(Cluster.class, gtpClusterCollectionName);
		
		// Get the list of top/bottom pairs.
		List<Cluster[]> pairs = getClusterPairs(clusters);
		
		// Iterate over the pairs and determine if any cluster passes
		// pair 1 trigger or the pair 1 modified trigger.
		boolean passedPair1 = false;
		boolean passedPair1Mod = false;
		pairLoop:
		for(Cluster[] pair : pairs) {
			// Check the cluster energy cut.
			if(!trigger[0].clusterTotalEnergyCut(pair[0])) { continue pairLoop; }
			if(!trigger[0].clusterTotalEnergyCut(pair[1])) { continue pairLoop; }
			
			// Check the pair cuts.
			if(!trigger[0].pairCoplanarityCut(pair)) { continue pairLoop; }
			if(!trigger[0].pairEnergyDifferenceCut(pair)) { continue pairLoop; }
			if(!trigger[0].pairEnergySumCut(pair)) { continue pairLoop; }
			if(!trigger[0].pairEnergySlopeCut(pair)) { continue pairLoop; }
			
			// Check if the pair passes the singles 0 hit count cut.
			if(trigger[0].clusterHitCountCut(pair[0]) && trigger[0].clusterHitCountCut(pair[1])) {
				// Note that a pair passed the pair 1 trigger.
				passedPair1 = true;
				
				// Check whether the pair passed the modified pair 1
				// trigger hit count cut.
				if(trigger[1].clusterHitCountCut(pair[0]) && trigger[1].clusterHitCountCut(pair[1])) {
					passedPair1Mod = true;
				}
			} else { continue pairLoop; }
		}
		
		// If no pair passed the pair 1 cut, nothing further need be done.
		if(!passedPair1) { return; }
		
		// Otherwise, increment the "passed pair 1" count and the
		// "passed pair 1 mod" count, if appropriate.
		events[0]++;
		if(passedPair1Mod) { events[1]++; }
		
		// Get the collection of reconstructed V0 candidates.
		List<ReconstructedParticle> candidateList = event.get(ReconstructedParticle.class, reconParticleCollectionName);
		
		// Populate the invariant mass plot.
		candidateLoop:
		for(ReconstructedParticle particle : candidateList) {
			// Track the electron and positron momenta.
			double electronMomentum = 0.0;
			double positronMomentum = 0.0;
			
			// Check that it has component particles that meet the
			// trident condition.
			boolean seenPositive = false;
			boolean seenNegative = false;
			for(ReconstructedParticle track : particle.getParticles()) {
				// Exactly one track must be negative. Its energy is
				// disallowed from exceeding 900 MeV.
				if(track.getCharge() < 0) {
					// Reject a second negative particle.
					if(seenNegative) { continue candidateLoop; }
					
					// Otherwise, note that one has been seen.
					seenNegative = true;
					electronMomentum = track.getMomentum().magnitude();
					
					// Reject electrons with a momentum exceeding 900 MeV.
					if(track.getMomentum().magnitude() > 0.900) {
						continue candidateLoop;
					}
				}
				
				// Exactly one track must be positive. Its energy is
				// not constrained.
				else if(track.getCharge() > 0) {
					// Reject a second positive particle.
					if(seenPositive) { continue candidateLoop; }
					
					// Otherwise, note that one has been seen.
					seenPositive = true;
					positronMomentum = track.getMomentum().magnitude();
				}
				
				// Lastly, reject any particle that produced a photon.
				else { continue candidateLoop; }
			}
			
			// Populate the plots.
			pair1MassHist.fill(particle.getMass());
			elasticElectronEnergyHist.fill(electronMomentum);
			elasticPositronEnergyHist.fill(positronMomentum);
			if(passedPair1Mod) { pair1ModMassHist.fill(particle.getMass()); }
		}
	}
	
	/**
	 * Creates a list of top/bottom cluster pairs.
	 * @param clusters - A <code>List</code> collection of objects of
	 * type <code>Cluster</code>.
	 * @return Returns a <code>List</code> collection of 2-entry arrays
	 * of <code>Cluster</code> objects representing top/bottom cluster
	 * pairs. The first entry is always the top cluster.
	 */
	private static final List<Cluster[]> getClusterPairs(List<Cluster> clusters) {
		// Separate the clusters into top and bottom clusters.
		List<Cluster> topList = new ArrayList<Cluster>();
		List<Cluster> botList = new ArrayList<Cluster>();
		for(Cluster cluster : clusters) {
			if(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
				topList.add(cluster);
			}
			else { botList.add(cluster); }
		}
		
		// Create a list of all top/bottom pairs.
		List<Cluster[]> pairs = new ArrayList<Cluster[]>();
		for(Cluster topCluster : topList) {
			for(Cluster botCluster : botList) {
				pairs.add(new Cluster[] { topCluster, botCluster });
			}
		}
		
		// Return the list of cluster pairs.
		return pairs;
	}
	
	private static final List<ReconstructedParticle[]> getTrackPairs(List<ReconstructedParticle> trackList) {
		// Create an empty list for the pairs.
		List<ReconstructedParticle[]> pairs = new ArrayList<ReconstructedParticle[]>();
		
		// Add all possible pairs of tracks.
		for(int i = 0; i < trackList.size(); i++) {
			for(int j = i + 1; j < trackList.size(); j++) {
				pairs.add(new ReconstructedParticle[] { trackList.get(i), trackList.get(j) });
			}
		}
		
		// Return the list of tracks.
		return pairs;
	}
}
