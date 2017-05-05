package org.hps.users.mgraham;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;

/**
 * Split the HelicalTrackHit collection into hits that 
 * are on a track and hits that are not
 */
public class SplitHitsOnTracks extends Driver {

    private String trackCollectionName = "MatchedTracks";
    private String inputHitCollectionName = "RotatedHelicalTrackHits";
    private String onTrackCollectionName = "OnTrackHits";
    private String offTrackCollectionName = "OffTrackHits";

    public void setTrackCollectionName(String name) {
        this.trackCollectionName = name;
    }

    public void setInputHitCollectionName(String name) {
        this.inputHitCollectionName = name;
    }

    public void setOnTrackHitCollectionName(String name) {
        this.onTrackCollectionName = name;
    }

    public void setOffTrackHitCollectionName(String name) {
        this.offTrackCollectionName = name;
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(TrackerHit.class, inputHitCollectionName))
            return;
        if (!event.hasCollection(Track.class, trackCollectionName))
            return;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        List<HelicalTrackHit> onTrack = new ArrayList<HelicalTrackHit>();
        for (Track trk : tracks) {
            SeedTrack st = (SeedTrack) trk;
            List<HelicalTrackHit> theseHits = st.getSeedCandidate().getHits();
            for (HelicalTrackHit hth : theseHits)
                if (!onTrack.contains(hth))
                    onTrack.add(hth);
        }
        List<HelicalTrackHit> offTrack = new ArrayList<HelicalTrackHit>();
        List<HelicalTrackHit> allHits = event.get(HelicalTrackHit.class, inputHitCollectionName);
        for (HelicalTrackHit hth : allHits)
            if (!onTrack.contains(hth))
                offTrack.add(hth);

        event.put(onTrackCollectionName, onTrack, HelicalTrackHit.class, 0);
        event.put(offTrackCollectionName, offTrack, HelicalTrackHit.class, 0);
    }
}
