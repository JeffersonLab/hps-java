package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.List;

import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.MCParticle.SimulatorStatus;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.event.Track;

/**
 *
 * @author Norman A. Graf
 */
public class MCTrackExtrapolationTestDriver extends Driver {

    AIDA aida = AIDA.defaultInstance();
    private static final String SUBDETECTOR_NAME = "Tracker";

    @Override
    protected void process(EventHeader event) {
        setupSensors(event);

        // get objects and collections from event header
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimTrackerHit> trackerHitsECal = event.get(SimTrackerHit.class, "TrackerHitsECal");
        List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");

        if (particles.size() != 1) {
            return;
        }

        MCParticle mcp = particles.get(0);
        SimulatorStatus simstat = mcp.getSimulatorStatus();
        if (simstat.isDecayedInCalorimeter() || simstat.hasLeftDetector()) {

        }

        List<Track> tracks = event.get(Track.class, "GBLTracks");
        System.out.println("found " + tracks.size() + " GBL tracks");
        for (Track t : tracks) {
//            TrackState tsAtEcal = TrackStateUtils.getTrackStateAtECal(t);
            Hep3Vector atEcal = new BasicHep3Vector(Double.NaN, Double.NaN, Double.NaN);
//            if (tsAtEcal != null) {
//                atEcal = new BasicHep3Vector(tsAtEcal.getReferencePoint());
//                atEcal = CoordinateTransformations.transformVectorToDetector(atEcal);
//            }
            int nSimTrackerHits = trackerHitsECal.size();
            double[] mcAtEcal = new double[3];

            System.out.println("found " + trackerHitsECal.size() + " SimTrackerHits at ECal scoring plane.");
            for (SimTrackerHit hit : trackerHitsECal) {
                System.out.println(hit.getPositionVec() + " " + hit.getTime());
                mcAtEcal = hit.getPosition();

                if (nSimTrackerHits == 1) {
                    System.out.println("Track State at ECal " + atEcal);
                    System.out.println("found " + trackerHitsECal.size() + " SimTrackerHits at ECal scoring plane.");
                }
            }
        }
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;

        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        } else {

            if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits")) {
                rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
            }
        }
        if (rawTrackerHits != null) {
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
    }

}
