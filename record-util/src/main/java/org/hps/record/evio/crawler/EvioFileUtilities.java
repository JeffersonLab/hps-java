package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

/**
 * A miscellaneous collection of EVIO file utility methods used by the crawler package.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileUtilities {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(EvioFileUtilities.class);

    /**
     * Milliseconds constant for conversion to/from second.
     */
    private static final long MILLISECONDS = 1000L;

    /**
     * Get a cached file path, assuming that the input file path is on the JLAB MSS e.g. it starts with "/mss".
     *
     * @param file the MSS file path
     * @return the cached file path (prepends "/cache" to the path)
     * @throws IllegalArgumentException if the file is not on the MSS (e.g. path does not start with "/mss")
     */
    static File getCachedFile(final File file) {
        if (!isMssFile(file)) {
            throw new IllegalArgumentException("File " + file.getPath() + " is not on the JLab MSS.");
        }
        if (isCachedFile(file)) {
            throw new IllegalArgumentException("File " + file.getPath() + " is already on the cache disk.");
        }
        return new File("/cache" + file.getPath());
    }

    /**
     * Get the end date
     *
     * @param evioReader the <code>EvioReader</code>
     * @return the run end date
     */
    static Long getEndTimestamp(final EvioReader evioReader) {

        // Date endDate = null;
        Long timestamp = null;

        try {
            // Search for the last physics event in the last 5 events of the file.
            System.out.println("going to event " + (evioReader.getEventCount() - 5) + " / "
                    + evioReader.getEventCount() + " to find end date");
            evioReader.gotoEventNumber(evioReader.getEventCount() - 5);
            EvioEvent evioEvent = null;
            EvioEvent lastPhysicsEvent = null;

            // Find last physics event.
            while ((evioEvent = evioReader.parseNextEvent()) != null) {
                if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                    lastPhysicsEvent = evioEvent;
                }
            }

            // If there is no physics event found this is an error.
            if (lastPhysicsEvent == null) {
                throw new RuntimeException("No physics event found.");
            }

            // Get the timestamp from the head bank of the physics event.
            LOGGER.info("getting head bank date from " + lastPhysicsEvent.getEventNumber());
            final Long eventTimestamp = getHeadBankTimestamp(lastPhysicsEvent);
            if (eventTimestamp != null) {
                LOGGER.info("found end timestamp " + eventTimestamp);
                timestamp = eventTimestamp;
            } else {
                throw new RuntimeException("No timestamp found in head bank.");
            }

        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }

    /**
     * Get the date from the head bank.
     *
     * @param event the EVIO file
     * @return the date from the head bank or null if not found
     */
    static Long getHeadBankTimestamp(final EvioEvent event) {
        // Date date = null;
        Long timestamp = null;
        final BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
        if (headBank != null) {
            final int[] data = headBank.getIntData();
            final long time = data[3];
            if (time != 0L) {
                timestamp = time * MILLISECONDS;
            }
        }
        return timestamp;
    }

    /**
     * Get the run number from the file name.
     *
     * @param file the EVIO file
     * @return the run number
     * @throws Exception if there is a problem parsing out the run number
     */
    static Integer getRun(final File file) {
        final String name = file.getName();
        final int startIndex = name.lastIndexOf("_") + 1;
        final int endIndex = name.indexOf(".");
        return Integer.parseInt(name.substring(startIndex, endIndex));
    }

    /**
     * Get the EVIO file sequence number, which is the number at the end of the file name.
     *
     * @param file the EVIO file
     * @return the file's sequence number
     * @throws Exception if there is an error parsing out the sequence number
     */
    static Integer getSequence(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    /**
     * Get the start date from the first physics event.
     *
     * @param evioReader the <code>EvioReader</code>
     * @return the run start date
     */
    static Long getStartTimestamp(final EvioReader evioReader) {

        Long timestamp = null;

        // Read events until there is a physics event and return its timestamp.
        try {
            EvioEvent event = null;
            while ((event = evioReader.parseNextEvent()) != null) {
                if (EvioEventUtilities.isPhysicsEvent(event)) {
                    if ((timestamp = getHeadBankTimestamp(event)) != null) {
                        break;
                    }
                }
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }

    /**
     * Get the date from the control bank of an EVIO event.
     *
     * @param file the EVIO file
     * @param eventTag the event tag on the bank
     * @param gotoEvent an event to start the scanning
     * @return the control bank date or null if not found
     */
    static Long getTimestamp(final EvioReader evioReader, final int eventTag, final int gotoEvent) {
        Long timestamp = null;
        try {
            EvioEvent evioEvent;
            if (gotoEvent > 0) {
                evioReader.gotoEventNumber(gotoEvent);
            } else if (gotoEvent < 0) {
                evioReader.gotoEventNumber(evioReader.getEventCount() + gotoEvent);
            }
            while ((evioEvent = evioReader.parseNextEvent()) != null) {
                if (evioEvent.getHeader().getTag() == eventTag) {
                    final int[] data = EvioEventUtilities.getControlEventData(evioEvent);
                    timestamp = (long) (data[0] * MILLISECONDS);
                    break;
                }
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }

    /**
     * Return <code>true</code> if this is a file on the cache disk e.g. the path starts with "/cache".
     *
     * @param file the file
     * @return <code>true</code> if the file is a cached file
     */
    static boolean isCachedFile(final File file) {
        return file.getPath().startsWith("/cache");
    }

    /**
     * Return <code>true</code> if a valid CODA <i>END</i> event can be located in the <code>EvioReader</code>'s current
     * file.
     *
     * @param reader the EVIO reader
     * @return <code>true</code> if valid END event is located
     * @throws Exception if there are IO problems using the reader
     */
    static boolean isEndOkay(final EvioReader reader) throws Exception {
        LOGGER.info("checking is END okay ...");

        boolean endOkay = false;

        // Go to second to last event for searching.
        reader.gotoEventNumber(reader.getEventCount() - 2);

        // Look for END event.
        EvioEvent event = null;
        while ((event = reader.parseNextEvent()) != null) {
            if (event.getHeader().getTag() == EvioEventConstants.END_EVENT_TAG) {
                endOkay = true;
                break;
            }
        }
        return endOkay;
    }

    /**
     * Return <code>true</code> if this file is on the JLAB MSS e.g. the path starts with "/mss".
     *
     * @param file the file
     * @return <code>true</code> if the file is on the MSS
     */
    static boolean isMssFile(final File file) {
        return file.getPath().startsWith("/mss");
    }

    /**
     * Open an EVIO file using an <code>EvioReader</code> in memory mapping mode.
     *
     * @param file the EVIO file
     * @return the new <code>EvioReader</code> for the file
     * @throws IOException if there is an IO problem
     * @throws EvioException if there is an error reading the EVIO data
     */
    static EvioReader open(final File file) throws IOException, EvioException {
        return open(file, false);
    }

    /**
     * Open an EVIO file, using the cached file path if necessary.
     *
     * @param file the EVIO file
     * @param sequential <code>true</code> to enable sequential reading
     * @return the new <code>EvioReader</code> for the file
     * @throws IOException if there is an IO problem
     * @throws EvioException if there is an error reading the EVIO data
     */
    static EvioReader open(final File file, final boolean sequential) throws IOException, EvioException {
        File openFile = file;
        if (isMssFile(file)) {
            openFile = getCachedFile(file);
        }
        final long start = System.currentTimeMillis();
        final EvioReader reader = new EvioReader(openFile, false, sequential);
        final long end = System.currentTimeMillis() - start;
        LOGGER.info("opened " + openFile.getPath() + " in " + (double) end / (double) MILLISECONDS + " seconds in "
                + (sequential ? "sequential" : "mmap" + " mode"));
        return reader;
    }

    /**
     * Open an EVIO file from a path string.
     *
     * @param path the path string
     * @return the new <code>EvioReader</code> for the file
     * @throws IOException if there is an IO problem
     * @throws EvioException if there is an error reading the EVIO data
     */
    static EvioReader open(final String path) throws IOException, EvioException {
        return open(new File(path), false);
    }

    /**
     * Present class instantiation.
     */
    private EvioFileUtilities() {
    }
}
