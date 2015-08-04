package org.hps.record.evio.crawler;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

/**
 * A filter which rejects files with run numbers not in a specified set.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunFilter implements FileFilter {

    /**
     * Set of run numbers to accept.
     */
    private final Set<Integer> acceptRuns;

    /**
     * Create a new <code>RunFilter</code> with a set of run numbers to accept.
     *
     * @param acceptRuns the set of runs to accept
     */
    RunFilter(final Set<Integer> acceptRuns) {
        if (acceptRuns.isEmpty()) {
            throw new IllegalArgumentException("the acceptRuns collection is empty");
        }
        this.acceptRuns = acceptRuns;
    }

    /**
     * Returns <code>true</code> if file is accepted.
     *
     * @param file the EVIO file
     * @return <code>true</code> if file is accepted
     */
    @Override
    public boolean accept(final File file) {
        return this.acceptRuns.contains(EvioFileUtilities.getRun(file));
    }
}
