package org.hps.monitoring.plotting;

import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


public final class StripChartUtil {

    private StripChartUtil() {
    }
    
    public static TimeSeries getTimeSeries(JFreeChart chart) {
        return (TimeSeries)((TimeSeriesCollection)chart.getXYPlot().getDataset()).getSeries().get(0);
    }
    
}
