package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.data.test.TestDataUtility;
import org.hps.evio.EvioToLcio;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class ConditionsIT extends TestCase {
        
    public void setUp() {
        new DatabaseConditionsManager();
    }
    
    public void testEvioToLcioEngRun2015() {
        File inputFile = new TestDataUtility().getTestData("run5772_integrationTest.evio");        
        File outputFile = new TestOutputFile("EngRun2015ReconTest");
        String args[] = {"-r", "-x", "/org/hps/steering/test/EngRun2015FullRecon_CondCheck.lcsim", "-d",
                "HPS-EngRun2015-Nominal-v3", "-D", "outputFile=" + outputFile.getPath(), "-n", "2",
                inputFile.getPath()};
        EvioToLcio.main(args);
    }
    
    public void testEvioToLcioPhysRun2016() {
    }
    
    public void testLcioRecon() {        
        // run_5772_data_only.slcio
    }
    
    public void testMCRun0() {        
    }
    
    public void testMCUserRun() {        
    }
    
    public void testReadoutSim() {        
    }
    
    public void testCLTools() {        
    }
    
    public void testSLACConnection() {
    }
    
    public void testTag() {
    }
}
