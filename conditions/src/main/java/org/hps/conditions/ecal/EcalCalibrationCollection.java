package org.hps.conditions.ecal;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;

/**
 * This class represents a list of {@link EcalCalibration} objects and their ECAL channel IDs.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibrationCollection extends ConditionsObjectCollection<EcalCalibration> {

    /**
     * Class constructor.
     */
    EcalCalibrationCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }

}
