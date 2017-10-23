package org.hps.recon.tracking.gbl;

import org.lcsim.event.GenericObject;

public class GBLEventData implements GenericObject {

    /*
     * Interface enumerator to access the correct data
     */
    private static class GBLINT {

        public static final int RUNNR = 0;
        public static final int BANK_INT_SIZE = 1;
    }

    private static class GBLDOUBLE {

        public static final int BFIELD = 0;
        public static final int BANK_DOUBLE_SIZE = 1;
    }

    // array holding the integer data
    private int bank_int[] = new int[GBLINT.BANK_INT_SIZE];
    private double bank_double[] = new double[GBLDOUBLE.BANK_DOUBLE_SIZE];

    /**
     * Constructor with event number as parameter
     * 
     * @param eventNumber the event number
     */
    public GBLEventData(int eventNumber, double Bz) {
        setRunNr(eventNumber);
        setBfield(Bz);
    }

    public void setRunNr(int val) {
        bank_int[GBLINT.RUNNR] = val;
    }

    public int getRunNr() {
        return this.getIntVal(GBLINT.RUNNR);
    }

    public void setBfield(double val) {
        bank_double[GBLDOUBLE.BFIELD] = val;
    }

    public double getBfield() {
        return this.getDoubleVal(GBLDOUBLE.BFIELD);
    }

    @Override
    public int getNInt() {
        return GBLINT.BANK_INT_SIZE;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return GBLDOUBLE.BANK_DOUBLE_SIZE;
    }

    @Override
    public int getIntVal(int index) {
        return bank_int[index];
    }

    @Override
    public float getFloatVal(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDoubleVal(int index) {
        return bank_double[index];
    }

    @Override
    public boolean isFixedSize() {
        // TODO Auto-generated method stub
        return false;
    }

}
