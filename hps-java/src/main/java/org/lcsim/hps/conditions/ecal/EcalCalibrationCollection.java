package org.lcsim.hps.conditions.ecal;

import java.util.LinkedHashMap;

/**
 * This class represents a list of {@link EcalCalibration} objects and their ECAL channel IDs.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibrationCollection extends LinkedHashMap<Integer,EcalCalibration> {
    /**
     * Class constructor.
     */
    EcalCalibrationCollection() {        
    }
}
