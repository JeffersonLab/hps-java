package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
//import org.hps.conditions.config.DevReadOnlyConfiguration;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.hps.conditions.ecal.EcalLedCalibration.EcalLedCalibrationCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * A very basic test to make sure ECAL LED information is 
 * readable from the conditions dev database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalLedTest extends TestCase {
          
    DatabaseConditionsManager conditionsManager;
    public void setUp() {
        conditionsManager = DatabaseConditionsManager.getInstance();
        try {
            conditionsManager.setDetector("HPS-ECalCommissioning-v2", 2000);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
	
    public void testEcalLed() {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        
        // LED channel information.
        EcalLedCollection leds = manager.getCollection(EcalLedCollection.class);
        for (EcalLed led : leds) {    	
        	System.out.println(led);
        }
        
        // LED calibration data.
        EcalLedCalibrationCollection calibrations = manager.getCollection(EcalLedCalibrationCollection.class);
        for (EcalLedCalibration calibration : calibrations) {        
            System.out.println(calibration);
        }
    }
}
