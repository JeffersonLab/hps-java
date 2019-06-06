package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
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
    
    static class Configuration {
        
        private String detectorName = "HPS-EngRun2015-Nominal-v5-0";
        private String steering = "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim";
        private Integer runNumber = 5772;
        private String outputName = "online_recon_events";
        private String outputDir = System.getProperty("user.dir");
        
        private Properties props = null;
        
        Configuration() {            
        }
        
        Configuration(File file) {
            if (file != null) {
                load(file);
            } else {
                throw new RuntimeException("The prop file points to null.");
            }
        }
        
        void load(File file) {
            LOGGER.config("Loading properties from file: " + file.getPath());
            this.props = new Properties();
            try {
                props.load(new FileInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            setProperties();
            LOGGER.config("Loaded properties: " + this.props.toString());
        }
        
        private void setProperties() {
            if (props.containsKey("detector")) {
                detectorName = props.getProperty("detector");
            }            
            if (props.contains("steering")) {
                steering = props.getProperty("steering");
            }
            if (props.contains("run")) {
                runNumber = Integer.parseInt(props.getProperty("run"));
            }
            if (props.contains("outputName")) {
                outputName = props.getProperty("outputName");
            }
            if (props.contains("outputDir")) {
                outputDir = props.getProperty("outputDir");
            }
        }    
        
        String getDetectorName() {
            return detectorName;
        }
        
        String getSteeringResource() {
            return steering;
        }
        
        Integer getRunNumber() {
            return runNumber;
        }
        
        String getOutputName() {
            return outputName;
        }
        
        String getOutputDir() {
            return outputDir;
        }
    }
    
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
        // TODO: the ET station needs to be fully configurable with parallel stations in blocking mode
        // TODO: name of ET station needs to be set from prop
        LOGGER.config("Configuring ET system ...");
        EtConnection conn = EtConnection.createDefaultConnection();
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
}