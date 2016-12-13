package org.lcsim.detector.converter.compact;

import org.jdom.Element;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsThinSiSensor;
import org.lcsim.geometry.compact.converter.HPSTracker2017JavaBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerJavaBuilder;
import org.lcsim.geometry.subdetector.HPSTracker2017;

public class HPSTracker2017Converter extends HPSTracker2014v1Converter {

    public HPSTracker2017Converter() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#
     * initializeBuilder(org.jdom.Element)
     */
    protected HPSTrackerJavaBuilder initializeBuilder(Element node) {
        return new HPSTracker2017JavaBuilder(_debug, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#
     * getSubdetectorType()
     */
    public Class getSubdetectorType() {
        return HPSTracker2017.class;
    }
    
    @Override
    HpsSiSensor createSiSensor(int sensorid, String name,
            IDetectorElement parent, String support, IIdentifier id) {
        int layer = HPSTrackerBuilder.getLayerFromVolumeName(name);
        if(layer == 1)  
            return new HpsThinSiSensor(sensorid, name, parent, support, id);
        else
            return new HpsSiSensor(sensorid, name, parent, support, id);
    }

    /*
     * Override this to handle different layer structure. (non-Javadoc)
     * 
     * TODO This function is duplicated! FIX THIS.
     * 
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#
     * getModuleNumber(org.lcsim.geometry.compact.converter.JavaSurveyVolume)
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
