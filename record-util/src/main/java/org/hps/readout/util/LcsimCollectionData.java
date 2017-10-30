package org.hps.readout.util;

import java.util.LinkedList;

import org.hps.readout.ReadoutDriver;

public class LcsimCollectionData<T> extends LcsimCollection<T> {
	private final LinkedList<TimedList<?>> data;
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, 0, null);
	}
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement,
			boolean persistent) {
		this(collectionName, productionDriver,objectType, persistent, globalTimeDisplacement, 0, null);
	}
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement,
			int flags) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, flags, null);
	}
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement,
			boolean persistent, int flags) {
		this(collectionName, productionDriver,objectType, persistent, globalTimeDisplacement, flags, null);
	}
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement,
			int flags, String readoutName) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, flags, readoutName);
	}
	
	public LcsimCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent, double globalTimeDisplacement,
			int flags, String readoutName) {
		super(collectionName, productionDriver, objectType, persistent, globalTimeDisplacement, flags, readoutName);
		this.data = new LinkedList<TimedList<?>>();
	}
	
	public LinkedList<TimedList<?>> getData() {
		return data;
	}
}