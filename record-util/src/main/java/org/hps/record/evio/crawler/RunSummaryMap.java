package org.hps.record.evio.crawler;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.hps.record.run.RunSummary;
import org.lcsim.util.log.LogUtil;

/**
 * This class maps run numbers to {@link RunSummary} objects.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
final class RunSummaryMap extends HashMap<Integer, RunSummary> {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunSummaryMap.class);

    /**
     * Get the collection of {@link RunSummary} objects.
     *
     * @return the collection of {@link RunSummary} objects
     */
    public Collection<RunSummary> getRunSummaries() {
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
    public RunSummary getRunSummary(final int run) {
        if (!this.containsKey(run)) {
            LOGGER.info("creating new RunSummary for run " + run);
            this.put(run, new RunSummary(run));
        }
        return this.get(run);
    }
}
