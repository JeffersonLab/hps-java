package org.hps.record.run;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

import org.hps.record.epics.EpicsData;
import org.hps.record.evio.crawler.EvioFileList;
import org.hps.record.scalers.ScalerData;

/**
 * This class models the run summary information which is persisted as a row in the <i>run_log</i> table of the run
 * database.
 * <p>
 * This information includes:
 * <ul>
 * <li>run number</li>
 * <li>start time (UTC)</li>
 * <li>end time (UTC)</li>
 * <li>total number of events in the run</li>
 * <li>number of EVIO files in the run</li>
 * <li>whether the END event was found indicating that the DAQ did not crash</li>
 * <li>whether the run is considered good (all <code>true</code> for now)</li>
 * <li>list of EVIO files in the run</li>
 * </ul>
 * There are also associated {@link org.hps.record.epics.EpicsData} and {@link org.hps.record.scalers.ScalerData}
 * objects representing the EPICS and scaler data summaries for the run. The EPICS data is averaged over the whole run
 * while the scalers are from the last scaler data bank that was found.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunSummary {

    /**
     * Default date display format.
     */
    private static final DateFormat DATE_DISPLAY = new SimpleDateFormat();

    static {
        /**
         * Set default time zone for display to East Coast (JLAB) where data was taken.
         */
        DATE_DISPLAY.setCalendar(new GregorianCalendar(TimeZone.getTimeZone("America/New_York")));
    }

    private Date created;

    /**
     * This is <code>true</code> if the END event is found in the data.
     */
    private boolean endOkay;

    /**
     * The run end time in UTC (milliseconds).
     */
    private long endTimeUtc;

    /**
     * The combined EPICS information for the run (uses the mean values for each variable).
     */
    private EpicsData epics;

    /**
     * The counts of different types of events that were found.
     */
    private Map<Object, Integer> eventTypeCounts;

    /**
     * The list of EVIO files in the run.
     */
    private EvioFileList evioFileList = new EvioFileList();

    /**
     * The run number.
     */
    private final int run;

    /**
     * Flag to indicate run was okay.
     */
    private boolean runOkay = true;

    /**
     * The scaler data from the last physics event in the run.
     */
    private ScalerData scalerData;

    /**
     * The run start time in UTC (milliseconds).
     */
    private long startTimeUtc;

    /**
     * The total events found in the run across all files.
     */
    private int totalEvents = -1;

    /**
     * The total number of files in the run.
     */
    private int totalFiles = 0;

    /**
     * Date when the run record was last updated.
     */
    private Date updated;

    /**
     * Create a run summary.
     *
     * @param run the run number
     */
    public RunSummary(final int run) {
        this.run = run;
    }

    /**
     * Add an EVIO file from this run to the list.
     *
     * @param file the file to add
     */
    public void addFile(final File file) {
        this.evioFileList.add(file);
    }

    /**
     * Get the creation date of this run record.
     *
     * @return the creation date of this run record
     */
    public Date getCreated() {
        return this.created;
    }

    /**
     * Get the end date.
     *
     * @return the end date
     */
    public Date getEndDate() {
        return new Date(this.endTimeUtc);
    }

    /**
     * Return <code>true</code> if END event was found in the data.
     *
     * @return <code>true</code> if END event was in the data
     */
    public boolean getEndOkay() {
        return this.endOkay;
    }

    /**
     * Get the end time in UTC (milliseconds)
     *
     * @return the end time in milliseconds
     */
    public long getEndTimeUtc() {
        return this.endTimeUtc;
    }

    /**
     * Get the EPICS data summary.
     * <p>
     * This is computed by taking the mean of each variable for the run.
     *
     * @return the EPICS data summary
     */
    public EpicsData getEpicsData() {
        return this.epics;
    }

    /**
     * Get the event rate (effectively the trigger rate) which is the total events divided by the number of seconds in
     * the run.
     *
     * @return the event rate
     */
    public double getEventRate() {
        if (this.getTotalEvents() <= 0) {
            throw new RuntimeException("Total events is zero or invalid.");
        }
        return (double) this.getTotalEvents() / (double) this.getTotalSeconds();
    }

    /**
     * Get the counts of different event types.
     *
     * @return the counts of different event types
     */
    public Map<Object, Integer> getEventTypeCounts() {
        return this.eventTypeCounts;
    }

    /**
     * Get the list of EVIO files in this run.
     *
     * @return the list of EVIO files in this run
     */
    public EvioFileList getEvioFileList() {
        return this.evioFileList;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    public int getRun() {
        return this.run;
    }

    /**
     * Return <code>true</code> if the run was okay (no major errors or data corruption occurred).
     *
     * @return <code>true</code> if the run was okay
     */
    public boolean getRunOkay() {
        return this.runOkay;
    }

    /**
     * Get the scaler data of this run (last event only).
     *
     * @return the scaler data of this run from the last event
     */
    public ScalerData getScalerData() {
        return this.scalerData;
    }

    /**
     * Get the start date.
     *
     * @return the start date
     */
    public Date getStartDate() {
        return new Date(this.startTimeUtc);
    }

    /**
     * Get the run start time in UTC (milliseconds).
     *
     * @return the start time in milliseconds
     */
    public long getStartTimeUtc() {
        return this.startTimeUtc;
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
     * Get the total number of files for this run.
     *
     * @return the total number of files for this run
     */
    public int getTotalFiles() {
        return this.totalFiles;
    }

    /**
     * Get the number of seconds in the run which is the difference between the start and end times.
     *
     * @return the total seconds in the run
     */
    public long getTotalSeconds() {
        return (this.endTimeUtc - this.startTimeUtc) / 1000;
    }

    /**
     * Get the date when this run record was last updated.
     *
     * @return the date when this run record was last updated
     */
    public Date getUpdated() {
        return updated;
    }

    /**
     * Print the run summary.
     *
     * @param ps the print stream for output
     */
    public void printOut(final PrintStream ps) {
        ps.println("--------------------------------------------");
        ps.println("run: " + this.run);
        ps.println("first file: " + this.evioFileList.first());
        ps.println("last file: " + this.evioFileList.last());
        ps.println("started: " + DATE_DISPLAY.format(this.getStartDate()));
        ps.println("ended: " + DATE_DISPLAY.format(this.getEndDate()));
        ps.println("total events: " + this.getTotalEvents());
        ps.println("end OK: " + this.getEndOkay());
        ps.println("event rate: " + this.getEventRate());
        ps.println("event types");
        for (final Object key : this.eventTypeCounts.keySet()) {
            ps.println("  " + key + ": " + this.eventTypeCounts.get(key));
        }
        ps.println(this.evioFileList.size() + " files");
        for (final File file : this.evioFileList) {
            ps.println("  " + file.getPath());
        }
    }

    /**
     * Set the creation date of the run record.
     *
     * @param created the creation date of the run record
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * Set if end is okay.
     *
     * @param endOkay <code>true</code> if end is okay
     */
    public void setEndOkay(final boolean endOkay) {
        this.endOkay = endOkay;
    }

    /**
     * Set the end date.
     *
     * @param endDate the end date
     */
    public void setEndTimeUtc(final long endTimeUtc) {
        this.endTimeUtc = endTimeUtc;
    }

    /**
     * Set the EPICS data for the run.
     *
     * @param epics the EPICS data for the run
     */
    public void setEpicsData(final EpicsData epics) {
        this.epics = epics;
    }

    /**
     * Set the event type counts for the run.
     *
     * @param eventTypeCounts the event type counts for the run
     */
    public void setEventTypeCounts(final Map<Object, Integer> eventTypeCounts) {
        this.eventTypeCounts = eventTypeCounts;
    }

    /**
     * Set the list of EVIO files for the run.
     *
     * @param evioFileList the list of EVIO files for the run
     */
    public void setEvioFileList(final EvioFileList evioFileList) {
        this.evioFileList = evioFileList;
    }

    /**
     * Set whether the run was "okay" meaning the data is usable for physics analysis.
     *
     * @param runOkay <code>true</code> if the run is okay
     */
    public void setRunOkay(final boolean runOkay) {
        this.runOkay = runOkay;
    }

    /**
     * Set the scaler data of the run.
     *
     * @param scalerData the scaler data
     */
    public void setScalerData(final ScalerData scalerData) {
        this.scalerData = scalerData;
    }

    /**
     * Set the start date of the run.
     *
     * @param startDate the start date of the run
     */
    public void setStartTimeUtc(final long startTimeUtc) {
        this.startTimeUtc = startTimeUtc;
    }

    /**
     * Set the total number of physics events in the run.
     *
     * @param totalEvents the total number of physics events in the run
     */
    public void setTotalEvents(final int totalEvents) {
        this.totalEvents = totalEvents;
    }

    /**
     * Set the total number of EVIO files in the run.
     *
     * @param totalFiles the total number of EVIO files in the run
     */
    public void setTotalFiles(final int totalFiles) {
        this.totalFiles = totalFiles;
    }

    /**
     * Set the date when this run record was last updated.
     *
     * @param updated the date when the run record was last updated
     */
    public void setUpdated(final Date updated) {
        this.updated = updated;
    }

    /**
     * Sort the files in the run by sequence number in place.
     */
    public void sortFiles() {
        this.evioFileList.sort();
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "RunSummary { run: " + this.getRun() + ", startTimeUtc: " + this.getStartTimeUtc() + ", endTimeUtc: "
                + this.getEndTimeUtc() + ", totalEvents: " + this.getTotalEvents() + ", totalFiles: "
                + this.getTotalFiles() + ", endOkay: " + this.getEndOkay() + ", runOkay: " + this.getRunOkay()
                + ", updated: " + this.getUpdated() + ", created: " + this.getCreated() + " }";
    }
}
