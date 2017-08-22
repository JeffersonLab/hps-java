package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;

public class SLICDataReadoutDriver<E> extends ReadoutDriver {
	protected final Class<E> type;
	protected String collectionName = null;
	
	SLICDataReadoutDriver(Class<E> classType) {
		type = classType;
	}
	
	@Override
	public void startOfData() {
		ReadoutDataManager.registerCollection(collectionName, this, type);
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
}
