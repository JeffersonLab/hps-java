package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 



public class TFileJna {

    public interface TFileInterface extends Library {

        TFileInterface INSTANCE = (TFileInterface) Native.loadLibrary("rootjna", TFileInterface.class);

        //TFile* TFileCtor(const char* fname, const char* option) {
        // not sure if I should pass a char[]
        Pointer TFileCtor(String fname, String option);
        void TFile_Close(Pointer self);
        void TFile_ls(Pointer self);
        void TFile_delete(Pointer self);
        Pointer TFile_Get1DHisto(Pointer self, String name);
        Pointer TFile_Get2DHisto(Pointer self, String name);
    }

    private Pointer self;

    public TFileJna(String name, String option) {

        self = TFileInterface.INSTANCE.TFileCtor(name,option);
    
    }

    public Pointer getPtr() {
        return self;
    }
  
    public void close() {

        TFileInterface.INSTANCE.TFile_Close(self);
    }

    public void ls() {
        TFileInterface.INSTANCE.TFile_ls(self);
    }


    public void delete() {
        TFileInterface.INSTANCE.TFile_delete(self);
    }


    Pointer get1DHisto(String name) {
        return TFileInterface.INSTANCE.TFile_Get1DHisto(self,name);
    }

    Pointer get2DHisto(String name) {
        return TFileInterface.INSTANCE.TFile_Get2DHisto(self,name);
    }
  
}
