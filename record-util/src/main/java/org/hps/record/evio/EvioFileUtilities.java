package org.hps.record.evio;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * A miscellaneous collection of EVIO file utility methods.
 */
public final class EvioFileUtilities {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EvioFileUtilities.class.getPackage().getName());

    /**
     * Milliseconds constant for conversion to/from second.
     */
    private static final long MILLISECONDS = 1000L;
   
    /**
     * Get the run number from the file name.
     *
     * @param file the EVIO file
     * @return the run number
     * @throws Exception if there is a problem parsing out the run number
     */
    public static Integer getRunFromName(final File file) {
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
    public static Integer getSequenceFromName(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    /**
     * Open an EVIO file using an <code>EvioReader</code> in memory mapping mode.
     *
     * @param file the EVIO file
     * @return the new <code>EvioReader</code> for the file
     * @throws IOException if there is an IO problem
     * @throws EvioException if there is an error reading the EVIO data
     */
    public static EvioReader open(final File file) throws IOException, EvioException {
        return open(file, true);
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
    public static EvioReader open(final File file, final boolean sequential) throws IOException, EvioException {
        LOGGER.info("opening " + file.getPath() + " in " + (sequential ? "sequential" : "mmap" + " mode"));
        final long start = System.currentTimeMillis();
        final EvioReader reader = new EvioReader(file, false, sequential);
        final long end = System.currentTimeMillis() - start;
        LOGGER.info("opened " + file.getPath() + " in " + (double) end / (double) MILLISECONDS + " seconds in "
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
    public static EvioReader open(final String path) throws IOException, EvioException {
        return open(new File(path), false);
    }

    /**
     * Sort a list of EVIO files by their sequence numbers.
     *
     * @param evioFileList the list of files to sort
     */
    public static void sortBySequence(final List<File> evioFileList) {
        Collections.sort(evioFileList, new EvioFileSequenceComparator());
    }

    /**
     * Prevent class instantiation.
     */
    private EvioFileUtilities() {
    }
}
