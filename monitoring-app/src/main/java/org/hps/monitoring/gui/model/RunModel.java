package org.hps.monitoring.gui.model;

import java.util.Date;

/**
 * Backing model for run information that shows in the {@link org.hps.monitoring.gui.RunPanel}.
 */
public class RunModel extends AbstractModel {
    
    public final static String RUN_NUMBER_PROPERTY = "RunNumber"; 
    public final static String START_DATE_PROPERTY = "StartDate";
    public final static String END_DATE_PROPERTY = "EndDate";
    public final static String RUN_LENGTH_PROPERTY = "RunLength"; // set at end, in seconds
    public final static String TOTAL_EVENTS_PROPERTY = "TotalEvents"; // only set at end
    public final static String EVENTS_RECEIVED_PROPERTY = "EventsReceived"; // events received so far
    public final static String ELAPSED_TIME_PROPERTY = "ElapsedTime"; // updated on the fly, in seconds
    public final static String DATA_RECEIVED_PROPERTY = "DataReceived"; // updated on the fly, in bytes
    public final static String EVENT_NUMBER_PROPERTY = "EventNumber"; // current event number

    static final String[] properties = new String[] {
        RUN_NUMBER_PROPERTY,
        START_DATE_PROPERTY,
        END_DATE_PROPERTY,
        RUN_LENGTH_PROPERTY,
        TOTAL_EVENTS_PROPERTY,
        ELAPSED_TIME_PROPERTY,
        DATA_RECEIVED_PROPERTY,
        EVENT_NUMBER_PROPERTY
    };
    
    int runNumber;
    Date startDate;
    Date endDate;
    int runLength;
    int totalEvents;
    int eventsReceived;
    int elapsedTime;
    long dataReceived;
    int eventNumber;
          
    public String[] getPropertyNames() {
        return properties;
    }
    
    public int getRunNumber() {
        return runNumber;
    }
    
    public void setRunNumber(int runNumber) {
        int oldValue = this.runNumber;
        this.runNumber = runNumber;
        this.firePropertyChange(RUN_NUMBER_PROPERTY, oldValue, this.runNumber);
    }
    
    public Date getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Date startDate) {
        Date oldValue = this.startDate;
        this.startDate = startDate;
        this.firePropertyChange(START_DATE_PROPERTY, oldValue, this.startDate);
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Date endDate) {
        Date oldValue = this.endDate;
        this.endDate = endDate;
        this.firePropertyChange(END_DATE_PROPERTY, oldValue, this.endDate);
    }
    
    public int getRunLength() {
        return runLength;
    }
    
    public void setRunLength(int runLength) {
        int oldValue = this.runLength;
        this.runLength = runLength;
        this.firePropertyChange(RUN_LENGTH_PROPERTY, oldValue, this.runLength);
    }
    
    public void computeRunLength() {
        if (startDate != null && endDate != null) {
            long elapsedMillis = endDate.getTime() - startDate.getTime();
            int elapsedSeconds = (int)((double)elapsedMillis / 1000.);
            this.setRunLength(elapsedSeconds);
        }
    }
    
    public int getTotalEvents() {
        return totalEvents;
    }
    
    public void setTotalEvents(int totalEvents) {
        int oldValue = this.totalEvents;
        this.totalEvents = totalEvents;
        this.firePropertyChange(TOTAL_EVENTS_PROPERTY, oldValue, this.totalEvents);
    }
    
    public int getEventsReceived() {
        return eventsReceived;
    }
    
    public void setEventsReceived(int eventsReceived) {
        int oldValue = this.eventsReceived;
        this.eventsReceived = eventsReceived;
        this.firePropertyChange(EVENTS_RECEIVED_PROPERTY, oldValue, this.eventsReceived);
    }
    
    public void incrementEventsReceived() {
        this.setEventsReceived(eventsReceived + 1);
    }
    
    public int getElapsedTime() {
        return elapsedTime;
    }
    
    public void setElapsedTime(int elapsedTime) {
        int oldValue = this.elapsedTime;
        this.elapsedTime = elapsedTime;
        this.firePropertyChange(ELAPSED_TIME_PROPERTY, oldValue, this.elapsedTime);
    }
           
    public long getDataReceived() {
        return dataReceived;
    }
    
    public void setDataReceived(long dataReceived) {
        long oldValue = this.dataReceived;
        this.dataReceived = dataReceived;
        this.firePropertyChange(DATA_RECEIVED_PROPERTY, oldValue, this.dataReceived);
    }
    
    public void addDataReceived(long addDataReceived) {
        this.setDataReceived(dataReceived + addDataReceived);
    }
    
    public void setEventNumber(int eventNumber) {
        int oldValue = this.eventNumber;
        this.eventNumber = eventNumber;
        this.firePropertyChange(EVENT_NUMBER_PROPERTY, oldValue, this.eventNumber);
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
