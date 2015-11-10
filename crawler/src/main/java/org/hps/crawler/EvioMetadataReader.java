package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EventTagMask;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.triggerbank.TriggerType;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * Reads metadata from EVIO files, including the event count, run min and run max expected by the datacat,
 * as well as many custom field values applicable to HPS EVIO raw data.
 * <p>
 * The size of the data file is set externally to this reader using the datacat client.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class EvioMetadataReader implements FileMetadataReader {

    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(EvioMetadataReader.class.getPackage().getName());
    
    /**
     * Get the EVIO file metadata.
     *      
     * @param file the EVIO file
     * @return the metadata map of key and value pairs
     */
    @Override
    public Map<String, Object> getMetadata(final File file) throws IOException {

        Integer firstTimestamp = null;
        Integer lastTimestamp = null;
        Integer firstPhysicsTimestamp = null;
        Integer lastPhysicsTimestamp = null;
        int totalEvents = 0;
        int badEvents = 0;
        int epicsEvents = 0;
        int scalerEvents = 0;
        int physicsEvents = 0;
        int syncEvents = 0;
        int pauseEvents = 0;
        int prestartEvents = 0;
        int endEvents = 0;
        int goEvents = 0;        
        boolean blinded = true;
        Integer run = null;
        Integer lastPhysicsEvent = null;
        Integer firstPhysicsEvent = null;
        double triggerRate = 0;
        List<ScalerData> scalerData = new ArrayList<ScalerData>();
        List<EpicsData> epicsData = new ArrayList<EpicsData>();
                               
        // Create map for counting event masks.        
        Map<TriggerType, Integer> eventCounts = new HashMap<TriggerType, Integer>();
        for (TriggerType mask : TriggerType.values()) {
            eventCounts.put(mask, 0);
        }
                
        // Scaler processor to check for scaler bank.
        ScalersEvioProcessor scalersProcessor = new ScalersEvioProcessor();
        
        // EPICS data processor.
        EpicsEvioProcessor epicsProcessor = new EpicsEvioProcessor(); 

        // Get the file number from the name.
        final int fileNumber = EvioFileUtilities.getSequenceFromName(file);
        
        // Only files divisible by 10 are unblinded (Eng Run 2015 scheme).
        if (fileNumber % 10 == 0) {
            blinded = false;
        }
        
        EvioReader evioReader = null;
        try {                        
            // Open file in sequential mode.
            evioReader = EvioFileUtilities.open(file, true);            
            EvioEvent evioEvent = null;

            // Event read loop.
            while (true) {
                
                // Read in an EVIO event, trapping exceptions in case a parse error occurs.
                boolean badEvent = false;
                try {
                    // Parse next event.
                    evioEvent = evioReader.parseNextEvent();                    
                } catch (IOException | EvioException e) {
                    // Trap event parsing errors from bad EVIO data.
                    badEvent = true;
                    badEvents++;
                    LOGGER.warning("bad EVIO event " + evioEvent.getEventNumber() + " could not be parsed");
                } finally {
                    // End of file.
                    if (!badEvent && evioEvent == null) {
                        LOGGER.info("EOF after " + totalEvents + " events");
                        break;
                    }
                    
                    // Increment event count.
                    totalEvents++;
                }
                                
                // Continue to next event if a parse error occurred.
                if (badEvent) {
                    continue;
                }                
                                
                // Debug print event number and tag.
                LOGGER.finest("parsed event " + evioEvent.getEventNumber() + " with tag 0x" + String.format("%08x", evioEvent.getHeader().getTag()));
                                
                // Process different event types.
                if (EventTagConstant.PRESTART.matches(evioEvent)) {
                                                            
                    // File has PRESTART event.
                    LOGGER.fine("found PRESTART event " + evioEvent.getEventNumber());
                    ++prestartEvents;
                    
                    // Set the run number from the PRESTART event.
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.fine("set run to " + run + " from PRESTART");
                    }
                    
                    // Set the first timestamp from the PRESTART event.
                    if (firstTimestamp == null) {
                        firstTimestamp = controlEventData[0];
                        LOGGER.fine("set first timestamp to " + firstTimestamp + " from PRESTART event " + evioEvent.getEventNumber());
                    }
                    
                } else if (EventTagConstant.GO.matches(evioEvent)) {
                    
                    // File has GO event.
                    goEvents++;
                    
                    // Set first timestamp from the GO event (will not override PRESTART time).
                    if (firstTimestamp == null) {
                        final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                        firstTimestamp = controlEventData[0];
                        LOGGER.fine("set first timestamp to " + firstTimestamp + " from GO event " + evioEvent.getEventNumber());
                    }
                                                           
                } else if (EventTagConstant.END.matches(evioEvent)) {
                    
                    // File has END event.
                    LOGGER.fine("got END event");
                    endEvents++;
                    
                    // Set the last timestamp from the END event.
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    lastTimestamp = controlEventData[0];
                    LOGGER.fine("set last timestamp " + lastTimestamp + " from END event " + evioEvent.getEventNumber());
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.fine("set run to " + run);
                    }
                    
                } else if (EventTagConstant.PAUSE.matches(evioEvent)) {
                    
                    // Count pause events.
                    pauseEvents++;
                    
                } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    
                    // Count physics events.
                    physicsEvents++;
                    
                    // Get head bank.
                    final int[] headBankData = EvioEventUtilities.getHeadBankData(evioEvent);
                    
                    // Is head bank present?
                    if (headBankData != null) {
                        
                        // Is timestamp set?
                        if (headBankData[3] != 0) {
                            
                            // Set first timestamp.
                            if (firstTimestamp == null) {
                                firstTimestamp = headBankData[3];
                                LOGGER.fine("set first timestamp to " + firstTimestamp + " from physics event " + evioEvent.getEventNumber());                                
                            }
                            
                            // Set first physics timestamp.
                            if (firstPhysicsTimestamp == null) {                     
                                firstPhysicsTimestamp = headBankData[3];
                                LOGGER.fine("set first physics timestamp to " + firstTimestamp + " from event " + evioEvent.getEventNumber());                                
                            }
                            
                            // Set last physics timestamp.
                            lastPhysicsTimestamp = headBankData[3];
                            LOGGER.finest("set last physics timestamp to " + firstTimestamp + " from event " + evioEvent.getEventNumber());
                            
                            // Set last timestamp.
                            lastTimestamp = headBankData[3];
                        }
                        
                        // Set run number.
                        if (run == null) {
                            run = headBankData[1];
                            LOGGER.info("set run to " + run + " from physics event " + evioEvent.getEventNumber());
                        }
                    }                                                                
                                                                                
                    // Get the event ID data.
                    final int[] eventIdData = EvioEventUtilities.getEventIdData(evioEvent);
                    if (eventIdData != null) {
                        
                        // Set the last physics event.
                        lastPhysicsEvent = eventIdData[0];

                        // Set the first physics event.
                        if (firstPhysicsEvent == null) {
                            firstPhysicsEvent = eventIdData[0];
                            LOGGER.fine("set start event " + firstPhysicsEvent + " from physics event " + evioEvent.getEventNumber());
                        }                        
                    }
                                                                         
                    // Count scaler events.
                    scalersProcessor.process(evioEvent);
                    if (scalersProcessor.getCurrentScalerData() != null) {
                        scalerData.add(scalersProcessor.getCurrentScalerData());
                        scalerEvents++;
                    }
                    
                    // Count trigger types for this event.
                    Set<TriggerType> triggerTypes = TriggerType.getTriggerTypes(evioEvent);
                    for (TriggerType mask : triggerTypes) {
                        int count = eventCounts.get(mask) + 1;
                        eventCounts.put(mask, count);
                        LOGGER.finer("incremented " + mask.name() + " to " + count);
                    }
                    
                    // Count sync events.
                    if (EventTagMask.SYNC.matches(evioEvent.getHeader().getTag())) {
                        // Count sync events.
                        ++syncEvents;
                        LOGGER.finer("got sync event from tag " + String.format("%08x", evioEvent.getHeader().getTag()));
                    }
                                          
                } else if (EventTagConstant.EPICS.matches(evioEvent)) {
                                        
                    // Count EPICS events.
                    ++epicsEvents;
                    
                    // Get EPICS data for charge calculation.
                    epicsProcessor.process(evioEvent);
                    EpicsData epicsEvent = epicsProcessor.getEpicsData();
                    if (epicsEvent.hasKey("scaler_calc1")) {
                        epicsData.add(epicsEvent);    
                    }                    
                } 
            }
            
        } catch (final EvioException e) {
            // Error reading the EVIO file.
            throw new IOException(e);
        } finally {
            // Close the reader.
            if (evioReader != null) {
                try {
                    evioReader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "error closing EVIO reader", e);
                }
            }
        }
        
        LOGGER.info("done reading "  + totalEvents + " events");
        
        // Rough trigger rate calculation.
        triggerRate = calculateTriggerRate(firstPhysicsTimestamp, lastPhysicsTimestamp, physicsEvents);

        // Calculate ungated charge.
        //double ungatedCharge = calculateCharge(epicsData, firstPhysicsTimestamp, lastPhysicsTimestamp);
        
        // Calculated gated charge.
        //double gatedCharge = calculateGatedCharge(ungatedCharge, scalerData, ScalerDataIndex.FCUP_TRG_GATED, ScalerDataIndex.FCUP_TRG_UNGATED);
        
        // Create and fill the metadata map.
        final Map<String, Object> metadataMap = new LinkedHashMap<String, Object>();
        
        // Built-in fields of datacat.
        metadataMap.put("runMin", run);
        metadataMap.put("runMax", run);
        metadataMap.put("eventCount", totalEvents);
        
        // Run number.
        metadataMap.put("RUN", run);
        
        // File sequence number.
        metadataMap.put("FILE_NUMBER", fileNumber);
        
        // Blinded flag.
        metadataMap.put("BLINDED", blinded);
        
        // First and last timestamps which may come from control or physics events.
        metadataMap.put("FIRST_TIMESTAMP", firstTimestamp);
        metadataMap.put("LAST_TIMESTAMP", lastTimestamp);
        
        // First and last physics events.
        metadataMap.put("FIRST_PHYSICS_EVENT", firstPhysicsEvent);
        metadataMap.put("LAST_PHYSICS_EVENT", lastPhysicsEvent);
        
        // First and last physics event timestamps.
        metadataMap.put("FIRST_PHYSICS_TIMESTAMP", firstPhysicsTimestamp);
        metadataMap.put("LAST_PHYSICS_TIMESTAMP", lastPhysicsTimestamp);
        
        // Event counts.
        metadataMap.put("PHYSICS_EVENTS", physicsEvents);
        metadataMap.put("BAD_EVENTS", badEvents);
        metadataMap.put("EPICS_EVENTS", epicsEvents);        
        metadataMap.put("SCALER_EVENTS", scalerEvents);
        metadataMap.put("END_EVENTS", endEvents);
        metadataMap.put("PRESTART_EVENTS", prestartEvents);
        metadataMap.put("GO_EVENTS", goEvents);
        metadataMap.put("PAUSE_EVENTS", pauseEvents);
        metadataMap.put("SYNC_EVENTS", syncEvents);
        
        // Trigger rate.
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        metadataMap.put("TRIGGER_RATE_KHZ", Double.parseDouble(df.format(triggerRate)));
                                            
        // Add the trigger counts.
        for (Entry<TriggerType, Integer> entry : eventCounts.entrySet()) {
            metadataMap.put(entry.getKey().name(), entry.getValue());
        }
        
        // Print the file metadata to log.
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (Entry<String, Object> entry : metadataMap.entrySet()) {
            sb.append("  " + entry.getKey() + " = " + entry.getValue() + '\n');
        }
        LOGGER.info("file metadata ..." + '\n' + sb.toString());
        
        // Return the completed metadata map.
        return metadataMap;
    }

    /**
     * Calculate the trigger rate in KHz.
     * 
     * @param firstTimestamp the first physics timestamp
     * @param lastTimestamp the last physics timestamp
     * @param physicsEvents the number of physics events
     * @return the trigger rate calculation in KHz
     */
    private double calculateTriggerRate(Integer firstTimestamp, Integer lastTimestamp, int physicsEvents) {
        return ((double) physicsEvents / ((double) lastTimestamp - (double) firstTimestamp)) / 1000.;
    }    
}
