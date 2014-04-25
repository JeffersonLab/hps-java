package org.hps.conditions.ecal;

import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.DefaultTestSetup;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.geometry.Detector;

/**
 * This test loads ECal conditions data onto the detector 
 * and checks some of the results for basic validity.  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsLoaderTest extends TestCase {
        
    /** Expected number of crystals. */
    private static final int CRYSTAL_COUNT_ANSWER = 442;
    
    /** Expected number of bad channels. */
    private static final int BAD_CHANNELS_ANSWER = 44;
    
    /** Valid minimum and maximum values for DAQ setup parameters. */
    private static final int MIN_CRATE_ANSWER = 1;    
    private static final int MAX_CRATE_ANSWER = 2;
    private static final int MIN_SLOT_ANSWER = 3;
    private static final int MAX_SLOT_ANSWER = 19;
    private static final int MIN_CHANNEL_ANSWER = 0;
    private static final int MAX_CHANNEL_ANSWER = 19;
    
    // The total number of crystals that should be processed.
    private static final int CRYSTAL_COUNT = 442;
        
    public void setUp() {
        new DefaultTestSetup().configure().setup();
    }
                                           
    /**
     * Load SVT conditions data onto the detector and perform basic checks afterwards.
     */
    public void testLoad() {
        
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        
        // Get the detector.
        Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        
        // Get conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();

        // Load conditions onto detector.
        EcalConditionsLoader loader = new EcalConditionsLoader();
        loader.load(detector.getSubdetector("Ecal"), conditions);
        
        // Get crystals from detector.
        List<EcalCrystal> crystals = detector.getDetectorElement().findDescendants(EcalCrystal.class);
        
        // Check number of crystals.
        assertEquals("Wrong number of crystals.", CRYSTAL_COUNT_ANSWER, crystals.size());

        // Counter for bad channels.
        int badChannelCount = 0;
        
        // Loop over crystals.
        int ncrystals = 0;
        for (EcalCrystal crystal : crystals) {
            
            // Get DAQ information.
            int crate = crystal.getCrate();
            int slot = crystal.getSlot();
            int channel = crystal.getChannel();
            
            // Check basic validity of DAQ setup information.
            assertTrue("Crate number is out of range.", crate >= MIN_CRATE_ANSWER && crate <= MAX_CRATE_ANSWER);            
            assertTrue("Slot number is out of range.", slot >= MIN_SLOT_ANSWER && slot <= MAX_SLOT_ANSWER);            
            assertTrue("Channel number is out of range.", MIN_CHANNEL_ANSWER >=0 && channel <= MAX_CHANNEL_ANSWER);

            // Get time dependent conditions.
            double pedestal = crystal.getPedestal();
            double noise = crystal.getNoise();
            double gain = crystal.getGain();
            boolean badChannel = crystal.isBadChannel();

            // Check basic validity of conditions.  They should all be non-zero.
            assertTrue("Pedestal value is zero.", pedestal != 0);
            assertTrue("Noise value is zero.", noise != 0);
            assertTrue("Gain value is zero.", gain != 0);
                        
            // Increment bad channel count.
            if (badChannel)
                ++badChannelCount;
            
            ++ncrystals;
        }
        
        assertEquals("The number of crystals was wrong.", CRYSTAL_COUNT, ncrystals);
        
        // Check total number of bad channels.
        assertEquals("Wrong number of bad channels.", BAD_CHANNELS_ANSWER, badChannelCount);

        System.out.println("Successfully loaded conditions onto " + ncrystals + " ECal crystals!");
    }
}
