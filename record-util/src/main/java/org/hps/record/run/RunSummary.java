package org.hps.record.run;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.triggerbank.TriggerConfigInt;

/**
 * This is an API for accessing run summary information which is persisted as a row in the <i>runs</i> table of the run
 * database.
 * <p>
 * This information includes:
 * <ul>
 * <li>run number</li>
 * <li>start date</li>
 * <li>end date</li>
 * <li>number of events</li>
 * <li>number of EVIO files</li>
 * <li>whether the END event was found indicating that the DAQ did not crash</li>
 * <li>whether the run is considered good (all <code>true</code> for now)</li>
 * </ul>
 * <p>
 * It also references several complex objects including lists of {@link org.hps.record.epics.EpicsData} and
 * {@link org.hps.record.scalers.ScalerData} for the run, as well as a list of EVIO files.
 *
 * @see RunSummaryImpl
 * @see RunSummaryDao
 * @see RunSummaryDaoImpl
 * @see RunManager
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface RunSummary {
  
    /**
     * Get the creation date of this run record.
     *
     * @return the creation date of this run record
     */
    Date getCreated();

    /**
     * Get the end date.
     *
     * @return the end date
     */
    Date getEndDate();

    /**
     * Return <code>true</code> if END event was found in the data.
     *
     * @return <code>true</code> if END event was in the data
     */
    boolean getEndOkay();

    /**
     * Get the EPICS data from the run.
     *
     * @return the EPICS data from the run
     */
    List<EpicsData> getEpicsData();

    /**
     * Get the event rate (effectively the trigger rate) which is the total events divided by the number of seconds in
     * the run.
     *
     * @return the event rate
     */
    double getEventRate();

    /**
     * Get the list of EVIO files in this run.
     *
     * @return the list of EVIO files in this run
     */
    List<File> getEvioFiles();

    /**
     * Get the run number.
     *
     * @return the run number
     */
    int getRun();

    /**
     * Return <code>true</code> if the run was okay (no major errors or data corruption occurred).
     *
     * @return <code>true</code> if the run was okay
     */
    boolean getRunOkay();

    /**
     * Get the scaler data of this run.
     *
     * @return the scaler data of this run
     */
    List<ScalerData> getScalerData();

    /**
     * Get the trigger config int values.
     *
     * @return the trigger config int values
     */
    TriggerConfigInt getTriggerConfigInt();

    /**
     * Get the start date.
     *
     * @return the start date
     */
    Date getStartDate();

    /**
     * Get the total events in the run.
     *
     * @return the total events in the run
     */
    int getTotalEvents();

    /**
     * Get the total number of files for this run.
     *
     * @return the total number of files for this run
     */
    int getTotalFiles();

    /**
     * Get the number of seconds in the run which is the difference between the start and end times.
     *
     * @return the total seconds in the run
     */
    long getTotalSeconds();

    /**
     * Get the date when this run record was last updated.
     *
     * @return the date when this run record was last updated
     */
    Date getUpdated();
}
