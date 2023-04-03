package org.hps.recon.tracking.gbl;

import com.sun.jna.Pointer;

/**
 * wrapper class for MilleBinary JNA functions
 * <p>
 * This class re-promotes those JNA functions into
 * class members and implements an extra constructor
 * with more sensible HPS defaults
 */
public class MilleBinaryJna {
    
    /**
     * internal handle to memory created and operated on by the GBL library
     */
    private Pointer self;

    /**
     * full one-to-one constructor
     */
    public MilleBinaryJna(String fileName, boolean doublePrec, boolean keepZeros, int aSize) {
        self = GblInterface.INSTANCE.MilleBinaryCtor(fileName, fileName.length(), doublePrec?1:0, keepZeros?1:0, aSize);
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
