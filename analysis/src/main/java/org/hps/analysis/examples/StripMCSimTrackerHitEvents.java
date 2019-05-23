package org.hps.analysis.examples;

import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author ngraf
 */
public class StripMCSimTrackerHitEvents extends Driver {

    private int _numberOfEventsWritten = 0;

    protected void process(EventHeader event) {
        boolean skipEvent = true;

        int nSimTrackerHits = event.get(SimTrackerHit.class, "TrackerHits").size();
        int nSimTrackerHitsEcal = event.get(SimTrackerHit.class, "TrackerHitsECal").size();

        if (nSimTrackerHits == 12 && nSimTrackerHitsEcal == 1) {
            skipEvent = false;
        }

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsWritten + " events");
    }
}
