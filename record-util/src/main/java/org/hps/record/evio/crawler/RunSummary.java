package org.hps.record.evio.crawler;

import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.lcsim.util.log.LogUtil;

/**
 * This class models the run summary information which is persisted as a row in the <i>run_log</i> table
 * of the run database.
 * <p>
 * This information includes:
 * <ul>
 * <li>run number</li>
 * <li>start date</li>
 * <li>end date</li>
 * <li>total number of events across all files in the run</li>
 * <li>number of files found belonging to the run</li>
 * <li>whether the EVIO END event was found</li>
 * <li>whether the run is considered good</li>
 * </ul>
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunSummary {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(RunSummary.class);

    /**
     * The end date of the run.
     */
    private Date endDate;

    /**
     * This is <code>true</code> if the END event is found in the data.
     */
    private boolean endOkay;

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
    private final EvioFileList files = new EvioFileList();

    /**
     * The run number.
     */
    private final int run;

    /**
     * The start date of the run.
     */
    private Date startDate;

    /**
     * The total events found in the run across all files.
     */
    private int totalEvents = -1;

    /**
     * Create a run summary.
     *
     * @param run the run number
     */
    RunSummary(final int run) {
        this.run = run;
    }

    /**
     * Add an EVIO file from this run to the list.
     *
     * @param file the file to add
     */
    void addFile(final File file) {
        this.files.add(file);

        // Total events must be recomputed.
        this.totalEvents = -1;
    }

    /**
     * Get the date when the run ended.
     * <p>
     * This will be extracted from the EVIO END event. If there is no END record it will be the last event time.
     *
     * @return the date when the run ended
     */
    Date getEndDate() {
        return this.endDate;
    }

    /**
     * Get the EPICS data summary.
     * <p>
     * This is computed by taking the mean of each variable for the run.
     *
     * @return the EPICS data summary
     */
    EpicsData getEpicsData() {
        return this.epics;
    }

    /**
     * Get the counts of different event types.
     *
     * @return the counts of different event types
     */
    Map<Object, Integer> getEventTypeCounts() {
        return this.eventTypeCounts;
    }

    /**
     * Get the list of EVIO files in this run.
     *
     * @return the list of EVIO files in this run
     */
    EvioFileList getEvioFileList() {
        return this.files;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    int getRun() {
        return this.run;
    }

    /**
     * Get the start date of the run.
     *
     * @return the start date of the run
     */
    Date getStartDate() {
        return this.startDate;
    }

    /**
     * Get the total events in the run.
     *
     * @return the total events in the run
     */
    int getTotalEvents() {
        if (this.totalEvents == -1) {
            this.totalEvents = this.files.getTotalEvents();
        }
        return this.totalEvents;
    }

    /**
     * Return <code>true</code> if END event was found in the data.
     *
     * @return <code>true</code> if END event was in the data
     */
    boolean isEndOkay() {
        return this.endOkay;
    }

    /**
     * Print the run summary.
     *
     * @param ps the print stream for output
     */
    void printRunSummary(final PrintStream ps) {
        ps.println("--------------------------------------------");
        ps.println("run: " + this.run);
        ps.println("first file: " + this.files.first());
        ps.println("last file: " + this.files.last());
        ps.println("started: " + this.getStartDate());
        ps.println("ended: " + this.getEndDate());
        ps.println("total events: " + this.getTotalEvents());
        ps.println("event types");
        for (final Object key : this.eventTypeCounts.keySet()) {
            ps.println("  " + key + ": " + this.eventTypeCounts.get(key));
        }
        ps.println("files " + this.files.size());
        for (final File file : this.files) {
            ps.println("  " + file.getPath());
        }
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
     * Set if end is okay.
     *
     * @param endOkay <code>true</code> if end is okay
     */
    void setEndOkay(final boolean endOkay) {
        this.endOkay = endOkay;
    }

    /**
     * Set the EPICS data for the run.
     *
     * @param epics the EPICS data for the run
     */
    void setEpicsData(final EpicsData epics) {
        this.epics = epics;
    }

    /**
     * Set the event type counts for the run.
     *
     * @param eventTypeCounts the event type counts for the run
     */
    void setEventTypeCounts(final Map<Object, Integer> eventTypeCounts) {
        this.eventTypeCounts = eventTypeCounts;
    }

    /**
     * Set the start date of the run.
     *
     * @param startDate the start date of the run
     */
    void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Sort the files in the run by sequence number in place.
     */
    void sortFiles() {
        this.files.sort();
    }
    
    public String toString() {
        return "RunSummary { run: " + this.run + ", started: " + this.getStartDate() + ", ended: " + this.getEndDate() + ", events: " 
                + this.getTotalEvents() + ", endOkay: " + endOkay + " }";
    }
}
