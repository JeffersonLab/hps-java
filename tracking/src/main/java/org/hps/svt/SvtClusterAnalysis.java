package org.hps.svt;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import org.lcsim.util.Driver; 
import org.lcsim.util.aida.AIDA;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.hps.conditions.beam.BeamEnergy;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;

/**
 * Analysis driver used to study SVT strip clusters. 
 * Copied from the SvtClusterAnalysis driver in org.hps.users.omoreno
 * These will be used for Svt NIM paper
 *  
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 * @auther Matt Solt mrsolt@slac.stanford.edu
 */
public class SvtClusterAnalysis extends Driver {

    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
  
    // Plotting
    private ITree tree = null; 
    private IHistogramFactory histogramFactory = null; 
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    protected AIDA aida = AIDA.defaultInstance();
   
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
    private Map<SiSensor, IHistogram1D> trackSingleHitClusterTimePlots = new HashMap<SiSensor, IHistogram1D>();
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
    private String trackCollectionName = "GBLTracks";
    
    private int runNumber = -1; 
    
    private double ebeam = Double.NaN;
    
    private boolean feeCut = false;
    private boolean trackQualityCut = false;
    private double minFee = 0.85;
    private double maxFee = 1.15;
    private double maxChi2 = 10;
        
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
    
    public void setFeeCut(boolean feeCut) { 
        this.feeCut = feeCut; 
    }
    
    public void setTrackQualityCut(boolean trackQualityCut) { 
        this.trackQualityCut = trackQualityCut; 
    }
    
    public void setMinFee(double minFee) { 
        this.minFee = minFee; 
    }
    
    public void setMaxFee(double maxFee) { 
        this.maxFee = maxFee; 
    }
    
    public void setMaxChi2(double maxChi2) { 
        this.maxChi2 = maxChi2; 
    }
    
    /** Default Constructor */
    public SvtClusterAnalysis() { }
    
    protected void detectorChanged(Detector detector) {
    
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = analysisFactory.createHistogramFactory(tree);
        
        if (Double.isNaN(ebeam)) {
            try {
                BeamEnergy.BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                        .getCachedConditions(BeamEnergy.BeamEnergyCollection.class, "beam_energies").getCachedData();
                ebeam = beamEnergyCollection.get(0).getBeamEnergy();
            } catch (Exception e) {
            }
        }
        
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
            
            trackSingleHitClusterTimePlots.put(sensor,
                    histogramFactory.createHistogram1D(sensorName + " - Track Single Hit Cluster Time", 100, -20, 20));
            
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
        //System.out.println("Number of clusters: " + clusters.size()); 
       
        for (TrackerHit cluster : clusters) { 
           
            // Get the sensor associated with this cluster
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) cluster.getRawHits().get(0)).getDetectorElement();
            
            Cluster clusterObject = this.calculateSignalToNoise(cluster.getRawHits());
            
            clusterSizePlots.get(sensor).fill(cluster.getRawHits().size());
            
            // Fill all plots
            clusterChargePlots.get(sensor).fill(clusterObject.getAmplitude());
            signalToNoisePlots.get(sensor).fill(clusterObject.getSignalToNoise());
            clusterTimePlots.get(sensor).fill(cluster.getTime());
            clusterChargeVsTimePlots.get(sensor).fill(clusterObject.getAmplitude(), cluster.getTime());
            
            if (cluster.getRawHits().size() == 1) { 
                singleHitClusterChargePlots.get(sensor).fill(clusterObject.getAmplitude());
                singleHitSignalToNoisePlots.get(sensor).fill(clusterObject.getSignalToNoise());
            } else { 
                multHitClusterChargePlots.get(sensor).fill(clusterObject.getAmplitude());
                multHitSignalToNoisePlots.get(sensor).fill(clusterObject.getSignalToNoise());
            }
        }
       
        if (!event.hasCollection(Track.class, trackCollectionName)) return;
        
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
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
       
        // Loop over all of the tracks in the event
        for(Track track : tracks){

            boolean isGoodTrack = track.getChi2() < maxChi2 && track.getTrackerHits().size() >= 6;
            if(trackQualityCut && !isGoodTrack)
                continue;

            // Calculate the momentum of the track
            HelicalTrackFit hlc_trk_fit = TrackUtils.getHTF(track.getTrackStates().get(0));
            double B_field = Math.abs(TrackUtils.getBField(event.getDetector()).y());
            double p = hlc_trk_fit.p(B_field);
            
            boolean isFee = p > minFee * ebeam && p < maxFee * ebeam && track.getTrackStates().get(0).getOmega() > 0;
            
            if(feeCut && !isFee)
                continue;

            for (TrackerHit rotatedStereoHit : track.getTrackerHits()) { 
            
                // Get the HelicalTrackHit corresponding to the RotatedHelicalTrackHit
                // associated with a track
                Set<TrackerHit> trackClusters = stereoHitToClusters.allFrom(hthToRotatedHth.from(rotatedStereoHit));
                
                for (TrackerHit trackCluster : trackClusters) { 
                
                    // Get the sensor associated with this cluster
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) trackCluster.getRawHits().get(0)).getDetectorElement();

                    Cluster clusterObject = this.calculateSignalToNoise(trackCluster.getRawHits());
                   
                    // Fill all plots
                    trackClusterChargePlots.get(sensor).fill(clusterObject.getAmplitude());
                    trackHitSignalToNoisePlots.get(sensor).fill(clusterObject.getSignalToNoise());
                    trackClusterChargeVsMomentum.get(sensor).fill(p, clusterObject.getAmplitude());
                    trackClusterChargeVsCosTheta.get(sensor).fill(TrackUtils.getCosTheta(track), clusterObject.getAmplitude());
                    trackClusterChargeVsSinPhi.get(sensor).fill(Math.sin(TrackUtils.getPhi0(track)), clusterObject.getAmplitude());
                    trackClusterTimePlots.get(sensor).fill(trackCluster.getTime());
                    if(trackCluster.getRawHits().size() == 1)
                        trackSingleHitClusterTimePlots.get(sensor).fill(trackCluster.getTime());
                }
            }
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
