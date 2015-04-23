package org.hps.users.omoreno;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver; 
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;

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
    private Map<RawTrackerHit, FittedRawTrackerHit> fittedRawTrackerHitMap 
        = new HashMap<RawTrackerHit, FittedRawTrackerHit>();
  
    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	private Map<HpsSiSensor, IHistogram1D> clusterChargePlots = new HashMap<HpsSiSensor, IHistogram1D>();
	private Map<HpsSiSensor, IHistogram1D> signalToNoisePlots = new HashMap<HpsSiSensor, IHistogram1D>();
	private Map<HpsSiSensor, IHistogram1D> singleHitClusterChargePlots = new HashMap<HpsSiSensor, IHistogram1D>();
	private Map<HpsSiSensor, IHistogram1D> singleHitSignalToNoisePlots = new HashMap<HpsSiSensor, IHistogram1D>();
	private Map<HpsSiSensor, IHistogram1D> multHitClusterChargePlots = new HashMap<HpsSiSensor, IHistogram1D>();
	private Map<HpsSiSensor, IHistogram1D> multHitSignalToNoisePlots = new HashMap<HpsSiSensor, IHistogram1D>();
    
    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    // Collections
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    
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
        
        for (HpsSiSensor sensor : sensors) { 
            
            clusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(clusterChargePlots.get(sensor), this.createStyle(1, "Cluster Amplitude [ADC Counts]", ""));
        
            singleHitClusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Single Hit Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(singleHitClusterChargePlots.get(sensor), this.createStyle(2, "Cluster Amplitude [ADC Counts]", ""));

            multHitClusterChargePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Multiple Hit Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(multHitClusterChargePlots.get(sensor), this.createStyle(2, "Cluster Amplitude [ADC Counts]", ""));
            
            signalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(signalToNoisePlots.get(sensor), this.createStyle(1, "Signal to Noise", ""));

            singleHitSignalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Single Hit Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(singleHitSignalToNoisePlots.get(sensor), this.createStyle(2, "Signal to Noise", ""));
        
            multHitSignalToNoisePlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Multiple Hit Signal to Noise", 50, 0, 50));
            plotters.get("Signal to Noise").region(this.computePlotterRegion(sensor))
                                           .plot(multHitSignalToNoisePlots.get(sensor), this.createStyle(2, "Signal to Noise", ""));
        }
        
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
    }

    public void process(EventHeader event) { 
     
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        // If the event doesn't contain fitted raw hits, skip it
        if (!event.hasCollection(FittedRawTrackerHit.class, fittedHitsCollectionName)) return;
        
        // Get the list of fitted hits from the event
        List<FittedRawTrackerHit> fittedHits = event.get(FittedRawTrackerHit.class, fittedHitsCollectionName);
        
        // Map the fitted hits to their corresponding raw hits
        this.mapFittedRawHits(fittedHits);
        
        // If the event doesn't contain any clusters, skip it
        if (!event.hasCollection(SiTrackerHitStrip1D.class, clusterCollectionName)) return;
        
        // Get the list of clusters in the event
        List<SiTrackerHitStrip1D> clusters = event.get(SiTrackerHitStrip1D.class, clusterCollectionName);
       
        for (SiTrackerHitStrip1D cluster : clusters) { 
            
            // Get the sensor associated with this cluster
            HpsSiSensor sensor = (HpsSiSensor) cluster.getSensor();
            
            // Get the raw hits composing this cluster and use them to calculate the amplitude of the hit
            double amplitude = 0;
            double noise = 0;
            for (RawTrackerHit rawHit : cluster.getRawHits()) {
                
                // Get the channel of the raw hit
                int channel = rawHit.getIdentifierFieldValue("strip");
                
                // Add the amplitude of that channel to the total amplitude
                amplitude += this.getFittedHit(rawHit).getAmp();
                
                // Calculate the mean noise for the channel
                double channelNoise = 0;
                for (int sampleN = 0; sampleN < 6; sampleN++) { 
                    channelNoise += sensor.getNoise(channel, sampleN);
                }
                channelNoise = channelNoise/6;
                
                noise += channelNoise * this.getFittedHit(rawHit).getAmp();
            }
       
            // Calculate the signal weighted noise
            noise = noise/amplitude;
            
            // Fill all plots
            clusterChargePlots.get(sensor).fill(amplitude);
            signalToNoisePlots.get(sensor).fill(amplitude/noise);
            
            if (cluster.getRawHits().size() == 1) { 
                singleHitClusterChargePlots.get(sensor).fill(amplitude);
                singleHitSignalToNoisePlots.get(sensor).fill(amplitude/noise);
            } else { 
                multHitClusterChargePlots.get(sensor).fill(amplitude);
                multHitSignalToNoisePlots.get(sensor).fill(amplitude/noise);
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
    private void mapFittedRawHits(List<FittedRawTrackerHit> fittedHits) { 
        
        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
       
        // Loop through all fitted hits and map them to their corresponding raw hits
        for (FittedRawTrackerHit fittedHit : fittedHits) { 
            fittedRawTrackerHitMap.put(fittedHit.getRawTrackerHit(), fittedHit);
        }
    }
  
    /**
     * 
     * @param rawHit
     * @return
     */
    private FittedRawTrackerHit getFittedHit(RawTrackerHit rawHit) { 
        return fittedRawTrackerHitMap.get(rawHit);
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
        }
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        return style;
    }
}
