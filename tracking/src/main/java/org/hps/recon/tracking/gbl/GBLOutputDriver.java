package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
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
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Make post-GBL plots needed for alignment.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 */
public class GBLOutputDriver extends Driver {

    public AIDA aida;
    private String outputPlots = "GBLplots.root";
    private String trackCollectionName = "GBLTracks";
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;
    public boolean debug = false;
    private double chi2Cut = 20;

    public void setChi2Cut(double input) {
        chi2Cut = input;
    }

    public void setOutputPlotsFilename(String fname) {
        outputPlots = fname;
    }

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

        RelationalTable trackMatchTable = null;
        trackMatchTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trackMatchRelation = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
        for (LCRelation relation : trackMatchRelation) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                trackMatchTable.add(relation.getFrom(), relation.getTo());
            }
        }

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        for (Track trk : tracks) {
            if (trk.getChi2() > chi2Cut)
                continue;
            GenericObject gblKink = GBLKinkData.getKinkData(event, trk);
            Track matchedTrack = (Track) trackMatchTable.from(trk);
            Map<HpsSiSensor, TrackerHit> sensorHits = new HashMap<HpsSiSensor, TrackerHit>();
            Map<HpsSiSensor, Integer> sensorNums = new HashMap<HpsSiSensor, Integer>();
            List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(trk, hitToStrips, hitToRotated);

            int i = 0;
            for (TrackerHit hit : hitsOnTrack) {
                HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
                if (sensor != null) {
                    sensorHits.put(sensor, hit);
                    sensorNums.put(sensor, i);
                    if (debug)
                        System.out.printf("adding sensor %d \n", i);
                }

                if (debug && sensor == null)
                    System.out.printf("TrackerHit null sensor %s \n", hit.toString());
                i++;
            }
            doBasicGBLtrack(trk);
            doGBLresiduals(trk, sensorHits);
            doMTresiduals(matchedTrack, sensorHits);
            doGBLkinks(gblKink, sensorNums);
        }
    }

    private void doGBLkinks(GenericObject kink, Map<HpsSiSensor, Integer> sensorNums) {
        for (HpsSiSensor sensor : sensorNums.keySet()) {
            int index = sensorNums.get(sensor);
            double phi = kink.getDoubleVal(index);
            float lambda = kink.getFloatVal(index);

            aida.histogram1D("lambda kink " + sensor.getName()).fill(lambda);
            aida.histogram1D("phi kink " + sensor.getName()).fill(phi);
        }

    }

    private void doMTresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        TrackState trackState = trk.getTrackStates().get(0);
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            Hep3Vector extrapPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector diff = VecOp.sub(extrapPos, hitPos);
            if (debug)
                System.out.printf("MextrapPos %s MhitPos %s \n Mdiff %s ", extrapPos.toString(), hitPos.toString(), diff.toString());

            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();
            trans.rotate(diff);

            aida.histogram1D("residual before GBL " + sensor.getName()).fill(diff.x());
            if (debug)
                System.out.printf("MdiffSensor %s \n", diff.toString());

        }
    }

    private void doBasicGBLtrack(Track trk) {
        TrackState trackState = trk.getTrackStates().get(0);

        String isTop = "bottom";
        if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
            isTop = "top";
        }
        aida.histogram1D("d0 " + isTop).fill(trackState.getD0());
        aida.histogram1D("z0 " + isTop).fill(trackState.getZ0());
        aida.histogram1D("p " + isTop).fill(new BasicHep3Vector(trackState.getMomentum()).magnitude());

        Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        if (debug)
            System.out.printf("beamspot %s transformed %s \n", beamspot.toString());
        aida.histogram1D("beamspot x " + isTop).fill(beamspot.x());
        aida.histogram1D("beamspot y " + isTop).fill(beamspot.y());
    }

    private void doGBLresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {

        for (HpsSiSensor sensor : sensorHits.keySet()) {
            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();

            // position predicted on track
            Hep3Vector extrapPos = null;
            Hep3Vector extrapPosSensor = null;
            extrapPos = TrackUtils.extrapolateTrackPositionToSensor(trk, sensor, sensors, bfield);
            if (extrapPos == null)
                return;
            extrapPosSensor = new BasicHep3Vector(extrapPos.v());
            trans.transform(extrapPosSensor);
            //aida.histogram2D("residual after GBL vs u predicted " + sensor.getName()).fill(extrapPosSensor.x(), res);
            aida.histogram2D("predicted v vs u sensor-frame " + sensor.getName()).fill(extrapPosSensor.x(), extrapPosSensor.y());

            // position of hit
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector hitPosSensor = new BasicHep3Vector(hitPos.v());
            trans.transform(hitPosSensor);
            aida.histogram2D("hit v vs u sensor-frame " + sensor.getName()).fill(hitPosSensor.y(), hitPosSensor.x());
            //aida.histogram2D("hit y vs x lab-frame " + sensor.getName()).fill(hitPos.y(), hitPos.x());

            // post-GBL residual
            Hep3Vector resSensor = VecOp.sub(hitPosSensor, extrapPosSensor);
            aida.histogram2D("residual after GBL vs v predicted " + sensor.getName()).fill(extrapPosSensor.y(), resSensor.x());
            aida.histogram2D("residual after GBL vs u hit " + sensor.getName()).fill(hitPosSensor.x(), resSensor.x());
            aida.histogram1D("residual after GBL " + sensor.getName()).fill(resSensor.x());

            if (debug) {
                System.out.printf("hitPos %s  hitPosSensor %s \n", hitPos.toString(), hitPosSensor.toString());
                System.out.printf("resSensor %s \n", resSensor.toString());
                System.out.printf("extrapPos %s  extrapPosSensor %s \n", extrapPos.toString(), extrapPosSensor.toString());
                ITransform3D electrodes_to_global = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
                Hep3Vector measuredCoordinate = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0);
                Hep3Vector unmeasuredCoordinate = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getUnmeasuredCoordinate(0);
                System.out.printf("unMeasCoordOrig %s MeasCoordOrig %s \n", unmeasuredCoordinate.toString(), measuredCoordinate.toString());
                measuredCoordinate = VecOp.mult(VecOp.mult(CoordinateTransformations.getMatrix(), electrodes_to_global.getRotation().getRotationMatrix()), measuredCoordinate);
                unmeasuredCoordinate = VecOp.mult(VecOp.mult(CoordinateTransformations.getMatrix(), electrodes_to_global.getRotation().getRotationMatrix()), unmeasuredCoordinate);
                Hep3Vector testX = trans.inverse().rotated(new BasicHep3Vector(1, 0, 0));
                Hep3Vector testY = trans.inverse().rotated(new BasicHep3Vector(0, 1, 0));
                Hep3Vector testZ = trans.inverse().rotated(new BasicHep3Vector(0, 0, 1));
                System.out.printf("unMeasCoord %s MeasCoord %s \n transX %s transY %s transZ %s \n", unmeasuredCoordinate.toString(), measuredCoordinate.toString(), testX.toString(), testY.toString(), testZ.toString());
            }
        }
    }

    private void setupPlots() {
        for (SiSensor sensor : sensors) {

            HpsSiSensor sens = (HpsSiSensor) sensor.getGeometry().getDetectorElement();
            double xmax = 1.0;
            int l = (sens.getLayerNumber() + 1) / 2;
            if (l > 1)
                xmax = 0.01 + (l - 1) * 0.6;
            aida.histogram1D("residual before GBL " + sensor.getName(), 50, -1.0 * xmax, xmax);

            xmax = 0.05;
            if (l == 6)
                xmax = 0.01;
            aida.histogram1D("residual after GBL " + sensor.getName(), 50, -1.0 * xmax, xmax);

            aida.histogram2D("residual after GBL vs u hit " + sensor.getName(), 100, -20.0, 20.0, 100, -0.04, 0.04);
            //aida.histogram2D("residual after GBL vs u predicted " + sensor.getName(), 100,-20.0,20.0,100,-0.04,0.04);
            aida.histogram2D("residual after GBL vs v predicted " + sensor.getName(), 100, -55.0, 55.0, 100, -0.04, 0.04);
            //aida.histogram2D("hit y vs x lab-frame " + sensor.getName(), 100,-50.0,50.0,100,-20,20);
            aida.histogram2D("hit v vs u sensor-frame " + sensor.getName(), 100, -50.0, 50.0, 100, -20, 20);
            aida.histogram2D("predicted v vs u sensor-frame " + sensor.getName(), 100, -60, 60, 100, -25, 25);

            xmax = 0.006;
            if (l == 6) {
                if (sens.isBottomLayer() && sens.isAxial())
                    xmax = 0.002;
                if (sens.isTopLayer() && !sens.isAxial())
                    xmax = 0.002;
            }
            aida.histogram1D("lambda kink " + sensor.getName(), 50, -1.0 * xmax, xmax);
            aida.histogram1D("phi kink " + sensor.getName(), 50, -1.0 * xmax, xmax);
        }

        aida.histogram1D("d0 top", 50, -2.0, 2.0);
        aida.histogram1D("z0 top", 50, -1.3, 1.3);
        aida.histogram1D("p top", 150, 0, 3);
        aida.histogram1D("beamspot x top", 50, -3, 3);
        aida.histogram1D("beamspot y top", 50, -3, 3);

        aida.histogram1D("d0 bottom", 50, -2.0, 2.0);
        aida.histogram1D("z0 bottom", 50, -1.3, 1.3);
        aida.histogram1D("p bottom", 150, 0, 3);
        aida.histogram1D("beamspot x bottom", 50, -3, 3);
        aida.histogram1D("beamspot y bottom", 50, -3, 3);
    }

    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
