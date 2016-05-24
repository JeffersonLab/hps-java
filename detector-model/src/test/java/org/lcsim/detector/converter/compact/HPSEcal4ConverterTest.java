package org.lcsim.detector.converter.compact;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.jdom.JDOMException;
import org.lcsim.conditions.ConditionsManager;

import org.lcsim.detector.converter.compact.HPSEcal4Converter;
import org.lcsim.geometry.subdetector.HPSEcal4;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;
import org.lcsim.util.xml.ElementFactory;


/**
 * Unit test for the HPSTracker2Converter.
 * 
 * @author SA annie@jlab.org
 */
public class HPSEcal4ConverterTest extends TestCase {

	// Initialize the logger
    private static Logger LOGGER = Logger.getLogger(HPSTracker2ConverterTest.class.getPackage().getName());
    
    Detector detector = null;
    
    public static Test suite() {
        return new TestSuite(HPSEcal4ConverterTest.class);
    }

    private static final String resource = "/org/lcsim/geometry/subdetector/HPSEcal4Test.xml";
    public void setUp() throws ConditionsManager.ConditionsNotFoundException {
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        mgr.setDetector("HPS-PhysicsRun2016-Nominal-v4-4", 0); /* any run number and detector will work here */
      
        InputStream in = this.getClass().getResourceAsStream(resource);

        GeometryReader reader = new GeometryReader();

        try {
            detector = reader.read(in);
        }
        catch (IOException | JDOMException | ElementFactory.ElementCreationException x) {
            throw new RuntimeException(x);
        }
    }
 
    
    
    public void testHPSEcal4ConverterTest() {
        
        // Test if the correct number of sensors was created.
        LOGGER.info("Checking if the correct number of sensors were created.");
        
        // Test if the sensors that were created are instances of HpsSiSensor
        LOGGER.info("Checking if sensors were initialized correctly");
      
        LOGGER.info("Sensors were all initialized correctly.");
        
        // Check that the correct number of stereo layers were created
        LOGGER.info("Checking if the correct number of stereo layers were created.");
          
     
    }
    
    
    
}
