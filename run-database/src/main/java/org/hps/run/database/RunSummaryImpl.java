package org.hps.run.database;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hps.datacat.client.DatasetFileFormat;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.triggerbank.TriggerConfig;

/**
 * Implementation of {@link RunSummary} for retrieving information from the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunSummaryImpl implements RunSummary {

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
     * The EPICS data from the run.
     */
    private List<EpicsData> epicsDataList;

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
     * Lists of files indexed by their format.
     */
    private Map<DatasetFileFormat, List<File>> fileMap = new HashMap<DatasetFileFormat, List<File>>();

    /**
     * Create a run summary.
     *
     * @param run the run number
     */
    public RunSummaryImpl(final int run) {
        this.run = run;
    }

    /**
     * Add an EVIO file from this run to the list.
     *
     * @param file the file to add
     */
    public void addEvioFile(final File file) {
        this.getEvioFiles().add(file);
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
    public List<EpicsData> getEpicsData() {
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
     * Get the list of EVIO files in this run.
     *
     * @return the list of EVIO files in this run
     */
    public List<File> getEvioFiles() {
        return this.fileMap.get(DatasetFileFormat.EVIO);
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
     * Set the creation date of the run record.
     *
     * @param created the creation date of the run record
     */
    void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * Set the start date.
     *
     * @param startDate the start date
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
    void setEpicsData(final List<EpicsData> epicsDataList) {
        this.epicsDataList = epicsDataList;
    }
    
    /**
     * Set whether the run was "okay" meaning the data is usable for physics
     * analysis.
     *
     * @param runOkay <code>true</code> if the run is okay
     */
    void setRunOkay(final boolean runOkay) {
        this.runOkay = runOkay;
    }

    /**
     * Set the scaler data of the run.
     *
     * @param scalerData the scaler data
     */
    void setScalerData(final List<ScalerData> scalerDataList) {
        this.scalerDataList = scalerDataList;
    }

    /**
     * Set the trigger config of the run.
     *
     * @param triggerConfig the trigger config
     */
    void setTriggerConfig(final TriggerConfig triggerConfig) {
        this.triggerConfig = triggerConfig;
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
     * Set the total number of physics events in the run.
     *
     * @param totalEvents the total number of physics events in the run
     */
    void setTotalEvents(final int totalEvents) {
        this.totalEvents = totalEvents;
    }

    /**
     * Set the total number of EVIO files in the run.
     *
     * @param totalFiles the total number of EVIO files in the run
     */
    void setTotalFiles(final int totalFiles) {
        this.totalFiles = totalFiles;
    }

    /**
     * Set the date when this run record was last updated.
     *
     * @param updated the date when the run record was last updated
     */
    void setUpdated(final Date updated) {
        this.updated = updated;
    }
    
    /**
     * Add a file associated with this run.
     * <p>
     * This is public because it is called by the file crawler.
     * 
     * @param file a file associated with this run
     */
    // FIXME: This should be removed from the run summary interface.
    public void addFile(DatasetFileFormat format, File file) {
        List<File> files = this.fileMap.get(file);
        if (files == null) {
            this.fileMap.put(format, new ArrayList<File>());
        }
        this.fileMap.get(format).add(file);
    }
    
    /**
     * Get a list of files in the run by format (EVIO, LCIO etc.).
     * 
     * @param format the file format
     * @return the list of files with the given format
     */
    public List<File> getFiles(DatasetFileFormat format) {
        if (!this.fileMap.containsKey(format)) {
            this.fileMap.put(format, new ArrayList<File>());
        }
        return this.fileMap.get(format);
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "RunSummary { " 
                + "run: " + this.getRun() 
                + ", startDate: " + (this.getStartDate() != null ? DATE_DISPLAY.format(this.getStartDate()) : null)
                + ", endDate: " + (this.getEndDate() != null ? DATE_DISPLAY.format(this.getEndDate()) : null) 
                + ", totalEvents: " + this.getTotalEvents()
                + ", totalFiles: " + this.getTotalFiles() 
                + ", endOkay: " + this.getEndOkay() 
                + ", runOkay: "
                + this.getRunOkay() 
                + ", updated: " + this.getUpdated() 
                + ", created: " + this.getCreated() 
                + " }";
    }
}
