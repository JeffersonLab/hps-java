package org.hps.readout.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Class <code>TimedList</code> is an extension of {@link
 * java.util.ArrayList ArrayList} that additionally stores a single
 * time. This is used to store a collection of LCIO data associated
 * with a specific simulation time. It can be compared to other
 * timed lists, but only compares the list time, not the contents of
 * the lists.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <E> - The object type of the stored data.
 */
public class TimedList<E> extends ArrayList<E> implements Comparable<TimedList<?>> {
    private static final long serialVersionUID = -4261502557924453657L;
    /**
     * The simulation time of the data stored in the list.
     */
    private final double listTime;
    
    /**
     * A default comparator for comparing the times of two lists.
     */
    private static final Comparator<TimedList<?>> COMPARATOR = new Comparator<TimedList<?>>() {
        @Override
        public int compare(TimedList<?> t0, TimedList<?> t1) {
            return t0.compareTo(t1);
        }
    };
    
    /**
     * Instantiates a <code>TimedList</code> object with a simulation
     * time <code>time</code>.
     * @param time - The time in nanoseconds at which the data stored
     * in the list occurred.
     */
    public TimedList(double time) {
        super();
        listTime = time;
    }
    
    /**
     * Instantiates a <code>TimedList</code> object with a simulation
     * time <code>time</code> which contains the objects stored in
     * <code>data</code>.
     * @param time - The time in nanoseconds at which the data stored
     * in the list occurred.
     * @param data - A collection of data to be stored in the list.
     */
    public TimedList(double time, Collection<? extends E> data) {
        super(data);
        listTime = time;
    }
    
    /**
     * Instantiates a <code>TimedList</code> object with a simulation
     * time <code>time</code> with a given initial capacity.
     * @param time - The time in nanoseconds at which the data stored
     * in the list occurred.
     * @param initialCapacity - The initial capacity of the list.
     */
    public TimedList(double time, int initialCapacity) {
        super(initialCapacity);
        listTime = time;
    }
    
    @Override
    public int compareTo(TimedList<?> arg0) {
        return Double.compare(listTime, arg0.listTime);
    }
    
    /**
     * Gets the simulation time of the data stored in the list.
     * @return Returns the simulation time of the list data in units
     * of nanoseconds.
     */
    public double getTime() {
        return listTime;
    }
    
    /**
     * Gets a comparator that compares the simulation times for any
     * two <code>TimedList</code> objects.
     * @return
     */
    public static final Comparator<TimedList<?>> getComparator() {
        return COMPARATOR;
    }
}