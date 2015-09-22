package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class MTEAnalysis extends Driver {
	// Define track LCIO information.
	private String bankCollectionName = "TriggerBank";
	private String particleCollectionName = "FinalStateParticles";
	private static final AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[] chargedTracksPlot = {
			aida.histogram1D("MTE Analysis/Møller Event Tracks", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Trident Event Tracks", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Elastic Event Tracks", 10, -0.5, 9.5)
	};
	private IHistogram1D[] clusterCountPlot = {
			aida.histogram1D("MTE Analysis/Møller Event Clusters", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Trident Event Clusters", 10, -0.5, 9.5),
			aida.histogram1D("MTE Analysis/Elastic Event Clusters", 10, -0.5, 9.5)
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
	private IHistogram1D negTrackCount = aida.histogram1D("MTE Analysis/All Negative Tracks", 10, -0.5, 9.5);
	private IHistogram1D posTrackCount = aida.histogram1D("MTE Analysis/All Positive Event Tracks", 10, -0.5, 9.5);
	private IHistogram1D chargedTrackCount = aida.histogram1D("MTE Analysis/All Event Event Tracks", 10, -0.5, 9.5);
	private TriggerPlotsModule allPlots = new TriggerPlotsModule("All");
	private TriggerPlotsModule møllerPlots = new TriggerPlotsModule("Møller");
	private TriggerPlotsModule tridentPlots = new TriggerPlotsModule("Trident");
	private TriggerPlotsModule elasticPlots = new TriggerPlotsModule("Elastic");
	private static final int MØLLER  = 0;
	private static final int TRIDENT = 1;
	private static final int ELASTIC = 2;
	private boolean verbose = false;
	private boolean excludeNoTrackEvents = false;
	private double timeCoincidenceCut = Double.MAX_VALUE;
	private Map<String, Integer> møllerBitMap = new HashMap<String, Integer>();
	private Map<String, Integer> tridentBitMap = new HashMap<String, Integer>();
	private Map<String, Integer> elasticBitMap = new HashMap<String, Integer>();
	private int møllerEvents = 0;
	private int tridentEvents = 0;
	private int elasticEvents = 0;
	private int totalEvents = 0;
	private int pair1Events = 0;
	private int pair0Events = 0;
	private int singles1Events = 0;
	private int singles0Events = 0;
	private int pulserEvents = 0;
	
	@Override
	public void startOfData() {
		for(int s0 = 0; s0 <= 1; s0++) {
			for(int s1 = 0; s1 <= 1; s1++) {
				for(int p0 = 0; p0 <= 1; p0++) {
					for(int p1 = 0; p1 <= 1; p1++) {
						for(int pulser = 0; pulser <=1; pulser++) {
							// Set each "trigger bit."
							boolean s0bit = (s0 == 1);
							boolean s1bit = (s1 == 1);
							boolean p0bit = (p0 == 1);
							boolean p1bit = (p1 == 1);
							boolean pulserBit = (p1 == 1);
							
							// Generate the bit string.
							String bitString = getBitString(s0bit, s1bit, p0bit, p1bit, pulserBit);
							
							// Set a default value of zero for this bit combination.
							møllerBitMap.put(bitString, 1);
							tridentBitMap.put(bitString, 1);
							elasticBitMap.put(bitString, 1);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void endOfData() {
		System.out.println("Møller  Events   :: " + møllerEvents);
		System.out.println("Trident Events   :: " + tridentEvents);
		System.out.println("Elastic Events   :: " + elasticEvents);
		System.out.println("Total Events     :: " + totalEvents);
		System.out.println("Pair 1 Events    :: " + pair1Events);
		System.out.println("Pair 0 Events    :: " + pair0Events);
		System.out.println("Singles 1 Events :: " + singles1Events);
		System.out.println("Singles 0 Events :: " + singles0Events);
		System.out.println("Pulser Events    :: " + pulserEvents);
		
		System.out.println("Plsr\tS0\tS1\tP0\tP1\tMøller");
		for(Entry<String, Integer> entry : møllerBitMap.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		
		System.out.println("Plsr\tS0\tS1\tP0\tP1\tTrident");
		for(Entry<String, Integer> entry : tridentBitMap.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		
		System.out.println("Plsr\tS0\tS1\tP0\tP1\tElastic");
		for(Entry<String, Integer> entry : elasticBitMap.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
	}
	
	private static final String getBitString(boolean s0, boolean s1, boolean p0, boolean p1, boolean pulser) {
		return String.format("%d\t%d\t%d\t%d\t%d", (pulser ? 1 : 0), (s0 ? 1 : 0), (s1 ? 1 : 0), (p0 ? 1 : 0), (p1 ? 1 : 0));
	}
	
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
			
			// Populate the all cluster plots.
			List<Cluster> topClusters = new ArrayList<Cluster>();
			List<Cluster> botClusters = new ArrayList<Cluster>();
			List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
			for(Cluster cluster : clusters) {
				allPlots.addCluster(cluster);
				if(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) { topClusters.add(cluster); }
				else { botClusters.add(cluster); }
			}
			
			// Make cluster pairs.
			List<Cluster[]> clusterPairs = new ArrayList<Cluster[]>();
			for(Cluster topCluster : topClusters) {
				for(Cluster botCluster : botClusters) {
					clusterPairs.add(new Cluster[] { topCluster, botCluster });
				}
			}
			
			// Populate the all cluster pair plots.
			for(Cluster[] pair : clusterPairs) {
				allPlots.addClusterPair(pair);
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
				// If trackless events are to be excluded, then require
				// that each "track" have a real track.
				if(excludeNoTrackEvents && (pair[0].getTracks().isEmpty() || pair[1].getTracks().isEmpty())) {
					continue møllerTrackLoop;
				}
				
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
				
				//timeCoincidenceAllCutsPlot.fill(Math.abs(seeds[0].getTime() - seeds[1].getTime()));
				
				// Note that this is a Møller event.
				isMøller = true;
				
				// Populate the Møller plots.
				energyPlot[MØLLER].fill(sum);
				møllerPlots.addClusterPair(trackClusters);
				electronPlot[MØLLER].fill(pair[0].getMomentum().magnitude());
				electronPlot[MØLLER].fill(pair[1].getMomentum().magnitude());
				energy2DPlot[MØLLER].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			}
			
			// Check the elastic condition. Elastic events should be
			// negatively and have an energy approximately equal to
			// the beam energy.
			elasticTrackLoop:
			for(ReconstructedParticle track : trackList) {
				// If trackless events are to be excluded, then require
				// that the "track" has a real track.
				if(excludeNoTrackEvents && track.getTracks().isEmpty()) {
					continue elasticTrackLoop;
				}
				
				// Check the elastic condition.
				if(track.getCharge() < 0 && track.getMomentum().magnitude() >= 0.900) {
					isElastic = true;
					energyPlot[ELASTIC].fill(track.getMomentum().magnitude());
					if(!track.getClusters().isEmpty()) {
						elasticPlots.addCluster(track.getClusters().get(0));
					}
				}
			}
			
			// Check the trident condition. Tridents are events that
			// contain both one positive and one negative track.
			tridentTrackLoop:
			for(ReconstructedParticle[] pair : pairList) {
				// If trackless events are to be excluded, then require
				// that each "track" have a real track.
				if(excludeNoTrackEvents && (pair[0].getTracks().isEmpty() || pair[1].getTracks().isEmpty())) {
					continue tridentTrackLoop;
				}
				
				// Check the trident condition.
				if((pair[0].getCharge() < 0 && pair[1].getCharge() > 0) || pair[0].getCharge() > 0 && pair[1].getCharge() < 0) {
					// Both tracks must have clusters associated with them.
					/*
					Cluster[] trackClusters = new Cluster[2];
					for(int i = 0; i < 2; i++) {
						// Disallow tracks with no associated clusters.
						if(pair[i].getClusters().size() == 0) {
							continue tridentTrackLoop;
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
						continue tridentTrackLoop;
					}
					*/
					
					// Require that the energy of the electron is below
					// 900 MeV.
					//if((pair[0].getCharge() < 0 && pair[0].getMomentum().magnitude() < 0.900)
					//		|| (pair[1].getCharge() < 0 && pair[1].getMomentum().magnitude() < 0.900)) {
						isTrident = true;
						//tridentPlots.addClusterPair(trackClusters);
						if(pair[0].getCharge() > 0) {
							positronPlot.fill(pair[1].getMomentum().magnitude());
							electronPlot[TRIDENT].fill(pair[0].getMomentum().magnitude());
						} else {
							positronPlot.fill(pair[0].getMomentum().magnitude());
							electronPlot[TRIDENT].fill(pair[1].getMomentum().magnitude());
						}
						energyPlot[TRIDENT].fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
						energy2DPlot[TRIDENT].fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
					//}
				}
			}
			
			if(verbose) {
				System.out.printf("\tMøller  :: %b%n", isMøller);
				System.out.printf("\tTrident :: %b%n", isTrident);
				System.out.printf("\tElastic :: %b%n", isElastic);
				System.out.println();
			}
			
			// Get the TI bits.
			String bitString = null;
			TIData tiBank = null;
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			for(GenericObject obj : bankList) {
				if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
					bitString = getBitString(tiBank.isPulserTrigger(), tiBank.isSingle0Trigger(),
							tiBank.isSingle1Trigger(), tiBank.isPair0Trigger(), tiBank.isPair1Trigger());
					
					if(tiBank.isPair1Trigger()) {
						pair1Events++;
					} else if(tiBank.isPair0Trigger()) {
						pair0Events++;
					} else if(tiBank.isSingle1Trigger()) {
						singles1Events++;
					} else if(tiBank.isSingle0Trigger()) {
						singles0Events++;
					} else if(tiBank.isPulserTrigger()) {
						pulserEvents++;
					}
				}
			}
			if(bitString == null) {
				System.out.println("No TI data found!!");
			}
			
			// Get the number of charged tracks in the event.
			int tracks = 0;
			int posTracks = 0;
			int negTracks = 0;
			for(ReconstructedParticle track : trackList) {
				if(track.getCharge() != 0 && tiBank.isPulserTrigger()) {
					if(excludeNoTrackEvents && !track.getTracks().isEmpty()) {
						tracks++;
						if(track.getCharge() > 0) { posTracks++; }
						else { negTracks++; }
					} else {
						tracks++;
						if(track.getCharge() > 0) { posTracks++; }
						else { negTracks++; }
					}
				}
			}
			
			// Populate the "all tracks" plots.
			posTrackCount.fill(posTracks);
			negTrackCount.fill(negTracks);
			chargedTrackCount.fill(tracks);
			
			// Add the result to the appropriate plots and increment
			// the appropriate trigger bit combination.
			if(isMøller) {
				møllerEvents++;
				chargedTracksPlot[MØLLER].fill(tracks);
				clusterCountPlot[MØLLER].fill(clusters.size());
				
				Integer val = møllerBitMap.get(bitString);
				if(val == null) { møllerBitMap.put(bitString, 1); }
				else { møllerBitMap.put(bitString, val + 1); }
			} else if(isTrident) {
				tridentEvents++;
				chargedTracksPlot[TRIDENT].fill(tracks);
				clusterCountPlot[TRIDENT].fill(clusters.size());
				
				Integer val = tridentBitMap.get(bitString);
				if(val == null) { tridentBitMap.put(bitString, 1); }
				else { tridentBitMap.put(bitString, val + 1); }
			} else if(isElastic) {
				elasticEvents++;
				chargedTracksPlot[ELASTIC].fill(tracks);
				clusterCountPlot[ELASTIC].fill(clusters.size());
				
				Integer val = elasticBitMap.get(bitString);
				if(val == null) { elasticBitMap.put(bitString, 1); }
				else { elasticBitMap.put(bitString, val + 1); }
			}
			totalEvents++;
		}
	}
	
	public void setTimeCoincidenceCut(double value) {
		timeCoincidenceCut = value;
	}
	
	public void setExcludeNoTrackEvents(boolean state) {
		excludeNoTrackEvents = state;
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
