package org.hps.readout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.lcsim.util.Driver;

public abstract class ReadoutDriver extends Driver {
	protected boolean debug = true;
	private final Set<String> dependencies = new HashSet<String>();
	protected java.util.List<TempOutputWriter> writers = new java.util.ArrayList<TempOutputWriter>();
	
	protected ReadoutDriver() {
		ReadoutDataManager.registerReadoutDriver(this);
	}
	
	@Override
	public void endOfData() {
		for(TempOutputWriter writer : writers) {
			if(debug && writer != null) { writer.close(); }
		}
	}
	
	@Override
	public void startOfData() {
		// Set the debug status for each writer. If the writer should
		// output anything, tell it to delete the file on exit.
		if(debug) {
			for(TempOutputWriter writer : writers) {
				System.out.println(writer.toString());
				writer.initialize();
				writer.setEnabled(debug);
			}
		}
	}
	
	protected void addDependency(String collectionName) {
		dependencies.add(collectionName);
	}
	
	protected Collection<String> getDependencies() {
		return dependencies;
	}
	
	protected Collection<LcsimSingleEventCollectionData<?>> getOnTriggerData(double triggerTime) {
		return null;
	}
	
	protected abstract double getTimeDisplacement();
	
	protected abstract double getTimeNeededForLocalOutput();
	
	public void setDebug(boolean state) {
		debug = state;
	}
}