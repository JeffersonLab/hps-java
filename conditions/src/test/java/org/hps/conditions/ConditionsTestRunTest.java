package org.hps.conditions;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Read conditions from the dev database and print them out.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsTestRunTest extends TestCase {
        
    static String config = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
    static String prop = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";
    
    public void testConditionsTestRun() {

        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.configure(config);
        manager.setConnectionResource(prop);
        manager.register();
        try {
            manager.setDetector("HPS-TestRun-v8-5", 0);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
                
        for (TableMetaData metaData : manager.getTableMetaDataList()) {
            ConditionsSeries series = manager.getConditionsSeries(metaData.getKey());
            if (series.getNumberOfCollections() > 0) {
                for (int i = 0; i < series.getNumberOfCollections(); i++) {
                    ConditionsObjectCollection<AbstractConditionsObject> collection = series.getCollection(i);
                    System.out.println("Printing " + collection.getObjects().size()  + " objects in collection " + metaData.getKey() + " ...");
                    for (ConditionsObject object : collection.getObjects()) {
                        System.out.println(object.toString());
                    }
                }
            } else {
                System.err.println("WARNING: No collections found for key <" + metaData.getKey() + "> in this conditions database!");
            }
        }
    }
}