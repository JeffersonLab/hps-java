package org.hps.monitoring.drivers.svt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.aida.AIDA;

/**
 * Monitoring driver that plots the raw hit samples for each of the hits on a
 * sensor.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SamplesPlots extends Driver {

    // TODO: Add documentation
    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    protected Map<HpsSiSensor, IHistogram2D> samplesPlots = new HashMap<HpsSiSensor, IHistogram2D>();
    private List<HpsSiSensor> sensors;

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";

    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 2 * (sensor.getLayerNumber() - 1);
            } else {
                return 2 * (sensor.getLayerNumber() - 1) + 1;
            }
        } else {

            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 4 * (sensor.getLayerNumber() - 7);
                } else {
                    return 4 * (sensor.getLayerNumber() - 7) + 1;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 4 * (sensor.getLayerNumber() - 7) + 2;
                } else {
                    return 4 * (sensor.getLayerNumber() - 7) + 3;
                }
            }
        }

        return -1;
    }

    protected void detectorChanged(Detector detector) {
        IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Raw Samples");
        IHistogramFactory histogramFactory = AIDA.defaultInstance().histogramFactory();

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
                    histogramFactory.createHistogram2D(sensor.getName() + " - Samples", 6, 0, 6, 1000, -200.0, 3000));

            if (sensor.getLayerNumber() < 7) {
                plotters.get("L1-L3 Raw hit samples").region(this.computePlotterRegion(sensor))
                        .plot(samplesPlots.get(sensor), this.createStyle(plotterFactory, "Sample Number", "Amplitude [ADC Counts]"));
            } else {
                plotters.get("L4-L6 Raw hit samples").region(this.computePlotterRegion(sensor))
                        .plot(samplesPlots.get(sensor), this.createStyle(plotterFactory, "Sample Number", "Amplitude [ADC Counts]"));
            }
        }

        for (IPlotter plotter : plotters.values()) {
            plotter.show();
        }
    }

    public void process(EventHeader event) {

        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            return;
        }

        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        for (RawTrackerHit rawHit : rawHits) {

            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            short[] adcValues = rawHit.getADCValues();
            int strip = rawHit.getIdentifierFieldValue("strip");
            double pedestal = sensor.getPedestal(strip, 0);
            for (int sampleN = 0; sampleN < 6; sampleN++) {
                samplesPlots.get(sensor).fill(sampleN, adcValues[sampleN] - pedestal);
            }
        }
    }

    IPlotterStyle createStyle(IPlotterFactory plotterFactory, String xAxisTitle, String yAxisTitle) {

        // Create a default style
        IPlotterStyle style = plotterFactory.createPlotterStyle();

        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);

        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);

        // Set the z axis to log scale
        style.zAxisStyle().setScaling("log");

        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);

        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(false);
        style.dataStyle().errorBarStyle().setVisible(false);

        // Turn off the legend
        style.legendBoxStyle().setVisible(true);

        return style;
    }

}
