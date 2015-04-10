package org.hps.monitoring.application.model;

import java.util.Date;

/**
 * Backing model for run information that shows in the {@link org.hps.monitoring.application.EventDashboard}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class RunModel extends AbstractModel {

    public final static String DATA_RATE_PROPERTY = "DataRate"; // data rate in megabytes per second
    public final static String DATA_RECEIVED_PROPERTY = "DataReceived"; // updated on the fly, in bytes
    public final static String ELAPSED_TIME_PROPERTY = "ElapsedTime"; // updated on the fly, in seconds
    public final static String END_DATE_PROPERTY = "EndDate";
    public final static String EVENT_NUMBER_PROPERTY = "EventNumber"; // current event number
    public final static String EVENT_RATE_PROPERTY = "EventRate"; // event rate per second
    public final static String EVENTS_RECEIVED_PROPERTY = "EventsReceived"; // events received so far
    public final static String RUN_LENGTH_PROPERTY = "RunLength"; // set at end, in seconds
    public final static String RUN_NUMBER_PROPERTY = "RunNumber";
    static final String[] RUN_PROPERTIES = AbstractModel.getPropertyNames(RunModel.class);
    public final static String START_DATE_PROPERTY = "StartDate";

    public final static String TOTAL_EVENTS_PROPERTY = "TotalEvents"; // only set at end

    Double dataRate;
    Double dataReceived;
    Integer elapsedTime;
    Date endDate;
    Integer eventNumber;
    Double eventRate;
    Integer eventsReceived;
    Integer runLength;
    Integer runNumber;
    Date startDate;
    Integer totalEvents;

    public void addDataReceived(final double addDataReceived) {
        this.setDataReceived(this.dataReceived + addDataReceived);
    }

    public void computeRunLength() {
        if (this.startDate != null && this.endDate != null) {
            final long elapsedMillis = this.endDate.getTime() - this.startDate.getTime();
            final int elapsedSeconds = (int) (elapsedMillis / 1000.);
            this.setRunLength(elapsedSeconds);
        }
    }

    public double getDataRate() {
        return this.dataRate;
    }

    public double getDataReceived() {
        return this.dataReceived;
    }

    public int getElapsedTime() {
        return this.elapsedTime;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public int getEventNumber() {
        return this.eventNumber;
    }

    public double getEventRate() {
        return this.eventRate;
    }

    public int getEventsReceived() {
        return this.eventsReceived;
    }

    @Override
    public String[] getPropertyNames() {
        return RUN_PROPERTIES;
    }

    public int getRunLength() {
        return this.runLength;
    }

    public int getRunNumber() {
        return this.runNumber;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public int getTotalEvents() {
        return this.totalEvents;
    }

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

    public void setDataRate(final double dataRate) {
        final Double oldValue = this.dataRate;
        this.dataRate = dataRate;
        this.firePropertyChange(DATA_RATE_PROPERTY, oldValue, this.dataRate);
    }

    public void setDataReceived(final double dataReceived) {
        final Double oldValue = this.dataReceived;
        this.dataReceived = dataReceived;
        this.firePropertyChange(DATA_RECEIVED_PROPERTY, oldValue, this.dataReceived);
    }

    public void setElapsedTime(final int elapsedTime) {
        final Integer oldValue = this.elapsedTime;
        this.elapsedTime = elapsedTime;
        this.firePropertyChange(ELAPSED_TIME_PROPERTY, oldValue, this.elapsedTime);
    }

    public void setEndDate(final Date endDate) {
        final Date oldValue = this.endDate;
        this.endDate = endDate;
        this.firePropertyChange(END_DATE_PROPERTY, oldValue, this.endDate);
    }

    public void setEventNumber(final int eventNumber) {
        final Integer oldValue = this.eventNumber;
        this.eventNumber = eventNumber;
        this.firePropertyChange(EVENT_NUMBER_PROPERTY, oldValue, this.eventNumber);
    }

    public void setEventRate(final double eventRate) {
        final Double oldValue = this.eventRate;
        this.eventRate = eventRate;
        this.firePropertyChange(EVENT_RATE_PROPERTY, oldValue, this.eventRate);
    }

    public void setEventsReceived(final int eventsReceived) {
        final Integer oldValue = this.eventsReceived;
        this.eventsReceived = eventsReceived;
        this.firePropertyChange(EVENTS_RECEIVED_PROPERTY, oldValue, this.eventsReceived);
    }

    public void setRunLength(final int runLength) {
        final Integer oldValue = this.runLength;
        this.runLength = runLength;
        this.firePropertyChange(RUN_LENGTH_PROPERTY, oldValue, this.runLength);
    }

    public void setRunNumber(final int runNumber) {
        final Integer oldValue = this.runNumber;
        this.runNumber = runNumber;
        this.firePropertyChange(RUN_NUMBER_PROPERTY, oldValue, this.runNumber);
    }

    public void setStartDate(final Date startDate) {
        final Date oldValue = this.startDate;
        this.startDate = startDate;
        this.firePropertyChange(START_DATE_PROPERTY, oldValue, this.startDate);
    }

    public void setTotalEvents(final int totalEvents) {
        final Integer oldValue = this.totalEvents;
        this.totalEvents = totalEvents;
        this.firePropertyChange(TOTAL_EVENTS_PROPERTY, oldValue, this.totalEvents);
    }
}