package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class MTEAnalysis extends Driver {
	// Define track LCIO information.
	private String particleCollectionName = "FinalStateParticles";
	private static final AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[] chargedTracksPlot = {
			aida.histogram1D("MTE Analysis/Møller Event Tracks", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Trident Event Tracks", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Elastic Event Tracks", 10, -0.5, 9.5)
	};
	private IHistogram1D[] energyPlot = {
			aida.histogram1D("MTE Analysis/Møller Energy Sum Distribution", 220, 0, 2.2),
			aida.histogram1D("MTE Analysis/Trident Energy Sum Distribution", 220, 0, 2.2),
			aida.histogram1D("MTE Analysis/Elastic Energy Distribution", 110, 0, 1.5)
	};
	private IHistogram1D[] electronPlot = {
			aida.histogram1D("MTE Analysis/Møller Electron Energy Distribution", 220, 0, 2.2),
			aida.histogram1D("MTE Analysis/Trident Electron Energy Distribution", 220, 0, 2.2),
	};
	private IHistogram1D positronPlot = aida.histogram1D("MTE Analysis/Trident Positron Energy Distribution", 220, 0, 2.2);
	private IHistogram2D[] energy2DPlot = {
			aida.histogram2D("MTE Analysis/Møller 2D Energy Distribution", 55, 0, 1.1, 55, 0, 1.1),
			aida.histogram2D("MTE Analysis/Trident 2D Energy Distribution", 55, 0, 1.1, 55, 0, 1.1),
	};
	private IHistogram1D timePlot = aida.histogram1D("MTE Analysis/Track Cluster Time Distribution", 4000, 0, 400);
	private IHistogram1D timeCoincidencePlot = aida.histogram1D("MTE Analysis/Møller Time Coincidence Distribution", 1000, 0, 100);
	private IHistogram1D timeCoincidenceAllCutsPlot = aida.histogram1D("MTE Analysis/Møller Time Coincidence Distribution (All Møller Cuts)", 1000, 0, 100);
	private static final int MØLLER  = 0;
	private static final int TRIDENT = 1;
	private static final int ELASTIC = 2;
	private boolean verbose = false;
	private double timeCoincidenceCut = Double.MAX_VALUE;
	
	@Override
	public void process(EventHeader event) {
		if(event.hasCollection(ReconstructedParticle.class, particleCollectionName)) {
			// Get the list of tracks.
			List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, particleCollectionName);
			
			// Plot the time stamps of all tracks.
			for(ReconstructedParticle track : trackList) {
				if(track.getClusters().size() != 0) {
					Cluster cluster = track.getClusters().get(0);
					timePlot.fill(cluster.getCalorimeterHits().get(0).getTime());
				}
			}
			
			if(verbose) {
				System.out.println(trackList.size() + " tracks found.");
				for(ReconstructedParticle track : trackList) {
					System.out.printf("Track :: Q = %4.1f; E = %6.3f%n",
							track.getCharge(), track.getEnergy());
				}
			}
			
			// Check each of the event-type conditions.
			boolean isMøller = false;
			boolean isTrident = false;
			boolean isElastic = false;
			
			// Produce all possible pairs of tracks.
			List<ReconstructedParticle[]> pairList = getTrackPairs(trackList);
			
			// Check the Møller condition. A Møller event is expected
			// to have two tracks, both negative, with a net energy
			// within a certain band of the beam energy.
			møllerTrackLoop:
			for(ReconstructedParticle[] pair : pairList) {
				// Both tracks are required to be negatively charged.
				if(pair[0].getCharge() >= 0 || pair[1].getCharge() >= 0) {
					continue møllerTrackLoop;
				}
				
				// Both tracks must have clusters associated with them.
				Cluster[] trackClusters = new Cluster[2];
				for(int i = 0; i < 2; i++) {
					// Disallow tracks with no associated clusters.
					if(pair[i].getClusters().size() == 0) {
						continue møllerTrackLoop;
					}
					
					// Store the first cluster associated with the track.
					trackClusters[i] = pair[i].getClusters().get(0);
				}
				
				// Require that the track clusters be within a certain
				// time window of one another.
				CalorimeterHit[] seeds = new CalorimeterHit[2];
				seeds[0] = trackClusters[0].getCalorimeterHits().get(0);
				seeds[1] = trackClusters[1].getCalorimeterHits().get(0);
				timeCoincidencePlot.fill(Math.abs(seeds[0].getTime() - seeds[1].getTime()));
				if(Math.abs(trackClusters[0].getCalorimeterHits().get(0).getTime() - trackClusters[1].getCalorimeterHits().get(0).getTime()) > timeCoincidenceCut) {
					continue møllerTrackLoop;
				}
				
				// Require both tracks to occur within the range of
				// 36.5 and 49 ns.
				if(seeds[0].getTime() < 36.5 || seeds[0].getTime() > 49) {
					continue møllerTrackLoop;
				} if(seeds[1].getTime() < 36.5 || seeds[1].getTime() > 49) {
					continue møllerTrackLoop;
				}
				
				// No track may have an energy that exceeds 900 MeV.
				if(pair[0].getMomentum().magnitude() >= 0.900 || pair[1].getMomentum().magnitude() >= 0.900) {
					continue møllerTrackLoop;
				}
				
				// Get the energy sum.
				double sum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
				
				// "Møller-like" track pairs must have energies within
				// an allowed energy range.
				if(sum < 0.800 || sum > 1.500) {
					continue møllerTrackLoop;
				}
				
				timeCoincidenceAllCutsPlot.fill(Math.abs(seeds[0].getTime() - seeds[1].getTime()));
				
				// Note that this is a Møller event.
				isMøller = true;
				
				// Populate the Møller plots.
				energyPlot[MØLLER].fill(sum);
				electronPlot[MØLLER].fill(pair[0].getMomentum().magnitude());
				electronPlot[MØLLER].fill(pair[1].getMomentum().magnitude());
				energy2DPlot[MØLLER].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			}
			
			// Check the elastic condition. Elastic events should be
			// negatively and have an energy approximately equal to
			// the beam energy.
			for(ReconstructedParticle track : trackList) {
				if(track.getCharge() < 0 && track.getMomentum().magnitude() >= 0.900) {
					isElastic = true;
					energyPlot[ELASTIC].fill(track.getMomentum().magnitude());
				}
			}
			
			// Check the trident condition. Tridents are events that
			// contain both one positive and one negative track.
			for(ReconstructedParticle[] pair : pairList) {
				if((pair[0].getCharge() < 0 && pair[1].getCharge() > 0) ||
						pair[0].getCharge() > 0 && pair[1].getCharge() < 0) {
					// Require that the energy of the electron is below
					// 900 MeV.
					if((pair[0].getCharge() < 0 && pair[0].getMomentum().magnitude() < 0.900)
							|| (pair[1].getCharge() < 0 && pair[1].getMomentum().magnitude() < 0.900)) {
						isTrident = true;
						if(pair[0].getCharge() > 0) {
							positronPlot.fill(pair[1].getMomentum().magnitude());
							electronPlot[TRIDENT].fill(pair[0].getMomentum().magnitude());
						} else {
							positronPlot.fill(pair[0].getMomentum().magnitude());
							electronPlot[TRIDENT].fill(pair[1].getMomentum().magnitude());
						}
						energyPlot[TRIDENT].fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
						energy2DPlot[TRIDENT].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
					}
				}
			}
			
			if(verbose) {
				System.out.printf("\tMøller  :: %b%n", isMøller);
				System.out.printf("\tTrident :: %b%n", isTrident);
				System.out.printf("\tElastic :: %b%n", isElastic);
				System.out.println();
			}
			
			// Get the number of charged tracks in the event.
			int tracks = 0;
			for(ReconstructedParticle track : trackList) {
				if(track.getCharge() != 0) { tracks++; }
			}
			
			// Add the result to the appropriate plots.
			if(isMøller) {
				chargedTracksPlot[MØLLER].fill(tracks);
			} else if(isTrident) {
				chargedTracksPlot[TRIDENT].fill(tracks);
			} else if(isElastic) {
				chargedTracksPlot[ELASTIC].fill(tracks);
			}
		}
	}
	
	public void setTimeCoincidenceCut(double value) {
		timeCoincidenceCut = value;
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
