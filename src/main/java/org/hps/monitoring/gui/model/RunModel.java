package org.hps.monitoring.gui.model;

import java.util.Date;

/**
 * Backing model for run information that shows in the {@link org.hps.monitoring.gui.RunPanel}.
 */
public final class RunModel extends AbstractModel {

    public final static String RUN_NUMBER_PROPERTY = "RunNumber";
    public final static String START_DATE_PROPERTY = "StartDate";
    public final static String END_DATE_PROPERTY = "EndDate";
    public final static String RUN_LENGTH_PROPERTY = "RunLength"; // set at end, in seconds
    public final static String TOTAL_EVENTS_PROPERTY = "TotalEvents"; // only set at end
    public final static String EVENTS_RECEIVED_PROPERTY = "EventsReceived"; // events received so far
    public final static String ELAPSED_TIME_PROPERTY = "ElapsedTime"; // updated on the fly, in seconds
    public final static String DATA_RECEIVED_PROPERTY = "DataReceived"; // updated on the fly, in bytes
    public final static String EVENT_NUMBER_PROPERTY = "EventNumber"; // current event number
    public final static String DATA_RATE_PROPERTY = "DataRate"; // data rate in megabytes per second
    public final static String EVENT_RATE_PROPERTY = "EventRate"; // event rate per second

    static final String[] RUN_PROPERTIES = AbstractModel.getPropertyNames(RunModel.class);
    
    Integer runNumber;
    Date startDate;
    Date endDate;
    Integer runLength;
    Integer totalEvents;
    Integer eventsReceived;
    Integer elapsedTime;
    Double dataReceived;
    Integer eventNumber;
    Double dataRate;
    Double eventRate;

    public String[] getPropertyNames() {
        return RUN_PROPERTIES;
    }

    public void setRunNumber(int runNumber) {
        Integer oldValue = this.runNumber;
        this.runNumber = runNumber;
        this.firePropertyChange(RUN_NUMBER_PROPERTY, oldValue, this.runNumber);
    }

    public void setStartDate(Date startDate) {
        Date oldValue = this.startDate;
        this.startDate = startDate;
        this.firePropertyChange(START_DATE_PROPERTY, oldValue, this.startDate);
    }

    public void setEndDate(Date endDate) {
        Date oldValue = this.endDate;
        this.endDate = endDate;
        this.firePropertyChange(END_DATE_PROPERTY, oldValue, this.endDate);
    }

    public void setRunLength(int runLength) {
        Integer oldValue = this.runLength;
        this.runLength = runLength;
        this.firePropertyChange(RUN_LENGTH_PROPERTY, oldValue, this.runLength);
    }

    public void computeRunLength() {
        if (startDate != null && endDate != null) {
            long elapsedMillis = endDate.getTime() - startDate.getTime();
            int elapsedSeconds = (int) ((double) elapsedMillis / 1000.);
            this.setRunLength(elapsedSeconds);
        }
    }

    public void setTotalEvents(int totalEvents) {
        Integer oldValue = this.totalEvents;
        this.totalEvents = totalEvents;
        this.firePropertyChange(TOTAL_EVENTS_PROPERTY, oldValue, this.totalEvents);
    }

    public void setEventsReceived(int eventsReceived) {
        Integer oldValue = this.eventsReceived;
        this.eventsReceived = eventsReceived;
        this.firePropertyChange(EVENTS_RECEIVED_PROPERTY, oldValue, this.eventsReceived);
    }

    public void setElapsedTime(int elapsedTime) {
        Integer oldValue = this.elapsedTime;
        this.elapsedTime = elapsedTime;
        this.firePropertyChange(ELAPSED_TIME_PROPERTY, oldValue, this.elapsedTime);
    }

    public void setDataReceived(double dataReceived) {
        Double oldValue = this.dataReceived;
        this.dataReceived = dataReceived;
        this.firePropertyChange(DATA_RECEIVED_PROPERTY, oldValue, this.dataReceived);
    }

    public void addDataReceived(double addDataReceived) {
        this.setDataReceived(dataReceived + addDataReceived);
    }

    public void setEventNumber(int eventNumber) {
        Integer oldValue = this.eventNumber;
        this.eventNumber = eventNumber;
        this.firePropertyChange(EVENT_NUMBER_PROPERTY, oldValue, this.eventNumber);
    }
    
    public void setDataRate(double dataRate) {
        Double oldValue = this.dataRate;
        this.dataRate = dataRate;
        this.firePropertyChange(DATA_RATE_PROPERTY, oldValue, this.dataRate);
    }
        
    public void setEventRate(double eventRate) {
        Double oldValue = this.eventRate;
        this.eventRate = eventRate;
        this.firePropertyChange(EVENT_RATE_PROPERTY, oldValue, this.eventRate);
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
}