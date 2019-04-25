package org.hps.conditions.hodoscope;

public class HodoscopeChannelConstants {

    private HodoscopeCalibration calibration = null;
    private HodoscopeGain gain = null;
    private HodoscopeTimeShift timeShift = null;
    
    public HodoscopeCalibration getCalibration() {
        return calibration;
    }
    
    public HodoscopeGain getGain() {
        return gain;
    }
    
    public HodoscopeTimeShift getTimeShift() {
        return timeShift;
    }

    void setCalibration(HodoscopeCalibration calibration) {
        this.calibration = calibration;
    }
    
    void setGain(HodoscopeGain gain) {
        this.gain = gain;
    }
    
    void setTimeShift(HodoscopeTimeShift timeShift) {
        this.timeShift = timeShift;
    }
    
}
