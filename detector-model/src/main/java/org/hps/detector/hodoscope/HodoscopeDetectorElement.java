package org.hps.detector.hodoscope;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.converter.compact.SubdetectorDetectorElement;
import org.lcsim.detector.solids.Box;
import org.lcsim.event.SimTrackerHit;

public final class HodoscopeDetectorElement extends SubdetectorDetectorElement{
                           
    public HodoscopeDetectorElement(String name, IDetectorElement parent) {
        super(name, parent);
    }
    
    public double[] getScintillatorHalfDimensions(SimTrackerHit hit) {
        IDetectorElement idDetElem = findDetectorElement(hit.getIdentifier()).get(0);
        Box box = (Box) idDetElem.getGeometry().getLogicalVolume().getSolid();
        return new double[] { box.getXHalfLength(), box.getYHalfLength(), box.getZHalfLength() };
    }
}