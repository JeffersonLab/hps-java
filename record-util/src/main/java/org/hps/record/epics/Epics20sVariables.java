package org.hps.record.epics;

import java.util.HashMap;
import java.util.Map;

/**
 * Static list of variable names contained in 20s EPICS data block from Eng Run 2015 data along
 * with a brief description (if known).
 *
 * @author Jeremy McCormick, SLAC
 */
public final class Epics20sVariables {

    /**
     * Map with variable names and description.
     */
    static final Map<String, String> VARIABLES = new HashMap<String, String>();

    /**
     * Variable definitions.
     */
    static {
        VARIABLES.put("beam_stop.RBV", "beam stop motor position");
        VARIABLES.put("hps:svt_bot:motor.RBV", "SVT bottom motor position");
        VARIABLES.put("hps:svt_top:motor.RBV", "SVT top motor position");
        VARIABLES.put("hps:target:motor.RBV", "target motor position");
        VARIABLES.put("hps_collimator.RBV", "collimator motor position");
        VARIABLES.put("scalerS10b", "DWN-B beamline counter");
        VARIABLES.put("scalerS11b", "DWN-R beamline counter");
        VARIABLES.put("scalerS8b", "DWN-T beamline counter");
        VARIABLES.put("scalerS9b", "DWN-L beamline counter");
        VARIABLES.put("scaler_cS3b", "UPS-L  beamline counter");
        VARIABLES.put("scaler_cS4b", "UPS-R beamline counter");
        VARIABLES.put("scaler_cS5b", "TAG-L beamline counter");
        VARIABLES.put("scaler_cS6b", "TAG-T beamline counter");
        VARIABLES.put("scaler_cS7b", "TAG-T2 beamline counter");
        VARIABLES.put("SMRPOSB", "");
    }

    /**
     * Get the variable map with the names and descriptions.
     * 
     * @return the variable map
     */
    public static Map<String, String> getVariables() {
        return VARIABLES;
    }

    /**
     * Do not allow class instantiation.
     */
    private Epics20sVariables() {
    }
}
