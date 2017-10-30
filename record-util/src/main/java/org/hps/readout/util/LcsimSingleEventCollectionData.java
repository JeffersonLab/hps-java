package org.hps.readout.util;

import java.util.ArrayList;
import java.util.List;

import org.hps.readout.ReadoutDriver;

public class LcsimSingleEventCollectionData<T> extends LcsimCollection<T> {
	private final List<T> data;
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType) {
		this(collectionName, productionDriver,objectType, true, 0, null);
	}
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent) {
		this(collectionName, productionDriver,objectType, persistent, 0, null);
	}
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags) {
		this(collectionName, productionDriver,objectType, true, flags, null);
	}
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent, int flags) {
		this(collectionName, productionDriver,objectType, persistent, flags, null);
	}
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags, String readoutName) {
		this(collectionName, productionDriver,objectType, true, flags, readoutName);
	}
	
	public LcsimSingleEventCollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent,
			int flags, String readoutName) {
		super(collectionName, productionDriver, objectType, persistent, Double.NaN, flags, readoutName);
		this.data = new ArrayList<T>();
	}
	
	public List<T> getData() {
		return data;
	}
}
