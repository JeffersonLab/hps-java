package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.HPSTracker2014LCDDBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerLCDDBuilder;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;

/**
 * 
 * Convert the HPS Test run tracker 2014 to the LCDD format.
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 *
 */
public class HPSTracker2014 extends HPSTracker2014Base
{
    public HPSTracker2014(Element node) throws JDOMException
    {
        super(node);
    }

    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.lcdd.HPSTracker2014Base#initializeBuilder(org.lcsim.geometry.compact.converter.lcdd.util.LCDD, org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector)
     */
    protected HPSTrackerLCDDBuilder initializeBuilder(LCDD lcdd, SensitiveDetector sens) {
        HPSTrackerLCDDBuilder b = new HPSTracker2014LCDDBuilder(_debug,node,lcdd,sens);
        return b;
    }

    protected int getModuleNumber(String surveyVolume) {
        boolean isTopLayer = HPSTrackerBuilder.getHalfFromName(surveyVolume).equals("top") ? true : false;
        int layer = HPSTrackerBuilder.getLayerFromVolumeName(surveyVolume);
        int moduleNumber = -1;
        if(isTopLayer) {
            if(layer < 4 ) {
                moduleNumber = 0;
            } else {
                if(HPSTrackerBuilder.isHoleFromName(surveyVolume)) {
                    moduleNumber = 0;
                } else {
                    moduleNumber = 2;
                }
            }
        } else {
            if(layer < 4 ) {
                moduleNumber = 1;
            } else {
                if(HPSTrackerBuilder.isHoleFromName(surveyVolume)) {
                    moduleNumber = 1;
                } else {
                    moduleNumber = 3;
                }
            }
        }

        if(moduleNumber<0) throw new RuntimeException("Invalid module nr found for " + surveyVolume);

                return moduleNumber;
    }

        

}





