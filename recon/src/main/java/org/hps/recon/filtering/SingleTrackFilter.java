package org.hps.recon.filtering;

import java.util.List;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;

/**
 * 
 * @author meeg
 */
public class SingleTrackFilter extends EventReconFilter {

    private String helicalTrackHitCollectionName = "HelicalTrackHits";

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (!event.hasCollection(TrackerHit.class, helicalTrackHitCollectionName)) {
            return;
        }

        int[] topHits = {0, 0, 0, 0, 0, 0};
        int[] botHits = {0, 0, 0, 0, 0, 0};
        List<TrackerHit> hth = event.get(TrackerHit.class, helicalTrackHitCollectionName);
        for (TrackerHit hit : hth) {
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            int module = layer / 2 + 1;
            if (hit.getPosition()[1] > 0) {
                topHits[module - 1]++;
            } else {
                botHits[module - 1]++;
            }
        }

        boolean topOne = true, topNone = true;
        boolean botOne = true, botNone = true;

        for (int i = 0; i < 6; i++) {
            if (topHits[i] != 0) {
                topNone = false;
            }
            if (topHits[i] != 1) {
                topOne = false;
            }
            if (botHits[i] != 0) {
                botNone = false;
            }
            if (botHits[i] != 1) {
                botOne = false;
            }
        }

        if (!((topOne && botNone) || (botOne && topNone))) {
            skipEvent();
        }
        incrementEventPassed();
    }
}
