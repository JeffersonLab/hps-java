package org.hps.record.evio.crawler;

import java.io.File;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EvioFileMetaData;
import org.hps.record.evio.EvioFileMetaDataReader;
import org.hps.record.evio.EvioFileSequenceComparator;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerConfig;
import org.hps.record.triggerbank.TriggerConfigVariable;
import org.hps.rundb.RunSummaryImpl;
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
     * The cache manager.
     */
    private final JCacheManager cacheManager;

    /**
     * Processor for extracting EPICS information.
     */
    private final EpicsRunProcessor epicsProcessor;

    /**
     * The data source with the list of EVIO files to process.
     */
    private final EvioFileSource evioFileSource;

    /**
     * The EVIO event processing loop.
     */
    private final EvioLoop evioLoop = new EvioLoop();

    /**
     * The run summary information updated by running this processor.
     */
    private final RunSummaryImpl runSummary;

    /**
     * Processor for extracting scaler data.
     */
    private final ScalersEvioProcessor scalersProcessor;

    /**
     * Processor for extracting TI time offset.
     */
    private final TiTimeOffsetEvioProcessor triggerTimeProcessor;

    /**
     * Set to <code>true</code> to use file caching.
     */
    private boolean useFileCache;

    /**
     * Create a run processor.
     *
     * @param runSummary the run summary object for the run
     * @return the run processor
     */
    RunProcessor(final JCacheManager cacheManager, final RunSummaryImpl runSummary, boolean useFileCache) {

        this.runSummary = runSummary;
        this.cacheManager = cacheManager;

        // Set whether file caching from MSS is enabled.
        this.useFileCache = useFileCache;
        
        // Sort the list of EVIO files.
        Collections.sort(runSummary.getEvioFiles(), new EvioFileSequenceComparator());

        // Setup record loop.
        evioFileSource = new EvioFileSource(runSummary.getEvioFiles());
        evioLoop.setEvioFileSource(evioFileSource);

        // Add EPICS processor.
        epicsProcessor = new EpicsRunProcessor();
        evioLoop.addEvioEventProcessor(epicsProcessor);

        // Add scaler data processor.
        scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);
        evioLoop.addEvioEventProcessor(scalersProcessor);

        // Add processor for extracting TI time offset. 
        triggerTimeProcessor = new TiTimeOffsetEvioProcessor();
        evioLoop.addEvioEventProcessor(triggerTimeProcessor);
    }

    /**
     * Cache all files and wait for the operation to complete.
     * <p>
     * Potentially, this operation can take a very long time. This can be managed using the
     * {@link JCacheManager#setWaitTime(long)} method to set a timeout.
     */
    private void cacheFiles() {

        LOGGER.info("caching files from run " + this.runSummary.getRun());

        // Cache all the files and wait for the operation to complete.
        this.cacheManager.cache(this.runSummary.getEvioFiles());
        final boolean cached = this.cacheManager.waitForCache();

        // If the files weren't cached then die.
        if (!cached) {
            throw new RuntimeException("The cache process did not complete in time.");
        }

        LOGGER.info("done caching files from run " + this.runSummary.getRun());
    }

    /**
     * Process the run by executing the registered {@link org.hps.record.evio.EvioEventProcessor}s and extracting the
     * start and end dates.
     * <p>
     * This method will also execute file caching from MSS, if enabled by the {@link #useFileCache} option.
     *
     * @throws Exception if there is an error processing a file
     */
    void processRun() throws Exception {

        LOGGER.info("processing " + this.runSummary.getEvioFiles().size() + " files from run "
                + this.runSummary.getRun());

        // Cache files from MSS if this is enabled.
        if (this.useFileCache) {
            LOGGER.info("caching files from MSS");
            this.cacheFiles();
        }

        // Run processors over all files.
        LOGGER.info("looping over all events");
        evioLoop.loop(-1);

        // Get run start date.
        LOGGER.info("processing first file");
        this.processFirstFile();

        // Get run end date.
        LOGGER.info("processing last file");
        this.processLastFile();

        // Update run summary from processors.
        LOGGER.info("updating run summary");
        this.updateRunSummary();

        LOGGER.info("done processing run " + this.runSummary.getRun());
    }

    /**
     * Extract meta data from last file in run.
     */
    private void processLastFile() {
        final File lastEvioFile = runSummary.getEvioFiles().get(runSummary.getEvioFiles().size() - 1);
        LOGGER.info("getting meta data for " + lastEvioFile.getPath());
        final EvioFileMetaDataReader metaDataReader = new EvioFileMetaDataReader();
        final EvioFileMetaData metaData = metaDataReader.getMetaData(lastEvioFile);
        LOGGER.info(metaData.toString());
        if (metaData.getEndDate() == null) {
            throw new IllegalStateException("The end date is not set in the EVIO file meta data from "
                    + lastEvioFile.getPath());
        }
        LOGGER.info("setting unix end time to " + metaData.getEndDate().getTime() + " from meta data");
        runSummary.setEndDate(metaData.getEndDate());
        runSummary.setEndOkay(metaData.hasEnd());
    }

    /**
     * Extract meta data from first file in run.
     */
    private void processFirstFile() {
        final File firstEvioFile = runSummary.getEvioFiles().get(0);
        LOGGER.info("getting meta data for " + firstEvioFile.getPath());
        final EvioFileMetaDataReader metaDataReader = new EvioFileMetaDataReader();
        final EvioFileMetaData metaData = metaDataReader.getMetaData(firstEvioFile);
        LOGGER.info(metaData.toString());
        if (metaData.getStartDate() == null) {
            throw new IllegalStateException("The start date is not set in the EVIO file meta data from "
                    + firstEvioFile.getPath());
        }
        LOGGER.info("setting unix start time to " + metaData.getStartDate().getTime() + " from meta data");
        runSummary.setStartDate(metaData.getStartDate());
    }

    /**
     * Update the current run summary by copying data to it from the EVIO processors and the event loop.
     */
    private void updateRunSummary() {

        LOGGER.info("setting total events " + evioLoop.getTotalCountableConsumed());
        // Set total number of events from the event loop.
        runSummary.setTotalEvents((int) evioLoop.getTotalCountableConsumed());

        // Add scaler data from the scalers EVIO processor.
        LOGGER.info("adding " + this.scalersProcessor.getScalerData().size() + " scaler data objects");
        runSummary.setScalerData(this.scalersProcessor.getScalerData());

        // Add EPICS data from the EPICS EVIO processor.
        LOGGER.info("adding " + this.epicsProcessor.getEpicsData().size() + " EPICS data objects");
        runSummary.setEpicsData(this.epicsProcessor.getEpicsData());

        // Add trigger config from the trigger time processor.
        LOGGER.info("updating trigger config");
        TriggerConfig triggerConfig = new TriggerConfig();
        this.triggerTimeProcessor.updateTriggerConfig(triggerConfig);
        LOGGER.info("tiTimeOffset: " + triggerConfig.get(TriggerConfigVariable.TI_TIME_OFFSET.name()));
        System.out.println("tiTimeOffset: " + triggerConfig.get(TriggerConfigVariable.TI_TIME_OFFSET.name()));
        runSummary.setTriggerConfigInt(triggerConfig);
        
        LOGGER.getHandlers()[0].flush();
    }  
}
