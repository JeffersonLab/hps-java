package org.hps.users.omoreno;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.util.Driver; 
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;

import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;

/**
 * Analysis driver used to study SVT strip clusters. 
 *  
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 */
public class SvtClusterAnalysis extends Driver {

    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
    private Map<Track, ReconstructedParticle> reconParticleMap = new HashMap<Track, ReconstructedParticle>();
  
    // Plotting
    private ITree tree = null; 
    private IHistogramFactory histogramFactory = null; 
   
    //----------------//
    //   Histograms   //
    //----------------//
    
    // Histograms of all clusters in an event
    private Map<SiSensor, IHistogram1D> clusterChargePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> singleHitClusterChargePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> multHitClusterChargePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> signalToNoisePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> singleHitSignalToNoisePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> multHitSignalToNoisePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> clusterSizePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> clusterTimePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram2D> clusterChargeVsTimePlots = new HashMap<SiSensor, IHistogram2D>();

    // Histograms of clusters associated with a track
    private Map<SiSensor, IHistogram1D> trackClusterChargePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> trackHitSignalToNoisePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram1D> trackClusterTimePlots = new HashMap<SiSensor, IHistogram1D>();
    private Map<SiSensor, IHistogram2D> trackClusterChargeVsMomentum = new HashMap<SiSensor, IHistogram2D>();
    private Map<SiSensor, IHistogram2D> trackClusterChargeVsCosTheta = new HashMap<SiSensor, IHistogram2D>();
    private Map<SiSensor, IHistogram2D> trackClusterChargeVsSinPhi = new HashMap<SiSensor, IHistogram2D>();
    
    // Detector name
    private String subdetectorName = "Tracker";
    
    // Collections
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String stereoHitRelationsColName = "HelicalTrackHitRelations";
    private String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";
    private String fsParticlesCollectionName = "FinalStateParticles";
    
    private int runNumber = -1; 
        
    //-------------//
    //   Setters   //
    //-------------//
    
    /**
     * Set the name of the sub-detector from which sensors will be retrieved.
     * 
     * @param subdetectorName Name of the sub-detector of interest.
     */
    public void setSubdetectorName(String subdetectorName) { 
       this.subdetectorName = subdetectorName; 
    }
    
    /** Default Constructor */
    public SvtClusterAnalysis() { }
    
    protected void detectorChanged(Detector detector) {
       
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(subdetectorName)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        for (HpsSiSensor sensor : sensors) { 
           
            // Get the name of the sensor
            String sensorName = sensor.getName();
            clusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Cluster Charge", 100, 0, 5000));
       
            singleHitClusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Single Hit Cluster Charge", 100, 0, 5000));

            multHitClusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Multiple Hit Cluster Charge", 100, 0, 5000));

            signalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Signal to Noise", 50, 0, 50));

            singleHitSignalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Single Hit Signal to Noise", 50, 0, 50));
        
            multHitSignalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Multiple Hit Signal to Noise", 50, 0, 50));

            clusterSizePlots.put(sensor,
                    histogramFactory.createHistogram1D(sensorName + " - Cluster Multiplicity", 10, 0, 10));
            
            clusterTimePlots.put(sensor,
                    histogramFactory.createHistogram1D(sensorName + " - Cluster Time", 100, -100, 100));

            trackClusterTimePlots.put(sensor,
                    histogramFactory.createHistogram1D(sensorName + " - Track Cluster Time", 100, -100, 100));
            
            clusterChargeVsTimePlots.put(sensor,
                    histogramFactory.createHistogram2D(sensorName + " - Cluster Amplitude vs Time", 100, 0, 5000, 100, -100, 100));
        
            trackClusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Tracker Cluster Charge", 100, 0, 5000));
        
            trackHitSignalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Track Signal to Noise", 50, 0, 50));
        
            trackClusterChargeVsMomentum.put(sensor,
                    histogramFactory.createHistogram2D(sensorName + " - Cluster Amplitude vs Momentum", 100, 0, 1.5, 100, 0, 5000));
        
            trackClusterChargeVsCosTheta.put(sensor,
                    histogramFactory.createHistogram2D(sensorName + " - Cluster Amplitude vs cos(theta)", 100, -0.1, 0.1, 100, 0, 5000));

            trackClusterChargeVsSinPhi.put(sensor,
                    histogramFactory.createHistogram2D(sensorName + " - Cluster Amplitude vs sin(phi0)", 100, -0.2, 0.2, 100, 0, 5000));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void process(EventHeader event) { 
     
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        // If the event doesn't contain fitted raw hits, skip it
        if (!event.hasCollection(LCRelation.class, fittedHitsCollectionName)) return;
        
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
        
        // Map the fitted hits to their corresponding raw hits
        this.mapFittedRawHits(fittedHits);
        
        // If the event doesn't contain any clusters, skip it
        if (!event.hasCollection(TrackerHit.class, clusterCollectionName)) return;
        
        // Get the list of clusters in the event
        List<TrackerHit> clusters = event.get(TrackerHit.class, clusterCollectionName);
        System.out.println("Number of clusters: " + clusters.size()); 
       
        for (TrackerHit cluster : clusters) { 
           
            // Get the sensor associated with this cluster
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) cluster.getRawHits().get(0)).getDetectorElement();
            
            Cluster clusterObject = this.calculateSignalToNoise(cluster.getRawHits());
            
            clusterSizePlots.get(sensor.getName()).fill(cluster.getRawHits().size());
            
            // Fill all plots
            clusterChargePlots.get(sensor.getName()).fill(clusterObject.getAmplitude());
            signalToNoisePlots.get(sensor.getName()).fill(clusterObject.getSignalToNoise());
            clusterTimePlots.get(sensor.getName()).fill(cluster.getTime());
            clusterChargeVsTimePlots.get(sensor.getName()).fill(clusterObject.getAmplitude(), cluster.getTime());
            
            if (cluster.getRawHits().size() == 1) { 
                singleHitClusterChargePlots.get(sensor.getName()).fill(clusterObject.getAmplitude());
                singleHitSignalToNoisePlots.get(sensor.getName()).fill(clusterObject.getSignalToNoise());
            } else { 
                multHitClusterChargePlots.get(sensor.getName()).fill(clusterObject.getAmplitude());
                multHitSignalToNoisePlots.get(sensor.getName()).fill(clusterObject.getSignalToNoise());
            }
        }
       
        if (!event.hasCollection(Track.class, "MatchedTracks")) return;
        
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        
        // Get the collection of LCRelations between a stereo hit and the strips making it up
        List<LCRelation> stereoHitRelations = event.get(LCRelation.class, stereoHitRelationsColName);
        BaseRelationalTable stereoHitToClusters = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : stereoHitRelations) { 
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                stereoHitToClusters.add(relation.getFrom(), relation.getTo());
            }
        }
        
        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits
        List<LCRelation> rotatedHthToHthRelations = event.get(LCRelation.class, rotatedHthRelationsColName);
        BaseRelationalTable hthToRotatedHth = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : rotatedHthToHthRelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hthToRotatedHth.add(relation.getFrom(), relation.getTo());
            }
        }
        
        // Get the list of final state particles from the event.  These will
        // be used to obtain the track momentum.
        List<ReconstructedParticle> fsParticles = event.get(ReconstructedParticle.class, fsParticlesCollectionName);
      
        this.mapReconstructedParticlesToTracks(tracks, fsParticles);
       
        // Loop over all of the tracks in the event
        for(Track track : tracks){

            // Calculate the momentum of the track
            double p = this.getReconstructedParticle(track).getMomentum().magnitude();
            
            for (TrackerHit rotatedStereoHit : track.getTrackerHits()) { 
            
                // Get the HelicalTrackHit corresponding to the RotatedHelicalTrackHit
                // associated with a track
                Set<TrackerHit> trackClusters = stereoHitToClusters.allFrom(hthToRotatedHth.from(rotatedStereoHit));
                
                for (TrackerHit trackCluster : trackClusters) { 
                
                    // Get the sensor associated with this cluster
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) trackCluster.getRawHits().get(0)).getDetectorElement();

                    Cluster clusterObject = this.calculateSignalToNoise(trackCluster.getRawHits());
                   
                    // Fill all plots
                    trackClusterChargePlots.get(sensor.getName()).fill(clusterObject.getAmplitude());
                    trackHitSignalToNoisePlots.get(sensor.getName()).fill(clusterObject.getSignalToNoise());
                    trackClusterChargeVsMomentum.get(sensor.getName()).fill(p, clusterObject.getAmplitude());
                    trackClusterChargeVsCosTheta.get(sensor.getName()).fill(TrackUtils.getCosTheta(track), clusterObject.getAmplitude());
                    trackClusterChargeVsSinPhi.get(sensor.getName()).fill(Math.sin(TrackUtils.getPhi0(track)), clusterObject.getAmplitude());
                    trackClusterTimePlots.get(sensor.getName()).fill(trackCluster.getTime());
                }
            }
        }
    }
    
    public void endOfData() { 
        
        String rootFile = "run" + runNumber + "_cluster_analysis.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    
    
    }
    
    private Cluster calculateSignalToNoise(List<Object> rawHitObjects) {
       
        double amplitudeSum = 0;
        double noiseSquared = 0;
           
        // Get the sensor associated with this cluster
        HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) rawHitObjects.get(0)).getDetectorElement();
           
        // Loop over all of the RawTrackerHits associated with this object and
        // calculate the signal to noise. 
        for (Object rawHitObject : rawHitObjects) {

            // Cast the raw hit object to a RawTrackerHit
            RawTrackerHit rawHit = (RawTrackerHit) rawHitObject; 
            
            // Get the channel of the raw hit
            int channel = rawHit.getIdentifierFieldValue("strip");
                
            
            // Add the amplitude of that channel to the total amplitude
            amplitudeSum += FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                
            // Calculate the mean noise for the channel
            double channelNoise = 0;
            for (int sampleN = 0; sampleN < 6; sampleN++) { 
                channelNoise += sensor.getNoise(channel, sampleN);
            }
            channelNoise = channelNoise/6;
           
            noiseSquared += channelNoise*channelNoise;
            
        }
        
        // Calculate the cluster noise 
        double noise = Math.sqrt(noiseSquared/rawHitObjects.size());
            
        return new Cluster(amplitudeSum, noise);
    }
    
    /**
     *  Method that creates a map between a fitted raw hit and it's corresponding raw fit
     *  
     * @param fittedHits : List of fitted hits to map
     */
    private void mapFittedRawHits(List<LCRelation> fittedHits) { 
        
        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
       
        // Loop through all fitted hits and map them to their corresponding raw hits
        for (LCRelation fittedHit : fittedHits) { 
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
    }
    
    /**
     * 
     * @param rawHit
     * @return
     */
    private LCRelation getFittedHit(RawTrackerHit rawHit) { 
        return fittedRawTrackerHitMap.get(rawHit);
    }
    
    private void mapReconstructedParticlesToTracks(List<Track> tracks, List<ReconstructedParticle> particles) {
        
       reconParticleMap.clear();
       for (ReconstructedParticle particle : particles) {
           for (Track track : tracks) {
               if (!particle.getTracks().isEmpty() && particle.getTracks().get(0) == track) {
                   reconParticleMap.put(track, particle);
               }
           }
       }
    }
    
    private ReconstructedParticle getReconstructedParticle(Track track) {
        return reconParticleMap.get(track);
    }
   
    /**
     * 
     * @author omoreno
     *
     */
    private class Cluster { 
        
        private double amplitude;
        private double noise; 
        
        /** Default Constructor */ 
        public Cluster(double amplitude, double noise) {
            this.amplitude = amplitude; 
            this.noise = noise; 
        }
        
        public double getAmplitude() { 
            return this.amplitude; 
        }
        
        public double getNoise() { 
            return this.noise;
        }
        
        public double getSignalToNoise() { 
            return this.getAmplitude()/this.getNoise();
        }
    }
}
