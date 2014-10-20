package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;
import org.lcsim.geometry.Detector;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.hps.recon.tracking.SvtSensorSetup}.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
// TODO: Add more test
public class SvtSensorSetupTest extends TestCase {

	//-----------------//
	//--- Constants ---//
	//-----------------//
	public static final String DETECTOR_NAME = "HPS-Proposal2014-v7-2pt2"; 
	public static final String SVT_SUBDETECTOR_NAME = "Tracker";
	public static final int NUMBER_OF_READOUT_STRIPS = 639;
	public static final int NUMBER_OF_SENSE_STRIPS = 1277;
	
	Detector detector = null; 
	
	public void setUp() {
		
		LCSimConditionsManagerImplementation.register();
        int runNumber = 0;
        try {
            ConditionsManager.defaultInstance().setDetector(DETECTOR_NAME, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        detector = ConditionsManager.defaultInstance().getCachedConditions(Detector.class, "compact.xml").getCachedData();	
	}
	
	public void testSvtSensorSetup(){

		SvtSensorSetup setup = new SvtSensorSetup(); 
		setup.setDebug(true);
		
		assertTrue("Detectors are mismatched!", DETECTOR_NAME.equals(detector.getName()));
		System.out.println(this.getClass().getSimpleName() + ": Getting sensors from " + detector.getDetectorName());
	 
		// Get the collection of all SiSensors from the SVT 
		List<SiSensor> sensors 
        	= detector.getSubdetector(SVT_SUBDETECTOR_NAME).
        			getDetectorElement().findDescendants(SiSensor.class);
		System.out.println(this.getClass().getSimpleName() + ": Number of sensors: " + sensors.size());
		
		for(SiSensor sensor : sensors){
			setup.setupSensor(sensor);
			
			assertTrue("Wrong number of readout electrodes found.",
					sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells() == NUMBER_OF_READOUT_STRIPS);	
			assertTrue("Wrong number of sense electrodes found.",
					sensor.getSenseElectrodes(ChargeCarrier.HOLE).getNCells() == NUMBER_OF_SENSE_STRIPS);
			
		}
	}
}
