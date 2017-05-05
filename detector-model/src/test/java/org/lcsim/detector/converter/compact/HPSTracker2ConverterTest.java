package org.lcsim.detector.converter.compact;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;

/**
 * Unit test for the HPSTracker2Coverter.
 */
public class HPSTracker2ConverterTest extends TestCase {
   
    // Initialize the logger
    private static Logger LOGGER = Logger.getLogger(HPSTracker2ConverterTest.class.getPackage().getName());
    
    Detector detector = null;
   
    //-----------------//
    //--- Constants ---//
    //-----------------//

    private static final int TOTAL_NUMBER_OF_SENSORS = 20; 
    private static final int TOTAL_NUMBER_OF_STEREO_LAYERS = 10; 
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    public static final int NUMBER_OF_READOUT_STRIPS = 639;
    public static final int NUMBER_OF_SENSE_STRIPS = 1277;
    
    //-----------------//
    //-----------------//
    
    public static Test suite() {
        return new TestSuite(HPSTracker2ConverterTest.class);
    }

    private static final String resource = "/org/lcsim/geometry/subdetector/HPSTracker2Test.xml";
    public void setUp() {
        InputStream in = this.getClass().getResourceAsStream(resource);

        GeometryReader reader = new GeometryReader();

        try {
            detector = reader.read(in);
        }
        catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }
    
    public void testHPSTracker2Converter() {
       
        // Test if the correct number of sensors was created.
        LOGGER.info("Checking if the correct number of sensors were created.");
        List<HpsSiSensor> sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        assertTrue("[ " + this.getClass().getSimpleName() + " ]: The wrong number of sensors were created.",
                sensors.size() == TOTAL_NUMBER_OF_SENSORS);
        LOGGER.info("Total number of sensors that were created: " + sensors.size());
       
        // Test if the sensors that were created are instances of HpsSiSensor
        LOGGER.info("Checking if sensors were initialized correctly");
        for(HpsSiSensor sensor : sensors) {
            assertTrue("[ " + this.getClass().getSimpleName() + " ]: Sensor is of wrong type: " + sensor.getClass().getSimpleName(),
                        sensor instanceof HpsSiSensor);
            assertTrue("[ " + this.getClass().getSimpleName() + " ]: Wrong number of readout electrodes found.",
                    sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells() == NUMBER_OF_READOUT_STRIPS);
            
            assertTrue("[ " + this.getClass().getSimpleName() + " ]: Wrong number of sense electrodes found.",
                    sensor.getSenseElectrodes(ChargeCarrier.HOLE).getNCells() == NUMBER_OF_SENSE_STRIPS);
            LOGGER.info(sensor.toString());
        }
        LOGGER.info("Sensors were all initialized correctly.");
        
        // Check that the correct number of stereo layers were created
        LOGGER.info("Checking if the correct number of stereo layers were created.");
        List<SvtStereoLayer> stereoLayers = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();
        // Check that the number of stereo layers created is as expected
        assertTrue("[ " + this.getClass().getSimpleName() + " ]: The wrong number of stereo layers were created.",
                stereoLayers.size() == TOTAL_NUMBER_OF_STEREO_LAYERS);
        LOGGER.info("Total number of stereo layers created: " + stereoLayers.size());
        
        for(SvtStereoLayer stereoLayer : stereoLayers){
            LOGGER.fine(stereoLayer.toString());
            
            // The sensors comprising the stereo layer should belong to the same detector volume
            assertTrue("[ " + this.getClass().getSimpleName() + " ]: Sensors belong to different detector volumes.", 
                    stereoLayer.getAxialSensor().getModuleNumber() == stereoLayer.getStereoSensor().getModuleNumber());
            
            // If the stereo layer is part of the top detector volume, the axial layers have an odd layer number.
            // If the stereo layer is part of the bottom detector volume, the axial layers have an even layer number.
            LOGGER.info("Checking if the layers are oriented correctly."); 
            if(stereoLayer.getAxialSensor().isTopLayer()){
                assertTrue("[ " + this.getClass().getSimpleName() + " ]: Sensors composing the stereo layer are flipped",
                        stereoLayer.getAxialSensor().getLayerNumber()%2 == 1);
            } else { 
                assertTrue("[ " + this.getClass().getSimpleName() + " ]: Sensors composing the stereo layer are flipped",
                        stereoLayer.getAxialSensor().getLayerNumber()%2 == 0);
            }
        }
    }
}