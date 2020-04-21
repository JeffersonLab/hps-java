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
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import hep.aida.IManagedObject;
import hep.aida.IBaseHistogram;


import org.lcsim.geometry.FieldMap;


/**
 * Make post-GBL plots needed for alignment.
 *
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @author Alessandra Filippi <filippi@to.infn.it>
 * @author PF <pbutti@slac.stanford.edu>
 */
public class GBLOutputDriver extends Driver {
    
    private AIDA aidaGBL; // era public 
    private String outputPlots = "GBLplots_ali.root";
    private String trackCollectionName = "GBLTracks";
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;
    public boolean debug = false;
    private double chi2Cut = 99999;
    String kinkFolder = "/gbl_kinks/";
    private boolean b_doGBLkinks = false;

    //This should be moved to the GBL Refitter!!!
    //The field map for extrapolation
    private FieldMap bFieldMap;

    //The location of the extrapolation
    private double bsZ = 0.;

    //Override the Z of the target.
    public void setBsZ (double input) {
        bsZ = input;
    }

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

        bFieldMap = detector.getFieldMap();

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
            Map<HpsSiSensor, Integer> sensorNums    = new HashMap<HpsSiSensor, Integer>();
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
            doGBLresiduals(trk, sensorHits,event);
            doMTresiduals(matchedTrack, sensorHits);
            if (b_doGBLkinks)
                doGBLkinks(gblKink, sensorNums);
        }
    }

    private void doGBLkinks(GenericObject kink, Map<HpsSiSensor, Integer> sensorNums) {
        for (HpsSiSensor sensor : sensorNums.keySet()) {
            int index = sensorNums.get(sensor);
            double phi = kink.getDoubleVal(index);
            float lambda = kink.getFloatVal(index);
            

            aidaGBL.histogram1D(kinkFolder+"lambda_kink_" + sensor.getName()).fill(lambda);
            aidaGBL.histogram1D(kinkFolder+"phi_kink_" + sensor.getName()).fill(phi);
        }

    }

    private void doMTresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        TrackState trackState = trk.getTrackStates().get(0);
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            Hep3Vector extrapPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            if (hitPos == null || extrapPos == null)
                return;
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
    
    private void FillGBLTrackPlot(String str, String isTop, String charge, double val) {
        aidaGBL.histogram1D(str+isTop).fill(val);
        aidaGBL.histogram1D(str+isTop+charge).fill(val);
    }

    private void FillGBLTrackPlot(String str, String isTop, String charge, double valX, double valY) {
        aidaGBL.histogram2D(str+isTop).fill(valX,valY);
        aidaGBL.histogram2D(str+isTop+charge).fill(valX,valY);
    }

    private void doBasicGBLtrack(Track trk) {
        TrackState trackState = trk.getTrackStates().get(0);

        String isTop = "_bottom";
        if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
            isTop = "_top";
        }
        
        String charge = "_pos";
        if (trk.getCharge()<0)
            charge = "_neg";
        
        double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
        
        FillGBLTrackPlot("d0",isTop,charge,trackState.getD0());
        FillGBLTrackPlot("z0",isTop,charge,trackState.getZ0());
        FillGBLTrackPlot("phi",isTop,charge,trackState.getPhi());
        FillGBLTrackPlot("tanLambda",isTop,charge,trackState.getTanLambda());
        FillGBLTrackPlot("p",isTop,charge,trackp);
        FillGBLTrackPlot("Chi2",isTop,charge,trk.getChi2());
                
        aidaGBL.histogram1D("nHits" + isTop).fill(trk.getTrackerHits().size());
        aidaGBL.histogram1D("nHits" + isTop+charge).fill(trk.getTrackerHits().size());

        Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        if (debug)
            System.out.printf("beamspot %s transformed %s \n", beamspot.toString());
        FillGBLTrackPlot("trk_extr_or_x",isTop,charge,beamspot.x());
        FillGBLTrackPlot("trk_extr_or_y",isTop,charge,beamspot.y());
        
        
        //Extrapolation to assumed tgt pos - helix
        Hep3Vector trkTgt = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState,bsZ));
        FillGBLTrackPlot("trk_extr_bs_x",isTop,charge,trkTgt.x());
        FillGBLTrackPlot("trk_extr_bs_y",isTop,charge,trkTgt.y());
                 
        BaseTrackState ts_bs = TrackUtils.getTrackExtrapAtVtxSurfRK(trackState,bFieldMap,0.,bsZ);
         
        FillGBLTrackPlot("trk_extr_bs_x_rk",isTop,charge,ts_bs.getReferencePoint()[1]);
        FillGBLTrackPlot("trk_extr_bs_y_rk",isTop,charge,ts_bs.getReferencePoint()[2]);

        //Ill defined - should be defined wrt bsX and bsY
        FillGBLTrackPlot("d0_vs_bs_rk",isTop,charge,ts_bs.getD0());
        
        //Ok
        FillGBLTrackPlot("z0_vs_bs_rk",isTop,charge,ts_bs.getZ0());

        //TH2D - Filling
        FillGBLTrackPlot("d0_vs_phi",isTop,charge,trackState.getPhi(),trackState.getD0());
        FillGBLTrackPlot("d0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getD0());
        FillGBLTrackPlot("d0_vs_p",isTop,charge,trackState.getD0(),trackp);
                
        //Ill defined - should be defined wrt bsX and bsY
        FillGBLTrackPlot("d0bs_vs_p",isTop,charge,trackState.getD0(),trackp);
        
        FillGBLTrackPlot("z0_vs_p",isTop,charge,trackState.getZ0(),trackp); 
        FillGBLTrackPlot("z0bs_vs_p",isTop,charge,ts_bs.getZ0(),trackp); 
        
        //Interesting plot to get a sense where z-vtx is. 
        //If z0 is referenced to the right BS z location, the slope of <z0> vs tanLambda is 0
        FillGBLTrackPlot("z0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getZ0());
        FillGBLTrackPlot("z0bs_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getTanLambda());
        
    }

    private void doGBLresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits, EventHeader event) {
        
        Map<Integer,HpsSiSensor> sensorMPIDs   = new HashMap<Integer,HpsSiSensor>();
        
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            //Also fill here the sensorMPIDs map
            sensorMPIDs.put(sensor.getMillepedeId(),sensor);
            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();
            
            // position of hit (track crossing the sensor before GBL extrapolation)
            // the hit information available on each sensor is meaningful only along the measurement direction,
            // Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            // instead: extract the information of the hit of the track at the sensor position before GBL
            TrackState trackState = trk.getTrackStates().get(0);
            Hep3Vector hitTrackPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            
            if (hitTrackPos == null) {
                if (debug) {
                    System.out.printf("GBLOutputDriver::doGBLresiduals:: hitTrackPos is null to sensor %s\n", sensor.toString());
                }
                continue;
            }
            
            Hep3Vector hitTrackPosSensor = new BasicHep3Vector(hitTrackPos.v());
            trans.transform(hitTrackPosSensor);
            // after the transformation x and y in the sensor frame are reversed
            // This plot is ill defined.
            
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
        }//loop on sensor hits
        
        RelationalTable trackResidualsTable = null;
        if (event.hasCollection(LCRelation.class, "TrackResidualsGBLRelations")) {
            trackResidualsTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trackresRelation = event.get(LCRelation.class, "TrackResidualsGBLRelations");
            for (LCRelation relation : trackresRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    trackResidualsTable.add(relation.getFrom(), relation.getTo());
                }
            }
        } else {
            System.out.println("null TrackResidualsGBL Data Relations.");
            //Failed finding TrackResidualsGBL
            return;
        }
        
        GenericObject trackRes = (GenericObject) trackResidualsTable.from(trk);
        if (trackRes == null) {
            System.out.println("null TrackResidualsGBL Data.");
            return;
        }
        
        //it's bias-unbias-bias-unbias-bias-unbias....
        //TODO add in trackRes the number of hits on tracks ?
        int nres = (trackRes.getNInt()-1);
        
        //get the bias first 
        for (int i_hit =0; i_hit < nres-1 ; i_hit+=2) {
            if (trackRes.getIntVal(i_hit)!=-999)  {
                String sensorName = (sensorMPIDs.get(trackRes.getIntVal(i_hit))).getName();
                if (debug) {
                    System.out.printf("NHits %d MPID sensor:%d %s %d\n", nres,trackRes.getIntVal(i_hit), sensorName,i_hit);
                    System.out.printf("Track residuals: %s %.5f %.5f\n",sensorName, trackRes.getDoubleVal(i_hit),trackRes.getFloatVal(i_hit));
                }
                aidaGBL.histogram1D("bresidual_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram1D("breserror_GBL_" + sensorName).fill(trackRes.getFloatVal(i_hit));
                aidaGBL.histogram1D("bres_pull_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit) / trackRes.getFloatVal(i_hit));
            }   
        }
        // get the unbias
        for (int i_hit =1; i_hit < nres-1 ; i_hit+=2) {
            if (trackRes.getIntVal(i_hit)!=-999)  {  
                String sensorName = (sensorMPIDs.get(trackRes.getIntVal(i_hit))).getName();
                if (debug) {
                    System.out.printf("NHits %d MPID sensor:%d %s %d\n", nres,trackRes.getIntVal(i_hit), sensorName,i_hit);
                    System.out.printf("Track uresiduals: %s %.5f %.5f\n",sensorName, trackRes.getDoubleVal(i_hit),trackRes.getFloatVal(i_hit));
                }
                aidaGBL.histogram1D("uresidual_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram1D("ureserror_GBL_" + sensorName).fill(trackRes.getFloatVal(i_hit));
                aidaGBL.histogram1D("ures_pull_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit) / trackRes.getFloatVal(i_hit));
            }
        }
    }//doGBLresiduals
    
    private void setupPlots() {
        
        for (SiSensor sensor : sensors) {

            HpsSiSensor sens = (HpsSiSensor) sensor.getGeometry().getDetectorElement();
            double xmax = 0.5;
            int nbins = 250;
            int l = (sens.getLayerNumber() + 1) / 2;
            if (l > 1) xmax = 0.05 + (l - 1) * 0.08;
            aidaGBL.histogram1D("residual_before_GBL_" + sensor.getName(), nbins, -1.0 * xmax, xmax);

            xmax = 0.250;
            if (l >= 6)
                xmax = 0.250;
            aidaGBL.histogram1D("residual_after_GBL_" + sensor.getName(),  nbins, -1.0 * xmax, xmax);
            aidaGBL.histogram1D("bresidual_GBL_" + sensor.getName(), nbins, -1.0 * xmax, xmax);
            aidaGBL.histogram1D("uresidual_GBL_" + sensor.getName(), nbins, -1.0 * xmax, xmax);
            aidaGBL.histogram1D("breserror_GBL_" + sensor.getName(), nbins, 0.0, 0.1);
            aidaGBL.histogram1D("ureserror_GBL_" + sensor.getName(), nbins, 0.0, 0.2);
            aidaGBL.histogram1D("bres_pull_GBL_" + sensor.getName(), nbins, -5, 5);
            aidaGBL.histogram1D("ures_pull_GBL_" + sensor.getName(), nbins, -5, 5);
            
            

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
            aidaGBL.histogram1D(kinkFolder+"lambda_kink_" + sensor.getName(), 50, -1.0 * xmax, xmax);
            aidaGBL.histogram1D(kinkFolder+"phi_kink_" + sensor.getName(), 50, -1.0 * xmax, xmax);
        }
        
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
        List<String> charges = new ArrayList<String>();
        charges.add("");
        charges.add("_pos");
        charges.add("_neg");
        
        int nbins_t = 100;
        
        //For momentum
        int nbins_p = 150;
        
        
        for (String vol : volumes) {
            for (String charge : charges) {
                
                
                //TH1Ds
                aidaGBL.histogram1D("d0"+vol+charge,nbins_t,-5.0,5.0);
                aidaGBL.histogram1D("z0"+vol+charge,nbins_t,-1.3,1.3);
                aidaGBL.histogram1D("phi"+vol+charge,nbins_t,-0.3,0.3);
                aidaGBL.histogram1D("tanLambda"+vol+charge,nbins_t,-0.2,0.2);
                aidaGBL.histogram1D("p"+vol+charge,nbins_p,0.,6.);
                                
                aidaGBL.histogram1D("Chi2"+vol+charge,nbins_t,0,50);
                aidaGBL.histogram1D("nHits"+vol+charge,14,0,14);
                aidaGBL.histogram1D("trk_extr_or_x"+vol+charge,nbins_t,-3,3);
                aidaGBL.histogram1D("trk_extr_or_y"+vol+charge,nbins_t,-3,3);
                aidaGBL.histogram1D("trk_extr_bs_x"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D("trk_extr_bs_y"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D("trk_extr_bs_x_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D("trk_extr_bs_y_rk"+vol+charge, 2*nbins_t, -3, 3);
                aidaGBL.histogram1D("d0_vs_bs_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D("z0_vs_bs_rk"+vol+charge, 2*nbins_t, -5, 5);
                
                
                //TH2Ds
                
                aidaGBL.histogram2D("d0_vs_phi"+vol+charge,nbins_t,-0.3,0.3,nbins_t,-5.0,5.0);
                //aidaGBL.histogram2D("d0_vs_phi_bs"+vol+charge,nbins_t,-5.0,5.0,nbins_t,-0.3,0.3);
                aidaGBL.histogram2D("d0_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("d0_vs_p"+vol+charge,  nbins_p,0.0,6.0,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("d0bs_vs_p"+vol+charge,nbins_p,0.0,6.0,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("z0_vs_p"+vol+charge,  nbins_p,0.0,6.0,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("z0bs_vs_p"+vol+charge,nbins_p,0.0,6.0,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("z0_vs_tanLambda"+vol+charge,  nbins_t,-0.2,0.2,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D("z0bs_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_t,-5.0,5.0);
                
            }//charge loop
        }//vol loop
        
        




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
