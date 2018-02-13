package org.hps.readout.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Class <code>RingBuffer</code> is a framework for defining a buffer
 * which loops around at the end back to the beginning. A subclass is
 * able to define an implementation of the class for any object type.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class RingBuffer<T, V> implements Iterable<T> {
    /**
     * The array containing the buffer data.
     */
    private T[] array;
    /**
     * The current position within the buffer.
     */
    private int index = 0;
    
    /**
     * Instantiates a ring buffer of the indicated size and for the
     * indicated object type.
     * @param size - The size of the buffer.
     * @param objectType - The class of the object that is to be
     * stored in the buffer.
     */
    @SuppressWarnings("unchecked")
    protected RingBuffer(int size, Class<T> objectType) {
        array = (T[]) Array.newInstance(objectType, size);
        instantiateBuffer();
    }
    
    /**
     * Adds a value or object to the specified buffer cell. The exact
     * behavior can vary based on the nature of parameterized object
     * type which the buffer contains.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     * @param value - The object to add to the cell.
     */
    public abstract void addToCell(int position, V value);
    
    /**
     * Clears the value at the current position in the buffer. The
     * contents of the cell will be cleared as appropriate to the
     * parameterized object type.
     */
    public abstract void clearValue();
    
    /**
     * Clears the contents at the specified position in the buffer.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     */
    public abstract void clearValue(int position);
    
    /**
     * Gets the current contents in the buffer.
     * @return Returns the current contents.
     */
    public T getValue() {
        return array[index];
    }
    
    /**
     * Returns the contents at the specified position in the buffer.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     * @return Returns the contents at the specified position of the
     * buffer.
     */
    public T getValue(int position) {
        validatePosition(position);
        return array[((index + position) % array.length + array.length) % array.length];
    }
    
    @Override
    public Iterator<T> iterator() {
        return new RingBufferIterator(this);
    }
    
    /**
     * Sets the current value in the buffer.
     * @param value - The new buffer value.
     */
    public void setValue(T value) {
        array[index] = value;
    }
    
    /**
     * Sets the value at the specified position in the buffer.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     * @param value - The new buffer value.
     */
    public void setValue(int position, T value) {
        validatePosition(position);
        array[((index + position) % array.length + array.length) % array.length] = value;
    }
    
    /**
     * Gets the size of the buffer.
     * @return Returns the size of the buffer.
     */
    public int size() {
        return array.length;
    }
    
    /**
     * Steps the buffer forward by one step.
     */
    public void stepForward() {
        // Increment the current index position.
        index++;
        
        // If the end of the array has been reached, wrap around to
        // the front.
        if(index == array.length) {
            index = 0;
        }
    }
    
    /**
     * Clears all values in the buffer.
     */
    protected abstract void clearAll();
    
    /**
     * Performs any actions necessary to instantiate the buffer.
     */
    protected abstract void instantiateBuffer();
    
    /**
     * Sets all values values in the buffer to the specified value.
     * @param value - The value to which all buffer entries should be
     * set.
     */
    protected void setAll(T value) {
        for(int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }
    
    /**
     * Produces an {@link java.lang.ArrayIndexOutOfBoundsException
     * ArrayIndexOutOfBoundsException} exception if the requested
     * buffer position is not valid.
     * @param position - The index to validate.
     * @throws ArrayIndexOutOfBoundsException Occurs if the specified
     * index meets the condition <code>Math.abs(position) >=
     * size()</code>.
     */
    protected final void validatePosition(int position) throws ArrayIndexOutOfBoundsException {
        if(position >= array.length || position <= -array.length) {
            throw new ArrayIndexOutOfBoundsException("Array index " + position + " is invalid for buffer size " + array.length + ".");
        }
    }
    
    /**
     * Class <code>RingBufferIterator</code> is an implementation of
     * {@link java.util.Iterator Iterator} for the class {@link
     * org.hps.readout.ecal.updated.NumericRingBuffer
     * NumericRingBuffer} and any subclasses.
     * 
     * @author Kyle McCarty <mccarty@jlab.org>
     */
    private class RingBufferIterator implements Iterator<T> {
        private int position = 0;
        private final RingBuffer<T, V> buffer;
        
        private RingBufferIterator(RingBuffer<T, V> base) {
            buffer = base;
        }
        
        @Override
        public boolean hasNext() {
            return (position < buffer.size());
        }
        
        @Override
        public T next() {
            if(!hasNext()) { throw new NoSuchElementException(); }
            T nextValue = buffer.getValue(position);
            position++;
            return nextValue;
        }
    }
}