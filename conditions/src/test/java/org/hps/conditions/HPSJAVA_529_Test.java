package org.hps.conditions;

import org.hps.conditions.database.DatabaseConditionsManager;


public class HPSJAVA_529_Test {
           
    public void test() throws Exception {
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        int startRun = 5000;
        int stopRun = 5772;
        for (int run = startRun; run < stopRun; run++) {
            conditionsManager.setDetector("HPS-EngRun2015-Nominal-v3", run);
        }
    }
}
