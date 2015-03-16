package org.hps.monitoring.plotting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * Utility methods for building strip charts using JFreeChart back end.
 */
public final class StripChartBuilder {

    private StripChartBuilder() {
    }
    
    /**
     * Create a strip chart with simple parameter settings.
     * @param name The title of the strip chart.
     * @param seriesCount The number of series in the data set.
     * @param timeBase The time interval for updating.
     * @param valueProvider The interface for providing values.
     * @return The StripChartUpdater for the chart.
     */
    static StripChartUpdater createStripChart(
            String name, 
            int seriesCount, 
            RegularTimePeriod timeBase,
            ValueProvider valueProvider) {
        return createStripChart(name, "Values", seriesCount, null, 9999, timeBase, valueProvider, 10000L);
    }
    
    /**
     * This creates a strip chart with full parameter settings, which will automatically
     * update at a certain time interval.
     * @param name The title of the chart.
     * @param rangeLabel The range axis label text.
     * @param seriesCount The number of series in the data set.
     * @param seriesNames The names of the series (if non-null the length must match seriesCount).
     * @param itemCount The maximum number of items in the series.
     * @param timeBase The time unit for updates.
     * @param valueProvider The interface for providing the series values.
     * @param rangeView The view in the domain axis around the current data point (milliseconds).
     * @return The StripChartUpdater for the chart.
     */
    static StripChartUpdater createStripChart(
            String name, 
            String rangeLabel,
            int seriesCount, 
            String[] seriesNames,
            int itemCount,
            RegularTimePeriod timeBase,
            ValueProvider valueProvider,
            long rangeView) {
                
        if (seriesNames != null && seriesCount != seriesNames.length) {
            throw new IllegalArgumentException("seriesNames is wrong length");
        }
        final DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(seriesCount, itemCount, timeBase);
        dataset.setTimeBase(timeBase);
        for (int series = 0; series < seriesCount; series++) {
            String seriesName = name + " " + series;
            if (seriesNames != null) {
                seriesName = seriesNames[series];
            }
            dataset.addSeries(new float[] {}, series, seriesName);
        }
        
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(name, "hh:mm:ss", rangeLabel, dataset, false, false, false);
        
        chart.getXYPlot().getRangeAxis().setAutoRange(true);

        StripChartUpdater updater = new StripChartUpdater(
                chart, 
                valueProvider,
                rangeView,
                timeBase
                );
        
        updater.start();
        
        return updater;
    }
        
    /**
     * This should be used when the time period for updating is variable.
     * 
     * To update a chart of this type:
     * 
     * <code>sensorSeries.add(new Minute(new Date()), newData);</code>
     * 
     * @param title The title of the chart.
     * @param yAxisLabel The range axis label.
     * @param maxAge The maximum age of an item.
     * @param maxCount The maximum count of items in the single data set series.
     * @return The chart that was created.
     */
    public static JFreeChart createTimeSeriesChart(
            String title, 
            String yAxisLabel, 
            int maxAge, 
            int maxCount,
            int rangeSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries timeSeries = new TimeSeries("Default Dataset");
        timeSeries.setMaximumItemAge(maxAge);
        timeSeries.setMaximumItemCount(maxCount);
        dataset.addSeries(timeSeries);

        final JFreeChart result = ChartFactory.createTimeSeriesChart(
                title, 
                "hh:mm:ss", 
                yAxisLabel, 
                dataset, 
                true, 
                false, 
                false);
        final XYPlot plot = result.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.getDomainAxis().setAutoRangeMinimumSize(rangeSize);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeIncludesZero(true);
        return result;
    }    
    
    /**
     * <p>
     * This can be used to create a strip chart with multiple <code>TimeSeries</code> 
     * in the data set.
     * <p>
     * To update a chart of this type, use the following types of method calls:
     * <pre>
     * dataset.getSeries(0).add(new Second(time), value1);
     * dataset.getSeries(1).add(new Second(time), value2);
     * </pre>
     * <p>
     * It is not updated manually but will refresh will values are added to the backing dataset.
     * 
     * @param title The title of the chart.
     * @param yAxisLabel The range axis label.
     * @param seriesCount The number of series in the dataset.
     * @param datasetNames The names of the datasets (can be null to use defaults).
     * @param rangeSize The range of values to show for auto-ranging in domain axis (in milliseconds).
     * @return The chart that was created.
     */
    public static JFreeChart createTimeSeriesChart(
            String title, 
            String yAxisLabel, 
            int seriesCount,
            String[] datasetNames,
            double rangeSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int i = 0; i < seriesCount; i++) {
            String datasetName = "Dataset " + i;
            if (datasetNames != null) {
                datasetName = datasetNames[i];
            }
            TimeSeries timeSeries = new TimeSeries(datasetName);
            dataset.addSeries(timeSeries);
        }
               
        final JFreeChart result = ChartFactory.createTimeSeriesChart(
                title, 
                "hh:mm:ss", 
                yAxisLabel, 
                dataset, 
                true, 
                false, 
                false);
        final XYPlot plot = result.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeIncludesZero(true);
        
        plot.getDomainAxis().setAutoRange(true);
        plot.getDomainAxis().setAutoRangeMinimumSize(rangeSize);
        
        return result;
    }        
}