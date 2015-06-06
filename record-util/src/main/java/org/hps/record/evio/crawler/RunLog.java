package org.hps.record.evio.crawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.lcsim.util.log.LogUtil;

/**
 * This class contains summary information about a series of runs that are themselves modeled with the {@link RunSummary} class. These can be looked
 * up by their run number.
 * <p>
 * This class is able to update the run database using the <code>insert</code> methods.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunLog {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunLog.class);

    /**
     * A map between run numbers and the run summary information.
     */
    private final Map<Integer, RunSummary> runs = new LinkedHashMap<Integer, RunSummary>();

    /**
     * Get a run summary by run number.
     * <p>
     * It will be created if it does not exist.
     *
     * @param run the run number
     * @return the <code>RunSummary</code> for the run number
     */
    public RunSummary getRunSummary(final int run) {
        if (!this.runs.containsKey(run)) {
            LOGGER.info("creating new RunSummary for run " + run);
            this.runs.put(run, new RunSummary(run));
        }
        return this.runs.get(run);
    }
    
    public Collection<RunSummary> getRunSummaries() {
        return this.runs.values();
    }

    /**
     * Get a list of sorted run numbers in this run log.
     * <p>
     * This is a copy of the keys from the map so modifying it will have no effect on this class.
     *
     * @return the list of sorted run numbers
     */
    List<Integer> getSortedRunNumbers() {
        final List<Integer> runList = new ArrayList<Integer>(this.runs.keySet());
        Collections.sort(runList);
        return runList;
    }

    /**
     * Print out the run summaries to <code>System.out</code>.
     */
    void printRunSummaries() {
        for (final int run : this.runs.keySet()) {
            this.runs.get(run).printRunSummary(System.out);
        }
    }

    /**
     * Sort all the file lists in place (by sequence number).
     */
    void sortAllFiles() {
        for (final Integer run : this.runs.keySet()) {
            this.runs.get(run).sortFiles();
        }
    }
}
