package org.hps.crawler;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.hps.run.database.RunSummary;
import org.hps.run.database.RunSummaryImpl;

/**
 * This class maps run numbers to {@link RunSummary} objects.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
final class RunSummaryMap extends HashMap<Integer, RunSummaryImpl> {

    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(RunSummaryMap.class.getPackage().getName());

    /**
     * Get the collection of {@link RunSummary} objects.
     *
     * @return the collection of {@link RunSummary} objects
     */
    Collection<RunSummaryImpl> getRunSummaries() {
        return this.values();
    }

    /**
     * Get a {@link RunSummary} by its run number.
     * <p>
     * It will be created if it does not exist already.
     *
     * @param run the run number
     * @return the <code>RunSummary</code> for the run number
     */
    RunSummaryImpl getRunSummary(final int run) {
        if (!this.containsKey(run)) {
            LOGGER.info("creating new RunSummary for run " + run);
            this.put(run, new RunSummaryImpl(run));
        }
        return this.get(run);
    }
}
