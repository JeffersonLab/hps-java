package org.hps.readout.util;

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
		getValue(position).add(value);
	}

	@Override
	public void clearValue() {
		getValue().clear();
	}
	
	@Override
	public void clearValue(int position) {
		getValue(position).clear();
	}
	
	@Override
	protected void clearAll() {
		for(int i = 0; i < size(); i++) { clearValue(i); }
	}
	
	@Override
	protected void instantiateBuffer() {
		for(int i = 0; i < size(); i++) { setValue(i, new HashSet<T>()); }
	}
}