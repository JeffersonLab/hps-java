package org.hps.readout.util;

import org.hps.readout.ReadoutDriver;

abstract class LcsimCollection<T> {
	private final int flags;
	private final Class<T> objectType;
	private final double timeDisplacement;
	private final String collectionName;
	private final String readoutName;
	private final boolean persistent;
	private final ReadoutDriver productionDriver;
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, 0, null);
	}
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement, boolean persistent) {
		this(collectionName, productionDriver,objectType, persistent, globalTimeDisplacement, 0, null);
	}
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement, int flags) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, flags, null);
	}
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement, boolean persistent, int flags) {
		this(collectionName, productionDriver,objectType, persistent, globalTimeDisplacement, flags, null);
	}
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement, int flags, String readoutName) {
		this(collectionName, productionDriver,objectType, true, globalTimeDisplacement, flags, readoutName);
	}
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent, double globalTimeDisplacement,
			int flags, String readoutName) {
		this.flags = flags;
		this.objectType = objectType;
		this.timeDisplacement = globalTimeDisplacement;
		this.productionDriver = productionDriver;
		this.readoutName = readoutName;
		this.collectionName = collectionName;
		this.persistent = persistent;
	}
	
	public String getCollectionName() {
		return collectionName;
	}
	
	public int getFlags() {
		return flags;
	}
	
	public Class<T> getObjectType() {
		return objectType;
	}
	
	public double getGlobalTimeDisplacement() {
		return timeDisplacement;
	}
	
	public ReadoutDriver getProductionDriver() {
		return productionDriver;
	}
	
	public String getReadoutName() {
		return readoutName;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
}