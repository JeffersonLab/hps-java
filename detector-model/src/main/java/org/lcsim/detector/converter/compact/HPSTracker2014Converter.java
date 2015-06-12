package org.lcsim.detector.converter.compact;

import org.jdom.Element;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.compact.converter.HPSTracker2014JavaBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerJavaBuilder;
import org.lcsim.geometry.compact.converter.JavaSurveyVolume;
import org.lcsim.geometry.subdetector.HPSTracker2014;


/**
 * Converts the HPSTracker2014 compact description into Java runtime objects
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTracker2014Converter extends HPSTracker2014ConverterBase {

    public HPSTracker2014Converter() {
        super();
    }

    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#initializeBuilder(org.jdom.Element)
     */
    protected HPSTrackerJavaBuilder initializeBuilder(Element node) {
       return new HPSTracker2014JavaBuilder(_debug, node);
    }

    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#getSubdetectorType()
     */
    public Class getSubdetectorType() {
        return HPSTracker2014.class;
    }

    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#createSiSensor(int, java.lang.String, org.lcsim.detector.IDetectorElement, java.lang.String, org.lcsim.detector.identifier.IIdentifier)
     */
    HpsSiSensor createSiSensor(int sensorid, String name,
            IDetectorElement parent, String support, IIdentifier id) {
       return new HpsSiSensor(sensorid, name, parent, support, id);
    }

    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#getModuleNumber(org.lcsim.geometry.compact.converter.JavaSurveyVolume)
     */
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

