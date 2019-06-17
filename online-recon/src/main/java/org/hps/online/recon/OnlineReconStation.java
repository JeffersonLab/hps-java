package org.hps.online.recon;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.cli.ParseException;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.evio.LCSimEngRunEventBuilder;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;

/**
 * Online reconstruction station which processes events from the ET system
 * and writes intermediate plot files.
 * 
 * @author jeremym
 */
public class OnlineReconStation {
       
    private static Logger LOGGER = Logger.getLogger(OnlineReconStation.class.getPackageName());
                
    private StationConfiguration config = null;
       
    public OnlineReconStation(StationConfiguration config) {
        this.config = config;
    }
           
    public StationConfiguration getConfiguration() {
        return this.config;
    }
    
    public static void main(String args[]) {
        StationConfiguration config = new StationConfiguration();
        try {
            config.parse(args);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line", e);
        }
        if (!config.isValid()) {
            throw new RuntimeException("Station configuration is not valid (see log messages).");
        }
        OnlineReconStation recon = new OnlineReconStation(config);
        recon.run();
    }
        
    /**
     * Run the online reconstruction station.
     */
    public void run() {
                
        // Composite loop configuration.
        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration();
                
        // Setup the condition system.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        DatabaseConditionsManagerSetup conditionsSetup = new DatabaseConditionsManagerSetup();
        boolean activateConditions = true;        
        if (config.getRunNumber() != null) {
            // Run number from configuration.
            conditionsSetup.setDetectorName(config.getDetectorName());
            conditionsSetup.setRun(config.getRunNumber());
            conditionsSetup.setFreeze(true);
        } else {
            // No run number in configuration so read from EVIO data.
            EvioDetectorConditionsProcessor evioConditions = new EvioDetectorConditionsProcessor(config.getDetectorName());
            loopConfig.add(evioConditions);
            activateConditions = false;
            LOGGER.config("No run number was set so conditions will be initialized from EVIO data.");
        }

        // Setup event builder and register with conditions system.
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        conditionsManager.addConditionsListener(builder);
        loopConfig.setLCSimEventBuilder(builder);
        
        // Setup the lcsim job manager.
        JobManager mgr = new JobManager();
        mgr.setDryRun(true);
        final String outputFilePath = config.getOutputDir() + File.separator + config.getOutputName();
        LOGGER.config("Output file path: " + outputFilePath);
        mgr.addVariableDefinition("outputFile", outputFilePath);
        mgr.setConditionsSetup(conditionsSetup); // FIXME: Is this even needed since not calling the run() method?
        mgr.setup(config.getSteeringResource());
       
        // Setup event number print outs.
        if (this.config.getEventPrintInterval() > 0) {
            loopConfig.add(new EventMarkerDriver(this.config.getEventPrintInterval()));
        } else {
            LOGGER.config("Event number printing is disabled.");
        }
        
        // Add drivers from the job manager to the loop.
        LOGGER.config("Adding " + mgr.getDriverExecList().size() + " drivers to loop ...");
        for (Driver driver : mgr.getDriverExecList()) {
            LOGGER.config("Adding driver " + driver.getClass().getCanonicalName());
            loopConfig.add(driver);
        }
        
        // Configure and add the AIDA driver for intermediate plot saving.
        OnlineReconAidaDriver aidaDriver = new OnlineReconAidaDriver();
        aidaDriver.setStationName(config.getStation());
        aidaDriver.setOutputDir(config.getOutputDir());
        aidaDriver.setResetAfterSave(true);
        aidaDriver.setEventSaveInterval(config.getEventSaveInterval());
        LOGGER.config("Adding AIDA driver to save plots every " + config.getEventSaveInterval() + " events");
        loopConfig.add(aidaDriver);
                
        // Activate the conditions system, if possible.
        if (activateConditions) {
            LOGGER.config("Activating conditions system");
            conditionsSetup.configure();
            try {
                conditionsSetup.setup();
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
            conditionsSetup.postInitialize();
        }
        
        // Configure the ET connection.
        LOGGER.config("Configuring ET system");
        final EtConnection conn;
        try {
            conn = createEtConnection(config);
        } catch (Exception e) {
            throw new RuntimeException("Error creating ET connection", e);
        }
        
        // Cleanly shutdown the ET station on exit.
        // TODO: Check if this is actually being run!
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (conn != null && conn.getEtStation() != null) {
                    LOGGER.info("Cleaning up ET station: " + conn.getEtStation().getName());
                    conn.cleanup();
                }
            }
        });
        
        // Configure more settings on the loop.
        loopConfig.setDataSourceType(DataSourceType.ET_SERVER);    
        loopConfig.setEtConnection(conn);
        loopConfig.setMaxQueueSize(1); // Should this be increased for EVIO conditions activation???
        loopConfig.setTimeout(-1L);
        loopConfig.setStopOnEndRun(true);
        loopConfig.setStopOnErrors(true);
               
        // Run the loop.
        CompositeLoop loop = new CompositeLoop(loopConfig);
        LOGGER.info("Running composite loop");
        loop.loop(-1);
    }
    
    /**
     * Create an ET connection appropriate for parallel stations.
     * @param config
     * @return
     * @throws Exception
     */
    private EtConnection createEtConnection(StationConfiguration config) throws Exception {
        return new EtParallelStation(
                config.getBufferName(),
                config.getHost(),
                config.getPort(),
                config.getQueueSize(),
                config.getPrescale(),
                config.getStation(),
                config.getWaitMode(),
                config.getWaitTime(),
                config.getChunkSize());
    }
}