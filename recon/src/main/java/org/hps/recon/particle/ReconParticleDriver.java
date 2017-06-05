package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;


/**
 * Driver used to create reconstructed particles and matching clusters
 * and tracks.
 *
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a>
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 */
public abstract class ReconParticleDriver extends Driver {

    /**
     * Utility used to determine if a track and cluster are matched
     */
    TrackClusterMatcher matcher = new TrackClusterMatcher();

    String[] trackCollectionNames = null;

    public static final int ELECTRON = 0;
    public static final int POSITRON = 1;
    public static final int MOLLER_TOP = 0;
    public static final int MOLLER_BOT = 1;
    
    // normalized cluster-track distance required for qualifying as a match:
    private double MAXNSIGMAPOSITIONMATCH=30.0;

    HPSEcal3 ecal;

    protected boolean isMC = false;
    private boolean disablePID = false;
    
    /**
     * Sets the condition of whether the data is Monte Carlo or not.
     * This is used to smear the cluster energy corrections so that
     * the energy resolution is consistent with data. False by default.
     * @param isMC
     */
    public void setIsMC(boolean state) {
        isMC = state;
    }   
    
    
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
     * Sets the beam position in the x-direction.
     *
     * @param X - The beam position at the target in the x-direction in mm.
     */
    public void setBeamPositionX(double X) {
        beamPosition[1] = X;  // The beamPosition array is in the tracking frame HPS X => TRACK Y
    }
    /**
     * Sets the beam size sigma in the x-direction.
     *
     * @param sigmaX - The standard deviation of the beam width in the
     * x-direction.
     */
    public void setBeamSigmaX(double sigmaX) {
        beamSize[1] = sigmaX;  // The beamsize array is in the tracking frame HPS X => TRACK Y
    }

    /**
     * Sets the beam position in the y-direction in mm.
     *
     * @param Y - The position of the beam in the y-direction in mm.
     */
    public void setBeamPositionY(double Y) {
        beamPosition[2] = Y; // The beamPosition array is in the tracking frame HPS Y => TRACK Z
    }
    
    /**
     * Sets the beam size sigma in the y-direction.
     *
     * @param sigmaY - The standard deviation of the beam width in the
     * y-direction.
     */
    public void setBeamSigmaY(double sigmaY) {
        beamSize[2] = sigmaY; // The beamsize array is in the tracking frame HPS Y => TRACK Z
    }  
    
    /**
     * Sets the beam position in the z-direction in mm.
     *
     * @param Z - The position of the beam in the y-direction in mm.
     */
    public void setBeamPositionZ(double Z) {
        beamPosition[0] = Z; // The beamPosition array is in the tracking frame HPS Z => TRACK X
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
     * Set the names of the LCIO track collections used as input.
     *
     * @param trackCollectionNames Array of collection names. If not set, use
     * all Track collections in the event.
     */
    public void setTrackCollectionNames(String[] trackCollectionNames) {
        this.trackCollectionNames = trackCollectionNames;
    }

    /**
     * Set the requirement on cluster-track position matching in terms of N-sigma.
     * 
     * @param nsigma
     */
    public void setNSigmaPositionMatch(double nsigma) {
        MAXNSIGMAPOSITIONMATCH=nsigma;
    }
    
    /** Disable setting the PID of an Ecal cluster. */
    public void setDisablePID(boolean disablePID) { 
        this.disablePID = disablePID;
    }
    
    /**
     * Updates the magnetic field parameters to match the appropriate values for
     * the current detector settings.
     */
    @Override
    protected void detectorChanged(Detector detector) {
        matcher.enablePlots(false);

        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0) {
            flipSign = -1;
        }

        ecal = (HPSEcal3) detector.getSubdetector("Ecal");
        matcher.setBFieldMap(detector.getFieldMap());

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
     * @param trackCollections - The list of event tracks.
     * @return Returns a <code>List</code> collection containing all of the
     * <code>ReconstructedParticle</code> objects generated from the argument
     * data.
     */
    protected List<ReconstructedParticle> makeReconstructedParticles(List<Cluster> clusters, List<List<Track>> trackCollections) {

        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Create a list of unmatched clusters. A cluster should be
        // removed from the list if a matching track is found.
        Set<Cluster> unmatchedClusters = new HashSet<Cluster>(clusters);
        
        // Create a mapping of matched clusters to corresponding tracks.
        HashMap<Cluster, Track> clusterToTrack = new HashMap<Cluster,Track>();
        
        // Loop through all of the track collections and try to match every
        // track to a cluster.  Allow a cluster to be matched to multiple 
        // tracks and use a probability (to be coded later) to determine what 
        // the best match is.
        // TODO: At some point, pull this out to it's own method
        for (List<Track> tracks : trackCollections) {
            
            for (Track track : tracks) {
                
                // Create a reconstructed particle to represent the track.
                ReconstructedParticle particle = new BaseReconstructedParticle();

                // Store the track in the particle.
                particle.addTrack(track);
                
                // Set the type of the particle.  This is used to identify
                // the tracking strategy used in finding the track associated with
                // this particle.
                ((BaseReconstructedParticle) particle).setType(track.getType());

                // Derive the charge of the particle from the track.
                int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega());
                ((BaseReconstructedParticle) particle).setCharge(charge * flipSign);

                // initialize PID quality to a junk value:
                ((BaseReconstructedParticle)particle).setGoodnessOfPid(9999);

                // Extrapolate the particle ID from the track. Positively
                // charged particles are assumed to be positrons and those
                // with negative charges are assumed to be electrons.
                if (particle.getCharge() > 0) {
                    ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
                } else if (particle.getCharge() < 0) {
                    ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));
                }

                // normalized distance of the closest match:
                double smallestNSigma=Double.MAX_VALUE;
               
                // try to find a matching cluster:
                Cluster matchedCluster = null;
                for (Cluster cluster : clusters) {

                    // normalized distance between this cluster and track:
                    final double thisNSigma=matcher.getNSigmaPosition(cluster, particle);

                    // ignore if matching quality doesn't make the cut:
                    if (thisNSigma > MAXNSIGMAPOSITIONMATCH) continue;

                    // ignore if we already found a cluster that's a better match:
                    if (thisNSigma > smallestNSigma) continue;

                    // we found a new best cluster candidate for this track:
                    smallestNSigma = thisNSigma;
                    matchedCluster = cluster;

                    // prefer using GBL tracks to correct (later) the clusters, for some consistency:
                    if (track.getType() >= 32 || !clusterToTrack.containsKey(matchedCluster)) {
                          clusterToTrack.put(matchedCluster,track);
                    }
                }

                // If a cluster was found that matches the track...
                if (matchedCluster != null) {

                    // add cluster to the particle:
                    particle.addCluster(matchedCluster);

                    // use pid quality to store track-cluster matching quality:
                    ((BaseReconstructedParticle)particle).setGoodnessOfPid(smallestNSigma);
                    
                    // propogate pid to the cluster:
                    final int pid = particle.getParticleIDUsed().getPDG();
                    if (Math.abs(pid) == 11) {
                        if (!disablePID) ((BaseCluster) matchedCluster).setParticleId(pid);
                    }

                    // unmatched clusters will (later) be used to create photon particles:
                    unmatchedClusters.remove(matchedCluster);
                }

                // Add the particle to the list of reconstructed particles.
                particles.add(particle);
            }
        }

        // Iterate over the remaining unmatched clusters.
        for (Cluster unmatchedCluster : unmatchedClusters) {

            // Create a reconstructed particle to represent the unmatched cluster.
            ReconstructedParticle particle = new BaseReconstructedParticle();

            // The particle is assumed to be a photon, since it did not leave a track.
            ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(22, 0, 0, 0));

            int pid = particle.getParticleIDUsed().getPDG();
            if (Math.abs(pid) != 11) {
                if (!disablePID) ((BaseCluster) unmatchedCluster).setParticleId(pid);
            }

            // Add the cluster to the particle.
            particle.addCluster(unmatchedCluster);

            // Set the reconstructed particle properties based on the cluster properties.
            ((BaseReconstructedParticle) particle).setCharge(0);

            // Add the particle to the reconstructed particle list.
            particles.add(particle);
        }

        // Apply the corrections to the Ecal clusters using track information, if available
        for (Cluster cluster : clusters) {
            if (cluster.getParticleId() != 0) {
                if (clusterToTrack.containsKey(cluster)){
                    Track matchedT = clusterToTrack.get(cluster);
                    double ypos = TrackUtils.getTrackStateAtECal(matchedT).getReferencePoint()[2];
                    ClusterUtilities.applyCorrections(ecal, cluster, ypos,isMC);
                }
                else {
                    ClusterUtilities.applyCorrections(ecal, cluster,isMC);               
                }
            }
        }

        for (ReconstructedParticle particle : particles) {
            double clusterEnergy = 0;
            Hep3Vector momentum = null;

            if (!particle.getClusters().isEmpty()) {
                clusterEnergy = particle.getClusters().get(0).getEnergy();
            }

            if (!particle.getTracks().isEmpty()) {
                momentum = new BasicHep3Vector(particle.getTracks().get(0).getTrackStates().get(0).getMomentum());
                momentum = CoordinateTransformations.transformVectorToDetector(momentum);
            } else if (!particle.getClusters().isEmpty()) {
                momentum = new BasicHep3Vector(particle.getClusters().get(0).getPosition());
                momentum = VecOp.mult(clusterEnergy, VecOp.unit(momentum));
            }
            HepLorentzVector fourVector = new BasicHepLorentzVector(clusterEnergy, momentum);
            ((BaseReconstructedParticle) particle).set4Vector(fourVector);
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

        // All events are required to contain Ecal clusters. If
        // the event lacks these, then it should be skipped.
        if (!event.hasCollection(Cluster.class, ecalClustersCollectionName)) {
            return;
        }

        // VERBOSE :: Note that a new event is being read.
        printDebug("\nProcessing Event...");

        // Get the list of Ecal clusters from an event.
        List<Cluster> clusters = event.get(Cluster.class, ecalClustersCollectionName);

        // VERBOSE :: Output the number of clusters in the event.
        printDebug("Clusters :: " + clusters.size());

        // Get all collections of the type Track from the event. This is
        // required in case an event contains different track collection
        // for each of the different tracking strategies.  If the event
        // doesn't contain any track collections, intialize an empty 
        // collection and add it to the list of collections.  This is 
        // needed in order to create final state particles from the the 
        // Ecal clusters in the event.
        List<List<Track>> trackCollections = new ArrayList<List<Track>>();
        if (trackCollectionNames != null) {
            for (String collectionName : trackCollectionNames) {
                if (event.hasCollection(Track.class, collectionName)) {
                    trackCollections.add(event.get(Track.class, collectionName));
                }
            }
        } else {
            if (event.hasCollection(Track.class)) {
                trackCollections = event.get(Track.class);
            } else {
                trackCollections.add(new ArrayList<Track>(0));
            }
        }

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

        // Loop through all of the track collections present in the event and 
        // create final state particles.
        finalStateParticles.addAll(makeReconstructedParticles(clusters, trackCollections));

        // VERBOSE :: Output the number of reconstructed particles.
        printDebug("Final State Particles :: " + finalStateParticles.size());

        // Add the final state ReconstructedParticles to the event
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
            printDebug("Beam-Constrained V0 Candidates: " + beamConV0Candidates.size());
            event.put(beamConV0CandidatesColName, beamConV0Candidates, ReconstructedParticle.class, 0);
        }
        if (targetConV0CandidatesColName != null) {
            printDebug("Target-Constrained V0 Candidates: " + targetConV0Candidates.size());
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
            printDebug("Target-Constrained V0 Vertices: " + targetConV0Vertices.size());
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
    protected boolean debug = false;

    /**
     * Indicates whether this is Monte Carlo or data
     */
    public boolean isMonteCarlo = false;
    
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
    protected double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    // Beam position variables.
    // The beamPosition array is in the tracking frame
    /* TODO get the beam position from the conditions db */
    protected double[] beamPosition = {0.0, 0.0, 0.0}; //
    protected double bField;

    //  flipSign is a kludge...
    //  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    //  so we set the B-field in +iveZ and flip signs of fitted tracks
    //  
    //  Note:  This should be -1 for test run configurations and +1 for 
    //         prop-2014 configurations 
    private int flipSign = 1;
}
