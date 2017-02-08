package org.lcsim.detector.converter.compact;

import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal2;

public class HPSEcal2Converter extends AbstractSubdetectorConverter {

    public void convert(Subdetector subdet, Detector detector) {
        System.out.println(this.getClass().getCanonicalName());
    }

    public Class getSubdetectorType() {
        return HPSEcal2.class;
    }
}
