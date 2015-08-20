package org.hps.record.evio;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Read {@link EvioFileMetaData} from an EVIO file.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EvioFileMetaDataReader {

    /**
     * The class logger.
     */
    private static Logger LOGGER = LogUtil.create(EvioFileMetaDataReader.class, new DefaultLogFormatter(), Level.ALL);

    /**
     * Get meta data from the EVIO file.
     *
     * @param evioFile the EVIO file
     * @return the file's meta data
     */
    public EvioFileMetaData getMetaData(final File evioFile) {

        LOGGER.info("getting meta data for " + evioFile.getPath());

        final EvioFileMetaData metaData = new EvioFileMetaData(evioFile);

        EvioEvent evioEvent = null;
        Date startDate = null;
        Date endDate = null;
        int badEventCount = 0;
        int eventCount = 0;
        int byteCount = 0;
        boolean hasPrestart = false;
        boolean hasEnd = false;
        int[] headBankData = null;
        int[] eventIdData = null;
        Integer run = null;
        Integer endEvent = null;
        Integer startEvent = null;

        final int fileNumber = EvioFileUtilities.getSequenceFromName(evioFile);
        LOGGER.info("set file number to " + fileNumber);

        try {
            final EvioReader evioReader = EvioFileUtilities.open(evioFile, false);
            LOGGER.getHandlers()[0].flush();
            eventCount = evioReader.getEventCount();
            while (true) {
                try {
                    evioEvent = evioReader.parseNextEvent();
                    if (evioEvent == null) {
                        break;
                    }
                } catch (final Exception e) {
                    badEventCount++;
                    LOGGER.log(Level.WARNING, "got bad EVIO event", e);
                    continue;
                }
                byteCount += evioEvent.getTotalBytes();
                if (EventTagConstant.PRESTART.isEventTag(evioEvent)) {
                    LOGGER.info("found PRESTART");
                    hasPrestart = true;
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    final long timestamp = controlEventData[0] * 1000L;
                    startDate = new Date(timestamp);
                    LOGGER.info("set start date to " + startDate);
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.info("set run to " + run);
                    }
                } else if (EventTagConstant.END.isEventTag(evioEvent)) {
                    LOGGER.info("found END");
                    hasEnd = true;
                    final int[] controlEventData = EvioEventUtilities.getControlEventData(evioEvent);
                    final long timestamp = controlEventData[0] * 1000L;
                    endDate = new Date(timestamp);
                    LOGGER.info("set end date to " + endDate);
                    if (run == null) {
                        run = controlEventData[1];
                        LOGGER.info("set run to " + run);
                    }
                } else if (EventTagBitMask.PHYSICS.isEventTag(evioEvent)) {
                    final BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
                    if (headBank != null) {
                        headBankData = headBank.getIntData();
                        if (startDate == null) {
                            startDate = new Date(headBankData[3] * 1000);
                            LOGGER.info("set start date to " + endDate + " from physics event");
                        }
                        if (run == null) {
                            run = headBankData[1];
                            LOGGER.info("set run to " + run + " from physics event");
                        }
                    }
                    eventIdData = EvioEventUtilities.getEventIdData(evioEvent);
                    if (eventIdData != null) {
                        if (startEvent == null) {
                            startEvent = eventIdData[0];
                            LOGGER.info("set start event " + startEvent);
                        }
                    }
                }
                LOGGER.getHandlers()[0].flush();
            }

            if (endDate == null) {
                if (headBankData != null) {
                    endDate = new Date(headBankData[3] * 1000);
                    LOGGER.info("set end date to " + endDate + " from last head bank");
                }
            }
            if (eventIdData != null) {
                endEvent = eventIdData[0];
                LOGGER.info("set end event " + endEvent);
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }

        metaData.setStartDate(startDate);
        metaData.setEndDate(endDate);
        metaData.setBadEventCount(badEventCount);
        metaData.setByteCount(byteCount);
        metaData.setEventCount(eventCount);
        metaData.setHasPrestart(hasPrestart);
        metaData.setHasEnd(hasEnd);
        metaData.setRun(run);
        metaData.setSequence(fileNumber);
        if (endEvent != null) {
            metaData.setEndEvent(endEvent);
        }
        if (startEvent != null) {
            metaData.setStartEvent(startEvent);
        }

        return metaData;
    }
}
