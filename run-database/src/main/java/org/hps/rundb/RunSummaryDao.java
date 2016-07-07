package org.hps.rundb;

import java.util.List;

/**
 * Database API for managing basic run summary information in the run database.
 *
 * @author jeremym
 */
public interface RunSummaryDao {
  
    /**
     * Delete a run summary by run number.
     *
     * @param run the run number
     */
    void deleteRunSummary(int run);

    /**
     * Get the list of run numbers.
     *
     * @return the list of run numbers
     */
    List<Integer> getRuns();
  
    /**
     * Get a run summary by run number without loading object state.
     *
     * @param run the run number
     * @return the run summary object
     */
    RunSummary getRunSummary(int run);
  
    /**
     * Insert a run summary.
     *
     * @param runSummary the run summary object
     */
    void insertRunSummary(RunSummary runSummary);

    /**
     * Return <code>true</code> if a run summary exists in the database.
     *
     * @param run the run number
     * @return <code>true</code> if <code>run</code> exists in the database
     */
    boolean runSummaryExists(int run);
    
    /**
     * Update a run summary that already exists.
     * 
     * @param runSummary the run summary to update
     */
    void updateRunSummary(RunSummary runSummary);
}
