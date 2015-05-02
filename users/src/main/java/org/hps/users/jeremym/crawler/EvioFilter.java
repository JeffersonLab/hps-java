package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.FileFilter;

class EvioFilter implements FileFilter {

    @Override
    public boolean accept(final File pathname) {
        return pathname.getName().contains(".evio");
    }
}