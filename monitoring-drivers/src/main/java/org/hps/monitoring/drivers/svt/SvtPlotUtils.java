package org.hps.monitoring.drivers.svt;

import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class SvtPlotUtils {

    public static int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 6 * (sensor.getLayerNumber() - 1);
            } else {
                return 6 * (sensor.getLayerNumber() - 1) + 1;
            }
        } else {

            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 2;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 4;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 5;
                }
            }
        }

        return -1;
    }

    /**
     * Create a plotter style.
     *
     * @param xAxisTitle : Title of the x axis
     * @param yAxisTitle : Title of the y axis
     * @return plotter style
     */
    // TODO: Move this to a utilities class
    public static IPlotterStyle createStyle(IPlotterFactory plotterFactory, String xAxisTitle, String yAxisTitle) {

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

        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);

        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);
        style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
        style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        style.dataStyle().errorBarStyle().setVisible(false);

        // Turn off the legend
        style.legendBoxStyle().setVisible(false);

        return style;
    }
}
