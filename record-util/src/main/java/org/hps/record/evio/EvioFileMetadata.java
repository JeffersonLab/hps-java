package org.hps.record.evio;

import java.io.File;
import java.util.Date;

/**
 * Meta data that can be extracted from EVIO files.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EvioFileMetadata {

    /**
     * The number of bad events in the file that are unreadable.
     */
    private int badEventCount;

    /**
     * The total number of bytes in the file.
     */
    private long byteCount;

    /**
     * The end date of the file which will be taken from the <i>END</i> event or the last physics event.
     */
    private Date endDate;

    /**
     * The last event number in the file.
     */
    private int endEvent;

    /**
     * The number of events in the file.
     */
    private int eventCount;

    /**
     * The EVIO file which is set at class construction time and cannot be changed.
     */
    private final File evioFile;

    /**
     * <code>true</code> if there is an <i>END</i> event in this file.
     */
    private boolean hasEnd;

    /**
     * <code>true</code> if there is a <i>PRESTART</i> event in this file.
     */
    private boolean hasPrestart;

    /**
     * The run number.
     */
    private int run;

    /**
     * The file sequence number.
     */
    private int sequence;

    /**
     * The start date which comes from the <i>PRESTART</i> event or the first physics event.
     */
    private Date startDate;

    /**
     * The first event number in the file.
     */
    private int startEvent;

    /**
     * Create a meta data object.
     *
     * @param evioFile the EVIO file to which the meta data applies
     */
    public EvioFileMetadata(final File evioFile) {        
        if (evioFile == null) {
            throw new IllegalArgumentException("The EVIO file argument is null.");
        }
        if (!evioFile.exists()) {
            throw new IllegalArgumentException("The file " + evioFile.getPath() + " does not exist.");
        }
        this.evioFile = evioFile;
        
        // Set sequence number.
        setSequence(EvioFileUtilities.getSequenceFromName(this.evioFile));
    }

    /**
     * Get the bad event count.
     *
     * @return the bad event count
     */
    public int getBadEventCount() {
        return badEventCount;
    }

    /**
     * Get the byte count.
     *
     * @return the byte count
     */
    public long getByteCount() {
        return byteCount;
    }

    /**
     * Get the end date.
     *
     * @return the end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Get the end event number.
     *
     * @return the end event number
     */
    public int getEndEvent() {
        return endEvent;
    }

    /**
     * Get the number of events in the file (all types).
     *
     * @return the number of events in the file
     */
    public int getEventCount() {
        return eventCount;
    }

    /**
     * Get the EVIO file.
     *
     * @return the EVIO file
     */
    public File getEvioFile() {
        return evioFile;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    public Integer getRun() {
        return run;
    }

    /**
     * Get the file sequence number, numbered from 0.
     *
     * @return the file sequence number
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Get the start date.
     *
     * @return the start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Get the first event number in the file.
     *
     * @return the first event number in the file
     */
    public int getStartEvent() {
        return startEvent;
    }

    /**
     * Return <code>true</code> if the file has an EVIO <i>END</i> event.
     *
     * @return <code>true</code> if the file has an EVIO <i>END</i> event
     */
    public boolean hasEnd() {
        return hasEnd;
    }

    /**
     * Return <code>true</code> if the file has an EVIO <i>PRESTART</i> event.
     *
     * @return <code>true</code> if the file has an EVIO <i>PRESTART</i> event
     */
    public boolean hasPrestart() {
        return hasPrestart;
    }

    /**
     * Set the bad event count.
     *
     * @param badEventCount the bad event count
     */
    void setBadEventCount(final int badEventCount) {
        if (badEventCount < 0) {
            throw new IllegalArgumentException("badEventCount");
        }
        this.badEventCount = badEventCount;
    }

    /**
     * Set the byte count.
     *
     * @param byteCount the byte count
     */
    void setByteCount(final long byteCount) {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount");
        }
        this.byteCount = byteCount;
    }

    /**
     * Set the end date.
     *
     * @param endDate the end date
     */
    void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Set the end event number.
     *
     * @param endEvent the end event number
     */
    void setEndEvent(final int endEvent) {
        this.endEvent = endEvent;
    }

    /**
     * Set the event count.
     *
     * @param eventCount the event count
     */
    void setEventCount(final int eventCount) {
        this.eventCount = eventCount;
    }

    /**
     * Set whether the file has an EVIO <i>END</i> event.
     *
     * @param hasEnd <code>true</code> if file has an EVIO <i>END</i> event
     */
    void setHasEnd(final boolean hasEnd) {
        this.hasEnd = hasEnd;
    }

    /**
     * Set whether the file has an EVIO <i>PRESTART</i> event.
     *
     * @param hasPrestart <code>true</code> if file has an EVIO <i>PRESTART</i> event
     */
    void setHasPrestart(final boolean hasPrestart) {
        this.hasPrestart = hasPrestart;
    }

    /**
     * Set the run number.
     *
     * @param run the run number
     */
    void setRun(final int run) {
        this.run = run;
    }

    /**
     * Set the sequence number
     *
     * @param sequence the sequence number
     */
    void setSequence(final int sequence) {
        this.sequence = sequence;
    }

    /**
     * Set the start date.
     *
     * @param startDate the start date
     */
    void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Set the start event number.
     *
     * @param startEvent the start event number
     */
    void setStartEvent(final int startEvent) {
        this.startEvent = startEvent;
    }

    /**
     * Convert this object to a human readable string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "EvioFileMetaData { evioFile: " + this.evioFile + ", startDate: " + this.startDate + ", endDate: "
                + this.endDate + ", badEventCount: " + this.badEventCount + ", byteCount: " + this.byteCount
                + ", eventCount: " + this.eventCount + ", hasPrestart: " + this.hasPrestart + ", hasEnd: "
                + this.hasEnd + ", run: " + this.run + ", fileNumber: " + sequence + ", startEvent:  "
                + this.startEvent + ", endEvent: " + endEvent + " }";
    }
}
