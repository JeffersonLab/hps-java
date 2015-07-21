package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TridentTrackDriver extends Driver {
	private String finalStateCollectionName = "FinalStateParticles";
	private String candidateCollectionName = "UnconstrainedV0Candidates";
	
	private int tracksCandidate = 0;
	private int tracksFinalState = 0;
	private int tracksCandidateCluster = 0;
	private int tracksFinalStateCluster = 0;
	
	private static final int ANY_CLUSTER = 0;
	private static final int HAS_CLUSTER = 1;
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[] tracks = new IHistogram1D[2];
	private IHistogram1D[] posTracks = new IHistogram1D[2];
	private IHistogram1D[] negTracks = new IHistogram1D[2];
	private IHistogram1D[] posMomentum = new IHistogram1D[2];
	private IHistogram1D[] negMomentum = new IHistogram1D[2];
	private IHistogram1D[] energySum = new IHistogram1D[2];
	private IHistogram1D[] energyMomentumDiff = new IHistogram1D[2];
	private IHistogram1D[] momentumSum = new IHistogram1D[2];
	private IHistogram1D[] invariantMass = new IHistogram1D[2];
	private IHistogram2D[] energySum2D = new IHistogram2D[2];
	private IHistogram2D[] momentumSum2D = new IHistogram2D[2];
	private IHistogram2D[] position = new IHistogram2D[2];
	
	@Override
	public void startOfData() {
		// Instantiate the "any cluster status" plots.
		tracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (All)", 7, -0.5, 6.5);
		posTracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (Positive)", 7, -0.5, 6.5);
		negTracks[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Tracks in Event (Negative)", 7, -0.5, 6.5);
		posMomentum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Momentum (Positive)", 110, 0, 1.1);
		negMomentum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Momentum (Negative)", 110, 0, 1.1);
		energySum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Energy Sum", 55, 0, 2.2);
		momentumSum[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Momentum Sum", 55, 0, 2.2);
		energyMomentumDiff[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Energy-Momentum Difference", 55, 0, 2.2);
		invariantMass[ANY_CLUSTER] = aida.histogram1D("Trident Analysis/All/Invariant Mass", 240, 0.000, 0.120);
		energySum2D[ANY_CLUSTER] = aida.histogram2D("Trident Analysis/All/2D Energy Sum", 55, 0, 1.1, 55, 0, 1.1);
		momentumSum2D[ANY_CLUSTER] = aida.histogram2D("Trident Analysis/All/2D Momentum Sum", 55, 0, 1.1, 55, 0, 1.1);
		position[ANY_CLUSTER] = aida.histogram2D("Trident Analysis/All/Track Cluster Position", 46, -23, 23, 11, -5.5, 5.5);
		
		// Instantiate the "has a cluster" plots.
		tracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (All)", 7, -0.5, 6.5);
		posTracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (Positive)", 7, -0.5, 6.5);
		negTracks[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Tracks in Event (Negative)", 7, -0.5, 6.5);
		posMomentum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Momentum (Positive)", 110, 0, 1.1);
		negMomentum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Momentum (Negative)", 110, 0, 1.1);
		energySum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Energy Sum", 55, 0, 2.2);
		momentumSum[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Momentum Sum", 55, 0, 2.2);
		energyMomentumDiff[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Energy-Momentum Difference", 55, 0, 2.2);
		invariantMass[HAS_CLUSTER] = aida.histogram1D("Trident Analysis/Cluster/Invariant Mass", 240, 0.000, 0.120);
		energySum2D[HAS_CLUSTER] = aida.histogram2D("Trident Analysis/Cluster/2D Energy Sum", 55, 0, 1.1, 55, 0, 1.1);
		momentumSum2D[HAS_CLUSTER] = aida.histogram2D("Trident Analysis/Cluster/2D Momentum Sum", 55, 0, 1.1, 55, 0, 1.1);
		position[HAS_CLUSTER] = aida.histogram2D("Trident Analysis/Cluster/Track Cluster Position", 46, -23, 23, 11, -5.5, 5.5);
	}
	
	@Override
	public void endOfData() {
		System.out.printf("Tracks (Candidate)           :: %d%n", tracksCandidate);
		System.out.printf("Tracks (Final State)         :: %d%n", tracksFinalState);
		System.out.printf("Cluster Tracks (Candidate)   :: %d%n", tracksCandidateCluster);
		System.out.printf("Cluster Tracks (Final State) :: %d%n", tracksFinalStateCluster);
	}
	
	@Override
	public void process(EventHeader event) {
		// Check for final state particles.
		if(event.hasCollection(ReconstructedParticle.class, finalStateCollectionName)) {
			// Get the final state particles.
			List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, finalStateCollectionName);
			
			// Store the positive and negative tracks.
			List<ReconstructedParticle> allTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> posTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> negTrackList = new ArrayList<ReconstructedParticle>();
			
			// Store the same tracks, but limited to those with clusters.
			List<ReconstructedParticle> allClusterTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> posClusterTrackList = new ArrayList<ReconstructedParticle>();
			List<ReconstructedParticle> negClusterTrackList = new ArrayList<ReconstructedParticle>();
			
			// Iterate over the tracks and populate the lists.
			for(ReconstructedParticle track : trackList) {
				// Skip instances with no raw tracks.
				if(track.getTracks().size() == 0) { continue; }
				
				// Add the cluster to the all track list.
				allTrackList.add(track);
				
				// Track the number of cluster tracks.
				tracksFinalState++;
				if(!track.getClusters().isEmpty()) {
					tracksFinalStateCluster++;
				}
				
				// Process the tracks based on charge.
				if(track.getCharge() > 0) {
					// Increment the counters and populate the momentum plots.
					posTrackList.add(track);
					posMomentum[ANY_CLUSTER].fill(track.getMomentum().magnitude());
					
					// Repeat for the "has clusters" plots if necessary.
					if(track.getClusters().size() > 0) {
						// Increment the counters and populate the
						// momentum plot.
						posClusterTrackList.add(track);
						allClusterTrackList.add(track);
						posMomentum[HAS_CLUSTER].fill(track.getMomentum().magnitude());
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				} else if(track.getCharge() < 0) {
					// Increment the counters and populate the momentum plots.
					negTrackList.add(track);
					negMomentum[ANY_CLUSTER].fill(track.getMomentum().magnitude());
					
					// Repeat for the "has clusters" plots if necessary.
					if(track.getClusters().size() > 0) {
						// Increment the counters and populate the
						// momentum plot.
						negClusterTrackList.add(track);
						allClusterTrackList.add(track);
						negMomentum[HAS_CLUSTER].fill(track.getMomentum().magnitude());
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				} else {
					if(track.getClusters().size() > 0) {
						// Increment the counter.
						allClusterTrackList.add(track);
						
						// Populate the cluster position plot.
						int ix = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
						int iy = track.getClusters().get(0).getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
						position[HAS_CLUSTER].fill(ix, iy);
					}
				}
			}
			
			// Populate the tracks per event plots.
			tracks[ANY_CLUSTER].fill(allTrackList.size());
			tracks[HAS_CLUSTER].fill(allClusterTrackList.size());
			posTracks[ANY_CLUSTER].fill(posTrackList.size());
			posTracks[HAS_CLUSTER].fill(posClusterTrackList.size());
			negTracks[ANY_CLUSTER].fill(negTrackList.size());
			negTracks[HAS_CLUSTER].fill(negClusterTrackList.size());
			
			/// Store track pairs.
			List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
			List<ReconstructedParticle[]> pairClusterList = new ArrayList<ReconstructedParticle[]>();
			
			// Form track pairs for all tracks.
			for(ReconstructedParticle posTrack : posTrackList) {
				for(ReconstructedParticle negTrack : negTrackList) {
					pairList.add(new ReconstructedParticle[] { posTrack, negTrack });
				}
			}
			
			// Form track pairs for cluster tracks.
			for(ReconstructedParticle posTrack : posClusterTrackList) {
				for(ReconstructedParticle negTrack : negClusterTrackList) {
					pairClusterList.add(new ReconstructedParticle[] { posTrack, negTrack });
				}
			}
			
			// Populate the track pair plots.
			for(ReconstructedParticle[] pair : pairList) {
				Hep3Vector pSum = new BasicHep3Vector(
						pair[0].getMomentum().x() + pair[1].getMomentum().x(),
						pair[0].getMomentum().y() + pair[1].getMomentum().y(),
						pair[0].getMomentum().z() + pair[1].getMomentum().z());
				
				energySum[ANY_CLUSTER].fill(pair[0].getEnergy() + pair[1].getEnergy());
				momentumSum[ANY_CLUSTER].fill(pSum.magnitude());
				energySum2D[ANY_CLUSTER].fill(pair[0].getEnergy(), pair[1].getEnergy());
				momentumSum2D[ANY_CLUSTER].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
				energyMomentumDiff[ANY_CLUSTER].fill(Math.abs((pair[0].getEnergy() + pair[1].getEnergy()) - pSum.magnitude()));
			}
			
			// Populate the cluster track pair plots.
			for(ReconstructedParticle[] pair : pairClusterList) {
				Hep3Vector pSum = new BasicHep3Vector(
						pair[0].getMomentum().x() + pair[1].getMomentum().x(),
						pair[0].getMomentum().y() + pair[1].getMomentum().y(),
						pair[0].getMomentum().z() + pair[1].getMomentum().z());
				
				energySum[HAS_CLUSTER].fill(pair[0].getEnergy() + pair[1].getEnergy());
				momentumSum[HAS_CLUSTER].fill(pSum.magnitude());
				energySum2D[HAS_CLUSTER].fill(pair[0].getEnergy(), pair[1].getEnergy());
				momentumSum2D[HAS_CLUSTER].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
				energyMomentumDiff[HAS_CLUSTER].fill(Math.abs((pair[0].getEnergy() + pair[1].getEnergy()) - pSum.magnitude()));
			}
		}
		
		// Check for V0 candidates.
		if(event.hasCollection(ReconstructedParticle.class, candidateCollectionName)) {
			// Get the candidate particles.
			List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, candidateCollectionName);
			
			// Increment the counter.
			tracksCandidate += trackList.size();
			
			// Increment the counter for cluster tracks.
			for(ReconstructedParticle track : trackList) {
				// Populate the invariant mass plot.
				invariantMass[ANY_CLUSTER].fill(track.getMass());
				
				// Check for a cluster track.
				if(track.getClusters().size() > 0) {
					tracksCandidateCluster++;
					invariantMass[HAS_CLUSTER].fill(track.getMass());
				}
			}
		}
	}
}
