package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.run.RunSummary;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Processes EVIO files from a run in order to extract various meta data information including start and end dates.
 * <p>
 * This class is a wrapper for activating different sub-tasks, including optionally caching all files from the JLAB MSS
 * to the cache disk.
 * <p>
 * There is also a list of processors which is run on all events from the run.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunProcessor {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(RunProcessor.class, new DefaultLogFormatter(), Level.FINE);

    /**
     * Process all the runs that were found.
     *
     * @param runs the run log containing the list of run summaries
     * @throws Exception if there is an error processing one of the runs
     */
    static void processAllRuns(final JCacheManager cacheManager, final RunLog runs, final CrawlerConfig config)
            throws Exception {

        // Configure the max wait time for file caching operations.
        if (config.waitTime() != null && config.waitTime() > 0L) {
            cacheManager.setWaitTime(config.waitTime());
        }

        // Process all of the runs that were found.
        for (final int run : runs.getSortedRunNumbers()) {

            // Get the run summary.
            final RunSummary runSummary = runs.getRunSummary(run);

            // Clear the cache manager.
            if (config.useFileCache()) {
                cacheManager.clear();
            }

            // Create a processor to process all the EVIO events in the run.
            final RunProcessor runProcessor = new RunProcessor(cacheManager, runSummary, config);

            // Add extra processors.
            for (final EvioEventProcessor processor : config.processors()) {
                runProcessor.addProcessor(processor);
                LOGGER.config("added extra EVIO processor " + processor.getClass().getName());
            }

            // Process all of the run's files.
            runProcessor.processRun();
        }
    }

    /**
     * The cache manager.
     */
    private final JCacheManager cacheManager;

    /**
     * Processor for extracting EPICS information.
     */
    private final EpicsLog epicsLog;

    /**
     * Processor for extracting event type counts (sync, physics, trigger types, etc.).
     */
    private final EventCountProcessor eventCountProcessor;

    /**
     * The event printing interval when processing EVIO files.
     */
    private int eventPrintInterval = 1000;

    /**
     * Max total files to read (default is unlimited).
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
     * Processor for extracting scaler data.
     */
    private final ScalersEvioProcessor scalersProcessor;

    /**
     * Set to <code>true</code> to use file caching.
     */
    private boolean useFileCache;

    /**
     * Create the processor for a single run.
     *
     * @param runSummary the run summary for the run
     * @return the run processor
     */
    RunProcessor(final JCacheManager cacheManager, final RunSummary runSummary, final CrawlerConfig config) {

        this.runSummary = runSummary;
        this.cacheManager = cacheManager;

        // EPICS processor.
        epicsLog = new EpicsLog();
        this.addProcessor(epicsLog);

        // Scaler data processor.
        scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);
        this.addProcessor(scalersProcessor);

        // Event log processor.
        eventCountProcessor = new EventCountProcessor();
        this.addProcessor(eventCountProcessor);

        // Max files.
        if (config.maxFiles() != -1) {
            this.setMaxFiles(config.maxFiles());
        }

        // Enable file caching.
        this.useFileCache(config.useFileCache());

        // Set event printing interval.
        this.setEventPrintInterval(config.eventPrintInterval());
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
     * Potentially, this operation can take a very long time. This can be managed using the
     * {@link JCacheManager#setWaitTime(long)} method to set a timeout.
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

    /**
     * Find the end date in the EVIO events.
     *
     * @param evioReader the open <code>EvioReader</code>
     */
    private void findEndDate(final EvioReader evioReader) {

        // Try to get end date from END event.
        Long endTimestamp = EvioFileUtilities.getTimestamp(evioReader, EvioEventConstants.END_EVENT_TAG, -5);

        if (endTimestamp != null) {
            // Flag end okay for the run.
            this.runSummary.setEndOkay(true);
        } else {
            // Try to find the end date from the last physics event.
            endTimestamp = EvioFileUtilities.getEndTimestamp(evioReader);
            this.runSummary.setEndOkay(false);
        }

        if (endTimestamp == null) {
            // Not finding the end date is a fatal error.
            throw new RuntimeException("Failed to find end date.");
        }

        LOGGER.info("found end timestamp " + endTimestamp);
        this.runSummary.setEndTimeUtc(endTimestamp);
    }

    /**
     * Find the start date in the EVIO events.
     *
     * @param evioReader the open <code>EvioReader</code>
     */
    private void findStartDate(final EvioReader evioReader) {

        // First try to find the start date in the PRESTART event.
        Long startTimestamp = EvioFileUtilities.getTimestamp(evioReader, EvioEventConstants.PRESTART_EVENT_TAG, 0);

        if (startTimestamp == null) {
            // Search for start date in first physics event.
            startTimestamp = EvioFileUtilities.getStartTimestamp(evioReader);
        }

        if (startTimestamp == null) {
            // Not finding the start date is a fatal error.
            throw new RuntimeException("Failed to find start date.");
        }

        LOGGER.fine("got run start " + startTimestamp);
        this.runSummary.setStartTimeUtc(startTimestamp);
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
            int toIndex = this.maxFiles;
            if (toIndex > files.size()) {
                toIndex = files.size();
            }
            files = files.subList(0, toIndex);
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
     * Return <code>true</code> if the file is the first one in the list for the run.
     *
     * @param file the EVIO <code>File</code>
     * @return <code>true</code> if the file is the first one in the list for the run
     */
    private boolean isFirstFile(final File file) {
        return file.equals(this.runSummary.getEvioFileList().first());
    }

    /**
     * Return <code>true</code> if the file is the last one in the list for the run.
     *
     * @param file the EVIO <code>File</code>
     * @return <code>true</code> if the file is the last one in the list for the run
     */
    private boolean isLastFile(final File file) {
        return file.equals(this.getFiles().get(this.getFiles().size() - 1));
    }

    /**
     * Process events using the list of EVIO processors.
     *
     * @param evioReader the open <code>EvioReader</code>
     * @throws IOException if there is a file IO error
     * @throws EvioException if there is an EVIO error
     * @throws Exception if there is some other error
     */
    private void processEvents(final EvioReader evioReader) throws IOException, EvioException, Exception {
        LOGGER.finer("running EVIO processors ...");
        evioReader.gotoEventNumber(0);
        int nProcessed = 0;
        if (!this.processors.isEmpty()) {
            EvioEvent event = null;
            while ((event = evioReader.parseNextEvent()) != null) {
                for (final EvioEventProcessor processor : this.processors) {
                    processor.process(event);
                }
                ++nProcessed;
                if (nProcessed % this.eventPrintInterval == 0) {
                    LOGGER.finer("processed " + nProcessed + " EVIO events");
                }
            }
            LOGGER.info("done running EVIO processors");
        }
    }

    /**
     * Process a single EVIO file from the run.
     *
     * @param file the EVIO file
     * @throws EvioException if there is an EVIO error
     * @throws IOException if there is some kind of IO error
     * @throws Exception if there is a generic error thrown by event processing
     */
    private void processFile(final File file) throws EvioException, IOException, Exception {

        LOGGER.fine("processing file " + file.getPath() + " ...");

        EvioReader evioReader = null;
        try {

            // Open file for reading (flag should be true for sequential or false for mem map).
            evioReader = EvioFileUtilities.open(file, true);

            // If this is the first file then get the start date.
            if (this.isFirstFile(file)) {
                LOGGER.fine("getting run start from first file " + file.getPath() + " ...");
                this.findStartDate(evioReader);
            }

            // Go back to the first event and process the events using the list of EVIO processors.
            this.processEvents(evioReader);

            // Find end date from last file in the run.
            if (this.isLastFile(file)) {
                LOGGER.fine("getting run end from last file " + file.getPath() + " ...");
                this.findEndDate(evioReader);
            }

        } finally {
            // Close the EvioReader for the current file.
            if (evioReader != null) {
                evioReader.close();
            }
        }
        LOGGER.fine("done processing " + file.getPath());
    }

    /**
     * Process the run by executing the registered {@link org.hps.record.evio.EvioEventProcessor}s extracting the start
     * and end dates.
     * <p>
     * This method will also activate file caching, if enabled by the {@link #useFileCache} option.
     *
     * @throws Exception if there is an error processing a file
     */
    void processRun() throws Exception {

        LOGGER.info("processing run " + this.runSummary.getRun() + " ...");

        // First cache all the files we will process, if necessary.
        if (this.useFileCache) {
            this.cacheFiles();
        }

        // Run the start of job hooks.
        for (final EvioEventProcessor processor : this.processors) {
            processor.startJob();
        }

        // Get the list of files, limited by max files setting.
        final List<File> files = this.getFiles();

        LOGGER.info("processing " + files.size() + " from run " + this.runSummary.getRun());

        // Process all the files.
        for (final File file : files) {
            this.processFile(file);
        }

        // Run the end job hooks.
        LOGGER.info("running end of job hooks on EVIO processors ...");
        for (final EvioEventProcessor processor : this.processors) {
            processor.endJob();
        }

        // Put scaler data from EVIO processor into run summary.
        runSummary.setScalerData(this.scalersProcessor.getScalerData());

        // Set the counts of event types on the run summary.
        runSummary.setEventTypeCounts(eventCountProcessor.getEventCounts());

        // Set total number of events on the run summary from the event counter.
        runSummary.setTotalEvents(this.eventCountProcessor.getTotalEventCount());

        // Set EpicsData for the run.
        runSummary.setEpicsData(this.epicsLog.getEpicsData());

        LOGGER.info("done processing run " + this.runSummary.getRun());
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
     * This is intended primarily for debugging.
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
     * Since EVIO data files at JLAB are primarily kept on the MSS, running without this option enabled there will
     * likely cause the job to fail.
     *
     * @param cacheFiles <code>true</code> to enabled file caching
     */
    void useFileCache(final boolean cacheFiles) {
        this.useFileCache = cacheFiles;
        LOGGER.config("file caching enabled");
    }
}
