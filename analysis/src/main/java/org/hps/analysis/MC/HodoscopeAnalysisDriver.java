package org.hps.analysis.MC;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class HodoscopeAnalysisDriver extends Driver {
    private int totalEvents = 0;
    private int hodoscopeEvents = 0;
    private int twoClusterEvents = 0;
    
    @Override
    public void process(EventHeader event) {
        // Verify that the event is a "good event." Good events need
        // to have at least one positive and one negative track, and
        // the positive track needs to have a matched cluster.
        
    }
}