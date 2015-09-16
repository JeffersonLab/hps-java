/**
 * 
 */
package org.hps.readout.svt;

import javassist.NotFoundException;

import org.lcsim.event.GenericObject;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtErrorBitData implements GenericObject {

    private final int rce;
    private final int feb;
    private final int hybrid;
    private final int apv;
    private final int error;
    
    
    /**
     * 
     */
    public SvtErrorBitData(int rce, int feb, int hybrid, int apv, int errorBit) {
        this.feb = feb;
        this.rce = rce;
        this.apv = apv;
        this.hybrid = hybrid;
        this.error = errorBit;
    }

    
    public int getRce() {
        return this.rce;
    }

    public int getFeb() {
        return this.feb;
    }

    public int getHybrid() {
        return this.hybrid;
    }

    public int getApv() {
        return this.apv;
    }
    
    public int getErrorBit() {
        return this.error;
    }


    @Override
    public int getNInt() {
        return 5;
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
    public int getIntVal(int index) {
        int value;
        switch (index) {
            case 0:
                value = this.rce;
                break;
            case 1:
                value = this.feb;
                break;
            case 2:
                value = this.hybrid;
                break;
            case 3:
                value = this.apv;
                break;
            case 4:
                value = this.error;
                break;
            default:
                throw new RuntimeException("Invalid index " + Integer.toString(index));
        }
        return value;
    }


    @Override
    public float getFloatVal(int index) {
        throw new ArrayIndexOutOfBoundsException();
    }


    @Override
    public double getDoubleVal(int index) {
        throw new ArrayIndexOutOfBoundsException();
    }


    @Override
    public boolean isFixedSize() {
        return true;
    }
    
    

}
