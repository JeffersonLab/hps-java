package org.hps.monitoring.drivers.svt;

import java.util.HashMap;
import java.util.List;
import java.util.Map; 

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.util.Driver; 
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;

import org.hps.recon.tracking.FittedRawTrackerHit;

/**
 *  Monitoring driver that will be used when 'timing in' the SVT.
 * 
 *  @author Sho Uemura <meeg@slac.stanford.edu>
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtTimingInPlots extends Driver {
	
    // TODO: Add documentation
    // TODO: Set plot styles
	
	static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	protected Map<SiSensor, IHistogram1D> t0Plots = new HashMap<SiSensor, IHistogram1D>(); 
	
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
		
		List<HpsSiSensor> sensors 
			= detector.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);
	
		//--- t0 Plots ---//
		//----------------//
		plotters.put("L1-L3 t0", plotterFactory.create("L1-L3 t0"));
		plotters.get("L1-L3 t0").createRegions(6,2);

		plotters.put("L4-L6 t0", plotterFactory.create("L4-L6 t0"));
		plotters.get("L4-L6 t0").createRegions(6,4);
		int index = 0;
		for (HpsSiSensor sensor : sensors) {

			t0Plots.put(sensor,histogramFactory.createHistogram1D(sensor.getName() + " - t0",75, -50, 100.0));
			if (sensor.getLayerNumber() < 7) {
			    plotters.get("L1-L3 t0").region(this.computePlotterRegion(sensor))
			                            .plot(t0Plots.get(sensor));
			} else {
				plotters.get("L4-L6 t0").region(this.computePlotterRegion(sensor))
				                        .plot(t0Plots.get(sensor));
			}
		}
	
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
	}
	
	public void process(EventHeader event) { 
		
		if (!event.hasCollection(LCRelation.class, "SVTFittedRawTrackerHits"))
			return;
		
		List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
		
		
		for (LCRelation fittedHit : fittedHits) { 
			
			HpsSiSensor sensor 
				= (HpsSiSensor) ((RawTrackerHit) fittedHit.getFrom()).getDetectorElement();
			
			double t0 = FittedRawTrackerHit.getT0(fittedHit);
			t0Plots.get(sensor).fill(t0);
		}	
	}
}
