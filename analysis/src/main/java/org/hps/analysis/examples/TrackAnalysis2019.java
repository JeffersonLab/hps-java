package org.hps.analysis.examples;

import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class TrackAnalysis2019 extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    protected void process(EventHeader event) {
        setupSensors(event);
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        for (ReconstructedParticle rp : rpList) {

            if (!TrackType.isGBL(rp.getType())) {
                continue;
            }
            Track t = rp.getTracks().get(0);
            int nHitsOnTrack = t.getTrackerHits().size();
            int pdgId = rp.getParticleIDUsed().getPDG();
            Hep3Vector pmom = rp.getMomentum();
            double thetaY = asin(pmom.y() / pmom.magnitude());
            double z0 = t.getTrackStates().get(0).getZ0();
            String torb = isTopTrack(t) ? "top " : "bottom ";
            aida.histogram1D(torb + pdgId + " track nHits", 10, 0., 10.).fill(nHitsOnTrack);

            aida.histogram1D(torb + pdgId + " track momentum", 100, 0., 7.).fill(rp.getMomentum().magnitude());
            aida.cloud1D(torb + pdgId + "|thetaY|").fill(abs(thetaY));
            aida.histogram1D(torb + pdgId + "z0", 100, -2., 2.).fill(z0);
            aida.cloud2D(torb + pdgId + "|thetaY| vs z0").fill(abs(thetaY), z0);
            aida.profile1D(torb + pdgId + "|thetaY| vs z0 profile", 10, 0.01, 0.1).fill(abs(thetaY), z0);

            aida.histogram1D(torb + pdgId + " " + nHitsOnTrack + " hit track momentum", 100, 0., 7.).fill(rp.getMomentum().magnitude());
            aida.cloud1D(torb + pdgId + " " + nHitsOnTrack + " hit |thetaY|").fill(abs(thetaY));
            aida.histogram1D(torb + pdgId + " " + nHitsOnTrack + " hit z0", 100, -2., 2.).fill(z0);
            aida.cloud2D(torb + pdgId + " " + nHitsOnTrack + " hit |thetaY| vs z0").fill(abs(thetaY), z0);
            aida.profile1D(torb + pdgId + " " + nHitsOnTrack + " hit |thetaY| vs z0 profile", 10, 0.01, 0.1).fill(abs(thetaY), z0);
        }

//        List<ReconstructedParticle> otherElectrons = event.get(ReconstructedParticle.class, "OtherElectrons");
//        for (ReconstructedParticle rp : otherElectrons) {
//
//            if (!TrackType.isGBL(rp.getType())) {
//                continue;
//            }
//            Track t = rp.getTracks().get(0);
//            
//            Hep3Vector pmom = rp.getMomentum();
//            double thetaY = asin(pmom.y() / pmom.magnitude());
//            double z0 = t.getTrackStates().get(0).getZ0();
//            String torb = isTopTrack(t) ? "top " : "bottom ";
//            aida.histogram1D(torb+"OtherElectron track momentum", 100, 0., 5.).fill(rp.getMomentum().magnitude());
//            aida.cloud1D("OtherElectron " + torb + "|thetaY|").fill(abs(thetaY));
//            aida.histogram1D("OtherElectron " + torb + "z0", 100, -2., 2.).fill(z0);
//            aida.cloud2D("OtherElectron " + torb + "|thetaY| vs z0").fill(abs(thetaY), z0);
//            aida.profile1D("OtherElectron " + torb + "|thetaY| vs z0 profile", 10, 0.01, 0.1).fill(abs(thetaY), z0);
//        }
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
}
