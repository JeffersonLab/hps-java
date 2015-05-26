package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Processes all the EVIO files from a run.
 * <p>
 * This class is a wrapper for activating different sub-tasks, including optionally caching all files from the JLAB MSS to the cache disk using
 * jcache.
 * <p>
 * There is also a list of processors which is run on all events from the run, if the processor list is not empty.
 *
 * @author Jeremy McCormick
 */
final class RunProcessor {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(RunProcessor.class, new DefaultLogFormatter(), Level.FINE);

    /**
     * The cache manager.
     */
    private final JCacheManager cacheManager;

    /**
     * The event printing interval when processing EVIO files.
     */
    private int eventPrintInterval = 1000;

    /**
     * Max files to read (defaults to unlimited).
     */
    private int maxFiles = -1;

    /**
     * The list of EVIO processors to run on the files that are found.
     */
    private final List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

    /**
     * The run summary information updated by running this processor.
     */
    private final RunSummary runSummary;

    /**
     * Set to <code>true</code> to use file caching.
     */
    private boolean useFileCache;

    /**
     * Create a new run processor.
     *
     * @param runSummary the run summary to update
     * @param cacheManager the cache manager for executing 'jcache' commands
     */
    RunProcessor(final RunSummary runSummary, final JCacheManager cacheManager) {
        this.runSummary = runSummary;
        this.cacheManager = cacheManager;
    }

    /**
     * Add a processor of EVIO events.
     *
     * @param processor the EVIO event processor
     */
    void addProcessor(final EvioEventProcessor processor) {
        this.processors.add(processor);
        LOGGER.config("added processor " + processor.getClass().getSimpleName());
    }

    /**
     * Cache all files and wait for the operation to complete.
     * <p>
     * Potentially, this operation can take a very long time. This can be managed using the {@link JCacheManager#setWaitTime(long)} method to set a
     * timeout.
     */
    private void cacheFiles() {

        LOGGER.info("caching files from run " + this.runSummary.getRun() + " ...");

        // Cache all the files and wait for the operation to complete (it will take awhile!).
        this.cacheManager.cache(this.getFiles());
        final boolean cached = this.cacheManager.waitForCache();

        // If the files weren't cached then die.
        if (!cached) {
            throw new RuntimeException("The cache process did not complete in time.");
        }

        LOGGER.info("done caching files from run " + this.runSummary.getRun());
    }

    Integer computeEventCount(final EvioReader reader) throws IOException, EvioException {
        return reader.getEventCount();
    }

    /**
     * Get the list of files to process, which will be limited by the {@link #maxFiles} value if it is set.
     *
     * @return the files to process
     */
    private List<File> getFiles() {
        // Get the list of files to process, taking into account the max files setting.
        List<File> files = this.runSummary.getEvioFileList();
        if (this.maxFiles != -1) {
            LOGGER.info("limiting processing to first " + this.maxFiles + " files from max files setting");
            files = files.subList(0, this.maxFiles - 1);
        }
        return files;
    }

    /**
     * Get the list of EVIO processors.
     *
     * @return the list of EVIO processors
     */
    List<EvioEventProcessor> getProcessors() {
        return this.processors;
    }

    /**
     * Return <code>true</code> if valid END event can be located. 
     * 
     * @param reader the EVIO reader
     * @return <code>true</code> if valid END event is located
     * @throws Exception if there are IO problems using the reader
     */
    boolean isEndOkay(final EvioReader reader) throws Exception {
        LOGGER.info("checking is END okay ...");
        boolean endOkay = false;
        reader.gotoEventNumber(reader.getEventCount() - 2);
        EvioEvent event = null;
        while ((event = reader.parseNextEvent()) != null) {
            if (event.getHeader().getTag() == EvioEventConstants.END_EVENT_TAG) {
                endOkay = true;
                break;
            }
        }
        return endOkay;
    }

    /**
     * Process the run.
     *
     * @throws Exception if there is an error processing a file
     */
    void process() throws Exception {

        LOGGER.info("processing run " + this.runSummary.getRun() + " ...");

        // First cache all the files we will process, if necessary.
        if (this.useFileCache) {
            this.cacheFiles();
        }

        // Run the start of job hooks.
        for (final EvioEventProcessor processor : this.processors) {
            processor.startJob();
        }

        // Process all the files.
        for (final File file : this.getFiles()) {
            this.process(file);
        }

        // Run the end of job hooks.
        for (final EvioEventProcessor processor : this.processors) {
            processor.endJob();
        }

        LOGGER.info("done processing run " + this.runSummary.getRun());
    }

    /**
     * Process a single EVIO file from the run.
     *
     * @param file the EVIO file
     * @throws EvioException if there is an EVIO error
     * @throws IOException if there is some kind of IO error
     * @throws Exception if there is a generic error thrown by event processing
     */
    // FIXME: I think this method is terribly inefficient right now.
    private void process(final File file) throws EvioException, IOException, Exception {
        LOGGER.fine("processing " + file.getPath() + " ...");

        EvioReader reader = null;
        try {
            // Open with wrapper method which will use the cached file path if necessary.
            LOGGER.fine("opening " + file.getPath() + " for reading ...");
            reader = EvioFileUtilities.open(file, true);
            LOGGER.fine("done opening " + file.getPath());

            // If this is the first file then get the start date.
            if (file.equals(this.runSummary.getEvioFileList().first())) {
                LOGGER.fine("getting run start ...");
                final Date runStart = EvioFileUtilities.getRunStart(file);
                LOGGER.fine("got run start " + runStart);
                this.runSummary.setStartDate(runStart);
            }

            // Compute event count for the file and store the value in the run summary's file list.
            LOGGER.info("getting event count for " + file.getPath() + "...");
            final int eventCount = this.computeEventCount(reader);
            this.runSummary.getEvioFileList().setEventCount(file, eventCount);
            LOGGER.info("set event count " + eventCount + " for " + file.getPath());

            // Process the events using the list of EVIO processors.
            LOGGER.info("running EVIO processors ...");
            reader.gotoEventNumber(0);
            int nProcessed = 0;
            if (!this.processors.isEmpty()) {
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    for (final EvioEventProcessor processor : this.processors) {
                        processor.process(event);
                        ++nProcessed;
                        if (nProcessed % this.eventPrintInterval == 0) {
                            LOGGER.finer("processed " + nProcessed + " EVIO events");
                        }
                    }
                }
            }
            LOGGER.info("done running EVIO processors");

            // Check if END event is present if this is the last file in the run.
            if (file.equals(this.runSummary.getEvioFileList().last())) {
                LOGGER.info("checking end okay ...");
                final boolean endOkay = this.isEndOkay(reader);
                this.runSummary.setEndOkay(endOkay);
                LOGGER.info("endOkay set to " + endOkay);

                LOGGER.info("getting end date ...");
                final Date endDate = EvioFileUtilities.getRunEnd(file);
                this.runSummary.setEndDate(endDate);
                LOGGER.info("found end date " + endDate);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        LOGGER.fine("done processing " + file.getPath());
    }

    /**
     * Set the event print interval when running the EVIO processors.
     *
     * @param eventPrintInterval the event print interval when running the EVIO processors
     */
    void setEventPrintInterval(final int eventPrintInterval) {
        this.eventPrintInterval = eventPrintInterval;
    }

    /**
     * Set the maximum number of files to process.
     * <p>
     * This is primarily used for debugging purposes.
     *
     * @param maxFiles the maximum number of files to process
     */
    void setMaxFiles(final int maxFiles) {
        this.maxFiles = maxFiles;
        LOGGER.config("max files set to " + maxFiles);
    }

    /**
     * Set whether or not to use the file caching, which copies files from the JLAB MSS to the cache disk.
     * <p>
     * Since EVIO data files at JLAB are primarily kept on the MSS, running without this option enabled there will likely cause the job to fail.
     *
     * @param cacheFiles <code>true</code> to enabled file caching
     */
    void useFileCache(final boolean cacheFiles) {
        this.useFileCache = cacheFiles;
        LOGGER.config("file caching enabled");
    }

}
