package org.hps.conditions.beam;

/**
 * This class is a simple data holder for the integrated beam current condition.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BeamCurrent {
    
    double beamCurrent = Double.NaN;
    
    /**
     * Class constructor.
     * @param beamCurrent The integrated beam current value.
     */
    BeamCurrent(double beamCurrent) {
        this.beamCurrent = beamCurrent;
    }
    
    /**
     * Get the integrated beam current.
     * @return The integrated beam current.
     */
    double getIntegratedBeamCurrent() {
        return beamCurrent;
    }
}
