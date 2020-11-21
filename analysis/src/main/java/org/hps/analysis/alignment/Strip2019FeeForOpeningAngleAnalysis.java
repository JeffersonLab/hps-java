package org.hps.analysis.alignment;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Simple Driver to skim off events for the SVT Opening Angle analysis
 *
 * @author Norman A. Graf
 */
public class Strip2019FeeForOpeningAngleAnalysis extends Driver {

    int _numberOfEventsSelected;
    private int _maxSvtRawTrackerHits = 250;
    boolean matchFullTracks = true;
    String fullTrackCollectionName = "s234_c5_e167";
    private String l0to3CollectionName = "L0to3Tracks";
    private String l4to6CollectionName = "L4to6Tracks";
    private AIDA aida = AIDA.defaultInstance();

    protected void process(EventHeader event) {
        boolean skipEvent = false;
        //filter out monster events
        if (event.get(RawTrackerHit.class, "SVTRawTrackerHits").size() < _maxSvtRawTrackerHits) {
            if (!event.hasCollection(Track.class, l0to3CollectionName)) {
                skipEvent = true;
            }
            if (!event.hasCollection(Track.class, l4to6CollectionName)) {
                skipEvent = true;
            }
            if (matchFullTracks) {
                if (!event.hasCollection(Track.class, fullTrackCollectionName)) {
                    skipEvent = true;
                }
            }
            if (event.get(Track.class, l0to3CollectionName).isEmpty()) {
                skipEvent = true;
            }
            if (event.get(Track.class, l4to6CollectionName).isEmpty()) {
                skipEvent = true;
            }
            if (matchFullTracks) {
                if (event.get(Track.class, fullTrackCollectionName).isEmpty()) {
                    skipEvent = true;
                }
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    public void endOfData() {
        System.out.println("selected " + _numberOfEventsSelected + " events for the opening angle alignment analysis");
    }
}
