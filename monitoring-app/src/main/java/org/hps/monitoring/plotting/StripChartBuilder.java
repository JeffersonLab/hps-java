package org.hps.monitoring.plotting;

import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public final class StripChartBuilder {

    private StripChartBuilder() {        
    }
    
    /**
     * This is appropriate for a strip chart that will be updated at fixed intervals on a timer.
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
        NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
        rangeAxis.setRange(0., 1.);
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeIncludesZero(true);
        return result;
    }
    
    /**
     * This should be used when the time period for updating is variable.  (I think???)
     * @param title
     * @param yAxisLabel
     * @param maxAge
     * @param maxCount
     * @return
     */
    /* 
    To update chart of this type:     
    sensorSeries.add(new Minute(new Date()), newData);
    */
    // TODO: test case
    public static JFreeChart createTimeSeriesChart(String title, String yAxisLabel, int maxAge, int maxCount) {
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries timeSeries = new TimeSeries("Default Dataset");
        timeSeries.setMaximumItemAge(maxAge);
        timeSeries.setMaximumItemCount(maxCount);
        
        final JFreeChart result = ChartFactory.createTimeSeriesChart(title, "hh:mm:ss", yAxisLabel, dataset, true, true, false);
        final XYPlot plot = result.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);                
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(1.0);
        return result;
    }
    
        
}