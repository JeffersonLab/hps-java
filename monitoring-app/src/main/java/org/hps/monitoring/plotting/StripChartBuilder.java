package org.hps.monitoring.plotting;

import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * Utility methods for building strip charts using JFreeChart backend.
 */
public final class StripChartBuilder {

    private StripChartBuilder() {
    }

    /**
     * This creates a strip chart that will be updated at fixed intervals from a timer.
     * @param title
     * @param yAxisLabel
     * @param size
     * @return
     */
    public static JFreeChart createDynamicTimeSeriesChart(String title, String yAxisLabel, int size) {
        final DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, size, new Second());
        dataset.setTimeBase(new Second(new Date()));
        dataset.addSeries(new float[] {}, 0, "Default Dataset");

        final JFreeChart result = ChartFactory.createTimeSeriesChart(title, "hh:mm:ss", yAxisLabel, dataset, true, true, false);
        final XYPlot plot = result.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeIncludesZero(true);
        return result;
    }

    /**
     * This should be used when the time period for updating is variable.
     * 
     * To update a chart of this type:
     * 
     * <code>sensorSeries.add(new Minute(new Date()), newData);</code>
     * 
     * @param title
     * @param yAxisLabel
     * @param maxAge
     * @param maxCount
     * @return
     */
    public static JFreeChart createTimeSeriesChart(String title, String yAxisLabel, int maxAge, int maxCount, int rangeSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries timeSeries = new TimeSeries("Default Dataset");
        timeSeries.setMaximumItemAge(maxAge);
        timeSeries.setMaximumItemCount(maxCount);
        dataset.addSeries(timeSeries);

        final JFreeChart result = ChartFactory.createTimeSeriesChart(title, "hh:mm:ss", yAxisLabel, dataset, true, true, false);
        final XYPlot plot = result.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.getDomainAxis().setAutoRangeMinimumSize(rangeSize);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeIncludesZero(true);
        return result;
    }

}