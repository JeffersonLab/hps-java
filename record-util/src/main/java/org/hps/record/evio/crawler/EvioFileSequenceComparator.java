package org.hps.users.jeremym.crawler;

import java.io.File;
import java.util.Comparator;

class EvioFileSequenceComparator implements Comparator<File> {

    @Override
    public int compare(final File o1, final File o2) {
        final Integer sequenceNumber1 = EvioFileUtilities.getSequenceNumber(o1);
        final Integer sequenceNumber2 = EvioFileUtilities.getSequenceNumber(o2);
        return sequenceNumber1.compareTo(sequenceNumber2);
    }
}
