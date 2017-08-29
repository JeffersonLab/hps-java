package org.hps.readout.ecal.updated;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lcsim.util.Driver;

public abstract class ReadoutDriver extends Driver {
	private int lcioFlags = 0;
	private boolean isTransient = false;
	private final Set<String> dependencies = new HashSet<String>();
	
	protected abstract double getTimeDisplacement();
	
	protected void addDependency(String collectionName) {
		dependencies.add(collectionName);
	}
	
	protected Collection<String> getDependencies() {
		return dependencies;
	}
	
	protected int getLCIOFlags() {
		return lcioFlags;
	}
	
	protected boolean isTransient() {
		return isTransient;
	}
	
	// TODO: This should probably be declared on a collection-to-collection basis to the manager.
	protected void setLCIOFlag(int bit, boolean state) {
		if(state) {
			lcioFlags = lcioFlags | (1 << bit);
		} else {
			lcioFlags = lcioFlags & ~(1 << bit);;
		}
	}
	
	protected void setTransient(boolean state) {
		isTransient = state;
	}
}
