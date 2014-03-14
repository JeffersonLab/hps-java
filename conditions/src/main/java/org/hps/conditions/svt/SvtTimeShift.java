package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;

/**
 * This class is a data holder for associating a time shift with a specific sensor
 * by FPGA and hybrid numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShift extends AbstractConditionsObject {
    
    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    int getFpga() {
        return getFieldValue("fpga");
    }
    
    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    int getHybrid() {
        return getFieldValue("hybrid");
    }
    
    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getTimeShift() {
        return getFieldValue("time_shift");
    }
}
