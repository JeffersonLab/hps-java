package org.hps.monitoring.drivers.svt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.util.Driver; 
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;


/**
 *  Monitoring driver that plots the raw hit samples for each of the hits
 *  on a sensor.
 * 
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SamplesPlots extends Driver {

    // TODO: Add documentation
    // TODO: Set plot styles
	
    static {
        hep.aida.jfree.AnalysisFactory.register();
    } 

    static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();

	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
	protected Map<HpsSiSensor, IHistogram2D> samplesPlots = new HashMap<HpsSiSensor, IHistogram2D>();
    private List<HpsSiSensor> sensors;
	
    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
   
    private int computePlotterRegion(HpsSiSensor sensor) {

		if (sensor.getLayerNumber() < 7) {
		    if (sensor.isTopLayer()) {
		        return 2*(sensor.getLayerNumber() - 1); 
			} else { 
				return 2*(sensor.getLayerNumber() - 1) + 1;
			}
		} else { 
		
			if (sensor.isTopLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 4*(sensor.getLayerNumber() - 7);
				} else { 
					return 4*(sensor.getLayerNumber() - 7) + 1;
				}
			} else if (sensor.isBottomLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 4*(sensor.getLayerNumber() - 7) + 2;
				} else {
					return 4*(sensor.getLayerNumber() - 7) + 3;
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
       
        plotters.put("L1-L3 Raw hit samples", plotterFactory.create("L1-L3 Raw hit samples"));
        plotters.get("L1-L3 Raw hit samples").createRegions(6, 2);
       
        plotters.put("L4-L6 Raw hit samples", plotterFactory.create("L4-L6 Raw hit samples"));
        plotters.get("L4-L6 Raw hit samples").createRegions(6, 4);

        for (HpsSiSensor sensor : sensors) { 
            
            samplesPlots.put(sensor, 
                    histogramFactory.createHistogram2D(sensor.getName() + " - Samples", 6, 0, 6, 1000, 1000, 7000));
            
            if (sensor.getLayerNumber() < 7) {
                plotters.get("L1-L3 Raw hit samples").region(this.computePlotterRegion(sensor))
                                                     .plot(samplesPlots.get(sensor));
            } else { 
                plotters.get("L4-L6 Raw hit samples").region(this.computePlotterRegion(sensor))
                                                     .plot(samplesPlots.get(sensor));
            }
        }
        
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
    }

    public void process(EventHeader event) {
    
    	if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
    		return;
    
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
    	
    	for (RawTrackerHit rawHit : rawHits) { 
    	    
    	    HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
    	    short[] adcValues = rawHit.getADCValues();
    	    
    	    for (int sampleN = 0; sampleN < 6; sampleN++) { 
    	        samplesPlots.get(sensor).fill(sampleN, adcValues[sampleN]);
    	    }
    	}
    }
}
