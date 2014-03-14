package org.hps.conditions.svt;

import org.hps.conditions.BasicConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;


/**
 * This class represents a list of {@link SvtGain} objects associated 
 * with their SVT channel IDs from the database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtGainCollection extends BasicConditionsObjectCollection {
        
    public SvtGainCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
}
