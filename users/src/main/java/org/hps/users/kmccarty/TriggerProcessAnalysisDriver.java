package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TriggerProcessAnalysisDriver extends Driver {
	private int eventsProcessed = 0;
	private int møllersProcessed = 0;
	private int tridentsProcessed = 0;
	private int gblMøllersProcessed = 0;
	private int gblTridentsProcessed = 0;
	private double timeCoincidence = 2.5;
	private AIDA aida = AIDA.defaultInstance();
	private String clusterCollectionName = "EcalClustersCorr";
	private String particleCollectionName = "FinalStateParticles";
	
	// Define trident cluster-track matched condition plots.
	private IHistogram1D trctmInvariantMass = aida.histogram1D("Tridents CTMatched/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D trctmInstancesInEvent = aida.histogram1D("Tridents CTMatched/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D trctmEnergySum1D = aida.histogram1D("Tridents CTMatched/Cluster Energy Sum", 150, 0.000, 1.500);
	private IHistogram1D trctmMomentumSum1D = aida.histogram1D("Tridents CTMatched/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D trctmElectronEnergy = aida.histogram1D("Tridents CTMatched/Electron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trctmElectronMomentum = aida.histogram1D("Tridents CTMatched/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trctmPositronEnergy = aida.histogram1D("Tridents CTMatched/Positron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D trctmPositronMomentum = aida.histogram1D("Tridents CTMatched/Positron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trctmTimeCoincidence = aida.histogram1D("Tridents CTMatched/Time Coincidence", 100, -4, 4);
	private IHistogram2D trctmClusterPosition = aida.histogram2D("Tridents CTMatched/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D trctmEnergySum2D = aida.histogram2D("Tridents CTMatched/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trctmMomentumSum2D = aida.histogram2D("Tridents CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trctmESumCoplanarity = aida.histogram2D("Tridents CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram2D trctmPSumCoplanarity = aida.histogram2D("Tridents CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the Møller cluster-track matched condition plots.
	private IHistogram1D møctmInvariantMass = aida.histogram1D("Møller CTMatched/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D møctmInstancesInEvent = aida.histogram1D("Møller CTMatched/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D møctmEnergySum1D = aida.histogram1D("Møller CTMatched/Cluster Energy Sum", 150, 0.000, 1.500);
	private IHistogram1D møctmMomentumSum1D = aida.histogram1D("Møller CTMatched/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D møctmElectronEnergy = aida.histogram1D("Møller CTMatched/Electron Cluster Energy", 150, 0.000, 1.500);
	private IHistogram1D møctmElectronMomentum = aida.histogram1D("Møller CTMatched/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D møctmTimeCoincidence = aida.histogram1D("Møller CTMatched/Time Coincidence", 100, -4, 4);
	private IHistogram2D møctmClusterPosition = aida.histogram2D("Møller CTMatched/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D møctmEnergySum2D = aida.histogram2D("Møller CTMatched/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møctmMomentumSum2D = aida.histogram2D("Møller CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møctmESumCoplanarity = aida.histogram2D("Møller CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	private IHistogram2D møctmPSumCoplanarity = aida.histogram2D("Møller CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the Møller track-only condition plots.
	private IHistogram1D møgblTimeCoincidence = aida.histogram1D("Møller Track-Only/Time Coincidence", 100, -4, 4);
	private IHistogram1D møgblInvariantMass = aida.histogram1D("Møller Track-Only/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D møgblInstancesInEvent = aida.histogram1D("Møller Track-Only/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D møgblMomentumSum1D = aida.histogram1D("Møller Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D møgblElectronMomentum = aida.histogram1D("Møller Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram2D møgblClusterPosition = aida.histogram2D("Møller Track-Only/Extrapolated Track Position", 138, -23, 23, 33, -5.5, 5.5);
	private IHistogram2D møgblMomentumSum2D = aida.histogram2D("Møller Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D møgblPSumCoplanarity = aida.histogram2D("Møller Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	// Define the GBL trident condition plots.
	private IHistogram1D trgblInvariantMass = aida.histogram1D("Tridents Track-Only/Invariant Mass", 140, 0.0, 0.070);
	private IHistogram1D trgblInstancesInEvent = aida.histogram1D("Tridents Track-Only/Instances in Event", 9, 0.5, 9.5);
	private IHistogram1D trgblMomentumSum1D = aida.histogram1D("Tridents Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
	private IHistogram1D trgblElectronMomentum = aida.histogram1D("Tridents Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trgblPositronMomentum = aida.histogram1D("Tridents Track-Only/Positron Track Momentum", 150, 0.000, 1.500);
	private IHistogram1D trgblTimeCoincidence = aida.histogram1D("Tridents Track-Only/Time Coincidence", 100, -4, 4);
	private IHistogram2D trgblClusterPosition = aida.histogram2D("Tridents Track-Only/Extrapolated Track Position", 46, -23, 23, 11, -5.5, 5.5);
	private IHistogram2D trgblMomentumSum2D = aida.histogram2D("Tridents Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
	private IHistogram2D trgblPSumCoplanarity = aida.histogram2D("Tridents Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
	
	@Override
	public void endOfData() {
		// Calculate the scaling factor for Hertz.
		double scale = 19000.0 / eventsProcessed;
		
		System.out.println("Processed " + eventsProcessed + " events.");
		System.out.println("Processed " + møllersProcessed + " Møller events");
		System.out.println("\tAcceptance :: " + (100.0 * møllersProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (møllersProcessed * scale) + " Hz");
		
		System.out.println("Processed " + tridentsProcessed + " trident events");
		System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
		
		System.out.println("Processed " + gblMøllersProcessed + " track-only Møller events");
		System.out.println("\tAcceptance :: " + (100.0 * gblMøllersProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (gblMøllersProcessed * scale) + " Hz");
		
		System.out.println("Processed " + gblTridentsProcessed + " Rafo trident events");
		System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
		System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
		
		// Scale the cluster-track matched Møller plots.
		møctmInvariantMass.scale(scale);
		møctmInstancesInEvent.scale(scale);
		møctmEnergySum1D.scale(scale);
		møctmMomentumSum1D.scale(scale);
		møctmElectronEnergy.scale(scale);
		møctmElectronMomentum.scale(scale);
		møctmTimeCoincidence.scale(scale);
		møctmClusterPosition.scale(scale);
		møctmEnergySum2D.scale(scale);
		møctmMomentumSum2D.scale(scale);
		
		// Scale the cluster-track matched trident plots.
		trctmInvariantMass.scale(scale);
		trctmInstancesInEvent.scale(scale);
		trctmEnergySum1D.scale(scale);
		trctmMomentumSum1D.scale(scale);
		trctmElectronEnergy.scale(scale);
		trctmElectronMomentum.scale(scale);
		trctmPositronEnergy.scale(scale);
		trctmPositronMomentum.scale(scale);
		trctmTimeCoincidence.scale(scale);
		trctmClusterPosition.scale(scale);
		trctmEnergySum2D.scale(scale);
		trctmMomentumSum2D.scale(scale);
	}
	
	@Override
	public void process(EventHeader event) {
		// Check whether the SVT was active in this event and, if so,
		// skip it.
		final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
		boolean svtGood = true;
        for(int i = 0; i < flagNames.length; i++) {
            int[] flag = event.getIntegerParameters().get(flagNames[i]);
            if(flag == null || flag[0] == 0) {
                svtGood = false;
            }
        }
        if(!svtGood) { return; }
        
        // Track the number of events with good SVT.
        eventsProcessed++;
        
		// Check if the event has a collection of tracks. If it exists,
        // extract it. Otherwise, skip the event.
		if(!event.hasCollection(ReconstructedParticle.class, particleCollectionName)) {
			return;
		}
		List<ReconstructedParticle> trackList = event.get(ReconstructedParticle.class, particleCollectionName);
		
		// Check if the event has a collection of clusters. If it
		// exists, extract it. Otherwise, skip the event.
		if(!event.hasCollection(Cluster.class, clusterCollectionName)) {
			return;
		}
		List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
		
		// Get cluster-track matched top/bottom pairs.
		List<ReconstructedParticle[]> gblMatchedPairs = getTopBottomTracksGBL(trackList);
		List<ReconstructedParticle[]> ctMatchedPairs  = getTopBottomTracksCTMatched(trackList);
		
		// Get the trident and Møller tracks for the matched track
		// and cluster pair condition sets.
		List<ReconstructedParticle[]> møllers     = getMøllerTracksCTMatched(ctMatchedPairs);
		List<ReconstructedParticle[]> møllersGBL  = getMøllerTracksGBL(gblMatchedPairs, event);
		List<ReconstructedParticle[]> tridents    = getTridentTracksCTMatched(ctMatchedPairs);
		List<ReconstructedParticle[]> tridentsGBL = getTridentClustersGBL(gblMatchedPairs, TriggerModule.getTopBottomPairs(clusterList, Cluster.class), event);
		
		// Track how many events had tridents and Møllers.
		if(!møllers.isEmpty()) { møllersProcessed++; }
		if(!tridents.isEmpty()) { tridentsProcessed++; }
		if(!møllersGBL.isEmpty()) { gblMøllersProcessed++; }
		if(!tridentsGBL.isEmpty()) { gblTridentsProcessed++; }
		
		// Produce Møller cluster-track matched plots.
		møctmInstancesInEvent.fill(møllers.size());
		for(ReconstructedParticle[] pair : møllers) {
			// Get the track clusters.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			
			// Populate the cluster plots.
			møctmElectronEnergy.fill(trackClusters[0].getEnergy());
			møctmElectronEnergy.fill(trackClusters[1].getEnergy());
			møctmEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
			møctmEnergySum2D.fill(trackClusters[0].getEnergy(), trackClusters[1].getEnergy());
			møctmESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
			møctmTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
			møctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
			møctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
			
			// Populate the momentum plots.
			møctmInvariantMass.fill(getInvariantMass(pair));
			møctmElectronMomentum.fill(pair[0].getMomentum().magnitude());
			møctmElectronMomentum.fill(pair[1].getMomentum().magnitude());
			møctmMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			møctmMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			møctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce trident cluster-track matched plots.
		trctmInstancesInEvent.fill(tridents.size());
		for(ReconstructedParticle[] pair : tridents) {
			// Get the electron and positron tracks.
			ReconstructedParticle electronTrack = pair[pair[0].getCharge() < 0 ? 0 : 1];
			ReconstructedParticle positronTrack = pair[pair[0].getCharge() > 0 ? 0 : 1];
			
			// Get the track clusters.
			Cluster electronCluster = electronTrack.getClusters().get(0);
			Cluster positronCluster = positronTrack.getClusters().get(0);
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			
			// Populate the cluster plots.
			trctmElectronEnergy.fill(electronCluster.getEnergy());
			trctmPositronEnergy.fill(positronCluster.getEnergy());
			trctmEnergySum2D.fill(pair[0].getEnergy(), pair[1].getEnergy());
			trctmEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
			trctmESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
			trctmTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
			trctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
			trctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
			
			// Populate the momentum plots.
			trctmInvariantMass.fill(getInvariantMass(pair));
			trctmElectronMomentum.fill(electronTrack.getMomentum().magnitude());
			trctmPositronMomentum.fill(positronTrack.getMomentum().magnitude());
			trctmMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			trctmMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			trctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce the Møller track-only plots.
		møgblInstancesInEvent.fill(møllersGBL.size());
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		for(ReconstructedParticle pair[] : møllersGBL) {
			// Get the tracks and track times.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			double times[] = {
					TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
					TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)	
			};
			
			// Fill the plots.
			møgblTimeCoincidence.fill(times[0] - times[1]);
			møgblInvariantMass.fill(getInvariantMass(pair));
			møgblElectronMomentum.fill(pair[0].getMomentum().magnitude());
			møgblElectronMomentum.fill(pair[1].getMomentum().magnitude());
			møgblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			møgblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			møgblClusterPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			møgblClusterPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			møgblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
		
		// Produce track-only trident plots.
		trgblInstancesInEvent.fill(tridentsGBL.size());
		for(ReconstructedParticle[] pair : tridentsGBL) {
			// Get the tracks and track times.
			Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
			double times[] = {
					TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
					TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)	
			};
			
			// Fill the plots.
			trgblTimeCoincidence.fill(times[0] - times[1]);
			trgblInvariantMass.fill(getInvariantMass(pair));
			trgblElectronMomentum.fill(pair[0].getMomentum().magnitude());
			trgblElectronMomentum.fill(pair[1].getMomentum().magnitude());
			trgblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
			trgblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
			trgblClusterPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
			trgblClusterPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
			trgblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
					getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
		}
	}
	
	/**
	 * Gets a list of all possible GBL top/bottom track pairs. These
	 * tracks are not guaranteed to have a matched cluster.
	 * @param trackList - A list of all possible tracks.
	 * @return Returns a list of track pairs.
	 */
	private static final List<ReconstructedParticle[]> getTopBottomTracksGBL(List<ReconstructedParticle> trackList) {
		// Separate the tracks into top and bottom tracks based on
		// the value of tan(Λ). Use only GBL tracks to avoid track
		// duplication.
		List<ReconstructedParticle> topTracks = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> botTracks = new ArrayList<ReconstructedParticle>();
		trackLoop:
		for(ReconstructedParticle track : trackList) {
			// Require that the ReconstructedParticle contain an actual
			// Track object.
			if(track.getTracks().isEmpty()) {
				continue trackLoop;
			}
			
			// Ignore tracks that are not GBL tracks.
			if(!TrackType.isGBL(track.getType())) {
				continue trackLoop;
			}
			
			// If the above tests pass, the ReconstructedParticle has
			// a track and is also a GBL track. Separate it into either
			// a top or a bottom track based on its tan(Λ) value.
			if(track.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0) {
				topTracks.add(track);
			} else {
				botTracks.add(track);
			}
		}
		
		// Form all top/bottom pairs with the unique tracks.
		List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
		for(ReconstructedParticle topTrack : topTracks) {
			for(ReconstructedParticle botTrack : botTracks) {
				pairList.add(new ReconstructedParticle[] { topTrack, botTrack });
			}
		}
		
		// Return the result.
		return pairList;
	}
	
	/**
	 * Produces pairs of tracks. The track pairs are required to be
	 * matched to a cluster and the associated clusters must form a
	 * top/bottom pair. If more than one track points to the same
	 * cluster, only the first track is retained.
	 * @param trackList - A list of all tracks.
	 * @return Returns a list of track pairs meeting the aforementioned
	 * conditions.
	 */
	private static final List<ReconstructedParticle[]> getTopBottomTracksCTMatched(List<ReconstructedParticle> trackList) {
		// Track clusters that have already been seen to prevent clusters
		// that have duplicate tracks from reappearing.
		Set<Cluster> clusterSet = new HashSet<Cluster>();
		
		// Separate the tracks into top and bottom tracks based on
		// the track cluster. Filter out tracks with no clusters.
		List<ReconstructedParticle> topTracks = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> botTracks = new ArrayList<ReconstructedParticle>();
		trackLoop:
		for(ReconstructedParticle track : trackList) {
			// Check if the track has a cluster. If not, skip it.
			if(track.getClusters().isEmpty()) {
				continue trackLoop;
			}
			
			// If the track doesn't have actual tracks, skip it.
			if(track.getTracks().isEmpty()) {
				continue trackLoop;
			}
			
			// Check if the track cluster has already seen.
			Cluster trackCluster = track.getClusters().get(0);
			if(clusterSet.contains(trackCluster)) {
				continue trackLoop;
			}
			
			// If the track has a unique cluster, add it to the proper
			// list based on the cluster y-index.
			clusterSet.add(trackCluster);
			if(TriggerModule.getClusterYIndex(trackCluster) > 0) {
				topTracks.add(track);
			} else {
				botTracks.add(track);
			}
		}
		
		// Form all top/bottom pairs with the unique tracks.
		List<ReconstructedParticle[]> pairList = new ArrayList<ReconstructedParticle[]>();
		for(ReconstructedParticle topTrack : topTracks) {
			for(ReconstructedParticle botTrack : botTracks) {
				pairList.add(new ReconstructedParticle[] { topTrack, botTrack });
			}
		}
		
		// Return the result.
		return pairList;
	}
	
	private final List<ReconstructedParticle[]> getTridentClustersGBL(List<ReconstructedParticle[]> pairList, List<Cluster[]> clusterList, EventHeader event) {
		// Store the set of track pairs that meet the trident condition.
		List<ReconstructedParticle[]> tridentTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Extract track relational tables from the event object.
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		
		// Tracks will not be considered for trident analysis unless there
		// is at least one top/bottom cluster pair within the time window.
		boolean passesClusterCondition = false;
		tridentClusterLoop:
		for(Cluster[] pair : clusterList) {
			// Ignore clusters that are too far apart temporally.
			if(TriggerModule.getValueTimeCoincidence(pair) > timeCoincidence) {
				continue tridentClusterLoop;
			}
			
			// Require that the cluster pair be top/bottom.
			boolean hasTop = TriggerModule.getClusterYIndex(pair[0]) > 0 || TriggerModule.getClusterYIndex(pair[1]) > 0;
			boolean hasBot = TriggerModule.getClusterYIndex(pair[0]) < 0 || TriggerModule.getClusterYIndex(pair[1]) < 0;
			if(!hasTop || !hasBot) {
				continue tridentClusterLoop;
			}
			
			// If the cluster passes, mark that it has done so and skip
			// the rest. Only one pair need pass.
			passesClusterCondition = true;
			break tridentClusterLoop;
		}
		
		// If no cluster pair passed the cluster condition, no tracks
		// are allowed to pass either.
		if(!passesClusterCondition) {
			return tridentTracks;
		}
		
		// Next, check the track pair list. A track pair must have a
		// positive and a negative track and must also be within the
		// time coincidence window.
		tridentTrackLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Check that there is at least one positive and one negative
			// track in the pair.
			boolean hasPositive = pair[0].getCharge() > 0 || pair[1].getCharge() > 0;
			boolean hasNegative = pair[0].getCharge() < 0 || pair[1].getCharge() < 0;
			if(hasPositive && hasNegative) {
				break tridentTrackLoop;
			}
			
			// Check that the track pair passes the time cut.
			double times[] = {
				TrackUtils.getTrackTime(pair[0].getTracks().get(0), hitToStrips, hitToRotated),
				TrackUtils.getTrackTime(pair[1].getTracks().get(0), hitToStrips, hitToRotated)	
			};
			
			if(Math.abs(times[0] - times[1]) > timeCoincidence) {
				continue tridentTrackLoop;
			}
			
			// Require that the negative track have less than 900 MeV
			// momentum to exclude elastic electrons.
			if(pair[0].getCharge() < 0 && pair[0].getMomentum().magnitude() > 0.900
					|| pair[1].getCharge() < 0 && pair[1].getMomentum().magnitude() > 0.900) {
				continue tridentTrackLoop;
			}
			
			// If the track passes both, it is considered a trident pair.
			tridentTracks.add(pair);
		}
		
		// Return the resultant pairs.
		return tridentTracks;
	}
	
	/**
	 * Gets a list track pairs that meet the trident condition defined
	 * using tracks with matched calorimeter clusters. A pair meets the
	 * cluster/track matched trident condition is it meets the following:
	 * <ul><li>Both tracks have matched clusters.</li>
	 * <li>Has one positive track.</li>
	 * <li>Has one negative track.</li>
	 * <li>Clusters have a time coincidence of 2.5 ns or less.</li>
	 * <li>The electron momentum is below 900 MeV.</li></ul>
	 * @param pairList - A <code>List</code> collection of parameterized
	 * type <code>ReconstructedParticle[]</code> containing all valid
	 * top/bottom pairs of tracks with matched clusters. These will be
	 * tested to see if they meet the process criteria.
	 * @return Returns a list containing pairs of tracks that meet the
	 * trident condition.
	 */
	private final List<ReconstructedParticle[]> getTridentTracksCTMatched(List<ReconstructedParticle[]> pairList) {
		// Store the set of track pairs that meet the trident condition.
		List<ReconstructedParticle[]> tridentTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the filtered pair list and apply the trident
		// condition test.
		tridentLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// There must be one positive and one negative track.
			ReconstructedParticle electron = null;
			ReconstructedParticle positron = null;
			if(pair[0].getCharge() > 0) { positron = pair[0]; }
			else if(pair[1].getCharge() > 0) { positron = pair[1]; }
			if(pair[0].getCharge() < 0) { electron = pair[0]; }
			else if(pair[1].getCharge() < 0) { electron = pair[1]; }
			if(electron == null || positron == null) {
				continue tridentLoop;
			}
			
			// Make sure that the clusters are not the same. This should
			// not actually ever be possible...
			if(pair[0].getClusters().get(0) == pair[1].getClusters().get(0)) {
				continue tridentLoop;
			}
			
			// The clusters must within a limited time window.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue tridentLoop;
			}
			
			// Require that the electron in the pair have an energy
			// below 900 MeV to exclude elastic electrons.
			if(electron.getMomentum().magnitude() >= 0.900) {
				continue tridentLoop;
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			tridentTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return tridentTracks;
	}
	
	private final List<ReconstructedParticle[]> getMøllerTracksGBL(List<ReconstructedParticle[]> pairList, EventHeader event) {
		// Store the set of track pairs that meet the Møller condition.
		List<ReconstructedParticle[]> møllerTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Extract track relational tables from the event object.
		RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
		RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
		
		// Loop over the filtered pair list and apply the Møller
		// condition test.
		møllerLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Both tracks must be negatively charged.
			if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
				continue møllerLoop;
			}
			
			// The clusters must within a limited time window.
			double times[] = {
				TrackUtils.getTrackTime(pair[0].getTracks().get(0), hitToStrips, hitToRotated),
				TrackUtils.getTrackTime(pair[1].getTracks().get(0), hitToStrips, hitToRotated)	
			};
			
			if(Math.abs(times[0] - times[1]) > timeCoincidence) {
				continue møllerLoop;
			}
			
			// Require that the electrons in the pair have energies
			// below 900 MeV to exclude elastic electrons.
			if(pair[0].getMomentum().magnitude() > 0.900 || pair[1].getMomentum().magnitude() > 0.900) {
				continue møllerLoop;
			}
			
			// Require that the energy of the pair be within a range
			// that is sufficiently "Møller-like."
			double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
			if(momentumSum < 0.800 || momentumSum > 1.500) {
				continue møllerLoop;
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			møllerTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return møllerTracks;
	}
	
	/**
	 * Gets a list track pairs that meet the Møller condition defined
	 * using tracks with matched calorimeter clusters. A pair meets the
	 * cluster/track matched Møller condition is it meets the following:
	 * <ul><li>Both tracks have matched clusters.</li>
	 * <li>Both tracks are negative.</li>
	 * <li>Clusters have a time coincidence of 2.5 ns or less.</li>
	 * <li>The electron momenta are below 900 MeV.</li>
	 * <li>The momentum sum of the tracks is in the range <code>800 MeV
	 * ≤ p1 + p2 ≤ 1500 MeV</li></ul>
	 * @param pairList - A <code>List</code> collection of parameterized
	 * type <code>ReconstructedParticle[]</code> containing all valid
	 * top/bottom pairs of tracks with matched clusters. These will be
	 * tested to see if they meet the process criteria.
	 * @return Returns a list containing pairs of tracks that meet the
	 * Møller condition.
	 */
	private final List<ReconstructedParticle[]> getMøllerTracksCTMatched(List<ReconstructedParticle[]> pairList) {
		// Store the set of track pairs that meet the Møller condition.
		List<ReconstructedParticle[]> møllerTracks = new ArrayList<ReconstructedParticle[]>();
		
		// Loop over the filtered pair list and apply the Møller
		// condition test.
		møllerLoop:
		for(ReconstructedParticle[] pair : pairList) {
			// Both tracks must be negatively charged.
			if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
				continue møllerLoop;
			}
			
			// The clusters must within a limited time window.
			Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
			if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
				continue møllerLoop;
			}
			
			// Require that the electrons in the pair have energies
			// below 900 MeV to exclude elastic electrons.
			if(pair[0].getMomentum().magnitude() > 0.900 || pair[1].getMomentum().magnitude() > 0.900) {
				continue møllerLoop;
			}
			
			// Require that the energy of the pair be within a range
			// that is sufficiently "Møller-like."
			double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
			if(momentumSum < 0.800 || momentumSum > 1.500) {
				continue møllerLoop;
			}
			
			// If all the above conditions are met, the pair is to be
			// considered a trident pair. Add it to the list.
			møllerTracks.add(pair);
		}
		
		// Return the list of pairs that passed the condition.
		return møllerTracks;
	}
	
	/**
	 * Calculates the approximate invariant mass for a pair of tracks
	 * from their momentum. This assumes that the particles are either
	 * electrons or positrons, and thusly have a sufficiently small
	 * mass term that it can be safely excluded.
	 * @param pair - The track pair for which to calculate the invariant
	 * mass.
	 * @return Returns the approximate invariant mass in units of GeV.
	 */
	private static final double getInvariantMass(ReconstructedParticle[] pair) {
		// Get the momentum squared.
		double p2 = Math.pow(pair[0].getMomentum().magnitude() + pair[1].getMomentum().magnitude(), 2);
		
		// Get the remaining terms.
		double xPro = pair[0].getMomentum().x() + pair[1].getMomentum().x();
		double yPro = pair[0].getMomentum().y() + pair[1].getMomentum().y();
		double zPro = pair[0].getMomentum().z() + pair[1].getMomentum().z();
		
		// Calculate the invariant mass.
		return Math.sqrt(p2 - Math.pow(xPro, 2) - Math.pow(yPro, 2) - Math.pow(zPro, 2));
	}
	
	/**
	 * Calculates the coplanarity angle between two points, specified
	 * by a double array. The array must be of the format (x, y, z).
	 * @param position - The first position array.
	 * @param otherPosition - The second position array.
	 * @return Returns the coplanarity angle between the points in units
	 * of degrees.
	 */
	private static final double getCalculatedCoplanarity(double[] position, double[] otherPosition) {
		// Define the x- and y-coordinates of the clusters as well as
		// calorimeter center.
		final double ORIGIN_X = 42.52;
		double x[] = { position[0], otherPosition[0] };
		double y[] = { position[1], otherPosition[1] };
		
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
	
	/**
	 * Calculates the coplanarity angle of a pair of clusters.
	 * @param pair - The pair of clusters for which to calculate the
	 * coplanarity angle.
	 * @return Returns the coplanarity angle between the two clusters
	 * in degrees.
	 */
	private static final double getCalculatedCoplanarity(Cluster[] pair) {
		return getCalculatedCoplanarity(pair[0].getPosition(), pair[1].getPosition());
	}
	
	/**
	 * Calculates the coplanarity angle of a pair of tracks. The track
	 * is extrapolated to the calorimeter face and its position there
	 * used for the arguments in the calculation.
	 * @param pair - The pair of tracks for which to calculate the
	 * coplanarity angle.
	 * @return Returns the coplanarity angle between the two tracks
	 * in degrees.
	 */
	private static final double getCalculatedCoplanarity(Track[] pair) {
		return getCalculatedCoplanarity(TrackUtils.getTrackPositionAtEcal(pair[0]).v(), TrackUtils.getTrackPositionAtEcal(pair[1]).v());
	}
}
// shawna.hollen@unh.edu