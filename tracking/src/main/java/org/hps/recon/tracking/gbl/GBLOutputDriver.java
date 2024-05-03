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
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import hep.aida.IManagedObject;
import hep.aida.IBaseHistogram;

import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.FieldMap;


// E/p plots
import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackState;
//Fiducial cuts on the calorimeter cluster
import org.hps.record.triggerbank.TriggerModule;

/**
 * Make post-GBL plots needed for alignment.
 */
public class GBLOutputDriver extends Driver {
    
    private AIDA aidaGBL; // era public
    private String outputPlots = "GBLplots_ali.root";
    private String trackCollectionName = "GBLTracks";
    private String inputCollectionName = "FinalStateParticles_KF";
    private String trackResidualsRelColName = "TrackResidualsGBLRelations";
    private String dataRelationCollection = GBLKinkData.DATA_RELATION_COLLECTION;
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;
    public boolean debug = false;
    private double chi2Cut = 99999;
    String kinkFolder = "/gbl_kinks/";
    String epullFolder = "/err_pulls/";
    String trkpFolder = "/trk_params/";
    String trkpDetailFolder="/trk_detail/";
    String resFolder="/res/";
    String hitFolder="/hit/";
    String eopFolder = "/EoP/";
    private boolean b_doGBLkinks     = false;
    private boolean b_doGBLresiduals = true;
    private boolean b_doDetailPlots  = false;

    //This should be moved to the GBL Refitter!!!
    //The field map for extrapolation
    private FieldMap bFieldMap;

    //The location of the extrapolation
    private double bsZ = 0.;

    //Spacing between top and bottom in the 2D histos
    private int mod = 5;
    
    private double minMom = 1.;
    private double maxMom = 6.;

    private double minPhi = -999.9;
    private double maxPhi = 999.9;

    private double minTanL = 0.015;
    private double maxTanL = 999.9;
    
    private int nHits = 6;


    private boolean useParticles = true;
    

    public void setUseParticles(boolean val) {
        useParticles = val;
    }
    
    public void setDataRelationCollection (String val) {
        dataRelationCollection = val;
    }

    public void setNHits (int val ) {
        nHits = val;
    }
    
    public void setMinMom (double val) {
        minMom = val;
    }

    public void setMaxMom (double val) {
        maxMom = val;
    }

    public void setMinPhi (double val) {
        minPhi = val;
    }

    public void setMaxPhi (double val) {
        maxPhi = val;
    }

    public void setMinTanL (double val) {
        minTanL = val;
    }

    public void setMaxTanL (double val) {
        maxTanL = val;
    }


    //Override the Z of the target.
    public void setBsZ (double input) {
        bsZ = input;
    }

    public void setDoGBLresiduals (boolean input) {
        b_doGBLresiduals = input;
    }
    
    public void setDoGBLkinks (boolean input) {
        b_doGBLkinks = input;
    }

    public void setTrackResidualsRelColName (String val) {
        trackResidualsRelColName = val;
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

    public void setInputCollectionName(String val) {
        inputCollectionName=val;
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

        if (trackCollectionName.contains("Kalman") || trackCollectionName.contains("KF")) { 
            
            kinkFolder  = "/kf_kinks/";
            epullFolder = "/kf_err_pulls/";
            trkpFolder  = "/kf_trk_params/";
            trkpDetailFolder = "/kf_trk_detail/";
            resFolder = "/kf_res/";
            hitFolder = "/kf_hit/";
        }



        setupPlots();
	setupEoPPlots();
    }

    @Override
    public void process(EventHeader event) {


	
	// Track Collection
	List<Track> tracks = new ArrayList<Track>();
	
	// Particle Collection
	List<ReconstructedParticle> particles = null;
	
	// Create a mapping of matched Tracks to corresponding Clusters. 
        HashMap<Track,Cluster> TrackClusterPairs = new HashMap<Track,Cluster>();
	
	if (!useParticles)
	    tracks = event.get(Track.class,trackCollectionName);
	else {
	    particles = event.get(ReconstructedParticle.class, inputCollectionName);
	    for (ReconstructedParticle particle : particles) {
                if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                    continue;
		Track track = particle.getTracks().get(0);
		Cluster cluster = particle.getClusters().get(0);
		tracks.add(track);
		TrackClusterPairs.put(track,cluster);
            }
	}
	
	
        /*
        System.out.print("GBLOutputDriver tracks (" + trackCollectionName + ") N hits: ");
        for (Track trk : tracks) {
          System.out.print(trk.getTrackerHits().size()+" ");
        }
        System.out.println();
         */

        int TrackType = 0;
        if (trackCollectionName.contains("Kalman") || trackCollectionName.contains("KF")) {
            TrackType = 1;
            //System.out.println("PF:: DEBUG :: Found Kalman Tracks in the event");

        }
        
        //System.out.println("Running on "+trackCollectionName);

        //RelationalTable trackMatchTable = null;
        //trackMatchTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        //List<LCRelation> trackMatchRelation = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
        //for (LCRelation relation : trackMatchRelation) {
        //    if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
        //        trackMatchTable.add(relation.getFrom(), relation.getTo());
        //    }
        //}

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        
        for (Track trk : tracks) {
            
            //Some Track selection
            
            //System.out.println("Track loop");
            
            if (trk.getChi2() > chi2Cut)
                continue;


            //System.out.println("Track passed chi2");
            
            //Remove tracks with less than 10 hits
            if ((TrackType == 0 && trk.getTrackerHits().size() < nHits) 
                || (TrackType == 1 && trk.getTrackerHits().size() < 2*nHits)) {
                //System.out.println("WARNING:: "+trk.getClass().getSimpleName()
                //        +" got to GBLOutputDriver with "+trk.getTrackerHits().size()+" hits"
                //        +" which is below the cut that should have been already applied.");
                continue;
            }

	    
            //System.out.println("Track passed hits");
	    Hep3Vector momentum = new BasicHep3Vector(trk.getTrackStates().get(0).getMomentum());
            
            if (momentum.magnitude() < minMom)
                continue;
            
            if (momentum.magnitude() > maxMom)
                continue;
	    
            //System.out.println("Track passed momentum");
            
            TrackState trackState = trk.getTrackStates().get(0);
            if (Math.abs(trackState.getTanLambda()) < minTanL)
                continue;

            if (Math.abs(trackState.getTanLambda()) > maxTanL)
                continue;
            
            if (Math.abs(trackState.getPhi()) < minPhi)
                continue;

            if (Math.abs(trackState.getPhi()) > maxPhi)
                continue;
            
            //System.out.println("Track passed tanLambda");
            
            GenericObject gblKink = GBLKinkData.getKinkData(event, trk);
            
            //if (gblKink == null) {
                //System.out.println("Failed finding gblKink object");
                //System.out.println("Looked for: "+GBLKinkData.DATA_RELATION_COLLECTION);
                //System.out.println("Event has "+GBLKinkData.DATA_RELATION_COLLECTION+" "+event.hasCollection(LCRelation.class, GBLKinkData.DATA_RELATION_COLLECTION));
            //}
            
	    //Track matchedTrack = (Track) trackMatchTable.from(trk);
            Map<HpsSiSensor, TrackerHit> sensorHits = new HashMap<HpsSiSensor, TrackerHit>();
            Map<HpsSiSensor, Integer> sensorNums    = new HashMap<HpsSiSensor, Integer>();
            List<TrackerHit> hitsOnTrack = new ArrayList<TrackerHit>();

            if (hitToStrips != null && hitToRotated != null)
                hitsOnTrack = TrackUtils.getStripHits(trk, hitToStrips, hitToRotated);
            
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
            

            //THIS IS TEMPORARY AND NEEDED FOR FIXING THE LOOP ON THE SENSORS ON TRACK FOR KALMAN TRACKS
            
            if (hitsOnTrack.size() == 0) {
                for (TrackerHit hit : trk.getTrackerHits()) {
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
                
            }
            
            doBasicGBLtrack(trk,sensorHits);
            if (b_doGBLresiduals) 
                doGBLresiduals(trk, sensorHits,event);
            
            //doMTresiduals(matchedTrack, sensorHits);
            if (b_doGBLkinks)
                doGBLkinks(trk,gblKink, sensorNums);



	    if (useParticles)
		doEoPPlots(trk,TrackClusterPairs.get(trk));
	    
	    
        }
    }
    
    private void doEoPPlots(Track track, Cluster cluster) {

	double energy = cluster.getEnergy();
	double[] trk_prms = track.getTrackParameters();
	double tanL = trk_prms[BaseTrack.TANLAMBDA];
	double phi  = trk_prms[BaseTrack.PHI];
	TrackState trackState = track.getTrackStates().get(0);
	double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
	double eop = energy / trackp;

	String vol = tanL > 0 ? "top" : "bottom";

	//Charge sign is flipped
	String charge = track.getCharge() > 0 ? "ele" : "pos";

	
	aidaGBL.histogram1D(eopFolder+"Ecluster_"+vol).fill(energy);
	aidaGBL.histogram1D(eopFolder+"EoP_"+vol).fill(eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_"+vol).fill(phi,eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_"+vol).fill(trackp,eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_"+vol).fill(tanL,eop);


	aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_"+charge+"_"+vol).fill(trackp,eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_"+charge+"_"+vol).fill(tanL,eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_"+charge+"_"+vol).fill(phi,eop);
	
	aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda").fill(tanL,eop);
	aidaGBL.histogram2D(eopFolder+"EoP_vs_phi").fill(tanL,eop);
	aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi").fill(tanL,
								   phi,
								   eop);

	
	if (TriggerModule.inFiducialRegion(cluster)) {
	    
	    aidaGBL.histogram1D(eopFolder+"Ecluster_"+vol+"_fid").fill(energy);
	    aidaGBL.histogram1D(eopFolder+"EoP_"+vol+"_fid").fill(eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_"+vol+"_fid").fill(phi,eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_"+vol+"_fid").fill(trackp,eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_"+vol+"_fid").fill(tanL,eop);
	    

	    aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_"+charge+"_"+vol+"_fid").fill(trackp,eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_"+charge+"_"+vol+"_fid").fill(tanL,eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_"+charge+"_"+vol+"_fid").fill(phi,eop);
	    
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_fid").fill(tanL,eop);
	    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_fid").fill(tanL,eop);
	    aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid").fill(tanL,
									   phi,
									   eop);

	    
	}
	
	
    }

    
    

    private void doGBLkinks(Track trk, GenericObject kink, Map<HpsSiSensor, Integer> sensorNums) {
        
        if (kink == null) {
            System.out.println("WARNING::Kink object is null");
            return;
        }
            

        String vol  = "_top";
        int spacing = 0;
        if (trk.getTrackStates().get(0).getTanLambda() < 0) {
            vol = "_bottom";
            spacing  = sensors.size() / 2 + mod;
        }
        
        for (HpsSiSensor sensor : sensorNums.keySet()) {
            int index = sensorNums.get(sensor);
            double phi = kink.getDoubleVal(index);
            float lambda = kink.getFloatVal(index);
            
            //(2019) For top 0-20, for bottom 25-45
            aidaGBL.histogram2D(kinkFolder+"lambda_kink_mod").fill(sensor.getMillepedeId()+spacing,lambda);
            aidaGBL.profile1D(kinkFolder+"lambda_kink_mod_p").fill(sensor.getMillepedeId()+spacing,lambda);
            aidaGBL.histogram2D(kinkFolder+"phi_kink_mod").fill(sensor.getMillepedeId()+spacing,phi);
            aidaGBL.profile1D(kinkFolder+"phi_kink_mod_p").fill(sensor.getMillepedeId()+spacing,phi);
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

            aidaGBL.histogram1D(resFolder+"residual_before_GBL_" + sensor.getName()).fill(diff.x());
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

    private void FillGBLTrackPlot(String str, String isTop, String charge, double valX, double valY, double valZ) {
        aidaGBL.histogram3D(str+isTop).fill(valX,valY,valZ);
        aidaGBL.histogram3D(str+isTop+charge).fill(valX,valY,valZ);
    }
    


    private void doBasicGBLtrack(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        
        TrackState trackState = trk.getTrackStates().get(0);

        String isTop = "_bottom";
        //if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
        //  isTop = "_top";
        //}

        //if (trk.getType()==1 && trk.getTrackerHits().size() < 10) {
        //    return;
        //}

        //List<Integer> missingHits; 
        //missingHits =  findMissingLayer(trk);
        
        if (trackState.getTanLambda() > 0) {
            isTop = "_top";
        }
                
        //There is a sign flip in the charge
        String charge = "_pos";
        if (trk.getCharge()>0)
            charge = "_neg";
        
        
        //Hep3Vector mom = new BasicHep3Vector(trackState.getMomentum());
        //System.out.println("Track momentum " + mom.toString());
        double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
        
        
        FillGBLTrackPlot(trkpFolder+"d0",isTop,charge,trackState.getD0());
        FillGBLTrackPlot(trkpFolder+"z0",isTop,charge,trackState.getZ0());
        FillGBLTrackPlot(trkpFolder+"phi",isTop,charge,trackState.getPhi());
        FillGBLTrackPlot(trkpFolder+"tanLambda",isTop,charge,trackState.getTanLambda());
        FillGBLTrackPlot(trkpFolder+"p",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==7)
            FillGBLTrackPlot(trkpFolder+"p7h",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==6)
            FillGBLTrackPlot(trkpFolder+"p6h",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==5)
            FillGBLTrackPlot(trkpFolder+"p5h",isTop,charge,trackp);
        
        if (TrackUtils.isHoleTrack(trk)) 
            FillGBLTrackPlot(trkpFolder+"p_hole",isTop,charge,trackp);
        else 
            FillGBLTrackPlot(trkpFolder+"p_slot",isTop,charge,trackp);
        
        
        //Momentum maps
        FillGBLTrackPlot(trkpFolder+"p_vs_phi",isTop,charge,trackState.getPhi(),trackp);
        FillGBLTrackPlot(trkpFolder+"p_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackp);
        FillGBLTrackPlot(trkpFolder+"p_vs_phi_tanLambda",isTop,charge,trackState.getPhi(),trackState.getTanLambda(),trackp);
        
        double tanLambda = trackState.getTanLambda();
        double cosLambda = 1. / (Math.sqrt(1+tanLambda*tanLambda));
        
        FillGBLTrackPlot(trkpFolder+"pT_vs_phi",isTop,charge,trackState.getPhi(),trackp*cosLambda);
        FillGBLTrackPlot(trkpFolder+"pT_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackp*cosLambda);
        
        
        //if (trk.getTrackerHits().size()==6)
        //    FillGBLTrackPlot(trkpFolder+"p_Missing1Hit",isTop,charge,missingHits.get(0),trackp);
        
        //if (missingHits.size()==1 && missingHits.get(0)==7) 
        //    FillGBLTrackPlot(trkpFolder+"p_MissingLastLayer",isTop,charge,trackp);
        
        
        FillGBLTrackPlot(trkpFolder+"Chi2",isTop,charge,trk.getChi2());
        FillGBLTrackPlot(trkpFolder+"Chi2_vs_p",isTop,charge,trackp,trk.getChi2());

        // deduce multiplication factor for ST-started GBL tracks
        int nhits = trk.getTrackerHits().size();
        if (nhits > 0 && trk.getTrackerHits().get(0) instanceof HelicalTrackCross) {
            // tracks created from cross hits have 2 measurments per hit instead of only
            // one so we have to double that count for an equal comparison
            nhits *= 2;
        }
        aidaGBL.histogram1D(trkpFolder+"nHits" + isTop).fill(nhits);
        aidaGBL.histogram1D(trkpFolder+"nHits" + isTop+charge).fill(nhits);

        Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        if (debug)
            System.out.printf("beamspot %s transformed %s \n", beamspot.toString());
        FillGBLTrackPlot(trkpFolder+"trk_extr_or_x",isTop,charge,beamspot.x());
        FillGBLTrackPlot(trkpFolder+"trk_extr_or_y",isTop,charge,beamspot.y());
        
        //Extrapolation to assumed tgt pos - helix
        Hep3Vector trkTgt = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState,bsZ));
        FillGBLTrackPlot(trkpFolder+"trk_extr_bs_x",isTop,charge,trkTgt.x());
        FillGBLTrackPlot(trkpFolder+"trk_extr_bs_y",isTop,charge,trkTgt.y());
        
        //Transform z to the beamspot plane
        //Get the PathToPlane
        
        BaseTrackState ts_bs = TrackUtils.getTrackExtrapAtVtxSurfRK(trackState,bFieldMap,0.,bsZ);
        
        
        //Get the track parameters wrt the beamline using helix
        double [] beamLine = new double [] {bsZ,0};
        double [] helixParametersAtBS = TrackUtils.getParametersAtNewRefPoint(beamLine, trackState);

                  
        FillGBLTrackPlot(trkpFolder+"trk_extr_bs_x_rk",isTop,charge,ts_bs.getReferencePoint()[1]);
        FillGBLTrackPlot(trkpFolder+"trk_extr_bs_y_rk",isTop,charge,ts_bs.getReferencePoint()[2]);

        //Ill defined - should be defined wrt bsX and bsY
        FillGBLTrackPlot(trkpFolder+"d0_vs_bs_rk",isTop,charge,ts_bs.getD0());
        FillGBLTrackPlot(trkpFolder+"d0_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.D0]);
        
        double s = HelixUtils.PathToXPlane(TrackUtils.getHTF(trackState),bsZ,0.,0).get(0);
        FillGBLTrackPlot(trkpFolder+"z0_vs_bs",isTop,charge,trackState.getZ0() + s*trackState.getTanLambda());
        FillGBLTrackPlot(trkpFolder+"z0_vs_bs_rk",isTop,charge,ts_bs.getZ0());
        FillGBLTrackPlot(trkpFolder+"z0_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.Z0]);
        

        FillGBLTrackPlot(trkpFolder+"phi_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.PHI]);
        
        //TH2D - Filling
        FillGBLTrackPlot(trkpFolder+"d0_vs_phi",isTop,charge,trackState.getPhi(),trackState.getD0());
        FillGBLTrackPlot(trkpFolder+"d0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getD0());
        FillGBLTrackPlot(trkpFolder+"d0_vs_p",isTop,charge,trackp,trackState.getD0());
        
        //Ill defined - should be defined wrt bsX and bsY
        FillGBLTrackPlot(trkpFolder+"d0bs_vs_p",isTop,charge,trackp,helixParametersAtBS[BaseTrack.D0]);
        
        FillGBLTrackPlot(trkpFolder+"z0_vs_p",isTop,charge,trackp,trackState.getZ0()); 
        FillGBLTrackPlot(trkpFolder+"z0bs_vs_p",isTop,charge,trackp,ts_bs.getZ0()); 
        
        //Interesting plot to get a sense where z-vtx is. 
        //If z0 is referenced to the right BS z location, the slope of <z0> vs tanLambda is 0
        FillGBLTrackPlot(trkpFolder+"z0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getZ0());
        FillGBLTrackPlot(trkpFolder+"z0bs_vs_tanLambda",isTop,charge,trackState.getTanLambda(),ts_bs.getZ0());
        
        
        if (b_doDetailPlots) {
            int ibins = 15;
            double start= -12;
            double end = -5;
            double step = (end-start) / (double)ibins;
            
            for (int ibin = 0; ibin<ibins;ibin++) {
                double bslocZ = start+step*ibin;
                double s_bslocZ = HelixUtils.PathToXPlane(TrackUtils.getHTF(trackState),bslocZ,0.,0).get(0);
                double z0Corr = trackState.getZ0() + s_bslocZ*trackState.getTanLambda();
                String ibinstr =  String.valueOf(ibin);
                aidaGBL.histogram2D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_"+ibinstr+isTop).fill(trackState.getTanLambda(),z0Corr);
                aidaGBL.profile1D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_p_"+ibinstr+isTop).fill(trackState.getTanLambda(),z0Corr);
                //System.out.printf("bslocZ %.5f s_bslocZ = %.5f z0C0rr %.5f \n", bslocZ, s_bslocZ, z0Corr);
                //aidaGBL.histogram3D(trkpDetailFolder+"z0_vs_tanLambda_bsZ"+isTop).fill(bslocZ,trackState.getTanLambda(),z0Corr);
                //aidaGBL.profile2D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_p"+isTop).fill(bslocZ,trackState.getTanLambda(),z0Corr);
            }        
        }
    }
    
    private void doGBLresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits, EventHeader event) {
        
        Map<Integer,HpsSiSensor> sensorMPIDs   = new HashMap<Integer,HpsSiSensor>();
        double trackTime = 0.;
        double trackTimeSD = 0.;
        

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
            
            aidaGBL.histogram2D(hitFolder+"hit_u_vs_v_sensor_frame_" + sensor.getName()).fill(hitTrackPosSensor.y(), hitTrackPosSensor.x());
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
            aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            // select track charge
            if(trk.getCharge()>0) {
                aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_pos_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            }else if(trk.getCharge()<0) {
                aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_neg_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
            }
            
            // post-GBL residual
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector hitPosSensor = new BasicHep3Vector(hitPos.v());
            trans.transform(hitPosSensor);
            Hep3Vector resSensor = VecOp.sub(hitPosSensor, extrapPosSensor);
            aidaGBL.histogram2D(resFolder+"residual_after_GBL_vs_v_predicted_" + sensor.getName()).fill(extrapPosSensor.y(), resSensor.x());
            aidaGBL.histogram2D(resFolder+"residual_after_GBL_vs_u_hit_" + sensor.getName()).fill(hitPosSensor.x(), resSensor.x());
            aidaGBL.histogram1D(resFolder+"residual_after_GBL_" + sensor.getName()).fill(resSensor.x());


            trackTime += sensorHits.get(sensor).getTime();
            

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

        
        trackTime /= (float)sensorHits.size();
        
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            trackTimeSD += Math.pow(trackTime - sensorHits.get(sensor).getTime(),2);
        }
        
        trackTimeSD = Math.sqrt(trackTimeSD / ((float) sensorHits.size() - 1.));
        

        
        RelationalTable trackResidualsTable = null;
        if (event.hasCollection(LCRelation.class, trackResidualsRelColName)) {
            trackResidualsTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trackresRelation = event.get(LCRelation.class, trackResidualsRelColName);
            for (LCRelation relation : trackresRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    trackResidualsTable.add(relation.getFrom(), relation.getTo());
                }
            }
        } else {
            //System.out.println("null TrackResidualsGBL Data Relations.");
            //Failed finding TrackResidualsGBL
            return;
        }
        
        GenericObject trackRes = (GenericObject) trackResidualsTable.from(trk);
        if (trackRes == null) {
            //System.out.println("null TrackResidualsGBL Data.");
            return;
        }
        
        //it's bias-unbias-bias-unbias-bias-unbias....
        //TODO add in trackRes the number of hits on tracks ?
        int nres = (trackRes.getNInt()-1);
        
        String vol = "_top";
        if (trk.getTrackStates().get(0).getTanLambda() < 0)
            vol = "_bottom";

        //get the bias first 
        for (int i_hit =0; i_hit <= nres-1 ; i_hit+=2) {
            if (trackRes.getIntVal(i_hit)!=-999)  {
                //System.out.println("PF::DEBUG:: "+ trackCollectionName+ " trackRes.getIntVal(i_hit) " + trackRes.getIntVal(i_hit));
                String sensorName = (sensorMPIDs.get(trackRes.getIntVal(i_hit))).getName();
                if (debug) {
                    //System.out.printf("NHits %d MPID sensor:%d %s %d\n", nres,trackRes.getIntVal(i_hit), sensorName,i_hit);
                    //System.out.printf("Track residuals: %s %.5f %.5f\n",sensorName, trackRes.getDoubleVal(i_hit),trackRes.getFloatVal(i_hit));
                }
                //General residuals Per volume
                aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol).fill(trackRes.getDoubleVal(i_hit));
                                
                if (trackRes.getIntVal(i_hit) < 9) 
                    //L1L4 
                    aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol+"_L1L4").fill(trackRes.getDoubleVal(i_hit));
                else 
                    //L5L7
                    aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol+"_L5L7").fill(trackRes.getDoubleVal(i_hit));
                
                //Top go from 0 to 20, bottom go from 25 to 45
                int spacing = 0;
                if (vol == "_bottom")
                    spacing = sensors.size() / 2 + mod;
                
                aidaGBL.histogram2D(resFolder  +"bresidual_GBL_mod").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
                aidaGBL.profile1D(resFolder  +"bresidual_GBL_mod_p").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
                
                aidaGBL.histogram1D(resFolder  +"bresidual_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram1D(epullFolder+"breserror_GBL_" + sensorName).fill(trackRes.getFloatVal(i_hit));
                aidaGBL.histogram1D(epullFolder+"bres_pull_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit) / trackRes.getFloatVal(i_hit));
            }
            else {
                System.out.printf("Track refit failed? No biased residual for %d\n", i_hit);
            }
        }
        // get the unbias
        for (int i_hit =1; i_hit <= nres-1 ; i_hit+=2) {
            if (trackRes.getIntVal(i_hit)!=-999)  {  


                //Measured hit
                HpsSiSensor hps_sensor = sensorMPIDs.get(trackRes.getIntVal(i_hit));
                Hep3Vector hitPosG = new BasicHep3Vector(sensorHits.get(hps_sensor).getPosition());
                Hep3Vector hitPosSensorG = new BasicHep3Vector(hitPosG.v());
                ITransform3D g2l = hps_sensor.getGeometry().getGlobalToLocal();
                g2l.transform(hitPosSensorG);
                String sensorName = (sensorMPIDs.get(trackRes.getIntVal(i_hit))).getName();

                //Predicted hit
                Hep3Vector extrapPos = null;
                Hep3Vector extrapPosSensor = null;
                extrapPos = TrackUtils.extrapolateTrackPositionToSensor(trk, hps_sensor, sensors, bfield);
                if (extrapPos == null)
                    continue;
                extrapPosSensor = new BasicHep3Vector(extrapPos.v());
                g2l.transform(extrapPosSensor);
                
                if (debug) {
                    System.out.printf("NHits %d MPID sensor:%d %s %d\n", nres,trackRes.getIntVal(i_hit), sensorName,i_hit);
                    System.out.printf("Track uresiduals: %s %.5f %.5f\n",sensorName, trackRes.getDoubleVal(i_hit),trackRes.getFloatVal(i_hit));
                }

                //General residuals Per volume
                aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol).fill(trackRes.getDoubleVal(i_hit));
                
                if (trackRes.getIntVal(i_hit) < 9) 
                    //L1L4 
                    aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol+"_L1L4").fill(trackRes.getDoubleVal(i_hit));
                else 
                    //L5L7
                    aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol+"_L5L7").fill(trackRes.getDoubleVal(i_hit));
                
                
                //Top go from 0 to 20, bottom go from 25 to 45
                int spacing = 0;
                if (vol == "_bottom")
                    spacing = sensors.size()/2 + mod;
                
                aidaGBL.histogram2D(resFolder+"uresidual_GBL_mod").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
                aidaGBL.profile1D(resFolder+"uresidual_GBL_mod_p").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram1D(resFolder+"uresidual_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_u_hit_" + sensorName).fill(hitPosSensorG.x(),trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_v_pred_" + sensorName).fill(extrapPosSensor.y(),trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram1D(epullFolder+"ureserror_GBL_" + sensorName).fill(trackRes.getFloatVal(i_hit));
                aidaGBL.histogram1D(epullFolder+"ures_pull_GBL_" + sensorName).fill(trackRes.getDoubleVal(i_hit) / trackRes.getFloatVal(i_hit));
                
                //Get the hit time
                double hitTime = sensorHits.get(hps_sensor).getTime();
                
                //Get the track time (it's the average of hits-on-track time)
                
                double dT_hit_track  =  hitTime - trackTime;
                double dT_hit_sigma  = (hitTime - trackTime) / trackTimeSD;
                
                aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_dT_hit_"+sensorName).fill(dT_hit_track,trackRes.getDoubleVal(i_hit));
                aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_dTs_hit_"+sensorName).fill(dT_hit_sigma,trackRes.getDoubleVal(i_hit));
                
                


            }
            else {
                if (debug){
                    System.out.printf("Track refit failed? No biased residual for %d\n", i_hit);
                }
            }
        }
    }//doGBLresiduals
    
    private List<Integer> findMissingLayer(Track trk) {
        
        List<Integer> layers = new ArrayList<Integer>();
        layers.add(1);
        layers.add(2);
        layers.add(3);
        layers.add(4);
        layers.add(5);
        layers.add(6);
        layers.add(7);
        
        List<Integer> LayersOnTrack = new ArrayList<Integer>();
        List<Integer> missingHits   = new ArrayList<Integer>();
        
        for (TrackerHit hit : trk.getTrackerHits()) {
            
            int stripLayer = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
            
            int hpslayer = (stripLayer + 1 ) / 2;
            LayersOnTrack.add(hpslayer);
        }

        for (Integer layer : layers) {
            if (!LayersOnTrack.contains(layer))
                missingHits.add(layer);
        }
        
        return missingHits;
    }
    
    private void setupEoPPlots() {
	
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
	
        List<String> charges = new ArrayList<String>();
        charges.add("");
        charges.add("_ele");
        charges.add("_pos");
        
        for (String vol : volumes) {
	    
            aidaGBL.histogram1D(eopFolder+"Ecluster"+vol,200,0,6);
            aidaGBL.histogram1D(eopFolder+"EoP"+vol,200,0,2);
            
            double lmin = 0.;
            double lmax = 0.08;
            if (vol == "_bot") {
                lmin = -0.08;
                lmax = 0.;
            }
	    
            for (String charge : charges) {
                aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol,200,0,6,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol,200,lmin,lmax,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol,200,-0.2,0.2,200,0,2);
            }
                        
            aidaGBL.histogram1D(eopFolder+"Ecluster"+vol+"_fid",200,0,5);
            aidaGBL.histogram1D(eopFolder+"EoP"+vol+"_fid",200,0,2);
            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+vol+"_fid",200,0,6,200,0,2);
	    
            for (String charge : charges) {
                aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol+"_fid",200,0,6,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol+"_fid",200,0.01,0.08,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol+"_fid",200,-0.2,0.2,200,0,2);
            }
        }
        
        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda",200,-0.1,0.1,200,0,2);
        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi",200,-0.2,0.2,200,0,2);
        aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi",200,-0.08,0.08,200,-0.2,0.2,200,0,2);
	
        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_fid",200,-0.1,0.1,200,0,2);
        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_fid",200,-0.2,0.2,200,0,2);
        aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid",200,-0.08,0.08,200,-0.2,0.2,200,0,2);
	
    }

    
    private void setupPlots() {
        

        double xmax = 0.25;
        double kxmax = 0.001;
        
        int nbins = 250;
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
        int mod_2dplot_bins = sensors.size()+mod*2; 
        
        for (String vol : volumes) {
            aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol,nbins, -xmax, xmax);
            aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol,nbins, -xmax, xmax);
            aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol+"_L1L4",nbins,-xmax,xmax);
            aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol+"_L1L4",nbins,-xmax,xmax);
            aidaGBL.histogram1D(resFolder+"bresidual_GBL"+vol+"_L5L7",nbins,-xmax,xmax);
            aidaGBL.histogram1D(resFolder+"uresidual_GBL"+vol+"_L5L7",nbins,-xmax,xmax);

        }
        
        //res/kinks TH2D
        //5 empty bins to distinguish between top and bottom
        
        aidaGBL.histogram2D(resFolder+"bresidual_GBL_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5, nbins, -xmax,xmax);
        aidaGBL.profile1D(resFolder+"bresidual_GBL_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
        aidaGBL.histogram2D(resFolder+"uresidual_GBL_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5, 400, -0.4,0.4);
        aidaGBL.profile1D(resFolder+"uresidual_GBL_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
            
        
        //Hits vs channel
        int nch  = 400;
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL1b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL2b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL3b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL4b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL5b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL6b",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL7b",nch,0,nch,nch,0,nch);
        
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL1t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL2t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL3t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL4t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL5t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL6t",nch,0,nch,nch,0,nch);
        aidaGBL.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL7t",nch,0,nch,nch,0,nch);
            
        

        for (SiSensor sensor : sensors) {

            HpsSiSensor sens = (HpsSiSensor) sensor.getGeometry().getDetectorElement();
            xmax = 0.5;
            nbins = 250;
            int l = (sens.getLayerNumber() + 1) / 2;
            if (l > 1) xmax = 0.05 + (l - 1) * 0.08;
            aidaGBL.histogram1D(resFolder+"residual_before_GBL_" + sensor.getName(), nbins, -xmax, xmax);
            
            xmax = 0.250;
            
            if (l >= 6)
                xmax = 0.250;
            aidaGBL.histogram1D(resFolder+"residual_after_GBL_" + sensor.getName(),  nbins, -xmax, xmax);
            aidaGBL.histogram1D(resFolder+"bresidual_GBL_" + sensor.getName(), nbins, -xmax, xmax);
            aidaGBL.histogram1D(resFolder+"uresidual_GBL_" + sensor.getName(), nbins, -xmax, xmax);
            aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_u_hit_" + sensor.getName(),100,-20.0,20.0,100,-0.1,0.1);
            aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_v_pred_" + sensor.getName(),300,-60.0,60.0,100,-0.1,0.1);
            aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_dT_hit_" + sensor.getName(),100,-10.0,10.0,100,-0.1,0.1);
            aidaGBL.histogram2D(resFolder+"uresidual_GBL_vs_dTs_hit_" + sensor.getName(),100,-5.0,5.0,100,-0.1,0.1);

            
            aidaGBL.histogram1D(epullFolder+"breserror_GBL_" + sensor.getName(), nbins, 0.0, 0.1);
            aidaGBL.histogram1D(epullFolder+"ureserror_GBL_" + sensor.getName(), nbins, 0.0, 0.2);
            aidaGBL.histogram1D(epullFolder+"bres_pull_GBL_" + sensor.getName(), nbins, -5, 5);
            aidaGBL.histogram1D(epullFolder+"ures_pull_GBL_" + sensor.getName(), nbins, -5, 5);
            
            aidaGBL.histogram2D(resFolder+"residual_after_GBL_vs_u_hit_" + sensor.getName(), 100, -20.0, 20.0, 100, -0.04, 0.04);
            aidaGBL.histogram2D(resFolder+"residual_after_GBL_vs_v_predicted_" + sensor.getName(), 100, -55.0, 55.0, 100, -0.04, 0.04);
            aidaGBL.histogram2D(hitFolder+"hit_u_vs_v_sensor_frame_" + sensor.getName(), 300, -60.0, 60.0, 300, -25, 25);
            aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_pos_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            aidaGBL.histogram2D(hitFolder+"predicted_u_vs_v_neg_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
            
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
            aidaGBL.histogram1D(kinkFolder+"lambda_kink_" + sensor.getName(), 250, -xmax, xmax);
            aidaGBL.histogram1D(kinkFolder+"phi_kink_" + sensor.getName(), 250, -xmax, xmax);
        }
        
        aidaGBL.histogram2D(kinkFolder+"lambda_kink_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5,nbins,-0.001,0.001);
        aidaGBL.profile1D(kinkFolder+"lambda_kink_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
        aidaGBL.histogram2D(kinkFolder+"phi_kink_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5   ,nbins,-0.001,0.001);
        aidaGBL.profile1D(kinkFolder+"phi_kink_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
        
        List<String> charges = new ArrayList<String>();
        charges.add("");
        charges.add("_pos");
        charges.add("_neg");
        
        int nbins_t = 200;
        
        //For momentum
        int nbins_p = 150;
        double pmax = 8.;
        
        double z0max = 1;
        double d0max = 5;
        double z0bsmax = 0.2;
        
        for (String vol : volumes) {
            for (String charge : charges) {
                
                
                //TH1Ds
                aidaGBL.histogram1D(trkpFolder+"d0"+vol+charge,nbins_t,-5.0,5.0);
                aidaGBL.histogram1D(trkpFolder+"z0"+vol+charge,nbins_t,-1.3,1.3);
                aidaGBL.histogram1D(trkpFolder+"phi"+vol+charge,nbins_t,-0.06,0.06);
                aidaGBL.histogram1D(trkpFolder+"tanLambda"+vol+charge,nbins_t,-0.2,0.2);
                aidaGBL.histogram1D(trkpFolder+"p"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p7h"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p6h"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p5h"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p_MissingLastLayer"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p_hole"+vol+charge,nbins_p,0.,pmax);
                aidaGBL.histogram1D(trkpFolder+"p_slot"+vol+charge,nbins_p,0.,pmax);
                                
                aidaGBL.histogram1D(trkpFolder+"Chi2"+vol+charge,nbins_t*2,0,200);
                aidaGBL.histogram1D(trkpFolder+"nHits"+vol+charge,15,0,15);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_or_x"+vol+charge,nbins_t,-3,3);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_or_y"+vol+charge,nbins_t,-3,3);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_bs_x"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_bs_y"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_bs_x_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D(trkpFolder+"trk_extr_bs_y_rk"+vol+charge, 2*nbins_t, -3, 3);
                aidaGBL.histogram1D(trkpFolder+"d0_vs_bs_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D(trkpFolder+"d0_vs_bs_extrap"+vol+charge, 2*nbins_t, -5, 5);
                aidaGBL.histogram1D(trkpFolder+"z0_vs_bs_rk"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaGBL.histogram1D(trkpFolder+"z0_vs_bs_extrap"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaGBL.histogram1D(trkpFolder+"z0_vs_bs"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaGBL.histogram1D(trkpFolder+"phi_vs_bs_extrap"+vol+charge,2*nbins_t, -0.06,0.06);
                
                
                //TH2Ds
                
                aidaGBL.histogram2D(trkpFolder+"d0_vs_phi"+vol+charge,nbins_t,-0.3,0.3,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D(trkpFolder+"Chi2_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t*2,0,200);
                //aidaGBL.histogram2D("d0_vs_phi_bs"+vol+charge,nbins_t,-5.0,5.0,nbins_t,-0.3,0.3);
                aidaGBL.histogram2D(trkpFolder+"d0_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D(trkpFolder+"d0_vs_p"+vol+charge,  nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D(trkpFolder+"d0bs_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D(trkpFolder+"z0_vs_p"+vol+charge,  nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
                aidaGBL.histogram2D(trkpFolder+"z0bs_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t,-z0bsmax,z0bsmax);
                aidaGBL.histogram2D(trkpFolder+"z0_vs_tanLambda"+vol+charge,  nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
                aidaGBL.histogram2D(trkpFolder+"z0bs_vs_tanLambda"+vol+charge,nbins_t,-0.1,0.1,nbins_t,-z0bsmax,z0bsmax);

                aidaGBL.histogram2D(trkpFolder+"p_Missing1Hit"+vol+charge,8,0,8,nbins_p,0.0,pmax);
                aidaGBL.histogram2D(trkpFolder+"p_vs_phi"+vol+charge,   nbins_t,-0.3,0.3, nbins_p,0.,pmax);
                aidaGBL.histogram2D(trkpFolder+"p_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_p,0.,pmax);
                aidaGBL.histogram3D(trkpFolder+"p_vs_phi_tanLambda"+vol+charge, 50,-0.3,0.3,50,-0.2,0.2,100,0.,pmax);

                aidaGBL.histogram2D(trkpFolder+"pT_vs_phi"+vol+charge,   nbins_t,-0.3,0.3, nbins_p,0.,pmax);
                aidaGBL.histogram2D(trkpFolder+"pT_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_p,0.,pmax);
                                

                
                if (b_doDetailPlots) { 
                    //TH2Ds - detail
                    int ibins = 15;
                    double start= -12;
                    double end = -5;
                    double step = (end-start) / (double)ibins;
                    for (int ibin = 0; ibin<ibins;ibin++) {
                        String ibinstr =  String.valueOf(ibin);
                        aidaGBL.histogram2D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_"+ibinstr+vol,nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
                        aidaGBL.profile1D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_p_"+ibinstr+vol,nbins_t,-0.1,0.1);
                    }
                    //aidaGBL.histogram3D("z0_vs_tanLambda_bsZ"+vol,60,-12,-6,nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
                    //aidaGBL.profile2D("z0_vs_tanLambda_bsZ_p"+vol,60,-12,6,nbins_t,-0.1,0.1);
                }
            }//charge loop
        }//vol loop
    }
    
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aidaGBL.saveAs(outputPlots);

		/*
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
		*/
            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
