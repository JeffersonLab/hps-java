package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.util.Driver; 
import org.lcsim.geometry.Detector;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;

/**
 *  Monitoring driver that provides information about the number
 *  of SVT hits per event.
 *   
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtHitPlots extends Driver {

    // TODO: Add documentation
    // TODO: Set plot styles
	
    static {
        hep.aida.jfree.AnalysisFactory.register();
    } 

    static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
    private List<HpsSiSensor> sensors;
    protected Map<HpsSiSensor, IHistogram1D> hitsPerSensorPlots 
        = new HashMap<HpsSiSensor, IHistogram1D>();
    protected Map<HpsSiSensor, int[]> hitsPerSensor 
        = new HashMap<HpsSiSensor, int[]>();
    protected Map<String, IHistogram1D> layersHitPlots = new HashMap<String, IHistogram1D>();
	
    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";

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

        sensors 
			= detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
   
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        plotters.put("Raw hits per sensor", plotterFactory.create("Raw hits per sensor")); 
		plotters.get("Raw hits per sensor").createRegions(6,6);
        
		for (HpsSiSensor sensor : sensors) {
		   hitsPerSensorPlots.put(sensor, 
		           histogramFactory.createHistogram1D(sensor.getName() + " - Raw Hits", 10, 0, 10)); 
		   plotters.get("Raw hits per sensor").region(this.computePlotterRegion(sensor))
		                                      .plot(hitsPerSensorPlots.get(sensor));
		   hitsPerSensor.put(sensor, new int[1]);
		}

		plotters.put("Number of layers hit", plotterFactory.create("Number of layers hit"));
		plotters.get("Number of layers hit").createRegions(1,2);

		layersHitPlots.put("Top",
		        histogramFactory.createHistogram1D("Top Layers Hit", 12, 0, 12));
		plotters.get("Number of layers hit").region(0).plot(layersHitPlots.get("Top"));
		layersHitPlots.put("Bottom",
		        histogramFactory.createHistogram1D("Bottom Layers Hit", 12, 0, 12));
		plotters.get("Number of layers hit").region(1).plot(layersHitPlots.get("Bottom"));
	
		
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
    }
    
    public void process(EventHeader event) {
        
    	if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
    		return;
    	
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
   
        this.clearHitMaps();
        for (RawTrackerHit rawHit : rawHits) { 
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            hitsPerSensor.get(sensor)[0]++;
        }
        
        int[] topLayersHit = new int[12];
        int[] botLayersHit = new int[12]; 
        for (HpsSiSensor sensor : sensors) { 
            hitsPerSensorPlots.get(sensor).fill(hitsPerSensor.get(sensor)[0]);
            
            if (hitsPerSensor.get(sensor)[0] > 0) { 
                if (sensor.isTopLayer()) topLayersHit[sensor.getLayerNumber() - 1]++;
                else botLayersHit[sensor.getLayerNumber() - 1]++; 
            }
        }
       
        int totalTopLayersHit = 0; 
        int totalBotLayersHit = 0; 
        for(int layerN = 0; layerN < 12; layerN++) { 
            if (topLayersHit[layerN] > 0) totalTopLayersHit++;
            if (botLayersHit[layerN] > 0) totalBotLayersHit++;
        }
        
        layersHitPlots.get("Top").fill(totalTopLayersHit);
        layersHitPlots.get("Bottom").fill(totalBotLayersHit);
        
    }
    
    private void clearHitMaps() { 
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensor.get(sensor)[0] = 0; 
        }
    }
}
