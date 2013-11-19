package org.lcsim.hps.conditions.ecal;

import java.util.LinkedHashMap;

/**
 * This class maps ECAL channel IDs from the database to ECal gain parameters.
 */
public class EcalGainCollection extends LinkedHashMap<Integer,EcalGain> {
    /**
     * Class constructor.
     */
    EcalGainCollection() {        
    }
}
