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
    
    private static final double[] BEAM_ENERGIES = {1.920, 1.056, 2.306};
    private static final int[][] RUNS = {{3000, 3500, 3999}, {4000, 6000, 6999}, {7000, 7500, 9999}};
    private static final String DETECTOR = "HPS-dummy-detector";
    
    public void testBeamEnergy() throws Exception {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();        
        for (int i = 0; i < BEAM_ENERGIES.length; i++) {
            double expectedBeamEnergy = BEAM_ENERGIES[i];
            for (int j = 0; j < 3; j++) {
                int run = RUNS[i][j];
                manager.setDetector(DETECTOR, run);
                BeamEnergyCollection beamEnergyCollection = 
                        manager.getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
                double beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
                System.out.println("read beam energy " + beamEnergy + " for run " + run);
                assertEquals("Beam energy has wrong value.", expectedBeamEnergy, beamEnergy);
            }
        }
    }   
}
