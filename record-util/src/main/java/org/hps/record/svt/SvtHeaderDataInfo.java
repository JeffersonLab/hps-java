/**
 * 
 */
package org.hps.record.svt;

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
        if(multisampleheaders.length % 4 != 0) 
            throw new RuntimeException("invalid number of multisample headers, need to be %4==0: " + multisampleheaders.length);
        this.multisampleheader = multisampleheaders;
    }
    
    public int[] getMultisampleHeader(int multisampleIndex) {
        int index = multisampleIndex*4; 
        if( multisampleIndex >= getNumberOfMultisampleHeaders() || multisampleIndex < 0)
            throw new ArrayIndexOutOfBoundsException(" num " + num + " the multisampleIndex " + multisampleIndex + " with index " + index + " is larger than then number of multisamples in the array " + getNumberOfMultisampleHeaders());
        int[] words = new int[4];
        System.arraycopy(this.multisampleheader, index, words, 0, words.length);
        return words;
    }
    
    public int getNumberOfMultisampleHeaders() {
        return this.multisampleheader.length/4;
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
                if( (index-3) >= this.multisampleheader.length )
                    throw new RuntimeException("Invalid index " + Integer.toString(index));
                return this.multisampleheader[ index - 3 ];
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
    
    public String toString() {
        return "num " + Integer.toString(num) + " header " + Integer.toHexString(header) + " tail " + Integer.toHexString(tail) + " nMultisamples " + Integer.toString(multisampleheader.length);
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

    public static int[] getMultisampleHeader(int iMultisample, SvtHeaderDataInfo header) {
        return header.getMultisampleHeader(iMultisample);
    }

      
    

}
