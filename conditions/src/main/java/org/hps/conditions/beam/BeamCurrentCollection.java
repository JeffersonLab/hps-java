package org.hps.conditions.beam;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

public class BeamCurrentCollection extends ConditionsObjectCollection<BeamCurrent> {
    
    /**
     * Class constructor.
     */
    BeamCurrentCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    

}
