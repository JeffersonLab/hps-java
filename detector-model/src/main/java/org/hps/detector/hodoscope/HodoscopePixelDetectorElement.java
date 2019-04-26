package org.hps.detector.hodoscope;

import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;

public class HodoscopePixelDetectorElement extends DetectorElement {
    
    public HodoscopePixelDetectorElement(String name, IDetectorElement parent, String support, IIdentifier id) {
        super(name, parent, support, id);
    }
}
