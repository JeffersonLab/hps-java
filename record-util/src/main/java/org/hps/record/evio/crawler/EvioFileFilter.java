package org.hps.record.evio.crawler;

import java.io.File;
import java.io.FileFilter;

/**
 * This is a simple file filter that will accept EVIO files with a certain convention to their naming which looks like
 * <i>FILENAME.evio.SEQUENCE</i>. This matches the convention used by the CODA DAQ software.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileFilter implements FileFilter {

    /**
     * Return <code>true</code> if file is an EVIO file with correct file name convention.
     *
     * @return <code>true</code> if file is an EVIO file with correct file name convention
     */
    @Override
    public boolean accept(final File pathname) {
        final boolean isEvio = pathname.getName().contains(".evio");
        boolean hasSeqNum = false;
        try {
            EvioFileUtilities.getSequenceNumber(pathname);
            hasSeqNum = true;
        } catch (final Exception e) {
        }
        return isEvio && hasSeqNum;
    }
}