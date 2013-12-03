package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

import org.hps.conditions.ConnectionManager;

/**
 * This class loads and prints {@link SvtConditions}, which internally uses the  
 * {@link SvtConditionsConverter}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsConverterTest extends TestCase {
    
    /** An example detector from hps-detectors. */
    private static final String detectorName = "HPS-conditions-test";
    
    /** The run number of the conditions set in the database. */
    private static final int runNumber = 777;
            
    /**
     * Load and print all SVT conditions for a certain run number.
     */
    public void test() {
        
        // Setup the conditions manager.        
        ConditionsManager.setDefaultConditionsManager(new LCSimConditionsManagerImplementation());
        ConditionsManager manager = ConditionsManager.defaultInstance();
        try {
            manager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        // Get conditions and print them out.
        SvtConditions svt = manager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svt);
        System.out.println(svt);
        System.out.println("Successfully loaded SVT conditions!");
        
        // Cleanup the connection.
        ConnectionManager.getConnectionManager().disconnect();
    }
}
