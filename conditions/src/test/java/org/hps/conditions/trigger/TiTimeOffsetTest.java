package org.hps.conditions.trigger;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

public final class TiTimeOffsetTest extends TestCase {

    private static final int RUN_NUMBER = 5772;

    private static DatabaseConditionsManager conditionsManager;

    public void setUp() {
        conditionsManager = DatabaseConditionsManager.getInstance();
        try {
            conditionsManager.setDetector("HPS-PhysicsRun2016-Nominal-v4-4", RUN_NUMBER);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void testTiTimeOffset() {
        System.out.println("Loading TI time offset for run " + conditionsManager.getRun());
        final TiTimeOffset t = 
                conditionsManager.getCachedConditions(TiTimeOffset.class, "ti_time_offsets").getCachedData();
        System.out.println("run " + RUN_NUMBER + "; ti_time_offset = " + t.getValue());
    }
}
