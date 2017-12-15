package org.hps.readout.util;

import java.util.ArrayList;
import java.util.List;

public class LcsimSingleEventCollectionData<T> extends LcsimCollection<T> {
	private final List<T> data;
	
	public LcsimSingleEventCollectionData(LcsimCollection<T> params) {
		super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), params.getGlobalTimeDisplacement());
		setPersistent(params.isPersistent());
		setFlags(params.getFlags());
		setReadoutName(params.getReadoutName());
		setWindowBefore(params.getWindowBefore());
		setWindowAfter(params.getWindowAfter());
		
		this.data = new ArrayList<T>();
	}
	
	public List<T> getData() {
		return data;
	}
}
