package org.hps.run.database;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.EvioDAQParser;

/**
 * Implementation of {@link RunSummary} for retrieving information from the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunSummaryImpl implements RunSummary {

    /**
     * Date this record was created.
     */
    private Date created;

    /**
     * DAQ config object built from string data.
     */
    private DAQConfig daqConfig;

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
    private Integer totalEvents;

    /**
     * The total number of files in the run.
     */
    private Integer totalFiles;

    /**
     * Map of crate number to trigger config string data.
     */
    private Map<Integer, String> triggerConfigData;

    /**
     * Get the name of the trigger config file.
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
    public DAQConfig getDAQConfig() {
        return this.daqConfig;
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
    public Integer getTotalEvents() {
        return this.totalEvents;
    }

    @Override
    public Integer getTotalFiles() {
        return this.totalFiles;
    }

    @Override
    public Map<Integer, String> getTriggerConfigData() {
        return this.triggerConfigData;
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
     * Load DAQ config object from trigger config string data.
     */
    private void loadDAQConfig() {
        if (this.triggerConfigData != null && !this.triggerConfigData.isEmpty()) {
            EvioDAQParser parser = new EvioDAQParser();
            for (Entry<Integer, String> entry : this.triggerConfigData.entrySet()) {
                parser.parse(entry.getKey(), this.getRun(), new String[] {entry.getValue()});
            }
            ConfigurationManager.updateConfiguration(parser);
            daqConfig = ConfigurationManager.getInstance();
        }
    }

    void setCreated(Date created) {
        this.created = created;
    }

    void setDAQConfig(DAQConfig daqConfig) {
        this.daqConfig = daqConfig;
    }

    void setEndTimestamp(Integer endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    void setGoTimestamp(Integer goTimestamp) {
        this.goTimestamp = goTimestamp;
    }

    void setLivetimeClock(Double livetimeClock) {
        this.livetimeClock = livetimeClock;
    }

    void setLivetimeFcupTdc(Double livetimeTdc) {
        this.livetimeTdc = livetimeTdc;
    }

    void setLivetimeFcupTrg(Double livetimeTrg) {
        this.livetimeTrg = livetimeTrg;
    }

    void setNotes(String notes) {
        this.notes = notes;
    }

    void setPrestartTimestamp(Integer prestartTimestamp) {
        this.prestartTimestamp = prestartTimestamp;
    }

    void setTarget(String target) {
        this.target = target;
    }

    /**
     * Set the TI time offset in ns.
     * 
     * @param tiTimeOffset the TIM time offset in ns
     */
    void setTiTimeOffset(Long tiTimeOffset) {
        this.tiTimeOffset = tiTimeOffset;
    }

    /**
     * Set the total number of physics events in the run.
     *
     * @param totalEvents the total number of physics events in the run
     */
    void setTotalEvents(final Integer totalEvents) {
        this.totalEvents = totalEvents;
    }

    /**
     * Set the total number of EVIO files in the run.
     *
     * @param totalFiles the total number of EVIO files in the run
     */
    void setTotalFiles(final Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    /**
     * Build the DAQ config from the trigger config string data.
     * 
     * @param triggerConfigData a map of crate number to the trigger config string data from the bank
     */
    void setTriggerConfigData(Map<Integer, String> triggerConfigData) {
        this.triggerConfigData = triggerConfigData;
        // Load DAQ config if not already set.
        if (daqConfig == null) {
            loadDAQConfig();
        }
    }

    /**
     * Set the trigger config file.
     * 
     * @param triggerConfigName the trigger config file
     */
    void setTriggerConfigName(String triggerConfigName) {
        this.triggerConfigName = triggerConfigName;
    }

    /**
     * Set the trigger rate in KHz.
     * 
     * @param triggerRate the trigger rate in KHz
     */
    void setTriggerRate(Double triggerRate) {
        this.triggerRate = triggerRate;
    }

    void setUpdated(Date updated) {
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
                + ", DAQConfig: " + (this.getDAQConfig() != null ? true : false)
                + ", triggerRate: " + this.getTriggerRate()
                + ", livetimeClock: " + this.getLivetimeClock()
                + ", livetimeTdc: " + this.getLivetimeFcupTdc()
                + ", livetimeTrg: " + this.getLivetimeFcupTrg()
                + ", tiTimeOffset: " + this.getTiTimeOffset() 
                + " }";
    }
}
