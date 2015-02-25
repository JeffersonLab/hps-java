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

public class SvtTimingInPlots extends Driver {
	
	
	static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	protected Map<SiSensor, IHistogram1D> t0Plots = new HashMap<SiSensor, IHistogram1D>(); 
	
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

			if (sensor.getLayerNumber() < 7) {
				if (sensor.isTopLayer()) {
					index = 2*(sensor.getLayerNumber() - 1); 
				} else { 
					index = 2*(sensor.getLayerNumber() - 1) + 1;
				}
				System.out.println("Layer number: " + sensor.getLayerNumber() + " Index: " + index);
				t0Plots.put(sensor,histogramFactory.createHistogram1D(sensor.getName() + " - t0",75, -50, 100.0));
				plotters.get("L1-L3 t0").region(index).plot(t0Plots.get(sensor));
			} else {
				if (sensor.isTopLayer() && sensor.isAxial()) {
					System.out.println("Top, axial");
					index = 4*(sensor.getLayerNumber() - 7);
				} else if (sensor.isTopLayer() && sensor.isStereo()) { 
					System.out.println("Top, stereo");
					index = 4*(sensor.getLayerNumber() - 7) + 1;
				} else if (sensor.isBottomLayer() && sensor.isAxial()) {
					System.out.println("Bottom, axial");
					index = 4*(sensor.getLayerNumber() - 7) + 2;
				} else if (sensor.isBottomLayer() && sensor.isStereo()) { 
					System.out.println("Bottom, stereo");
					index = 4*(sensor.getLayerNumber() - 7) + 3;
				}
				System.out.println("Layer number: " + sensor.getLayerNumber() + " Index: " + index);
				t0Plots.put(sensor,histogramFactory.createHistogram1D(sensor.getName() + " - t0",75, -50, 100.0));
				plotters.get("L4-L6 t0").region(index).plot(t0Plots.get(sensor));
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
