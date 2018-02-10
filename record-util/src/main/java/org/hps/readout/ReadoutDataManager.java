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
import org.hps.readout.util.LcsimCollection;
import org.hps.readout.util.LcsimCollectionData;
import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.hps.readout.util.TimedList;
import org.hps.readout.util.TriggerTime;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * Class <code>ReadoutDataManager</code> is the central management
 * class for the HPS readout chain. It is responsible for tracking
 * most LCSim collection data, for syncing readout data production
 * drivers and their output, for passing managed data objects to
 * drivers as input, for managing triggers, and for writing out data.
 * <br/><br/>
 * More information on how a readout driver should interface
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ReadoutDataManager extends Driver {
	/**
	 * Defines the default size of the readout window in units of
	 * nanoseconds.
	 */
	private static int readoutWindow = 200;
	/**
	 * Defines the name of the output file for the run.
	 */
	private static String outputFileName = null;
	/**
	 * Defines where the trigger time should occur within the default
	 * readout window. For instance, a value of <code>t</code> means
	 * that a period of time equal to <code>t</code> will be included
	 * before the trigger time, and a period of time equal to
	 * <code>readoutWindow - t</code> will be included after it.
	 */
	private static double triggerTimeDisplacement = 50;
	/**
	 * Defines the length of an event in units of nanoseconds.
	 */
	private static final double BEAM_BUNCH_SIZE = 2.0;
	/**
	 * Tracks the current simulation time in units of nanoseconds.
	 */
	private static double currentTime = 0.0;
	/**
	 * Tracks the collection with the largest time displacement. This
	 * is used to determine how large the data buffer needs to be.
	 */
	private static String largestDisplacement = null;
	/**
	 * Tracks all registered readout drivers.
	 */
	private static final Set<ReadoutDriver> driverSet = new HashSet<ReadoutDriver>();
	/**
	 * Tracks all data collections which are managed by the readout
	 * manager as well as their properties.
	 */
	private static final Map<String, LcsimCollectionData<?>> collectionMap = new HashMap<String, LcsimCollectionData<?>>();
	/**
	 * Tracks the time displacement for trigger drivers.
	 */
	private static final Map<ReadoutDriver, Double> triggerTimeDisplacementMap = new HashMap<ReadoutDriver, Double>();
	/**
	 * Stores trigger requests from trigger drivers until enough time
	 * has passed to fully buffer the necessary readout data.
	 */
	private static final PriorityQueue<TriggerTime> triggerQueue = new PriorityQueue<TriggerTime>();
	/**
	 * A writer for writing readout events to an output LCIO file.
	 */
	private static LCIOWriter outputWriter = null;
	/**
	 * Tracks the largest amount of time that must be buffered before
	 * the trigger time for readout.
	 */
	private static double bufferBefore = 0.0;
	/**
	 * Tracks the largest amount of time that must be buffered after
	 * the trigger time for readout.
	 */
	private static double bufferAfter = 0.0;
	/**
	 * Tracks the largest amount of time that must be buffered to
	 * account for the time displacement of the trigger drivers.
	 */
	private static double bufferTrigger = 0.0;
	/**
	 * Tracks the total amount of time that must be buffered to allow
	 * for readout to occur.
	 */
	private static double bufferTotal = 0.0;
	
	private static StringBuffer outputBuffer = new StringBuffer();
	
	@Override
	public void startOfData() {
		// Instantiate the readout LCIO file.
		if(outputFileName == null) {
			throw new IllegalArgumentException("Error: Output file name not defined!");
		}
		//try { outputWriter = new LCIOWriter(new File("C:\\cygwin64\\home\\Kyle\\newReadout.slcio")); }
		try { outputWriter = new LCIOWriter(new File(outputFileName)); }
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		
		// Determine the total amount of time that must be included
		// in the data buffer in order to safely write out all data.
		// An extra 100 ns of data is retained as a safety, just in
		// case some driver needs to look unusually far back.
		bufferTotal = bufferTrigger + bufferBefore + bufferAfter + 100.0;
		System.out.println("Buffer Total: " + bufferTotal);
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
		// TODO: Currently only works for a single trigger. Should support multiple triggers with different offsets.
		
		// Check the trigger queue.
		if(!triggerQueue.isEmpty()) {
			// Check the earliest possible trigger write time.
			boolean isWritable = checkCollectionStatus(largestDisplacement, triggerQueue.peek().getTriggerWriteTime());
			
			// If all collections are available to be written, the
			// event should be output.
			if(isWritable) {
				// Store the current trigger data.
				TriggerTime trigger = triggerQueue.poll();
				
				// Make a new LCSim event.
				int triggerEventNumber = event.getEventNumber() - ((int) Math.floor((getCurrentTime() - trigger.getTriggerTime()) / 2.0));
				EventHeader lcsimEvent = new BaseLCSimEvent(DatabaseConditionsManager.getInstance().getRun(),
						triggerEventNumber, event.getDetectorName(), (long) 4 * (Math.round(trigger.getTriggerTime() / 4)), false);
				
				// Calculate the readout window time range. This is
				// used for any production driver that does not have
				// a manually specified output range.
				double startTime = trigger.getTriggerTime() - triggerTimeDisplacement;
				double endTime = startTime + readoutWindow;
				
				outputBuffer.append("\n\nTrigger is now writing.\n");
				outputBuffer.append("\tCurrent Time         :: " + getCurrentTime() + "\n");
				outputBuffer.append("\tTrigger Time         :: " + trigger.getTriggerTime() + "\n");
				outputBuffer.append("\tTrigger Displacement :: " + triggerTimeDisplacement + "\n");
				outputBuffer.append("\tDefault Start Time   :: " + startTime + "\n");
				outputBuffer.append("\tDefault End Time     :: " + endTime + "\n");
				
				System.out.printf("Buffered Range :: (%.0f, %.0f]%n", getCurrentTime() - bufferTotal, getCurrentTime());
				
				// Write out the writable collections into the event.
				for(LcsimCollectionData<?> collectionData : collectionMap.values()) {
					// Ignore any collections that are not set to be persisted.
					if(!collectionData.isPersistent()) {
						continue;
					}
					
					// Get the local start and end times. A driver
					// may manually specify an amount of time before
					// and after the trigger time which should be
					// output. If this is the case, use it instead of
					// the time found through use of the readout
					// window/trigger time displacement calculation.
					double localStartTime = startTime;
					if(!Double.isNaN(collectionData.getWindowBefore())) {
						localStartTime = trigger.getTriggerTime() - collectionData.getWindowBefore();
					}
					
					double localEndTime = endTime;
					if(!Double.isNaN(collectionData.getWindowAfter())) {
						localEndTime = trigger.getTriggerTime() + collectionData.getWindowAfter();
					}
					
					// Persisted collection should be added to event
					// within the readout window time range.
					storeCollection(localStartTime, localEndTime, collectionData, lcsimEvent);
					
					System.out.printf("\tOutput  Range  :: (%.0f, %.0f]   [%s]%n", localStartTime, localEndTime, collectionData.getCollectionName());
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
							System.out.println("On-Trigger Collection: " + triggerData.getCollectionName());
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
		
		// Remove all data from the buffer that occurs before the max
		// buffer length cut-off.
		for(LcsimCollectionData<?> data : collectionMap.values()) {
			while(!data.getData().isEmpty() && (data.getData().getFirst().getTime() < (getCurrentTime() - bufferTotal))) {
				data.getData().removeFirst();
			}
		}
		
		if(outputBuffer.length() != 0) {
			System.out.println("\n\n\n");
			System.out.println(outputBuffer.toString());
			outputBuffer = new StringBuffer();
		}
		
		// Increment the current time.
		currentTime += BEAM_BUNCH_SIZE;
		
		System.out.println("==== End Event " + event.getEventNumber() + " ===================================== ");
	}
	
	/**
	 * Adds a new set of data objects to the data manager at the time
	 * specified.
	 * @param collectionName - The collection name to which the data
	 * should be added.
	 * @param dataTime - The truth time at which the data objects
	 * occurred. This represents the time of the object, corrected
	 * for time displacement due to buffering on processing on the
	 * part of the production driver.
	 * @param data - The data to add.
	 * @param dataType - The class type of the data objects.
	 * @throws IllegalArgumentException Occurs if either the
	 * collection specified does not exist, or if the object type of
	 * the data objects does not match the object type of the data in
	 * the collection.
	 * @param <T> - Specifies the class type of the data to be added
	 * to the collection.
	 */
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
			
			outputBuffer.append("Adding " + data.size() + " objects of type " + dataType.getSimpleName() + " into collection \""
					+ collectionName + "\" at time t = " + time + ".\n");
		}
	}
	
	/**
	 * Adds a new set of data objects to the data manager at a time
	 * calculated based on the current simulation time corrected by
	 * the total time offset of the collection.
	 * @param collectionName - The collection name to which the data
	 * should be added.
	 * @param data - The data to add.
	 * @param dataType - The class type of the data objects.
	 * @throws IllegalArgumentException Occurs if either the
	 * collection specified does not exist, or if the object type of
	 * the data objects does not match the object type of the data in
	 * the collection.
	 * @param <T> - Specifies the class type of the data to be added
	 * to the collection.
	 */
	public static final <T> void addData(String collectionName, Collection<T> data, Class<T> dataType) {
		addData(collectionName, Double.NaN, data, dataType);
	}
	
	/**
	 * Checks whether or not a collection has been populated up to
	 * the indicated time.
	 * @param collectionName - The collection to check.
	 * @param time - The time at which the collection should be
	 * filled.
	 * @return Returns <code>true</code> if the collection has data
	 * generated up to at least the specified time, and
	 * <code>false</code> if it does not.
	 */
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
	
	/**
	 * Gets the length in nanoseconds of a single event (beam bunch).
	 * @return Returns the length in ns of a single beam bunch.
	 */
	public static final double getBeamBunchSize() {
		return BEAM_BUNCH_SIZE;
	}
	
	/**
	 * Gets the current simulation time in nanoseconds.
	 * @return Returns the simulation time in nanoseconds.
	 */
	public static final double getCurrentTime() {
		return currentTime;
	}
	
	/**
	 * Gets a collection of data objects from a collection within the
	 * time range specified.
	 * @param startTime - The (inclusive) start of the time range.
	 * @param endTime The (exclusive) end of the time range.
	 * @param collectionName - The name of the collection.
	 * @param objectType - The class type of the data stored in the
	 * collection.
	 * @return Returns the data in the specified time range in the
	 * data collection in a {@link java.util.List List}.
	 * @param <T> - Specifies the class type of the data stored in
	 * the collection.
	 */
	public static final <T> Collection<T> getData(double startTime, double endTime, String collectionName, Class<T> objectType) {
		return getDataList(startTime, endTime, collectionName, objectType);
	}
	
	/**
	 * Gets the default size of the readout window.
	 * @return Returns the default size of the readout window in
	 * units of nanoseconds.
	 */
	public static final int getReadoutWindow() {
		return readoutWindow;
	}
	
	/**
	 * Gets the total amount of time by which a collection is
	 * displaced between the actual truth data's occurrence in the
	 * simulation, and the time at which the object is actually
	 * produced. This includes both the time displacement introduced
	 * by the collection's production driver as well as displacement
	 * introduced by any preceding drivers that serve as input for
	 * the production driver.
	 * @param collectionName - The name of the collection.
	 * @return Returns the total time displacement in nanoseconds.
	 */
	public static final double getTotalTimeDisplacement(String collectionName) {
		return collectionMap.get(collectionName).getGlobalTimeDisplacement();
	}
	
	/**
	 * Adds a managed collection to the data manager. All collections
	 * which serve as either input or output from a {@link
	 * org.hps.readout.ReadoutDriver ReadoutDriver} are required to
	 * be registered and managed by the data manager. On-trigger
	 * special collections should not be registered.
	 * @param params - An object describing the collection
	 * parameters.
	 * @param <T> - Specifies the class type of the data stored by
	 * the collection.
	 */
	public static final <T> void registerCollection(LcsimCollection<T> params) {
		// Make sure that all arguments are defined.
		if(params.getCollectionName() == null) {
			throw new IllegalArgumentException("Error: Collection name must be defined.");
		} if(params.getObjectType() == null) {
			throw new IllegalArgumentException("Error: Collection object class must be defined.");
		} if(params.getProductionDriver() == null) {
			throw new IllegalArgumentException("Error: Production driver must be defined.");
		}
		
		// There should only be one collection for a given name.
		if(collectionMap.containsKey(params.getCollectionName())) {
			throw new IllegalArgumentException("Collection \"" + params.getCollectionName() + "\" of object type "
					+ params.getObjectType().getSimpleName() + " already exists.");
		}
		
		// Create a collection data object.
		double timeDisplacement = getTotalTimeDisplacement(params.getCollectionName(), params.getProductionDriver());
		LcsimCollectionData<T> collectionData = new LcsimCollectionData<T>(params, timeDisplacement);
		collectionMap.put(params.getCollectionName(), collectionData);
		
		// Track which collection has the largest time displacement.
		// This only applied if the registered collection is to be
		// persisted.
		if(params.isPersistent()) {
			if(largestDisplacement == null) {
				largestDisplacement = params.getCollectionName();
			} else if(timeDisplacement > collectionMap.get(largestDisplacement).getGlobalTimeDisplacement()) {
				largestDisplacement = params.getCollectionName();
			}
		}
		
		// Track the largest window both before and after the trigger
		// time that must be buffered to safely output the data. If
		// the collection gives NaN for its time window, it will use
		// the default readout window and trigger time displacement.
		// Otherwise, it will use its own specified window.
		if(Double.isNaN(params.getWindowAfter())) {
			bufferAfter = Math.max(bufferAfter, (readoutWindow - triggerTimeDisplacement));
		} else {
			bufferAfter = Math.max(bufferAfter, params.getWindowAfter());
		}
		
		if(Double.isNaN(params.getWindowBefore())) {
			bufferBefore = Math.max(bufferBefore, triggerTimeDisplacement);
		} else {
			bufferBefore = Math.max(bufferBefore, params.getWindowBefore());
		}
		
		// Store the readout driver in the driver set.
		driverSet.add(params.getProductionDriver());
	}
	
	/**
	 * Registers a {@link org.hps.readout.ReadoutDriver
	 * ReadoutDriver} with the data manager. All readout drivers must
	 * be registered in order for their on-trigger special data to be
	 * added to the output event.
	 * @param productionDriver - The readout driver to register.
	 */
	public static final void registerReadoutDriver(ReadoutDriver productionDriver) {
		// Trigger drivers are registered differently.
		if(productionDriver instanceof TriggerDriver) {
			System.err.println("Error: Attempted to register TriggerDriver \"" + productionDriver.getClass().getSimpleName() + "\" as a readout driver.");
			System.err.println("       Trigger drivers are registered via the method \"registerTrigger(TriggerDriver)\".");
			System.err.println("       Ignoring request.");
			return;
		}
		
		// Add the readout driver.
		driverSet.add(productionDriver);
		System.out.println("Registered driver: " + productionDriver.getClass().getSimpleName());
	}
	
	/**
	 * Registers a trigger driver with the data manager.
	 * @param triggerDriver - The trigger driver to register.
	 */
	public static final void registerTrigger(TriggerDriver triggerDriver) {
		// Get the total time displacement for the trigger driver.
		double timeDisplacement = getTotalTimeDisplacement("", triggerDriver);
		
		// Track the longest displacement needed for trigger output.
		bufferTrigger = Math.max(bufferTrigger, timeDisplacement + triggerDriver.getTimeDisplacement());
		
		// Store the time displacement in the trigger driver map.
		triggerTimeDisplacementMap.put(triggerDriver, timeDisplacement);
		System.out.println("Registered trigger: " + triggerDriver.getClass().getSimpleName());
	}
	
	/**
	 * Indicates that the specified driver saw a trigger and readout
	 * should occur.
	 * @param driver - The triggering driver.
	 * @throws IllegalArgumentException Occurs if the argument
	 * triggering driver is not registered as a trigger driver with
	 * the data manager.
	 */
	static final void sendTrigger(TriggerDriver driver) {
		// Check that the triggering driver is registered as a
		// trigger driver.
		if(!triggerTimeDisplacementMap.containsKey(driver)) {
			throw new IllegalArgumentException("Error: Driver \"" + driver.getClass().getSimpleName() + "\" is not a registered trigger driver.");
		}
		
		// Calculate the trigger and readout times.
		double triggerTime = getCurrentTime() - triggerTimeDisplacementMap.get(driver);
		double readoutTime = triggerTime + (readoutWindow - triggerTimeDisplacement);
		
		// Add the trigger to the trigger queue.
		triggerQueue.add(new TriggerTime(triggerTime, readoutTime, driver));
		System.out.println("Added trigger to queue with trigger time " + triggerTime + " and readout time " + readoutTime + " from driver "
				+ driver.getClass().getSimpleName() + ".");
	}
	
	/**
	 * Gets a list of data objects from a collection within the time
	 * range specified.
	 * @param startTime - The (inclusive) start of the time range.
	 * @param endTime The (exclusive) end of the time range.
	 * @param collectionName - The name of the collection.
	 * @param objectType - The class type of the data stored in the
	 * collection.
	 * @return Returns the data in the specified time range in the
	 * data collection in a {@link java.util.List List}.
	 * @param <T> - Specifies the class type of the data stored in
	 * the collection.
	 */
	private static final <T> List<T> getDataList(double startTime, double endTime, String collectionName, Class<T> objectType) {
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
		
		// Throw an alert if the earliest requested time precedes the
		// earliest buffered time, and similarly for the latest time.
		LinkedList<TimedList<?>> dataLists = collectionData.getData();
		/*
		if(!dataLists.isEmpty()) {
			if(startTime >= 0 && dataLists.getFirst().getTime() > startTime) {
				System.err.println("Warning :: Requested data for collection \"" + collectionName + "\" from time [" + startTime
						+ ", " + endTime + ") when no data is buffered before time " + dataLists.getFirst().getTime() + ".");
			}
			if((dataLists.getLast().getTime() + BEAM_BUNCH_SIZE) < endTime) {
				System.err.println("Warning :: Requested data for collection \"" + collectionName + "\" from time [" + startTime
						+ ", " + endTime + ") when no data is buffered after time " + (dataLists.getLast().getTime() + BEAM_BUNCH_SIZE) + ".");
			}
		} else {
			System.err.println("Warning :: Requested data for collection \"" + collectionName + "\" from time [" + startTime
					+ ", " + endTime + ") when no data is buffered.");
		}
			*/
		
		// Iterate through the data and collect all entries that have
		// an associated truth time within the given time range. The
		// lower bound is inclusive, the upper bound is exclusive.
		List<T> outputList = new ArrayList<T>();
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
	
	/**
	 * Calculates the total time displacement of a collection based
	 * on its production driver, and the time displacements of the
	 * input collections from which it is produced. This is processed
	 * recursively, so all time displacements in the production chain
	 * of a collection are accounted for.
	 * @param collectionName - The name of the collection.
	 * @param productionDriver - The driver which produces the
	 * collection.
	 * @return Returns the total time displacement for the collection
	 * in units of nanoseconds.
	 */
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
	
	/**
	 * Gets the buffered data for the LCSim collection
	 * <code>collectionData</code> between <code>startTime</code> and
	 * <code>endTime</code> and writes it to the output event
	 * <code>event</code>.
	 * @param startTime - The (inclusive) start time of the range of
	 * data which should be output for the collection.
	 * @param endTime - The (exclusive) end time for the range of
	 * data that should be output for the collection.
	 * @param collectionData - The collection data object which
	 * contains the data to be output.
	 * @param event - The output event into which the data should be
	 * written.
	 * @param <T> - Specifies the class type of the data that is to be
	 * written to the output event.
	 */
	private static final <T> void storeCollection(double startTime, double endTime, LcsimCollectionData<T> collectionData, EventHeader event) {
		// Get the trigger window data.
		List<T> triggerData = getDataList(startTime, endTime, collectionData.getCollectionName(), collectionData.getObjectType());
		
		// Store the trigger window data.
		storeCollection(collectionData.getCollectionName(), collectionData.getObjectType(), collectionData.getFlags(), collectionData.getReadoutName(),
				triggerData, event);
		
		outputBuffer.append("Storing output collection \"" + collectionData.getCollectionName() + "\" in range [" + startTime + ", "
				+ endTime + "). " + triggerData.size() + " objects found.\n");
	}
	
	/**
	 * Writes an entire {@link org.hps.readout.ReadoutDriver
	 * ReadoutDriver} on-trigger data collection to the specified
	 * output event.
	 * @param collectionData - The on-trigger readout data.
	 * @param event - The output event.
	 * @param <T> - Specifies the class type of the data that is to be
	 * written to the output event.
	 */
	private static final <T> void storeCollection(LcsimSingleEventCollectionData<T> collectionData, EventHeader event) {
		storeCollection(collectionData.getCollectionName(), collectionData.getObjectType(), collectionData.getFlags(), collectionData.getReadoutName(),
				collectionData.getData(), event);
	}
	
	/**
	 * Writes the specified data to the output event.
	 * @param collectionName - The name of the output collection.
	 * @param objectType - The class of the output collection data
	 * objects.
	 * @param flags - Any LCIO flags which apply to the data.
	 * @param readoutName - The readout name for the data, if it is
	 * needed. <code>null</code> should be used if a readout name is
	 * not required.
	 * @param collectionData - A parameterized {@link java.util.List
	 * List} containing the data that is to be written.
	 * @param event - The event into which the data is to be written.
	 * @param <T> - Specifies the class type of the data that is to be
	 * written to the output event.
	 */
	private static final <T> void storeCollection(String collectionName, Class<T> objectType, int flags, String readoutName,
			List<T> collectionData, EventHeader event) {
		// Place the data into the LCSim event.
		if(readoutName == null) {
			event.put(collectionName, collectionData, objectType, flags);
		} else {
			event.put(collectionName, collectionData, objectType, flags, readoutName);
		}
	}
	
	/**
	 * Checks that the dependencies of a collection are valid. This
	 * consists of checking that any dependencies are registered with
	 * the data management driver and also that there are no circular
	 * dependencies present.
	 * @param collectionName - The name of the collection to check.
	 * @param productionDriver - The production driver of the
	 * collection to check.
	 * @param dependents - A set containing all of the collections
	 * which depend on this driver in the chain. Note that for the
	 * first call, this should be an empty set.
	 */
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
	
	/**
	 * Sets the output file name for the triggered data file.
	 * @param filepath - The file path for the output file.
	 */
	public static final void setOutputFile(String filepath) {
		outputFileName = filepath;
	}
	
	/**
	 * Sets the default size of the readout window, in units of
	 * nanoseconds. Note that this can be overridden by specific
	 * drivers.
	 * @param nanoseconds - The length of the default readout window.
	 */
	public static final void setReadoutWindow(int nanoseconds) {
		readoutWindow = nanoseconds;
	}
}
