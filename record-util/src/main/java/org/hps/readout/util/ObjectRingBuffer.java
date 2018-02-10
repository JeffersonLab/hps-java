package org.hps.readout.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>ObjectRingBuffer</code> is an implementation of the
 * {@link org.hps.readout.util.RingBuffer RingBuffer} class that can
 * store a set of parameterized objects in each of its cells. It's
 * {@link org.hps.readout.util.RingBuffer#addToCell(int, Object)
 * addToCell(int, Object)} method simply adds the indicated object to
 * the set stored in the specified cell.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The type of object that is stored in each of the
 * buffer's cells.
 */
public class ObjectRingBuffer<T> extends RingBuffer<Set<T>, T> {
    /**
     * Instantiates a new <code>ObjectRingBuffer</code> of the size
     * specified.
     * @param size - The size of the buffer.
     */
    @SuppressWarnings("unchecked")
    public ObjectRingBuffer(int size) {
        // Note: This bit of tortuous self-reflection is needed to
        // work around Java's somewhat abstruse handling of class
        // objects for parameterized classes. While it technically
        // reads as "unchecked" there really shouldn't be any way for
        // it to produce a cast exception.
        super(size, (Class<Set<T>>) (new HashSet<T>(0)).getClass().getSuperclass());
    }
    
    @Override
    public void addToCell(int position, T value) {
        super.getValue(position).add(value);
    }

    @Override
    public void clearValue() {
        super.getValue().clear();
    }
    
    @Override
    public void clearValue(int position) {
        super.getValue(position).clear();
    }
    
    // ==============================================================
    // Selector methods are overridden to return an unmodifiable Set
    // object instead of the actual underlying set object to prevent
    // a user from directly altering the underlying Set itself, which
    // could produce unexpected behavior.
    // ==============================================================
    
    @Override
    public Set<T> getValue() {
        return Collections.unmodifiableSet(super.getValue());
    }
    
    @Override
    public Set<T> getValue(int position) {
        return Collections.unmodifiableSet(super.getValue(position));
    }
    
    // ==============================================================
    // Mutator methods are overridden to prevent direct modification
    // of the underlying Set objects used by the buffer. Instead, the
    // underlying sets are simply cleared and the elements of the
    // arguments are added instead.
    // ==============================================================
    
    @Override
    public void setValue(Set<T> values) {
        setValue(0, values);
    }
    
    /**
     * Sets the current value in the buffer.
     * @param values - The new buffer values.
     */
    @SuppressWarnings("unchecked")
    public void setValue(T... values) {
        setValue(0, values);
    }
    
    /**
     * Sets the current value in the buffer.
     * @param values - The new buffer values.
     */
    public void setValue(Collection<T> values) {
        setValue(0, values);
    }
    
    @Override
    public void setValue(int position, Set<T> values) {
        setValue(position, (Collection<T>) values);
    }
    
    /**
     * Sets the value at the specified position in the buffer.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     * @param values - The new buffer values.
     */
    @SuppressWarnings("unchecked")
    public void setValue(int position, T... values) {
        // Make sure that the indicated position is valid, and then
        // clear the underlying buffer set.
        validatePosition(position);
        clearValue(position);
        
        // If the argument array is empty, then there is nothing new
        // to add. Similarly, a null argument is treated as an empty
        // array. Otherwise, add the argument elements.
        if(values != null && values.length != 0) {
            for(T element : values) { addToCell(0, element); }
        }
    }
    
    /**
     * Sets the value at the specified position in the buffer.
     * @param position - The position of the target value relative
     * to the current value. A value of <code>0</code> represents the
     * current position.
     * @param values - The new buffer values.
     */
    public void setValue(int position, Collection<T> values) {
        // Make sure that the indicated position is valid, and then
        // clear the underlying buffer set.
        validatePosition(position);
        clearValue(position);
        
        // If the argument collection is empty, then there is nothing
        // new to add. Similarly, a null argument is treated as an
        // empty collection. Otherwise, add the argument elements.
        if(values != null && !values.isEmpty()) {
            for(T element : values) { addToCell(0, element); }
        }
    }
    
    @Override
    protected void clearAll() {
        for(int i = 0; i < size(); i++) { clearValue(i); }
    }
    
    @Override
    protected void instantiateBuffer() {
        for(int i = 0; i < size(); i++) { super.setValue(i, new HashSet<T>()); }
    }
}