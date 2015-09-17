/**
 * 
 */
package org.hps.readout.svt;

import org.lcsim.event.GenericObject;

/**
 * Helper class to extract SVT header data from {@SvtHeaderDataInfo}.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderData {

    public SvtHeaderData() {}
    

    
    public static int getNum(SvtHeaderDataInfo data) {
        return data.getNum();
    }

    /*
    public static int getEventCount(SvtHeaderDataInfo data) {
        return 
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
    */
    

}
