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
import org.hps.record.scalers.ScalersEvioProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Processes EVIO files from a run in order to extract various meta data information including start and end dates.
 * <p>
 * This class is a wrapper for activating different sub-tasks, including optionally caching all files from the JLAB MSS to the cache disk.
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
     * Create the processor for a single run.
     *
     * @param runSummary the run summary for the run
     * @return the run processor
     */
    static RunProcessor createRunProcessor(final JCacheManager cacheManager, final RunSummary runSummary, CrawlerConfig config) {

        // Create new run processor.
        final RunProcessor processor = new RunProcessor(runSummary, cacheManager);

        // EPICS processor.
        processor.addProcessor(new EpicsLog(runSummary));

        // Scaler data processor.
        final ScalersEvioProcessor scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);
        processor.addProcessor(scalersProcessor);

        // Event log processor.
        processor.addProcessor(new EventTypeLog(runSummary));

        // Max files.
        if (config.maxFiles() != -1) {
            processor.setMaxFiles(config.maxFiles());
        }

        // Enable file caching.
        processor.useFileCache(config.useFileCache());

        // Set event printing interval.
        processor.setEventPrintInterval(config.eventPrintInterval());

        return processor;
    }

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

    /**
     * Get the event count from the current <code>EvioReader</code>.
     *
     * @param reader the current <code>EvioReader</code>
     * @return the event count
     * @throws IOException if there is a generic IO error
     * @throws EvioException if there is an EVIO related error
     */
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
            LOGGER.info("limiting files to max " + this.maxFiles);
            int toIndex = this.maxFiles;
            if (toIndex > files.size()) {
                toIndex = files.size();                
            }            
            files = files.subList(0, toIndex);
            LOGGER.info("using file list with size " + files.size());
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

    ScalersEvioProcessor getScalersProcessor() {
        for (final EvioEventProcessor processor : this.processors) {
            if (processor instanceof ScalersEvioProcessor) {
                return ScalersEvioProcessor.class.cast(processor);
            }
        }
        return null;
    }

    /**
     * Return <code>true</code> if a valid CODA <i>END</i> event can be located in the <code>EvioReader</code>'s current file.
     *
     * @param reader the EVIO reader
     * @return <code>true</code> if valid END event is located
     * @throws Exception if there are IO problems using the reader
     */
    boolean isEndOkay(final EvioReader reader) throws Exception {
        LOGGER.info("checking is END okay ...");

        boolean endOkay = false;

        // Go to second to last event for searching.
        reader.gotoEventNumber(reader.getEventCount() - 2);

        // Look for END event.
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
     * Process the run by executing the registered {@link org.hps.record.evio.EvioEventProcessor}s and performing special tasks such as the extraction
     * of start and end dates.
     * <p>
     * This method will also activate file caching, if enabled by the {@link #useFileCache} option.
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

            // Compute the event count for the file and store the value in the run summary's file list.
            LOGGER.info("getting event count for " + file.getPath() + "...");
            final int eventCount = this.computeEventCount(reader);
            this.runSummary.getEvioFileList().setEventCount(file, eventCount);
            LOGGER.info("set event count " + eventCount + " for " + file.getPath());

            // Process the events using the EVIO processors.
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
                final boolean endOkay = this.isEndOkay(reader);
                this.runSummary.setEndOkay(endOkay);
                LOGGER.info("endOkay set to " + endOkay);

                LOGGER.info("getting end date ...");
                final Date endDate = EvioFileUtilities.getRunEnd(file);
                this.runSummary.setEndDate(endDate);
                LOGGER.info("found end date " + endDate);
            }

            // Pull scaler data from EVIO processor into run summary.
            final ScalersEvioProcessor scalersProcessor = this.getScalersProcessor();
            if (scalersProcessor != null) {
                runSummary.setScalerData(scalersProcessor.getScalerData());
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
     * Since EVIO data files at JLAB are primarily kept on the MSS, running without this option enabled there will likely cause the job to fail.
     *
     * @param cacheFiles <code>true</code> to enabled file caching
     */
    void useFileCache(final boolean cacheFiles) {
        this.useFileCache = cacheFiles;
        LOGGER.config("file caching enabled");
    }
    
    /**
     * Process all the runs that were found.
     *
     * @param runs the run log containing the list of run summaries
     * @throws Exception if there is an error processing one of the runs
     */
    static void processRuns(JCacheManager cacheManager, final RunLog runs, CrawlerConfig config) throws Exception {
        
        // Configure max wait time of jcache manager.
        if (config.waitTime() != null && config.waitTime() > 0L) {
            cacheManager.setWaitTime(config.waitTime());
            LOGGER.config("JCacheManager max wait time set to " + config.waitTime());
        }                                
        
        // Process all of the runs that were found.
        for (final int run : runs.getSortedRunNumbers()) {
            
            // Get the run summary.
            RunSummary runSummary = runs.getRunSummary(run);
            
            // Clear the cache manager.
            if (config.useFileCache()) {
                cacheManager.clear();
            }

            // Create a processor to process all the EVIO events in the run.
            final RunProcessor runProcessor = RunProcessor.createRunProcessor(cacheManager, runSummary, config);

            for (final EvioEventProcessor processor : config.processors()) {
                runProcessor.addProcessor(processor);
                LOGGER.config("added extra EVIO processor " + processor.getClass().getName());
            }

            // Process all of the run's files.
            runProcessor.process();
        }
    }
}
