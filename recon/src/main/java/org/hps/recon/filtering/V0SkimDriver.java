package org.hps.recon.filtering;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

/**
 *
 * @author Norman Graf
 */
public class V0SkimDriver extends EventReconFilter {

    private String v0CollectionName = "UnconstrainedV0Candidates";

    /**
     * sets the V0 candidate collection to use.
     *
     * @param val
     */
    public void setV0CollectionName(String val) {
        this.v0CollectionName = val;
    }

    public void process(EventHeader event) {
        incrementEventProcessed();
        List<ReconstructedParticle> vertices = event.get(ReconstructedParticle.class, v0CollectionName);
        if (vertices.size() == 0) {
            skipEvent();
        }
        incrementEventPassed();
    }
}
