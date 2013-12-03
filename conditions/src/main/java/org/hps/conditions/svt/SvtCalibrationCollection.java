package org.hps.conditions.svt;

import java.util.LinkedHashMap;

/**
 * This class is a collection of {@link SvtCalibration} objects associated to their 
 * SVT channel IDs from the database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtCalibrationCollection extends LinkedHashMap<Integer,SvtCalibration> {
    /**
     * Class constructor.
     */
    SvtCalibrationCollection() {        
    }
}
