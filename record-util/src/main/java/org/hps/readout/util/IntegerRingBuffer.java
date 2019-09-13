package org.hps.readout.util;

/**
 * Class <code>IntegerRingBuffer</code> is an implementation of
 * {@link org.hps.readout.ecal.updated.RingBuffer RingBuffer} for
 * integers. Each buffer cell represents a integer value, which can
 * be modified through the method {@link
 * org.hps.readout.util.RingBuffer#addToCell(int, Object)
 * addToCell(int, Object)}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class IntegerRingBuffer extends RingBuffer<Integer, Integer> {
    /**
     * Instantiates an <code>IntegerRingBuffer</code> of the
     * specified size and initializes all values to zero.
     * @param size - The number of entries in the buffer.
     */
    public IntegerRingBuffer(int size) {
        super(size, Integer.class);
    }
    
    /**
     * Instantiates an <code>IntegerRingBuffer</code> of the
     * specified size and initializes all values to the indicated
     * value.
     * @param size - The number of entries in the buffer.
     * @param initialValue - The initial value of buffer cells.
     */
    public IntegerRingBuffer(int size, int initialValue) {
        super(size, Integer.class);
        setAll(initialValue);
    }
    
    @Override
    public void addToCell(int position, Integer value) {
        validatePosition(position);
        setValue(position, getValue(position) + value);
    }
    
    @Override
    public void clearValue() {
        setValue(0);
    }
    
    @Override
    public void clearValue(int position) {
        setValue(position, 0);
    }
    
    @Override
    protected void clearAll() {
        setAll(0);
    }
    
    @Override
    protected void instantiateBuffer() {
        clearAll();
    }
}