package org.hps.conditions;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Read conditions from the dev database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDevTest extends TestCase {

    static final String[] conditionsKeys = {
            TableConstants.ECAL_CALIBRATIONS,
            TableConstants.ECAL_CHANNELS,
            TableConstants.ECAL_GAINS,
            TableConstants.ECAL_LEDS,
            TableConstants.ECAL_TIME_SHIFTS,
            TableConstants.SVT_ALIGNMENTS,
            TableConstants.SVT_CALIBRATIONS,
            TableConstants.SVT_CHANNELS,
            TableConstants.SVT_DAQ_MAP,
            TableConstants.SVT_GAINS,
            TableConstants.SVT_PULSE_PARAMETERS,
            TableConstants.SVT_TIME_SHIFTS
    };
    
    public void testConditionsDev() {

        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.configure("/org/hps/conditions/config/conditions_dev.xml");
        manager.setConnectionResource("/org/hps/conditions/config/conditions_dev.properties");
        manager.register();
        try {
            manager.setDetector("HPS-Proposal2014-v8-6pt6", 0);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        for (String conditionsKey : conditionsKeys) {
            TableMetaData metaData = manager.findTableMetaData(conditionsKey);            
            ConditionsSeries series = manager.getConditionsSeries(metaData.getKey());            
            for (int i = 0; i < series.getNumberOfCollections(); i++) {
                ConditionsObjectCollection<AbstractConditionsObject> collection = series.getCollection(i);
                System.out.println("Printing " + collection.getObjects().size() 
                        + " objects in collection " + metaData.getKey() + " ...");
                for (ConditionsObject object : collection.getObjects()) {
                    System.out.println(object.toString());
                }
            }            
        }
    }

}
