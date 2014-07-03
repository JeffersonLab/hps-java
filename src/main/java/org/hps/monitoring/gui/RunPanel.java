package org.hps.monitoring.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Dashboard for displaying information about the current run.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class RunPanel extends JPanel {

    FieldPanel runNumberField = new FieldPanel("Run Number", "", 10, false);
    DatePanel startDateField = new DatePanel("Run Start", "", 16, false); 
    DatePanel endDateField = new DatePanel("Run End", "", 16, false);
    FieldPanel lengthField = new FieldPanel("Run Length [sec]", "", 12, false);
    FieldPanel totalEventsField = new FieldPanel("Total Events in Run", "", 14, false);
    FieldPanel elapsedTimeField = new FieldPanel("Elapsed Time [sec]", "", 14, false);;
    FieldPanel eventsReceivedField = new FieldPanel("Events Received", "", 14, false);
    FieldPanel dataReceivedField = new FieldPanel("Data Received [bytes]", "", 14, false);
          
    RunPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Run Summary");
        setBorder(titledBorder);
        add(runNumberField);
        add(startDateField);
        add(endDateField);
        add(lengthField);
        add(totalEventsField);
        add(elapsedTimeField);
        add(eventsReceivedField);
        add(dataReceivedField);
        this.setMinimumSize(new Dimension(0, 190));
    }
    
    /**
     * An <tt>EvioEventProcessor</tt> for updating the <tt>RunPanel</tt>
     * by processing EVIO events.
     */
    class RunPanelUpdater extends EvioEventProcessor {
    
        long startMillis;
        long endMillis;
        int eventsReceived;
        long totalBytes;
        Timer timer;        
        long jobStartMillis;
        
        public void startJob() {
            eventsReceived = 0;
            runNumberField.setValue("");
            startDateField.setValue("");
            endDateField.setValue("");
            lengthField.setValue("");
            totalEventsField.setValue("0");
            
            timer = new Timer("UpdateTime");
            jobStartMillis = System.currentTimeMillis();   
            TimerTask updateTimeTask = new TimerTask() {                       
                public void run() {
                    final long elapsedTime = (System.currentTimeMillis() - jobStartMillis) / 1000;
                    elapsedTimeField.setValue(elapsedTime);
                }            
            };        
            timer.scheduleAtFixedRate(updateTimeTask, 0, 1000);            
        }
        
        public void processEvent(EvioEvent event) {
            ++eventsReceived;
            totalBytes += (long)event.getTotalBytes();
            eventsReceivedField.setValue(eventsReceived);
            dataReceivedField.setValue(totalBytes);
        }
        
        public void startRun(EvioEvent event) {
   
            // Get start of run data.
            int[] data = event.getIntData();
            int seconds = data[0];
            int runNumber = data[1];        
            startMillis = ((long) seconds) * 1000;
            
            // Update the GUI.
            runNumberField.setValue(runNumber);
            startDateField.setValue(new Date(startMillis));
        }

        public void endRun(EvioEvent event) {

            // Get end run data.
            int[] data = event.getIntData();
            int seconds = data[0];
            int eventCount = data[2];
            endMillis = ((long) seconds) * 1000;
            long elapsedMillis = endMillis - startMillis;
            long elapsedSeconds = (long)((double)elapsedMillis / 1000.);
            
            // Update the GUI.
            endDateField.setValue(new Date(endMillis));
            totalEventsField.setValue(eventCount);
            lengthField.setValue(elapsedSeconds);
        }
        
        public void endJob() {
            //System.out.println("RunPanelUpdater.endJob");
            timer.cancel();
            timer.purge();
        }
    }
}