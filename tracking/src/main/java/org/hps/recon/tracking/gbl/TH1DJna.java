package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 



public class TH1DJna {

    public interface TH1DInterface extends Library {

        TH1DInterface INSTANCE = (TH1DInterface) Native.loadLibrary("rootjna", TH1DInterface.class);
        Pointer TH1DCtor(String name, String title, int bins, double xlow, double xhigh);
        int TH1D_GetEntries(Pointer self);
        int TH1D_FindBin(Pointer self, double x);
        double TH1D_GetBinContent(Pointer self, int i);
        double TH1D_GetBinError(Pointer self, int i);
        
    }

    private Pointer self;


    public TH1DJna(Pointer h) {
        self = h;
    }
  
    public TH1DJna(String name, String title, int bins, double xlow, double xhigh) {

        self = TH1DInterface.INSTANCE.TH1DCtor(name,title,bins,xlow,xhigh);
    
    }

    public int getEntries() {
        return TH1DInterface.INSTANCE.TH1D_GetEntries(self);
    }

    public int findBin(double x) {
        return TH1DInterface.INSTANCE.TH1D_FindBin(self,x);
    }
    public double getBinContent(int i){
        return TH1DInterface.INSTANCE.TH1D_GetBinContent(self,i);
    }
    public double getBinError(int i){
        return TH1DInterface.INSTANCE.TH1D_GetBinError(self,i);
    }
}
