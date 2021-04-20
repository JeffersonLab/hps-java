package org.hps.recon.particle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.utils.TrackClusterMatcherFactory;
import org.hps.record.StandardCuts;

public class FinalStateParticleDriver extends Driver {

    /** LCIO collection name for calorimeter clusters. */
    private String ecalClustersCollectionName = "";

    /** LCIO collection name for tracks. */
    private String trackCollectionName = "";

    /** Track cluster matcher. */
    private TrackClusterMatcher matcher;

    /** Beam energy. This is retrieved from the conditions database. */
    private double beamEnergy = 0;

    /** MOUSE cuts */
    private StandardCuts cuts = null;

    /** The name of the track-cluster matching algorithm to use. */
    private String trackClusterMatcherAlgo = "TrackClusterMatcherMinDistance";

    /** Enable/disable track-cluster matching plots. */
    private boolean enableTrackClusterMatchPlots = false;

    /**
     * These are new for 2019 running and should be set to false in the steering file.
     * Default values should replicate correct behavior for 2015 and 2016 data.
     */
    private boolean useTrackPositionForClusterCorrection = true;

    /**
     * flipSign is a kludge...
     * HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
     * so we set the B-field in +iveZ and flip signs of fitted tracks
     * Note: This should be -1 for test run configurations and +1 for
     * prop-2014 configurations
     */
    private int flipSign = 1;

    /** Flag denoting whether the data being processed is Monte Carlo. */
    private boolean isMC = false;

    /** The ECal geometry. */
    private HPSEcal3 ecal;
    
    /** ? */
    private boolean disablePID = false;

    /** ? */
    private boolean applyClusterCorrections = true;
    
    /** Name of the final state particles collection. */
    private String finalStateParticlesCollName = ""; 

    /**
     * Sets the LCIO collection name for calorimeter cluster data.
     *
     * @param ecalClustersCollectionName The LCIO collection name.
     */
    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    /**
     * Sets the LCIO collection name for particle track data.
     *
     * @param trackCollectionName The LCIO collection name.
     */
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    /**
     * Enable/disable the creation and persistence of the track-cluster
     * matching plots.
     * 
     * @param enable True to enable plots, false otherwise.
     */
    public void setEnableTrackClusterMatchPlots(boolean enable) {
        enableTrackClusterMatchPlots = enable;
    }

    /**
     * Enable/disable the use of the track position for correcting the
     * cluster.
     * 
     * @param enable True to enable the correction, false otherwise.
     */
    public void setUseTrackPositionForClusterCorrection(boolean enable) {
        useTrackPositionForClusterCorrection = enable;
    }

    /**
     * Sets the condition of whether the data being processed is Monte Carlo.
     * This is used to smear the cluster energy corrections so that the energy
     * resolution is consistent with data. False by default.
     *
     * @param state Set to true if the data is MC, false otherwise.
     */
    public void setIsMC(boolean state) {
        isMC = state;
    }

    @Override
    final protected void detectorChanged(Detector detector) {

        // Get the beam energy from the conditions database
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();

        // Instantiate the "MOUSE" cuts
        cuts = new StandardCuts(beamEnergy);

        // By default, use the original track-cluster matching class
        matcher = TrackClusterMatcherFactory.create(trackClusterMatcherAlgo);
        matcher.setBFieldMap(detector.getFieldMap());
        matcher.setTrackCollectionName(trackCollectionName);
        matcher.enablePlots(enableTrackClusterMatchPlots);

        // Retrieve the ECal geometry
        ecal = (HPSEcal3) detector.getSubdetector("Ecal");

    }

    @Override
    final protected void process(EventHeader event) {

        // Get the collection of ECal clusters from the event. If there are no
        // clusters in the event, create an empty collection to pass to the
        // matcher.
        List<Cluster> clusters = event.hasCollection(Cluster.class, ecalClustersCollectionName)
                ? event.get(Cluster.class, ecalClustersCollectionName)
                : new ArrayList<Cluster>();
        System.out.println("Cluster size: " + clusters.size());

        // Get the collection of tracks from the event. If a collection
        // of tracks isn't found, create an empty collection to pass to the
        // matcher.
        List<Track> tracks = event.hasCollection(Track.class, trackCollectionName)
                ? event.get(Track.class, trackCollectionName)
                : new ArrayList<Track>();
        System.out.println("Track size: " + tracks.size());

        // Loop through all of the track collections present in the event and
        // create final state particles. These particles include those
        // which are composed by tracks or clusters only as well as
        // track-cluster pairs.
        List<ReconstructedParticle> finalStateParticles = makeFinalStateParticles(event, clusters, tracks);
    
        // Add the final state ReconstructedParticles to the event
        event.put(finalStateParticlesCollName, finalStateParticles, ReconstructedParticle.class, 0);
    
    }

    private List<ReconstructedParticle> makeFinalStateParticles(EventHeader event, List<Cluster> clusters,
            List<Track> tracks) {

        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Create the list of list needed by the track-cluster matcher
        // TODO: OM: The track-cluster matchers need to be updated such that they just take
        // a single list of tracks i.e. List<*> versus List<List<*>>.
        List<List<Track>> trackColl = new ArrayList<List<Track>>();
        trackColl.add(tracks);

        // Matcher returns a mapping of Tracks with matched Clusters.
        HashMap<Track, Cluster> trackClusterPairs = matcher.matchTracksToClusters(event, trackColl, clusters, cuts,
                flipSign, useTrackPositionForClusterCorrection, isMC, ecal, beamEnergy);
        
        // Create a list of final state particles that are composed of a track
        // or a track-cluster pair.
        trackClusterPairs.entrySet().parallelStream().filter(entry -> entry.getKey() != null)
                .forEach(entry -> particles.add(makeFinalStateParticle(entry.getKey(), entry.getValue())));

        // Create a list of final state particles out of unmatched clusters. These
        // would be track-cluster pairs where the track == null.
        trackClusterPairs.entrySet().parallelStream().filter(entry -> entry.getKey() == null)
                .forEach(entry -> particles.add(makeReconstructedParticle(entry.getValue())));
        
        // Apply the corrections to the Ecal clusters using track information, if available
        if (applyClusterCorrections) {
            matcher.applyClusterCorrections(useTrackPositionForClusterCorrection, clusters, beamEnergy, ecal, isMC);
        }
        return particles;
    }

    private ReconstructedParticle makeFinalStateParticle(Track track, Cluster cluster) {

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

}
