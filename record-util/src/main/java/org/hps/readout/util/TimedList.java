package org.hps.readout.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class TimedList<E> extends ArrayList<E> implements Comparable<TimedList<?>> {
	private static final long serialVersionUID = -4261502557924453657L;
	private final double listTime;
	private static final Comparator<TimedList<?>> COMPARATOR = new Comparator<TimedList<?>>() {
		@Override
		public int compare(TimedList<?> arg0, TimedList<?> arg1) {
			return arg0.compareTo(arg1);
		}
	};
	
	public TimedList(double time) {
		super();
		listTime = time;
	}
	
	public TimedList(double time, Collection<? extends E> data) {
		super(data);
		listTime = time;
	}
	
	public TimedList(double time, int initialCapacity) {
		super(initialCapacity);
		listTime = time;
	}
	
	public double getTime() {
		return listTime;
	}
	
	@Override
	public int compareTo(TimedList<?> arg0) {
		return Double.compare(listTime, arg0.listTime);
	}
	
	public static final Comparator<TimedList<?>> getComparator() {
		return COMPARATOR;
	}
}
