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
            @SuppressWarnings("rawtypes")
            ConditionsObjectCollection collection = (ConditionsObjectCollection)manager.getConditionsData(metaData.collectionClass, metaData.getKey());
            System.out.println(metaData.getKey() + " has " + collection.getObjects().size() + " objects");
        }
    }

}
