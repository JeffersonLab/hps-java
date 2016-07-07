package org.hps.rundb;

import java.util.Date;

/**
 * Implementation of {@link RunSummary} for retrieving information from the run database.
 *
 * @author jeremym
 */
public final class RunSummaryImpl implements RunSummary {

    /**
     * Date this record was created.
     */
    private Date created;

    /**
     * Timestamp of END event.
     */
    private Integer endTimestamp;

    /**
     * Timestamp of GO event.
     */
    private Integer goTimestamp;

    /**
     * Clock livetime calculation.
     */
    private Double livetimeClock;

    /**
     * FCup TDC livetime calculation.
     */
    private Double livetimeTdc;

    /**
     * FCup TRG livetime calculation.
     */
    private Double livetimeTrg;

    /**
     * Notes about the run (from spreadsheet).
     */
    private String notes;

    /**
     * Timestamp of PRESTART event.
     */
    private Integer prestartTimestamp;

    /**
     * The run number.
     */
    private final Integer run;

    /**
     * Target setup (string from run spreadsheet).
     */
    private String target;

    /**
     * TI time offset in ns.
     */
    private Long tiTimeOffset;

    /**
     * The total events found in the run across all files.
     */
    private Long totalEvents;

    /**
     * The total number of files in the run.
     */
    private Integer totalFiles;
   
    /**
     * Name of the trigger config file.
     */
    private String triggerConfigName;

    /**
     * Trigger rate in KHz.
     */
    private double triggerRate;

    /**
     * Date when the run record was last updated.
     */
    private Date updated;

    /**
     * Create a run summary.
     *
     * @param run the run number
     */
    public RunSummaryImpl(final int run) {
        this.run = run;
    }

    @Override
    public Date getCreated() {
        return this.created;
    }

    @Override
    public Integer getEndTimestamp() {
        return endTimestamp;
    }

    @Override
    public Integer getGoTimestamp() {
        return goTimestamp;
    }

    @Override
    public Double getLivetimeClock() {
        return this.livetimeClock;
    }

    @Override
    public Double getLivetimeFcupTdc() {
        return this.livetimeTdc;
    }

    @Override
    public Double getLivetimeFcupTrg() {
        return this.livetimeTrg;
    }

    @Override
    public String getNotes() {
        return this.notes;
    }

    @Override
    public Integer getPrestartTimestamp() {
        return prestartTimestamp;
    }

    @Override
    public Integer getRun() {
        return this.run;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public Long getTiTimeOffset() {
        return this.tiTimeOffset;
    }

    @Override
    public Long getTotalEvents() {
        return this.totalEvents;
    }

    @Override
    public Integer getTotalFiles() {
        return this.totalFiles;
    }
   
    @Override
    public String getTriggerConfigName() {
        return this.triggerConfigName;
    }

    @Override
    public Double getTriggerRate() {
        return this.triggerRate;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    /**
     * Set the creation date of the run summary.
     * 
     * @param created the creation date
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Set the end timestamp.
     * 
     * @param endTimestamp the end timestamp
     */
    public void setEndTimestamp(Integer endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    /**
     * Set the GO timestamp.
     * 
     * @param goTimestamp the GO timestamp
     */
    public void setGoTimestamp(Integer goTimestamp) {
        this.goTimestamp = goTimestamp;
    }

    /**
     * Set the clock livetime. 
     * 
     * @param livetimeClock the clock livetime
     */
    public void setLivetimeClock(Double livetimeClock) {
        this.livetimeClock = livetimeClock;
    }

    /**
     * Set the FCUP TDC livetime.
     * 
     * @param livetimeTdc the FCUP TDC livetime
     */
    public void setLivetimeFcupTdc(Double livetimeTdc) {
        this.livetimeTdc = livetimeTdc;
    }

    /**
     * Set the FCUP TRG livetime.
     * 
     * @param livetimeTrg the FCUP TRG livetime
     */
    public void setLivetimeFcupTrg(Double livetimeTrg) {
        this.livetimeTrg = livetimeTrg;
    }

    /**
     * Set the notes.
     * 
     * @param notes the notes
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Set the PRESTART timestamp.
     * 
     * @param prestartTimestamp the PRESTART timestamp
     */
    public void setPrestartTimestamp(Integer prestartTimestamp) {
        this.prestartTimestamp = prestartTimestamp;
    }

    /**
     * Set the target description.
     * 
     * @param target the target description
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Set the TI time offset in ns.
     * 
     * @param tiTimeOffset the TIM time offset in ns
     */
    public void setTiTimeOffset(Long tiTimeOffset) {
        this.tiTimeOffset = tiTimeOffset;
    }

    /**
     * Set the total number of physics events in the run.
     *
     * @param totalEvents the total number of physics events in the run
     */
    public void setTotalEvents(final Long totalEvents) {
        this.totalEvents = totalEvents;
    }

    /**
     * Set the total number of EVIO files in the run.
     *
     * @param totalFiles the total number of EVIO files in the run
     */
    public void setTotalFiles(final Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    /**
     * Set the trigger config file.
     * 
     * @param triggerConfigName the trigger config file
     */
    public void setTriggerConfigName(String triggerConfigName) {
        this.triggerConfigName = triggerConfigName;
    }

    /**
     * Set the trigger rate in KHz.
     * 
     * @param triggerRate the trigger rate in KHz
     */
    public void setTriggerRate(Double triggerRate) {
        this.triggerRate = triggerRate;
    }

    /**
     * Set the updated date of the summary.
     * 
     * @param updated the updated date
     */
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    /**
     * Convert the object to a string.
     * 
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "RunSummary { " 
                + "run: " + this.getRun() 
                + ", events: " + this.getTotalEvents() 
                + ", files: " + this.getTotalFiles() 
                + ", created: " + this.getCreated() 
                + ", updated: " + this.getUpdated()
                + ", prestartTimestamp: " + this.getPrestartTimestamp()
                + ", goTimestamp: " + this.getGoTimestamp()
                + ", endTimestamp: " + this.getEndTimestamp()
                + ", triggerConfigFile: " + this.getTriggerConfigName()
                + ", triggerRate: " + this.getTriggerRate()
                + ", livetimeClock: " + this.getLivetimeClock()
                + ", livetimeTdc: " + this.getLivetimeFcupTdc()
                + ", livetimeTrg: " + this.getLivetimeFcupTrg()
                + ", tiTimeOffset: " + this.getTiTimeOffset() 
                + " }";
    }
}
