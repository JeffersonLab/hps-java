package org.hps.online.recon;

import org.hps.evio.LCSimEngRunEventBuilder;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

public class OnlineRecon {
   
    class Configuration {
        
        // FIXME: defaults for 2015 data
        public String detectorName = "HPS-EngRun2015-Nominal-v5-0";
        public String steering = "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim";
        public Integer runNumber = 5772;
    }
    
    public static void main(String args[]) {
        OnlineRecon recon = new OnlineRecon();
        
        // TODO: create config from command line args
        
        recon.run(recon.new Configuration());
    }
    
    public void run(Configuration config) {
                
        // conditions setup
        DatabaseConditionsManagerSetup conditions = new DatabaseConditionsManagerSetup();
        conditions.setDetectorName(config.detectorName);
        conditions.setRun(config.runNumber);
        conditions.setFreeze(true);

        // job manager setup
        JobManager mgr = new JobManager();
        mgr.setDryRun(true);
        mgr.addVariableDefinition("outputFile", "online_recon_test.slcio");
        mgr.setConditionsSetup(conditions); // FIXME: Is this even needed since not calling the run() method?
        mgr.setup(config.steering);
               
        // add drivers to composite loop as job manager not called directory
        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration();
        for (Driver driver : mgr.getDriverExecList()) {
            loopConfig.add(driver);
        }

        // not sure why this is needed
        loopConfig.setSupplyLcioEvents(true);
        
        // setup event builder and register with conditions manager
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        loopConfig.setLCSimEventBuilder(builder);
        conditions.addConditionsListener(builder);

        // activate conditions system
        try {
            conditions.setup();
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        conditions.postInitialize();
        
        // ET configuration
        EtConnection conn = EtConnection.createDefaultConnection();
        loopConfig.setDataSourceType(DataSourceType.ET_SERVER);    
        loopConfig.setEtConnection(conn);
        loopConfig.setMaxQueueSize(1);
        loopConfig.setTimeout(-1L);        
        loopConfig.setStopOnEndRun(true).setStopOnErrors(true);

        // run the loop
        CompositeLoop loop = new CompositeLoop(loopConfig);
        loop.loop(-1);
    }    
}