package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.HPSEcalClusterIC;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Driver framework for generating reconstructed particles and matching clusters
 * and tracks.
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public abstract class ReconParticleDriver extends Driver {

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
     * Sets the maximum allowed separation distance between a matched cluster
     * and track pair.
     *
     * @param dxCut - The maximum separation distance in the x-direction.
     */
    public void setDxCut(double dxCut) {
        this.dxCut = dxCut;
    }

    /**
     * Sets the maximum allowed separation distance between a matched cluster
     * and track pair.
     *
     * @param dyCut - The maximum separation distance in the y-direction.
     */
    public void setDyCut(double dyCut) {
        this.dyCut = dyCut;
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
     * @param tracksCollectionName - The LCIO collection name.
     */
    public void setTracksCollectionName(String tracksCollectionName) {
        this.tracksCollectionName = tracksCollectionName;
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
                if (isMatch(cluster, track)) {
                    // Store the matched cluster.
                    matchedCluster = cluster;

                    // Since a match has been found, the loop can be
                    // terminated.
                    break clusterLoop;
                }
            }

            // If a cluster was found that matches the track...
            if (matchedCluster != null) {

                if (matchedCluster instanceof HPSEcalClusterIC) {
                    int pid = particle.getParticleIDUsed().getPDG();
                    if (pid == -11) {
                        ((HPSEcalClusterIC) matchedCluster).setParticleID(pid);
                    }// End of cluster position/energy corrections. 
                    else if (pid == 11) {
                        ((HPSEcalClusterIC) matchedCluster).setParticleID(pid);
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
                // Create a reconstructed particle to represent the
                // unmatched cluster.
                ReconstructedParticle particle = new BaseReconstructedParticle();
                HepLorentzVector fourVector = new BasicHepLorentzVector(0, 0, 0, 0);

                // Add the cluster to the particle.
                particle.addCluster(unmatchedCluster);

                // Set the reconstructed particle properties based on
                // the cluster properties.
                ((BasicHepLorentzVector) fourVector).setT(unmatchedCluster.getEnergy());
                ((BaseReconstructedParticle) particle).setCharge(0);
                ((BaseReconstructedParticle) particle).set4Vector(fourVector);

                // The particle is assumed to be a photon, since it
                // did not leave any track.
                ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(22, 0, 0, 0));

                if (unmatchedCluster instanceof HPSEcalClusterIC) {
                    int pid = particle.getParticleIDUsed().getPDG();
                    if (pid != 11) {
                        ((HPSEcalClusterIC) unmatchedCluster).setParticleID(pid);
                    }// End of cluster position/energy corrections. 
                }

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
        if (event.hasCollection(Track.class, tracksCollectionName)) {
            tracks = event.get(Track.class, tracksCollectionName);
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
        if (tracksCollectionName == null) {
            tracksCollectionName = "MatchedTracks";
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

    /**
     * Determines if a cluster is a potential match for a given track. If it is,
     * returns the distance between the extrapolation of the track to the
     * z-position of the cluster and the cluster position. Otherwise, returns
     * <code>null</code> to indicate that the pair is not a valid match.
     *
     * @param cluster - The cluster to check.
     * @param track - The track to check.
     * @return Returns the distance between the cluster and extrapolated track
     * position in millimeters as a <code>Double</code> if the pair is a
     * potential match. Returns <code>null</code> otherwise.
     */
    private boolean isMatch(Cluster cluster, Track track) {
        // Get the position of the cluster and extrapolate the position
        // of the track at the z-position of the cluster.

        Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());
        if (cluster instanceof HPSEcalClusterIC) {
            clusterPosition = new BasicHep3Vector(((HPSEcalClusterIC) cluster).getCorrPosition());
        }
        Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());

        // TODO: There are some track whose extrapolated coordinates
        //       are NaN. The problem seems to be that the y-coordinate
        //       of the extrapolated helix is found to be non-real. This
        //       needs to be fixed.
        // There is an issue with track extrapolation that sometimes
        // yields NaN for extrapolated track parameters. Tracks with
        // this issue are not usable and thusly the check should be
        // skipped.
        if (Double.isNaN(trackPosAtEcal.x()) || Double.isNaN(trackPosAtEcal.y())) {
            // VERBOSE :: Indicate the reason for the match failing.
            printDebug("\tFailure :: Track extrapolation error.");

            // Return false to indicate that the pair do not match.
            return false;
        }

        // VERBOSE :: Output the position of the extrapolated track
        //            and the cluster.
        printDebug("\tCluster Position :: " + clusterPosition.toString());
        printDebug("\tTrack Position   :: " + trackPosAtEcal.toString());

        // If one of either the cluster or extrapolated track fall on
        // one volume of the detector and the other is in the other
        // volume, then they can not be a match. (i.e. both parts of
        // the pair must be on the top or bottom of the detector.)
        if (clusterPosition.y() * trackPosAtEcal.y() < 0) {
            // VERBOSE :: Indicate the reason for the match failing.
            printDebug("\tFailure :: Cluster/Track pair in opposite volumes.");

            // Return false to indicate that the pair do not match.
            return false;
        }

        // Check to make sure that the x and y displacements between
        // the extrapolated track position and cluster position are
        // within the allowed bounds. If they are not, this pair is
        // not a match.
        if (Math.abs(trackPosAtEcal.x() - clusterPosition.x()) > dxCut) {
            // VERBOSE :: Indicate the reason for the match failing.
            printDebug("\tFailure :: Pair x-displacement exceeds allowed threshold.");

            // Return false to indicate that the pair do not match.
            return false;
        }

        if (Math.abs(trackPosAtEcal.y() - clusterPosition.y()) > dyCut) {
            // VERBOSE :: Indicate the reason for the match failing.
            printDebug("\tFailure :: Pair y-displacement exceeds allowed threshold.");

            // Return false to indicate that the pair do not match.
            return false;
        }

        // VERBOSE :: Indicate the reason for the match failing.
        printDebug("\tSuccess :: Cluster/track pair match!.");

        // A pair that has reached this point is a potential match.
        // Return true to indicate a match.
        return true;
    }

    // ==============================================================
    // ==== Class Variables =========================================
    // ==============================================================
    // Local variables.
    /**
     * The maximum separation distance in the x-direction beyond which a cluster
     * and track will be rejected for pairing.
     */
    private double dxCut = 20.0;
    /**
     * The maximum separation distance in the y-direction beyond which a cluster
     * and track will be rejected for pairing.
     */
    private double dyCut = 20.0;
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
    private String tracksCollectionName = "MatchedTracks";
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
