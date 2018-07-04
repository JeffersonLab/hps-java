package org.hps.monitoring.drivers.svt;

import static org.hps.readout.svt.HPSSVTConstants.SAMPLING_INTERVAL;
import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.readout.svt.HPSSVTConstants;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.cat.util.Const;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTPulseFitPlots extends Driver {

    //static {
    //    hep.aida.jfree.AnalysisFactory.register();
    //}

    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private static ITree tree = null;
    private final IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private final IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Pulse Fits");

    private static final Map<String, IHistogram1D> t0Plots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> ampPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram1D> chiprobPlots = new HashMap<String, IHistogram1D>();
    private static final Map<String, IHistogram2D> t0aPlots = new HashMap<String, IHistogram2D>();
    private static final Map<String, IHistogram2D> shapePlots = new HashMap<String, IHistogram2D>();

    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    private static final String subdetectorName = "Tracker";
    private SvtTimingConstants timingConstants;

    @Override
    protected void detectorChanged(Detector detector) {
        timingConstants = DatabaseConditionsManager.getInstance()
                .getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants")
                .getCachedData().get(0);

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement()
                .findDescendants(HpsSiSensor.class);

        plotters.put("Timing", plotterFactory.create("Timing"));
        plotters.get("Timing").createRegions(6, 6);

        plotters.put("Amplitude", plotterFactory.create("Amplitude"));
        plotters.get("Amplitude").createRegions(6, 6);

        plotters.put("Chisq", plotterFactory.create("Chisq"));
        plotters.get("Chisq").createRegions(6, 6);

        plotters.put("A vs. T0", plotterFactory.create("A vs. T0"));
        plotters.get("A vs. T0").createRegions(6, 6);

        plotters.put("Pulse shape", plotterFactory.create("Pulse shape"));
        plotters.get("Pulse shape").createRegions(6, 6);

        tree = analysisFactory.createTreeFactory().create();
        IHistogramFactory histogramFactory = analysisFactory.createHistogramFactory(tree);

        // Setup the occupancy plots.
        for (HpsSiSensor sensor : sensors) {

            t0Plots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + "_timing", 50, -100, 100.0));
            plotters.get("Timing").region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(t0Plots.get(sensor.getName()), this.createStyle(sensor, "Hit time [ns]", ""));
            ampPlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + "_amplitude", 50, 0, 2000.0));
            plotters.get("Amplitude").region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(ampPlots.get(sensor.getName()), this.createStyle(sensor, "Hit amplitude [ADC]", ""));
            chiprobPlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + "_chiprob", 100, 0, 1.0));
            plotters.get("Chisq").region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(chiprobPlots.get(sensor.getName()), this.createStyle(sensor, "Chisq probability", ""));
            t0aPlots.put(sensor.getName(),
                    histogramFactory.createHistogram2D(sensor.getName() + " A vs. T0", 100, -100, 100, 100, 0, 2000));
            plotters.get("A vs. T0")
                    .region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(t0aPlots.get(sensor.getName()),
                            this.createStyle(sensor, "Hit time [ns]", "Hit amplitude [ADC]"));
            shapePlots.put(sensor.getName(), histogramFactory.createHistogram2D(sensor.getName() + " Shape", 200, -1
                    * SAMPLING_INTERVAL, 6 * SAMPLING_INTERVAL, 100, -0.5, 1.5));
            plotters.get("Pulse shape")
                    .region(SvtPlotUtils.computePlotterRegion(sensor))
                    .plot(shapePlots.get(sensor.getName()),
                            this.createStyle(sensor, "Time after hit [ns]", "Normalized amplitude"));
        }

        for (IPlotter plotter : plotters.values()) {
            plotter.show();
        }
    }

    IPlotterStyle createStyle(HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) {
        IPlotterStyle style = SvtPlotUtils.createStyle(plotterFactory, xAxisTitle, yAxisTitle);

        if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }

        return style;
    }

    public SVTPulseFitPlots() {
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
        this.fittedTrackerHitCollectionName = fittedTrackerHitCollectionName;
    }

    @Override
    public void process(EventHeader event) {
        List<FittedRawTrackerHit> fittedrawHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        for (FittedRawTrackerHit fit : fittedrawHits) {
            HpsSiSensor sensor = (HpsSiSensor) fit.getRawTrackerHit().getDetectorElement();
            int strip = fit.getRawTrackerHit().getIdentifierFieldValue("strip");
            double fittedAmp = fit.getAmp();
            double fittedT0 = fit.getT0();

            double tof = fit.getRawTrackerHit().getDetectorElement().getGeometry().getPosition().magnitude()
                    / (Const.SPEED_OF_LIGHT * Const.nanosecond);
            double offset = timingConstants.getOffsetTime()
                    + (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - 12)
                    + sensor.getShapeFitParameters(strip)[HpsSiSensor.T0_INDEX] + sensor.getT0Shift() + tof;

            t0Plots.get(sensor.getName()).fill(fittedT0);
            ampPlots.get(sensor.getName()).fill(fittedAmp);
            chiprobPlots.get(sensor.getName()).fill(fit.getShapeFitParameters().getChiProb());

            t0aPlots.get(sensor.getName()).fill(fit.getT0(), fit.getAmp());
            if (fit.getAmp() > 4 * sensor.getNoise(strip, 0)) {
                for (int i = 0; i < fit.getRawTrackerHit().getADCValues().length; i++) {
                    shapePlots.get(sensor.getName()).fill(
                            (i * HPSSVTConstants.SAMPLING_INTERVAL - fit.getT0() - offset),
                            (fit.getRawTrackerHit().getADCValues()[i] - sensor.getPedestal(strip, i)) / fit.getAmp());
                }
            }
        }
    }

}
