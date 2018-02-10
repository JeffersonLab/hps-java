package org.hps.readout.util;

/**
 * Class <code>DoubleRingBuffer</code> is an implementation of {@link
 * org.hps.readout.ecal.updated.RingBuffer RingBuffer} for doubles.
 * Each buffer cell represents a double value, which can be modified
 * through the method {@link
 * org.hps.readout.util.RingBuffer#addToCell(int, Object)
 * addToCell(int, Object)}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class DoubleRingBuffer extends RingBuffer<Double, Double> {
    /**
     * Instantiates a <code>DoubleRingBuffer</code> of the specified
     * size and initializes all values to zero.
     * @param size - The number of entries in the buffer.
     */
    public DoubleRingBuffer(int size) {
        super(size, Double.class);
    }
    
    /**
     * Instantiates a <code>DoubleRingBuffer</code> of the specified
     * size and initializes all values to the indicated value.
     * @param size - The number of entries in the buffer.
     * @param initialValue - The initial value of buffer cells.
     */
    public DoubleRingBuffer(int size, double initialValue) {
        super(size, Double.class);
        setAll(initialValue);
    }
    
    @Override
    public void addToCell(int position, Double value) {
        validatePosition(position);
        setValue(position, getValue(position) + value);
    }
    
    @Override
    public void clearValue() {
        setValue(0.0);
    }
    
    @Override
    public void clearValue(int position) {
        validatePosition(position);
        setValue(position, 0.0);
    }

    @Override
    protected void clearAll() {
        setAll(0.0);
    }
    
    @Override
    protected void instantiateBuffer() {
        clearAll();
    }
}