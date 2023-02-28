package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 



public class TH2DJna {

    public interface TH2DInterface extends Library {

        TH2DInterface INSTANCE = (TH2DInterface) Native.loadLibrary("rootjna", TH2DInterface.class);
        Pointer TH2DCtor(String name, String title, 
                         int binsX, double xlow, double xhigh,
                         int binsY, double ylow, double yhigh);
        void TH2D_Fill(Pointer self, double x , double y);
        int TH2D_FindBin(Pointer self, double x, double y);
        double TH2D_GetBinContent(Pointer self, int i);
        double TH2D_GetBinError(Pointer self, int i);
        
    }

    private Pointer self;


    public TH2DJna(Pointer h) {
        self = h;
    }
  
    public TH2DJna(String name, String title, 
                   int binsx, double xlow, double xhigh,
                   int binsy, double ylow, double yhigh) {

        self = TH2DInterface.INSTANCE.TH2DCtor(name,title,
                                               binsx,xlow,xhigh,
                                               binsy,ylow,yhigh);
    
    }

    //public int getEntries() {
    //    return TH2DInterface.INSTANCE.TH2D_GetEntries(self);
    //}

    public int findBin(double x, double y) {
        return TH2DInterface.INSTANCE.TH2D_FindBin(self,x,y);
    }
    public double getBinContent(int i){
        return TH2DInterface.INSTANCE.TH2D_GetBinContent(self,i);
    }
    public double getBinError(int i){
        return TH2DInterface.INSTANCE.TH2D_GetBinError(self,i);
    }
}
