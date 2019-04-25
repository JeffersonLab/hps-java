package org.hps.conditions.database;

import junit.framework.TestCase;

import org.hps.conditions.ecal.EcalCalibration;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;

public class OfflineConditionsManagerTest extends TestCase {
    
    /**
     * Perform basic tests of the database conditions manager.
     * 
     * @throws Exception if any error is thrown
     */
    public void testOfflineDatabaseConditionsManager() throws Exception {
       
        System.setProperty("org.hps.conditions.url", "jdbc:sqlite:hps_conditions.db"); 
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.openConnection();

        manager.setDetector("HPS-EngRun2015-Nominal-v2", 5772);

        EcalCalibrationCollection testCollection = manager.getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
        TestCase.assertTrue("The test collection should not be empty.", testCollection.size() > 0);
        for (EcalCalibration calib : testCollection) {
            System.out.println(calib);
        }
  
        manager.closeConnection();
    }
}
