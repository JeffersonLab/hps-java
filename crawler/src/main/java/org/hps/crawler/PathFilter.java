package org.hps.crawler;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link java.io.FileFilter} which accepts a file if its path is 
 * equal to any of the paths in a set of strings.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class PathFilter implements FileFilter {

    private static Logger LOGGER = Logger.getLogger(PathFilter.class.getPackage().getName());
    
    /**
     * Set of paths for filtering.
     */
    private Set<String> paths = null;
    
    PathFilter(Set<String> paths) {
        this.paths = paths;
    }

    /**
     * Return <code>true</code> if the <code>pathname</code> has a path which is in the set of <code>paths</code>.
     * 
     * @return <code>true</code> if <code>pathname</code> passes the filter
     */
    @Override
    public boolean accept(File pathname) {
        for (String acceptPath : paths) {
            // FIXME: Use endsWith, equals or contains here????
            if (pathname.getPath().endsWith(acceptPath)) {
                LOGGER.info("accepted path " + pathname);
                return true;
            }
        }
        LOGGER.info("rejected path " + pathname);
        return false;
    }
}
