package org.hps.recon.filtering;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

/**
 *
 * @author Norman Graf
 */
public class MollerSkimDriver extends EventReconFilter {

    private String mollerCollectionName = "UnconstrainedMollerCandidates";

    /**
     * sets the Moller candidate collection to use.
     *
     * @param val
     */
    public void setMollerCollectionName(String val) {
        this.mollerCollectionName = val;
    }

    public void process(EventHeader event) {
        incrementEventProcessed();
        List<ReconstructedParticle> mollers = event.get(ReconstructedParticle.class, mollerCollectionName);
        if (mollers.size() == 0) {
            skipEvent();
        }
        incrementEventPassed();
    }
}
