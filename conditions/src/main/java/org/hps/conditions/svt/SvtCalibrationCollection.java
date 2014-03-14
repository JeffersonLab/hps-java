package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * This class is a collection of {@link SvtCalibration} objects associated to their 
 * SVT channel IDs from the database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtCalibrationCollection extends ConditionsObjectCollection<SvtCalibration> {

    /**
     * Class constructor.
     */
    SvtCalibrationCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
}
