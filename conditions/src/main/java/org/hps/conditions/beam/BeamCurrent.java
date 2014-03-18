package org.hps.conditions.beam;

import org.hps.conditions.AbstractConditionsObject;

/**
 * This class is a simple data holder for the integrated beam current condition.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BeamCurrent extends AbstractConditionsObject {
            
    /**
     * Get the integrated beam current.
     * @return The integrated beam current.
     */
    double getIntegratedBeamCurrent() {
        return getFieldValue("beam_current");
    }
}
