package org.hps.conditions.ecal;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * A collection of bad channel IDs in the ECAL.
 */
public class EcalBadChannelCollection extends ConditionsObjectCollection<EcalBadChannel> {
    
    public EcalBadChannelCollection() {        
    }
    
    public EcalBadChannelCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    
}
