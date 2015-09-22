package org.hps.users.meeg;

import java.util.List;
import org.hps.recon.filtering.EventReconFilter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class V0Filter extends EventReconFilter {

    private String v0CollectionName = "UnconstrainedV0Candidates";
    private double massMin = 0.025;
    private double massMax = 0.030;
    private double minPTot = 0.7 * 1.05;

    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();
        if (!event.hasCollection(ReconstructedParticle.class, v0CollectionName)) {
            skipEvent();
        }
        List<ReconstructedParticle> v0List = event.get(ReconstructedParticle.class, v0CollectionName);
        if (v0List.isEmpty()) {
            skipEvent();
        }
        boolean hasGoodVertex = false;
        for (ReconstructedParticle rp : v0List) {
            double mass = rp.getMass();
            if (mass > massMin && mass < massMax && rp.getMomentum().magnitude() > minPTot) {
                hasGoodVertex = true;
                break;
            }
        }
        if (!hasGoodVertex) {
            skipEvent();
        }
        incrementEventPassed();
    }
}
