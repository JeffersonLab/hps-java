package org.hps.run.database;

import java.util.List;

import junit.framework.TestCase;

import org.hps.record.triggerbank.TriggerConfig;

/**
 * Test of getting the TI trigger offset from the run database.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TiTriggerOffsetTest extends TestCase {
    
    /**
     * Get the TI trigger offset for all runs.
     */
    public void testAllRuns() {
        RunManager runManager = new RunManager();
        List<Integer> runs = runManager.getRuns();  
        for (Integer run : runs) {
            runManager.setRun(run);
            TriggerConfig triggerConfig = runManager.getTriggerConfig();
            Long tiTimeOffset = triggerConfig.getTiTimeOffset();
            System.out.println("run " + run + " tiTriggerOffset = " + tiTimeOffset);
        }
    }
    
    /**
     * Get the TI trigger offset for a single run.
     */
    public void testSingleRun() {
        int run = 5772;
        RunManager runManager = new RunManager();
        runManager.setRun(run);
        TriggerConfig triggerConfig = runManager.getTriggerConfig();
        Long tiTimeOffset = triggerConfig.getTiTimeOffset();
        System.out.println("run " + run + " tiTriggerOffset = " + tiTimeOffset);
    }
}
