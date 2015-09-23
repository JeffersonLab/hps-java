package org.hps.run.database;

import java.util.List;

/**
 * Database API for managing basic run summary information in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
interface RunSummaryDao {

    /**
     * Delete a run summary from the database including its referenced objects such as EPICS data.
     *
     * @param runSummary the run summary to delete
     */
    void deleteFullRun(int run);

    /**
     * Delete a run summary by run number.
     *
     * @param run the run number
     */
    void deleteRunSummary(int run);

    /**
     * Delete a run summary but not its objects.
     *
     * @param runSummary the run summary object
     */
    void deleteRunSummary(RunSummary runSummary);

    /**
     * Get the list of run numbers.
     *
     * @return the list of run numbers
     */
    List<Integer> getRuns();

    /**
     * Get a list of run summaries without loading their objects such as EPICS data.
     *
     * @return the list of run summaries
     */
    List<RunSummary> getRunSummaries();

    /**
     * Get a run summary by run number without loading object state.
     *
     * @param run the run number
     * @return the run summary object
     */
    RunSummary getRunSummary(int run);

    /**
     * Insert a list of run summaries along with its referenced objects such as scaler and EPICS data.
     *
     * @param runSummaryList the list of run summaries
     * @param deleteExisting <code>true</code> to allow deletion and replacement of existing run summaries
     */
    void insertFullRunSummaries(List<RunSummary> runSummaryList, boolean deleteExisting);

    /**
     * Insert a run summary including all its objects.
     *
     * @param runSummary the run summary object
     */
    void insertFullRunSummary(RunSummary runSummary);

    /**
     * Insert a run summary but not its objects.
     *
     * @param runSummary the run summary object
     */
    void insertRunSummary(RunSummary runSummary);

    /**
     * Read a run summary and its objects such as scaler data.
     *
     * @param run the run number
     * @return the full run summary
     */
    RunSummary readFullRunSummary(int run);

    /**
     * Return <code>true</code> if a run summary exists in the database.
     *
     * @param run the run number
     * @return <code>true</code> if <code>run</code> exists in the database
     */
    boolean runSummaryExists(int run);

    /**
     * Update a run summary but not its objects.
     *
     * @param runSummary the run summary to update
     */
    void updateRunSummary(RunSummary runSummary);
}
