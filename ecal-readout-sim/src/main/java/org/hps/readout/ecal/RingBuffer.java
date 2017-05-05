package org.hps.readout.ecal;

/**
 * Ring buffer for storing ECal and SVT signals for trigger and readout.
 */
public class RingBuffer {

    protected double[] array;
    protected int ptr;

    public RingBuffer(int size) {
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
        return array[((ptr + pos) % array.length + array.length) % array.length];
    }

    /**
     * Clear value at current cell and step to the next one
     */
    public void step() {
        array[ptr] = 0;
        ptr++;
        if (ptr == array.length) {
            ptr = 0;
        }
    }

    /**
     * Add given value to specified cell
     * @param pos Target position relative to current cell (pos=0 for current cell)
     * @param val 
     */
    public void addToCell(int pos, double val) {
        array[(ptr + pos) % array.length] += val;
    }

    public int getLength() {
        return array.length;
    }
}
