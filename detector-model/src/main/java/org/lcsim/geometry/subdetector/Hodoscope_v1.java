package org.lcsim.geometry.subdetector;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.converter.heprep.DetectorElementToHepRepConverter;

import hep.graphics.heprep.HepRep;
import hep.graphics.heprep.HepRepFactory;

public class Hodoscope_v1 extends AbstractTracker {

    Hodoscope_v1(Element node) throws JDOMException {
        super(node);
    }

    public void appendHepRep(HepRepFactory factory, HepRep heprep) {
        DetectorElementToHepRepConverter.convert(getDetectorElement(), factory, heprep, -1, false, 
                getVisAttributes().getColor());
    }

    public boolean isEndcap() {
        return false;
    }

    public boolean isBarrel() {
        return true;
    }
}
