package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * This class represents a set of bad channels in the SVT by their channel IDs
 * from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.staford.edu>
 */
public class SvtBadChannelCollection extends ConditionsObjectCollection<SvtBadChannel> {
    
    public SvtBadChannelCollection() {        
    }
    
    public SvtBadChannelCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    
}
