package org.hps.conditions.beam;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;

/**
 * This class is a simple data holder for the integrated beam current condition.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class BeamCurrent extends AbstractConditionsObject {
    
    public static class BeamCurrentCollection extends ConditionsObjectCollection<BeamCurrent> {
    }
            
    /**
     * Get the integrated beam current.
     * @return The integrated beam current.
     */
    double getIntegratedBeamCurrent() {
        return getFieldValue("beam_current");
    }
}
