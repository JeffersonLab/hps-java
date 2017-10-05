package org.hps.recon.tracking;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;

/**
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu> $Id:
 *         HoleCreationDriver.java, v1 05/30/2017$ Removes hits from selected
 *         layers of tracks. Writes new RawTrackerHits, HelicalTrackHits,
 *         RotatedHelicalTrackHits collections without these hits.
 */

public class HoleCreationDriver extends Driver {
    IDDecoder dec;
    private String inputTrackCollectionName = "MatchedTracks";
    private String inputTrackHitCollectionName = "TrackerHits";
    // private String outputTrackCollectionName = "TracksWithHoles";

    private String outputRawTrackHitCollectionName = "RawTrackerHitsWithHoles";
    private String inputRawTrackHitCollectionName = "SVTRawTrackerHits";
    private String inputHelicalTrackHitCollectionName = "HelicalTrackHits";
    private String outputHelicalTrackHitCollectionName = "HelicalTrackHitsWithHoles";
    private String inputRHelicalTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String outputRHelicalTrackHitCollectionName = "RotatedHelicalTrackHitsWithHoles";
    RelationalTable hitToRotated;

    private int randomHoles;

    private boolean[] holePattern;
    List<Track> inputTrackCollection;
    List<TrackerHit> inputTrackHitCollection;
    // List<Track> outputTrackCollection;
    List<RawTrackerHit> inputRawTrackHitCollection;
    List<HelicalTrackHit> inputHelicalTrackHitCollection;
    List<HelicalTrackHit> inputRHelicalTrackHitCollection;
    Random rn;

    /**
     * Default constructor. Pattern defaults to no holes. Random holes defaults
     * to none.
     */
    public HoleCreationDriver() {
        rn = new Random();
        boolean[] temp = { false, false, false, false, false, false, false, false, false, false, false, false };
        setHolePattern(temp);
        setRandomHoles(0);
    }

    /**
     * 
     * @param pattern
     *            : specifies layers in which to create holes. One boolean per
     *            layer, true=hole
     * @param numRandom
     *            : number of extra holes to put in random layers, on top of
     *            pattern
     */
    public HoleCreationDriver(boolean[] pattern, int num) {
        new HoleCreationDriver();
        setHolePattern(pattern);
        setRandomHoles(num);
    }

    /**
     * 
     * @param numRandom
     *            : number of extra holes to put in random layers, on top of
     *            pattern
     */
    public void setRandomHoles(int num) {
        if (num < 0)
            num = 0;
        randomHoles = num;
    }

    public void setInputTrackCollection(String name) {
        inputTrackCollectionName = name;
    }

    public void setInputTrackHitCollectionName(String name) {
        inputTrackHitCollectionName = name;
    }

    public void setInputHelicalTrackHitCollectionName(String name) {
        inputHelicalTrackHitCollectionName = name;
    }

    public void setInputRotatedHelicalTrackHitCollectionName(String name) {
        inputRHelicalTrackHitCollectionName = name;
    }

    public void setInputRawTrackHitCollectionName(String name) {
        inputRawTrackHitCollectionName = name;
    }

    /*
     * public void setOutputTrackCollection(String name) {
     * outputTrackCollectionName = name; }
     */

    public void setOutputRawTrackHitCollectionName(String name) {
        outputRawTrackHitCollectionName = name;
    }

    public void setOutputRotatedHelicalTrackHitCollectionName(String name) {
        outputRHelicalTrackHitCollectionName = name;
    }

    public void setOutputHelicalTrackHitCollectionName(String name) {
        outputHelicalTrackHitCollectionName = name;
    }

    /**
     * 
     * @param pattern
     *            : specifies layers in which to create holes. One boolean per
     *            layer, true=hole
     */
    public void setHolePattern(boolean[] holes) {
        if (holes.length == 12)
            holePattern = holes;
    }

    /**
     * Create holes in given track; remove the necessary hits from the
     * collections
     * 
     * @param track
     */
    private void createHoles(Track trk) {

        for (int i = 0; i < randomHoles; i++) {
            holePattern[rn.nextInt(12)] = true;
        }

        List<TrackerHit> hitsOnTrack = trk.getTrackerHits();

        Iterator<TrackerHit> iter = hitsOnTrack.iterator();
        while (iter.hasNext()) {
            TrackerHit hit = iter.next();
            TrackerHit hhit = (TrackerHit) hitToRotated.from(hit);

            // HelicalTrackHit hhit2 =
            // TrackUtils.makeHelicalTrackHitFromTrackerHit(hhit);;

            List<RawTrackerHit> rawHits = (List<RawTrackerHit>) hit.getRawHits();
            RawTrackerHit rawHit = rawHits.get(0);
            // System.out.printf("found rawHits of size %d\n",
            // hit.getRawHits().size());
            dec.setID(rawHit.getCellID());
            int layer = dec.getValue("layer");

            if (holePattern[layer - 1] == true) {
                inputRawTrackHitCollection.removeAll(rawHits);
                inputHelicalTrackHitCollection.remove(hhit);
                inputRHelicalTrackHitCollection.remove(hit);
                iter.remove();
            }
            if (hit instanceof HelicalTrackCross)
                ((HelicalTrackCross) hit).resetTrackDirection();
            if (hhit instanceof HelicalTrackCross)
                ((HelicalTrackCross) hhit).resetTrackDirection();
        }

    }

    @Override
    public void detectorChanged(Detector detector) {
        dec = detector.getSubdetector("Tracker").getIDDecoder();
    }

    protected void loadCollections(EventHeader event) {
        inputTrackCollection = event.get(Track.class, inputTrackCollectionName);
        inputTrackHitCollection = event.get(TrackerHit.class, inputTrackHitCollectionName);
        inputRawTrackHitCollection = event.get(RawTrackerHit.class, inputRawTrackHitCollectionName);
        inputHelicalTrackHitCollection = event.get(HelicalTrackHit.class, inputHelicalTrackHitCollectionName);
        inputRHelicalTrackHitCollection = event.get(HelicalTrackHit.class, inputRHelicalTrackHitCollectionName);

        hitToRotated = TrackUtils.getHitToRotatedTable(event);

        rn = new Random();
    }

    /**
     * Writes new raw hit, helical track hit, and rotated helical track hit
     * collections. Does not keep the old ones.
     * 
     * @param event
     */
    protected void putCollections(EventHeader event) {
        event.put(outputRawTrackHitCollectionName, inputRawTrackHitCollection);
        event.put(outputHelicalTrackHitCollectionName, inputHelicalTrackHitCollection);
        event.put(outputRHelicalTrackHitCollectionName, inputRHelicalTrackHitCollection);
    }

    @Override
    public void process(EventHeader event) {
        loadCollections(event);

        for (Track trk : inputTrackCollection) {
            createHoles(trk);
        }

        putCollections(event);

    }
}
