package org.hps.conditions.database;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

/**
 * General test of the {@link DatabaseConditionsManager} class.
 * 
 * @author Jeremy McCormick, SLAC
 * @see DatabaseConditionsManager
 */
public class DatabaseConditionsManagerTest extends TestCase {
    
    /**
     * Perform basic tests of the database conditions manager.
     * 
     * @throws Exception if any error is thrown
     */
    public void testDatabaseConditionsManager() throws Exception {
        
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.ALL);
        
        // Check initial state.
        TestCase.assertTrue("The conditions manager instance is null.", manager != null);        
        TestCase.assertFalse("The manager should not be connected.", manager.isConnected());        
        TestCase.assertFalse("The manager should not be initialized.", manager.isInitialized());        
        TestCase.assertFalse("The manager should not be frozen.", manager.isFrozen());        
        TestCase.assertTrue("The manager should be setup.", ConditionsManager.isSetup());
        
        // Open database connection.
        manager.openConnection();
        
        // Check that a new collection can be created.
        EcalCalibrationCollection newCollection = manager.newCollection(EcalCalibrationCollection.class, "ecal_calibrations");
        TestCase.assertNotNull("New collection should have metadata.", newCollection.getTableMetaData());
                
        // Check connection state.
        TestCase.assertTrue("The manager should be connected.", manager.isConnected());        
        TestCase.assertNotNull("The connection is null.", manager.getConnection());
        
        // Turn of SVT detector setup becaues some classes are not available from this module.
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");    
        
        // Initialize the conditions system.
        manager.setDetector("HPS-EngRun2015-Nominal-v2", 5772);
        
        // Check basic state after initialization.
        TestCase.assertFalse("Manager should not be configured for test run.", manager.isTestRun());        
        TestCase.assertTrue("The manager should be initialized.", manager.isInitialized());
        
        // Make sure that freezing the conditions system works properly.
        manager.freeze();        
        TestCase.assertTrue("The manager should be frozen.", manager.isFrozen());                
        manager.setDetector("HPS-EngRun2015-Nominal-v2", 1234);        
        TestCase.assertEquals("The run number should be the same because system was frozen.", 5772, manager.getRun());
        
        // Check detector object state.
        Detector detector = manager.getDetectorObject();
        TestCase.assertNotNull("The detector is null.", detector);
        TestCase.assertNotNull("The ECal conditions are null.", manager.getEcalConditions());        
        TestCase.assertNotNull("The ECal name is null.", manager.getEcalName());        
        TestCase.assertNotNull("The ECal subdetector is null.", manager.getEcalSubdetector());        
        TestCase.assertNotNull("The SVT conditions are null.", manager.getSvtConditions());        
        
        // Load a test collection.
        EcalCalibrationCollection testCollection = manager.getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
        TestCase.assertTrue("The test collection should not be empty.", testCollection.size() > 0);
        
        // Load a conditions series.
        ConditionsSeries<?, ?> series = manager.getConditionsSeries(EcalCalibrationCollection.class, "ecal_calibrations");
        TestCase.assertTrue("The conditions series should not be empty.", !series.isEmpty());
                                               
        // Set a tag and check that it seems to work properly (not very detailed).
        TestCase.assertFalse("There should be available tags.", manager.getAvailableTags().isEmpty());
        TestCase.assertTrue("The pass1 tag should be available.", manager.getAvailableTags().contains("pass1"));        
        manager.addTag("pass1");
        TestCase.assertFalse("There should be an active tag.", manager.getActiveTags().isEmpty());
        manager.setDetector("HPS-EngRun2015-Nominal-v2", 5772);
        SvtTimingConstantsCollection testTagCollection = manager.getCachedConditions(SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData();
        TestCase.assertTrue("The collection should not be empty.", testTagCollection.size() > 0);                        
        manager.clearTags();
        TestCase.assertTrue("The tags should be cleared.", manager.getActiveTags().isEmpty());
        
        // See that some conditions records are available.
        ConditionsRecordCollection conditionsRecords = manager.getConditionsRecords();
        TestCase.assertTrue("Conditions records should have been found.", conditionsRecords.size() > 0);               
        conditionsRecords = manager.findConditionsRecords("ecal_calibrations");
        TestCase.assertTrue("A conditions record should have been found.", conditionsRecords.size() > 0);        
        TestCase.assertTrue("A conditions record should exist.", manager.hasConditionsRecord("ecal_calibrations"));
        TestCase.assertNotNull("Table meta data should exist for collection.", manager.findTableMetaData("ecal_calibrations"));
        
        // Un-freeze the conditions system.
        manager.unfreeze();
        TestCase.assertFalse("Manager should not be frozen.", manager.isFrozen());
        
        // Load Test Run setup.
        manager.setDetector("HPS-TestRun-v8-5", 1365);
        TestCase.assertEquals("Run number should have changed.", manager.getRun(), 1365);        
        TestCase.assertTrue("Manager should be configured for test run.", manager.isTestRun());
                       
        // Check SLAC connection setup.
        manager.closeConnection();
        manager.setConnectionResource("/org/hps/conditions/config/slac_connection.prop");
        manager.openConnection();
        TestCase.assertTrue("Connection should be slac host.", manager.getConnection().getMetaData().getURL().contains("slac"));
        
        // Check JLAB connection setup.
        manager.closeConnection();
        manager.setConnectionResource("/org/hps/conditions/config/jlab_connection.prop");
        manager.openConnection();
        TestCase.assertTrue("Connection should be slac host.", manager.getConnection().getMetaData().getURL().contains("jlab"));
        
        manager.closeConnection();
    }
}
