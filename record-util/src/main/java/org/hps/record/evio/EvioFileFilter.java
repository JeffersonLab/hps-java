package org.hps.record.evio;

import java.io.File;
import java.io.FileFilter;

/**
 * This is a simple file filter that will accept EVIO files with a certain pattern to their file names:<br/>
 * <i>FILENAME.evio.SEQUENCE</i>.
 * <p>
 * This matches the convention used by the CODA DAQ software.
 */
public final class EvioFileFilter implements FileFilter {

    /**
     * Return <code>true</code> if file is an EVIO file with the correct file naming convention.
     *
     * @return <code>true</code> if file is an EVIO file with correct file naming convention
     */
    @Override
    public boolean accept(final File pathname) {
        final boolean isEvio = pathname.getName().contains(".evio");
        boolean hasSeqNum = false;
        try {
            EvioFileUtilities.getSequenceFromName(pathname);
            hasSeqNum = true;
        } catch (final Exception e) {
        }
        return isEvio && hasSeqNum;
    }
}