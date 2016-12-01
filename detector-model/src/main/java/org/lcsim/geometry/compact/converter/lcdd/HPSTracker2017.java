package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.HPSTracker2017LCDDBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerLCDDBuilder;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;

public class HPSTracker2017 extends HPSTracker2014v1 {
    public HPSTracker2017(Element node) throws JDOMException {
        super(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.lcsim.geometry.compact.converter.lcdd.HPSTracker2014Base#
     * initializeBuilder(org.lcsim.geometry.compact.converter.lcdd.util.LCDD,
     * org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector)
     */
    protected HPSTrackerLCDDBuilder initializeBuilder(LCDD lcdd,
            SensitiveDetector sens) {
        return new HPSTracker2017LCDDBuilder(_debug, node, lcdd, sens);
    }

    /*
     * Override this to handle different layer numbering.
     * 
     * (non-Javadoc)
     * 
     * @see
     * org.lcsim.geometry.compact.converter.lcdd.HPSTracker2014#getModuleNumber
     * (java.lang.String)
     */
    protected int getModuleNumber(String surveyVolume) {
        boolean isTopLayer = HPSTrackerBuilder.getHalfFromName(surveyVolume)
                .equals("top") ? true : false;
        int layer = HPSTrackerBuilder.getLayerFromVolumeName(surveyVolume);
        int moduleNumber = -1;
        if (isTopLayer) {
            if (layer == 1 || layer > 4) {
                if (HPSTrackerBuilder.isHoleFromName(surveyVolume)) {
                    moduleNumber = 2;
                } else {
                    moduleNumber = 0;
                }
            } else {
                moduleNumber = 0;
            }
        } else {
            if (layer == 1 || layer > 4) {
                if (HPSTrackerBuilder.isHoleFromName(surveyVolume)) {
                    moduleNumber = 1;
                } else {
                    moduleNumber = 3;
                }
            } else {
                moduleNumber = 1;
            }
        }

        if (moduleNumber < 0)
            throw new RuntimeException("Invalid module nr found for "
                    + surveyVolume);

        return moduleNumber;
    }

}
