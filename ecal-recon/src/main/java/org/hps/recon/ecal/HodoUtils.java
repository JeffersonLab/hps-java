package org.hps.recon.ecal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.detector.hodoscope.HodoscopePixelDetectorElement;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.geometry.Detector;

/**
 *
 * @author mgraham
 * Utility classes for Hodoscope
 * created: 7/20/2019
 */
public class HodoUtils {

    public static Map<IIdentifier, DetectorElement> getHodoscopeMap(Detector detector) {
        Map<IIdentifier, DetectorElement> hodoMap = new HashMap<IIdentifier, DetectorElement>();
        List<HodoscopePixelDetectorElement> pixels = detector.getSubdetector("Hodoscope").getDetectorElement().findDescendants(HodoscopePixelDetectorElement.class);
        for (HodoscopePixelDetectorElement pix : pixels)
            hodoMap.put(pix.getIdentifier(), pix);
        return hodoMap;
    }
}
