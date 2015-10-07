package org.hps.record.evio;

import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

import org.jlab.coda.jevio.EvioEvent;

// TODO: delete me
public class EvioFileMetadataProcessor extends EvioEventProcessor {
    
    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EvioFileMetadataProcessor.class.getPackage().getName());
    
    private File evioFile = null;
    private Date startDate = null;
    private Date endDate = null;
    private int badEventCount = 0;
    private int eventCount = 0;
    private int byteCount = 0;
    private boolean hasPrestart = false;
    private boolean hasEnd = false;
    private int[] eventIdData = null;
    private Integer run = null;
    private Integer endEvent = null;
    private Integer startEvent = null;
    private Long lastTimestamp = null;
            
    EvioFileMetadataProcessor(File evioFile) {
        this.evioFile = evioFile;
    }
              
    public void process(EvioEvent evioEvent) {
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
    
    EvioFileMetadata createEvioFileMetadata() {
        
        EvioFileMetadata metadata = new EvioFileMetadata(evioFile);
        
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
        
        // Set values on the metadata object.
        metadata.setStartDate(startDate);
        metadata.setEndDate(endDate);
        metadata.setBadEventCount(badEventCount);
        metadata.setByteCount(byteCount);
        metadata.setEventCount(eventCount);
        metadata.setHasPrestart(hasPrestart);
        metadata.setHasEnd(hasEnd);
        metadata.setRun(run);
        metadata.setEndEvent(endEvent);        
        metadata.setStartEvent(startEvent);
        
        return metadata;
    }
}
