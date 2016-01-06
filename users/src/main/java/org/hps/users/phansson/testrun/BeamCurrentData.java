/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson.testrun;

/**
 *
 * @author phansson
 */
public class BeamCurrentData {
    
    private double _int_current;
    private int _start_time;
    private int _stop_time;
    private int _run_nr;
    
    public BeamCurrentData(int run,int start,int stop,double int_cur) {
        _run_nr = run;
        _start_time = start;
        _stop_time = stop;
        _int_current = int_cur;
        
    }
    
    public int getRun() {
        return this._run_nr;
    }
    public int getStartTime() {
        return this._start_time;
    }
    public int getStopTime() {
        return this._stop_time;
    }
    public double getIntCurrent() {
        return this._int_current;
    }
    public double getIntCurrentError() {
        return this._int_current*0.0; //0.05;
    }
    
    public String toString() {
         String str = String.format("%10s\t%8d\t%8d\t%8.2f+-%.2f",getRun(),getStartTime(),getStopTime(),getIntCurrent(),getIntCurrentError());
         return str;
    }
    
    
    
}
