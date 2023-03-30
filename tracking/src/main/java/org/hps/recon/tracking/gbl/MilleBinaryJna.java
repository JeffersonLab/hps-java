package org.hps.recon.tracking.gbl;

import com.sun.jna.Pointer;

public class MilleBinaryJna {
    
    private Pointer self;

    /**
     * full one-to-one constructor
     */
    public MilleBinaryJna(String fileName, int doublePrec, int keepZeros, int aSize) {
        self = GblInterface.INSTANCE.MilleBinaryCtor(fileName, fileName.length(), doublePrec, keepZeros, aSize);
    }
    
    /**
     * more useful constructor with HPS-sensible defaults
     */
    public MilleBinaryJna(String fileName) {
        self = GblInterface.INSTANCE.MilleBinaryCtor(fileName, fileName.length(), 0, 0, 200000);
    }

    public Pointer getPtr() {
        return self;
    }

    public void close() {
        GblInterface.INSTANCE.MilleBinary_close(self);
    }

}
