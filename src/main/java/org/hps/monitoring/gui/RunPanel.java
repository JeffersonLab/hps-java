package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.model.RunModel.DATA_RECEIVED_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.ELAPSED_TIME_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.END_DATE_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.EVENTS_RECEIVED_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.EVENT_NUMBER_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.RUN_LENGTH_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.RUN_NUMBER_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.START_DATE_PROPERTY;
import static org.hps.monitoring.gui.model.RunModel.TOTAL_EVENTS_PROPERTY;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.hps.evio.EventConstants;
import org.hps.monitoring.gui.model.RunModel;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Dashboard for displaying information about the current run.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add current data rate field (measured over last ~second).
// TOOD: Add current event rate field (measured over last ~second).
// TODO: Add event sequence number from CompositeRecord.
// TODO: Add average data rate field (over entire session).
// TODO: Add average proc time per event field (over entire session).
public class RunPanel extends JPanel implements PropertyChangeListener {

    FieldPanel runNumberField = new FieldPanel("Run Number", "", 10, false);
    DatePanel startDateField = new DatePanel("Run Start", "", 16, false); 
    DatePanel endDateField = new DatePanel("Run End", "", 16, false);
    FieldPanel lengthField = new FieldPanel("Run Length [sec]", "", 12, false);
    FieldPanel totalEventsField = new FieldPanel("Total Events in Run", "", 14, false);
    FieldPanel elapsedTimeField = new FieldPanel("Elapsed Time [sec]", "", 14, false);
    FieldPanel eventsReceivedField = new FieldPanel("Events Received", "", 14, false);
    FieldPanel dataReceivedField = new FieldPanel("Data Received [bytes]", "", 14, false);
    FieldPanel eventNumberField = new FieldPanel("Event Number", "", 14, false);
    
    Timer timer;
    long jobStartMillis;
    
    RunModel model;
          
    RunPanel(RunModel model) {
        this.model = model;
        this.model.addPropertyChangeListener(this);
        
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
        add(eventNumberField);
        
        this.setMinimumSize(new Dimension(0, 190));
    }
           
    void startJobTimer() {
        timer = new Timer("JobTimer");
        jobStartMillis = System.currentTimeMillis();
        TimerTask updateTimeTask = new TimerTask() {                       
            public void run() {
                final int elapsedTime = (int)((System.currentTimeMillis() - jobStartMillis) / 1000);
                model.setElapsedTime(elapsedTime);
            }            
        };
        timer.scheduleAtFixedRate(updateTimeTask, 0, 1000);
    }
    
    void stopRunTimer() {
        timer.cancel();
        timer.purge();
    }
             
    class RunModelUpdater extends CompositeProcessor {
       
        @Override
        public void startJob() {
            model.reset();
            RunPanel.this.startJobTimer();
        }
        
        @Override
        public void process(CompositeRecord event) {
            model.incrementEventsReceived();            
            EvioEvent evioEvent = event.getEvioEvent();
            if (event.getEtEvent() != null && event.getEvioEvent() == null) {
                model.addDataReceived(event.getEtEvent().getData().length);
            } else if (evioEvent != null) {
                model.addDataReceived((long)evioEvent.getTotalBytes());
                model.setEventNumber(evioEvent.getEventNumber());
                if (EventConstants.isPreStartEvent(evioEvent)) {                    
                    startRun(evioEvent);
                } else if (EventConstants.isEndEvent(evioEvent)) {                    
                    endRun(evioEvent);
                }        
            } else if (event.getLcioEvent() != null) {
                model.setEventNumber(event.getLcioEvent().getEventNumber());
            }
        }

        private void endRun(EvioEvent evioEvent) {            
            // Get end run data.
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            int eventCount = data[2];
            long endMillis = ((long) seconds) * 1000;
            
            // Update the GUI.
            model.setEndDate(new Date(endMillis));
            model.computeRunLength();              
            model.setTotalEvents(eventCount);
        }

        private void startRun(EvioEvent evioEvent) {            
            // Get start of run data.
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            int runNumber = data[1];        
            long startMillis = ((long) seconds) * 1000;
            
            // Update the GUI.
            model.setRunNumber(runNumber);
            model.setStartDate(new Date(startMillis));
        }
                
        @Override
        public void endJob() {
            RunPanel.this.stopRunTimer();
        }        
    }

    /**
     * Update the GUI from changes in the underlying RunModel object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {        
        Object value = evt.getNewValue();
        if (RUN_NUMBER_PROPERTY.equals(evt.getPropertyName())) {            
            this.runNumberField.setValue((Integer) value);            
        } else if (START_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null)
                this.startDateField.setValue((Date) value);
            else
                this.startDateField.setValue("");
        } else if (END_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null)
                this.endDateField.setValue((Date) value);
            else
                this.endDateField.setValue("");
        } else if (RUN_LENGTH_PROPERTY.equals(evt.getPropertyName())) {
            this.lengthField.setValue((Integer) value);
        } else if (TOTAL_EVENTS_PROPERTY.equals(evt.getPropertyName())) {
            this.totalEventsField.setValue((Integer) value);
        } else if (EVENTS_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            this.eventsReceivedField.setValue((Integer) value);
        } else if (ELAPSED_TIME_PROPERTY.equals(evt.getPropertyName())) {
            this.elapsedTimeField.setValue((Integer) value);
        } else if (DATA_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            this.dataReceivedField.setValue((Long) value);
        } else if (EVENT_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            this.eventNumberField.setValue((Integer) value);
        }
    }
}