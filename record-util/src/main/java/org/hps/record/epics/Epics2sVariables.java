package org.hps.record.epics;

import java.util.HashMap;
import java.util.Map;

/**
 * List of EPICS variables and their descriptions contained in the 2s data bank from Eng Run 2015 data.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class Epics2sVariables {

    /**
     * Map with variable names and description.
     */
    static final Map<String, String> VARIABLES = new HashMap<String, String>();

    /**
     * Variable definitions.
     */
    static {
        VARIABLES.put("MBSY2C_energy", "Beam energy according to Hall B BSY dipole string");
        VARIABLES.put("PSPECIRBCK", "Pair Spectrometer Current Readback");
        VARIABLES.put("HPS:LS450_2:FIELD", "Frascati probe field");
        VARIABLES.put("HPS:LS450_1:FIELD", "Pair Spectrometer probe field");
        VARIABLES.put("MTIRBCK", "Frascati Current Readback");
        VARIABLES.put("VCG2C21 2C21", "Vacuum gauge pressure");
        VARIABLES.put("VCG2C21A", "2C21A Vacuum gauge pressure");
        VARIABLES.put("VCG2C24A", "2C24A Vacuum gauge pressure");
        VARIABLES.put("VCG2H00A", "2H00 Vacuum gauge pressure");
        VARIABLES.put("VCG2H01A", "2H01 Vacuum gauge pressure");
        VARIABLES.put("VCG2H02A", "2H02 Vacuum gauge pressure");
        VARIABLES.put("scaler_calc1", "Faraday cup current");
        VARIABLES.put("scalerS12b", "HPS-Left beam halo count");
        VARIABLES.put("scalerS13b", "HPS-Right beam halo count");
        VARIABLES.put("scalerS14b", "HPS-Top beam halo count");
        VARIABLES.put("scalerS15b", "HPS-SC beam halo count");
        VARIABLES.put("hallb_IPM2C21A_XPOS", "Beam position X at 2C21");
        VARIABLES.put("hallb_IPM2C21A_YPOS", "Beam position Y at 2C21");
        VARIABLES.put("hallb_IPM2C21A_CUR", "Current at 2C21");
        VARIABLES.put("hallb_IPM2C24A_XPOS", "Beam position X at 2C24");
        VARIABLES.put("hallb_IPM2C24A_YPOS", "Beam position Y at 2C24");
        VARIABLES.put("hallb_IPM2C24A_CUR", "Current at 2C24");
        VARIABLES.put("hallb_IPM2H00_XPOS", "Beam position X at 2H00");
        VARIABLES.put("hallb_IPM2H00_YPOS", "Beam position Y at 2H00");
        VARIABLES.put("hallb_IPM2H00_CUR", "Current at 2H00");
        VARIABLES.put("hallb_IPM2H02_YPOS", "Beam position X at 2H02");
        VARIABLES.put("hallb_IPM2H02_XPOS", "Beam position Y at 2H02");
    }

    /**
     * Get the variable map.
     * 
     * @return the variable map
     */
    public static Map<String, String> getVariables() {
        return VARIABLES;
    }

    /**
     * Do not allow class instantiation.
     */
    private Epics2sVariables() {
    }
}
