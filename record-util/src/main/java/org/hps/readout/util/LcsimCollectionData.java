package org.hps.readout.util;

import java.util.LinkedList;

/**
 * Class <code>LcsimCollectionData</code> is an extension of {@link
 * org.hps.readout.util.LcsimCollection LcsimCollection} that can
 * additionally hold data. It contains a list of {@link
 * org.hps.readout.util.TimedList TimedList} objects, each of which
 * represent collection data generated at some simulation time.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class LcsimCollectionData<T> extends LcsimCollection<T> {
	/**
	 * The collection data. Each entry in the data list represents a
	 * specific simulation time quantum, while the list itself holds
	 * the collection object data.
	 */
	private final LinkedList<TimedList<?>> data;
	
	/**
	 * Creates a new <code>LcsimCollectionData</code> based on the
	 * collection parameters defined by the <code>params</code>
	 * object.
	 * @param params - The collection parameters.
	 */
	public LcsimCollectionData(LcsimCollection<T> params) {
		super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), params.getGlobalTimeDisplacement());
		setPersistent(params.isPersistent());
		setFlags(params.getFlags());
		setReadoutName(params.getReadoutName());
		setWindowBefore(params.getWindowBefore());
		setWindowAfter(params.getWindowAfter());
		
		this.data = new LinkedList<TimedList<?>>();
	}
	
	/**
	 * Creates a new <code>LcsimCollectionData</code> based on the
	 * collection parameters defined by the <code>params</code>
	 * object and a custom time displacement.
	 * @param params - The collection parameters.
	 * @param timeDisplacement - The time displacement for the
	 * collection. This overrides any values set in
	 * <code>params</code>.
	 */
	public LcsimCollectionData(LcsimCollection<T> params, double timeDisplacement) {
		super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), timeDisplacement);
		setPersistent(params.isPersistent());
		setFlags(params.getFlags());
		setReadoutName(params.getReadoutName());
		setWindowBefore(params.getWindowBefore());
		setWindowAfter(params.getWindowAfter());
		
		this.data = new LinkedList<TimedList<?>>();
	}
	
	/**
	 * Gets the collection data.
	 * @return Returns the collection data. Collection data is stored
	 * in a single list, which itself contains {@link
	 * org.hps.readout.util.TimedList TimedList} objects. Each
	 * <code>TimedList</code> object represents the collection data
	 * generated at the simulation time of the list, which can be
	 * obtained through the method {@link
	 * org.hps.readout.util.TimedList#getTime() TimedList.getTime()}.
	 */
	public LinkedList<TimedList<?>> getData() {
		return data;
	}
}