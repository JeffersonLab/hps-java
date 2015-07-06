package org.hps.users.omoreno;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.util.Driver; 
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
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
 * 
 * @author Omar Moreno
 *
 */
public class SvtClusterAnalysis extends Driver {
   
    
    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
    private Map<Track, ReconstructedParticle> reconParticleMap = new HashMap<Track, ReconstructedParticle>();
  
    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	
	// All clusters
	private Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> singleHitClusterChargePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> multHitClusterChargePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> signalToNoisePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> singleHitSignalToNoisePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> multHitSignalToNoisePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> clusterSizePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> clusterTimePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram2D> clusterChargeVsTimePlots = new HashMap<String, IHistogram2D>();

	// Clusters on track
	private Map<String, IHistogram1D> trackClusterChargePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> trackHitSignalToNoisePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram1D> trackClusterTimePlots = new HashMap<String, IHistogram1D>();
	private Map<String, IHistogram2D> trackClusterChargeVsMomentum = new HashMap<String, IHistogram2D>();
	private Map<String, IHistogram2D> trackClusterChargeVsCosTheta = new HashMap<String, IHistogram2D>();
	private Map<String, IHistogram2D> trackClusterChargeVsSinPhi = new HashMap<String, IHistogram2D>();
	
    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    // Collections
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String stereoHitRelationsColName = "HelicalTrackHitRelations";
    private String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";
    private String fsParticlesCollectionName = "FinalStateParticles";
    
    private int runNumber = -1; 
        
    
    /**
     * Default Ctor
     */
    public SvtClusterAnalysis() { }
    
    private int computePlotterRegion(HpsSiSensor sensor) {

		if (sensor.getLayerNumber() < 7) {
			if (sensor.isTopLayer()) {
				return 6*(sensor.getLayerNumber() - 1); 
			} else { 
				return 6*(sensor.getLayerNumber() - 1) + 1;
			} 
		} else { 
		
			if (sensor.isTopLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 6*(sensor.getLayerNumber() - 7) + 2;
				} else { 
					return 6*(sensor.getLayerNumber() - 7) + 3;
				}
			} else if (sensor.isBottomLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 6*(sensor.getLayerNumber() - 7) + 4;
				} else {
					return 6*(sensor.getLayerNumber() - 7) + 5;
				}
			}
		}
		return -1; 
    }
    
    protected void detectorChanged(Detector detector) {
       
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
       
        plotters.put("Cluster Amplitude", plotterFactory.create("Cluster Amplitude"));
        plotters.get("Cluster Amplitude").createRegions(6, 6);
       
        plotters.put("Signal to Noise", plotterFactory.create("Signal to Noise"));
        plotters.get("Signal to Noise").createRegions(6, 6);
        
        plotters.put("Cluster Multiplicity", plotterFactory.create("Cluster Multiplicity"));
        plotters.get("Cluster Multiplicity").createRegions(6, 6);
        
        plotters.put("Cluster Time", plotterFactory.create("Cluster Time"));
        plotters.get("Cluster Time").createRegions(6, 6);
        
        plotters.put("Cluster Amplitude vs Cluster Time", plotterFactory.create("Cluster Amplitude vs Cluster Time"));
        plotters.get("Cluster Amplitude vs Cluster Time").createRegions(6, 6);
        
        plotters.put("Cluster Amplitude vs Momentum", plotterFactory.create("Cluster Amplitude vs Momentum"));
        plotters.get("Cluster Amplitude vs Momentum").createRegions(6, 6);
        
        plotters.put("Cluster Amplitude vs cos(theta)", plotterFactory.create("Cluster Amplitude vs cos(theta)"));
        plotters.get("Cluster Amplitude vs cos(theta)").createRegions(6, 6);
        
        plotters.put("Cluster Amplitude vs sin(phi0)", plotterFactory.create("Cluster Amplitude vs sin(phi0)"));
        plotters.get("Cluster Amplitude vs sin(phi0)").createRegions(6, 6);

        for (HpsSiSensor sensor : sensors) { 
            
            clusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(clusterChargePlots
                                             .get(sensor.getName()), this.createStyle(1, "Cluster Amplitude [ADC Counts]", ""));
       
            singleHitClusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Single Hit Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(singleHitClusterChargePlots
                                             .get(sensor.getName()), this.createStyle(2, "Cluster Amplitude [ADC Counts]", ""));

            multHitClusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Multiple Hit Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(multHitClusterChargePlots
                                             .get(sensor.getName()), this.createStyle(2, "Cluster Amplitude [ADC Counts]", ""));

            signalToNoisePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(signalToNoisePlots
                                           .get(sensor.getName()), this.createStyle(1, "Signal to Noise", ""));

            singleHitSignalToNoisePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Single Hit Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(singleHitSignalToNoisePlots
                                           .get(sensor.getName()), this.createStyle(2, "Signal to Noise", ""));
        
            multHitSignalToNoisePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Multiple Hit Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(multHitSignalToNoisePlots
                                           .get(sensor.getName()), this.createStyle(2, "Signal to Noise", ""));

            clusterSizePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Multiplicity", 10, 0, 10));
            plotters.get("Cluster Multiplicity").region(this.computePlotterRegion(sensor))
                                                .plot(clusterSizePlots
                                                .get(sensor.getName()), this.createStyle(1, "Cluster Multiplicity", ""));
            
            clusterTimePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Time", 100, -100, 100));
            plotters.get("Cluster Time").region(this.computePlotterRegion(sensor))
                                        .plot(clusterTimePlots
                                        .get(sensor.getName()), this.createStyle(1, "Cluster Time [ns]", ""));

            trackClusterTimePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Track Cluster Time", 100, -100, 100));
            plotters.get("Cluster Time").region(this.computePlotterRegion(sensor))
                                        .plot(trackClusterTimePlots
                                        .get(sensor.getName()), this.createStyle(3, "Cluster Time [ns]", ""));
            
            clusterChargeVsTimePlots.put(sensor.getName(),
                    histogramFactory.createHistogram2D(sensor.getName() + " - Cluster Amplitude vs Time", 100, 0, 5000, 100, -100, 100));
            plotters.get("Cluster Amplitude vs Cluster Time").region(this.computePlotterRegion(sensor))
                                                             .plot(clusterChargeVsTimePlots
                                                             .get(sensor.getName()));
        
            trackClusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Tracker Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(trackClusterChargePlots
                                             .get(sensor.getName()), this.createStyle(3, "Cluster Amplitude [ADC Counts]", ""));
        
            trackHitSignalToNoisePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Track Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(trackHitSignalToNoisePlots
                                           .get(sensor.getName()), this.createStyle(3, "Signal to Noise", ""));
        
            trackClusterChargeVsMomentum.put(sensor.getName(),
                    histogramFactory.createHistogram2D(sensor.getName() + " - Cluster Amplitude vs Momentum", 100, 0, 1.5, 100, 0, 5000));
            plotters.get("Cluster Amplitude vs Momentum").region(this.computePlotterRegion(sensor))
                                                         .plot(trackClusterChargeVsMomentum
                                                         .get(sensor.getName()));
        
            trackClusterChargeVsCosTheta.put(sensor.getName(),
                    histogramFactory.createHistogram2D(sensor.getName() + " - Cluster Amplitude vs cos(theta)", 100, -0.1, 0.1, 100, 0, 5000));
            plotters.get("Cluster Amplitude vs cos(theta)").region(this.computePlotterRegion(sensor))
                                                         .plot(trackClusterChargeVsCosTheta
                                                         .get(sensor.getName()));

            trackClusterChargeVsSinPhi.put(sensor.getName(),
                    histogramFactory.createHistogram2D(sensor.getName() + " - Cluster Amplitude vs sin(phi0)", 100, -0.2, 0.2, 100, 0, 5000));
            plotters.get("Cluster Amplitude vs sin(phi0)").region(this.computePlotterRegion(sensor))
                                                          .plot(trackClusterChargeVsSinPhi
                                                          .get(sensor.getName()));
        }
        
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
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
            
            // Get the raw hits composing this cluster and use them to calculate the amplitude of the hit
            double amplitudeSum = 0;
            double noise = 0;
            for (Object rawHitObject : cluster.getRawHits()) {

                RawTrackerHit rawHit = (RawTrackerHit) rawHitObject; 
                
                // Get the channel of the raw hit
                int channel = rawHit.getIdentifierFieldValue("strip");
                
                // Add the amplitude of that channel to the total amplitude
                double amplitude = FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                amplitudeSum += FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                
                // Calculate the mean noise for the channel
                double channelNoise = 0;
                for (int sampleN = 0; sampleN < 6; sampleN++) { 
                    channelNoise += sensor.getNoise(channel, sampleN);
                }
                channelNoise = channelNoise/6;
                
                noise += channelNoise * amplitude;
            }
      
            clusterSizePlots.get(sensor.getName()).fill(cluster.getRawHits().size());
            
            // Calculate the signal weighted noise
            noise = noise/amplitudeSum;
            
            // Fill all plots
            clusterChargePlots.get(sensor.getName()).fill(amplitudeSum);
            signalToNoisePlots.get(sensor.getName()).fill(amplitudeSum/noise);
            clusterTimePlots.get(sensor.getName()).fill(cluster.getTime());
            clusterChargeVsTimePlots.get(sensor.getName()).fill(amplitudeSum, cluster.getTime());
            
            if (cluster.getRawHits().size() == 1) { 
                singleHitClusterChargePlots.get(sensor.getName()).fill(amplitudeSum);
                singleHitSignalToNoisePlots.get(sensor.getName()).fill(amplitudeSum/noise);
            } else { 
                multHitClusterChargePlots.get(sensor.getName()).fill(amplitudeSum);
                multHitSignalToNoisePlots.get(sensor.getName()).fill(amplitudeSum/noise);
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
                
                    // Get the raw hits composing this cluster and use them to calculate the amplitude of the hit
                    double amplitudeSum = 0;
                    double noise = 0;
                    HpsSiSensor sensor = null;

                    for (Object rawHitObject : trackCluster.getRawHits()) {
                        RawTrackerHit rawHit = (RawTrackerHit) rawHitObject; 
                        
                        sensor = (HpsSiSensor) rawHit.getDetectorElement();
                       
                        // Get the channel of the raw hit
                        int channel = rawHit.getIdentifierFieldValue("strip");
                
                        // Add the amplitude of that channel to the total amplitude
                        double amplitude = FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                        amplitudeSum += FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                
                        // Calculate the mean noise for the channel
                        double channelNoise = 0;
                        for (int sampleN = 0; sampleN < 6; sampleN++) { 
                            channelNoise += sensor.getNoise(channel, sampleN);
                        }
                        channelNoise = channelNoise/6;
                
                        noise += channelNoise * amplitude;
                        
                    }

                    // Calculate the signal weighted noise
                    noise = noise/amplitudeSum;
                   
                    // Fill all plots
                    trackClusterChargePlots.get(sensor.getName()).fill(amplitudeSum);
                    trackHitSignalToNoisePlots.get(sensor.getName()).fill(amplitudeSum/noise);
                    trackClusterChargeVsMomentum.get(sensor.getName()).fill(p, amplitudeSum);
                    trackClusterChargeVsCosTheta.get(sensor.getName()).fill(TrackUtils.getCosTheta(track), amplitudeSum);
                    trackClusterChargeVsSinPhi.get(sensor.getName()).fill(Math.sin(TrackUtils.getPhi0(track)), amplitudeSum);
                    //trackClusterTimePlots.get(sensor.getName()).fill(trackCluster.time());
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
   
    IPlotterStyle createStyle(int color, String xAxisTitle, String yAxisTitle) { 
       
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);
        
        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(4);
        style.dataStyle().fillStyle().setVisible(true);
        
        if (color == 1) { 
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
            style.dataStyle().fillStyle().setOpacity(.30);
        } else if (color == 2) { 
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
            style.dataStyle().fillStyle().setOpacity(.70);
        } else if (color == 3) { 
            style.dataStyle().fillStyle().setColor("255, 38, 38, 1");
            style.dataStyle().outlineStyle().setColor("255, 38, 38, 1");
            style.dataStyle().fillStyle().setOpacity(.70);
        }
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        return style;
    }
}
