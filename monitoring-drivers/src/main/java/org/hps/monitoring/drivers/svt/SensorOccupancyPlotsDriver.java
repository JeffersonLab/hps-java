package org.hps.monitoring.drivers.svt;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *	This Driver makes plots of sensor occupancies across a run. It is intended to
 * 	be used with the monitoring system.
 *
 *  @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *	@author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SensorOccupancyPlotsDriver extends Driver {

    // TODO: Add documentation
    // TODO: Set plot styles
	
	static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	protected Map<HpsSiSensor, IHistogram1D> occupancyPlots = new HashMap<HpsSiSensor, IHistogram1D>(); 
	protected Map<HpsSiSensor, int[]> occupancyMap = new HashMap<HpsSiSensor, int[]>(); 
    private List<HpsSiSensor> sensors;
	
    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";

    private int eventCount = 0;
    private int eventRefreshRate = 1;

    public SensorOccupancyPlotsDriver() {}

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

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

        plotters.put("Occupancy", plotterFactory.create("Occupancy"));
		plotters.get("Occupancy").createRegions(6,6);
		
		for (HpsSiSensor sensor : sensors) {
			occupancyPlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy", 640, 0, 640)); 
			plotters.get("Occupancy").region(this.computePlotterRegion(sensor))
									 .plot(occupancyPlots.get(sensor));
            occupancyMap.put(sensor, new int[640]);
		}
		
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

         // Increment strip hit count.
         for (RawTrackerHit rawHit : rawHits) {
        	 int[] strips = occupancyMap.get((HpsSiSensor) rawHit.getDetectorElement());
             strips[rawHit.getIdentifierFieldValue("strip")] += 1;
         }

         // Plot strip occupancies.
         if (eventCount % eventRefreshRate == 0) {
        	 for (HpsSiSensor sensor : sensors) {
                 int[] strips = occupancyMap.get(sensor);
                 for (int i = 0; i < strips.length; i++) {
                     double stripOccupancy = (double) strips[i] / (double) eventCount;
                     if (stripOccupancy != 0) {
                     	occupancyPlots.get(sensor).fill(i, stripOccupancy);
                     }
                 }
             }
         }
    }
}
