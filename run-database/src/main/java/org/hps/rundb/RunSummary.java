package org.hps.rundb;

import java.util.Date;

/**
 * This is an API for accessing run summary information which is persisted as a row in the <i>run_summaries</i> table.
 * <p>
 * All timestamp fields use the Unix convention (seconds since the epoch).
 *  
 * @see RunSummaryImpl
 * @see RunSummaryDao
 * @see RunSummaryDaoImpl
 * @see RunManager
 */
public interface RunSummary {

    /**
     * Get the creation date of this record.
     *
     * @return the creation date of this record
     */
    Date getCreated();

    /**
     * Get the END event timestamp or the timestamp from the last head bank if END is not present.
     * 
     * @return the last event timestamp
     */
    Integer getEndTimestamp();

    /**
     * Get the GO event timestamp.
     * 
     * @return the GO event timestamp
     */
    Integer getGoTimestamp();

    /**
     * Get the livetime computed from the clock scaler.
     * 
     * @return the livetime computed from the clock scaler
     */
    Double getLivetimeClock();

    /**
     * Get the livetime computed from the FCUP_TDC scaler.
     * 
     * @return the livetime computed from the FCUP_TDC scaler
     */
    Double getLivetimeFcupTdc();

    /**
     * Get the livetime computed from the FCUP_TRG scaler.
     * 
     * @return the livetime computed from the FCUP_TRG scaler
     */
    Double getLivetimeFcupTrg();

    /**
     * Get the notes for the run (from the run spreadsheet).
     * 
     * @return the notes for the run
     */
    String getNotes();

    /**
     * Get the PRESTART event timestamp.
     * 
     * @return the PRESTART event timestamp
     */
    Integer getPrestartTimestamp();

    /**
     * Get the run number.
     *
     * @return the run number
     */
    Integer getRun();
   
    /**
     * Get the target setting for the run (string from run spreadsheet).
     * 
     * @return the target setting for the run
     */
    String getTarget();

    /**
     * Get the TI time offset in ns.
     * 
     * @return the TI time offset in ns
     */
    Long getTiTimeOffset();

    /**
     * Get the total number of events in the run.
     *
     * @return the total number of events in the run
     */
    Long getTotalEvents();

    /**
     * Get the total number of EVIO files in this run.
     *
     * @return the total number of files in this run
     */
    Integer getTotalFiles();

    /**
     * Get the trigger config name (from the run spreadsheet).
     * 
     * @return the trigger config name
     */
    String getTriggerConfigName();

    /**
     * Get the trigger rate in KHz.
     * 
     * @return the trigger rate in KHz
     */
    Double getTriggerRate();

    /**
     * Get the date when this record was last updated.
     *
     * @return the date when this record was last updated
     */
    Date getUpdated();
}
