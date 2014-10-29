package org.hps.conditions.svt;

import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.DevReadOnlyConfiguration;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;

/**
 * This test loads {@link SvtConditions} data onto the detector and then checks that all
 * channels of each sensor have non-zero data values for applicable parameters.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
// TODO: Update this test with more meaningful test.
public class SvtDetectorSetupTest extends TestCase {
	
	
	//--- Constants ---//
	//-----------------//
	// TODO: Move all of these constants to their own class
	
	// Total number of SVT sensors
	public static final int TOTAL_NUMBER_OF_SENSORS = 36;	
	// Max FEB ID 
	public static final int MAX_FEB_ID = 9; 
	// Max FEB Hybrid ID
	public static final int MAX_FEB_HYBRID_ID = 3;
	// Max channel number
	public static final int MAX_CHANNEL_NUMBER = 639;
	// SVT Subdetector name
	public static final String SVT_SUBDETECTOR_NAME = "Tracker";
	
	public void setUp(){
	    new DevReadOnlyConfiguration().setup().load("HPS-Proposal2014-v7-2pt2", 0);
	}

    /**
     * Load SVT conditions data onto the detector and perform basic checks afterwards.
     */
    public void test() {
    	
		DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
    	
        // Get the detector.
        Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();

        // Get all SVT conditions.
        SvtConditions conditions = conditionsManager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();

        // Load the SVT conditions onto detector.
        SvtDetectorSetup loader = new SvtDetectorSetup();
        loader.load(detector.getSubdetector(SVT_SUBDETECTOR_NAME), conditions);

        // Check sensor data.
        List<HpsSiSensor> sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        
        // Check for correct number of sensors processed.
		this.printDebug("Total number of sensors found: " + sensors.size());
		assertTrue(sensors.size() == TOTAL_NUMBER_OF_SENSORS);
        
        // Loop over sensors.
        int totalSensors = 0; 
        for (HpsSiSensor sensor : sensors) {

        	int nChannels = sensor.getNumberOfChannels();
        	assertTrue("The number of channels this sensor has is invalid", nChannels <= MAX_CHANNEL_NUMBER);
        
        	this.printDebug(sensor.toString());
        	
        	// Check that the FEB ID as within the appropriate range
            int febID = sensor.getFebID();
            assertTrue("FEB ID is invalid.  The FEB ID should be less than " + MAX_FEB_ID,
            		febID <= MAX_FEB_ID);
            
            int febHybridID = sensor.getFebHybridID();
            assertTrue("FEB Hybrid ID is invalid.  The FEB Hybrid ID should be less than " + MAX_FEB_HYBRID_ID,
            		febHybridID <= MAX_FEB_HYBRID_ID);
            
            for (int channel = 0; channel < nChannels; channel++) {
        
            	//
                // Check that channel conditions values are not zero
            	//
            	for(int sampleN = 0; sampleN < 6; sampleN++){
            		assertTrue("Pedestal sample " + sampleN + " is zero.",
            				sensor.getPedestal(channel, sampleN) != 0);
            		assertTrue("Noise sample " + sampleN + " is zero.",
            				sensor.getNoise(channel, sampleN) != 0);
            	}
                assertTrue("Gain is zero.", sensor.getGain(channel) != 0);
                assertTrue("Shape fit parameters points to null.",
                		sensor.getShapeFitParameters(channel) != null);
                
            }
        }
        
        System.out.println("Successfully loaded conditions data onto " + totalSensors + " SVT sensors!"); 
    }

    private void printDebug(String debugMessage){
		System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
	}
}
