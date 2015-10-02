package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * A Driver which refits tracks using GBL. Does not require GBL collections to
 * be present in the event.
 */
public class GBLRefitterDriver extends Driver {

    private String inputCollectionName = "MatchedTracks";
    private String outputCollectionName = "GBLTracks";

    private double bfield;
    private final MultipleScattering _scattering = new MultipleScattering(new MaterialSupervisor());

    public void setInputCollectionName(String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only
    }

    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(Track.class, inputCollectionName)) {
            return;
        }
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<Track> refittedTracks = new ArrayList<Track>();

        Map<Track, Track> inputToRefitted = new HashMap<Track, Track>();
        for (Track track : tracks) {
            HelicalTrackFit helix = TrackUtils.getHTF(track);
            FittedGblTrajectory fit = GblUtils.doGBLFit(helix, TrackUtils.getStripHits(track, hitToStrips, hitToRotated), _scattering, bfield, 0);

            Track newTrack = MakeGblTracks.makeCorrectedTrack(fit, helix, track.getTrackerHits(), track.getType(), bfield);
            refittedTracks.add(newTrack);
            inputToRefitted.put(track, newTrack);
        }

        Map<Set<TrackerHit>, List<Track>> hitSetToTrackList = new HashMap<Set<TrackerHit>, List<Track>>();

        for (Track track : tracks) {
            Set<TrackerHit> trackHth = new HashSet<TrackerHit>(track.getTrackerHits());
            for (Track otherTrack : tracks) {
                Set<TrackerHit> allHth = new HashSet<TrackerHit>(otherTrack.getTrackerHits());
                allHth.addAll(trackHth);
                List<TrackerHit> hthList = new ArrayList<TrackerHit>(allHth);
                if (hthList.size() == trackHth.size()) {
                    continue;
                }

                boolean[] hasHit = new boolean[6];
                boolean isGood = true;

                for (TrackerHit hit : hthList) {
                    int layer = (TrackUtils.getLayer(hit) - 1) / 2;
                    if (hasHit[layer]) {
                        isGood = false;
                        break;
                    }
                    hasHit[layer] = true;
                }
                if (isGood) {
                    HelicalTrackFit helix = TrackUtils.getHTF(track);
                    Set<TrackerHit> allStripHits = new HashSet<TrackerHit>(TrackUtils.getStripHits(track, hitToStrips, hitToRotated));
                    allStripHits.addAll(TrackUtils.getStripHits(otherTrack, hitToStrips, hitToRotated));

                    FittedGblTrajectory fit = GblUtils.doGBLFit(helix, new ArrayList<TrackerHit>(allStripHits), _scattering, bfield, 0);
                    Track newTrack = MakeGblTracks.makeCorrectedTrack(fit, helix, new ArrayList<TrackerHit>(allHth), 0, bfield);
                    System.out.format("%f %f %f\n", fit.get_chi2(), inputToRefitted.get(track).getChi2(), inputToRefitted.get(otherTrack).getChi2());
                }
            }
        }
        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, refittedTracks, Track.class, flag);
    }
}
