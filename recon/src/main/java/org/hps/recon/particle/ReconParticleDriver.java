package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;

/**
 * Driver that matches SVT Tracks and Ecal Clusters and creates
 * ReconstructedParticles.
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public abstract class ReconParticleDriver extends Driver {

    // Flags
    boolean debug = false;

    // Reconstructed particle collections
    List<ReconstructedParticle> finalStateParticles;
    List<ReconstructedParticle> unconstrainedV0Candidates;
    List<ReconstructedParticle> beamConV0Candidates;
    List<ReconstructedParticle> targetConV0Candidates;
    List<ReconstructedParticle> electrons;
    List<ReconstructedParticle> positrons;
    List<Vertex> unconstrainedV0Vertices;
    List<Vertex> beamConV0Vertices;
    List<Vertex> targetConV0Vertices;

    // Collections
    String ecalClustersCollectionName = "EcalClusters";
    String tracksCollectionName = "MatchedTracks";
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetConV0CandidatesColName = "TargetConstrainedV0Candidates";
    String unconstrainedV0VerticesColName = "UnconstrainedV0Vertices";
    String beamConV0VerticesColName = "BeamspotConstrainedV0Vertices";
    String targetConV0VerticesColName = "TargetConstrainedV0Vertices";
//    String unconstrainedV0CandidatesColName = null;
//    String beamConV0CandidatesColName = null;
//    String targetConV0CandidatesColName = null;
//    String vertexCandidatesColName = null;
//    String vertexBeamConsCandidatesName = null;
//	String unconstrainedV0VerticesColName = null;
//	String beamConV0VerticesColName = null;
//	String targetConV0VerticesColName = null;

    // The beamsize array is in the tracking frame
    /* TODO  mg-May 14, 2014:  the the beam size from the conditions db...also beam position!  */
    double[] beamsize = {0.001, 0.2, 0.02};
    double maxTrackClusterDistance = 10000; // [mm] 
    double bField;

    //  flipSign is a kludge...
    //  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    //  so we set the B-field in +iveZ and flip signs of fitted tracks
    //  
    //  Note:  This should be -1 for test run configurations and +1 for 
    //         prop-2014 configurations 
    int flipSign = 1;

    public ReconParticleDriver() {
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setMaxTrackClusterDistance(double maxTrackClusterDistance) {
        this.maxTrackClusterDistance = maxTrackClusterDistance;
    }

    public void setBeamSigmaX(double sigma_x) {
        beamsize[1] = sigma_x;
    }

    public void setBeamSigmaY(double sigma_y) {
        beamsize[2] = sigma_y;
    }

    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    public void setTracksCollectionName(String tracksCollectionName) {
        this.tracksCollectionName = tracksCollectionName;
    }
    
    public void setFinalStateParticlesColName(String finalStateParticlesColName) {
        this.finalStateParticlesColName = finalStateParticlesColName;
    }
    
     
    public void setUnconstrainedV0CandidatesColName(String unconstrainedV0CandidatesColName) {
        this.unconstrainedV0CandidatesColName = unconstrainedV0CandidatesColName;
    }
    
    
    public void setBeamConV0CandidatesColName(String beamConV0CandidatesColName) {
        this.beamConV0CandidatesColName = beamConV0CandidatesColName;
    }
    
     
    public void setTargetConV0CandidatesColName(String targetV0CandidatesColName) {
        this.targetConV0CandidatesColName = targetConV0CandidatesColName;
    }
    
    
    public void setUnconstrainedV0VerticesColName(String unconstrainedV0VerticesColName) {
        this.unconstrainedV0VerticesColName = unconstrainedV0VerticesColName;
    }
    
    
    public void setBeamConV0VerticesColName(String beamConV0VerticesColName) {
        this.beamConV0VerticesColName = beamConV0VerticesColName;
    }
    
     
    public void setTargetConV0VerticesColName(String targetV0VerticesColName) {
        this.targetConV0VerticesColName = targetConV0VerticesColName;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        Hep3Vector ip = new BasicHep3Vector(0., 0., 1.);
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0)
            flipSign = -1;

    }

    public void process(EventHeader event) {

        // All events should have a collection of Ecal clusters.  If the event 
        // doesn't have one, skip the event.
        if (!event.hasCollection(Cluster.class, ecalClustersCollectionName))
            return;

        // Get the collection of Ecal clusters from the event. A triggered 
        // event should have Ecal clusters.  If it doesn't, skip the event.
        List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);
        //if(clusters.isEmpty()) return;  
        this.printDebug("Number of Ecal clusters: " + clusters.size());

        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, tracksCollectionName);
        this.printDebug("Number of Tracks in "+tracksCollectionName+" : " + tracks.size());

        finalStateParticles = new ArrayList<ReconstructedParticle>();
        electrons = new ArrayList<ReconstructedParticle>();
        positrons = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Candidates = new ArrayList<ReconstructedParticle>();
        beamConV0Candidates = new ArrayList<ReconstructedParticle>();
        targetConV0Candidates = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Vertices = new ArrayList<Vertex>();
        beamConV0Vertices = new ArrayList<Vertex>();
        targetConV0Vertices = new ArrayList<Vertex>();

        // 
        finalStateParticles = this.makeReconstructedParticles(clusters, tracks);
        this.printDebug("Total number of Final State Particles: " + finalStateParticles.size());

        // Put all the reconstructed particles in the event
        event.put(finalStateParticlesColName, finalStateParticles, ReconstructedParticle.class, 0);

        // Loop through the list of final state particles and separate the
        // charged particles to either electrons or positrons.  These lists
        // will be used for vertexing purposes.
        for (ReconstructedParticle finalStateParticle : finalStateParticles)
            if (finalStateParticle.getCharge() > 0)
                positrons.add(finalStateParticle);
            else if (finalStateParticle.getCharge() < 0)
                electrons.add(finalStateParticle);
        this.printDebug("Number of Electrons: " + electrons.size());
        this.printDebug("Number of Positrons: " + positrons.size());

        // Vertex electron and positron candidates 
        findVertices(electrons, positrons);

        // If the list exist, put the vertexed candidates and vertices into the event
        if (unconstrainedV0CandidatesColName != null) {
            this.printDebug("Total number of unconstrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(unconstrainedV0CandidatesColName, unconstrainedV0Candidates, ReconstructedParticle.class, 0);
        }
        if (beamConV0CandidatesColName != null) {
            this.printDebug("Total number of beam constrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(beamConV0CandidatesColName, beamConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (targetConV0CandidatesColName != null) {
            this.printDebug("Total number of target constrained V0 candidates: " + unconstrainedV0Candidates.size());
            event.put(targetConV0CandidatesColName, targetConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (unconstrainedV0VerticesColName != null) {
            this.printDebug("Total number of unconstrained V0 vertices: " + unconstrainedV0Vertices.size());
            event.put(unconstrainedV0VerticesColName, unconstrainedV0Vertices, Vertex.class, 0);
        }
        if (beamConV0VerticesColName != null) {
            this.printDebug("Total number of beam constrained V0 vertices: " + beamConV0Vertices.size());
            event.put(beamConV0VerticesColName, beamConV0Vertices, Vertex.class, 0);
        }
        if (targetConV0VerticesColName != null) {
            this.printDebug("Total number of target constrained V0 vertices: " + beamConV0Vertices.size());
            event.put(targetConV0VerticesColName, targetConV0Vertices, Vertex.class, 0);
        }
    }

    /**
     *
     */
    abstract void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons);

    /**
     * make the final state particles from clusters & tracks
     * loop over the tracks first and try to match with clusters
     */
    protected List<ReconstructedParticle> makeReconstructedParticles(List<Cluster> clusters, List<Track> tracks) {

        // Instantiate the list of reconstructed particles
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Instantiate the list of unmatched  clusters.  Remove if we find track match
        List<Cluster> unmatchedClusters = new ArrayList<Cluster>(clusters);

        for (Track track : tracks) {

            ReconstructedParticle particle = new BaseReconstructedParticle();
            HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);

            //
            // Add all track information to the ReconstructedParticle
            //
            particle.addTrack(track);

            // Set the momentum of the ReconstructedParticle
            Hep3Vector momentum = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
            momentum = CoordinateTransformations.transformVectorToDetector(momentum);
            ((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), momentum);
            // Set the charge of the ReconstructedParticle
            ((BaseReconstructedParticle) particle).setCharge(track.getCharge() * flipSign);
            // Set the particle ID
            if (particle.getCharge() > 0)
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
            else if (particle.getCharge() < 0)
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));

            Cluster matchedCluster = null;
            // Loop through all of the clusters and find the one that best matches
            // the track.
            for (Cluster cluster : unmatchedClusters) {

                // Get the position of the Ecal cluster
                Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());

                // Extrapolate the track to the Ecal cluster position
                Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());
                this.printDebug("Ecal cluster position: " + clusterPosition.toString());

                double rMax = Double.MAX_VALUE;

                // Check if any of the extrapolated values are invalid.
                // TODO: There are some track whose extrapolated coordinates
                //        are NaN. The problem seems to be that the y-coordinate
                //        of the extrapolated helix is found to be non-real. This
                //        needs to be fixed.
                if (Double.isNaN(trackPosAtEcal.x()) || Double.isNaN(trackPosAtEcal.y()))
                    continue;
                this.printDebug("Track position at shower max: " + trackPosAtEcal.toString());

//                double r = VecOp.sub(trackPosAtEcal, clusterPosition).magnitude();
                // Don't trust extrapolation...just do y-difference for now
                double r = Math.abs(clusterPosition.y() - trackPosAtEcal.y());
                this.printDebug("Distance between Ecal cluster and track position: " + r + " mm");

                // Check if the Ecal cluster and track are within the same 
                // detector volume i.e. both top or bottom
                if (clusterPosition.y() * trackPosAtEcal.y() < 0) {
                    this.printDebug("Track and Ecal cluster are in opposite volumes. Track Y @ ECAL = " + trackPosAtEcal.z());
                    continue;
                }

                // TODO: Checking whether r < rMax should be occuring within isMatch.  isMatch 
                // 		 is basically repeating a lot of the same code as above.
                if (r < rMax && isMatch(cluster, track)) {
                    rMax = r;
                    matchedCluster = cluster;
                }
            }

            if (matchedCluster != null) {
                particle.addCluster(matchedCluster);
                ((BasicHepLorentzVector) fourVector).setT(matchedCluster.getEnergy());
                unmatchedClusters.remove(matchedCluster);
            }
            ((BaseReconstructedParticle) particle).set4Vector(fourVector);
            particles.add(particle);
        }

        if (!unmatchedClusters.isEmpty())
            for (Cluster unmatchedCluster : unmatchedClusters) {
                // Create a reconstructed particle and add it to the 
                // collection of particles
                ReconstructedParticle particle = new BaseReconstructedParticle();
                HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);

                particle.addCluster(unmatchedCluster);
                ((BasicHepLorentzVector) fourVector).setT(unmatchedCluster.getEnergy());
                ((BaseReconstructedParticle) particle).setCharge(0);
                ((BaseReconstructedParticle) particle).set4Vector(fourVector);
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(22, 0, 0, 0));
                particles.add(particle);
            }

        return particles;
    }

    /**
     *
     * @param debugMessage
     */
    protected void printDebug(String debugMessage) {
        if (debug)
            System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
    }

    /**
     *
     */
    boolean isMatch(Cluster cluster, Track track) {

        // Get the position of the Ecal cluster
        Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());

        double dxCut = 20.0;
        double dyCut = 20.0;

        if (Math.abs(trackPosAtEcal.x() - clusterPosition.x()) > dxCut)
            return false;

        if (Math.abs(trackPosAtEcal.y() - clusterPosition.y()) > dyCut)
            return false;

        return true;
    }
}
