package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsManager;

/**
 * This test loads and prints {@link SvtConditions}, which internally uses the  
 * {@link SvtConditionsConverter}.  It does not perform any assertions.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsConverterTest extends TestCase {
    
    /** An example detector from hps-detectors. */
    private static final String detectorName = "HPS-conditions-test";
    
    /** The run number of the conditions set in the database. */
    private static final int runNumber = 777;
    
    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {
        // Create and configure the conditions manager.
        conditionsManager = DatabaseConditionsManager.createInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_testrun_2013.xml");
        conditionsManager.setDetectorName(detectorName);
        conditionsManager.setRunNumber(runNumber);
        conditionsManager.setup();
    }
    
    /**
     * Load and print all SVT conditions for a certain run number.
     */
    public void test() {
                
        // Get conditions and print them out.
        SvtConditions svt = conditionsManager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svt);
        System.out.println(svt);
        System.out.println("Successfully loaded SVT conditions!");
        
        // Cleanup the connection.
        ConnectionManager.getConnectionManager().disconnect();
    }
}
