package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * A collection of {@link SvtPulseParameters} objects stored by SVT channel ID.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtPulseParametersCollection extends ConditionsObjectCollection<SvtPulseParameters> {
	
    public SvtPulseParametersCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    
}
