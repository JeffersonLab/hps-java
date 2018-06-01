package org.hps.analysis.alignment;

import java.util.List;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class StraightTrackSvtAlignmentDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    protected void process(EventHeader event) {
        setupSensors(event);
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        //System.out.println("found " + tracks.size() + " tracks ");
        for (Track t : tracks) {
            if (!isGoodTrack(t)) {
                continue;
            }
            plotTrack(t);

        }
    }

    private void plotTrack(Track t) {
        // in principle, tracks with multi-strip hits are better measured...
        // 1st axial layer has greatest influence on theta, so require 2 strips in hit
        // TODO should I also require 2 strips in stereo layers?
        int tL1AxialNstrips = 0;
        int tL1StereoNstrips = 0;
        int tL2AxialNstrips = 0;
        int tL2StereoNstrips = 0;
        int tL1AxialStripNumber = 0;
        int tL1StereoStripNumber = 0;
        int tL2AxialStripNumber = 0;
        int tL2StereoStripNumber = 0;
        TrackState ts = t.getTrackStates().get(0);
        double d0 = ts.getD0();
        double z0 = ts.getZ0();
        double tanL = ts.getTanLambda();

        String half = isTopTrack(t) ? "top" : "bottom";

        for (TrackerHit hit : TrackUtils.getStripHits(t, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    tL1AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        tL1AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 640, 0., 640.).fill(tL1AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    tL1StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        tL1StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 640, 0., 640.).fill(tL1StereoStripNumber);
                    }
                }
            }
            if (moduleName.contains("module_L2")) {
                if (moduleName.contains("axial")) {
                    tL2AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        tL2AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 640, 0., 640.).fill(tL2AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    tL2StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        tL2StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 640, 0., 640.).fill(tL2StereoStripNumber);
                    }
                }
            }
        }

        if (isTopTrack(t)) {
            aida.histogram1D(half + " Track d0", 50, -50., 0.).fill(d0);
            aida.histogram1D(half + " Track z0", 20, 20., 40.).fill(z0);
            aida.histogram1D(half + " Track tanL", 1000, 0.005, 0.02).fill(tanL);
            aida.histogram2D(half + " Track tanL vs z0", 50, 0.005, 0.02, 20, 20., 40.).fill(tanL, z0);
            aida.profile1D(half + " Track tanL vs z0 profile", 100, 0.011, 0.0150).fill(tanL, z0);
            if (tL1AxialNstrips < 3 && tL2AxialNstrips < 3) {
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track d0", 50, -50., 0.).fill(d0);
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track z0", 20, 20., 40.).fill(z0);
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL", 1000, 0.005, 0.02).fill(tanL);
                aida.histogram2D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL vs z0", 50, 0.005, 0.02, 20, 20., 40.).fill(tanL, z0);
                aida.profile1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL vs z0 profile", 100, 0.011, 0.0150).fill(tanL, z0);
            }
        } else {
            aida.histogram1D(half + " Track d0", 50, -50., 0.).fill(d0);
            aida.histogram1D(half + " Track z0", 20, -40., -20.).fill(z0);
            aida.histogram1D(half + " Track tanL", 1000, -0.02, -0.005).fill(tanL);
            aida.histogram2D(half + " Track tanL vs z0", 50, -0.02, -0.005, 20, -40., -20.).fill(tanL, z0);
            aida.profile1D(half + " Track tanL vs z0 profile", 100, -0.015, -0.011).fill(tanL, z0);
            if (tL1AxialNstrips < 3 && tL2AxialNstrips < 3) {
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track d0", 50, -50., 0.).fill(d0);
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track z0", 20, -40., -20.).fill(z0);
                aida.histogram1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL", 1000, -0.02, -0.005).fill(tanL);
                aida.histogram2D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL vs z0", 50, -0.02, -0.005, 20, -40., -20.).fill(tanL, z0);
                aida.profile1D(half + " " + tL1AxialNstrips + " " + tL2AxialNstrips + " Track tanL vs z0 profile", 100, -0.015, -0.011).fill(tanL, z0);
            }
        }

    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    private boolean isGoodTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return true;
        }
        return false;
    }

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");
    }
}
