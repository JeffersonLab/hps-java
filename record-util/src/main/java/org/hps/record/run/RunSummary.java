package org.hps.record.run;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hps.record.epics.EpicsData;
import org.hps.record.evio.EvioFileSequenceComparator;
import org.hps.record.scalers.ScalerData;

/**
 * This class models the run summary information which is persisted as a row in
 * the <i>run_log</i> table of the run database.
 * <p>
 * This information includes:
 * <ul>
 * <li>run number</li>
 * <li>start time (UTC)</li>
 * <li>end time (UTC)</li>
 * <li>total number of events in the run</li>
 * <li>number of EVIO files in the run</li>
 * <li>whether the END event was found indicating that the DAQ did not
 * crash</li>
 * <li>whether the run is considered good (all <code>true</code> for now)</li>
 * </ul>
 * <p>
 * It also references several complex objects including lists of
 * {@link org.hps.record.epics.EpicsData} and
 * {@link org.hps.record.scalers.ScalerData} for the run, as well as a list of
 * EVIO files.
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
         * Set default time zone for display to East Coast (JLAB) where data was
         * taken.
         */
        DATE_DISPLAY.setCalendar(new GregorianCalendar(TimeZone.getTimeZone("America/New_York")));
    }

    /**
     * Date this record was created.
     */
    private Date created;

    /**
     * End date of run.
     */
    private Date endDate;

    /**
     * This is <code>true</code> if the END event is found in the data.
     */
    private boolean endOkay;

    /**
     * The run end time in UTC (milliseconds).
     */
    private long endTimeUtc;

    /**
     * The EPICS data from the run.
     */
    private List<EpicsData> epicsDataList;

    /**
     * The counts of different types of events that were found.
     */
    private Map<Object, Integer> eventTypeCounts;

    /**
     * The list of EVIO files in the run.
     */
    private List<File> evioFileList = new ArrayList<File>();

    /**
     * The run number.
     */
    private final int run;

    /**
     * Flag to indicate run was okay.
     */
    private boolean runOkay = true;

    /**
     * The scaler data for the run.
     */
    private List<ScalerData> scalerDataList;

    /**
     * The trigger data for the run.
     */
    private TriggerConfig triggerConfig;

    /**
     * Start date of run.
     */
    private Date startDate;

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
        return endDate;
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
     * Get the EPICS data from the run.
     *
     * @return the EPICS data from the run
     */
    public List<EpicsData> getEpicsDataSet() {
        return this.epicsDataList;
    }

    /**
     * Get the event rate (effectively the trigger rate) which is the total
     * events divided by the number of seconds in the run.
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
    public List<File> getEvioFileList() {
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
     * Return <code>true</code> if the run was okay (no major errors or data
     * corruption occurred).
     *
     * @return <code>true</code> if the run was okay
     */
    public boolean getRunOkay() {
        return this.runOkay;
    }

    /**
     * Get the scaler data of this run.
     *
     * @return the scaler data of this run
     */
    public List<ScalerData> getScalerData() {
        return this.scalerDataList;
    }

    /**
     * Get the trigger config of this run.
     *
     * @return the trigger config of this run
     */
    public TriggerConfig getTriggerConfig() {
        return triggerConfig;
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
     * Get the number of seconds in the run which is the difference between the
     * start and end times.
     *
     * @return the total seconds in the run
     */
    public long getTotalSeconds() {
        return (endDate.getTime() - startDate.getTime()) / 1000;
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
        ps.println("first file: " + this.evioFileList.get(0));
        ps.println("last file: " + this.evioFileList.get(evioFileList.size() - 1));
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
     * Set the start date.
     *
     * @param startDate the start date
     */
    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
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
     * @param endTimeUtc the end date
     */
    public void setEndTimeUtc(final long endTimeUtc) {
        this.endTimeUtc = endTimeUtc;
    }

    /**
     * Set the EPICS data for the run.
     *
     * @param epics the EPICS data for the run
     */
    public void setEpicsData(final List<EpicsData> epicsDataList) {
        this.epicsDataList = epicsDataList;
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
    public void setEvioFileList(final List<File> evioFileList) {
        this.evioFileList = evioFileList;
    }

    /**
     * Set whether the run was "okay" meaning the data is usable for physics
     * analysis.
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
    public void setScalerData(final List<ScalerData> scalerDataList) {
        this.scalerDataList = scalerDataList;
    }

    /**
     * Set the trigger config of the run.
     *
     * @param triggerConfig the trigger config
     */
    public void setTriggerConfig(final TriggerConfig triggerConfig) {
        this.triggerConfig = triggerConfig;
    }

    /**
     * Set the start date.
     *
     * @param startDate the start date
     */
    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
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
        Collections.sort(this.evioFileList, new EvioFileSequenceComparator());
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "RunSummary { run: " + this.getRun() + ", startDate: " + DATE_DISPLAY.format(this.getStartDate())
                + ", endDate: " + DATE_DISPLAY.format(this.getEndDate()) + ", totalEvents: " + this.getTotalEvents()
                + ", totalFiles: " + this.getTotalFiles() + ", endOkay: " + this.getEndOkay() + ", runOkay: "
                + this.getRunOkay() + ", updated: " + this.getUpdated() + ", created: " + this.getCreated() + " }";
    }
}
