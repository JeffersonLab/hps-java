package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.util.Resettable;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTPulseFitPlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
//    private String trackerName = "Tracker";
    private int eventCount;
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    IPlotter plotter5;
    private String outputPlots = null;
    private IHistogram1D[][] t0 = new IHistogram1D[2][10];
    private IHistogram1D[][] amp = new IHistogram1D[2][10];
    private IHistogram1D[][] chiprob = new IHistogram1D[2][10];
    private IHistogram2D[][] t0a = new IHistogram2D[2][10];
    private IHistogram2D[][] shape = new IHistogram2D[2][10];
//    private IHistogram2D shape;

    private static final String subdetectorName = "Tracker";
    
    protected void detectorChanged(Detector detector) {

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory("SVT Pulse Fit").create("HPS SVT Timing Plots");
        plotter.setTitle("Timing");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(4, 5);

        plotter2 = fac.createPlotterFactory("SVT Pulse Fit").create("HPS SVT Amplitude Plots");
        plotter2.setTitle("Amplitude");
        IPlotterStyle style2 = plotter2.style();
        style2.dataStyle().fillStyle().setColor("yellow");
        style2.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(4, 5);

        plotter3 = fac.createPlotterFactory("SVT Pulse Fit").create("HPS SVT Chisq Plots");
        plotter3.setTitle("Chisq");
        plotter3.style().dataStyle().fillStyle().setColor("yellow");
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(4, 5);

        plotter4 = fac.createPlotterFactory("SVT Pulse Fit").create("A vs. T0");
        plotter4.setTitle("A vs. T0");
        plotter4.style().dataStyle().errorBarStyle().setVisible(false);
        plotter4.style().statisticsBoxStyle().setVisible(false);
        plotter4.style().setParameter("hist2DStyle", "colorMap");
        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter4.style().zAxisStyle().setParameter("scale", "log");
        plotter4.style().zAxisStyle().setVisible(false);
        plotter4.createRegions(4, 5);

        plotter5 = fac.createPlotterFactory("SVT Pulse Fit").create("Pulse Shape");
        plotter5.setTitle("Pulse shape");
        plotter5.style().dataStyle().errorBarStyle().setVisible(false);
        plotter5.style().statisticsBoxStyle().setVisible(false);
        plotter5.style().setParameter("hist2DStyle", "colorMap");
        plotter5.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter5.style().zAxisStyle().setParameter("scale", "log");
        plotter5.style().zAxisStyle().setVisible(false);
        plotter5.createRegions(4, 5);

        
        // Setup the occupancy plots.
        for(HpsSiSensor sensor : sensors){
        	int module = sensor.getModuleNumber();
        	int layer = sensor.getLayerNumber(); 
            int region = computePlotterRegion(layer + 1, module);
            t0[module][layer] = aida.histogram1D(sensor.getName() + "_timing", 50, -100, 100.0);
            plotter.region(region).plot(t0[module][layer]);
            amp[module][layer] = aida.histogram1D(sensor.getName() + "_amplitude", 50, 0, 2000.0);
            plotter2.region(region).plot(amp[module][layer]);
            chiprob[module][layer] = aida.histogram1D(sensor.getName() + "_chiprob", 100, 0, 1.0);
            plotter3.region(region).plot(chiprob[module][layer]);
            t0a[module][layer] = aida.histogram2D(sensor.getName() + " A vs. T0", 100, -100, 100, 100, 0, 2000);
            plotter4.region(region).plot(t0a[module][layer]);
            shape[module][layer] = aida.histogram2D(sensor.getName() + " Shape", 200, -1, 3, 200, -0.5, 2);
            plotter5.region(region).plot(shape[module][layer]);
        }
        

        /* ==> 
        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                SiSensor sensor = SvtUtils.getInstance().getSensor(module, layer);
                int region = computePlotterRegion(layer + 1, module);
                t0[module][layer] = aida.histogram1D(sensor.getName() + "_timing", 50, -100, 100.0);
                plotter.region(region).plot(t0[module][layer]);
                amp[module][layer] = aida.histogram1D(sensor.getName() + "_amplitude", 50, 0, 2000.0);
                plotter2.region(region).plot(amp[module][layer]);
                chiprob[module][layer] = aida.histogram1D(sensor.getName() + "_chiprob", 100, 0, 1.0);
                plotter3.region(region).plot(chiprob[module][layer]);
                t0a[module][layer] = aida.histogram2D(sensor.getName() + " A vs. T0", 100, -100, 100, 100, 0, 2000);
                plotter4.region(region).plot(t0a[module][layer]);
                shape[module][layer] = aida.histogram2D(sensor.getName() + " Shape", 200, -1, 3, 200, -0.5, 2);
                plotter5.region(region).plot(shape[module][layer]);

            }
        } ===> */
    }

    public SVTPulseFitPlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
        this.fittedTrackerHitCollectionName = fittedTrackerHitCollectionName;
    }

    public void process(EventHeader event) {
        ++eventCount;
        List<FittedRawTrackerHit> fittedrawHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        for (FittedRawTrackerHit fit : fittedrawHits) {
            HpsSiSensor sensor = (HpsSiSensor) fit.getRawTrackerHit().getDetectorElement();
            int strip = fit.getRawTrackerHit().getIdentifierFieldValue("strip");
            int layer = fit.getRawTrackerHit().getIdentifierFieldValue("layer"); // 1-10; axial layers are odd layers; stereo layers are even
            int module = fit.getRawTrackerHit().getIdentifierFieldValue("module"); // 0-1; module number is top or bottom
//            int layer = hrth.getRawTrackerHit().getLayerNumber();
            double fittedAmp = fit.getAmp();
            double fittedT0 = fit.getT0();
            String sensorName = sensor.getName();
            aida.histogram1D(sensorName + "_timing").fill(fittedT0);
            aida.histogram1D(sensorName + "_amplitude").fill(fittedAmp);
            aida.histogram1D(sensorName + "_chiprob").fill(fit.getShapeFitParameters().getChiProb());

            //===> double noise = HPSSVTCalibrationConstants.getNoise(sensor, strip);
            //===> double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
            //===> double tp = HPSSVTCalibrationConstants.getTShaping(sensor, strip);
            double tp = sensor.getShapeFitParameters(strip)[HpsSiSensor.TP_INDEX];
            
            t0a[module][layer - 1].fill(fit.getT0(), fit.getAmp());
            //===> if (fit.getAmp() > 4 * noise) {
            if (fit.getAmp() > 4 * sensor.getNoise(strip, 0)) {
                for (int i = 0; i < fit.getRawTrackerHit().getADCValues().length; i++) {
                    //====> shape[module][layer - 1].fill((i * HPSSVTConstants.SAMPLING_INTERVAL - fit.getT0()) / tp, (fit.getRawTrackerHit().getADCValues()[i] - pedestal) / fit.getAmp());
                    shape[module][layer - 1].fill((i * HPSSVTConstants.SAMPLING_INTERVAL - fit.getT0()) / tp,
                    		(fit.getRawTrackerHit().getADCValues()[i] - sensor.getPedestal(strip, i)) / fit.getAmp());
//                    shape.fill((i * HPSSVTConstants.SAMPLE_INTERVAL - hrth.getT0()) / tp, (hrth.getRawTrackerHit().getADCValues()[i] - pedestal) / hrth.getAmp());
                }
            }
        }
    }

    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private int computePlotterRegion(int layer, int module) {
        int iy = (layer - 1) / 2;
        int ix = 0;
        if (module > 0) {
            ix += 2;
        }
        if (layer % 2 == 0) {
            ix += 1;
        }
        int region = ix * 5 + iy;
        return region;
    }
}
