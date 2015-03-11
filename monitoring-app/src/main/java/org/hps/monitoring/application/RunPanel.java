package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import org.hps.monitoring.application.model.RunModel;
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

    RunModel runModel;
    
    static final NumberFormat formatter = new DecimalFormat("#0.00"); 

    public RunPanel() {
        build();
    }
    
    public RunPanel(RunModel runModel) {
        this.runModel = runModel;
        this.runModel.addPropertyChangeListener(this);
        build();
    }
    
    private void build() {
        
        setLayout(new GridBagLayout());

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Run Summary");
        setBorder(titledBorder);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;                
        add(runNumberField, c);
        add(startDateField, c);
        add(endDateField, c);
        add(lengthField, c);
        add(totalEventsField, c);
        
        c = new GridBagConstraints();
        c.gridx = 1;
        add(elapsedTimeField, c);
        add(eventsReceivedField, c);
        add(dataReceivedField, c);
        add(eventNumberField, c);
        add(dataRateField, c);
        add(eventRateField, c);

        setMinimumSize(new Dimension(400, 240));
    }
    
    public void setModel(RunModel runModel) {
        this.runModel = runModel;
    }

    class RunPanelUpdater extends CompositeRecordProcessor {

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
                
                runModel.setElapsedTime(elapsedTime);
                runModel.setEventsReceived(totalEvents);
                runModel.setDataRate(megaBytesReceived / tickLengthSeconds);
                runModel.addDataReceived(megaBytesReceived);
                runModel.setEventNumber(eventNumber);
                runModel.setEventRate(eventsReceived / tickLengthSeconds);
                
                eventsReceived = 0;
                bytesReceived = 0;
                eventNumber = 0;  
                
                lastTickMillis = System.currentTimeMillis();
                
                // System.out.println();
            }        
        }
        
        @Override
        public void startJob() {
            runModel.reset();
            jobStartMillis = System.currentTimeMillis();
            
            // Start the timer to update GUI components about once per second.
            timer = new Timer("RunModelUpdaterTimer");
            lastTickMillis = System.currentTimeMillis();
            timer.scheduleAtFixedRate(new RunTimerTask(), 0, 1000);
        }

        @Override
        public void process(CompositeRecord event) {          
            // FIXME: CompositeRecord number is always -1 here.
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
                    runModel.setRunNumber(headBankRun);
                    runModel.setStartDate(new Date(headBank.getIntData()[3] * 1000));
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
                runModel.setEndDate(new Date(endMillis));
                runModel.computeRunLength();
                runModel.setTotalEvents(eventCount);
            }
        }

        private void startRun(EvioEvent evioEvent) {
            // Get start of run data.
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                runNumber = data[1];
                
                // Update the GUI.
                runModel.setRunNumber(runNumber);
                runModel.setStartDate(new Date(seconds * 1000));
            }
        }
        
        private void startRun(EventHeader lcioEvent) {
            runModel.setRunNumber(lcioEvent.getRunNumber());
            long seconds = lcioEvent.getTimeStamp() / 1000000000;
            runModel.setStartDate(new Date((int)seconds));
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
        //System.out.println("RunPanel.propertyChange - " + evt.getPropertyName());
        Object value = evt.getNewValue();
        if (RunModel.RUN_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            runNumberField.setValue((Integer) value);
        } else if (RunModel.START_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null)
                startDateField.setValue((Date) value);
            else
                startDateField.setValue("");
        } else if (RunModel.END_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null)
                endDateField.setValue((Date) value);
            else
                endDateField.setValue("");
        } else if (RunModel.RUN_LENGTH_PROPERTY.equals(evt.getPropertyName())) {
            lengthField.setValue((Integer) value);
        } else if (RunModel.TOTAL_EVENTS_PROPERTY.equals(evt.getPropertyName())) {
            totalEventsField.setValue((Integer) value);
        } else if (RunModel.EVENTS_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            eventsReceivedField.setValue((Integer) value);
        } else if (RunModel.ELAPSED_TIME_PROPERTY.equals(evt.getPropertyName())) {
            elapsedTimeField.setValue((Integer) value);
        } else if (RunModel.DATA_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            dataReceivedField.setValue(formatter.format((Double) value));
        } else if (RunModel.EVENT_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            eventNumberField.setValue((Integer) value);
        } else if (RunModel.DATA_RATE_PROPERTY.equals(evt.getPropertyName())) {
            dataRateField.setValue(formatter.format((Double) value));
        } else if (RunModel.EVENT_RATE_PROPERTY.equals(evt.getPropertyName())) {
            eventRateField.setValue(formatter.format((Double) value));
        }
    }
}