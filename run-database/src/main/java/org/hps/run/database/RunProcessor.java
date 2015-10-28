package org.hps.run.database;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerConfig;
import org.hps.record.triggerbank.TriggerConfigVariable;

/**
 * Processes EVIO files from a run and extracts meta data for updating the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunProcessor {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RunProcessor.class.getPackage().getName());

    /**
     * Processor for extracting EPICS information.
     */
    private EpicsRunProcessor epicsProcessor;

    /**
     * The data source with the list of EVIO files to process.
     */
    private final EvioFileSource evioFileSource;

    /**
     * The EVIO event processing loop.
     */
    private final EvioLoop evioLoop = new EvioLoop();

    /**
     * Processor for extracting scaler data.
     */
    private ScalersEvioProcessor scalersProcessor;

    /**
     * Processor for extracting TI time offset.
     */
    private TiTimeOffsetEvioProcessor triggerTimeProcessor;
    
    /**
     * The run summary for the run.
     */
    private RunSummaryImpl runSummary;

    /**
     * List of EVIO files in the run. 
     */
    private List<File> evioFiles;

    /**
     * Create a run processor.
     *
     * @param runSummary the run summary object for the run
     * @return the run processor
     */
    public RunProcessor(RunSummaryImpl runSummary, List<File> evioFiles) {
        if (runSummary == null) {
            throw new IllegalArgumentException("The run summary is null.");
        }
        if (evioFiles == null) {
            throw new IllegalArgumentException("The evio file list is null.");
        }
        if (evioFiles.isEmpty()) {
            throw new IllegalArgumentException("No EVIO files found in file set.");
        }

        this.runSummary = runSummary;
        this.evioFiles = evioFiles;

        // Setup record loop.
        evioFileSource = new EvioFileSource(evioFiles);
        evioLoop.setEvioFileSource(evioFileSource);       
    }
    
    public void addEpicsProcessor() {
        // Add EPICS processor.
        this.epicsProcessor = new EpicsRunProcessor();
        evioLoop.addEvioEventProcessor(epicsProcessor);
    }
    
    public void addScalerProcessor() {
        // Add scaler data processor.
        scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);
        evioLoop.addEvioEventProcessor(scalersProcessor);
    }
    
    public void addTriggerTimeProcessor() {
        // Add processor for extracting TI time offset.
        triggerTimeProcessor = new TiTimeOffsetEvioProcessor();
        evioLoop.addEvioEventProcessor(triggerTimeProcessor);
    }

    /**
     * Process the run by executing the registered {@link org.hps.record.evio.EvioEventProcessor}s
     *
     * @throws Exception if there is an error processing a file
     */
    public void processRun() throws Exception {

        LOGGER.info("processing " + this.evioFiles.size() + " files from run "
                + this.runSummary.getRun());

        // Run processors over all files.
        LOGGER.info("looping over all events");
        evioLoop.loop(-1);
                
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

        if (scalersProcessor != null) {
            // Add scaler data from the scalers EVIO processor.
            LOGGER.info("adding " + this.scalersProcessor.getScalerData().size() + " scaler data objects");
            runSummary.setScalerData(this.scalersProcessor.getScalerData());
        }

        if (epicsProcessor != null) {
            // Add EPICS data from the EPICS EVIO processor.
            LOGGER.info("adding " + this.epicsProcessor.getEpicsData().size() + " EPICS data objects");
            runSummary.setEpicsData(this.epicsProcessor.getEpicsData());
        }

        if (triggerTimeProcessor != null) {
            // Add trigger config from the trigger time processor.
            LOGGER.info("updating trigger config");
            final TriggerConfig triggerConfig = new TriggerConfig();
            this.triggerTimeProcessor.updateTriggerConfig(triggerConfig);
            LOGGER.info("tiTimeOffset: " + triggerConfig.get(TriggerConfigVariable.TI_TIME_OFFSET));
            runSummary.setTriggerConfig(triggerConfig);
        }
    }        
}
