package org.hps.monitoring.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.tracking.HPSFittedRawTrackerHit;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants;
import org.lcsim.hps.recon.tracking.HPSSVTConstants;
import org.lcsim.hps.recon.tracking.SvtUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 * @version $Id: SVTHitReconstructionPlots.java,v 1.14 2012/05/18 07:41:49 meeg
 * Exp $
 */
public class SVTPulseFitPlots extends Driver implements Resettable {

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
    private IHistogram1D[][] chisq = new IHistogram1D[2][10];
    private IHistogram2D[][] t0a = new IHistogram2D[2][10];
    private IHistogram2D[][] shape = new IHistogram2D[2][10];
//    private IHistogram2D shape;

    protected void detectorChanged(Detector detector) {

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

        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                SiSensor sensor = SvtUtils.getInstance().getSensor(module, layer);
                int region = computePlotterRegion(layer + 1, module);
                t0[module][layer] = aida.histogram1D(sensor.getName() + "_timing", 50, -100, 100.0);
                plotter.region(region).plot(t0[module][layer]);
                amp[module][layer] = aida.histogram1D(sensor.getName() + "_amplitude", 50, 0, 2000.0);
                plotter2.region(region).plot(amp[module][layer]);
                chisq[module][layer] = aida.histogram1D(sensor.getName() + "_chisq", 100, 0, 10.0);
                plotter3.region(region).plot(chisq[module][layer]);
                t0a[module][layer] = aida.histogram2D(sensor.getName() + " A vs. T0", 100, -100, 100, 100, 0, 2000);
                plotter4.region(region).plot(t0a[module][layer]);
                shape[module][layer] = aida.histogram2D(sensor.getName() + " Shape", 200, -1, 3, 200, -0.5, 2);
                plotter5.region(region).plot(shape[module][layer]);

            }
        }
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
        List<HPSFittedRawTrackerHit> fittedrawHits = event.get(HPSFittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        for (HPSFittedRawTrackerHit fit : fittedrawHits) {
            SiSensor sensor = (SiSensor) fit.getRawTrackerHit().getDetectorElement();
            int strip = fit.getRawTrackerHit().getIdentifierFieldValue("strip");
            int layer = fit.getRawTrackerHit().getIdentifierFieldValue("layer"); // 1-10; axial layers are odd layers; stereo layers are even
            int module = fit.getRawTrackerHit().getIdentifierFieldValue("module"); // 0-1; module number is top or bottom
//            int layer = hrth.getRawTrackerHit().getLayerNumber();
            double fittedAmp = fit.getAmp();
            double fittedT0 = fit.getT0();
            String sensorName = sensor.getName();
            aida.histogram1D(sensorName + "_timing").fill(fittedT0);
            aida.histogram1D(sensorName + "_amplitude").fill(fittedAmp);
            aida.histogram1D(sensorName + "_chisq").fill(fit.getShapeFitParameters().getChiSq());

            double noise = HPSSVTCalibrationConstants.getNoise(sensor, strip);
            double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
            double tp = HPSSVTCalibrationConstants.getTShaping(sensor, strip);

            t0a[module][layer - 1].fill(fit.getT0(), fit.getAmp());
            if (fit.getAmp() > 4 * noise) {
                for (int i = 0; i < fit.getRawTrackerHit().getADCValues().length; i++) {
                    shape[module][layer - 1].fill((i * HPSSVTConstants.SAMPLING_INTERVAL - fit.getT0()) / tp, (fit.getRawTrackerHit().getADCValues()[i] - pedestal) / fit.getAmp());
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

    public void reset() {
        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                t0[module][layer].reset();
                amp[module][layer].reset();
                chisq[module][layer].reset();
                t0a[module][layer].reset();
                shape[module][layer].reset();
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
