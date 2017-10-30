package org.hps.readout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.util.LcsimCollectionData;
import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.hps.readout.util.TimedList;
import org.hps.readout.util.TriggerTime;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

public class ReadoutDataManager extends Driver {
	private static double deadTime = 0.0;
	private static double lastTrigger = Double.MIN_VALUE;
	private static int readoutWindow = 200;
	private static double triggerTimeDisplacement = 50;
	private static final double BEAM_BUNCH_SIZE = 2.0;
	private static double currentTime = 0.0;
	private static String largestDisplacement = null;
	private static final Set<ReadoutDriver> driverSet = new HashSet<ReadoutDriver>();
	private static final Map<String, LcsimCollectionData<?>> collectionMap = new HashMap<String, LcsimCollectionData<?>>();
	private static final Map<ReadoutDriver, Double> triggerTimeDisplacementMap = new HashMap<ReadoutDriver, Double>();
	private static final PriorityQueue<TriggerTime> triggerQueue = new PriorityQueue<TriggerTime>();
	
	private static final StringBuffer debugBuffer = new StringBuffer();
	
	private static LCIOWriter outputWriter = null;
	
	@Override
	public void startOfData() {
		try { outputWriter = new LCIOWriter(new File("C:\\cygwin64\\home\\Kyle\\newReadout.slcio")); }
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	@Override
	public void endOfData() {
		try { outputWriter.close(); }
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// TODO: Currently only works for a single trigger. Needs to support multiple triggers with different offsets.
		
		// Check the trigger queue.
		if(!triggerQueue.isEmpty()) {
			System.out.println("A trigger is queued for time " + triggerQueue.peek().getTriggerWriteTime() + "; it is now " + getCurrentTime() + ".");
			System.out.println("The most displaced trigger collection, \"" + largestDisplacement + ",\" is at time "
					+ (getCurrentTime() - collectionMap.get(largestDisplacement).getGlobalTimeDisplacement()));
			
			// Check the earliest possible trigger write time.
			boolean isWritable = checkCollectionStatus(largestDisplacement, triggerQueue.peek().getTriggerWriteTime());
			
			// If all collections are available to be written, the
			// event should be output.
			if(isWritable) {
				System.out.println("\tA trigger is writable.");
				// Store the current trigger data.
				TriggerTime trigger = triggerQueue.poll();
				lastTrigger = trigger.getTriggerTime();
				
				// Make a new LCSim event.
				int triggerEventNumber = event.getEventNumber() - ((int) Math.floor((getCurrentTime() - trigger.getTriggerTime()) / 2.0));
				EventHeader lcsimEvent = new BaseLCSimEvent(DatabaseConditionsManager.getInstance().getRun(),
						triggerEventNumber, event.getDetectorName(), (long) 4 * (Math.round(trigger.getTriggerTime() / 4)), false);
				
				// Calculate the readout window time range.
				double startTime = trigger.getTriggerTime() - triggerTimeDisplacement;
				double endTime = startTime + readoutWindow;
				
				// Write out the writable collections into the event.
				for(LcsimCollectionData<?> collectionData : collectionMap.values()) {
					// Ignore any collections that are not set to be persisted.
					if(!collectionData.isPersistent()) {
						System.out.println("Ignoring collection \"" + collectionData.getCollectionName() + "\" - It is transient.");
						continue;
					}
					
					// Persisted collection should be added to event
					// within the readout window time range.
					storeCollection(startTime, endTime, collectionData, lcsimEvent);
				}
				
				// Write out any special on-trigger collections into
				// the event as well.
				for(ReadoutDriver driver : driverSet) {
					// Get the special collection(s) from the current
					// driver, if it exists.
					Collection<LcsimSingleEventCollectionData<?>> onTriggerData = driver.getOnTriggerData(trigger.getTriggerTime());
					
					// If there are special collections, write them.
					if(onTriggerData != null) {
						for(LcsimSingleEventCollectionData<?> triggerData : onTriggerData) {
							storeCollection(triggerData, lcsimEvent);
						}
					}
				}
				
				// Write the event to the output file.
				try { outputWriter.write(lcsimEvent); }
				catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		}
		
		// DEBUG :: Output debug logging.
		System.out.println("===== EVENT " + event.getEventNumber() + " =====");
		System.out.println(debugBuffer.toString());
		debugBuffer.delete(0, debugBuffer.length() - 1);
		currentTime += BEAM_BUNCH_SIZE;
	}
	
	private static final <T> void storeCollection(double startTime, double endTime, LcsimCollectionData<T> collectionData, EventHeader event) {
		// Get the trigger window data.
		List<T> triggerData = getDataList(startTime, endTime, collectionData.getCollectionName(), collectionData.getObjectType());
		
		// Store the trigger window data.
		storeCollection(collectionData.getCollectionName(), collectionData.getObjectType(), collectionData.getFlags(), collectionData.getReadoutName(),
				triggerData, event);
	}
	
	private static final <T> void storeCollection(LcsimSingleEventCollectionData<T> collectionData, EventHeader event) {
		storeCollection(collectionData.getCollectionName(), collectionData.getObjectType(), collectionData.getFlags(), collectionData.getReadoutName(),
				collectionData.getData(), event);
	}
	
	private static final <T> void storeCollection(String collectionName, Class<T> objectType, int flags, String readoutName,
			List<T> collectionData, EventHeader event) {
		// DEBUG :: Test the object type.
		System.out.println("Output collection type is " + objectType.getSimpleName());
		
		// Place the data into the LCSim event.
		if(readoutName == null) {
			event.put(collectionName, collectionData, objectType, flags);
		} else {
			event.put(collectionName, collectionData, objectType, flags, readoutName);
		}
		System.out.println("Adding collection \"" + collectionName + "\" of size " + collectionData.size() + " of type " + objectType.getSimpleName() + ".");
		for(T element : collectionData) {
			if(element instanceof Cluster) {
				System.out.println("\tCluster Hits: " + ((Cluster) element).getCalorimeterHits().size());
			}
		}
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType) {
		registerCollection(collectionName, productionDriver, objectType, 0, null, true);
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, boolean persistent) {
		registerCollection(collectionName, productionDriver, objectType, 0, null, persistent);
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags) {
		registerCollection(collectionName, productionDriver, objectType, flags, null, true);
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags, boolean persistent) {
		registerCollection(collectionName, productionDriver, objectType, flags, null, persistent);
	}
	
	public static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags, String readoutName) {
		registerCollection(collectionName, productionDriver, objectType, flags, readoutName, true);
	}
	
	private static final <T> void registerCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags, String readoutName,
			boolean persistent) {
		// Make sure that all arguments are defined.
		if(collectionName == null) {
			throw new IllegalArgumentException("Error: Collection name must be defined.");
		} if(objectType == null) {
			throw new IllegalArgumentException("Error: Collection object class must be defined.");
		} if(productionDriver == null) {
			throw new IllegalArgumentException("Error: Production driver must be defined.");
		}
		
		// There should only be one collection for a given name.
		if(collectionMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Collection \"" + collectionName + "\" of object type " + objectType.getSimpleName() + " already exists.");
		}
		
		// Create a collection data object.
		double tempTime = getTotalTimeDisplacement(collectionName, productionDriver);
		LcsimCollectionData<T> collectionData = new LcsimCollectionData<T>(collectionName, productionDriver, objectType, persistent, tempTime, flags, readoutName);
		collectionMap.put(collectionName, collectionData);
		
		// Track which collection has the largest time displacement.
		// This only applied if the registered collection is to be
		// persisted.
		if(persistent) {
			if(largestDisplacement == null) {
				largestDisplacement = collectionName;
			} else if(tempTime > collectionMap.get(largestDisplacement).getGlobalTimeDisplacement()) {
				largestDisplacement = collectionName;
			}
		}
		
		// Store the readout driver in the driver set.
		driverSet.add(productionDriver);
		
		// DEBUG
		debugBuffer.append("Registered readout collection \"" + collectionName + "\" of object type " + objectType.getSimpleName() + "."
				+ " Total time displacement for the collection is " + tempTime + " ns.\n");
	}
	
	public static final void registerTrigger(ReadoutDriver triggerDriver) {
		// Get the total time displacement for the trigger driver.
		double timeDisplacement = getTotalTimeDisplacement("", triggerDriver);
		
		/*
		for(String dependency : triggerDriver.getDependencies()) {
			double tempTime = getTotalTimeDisplacement(dependency);
			if(tempTime > timeDisplacement) {
				timeDisplacement = tempTime;
			}
		}
		timeDisplacement += triggerDriver.getTimeDisplacement();
		*/
		
		// Store the time displacement in the trigger driver map.
		triggerTimeDisplacementMap.put(triggerDriver, timeDisplacement);
		
		// DEBUG
		debugBuffer.append("Registered trigger driver of type \"" + triggerDriver.getClass().getSimpleName()
				+ "\" with a total time displacement of " + timeDisplacement + " ns.\n");
	}
	
	public static final <T> void addData(String collectionName, double dataTime, Collection<T> data, Class<T> dataType) {
		// Validate that the collection has been registered.
		if(!collectionMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" has not been registered.");
		}
		
		// Get the collection data object.
		LcsimCollectionData<?> collectionData = collectionMap.get(collectionName);
		
		// Validate that the data type is correct.
		if(collectionData.getObjectType() != dataType) {
			throw new IllegalArgumentException("Error: Saw data type \"" + dataType.getSimpleName() + "\" but expected data type \""
					+ collectionData.getObjectType().getSimpleName() + "\" instead.");
		}
		
		// If the data is empty, then there is no need to add it to
		// the buffer.
		if(!data.isEmpty()) {
			// Add the new data to the data buffer.
			double time = Double.isNaN(dataTime) ? currentTime - collectionData.getGlobalTimeDisplacement() : dataTime;
			LinkedList<TimedList<?>> dataBuffer = collectionData.getData();
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
	
	public static final <T> List<T> getDataList(double startTime, double endTime, String collectionName, Class<T> objectType) {
		// Get the collection data.
		LcsimCollectionData<?> collectionData = collectionMap.get(collectionName);
		
		// Verify that the a collection of the indicated name exists
		// and that it is the appropriate object type.
		if(collectionData != null) {
			if(!objectType.isAssignableFrom(collectionData.getObjectType())) {
				throw new IllegalArgumentException("Error: Expected object type " + objectType.getSimpleName() + " for collection \"" + collectionName
						+ ",\" but found object type " + collectionData.getObjectType().getSimpleName() + ".");
			}
		} else {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
		}
		
		// Iterate through the data and collect all entries that have
		// an associated truth time within the given time range. The
		// lower bound is inclusive, the upper bound is exclusive.
		List<T> outputList = new ArrayList<T>();
		LinkedList<TimedList<?>> dataLists = collectionData.getData();
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
	
	public static final <T> Collection<T> getData(double startTime, double endTime, String collectionName, Class<T> objectType) {
		return getDataList(startTime, endTime, collectionName, objectType);
	}
	
	public static final boolean checkCollectionStatus(String collectionName, double time) {
		// Verify that the requested collection exists.
		if(!collectionMap.containsKey(collectionName)) {
			throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" is not a registered collection.");
		}
		
		// Otherwise, check if enough time has passed for the driver
		// which controls to the collection to have produced output
		// for the requested time period.
		return time <= getCurrentTime() - collectionMap.get(collectionName).getGlobalTimeDisplacement();
	}
	
	public static final int getReadoutWindow() {
		return readoutWindow;
	}
	
	public static final void sendTrigger(ReadoutDriver driver) {
		// Calculate the trigger and readout times.
		double triggerTime = getCurrentTime() - triggerTimeDisplacementMap.get(driver);
		double readoutTime = triggerTime + (readoutWindow - triggerTimeDisplacement); //getCurrentTime() + (readoutWindow - triggerTimeDisplacement);
		
		// Add the trigger to the trigger queue.
		triggerQueue.add(new TriggerTime(triggerTime, readoutTime, driver));
		System.out.println("Added trigger to queue with trigger time " + triggerTime + " and readout time " + readoutTime + " from driver "
				+ driver.getClass().getSimpleName() + ".");
	}
	
	private static final double getTotalTimeDisplacement(String collectionName, ReadoutDriver productionDriver) {
		// Make sure that there are no circular dependencies.
		validateDependencies(collectionName, productionDriver, new HashSet<String>());
		
		// The total time displacement is the displacement of the
		// dependent collection with the largest displacement plus
		// the local time displacement of the production driver.
		double baseDisplacement = 0.0;
		for(String dependency : productionDriver.getDependencies()) {
			// All dependencies must already be registered. Check
			// that it is.
			double dependencyDisplacement = 0.0;
			if(collectionMap.containsKey(dependency)) {
				dependencyDisplacement = collectionMap.get(dependency).getGlobalTimeDisplacement();
			} else {
				throw new IllegalArgumentException("Error: Collection \"" + dependency + "\" has not been registered.");
			}
			
			// Select the largest value.
			baseDisplacement = Math.max(baseDisplacement, dependencyDisplacement);
		}
		
		// Return the sum of the largest base displacement and the
		// production driver.
		return baseDisplacement + productionDriver.getTimeDisplacement();
	}
	
	private static final void validateDependencies(String collectionName, ReadoutDriver productionDriver, Set<String> dependents) {
		// Add the current driver to the list of dependents.
		dependents.add(collectionName);
		
		// Check that none of the dependencies of the current driver
		// are also dependencies of a driver higher in the chain.
		for(String dependency : productionDriver.getDependencies()) {
			// The dependency must be registered.
			if(!collectionMap.containsKey(dependency)) {
				throw new IllegalArgumentException("Error: Collection \"" + dependency + "\" has not been registered.");
			}
			
			// Get the collection data for the dependency.
			LcsimCollectionData<?> collectionData = collectionMap.get(dependency);
			
			// Check that this dependency does not depend on the
			// higher driver.
			for(String dependent : dependents) {
				if(collectionData.getProductionDriver().getDependencies().contains(dependent)) {
					throw new IllegalStateException("Error: Collection \"" + dependency + "\" depends on collection \"" + dependent
							+ ",\" but collection \"" + dependent + "\" also depends of collection \"" + dependency + ".\"");
				}
			}
			
			// If there are no detected circular dependencies, then
			// perform the same check on the dependencies of this
			// dependency.
			Set<String> dependencySet = new HashSet<String>();
			dependencySet.addAll(dependents);
			validateDependencies(dependency, collectionData.getProductionDriver(), dependencySet);
		}
	}
}
