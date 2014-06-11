package org.hps.monitoring.subsys.et;

import org.hps.monitoring.record.etevent.EtEventProcessor;

/**
 * This is a barebones implementation of an ET system monitor.
 * It does not do much right now but accumulate statistics 
 * and set basic system statuses.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtSystemMonitor extends EtEventProcessor {
    /*
//implements HasSystemInfo {
        
    //SystemInfo info = new SystemInfoImpl("EtSystem");
    SystemStatistics stats = new SystemStatisticsImpl();
    
    // TEST: strip chart stuff
    JFrame plotFrame;
    JFreeChart stripChart;
    StripChartUpdater updater;
    Timer timer;
    
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ET System Monitoring");
    
    @Override
    public void startJob() {
        stats.start();
        //info.getStatus().setStatus(StatusCode.OKAY, "EtSystemMonitor set okay.");
        
        // TEST: setup strip chart
        JFreeChart stripChart = plotFactory.createStripChart("Average Event Rate", "Event Count", 100);
        updater = new AverageEventRateUpdater();
        updater.setChart(stripChart);
        timer = updater.start();
    }
    
    @Override
    public void processEvent(EtEvent event) {
        stats.update(event.getLength());
        //info.getStatistics()
    }    
    
    @Override
    public void endJob() {
        //info.getStatistics().stop();
        stats.stop();
        //info.getStatus().setStatus(StatusCode.OFFLINE, "EtSystemMonitor set offline.");

        // TEST: stop timer for updating strip chart
        timer.cancel();
        timer.purge();
    }
        
    class AverageEventRateUpdater extends StripChartUpdater {

        @Override
        public float nextValue() {
            return (float)stats.getAverageEventsPerSecond();
        }
        
    }
    */
}
