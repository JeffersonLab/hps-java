package org.hps.online.recon;

import java.io.File;
import java.util.logging.Logger;

import org.hps.evio.LCSimEngRunEventBuilder;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class OnlineRecon {
   
    private static Logger LOGGER = Logger.getLogger(OnlineRecon.class.getPackageName());
        
    static class DummyDriver extends Driver {        
        public void process(EventHeader event) {
            LOGGER.info(">>> Online recon processing event " + event.getEventNumber());
        }        
    }
        
    private Configuration config = null;
    
    public OnlineRecon(Configuration config) {
        this.config = config;
    }
           
    public Configuration getConfiguration() {
        return this.config;
    }
    
    public static void main(String args[]) {
        Configuration config = new Configuration(new File(args[0]));
        OnlineRecon recon = new OnlineRecon(config);
        recon.run();
    }
        
    public void run() {
                
        // composite loop configuration
        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration();
                
        // initialize conditions setup and set basic parameters
        // TODO: run number should come from EVIO files
        DatabaseConditionsManagerSetup conditions = new DatabaseConditionsManagerSetup();
        conditions.setDetectorName(config.getDetectorName());
        conditions.setRun(config.getRunNumber());
        conditions.setFreeze(true);

        // setup event builder and register with conditions system
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        conditions.addConditionsListener(builder);
        loopConfig.setLCSimEventBuilder(builder);
        
        // job manager setup
        JobManager mgr = new JobManager();
        mgr.setDryRun(true);
        final String outputFilePath = config.getOutputDir() + File.separator + config.getOutputName();
        LOGGER.config("Output file path set to: " + outputFilePath);
        mgr.addVariableDefinition("outputFile", outputFilePath);
        mgr.setConditionsSetup(conditions); // FIXME: Is this even needed since not calling the run() method?
        mgr.setup(config.getSteeringResource());
               
        // add drivers from job manager to composite loop
        loopConfig.add(new DummyDriver());
        LOGGER.config("Adding " + mgr.getDriverExecList().size() + " drivers to loop ...");
        for (Driver driver : mgr.getDriverExecList()) {
            LOGGER.config("Adding driver: " + driver.getClass().getCanonicalName());
            loopConfig.add(driver);
        }
        
        // activate conditions system
        LOGGER.config("Activating conditions system ...");
        conditions.configure();
        try {
            conditions.setup();
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        conditions.postInitialize();
        
        // ET configuration
        LOGGER.config("Configuring ET system ...");
        //EtConnection conn = EtConnection.createDefaultConnection();
        EtConnection conn = null;
        try {
            conn = createEtConnection(config);
        } catch (Exception e) {
            throw new RuntimeException("Error creating ET connection.", e);
        }
        loopConfig.setDataSourceType(DataSourceType.ET_SERVER);    
        loopConfig.setEtConnection(conn);
        loopConfig.setMaxQueueSize(1);
        loopConfig.setTimeout(-1L);        
        loopConfig.setStopOnEndRun(true).setStopOnErrors(true);
        
        // run the loop
        LOGGER.config("Running composite loop ...");
        CompositeLoop loop = new CompositeLoop(loopConfig);
        loop.loop(-1);
    }
    
    private EtConnection createEtConnection(Configuration config) throws Exception {
        //String stationName = config.getStation() + "_" + String.format("%02d", config.getID());
        return new EtParallelStation(
                config.getBufferName(),
                config.getHost(),
                config.getPort(),
                config.getBlocking(),
                config.getQueueSize(),
                config.getPrescale(),
                config.getStation(),
                config.getPosition(),
                config.getMode(),
                config.getWaitTime(),
                config.getChunkSize()
                );       
    }
}