/**
 * 
 */
package org.hps.readout.svt;

import org.lcsim.event.GenericObject;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderData implements GenericObject {

    private final int num;
    private final int eventCount;
    private final int overflowError;
    private final int syncError;
    private final int skipCount;
    private final int multisampleCount;
    
    
    /**
     * 
     */
    public SvtHeaderData(int num, int eventCount, int overflowError, int syncError, int skipCount, int multisampleCount) {
        this.eventCount = eventCount;
        this.num = num;
        this.syncError = syncError;
        this.overflowError = overflowError;
        this.skipCount = skipCount;
        this.multisampleCount = multisampleCount;
    }

    
    public int getNum() {
        return this.num;
    }

    public int getEventCount() {
        return this.eventCount;
    }

    public int getOverflowError() {
        return this.overflowError;
    }

    public int getSyncError() {
        return this.syncError;
    }
    
    public int getSkipCount() {
        return this.skipCount;
    }
    
    public int getMultisampleCount() {
        return this.multisampleCount;
    }


    @Override
    public int getNInt() {
        return 6;
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
                value = this.num;
                break;
            case 1:
                value = this.eventCount;
                break;
            case 2:
                value = this.overflowError;
                break;
            case 3:
                value = this.syncError;
                break;
            case 4:
                value = this.skipCount;
                break;
            case 5:
                value = this.multisampleCount;
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
