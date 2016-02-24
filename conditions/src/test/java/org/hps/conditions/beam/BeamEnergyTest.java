package org.hps.conditions.beam;

import junit.framework.TestCase;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * Basic test of {@link BeamEnergy} condition.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class BeamEnergyTest extends TestCase {
    
    public void testBeamEnergy() throws Exception {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setDetector("HPS-dummy-detector", 7450);        
        BeamEnergyCollection beamEnergyCollection = 
                manager.getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        double beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        System.out.println("read beam energy " + beamEnergy);
        assertEquals("Beam energy has wrong value.", beamEnergy, 2.3);
    }   
}
