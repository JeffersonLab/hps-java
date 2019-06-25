package org.hps.online.recon;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class Station {
       
    /**
     * Class logger.
     */
    private static Logger LOGGER = Logger.getLogger(Station.class.getPackageName());
                
    /**
     * The station configuration.
     */
    private StationConfiguration config = null;
       
    /**
     * Create new online reconstruction station with a configuration.
     * @param config The station configuration
     */
    Station(StationConfiguration config) {
        this.config = config;
    }
           
    /**
     * Get the configuration of the station.
     * @return The configuration of the station.
     */
    StationConfiguration getConfiguration() {
        return this.config;
    }
    
    /**
     * Run from the command line.
     * @param args The command line arguments
     */
    public static void main(String args[]) {        
        if (args.length == 0) {
            throw new RuntimeException("Missing config properties file");
        }         
        StationConfiguration sc = new StationConfiguration(new File(args[0]));
        if (!sc.isValid()) {
            throw new RuntimeException("Station configuration is not valid (see log messages).");
        }
        Station recon = new Station(sc);
        recon.run();
    }
        
    /**
     * Run the online reconstruction station.
     */
    void run() {
                
        LOGGER.info("Started: " + new Date().toString());
        LOGGER.config("Running station <" + this.getConfiguration().getStation() + 
                "> with config " + this.config.toString());
        
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
        // FIXME: use loop adapter instead
        if (this.config.getEventPrintInterval() > 0) {
            loopConfig.add(new EventMarkerDriver(this.config.getEventPrintInterval()));
        } else {
            LOGGER.config("Event number printing is disabled.");
        }
        
        // Add drivers from the job manager to the loop.
        for (Driver driver : mgr.getDriverExecList()) {
            LOGGER.config("Adding driver " + driver.getClass().getCanonicalName());
            loopConfig.add(driver);
        }
        
        // Configure and add the AIDA driver for intermediate plot saving.
        int plotSaveInterval = config.getPlotSaveInterval();
        if (plotSaveInterval > 0) {
            PlotDriver aidaDriver = new PlotDriver();
            aidaDriver.setStationName(config.getStation());
            aidaDriver.setOutputDir(config.getOutputDir());
            aidaDriver.setResetAfterSave(true); // TODO: Should this be a config option?
            aidaDriver.setEventSaveInterval(plotSaveInterval);
            LOGGER.config("Adding AIDA driver to save plots every " + config.getPlotSaveInterval() + " events");
            loopConfig.add(aidaDriver);
        } else {
            LOGGER.config("Automatic plot saving is disabled.");
        }
        
        if (config.getEventStatisticsInterval() > 0) {
            EventStatisticsDriver esd = new EventStatisticsDriver();
            esd.setEventPrintInterval(config.getEventStatisticsInterval());
            loopConfig.add(esd);
            LOGGER.config("Added event statistics driver with event interval: " + config.getEventStatisticsInterval());
        } else {
            LOGGER.config("Event statistics disabled.");
        }
                
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
        LOGGER.info("Running record loop for station: " + this.getConfiguration().getStation());
        try {
            loop.loop(-1);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Event processing error", e);
            e.printStackTrace();
        }
        //LOGGER.info("Station is done looping: " + this.getConfiguration().getStation());
        LOGGER.info("Ended: " + new Date().toString());
    }
    
    /**
     * Create an ET connection appropriate for parallel stations.
     * @param config The station configuration with ET parameters
     * @return The <code>EtConnection</code>
     * @throws Exception If there are ET system errors when connecting
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
                config.getChunkSize(),
                config.getEtLogLevel());
    }
}