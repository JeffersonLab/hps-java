package org.hps.readout.util;

import org.hps.readout.ReadoutDriver;

public class LcsimCollection<T> {
	private int flags = 0;
	private final Class<T> objectType;
	private final double timeDisplacement;
	private final String collectionName;
	private String readoutName = null;
	private boolean persistent = true;
	private final ReadoutDriver productionDriver;
	private double windowBefore = Double.NaN;
	private double windowAfter = Double.NaN;
	
	public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement) {
		this.objectType = objectType;
		this.timeDisplacement = globalTimeDisplacement;
		this.productionDriver = productionDriver;
		this.collectionName = collectionName;
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
	
	public double getWindowAfter() {
		return windowAfter;
	}
	
	public double getWindowBefore() {
		return windowBefore;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	
	public void setPersistent(boolean state) {
		persistent = state;
	}
	
	public void setFlags(int value) {
		flags = value;
	}
	
	public void setReadoutName(String value) {
		readoutName = value;
	}
	
	public void setWindowBefore(double value) {
		windowBefore = value;
	}
	
	public void setWindowAfter(double value) {
		windowAfter = value;
	}
}