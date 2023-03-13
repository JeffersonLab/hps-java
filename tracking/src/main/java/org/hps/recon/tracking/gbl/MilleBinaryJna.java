package org.hps.recon.tracking.gbl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class MilleBinaryJna {
    
    public interface MilleBinaryInterface extends Library {
        MilleBinaryInterface INSTANCE = (MilleBinaryInterface) Native.loadLibrary("GBL",MilleBinaryInterface.class);
        Pointer MilleBinaryCtor(String fileName, int filenamesize, int doublePrec, int keepZeros, int aSize);
        void MilleBinary_close(Pointer self);
    }
    
    private Pointer self;
    
    public MilleBinaryJna(String fileName) {
        self = MilleBinaryInterface.INSTANCE.MilleBinaryCtor(fileName,fileName.length(), 0, 0, 200000);
    }

    public Pointer getPtr() {
        return self;
    }

    public void close() {
        MilleBinaryInterface.INSTANCE.MilleBinary_close(self);
    }

}
