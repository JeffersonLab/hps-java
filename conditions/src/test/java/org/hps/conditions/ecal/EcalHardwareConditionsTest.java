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
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EcalHardwareConditionsTest extends TestCase {

    /**
     * Name of hardware calibrations table.
     */
    private static final String CALIBRATIONS_TABLE = "ecal_hardware_calibrations";

    /**
     * Name of hardware gains table.
     */
    private static final String GAINS_TABLE = "ecal_hardware_gains";

    /**
     * Number of conditions records (matches number of channels).
     */
    private static final int RECORD_COUNT = 442;

    /**
     * Load the ECAL hardware conditions.
     * @throws Exception if there is a conditions error
     */
    public void testEcalHardwareConditions() throws Exception {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        try {
            manager.setDetector("HPS-ECalCommissioning-v2", 0);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        manager.setLogLevel(Level.ALL);

        // Read hardware calibrations.
        final EcalCalibrationCollection calibrations = manager.getCachedConditions(EcalCalibrationCollection.class,
                CALIBRATIONS_TABLE).getCachedData();
        assertEquals("Wrong name in conditions record.", CALIBRATIONS_TABLE,
                calibrations.getConditionsRecord().getTableName());
        assertEquals("Wrong table name in conditions record.", CALIBRATIONS_TABLE,
                calibrations.getConditionsRecord().getTableName());
        assertEquals("Wrong number of records.", RECORD_COUNT, calibrations.size());
        System.out.println("successfully read " + calibrations.size() + " gain records from " + CALIBRATIONS_TABLE);

        // Read hardware gains.
        final EcalGainCollection gains = manager.getCachedConditions(
                EcalGainCollection.class, GAINS_TABLE).getCachedData();
        assertEquals("Wrong name in conditions record.", GAINS_TABLE, gains.getConditionsRecord().getTableName());
        assertEquals("Wrong table name in conditions record.", GAINS_TABLE, gains.getConditionsRecord().getTableName());
        assertEquals("Wrong number of records.", RECORD_COUNT, gains.size());
        System.out.println("successfully read " + gains.size() + " gain records from " + GAINS_TABLE);
    }
}
