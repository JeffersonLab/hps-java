package org.hps.conditions.svt;

/**
 * This class is a data holder for associating a time shift with a specific sensor
 * by FPGA and hybrid numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShift {
    
    int fpga;
    int hybrid;
    double timeShift;
    
    /**
     * Fully qualified constructor.
     * @param fpga The FPGA number.
     * @param hybrid The hybrid number.
     * @param timeShift The time shift.
     */
    SvtTimeShift(int fpga, int hybrid, double timeShift) {
        this.fpga = fpga;
        this.hybrid = hybrid;
        this.timeShift = timeShift;
    }
    
    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    int getFpga() {
        return fpga;
    }
    
    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    int getHybrid() {
        return hybrid;
    }
    
    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getTimeShift() {
        return timeShift;
    }
}
