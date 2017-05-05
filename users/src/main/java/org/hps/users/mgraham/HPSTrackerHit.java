/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.mgraham;

import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;

public class HPSTrackerHit extends BaseRawTrackerHit{
        double t0;
        double amp;                
      public HPSTrackerHit(
            long id,
            int time,
            short[] adcValues, double t0, double Amp) {
        this.cellId = id;
        this.packedID = new Identifier(id);
        this.time = time;
        this.adcValues = adcValues;
        this.t0=t0;
        this.amp=Amp;        
    }
      
      public HPSTrackerHit(
            RawTrackerHit rth, double t0, double Amp) {
        this.cellId = rth.getCellID();
        this.packedID = new Identifier(rth.getCellID());
        this.time = rth.getTime();
        this.adcValues = rth.getADCValues();
        this.t0=t0;
        this.amp=Amp;        
    }
      
      public double getT0(){return t0;}
       public double getAmp(){return amp;}               
    
}
