package org.lcsim.hps.conditions.svt;

import java.util.List;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.conditions.ConnectionManager;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;

/**
 * This test loads conditions data onto the detector and then checks that 
 * all channels of each sensor have non-zero data values for these parameters. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsLoaderTest extends TestCase {
    
    /** An example detector from hps-detectors. */
    private static final String detectorName = "HPS-conditions-test";
    
    /** The run number of the conditions set in the database. */
    private static final int runNumber = 1351;
    
    /**
     * The number of bad channels that should be returned for the run.
     * One of these is a duplicate so the row count is actually 442 in the database. 
     */
    private static final int BAD_CHANNELS_ANSWER = 441;
    
    /** The number of channels where pulse information is all zeroes. */
    private static final int PULSE_NOT_SET_ANSWER = 4;
            
    /**
     * Load SVT conditions data onto the detector and perform basic checks afterwards.
     */
    public void test() {
        
        // Setup the conditions manager.        
        ConditionsManager.setDefaultConditionsManager(new LCSimConditionsManagerImplementation());
        ConditionsManager manager = ConditionsManager.defaultInstance();
        try {
            manager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        // Get the detector.
        Detector detector = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        
        // Get conditions.
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();

        // Load conditions onto detector.
        SvtConditionsLoader loader = new SvtConditionsLoader();
        loader.load(detector, conditions);
        
        // Check sensor data.
        List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
        final int nchannels = sensors.get(0).getNumberOfChannels();
        int badChannels = 0;
        int pulseNotSet = 0;
        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {
            // Loop over channels.
            for (int channel=0; channel<nchannels; channel++) {
                
                // Check that hardware information seems reasonable.
                int hybrid = sensor.getHybridNumber();
                assertTrue("Invalid hybrid value.", hybrid >= 0 && hybrid <= 2);
                int fpga = sensor.getFpgaNumber();
                assertTrue("Invalid FPGA value.", fpga >= 0 && fpga <= 6);
                
                // Check that conditions values are not zero:
                assertTrue("Gain is zero.", sensor.getGain(channel) != 0);
                assertTrue("Noise is zero.", sensor.getNoise(channel) != 0);
                assertTrue("Pedestal is zero.", sensor.getPedestal(channel) != 0);
                assertTrue("Time offset is zero.", sensor.getTimeOffset(channel) != 0);
                assertTrue("PulseParameters points to null.", sensor.getPulseParameters(channel) != null);               
                double[] pulse = sensor.getPulseParameters(channel);
                
                // There are four channels in the database where these are all zeroes.
                if (pulse[0] != 0) {                
                    // Check pulse parameters:
                    assertTrue("amplitude is zero.", pulse[0] != 0);
                    assertTrue("t0 is zero.", pulse[1] != 0);
                    assertTrue("tp is zero.", pulse[2] != 0);
                    assertTrue("chisq is zero.", pulse[3] != 0);
                } else {
                    pulseNotSet += 1;
                }
                
                // Add to bad channel count.
                if (sensor.isBadChannel(channel)) {
                    ++badChannels;
                }
            }

            // Check that time shift is set for the sensor.  When unset, it's value will be NaN.
            assertTrue("Time shift was not set.", sensor.getTimeShift() != Double.NaN);
        }
        
        // Check that there were at least some bad channels.
        assertTrue("Number of bad channels was zero.", badChannels != 0);
        
        // Now check the exact number of bad channels, which should be the QA set plus those for run 1351.
        assertEquals("Wrong number of dead channels found.", BAD_CHANNELS_ANSWER, badChannels);

        // There should be exactly 4 channels where the pulse parameters are all zeroes.
        assertEquals("The number of channels for which pulse was not set is wrong.", PULSE_NOT_SET_ANSWER, pulseNotSet);
        
        // Cleanup the database connection.
        ConnectionManager.getConnectionManager().disconnect();
    }
}
