package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.config.DevDatabaseReadOnlyConfig;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;

/**
 * A very basic test to make sure ECAL LED information is 
 * readable from the conditions dev database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalLedTest extends TestCase {
    
    DevDatabaseReadOnlyConfig db = new DevDatabaseReadOnlyConfig();
    
    public void setUp() {
        db.setup();
        db.load("HPS-TestRun-v5", 1351);
    }
    
    public void testEcalLed() {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        EcalLedCollection collection = manager.getConditionsData(EcalLedCollection.class, TableConstants.ECAL_LEDS);
        for (EcalLed led : collection) {
            System.out.println("ECAL LED info ...");
            System.out.println("ecal_channel_id: " + led.getEcalChannelId());
            System.out.println("crate: " + led.getCrateNumber());
            System.out.println("number: " + led.getLedNumber());
            System.out.println("time_delay: " + led.getTimeDelay());
            System.out.println("amplitude_low: " + led.getAmplitudeLow());
            System.out.println("amplitude_high: " + led.getAmplitudeHigh());
        }
    }
}
