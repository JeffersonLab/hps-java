package org.hps.readout.ecal.updated;

import java.util.LinkedList;

class CollectionData<T> {
	private final int flags;
	private final Class<T> objectType;
	private final double timeDisplacement;
	private final LinkedList<TimedList<?>> data;
	private final ReadoutDriver productionDriver;
	
	CollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement) {
		this(collectionName, productionDriver,objectType, globalTimeDisplacement, 0);
	}
	
	CollectionData(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement, int flags) {
		this.flags = flags;
		this.objectType = objectType;
		this.timeDisplacement = globalTimeDisplacement;
		this.productionDriver = productionDriver;
		this.data = new LinkedList<TimedList<?>>();
	}
	
	int getFlags() {
		return flags;
	}
	
	Class<T> getObjectType() {
		return objectType;
	}
	
	double getGlobalTimeDisplacement() {
		return timeDisplacement;
	}
	
	ReadoutDriver getProductionDriver() {
		return productionDriver;
	}
	
	LinkedList<TimedList<?>> getData() {
		return data;
	}
}