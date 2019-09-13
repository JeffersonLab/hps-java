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
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.util.TimedList;
import org.hps.readout.util.TriggerTime;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.ManagedLCIOCollection;
import org.hps.readout.util.collection.ManagedLCIOData;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.hps.record.triggerbank.TestRunTriggerData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.geometry.IDDecoder;
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
     * Tracks all registered readout drivers.
     */
    private static final Set<ReadoutDriver> driverSet = new HashSet<ReadoutDriver>();
    /**
     * Tracks all data collections which are managed by the readout
     * manager as well as their properties.
     */
    private static final Map<String, ManagedLCIOData<?>> collectionMap = new HashMap<String, ManagedLCIOData<?>>();
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
     * Tracks the total amount of time that must be buffered to allow
     * for readout to occur.
     */
    private static double bufferTotal = 0.0;
    /**
     * The total number of triggers seen.
     */
    private static int triggers = 0;
    /**
     * The delay between when a trigger occurs, and when readout is
     * performed.
     */
    private static double triggerDelay = 0.0;
    
    /**
     * Collection parameters for the dummy trigger bank object.
     */
    private static LCIOCollection<GenericObject> triggerBankParams = null;
    
    private static final String nl = String.format("%n");
    private static final Logger logger = Logger.getLogger(ReadoutDataManager.class.getSimpleName());
    
    @Override
    public void startOfData() {
        // Instantiate the readout LCIO file.
        if(outputFileName == null) {
            throw new IllegalArgumentException("Error: Output file name not defined!");
        }
        try { outputWriter = new LCIOWriter(new File(outputFileName)); }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        
        // Create a collection for the dummy trigger bank.
        LCIOCollectionFactory.setCollectionName("TriggerBank");
        LCIOCollectionFactory.setFlags(0);
        triggerBankParams = LCIOCollectionFactory.produceLCIOCollection(GenericObject.class);
        
        // Get the total amount of time that the readout system must
        // wait to make sure that all data has been safely buffered
        // and exists to read out.
        double longestBufferBefore = 0.0;
        double longestBufferAfter = 0.0;
        double longestLocalBuffer = 0.0;
        double longestTimeDisplacement = 0.0;
        double longestDisplacedAfter = 0.0;
        double longestTriggerDisplacement = 0.0;
        
        StringBuffer initializationBuffer = new StringBuffer();
        initializationBuffer.append("Getting longest trigger time displacement..." + nl);
        for(Entry<ReadoutDriver, Double> entry : triggerTimeDisplacementMap.entrySet()) {
            initializationBuffer.append(String.format("\t%-30s :: %.0f%n", entry.getKey().getClass().getSimpleName(), entry.getValue().doubleValue()));
            longestTriggerDisplacement = Math.max(longestTriggerDisplacement, entry.getValue().doubleValue());
        }
        initializationBuffer.append("Longest is: " + longestTriggerDisplacement + nl + nl);
        
        initializationBuffer.append("Getting longest driver collection buffers..." + nl);
        for(ManagedLCIOData<?> data : collectionMap.values()) {
            double before = Double.isNaN(data.getCollectionParameters().getWindowBefore()) ? 0.0 : data.getCollectionParameters().getWindowBefore();
            double after = Double.isNaN(data.getCollectionParameters().getWindowAfter()) ? 0.0 : data.getCollectionParameters().getWindowAfter();
            double displacement = data.getCollectionParameters().getProductionDriver().getTimeDisplacement();
            double local = data.getCollectionParameters().getProductionDriver().getTimeNeededForLocalOutput();
            
            initializationBuffer.append("\t" + data.getCollectionParameters().getCollectionName() + nl);
            initializationBuffer.append(String.format("\t\t%-20s :: %.0f%n", "Buffer Before", before));
            initializationBuffer.append(String.format("\t\t%-20s :: %.0f%n", "Buffer After", after));
            initializationBuffer.append(String.format("\t\t%-20s :: %.0f%n", "Local Buffer", local));
            initializationBuffer.append(String.format("\t\t%-20s :: %.0f%n", "Displacement", displacement));
            initializationBuffer.append(String.format("\t\t%-20s :: %.0f%n", "Displaced After", (displacement + after)));
            
            longestBufferBefore = Math.max(longestBufferBefore, before);
            longestBufferAfter = Math.max(longestBufferAfter, after);
            longestLocalBuffer = Math.max(longestLocalBuffer, local);
            longestTimeDisplacement = Math.max(longestTimeDisplacement, displacement);
            longestDisplacedAfter = Math.max(longestDisplacedAfter, displacement + after);
        }
        initializationBuffer.append("Longest (before) is: " + longestBufferBefore + nl);
        initializationBuffer.append("Longest (after) is: " + longestBufferAfter + nl);
        initializationBuffer.append("Longest (local) is: " + longestLocalBuffer + nl);
        initializationBuffer.append("Longest (displacement) is: " + longestTimeDisplacement + nl);
        initializationBuffer.append("Longest (displacemed after) is: " + longestDisplacedAfter + nl + nl);
        
        initializationBuffer.append("Readout Window: " + readoutWindow + nl);
        initializationBuffer.append("Trigger Offset: " + triggerTimeDisplacement + nl);
        initializationBuffer.append("Default Before: " + triggerTimeDisplacement + nl);
        initializationBuffer.append("Default After : " + (readoutWindow - triggerTimeDisplacement) + nl + nl);
        
        triggerDelay = Math.max(longestTriggerDisplacement, longestDisplacedAfter);
        triggerDelay = Math.max(triggerDelay, longestLocalBuffer);
        double totalNeededDisplacement = triggerDelay + longestBufferBefore + 150;
        
        initializationBuffer.append("Total Time Needed: " + totalNeededDisplacement + nl);
        logger.fine(nl + initializationBuffer.toString());
        
        // Determine the total amount of time that must be included
        // in the data buffer in order to safely write out all data.
        // An extra 150 ns of data is retained as a safety, just in
        // case some driver needs to look unusually far back.
        bufferTotal = totalNeededDisplacement;
    }
    
    @Override
    public void endOfData() {
        try { outputWriter.close(); }
        catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        
        System.out.println("Wrote " + triggers + " triggers.");
    }
    
    @Override
    public void process(EventHeader event) {
        // TODO: Currently only works for a single trigger. Should support multiple triggers with different offsets.
        // Check the trigger queue.
        if(!triggerQueue.isEmpty()) {
            // Check the earliest possible trigger write time.
            boolean isWritable = getCurrentTime() >= triggerQueue.peek().getTriggerTime() + bufferTotal;
            
            // If all collections are available to be written, the
            // event should be output.
            if(isWritable) {
                // Store the current trigger data.
                TriggerTime trigger = triggerQueue.poll();
                triggers++;
                
                // Make a new LCSim event.
                int triggerEventNumber = event.getEventNumber() - ((int) Math.floor((getCurrentTime() - trigger.getTriggerTime()) / 2.0));
                EventHeader lcsimEvent = new BaseLCSimEvent(DatabaseConditionsManager.getInstance().getRun(),
                        triggerEventNumber, event.getDetectorName(), (long) 4 * (Math.round(trigger.getTriggerTime() / 4)), false);
                
                // Calculate the readout window time range. This is
                // used for any production driver that does not have
                // a manually specified output range.
                double startTime = trigger.getTriggerTime() - triggerTimeDisplacement;
                double endTime = startTime + readoutWindow;
                
                logger.finer("Trigger Time: " + trigger.getTriggerTime());
                logger.finer("Default Time Range: " + startTime + " - " + endTime);
                
                // All readout output is initially stored in a single
                // object. This allows the readout from multiple
                // drivers to be merged, if needed, and also prevents
                // duplicate instances of an object from being
                // written.
                Map<String, TriggeredLCIOData<?>> triggeredDataMap = new HashMap<String, TriggeredLCIOData<?>>();
                
                // Write out the writable collections into the event.
                for(ManagedLCIOData<?> collectionData : collectionMap.values()) {
                    // Ignore any collections that are not set to be persisted.
                    if(!collectionData.getCollectionParameters().isPersistent()) {
                        continue;
                    }
                    
                    // Get the local start and end times. A driver
                    // may manually specify an amount of time before
                    // and after the trigger time which should be
                    // output. If this is the case, use it instead of
                    // the time found through use of the readout
                    // window/trigger time displacement calculation.
                    double localStartTime = startTime;
                    if(!Double.isNaN(collectionData.getCollectionParameters().getWindowBefore())) {
                        localStartTime = trigger.getTriggerTime() - collectionData.getCollectionParameters().getWindowBefore();
                    }
                    
                    double localEndTime = endTime;
                    if(!Double.isNaN(collectionData.getCollectionParameters().getWindowAfter())) {
                        localEndTime = trigger.getTriggerTime() + collectionData.getCollectionParameters().getWindowAfter();
                    }
                    
                    // Get the object data for the time range.
                    addDataToMap(collectionData.getCollectionParameters(), localStartTime, localEndTime, triggeredDataMap);
                }
                
                // Write out any special on-trigger collections into
                // the event as well. These are collated so that if
                // more than one driver contributes to the same
                // collection, they will be properly merged.
                for(ReadoutDriver driver : driverSet) {
                    // Get the special collection(s) from the current
                    // driver, if it exists.
                    Collection<TriggeredLCIOData<?>> onTriggerData = driver.getOnTriggerData(trigger.getTriggerTime());
                    
                    // If there are special collections, write them.
                    if(onTriggerData != null) {
                        for(TriggeredLCIOData<?> triggerData : onTriggerData) {
                            addDataToMap(triggerData, triggerData.getCollectionParameters().getObjectType(), triggeredDataMap);
                        }
                    }
                }
                
                // Create the dummy trigger bank data and store it.
                TriggeredLCIOData<GenericObject> triggerBankData = new TriggeredLCIOData<GenericObject>(triggerBankParams);
                triggerBankData.getData().add(new TestRunTriggerData(new int[8]));
                addDataToMap(triggerBankData, triggerBankData.getCollectionParameters().getObjectType(), triggeredDataMap);
                
                // Readout timestamps should be generated for both
                // the "system" and the trigger. This corresponds to
                // the simulation time at which the trigger occurred.
                // Note that there is a "trigger delay" parameter in
                // the old readout, but this does not exist in the
                // new system, so both timestamps are the same.
                
                // Calculate the simulation trigger time.
                double simTriggerTime = trigger.getTriggerTime() + triggerTimeDisplacementMap.get(trigger.getTriggeringDriver()).doubleValue();
                ReadoutTimestamp systemTimestamp = new ReadoutTimestamp(ReadoutTimestamp.SYSTEM_TRIGGERBITS, simTriggerTime);
                ReadoutTimestamp triggerTimestamp = new ReadoutTimestamp(ReadoutTimestamp.SYSTEM_TRIGGERTIME, simTriggerTime);
                LCIOCollectionFactory.setCollectionName(ReadoutTimestamp.collectionName);
                LCIOCollection<ReadoutTimestamp> timestampCollection = LCIOCollectionFactory.produceLCIOCollection(ReadoutTimestamp.class);
                TriggeredLCIOData<ReadoutTimestamp> timestampData = new TriggeredLCIOData<ReadoutTimestamp>(timestampCollection);
                timestampData.getData().add(systemTimestamp);
                timestampData.getData().add(triggerTimestamp);
                addDataToMap(timestampData, timestampData.getCollectionParameters().getObjectType(), triggeredDataMap);
                
                // Store all of the data collections.
                for(TriggeredLCIOData<?> triggerData : triggeredDataMap.values()) {
                    storeCollection(triggerData, lcsimEvent);
                }
                
                // Write the event to the output file.
                try { outputWriter.write(lcsimEvent); }
                catch(IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }
        
        // Remove all data from the buffer that occurs before the max
        // buffer length cut-off.
        for(ManagedLCIOData<?> data : collectionMap.values()) {
            while(!data.getData().isEmpty() && (data.getData().getFirst().getTime() < (getCurrentTime() - 500))) {
                data.getData().removeFirst();
            }
        }
        
        // Increment the current time.
        currentTime += BEAM_BUNCH_SIZE;
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
        ManagedLCIOData<?> collectionData = collectionMap.get(collectionName);
        
        // Validate that the data type is correct.
        if(!collectionData.getCollectionParameters().getObjectType().isAssignableFrom(dataType)) {
            throw new IllegalArgumentException("Error: Saw data type \"" + dataType.getSimpleName() + "\" but expected data type \""
                    + collectionData.getCollectionParameters().getObjectType().getSimpleName() + "\" instead.");
        }
        
        // If the data is empty, then there is no need to add it to
        // the buffer.
        if(!data.isEmpty()) {
            // Add the new data to the data buffer.
            double time = Double.isNaN(dataTime) ? currentTime - collectionData.getCollectionParameters().getGlobalTimeDisplacement() : dataTime;
            LinkedList<TimedList<?>> dataBuffer = collectionData.getData();
            dataBuffer.add(new TimedList<T>(time, data));
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
        return time <= getCurrentTime() - collectionMap.get(collectionName).getCollectionParameters().getGlobalTimeDisplacement();
    }
    
    /**
     * Gets the length in nanoseconds of a single event (beam bunch).
     * @return Returns the length in ns of a single beam bunch.
     */
    public static final double getBeamBunchSize() {
        return BEAM_BUNCH_SIZE;
    }
    
    /**
     * Gets the LCIO collection parameters for a collection.
     * @param collectionName - The name of the collection.
     * @param objectType - The data type of the collection.
     * @return Returns the collection parameters.
     */
    @SuppressWarnings("unchecked")
    public static final <T> LCIOCollection<T> getCollectionParameters(String collectionName, Class<T> objectType) {
        // Verify that the requested collection actually exists.
        if(!collectionMap.containsKey(collectionName)) {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
        }
        
        // Get the collection and check that it is of the appropriate
        // parameterized type.
        LCIOCollection<?> collection = collectionMap.get(collectionName).getCollectionParameters();
        if(collection.getObjectType() != objectType) {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" is of type " + collection.getObjectType().getSimpleName()
                    + " while object type " + objectType.getSimpleName() + " was requested.");
        }
        
        // Return the collection parameters.
        // NOTE: This type case is safe, since it is verified above
        //       that the collection object is of the same class type
        //       as the parameterized type.
        return (LCIOCollection<T>) collection;
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
     * Gets the {@link org.lcsim.geometry.IDDecoder IDDecoder} that
     * is used for the indicated managed collection, if it exists.
     * @param collectionName - The collection to which the decoder
     * should correspond.
     * @return Returns the decoder for the collection, if it exists,
     * and <code>null</code> otherwise.
     */
    public static final IDDecoder getIDDecoder(String collectionName) {
        // Verify that the requested collection actually exists.
        if(!collectionMap.containsKey(collectionName)) {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
        }
        
        // Get the collection and obtain the ID decoder, if possible.
        // If it does not exist, then leave it as a value of null.
        LCIOCollection<?> collection = collectionMap.get(collectionName).getCollectionParameters();
        IDDecoder decoder = null;
        try { decoder = collection.getProductionDriver().getIDDecoder(collectionName); }
        catch(UnsupportedOperationException e) { }
        
        // Return the decoder.
        return decoder;
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
        if(collectionMap.containsKey(collectionName)) {
            return collectionMap.get(collectionName).getCollectionParameters().getGlobalTimeDisplacement();
        } else {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
        }
    }
    
    /**
     * Gets the time displacement between when a trigger occurs, and
     * when the triggered data is actually written out.
     * @return Returns the trigger delay in units of nanoseconds.
     */
    public static final double getTriggerDelay() {
        return bufferTotal;
    }
    
    /**
     * Gets the time by which the trigger is offset in the readout
     * window.
     * @return Returns the trigger offset in units of nanoseconds.
     */
    public static final double getTriggerOffset() {
        return triggerTimeDisplacement;
    }
    
    /**
     * Adds a managed collection to the data manager. All collections
     * which serve as either input or output from a {@link
     * org.hps.readout.ReadoutDriver ReadoutDriver} are required to
     * be registered and managed by the data manager. On-trigger
     * special collections should not be registered.
     * @param params - An object describing the collection
     * parameters.
     * @param persistent - Sets whether this collection should be
     * written out to the readout LCIO file.
     * @param <T> - Specifies the class type of the data stored by
     * the collection.
     */
    public static final <T> void registerCollection(LCIOCollection<T> params, boolean persistent) {
        registerCollection(params, persistent, Double.NaN, Double.NaN);
    }
    
    /**
     * Adds a managed collection to the data manager. All collections
     * which serve as either input or output from a {@link
     * org.hps.readout.ReadoutDriver ReadoutDriver} are required to
     * be registered and managed by the data manager. On-trigger
     * special collections should not be registered.
     * @param params - An object describing the collection
     * parameters.
     * @param persistent - Sets whether this collection should be
     * written out to the readout LCIO file.
     * @param readoutWindowBefore - Defines a custom period of time
     * before the trigger time in which all objects will be output to
     * the output LCIO file.
     * @param readoutWindowAfter - Defines a custom period of time
     * after the trigger time in which all objects will be output to
     * the output LCIO file.
     * @param <T> - Specifies the class type of the data stored by
     * the collection.
     */
    public static final <T> void registerCollection(LCIOCollection<T> params, boolean persistent, double readoutWindowBefore, double readoutWindowAfter) {
        // Make sure that all arguments are defined.
        if(params.getCollectionName() == null) {
            throw new IllegalArgumentException("Error: Collection name must be defined.");
        } 
        if(params.getObjectType() == null) {
            throw new IllegalArgumentException("Error: Collection object class must be defined.");
        }
        if(params.getProductionDriver() == null) {
            throw new IllegalArgumentException("Error: Production driver must be defined.");
        }
        
        // There should only be one collection for a given name.
        if(collectionMap.containsKey(params.getCollectionName())) {
            throw new IllegalArgumentException("Collection \"" + params.getCollectionName() + "\" of object type "
                    + params.getObjectType().getSimpleName() + " already exists.");
        }
        
        // Create a collection data object.
        double timeDisplacement = getTotalTimeDisplacement(params.getCollectionName(), params.getProductionDriver());
        LCIOCollectionFactory.setParams(params);
        LCIOCollectionFactory.setGlobalTimeDisplacement(timeDisplacement);
        LCIOCollectionFactory.setPersistent(persistent);
        LCIOCollectionFactory.setWindowAfter(readoutWindowAfter);
        LCIOCollectionFactory.setWindowBefore(readoutWindowBefore);
        ManagedLCIOCollection<T> managedParams = LCIOCollectionFactory.produceManagedLCIOCollection(params.getObjectType());
        ManagedLCIOData<T> collectionData = new ManagedLCIOData<T>(managedParams);
        collectionMap.put(params.getCollectionName(), collectionData);
        
        // Store the readout driver in the driver set.
        driverSet.add(params.getProductionDriver());
        
        logger.config("Registered collection \"" + managedParams.getCollectionName() + "\" of class type "
                + managedParams.getObjectType().getSimpleName() + ".");
        
        StringBuffer detailsBuffer = new StringBuffer();
        detailsBuffer.append("\tCollection Name   :: " + params.getCollectionName());
        detailsBuffer.append("\tFlags             :: " + Integer.toHexString(params.getFlags()));
        detailsBuffer.append("\tObject Type       :: " + params.getObjectType().getSimpleName());
        detailsBuffer.append("\tReadout Name      :: " + params.getReadoutName());
        detailsBuffer.append("\tProduction Driver :: " + params.getProductionDriver().getClass().getSimpleName());
        logger.finer(nl + detailsBuffer.toString());
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
            logger.warning(nl + "Error: Attempted to register TriggerDriver \"" + productionDriver.getClass().getSimpleName() + "\" as a readout driver."
                    + nl + "       Trigger drivers are registered via the method \"registerTrigger(TriggerDriver)\"."
                    + nl + "       Ignoring request.");
            return;
        }
        
        // Add the readout driver.
        driverSet.add(productionDriver);
        logger.config("Registered driver: " + productionDriver.getClass().getSimpleName());
    }
    
    /**
     * Registers a trigger driver with the data manager.
     * @param triggerDriver - The trigger driver to register.
     */
    public static final void registerTrigger(TriggerDriver triggerDriver) {
        // Get the total time displacement for the trigger driver.
        double timeDisplacement = getTotalTimeDisplacement("", triggerDriver);
        
        // Store the time displacement in the trigger driver map.
        triggerTimeDisplacementMap.put(triggerDriver, timeDisplacement);
        logger.config("Registered trigger: " + triggerDriver.getClass().getSimpleName());
    }
    
    /**
     * Changes the "readout name" parameter for a collection, while
     * retaining all other parameters and stored data.
     * @param collectionName - The name of the collection to modify.
     * @param objectType - The object type of the collection.
     * @param newReadoutName - The new name for the "readout name"
     * parameter.
     * @param <T> - The object type of the data stored in the
     * collection that is to be modified.
     */
    public static final <T> void updateCollectionReadoutName(String collectionName, Class<T> objectType, String newReadoutName) {
        // Get the collection.
        if(!collectionMap.containsKey(collectionName)) {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
        }
        ManagedLCIOData<?> oldData = collectionMap.get(collectionName);
        
        // Make a new managed LCIO collection with the new readout.
        LCIOCollectionFactory.setParams(oldData.getCollectionParameters());
        LCIOCollectionFactory.setReadoutName(newReadoutName);
        ManagedLCIOCollection<T> newParams = LCIOCollectionFactory.produceManagedLCIOCollection(objectType);
        
        // Create a new managed LCIO data object and transfer all the
        // data from the old object to it.
        ManagedLCIOData<T> newData = new ManagedLCIOData<T>(newParams);
        for(TimedList<?> oldList : oldData.getData()) {
            newData.getData().add(oldList);
        }
        
        // Put the new data list into the map.
        collectionMap.put(collectionName, newData);
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
        
        // Add the trigger to the trigger queue.
        triggerQueue.add(new TriggerTime(triggerTime, driver));
        logger.finer("Added trigger to queue with trigger time " + triggerTime + " and readout time " + (triggerTime + bufferTotal) + " from driver "
                + driver.getClass().getSimpleName() + ".");
    }
    
    /**
     * Adds a data collection corresponding to a given parameter set
     * to the data map. If there is already data existing under the
     * same collection, it is then merged without duplicating any
     * objects.
     * @param params - The collection parameters for the data.
     * @param readoutData - The data to add.
     * @param triggeredDataMap - The data map into which the data
     * collection should be added.
     */
    @SuppressWarnings("unchecked")
    private static final <T> void addDataToMap(LCIOCollection<T> params, Collection<T> readoutData, Map<String, TriggeredLCIOData<?>> triggeredDataMap) {
        // Check and see if an output collection already exists for
        // this parameter set. If so, use it; otherwise, make a new
        // entry for it.
        TriggeredLCIOData<?> untypedData = triggeredDataMap.get(params.getCollectionName());
        TriggeredLCIOData<T> typedData = null;
        if(untypedData == null) {
            typedData = new TriggeredLCIOData<T>(params);
            triggeredDataMap.put(params.getCollectionName(), typedData);
        } else {
            // Verify that the collection parameters are the same.
            if(untypedData.getCollectionParameters().equals(params)) {
                // Note: This cast is safe; if the parameters objects
                // are the same, then the object sets are necessarily
                // of the same object type.
                typedData = (TriggeredLCIOData<T>) untypedData;
            } else {
                throw new RuntimeException("Error: Found multiple collections of name \"" + params.getCollectionName() + "\", but of differing definitions.");
            }
        }
        
        // Add the readout data to the collection data list.
        typedData.getData().addAll(readoutData);
    }
    
    /**
     * Adds data stored in the collection defined by the parameters
     * object within the given time range to the data map. If there
     * is already data existing under the same collection, it is then
     * merged without duplicating any objects.
     * @param params - The parameters for the collection to add.
     * @param startTime - The start of the time range within the data
     * buffer from which data should be drawn.
     * @param endTime - The end of the time range within the data
     * buffer from which data should be drawn.
     * @param triggeredDataMap - The data map into which the data
     * collection should be added.
     */
    private static final <T> void addDataToMap(LCIOCollection<T> params, double startTime, double endTime, Map<String, TriggeredLCIOData<?>> triggeredDataMap) {
        // Get the readout data objects.
        List<T> triggerData = getDataList(startTime, endTime, params.getCollectionName(), params.getObjectType());
        
        // Pass the readout data to the merging method.
        addDataToMap(params, triggerData, triggeredDataMap);
    }
    
    /**
     * Adds data stored in a triggered collection object to the data
     * map. If there is already data existing under the same
     * collection, it is then merged without duplicating any objects.
     * @param dataList - The collection data to be added.
     * @param objectType - the object type of the collection data.
     * @param triggeredDataMap - The data map into which the data
     * collection should be added.
     */
    private static final <T> void addDataToMap(TriggeredLCIOData<?> dataList, Class<T> objectType, Map<String, TriggeredLCIOData<?>> triggeredDataMap) {
        // Check that the parameters object is the same object type
        // as is specified.
        if(dataList.getCollectionParameters().getObjectType() != objectType) {
            throw new IllegalArgumentException("Error: Can not process class type " + dataList.getCollectionParameters().getObjectType().getSimpleName()
                    + " as class type " + objectType.getSimpleName());
        } else {
            // Note: This is safe - the above check requires that the
            // object type be the parameterized type.
            @SuppressWarnings("unchecked")
            TriggeredLCIOData<T> typedDataList = (TriggeredLCIOData<T>) dataList;
            Set<T> triggerData = typedDataList.getData();
            addDataToMap(typedDataList.getCollectionParameters(), triggerData, triggeredDataMap);
        }
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
        ManagedLCIOData<?> collectionData = collectionMap.get(collectionName);
        
        // Verify that the a collection of the indicated name exists
        // and that it is the appropriate object type.
        if(collectionData != null) {
            if(!objectType.isAssignableFrom(collectionData.getCollectionParameters().getObjectType())) {
                throw new IllegalArgumentException("Error: Expected object type " + objectType.getSimpleName() + " for collection \"" + collectionName
                        + ",\" but found object type " + collectionData.getCollectionParameters().getObjectType().getSimpleName() + ".");
            }
        } else {
            throw new IllegalArgumentException("Error: Collection \"" + collectionName + "\" does not exist.");
        }
        
        // Throw an alert if the earliest requested time precedes the
        // earliest buffered time, and similarly for the latest time.
        LinkedList<TimedList<?>> dataLists = collectionData.getData();
        
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
                dependencyDisplacement = collectionMap.get(dependency).getCollectionParameters().getGlobalTimeDisplacement();
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
     * Writes an entire {@link org.hps.readout.ReadoutDriver
     * ReadoutDriver} on-trigger data collection to the specified
     * output event.
     * @param collectionData - The on-trigger readout data.
     * @param event - The output event.
     * @param <T> - Specifies the class type of the data that is to be
     * written to the output event.
     */
    private static final <T> void storeCollection(TriggeredLCIOData<T> collectionData, EventHeader event) {
        storeCollection(collectionData.getCollectionParameters().getCollectionName(), collectionData.getCollectionParameters().getObjectType(),
                collectionData.getCollectionParameters().getFlags(), collectionData.getCollectionParameters().getReadoutName(),
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
     * @param collectionData - A parameterized {@link
     * java.util.Collection Collection} containing the data that is
     * to be written.
     * @param event - The event into which the data is to be written.
     * @param <T> - Specifies the class type of the data that is to be
     * written to the output event.
     */
    private static final <T> void storeCollection(String collectionName, Class<T> objectType, int flags, String readoutName,
            Collection<T> collectionData, EventHeader event) {
        // The input collection must be a list. If it already is,
        // just use it directly. Otherwise, copy the contents into an
        // appropriately parameterized list.
        List<T> dataList;
        if(collectionData instanceof List) {
            dataList = (List<T>) collectionData;
        } else {
            dataList = new ArrayList<T>(collectionData.size());
            dataList.addAll(collectionData);
        }
        
        // Place the data into the LCIO event.
        if(readoutName == null) {
            event.put(collectionName, dataList, objectType, flags);
        } else {
            event.put(collectionName, dataList, objectType, flags, readoutName);
        }
        
        logger.finer(String.format("Output %d objects of type %s to collection \"%s\".", dataList.size(), objectType.getSimpleName(), collectionName));
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
            ManagedLCIOData<?> collectionData = collectionMap.get(dependency);
            
            // Check that this dependency does not depend on the
            // higher driver.
            for(String dependent : dependents) {
                if(collectionData.getCollectionParameters().getProductionDriver().getDependencies().contains(dependent)) {
                    throw new IllegalStateException("Error: Collection \"" + dependency + "\" depends on collection \"" + dependent
                            + ",\" but collection \"" + dependent + "\" also depends of collection \"" + dependency + ".\"");
                }
            }
            
            // If there are no detected circular dependencies, then
            // perform the same check on the dependencies of this
            // dependency.
            Set<String> dependencySet = new HashSet<String>();
            dependencySet.addAll(dependents);
            validateDependencies(dependency, collectionData.getCollectionParameters().getProductionDriver(), dependencySet);
        }
    }
    
    /**
     * Adds the argument particle and all of its direct parents to
     * the particle set.
     * @param particle - The base particle.
     * @param particleSet - The set that is to contain the full tree
     * of particles.
     */
    public static final void addParticleParents(MCParticle particle, Set<MCParticle> particleSet) {
        // Add the particle itself to the set.
        particleSet.add(particle);
        
        // If the particle has parents, run the same method for each
        // parent.
        if(!particle.getParents().isEmpty()) {
            for(MCParticle parent : particle.getParents()) {
                addParticleParents(parent, particleSet);
            }
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