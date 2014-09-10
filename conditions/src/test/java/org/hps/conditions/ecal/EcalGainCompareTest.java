package org.hps.conditions.ecal;

import static org.hps.conditions.deprecated.EcalConditions.makePhysicalID;
import static org.hps.conditions.deprecated.EcalConditions.physicalToGain;
import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.config.TestRunReadOnlyConfiguration;
import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;

/**
 * This is a test to compare the ECAL channel gain values between
 * the old text-based conditions and the new database system, in order
 * to make sure they are exactly the same.
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalGainCompareTest extends TestCase {
    
    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {
        new TestRunReadOnlyConfiguration(true);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }
    
    public void testEcalGainCompareTest() {
        
        // Load the old text-based conditions for the ECAL in order to compare against database values.
        CalibrationDriver calibrationDriver = new CalibrationDriver();
        calibrationDriver.detectorChanged(conditionsManager.getDetectorObject());
        
        // Fetch conditions from the database.
        EcalGainCollection gains = conditionsManager.getConditionsData(EcalGainCollection.class, TableConstants.ECAL_GAINS);
        EcalChannelCollection channels = conditionsManager.getConditionsData(EcalChannelCollection.class, TableConstants.ECAL_CHANNELS);
        
        // Loop over the gain values and compare them with each other.
        int nCompared = 0;
        for (EcalGain gain : gains) {            
            EcalChannel channel = channels.findChannel(gain.getChannelId());            
            long physicalID = makePhysicalID(channel.getX(), channel.getY());
            double oldGainValue = physicalToGain(physicalID);            
            assertEquals("The new and old gain values are different.", gain.getGain(), oldGainValue);
            ++nCompared;
        }
        System.out.println("Compared " + nCompared + " ECAL gain values.");
        assertEquals("Wrong number of gain values compared.", 442, nCompared);
    }    
}
