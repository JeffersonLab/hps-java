package org.hps.readout;

import java.util.ArrayList;
import java.util.List;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.lcsim.event.EventHeader;

public class SLICDataReadoutDriver<E> extends ReadoutDriver {
	protected final int flags;
	protected final Class<E> type;
	protected String collectionName = null;
	protected TempOutputWriter writer = null;
	
	protected SLICDataReadoutDriver(Class<E> classType) {
		this(classType, 0);
	}
	
	protected SLICDataReadoutDriver(Class<E> classType, int flags) {
		type = classType;
		this.flags = flags;
	}
	
	@Override
	public void startOfData() {
		ReadoutDataManager.registerCollection(collectionName, this, type, flags);
		writer = new TempOutputWriter(collectionName + ".log");
	}
	
	@Override
	public void endOfData() {
		writer.close();
	}
	
	@Override
	public void process(EventHeader event) {
		// Get the collection from the event header. If none exists,
		// just produce an empty list.
		List<E> slicData;
		if(event.hasCollection(type, collectionName)) {
			slicData = event.get(type, collectionName);
		} else {
			slicData = new ArrayList<E>(0);
		}
		
		writeData(slicData);
		
		// Add the SLIC data to the readout data manager.
		ReadoutDataManager.addData(collectionName, slicData, type);
	}
	
	@Override
	protected double getTimeDisplacement() {
		return 0;
	}
	
	public void setCollectionName(String collection) {
		collectionName = collection;
	}
	
	protected void writeData(List<E> data) {
		
	}
}
