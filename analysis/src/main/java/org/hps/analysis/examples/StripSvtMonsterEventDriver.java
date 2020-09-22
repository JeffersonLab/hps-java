package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripSvtMonsterEventDriver extends Driver {

    private int _maxNumberOfHits = 250;
    private int _numberOfEventsSelected = 0;

    @Override
    protected void process(EventHeader event) {
        boolean skipEvent = true;

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        if (rawHits.size() > _maxNumberOfHits) {
            skipEvent = false;
        }

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    @Override
    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }

    public void setMaxNumberOfHits(int i) {
        _maxNumberOfHits = i;
    }

}
