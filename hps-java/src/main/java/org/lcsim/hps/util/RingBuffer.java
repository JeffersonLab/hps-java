package org.lcsim.hps.util;

/**
 * Ring buffer for storing ECal (and possibly SVT) samples for trigger and readout
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: RingBuffer.java,v 1.5 2012/04/10 01:00:13 meeg Exp $
 */
public class RingBuffer {

	protected double[] array;
	protected int size;
	protected int ptr;

	public RingBuffer(int size) {
		this.size = size;
		array = new double[size]; //initialized to 0
		ptr = 0;
	}

	/**
	 * 
	 * @return value stored at current cell
	 */
	public double currentValue() {
		return array[ptr];
	}

	//return content of specified cell (pos=0 for current cell)
	public double getValue(int pos) {
		return array[((ptr + pos) % size + size) % size];
	}

	/**
	 * Clear value at current cell and step to the next one
	 */
	public void step() {
		array[ptr] = 0;
		ptr++;
		if (ptr == size) {
			ptr = 0;
		}
	}

	/**
	 * Add given value to specified cell
	 * @param pos Target position relative to current cell (pos=0 for current cell)
	 * @param val 
	 */
	public void addToCell(int pos, double val) {
		array[(ptr + pos) % size] += val;
	}
}
