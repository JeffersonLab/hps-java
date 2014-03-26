package org.hps.util;

/**
 * Drivers that will be attached to monitoring system should implement 
 * this if they will be reset when the "reset" button is pressed on the
 * monitoring app.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @deprecated Use standard methods like {@link org.lcsim.util.Driver#startOfData()} 
 * or {@link org.lcsim.util.Driver#detectorChanged(Detector)}.
 */
@Deprecated
public interface Resettable {
    void reset();
}
