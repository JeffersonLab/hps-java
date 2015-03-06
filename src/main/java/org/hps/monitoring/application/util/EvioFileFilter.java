package org.hps.monitoring.application.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * This is a simple file filter that will accept files with ".evio" anywhere in their name. 
 */
public final class EvioFileFilter extends FileFilter {

    public EvioFileFilter() {            
    }
    
    @Override
    public boolean accept(File pathname) {
        if (pathname.getName().contains(".evio") || pathname.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "EVIO files";
    }        
}