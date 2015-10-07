package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.evio.EvioFileUtilities;
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

        Date startDate = null;
        Date endDate = null;
        int badEventCount = 0;
        int eventCount = 0;
        int byteCount = 0;
        boolean hasPrestart = false;
        boolean hasEnd = false;
        int[] eventIdData = null;
        Integer run = null;
        Integer endEvent = null;
        Integer startEvent = null;
        Long lastTimestamp = null;

        EvioReader evioReader = null;
        try {
            evioReader = EvioFileUtilities.open(file, false);
        } catch (final EvioException e) {
            throw new IOException(e);
        }

        final int fileNumber = EvioFileUtilities.getSequenceFromName(file);

        EvioEvent evioEvent = null;

        while (true) {
            try {
                evioEvent = evioReader.parseNextEvent();
            } catch (IOException | EvioException e) {
                ++badEventCount;
                continue;
            }
            if (evioEvent == null) {
                break;
            }
            byteCount += evioEvent.getTotalBytes();
            if (EventTagConstant.PRESTART.equals(evioEvent)) {
                LOGGER.info("found PRESTART");
                hasPrestart = true;
                final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                final long timestamp = controlEventData[0] * 1000L;
                startDate = new Date(timestamp);
                LOGGER.info("set start date to " + startDate + " from PRESTART");
                if (run == null) {
                    run = controlEventData[1];
                    LOGGER.info("set run to " + run);
                }
            } else if (EventTagConstant.END.equals(evioEvent)) {
                LOGGER.info("found END event");
                hasEnd = true;
                final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                final long timestamp = controlEventData[0] * 1000L;
                endDate = new Date(timestamp);
                LOGGER.info("set end date to " + endDate);
                if (run == null) {
                    run = controlEventData[1];
                    LOGGER.info("set run to " + run);
                }
            } else if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                final int[] headBankData = EvioEventUtilities.getHeadBankData(evioEvent);
                if (startDate == null) {
                    if (headBankData[3] != 0) {
                        startDate = new Date(headBankData[3] * 1000L);
                        LOGGER.info("set start date to " + startDate + " from physics event");
                    }
                }
                if (run == null) {
                    run = headBankData[1];
                    LOGGER.info("set run to " + run + " from physics event");
                }
                eventIdData = EvioEventUtilities.getEventIdData(evioEvent);
                if (startEvent == null) {
                    startEvent = eventIdData[0];
                    LOGGER.info("set start event " + startEvent);
                }
                if (headBankData[3] != 0) {
                    lastTimestamp = headBankData[3] * 1000L;
                }
                ++eventCount;
            }
        }

        // Set end date from last valid timestamp.
        if (endDate == null) {
            endDate = new Date(lastTimestamp);
            LOGGER.info("set end date to " + endDate + " from last timestamp " + lastTimestamp);
        }

        // Set end event number.
        if (eventIdData != null) {
            endEvent = eventIdData[0];
            LOGGER.info("set end event " + endEvent);
        }

        final Map<String, Object> metaDataMap = new HashMap<String, Object>();

        metaDataMap.put("runMin", run);
        metaDataMap.put("runMax", run);
        metaDataMap.put("eventCount", eventCount);
        metaDataMap.put("size", byteCount);
        metaDataMap.put("fileNumber", fileNumber);
        metaDataMap.put("badEventCount", badEventCount);
        metaDataMap.put("endTimestamp", endDate.getTime());
        metaDataMap.put("startTimestamp", startDate.getTime());
        metaDataMap.put("startEvent", startEvent);
        metaDataMap.put("endEvent", endEvent);
        metaDataMap.put("hasEnd", hasEnd ? 1 : 0);
        metaDataMap.put("hasPrestart", hasPrestart ? 1 : 0);

        return metaDataMap;
    }
}
