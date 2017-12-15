package org.hps.readout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.lcsim.util.Driver;

public abstract class ReadoutDriver extends Driver {
	private final Set<String> dependencies = new HashSet<String>();
	
	protected abstract double getTimeDisplacement();
	
	protected void addDependency(String collectionName) {
		dependencies.add(collectionName);
	}
	
	protected Collection<String> getDependencies() {
		return dependencies;
	}
	
	protected Collection<LcsimSingleEventCollectionData<?>> getOnTriggerData(double triggerTime) {
		return null;
	}
}