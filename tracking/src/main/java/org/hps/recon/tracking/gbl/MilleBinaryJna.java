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
        System.out.println("DEBUG::Tom::java::MilleBinaryJna("+fileName+")");
        self = MilleBinaryInterface.INSTANCE.MilleBinaryCtor(fileName,fileName.length(), 0, 0, 200000);
    }

    public Pointer getPtr() {
        System.out.println("DEBUG::Tom::java::MilleBinaryJna.getPtr");
        return self;
    }

    public void close() {
        System.out.println("DEBUG::Tom::java::MilleBinaryJna.close");
        MilleBinaryInterface.INSTANCE.MilleBinary_close(self);
    }

}
