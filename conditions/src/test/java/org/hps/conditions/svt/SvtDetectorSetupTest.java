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
 * @version $Id$
 */
// TODO: Update this test with more meaningful test.
public class SvtDetectorSetupTest extends TestCase {
	
	
	//--- Constants ---//
	//-----------------//
	
	// Total number of SVT sensors
	public static final int TOTAL_NUMBER_OF_SENSORS = 36;	
	   
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
        loader.load(detector, conditions);

        // Check sensor data.
        List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
        
        int nChannels = sensors.get(0).getNumberOfChannels();
        this.printDebug("Total number of channels per sensor: " + nChannels);
        
        // Loop over sensors.
        int totalSensors = 0; 
        for (HpsSiSensor sensor : sensors) {

        	this.printDebug(sensor.toString());
        	
        	totalSensors++; 
        	
        	// Check that hardware information seems reasonable.
            int febHybridID = sensor.getFebHybridID();
            int febID = sensor.getFebID();

            for (int channel = 0; channel < nChannels; channel++) {
        
            	//
                // Check that channel conditions values are not zero
            	//
            	for(int sampleN = 0; sampleN < 6; sampleN++){
            		assertTrue("Pedestal is zero.", sensor.getPedestal(channel, sampleN) != 0);
            		assertTrue("Noise is zero.", sensor.getNoise(channel, sampleN) != 0);
            	}
                assertTrue("Gain is zero.", sensor.getGain(channel) != 0);
                assertTrue("PulseParameters points to null.", sensor.getShapeFitParameters(channel) != null);
                
            }
        }
        
        // Check for correct number of sensors processed.
		this.printDebug("Total number of sensors found: " + totalSensors);
		//assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);
        
        System.out.println("Successfully loaded conditions data onto " + totalSensors + " SVT sensors!"); 
    }

    private void printDebug(String debugMessage){
		System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
	}
}
