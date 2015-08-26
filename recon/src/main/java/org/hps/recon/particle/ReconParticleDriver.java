package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.ecal.cluster.ClusterType;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.utils.TrackClusterMatcher;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Driver framework for generating reconstructed particles and matching clusters
 * and tracks.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 */
public abstract class ReconParticleDriver extends Driver {

    /** Utility used to determine if a track and cluster are matched */
    TrackClusterMatcher matcher = new TrackClusterMatcher(); 
    
    /**
     * Sets the name of the LCIO collection for beam spot constrained V0
     * candidate particles.
     *
     * @param beamConV0CandidatesColName - The LCIO collection name.
     */
    public void setBeamConV0CandidatesColName(String beamConV0CandidatesColName) {
        this.beamConV0CandidatesColName = beamConV0CandidatesColName;
    }

    /**
     * Sets the name of the LCIO collection for beam spot constrained V0
     * candidate vertices.
     *
     * @param beamConV0VerticesColName - The LCIO collection name.
     */
    public void setBeamConV0VerticesColName(String beamConV0VerticesColName) {
        this.beamConV0VerticesColName = beamConV0VerticesColName;
    }

    /**
     * Sets the beam size sigma in the x-direction.
     *
     * @param sigmaX - The standard deviation of the beam width in the
     * x-direction.
     */
    public void setBeamSigmaX(double sigmaX) {
        beamSize[1] = sigmaX;
    }

    /**
     * Sets the beam size sigma in the y-direction.
     *
     * @param sigmaY - The standard deviation of the beam width in the
     * y-direction.
     */
    public void setBeamSigmaY(double sigmaY) {
        beamSize[2] = sigmaY;
    }

    /**
     * Indicates whether verbose debug text should be written out during runtime
     * or note. Defaults to <code>false</code>.
     *
     * @param debug - <code>true</code> indicates that debug text should be
     * written and <code>false</code> that it should be suppressed.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets the LCIO collection name for calorimeter cluster data.
     *
     * @param ecalClustersCollectionName - The LCIO collection name.
     */
    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    /**
     * Sets the name of the LCIO collection for reconstructed particles.
     *
     * @param finalStateParticlesColName - The LCIO collection name.
     */
    public void setFinalStateParticlesColName(String finalStateParticlesColName) {
        this.finalStateParticlesColName = finalStateParticlesColName;
    }

    /**
     * Sets the name of the LCIO collection for target constrained V0 candidate
     * particles.
     *
     * @param targetConV0CandidatesColName - The LCIO collection name.
     */
    public void setTargetConV0CandidatesColName(String targetConV0CandidatesColName) {
        this.targetConV0CandidatesColName = targetConV0CandidatesColName;
    }

    /**
     * Sets the name of the LCIO collection for target constrained V0 candidate
     * vertices.
     *
     * @param targetConV0VerticesColName - The LCIO collection name.
     */
    public void setTargetConV0VerticesColName(String targetConV0VerticesColName) {
        this.targetConV0VerticesColName = targetConV0VerticesColName;
    }

    /**
     * Sets the LCIO collection name for particle track data.
     *
     * @param trackCollectionName - The LCIO collection name.
     */
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    /**
     * Sets the name of the LCIO collection for unconstrained V0 candidate
     * particles.
     *
     * @param unconstrainedV0CandidatesColName - The LCIO collection name.
     */
    public void setUnconstrainedV0CandidatesColName(String unconstrainedV0CandidatesColName) {
        this.unconstrainedV0CandidatesColName = unconstrainedV0CandidatesColName;
    }

    /**
     * Sets the name of the LCIO collection for unconstrained V0 candidate
     * vertices.
     *
     * @param unconstrainedV0VerticesColName - The LCIO collection name.
     */
    public void setUnconstrainedV0VerticesColName(String unconstrainedV0VerticesColName) {
        this.unconstrainedV0VerticesColName = unconstrainedV0VerticesColName;
    }

    /**
     * Updates the magnetic field parameters to match the appropriate values for
     * the current detector settings.
     */
    @Override
    protected void detectorChanged(Detector detector) {
        //matcher.enablePlots(true);
        
        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 1.);
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0) {
            flipSign = -1;
        }
    }

    /**
     * Generates reconstructed V0 candidate particles and vertices from sets of
     * positrons and electrons. Implementing methods should place the
     * reconstructed vertices and candidate particles into the appropriate class
     * variable lists in <code>ReconParticleDriver
     * </code>.
     *
     * @param electrons - The list of electrons.
     * @param positrons - The list of positrons.
     */
    protected abstract void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons);

    /**
     * Create the set of final state particles from the event tracks and
     * clusters. Clusters will be matched with tracks when this is possible.
     *
     * @param clusters - The list of event clusters.
     * @param tracks - The list of event tracks.
     * @return Returns a <code>List</code> collection containing all of the
     * <code>ReconstructedParticle</code> objects generated from the argument
     * data.
     */
    protected List<ReconstructedParticle> makeReconstructedParticles(List<Cluster> clusters, List<Track> tracks) {
        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Create a list of unmatched clusters. A cluster should be
        // removed from the list if a matching track is found.
        //List<Cluster> unmatchedClusters = new ArrayList<Cluster>(clusters);
        java.util.Set<Cluster> unmatchedClusters = new java.util.HashSet<Cluster>(clusters);

        // Iterate over all of the tracks and generate reconstructed
        // particles for each one. If possible, match a cluster to the
        // track as well.
        for (Track track : tracks) {
            // Create a reconstructed particle to represent the track.
            ReconstructedParticle particle = new BaseReconstructedParticle();
            HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);

            // Store the track in the particle.
            particle.addTrack(track);

            // Store the momentum derived from the track in the particle.
            Hep3Vector momentum = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
            momentum = CoordinateTransformations.transformVectorToDetector(momentum);
            ((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), momentum);

            // Derive the charge of the particle from the track.
            ((BaseReconstructedParticle) particle).setCharge(track.getCharge() * flipSign);

            // Extrapolate the particle ID from the track. Positively
            // charged particles are assumed to be positrons and those
            // with negative charges are assumed to be electrons.
            if (particle.getCharge() > 0) {
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
            } else if (particle.getCharge() < 0) {
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));
            }

            // Track the best matching cluster for the track. A null
            // value indicates that no cluster matches the track to
            // within the maximum displacement limits.
            Cluster matchedCluster = null;

            // Loop through all the unmatched clusters and select the
            // cluster, if any, that best fits with the track.
            clusterLoop:
            for (Cluster cluster : unmatchedClusters) {
                // Check if the cluster and track are a valid match.
                if (matcher.isMatch(cluster, track)) {
                    // Store the matched cluster.
                    matchedCluster = cluster;

                    // Since a match has been found, the loop can be
                    // terminated.
                    break clusterLoop;
                }
            }

            // If a cluster was found that matches the track...
            if (matchedCluster != null) {

                if (matchedCluster.getType() == ClusterType.RECON.getType()) {
                    int pid = particle.getParticleIDUsed().getPDG();
                    // Is cluster from an electron or positron?
                    if (pid == -11 || pid == 11) {
                        // Set PID and apply corrections.
                        ((BaseCluster) matchedCluster).setParticleId(pid);
                        ClusterUtilities.applyCorrections(matchedCluster);
                    }
                }

                // Update the reconstructed particle with the data from
                // the cluster.
                particle.addCluster(matchedCluster);
                ((BasicHepLorentzVector) fourVector).setT(matchedCluster.getEnergy());

                // Remove the cluster from the set of unmatched clusters.
                unmatchedClusters.remove(matchedCluster);
            }

            // Store the momentum vector in the reconstructed particle.
            ((BaseReconstructedParticle) particle).set4Vector(fourVector);

            // Add the particle to the list of reconstructed particles.
            particles.add(particle);
        }

        // If any cluster remain unmatched after the tracks are finished,
        // they should be processed into additional reconstructed particles.
        if (!unmatchedClusters.isEmpty()) // Iterate over the remaining unmatched clusters.
        {
            for (Cluster unmatchedCluster : unmatchedClusters) {
                // Create a reconstructed particle to represent the unmatched cluster.
                ReconstructedParticle particle = new BaseReconstructedParticle();
                
                // The particle is assumed to be a photon, since it did not leave a track.
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(22, 0, 0, 0));
                
                // apply cluster corrections
                if (unmatchedCluster.getType() == ClusterType.RECON.getType()) {
                    int pid = particle.getParticleIDUsed().getPDG();
                    // If not electron....
                    if (pid != 11) {
                        // Set PID and apply corrections.
                        ((BaseCluster) unmatchedCluster).setParticleId(pid);
                        ClusterUtilities.applyCorrections(unmatchedCluster);
                    }
                }
                //get energy and direction from the cluster
                Hep3Vector p = new BasicHep3Vector(unmatchedCluster.getPosition());
                double e = unmatchedCluster.getEnergy();
                // create momentum vector from direction unit vector times the energy (massless photon)
                HepLorentzVector fourVector = new BasicHepLorentzVector(e, VecOp.mult(e, VecOp.unit(p)));

                // Add the cluster to the particle.
                particle.addCluster(unmatchedCluster);

                // Set the reconstructed particle properties based on the cluster properties.
                ((BaseReconstructedParticle) particle).setCharge(0);
                ((BaseReconstructedParticle) particle).set4Vector(fourVector);

                // Add the particle to the reconstructed particle list.
                particles.add(particle);
            }
        }

        // Return the list of reconstructed particles.
        return particles;
    }

    /**
     * Prints a message as per <code>System.out.println</code> to the output
     * stream if the verbose debug output option is enabled.
     *
     * @param debugMessage - The message to print.
     */
    protected void printDebug(String debugMessage) {
        // If verbose debug mode is enabled, print out the message.
        if (debug) {
            System.out.printf("%s :: %s%n", simpleName, debugMessage);
        }
    }

    /**
     * Processes the track and cluster collections in the event into
     * reconstructed particles and V0 candidate particles and vertices. These
     * reconstructed particles are then stored in the event.
     *
     * @param event - The event to process.
     */
    @Override
    protected void process(EventHeader event) {
        // All events are required to contain calorimeter clusters. If
        // the event lacks these, then it should be skipped.
        if (!event.hasCollection(Cluster.class, ecalClustersCollectionName)) {
            return;
        }

        // VERBOSE :: Note that a new event is being read.
        printDebug("\nProcessing Event...");

        // Otherwise, get the list of calorimeter clusters.
        List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);

        // VERBOSE :: Output the number of clusters in the event.
        printDebug("Clusters :: " + clusters.size());

        // Get the set of tracks from the event. If no such collection
        // exists, initialize an empty list instead.
        List<Track> tracks;
        if (event.hasCollection(Track.class, trackCollectionName)) {
            tracks = event.get(Track.class, trackCollectionName);
        } else {
            tracks = new ArrayList<Track>(0);
        }

        // VERBOSE :: Output the number of tracks in the event.
        printDebug("Tracks :: " + tracks.size());

        // Instantiate new lists to store reconstructed particles and
        // V0 candidate particles and vertices.
        finalStateParticles = new ArrayList<ReconstructedParticle>();
        electrons = new ArrayList<ReconstructedParticle>();
        positrons = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Candidates = new ArrayList<ReconstructedParticle>();
        beamConV0Candidates = new ArrayList<ReconstructedParticle>();
        targetConV0Candidates = new ArrayList<ReconstructedParticle>();
        unconstrainedV0Vertices = new ArrayList<Vertex>();
        beamConV0Vertices = new ArrayList<Vertex>();
        targetConV0Vertices = new ArrayList<Vertex>();

        // Generate the reconstructed particles.
        finalStateParticles = makeReconstructedParticles(clusters, tracks);

        // VERBOSE :: Output the number of reconstructed particles.
        printDebug("Final State Particles :: " + finalStateParticles.size());

        // Store the reconstructed particles collection.
        event.put(finalStateParticlesColName, finalStateParticles, ReconstructedParticle.class, 0);

        // Separate the reconstructed particles into electrons and
        // positrons so that V0 candidates can be generated from them.
        for (ReconstructedParticle finalStateParticle : finalStateParticles) {
            // If the charge is positive, assume an electron.
            if (finalStateParticle.getCharge() > 0) {
                positrons.add(finalStateParticle);
            } // Otherwise, assume the particle is a positron.
            else if (finalStateParticle.getCharge() < 0) {
                electrons.add(finalStateParticle);
            }
        }

        // VERBOSE :: Output the number of reconstructed positrons
        // and electrons.
        printDebug("Number of Electrons: " + electrons.size());
        printDebug("Number of Positrons: " + positrons.size());

        // Form V0 candidate particles and vertices from the electron
        // and positron reconstructed particles.
        findVertices(electrons, positrons);

        // Store the V0 candidate particles and vertices for each type
        // of constraint in the appropriate collection in the event,
        // as long as a collection name is defined.
        if (unconstrainedV0CandidatesColName != null) {
            printDebug("Unconstrained V0 Candidates: " + unconstrainedV0Candidates.size());
            event.put(unconstrainedV0CandidatesColName, unconstrainedV0Candidates, ReconstructedParticle.class, 0);
        }
        if (beamConV0CandidatesColName != null) {
            printDebug("Beam-Constrained V0 Candidates: " + unconstrainedV0Candidates.size());
            event.put(beamConV0CandidatesColName, beamConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (targetConV0CandidatesColName != null) {
            printDebug("Target-Constrained V0 Candidates: " + unconstrainedV0Candidates.size());
            event.put(targetConV0CandidatesColName, targetConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (unconstrainedV0VerticesColName != null) {
            printDebug("Unconstrained V0 Vertices: " + unconstrainedV0Vertices.size());
            event.put(unconstrainedV0VerticesColName, unconstrainedV0Vertices, Vertex.class, 0);
        }
        if (beamConV0VerticesColName != null) {
            printDebug("Beam-Constrained V0 Vertices: " + beamConV0Vertices.size());
            event.put(beamConV0VerticesColName, beamConV0Vertices, Vertex.class, 0);
        }
        if (targetConV0VerticesColName != null) {
            printDebug("Target-Constrained V0 Vertices: " + beamConV0Vertices.size());
            event.put(targetConV0VerticesColName, targetConV0Vertices, Vertex.class, 0);
        }
    }

    /**
     * Sets the LCIO collection names to their default values if they are not
     * already defined.
     */
    @Override
    protected void startOfData() {
        // If any of the LCIO collection names are not properly defined, define them now.
        if (ecalClustersCollectionName == null) {
            ecalClustersCollectionName = "EcalClusters";
        }
        if (trackCollectionName == null) {
            trackCollectionName = "MatchedTracks";
        }
        if (finalStateParticlesColName == null) {
            finalStateParticlesColName = "FinalStateParticles";
        }
        if (unconstrainedV0CandidatesColName == null) {
            unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
        }
        if (beamConV0CandidatesColName == null) {
            beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
        }
        if (targetConV0CandidatesColName == null) {
            targetConV0CandidatesColName = "TargetConstrainedV0Candidates";
        }
        if (unconstrainedV0VerticesColName == null) {
            unconstrainedV0VerticesColName = "UnconstrainedV0Vertices";
        }
        if (beamConV0VerticesColName == null) {
            beamConV0VerticesColName = "BeamspotConstrainedV0Vertices";
        }
        if (targetConV0VerticesColName == null) {
            targetConV0VerticesColName = "TargetConstrainedV0Vertices";
        }
    }

    @Override
    protected void endOfData() { 
        //matcher.saveHistograms();
    }

    // ==============================================================
    // ==== Class Variables =========================================
    // ==============================================================
    // Local variables.
    
    /**
     * Indicates whether debug text should be output or not.
     */
    private boolean debug = false;
    
    /**
     * The simple name of the class used for debug print statements.
     */
    private final String simpleName = getClass().getSimpleName();

    // Reconstructed Particle Lists
    /**
     * Stores reconstructed electron particles.
     */
    private List<ReconstructedParticle> electrons;
    /**
     * Stores reconstructed positron particles.
     */
    private List<ReconstructedParticle> positrons;
    /**
     * Stores particles reconstructed from an event.
     */
    protected List<ReconstructedParticle> finalStateParticles;
    /**
     * Stores reconstructed V0 candidate particles generated without
     * constraints.
     */
    protected List<ReconstructedParticle> unconstrainedV0Candidates;
    /**
     * Stores reconstructed V0 candidate particles generated with beam spot
     * constraints.
     */
    protected List<ReconstructedParticle> beamConV0Candidates;
    /**
     * Stores reconstructed V0 candidate particles generated with target
     * constraints.
     */
    protected List<ReconstructedParticle> targetConV0Candidates;
    /**
     * Stores reconstructed V0 candidate vertices generated without constraints.
     */
    protected List<Vertex> unconstrainedV0Vertices;
    /**
     * Stores reconstructed V0 candidate vertices generated with beam spot
     * constraints.
     */
    protected List<Vertex> beamConV0Vertices;
    /**
     * Stores reconstructed V0 candidate vertices generated with target
     * constraints.
     */
    protected List<Vertex> targetConV0Vertices;

    // LCIO Collection Names
    /**
     * LCIO collection name for calorimeter clusters.
     */
    private String ecalClustersCollectionName = "EcalClusters";
    /**
     * LCIO collection name for tracks.
     */
    private String trackCollectionName = "MatchedTracks";
    /**
     * LCIO collection name for reconstructed particles.
     */
    private String finalStateParticlesColName = "FinalStateParticles";
    /**
     * LCIO collection name for V0 candidate particles generated without
     * constraints.
     */
    protected String unconstrainedV0CandidatesColName = null;
    /**
     * LCIO collection name for V0 candidate particles generated with beam spot
     * constraints.
     */
    protected String beamConV0CandidatesColName = null;
    /**
     * LCIO collection name for V0 candidate particles generated with target
     * constraints.
     */
    protected String targetConV0CandidatesColName = null;
    /**
     * LCIO collection name for V0 candidate vertices generated without
     * constraints.
     */
    protected String unconstrainedV0VerticesColName = null;
    /**
     * LCIO collection name for V0 candidate vertices generated with beam spot
     * constraints.
     */
    protected String beamConV0VerticesColName = null;
    /**
     * LCIO collection name for V0 candidate vertices generated with target
     * constraints.
     */
    protected String targetConV0VerticesColName = null;

    // Beam size variables.
    // The beamsize array is in the tracking frame
    /* TODO  mg-May 14, 2014:  the the beam size from the conditions db...also beam position!  */
    protected double[] beamSize = {0.001, 0.2, 0.02};
    protected double bField;

    //  flipSign is a kludge...
    //  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    //  so we set the B-field in +iveZ and flip signs of fitted tracks
    //  
    //  Note:  This should be -1 for test run configurations and +1 for 
    //         prop-2014 configurations 
    private int flipSign = 1;
}
