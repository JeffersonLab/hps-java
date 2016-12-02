package org.hps.conditions.trigger;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.trigger.TriggerTimeWindow.TriggerTimeWindowCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

public final class TriggerTimeWindowTest extends TestCase {

    private static final int RUN_NUMBER = 0;

    private static DatabaseConditionsManager conditionsManager;

    @Override
    public void setUp() {
        conditionsManager = DatabaseConditionsManager.getInstance();
        try {
            conditionsManager.setDetector("HPS-PhysicsRun2016-Nominal-v4-4", RUN_NUMBER);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void testTriggerTimeWindow() {
        System.out.println("Loading trigger time window for run " + conditionsManager.getRun());
        final TriggerTimeWindowCollection trigColl = 
                conditionsManager.getCachedConditions(TriggerTimeWindowCollection.class, "trigger_time_windows").getCachedData();
        for (final TriggerTimeWindow trigTime : trigColl) {
            System.out.println(trigTime);
        }
    }
}
