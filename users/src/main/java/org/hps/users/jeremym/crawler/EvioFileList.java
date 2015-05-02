package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

class EvioFileList extends ArrayList<File> {

    void cache() {
        for (final File file : this) {
            EvioFileUtilities.cache(file);
        }
    }

    int computeTotalEvents() {
        int totalEvents = 0;
        for (final File file : this) {
            EvioReader reader = null;
            try {
                reader = new EvioReader(file, false);
                totalEvents += reader.getEventCount();
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
        return totalEvents;
    }

    File first() {
        return this.get(0);
    }

    File last() {
        return this.get(this.size() - 1);
    }

    void sort() {
        final List<File> fileList = new ArrayList<File>(this);
        Collections.sort(fileList, new EvioFileSequenceComparator());
        this.clear();
        this.addAll(fileList);
    }
}