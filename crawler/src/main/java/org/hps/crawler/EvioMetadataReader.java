package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EventTagBitMask;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * Reads metadata from EVIO files.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EvioMetadataReader implements FileMetadataReader {

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
        int badEvents = 0;
        int eventCount = 0;
        int epicsEvents = 0;
        int scalerBanks = 0;
        boolean prestart = false;
        boolean end = false;
        boolean go = false;
        boolean blinded = true;
        Integer run = null;
        Integer lastPhysicsEvent = null;
        Integer firstPhysicsEvent = null;
                
        // Create map for counting event masks.        
        Map<EventTagBitMask, Integer> triggerCounts = new HashMap<EventTagBitMask, Integer>();
        for (EventTagBitMask mask : EventTagBitMask.values()) {
            triggerCounts.put(mask, 0);
        }
        
        // Scaler processor to check for scaler bank.
        ScalersEvioProcessor scalersProcessor = new ScalersEvioProcessor();

        // Get the file number from the name.
        final int fileNumber = EvioFileUtilities.getSequenceFromName(file);
        
        // Only files divisible by 10 are unblinded (Eng Run 2015 scheme).
        if (fileNumber % 10 == 0) {
            blinded = false;
        }
        
        EvioReader evioReader = null;
        try {
                                   
            evioReader = EvioFileUtilities.open(file, true);
            
            EvioEvent evioEvent = null;

            // Event read loop.
            while (true) {
                
                // Read in an EVIO event, trapping exceptions in case a parse error occurs.
                boolean badEvent = false;
                try {
                    evioEvent = evioReader.parseNextEvent();
                } catch (IOException | EvioException e) {
                    badEvent = true;
                    LOGGER.warning("bad EVIO event " + evioEvent.getEventNumber() + " could not be parsed");
                }
                                
                // Increment bad event count and continue.
                if (badEvent) {
                    badEvents++;
                    continue;
                }
                
                // End of file.
                if (evioEvent == null) {
                    LOGGER.info("end of file reached after " + eventCount + " events");
                    break;
                }
                
                // Process different event types.
                if (EventTagConstant.PRESTART.equals(evioEvent)) {
                                                            
                    // File has PRESTART event.
                    LOGGER.info("found PRESTART event " + evioEvent.getEventNumber());
                    prestart = true;
                    
                    // Set the run number from the PRESTART event.
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.info("set run to " + run + " from PRESTART");
                    }
                    
                } else if (EventTagConstant.GO.equals(evioEvent)) {
                    
                    // File has GO event.
                    go = true;
                    
                    // Set the first timestamp from the GO event.
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    firstTimestamp = controlEventData[0];
                    LOGGER.info("set first timestamp to " + firstTimestamp + " from GO event " + evioEvent.getEventNumber());
                    
                } else if (EventTagConstant.END.equals(evioEvent)) {
                    
                    // File has END event.
                    LOGGER.info("got END event");
                    end = true;
                    
                    // Set the last timestamp from the END event.
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    lastTimestamp = controlEventData[0];
                    LOGGER.info("set last timestamp " + lastTimestamp + " from END event " + evioEvent.getEventNumber());
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.info("set run to " + run);
                    }
                    
                } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    
                    // Event count on the file only includes physics events.
                    eventCount++;
                    
                    // Get head bank.
                    final int[] headBankData = EvioEventUtilities.getHeadBankData(evioEvent);
                    
                    // Set first timestamp from head bank.
                    if (firstTimestamp == null) {
                        if (headBankData[3] != 0) {
                            firstTimestamp = headBankData[3];
                            LOGGER.info("set first timestamp to " + firstTimestamp + " from physics event " + evioEvent.getEventNumber());
                        }
                    }
                    
                    // Set run number from head bank if not set already.
                    if (run == null) {
                        run = headBankData[1];
                        LOGGER.info("set run to " + run + " from physics event " + evioEvent.getEventNumber());
                    }
                    
                    // Get the event ID data.
                    final int[] eventIdData = EvioEventUtilities.getEventIdData(evioEvent);
                    if (eventIdData == null) {
                        throw new RuntimeException("The event ID data bank for event " + evioEvent.getEventNumber() + " is null.");
                    }                    
                    
                    // Set the first physics event from event ID data.
                    if (firstPhysicsEvent == null) {
                        firstPhysicsEvent = eventIdData[0];
                        LOGGER.info("set start event " + firstPhysicsEvent + " from physics event " + evioEvent.getEventNumber());
                    }
                    
                    // Set the last physics event from the event ID data.
                    lastPhysicsEvent = eventIdData[0];
                   
                    // Set the last timestamp from head bank.
                    if (headBankData[3] != 0) {
                        lastTimestamp = headBankData[3];
                    }
                    
                    // Increment scaler bank count if exists in event.
                    scalersProcessor.process(evioEvent);
                    if (scalersProcessor.getCurrentScalerData() != null) {
                        scalerBanks++;
                    }
                    
                    // Increment event mask counts for each type.
                    Set<EventTagBitMask> masks = EventTagBitMask.getEventTagBitMasks(evioEvent);
                    for (EventTagBitMask mask : masks) {
                        int count = triggerCounts.get(mask) + 1;
                        triggerCounts.put(mask, count);
                    }                                    
                    
                } else if (EventTagConstant.EPICS.equals(evioEvent)) {
                    // Count EPICS events.
                    ++epicsEvents;
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
     
        // Create and fill the metadata map.
        final Map<String, Object> metaDataMap = new LinkedHashMap<String, Object>();
        metaDataMap.put("runMin", run);
        metaDataMap.put("runMax", run);
        metaDataMap.put("eventCount", eventCount);
        metaDataMap.put("FILE", fileNumber);
        metaDataMap.put("FIRST_TIMESTAMP", firstTimestamp);
        metaDataMap.put("LAST_TIMESTAMP", lastTimestamp);
        metaDataMap.put("FIRST_PHYSICS_EVENT", firstPhysicsEvent);
        metaDataMap.put("LAST_PHYSICS_EVENT", lastPhysicsEvent);        
        metaDataMap.put("BAD_EVENTS", badEvents);
        metaDataMap.put("EPICS_EVENTS", epicsEvents);        
        metaDataMap.put("SCALER_BANKS", scalerBanks);
        metaDataMap.put("END", end);
        metaDataMap.put("PRESTART", prestart);
        metaDataMap.put("GO", go);        
        metaDataMap.put("BLINDED", blinded);
                      
        // Add the event mask counts.
        for (Entry<EventTagBitMask, Integer> entry : triggerCounts.entrySet()) {
            // These two keys aren't working right so don't include them.
            if (EventTagBitMask.PHYSICS != entry.getKey() && EventTagBitMask.SYNC != entry.getKey()) {
                metaDataMap.put(entry.getKey().name(), entry.getValue());
            }
        }

        // Return the completed metadata map.
        return metaDataMap;
    }
}
