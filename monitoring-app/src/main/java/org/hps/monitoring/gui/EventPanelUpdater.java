package org.hps.monitoring.gui;

import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This class is an {@link org.hps.monitoring.record.evio.EvioEventProcessor}
 * that updates the {@link EventPanel} as EVIO events are processed. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EventPanelUpdater extends EvioEventProcessor {
    
    EventPanel eventPanel;
    
    long jobStartTime;
    
    EventPanelUpdater(EventPanel eventPanel) {
        this.eventPanel = eventPanel;
    }
    
    public void startJob() {
        
        // Reset event GUI.
        eventPanel.reset();

        // This is only reset between different jobs.
        eventPanel.resetSessionSupplied();
        
        eventPanel.setJobStartTime(System.currentTimeMillis());
    }
    
    public void processEvent(EvioEvent evioEvent) {
        eventPanel.updateEventCount();
        eventPanel.updateAverageEventRate();
    }

    public void endJob() {
        // Push final event counts to the GUI.
        eventPanel.endJob();      
    }

    public void startRun(EvioEvent event) {
        
        // Get start of run data.
        int[] data = event.getIntData();
        int seconds = data[0];
        int runNumber = data[1];        
        final long millis = ((long) seconds) * 1000;
        
        // Update the GUI.
        eventPanel.setRunNumber(runNumber);
        eventPanel.setRunStartTime(millis);        
    }

    public void endRun(EvioEvent event) {
        
        // Get end run data.
        int[] data = event.getIntData();
        int seconds = data[0];
        int eventCount = data[2];
        final long millis = ((long) seconds) * 1000;
        
        // Update the GUI.
        eventPanel.setRunEndTime(millis);
        eventPanel.setRunEventCount(eventCount);
    }
    
}
