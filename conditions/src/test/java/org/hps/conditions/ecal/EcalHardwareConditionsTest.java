package org.hps.conditions.ecal;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This is a simple test that reads ECAL hardware calibrations and gains 
 * from the conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalHardwareConditionsTest extends TestCase {

    static String CALIBRATIONS_TABLE = "ecal_hardware_calibrations"; 
    static String GAINS_TABLE = "ecal_hardware_gains";
    static int RECORD_COUNT = 442;
    
    public void testEcalHardwareConditions() throws Exception {
        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        try {
            manager.setDetector("HPS-ECalCommissioning-v2", 0);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        manager.setLogLevel(Level.ALL);
        
        // Read hardware calibrations.
        EcalCalibrationCollection calibrations = manager.getCachedConditions(EcalCalibrationCollection.class, CALIBRATIONS_TABLE).getCachedData();
        assertEquals("Wrong name in conditions record.", CALIBRATIONS_TABLE, calibrations.getConditionsRecord().getTableName());
        assertEquals("Wrong table name in conditions record.", CALIBRATIONS_TABLE, calibrations.getConditionsRecord().getTableName());
        assertEquals("Wrong number of records.", RECORD_COUNT, calibrations.size());
        System.out.println("successfully read " + calibrations.size() + " gain records from " + CALIBRATIONS_TABLE);
        
        // Read hardware gains.
        EcalGainCollection gains = manager.getCachedConditions(EcalGainCollection.class, GAINS_TABLE).getCachedData();
        assertEquals("Wrong name in conditions record.", GAINS_TABLE, gains.getConditionsRecord().getTableName());
        assertEquals("Wrong table name in conditions record.", GAINS_TABLE, gains.getConditionsRecord().getTableName());
        assertEquals("Wrong number of records.", RECORD_COUNT, gains.size());
        System.out.println("successfully read " + gains.size() + " gain records from " + GAINS_TABLE);
    }
    
}
