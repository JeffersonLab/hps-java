package org.hps.record.evio.crawler;

import java.io.File;
import java.util.Comparator;

/**
 * Compare two EVIO files by their sequence numbers.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileSequenceComparator implements Comparator<File> {

    /**
     * Compare two EVIO files by their sequence numbers.
     *
     * @return -1 if the first file's sequence number is less than the second's; 0 if equal; 1 if greater than
     */
    @Override
    public int compare(final File o1, final File o2) {
        final Integer sequenceNumber1 = EvioFileUtilities.getSequenceNumber(o1);
        final Integer sequenceNumber2 = EvioFileUtilities.getSequenceNumber(o2);
        return sequenceNumber1.compareTo(sequenceNumber2);
    }
}
