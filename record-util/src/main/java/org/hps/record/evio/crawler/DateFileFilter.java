package org.hps.record.evio.crawler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

/**
 * Filter a file on its creation date.
 * <p>
 * Files with a creation date after the time stamp will be rejected.
 *
 * @author Jeremy McCormick, SLAC
 */
final class DateFileFilter implements FileFilter {

    /**
     * The cut off timestamp.
     */
    private final Date date;

    /**
     * Create a filter with the given date as the cut off.
     *
     * @param date the time stamp cut off
     */
    DateFileFilter(final Date date) {
        this.date = date;
    }

    /**
     * Return <code>true</code> if the file was created before the time stamp date.
     *
     * @return <code>true</code> if file was created before the time stamp date
     */
    @Override
    public boolean accept(final File pathname) {
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(pathname.toPath(), BasicFileAttributes.class);
        } catch (final IOException e) {
            throw new RuntimeException("Error getting file attributes.", e);
        }
        return attr.creationTime().toMillis() > this.date.getTime();
    }
}
