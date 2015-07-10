package org.hps.record.evio.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.lcsim.util.log.LogUtil;

/**
 * This is a list of <code>File</code> objects that are assumed to be EVIO files.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileList extends ArrayList<File> {

    /**
     * Get the first file.
     *
     * @return the first file
     */
    File first() {
        return this.get(0);
    }

    /**
     * Get the last file.
     *
     * @return the last file
     */
    File last() {
        return this.get(this.size() - 1);
    }

    /**
     * Sort the files in-place by their sequence number.
     */
    void sort() {
        final List<File> fileList = new ArrayList<File>(this);
        Collections.sort(fileList, new EvioFileSequenceComparator());
        this.clear();
        this.addAll(fileList);
    }
}