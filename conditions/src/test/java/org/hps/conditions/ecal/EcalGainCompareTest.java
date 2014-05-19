package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.DetectorSetup;
import org.hps.conditions.TableConstants;
import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import static org.hps.conditions.deprecated.EcalConditions.makePhysicalID;
import static org.hps.conditions.deprecated.EcalConditions.physicalToGain;

/**
 * This is a test to compare the ECAL channel gain values between
 * the old text-based conditions and the new database system, in order
 * to make sure they are exactly the same.
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalGainCompareTest extends TestCase {
    
    DatabaseConditionsManager conditionsManager;
    
    static final String detectorName = "HPS-TestRun-v8-5";
    static final int runNumber = 1351;
    
    public void setUp() {
        conditionsManager = new DetectorSetup(detectorName, 0).configure().setup();
    }
    
    public void testEcalGainCompareTest() {
        
        CalibrationDriver calibrationDriver = new CalibrationDriver();
        calibrationDriver.detectorChanged(conditionsManager.getDetectorObject());
        
        EcalGainCollection gains = conditionsManager.getConditionsData(EcalGainCollection.class, TableConstants.ECAL_GAINS);
        EcalChannelCollection channels = conditionsManager.getConditionsData(EcalChannelCollection.class, TableConstants.ECAL_CHANNELS);
        for (EcalGain gain : gains) {
            
            EcalChannel channel = channels.findChannel(gain.getChannelId());
            
            long physicalID = makePhysicalID(channel.getX(), channel.getY());
            double oldGainValue = physicalToGain(physicalID);
            
            assertEquals("The new and old gain values are different.", gain.getGain(), oldGainValue);
        }
    }    
}
