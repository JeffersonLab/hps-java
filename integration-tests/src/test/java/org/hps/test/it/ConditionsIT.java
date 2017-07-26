package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.data.test.TestDataUtility;
import org.hps.evio.EvioToLcio;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class ConditionsIT extends TestCase {
        
    public void setUp() {
        new DatabaseConditionsManager();
    }
    
    public void testEvioToLcioEngRun2015() {
        File inputFile = new TestDataUtility().getTestData("run5772_integrationTest.evio");        
        File outputFile = new TestOutputFile("EvioToLcioEngRun2015");
        String args[] = {"-r", "-x", "/org/hps/steering/test/EngRun2015FullRecon_CondCheck.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
        
    public void testLcioRecon() {
        File inputFile = new TestDataUtility().getTestData("run_5772_data_only.slcio");
        File outputFile = new TestOutputFile("LcioRecon");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/EngRun2015FullRecon_CondCheck.lcsim");
        job.setNumberOfEvents(2);
        job.run();
    }
    
    public void testEvioToLcioPhysRun2016() {
        File inputFile = new TestDataUtility().getTestData("hps_007457_1000.evio.0");        
        File outputFile = new TestOutputFile("EvioToLcioEngRun2015");
        String args[] = {"-r", "-x", "/org/hps/steering/recon/PhysicsRun2016FullRecon.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
    
    public void testEvioToLcioTestRun() {
    }

    public void testMCDefaultConditions() {        
    }
    
    public void testMCUserConditions() {
    }
    
    public void testReadoutSim() {
    }
}
