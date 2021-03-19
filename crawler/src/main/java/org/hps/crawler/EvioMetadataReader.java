package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerType;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * Creates detailed metadata for the datacat from an EVIO input file.
 */
final class EvioMetadataReader implements FileMetadataReader {

    /**
     * Initialize the package logger.
     */
    private static Logger LOGGER = Logger.getLogger(EvioMetadataReader.class.getPackage().getName());

    /**
     * Head bank definition.
     */
    private static IntBankDefinition HEAD_BANK = new IntBankDefinition(HeadBankData.class, new int[] {0x2e, 0xe10f});

    /**
     * Get the EVIO file metadata.
     * 
     * @param file the EVIO file
     * @return the metadata map of key and value pairs
     */
    @Override
    public Map<String, Object> getMetadata(final File file) throws IOException {
        
        long totalEvents = 0;
        int physicsEvents = 0;
        int badEvents = 0;
        int blinded = 0;
        Long run = null;
        Integer firstHeadTimestamp = null;
        Integer lastHeadTimestamp = null;
        Integer lastPhysicsEvent = null;
        Integer firstPhysicsEvent = null;
        Integer prestartTimestamp = null;        
        Integer endTimestamp = null;
        Integer goTimestamp = null;
        Double triggerRate = null;
        Integer endEventCount = null;
        
        // Processor for calculating TI time offsets.
        TiTimeOffsetEvioProcessor tiProcessor = new TiTimeOffsetEvioProcessor();

        // Create map for counting trigger types.
        Map<TriggerType, Integer> triggerCounts = new LinkedHashMap<TriggerType, Integer>();
        for (TriggerType triggerType : TriggerType.values()) {
            triggerCounts.put(triggerType, 0);
        }

        // Get the file number from the name.
        final int fileNumber = EvioFileUtilities.getSequenceFromName(file);

        // File numbers indivisible by 10 are blinded (Eng Run 2015 scheme).
        if (!(fileNumber % 10 == 0)) {
            blinded = 1;
        }
        
        // Get file size.
        long size = 0;
        File cacheFile = file;
        if (FileUtilities.isMssFile(file)) {
            cacheFile = FileUtilities.getCachedFile(file);
        }
        size = cacheFile.length();
        
        // Compute MD5 checksum string.
        String checksum = FileUtilities.createMD5Checksum(cacheFile);

        EvioReader evioReader = null;
        try {
            // Open file in sequential mode.
            evioReader = EvioFileUtilities.open(file, true);
            EvioEvent evioEvent = null;

            // Event read loop.
            eventLoop: while (true) {
                try {
                    // Parse next event.
                    evioEvent = evioReader.parseNextEvent();

                    // End of file.
                    if (evioEvent == null) {
                        LOGGER.fine("EOF after " + totalEvents + " events.");
                        break eventLoop;
                    }
                    
                    // Increment event count (doesn't count events that can't be parsed).
                    ++totalEvents;

                    // Debug print event number and tag.
                    LOGGER.finest("Parsed event " + evioEvent.getEventNumber() + " with tag 0x"
                            + String.format("%08x", evioEvent.getHeader().getTag()));

                    // Get head bank.
                    BaseStructure headBank = HEAD_BANK.findBank(evioEvent);

                    // Current timestamp.
                    int thisTimestamp = 0;

                    // Process head bank if not null.
                    if (headBank != null) {
                        if (headBank != null) {
                            final int[] headBankData = headBank.getIntData();
                            thisTimestamp = headBankData[3];
                            if (thisTimestamp != 0) {
                                // First header timestamp.
                                if (firstHeadTimestamp == null) {
                                    firstHeadTimestamp = thisTimestamp;
                                    LOGGER.finer("First head timestamp " + firstHeadTimestamp + " from event "
                                            + evioEvent.getEventNumber());
                                }

                                // Last header timestamp.
                                lastHeadTimestamp = thisTimestamp;
                            }

                            // Run number.
                            if (run == null) {
                                if (headBankData[1] != 0) {
                                    run = (long) headBankData[1];
                                    LOGGER.finer("Run number " + run + " from event " + evioEvent.getEventNumber());
                                }
                            }
                        }
                    }
                    
                    if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                                                
                        final int[] eventIdData = EvioEventUtilities.getEventIdData(evioEvent);
                        
                        if (eventIdData != null) {
                        
                            // Set the last physics event.
                            lastPhysicsEvent = eventIdData[0];

                            // Set the first physics event.
                            if (firstPhysicsEvent == null) {
                                firstPhysicsEvent = eventIdData[0];
                                LOGGER.finer("Set first physics event " + firstPhysicsEvent);
                            }
                        }
                        
                        ++physicsEvents;
                    } else if (EvioEventUtilities.isControlEvent(evioEvent)) {
                        int[] controlData = EvioEventUtilities.getControlEventData(evioEvent);
                        if (controlData != null) { /* Why is this null sometimes? */
                            if (controlData[0] != 0) {
                                if (EventTagConstant.PRESTART.isEventTag(evioEvent)) {
                                    prestartTimestamp = controlData[0];
                                }                        
                                if (EventTagConstant.GO.isEventTag(evioEvent)) {
                                    goTimestamp = controlData[0];
                                }
                                if (EventTagConstant.END.isEventTag(evioEvent)) {
                                    endTimestamp = controlData[0];
                                    endEventCount = controlData[2];
                                }
                            }
                        } else {
                            LOGGER.warning("Event " + evioEvent.getEventNumber() + " is missing valid control data bank.");
                        }
                    }

                    // Count trigger types for this event.
                    Set<TriggerType> triggerTypes = TriggerType.getTriggerTypes(evioEvent);
                    for (TriggerType mask : triggerTypes) {
                        int count = triggerCounts.get(mask) + 1;
                        triggerCounts.put(mask, count);
                        LOGGER.finest("Incremented " + mask.name() + " to " + count);
                    }
                    
                    // Activate TI time offset processor.
                    tiProcessor.process(evioEvent);
                    
                } catch (Exception e) {  
                    
                    // Log event processing errors.
                    LOGGER.log(Level.WARNING, "Error processing EVIO event " + evioEvent.getEventNumber(), e);
                    
                    // Increment bad event count.
                    badEvents++;
                }
            }
        } catch (final EvioException e) {
            // Error reading the EVIO file.
            throw new IOException("Error reading EVIO file.", e);
        } finally {
            // Close the reader.
            if (evioReader != null) {
                try {
                    evioReader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing EVIO reader", e);
                }
            }
        }

        LOGGER.info("Done reading " + totalEvents + " events from " + file.getPath());

        // Rough trigger rate calculation.
        try {
            if (firstHeadTimestamp != null && lastHeadTimestamp != null && totalEvents > 0 
                    && (firstHeadTimestamp - lastHeadTimestamp != 0)) {
                triggerRate = calculateTriggerRate(firstHeadTimestamp, lastHeadTimestamp, totalEvents);
            } else {
                LOGGER.log(Level.WARNING, "Missing information for calculating trigger rate.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calculating the trigger rate.", e);
        }

        // Create and fill the metadata map.
        final Map<String, Object> metadataMap = new LinkedHashMap<String, Object>();

        try {
            if (run == null) {
                run = new Long(EvioFileUtilities.getRunFromName(file));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get run number from event data or file name.", e);
        }

        // Set locationExtras metadata.
        metadataMap.put("runMin", run);
        metadataMap.put("runMax", run);
        metadataMap.put("eventCount", totalEvents);
        metadataMap.put("size", size);
        metadataMap.put("checksum", checksum);     
        
        // File sequence number.
        metadataMap.put("FILE", fileNumber);

        // Blinded flag.
        metadataMap.put("BLINDED", blinded);

        // First and last timestamps which may come from control or physics events.
        if (firstHeadTimestamp != null) {
            metadataMap.put("FIRST_HEAD_TIMESTAMP", firstHeadTimestamp);
        } 
        
        if (lastHeadTimestamp != null) {
            metadataMap.put("LAST_HEAD_TIMESTAMP", lastHeadTimestamp);
        } 

        // First and last physics event numbers.
        if (firstPhysicsEvent != null) {
            metadataMap.put("FIRST_PHYSICS_EVENT", firstPhysicsEvent);
        } 
        
        if (lastPhysicsEvent != null) {
            metadataMap.put("LAST_PHYSICS_EVENT", lastPhysicsEvent);
        }
        
        // Timestamps which are only set if the corresponding control events were found in the file.
        if (prestartTimestamp != null) {
            metadataMap.put("PRESTART_TIMESTAMP", prestartTimestamp);
        }
        if (endTimestamp != null) {
            metadataMap.put("END_TIMESTAMP", endTimestamp);
        }
        if (goTimestamp != null) {
            metadataMap.put("GO_TIMESTAMP", goTimestamp);
        }
        
        if (endEventCount != null) {
            metadataMap.put("END_EVENT_COUNT", endEventCount);
        }

        // TI times and offset.
        metadataMap.put("TI_TIME_MIN_OFFSET", new Long(tiProcessor.getMinOffset()).toString());
        metadataMap.put("TI_TIME_MAX_OFFSET", new Long(tiProcessor.getMaxOffset()).toString());
        metadataMap.put("TI_TIME_N_OUTLIERS", tiProcessor.getNumOutliers());
        
        // Bad event count.
        metadataMap.put("BAD_EVENTS", badEvents);
        
        // Physics event count.
        metadataMap.put("PHYSICS_EVENTS", physicsEvents);
        
        // Rough trigger rate calculation.
        if (triggerRate != null && !Double.isInfinite(triggerRate) && !Double.isNaN(triggerRate)) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            LOGGER.info("Setting trigger rate to " + triggerRate + " Hz.");
            metadataMap.put("TRIGGER_RATE", Double.parseDouble(df.format(triggerRate)));
        } else {
            LOGGER.warning("Failed to calculate trigger rate.");
        }        

        // Trigger type counts.
        for (Entry<TriggerType, Integer> entry : triggerCounts.entrySet()) {
            metadataMap.put(entry.getKey().name(), entry.getValue());
        }

        // Print the file metadata to log.
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (Entry<String, Object> entry : metadataMap.entrySet()) {
            sb.append("  " + entry.getKey() + " = " + entry.getValue() + '\n');
        }
        LOGGER.info("File metadata ..." + '\n' + sb.toString());

        // Return the completed metadata map.
        return metadataMap;
    }
         
    /**
     * Calculate the trigger rate in Hz.
     * 
     * @param firstTimestamp the first physics timestamp
     * @param lastTimestamp the last physics timestamp
     * @param events the number of physics events
     * @return the trigger rate in Hz
     */
    private double calculateTriggerRate(Integer firstTimestamp, Integer lastTimestamp, long events) {
        return ((double) events / ((double) lastTimestamp - (double) firstTimestamp));
    }
}
