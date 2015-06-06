package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

/**
 * A miscellaneous collection of EVIO file utility methods used by classes in the crawler package.
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
     * Get the date from the control bank of an EVIO event.
     *
     * @param file the EVIO file
     * @param eventTag the event tag on the bank
     * @param gotoEvent an event to start the scanning
     * @return the control bank date or null if not found
     */
    static Date getControlDate(final File file, final int eventTag, final int gotoEvent) {
        Date date = null;
        EvioReader reader = null;
        try {
            reader = open(file, true);
            EvioEvent event;
            if (gotoEvent > 0) {
                reader.gotoEventNumber(gotoEvent);
            } else if (gotoEvent < 0) {
                reader.gotoEventNumber(reader.getEventCount() + gotoEvent);
            }
            while ((event = reader.parseNextEvent()) != null) {
                if (event.getHeader().getTag() == eventTag) {
                    final int[] data = EvioEventUtilities.getControlEventData(event);
                    final long seconds = data[0];
                    date = new Date(seconds * MILLISECONDS);
                    break;
                }
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return date;
    }

    /**
     * Get the date from the head bank.
     *
     * @param event the EVIO file
     * @return the date from the head bank or null if not found
     */
    static Date getHeadBankDate(final EvioEvent event) {
        Date date = null;
        final BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
        if (headBank != null) {
            final int[] data = headBank.getIntData();
            final long time = data[3];
            if (time != 0L) {
                date = new Date(time * MILLISECONDS);
            }
        }
        return date;
    }

    /**
     * Get the run end date which is taken either from the END event or the last physics event is the END event is not found.
     *
     * @param file the EVIO file
     * @return the run end date
     */
    static Date getRunEnd(final File file) {
        Date date = getControlDate(file, EvioEventConstants.END_EVENT_TAG, -10);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file, true);
                reader.gotoEventNumber(reader.getEventCount() - 11);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    /**
     * Get the run number from the file name.
     *
     * @param file the EVIO file
     * @return the run number
     * @throws Exception if there is a problem parsing out the run number
     */
    static Integer getRunFromName(final File file) {
        final String name = file.getName();
        final int startIndex = name.lastIndexOf("_") + 1;
        final int endIndex = name.indexOf(".");
        return Integer.parseInt(name.substring(startIndex, endIndex));
    }

    /**
     * Get the run start date from an EVIO file (should be the first in the run).
     * <p>
     * This is taken from the PRESTART event.
     *
     * @param file the EVIO file
     * @return the run start date
     */
    static Date getRunStart(final File file) {
        Date date = getControlDate(file, EvioEventConstants.PRESTART_EVENT_TAG, 0);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file, true);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    /**
     * Get the EVIO file sequence number (the number at the end of the file name).
     *
     * @param file the EVIO file
     * @return the file's sequence number
     * @throws Exception if there is an error parsing out the sequence number
     */
    static Integer getSequenceNumber(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    /**
     * Return <code>true</code> if this is a cached file e.g. the path starts with "/cache".
     *
     * @param file the file
     * @return <code>true</code> if the file is a cached file
     */
    static boolean isCachedFile(final File file) {
        return file.getPath().startsWith("/cache");
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
     * Open an EVIO file using a <code>EvioReader</code>.
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
        LOGGER.info("opened " + openFile.getPath() + " in " + end / MILLISECONDS + " seconds");
        return reader;
    }

    /**
     * Open an EVIO from a path.
     *
     * @param path the file path
     * @return the new <code>EvioReader</code> for the file
     * @throws IOException if there is an IO problem
     * @throws EvioException if there is an error reading the EVIO data
     */
    static EvioReader open(final String path) throws IOException, EvioException {
        return open(new File(path), true);
    }
}
