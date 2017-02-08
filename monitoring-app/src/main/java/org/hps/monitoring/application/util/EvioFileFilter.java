package org.hps.monitoring.application.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * This is a file filter that will accept files with ".evio" anywhere in their name.
 */
public final class EvioFileFilter extends FileFilter {

    /**
     * Class constructor.
     */
    public EvioFileFilter() {
    }

    /**
     * Return <code>true</code> if path should be accepted.
     *
     * @return <code>true</code> to accept the path
     */
    @Override
    public boolean accept(final File path) {
        if (path.getName().contains(".evio") || path.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the description of the file filter.
     *
     * @return the description of the file filter
     */
    @Override
    public String getDescription() {
        return "EVIO files";
    }
}