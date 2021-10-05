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
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.cat.util.Const;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SVTPulseFitPlots extends Driver {

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
    private boolean correctForT0Offset = false;
    private boolean doShapePlots = false;
    private boolean removeRandomEvents=true;    
    public void setRemoveRandomEvents(boolean doit) {
        this.removeRandomEvents=doit;
    }
    public void setCorrectForT0Offset(boolean correct) {
        this.correctForT0Offset = correct;
    }

    public void setDoShapePlots(boolean doit) {
        this.doShapePlots = doit;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        timingConstants = DatabaseConditionsManager.getInstance()
                .getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants")
                .getCachedData().get(0);

        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement()
                .findDescendants(HpsSiSensor.class);

        plotters.put("Timing:  L0-L3", plotterFactory.create("3a Timing"));
        plotters.get("Timing:  L0-L3").createRegions(4, 4);
        plotters.put("Timing:  L4-L6", plotterFactory.create("3b Timing"));
        plotters.get("Timing:  L4-L6").createRegions(6, 4);

        plotters.put("Amplitude:  L0-L3", plotterFactory.create("3c Amplitude"));
        plotters.get("Amplitude:  L0-L3").createRegions(4, 4);
        plotters.put("Amplitude:  L4-L6", plotterFactory.create("3d Amplitude"));
        plotters.get("Amplitude:  L4-L6").createRegions(6, 4);

        plotters.put("Chisq:  L0-L3", plotterFactory.create("3e Chisq"));
        plotters.get("Chisq:  L0-L3").createRegions(4, 4);
        plotters.put("Chisq:  L4-L6", plotterFactory.create("3f Chisq"));
        plotters.get("Chisq:  L4-L6").createRegions(6, 4);

        plotters.put("A vs. T0:  L0-L3", plotterFactory.create("3g A vs. T0"));
        plotters.get("A vs. T0:  L0-L3").createRegions(4, 4);
        plotters.put("A vs. T0:  L4-L6", plotterFactory.create("3h A vs. T0"));
        plotters.get("A vs. T0:  L4-L6").createRegions(6, 4);
        if (this.doShapePlots) {
            plotters.put("Pulse shape:  L0-L3", plotterFactory.create("3i Pulse shape"));
            plotters.get("Pulse shape:  L0-L3").createRegions(4, 4);
            plotters.put("Pulse shape:  L4-L6", plotterFactory.create("3j Pulse shape"));
            plotters.get("Pulse shape:  L4-L6").createRegions(6, 4);
        }
        tree = AIDA.defaultInstance().tree();
        IHistogramFactory histogramFactory = analysisFactory.createHistogramFactory(tree);

        // Setup the occupancy plots.
        for (HpsSiSensor sensor : sensors) {

            t0Plots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(
                    SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "_timing", 50, -100, 100.0));

            ampPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram1D(
                    SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "_amplitude", 50, 0, 2000.0));

            chiprobPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory
                    .createHistogram1D(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + "_chiprob", 50, 0, 1.0));

            t0aPlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()), histogramFactory.createHistogram2D(
                    SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " A vs. T0", 100, -100, 100, 100, 0, 2000));
            if (this.doShapePlots) {
                shapePlots.put(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()),
                        histogramFactory.createHistogram2D(
                                SvtPlotUtils.fixSensorNumberLabel(sensor.getName()) + " Shape", 100,
                                -1 * SAMPLING_INTERVAL, 6 * SAMPLING_INTERVAL, 100, -0.5, 1.5));
            }
            if (sensor.getLayerNumber() < 9) {
                plotters.get("Timing:  L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit time [ns]", ""));
                plotters.get("Amplitude:  L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        ampPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit amplitude [ADC]", ""));
                plotters.get("Chisq:  L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        chiprobPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Chisq probability", ""));
                plotters.get("A vs. T0:  L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        t0aPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit time [ns]", "Hit amplitude [ADC]"));
                if (this.doShapePlots) {
                    plotters.get("Pulse shape:  L0-L3").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(shapePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createStyle(sensor, "Time after hit [ns]", "Normalized amplitude"));
                }
            } else {
                plotters.get("Timing:  L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit time [ns]", ""));
                plotters.get("Amplitude:  L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        ampPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit amplitude [ADC]", ""));
                plotters.get("Chisq:  L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        chiprobPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Chisq probability", ""));
                plotters.get("A vs. T0:  L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor)).plot(
                        t0aPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                        this.createStyle(sensor, "Hit time [ns]", "Hit amplitude [ADC]"));
                if (this.doShapePlots) {
                    plotters.get("Pulse shape:  L4-L6").region(SvtPlotUtils.computePlotterRegionSvtUpgrade(sensor))
                            .plot(shapePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())),
                                    this.createStyle(sensor, "Time after hit [ns]", "Normalized amplitude"));
                }

            }
        }

        for (IPlotter plotter : plotters.values())
            plotter.show();
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
        if (removeRandomEvents && event.hasCollection(GenericObject.class, "TSBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TSBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TSData2019.BANK_TAG){
                    TSData2019 triggerData = new TSData2019(data); 
                    if (triggerData.isPulserTrigger() || triggerData.isFaradayCupTrigger()) {                       
                        return; 
                    }
                }
        }
        
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
            if (!correctForT0Offset)
                offset = 0.0;

            t0Plots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(fittedT0);
            ampPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(fittedAmp);
            chiprobPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName()))
                    .fill(fit.getShapeFitParameters().getChiProb());

            t0aPlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(fit.getT0(), fit.getAmp());
            if (this.doShapePlots) {
                if (fit.getAmp() > 4 * sensor.getNoise(strip, 0))
                    for (int i = 0; i < fit.getRawTrackerHit().getADCValues().length; i++)
                        shapePlots.get(SvtPlotUtils.fixSensorNumberLabel(sensor.getName())).fill(
                                (i * HPSSVTConstants.SAMPLING_INTERVAL - fit.getT0() - offset),
                                (fit.getRawTrackerHit().getADCValues()[i] - sensor.getPedestal(strip, i))
                                        / fit.getAmp());
            }
        }
    }

}
