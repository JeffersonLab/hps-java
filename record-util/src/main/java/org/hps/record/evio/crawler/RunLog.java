package org.hps.record.evio.crawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.record.run.RunSummary;
import org.lcsim.util.log.LogUtil;

/**
 * This class contains information about a series of runs which each have a {@link RunSummary} object.
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
     * Get a {@link RunSummary} by its run number.
     * <p>
     * It will be created if it does not exist already.
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

    /**
     * Get the collection of {@link RunSummary} objects.
     * 
     * @return the collection of {@link RunSummary} objects
     */
    public Collection<RunSummary> getRunSummaries() {
        return this.runs.values();
    }

    /**
     * Get a list of sorted run numbers from this run log.
     * <p>
     * This is a copy of the keys from the map, so modifying it will have no effect on the original.
     *
     * @return the list of sorted run numbers
     */
    List<Integer> getSortedRunNumbers() {
        final List<Integer> runList = new ArrayList<Integer>(this.runs.keySet());
        Collections.sort(runList);
        return runList;
    }

    /**
     * Print out each {@link RunSummary} to <code>System.out</code>.
     */
    void printRunSummaries() {
        for (final int run : this.runs.keySet()) {
            this.runs.get(run).printOut(System.out);
        }
    }

    /**
     * Sort the file list for each run in place by EVIO sequence numbers.
     */
    void sortFiles() {
        for (final Integer run : this.runs.keySet()) {
            this.runs.get(run).sortFiles();
        }
    }

    /**
     * Print the run numbers to the log.
     */
    void printRunNumbers() {
        // Print the list of runs that were found.
        final StringBuffer sb = new StringBuffer();
        for (final Integer run : getSortedRunNumbers()) {
            sb.append(run + " ");
        }
        LOGGER.info("found EVIO files from runs: " + sb.toString());
    }

}
