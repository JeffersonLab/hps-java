package org.hps.detector.hodoscope;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.converter.compact.SubdetectorDetectorElement;

public final class HodoscopeDetectorElement extends SubdetectorDetectorElement{
                           
    public HodoscopeDetectorElement(String name, IDetectorElement parent) {
        super(name, parent);
    }
}