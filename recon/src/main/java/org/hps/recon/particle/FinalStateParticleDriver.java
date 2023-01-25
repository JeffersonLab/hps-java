package org.hps.recon.particle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.javatuples.Triplet;

import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
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

/**
 * A Driver used to create unvertexed ReconstructedParticles from SVT tracks 
 * and ECal clusters.  First, all track-cluster matches are found and used 
 * to create ReconstructedParticles.  The remaining unmatched tracks and 
 * clusters are also made into recon particles. The collection of 
 * recon particles (FinalStateParticles) is then added to the event. 
 */
public class FinalStateParticleDriver extends Driver {

    /// Collection of ECal clusters
    private String ecalClustersCollectionName = "";

    /// Collection of SVT Tracks
    private String trackCollectionName = "";

    /// Track cluster matcher.
    private TrackClusterMatcher matcher;

    /// The name of the track-cluster matching algorithm to use. 
    private String trackClusterMatcherAlgo = "TrackClusterMatcherMinDistance";
   
    /// Enable/disable track-cluster matching plots.
    private boolean enableTrackClusterMatchPlots = false;

    /// Beam energy. This is retrieved from the conditions database. 
    private double beamEnergy = 0;

    /// MOUSE cuts 
    private StandardCuts cuts = null;

    /**
     * These are new for 2019 running and should be set to false in the steering
     * file. Default values should replicate correct behavior for 2015 and 2016
     * data.
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

    ///  Flag denoting whether the data being processed is Monte Carlo. 
    private boolean isMC = false;
  
    /// The ECal geometry. 
    private HPSEcal3 ecal;

    /// Flag to enable/disable setting of the PID
    private boolean disablePID = false;

    /// Flag indicating whether a track should be used to correct a cluster.
    private boolean applyClusterCorrections = true;

    /// Name of the final state particles collection. 
    private String finalStateParticlesCollName = "FinalStateParticles";

    /**
     * Sets the LCIO collection name of the ECal clusters used to create final
     * state particles.
     *
     * @param ecalClustersCollectionName The name of the collection of ECal 
     *  clusters.
     */
    public void setEcalClusterCollectionName(String ecalClustersCollectionName) {
        this.ecalClustersCollectionName = ecalClustersCollectionName;
    }

    /**
     * Sets the LCIO collection name of the SVT tracks that will be used to 
     * create final state particles.
     *
     * @param trackCollectionName The name of the collection of SVT tracks.
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

    /**
     * Called by the framework before the process method when the detector 
     * geometry changes. This method is gauranteed to be called once before 
     * the first call to process.
     *
     * @param Detector The new detector
     */
    @Override
    final protected void detectorChanged(Detector detector) { 
  
        // Get the beam energy from the conditions database
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(
                        BeamEnergyCollection.class, "beam_energies")
                .getCachedData();
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

    /**
     * Called by the framework to process an event. 
     *
     * @param event The event to be processed
     */
    @Override
    final protected void process(EventHeader event) {

        // Get the collection of ECal clusters from the event.  If there are no
        // clusters in the event, create an empty collection to pass to the matcher.
        List<Cluster> clusters 
                = event.hasCollection(Cluster.class, ecalClustersCollectionName)
                ? event.get(Cluster.class, ecalClustersCollectionName)
                : new ArrayList<Cluster>();

        // Get the collection of tracks from the event.  If a collection
        // of tracks isn't found, create an empty collection to pass to the 
        // matcher.
        List<Track> tracks 
                = event.hasCollection(Track.class, trackCollectionName)
                ? event.get(Track.class, trackCollectionName)
                : new ArrayList<Track>();
      
        // Loop through all of the track collections present in the event and 
        // create the collection of final state particles, electrons and 
        // positrons. These particles include those which are composed by 
        // tracks or clusters only as well as track-cluster pairs.
        Triplet<List<ReconstructedParticle>, List<ReconstructedParticle>, 
                List<ReconstructedParticle>> finalStateParticles 
                    = makeFinalStateParticles(event, clusters, tracks);

        // Add the ReconstructedParticles to the event. 
        event.put(finalStateParticlesCollName, 
                  finalStateParticles.getValue0(), ReconstructedParticle.class, 0);
        event.put("FinalStateElectrons", 
                  finalStateParticles.getValue1(), ReconstructedParticle.class, 0);
        event.put("FinalStatePositrons", 
                  finalStateParticles.getValue2(), ReconstructedParticle.class, 0);
       
        LCMetaData fseMeta = event.getMetaData(finalStateParticles.getValue1());
        fseMeta.setTransient(true);

        LCMetaData fspMeta = event.getMetaData(finalStateParticles.getValue2());
        fspMeta.setTransient(true);
    }

    /**
     * Create a collection of ReconstructedParticles from a list of tracks
     * and clusters.  The collection of ReconstructedParticles will include
     * particles where a track-cluster have been matched and those that 
     * are made of just tracks or clusters. 
     *
     * @param event The event to be processed.
     * @param clusters The collection of ECal clusters. 
     * @param tracks The collection of SVT tracks.
     * @return A collection of ReconstructedParticles.
     */
    private Triplet<List<ReconstructedParticle>, List<ReconstructedParticle>, 
            List<ReconstructedParticle>> makeFinalStateParticles(
            EventHeader event, List<Cluster> clusters, List<Track> tracks) { 
  
        // Create the list of list needed by the track-cluster matcher
        // TODO: OM: The track-cluster matchers need to be updated such that 
        //  they just take a single list of tracks i.e. List<*> versus 
        //  List<List<*>>.
        List<List<Track>> trackColl = new ArrayList<List<Track>>();
        trackColl.add(tracks);

        // Matcher returns a mapping of Tracks with matched Clusters.
        HashMap<Track, Cluster> trackClusterPairs 
                = matcher.matchTracksToClusters(event, trackColl, clusters, cuts,
                flipSign, useTrackPositionForClusterCorrection, isMC, ecal, 
                beamEnergy);

        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles 
                = new ArrayList<ReconstructedParticle>();
        List<ReconstructedParticle> electrons
                = new ArrayList<ReconstructedParticle>(); 
        List<ReconstructedParticle> positrons
                = new ArrayList<ReconstructedParticle>(); 

        // Create the list of final state particles, electrons and positrons.
        // These list are composed of ReconstructedParticles that have a track
        // or a track-cluster pair. 
        trackClusterPairs.entrySet().parallelStream()
                .filter(entry -> entry.getKey() != null)
                .forEach(entry -> makeCollections(particles, electrons, 
                    positrons, entry.getKey(), entry.getValue())); 

        // Create a list of final state particles out of unmatched clusters.
        // These would be track-cluster pairs where the track == null.
        trackClusterPairs.entrySet().parallelStream()
                .filter(entry -> entry.getKey() == null)
                .forEach(entry -> particles.add(
                            makeReconstructedParticle(entry.getValue())));

        // If available, apply the corrections to the Ecal clusters using track
        // information.
        if (applyClusterCorrections) {
            matcher.applyClusterCorrections(
                    useTrackPositionForClusterCorrection, clusters, beamEnergy,
                    ecal, isMC);
        }
        
        // Add the collections to a triplet tuple and return.
        return new Triplet<List<ReconstructedParticle>, 
            List<ReconstructedParticle>, List<ReconstructedParticle>>(
            particles, electrons, positrons); 

    }

    /**
     * Create a FinalStateParticle and add it to the appropriate collection. 
     * 
     * @param particles The collections of FinalStateParticle's to add the 
     *  created particles.
     * @param electrons The collection of FinalStateParticle's identified as
     *  as electrons.
     * @param positrons The collection of FinalStateParticle's identified as
     *  as positrons.
     */
    private void makeCollections(List<ReconstructedParticle> particles, 
            List<ReconstructedParticle> electrons, 
            List<ReconstructedParticle> positrons, Track track, 
            Cluster cluster) {
        
        // Create the FinalStateParticles out of the track and clusters. 
        ReconstructedParticle particle 
                = makeFinalStateParticle(track, cluster); 
        particles.add(particle); 

        // If the particle is identified as an electron or positrons, add
        // it to the appropriate collection.
        if (particle.getParticleIDUsed().getPDG() == 11) 
            electrons.add(particle);
        else positrons.add(particle);
    }

    /**
     * Create a ReconParticle out of a track and cluster or a track alone. 
     * If  track-cluster match has been found, this method is used to create 
     * a recon particle and to set all available information about the match.
     * This method also handles the case where a track doesn't have a matching
     * cluster.
     *
     * @param track The SVT track used to build this recon particle.
     * @param cluster The ECal cluster used to build this recon particle.
     * @return The ReconstructedParticle object.
     */
    private ReconstructedParticle makeFinalStateParticle(Track track, Cluster cluster) {

        // Create a reconstructed particle to represent the track or 
        // track-cluster pair.
        BaseReconstructedParticle particle = new BaseReconstructedParticle();

        // Store the track in the particle.
        particle.addTrack(track);

        // Derive the charge of the particle from the track.
        int charge = ((int) Math.signum(
                    track.getTrackStates().get(0).getOmega())) * flipSign;
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
        Hep3Vector momentum 
                = CoordinateTransformations.transformVectorToDetector(
                new BasicHep3Vector(
                    particle.getTracks()
                            .get(0).getTrackStates().get(0).getMomentum()));
        particle.set4Vector(new BasicHepLorentzVector(clusterEnergy, momentum));

        return particle;
    }
    
    /**
     * Make a ReconstructedParticle out of a cluster.  
     *
     * @param cluster The ECal cluster used to make the recon particle.
     * @return A ReconstructedParticle.
     */
    private ReconstructedParticle makeReconstructedParticle(Cluster cluster) {

        // Create a reconstructed particle to represent the unmatched cluster.
        BaseReconstructedParticle particle = new BaseReconstructedParticle();

        // The particle is assumed to be a photon, since it did not leave a
        // track.
        int pdgID = 22;
        particle.setParticleIdUsed(new SimpleParticleID(pdgID, 0, 0, 0));

        if (!disablePID)
            ((BaseCluster) cluster).setParticleId(pdgID);

        // Add the cluster to the particle.
        particle.addCluster(cluster);

        // Set the reconstructed particle properties based on the cluster 
        // properties.
        particle.setCharge(0);

        // Set the momentum of the cluster based on its energy
        double clusterEnergy = cluster.getEnergy();
        Hep3Vector momentum 
                = new BasicHep3Vector(particle.getClusters().get(0)
                                              .getPosition());
        momentum = VecOp.mult(clusterEnergy, VecOp.unit(momentum));
        particle.set4Vector(
                new BasicHepLorentzVector(clusterEnergy, momentum));

        return particle;
    }
}
