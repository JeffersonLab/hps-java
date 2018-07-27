package org.hps.recon.tracking.gbl;

import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Make post-GBL plots needed for alignment.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 */
public class GBLOutput extends Driver {

    public AIDA aida;
    public String outputPlots = "GBLplots.root";
    private String trackCollectionName = "GBLTracks";
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;

    @Override
    protected void detectorChanged(Detector detector) {
        if (aida == null)
            aida = AIDA.defaultInstance();

        aida.tree().cd("/");
        for (HpsSiSensor s : detector.getDetectorElement().findDescendants(HpsSiSensor.class)) {
            if (s.getName().startsWith("module_") && s.getName().endsWith("sensor0")) {
                sensors.add(s);
            }
        }

        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        this.bfield = Math.abs(fieldInTracker.y());

        setupPlots();
    }

    @Override
    public void process(EventHeader event) {
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        RelationalTable trackResidualsTable = null;
        trackResidualsTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackresRelation = event.get(LCRelation.class, "TrackResidualsRelations");
        for (LCRelation relation : trackresRelation) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                trackResidualsTable.add(relation.getFrom(), relation.getTo());
            }
        }

        RelationalTable kinkTable = null;
        kinkTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> kinkRelation = event.get(LCRelation.class, "TrackResidualsRelations");
        for (LCRelation relation : kinkRelation) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                kinkTable.add(relation.getFrom(), relation.getTo());
            }
        }

        RelationalTable trackMatchTable = null;
        trackMatchTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackMatchRelation = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
        for (LCRelation relation : trackMatchRelation) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                trackMatchTable.add(relation.getFrom(), relation.getTo());
            }
        }

        for (Track trk : tracks) {
            GenericObject trackRes = (GenericObject) trackResidualsTable.from(trk);
            GenericObject gblKink = (GenericObject) kinkTable.to(trk);
            Track matchedTrack = (Track) trackMatchTable.from(trk);
            Map<HpsSiSensor, TrackerHit> sensorHits = new HashMap<HpsSiSensor, TrackerHit>();
            Map<HpsSiSensor, Integer> sensorNums = new HashMap<HpsSiSensor, Integer>();
            List<TrackerHit> hitsOnTrack = trk.getTrackerHits();

            int i = 0;
            for (TrackerHit hit : hitsOnTrack) {
                HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
                sensorHits.put(sensor, hit);
                sensorNums.put(sensor, i);
                i++;
            }
            doGBLresiduals(trk, sensorHits, sensorNums, trackRes);
            doMTresiduals(matchedTrack, sensorHits);
            doGBLkinks(gblKink, sensorNums);
        }
    }

    private void doGBLkinks(GenericObject kink, Map<HpsSiSensor, Integer> sensorNums) {
        for (HpsSiSensor sensor : sensorNums.keySet()) {
            int index = sensorNums.get(sensor);
            double phi = kink.getDoubleVal(index);
            float lambda = kink.getFloatVal(index);
        }

    }

    private void doMTresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        TrackState trackState = trk.getTrackStates().get(0);
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            Hep3Vector extrapPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector res = VecOp.sub(extrapPos, hitPos);

        }
    }

    private void doGBLresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits, Map<HpsSiSensor, Integer> sensorNums, GenericObject GBLres) {
        TrackState trackState = trk.getTrackStates().get(0);

        for (HpsSiSensor sensor : sensorHits.keySet()) {
            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();

            // post-GBL residual
            int index = sensorNums.get(sensor);
            float resY = GBLres.getFloatVal(index);
            double resX = GBLres.getDoubleVal(index);
            double res = Math.sqrt(resX * resX + resY * resY);

            // position predicted on track
            Hep3Vector extrapPos = null;
            Hep3Vector extrapPosSensor = null;
            //if ((trackState.getTanLambda() > 0 && sensor.isTopLayer()) || (trackState.getTanLambda() < 0 && sensor.isBottomLayer()))
            extrapPos = TrackUtils.extrapolateTrackPositionToSensor(trk, sensor, sensors, bfield);
            if (extrapPos != null) {
                if ((trackState.getTanLambda() > 0 && sensor.isTopLayer()) || (trackState.getTanLambda() < 0 && sensor.isBottomLayer())) {
                    extrapPosSensor = new BasicHep3Vector(extrapPos.v());
                    trans.transform(extrapPosSensor);
                }
            }

            // position of hit
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector hitPosSensor = new BasicHep3Vector(hitPos.v());
            trans.transform(hitPosSensor);

            //SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(hit);
        }
    }

    private void setupPlots() {
        for (SiSensor sensor : sensors) {
            aida.histogram1D("residual before GBL " + sensor.getName(), 10, 0, 10);
            aida.histogram1D("residual after GBL " + sensor.getName(), 10, 0, 10);
            aida.histogram2D("residual after GBL vs u hit " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram2D("residual after GBL vs u predicted " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram2D("residual after GBL vs v predicted " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram2D("hit y vs x lab-frame " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram2D("hit v vs u sensor-frame " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram2D("predicted v vs u sensor-frame " + sensor.getName(), 10, 0, 10, 10, 0, 10);
            aida.histogram1D("lambda kink " + sensor.getName(), 10, 0, 10);
            aida.histogram1D("phi kink " + sensor.getName(), 10, 0, 10);
        }

        aida.histogram1D("d0 top", 10, 0, 10);
        aida.histogram1D("z0 top", 10, 0, 10);
        aida.histogram1D("p top", 10, 0, 10);
        aida.histogram1D("beamspot x top", 10, 0, 10);
        aida.histogram1D("beamspot y top", 10, 0, 10);

        aida.histogram1D("d0 bottom", 10, 0, 10);
        aida.histogram1D("z0 bottom", 10, 0, 10);
        aida.histogram1D("p bottom", 10, 0, 10);
        aida.histogram1D("beamspot x bottom", 10, 0, 10);
        aida.histogram1D("beamspot y bottom", 10, 0, 10);
    }
}
