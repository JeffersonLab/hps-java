package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

class RunFilter implements FileFilter {
    Set<Integer> acceptRuns;

    RunFilter(final Set<Integer> acceptRuns) {
        this.acceptRuns = acceptRuns;
    }

    @Override
    public boolean accept(final File file) {
        return this.acceptRuns.contains(EvioFileUtilities.getRunFromName(file));
    }
}
