package org.hps.conditions;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Read conditions from the dev database and print them out.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDevTest extends TestCase {
    
    static String config = "/org/hps/conditions/config/conditions_dev.xml";
    static String prop = "/org/hps/conditions/config/conditions_dev.properties";
    
    public void testConditionsDev() {

        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.configure(config);
        manager.setConnectionResource(prop);
        manager.register();
        try {
            manager.setDetector("HPS-Proposal2014-v8-6pt6", 0);
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
            }
        }
    }
}
