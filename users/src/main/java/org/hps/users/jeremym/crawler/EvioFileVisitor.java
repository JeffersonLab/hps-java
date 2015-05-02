package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lcsim.util.log.LogUtil;

class EvioFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER = LogUtil.create(EvioFileVisitor.class);

    List<FileFilter> filters = new ArrayList<FileFilter>();

    RunLog runs = new RunLog();

    EvioFileVisitor() {
        addFilter(new EvioFilter());
    }

    boolean accept(final File file) {
        boolean accept = true;
        for (final FileFilter filter : this.filters) {
            accept = filter.accept(file);
            if (accept == false) {
                LOGGER.fine(filter.getClass().getSimpleName() + " rejected file: " + file.getPath());
                break;
            }
        }
        return accept;
    }

    void addFilter(final FileFilter filter) {
        this.filters.add(filter);
        LOGGER.config("added filter: " + filter.getClass().getSimpleName());
    }

    RunLog getRunLog() {
        return this.runs;
    }

    @Override
    public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {

        final File file = path.toFile();
        if (accept(file)) {

            final Integer run = EvioFileUtilities.getRunFromName(file);
            final Integer seq = EvioFileUtilities.getSequenceNumber(file);

            LOGGER.info("adding file: " + file.getPath() + "; run: " + run + "; seq = " + seq);

            this.runs.getRunSummary(run).addFile(file);
        } else {
            LOGGER.fine("rejected file: " + file.getPath());
        }
        return FileVisitResult.CONTINUE;
    }
}
