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
import hep.aida.IManagedObject;
import hep.aida.IBaseHistogram;


/**
 * Make post-GBL plots needed for alignment.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @author Alessandra Filippi <filippi@to.infn.it>
 */
public class GBLOutputDriver extends Driver {

    private AIDA aidaGBL; // era public 
    private String outputPlots = "GBLplots_ali.root";
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

    public void setTrackCollectionName(String val) {
        trackCollectionName=val;
    }
        

    @Override
    protected void detectorChanged(Detector detector) {
        if (aidaGBL == null)
            aidaGBL = AIDA.defaultInstance();

        aidaGBL.tree().cd("/");

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

            aidaGBL.histogram1D("lambda_kink_" + sensor.getName()).fill(lambda);
            aidaGBL.histogram1D("phi_kink_" + sensor.getName()).fill(phi);
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

            aidaGBL.histogram1D("residual_before_GBL_" + sensor.getName()).fill(diff.x());
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
        aidaGBL.histogram1D("d0_" + isTop).fill(trackState.getD0());
        aidaGBL.histogram1D("z0_" + isTop).fill(trackState.getZ0());
        aidaGBL.histogram1D("p_" + isTop).fill(new BasicHep3Vector(trackState.getMomentum()).magnitude());

        Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        if (debug)
            System.out.printf("beamspot %s transformed %s \n", beamspot.toString());
        aidaGBL.histogram1D("beamspot_x_" + isTop).fill(beamspot.x());
        aidaGBL.histogram1D("beamspot_y_" + isTop).fill(beamspot.y());
    }

    private void doGBLresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();

            // position of hit (track crossing the sensor before GBL extrapolation)
            // the hit information available on each sensor is meaningful only along the measurement direction,
            // Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            // instead: extract the information of the hit of the track at the sensor position before GBL
            TrackState trackState = trk.getTrackStates().get(0);
            Hep3Vector hitTrackPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            Hep3Vector hitTrackPosSensor = new BasicHep3Vector(hitTrackPos.v());
            trans.transform(hitTrackPosSensor);
            // after the transformation x and y in the sensor frame are reversed
            aidaGBL.histogram2D("hit_u_vs_v_sensor_frame_" + sensor.getName()).fill(hitTrackPosSensor.y(), hitTrackPosSensor.x());
            //aidaGBL.histogram2D("hit_u_vs_v_sensor_frame_" + sensor.getName()).fill(hitPos.y(), hitPos.x());
            //aidaGBL.histogram2D("hit y vs x lab-frame " + sensor.getName()).fill(hitPos.y(), hitPos.x());

            // position predicted on track after GBL
            Hep3Vector extrapPos = null;
            Hep3Vector extrapPosSensor = null;
            extrapPos = TrackUtils.extrapolateTrackPositionToSensor(trk, sensor, sensors, bfield);
            if (extrapPos == null)
                return;
            extrapPosSensor = new BasicHep3Vector(extrapPos.v());
            trans.transform(extrapPosSensor);
            //aidaGBL.histogram2D("residual after GBL vs u predicted " + sensor.getName()).fill(extrapPosSensor.x(), res);
            aidaGBL.histogram2D("predicted_u_vs_v_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            // select track charge
            if(trk.getCharge()>0) {
                aidaGBL.histogram2D("predicted_u_vs_v_pos_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            }else if(trk.getCharge()<0) {
                aidaGBL.histogram2D("predicted_u_vs_v_neg_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            }
            
            // post-GBL residual
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector hitPosSensor = new BasicHep3Vector(hitPos.v());
            trans.transform(hitPosSensor);
            Hep3Vector resSensor = VecOp.sub(hitPosSensor, extrapPosSensor);
            aidaGBL.histogram2D("residual_after_GBL_vs_v_predicted_" + sensor.getName()).fill(extrapPosSensor.y(), resSensor.x());
            aidaGBL.histogram2D("residual_after_GBL_vs_u_hit_" + sensor.getName()).fill(hitPosSensor.x(), resSensor.x());
            aidaGBL.histogram1D("residual_after_GBL_" + sensor.getName()).fill(resSensor.x());

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
            double xmax = 0.1;
            int l = (sens.getLayerNumber() + 1) / 2;
            if (l > 1) xmax = 0.05 + (l - 1) * 0.08;
            aidaGBL.histogram1D("residual_before_GBL_" + sensor.getName(), 50, -1.0 * xmax, xmax);

            xmax = 0.05;
            if (l >= 6)
                xmax = 0.01;
            aidaGBL.histogram1D("residual_after_GBL_" + sensor.getName(), 50, -1.0 * xmax, xmax);

            aidaGBL.histogram2D("residual_after_GBL_vs_u_hit_" + sensor.getName(), 100, -20.0, 20.0, 100, -0.04, 0.04);
            aidaGBL.histogram2D("residual_after_GBL_vs_v_predicted_" + sensor.getName(), 100, -55.0, 55.0, 100, -0.04, 0.04);
            aidaGBL.histogram2D("hit_u_vs_v_sensor_frame_" + sensor.getName(), 100, -60.0, 60.0, 100, -25, 25);
            aidaGBL.histogram2D("predicted_u_vs_v_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            aidaGBL.histogram2D("predicted_u_vs_v_pos_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            aidaGBL.histogram2D("predicted_u_vs_v_neg_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            
            xmax = 0.0006;
            if(l==1){
                xmax = 0.0002;
            }else if(l==2){
                xmax = 0.0005;
            }else if(l==3 || l==4){
                xmax = 0.0006;
            }else if(l >= 5) {
                if (sens.isBottomLayer() && sens.isAxial())
                    xmax = 0.001;
                if (sens.isTopLayer() && !sens.isAxial())
                    xmax = 0.001;
            }
            aidaGBL.histogram1D("lambda_kink_" + sensor.getName(), 50, -1.0 * xmax, xmax);
            aidaGBL.histogram1D("phi_kink_" + sensor.getName(), 50, -1.0 * xmax, xmax);
        }

        aidaGBL.histogram1D("d0_top", 50, -2.0, 2.0);
        aidaGBL.histogram1D("z0_top", 50, -1.3, 1.3);
        aidaGBL.histogram1D("beamspot_x_top", 50, -3, 3);
        aidaGBL.histogram1D("beamspot_y_top", 50, -3, 3);
        aidaGBL.histogram1D("d0_bottom", 50, -2.0, 2.0);
        aidaGBL.histogram1D("z0_bottom", 50, -1.3, 1.3);
        if(bfield > 1.03 && bfield < 1.04){ // 2019 bfield: 1.03 T
            aidaGBL.histogram1D("p_top", 150, 0., 6.);
            aidaGBL.histogram1D("p_bottom", 150, 0., 6.);
        }else if(bfield > 0.5 && bfield < 0.6){ // 2016 bfield: 0.523 T
            aidaGBL.histogram1D("p_top", 150, 0., 3.);
            aidaGBL.histogram1D("p_bottom", 150, 0., 3.);
        }else if(bfield <0.25){ // 2015 bfield: 0.24 T
            aidaGBL.histogram1D("p_top", 150, 0., 2.);
            aidaGBL.histogram1D("p_bottom", 150, 0., 2.);
        }
        aidaGBL.histogram1D("beamspot_x_bottom", 50, -3, 3);
        aidaGBL.histogram1D("beamspot_y_bottom", 50, -3, 3);
    }

    public void endOfData() {
        if (outputPlots != null) {
            try {
                aidaGBL.saveAs(outputPlots);
                // remove all GBL histograms from heap after they have been written on output file
                String[] type = aidaGBL.tree().listObjectNames("/",true);
                for (int i=0; i<type.length; i++){
                    // strip the trailing / from the object name and check if any else
                    String namtyp = type[i].substring(1);
                    if(namtyp.contains("/")) {
                        continue;
                    }else{
                        IManagedObject obj = aidaGBL.tree().find(namtyp);
                        if (obj instanceof IBaseHistogram) aidaGBL.tree().rm(obj.name()) ;
                    }
                }

            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
