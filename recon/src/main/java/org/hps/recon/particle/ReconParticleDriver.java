package org.hps.recon.particle;

//--- java ---//
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
//--- hep ---//
//--- lcsim ---//
//--- hps-java ---//
import org.hps.recon.ecal.HPSEcalCluster;

/**
 * Driver that matches SVT Tracks and Ecal Clusters and creates
 * ReconstructedParticles.
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: ReconParticleDriver.java,v 1.8 2013/10/14 22:58:03 phansson Exp $ 
 */
public abstract class ReconParticleDriver extends Driver {

	
	// Flags
	boolean debug = false; 
	
	// Reconstructed particle collections
	List<ReconstructedParticle> reconParticles;
    List<ReconstructedParticle> candidates;
    List<ReconstructedParticle> candidatesBeamCon;
    List<ReconstructedParticle> candidatesTargetCon;
    List<ReconstructedParticle> electrons;
    List<ReconstructedParticle> positrons;
    
    // Collections
    String ecalClustersCollectionName 			= "EcalClusters";
    String tracksCollectionName       			= "MatchedTracks";
    String finalStateParticlesCollectionName 	= "FinalStateParticles";
    String candidatesCollectionName 			= null; 
    String candidatesBeamConCollectionName 		= null;
    String candidatesTargetConCollectionName	= null;
    String vertexCandidatesCollectionName       = null;
    String vertexBeamConsCandidatesName         = null;
    
    // The beamsize array is in the tracking frame
    double[] beamsize = {0.001, 0.2, 0.02};
    double maxTrackClusterDistance = 1000; // [mm] 
	double bField; 
    
     
    //  flipSign is a kludge...
    //  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
	//  so we set the B-field in +iveZ and flip signs of fitted tracks
    //  
    //  Note:  This should be -1 for Test configurations and +1 for 
    //         Full (v3.X and lower) configurations this is set by the _config
    //         variable (detType in HeavyPhotonDriver)
    int flipSign = 1;

    public ReconParticleDriver() {
    }

    public void setDebug(boolean debug){
    	this.debug = debug; 
    }
    
    public void setMaxTrackClusterDistance(double maxTrackClusterDistance){
    	this.maxTrackClusterDistance = maxTrackClusterDistance; 
    }

    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    public void setTrackCollectoinName(String tracksCollectionName) {
        this.tracksCollectionName = tracksCollectionName;
    }

    public void setReconParticlesCollectionName(String reconParticlesCollectionName) {
        this.finalStateParticlesCollectionName = reconParticlesCollectionName;
    }

    public void setBeamSigmaX(double sigma_x) {
        beamsize[1] = sigma_x; 
    }

    public void setBeamSigmaY(double sigma_y) {
        beamsize[2] = sigma_y;  
    }
    
    
	@Override
	protected void detectorChanged(Detector detector){
		
		Hep3Vector ip = new BasicHep3Vector(0., 0., 1.);
		bField = detector.getFieldMap().getField(ip).y(); 
		if(bField < 0) flipSign = -1; 
	
	}

    public void process(EventHeader event) {
       
        // If the event does not have Ecal clusters, skip the event
        if (!event.hasCollection(HPSEcalCluster.class, ecalClustersCollectionName)) return;
        
        // Get the clusters in the event
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClustersCollectionName);
        if(clusters.isEmpty()) return; 
        this.printDebug("Number of Ecal clusters: " + clusters.size());
        
        // Get the tracks in the event
        List<Track> tracks = event.get(Track.class, tracksCollectionName);
        this.printDebug("Number of Tracks: " + tracks.size()); 

        reconParticles		= new ArrayList<ReconstructedParticle>();
        electrons 			= new ArrayList<ReconstructedParticle>();
        positrons 			= new ArrayList<ReconstructedParticle>();
        candidates 			= new ArrayList<ReconstructedParticle>();
        candidatesBeamCon 	= new ArrayList<ReconstructedParticle>(); 
        candidatesTargetCon = new ArrayList<ReconstructedParticle>(); 

        // If there are clusters and tracks in the event, try to match them
        // to each other
        if (!tracks.isEmpty() && !clusters.isEmpty()) this.matchTracksToClusters(clusters, tracks);

        // Put all the reconstructed particles in the event
        event.put(finalStateParticlesCollectionName, reconParticles, ReconstructedParticle.class, 0);

        // Vertex electron and positron candidates 
        vertexParticles(electrons, positrons);
       
        // If the list exist, put the vertexed candidates into the event
        if(candidatesCollectionName != null)
        	event.put(candidatesCollectionName, candidates, ReconstructedParticle.class, 0);
        if(candidatesBeamConCollectionName != null)
        	event.put(candidatesBeamConCollectionName, candidatesBeamCon, ReconstructedParticle.class, 0);
        if(candidatesTargetConCollectionName != null)
        	event.put(candidatesTargetConCollectionName, candidatesTargetCon, ReconstructedParticle.class, 0);
    }

    /**
     * 
     */
    abstract void vertexParticles(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons);
    
    /**
     * 
     */
    private void matchTracksToClusters(List<HPSEcalCluster> clusters, List<Track> tracks){
    	
        	// Instantiate the list of matched tracks and clusters. This list 
        	// will be used to check if a track has already been previously matched
        	List<Track> matchedTracks = new ArrayList<Track>(); 
    	    List<HPSEcalCluster> matchedClusters = new ArrayList<HPSEcalCluster>(); 
        	
        	// Loop over all clusters 
    	    HPSEcalCluster matchedCluster = null;
        	for(HPSEcalCluster cluster : clusters){
    	
        		// If the cluster has already been matched to a track, continue
        		// on to the next cluster
        		if(matchedClusters.contains(cluster)) continue;
        		matchedCluster = cluster; 	
        		
        		// Get the Ecal cluster position
        		Hep3Vector clusterPos = new BasicHep3Vector(cluster.getPosition());
        		this.printDebug("Ecal cluster position: " + clusterPos.toString());
        	
                Track matchedTrack = null; 
        		Hep3Vector matchedTrackPosition = null; 
                double rMax = Double.MAX_VALUE;
        		// Loop over all tracks in the event
                for(Track track : tracks){        	
                	
                	// Check if the track has already been matched to another cluster
                	if(matchedTracks.contains(track)){
                		this.printDebug("Track has already been matched"); 
                		continue; 
                	}
                	
        			
        			// Extrapolate the track to the Ecal cluster shower max
        			Hep3Vector trkPosAtShowerMax = TrackUtils.extrapolateTrack(track,clusterPos.z());
        			// Check if any of the extrapolated values are invalid.
        			// TODO: There are some track whose extrapolated coordinates
        			//        are NaN. The problem seems to be that the y-coordinate
        			//        of the extrapolated helix is found to be non-real. This
        			//        needs to be fixed.
        			if(Double.isNaN(trkPosAtShowerMax.x()) || Double.isNaN(trkPosAtShowerMax.y())) continue; 
        			this.printDebug("Track position at shower max: " + trkPosAtShowerMax.toString());
        			
        			// Find the distance between the track position at shower
        			// max and the cluster position
        			double r = VecOp.sub(trkPosAtShowerMax, clusterPos).magnitude();
        			this.printDebug("Distance between Ecal cluster and track position at shower max: " + r + " mm");

        			// Check if the track is the closest to the cluster.  If it is, then
        			// save the track and contineu looping over all other tracks
                    if (r < rMax && r <= maxTrackClusterDistance) {
                        rMax = r;
                        matchedTrack = track; 
                        matchedTrackPosition = trkPosAtShowerMax; 
                    }
                }
                
                
                // If a matching track isn't found, continue to the next cluster 
                // in the event. This will occur when a track is found to have 
                // NaN coordinate values as explained above or when the distance
                // between a cluster and a track fails the maximum track cluster
                // distance.
                if(matchedTrack == null){                
        			this.printDebug("No matching cluster found!");
                	continue;
                }
                
                // Check if the track is a closer match to any other cluster in
                // the event
                for(HPSEcalCluster ecalCluster : clusters){ 
                	
                	// Skip the current cluster and any other cluster that has 
                	// been matched already
                	if(ecalCluster.equals(cluster) || matchedClusters.contains(ecalCluster)) continue; 
                	
                	// Get the Ecal cluster position
                	Hep3Vector ecalClusterPos = new BasicHep3Vector(ecalCluster.getPosition());
                	this.printDebug("Ecal cluster position: " + ecalClusterPos.toString());
                	
                	// Get the position of the track at the Ecal cluster shower max
                	Hep3Vector trkPosAtShowerMax = TrackUtils.extrapolateTrack(matchedTrack,ecalClusterPos.z());
        			this.printDebug("Track position at shower max: " + trkPosAtShowerMax.toString());
        			
        			// Get the distance between the track and the cluster position
        			// and check if the track has a better cluster match
        			double r = VecOp.sub(trkPosAtShowerMax, ecalClusterPos).magnitude();
        			if(r < rMax && r <= maxTrackClusterDistance){
        				rMax = r; 
        				matchedCluster = ecalCluster; 
        			}
                }
               
               // Add the track to the list of matched tracks
                matchedTracks.add(matchedTrack);
                matchedClusters.add(matchedCluster);
                this.printDebug("Matched track position: " + matchedTrackPosition.toString()); 
                this.printDebug("Matched Cluster Position: " + (new BasicHep3Vector(matchedCluster.getPosition()).toString()));
                
                // Create a reconstructed particle and add it to the 
                // collection of final state particles
                ReconstructedParticle particle = new BaseReconstructedParticle(); 
                particle.addCluster(matchedCluster); 
                particle.addTrack(matchedTrack); 
                reconParticles.add(particle);	
        	
                // Add the particle to either the collection of possible
                // electrons or positrons
                if(matchedTrack.getCharge()*flipSign > 0) positrons.add(particle);
                else electrons.add(particle); 
        	
        	}
        	
        // After iterating through all tracks, if there still
        // exist some clusters in the list then assign them 
        // to their own reconstructed particles
        if (!clusters.isEmpty()) {
            for (HPSEcalCluster cluster : clusters) {
                ReconstructedParticle particle = new BaseReconstructedParticle();
                particle.addCluster(cluster);
                reconParticles.add(particle);
            }
        }
    }
       
    /**
     * 
     * @param debugMessage
     */
    private void printDebug(String debugMessage){
    	if(debug)
    		System.out.println(this.getClass().getSimpleName() + ": " + debugMessage); 
    }
}
