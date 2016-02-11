package org.hps.run.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.record.AbstractRecordProcessor;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.TriggerConfigEvioProcessor;
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
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.TiTimeOffsetCalculator;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerConfigData;
import org.hps.record.triggerbank.TriggerConfigData.Crate;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.shared.DatasetLocation;

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
     * Detector name for initializing conditions system.
     */
    private String detectorName;

    /**
     * Enable dry run to not perform database updates (off by default).
     */
    private boolean dryRun = false;

    /**
     * List of EPICS data read from the EVIO files.
     */
    private List<EpicsData> epicsData;

    /**
     * List of EVIO datasets found in the datacat for the run.
     */
    private List<DatasetModel> evioDatasets;

    /**
     * List of EVIO files for processing.
     */
    private List<File> evioFiles;
       
    /**
     * Allow replacement of information in the database (off by default).
     */
    private boolean replace = false;

    /**
     * Run summary to be updated.
     */
    private RunSummaryImpl runSummary;

    /**
     * List of scaler data read from the EVIO files.
     */
    private List<ScalerData> scalerData;

    /**
     * Skip full EVIO file processing (off by default).
     */
    private boolean skipEvioProcessing = false;

    /**
     * Run spreadsheet CSV file with supplementary information (not used by default).
     */
    private File spreadsheetFile;

    /**
     * List of SVT configuration bank data.
     */
    //private List<SvtConfigData> svtConfigs;
    
    /**
     * The trigger config object.
     */
    private TriggerConfigData config;
    
    /**
     * Reload run data after insert for debugging.
     */
    private boolean reload;
    
    /**
     * Data catalog client interface.
     */
    private Client datacatClient;
    
    /**
     * Datacat site to use.
     */
    private String site;
    
    /**
     * Default folder for file search.
     */
    private String folder;
    
    /**
     * Reload state for the current run number for testing.
     */
    static void reload(Connection connection, int run) {
        
        RunManager runManager = new RunManager(connection);
        runManager.setRun(run);

        RunSummary runSummary = runManager.getRunSummary();

        LOGGER.info("loaded run summary ..." + '\n' + runSummary);

        LOGGER.info("loaded " + runManager.getEpicsData(EpicsType.EPICS_2S).size() + " EPICS 2S records");
        LOGGER.info("loaded " + runManager.getEpicsData(EpicsType.EPICS_20S).size() + " EPICS 20S records");

        List<ScalerData> scalerData = runManager.getScalerData();
        LOGGER.info("loaded " + scalerData.size() + " scaler records");

        //List<SvtConfigData> svtConfigs = runManager.getSvtConfigData();
        //LOGGER.info("loaded " + svtConfigs.size() + " SVT configurations");
            
        LOGGER.info("printing DAQ config ...");
        DAQConfig daqConfig = runManager.getDAQConfig();
        daqConfig.printConfig(System.out);
        
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
     * Create the EVIO file list from the data catalog datasets.
     */
    private void createEvioFileList() {
        this.evioFiles = new ArrayList<File>();
        
        for (DatasetModel dataset : this.evioDatasets) {            
            String resource = 
                    ((DatasetWithViewModel) dataset).getViewInfo().getLocations().iterator().next().getResource();
            File file = new File(resource);
            if (file.getPath().startsWith("/mss")) {
                file = new File("/cache" + resource);
            }
            LOGGER.info("adding EVIO file " + file.getPath() + " from dataset " + dataset.getName());
            this.evioFiles.add(file);
        }
        EvioFileUtilities.sortBySequence(this.evioFiles);
    }

    /**
     * Find EVIO files in the data catalog.
     */
    private void findEvioDatasets() {
        
        LOGGER.info("finding EVIO datasets for run " + getRun() + " in folder " + this.folder + " at site " + this.site);
        
        DatasetResultSetModel results = datacatClient.searchForDatasets(
                this.folder,
                "current",
                this.site,
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + getRun(),
                null,
                null
                );
        
        this.evioDatasets = results.getResults();
        
        if (this.evioDatasets.isEmpty()) {
            throw new RuntimeException("No EVIO datasets found in data catalog.");
        }
    }
    
    /**
     * Get the folder for dataset search.
     * 
     * @return the folder for dataset search
     */
    String folder() {
        return folder;
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
        final DaoProvider runFactory = new DaoProvider(connection);

        // Insert the run summary record.
        LOGGER.info("inserting run summary");
        runFactory.getRunSummaryDao().insertRunSummary(runSummary);

        // Insert the EPICS data.
        if (epicsData != null && !epicsData.isEmpty()) {
            LOGGER.info("inserting EPICS data");
            runFactory.getEpicsDataDao().insertEpicsData(epicsData, getRun());
        } else {
            LOGGER.warning("no EPICS data to insert");
        }

        // Insert the scaler data.
        if (scalerData != null) {
            LOGGER.info("inserting scaler data");
            runFactory.getScalerDataDao().insertScalerData(scalerData, getRun());
        } else {
            LOGGER.warning("no scaler data to insert");
        }

        // Insert SVT config data.
        //if (this.svtConfigs != null) {
        //    LOGGER.info("inserting SVT config");
        //    runFactory.getSvtConfigDao().insertSvtConfigs(svtConfigs, getRun());
        //} else {
        //    LOGGER.warning("no SVT config to insert");
        //}
        
        // Insert trigger config data.
        if (this.config != null) {
            LOGGER.info("inserting trigger config");
            runFactory.getTriggerConfigDao().insertTriggerConfig(config, getRun());
        } else {
            LOGGER.warning("no trigger config to insert");
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
        /*
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
        */
        if (config != null) {
            for (Entry<Crate, String> entry : config.getData().entrySet()) {
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

        if (detectorName == null) {
            throw new IllegalStateException("The detector name was not set.");
        }

        // Initialize the conditions system because the DAQ config processor needs it.
        /*
        try {
            DatabaseConditionsManager dbManager = DatabaseConditionsManager.getInstance();
            DatabaseConditionsManager.getInstance().setDetector(detectorName, runSummary.getRun());
            dbManager.freeze();
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        */

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
        TriggerConfigEvioProcessor daqProcessor = new TriggerConfigEvioProcessor();
        processors.add(daqProcessor);

        // Processor for getting the SVT XML config.
        //SvtConfigEvioProcessor svtProcessor = new SvtConfigEvioProcessor();
        //processors.add(svtProcessor);

        // Run the job using the EVIO loop.
        EvioLoop loop = new EvioLoop();
        loop.addProcessors(processors);
        EvioFileSource source = new EvioFileSource(this.evioFiles);
        source.setContinueOnErrors(true); // FIXME: errors should be handled by the loop instead
        loop.setEvioFileSource(source);
        loop.loop(-1);
        
        // Update total events from loop state.
        runSummary.setTotalEvents(loop.getTotalCountableConsumed());
        
        // Set livetime field values.
        updateLivetimes(scalersProcessor);

        // Set TI time offset.
        runSummary.setTiTimeOffset(tiProcessor.getTiTimeOffset());

        // Set EPICS data list.
        epicsData = epicsProcessor.getEpicsData();

        // Set scalers list.
        scalerData = scalersProcessor.getScalerData();

        // Set SVT config data strings.
        //svtConfigs = svtProcessor.getSvtConfigs();
        
        // Set trigger config object.
        config = daqProcessor.getTriggerConfigData();

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
        
        // Create list of EVIO files from datasets.
        createEvioFileList();

        // Set total number of files.
        updateTotalFiles();

        // Set GO and PRESTART timestamps.
        updateStartTimestamps();

        // Set END timestamp.
        updateEndTimestamp();
                
        // Run the full EVIO processing job.
        if (!this.skipEvioProcessing) {
            processEvioFiles();
        } else {
            LOGGER.info("EVIO file processing is skipped.");
        }
        
        // Calculate trigger rate.
        updateTriggerRate();
       
        // Get extra info from the spreadsheet.
        if (this.spreadsheetFile != null) {
            updateFromSpreadsheet();
        } else {
            LOGGER.info("Run spreadsheet not used.");
        }

        // Print out summary info before updating database.
        printSummary();
        
        if (!dryRun) {
            
            // Perform the database update; this will throw a runtime exception if there is an error.
            updateDatabase();
                        
            // Optionally load back run information.
            if (reload) {
                LOGGER.info("reloading data for run " + getRun() + " ...");
                reload(connectionParameters.createConnection(), getRun());
            }
            
        } else {
            // Dry run so database is not updated.
            LOGGER.info("Dry run enabled so no updates were performed.");
        }
        
        LOGGER.info("Done!");
                        
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
    RunDatabaseBuilder setDatacatClient(Client datacatClient) {
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
     * Default folder for file search.
     */
    RunDatabaseBuilder setFolder(String folder) {
        this.folder = folder;
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
     * Set the datacat site.
     * 
     * @param site the datacat site
     * @return this object
     */
    RunDatabaseBuilder setSite(String site) {
        this.site = site;
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
                throw new RuntimeException("Failed to insert run.");
            } catch (SQLException e2) {
                throw new RuntimeException("Error performing rollback.", e2);
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
        File lastEvioFile = evioFiles.get(evioFiles.size() - 1);
        LOGGER.info("setting end timestamp from file " + lastEvioFile.getPath());
        EvioReader reader = null;
        Integer endTimestamp = null;
        try {
            reader = EvioFileUtilities.open(lastEvioFile, true);            
            while (true) {
                if (reader.getNumEventsRemaining() == 0) {
                    break;
                }
                EvioEvent evioEvent = null;
                try {                                   
                    evioEvent = reader.parseNextEvent();
                } catch (Exception e) {
                    LOGGER.severe("Error parsing EVIO event; skipping to next event.");
                    continue;
                }
                if (EventTagConstant.END.matches(evioEvent)) {
                    endTimestamp = EvioEventUtilities.getControlEventData(evioEvent)[0];
                    LOGGER.fine("found END timestamp " + endTimestamp + " in event " + evioEvent.getEventNumber());
                    break;
                }
                BaseStructure headBank = headBankDefinition.findBank(evioEvent);
                if (headBank != null) {
                    if (headBank.getIntData()[0] != 0) {
                        endTimestamp = headBank.getIntData()[0];
                    }
                }
            }
        } catch (IOException | EvioException e2) {
            throw new RuntimeException("Error getting END timestamp.", e2);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        if (endTimestamp != null) {
            runSummary.setEndTimestamp(endTimestamp);
        }
        LOGGER.fine("end timestamp was set to " + endTimestamp);
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
        if (scalers != null) {
            double[] livetimes = ScalerUtilities.getLiveTimes(scalers);
            runSummary.setLivetimeClock(livetimes[LiveTimeIndex.CLOCK.ordinal()]);
            runSummary.setLivetimeFcupTdc(livetimes[LiveTimeIndex.FCUP_TDC.ordinal()]);
            runSummary.setLivetimeFcupTrg(livetimes[LiveTimeIndex.FCUP_TRG.ordinal()]);
            LOGGER.info("clock livetime = " + runSummary.getLivetimeClock());
            LOGGER.info("fcup tdc livetime = " + runSummary.getLivetimeFcupTdc());
            LOGGER.info("fcup trg livetime = " + runSummary.getLivetimeFcupTrg());
        } else {
            LOGGER.warning("Could not calculate livetimes; no scaler data was found by the EVIO processor.");
        }
    }

    /**
     * Update the starting timestamps from the first EVIO file.
     */
    private void updateStartTimestamps() {
        LOGGER.fine("updating start timestamps");
        File firstEvioFile = evioFiles.get(0);
        LOGGER.info("setting start timestamps from file " + firstEvioFile.getPath());
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
        Integer startTimestamp = null;
        if (runSummary.getGoTimestamp() != null) {
            startTimestamp = runSummary.getGoTimestamp();
        } else if (runSummary.getPrestartTimestamp() != null) {
            startTimestamp = runSummary.getPrestartTimestamp();
        }
        Integer endTimestamp = runSummary.getEndTimestamp();
        if (endTimestamp!= null && startTimestamp != null && runSummary.getTotalEvents() > 0) {
            try {
                double triggerRate = ((double) runSummary.getTotalEvents() /
                        ((double) endTimestamp - (double) startTimestamp));
                runSummary.setTriggerRate(triggerRate);
                LOGGER.info("trigger rate set to " + runSummary.getTriggerRate() + " Hz");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calculating trigger rate.", e);
            }
        } else {
            LOGGER.warning("Skipped trigger rate calculation due to missing data.");
        }
    }
}
