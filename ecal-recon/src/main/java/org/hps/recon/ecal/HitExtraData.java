/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.ecal;

import org.lcsim.event.GenericObject;

public class HitExtraData implements GenericObject {

    protected final int mode;
    protected int[] data;

    public HitExtraData(int mode, int[] data) {
        this.mode = mode;
        this.data = data;
    }

    @Override
    public int getNInt() {
        return 1 + data.length;
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
                return mode;
            default:
                return data[i - 1];
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

    public static class Mode7Data extends HitExtraData {

    private static final int ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE = 4; //FADC mode 7

        public Mode7Data(int amplLow, int amplHigh) {
            super(ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE, null);
            int[] newData = {amplLow, amplHigh};
            this.data = newData;
        }

        public int getAmplLow() {
            return data[0];
        }

        public int getAmplHigh() {
            return data[1];
        }

        public static int getAmplLow(GenericObject object) {
            return object.getIntVal(1);
        }

        public static int getAmplHigh(GenericObject object) {
            return object.getIntVal(2);
        }
    }
}
