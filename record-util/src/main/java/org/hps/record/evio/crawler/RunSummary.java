package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

/**
 * This class models the run summary information which is persisted as one record in the <i>run_log</i> table.
 * <p>
 * This information includes:
 * <ul>
 * <li>run number</li>
 * <li>start date</li>
 * <li>end date</li>
 * <li>total number of events across all files in the run</li>
 * <li>number of files found belonging to the run</li>
 * <li>whether the EVIO END event was found</li>
 * </ul>
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class RunSummary {

    private static final Logger LOGGER = LogUtil.create(RunSummary.class);

    private Date endDate;
    private EpicsData epics;
    private Map<Object, Integer> eventTypeCounts;
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

    EpicsData getEpicsData() {
        return this.epics;
    }

    Map<Object, Integer> getEventTypeCounts() {
        return this.eventTypeCounts;
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
                reader = EvioFileUtilities.open(lastFile);
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
        ps.println("event types");
        for (final Object key : this.eventTypeCounts.keySet()) {
            ps.println("  " + key + ": " + this.eventTypeCounts.get(key));
        }
        ps.println("files" + this.files.size());
        for (final File file : this.files) {
            ps.println(file.getPath());
        }
    }

    void setEpicsData(final EpicsData epics) {
        this.epics = epics;
    }

    void setEventTypeCounts(final Map<Object, Integer> eventTypeCounts) {
        this.eventTypeCounts = eventTypeCounts;
    }

    void sortFiles() {
        this.files.sort();
    }
}
