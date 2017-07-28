package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.data.test.TestDataUtility;
import org.hps.evio.EvioToLcio;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.test.util.TestOutputFile;

/**
 * Test the conditions system using various job and data configurations.
 */
public class ConditionsIT extends TestCase {
    
    static {
        System.getProperties().setProperty("disableSvtAlignmentConstants", "true");
    }
            
    /**
     * Resets the conditions system before each test method runs.
     */
    public void setUp() {
        new DatabaseConditionsManager();
    }
    
    /**
     * Convert EVIO from Engineering Run 2015 to LCIO with detector name specified from the command line,
     * and also run the reconstruction within the job.  The run number from the EVIO data header will activate
     * the conditions system.  Conditions are checked for validity against their expected collection IDs.
     */
    public void testEvioToLcioEngRun2015() {
        File inputFile = new TestDataUtility().getTestData("run5772_integrationTest.evio");
        File outputFile = new TestOutputFile(ConditionsIT.class, "EvioToLcioEngRun2015");
        String args[] = {"-r", "-x", "/org/hps/steering/test/EngRun2015FullRecon_CondCheck.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
     
    /**
     * Run the physics reconstruction on LCIO data from Engineering Run 2015.  The run number and detector 
     * from the LCIO file will activate the conditions.  Condition sets are checked for validity against their
     * expected collection IDs.
     */
    public void testLcioReconEngRun2015() {
        File inputFile = new TestDataUtility().getTestData("run_5772_data_only.slcio");
        File outputFile = new TestOutputFile(ConditionsIT.class, "LcioRecon");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/EngRun2015FullRecon_CondCheck.lcsim");
        job.setNumberOfEvents(2);
        job.run();
    }
    
    /**
     * Convert EVIO data from Physics Run 2016 to LCIO and run the reconstruction.  The detector name
     * is provided as a command line argument and the run number from the EVIO header will activate the 
     * conditions.  Conditions are checked for validity against their expected collection IDs.
     */
    public void testEvioToLcioPhysRun2016() {
        File inputFile = new TestDataUtility().getTestData("hps_007457_1000.evio.0");        
        File outputFile = new TestOutputFile(ConditionsIT.class, "EvioToLcioEngRun2015");
        String args[] = {"-r", "-x", "/org/hps/steering/test/PhysicsRun2016FullRecon_CondCheck.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
    
    /**
     * Convert EVIO data from Test Run 2014 to LCIO.  The detector name is provided as a command
     * line argument and the run number from the EVIO header will activate the conditions.  
     * Conditions are checked validity against their expected collection IDs.
     */
    public void testEvioToLcioTestRun() {
        File inputFile = new TestDataUtility().getTestData("hps_001306_1000.evio.0");
        File outputFile = new TestOutputFile(ConditionsIT.class, "EvioToLcioTestRun.slcio");
        String args[] = {"-r", "-d", "HPS-TestRun-v8-5", "-l", outputFile.getPath(), "-n", "2",
                "-x", "/org/hps/steering/test/TestRun2014_CondCheck.lcsim",
                inputFile.getPath()};        
        EvioToLcio.main(args);   
    }
      
    /* 
     * Tests MC recon using the default "run 0" conditions, but it is disabled for now.
     * It isn't clear that the baseline conditions when using run 0 are compatible with current
     * MC output so this needs to be solved first before this configuration is testable.
     */ 
    /*
    public void testMCReconDefaultConditions() {
        File inputFile = new TestDataUtility().getTestData("tritrig_EngRun2015_readout.slcio");
        File outputFile = new TestOutputFile(ConditionsIT.class, "MCReconDefaultConditions");
        JobManager job = new JobManager();
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/EngineeringRun2015FullReconMC_CondCheck.lcsim");
        job.setNumberOfEvents(2);
        job.run();
    }
    */
    
    /**
     * Run MC reconstruction on LCIO output from readout simulation.  The detector and run number
     * are specified to the job manager in the conditions setup.  Conditions are checked for 
     * validity against their expected collection IDs.
     */
    public void testMCReconUserConditions() {
        File inputFile = new TestDataUtility().getTestData("tritrig_EngRun2015_readout.slcio");
        File outputFile = new TestOutputFile(ConditionsIT.class, "MCReconDefaultConditions");
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
    
    /**
     * Run the readout and trigger simulation on LCIO output from SLIC with bunch spacing applied.
     * The detector and run number are specified to the job manager in the conditions setup.  
     * Conditions are checked for validity against their expected collection IDs.
     */
    public void testReadoutSim() {
        File inputFile = new TestDataUtility().getTestData("tritrig_EngRun2015_filt.slcio");
        File outputFile = new TestOutputFile(ConditionsIT.class, "ReadoutSim");
        JobManager job = new JobManager();
        DatabaseConditionsManagerSetup cond = new DatabaseConditionsManagerSetup();
        cond.setDetectorName("HPS-EngRun2015-Nominal-v5-0-fieldmap");
        cond.setRun(5772);
        cond.setFreeze(true);
        job.setConditionsSetup(cond);
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);        
        job.setup("/org/hps/steering/test/EngineeringRun2015TrigPairs1_Pass2_CondCheck.lcsim");
        job.setNumberOfEvents(1000);
        job.run();
    }
}
