package org.hps.conditions.ecal;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * This class maps ECAL channel IDs from the database to ECal gain parameters.
 */
public class EcalGainCollection extends ConditionsObjectCollection<EcalGain> {
    
    /**
     * Class constructor.
     */
    EcalGainCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
}
