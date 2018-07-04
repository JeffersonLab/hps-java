package org.hps.readout.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseSimTrackerHit;
import org.lcsim.lcio.LCIOConstants;

public class InactiveSiHitReadout extends TriggerableDriver {
    
    private static String COLLNAME = "TrackerHits_Inactive";
    private List<SimTrackerHit> hits = new ArrayList<SimTrackerHit>();    
    static final int FLAG = (1 << LCIOConstants.THBIT_MOMENTUM);
    
    public void process(EventHeader event) {
        if (event.hasCollection(SimTrackerHit.class, COLLNAME)) {
            List<SimTrackerHit> newHits = event.get(SimTrackerHit.class, COLLNAME);
            for (SimTrackerHit hit : newHits) {
                double hitTime = hit.getTime();
                hitTime += ClockSingleton.getTime();
                ((BaseSimTrackerHit)hit).setTime(hitTime);
            }
            hits.addAll(hits);
        }
        checkTrigger(event);
    }

    @Override
    protected void processTrigger(EventHeader event) {
        event.put(COLLNAME, hits, SimTrackerHit.class, FLAG, "TrackerHits");
        hits.clear();
    }

    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRACKER;
    }         
}
