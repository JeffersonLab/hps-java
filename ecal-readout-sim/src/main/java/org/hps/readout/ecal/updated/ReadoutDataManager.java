package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class ReadoutDataManager extends Driver {
	private static final double BEAM_BUNCH_SIZE = 2.0;
	private static double currentTime = 0.0;
	private static final Map<String, Class<?>> collectionTypeMap = new HashMap<String, Class<?>>();
	private static final Map<String, Double> timeDisplacementMap = new HashMap<String, Double>();
	private static final Map<String, ReadoutDriver> collectionSourceMap = new HashMap<String, ReadoutDriver>();
	private static final Map<String, LinkedList<TimedList<?>>> collectionDataMap = new HashMap<String, LinkedList<TimedList<?>>>();
	
	private static final StringBuffer debugBuffer = new StringBuffer();
	
	@Override
	public void process(EventHeader event) {
		System.out.println("===== EVENT " + event.getEventNumber() + " =====");
		System.out.println(debugBuffer.toString());
		debugBuffer.delete(0, debugBuffer.length() - 1);
		currentTime += BEAM_BUNCH_SIZE;
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType) {
		// Make sure that all arguments are defined.
		if(collectionName == null) {
			throw new IllegalArgumentException("Error: Collection name must be defined.");
		} if(objectType == null) {
			throw new IllegalArgumentException("Error: Collection object class must be defined.");
		}
		
		// There should only be one collection for a given name.
		if(collectionTypeMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Collection \"" + collectionName + "\" of object type " + objectType.getSimpleName() + " already exists.");
		}
		
		// Input the collection into the data maps.
		collectionTypeMap.put(collectionName, objectType);
		collectionSourceMap.put(collectionName, productionDriver);
		collectionDataMap.put(collectionName, new LinkedList<TimedList<?>>());
		
		double tempTime = getTotalTimeDisplacement(collectionName);
		
		timeDisplacementMap.put(collectionName, tempTime); //getTotalTimeDisplacement(collectionName));
		
		// DEBUG
		debugBuffer.append("Registered readout collection \"" + collectionName + "\" of object type " + objectType.getSimpleName() + "."
				+ " Total time displacement for the collection is " + tempTime + " ns.\n");
	}
	
	public static final <T> void addData(String collectionName, double dataTime, Collection<T> data, Class<T> dataType) {
		// Validate that the collection has been registered.
		if(!collectionTypeMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" has not been registered.");
		}
		
		// Validate that the data type is correct.
		if(collectionTypeMap.get(collectionName) != dataType) {
			throw new IllegalArgumentException("Error: Saw data type \"" + dataType.getSimpleName() + "\" but expected data type \""
					+ collectionTypeMap.get(collectionName).getSimpleName() + "\" instead.");
		}
		
		// If the data is empty, then there is no need to add it to
		// the buffer.
		if(!data.isEmpty()) {
			// Add the new data to the data buffer.
			double time = Double.isNaN(dataTime) ? currentTime - timeDisplacementMap.get(collectionName) : dataTime;
			LinkedList<TimedList<?>> dataBuffer = collectionDataMap.get(collectionName);
			dataBuffer.add(new TimedList<T>(time, data));
			
			// DEBUG
			debugBuffer.append("Added " + data.size() + " objects of type " + dataType.getSimpleName() + " to the buffer for collection \""
					+ collectionName + "\" at real time " + currentTime + " and simulation time " + time + ".\n");
		}
	}
	
	public static final <T> void addData(String collectionName, Collection<T> data, Class<T> dataType) {
		addData(collectionName, Double.NaN, data, dataType);
	}
	
	public static final double getCurrentTime() {
		return currentTime;
	}
	
	/**
	 * Gets the length in nanoseconds of a single event (beam bunch).
	 * @return Returns the length in ns of a single beam bunch.
	 */
	public static final double getBeamBunchSize() {
		return BEAM_BUNCH_SIZE;
	}
	
	public static final <T> Collection<T> getData(double startTime, double endTime, String collectionName, Class<T> objectType) {
		// Verify that the a collection of the indicated name exists
		// and that it is the appropriate object type.
		if(collectionTypeMap.containsKey(collectionName)) {
			if(!objectType.isAssignableFrom(collectionTypeMap.get(collectionName))) {
				throw new IllegalArgumentException("Error: Expected object type " + objectType.getSimpleName() + " for collection \"" + collectionName
						+ ",\" but found object type " + collectionTypeMap.get(collectionName).getSimpleName() + ".");
			}
		} else {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
		}
		
		// Iterate through the data and collect all entries that have
		// an associated truth time within the given time range. The
		// lower bound is inclusive, the upper bound is exclusive.
		List<T> outputList = new ArrayList<T>();
		LinkedList<TimedList<?>> dataLists = collectionDataMap.get(collectionName);
		for(TimedList<?> dataList : dataLists) {
			if(dataList.getTime() >= startTime && dataList.getTime() < endTime) {
				// Add the items from the list to the output list.
				for(Object o : dataList) {
					if(objectType.isAssignableFrom(o.getClass())) {
						outputList.add(objectType.cast(o));
					} else {
						throw new ClassCastException("Error: Unexpected object of type " + o.getClass().getSimpleName() + " in collection \""
								+ collectionName + ".\"");
					}
				}
			}
		}
		
		// Return the collected items.
		return outputList;
	}
	
	public static final boolean checkCollectionStatus(String collectionName, double time) {
		// Verify that the requested collection exists.
		if(!timeDisplacementMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" is not a registered collection.");
		}
		
		// Otherwise, check if enough time has passed for the driver
		// which controls to the collection to have produced output
		// for the requested time period.
		return time <= getCurrentTime() + BEAM_BUNCH_SIZE - timeDisplacementMap.get(collectionName);
	}
	
	private static final double getTotalTimeDisplacement(String collectionName) {
		return getTotalTimeDisplacement(collectionName, new HashSet<String>());
	}
	
	private static final double getTotalTimeDisplacement(String collectionName, Set<String> dependents) {
		// Verify that the collection has been registered.
		if(!collectionTypeMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" has not been registered.");
		}
		
		// Get the list of dependency collections from the governing
		// readout driver for this collection.
		ReadoutDriver sourceDriver = collectionSourceMap.get(collectionName);
		Collection<String> dependencies = sourceDriver.getDependencies();
		
		// If there are no dependencies, just add the readout driver
		// local time displacement and return.
		if(dependencies.isEmpty()) {
			return sourceDriver.getTimeDisplacement();
		}
		
		// Add this collection to the list of dependents.
		dependents.add(collectionName);
		
		// Otherwise, iterate over the dependencies and determine the
		// total time displacement for each one. Keep the largest.
		double displacement = 0.0;
		for(String dependency : dependencies) {
			// If the dependency has already been seen, then there is
			// a circular dependency chain. Throw an error.
			if(dependents.contains(dependency)) {
				throw new RuntimeException("Error: Collection \"" + collectionName + "\" depends on collection \"" + dependency + ",\" but collection \""
						+ dependency + "\" also depends of collection \"" + collectionName + ".\"");
			}
			
			// Get the total time displacement for the dependency.
			double dependencyDisplacement = getTotalTimeDisplacement(dependency, dependents);
			
			// If this time is larger than previously seen largest
			// displacement, keep it.
			displacement = Math.max(displacement, dependencyDisplacement);
		}
		
		// Return the previously observed total displacement, plus
		// the largest total displacement from the dependencies.
		return sourceDriver.getTimeDisplacement() + displacement;
	}
}
