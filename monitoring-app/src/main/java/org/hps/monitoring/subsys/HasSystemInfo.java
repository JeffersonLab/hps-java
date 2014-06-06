package org.hps.monitoring.subsys;

/**
 * A simple mix-in interface for objects that carry {@link SystemInfo}
 * about some detector system.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface HasSystemInfo {
    
    SystemInfo getSystemInfo();
    
}
