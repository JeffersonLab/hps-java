package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.particle.SimpleParticleID;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;

/**
 * Driver to skim selected events from LCIO files based on 
 * track picking up the wrong hit.
 * It outputs the bad track, the MCParticle associated with the bad hit,
 * the truth hits associated with the truth match, and the track of the 
 * other particle associated with the bad hit (if any)
 * This driver can only be run on MC readout with full truth.
 *
 * @author Matt Solt
 *
 * @version $Id:
 */
public class IdentifyBadTracksDriver extends Driver{


    TrackClusterMatcher matcher = new TrackClusterMatcher();
    //Collection name
    //TODO Make these names configurable by steering file
    private final String trackColName = "GBLTracks";
    private final String badTrackColName = "GBLTracks_bad";
    private final String badMCParticleRelationsColName = "TrackBadToMCParticleBadRelations";
    private final String simhitOutputColName = "TrackerHits_truth";
    private final String trackBadToTruthMatchRelationsOutputColName = "TrackBadToMCParticleRelations";
    private final String trackToTruthMatchRelationsOutputColName = "TrackToMCParticleRelations";
    private final String otherParticleRelationsColName = "TrackBadToOtherParticleRelations";
    private final String otherParticleColName = "OtherReconParticle";
    private final String ecalClustersCollectionName = "EcalClusters";

  //List of Sensors
    private List<HpsSiSensor> sensors = null;
    FieldMap bFieldMap = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected static Subdetector trackerSubdet;
    // normalized cluster-track distance required for qualifying as a match:
    private double MAXNSIGMAPOSITIONMATCH=15.0;
    private boolean disablePID = false;
    protected double beamEnergy = 1.056;
    
    /** Disable setting the PID of an Ecal cluster. */
    public void setDisablePID(boolean disablePID) {
        this.disablePID = disablePID;
    }

    public void detectorChanged(Detector detector){
        
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
        
        trackerSubdet = detector.getSubdetector(SUBDETECTOR_NAME);
        
        matcher.setBFieldMap(detector.getFieldMap());
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        matcher.setBeamEnergy(beamEnergy); 
    }

    @Override
    protected void process(EventHeader event){
        List<Track> allTracks = event.get(Track.class, trackColName);
        List<Track> badTracks = new ArrayList<Track>();
        List<SimTrackerHit> truthHits = new ArrayList<SimTrackerHit>();
        List<MCFullDetectorTruth> truthMatchWithBadTrack = new ArrayList<MCFullDetectorTruth>();
        List<LCRelation> trackBadToTruthMatchRelations = new ArrayList<LCRelation>();
        List<LCRelation> trackToTruthMatchRelations = new ArrayList<LCRelation>();
        List<LCRelation> trackBadToBadMCParticleRelations = new ArrayList<LCRelation>();
        List<LCRelation> trackBadToBadReconParticleRelations = new ArrayList<LCRelation>();
        List<ReconstructedParticle> otherReconParticles = new ArrayList<ReconstructedParticle>();
        List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);
        //Loop over all tracks
        for(Track track:allTracks){
            //Match the track to a MC truth particle
            MCFullDetectorTruth truthMatch = new MCFullDetectorTruth(event, track, bFieldMap, sensors, trackerSubdet);
            if(truthMatch.getMCParticle() == null){
                continue;
            }
            //Add the truth match to track LC relation
            trackToTruthMatchRelations.add(new BaseLCRelation(track, truthMatch.getMCParticle()));
            
            //Check to see if the truth match contains a hit not associated with the truth particle
            //(i.e. the purity is less than 1.0)
            if((truthMatch.getPurity() == 1.0)){
                continue;
            }
            //Identify MCParticle responsible for bad hit
            MCParticle badPart = SelectBadMCParticle(truthMatch,track);
            Track badTrk = SelectBadTrack(truthMatch,track);
            ReconstructedParticle badReconPart = null;
            if(badTrk != null){
                badReconPart = makeReconstructedParticles(clusters,badTrk);
                otherReconParticles.add(badReconPart);
            }

            truthHits = truthMatch.getActiveHitListMCParticle();
            badTracks.add(track);
            truthMatchWithBadTrack.add(truthMatch);
            trackBadToTruthMatchRelations.add(new BaseLCRelation(track, truthMatch.getMCParticle()));
            trackBadToBadMCParticleRelations.add(new BaseLCRelation(track, badPart));
            trackBadToBadReconParticleRelations.add(new BaseLCRelation(track, badReconPart));
        }

        //Fill the collections
        event.put(otherParticleColName, otherReconParticles, ReconstructedParticle.class, 0);
        event.put(badMCParticleRelationsColName, trackBadToBadMCParticleRelations, LCRelation.class, 0);
        event.put(otherParticleRelationsColName, trackBadToBadReconParticleRelations, LCRelation.class, 0);
        event.put(simhitOutputColName, truthHits, SimTrackerHit.class, 0);
        event.put(badTrackColName, badTracks, Track.class, 0);
        event.put(trackBadToTruthMatchRelationsOutputColName, trackBadToTruthMatchRelations, LCRelation.class, 0);
        event.put(trackToTruthMatchRelationsOutputColName, trackToTruthMatchRelations, LCRelation.class, 0);
    }
    
    //This function identifies MCParticle responsible for bad hit
    //It selects the particle that at the innermost bad hit
    //If this has multiple particles, select the higher momentum particle
    //TODO Use better selection criteria or incorporate all particles that contribute to bad hits on track

    MCParticle SelectBadMCParticle(MCFullDetectorTruth truthMatch, Track trk){
        MCParticle badP = null;
        List<TrackerHit> hits = trk.getTrackerHits();
        //for(TrackerHit hit : hits){
        for(int layer = 1; layer < 13; layer++){
            if (truthMatch.getHitList(layer) == null || truthMatch.getHitMCParticleList(layer) == null)
                continue;
            if(truthMatch.getHitList(layer))
                continue;
            Set<MCParticle> badPList = truthMatch.getHitMCParticleList(layer);
            if(badPList ==  null)
                continue;
            double maxP = 0.0;
            for(MCParticle part : badPList){
                double p = part.getMomentum().magnitude();
                if(p > maxP){
                    badP = part;
                    break;
                }
            }
            if(badP != null)
                return badP;
        }
        return badP;
    }
    
    //This function identifies a Track responsible for bad hit
    //It selects the track that at the innermost bad hit
    //If this has multiple tracks, select the higher momentum track
    //TODO Use better selection criteria or incorporate all tracks that contribute to bad hits on track
    
    Track SelectBadTrack(MCFullDetectorTruth truthMatch, Track trk){
        Track badTrk = null;
        List<TrackerHit> hits = trk.getTrackerHits();
        for(TrackerHit hit : hits){
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            if(truthMatch.getHitList(layer))
                continue;
            Set<Track> badTrkList = truthMatch.getHitTrackList(layer);
            if(badTrkList ==  null)
                continue;
            double maxP = 0.0;
            for(Track track : badTrkList){
                double p = Math.abs(TrackUtils.getHTF(track.getTrackStates().get(0)).p(bFieldMap.getField(new BasicHep3Vector(0, 0, 500)).y()));
                if(p > maxP){
                    badTrk = track;
                    break;
                }
            }
            if(badTrk != null){
                return badTrk;
            }
        }
        return badTrk;
    }
    
    protected ReconstructedParticle makeReconstructedParticles(List<Cluster> clusters, Track track) {

        // Loop through all of the track collections and try to match every
        // track to a cluster. Allow a cluster to be matched to multiple
        // tracks and use a probability (to be coded later) to determine what
        // the best match is.

        // Create a reconstructed particle to represent the track.
        ReconstructedParticle particle = new BaseReconstructedParticle();

        // Store the track in the particle.
        particle.addTrack(track);

        // Set the type of the particle. This is used to identify
        // the tracking strategy used in finding the track associated with
        // this particle.
        ((BaseReconstructedParticle) particle).setType(track.getType());

        // Derive the charge of the particle from the track.
        int flipSign = 1;
        int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega());
        ((BaseReconstructedParticle) particle).setCharge(charge * flipSign);

        // initialize PID quality to a junk value:
        ((BaseReconstructedParticle) particle).setGoodnessOfPid(9999);

        // Extrapolate the particle ID from the track. Positively
        // charged particles are assumed to be positrons and those
        // with negative charges are assumed to be electrons.
        if (particle.getCharge() > 0) {
            ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
        } else if (particle.getCharge() < 0) {
            ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));
        }

        // normalized distance of the closest match:
        double smallestNSigma = Double.MAX_VALUE;

        // try to find a matching cluster:
        Cluster matchedCluster = null;
        for (Cluster cluster : clusters) {
            //double clusTime = ClusterUtilities.getSeedHitTime(cluster);
            //double trkT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);

                    
            //if the option to use corrected cluster positions is selected, then
            //create a copy of the current cluster, and apply corrections to it
            //before calculating nsigma.  Default is don't use corrections.  
            Cluster originalCluster = cluster;
                    
            // normalized distance between this cluster and track:
            final double thisNSigma = matcher.getNSigmaPosition(cluster, particle);

            // ignore if matching quality doesn't make the cut:
            if (thisNSigma > MAXNSIGMAPOSITIONMATCH)
                continue;

            // ignore if we already found a cluster that's a better match:
            if (thisNSigma > smallestNSigma)
                continue;

            // we found a new best cluster candidate for this track:
            smallestNSigma = thisNSigma;
            matchedCluster = originalCluster;
        }

        // If a cluster was found that matches the track...
        if (matchedCluster != null) {

            // add cluster to the particle:
            particle.addCluster(matchedCluster);

            // use pid quality to store track-cluster matching quality:
            ((BaseReconstructedParticle) particle).setGoodnessOfPid(smallestNSigma);

            // propogate pid to the cluster:
            final int pid = particle.getParticleIDUsed().getPDG();
            if (Math.abs(pid) == 11) {
                if (!disablePID)
                    ((BaseCluster) matchedCluster).setParticleId(pid);
            }
        }
        return particle;
    }
}