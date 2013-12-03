package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.hps.conditions.ConnectionManager;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

public class EcalConditionsConverterTest extends TestCase {
    
    /** An example detector from hps-detectors. */
    private static final String detectorName = "HPS-conditions-test";
    
    /** The run number of the conditions set in the database. */
    private static final int runNumber = 777;
        
    public void test() {
        
        // Setup the conditions manager.        
        ConditionsManager.setDefaultConditionsManager(new LCSimConditionsManagerImplementation());
        ConditionsManager manager = ConditionsManager.defaultInstance();
        try {
            manager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
                                        
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = manager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();        
        assertNotNull(conditions);
        System.out.println(conditions);
        
        // Cleanup the connection.
        ConnectionManager.getConnectionManager().disconnect();
    }    
}
