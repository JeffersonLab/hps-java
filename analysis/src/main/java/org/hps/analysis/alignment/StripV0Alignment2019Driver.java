package org.hps.analysis.alignment;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 */
public class StripV0Alignment2019Driver extends Driver {

    int _numberOfEventsSelected;

    protected void process(EventHeader event) {
        boolean skipEvent = true;

        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");

        if (V0List.size() != 0) {
            skipEvent = false;
        }

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }

    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }
}
