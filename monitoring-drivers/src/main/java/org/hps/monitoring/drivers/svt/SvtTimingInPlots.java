package org.hps.monitoring.drivers.svt;

import java.util.HashMap;
import java.util.List;
import java.util.Map; 

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import org.lcsim.util.Driver; 
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;

/**
 *  Monitoring driver that will be used when 'timing in' the SVT.
 * 
 *  @author Sho Uemura <meeg@slac.stanford.edu>
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtTimingInPlots extends Driver {

    // TODO: Add documentation
    // TODO: Set plot styles

    static {
        hep.aida.jfree.AnalysisFactory.register();
    } 
    
	static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
	IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
	protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
	protected Map<SiSensor, IHistogram1D> t0Plots = new HashMap<SiSensor, IHistogram1D>(); 
	protected Map<SiSensor, IHistogram1D> amplitudePlots = new HashMap<SiSensor, IHistogram1D>(); 
	protected Map<SiSensor, IHistogram1D> chi2Plots = new HashMap<SiSensor, IHistogram1D>(); 
	protected Map<SiSensor, IHistogram2D> t0vAmpPlots = new HashMap<SiSensor, IHistogram2D>(); 
	protected Map<SiSensor, IHistogram2D> t0vChi2Plots = new HashMap<SiSensor, IHistogram2D>(); 
	protected Map<SiSensor, IHistogram2D> chi2vAmpPlots = new HashMap<SiSensor, IHistogram2D>(); 
	
	IPlotterStyle style = null; 
	
	
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
	
		plotters.put("L1-L3 t0", plotterFactory.create("L1-L3 t0"));
		plotters.get("L1-L3 t0").createRegions(6,2);

		plotters.put("L4-L6 t0", plotterFactory.create("L4-L6 t0"));
		plotters.get("L4-L6 t0").createRegions(6,4);
		
		plotters.put("L1-L3 Amplitude", plotterFactory.create("L1-L3 Amplitude"));
		plotters.get("L1-L3 Amplitude").createRegions(6,2);

		plotters.put("L4-L6 Amplitude", plotterFactory.create("L4-L6 Amplitude"));
		plotters.get("L4-L6 Amplitude").createRegions(6,4);
		
		plotters.put("L1-L3 Chi^2 Probability", plotterFactory.create("L1-L3 Chi^2 Probability"));
		plotters.get("L1-L3 Chi^2 Probability").createRegions(6,2);

		plotters.put("L4-L6 Chi^2 Probability", plotterFactory.create("L1-L3 Chi^2 Probability"));
		plotters.get("L4-L6 Chi^2 Probability").createRegions(6,4);
	
		plotters.put("L1-L3 t0 vs Amplitude", plotterFactory.create("L1-L3 t0 vs Amplitude"));
		plotters.get("L1-L3 t0 vs Amplitude").createRegions(6, 2);
		
		plotters.put("L4-L6 t0 vs Amplitude", plotterFactory.create("L4-L6 t0 vs Amplitude"));
		plotters.get("L4-L6 t0 vs Amplitude").createRegions(6, 4);

		plotters.put("L1-L3 t0 vs Chi^2 Prob.", plotterFactory.create("L1-L3 t0 vs Chi^2 Prob."));
		plotters.get("L1-L3 t0 vs Chi^2 Prob.").createRegions(6, 2);
		
		plotters.put("L4-L6 t0 vs Chi^2 Prob.", plotterFactory.create("L4-L6 t0 vs Chi^2 Prob."));
		plotters.get("L4-L6 t0 vs Chi^2 Prob.").createRegions(6, 4);

		plotters.put("L1-L3 Chi^2 Prob. vs Amplitude", plotterFactory.create("L1-L3 Chi^2 Prob. vs Amplitude"));
		plotters.get("L1-L3 Chi^2 Prob. vs Amplitude").createRegions(6, 2);
		
		plotters.put("L4-L6 Chi^2 Prob. vs Amplitude", plotterFactory.create("L4-L6 Chi^2 Prob. vs Amplitude"));
		plotters.get("L4-L6 Chi^2 Prob. vs Amplitude").createRegions(6, 4);

		for (HpsSiSensor sensor : sensors) {

			t0Plots.put(sensor,histogramFactory.createHistogram1D(sensor.getName() + " - t0",75, -50, 100.0));
			amplitudePlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Amplitude", 200, 0, 2000));
			chi2Plots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Chi^2 Probability", 20, 0, 1));
			t0vAmpPlots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - t0 v Amplitude", 75, -50, 100.0, 200, 0, 2000));
			t0vChi2Plots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - t0 v Chi^2 Probability", 75, -50, 100.0, 20, 0, 1));
			chi2vAmpPlots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - Chi2 v Amplitude", 20, 0, 1, 200, 0, 2000));
			
			if (sensor.getLayerNumber() < 7) {
			    plotters.get("L1-L3 t0").region(this.computePlotterRegion(sensor))
			                            .plot(t0Plots.get(sensor));
			    plotters.get("L1-L3 Amplitude").region(this.computePlotterRegion(sensor))
			                                   .plot(amplitudePlots.get(sensor));
			    plotters.get("L1-L3 Chi^2 Probability").region(this.computePlotterRegion(sensor))
			                                   .plot(chi2Plots.get(sensor));
			    plotters.get("L1-L3 t0 vs Amplitude").region(this.computePlotterRegion(sensor))
			                                         .plot(t0vAmpPlots.get(sensor));
			    plotters.get("L1-L3 t0 vs Chi^2 Prob.").region(this.computePlotterRegion(sensor))
			                                         .plot(t0vChi2Plots.get(sensor));
			    plotters.get("L1-L3 Chi^2 Prob. vs Amplitude").region(this.computePlotterRegion(sensor))
			                                         .plot(chi2vAmpPlots.get(sensor));
			    
			} else {
				plotters.get("L4-L6 t0").region(this.computePlotterRegion(sensor))
				                        .plot(t0Plots.get(sensor));
			    plotters.get("L4-L6 Amplitude").region(this.computePlotterRegion(sensor))
			                                   .plot(amplitudePlots.get(sensor));
			    plotters.get("L4-L6 Chi^2 Probability").region(this.computePlotterRegion(sensor))
			                                   .plot(chi2Plots.get(sensor));
			    plotters.get("L4-L6 t0 vs Amplitude").region(this.computePlotterRegion(sensor))
			                                         .plot(t0vAmpPlots.get(sensor));
			    plotters.get("L4-L6 t0 vs Chi^2 Prob.").region(this.computePlotterRegion(sensor))
			                                         .plot(t0vChi2Plots.get(sensor));
			    plotters.get("L4-L6 Chi^2 Prob. vs Amplitude").region(this.computePlotterRegion(sensor))
			                                         .plot(chi2vAmpPlots.get(sensor));
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
			
		    RawTrackerHit rawHit = (RawTrackerHit) fittedHit.getFrom();
		    
			HpsSiSensor sensor 
				= (HpsSiSensor) rawHit.getDetectorElement();
			
			double t0 = FittedRawTrackerHit.getT0(fittedHit);
			t0Plots.get(sensor).fill(t0);
			
			double amplitude = FittedRawTrackerHit.getAmp(fittedHit);
			amplitudePlots.get(sensor).fill(amplitude);
			
			double chi2Prob = ShapeFitParameters.getChiProb(FittedRawTrackerHit.getShapeFitParameters(fittedHit));
			chi2Plots.get(sensor).fill(chi2Prob);
	
			t0vAmpPlots.get(sensor).fill(t0, amplitude);
			t0vChi2Plots.get(sensor).fill(t0, chi2Prob);
			chi2vAmpPlots.get(sensor).fill(chi2Prob, amplitude);
			
		}	
	}
}
