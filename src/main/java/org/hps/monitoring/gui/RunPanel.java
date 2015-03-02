package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.model.RunModel.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.hps.monitoring.gui.model.RunModel;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * Dashboard for displaying information about the current run.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class RunPanel extends JPanel implements PropertyChangeListener {

    FieldPanel runNumberField = new FieldPanel("Run Number", "", 10, false);
    DatePanel startDateField = new DatePanel("Run Start", "", 16, false);
    DatePanel endDateField = new DatePanel("Run End", "", 16, false);
    FieldPanel lengthField = new FieldPanel("Run Length [sec]", "", 12, false);
    FieldPanel totalEventsField = new FieldPanel("Total Events in Run", "", 14, false);
    FieldPanel elapsedTimeField = new FieldPanel("Elapsed Time [sec]", "", 14, false);
    FieldPanel eventsReceivedField = new FieldPanel("Events Received", "", 14, false);
    FieldPanel dataReceivedField = new FieldPanel("Data Received [MB]", "", 14, false);
    FieldPanel eventNumberField = new FieldPanel("Event Number", "", 14, false);
    FieldPanel dataRateField = new FieldPanel("Data Rate [MB/s]", "", 12, false);
    FieldPanel eventRateField = new FieldPanel("Event Rate [evt/s]", "", 14, false);

    RunModel model;
    
    static final NumberFormat formatter = new DecimalFormat("#0.00"); 

    RunPanel(RunModel model) {
        this.model = model;
        this.model.addPropertyChangeListener(this);

        setLayout(new FlowLayout(FlowLayout.LEFT));

        TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Run Summary");
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
        add(dataRateField);
        add(eventRateField);

        this.setMinimumSize(new Dimension(0, 240));
    }

    class RunModelUpdater extends CompositeRecordProcessor {

        Timer timer;
        
        int eventsReceived;
        double bytesReceived;
        int totalEvents;
        int eventNumber;
        int runNumber = -1;
        long jobStartMillis;
        long lastTickMillis = 0;
        static final long millis = 1000;
        
        class RunTimerTask extends TimerTask {
            
            public void run() {                     
                
                double tickLengthSeconds = (System.currentTimeMillis() - lastTickMillis) / (double)millis;
                int elapsedTime = (int) ((System.currentTimeMillis() - jobStartMillis) / (double)millis);
                double megaBytesReceived = bytesReceived / 1000000;
                totalEvents += eventsReceived;

                /*
                System.out.println("tickLengthSeconds = " + tickLengthSeconds);
                System.out.println("elapsedTime = " + elapsedTime);
                System.out.println("eventsReceived = " + eventsReceived);
                System.out.println("dataRate = " + (megaBytesReceived / tickLengthSeconds));
                System.out.println("eventNumber = " + eventNumber);
                System.out.println("eventRate = " + (eventsReceived / tickLengthSeconds));
                System.out.println("totalEvents = " + totalEvents);
                System.out.println("megaBytesReceived = " + megaBytesReceived);
                */
                
                model.setElapsedTime(elapsedTime);
                model.setEventsReceived(totalEvents);
                model.setDataRate(megaBytesReceived / tickLengthSeconds);
                model.addDataReceived(megaBytesReceived);
                model.setEventNumber(eventNumber);
                model.setEventRate(eventsReceived / tickLengthSeconds);
                
                eventsReceived = 0;
                bytesReceived = 0;
                eventNumber = 0;  
                
                lastTickMillis = System.currentTimeMillis();
                
                // System.out.println();
            }        
        }
        
        @Override
        public void startJob() {
            model.reset();
            jobStartMillis = System.currentTimeMillis();
            
            // Start the timer to update GUI components about once per second.
            timer = new Timer("RunModelUpdaterTimer");
            lastTickMillis = System.currentTimeMillis();
            timer.scheduleAtFixedRate(new RunTimerTask(), 0, 1000);
        }

        @Override
        public void process(CompositeRecord event) {            
            if (event.getEvioEvent() != null) {
                EvioEvent evioEvent = event.getEvioEvent();
                bytesReceived += evioEvent.getTotalBytes();
                if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
                    // Get run start info from pre start event.
                    startRun(evioEvent);
                } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
                    // Get end run info from end event.
                    endRun(evioEvent);
                } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {                    
                    // Check for run info in head bank.
                    checkHeadBank(evioEvent);
                    eventNumber = evioEvent.getEventNumber();
                    eventsReceived += 1;
                }
            } else if (event.getEtEvent() != null) {
                bytesReceived += event.getEtEvent().getData().length;
                eventNumber = event.getEtEvent().getId();
                eventsReceived += 1;
            } else if (event.getLcioEvent() != null) {
                EventHeader lcioEvent = event.getLcioEvent();
                eventNumber = lcioEvent.getEventNumber();
                if (lcioEvent.getRunNumber() != runNumber) {
                    runNumber = lcioEvent.getRunNumber();
                    startRun(lcioEvent);
                }
                eventsReceived += 1;
            }                    
        }

        /**
         * Check for head bank and update the run info if necessary.
         * @param evioEvent The EVIO event.
         */
        private void checkHeadBank(EvioEvent evioEvent) {
            BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
            if (headBank != null) {
                int headBankRun = headBank.getIntData()[1];
                if (headBankRun != runNumber) {
                    runNumber = headBankRun;
                    model.setRunNumber(headBankRun);
                    model.setStartDate(new Date(headBank.getIntData()[3] * 1000));
                }
            }
        }

        private void endRun(EvioEvent evioEvent) {
            // Get end run data.
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                int eventCount = data[2];
                long endMillis = ((long) seconds) * 1000;

                // Update the GUI.
                model.setEndDate(new Date(endMillis));
                model.computeRunLength();
                model.setTotalEvents(eventCount);
            }
        }

        private void startRun(EvioEvent evioEvent) {
            // Get start of run data.
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                runNumber = data[1];
                
                // Update the GUI.
                model.setRunNumber(runNumber);
                model.setStartDate(new Date(seconds * 1000));
            }
        }
        
        private void startRun(EventHeader lcioEvent) {
            model.setRunNumber(lcioEvent.getRunNumber());
            long seconds = lcioEvent.getTimeStamp() / 1000000000;
            model.setStartDate(new Date((int)seconds));
        }
        
        @Override
        public void endJob() {
            timer.cancel();
            
            // Push final values into GUI.
            timer = new Timer("RunModelUpdaterEndJobTimer");
            timer.schedule(new RunTimerTask(), 0);
        }
    }
    
    /**
     * Update the GUI from changes to the backing RunModel object.
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
            this.dataReceivedField.setValue(formatter.format((Double) value));
        } else if (EVENT_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            this.eventNumberField.setValue((Integer) value);
        } else if (DATA_RATE_PROPERTY.equals(evt.getPropertyName())) {
            this.dataRateField.setValue(formatter.format((Double) value));
        } else if (EVENT_RATE_PROPERTY.equals(evt.getPropertyName())) {
            this.eventRateField.setValue(formatter.format((Double) value));
        }
    }
}