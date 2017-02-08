/**
 * 
 */
package org.lcsim.detector.converter.compact;

import org.jdom.Element;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014JavaBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerJavaBuilder;
import org.lcsim.geometry.subdetector.HPSTestRunTracker2014;

/**
 * Converts the HPSTestRunTracker2014 compact description into Java runtime objects.
 */
public class HPSTestRunTracker2014Converter extends HPSTracker2014ConverterBase {

    public HPSTestRunTracker2014Converter() {
        super();
    }
    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#initializeBuilder(org.jdom.Element)
     */
    protected HPSTrackerJavaBuilder initializeBuilder(Element node) {
         return new HPSTestRunTracker2014JavaBuilder(_debug,node);
     }
    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#getSubdetectorType()
     */
    public Class getSubdetectorType() {
        return HPSTestRunTracker2014.class;
    }

    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#createSiSensor(int, java.lang.String, org.lcsim.detector.IDetectorElement, java.lang.String, org.lcsim.detector.identifier.IIdentifier)
     */
    HpsSiSensor createSiSensor(int sensorid, String name,
            IDetectorElement parent, String support, IIdentifier id) {
        return new HpsTestRunSiSensor(sensorid, name, parent, support, id);
    }
    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#getModuleNumber(org.lcsim.geometry.compact.converter.JavaSurveyVolume)
     */
    protected int getModuleNumber(String surveyVolume) {
        return HPSTrackerBuilder.getHalfFromName(surveyVolume).equals("top") ? 0 : 1;
    }
    
}
