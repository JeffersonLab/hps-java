package org.hps.monitoring.application.model;

import java.util.Date;

/**
 * Backing model for run information that shows in the {@link org.hps.monitoring.application.EventDashboard}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class RunModel extends AbstractModel {

    /**
     * The data rate in megabytes per second.
     */
    public static final String DATA_RATE_PROPERTY = "DataRate";

    /**
     * The amount of data received in bytes since the last timer tick.
     */
    public static final String DATA_RECEIVED_PROPERTY = "DataReceived";

    /**
     * The total elapsed time in the session.
     */
    public static final String ELAPSED_TIME_PROPERTY = "ElapsedTime";

    /**
     * The end date of the run which comes from the EVIO END record.
     */
    public static final String END_DATE_PROPERTY = "EndDate";

    /**
     * The event number currently being processed which usually comes from the EVIO or LCIO records.
     */
    public static final String EVENT_NUMBER_PROPERTY = "EventNumber";

    /**
     * The event rate in Hertz.
     */
    public static final String EVENT_RATE_PROPERTY = "EventRate";

    /**
     * The total number of events received in the session.
     */
    public static final String EVENTS_RECEIVED_PROPERTY = "EventsReceived";

    /**
     * The total length of the run which is set from the EVIO END record.
     */
    public static final String RUN_LENGTH_PROPERTY = "RunLength";

    /**
     * The run number which comes from the EVIO control data.
     */
    public static final String RUN_NUMBER_PROPERTY = "RunNumber";

    /**
     * The properties of this model.
     */
    private static final String[] RUN_PROPERTIES = AbstractModel.getPropertyNames(RunModel.class);

    /**
     * The start date of the run which comes from the EVIO PRESTART event.
     */
    public static final String START_DATE_PROPERTY = "StartDate";

    /**
     * The total events in the run which comes from the EVIO END event.
     */
    public static final String TOTAL_EVENTS_PROPERTY = "TotalEvents";

    /**
     * The data rate in MB/s.
     */
    private Double dataRate;

    /**
     * The data received in bytes.
     */
    private Double dataReceived;

    /**
     * The elapsed time in seconds.
     */
    private Integer elapsedTime;

    /**
     * The end date of the run.
     */
    private Date endDate;

    /**
     * The current event number.
     */
    private Integer eventNumber;

    /**
     * The event rate in Hertz.
     */
    private Double eventRate;

    /**
     * The number of events received.
     */
    private Integer eventsReceived;

    /**
     * The length of the run.
     */
    private Integer runLength;

    /**
     * The run number.
     */
    private Integer runNumber;

    /**
     * The run start date.
     */
    private Date startDate;

    /**
     * The total events received in the run.
     */
    private Integer totalEvents;

    /**
     * Add data received in bytes.
     *
     * @param addDataReceived the amount of data received in bytes
     */
    public void addDataReceived(final double addDataReceived) {
        this.setDataReceived(this.dataReceived + addDataReceived);
    }

    /**
     * Compute the run length from the start and end date and set its value in the GUI.
     */
    public void computeRunLength() {
        if (this.startDate != null && this.endDate != null) {
            final long elapsedMillis = this.endDate.getTime() - this.startDate.getTime();
            final int elapsedSeconds = (int) (elapsedMillis / 1000.);
            this.setRunLength(elapsedSeconds);
        }
    }

    /**
     * Get the data rate in MB/s.
     *
     * @return the data rate in MB/s
     */
    public double getDataRate() {
        return this.dataRate;
    }

    /**
     * Get the data received in bytes.
     *
     * @return the data received in bytes
     */
    public double getDataReceived() {
        return this.dataReceived;
    }

    /**
     * Get the elapsed time in seconds.
     *
     * @return the elapsed time in seconds
     */
    public int getElapsedTime() {
        return this.elapsedTime;
    }

    /**
     * Get the run end date.
     *
     * @return the run end date
     */
    public Date getEndDate() {
        return this.endDate;
    }

    /**
     * Get the current event number.
     *
     * @return the current event number
     */
    public int getEventNumber() {
        return this.eventNumber;
    }

    /**
     * Get the event rate in Hertz.
     *
     * @return the event rate in Hertz
     */
    public double getEventRate() {
        return this.eventRate;
    }

    /**
     * Get the number of events received in the session.
     *
     * @return the number of events received
     */
    public int getEventsReceived() {
        return this.eventsReceived;
    }

    /**
     * Get the property names for this model.
     *
     * @return the property names for this model
     */
    @Override
    public String[] getPropertyNames() {
        return RUN_PROPERTIES;
    }

    /**
     * Get the run length in seconds.
     *
     * @return the run length in seconds
     */
    public int getRunLength() {
        return this.runLength;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    public int getRunNumber() {
        return this.runNumber;
    }

    /**
     * Get the start date.
     *
     * @return the start date
     */
    public Date getStartDate() {
        return this.startDate;
    }

    /**
     * Get the total events in the run.
     *
     * @return the total events in the run
     */
    public int getTotalEvents() {
        return this.totalEvents;
    }

    /**
     * Reset the model for new run.
     */
    public void reset() {
        setDataReceived(0);
        setElapsedTime(0);
        setEndDate(null);
        setEventsReceived(0);
        setRunLength(0);
        setRunNumber(0);
        setStartDate(null);
        setTotalEvents(0);
    }

    /**
     * Set the data rate in MB/s.
     *
     * @param dataRate the data rate in MB/s
     */
    public void setDataRate(final double dataRate) {
        final Double oldValue = this.dataRate;
        this.dataRate = dataRate;
        this.firePropertyChange(DATA_RATE_PROPERTY, oldValue, this.dataRate);
    }

    /**
     * Set the data received in bytes.
     *
     * @param dataReceived the data received in bytes
     */
    public void setDataReceived(final double dataReceived) {
        final Double oldValue = this.dataReceived;
        this.dataReceived = dataReceived;
        this.firePropertyChange(DATA_RECEIVED_PROPERTY, oldValue, this.dataReceived);
    }

    /**
     * Set the elapsed time in seconds.
     *
     * @param elapsedTime the elapsed time in seconds
     */
    public void setElapsedTime(final int elapsedTime) {
        final Integer oldValue = this.elapsedTime;
        this.elapsedTime = elapsedTime;
        this.firePropertyChange(ELAPSED_TIME_PROPERTY, oldValue, this.elapsedTime);
    }

    /**
     * Set the run end date.
     *
     * @param endDate the run end date
     */
    public void setEndDate(final Date endDate) {
        final Date oldValue = this.endDate;
        this.endDate = endDate;
        this.firePropertyChange(END_DATE_PROPERTY, oldValue, this.endDate);
    }

    /**
     * Set the current event number.
     *
     * @param eventNumber the current event number
     */
    public void setEventNumber(final int eventNumber) {
        final Integer oldValue = this.eventNumber;
        this.eventNumber = eventNumber;
        this.firePropertyChange(EVENT_NUMBER_PROPERTY, oldValue, this.eventNumber);
    }

    /**
     * Set the event rate in Hertz.
     *
     * @param eventRate the event rate in Hertz
     */
    public void setEventRate(final double eventRate) {
        final Double oldValue = this.eventRate;
        this.eventRate = eventRate;
        this.firePropertyChange(EVENT_RATE_PROPERTY, oldValue, this.eventRate);
    }

    /**
     * Set the number of events received.
     *
     * @param eventsReceived the number of events received
     */
    public void setEventsReceived(final int eventsReceived) {
        final Integer oldValue = this.eventsReceived;
        this.eventsReceived = eventsReceived;
        this.firePropertyChange(EVENTS_RECEIVED_PROPERTY, oldValue, this.eventsReceived);
    }

    /**
     * Set the length of the run in seconds.
     *
     * @param runLength the length of the run in seconds
     */
    public void setRunLength(final int runLength) {
        final Integer oldValue = this.runLength;
        this.runLength = runLength;
        this.firePropertyChange(RUN_LENGTH_PROPERTY, oldValue, this.runLength);
    }

    /**
     * Set the run number.
     *
     * @param runNumber the run number
     */
    public void setRunNumber(final int runNumber) {
        final Integer oldValue = this.runNumber;
        this.runNumber = runNumber;
        this.firePropertyChange(RUN_NUMBER_PROPERTY, oldValue, this.runNumber);
    }

    /**
     * Set the start date.
     *
     * @param startDate the start date
     */
    public void setStartDate(final Date startDate) {
        final Date oldValue = this.startDate;
        this.startDate = startDate;
        this.firePropertyChange(START_DATE_PROPERTY, oldValue, this.startDate);
    }

    /**
     * Set the total number of events in the run.
     *
     * @param totalEvents the total number of events in the run
     */
    public void setTotalEvents(final int totalEvents) {
        final Integer oldValue = this.totalEvents;
        this.totalEvents = totalEvents;
        this.firePropertyChange(TOTAL_EVENTS_PROPERTY, oldValue, this.totalEvents);
    }
}
