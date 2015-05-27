package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.HPSTracker2014v1LCDDBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerLCDDBuilder;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;

public class HPSTracker2014v1 extends HPSTracker2014
{
    public HPSTracker2014v1(Element node) throws JDOMException
    {
        super(node);
    }

    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.lcdd.HPSTracker2014Base#initializeBuilder(org.lcsim.geometry.compact.converter.lcdd.util.LCDD, org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector)
     */
    protected HPSTrackerLCDDBuilder initializeBuilder(LCDD lcdd, SensitiveDetector sens) {
        return new HPSTracker2014v1LCDDBuilder(_debug,node,lcdd,sens);
    }

    
    
}
