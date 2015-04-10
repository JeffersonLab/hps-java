package org.hps.monitoring.application;

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

import org.hps.monitoring.application.model.RunModel;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This class implements a "dashboard" for displaying information about the current run.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class EventDashboard extends JPanel implements PropertyChangeListener {

    /**
     * Updates the fields as events are processed.
     */
    class EventDashboardUpdater extends CompositeRecordProcessor {

        /**
         * Task to periodically update the fields as events are processed.
         */
        class RunTimerTask extends TimerTask {

            @Override
            public void run() {

                final double tickLengthSeconds = (System.currentTimeMillis() - EventDashboardUpdater.this.lastTickMillis)
                        / (double) MILLIS;
                final int elapsedTime = (int) ((System.currentTimeMillis() - EventDashboardUpdater.this.jobStartMillis) / (double) MILLIS);
                final double megaBytesReceived = EventDashboardUpdater.this.bytesReceived / 1000000;
                EventDashboardUpdater.this.totalEvents += EventDashboardUpdater.this.eventsReceived;

                // Print to System.out if debugging the processor (by default this is off).
                if (DEBUG) {
                    System.out.println("tickLengthSeconds = " + tickLengthSeconds);
                    System.out.println("elapsedTime = " + elapsedTime);
                    System.out.println("eventsReceived = " + EventDashboardUpdater.this.eventsReceived);
                    System.out.println("dataRate = " + megaBytesReceived / tickLengthSeconds);
                    System.out.println("eventNumber = " + EventDashboardUpdater.this.eventNumber);
                    System.out.println("eventRate = " + EventDashboardUpdater.this.eventsReceived / tickLengthSeconds);
                    System.out.println("totalEvents = " + EventDashboardUpdater.this.totalEvents);
                    System.out.println("megaBytesReceived = " + megaBytesReceived);

                }

                EventDashboard.this.runModel.setElapsedTime(elapsedTime);
                EventDashboard.this.runModel.setEventsReceived(EventDashboardUpdater.this.totalEvents);
                EventDashboard.this.runModel.setDataRate(megaBytesReceived / tickLengthSeconds);
                EventDashboard.this.runModel.addDataReceived(megaBytesReceived);
                EventDashboard.this.runModel.setEventNumber(EventDashboardUpdater.this.eventNumber);
                EventDashboard.this.runModel
                        .setEventRate(EventDashboardUpdater.this.eventsReceived / tickLengthSeconds);

                EventDashboardUpdater.this.eventsReceived = 0;
                EventDashboardUpdater.this.bytesReceived = 0;
                EventDashboardUpdater.this.eventNumber = 0;

                EventDashboardUpdater.this.lastTickMillis = System.currentTimeMillis();

                // System.out.println();
            }
        }

        /**
         * Set to <code>true</code> to enable debugging.
         */
        private static final boolean DEBUG = false;

        /**
         * Helper for second to milliseconds conversion.
         */
        private static final long MILLIS = 1000;

        /**
         * Helper for second to nanoseconds conversion.
         */
        private static final int NANOS = 1000000000;

        /**
         * The number of bytes received.
         */
        private double bytesReceived;

        /**
         * The current event number.
         */
        private int eventNumber;

        /**
         * The number of events received.
         */
        private int eventsReceived;

        /**
         * The system time in milliseconds when the job started.
         */
        private long jobStartMillis;

        /**
         * The system time in milliseconds when the last timer tick occurred.
         */
        private long lastTickMillis = 0;

        /**
         * The current run number.
         */
        private int runNumber = -1;

        /**
         * The timer for running the update task.
         */
        private Timer timer;

        /**
         * The total number of events received.
         */
        private int totalEvents;

        /**
         * Check for head bank and update the run info if necessary.
         *
         * @param evioEvent the EVIO event
         */
        private void checkHeadBank(final EvioEvent evioEvent) {
            final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
            if (headBank != null) {
                final int headBankRun = headBank.getIntData()[1];
                if (headBankRun != this.runNumber) {
                    this.runNumber = headBankRun;
                    EventDashboard.this.runModel.setRunNumber(headBankRun);
                    EventDashboard.this.runModel.setStartDate(new Date(headBank.getIntData()[3] * MILLIS));
                }
            }
        }

        /**
         * Perform end of job hook, which will cancel the update timer.
         */
        @Override
        public void endJob() {
            this.timer.cancel();

            // Push final values into GUI.
            this.timer = new Timer("RunModelUpdaterEndJobTimer");
            this.timer.schedule(new RunTimerTask(), 0);
        }

        /**
         * Handle an EVIO END event.
         *
         * @param evioEvent the EVIO END event
         */
        private void endRun(final EvioEvent evioEvent) {
            // Get end run data.
            final int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                final int seconds = data[0];
                final int eventCount = data[2];
                final long endMillis = (long) seconds * 1000;

                // Update the GUI.
                EventDashboard.this.runModel.setEndDate(new Date(endMillis));
                EventDashboard.this.runModel.computeRunLength();
                EventDashboard.this.runModel.setTotalEvents(eventCount);
            }
        }

        /**
         * Process a {@link org.hps.record.composite.CompositeRecord} to extract information from available event
         * sources and update the running values.
         */
        @Override
        public void process(final CompositeRecord event) {
            // FIXME: CompositeRecord number is always -1 here.
            if (event.getEvioEvent() != null) {
                final EvioEvent evioEvent = event.getEvioEvent();
                this.bytesReceived += evioEvent.getTotalBytes();
                if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
                    // Get run start info from pre start event.
                    startRun(evioEvent);
                } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
                    // Get end run info from end event.
                    endRun(evioEvent);
                } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    // Check for run info in head bank.
                    checkHeadBank(evioEvent);
                    this.eventNumber = evioEvent.getEventNumber();
                    this.eventsReceived += 1;
                }
            } else if (event.getEtEvent() != null) {
                this.bytesReceived += event.getEtEvent().getData().length;
                this.eventNumber = event.getEtEvent().getId();
                this.eventsReceived += 1;
            } else if (event.getLcioEvent() != null) {
                final EventHeader lcioEvent = event.getLcioEvent();
                this.eventNumber = lcioEvent.getEventNumber();
                if (lcioEvent.getRunNumber() != this.runNumber) {
                    this.runNumber = lcioEvent.getRunNumber();
                    startRun(lcioEvent);
                }
                this.eventsReceived += 1;
            }
        }

        /**
         * Perform start of job hook which initializes this processor.
         */
        @Override
        public void startJob() {
            EventDashboard.this.runModel.reset();
            this.jobStartMillis = System.currentTimeMillis();

            // Start the timer to update GUI components about once per second.
            this.timer = new Timer("RunModelUpdaterTimer");
            this.lastTickMillis = System.currentTimeMillis();
            this.timer.scheduleAtFixedRate(new RunTimerTask(), 0, MILLIS);
        }

        /**
         * Handle start of run using an LCIO event.
         *
         * @param lcioEvent the LCIO event
         */
        private void startRun(final EventHeader lcioEvent) {
            EventDashboard.this.runModel.setRunNumber(lcioEvent.getRunNumber());
            final long seconds = lcioEvent.getTimeStamp() / NANOS;
            EventDashboard.this.runModel.setStartDate(new Date((int) seconds));
        }

        /**
         * Handle start of run using an EVIO START event.
         *
         * @param evioEvent the EVIO START event
         */
        private void startRun(final EvioEvent evioEvent) {
            // Get start of run data.
            final int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                final int seconds = data[0];
                this.runNumber = data[1];

                // Update the GUI.
                EventDashboard.this.runModel.setRunNumber(this.runNumber);
                EventDashboard.this.runModel.setStartDate(new Date(seconds * MILLIS));
            }
        }
    }

    /**
     * The decimal format (shows decimal numbers to 4 places).
     */
    static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#0.0000");

    /**
     * Field for showing the data rate in MB per second.
     */
    FieldPanel dataRateField = new FieldPanel("Data Rate [MB/s]", "", 12, false);

    /**
     * Field for showing the total data received in MB.
     */
    FieldPanel dataReceivedField = new FieldPanel("Data Received [MB]", "", 14, false);

    /**
     * Field for showing the elapsed job time in seconds.
     */
    FieldPanel elapsedTimeField = new FieldPanel("Elapsed Time [sec]", "", 14, false);

    /**
     * Field for showing the end date.
     */
    DatePanel endDateField = new DatePanel("Run End", "", 14, false);

    /**
     * Field for showing the current event number.
     */
    FieldPanel eventNumberField = new FieldPanel("Event Number", "", 14, false);

    /**
     * Field showing the event rate in Hertz.
     */
    FieldPanel eventRateField = new FieldPanel("Event Rate [Hz]", "", 14, false);

    /**
     * Field for showing the total number of events received.
     */
    FieldPanel eventsReceivedField = new FieldPanel("Events Received", "", 14, false);

    /**
     * Field for showing the length of the run in seconds.
     */
    FieldPanel lengthField = new FieldPanel("Run Length [sec]", "", 12, false);

    /**
     * The backing model with run and event information.
     */
    RunModel runModel;

    /**
     * Field for showing the run number.
     */
    FieldPanel runNumberField = new FieldPanel("Run Number", "", 10, false);

    /**
     * Field for showing the start date.
     */
    DatePanel startDateField = new DatePanel("Run Start", "", 14, false);

    /**
     * Field for showing the total events in the run.
     */
    FieldPanel totalEventsField = new FieldPanel("Total Events in Run", "", 14, false);

    /**
     * Class constructor which will build the GUI components.
     */
    public EventDashboard() {
        build();
    }

    /**
     * Class constructor which takes reference to backing model.
     *
     * @param runModel the backing {@link org.hps.monitoring.application.model.RunModel} with event and run information
     */
    public EventDashboard(final RunModel runModel) {
        this.runModel = runModel;
        this.runModel.addPropertyChangeListener(this);
        build();
    }

    /**
     * Build the GUI components.
     */
    private void build() {

        setLayout(new FlowLayout(FlowLayout.LEADING));

        add(this.runNumberField);
        add(this.startDateField);
        add(this.endDateField);
        add(this.lengthField);
        add(this.totalEventsField);

        add(this.elapsedTimeField);
        add(this.eventsReceivedField);
        add(this.dataReceivedField);
        add(this.eventNumberField);
        add(this.dataRateField);
        add(this.eventRateField);
    }

    /**
     * Update the GUI from changes to the backing {@link org.hps.monitoring.application.model.RunModel} object.
     *
     * @param evt the {@link java.beans.PropertyChangeEvent} to handle
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // System.out.println("RunPanel.propertyChange - " + evt.getPropertyName());
        final Object value = evt.getNewValue();
        if (RunModel.RUN_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            this.runNumberField.setValue((Integer) value);
        } else if (RunModel.START_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null) {
                this.startDateField.setValue((Date) value);
            } else {
                this.startDateField.setValue("");
            }
        } else if (RunModel.END_DATE_PROPERTY.equals(evt.getPropertyName())) {
            if (value != null) {
                this.endDateField.setValue((Date) value);
            } else {
                this.endDateField.setValue("");
            }
        } else if (RunModel.RUN_LENGTH_PROPERTY.equals(evt.getPropertyName())) {
            this.lengthField.setValue((Integer) value);
        } else if (RunModel.TOTAL_EVENTS_PROPERTY.equals(evt.getPropertyName())) {
            this.totalEventsField.setValue((Integer) value);
        } else if (RunModel.EVENTS_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            this.eventsReceivedField.setValue((Integer) value);
        } else if (RunModel.ELAPSED_TIME_PROPERTY.equals(evt.getPropertyName())) {
            this.elapsedTimeField.setValue((Integer) value);
        } else if (RunModel.DATA_RECEIVED_PROPERTY.equals(evt.getPropertyName())) {
            this.dataReceivedField.setValue(DECIMAL_FORMAT.format(value));
        } else if (RunModel.EVENT_NUMBER_PROPERTY.equals(evt.getPropertyName())) {
            this.eventNumberField.setValue((Integer) value);
        } else if (RunModel.DATA_RATE_PROPERTY.equals(evt.getPropertyName())) {
            this.dataRateField.setValue(DECIMAL_FORMAT.format(value));
        } else if (RunModel.EVENT_RATE_PROPERTY.equals(evt.getPropertyName())) {
            this.eventRateField.setValue(DECIMAL_FORMAT.format(value));
        }
    }

    /**
     * Set the backing {@link org.hps.monitoring.application.model.RunModel} of the component.
     *
     * @param runModel the backing {@link org.hps.monitoring.application.model.RunModel} of the component
     */
    public void setModel(final RunModel runModel) {
        this.runModel = runModel;
    }
}
