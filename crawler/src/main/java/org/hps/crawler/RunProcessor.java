package org.hps.crawler;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EvioFileMetadata;
import org.hps.record.evio.EvioFileMetadataAdapter;
import org.hps.record.evio.EvioFileSequenceComparator;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerConfig;
import org.hps.record.triggerbank.TriggerConfigVariable;
import org.hps.run.database.RunSummaryImpl;
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
     * Record loop adapter for getting file metadata.
     */
    private final EvioFileMetadataAdapter metadataAdapter = new EvioFileMetadataAdapter();

    /**
     * Create a run processor.
     *
     * @param runSummary the run summary object for the run
     * @return the run processor
     */
    RunProcessor(final RunSummaryImpl runSummary) {

        this.runSummary = runSummary;

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
        
        // Add file metadata processor.
        evioLoop.addRecordListener(metadataAdapter);
        evioLoop.addLoopListener(metadataAdapter);
    }

    /**
     * Extract meta data from first file in run.
     */
    private void processFirstFile() {        
        final EvioFileMetadata metadata = metadataAdapter.getEvioFileMetadata().get(0);
        if (metadata == null) {
            throw new IllegalStateException("No meta data exists for first file.");
        }
        LOGGER.info("first file metadata: " + metadata.toString());
        if (metadata.getStartDate() == null) {
            throw new IllegalStateException("The start date is not set in the metadata.");
        }
        LOGGER.info("setting unix start time to " + metadata.getStartDate().getTime() + " from meta data");
        runSummary.setStartDate(metadata.getStartDate());
    }

    /**
     * Extract meta data from last file in run.
     */
    private void processLastFile() {
        LOGGER.info("looking for " + runSummary.getEvioFiles().get(runSummary.getEvioFiles().size() - 1).getPath() + " metadata");
        LOGGER.getHandlers()[0].flush();
        final EvioFileMetadata metadata = this.metadataAdapter.getEvioFileMetadata().get(this.metadataAdapter.getEvioFileMetadata().size() - 1);
        if (metadata == null) {
            throw new IllegalStateException("Failed to find metadata for last file.");
        }
        LOGGER.info("last file metadata: " + metadata.toString());
        if (metadata.getEndDate() == null) {
            throw new IllegalStateException("The end date is not set in the metadata.");
        }
        LOGGER.info("setting unix end time to " + metadata.getEndDate().getTime() + " from meta data");
        runSummary.setEndDate(metadata.getEndDate());
        LOGGER.info("setting has END to " + metadata.hasEnd());
        runSummary.setEndOkay(metadata.hasEnd());
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

        // Run processors over all files.
        LOGGER.info("looping over all events");
        evioLoop.loop(-1);
                
        LOGGER.info("got " + metadataAdapter.getEvioFileMetadata().size() + " metadata objects from loop");
        LOGGER.getHandlers()[0].flush();

        // Set start date from first file.
        LOGGER.info("processing first file");
        this.processFirstFile();

        // Set end date from last file.
        LOGGER.info("processing last file");
        this.processLastFile();

        // Update run summary from processors.
        LOGGER.info("updating run summary");
        this.updateRunSummary();

        LOGGER.info("run processor done with run " + this.runSummary.getRun());
    }

    /**
     * Update the current run summary by copying data to it from the EVIO processors and the event loop.
     */
    private void updateRunSummary() {

        // Set total number of events from the event loop.
        LOGGER.info("setting total events " + evioLoop.getTotalCountableConsumed());
        runSummary.setTotalEvents((int) evioLoop.getTotalCountableConsumed());

        // Add scaler data from the scalers EVIO processor.
        LOGGER.info("adding " + this.scalersProcessor.getScalerData().size() + " scaler data objects");
        runSummary.setScalerData(this.scalersProcessor.getScalerData());

        // Add EPICS data from the EPICS EVIO processor.
        LOGGER.info("adding " + this.epicsProcessor.getEpicsData().size() + " EPICS data objects");
        runSummary.setEpicsData(this.epicsProcessor.getEpicsData());

        // Add trigger config from the trigger time processor.
        LOGGER.info("updating trigger config");
        final TriggerConfig triggerConfig = new TriggerConfig();
        this.triggerTimeProcessor.updateTriggerConfig(triggerConfig);
        LOGGER.info("tiTimeOffset: " + triggerConfig.get(TriggerConfigVariable.TI_TIME_OFFSET));
        runSummary.setTriggerConfigInt(triggerConfig);

        LOGGER.getHandlers()[0].flush();
    }        
    
    /**
     * Get list of metadata created by processing the files.
     * 
     * @return the list of metadata
     */
    List<EvioFileMetadata> getEvioFileMetaData() {
        return this.metadataAdapter.getEvioFileMetadata();
    }
}
