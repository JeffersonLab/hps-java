package org.hps.readout.util;

import java.util.LinkedList;

public class LcsimCollectionData<T> extends LcsimCollection<T> {
	private final LinkedList<TimedList<?>> data;
	
	public LcsimCollectionData(LcsimCollection<T> params) {
		super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), params.getGlobalTimeDisplacement());
		setPersistent(params.isPersistent());
		setFlags(params.getFlags());
		setReadoutName(params.getReadoutName());
		setWindowBefore(params.getWindowBefore());
		setWindowAfter(params.getWindowAfter());
		
		this.data = new LinkedList<TimedList<?>>();
	}
	
	public LcsimCollectionData(LcsimCollection<T> params, double timeDisplacement) {
		super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), timeDisplacement);
		setPersistent(params.isPersistent());
		setFlags(params.getFlags());
		setReadoutName(params.getReadoutName());
		setWindowBefore(params.getWindowBefore());
		setWindowAfter(params.getWindowAfter());
		
		this.data = new LinkedList<TimedList<?>>();
	}
	
	public LinkedList<TimedList<?>> getData() {
		return data;
	}
}