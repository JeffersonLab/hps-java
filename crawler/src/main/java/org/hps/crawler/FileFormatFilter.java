package org.hps.crawler;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.datacat.client.DatasetFileFormat;

/**
 * Filter files on their format.
 * <p>
 * Only files matching the format will be accepted by the file visitor.
 *
 * @author Jeremy McCormick, SLAC
 */
public class FileFormatFilter implements FileFilter {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(FileFormatFilter.class.getPackage().getName());

    /**
     * The file format.
     */
    private final Set<DatasetFileFormat> formats;

    /**
     * Create a new filter with the given format.
     *
     * @param format the file format
     */
    FileFormatFilter(final Set<DatasetFileFormat> formats) {
        if (formats == null) {
            throw new IllegalArgumentException("The formats collection is null.");
        }
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("The formats collection is empty.");
        }
        this.formats = formats;
    }

    /**
     * Returns <code>true</code> if the file should be accepted, e.g. it matches the filer's format.
     *
     * @param pathname the file's full path
     */
    @Override
    public boolean accept(final File pathname) {
        LOGGER.info(pathname.getPath());
        final DatasetFileFormat fileFormat = DatacatUtilities.getFileFormat(pathname);
        if (fileFormat != null) {
            LOGGER.info("file " + pathname.getPath() + " has format " + fileFormat.name());
            return formats.contains(fileFormat);
        } else {
            LOGGER.info("rejected file " + pathname.getPath() + " with unknown format");
            return false;
        }
    }
}
