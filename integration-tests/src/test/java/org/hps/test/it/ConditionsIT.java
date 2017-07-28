package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.data.test.TestDataUtility;
import org.hps.evio.EvioToLcio;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class ConditionsIT extends TestCase {
    
    static {
        System.getProperties().setProperty("disableSvtAlignmentConstants", "true");
    }
            
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
        
    public void testLcioReconEngRun2015() {
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
        String args[] = {"-r", "-x", "/org/hps/steering/test/PhysicsRun2016FullRecon_CondCheck.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
    
    public void testEvioToLcioTestRun() {
        File inputFile = new TestDataUtility().getTestData("hps_001306_1000.evio.0");
        File outputFile = new TestOutputFile("EvioToLcioTestRun.slcio");
        String args[] = {"-r", "-d", "HPS-TestRun-v8-5", "-l", outputFile.getPath(), "-n", "1000",
                inputFile.getPath()};
        try {
            EvioToLcio.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
       
    public void testMCReconDefaultConditions() {
        File inputFile = new TestDataUtility().getTestData("tritrig_EngRun2015_readout.slcio");
        File outputFile = new TestOutputFile("MCReconDefaultConditions");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/EngineeringRun2015FullReconMC_CondCheck.lcsim");
        job.setNumberOfEvents(2);
        job.run();
    }
    
    public void testMCReconUserConditions() {
        File inputFile = new TestDataUtility().getTestData("tritrig_EngRun2015_readout.slcio");
        File outputFile = new TestOutputFile("MCReconDefaultConditions");
        JobManager job = new JobManager();
        DatabaseConditionsManagerSetup cond = new DatabaseConditionsManagerSetup();
        cond.setDetectorName("HPS-EngRun2015-Nominal-v5-0-fieldmap");
        cond.setRun(5772);
        cond.setFreeze(true);
        job.setConditionsSetup(cond);
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/EngineeringRun2015FullReconMC_CondCheck.lcsim");
        job.setNumberOfEvents(2);
        job.run();
    }    
    
    public void testReadoutSim() {
        // TODO: readout simulation job
        // /org/hps/steering/readout/EngineeringRun2015TrigPairs1_Pass2.lcsim
    }
}
