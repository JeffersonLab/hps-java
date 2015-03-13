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
    protected Map<HpsSiSensor, IHistogram1D> hitsPerSensorPlots = new HashMap<HpsSiSensor, IHistogram1D>();
    protected Map<HpsSiSensor, int[]> hitsPerSensor = new HashMap<HpsSiSensor, int[]>();
    protected Map<String, IHistogram1D> layersHitPlots = new HashMap<String, IHistogram1D>();
	protected Map<String, IHistogram1D> hitCountPlots = new HashMap<String, IHistogram1D>();
    
    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
   
    // Counters
    double eventCount = 0;
    double totalHitCount = 0; 
    double totalTopHitCount = 0; 
    double totalBotHitCount = 0; 

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
    
    private void clearHitMaps() { 
        for (HpsSiSensor sensor : sensors) {
            hitsPerSensor.get(sensor)[0] = 0; 
        }
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
	
		plotters.put("Raw hit counts/Event", plotterFactory.create("Raw hit counts/Event")); 
		plotters.get("Raw hit counts/Event").createRegions(2, 2);

		hitCountPlots.put("Raw hit counts/Event", 
		        histogramFactory.createHistogram1D("Raw hit counts", 40, 0, 40));
		plotters.get("Raw hit counts/Event").region(0).plot(hitCountPlots.get("Raw hit counts/Event"));
		hitCountPlots.put("SVT top raw hit counts/Event", 
		        histogramFactory.createHistogram1D("SVT top raw hit counts", 40, 0, 40));
		plotters.get("Raw hit counts/Event").region(1).plot(hitCountPlots.get("SVT top raw hit counts/Event"));
		hitCountPlots.put("SVT bottom raw hit counts/Event", 
		        histogramFactory.createHistogram1D("SVT bottom raw hit counts", 40, 0, 40));
		plotters.get("Raw hit counts/Event").region(2).plot(hitCountPlots.get("SVT bottom raw hit counts/Event"));
		
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
    }
    
    public void process(EventHeader event) {
        
    	if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
    		return;
    
    	eventCount++;
    	
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
   
        this.clearHitMaps();
        for (RawTrackerHit rawHit : rawHits) { 
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            hitsPerSensor.get(sensor)[0]++;
        }
        
        int[] topLayersHit = new int[12];
        int[] botLayersHit = new int[12]; 
        int eventHitCount = 0;
        int topEventHitCount = 0;
        int botEventHitCount = 0;
        for (HpsSiSensor sensor : sensors) { 
            int hitCount = hitsPerSensor.get(sensor)[0];
            hitsPerSensorPlots.get(sensor).fill(hitCount);
            
            eventHitCount += hitCount;
            
            if (hitsPerSensor.get(sensor)[0] > 0) { 
                if (sensor.isTopLayer()) { 
                    topLayersHit[sensor.getLayerNumber() - 1]++;
                    topEventHitCount += hitCount;
                }
                else { 
                    botLayersHit[sensor.getLayerNumber() - 1]++; 
                    botEventHitCount += hitCount;
                }
            }
        }
      
        totalHitCount += eventHitCount; 
        totalTopHitCount += topEventHitCount; 
        totalBotHitCount += botEventHitCount;
    
        hitCountPlots.get("Raw hit counts/Event").fill(eventHitCount);
        hitCountPlots.get("SVT top raw hit counts/Event").fill(topEventHitCount);
        hitCountPlots.get("SVT bottom raw hit counts/Event").fill(botEventHitCount);
        
        int totalTopLayersHit = 0; 
        int totalBotLayersHit = 0; 
        for(int layerN = 0; layerN < 12; layerN++) { 
            if (topLayersHit[layerN] > 0) totalTopLayersHit++;
            if (botLayersHit[layerN] > 0) totalBotLayersHit++;
        }
        
        layersHitPlots.get("Top").fill(totalTopLayersHit);
        layersHitPlots.get("Bottom").fill(totalBotLayersHit);
        
    }
   
    @Override
    protected void endOfData() {
        
        System.out.println("%================================================%");
        System.out.println("%============ SVT Raw Hit Statistics ============%");
        System.out.println("%================================================%\n%");
        System.out.println("% Total Hits/Event: " + totalHitCount/eventCount);
        System.out.println("% Total Top SVT Hits/Event: " + totalTopHitCount/eventCount);
        System.out.println("% Total Bottom SVT Hits/Event: " + totalBotHitCount/eventCount);
        System.out.println("\n%================================================%");
    }    
}
