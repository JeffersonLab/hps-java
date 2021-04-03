package org.hps.recon.particle;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.record.StandardCuts;

import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.utils.TrackClusterMatcherFactory;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;

/**
 * Driver used to create reconstructed particles and matching clusters and tracks.
 */
public abstract class ReconParticleDriver extends Driver {

    private String clusterParamFileName = null;

    // normalized cluster-track distance required for qualifying as a match:
    // private double MAXNSIGMAPOSITIONMATCH = 15.0;

    HPSEcal3 ecal;

    protected boolean isMC = false;
    private boolean disablePID = false;

    // Standard set of cuts applied to the tracks and clusters.
    // TODO: OM: Does this really belong here or should this be moved to its own
    // driver. Maybe this can be turned into a singleton.
    private StandardCuts cuts = new StandardCuts();

    // Track to Cluster matching algorithms interfaced from
    // TrackClusteMatcherInter and the specific algorithm is chosen by name using
    // TrackClusterMatcherFactory
    TrackClusterMatcher matcher;

    protected boolean enableTrackClusterMatchPlots = false;

    boolean useCorrectedClusterPositionsForMatching = false;

    // These are new for 2019 running and should be set to false in the steering file.
    // Default values should replicate correct behavior for 2015 and 2016 data
    boolean useTrackPositionForClusterCorrection = true;
    boolean applyClusterCorrections = true;

    /**
     * LCIO collection name for calorimeter clusters.
     */
    protected String ecalClustersCollectionName = "EcalClustersCorr";
    /**
     * LCIO collection name for tracks.
     */
    protected String trackCollectionName = "GBLTracks";
    /**
     * Track Cluster Algorithm set to Kalman or GBL Tracks
     */
    private String trackClusterMatcherAlgo = "TrackClusterMatcherNSigma";
    /**
     * LCIO collection name for reconstructed particles.
     */
    protected String finalStateParticlesColName = "FinalStateParticles";

    // The beam energy used to configure the standard cuts.
    double beamEnergy;

    // flipSign is a kludge...
    // HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    // so we set the B-field in +iveZ and flip signs of fitted tracks
    //
    // Note: This should be -1 for test run configurations and +1 for
    // prop-2014 configurations
    private int flipSign = 1;

    /**
     * Sets the condition of whether the data is Monte Carlo or not. This is
     * used to smear the cluster energy corrections so that the energy
     * resolution is consistent with data. False by default.
     *
     * @param isMC
     */
    public void setIsMC(boolean state) {
        isMC = state;
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
     * Sets the LCIO collection name for particle track data.
     *
     * @param trackCollectionName - The LCIO collection name.
     */
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    /**
     * Selects track-EcalCluster matching algorithm - TrackClusterMatcherNSigma or
     * TrackClusterMatcherMinDistance
     *
     * @param trackClusterMatcherAlgo - GBL or Kalman Track.
     */
    public void setTrackClusterMatcherAlgo(String trackClusterMatcherAlgo) {
        this.trackClusterMatcherAlgo = trackClusterMatcherAlgo;
    }

    /**
     * Set the requirement on cluster-track position matching in terms of
     * N-sigma.
     *
     * @param nsigma
     */
    /*
     * public void setNSigmaPositionMatch(double nsigma) {
     * MAXNSIGMAPOSITIONMATCH = nsigma;
     * }
     */

    /**
     * Disable setting the PID of an Ecal cluster.
     */
    public void setDisablePID(boolean disablePID) {
        this.disablePID = disablePID;
    }

    public void setClusterParamFileName(String input) {
        clusterParamFileName = input;
    }

    public void setTrackClusterMatchPlots(boolean input) {
        enableTrackClusterMatchPlots = input;
    }

    public void setUseCorrectedClusterPositionsForMatching(boolean val) {
        useCorrectedClusterPositionsForMatching = val;
    }

    public void setUseTrackPositionForClusterCorrection(boolean val) {
        useTrackPositionForClusterCorrection = val;
    }

    public void setApplyClusterCorrections(boolean val) {
        applyClusterCorrections = val;
    }

    public void setMaxMatchChisq(double input) {
        cuts.setMaxMatchChisq(input);
    }

    public void setMaxElectronP(double input) {
        cuts.setMaxElectronP(input);
    }

    public void setMaxMatchDt(double input) {
        cuts.setMaxMatchDt(input);
    }

    public void setTrackClusterTimeOffset(double input) {
        cuts.setTrackClusterTimeOffset(input);
    }
    
    /**
     * Updates the magnetic field parameters to match the appropriate values for
     * the current detector settings.
     */
    @Override
    protected void detectorChanged(Detector detector) {

        // Get the beam energy from the conditions database
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();

        // Initialize the track-cluster matcher
        // TODO: OM: Probably wont need this once the track-cluster matcher is
        // updated.
        if (clusterParamFileName == null) {
            if (beamEnergy > 2) {
                setClusterParamFileName("ClusterParameterization2016.dat");
            } else {
                setClusterParamFileName("ClusterParameterization2015.dat");
            }
        }

        // By default, use the original track-cluster matching class
        matcher = TrackClusterMatcherFactory.create(trackClusterMatcherAlgo);
        matcher.initializeParameterization(clusterParamFileName);
        matcher.setBFieldMap(detector.getFieldMap());
        matcher.enablePlots(enableTrackClusterMatchPlots);

        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
        double bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0) {
            flipSign = -1;
        }

        ecal = (HPSEcal3) detector.getSubdetector("Ecal");

        // Set the beam energy used by the cuts
        cuts.changeBeamEnergy(beamEnergy);
    }

    // protected abstract List<ReconstructedParticle> particleCuts(List<ReconstructedParticle> finalStateParticles);

    /**
     *
     */
    private ReconstructedParticle makeReconstructedParticle(Track track, Cluster cluster) {

        // Create a reconstructed particle to represent the track or track-cluster pair.
        BaseReconstructedParticle particle = new BaseReconstructedParticle();

        // Store the track in the particle.
        particle.addTrack(track);

        // Derive the charge of the particle from the track.
        int charge = ((int) Math.signum(track.getTrackStates().get(0).getOmega())) * flipSign;
        particle.setCharge(charge);

        // initialize PID quality to a junk value:
        particle.setGoodnessOfPid(Integer.MIN_VALUE);

        // Extrapolate the particle ID from the track. Positively
        // charged particles are assumed to be positrons and those
        // with negative charges are assumed to be electrons.
        int pdgID = (charge != 0) && charge > 0 ? -11 : 11;
        particle.setParticleIdUsed(new SimpleParticleID(pdgID, 0, 0, 0));

        // If a cluster was found that matches the track...
        double clusterEnergy = 0.;
        if (cluster != null) {

            // Get the cluster energy
            clusterEnergy = cluster.getEnergy();

            // Add cluster to the particle:
            particle.addCluster(cluster);

            // Use PID quality to store track-cluster matching quality:
            particle.setGoodnessOfPid(matcher.getMatchQC(cluster, particle));

            // Propogate PID to the cluster
            if ((Math.abs(pdgID) == 11) && !disablePID)
                ((BaseCluster) cluster).setParticleId(pdgID);
        }

        // Calculate and set the momentum of the track
        Hep3Vector momentum = CoordinateTransformations.transformVectorToDetector(
                new BasicHep3Vector(particle.getTracks().get(0).getTrackStates().get(0).getMomentum()));
        particle.set4Vector(new BasicHepLorentzVector(clusterEnergy, momentum));

        return particle;
    }

    /**
     *
     * @param track
     * @param cluster
     * @return
     */
    private ReconstructedParticle makeReconstructedParticle(Cluster cluster) {

        // Create a reconstructed particle to represent the unmatched cluster.
        BaseReconstructedParticle particle = new BaseReconstructedParticle();

        // The particle is assumed to be a photon, since it did not leave a track.
        int pdgID = 22;
        particle.setParticleIdUsed(new SimpleParticleID(pdgID, 0, 0, 0));

        if (!disablePID)
            ((BaseCluster) cluster).setParticleId(pdgID);

        // Add the cluster to the particle.
        particle.addCluster(cluster);

        // Set the reconstructed particle properties based on the cluster properties.
        particle.setCharge(0);

        // Set the momentum of the cluster based on its energy
        double clusterEnergy = cluster.getEnergy();
        Hep3Vector momentum = new BasicHep3Vector(particle.getClusters().get(0).getPosition());
        momentum = VecOp.mult(clusterEnergy, VecOp.unit(momentum));
        particle.set4Vector(new BasicHepLorentzVector(clusterEnergy, momentum));

        return particle;
    }

    /**
     * Create the set of final state particles from the event tracks and
     * clusters. Clusters will be matched with tracks when possible.
     *
     * @param clusters - The list of event clusters.
     * @param trackCollections - The list of event tracks.
     * @return Returns a <code>List</code> collection containing all of the
     * <code>ReconstructedParticle</code> objects generated from the argument
     * data.
     */
    protected List<ReconstructedParticle> makeReconstructedParticles(EventHeader event, List<Cluster> clusters,
            List<Track> tracks) {

        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Matcher returns a mapping of Tracks with matched Clusters.
        HashMap<Track, Cluster> trackClusterPairs = matcher.matchTracksToClusters(event, tracks, clusters, cuts,
                flipSign, useTrackPositionForClusterCorrection, isMC, ecal, beamEnergy);

        // Create a list of final state particles that are composed of a track
        // or a track-cluster pair.
        trackClusterPairs.entrySet().parallelStream().filter(entry -> entry.getKey() != null)
                .forEach(entry -> particles.add(makeReconstructedParticle(entry.getKey(), entry.getValue())));

        // Create a list of final state particles out of unmatched clusters. These
        // would be track-cluster pairs where the track == null.
        trackClusterPairs.entrySet().parallelStream().filter(entry -> entry.getKey() == null)
                .forEach(entry -> particles.add(makeReconstructedParticle(entry.getValue())));

        // Apply the corrections to the Ecal clusters using track information, if available
        if (applyClusterCorrections) {
            matcher.applyClusterCorrections(useTrackPositionForClusterCorrection, clusters, beamEnergy, ecal, isMC);
        }

        // recalculate track-cluster matching n_sigma using corrected cluster positions
        // if that option is selected
        if (useCorrectedClusterPositionsForMatching) {
            particles.parallelStream().filter(particle -> !particle.getClusters().isEmpty())
                    .forEach(particle -> ((BaseReconstructedParticle) particle)
                            .setGoodnessOfPid(matcher.getMatchQC(particle.getClusters().get(0), particle)));
        }

        // Return the list of reconstructed particles.
        return particles;
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

        // Get the collection of ECal clusters from the event. If there are no
        // clusters in the event, create an empty collection to pass to the
        // matcher.
        List<Cluster> clusters = event.hasCollection(Cluster.class, ecalClustersCollectionName)
                ? event.get(Cluster.class, ecalClustersCollectionName)
                : new ArrayList<Cluster>();

        // Get the collection of tracks from the event. If a collection
        // of tracks isn't found, create an empty collection to pass to the
        // matcher.
        List<Track> tracks = event.hasCollection(Track.class, trackCollectionName)
                ? event.get(Track.class, trackCollectionName)
                : new ArrayList<Track>();

        // hitToRotated = TrackUtils.getHitToRotatedTable(event);
        // hitToStrips = TrackUtils.getHitToStripsTable(event);

        // Loop through all of the track collections present in the event and
        // create final state particles. These particles include those
        // which are composed by tracks or clusters only as well as
        // track-cluster pairs.
        List<ReconstructedParticle> finalStateParticles = makeReconstructedParticles(event, clusters, tracks);

        // Separate the reconstructed particles into electrons and
        // positrons so that V0 candidates can be generated from them.
        /*
         * Predicate<ReconstructedParticle> isElectron = particle -> particle.getCharge() < 0;
         * Predicate<ReconstructedParticle> isPositron = particle -> particle.getCharge() > 0;
         * List<ReconstructedParticle> electrons = finalStateParticles.parallelStream().filter(isElectron)
         * .collect(Collectors.toList());
         * List<ReconstructedParticle> positrons = finalStateParticles.parallelStream().filter(isPositron)
         * .collect(Collectors.toList());
         */

        // List<ReconstructedParticle> goodFinalStateParticles = particleCuts(finalStateParticles);

        // Add the final state ReconstructedParticles to the event
        event.put(finalStateParticlesColName, finalStateParticles, ReconstructedParticle.class, 0);
    }

    @Override
    protected void endOfData() {
        if (enableTrackClusterMatchPlots) {
            matcher.saveHistograms();
        }
    }

    public void setSnapToEdge(boolean val) {
        this.matcher.setSnapToEdge(val);
    }
}
