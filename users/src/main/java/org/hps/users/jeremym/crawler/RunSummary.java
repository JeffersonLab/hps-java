package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

class RunSummary {

    private static final Logger LOGGER = LogUtil.create(RunSummary.class);

    private Date endDate;
    private final EvioFileList files = new EvioFileList();
    private Boolean isEndOkay;
    private final int run;
    private Date startDate;
    private int totalEvents = -1;

    RunSummary(final int run) {
        this.run = run;
    }

    void addFile(final File file) {
        this.files.add(file);

        // Total events must be recomputed.
        this.totalEvents = -1;
    }

    Date getEndDate() {
        if (this.endDate == null) {
            this.endDate = EvioFileUtilities.getRunEnd(this.files.last());
        }
        return this.endDate;
    }

    EvioFileList getFiles() {
        return this.files;
    }

    Date getStartDate() {
        if (this.startDate == null) {
            this.startDate = EvioFileUtilities.getRunStart(this.files.first());
        }
        return this.startDate;
    }

    int getTotalEvents() {
        if (this.totalEvents == -1) {
            this.totalEvents = this.files.computeTotalEvents();
        }
        return this.totalEvents;
    }

    boolean isEndOkay() {
        if (this.isEndOkay == null) {
            LOGGER.info("checking is END okay ...");
            this.isEndOkay = false;
            final File lastFile = this.files.last();
            EvioReader reader = null;
            try {
                reader = new EvioReader(lastFile, false);
                reader.gotoEventNumber(reader.getEventCount() - 5);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (event.getHeader().getTag() == EvioEventConstants.END_EVENT_TAG) {
                        this.isEndOkay = true;
                        break;
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return this.isEndOkay;
    }

    void printRunSummary(final PrintStream ps) {
        ps.println("--------------------------------------------");
        ps.println("run: " + this.run);
        ps.println("first file: " + this.files.first());
        ps.println("last file: " + this.files.last());
        ps.println("started: " + getStartDate());
        ps.println("ended: " + getEndDate());
        ps.println("total events: " + this.getTotalEvents());
        ps.println("files: " + this.files.size());
        for (final File file : this.files) {
            ps.println(file.getPath());
        }
    }

    void sortFiles() {
        this.files.sort();
    }
}
