/**
 * 
 */
package org.hps.readout.svt;

import org.lcsim.event.GenericObject;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderDataInfo implements GenericObject {

    private final int num;
    private final int header;
    private final int tail;
    private int[] multisampleheader;
    
    
        
    public SvtHeaderDataInfo(int num, int header, int tail) {
        this.num = num;
        this.header = header;
        this.tail = tail;
    }
    
    public SvtHeaderDataInfo(int num, int header, int tail, int[] multisampleheaders) {
        this.num = num;
        this.header = header;
        this.tail = tail;
        this.multisampleheader = multisampleheaders;
    }
    
    public void setMultisampleHeaders(int[] multisampleheaders) {
        this.multisampleheader = multisampleheaders;
    }
    
    public int[] getMultisampleHeaders() {
        return this.multisampleheader;
    }
    
    public int getMultisampleHeader(int index) {
        if( index >= getMultisampleHeaders().length || index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        return this.multisampleheader[index];
    }
    
    public int getNum() {
        return this.num;
    }
    
    public int getTail() {
        return this.tail;
    }

    public int getHeader() {
        return this.header;
    }

    @Override
    public int getNInt() {
        return 3 + multisampleheader.length;
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
        switch (index) {
            case 0:
                return getNum();
            case 1:
                return getHeader();
            case 2:
                return getTail();
            default:
                if( (index-3) >= getMultisampleHeaders().length )
                    throw new RuntimeException("Invalid index " + Integer.toString(index));
                return getMultisampleHeader(index -3);
        }
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

    public static int getNum(GenericObject header) {
        return header.getIntVal(0);
    }

    public static int getHeader(GenericObject header) {
        return header.getIntVal(1);
    }

    public static int getTail(GenericObject header2) {
        return header2.getIntVal(2);
    }

    public static int getMultisample(int iMultiSample, GenericObject header) {
        return header.getIntVal(iMultiSample+3);
    }
    
    

}
