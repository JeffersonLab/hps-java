package org.hps.conditions.beam;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;

/**
 * This class is a simple data holder for the integrated beam current condition.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class BeamCurrent extends AbstractConditionsObject {

    public static class BeamCurrentCollection extends AbstractConditionsObjectCollection<BeamCurrent> {
    }

    /**
     * Get the integrated beam current.
     * @return The integrated beam current.
     */
    double getIntegratedBeamCurrent() {
        return getFieldValue("beam_current");
    }
}
