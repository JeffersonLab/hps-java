package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 



public class TH3DJna {

    public interface TH3DInterface extends Library {

        TH3DInterface INSTANCE = (TH3DInterface) Native.loadLibrary("rootjna", TH3DInterface.class);
        Pointer TH3DCtor(String name, String title,
                         int xbins, double xlow, double xhigh,
                         int ybins, double ylow, double yhigh,
                         int zbins, double zlow, double zhigh);
        //int TH3D_GetEntries(Pointer self);
        //int TH3D_FindBin(Pointer self, double x);
        //double TH3D_GetBinContent(Pointer self, int i);
        //double TH3D_GetBinError(Pointer self, int i);
        void TH3D_Fill(Pointer self, double x, double y, double z);
        void TH3D_Write(Pointer self, Pointer ofile);
        void TH3D_Delete(Pointer self);
        
    }

    private Pointer self;

    
    public TH3DJna(Pointer h) {
        self = h;
    }
  
    public TH3DJna(String name, String title,
                   int xbins, double xlow, double xhigh,
                   int ybins, double ylow, double yhigh,
                   int zbins, double zlow, double zhigh) {
        

        self = TH3DInterface.INSTANCE.TH3DCtor(name,title,
                                               xbins,xlow,xhigh,
                                               ybins,ylow,yhigh,
                                               zbins,zlow,zhigh);
        
    }
    
    public void fill(double x, double y, double z) {
        TH3DInterface.INSTANCE.TH3D_Fill(self,x,y,z);
    }

    public void write(TFileJna ofile) {
        
        TH3DInterface.INSTANCE.TH3D_Write(self,ofile.getPtr());
    }

    public void delete() {
        TH3DInterface.INSTANCE.TH3D_Delete(self);
    }

    //public int getEntries() {
    //    return TH1DInterface.INSTANCE.TH1D_GetEntries(self);
    //}

    //public int findBin(double x) {
    //    return TH1DInterface.INSTANCE.TH1D_FindBin(self,x);
    //}
    //public double getBinContent(int i){
    //     return TH1DInterface.INSTANCE.TH1D_GetBinContent(self,i);
    //}
    //public double getBinError(int i){
    //    return TH1DInterface.INSTANCE.TH1D_GetBinError(self,i);
    //}
}
