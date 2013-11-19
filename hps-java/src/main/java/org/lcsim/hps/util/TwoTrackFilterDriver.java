package org.lcsim.hps.util;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;

/**
 *
 * @author phansson
 */
public class TwoTrackFilterDriver extends LCIOFilterDriver {

    private String trackCollectionName = "MatchedTracks";

    public TwoTrackFilterDriver() {
    }

    public void setTrackCollectionNamePath(String trackCollection) {
        this.trackCollectionName = trackCollection;
    }

    @Override
    boolean eventFilter(EventHeader event) {
        boolean pass = false;

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            throw new RuntimeException("Error, event doesn't have the track collection");
        }

        if (this.debug) {
            System.out.printf("%s: %d tracks in event %d\n", this.getClass().getSimpleName(), event.get(Track.class, trackCollectionName).size(), event.getEventNumber());
        }

        if (event.get(Track.class, trackCollectionName).size() > 1) {
            if (this.debug) {
                System.out.printf("%s: write event %d \n", this.getClass().getSimpleName(), event.getEventNumber());
            }
            pass = true;
        }


        return pass;
    }
}
