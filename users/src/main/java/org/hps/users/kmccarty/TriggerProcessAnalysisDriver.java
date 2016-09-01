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
    private int mollersProcessed = 0; 
    private boolean checkSVT = false;
    private int tridentsProcessed = 0;
    private int gblMollersProcessed = 0;
    private int gblTridentsProcessed = 0;
    private double timeCoincidence = 2.5;
    private double elasticThreshold = 0.800;
    private double mollerLowerRange = 0.900;
    private double mollerUpperRange = 1.200;
    private AIDA aida = AIDA.defaultInstance();
    private boolean checkTriggerTimeWindow = false;
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
    private IHistogram2D trctmTrackPosition = aida.histogram2D("Tridents CTMatched/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
    private IHistogram2D trctmMomentumSum2D = aida.histogram2D("Tridents CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
    private IHistogram2D trctmESumCoplanarity = aida.histogram2D("Tridents CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    private IHistogram2D trctmPSumCoplanarity = aida.histogram2D("Tridents CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    
    // Define the Moller cluster-track matched condition plots.
    private IHistogram1D moctmInvariantMass = aida.histogram1D("Moller CTMatched/Invariant Mass", 140, 0.0, 0.070);
    private IHistogram1D moctmInstancesInEvent = aida.histogram1D("Moller CTMatched/Instances in Event", 9, 0.5, 9.5);
    private IHistogram1D moctmEnergySum1D = aida.histogram1D("Moller CTMatched/Cluster Energy Sum", 150, 0.000, 1.500);
    private IHistogram1D moctmMomentumSum1D = aida.histogram1D("Moller CTMatched/Track Momentum Sum", 150, 0.000, 1.500);
    private IHistogram1D moctmElectronEnergy = aida.histogram1D("Moller CTMatched/Electron Cluster Energy", 150, 0.000, 1.500);
    private IHistogram1D moctmElectronMomentum = aida.histogram1D("Moller CTMatched/Electron Track Momentum", 150, 0.000, 1.500);
    private IHistogram1D moctmTimeCoincidence = aida.histogram1D("Moller CTMatched/Time Coincidence", 100, -4, 4);
    private IHistogram2D moctmClusterPosition = aida.histogram2D("Moller CTMatched/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
    private IHistogram2D moctmEnergySum2D = aida.histogram2D("Moller CTMatched/Cluster Energy Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
    private IHistogram2D moctmTrackPosition = aida.histogram2D("Moller CTMatched/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
    private IHistogram2D moctmMomentumSum2D = aida.histogram2D("Moller CTMatched/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
    private IHistogram2D moctmESumCoplanarity = aida.histogram2D("Moller CTMatched/Cluster Energy Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    private IHistogram2D moctmPSumCoplanarity = aida.histogram2D("Moller CTMatched/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    
    // Define the Moller track-only condition plots.
    private IHistogram1D mogblTimeCoincidence = aida.histogram1D("Moller Track-Only/Time Coincidence", 100, -4, 4);
    private IHistogram1D mogblInvariantMass = aida.histogram1D("Moller Track-Only/Invariant Mass", 140, 0.0, 0.070);
    private IHistogram1D mogblInstancesInEvent = aida.histogram1D("Moller Track-Only/Instances in Event", 9, 0.5, 9.5);
    private IHistogram1D mogblMomentumSum1D = aida.histogram1D("Moller Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
    private IHistogram1D mogblElectronMomentum = aida.histogram1D("Moller Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
    private IHistogram2D mogblTrackPosition = aida.histogram2D("Moller Track-Only/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
    private IHistogram2D mogblMomentumSum2D = aida.histogram2D("Moller Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
    private IHistogram2D mogblPSumCoplanarity = aida.histogram2D("Moller Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    
    // Define the GBL trident condition plots.
    private IHistogram1D trgblInvariantMass = aida.histogram1D("Tridents Track-Only/Invariant Mass", 140, 0.0, 0.070);
    private IHistogram1D trgblInstancesInEvent = aida.histogram1D("Tridents Track-Only/Instances in Event", 9, 0.5, 9.5);
    private IHistogram1D trgblMomentumSum1D = aida.histogram1D("Tridents Track-Only/Track Momentum Sum", 150, 0.000, 1.500);
    private IHistogram1D trgblElectronMomentum = aida.histogram1D("Tridents Track-Only/Electron Track Momentum", 150, 0.000, 1.500);
    private IHistogram1D trgblPositronMomentum = aida.histogram1D("Tridents Track-Only/Positron Track Momentum", 150, 0.000, 1.500);
    private IHistogram1D trgblTimeCoincidence = aida.histogram1D("Tridents Track-Only/Time Coincidence", 100, -4, 4);
    private IHistogram2D trgblTrackPosition = aida.histogram2D("Tridents Track-Only/Extrapolated Track Position", 200, -400, 400, 55, -110, 110);
    private IHistogram2D trgblMomentumSum2D = aida.histogram2D("Tridents Track-Only/Track Momentum Sum 2D", 300, 0.000, 1.500, 300, 0.000, 1.500);
    private IHistogram2D trgblPSumCoplanarity = aida.histogram2D("Tridents Track-Only/Track Momentum Sum vs. Coplanarity", 300, 0.000, 1.500, 360, 0, 360);
    
    @Override
    public void endOfData() {
        // Calculate the scaling factor for Hertz.
        double scale = 19000.0 / eventsProcessed;
        
        System.out.println("Processed " + eventsProcessed + " events.");
        System.out.println("Processed " + mollersProcessed + " Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * mollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (mollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + tridentsProcessed + " trident events");
        System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblMollersProcessed + " track-only Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * gblMollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblMollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblTridentsProcessed + " Rafo trident events");
        System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
    }
    
/*
    @Override
    public void process(EventHeader event) {
        // Check whether the SVT was active in this event and, if so,
        // skip it. This can be disabled through the steering file for
        // Monte Carlo data, where the "SVT" is always active.
        if(checkSVT) {
            final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
            boolean svtGood = true;
            for(int i = 0; i < flagNames.length; i++) {
                int[] flag = event.getIntegerParameters().get(flagNames[i]);
                if(flag == null || flag[0] == 0) {
                    svtGood = false;
                }
            }
            if(!svtGood) { return; }
        }
        
        System.out.println("Processed " + eventsProcessed + " events.");
        System.out.println("Processed " + mollersProcessed + " Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * mollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (mollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + tridentsProcessed + " trident events");
        System.out.println("\tAcceptance :: " + (100.0 * tridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (tridentsProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblMollersProcessed + " track-only Moller events");
        System.out.println("\tAcceptance :: " + (100.0 * gblMollersProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblMollersProcessed * scale) + " Hz");
        
        System.out.println("Processed " + gblTridentsProcessed + " Rafo trident events");
        System.out.println("\tAcceptance :: " + (100.0 * gblTridentsProcessed / eventsProcessed) + "%");
        System.out.println("\tRate       :: " + (gblTridentsProcessed * scale) + " Hz");
    }
    */
    
    @Override
    public void process(EventHeader event) {
        // Check whether the SVT was active in this event and, if so,
        // skip it. This can be disabled through the steering file for
        // Monte Carlo data, where the "SVT" is always active.
        if(checkSVT) {
            final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
            boolean svtGood = true;
            for(int i = 0; i < flagNames.length; i++) {
                int[] flag = event.getIntegerParameters().get(flagNames[i]);
                if(flag == null || flag[0] == 0) {
                    svtGood = false;
                }
            }
            if(!svtGood) { return; }
        }
        
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
        
        System.out.println("CTM Pairs :: " + ctMatchedPairs.size());
        System.out.println("GBL Pairs :: " + gblMatchedPairs.size());
        
        // Get the trident and Moller tracks for the matched track
        // and cluster pair condition sets.
        List<ReconstructedParticle[]> mollers     = getMollerTracksCTMatched(ctMatchedPairs);
        List<ReconstructedParticle[]> mollersGBL  = getMollerTracksGBL(gblMatchedPairs, event);
        List<ReconstructedParticle[]> tridents    = getTridentTracksCTMatched(ctMatchedPairs);
        List<ReconstructedParticle[]> tridentsGBL = getTridentClustersGBL(gblMatchedPairs, TriggerModule.getTopBottomPairs(clusterList, Cluster.class), event);
        
        // Track how many events had tridents and Mollers.
        if(!mollers.isEmpty()) { mollersProcessed++; }
        if(!tridents.isEmpty()) { tridentsProcessed++; }
        if(!mollersGBL.isEmpty()) { gblMollersProcessed++; }
        if(!tridentsGBL.isEmpty()) { gblTridentsProcessed++; }
        
        // Produce Moller cluster-track matched plots.
        moctmInstancesInEvent.fill(mollers.size());
        for(ReconstructedParticle[] pair : mollers) {
            // Get the track clusters.
            Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
            Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
            
            // Populate the cluster plots.
            moctmElectronEnergy.fill(trackClusters[0].getEnergy());
            moctmElectronEnergy.fill(trackClusters[1].getEnergy());
            moctmEnergySum1D.fill(TriggerModule.getValueEnergySum(trackClusters));
            moctmEnergySum2D.fill(trackClusters[0].getEnergy(), trackClusters[1].getEnergy());
            moctmESumCoplanarity.fill(TriggerModule.getValueEnergySum(trackClusters), getCalculatedCoplanarity(trackClusters));
            moctmTimeCoincidence.fill(TriggerModule.getClusterTime(trackClusters[0]) - TriggerModule.getClusterTime(trackClusters[1]));
            moctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[0]), TriggerModule.getClusterYIndex(trackClusters[0]));
            moctmClusterPosition.fill(TriggerModule.getClusterXIndex(trackClusters[1]), TriggerModule.getClusterYIndex(trackClusters[1]));
            
            // Populate the momentum plots.
            moctmInvariantMass.fill(getInvariantMass(pair));
            moctmElectronMomentum.fill(pair[0].getMomentum().magnitude());
            moctmElectronMomentum.fill(pair[1].getMomentum().magnitude());
            moctmMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
            moctmMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
            moctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
            moctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
            moctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
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
            Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
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
            trctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
            trctmTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
            trctmPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
                    getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
        }
        
        // Produce the Moller track-only plots.
        mogblInstancesInEvent.fill(mollersGBL.size());
        RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
        for(ReconstructedParticle pair[] : mollersGBL) {
            // Get the tracks and track times.
            Track[] tracks = { pair[0].getTracks().get(0), pair[1].getTracks().get(0) };
            double times[] = {
                    TrackUtils.getTrackTime(tracks[0], hitToStrips, hitToRotated),
                    TrackUtils.getTrackTime(tracks[1], hitToStrips, hitToRotated)    
            };
            
            // Fill the plots.
            mogblTimeCoincidence.fill(times[0] - times[1]);
            mogblInvariantMass.fill(getInvariantMass(pair));
            mogblElectronMomentum.fill(pair[0].getMomentum().magnitude());
            mogblElectronMomentum.fill(pair[1].getMomentum().magnitude());
            mogblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
            mogblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
            mogblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
            mogblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
            mogblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
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
            
            // Get the positron and the electron.
            ReconstructedParticle positron = pair[0].getCharge() > 0 ? pair[0] : pair[1];
            ReconstructedParticle electron = pair[0].getCharge() < 0 ? pair[0] : pair[1];
            
            // Fill the plots.
            trgblTimeCoincidence.fill(times[0] - times[1]);
            trgblInvariantMass.fill(getInvariantMass(pair));
            trgblElectronMomentum.fill(electron.getMomentum().magnitude());
            trgblPositronMomentum.fill(positron.getMomentum().magnitude());
            trgblMomentumSum1D.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude());
            trgblMomentumSum2D.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
            trgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[0]).x(), TrackUtils.getTrackPositionAtEcal(tracks[0]).y());
            trgblTrackPosition.fill(TrackUtils.getTrackPositionAtEcal(tracks[1]).x(), TrackUtils.getTrackPositionAtEcal(tracks[1]).y());
            trgblPSumCoplanarity.fill(VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude(),
                    getCalculatedCoplanarity(new Track[] { pair[0].getTracks().get(0), pair[1].getTracks().get(0) }));
        }
    }
    
    public void setCheckSVT(boolean state) {
        checkSVT = state;
    }
    
    public void setCheckTriggerTimeWindow(boolean state) {
        checkTriggerTimeWindow = state;
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
            if(!hasPositive || !hasNegative) {
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
            
            // Require that the negative track have less than the
            // elastic threshold momentum to exclude elastic electrons.
            if(pair[0].getCharge() < 0 && pair[0].getMomentum().magnitude() > elasticThreshold
                    || pair[1].getCharge() < 0 && pair[1].getMomentum().magnitude() > elasticThreshold) {
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
            /*
            Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
            if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
                continue tridentLoop;
            }
            */
            
            // The clusters must be coincidental within an energy
            // dependent coincidence window.
            Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
            if(!isCoincidental(trackClusters)) {
                continue tridentLoop;
            }
            
            // Require that the electron in the pair have an energy
            // below the elastic threshold to exclude elastic electrons.
            if(electron.getMomentum().magnitude() >= elasticThreshold) {
                continue tridentLoop;
            }
            
            // Require that all clusters occur within the trigger time
            // window to exclude accidentals.
            if(checkTriggerTimeWindow) {
                if(!inTriggerWindow(trackClusters[0]) || !inTriggerWindow(trackClusters[1])) {
                    continue tridentLoop;
                }
            }
            
            // If all the above conditions are met, the pair is to be
            // considered a trident pair. Add it to the list.
            tridentTracks.add(pair);
        }
        
        // Return the list of pairs that passed the condition.
        return tridentTracks;
    }
    
    private final List<ReconstructedParticle[]> getMollerTracksGBL(List<ReconstructedParticle[]> pairList, EventHeader event) {
        // Store the set of track pairs that meet the Moller condition.
        List<ReconstructedParticle[]> mollerTracks = new ArrayList<ReconstructedParticle[]>();
        
        // Extract track relational tables from the event object.
        RelationalTable<?, ?> hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable<?, ?> hitToRotated = TrackUtils.getHitToRotatedTable(event);
        
        // Loop over the filtered pair list and apply the Moller
        // condition test.
        mollerLoop:
        for(ReconstructedParticle[] pair : pairList) {
            // Both tracks must be negatively charged.
            if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
                continue mollerLoop;
            }
            
            // The clusters must within a limited time window.
            double times[] = {
                TrackUtils.getTrackTime(pair[0].getTracks().get(0), hitToStrips, hitToRotated),
                TrackUtils.getTrackTime(pair[1].getTracks().get(0), hitToStrips, hitToRotated)    
            };
            
            if(Math.abs(times[0] - times[1]) > timeCoincidence) {
                continue mollerLoop;
            }
            
            // Require that the electrons in the pair have energies
            // below the elastic threshold to exclude said electrons.
            if(pair[0].getMomentum().magnitude() > elasticThreshold || pair[1].getMomentum().magnitude() > elasticThreshold) {
                continue mollerLoop;
            }
            
            // Require that the energy of the pair be within a range
            // that is sufficiently "Moller-like."
            double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
            if(momentumSum < mollerLowerRange || momentumSum > mollerUpperRange) {
                continue mollerLoop;
            }
            
            // If all the above conditions are met, the pair is to be
            // considered a trident pair. Add it to the list.
            mollerTracks.add(pair);
        }
        
        // Return the list of pairs that passed the condition.
        return mollerTracks;
    }
    
    /**
     * Gets a list track pairs that meet the Moller condition defined
     * using tracks with matched calorimeter clusters. A pair meets the
     * cluster/track matched Moller condition is it meets the following:
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
     * Moller condition.
     */
    private final List<ReconstructedParticle[]> getMollerTracksCTMatched(List<ReconstructedParticle[]> pairList) {
        // Store the set of track pairs that meet the Moller condition.
        List<ReconstructedParticle[]> mollerTracks = new ArrayList<ReconstructedParticle[]>();
        
        // Loop over the filtered pair list and apply the Moller
        // condition test.
        mollerLoop:
        for(ReconstructedParticle[] pair : pairList) {
            // Both tracks must be negatively charged.
            if(pair[0].getCharge() > 0 || pair[1].getCharge() > 0) {
                continue mollerLoop;
            }
            
            // The clusters must within a limited time window.
            /*
            Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
            if(TriggerModule.getValueTimeCoincidence(trackClusters) > timeCoincidence) {
                continue mollerLoop;
            }
            */
            
            // The clusters must be coincidental within an energy
            // dependent coincidence window.
            Cluster[] trackClusters = { pair[0].getClusters().get(0), pair[1].getClusters().get(0) };
            if(!isCoincidental(trackClusters)) {
                continue mollerLoop;
            }
            
            // Require that the electrons in the pair have energies
            // below the elastic threshold to exclude said electrons.
            if(pair[0].getMomentum().magnitude() > elasticThreshold || pair[1].getMomentum().magnitude() > elasticThreshold) {
                continue mollerLoop;
            }
            
            // Require that the energy of the pair be within a range
            // that is sufficiently "Moller-like."
            double momentumSum = VecOp.add(pair[0].getMomentum(), pair[1].getMomentum()).magnitude();
            if(momentumSum < mollerLowerRange || momentumSum > mollerUpperRange) {
                continue mollerLoop;
            }
            
            // Require that all clusters occur within the trigger time
            // window to exclude accidentals.
            if(checkTriggerTimeWindow) {
                if(!inTriggerWindow(trackClusters[0]) || !inTriggerWindow(trackClusters[1])) {
                    continue mollerLoop;
                }
            }
            
            // If all the above conditions are met, the pair is to be
            // considered a trident pair. Add it to the list.
            mollerTracks.add(pair);
        }
        
        // Return the list of pairs that passed the condition.
        return mollerTracks;
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
    
    private static final boolean inTriggerWindow(Cluster cluster) {
        // Get the cluster time.
        double clusterTime = TriggerModule.getClusterTime(cluster);
        
        // Check that it is within the allowed bounds.
        return (35 <= clusterTime && clusterTime <= 50);
    }
    
    private static final boolean isCoincidental(Cluster[] pair) {
        // Get the energy sum and the time coincidence.
        double energySum = pair[0].getEnergy() + pair[1].getEnergy();
        double timeCoincidence = TriggerModule.getValueTimeCoincidence(pair);
        
        // Get the upper and lower bounds of the allowed range.
        double mean = getTimeDependenceMean(energySum);
        double threeSigma = 3.0 * getTimeDependenceSigma(energySum);
        double lowerBound = mean - threeSigma;
        double upperBound = mean + threeSigma;
        
        // Perform the time coincidence check.
        return (lowerBound <= timeCoincidence && timeCoincidence <= upperBound);
    }
    
    private static final double getTimeDependenceMean(double energySum) {
        // Define the fit parameters.
        double[] param = { 0.289337, -2.81998, 9.03475, -12.93, 8.71476, -2.26969 };
        
        // Calculate the mean.
        return param[0] + energySum * (param[1] + energySum * (param[2] + energySum * (param[3] + energySum * (param[4] + energySum * (param[5])))));
    }
    
    private static final double getTimeDependenceSigma(double energySum) {
        // Define the fit parameters.
        double[] param = { 4.3987, -24.2371, 68.9567, -98.2586, 67.562, -17.8987 };
        
        // Calculate the standard deviation.
        return param[0] + energySum * (param[1] + energySum * (param[2] + energySum * (param[3] + energySum * (param[4] + energySum * (param[5])))));
    }
}