package org.lcsim.detector.converter.compact;

import java.io.InputStream;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;

/**
 * Unit test for the {@link HPSTracker2Converter} when the sensor type
 * is equal to {@link HpsTestRunSiSensor} 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class HpsTestRunSiSensorConverterTest extends TestCase {
    
    Detector detector = null;
   
    //-----------------//
    //--- Constants ---//
    //-----------------//
    private static final int TOTAL_NUMBER_OF_SENSORS = 20; 
    private static final int TOTAL_NUMBER_OF_STEREO_LAYERS = 10; 
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static final String RESOURCE = "/org/lcsim/geometry/subdetector/HpsTestRunSiSensorConverterTest.xml";
    
    /*public static Test suite() {
        return new TestSuite(HPSTracker2ConverterTest.class);
    }*/
    
    public void setUp() { 
        
        InputStream in = this.getClass().getResourceAsStream(RESOURCE);

        GeometryReader reader = new GeometryReader();

        try {
            detector = reader.read(in);
        }
        catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }
    
    
    public void testHPSTracker2Converter() { 
        
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Checking if the correct number of sensors were created.");
        List<HpsSiSensor> sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        assertTrue("[ " + this.getClass().getSimpleName() + " ]: The wrong number of sensors were created.", sensors.size() == TOTAL_NUMBER_OF_SENSORS);
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Total number of sensors that were created: " + sensors.size());
        
        
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Checking if sensor is instance of HpsTestRunSiSensor.");
        for(HpsSiSensor sensor : sensors) {
            assertTrue("[ " + this.getClass().getSimpleName() + " ]: Sensor is of wrong type: " + sensor.getClass().getSimpleName(),
                        sensor instanceof HpsTestRunSiSensor);
        }
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Sensors are all instances of HpsTestRunSiSensor.");
        
        
        // Check that the correct number of stereo layers were created
        System.out.println("[ HPSTracker2ConverterTest ]: Checking if the correct number of stereo layers were created.");
        List<SvtStereoLayer> stereoLayers = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();
        // Check that the number of stereo layers created is as expected
        assertTrue("The wrong number of stereo layers were created.", stereoLayers.size() == TOTAL_NUMBER_OF_STEREO_LAYERS);
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Total number of stereo layers created: " + stereoLayers.size());

        for(SvtStereoLayer stereoLayer : stereoLayers){
            System.out.println("[ " + this.getClass().getSimpleName() + " ]: " + stereoLayer.toString());
            
            // The sensors comprising the stereo layer should belong to the same detector volume
            assertTrue("Sensors belong to different detector volumes.", 
                    stereoLayer.getAxialSensor().getModuleNumber() == stereoLayer.getStereoSensor().getModuleNumber());
            
            // If the stereo layer is part of the top detector volume, the axial layers have an odd layer number.
            // If the stereo layer is part of the bottom detector volumen, the axial layers have an even layer number. 
            System.out.println("[ " + this.getClass().getSimpleName() + " ]: check if the layers are oriented correctly."); 
            if(stereoLayer.getAxialSensor().isTopLayer()){
                assertTrue("Sensors composing the stereo layer are flipped", stereoLayer.getAxialSensor().getLayerNumber()%2 == 1);
            } else { 
                assertTrue("Sensors composing the stereo layer are flipped", stereoLayer.getAxialSensor().getLayerNumber()%2 == 0);
                
            }
        } 
    }
}
