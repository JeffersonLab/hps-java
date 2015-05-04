package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

class DateFileFilter implements FileFilter {

    private final Date date;

    DateFileFilter(final Date date) {
        this.date = date;
    }

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
