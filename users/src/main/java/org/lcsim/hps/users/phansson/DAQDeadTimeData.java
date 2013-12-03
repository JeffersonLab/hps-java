/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

/**
 *
 * @author phansson
 */
public class DAQDeadTimeData {
    
    private double _daq_rate;
    private double _trigger_rate;
    private int _run_nr;
    
    public DAQDeadTimeData(int run,double trig, double daq) {
        _run_nr = run;
        _trigger_rate = trig;
        _daq_rate = daq;
        
    }
    
    public int getRun() {
        return this._run_nr;
    }
    public double getDAQLiveTimeFraction() {
        return this._daq_rate/this._trigger_rate;
    }
    public double getDAQLiveTimeFractionError() {
        return this._daq_rate/this._trigger_rate*0.0;//0.05;
    }
    
    public String toString() {
        String str = String.format("%10s\tDAQ rate %8.1f Hz\tTrigger rate%8.2f Hz -> livetime %8.2f +- %.2f",getRun(),_daq_rate,_trigger_rate,this.getDAQLiveTimeFraction(),this.getDAQLiveTimeFractionError());
         return str;
    }
    
    
    
}
