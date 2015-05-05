package org.lcsim.detector.converter.compact;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTracker2014v1JavaBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerJavaBuilder;
import org.lcsim.geometry.subdetector.HPSTracker2014v1;

public class HPSTracker2014v1Converter extends HPSTracker2014Converter {

    public HPSTracker2014v1Converter() {
        super();
    }
    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#initializeBuilder(org.jdom.Element)
     */
    protected HPSTrackerJavaBuilder initializeBuilder(Element node) {
       return new HPSTracker2014v1JavaBuilder(_debug, node);
    }

    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#getSubdetectorType()
     */
    public Class getSubdetectorType() {
        return HPSTracker2014v1.class;
    }

    
}
