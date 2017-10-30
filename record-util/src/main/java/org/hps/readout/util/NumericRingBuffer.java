package org.hps.readout.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Class <code>NumericRingBuffer</code> is a framework for defining a
 * buffer which loops around at the end back to the beginning. A
 * subclass is able to define an implementation of the class for any
 * object type that is a subclass of {@link java.lang.Number Number}.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class NumericRingBuffer<T extends Number> implements Iterable<T> {
	/**
	 * The array containing the buffer data.
	 */
	protected T[] array;
	/**
	 * The current position within the buffer.
	 */
	protected int index = 0;
	
	@SuppressWarnings("unchecked")
	protected NumericRingBuffer(int size, Class<T> numberType) {
		array = (T[]) Array.newInstance(numberType, size);
		clearAll();
	}
	
	/**
	 * Adds a value equal to <code>value</code> to the specified
	 * buffer cell.
	 * @param position - The position of the target value relative
	 * to the current value. A value of <code>0</code> represents the
	 * current position.
	 * @param value - The amount to add to the target buffer cell.
	 */
	public abstract void addToCell(int position, T value);
	
	/**
	 * Clears the value at the current position in the buffer. The
	 * value will be set to the appropriate numerical form of zero.
	 */
	public abstract void clearValue();
	
	/**
	 * Clears the value at the specified position in the buffer. The
	 * value will be set to the appropriate numerical form of zero.
	 * @param position - The position of the target value relative
	 * to the current value. A value of <code>0</code> represents the
	 * current position.
	 */
	public abstract void clearValue(int position);
	
	/**
	 * Gets the current value in the buffer.
	 * @return Returns the current value.
	 */
	public T getValue() {
		return array[index];
	}
	
	/**
	 * Returns the value at the specified position in the buffer.
	 * @param position - The position of the target value relative
	 * to the current value. A value of <code>0</code> represents the
	 * current position.
	 * @return Returns the value at the specified position of the
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
            throw new ArrayIndexOutOfBoundsException();
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
		private final NumericRingBuffer<T> buffer;
		
		private RingBufferIterator(NumericRingBuffer<T> base) {
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
