package org.hps.recon.ecal;

import org.lcsim.event.GenericObject;

/**
 * GenericObject to store hit information for FADC channels not corresponding to
 * ECal crystals. Intended for scintillator paddles and other hardware plugged
 * into unused FADC channels.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class FADCGenericHit implements GenericObject {

    private final int readoutMode;
    private final int crate;
    private final int slot;
    private final int channel;
    private final int[] data;

    public FADCGenericHit(int readoutMode, int crate, int slot, int channel, int[] data) {
        this.readoutMode = readoutMode;
        this.crate = crate;
        this.slot = slot;
        this.channel = channel;
        this.data = data;
    }
    
    public FADCGenericHit(GenericObject object) {
        this.readoutMode = getReadoutMode(object);
        this.crate = getCrate(object);
        this.slot = getSlot(object);
        this.channel = getChannel(object);
        this.data = getData(object);
    }

    @Override
    public int getNInt() {
        return 4 + data.length;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int i) {
        switch (i) {
            case 0:
                return readoutMode;
            case 1:
                return crate;
            case 2:
                return slot;
            case 3:
                return channel;
            default:
                return data[i-4];
        }
    }

    @Override
    public float getFloatVal(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public double getDoubleVal(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public boolean isFixedSize() {
        return true;
    }

    public int getReadoutMode() {
        return readoutMode;
    }

    public int getCrate() {
        return crate;
    }

    public int getSlot() {
        return slot;
    }

    public int getChannel() {
        return channel;
    }

    public int[] getData() {
        return data;
    }

    public static int getReadoutMode(GenericObject object) {
        return object.getIntVal(0);
    }

    public static int getCrate(GenericObject object) {
        return object.getIntVal(1);
    }

    public static int getSlot(GenericObject object) {
        return object.getIntVal(2);
    }

    public static int getChannel(GenericObject object) {
        return object.getIntVal(3);
    }

    public static int[] getData(GenericObject object) {
        int[] data = new int[object.getNInt()-4];
        for (int i=0;i<data.length;i++) {
            data[i] = object.getIntVal(i+4);
        }
        return data;
    }    
}
