package org.hps.monitoring.subsys.et;

import java.util.ArrayList;
import java.util.List;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUpdater;
import org.hps.monitoring.subsys.SystemStatisticsImpl;
import org.hps.record.et.EtEventProcessor;
import org.jfree.data.time.Second;
import org.jlab.coda.et.EtEvent;
import org.lcsim.util.aida.AIDA;

/**
 * A basic set of strip charts for monitoring the ET system.
 */
public final class EtSystemStripCharts extends EtEventProcessor {

    SystemStatisticsImpl stats = new SystemStatisticsImpl();
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ET System Monitoring");
    List<StripChartUpdater> updaters = new ArrayList<StripChartUpdater>();
    
    public EtSystemStripCharts() {
        stats.setTickLengthMillis(1000);
    }
    
    /**
     * Setup the strip charts for ET system monitoring and start accumulating statistics.
     */
    @Override
    public void startJob() {

        // Create the ET system strip charts.
        createStripCharts();

        // Start systems statistics task.
        stats.start();
    }

    /**
     * 
     */
    private void createStripCharts() {
        updaters.add(plotFactory.createStripChart(
                "Data Rate", 
                "MB / second", 
                1, 
                new String[] { "Data" }, 
                999, 
                new Second(), 
                stats.new MegabytesPerSecondProvider(), 
                200000L));

        updaters.add(plotFactory.createStripChart(
                "Total Data", 
                "Megabytes", 
                1, 
                new String[] { "Data" },
                999,
                new Second(), 
                stats.new TotalMegabytesProvider(), 
                200000L));
        
        updaters.add(plotFactory.createStripChart(
                "Event Rate", 
                "Events / s", 
                1, 
                new String[] { "Data" }, 
                999, 
                new Second(), 
                stats.new EventsPerSecondProvider(), 
                200000L));
        
        updaters.add(plotFactory.createStripChart(
                "Total Events", 
                "Number of Events", 
                1, 
                new String[] { "Data" }, 
                999, 
                new Second(), 
                stats.new TotalEventsProvider(), 
                200000L));
    }

    @Override
    public void process(EtEvent event) {
        stats.update(event.getLength());
    }

    public void endJob() {

        // Stop the strip chart updaters.
        for (StripChartUpdater updater : updaters) {
            updater.stop();
        }
        
        // Stop system statistics task.
        stats.stop();
    }
}