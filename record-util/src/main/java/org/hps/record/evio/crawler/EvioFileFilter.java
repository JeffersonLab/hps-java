package org.hps.record.evio.crawler;

import java.io.File;
import java.io.FileFilter;

final class EvioFileFilter implements FileFilter {

    @Override    
    public boolean accept(final File pathname) {
        boolean isEvio = pathname.getName().contains(".evio");
        boolean hasSeqNum = false;
        try {
            EvioFileUtilities.getSequenceNumber(pathname);
            hasSeqNum = true;
        } catch (Exception e) {
        }
        return isEvio && hasSeqNum;
    }
}