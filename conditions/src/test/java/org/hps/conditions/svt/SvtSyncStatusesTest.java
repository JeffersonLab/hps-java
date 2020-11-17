package org.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtSyncStatus.SvtSyncStatusCollection;

public class SvtSyncStatusesTest extends TestCase {
    public void testSvtSyncStatuses() throws Exception {
        Map<Integer, Boolean> statuses = new HashMap<Integer, Boolean>();
        statuses.put(10022, true);
        statuses.put(10740, false);
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        for (Map.Entry<Integer,Boolean> entry : statuses.entrySet()) {
            Integer run = entry.getKey();
            manager.setDetector("HPS-dummy-detector", run);
            SvtSyncStatusCollection coll = manager.getCachedConditions(SvtSyncStatusCollection.class, "svt_sync_statuses").getCachedData();
            Boolean good = coll.get(0).isGood();
            System.out.println(run.toString() + ":" + good.toString());
            assertTrue(good == entry.getValue());
        }
    }
}
