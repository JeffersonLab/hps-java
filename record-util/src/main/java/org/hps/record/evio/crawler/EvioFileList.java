package org.hps.record.evio.crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.lcsim.util.log.LogUtil;

/**
 * This is a list of <code>File</code> objects that are assumed to be EVIO files. There are some added utilities for getting the total number of
 * events in all the files.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileList extends ArrayList<File> {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(EvioFileList.class);

    /**
     * Event count by file.
     */
    private final Map<File, Integer> eventCounts = new HashMap<File, Integer>();

    /**
     * Get the first file.
     *
     * @return the first file
     */
    File first() {
        return this.get(0);
    }

    /**
     * Get the event count for an EVIO file.
     *
     * @param file the EVIO file
     * @return the event count for the file
     * @throws RuntimeException if the count was never computed (file is not in map)
     */
    int getEventCount(final File file) {
        if (this.eventCounts.get(file) == null) {
            throw new RuntimeException("The event count for " + file.getPath() + " was never computed.");
        }
        return this.eventCounts.get(file);
    }

    /**
     * Get the total number of events.
     * <p>
     * Files which do not have their event counts computed will be ignored.
     *
     * @return the total number of events
     */
    int getTotalEvents() {
        int totalEvents = 0;
        for (final File file : this) {
            if (this.eventCounts.containsKey(file)) {
                totalEvents += this.eventCounts.get(file);
            } else {
                // Warn about non-computed count.
                // FIXME: Perhaps this should actually be a fatal error.
                LOGGER.warning("event count for " + file.getPath() + " was not computed and will not be reflected in total");
            }
        }
        return totalEvents;
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
     * Set the event count for a file.
     *
     * @param file the EVIO file
     * @param eventCount the event count
     */
    void setEventCount(final File file, final Integer eventCount) {
        this.eventCounts.put(file, eventCount);
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