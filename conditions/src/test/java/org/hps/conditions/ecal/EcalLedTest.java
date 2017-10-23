package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
// import org.hps.conditions.config.DevReadOnlyConfiguration;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.hps.conditions.ecal.EcalLedCalibration.EcalLedCalibrationCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * A test to make sure ECAL LED information is readable from the conditions dev database.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EcalLedTest extends TestCase {

    /**
     * Run number to use for test.
     */
    private static final int RUN_NUMBER = 2000;

    /**
     * The conditions manager.
     */
    private static DatabaseConditionsManager conditionsManager;

    /**
     * Setup the conditions manager.
     */
    @Override
    public void setUp() {
        conditionsManager = new DatabaseConditionsManager();
        try {
            conditionsManager.setDetector("HPS-ECalCommissioning-v2", RUN_NUMBER);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load ECAL LED conditions.
     */
    public void testEcalLed() {

        // LED channel information.
        final EcalLedCollection leds = conditionsManager.getCachedConditions(EcalLedCollection.class, "ecal_leds")
                .getCachedData();
        for (final EcalLed led : leds) {
            System.out.println(led);
        }

        // LED calibration data.
        final EcalLedCalibrationCollection calibrations = conditionsManager.getCachedConditions(
                EcalLedCalibrationCollection.class, "ecal_led_calibrations").getCachedData();
        for (final EcalLedCalibration calibration : calibrations) {
            System.out.println(calibration);
        }
    }
}
