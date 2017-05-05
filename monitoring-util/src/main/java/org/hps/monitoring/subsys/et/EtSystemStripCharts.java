/**
 * 
 */
package org.hps.monitoring.subsys.et;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.SystemStatistics;
import org.hps.monitoring.subsys.SystemStatisticsImpl;
import org.hps.monitoring.subsys.SystemStatisticsListener;
import org.hps.record.et.EtEventProcessor;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jlab.coda.et.EtEvent;
import org.lcsim.util.aida.AIDA;

/**
 * This will show a series of strip charts from ET system performance statistics
 * such as event and data rates.
 * 
 */
public class EtSystemStripCharts extends EtEventProcessor implements SystemStatisticsListener {

    // The system statistics.
    SystemStatisticsImpl stats = new SystemStatisticsImpl();
    
    // Plotting API.
    MonitoringPlotFactory plotFactory = 
            (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ET System Monitoring");
    
    // List of charts.
    List<JFreeChart> charts = new ArrayList<JFreeChart>();
    
    // Range size in milliseconds.
    static final double RANGE_SIZE = 200000;
    
    // Chart collection indices.
    static final int DATA_RATE_COLLECTION_INDEX = 0;    
    static final int TOTAL_DATA_COLLECTION_INDEX = 1;    
    static final int EVENT_RATE_COLLECTION_INDEX = 2;
    static final int TOTAL_EVENTS_COLLECTION_INDEX = 3;
        
    public EtSystemStripCharts() {          
        // Set 2 seconds between statistics updates.
        stats.setNominalTickLengthMillis(1000);
    }
    
    /**
     * Setup the strip charts for ET system monitoring and start accumulating statistics.
     */
    @Override
    public void startJob() {
           
        // Register this class as a listener to activate update at end of statistics clock tick.
        stats.addSystemStatisticsListener(this);

        // Start systems statistics task.
        stats.start();
    }

    /**
     * Create the strip charts for plotting the basic ET system statistics.
     */
    private void createStripCharts() {
        
        // Data rate and average data reate in megabytes per second.
        charts.add(plotFactory.createTimeSeriesChart(
                "Data Rate", 
                "MB / second",
                2, 
                new String[] { "Data Rate", "Average Data Rate" },
                RANGE_SIZE));
                
        // Total megabytes received.
        charts.add(plotFactory.createTimeSeriesChart("Total Data", "Megabytes", 1, null, RANGE_SIZE));
        
        // Event rate and average event rate in hertz.
        charts.add(plotFactory.createTimeSeriesChart(
                "Event Rate", 
                "Hz", 
                2, 
                new String[] { "Event Rate", "Average Event Rate" }, 
                RANGE_SIZE));
        
        // Total number of events received.
        charts.add(plotFactory.createTimeSeriesChart("Total Events", "Number of Events", 1, null, RANGE_SIZE));
              
    }

    @Override
    public void process(EtEvent event) {
        stats.update(event.getLength());
    }

    public void endJob() {
        // Stop system statistics task.
        stats.stop();
    }
    
    TimeSeriesCollection getTimeSeriesCollection(int chartIndex) {
        return (TimeSeriesCollection) charts.get(chartIndex).getXYPlot().getDataset();
    }

    /**
     * Hook for updating the charts at end of statistics clock tick.
     * @param stats The statistics with the system information.
     */
    @Override
    public void endTick(SystemStatistics stats) {
        
        Date now = new Date(stats.getTickEndTimeMillis());
                
        getTimeSeriesCollection(DATA_RATE_COLLECTION_INDEX).getSeries(0).addOrUpdate(
                new Second(now), stats.getMegabytesPerSecond());
        
        getTimeSeriesCollection(DATA_RATE_COLLECTION_INDEX).getSeries(1).addOrUpdate(
                new Second(now), stats.getAverageMegabytesPerSecond());
        
        getTimeSeriesCollection(TOTAL_DATA_COLLECTION_INDEX).getSeries(0).addOrUpdate(
                new Second(now), stats.getTotalMegabytes());
        
        getTimeSeriesCollection(EVENT_RATE_COLLECTION_INDEX).getSeries(0).addOrUpdate(
                new Second(now), stats.getEventsPerSecond());
        
        getTimeSeriesCollection(EVENT_RATE_COLLECTION_INDEX).getSeries(1).addOrUpdate(
                new Second(now), stats.getAverageEventsPerSecond());
        
        getTimeSeriesCollection(TOTAL_EVENTS_COLLECTION_INDEX).getSeries(0).addOrUpdate(
                new Second(now), stats.getTotalEvents());
    }
     
    @Override
    public void started(SystemStatistics stats) {
        createStripCharts();
    }
   
    @Override
    public void stopped(SystemStatistics stats) {
    }
}
