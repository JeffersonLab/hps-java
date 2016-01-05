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

import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.TIData;
import org.hps.record.triggerbank.TiTimeOffsetEvioProcessor;
import org.hps.record.triggerbank.TriggerType;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * Reads metadata from EVIO files, including the event count, run min and run max expected by the datacat, as well as
 * many custom field values applicable to HPS EVIO raw data.
 * 
 * @author Jeremy McCormick, SLAC
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
     * TI data bank definition.
     */
    private static IntBankDefinition TI_BANK = new IntBankDefinition(TIData.class, new int[] {0x2e, 0xe10a});

    /**
     * Get the EVIO file metadata.
     * 
     * @param file the EVIO file
     * @return the metadata map of key and value pairs
     */
    @Override
    public Map<String, Object> getMetadata(final File file) throws IOException {
        
        long events = 0;
        int badEvents = 0;
        int blinded = 0;
        Long run = null;
        Integer firstHeadTimestamp = null;
        Integer lastHeadTimestamp = null;
        Integer lastPhysicsEvent = null;
        Integer firstPhysicsEvent = null;
        double triggerRate = 0;
        long lastTI = 0;
        long minTIDelta = 0;
        long maxTIDelta = 0;
        long firstTI = 0;
        
        TiTimeOffsetEvioProcessor tiProcessor = new TiTimeOffsetEvioProcessor();

        // Create map for counting trigger types.
        Map<TriggerType, Integer> triggerCounts = new LinkedHashMap<TriggerType, Integer>();
        for (TriggerType triggerType : TriggerType.values()) {
            triggerCounts.put(triggerType, 0);
        }

        // Get the file number from the name.
        final int fileNumber = EvioFileUtilities.getSequenceFromName(file);

        // Files with a sequence number that is not divisible by 10 are blinded (Eng Run 2015 scheme).
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
            fileLoop: while (true) {
                try {
                    // Parse next event.
                    evioEvent = evioReader.parseNextEvent();

                    // End of file.
                    if (evioEvent == null) {
                        LOGGER.fine("EOF after " + events + " events");
                        break fileLoop;
                    }
                    
                    // Increment event count (doesn't count events that can't be parsed).
                    ++events;

                    // Debug print event number and tag.
                    LOGGER.finest("parsed event " + evioEvent.getEventNumber() + " with tag 0x"
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
                                    LOGGER.finer("first head timestamp " + firstHeadTimestamp + " from event "
                                            + evioEvent.getEventNumber());
                                }

                                // Last header timestamp.
                                lastHeadTimestamp = thisTimestamp;
                            }

                            // Run number.
                            if (run == null) {
                                if (headBankData[1] != 0) {
                                    run = (long) headBankData[1];
                                    LOGGER.finer("run " + run + " from event " + evioEvent.getEventNumber());
                                }
                            }
                        }
                    }

                    // Process trigger bank data for TI times (copied from Sho's BasicEvioFileReader class).
                    BaseStructure tiBank = TI_BANK.findBank(evioEvent);
                    if (tiBank != null) {
                        TIData tiData = new TIData(tiBank.getIntData());
                        if (lastTI == 0) {
                            firstTI = tiData.getTime();
                        }
                        lastTI = tiData.getTime();
                        if (thisTimestamp != 0) {
                            long delta = thisTimestamp * 1000000000L - tiData.getTime();
                            if (minTIDelta == 0 || minTIDelta > delta) {
                                minTIDelta = delta;
                            }
                            if (maxTIDelta == 0 || maxTIDelta < delta) {
                                maxTIDelta = delta;
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
                                LOGGER.finer("set first physics event " + firstPhysicsEvent);
                            }
                        }
                    }

                    // Count trigger types for this event.
                    Set<TriggerType> triggerTypes = TriggerType.getTriggerTypes(evioEvent);
                    for (TriggerType mask : triggerTypes) {
                        int count = triggerCounts.get(mask) + 1;
                        triggerCounts.put(mask, count);
                        LOGGER.finest("incremented " + mask.name() + " to " + count);
                    }
                    
                    // Activate TI time offset processor.
                    tiProcessor.process(evioEvent);
                    
                } catch (IOException | EvioException e) {
                    // Trap event processing errors.
                    badEvents++;
                    LOGGER.warning("error processing EVIO event " + evioEvent.getEventNumber());
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
                    LOGGER.log(Level.WARNING, "error closing EVIO reader", e);
                }
            }
        }

        LOGGER.info("done reading " + events + " events from " + file.getPath());

        // Rough trigger rate calculation.
        try {
            if (firstHeadTimestamp != null && lastHeadTimestamp != null && events > 0) {
                triggerRate = calculateTriggerRate(firstHeadTimestamp, lastHeadTimestamp, events);
            } else {
                LOGGER.log(Level.WARNING, "Missing information for calculating trigger rate.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calculating trigger rate.", e);
        }

        // Create and fill the metadata map.
        final Map<String, Object> metadataMap = new LinkedHashMap<String, Object>();

        // Set built-in system metadata.
        metadataMap.put("runMin", run);
        metadataMap.put("runMax", run);
        metadataMap.put("eventCount", events);
        metadataMap.put("size", size);
        metadataMap.put("checksum", checksum);
        
        // File sequence number.
        metadataMap.put("FILE", fileNumber);

        // Blinded flag.
        metadataMap.put("BLINDED", blinded);

        // First and last timestamps which may come from control or physics events.
        metadataMap.put("FIRST_HEAD_TIMESTAMP", firstHeadTimestamp);
        metadataMap.put("LAST_HEAD_TIMESTAMP", lastHeadTimestamp);

        // First and last physics event numbers.
        metadataMap.put("FIRST_PHYSICS_EVENT", firstPhysicsEvent);
        metadataMap.put("LAST_PHYSICS_EVENT", lastPhysicsEvent);

        // TI times and offset.
        metadataMap.put("FIRST_TI_TIME", firstTI);
        metadataMap.put("LAST_TI_TIME", lastTI);
        metadataMap.put("TI_TIME_DELTA", maxTIDelta - minTIDelta);
        
        // TI time offset (stored as string because of bug in MySQL datacat backend).
        metadataMap.put("TI_TIME_OFFSET", tiProcessor.getTiTimeOffset());

        // Event counts.
        metadataMap.put("BAD_EVENTS", badEvents);
        
        // Trigger rate in KHz.
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        metadataMap.put("TRIGGER_RATE", Double.parseDouble(df.format(triggerRate)));

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
        LOGGER.info("file metadata ..." + '\n' + sb.toString());

        // Return the completed metadata map.
        return metadataMap;
    }
         
    /**
     * Calculate the trigger rate in Hz.
     * 
     * @param firstTimestamp the first physics timestamp
     * @param lastTimestamp the last physics timestamp
     * @param events the number of physics events
     * @return the trigger rate calculation in KHz
     */
    private double calculateTriggerRate(Integer firstTimestamp, Integer lastTimestamp, long events) {
        return ((double) events / ((double) lastTimestamp - (double) firstTimestamp));
    }
}
