package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsManager;

/**
 * Tests that a {@link EcalConditions} objects loads without errors.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverterTest extends TestCase {
    
    final String detectorName = "HPS-conditions-test";
    final int runNumber = 777;
    
    DatabaseConditionsManager conditionsManager = new DatabaseConditionsManager();
    
    public void setUp() {
        // Create and configure the conditions manager.
        conditionsManager = DatabaseConditionsManager.createInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_testrun_2013.xml");
        conditionsManager.setDetectorName(detectorName);
        conditionsManager.setRunNumber(runNumber);
        conditionsManager.setup();
    }
            
    public void test() {
                                                
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();        
        assertNotNull(conditions);
        System.out.println(conditions);
        
        // Cleanup the connection.
        ConnectionManager.getConnectionManager().disconnect();
    }    
}
