package org.hps.record.triggerbank;

import java.util.Arrays;
import org.lcsim.event.GenericObject;

/**
 * Class <code>GenericObject</code> representation of an INT32/UINT32
 * bank read from EvIO. The bank header tag identifies the type of
 * data, and is stored as the first int in the <code>GenericObject</code>.
 * The contents of the bank are the remaining N-1 <code>int</code>
 * primitives. Constructors are provided from <code>int[]</code> (for
 * reading from EvIO) and from <code>GenericObject</code> (for reading
 * from LCIO).<br/>
 * <br/>
 * Subclasses must implement the two constructors and two abstract methods, plus
 * whatever methods are needed to access the parsed data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @see GenericObject
 */
public abstract class AbstractIntData implements GenericObject {
	/** The data bank. */
    protected int[] bank;
    
    /**
     * Constructs an <code>AbstractIntData</code> from a raw EvIO integer
     * bank. It is expected that the EvIO reader will verify that the
     * bank tag is the appropriate type before calling the constructor.
     * @param bank - An EvIO bank of <code>int</code> data.
     */
    protected AbstractIntData(int[] bank) {
        if(bank == null) { this.bank = new int[0]; }
        else { this.bank = Arrays.copyOf(bank, bank.length); }
    }
    
    /**
     * Create an <code>AbstractIntData</code> object from an LCIO
     * <code>genericObject</code>. Constructor requires that the
     * <code>GenericObject</code> tag match the expected EvIO header
     * tag type as defined by the implementing class.
     * @param data - The source data bank.
     * @param expectedTag - The required EvIO bank header tag.
     */
    protected AbstractIntData(GenericObject data, int expectedTag) {
    	// If the EvIO bank header tag is not the required type,
    	// produce an exception.
        if(getTag(data) != expectedTag) {
            throw new RuntimeException("expected tag " + expectedTag + ", got " + getTag(data));
        }
        
        // Otherwise, store the bank.
        this.bank = getBank(data);
    }
    
    /**
     * Gets the entire, unparsed integer data bank from the object.
     * @return Returns the data as an <code>int[]</code> array.
     */
    public int[] getBank() {
        return bank;
    }
    
    /**
     * Return the int bank of an AbstractIntData read from LCIO.
     * @param object
     * @return
     */
    public static int[] getBank(GenericObject object) {
        int N = object.getNInt() - 1;
        int[] bank = new int[N];
        for (int i = 0; i < N; i++) {
            bank[i] = object.getIntVal(i + 1);
        }
        return bank;
    }
    
    /**
     * Return a single value from the integer bank of an
     * <code>AbstractIntData</code>.
     * @param object - The bank from which to obtain the data.
     * @param index - The index of the data in the bank.
     * @return Returns the requested entry from the integer bank.
     */
    public static int getBankInt(GenericObject object, int index) {
        return object.getIntVal(index + 1);
    }
    
    /**
     * Returns the EvIO bank header tag expected for this data.
     * @return Returns the tag as an <code>int</code> primitive.
     */
    public abstract int getTag();
    
    /**
     * Returns the EVIO bank tag for a data object.
     * @param data - A <code>GenericObject</code> representing an integer
     * data bank.
     * @return Returns the EvIO tag identifying the type of bank the object
     * represents.
     */
    public static int getTag(GenericObject data) {
        return data.getIntVal(0);
    }
    
    /**
     * Parses the bank so the object can be used in analysis.
     */
    protected abstract void decodeData();
    
    @Override
    public int getNInt() {
        return bank.length + 1;
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
        if (index == 0) {
            return getTag();
        }
        return bank[index - 1];
    }
    
    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }
    
    @Override
    public double getDoubleVal(int index) {
        throw new UnsupportedOperationException("No double values in " + this.getClass().getSimpleName());
    }
    
    @Override
    public boolean isFixedSize() {
        return true;
    }
}