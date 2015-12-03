package org.hps.run.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.Dataset;
import org.hps.record.AbstractRecordProcessor;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.DAQConfigEvioProcessor;
import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerUtilities;
import org.hps.record.scalers.ScalerUtilities.LiveTimeIndex;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.svt.SvtConfigData;
import org.hps.record.svt.SvtConfigEvioProcessor;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.util.FileUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Builds a complete {@link RunSummary} object from various data sources, including the data catalog and the run
 * spreadsheet, so that it is ready to be inserted into the run database using the DAO interfaces.  This class also 
 * extracts EPICS data, scaler data, trigger config and SVT config information from all of the EVIO files in a run.
 * <p>
 * The setters and some other methods follow the builder pattern and so can be chained by the caller.
 * 
 * @author Jeremy McCormick, SLAC
 * @see RunSummary
 * @see RunSummaryImpl
 */
final class RunDatabaseBuilder {

    /**
     * Package logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RunDatabaseBuilder.class.getPackage().getName());

    /**
     * Database connection.
     */
    private ConnectionParameters connectionParameters;

    /**
     * Data catalog client API.
     */
    private DatacatClient datacatClient;

    /**
     * Detector name for initializing conditions system.
     */
    private String detectorName;

    /**
     * Dry run to not perform database updates (off by default).
     */
    private boolean dryRun = false;

    /**
     * List of EPICS data from the run.
     */
    private List<EpicsData> epicsData;

    /**
     * Map of EVIO files to their dataset objects.
     */
    private Map<File, Dataset> evioDatasets;

    /**
     * List of EVIO files.
     */
    private List<File> evioFiles;
    
    /**
     * List of EVIO files with cache path/
     */
    private List<File> cacheFiles;

    /**
     * Allow replacement of information in the database (off by default).
     */
    private boolean replace = false;

    /**
     * Run summary to be updated.
     */
    private RunSummaryImpl runSummary;

    /**
     * List of scaler data from the run.
     */
    private List<ScalerData> scalerData;

    /**
     * Skip full EVIO file processing (off by default).
     */
    private boolean skipEvioProcessing = false;

    /**
     * Path to run spreadsheet CSV file (not used by default).
     */
    private File spreadsheetFile;

    /**
     * List of SVT configuration bank data.
     */
    private List<SvtConfigData> svtConfigs;
    
    /**
     * The trigger config object.
     */
    private TriggerConfig config;
    
    /**
     * Reload run data after insert for debugging.
     */
    private boolean reload;
    
    /**
     * Reload state for the current run number (used for testing after a database insert).
     */
    static void reload(Connection connection, int run) {
        
        RunManager runManager = new RunManager(connection);
        runManager.setRun(run);

        RunSummary runSummary = runManager.getRunSummary();

        LOGGER.info("loaded run summary ..." + '\n' + runSummary);

        LOGGER.info("loaded " + runManager.getEpicsData(EpicsType.EPICS_2s).size() + " EPICS 2S records");
        LOGGER.info("loaded " + runManager.getEpicsData(EpicsType.EPICS_20s).size() + " EPICS 20S records");

        List<ScalerData> scalerData = runManager.getScalerData();
        LOGGER.info("loaded " + scalerData.size() + " scaler records");

        List<SvtConfigData> svtConfigs = runManager.getSvtConfigData();
        LOGGER.info("loaded " + svtConfigs.size() + " SVT configurations");
            
        LOGGER.info("printing DAQ config ...");
        DAQConfig daqConfig = runManager.getDAQConfig();
        daqConfig.printConfig();
        
        runManager.closeConnection();
    }
                      
    /**
     * Create an empty run summary.
     * 
     * @param run the run number
     * @return the empty run summary
     */
    RunDatabaseBuilder createRunSummary(int run) {
        runSummary = new RunSummaryImpl(run);
        return this;
    }

    /**
     * Find EVIO files in the data catalog.
     */
    private void findEvioDatasets() {
        LOGGER.info("finding EVIO datasets for run " + getRun());
        
        // Metadata to return from search.
        final Set<String> metadata = new LinkedHashSet<String>();
        metadata.add("runMin");
        metadata.add("eventCount");
        
        // Initialize map of files to datasets.
        evioDatasets = new HashMap<File, Dataset>();
        
        // Find datasets in the datacat using a search.
        final List<Dataset> datasets = datacatClient.findDatasets(
                "data/raw",
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + getRun(), 
                metadata);
        if (datasets.isEmpty()) {
            // No files for the run in datacat is a fatal error.
            throw new IllegalStateException("No EVIO datasets for run " + getRun() + " were found in the data catalog.");
        }
        
        // Map files to datasets.
        for (final Dataset dataset : datasets) {
            evioDatasets.put(new File(dataset.getLocations().get(0).getResource()), dataset);
        }
        
        // Create the list of sorted EVIO files.
        evioFiles = new ArrayList<File>();
        evioFiles.addAll(evioDatasets.keySet());
        EvioFileUtilities.sortBySequence(evioFiles);
        
        // Create a list of files with cache paths in case running at JLAB.
        cacheFiles = new ArrayList<File>();
        for (File file : evioFiles) {
            if (FileUtilities.isMssFile(file)) {
                cacheFiles.add(FileUtilities.getCachedFile(file));
            } else {
                cacheFiles.add(file);
            }
        }        
        
        LOGGER.info("found " + evioFiles.size() + " EVIO file(s) for run " + runSummary.getRun());
    }
   
    /**
     * Get the current run number from the run summary.
     * 
     * @return the run number from the run summary
     */
    int getRun() {
        return runSummary.getRun();
    }
    
    /**
     * Insert the run data into the database using the current connection.
     */
    private void insertRun(Connection connection) {

        LOGGER.info("inserting run " + runSummary.getRun() + " into db");

        // Create DAO factory.
        final RunDatabaseDaoFactory runFactory = new RunDatabaseDaoFactory(connection);

        // Insert the run summary record.
        LOGGER.info("inserting run summary");
        runFactory.createRunSummaryDao().insertRunSummary(runSummary);

        // Insert the EPICS data.
        if (epicsData != null) {
            LOGGER.info("inserting EPICS data");
            runFactory.createEpicsDataDao().insertEpicsData(epicsData);
        } else {
            LOGGER.warning("no EPICS data to insert");
        }

        // Insert the scaler data.
        if (scalerData != null) {
            LOGGER.info("inserting scaler data");
            runFactory.createScalerDataDao().insertScalerData(scalerData, getRun());
        } else {
            LOGGER.warning("no scaler data to insert");
        }

        // Insert SVT config data.
        if (this.svtConfigs != null) {
            LOGGER.info("inserting SVT config");
            runFactory.createSvtConfigDao().insertSvtConfigs(svtConfigs, getRun());
        } else {
            LOGGER.warning("no SVT config to insert");
        }
        
        // Insert trigger config data.
        if (this.config != null) {
            LOGGER.info("inserting trigger config");
            runFactory.createTriggerConfigDao().insertTriggerConfig(config, getRun());
        } else {
            LOGGER.warning("no trigger config to inesrt");
        }
                       
        LOGGER.info("done inserting run " + getRun());
    }
          
    /**
     * Print summary information to the log.
     */
    private void printSummary() {
        LOGGER.info("built run summary ..." + '\n' + runSummary.toString());
        if (epicsData != null) {
            LOGGER.info("found " + epicsData.size() + " EPICS data records");
        } else {
            LOGGER.info("no EPICS data");
        }
        if (scalerData != null) {
            LOGGER.info("found " + scalerData.size() + " scalers");
        } else {
            LOGGER.info("no scaler data");
        }
        if (svtConfigs != null) {
            for (SvtConfigData config : svtConfigs) {
                try {
                    LOGGER.info("SVT XML config with timestamp " + config.getTimestamp() + " ..." + config.toXmlString());
                } catch (Exception e) {
                    LOGGER.warning("Could not print config!  Probably bad string data.");
                }
            }
        } else {
            LOGGER.info("no SVT config");
        }
        if (config != null) {
            for (Entry<Integer, String> entry : config.getData().entrySet()) {
                LOGGER.info("trigger config data " + entry.getKey() + " with timestamp " + config.getTimestamp() + " ..." + entry.getValue());
            }
        } else {
            LOGGER.info("no trigger config");
        }
    }    

    /**
     * Process all the EVIO files in the run and set information on the current run summary.
     */
    private void processEvioFiles() {

        LOGGER.fine("processing EVIO files");

        if (evioFiles == null || evioFiles.isEmpty()) {
            throw new IllegalStateException("No EVIO files were found.");
        }

        if (detectorName == null) {
            throw new IllegalStateException("The detector name was not set.");
        }

        // Initialize the conditions system because the DAQ config processor needs it.
        try {
            DatabaseConditionsManager dbManager = DatabaseConditionsManager.getInstance();
            DatabaseConditionsManager.getInstance().setDetector(detectorName, runSummary.getRun());
            dbManager.freeze();
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }

        // List of processors to execute in the job.
        ArrayList<AbstractRecordProcessor<EvioEvent>> processors = new ArrayList<AbstractRecordProcessor<EvioEvent>>();

        // Processor to get scaler data.
        ScalersEvioProcessor scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);
        processors.add(scalersProcessor);
        
        // Processor for getting EPICS data.
        EpicsRunProcessor epicsProcessor = new EpicsRunProcessor();
        processors.add(epicsProcessor);

        // Processor for calculating the TI time offset.
        TiTimeOffsetEvioProcessor tiProcessor = new TiTimeOffsetEvioProcessor();
        processors.add(tiProcessor);

        // Processor for getting DAQ config.
        DAQConfigEvioProcessor daqProcessor = new DAQConfigEvioProcessor();
        processors.add(daqProcessor);

        // Processor for getting the SVT XML config.
        SvtConfigEvioProcessor svtProcessor = new SvtConfigEvioProcessor();
        processors.add(svtProcessor);

        // Run the job using the EVIO loop.
        EvioLoop loop = new EvioLoop();
        loop.addProcessors(processors);
        EvioFileSource source = new EvioFileSource(cacheFiles);
        loop.setEvioFileSource(source);
        loop.loop(-1);

        // Set livetime field values.
        updateLivetimes(scalersProcessor);

        // Set TI time offset.
        runSummary.setTiTimeOffset(tiProcessor.getTiTimeOffset());

        // Set EPICS data list.
        epicsData = epicsProcessor.getEpicsData();

        // Set scalers list.
        scalerData = scalersProcessor.getScalerData();

        // Set SVT config data strings.
        svtConfigs = svtProcessor.getSvtConfigs();
        
        // Set trigger config object.
        if (!daqProcessor.getTriggerConfigData().isEmpty()) {            
            config = new TriggerConfig(daqProcessor.getTriggerConfigData(), daqProcessor.getTimestamp());
        }

        LOGGER.info("done processing EVIO files");
    }

    /**
     * Run the job to build the information for the database and perform an update (if not dry run).
     * 
     * @return this object
     */
    RunDatabaseBuilder run() {
        
        LOGGER.info("building run " + getRun());
        
        if (this.runSummary == null) {
            throw new IllegalStateException("The run summary was never created.");
        }        
        
        if (this.datacatClient == null) {
            throw new IllegalStateException("The datacat client was not set.");
        }
                
        // Find EVIO datasets in the datacat.
        findEvioDatasets();

        // Set total number of files.
        updateTotalFiles();

        // Set GO and PRESTART timestamps.
        updateStartTimestamps();

        // Set END timestamp.
        updateEndTimestamp();

        // Set total number of events.
        updateTotalEvents();

        // Calculate trigger rate.
        updateTriggerRate();
                
        // Run EVIO job if enabled.
        if (!this.skipEvioProcessing) {
            processEvioFiles();
        } else {
            LOGGER.info("EVIO file processing is skipped.");
        }

        // Get extra info from spreadsheet if enabled.
        if (this.spreadsheetFile != null) {
            updateFromSpreadsheet();
        } else {
            LOGGER.info("Run spreadsheet not used.");
        }

        // Print out summary info to the log before updating database.
        printSummary();

        if (!dryRun) {
            // Update the database.
            updateDatabase();
            
            if (reload) {
                LOGGER.info("reloading data for run " + getRun() + " ...");
                reload(connectionParameters.createConnection(), getRun());
            }
            
        } else {
            // Dry run so database is not updated.
            LOGGER.info("Dry run enabled so no updates were performed.");
        }
                        
        return this;
    }

    /**
     * Set the database connection to the run database.
     * 
     * @param connection the database connection to the run database
     * @return this object
     */
    RunDatabaseBuilder setConnectionParameters(ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        return this;
    }
    
    /**
     * Set the datacat client for querying the data catalog.
     * 
     * @param datacatClient the datacat client
     * @return this object
     */
    RunDatabaseBuilder setDatacatClient(DatacatClient datacatClient) {
        this.datacatClient = datacatClient;
        return this;
    }

    /**
     * Set the detector name for initializing the conditions system.
     * 
     * @param detectorName the detector name for initializing the conditions system
     * @return this object
     */
    RunDatabaseBuilder setDetectorName(String detectorName) {
        this.detectorName = detectorName;
        LOGGER.config("detector = " + this.detectorName);
        return this;
    }

    /**
     * Set dry run which will not update the database.
     * 
     * @param dryRun <code>true</code> to perform dry run
     * @return this object
     */
    RunDatabaseBuilder setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        LOGGER.config("dryRun = " + this.dryRun);
        return this;
    }
    
    /**
     * Set whether data should be reloaded at end (as debug check).
     * 
     * @param reload <code>true</code> to reload data at end of job
     * @return this object
     */
    RunDatabaseBuilder setReload(boolean reload) {
        this.reload = reload;
        return this;
    }

    /**
     * Enable replacement of existing records in the database.
     * 
     * @param replace <code>true</code> to allow replacement of records
     * @return this object
     */
    RunDatabaseBuilder setReplace(boolean replace) {
        this.replace = replace;
        LOGGER.config("replace = " + this.replace);
        return this;
    }

    /**
     * Set the path to the run spreadsheet CSV file from Google Docs.
     * 
     * @param spreadsheetFile spreadsheet CSV file (can be <code>null</code>)
     * @return this object
     */
    RunDatabaseBuilder setSpreadsheetFile(File spreadsheetFile) {
        this.spreadsheetFile = spreadsheetFile;
        if (this.spreadsheetFile != null) {
            LOGGER.config("spreadsheetFile = " + this.spreadsheetFile.getPath());
        }
        return this;
    }
    
    /**
     * Set whether full EVIO file processing should occur to extract EPICS data, etc. 
     * <p>
     * Even if this is disabled, the first and last EVIO files will still be processed
     * for timestamps.
     * 
     * @param skipEvioFileProcessing <code>true</code> to disable full EVIO file processing
     * @return this object
     */
    RunDatabaseBuilder skipEvioProcessing(boolean skipEvioProcessing) {
        this.skipEvioProcessing = skipEvioProcessing;
        LOGGER.config("skipEvioFileProcessing = " + this.skipEvioProcessing);
        return this;
    }

    /**
     * Update the database after the run information has been created.
     */
    private void updateDatabase() {

        LOGGER.fine("updating the run database");
        
        // Initialize the run manager.
        Connection connection = connectionParameters.createConnection();
        RunManager runManager = new RunManager(connection);
        runManager.setRun(runSummary.getRun());
        
        // Turn off autocommit to start transaction.
        try {
            connection.setAutoCommit(false);

            // Does run exist?
            if (runManager.runExists()) {
            
                LOGGER.info("run already exists");
            
                // If replacement is not enabled and run exists, then this is a fatal exception.
                if (!replace) {
                    throw new RuntimeException("Run already exists (use -x option to enable replacement).");
                }

                // Delete the run so insert statements can be used to rebuild it.
                LOGGER.info("deleting existing run");
                runManager.deleteRun();
            }

            // Insert the run data into the database.
            LOGGER.info("inserting the run data");
            insertRun(connection);
        
            // Commit the transaction.                                 
            LOGGER.info("committing to run db ...");
            connection.commit();
            LOGGER.info("done committing");
            
        } catch (Exception e1) {
            try {
                LOGGER.log(Level.SEVERE, "Error occurred updating database; rolling back transaction...", e1);
                connection.rollback();
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }        

        // Close the database connection.
        runManager.closeConnection();
    }

    /**
     * Update the run summary's end timestamp.
     */
    private void updateEndTimestamp() {
        LOGGER.info("updating end timestamp");
        IntBankDefinition headBankDefinition = new IntBankDefinition(HeadBankData.class, new int[] {0x2e, 0xe10f});
        File lastEvioFile = cacheFiles.get(cacheFiles.size() - 1);
        EvioReader reader = null;
        Integer endTimestamp = null;
        try {
            reader = EvioFileUtilities.open(lastEvioFile, true);
            EvioEvent evioEvent = reader.parseNextEvent();
            while (evioEvent != null) {
                if (EventTagConstant.END.matches(evioEvent)) {
                    endTimestamp = EvioEventUtilities.getControlEventData(evioEvent)[0];
                    LOGGER.fine("found END timestamp " + endTimestamp);
                    break;
                }
                BaseStructure headBank = headBankDefinition.findBank(evioEvent);
                if (headBank != null) {
                    if (headBank.getIntData()[0] != 0) {
                        endTimestamp = headBank.getIntData()[0];
                    }
                }
                evioEvent = reader.parseNextEvent();
            }
        } catch (IOException | EvioException e) {
            throw new RuntimeException("Error reading first EVIO file.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        runSummary.setEndTimestamp(endTimestamp);
        LOGGER.fine("end timestamp set to " + endTimestamp);
    }

    /**
     * Update the current run summary from information in the run spreadsheet.
     * 
     * @param spreadsheetFile file object pointing to the run spreadsheet (CSV format)
     * @return this object
     */
    private void updateFromSpreadsheet() {       
        LOGGER.fine("updating from spreadsheet file " + spreadsheetFile.getPath());
        RunSpreadsheet runSpreadsheet = new RunSpreadsheet(spreadsheetFile);
        RunData data = runSpreadsheet.getRunMap().get(runSummary.getRun());        
        if (data != null) {
            LOGGER.info("found run data ..." + '\n' + data.getRecord());
            
            // Trigger config name.
            String triggerConfigName = data.getRecord().get("trigger_config");
            if (triggerConfigName != null) {
                runSummary.setTriggerConfigName(triggerConfigName);
                LOGGER.info("set trigger config name <" + runSummary.getTriggerConfigName() + "> from spreadsheet");
            }
            
            // Notes.
            String notes = data.getRecord().get("notes");
            if (notes != null) {
                runSummary.setNotes(notes);
                LOGGER.info("set notes <" + runSummary.getNotes() + "> from spreadsheet");
            }
            
            // Target.
            String target = data.getRecord().get("target");
            if (target != null) {
                runSummary.setTarget(target);
                LOGGER.info("set target <" + runSummary.getTarget() + "> from spreadsheet");
            }
        } else {
            LOGGER.warning("No record for this run was found in spreadsheet.");
        }
    }

    /**
     * Calculate the DAQ livetime measurements from the last scaler data bank.
     * 
     * @param scalersProcessor the EVIO scaler data processor
     */
    private void updateLivetimes(ScalersEvioProcessor scalersProcessor) {
        LOGGER.fine("updating livetime calculations");
        ScalerData scalers = scalersProcessor.getCurrentScalerData();
        if (scalers == null) {
            throw new IllegalStateException("No scaler data was found by the EVIO processor.");
        }
        double[] livetimes = ScalerUtilities.getLiveTimes(scalers);
        runSummary.setLivetimeClock(livetimes[LiveTimeIndex.CLOCK.ordinal()]);
        runSummary.setLivetimeFcupTdc(livetimes[LiveTimeIndex.FCUP_TDC.ordinal()]);
        runSummary.setLivetimeFcupTrg(livetimes[LiveTimeIndex.FCUP_TRG.ordinal()]);
        LOGGER.info("clock livetime = " + runSummary.getLivetimeClock());
        LOGGER.info("fcup tdc livetime = " + runSummary.getLivetimeFcupTdc());
        LOGGER.info("fcup trg livetime = " + runSummary.getLivetimeFcupTrg());
    }

    /**
     * Update the starting timestamps from the first EVIO file.
     */
    private void updateStartTimestamps() {
        LOGGER.fine("updating start timestamps");
        File firstEvioFile = cacheFiles.get(0);
        int sequence = EvioFileUtilities.getSequenceFromName(firstEvioFile);
        if (sequence != 0) {
            LOGGER.warning("first file does not have sequence 0");
        }
        EvioReader reader = null;
        try {
            reader = EvioFileUtilities.open(firstEvioFile, true);
            EvioEvent evioEvent = reader.parseNextEvent();
            Integer prestartTimestamp = null;
            Integer goTimestamp = null;
            while (evioEvent != null) {
                if (EventTagConstant.PRESTART.matches(evioEvent)) {
                    prestartTimestamp = EvioEventUtilities.getControlEventData(evioEvent)[0];
                } else if (EventTagConstant.GO.matches(evioEvent)) {
                    goTimestamp = EvioEventUtilities.getControlEventData(evioEvent)[0];
                }
                if (prestartTimestamp != null && goTimestamp != null) {
                    break;
                }
                evioEvent = reader.parseNextEvent();
            }
            runSummary.setPrestartTimestamp(prestartTimestamp);
            runSummary.setGoTimestamp(goTimestamp);
        } catch (IOException | EvioException e) {
            throw new RuntimeException("Error reading first EVIO file.", e);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        LOGGER.info("PRESTART timestamp set to " + runSummary.getPrestartTimestamp());
        LOGGER.info("GO timestamp set to " + runSummary.getGoTimestamp());
    }

    /**
     * Update the total number of events.
     */
    private void updateTotalEvents() {
        LOGGER.fine("updating total events");
        int totalEvents = 0;
        for (Entry<File, Dataset> entry : evioDatasets.entrySet()) {
            totalEvents += entry.getValue().getLocations().get(0).getEventCount();
        }
        runSummary.setTotalEvents(totalEvents);
        LOGGER.info("total events set to " + runSummary.getTotalEvents());
    }

    /**
     * Update the total number of EVIO files in the run.
     */
    private void updateTotalFiles() {
        LOGGER.fine("updating total files");
        // Set number of files from datacat query.
        runSummary.setTotalFiles(evioFiles.size());
        LOGGER.info("total files set to " + runSummary.getTotalFiles());
    }

    /**
     * Update the trigger rate.
     */
    private void updateTriggerRate() {
        LOGGER.fine("updating trigger rate");
        if (runSummary.getEndTimestamp() != null && runSummary.getGoTimestamp() != null) {
            double triggerRate = ((double) runSummary.getTotalEvents() / ((double) runSummary.getEndTimestamp() - (double) runSummary
                    .getGoTimestamp())) / 1000.;
            runSummary.setTriggerRate(triggerRate);
            LOGGER.info("trigger rate set to " + runSummary.getTriggerRate());
        } else {
            LOGGER.warning("Skipped trigger rate calculation because END or GO timestamp is missing.");
        }
    }
}
