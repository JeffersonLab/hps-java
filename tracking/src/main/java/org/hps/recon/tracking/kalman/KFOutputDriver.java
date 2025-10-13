package org.hps.recon.tracking.kalman;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import org.hps.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
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
import org.lcsim.event.base.BaseTrackerHit;
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
 * Make post-KF plots 
 */
public class KFOutputDriver extends Driver {
    
    private AIDA aidaKF; // era public
    private String outputPlots = "KalmanTrackingPlots.root";
    private String trackCollectionName = "KalmanTracks";
    private String inputCollectionName = "FinalStateParticles_KF";
    private String trackResidualsRelColName = "KFUnbiasResRelations";
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String siHitsCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    //    private String dataRelationCollection = KFKinkData.DATA_RELATION_COLLECTION;
    List<LCRelation>  _fittedHits;
    List<SiTrackerHitStrip1D> _siClusters;
    private List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
    private double bfield;
    public boolean debug = false;
    private double chi2Cut = 99999;
    private double timeOffset=-40.0; //-40ns for MC ,-55 for data (2016 numbers) 
    //String kinkFolder = "/kf_kinks/";
    String epullFolder = "/err_pulls/";
    String trkpFolder = "/trk_params/";
    String trkpDetailFolder="/trk_detail/";
    String resFolder="/res/";
    String hitFolder="/hit/";
    String eopFolder = "/EoP/";
    //    private boolean b_doKFkinks     = false;
    private boolean b_doKFresiduals = false;
    private boolean b_doDetailPlots  = false;
    private boolean b_doRawHitPlots = false;
    private boolean b_doBig2DPlots = false;
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
    
    private int nHits = 10;

    private boolean useParticles = false;

    private Pair<Double,Double> _trkTimeSigma; 

    public void setDebug(boolean val) {
	debug = val;
    }

    public void setTimeOffset(double val){
	timeOffset=val;
    }
    
    public void setUseParticles(boolean val) {
        useParticles = val;
    }
    /*    
    public void setDataRelationCollection (String val) {
        dataRelationCollection = val;
    }
    */
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

    public void setDoKFresiduals (boolean input) {
        b_doKFresiduals = input;
    }
    
    //    public void setDoKFkinks (boolean input) {
    //    b_doKFkinks = input;
    // }

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
        if (aidaKF == null)
            aidaKF = AIDA.defaultInstance();

        aidaKF.tree().cd("/");

        for (HpsSiSensor s : detector.getDetectorElement().findDescendants(HpsSiSensor.class)) {
            if (s.getName().startsWith("module_") && s.getName().endsWith("sensor0")) {
                sensors.add(s);
            }
        }
                
        
        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        this.bfield = Math.abs(fieldInTracker.y());

        bFieldMap = detector.getFieldMap();

        if (trackCollectionName.contains("Kalman") || trackCollectionName.contains("KF")) { 
            
	    //            kinkFolder  = "/kf_kinks/";
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
	if(b_doRawHitPlots){
	    // Get the list of fitted hits from the event
	    _fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
	    _siClusters=event.get(SiTrackerHitStrip1D.class, siHitsCollectionName);
	    this.mapFittedRawHits(_fittedHits);	    
	}
	int TrackType = 0;
	if (!useParticles) {
	    if (debug)
		System.out.println("PF:: DEBUG :: NOT Using particles" + trackCollectionName);
	    if (trackCollectionName.contains("Kalman") || trackCollectionName.contains("KF")) {
		TrackType = 1;
	    }
	}
	else {
	    if (debug)
		System.out.println("PF:: DEBUG :: Using particles" + inputCollectionName);
	    if (inputCollectionName.contains("Kalman") || inputCollectionName.contains("KF")) {
		
		TrackType = 1 ;
	    }
	
	}
        if (debug)
	    System.out.println("PF:: DEBUG :: Track Type=" + TrackType);

	
	if (!useParticles)
	    tracks = event.get(Track.class,trackCollectionName);
	else {
	    particles = event.get(ReconstructedParticle.class, inputCollectionName);
	    for (ReconstructedParticle particle : particles) {
		//this requires track cluster match
		if(debug)
		    System.out.println(this.getClass().getName()+":: from ReconParticle found "+particle.getTracks().size()+" tracks and "+
				       particle.getClusters().size()+" clusters "); 
                if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                    continue;
		Track track = particle.getTracks().get(0);
		Cluster cluster = particle.getClusters().get(0);
		if(debug)
		    System.out.println(this.getClass().getName()+":: adding track and cluster to lists"); 
		tracks.add(track);
		TrackClusterPairs.put(track,cluster);
	    }
	}	    

	int nTracks=tracks.size();
	if(debug)
	    System.out.println(this.getClass()+":: found "+nTracks + " tracks");
	aidaKF.histogram1D(trkpFolder+"nTracks").fill(nTracks);
	RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        
        for (Track trk : tracks) {
	    if(debug) System.out.println(this.getClass().getName()+"::  track chi2 = "+trk.getChi2());
	    if(debug) System.out.println(this.getClass().getName()+"::  track nHits = "+trk.getTrackerHits().size());
            if (trk.getChi2() > chi2Cut)
                continue;

            if (trk.getTrackerHits().size() < nHits) 
		continue;
            

	    if(debug)
		System.out.println("Track passed hits d0 = "+trk.getTrackStates().get(0).getD0());

	    if(debug)System.out.println(this.getClass().getName()+":: local B field = "+trk.getTrackStates().get(0).getBLocal()); 
	    //	    ((BaseTrackState)trk.getTrackStates().get(0)).computeMomentum(trk.getTrackStates().get(0).getBLocal()); 
	    Hep3Vector momentum = new BasicHep3Vector(trk.getTrackStates().get(0).getMomentum());	    
	    if(debug) System.out.println(this.getClass().getName()+"::  track momentum = "+momentum.magnitude()); 
            if (momentum.magnitude() < minMom)
                continue;
            
            if (momentum.magnitude() > maxMom)
                continue;
	    
            if(debug)
		System.out.println("Track passed momentum");
            
            TrackState trackState = trk.getTrackStates().get(0);
            if (Math.abs(trackState.getTanLambda()) < minTanL)
                continue;

            if (Math.abs(trackState.getTanLambda()) > maxTanL)
                continue;
            
            if (Math.abs(trackState.getPhi()) < minPhi)
                continue;

            if (Math.abs(trackState.getPhi()) > maxPhi)
                continue;
	    
	    if(debug)
		System.out.println("Track passed tanLambda");

	
            Map<HpsSiSensor, TrackerHit> sensorHits = new HashMap<HpsSiSensor, TrackerHit>();
           
	    for (TrackerHit hit : trk.getTrackerHits()) {
		HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement());
		if (sensor != null) {
		    if(debug){
			System.out.println(this.getClass().getName()+":: inserting hit on sensor = "+sensor.getName());
		    }
		    sensorHits.put(sensor, hit);
		}
		
		if (debug && sensor == null)
		    System.out.printf("TrackerHit null sensor %s \n", hit.toString());
	    }
            _trkTimeSigma=getTrackTime(sensorHits);
	    doBasicKFtrack(trk,sensorHits);
            if (b_doKFresiduals) 
                doKFresiduals(trk, sensorHits,event);
            
	    if (useParticles)
		doEoPPlots(trk,TrackClusterPairs.get(trk));
	    if (b_doRawHitPlots)
		doAllHits();
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

	
	aidaKF.histogram1D(eopFolder+"Ecluster_"+vol).fill(energy);
	aidaKF.histogram1D(eopFolder+"EoP_"+vol).fill(eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_phi_"+vol).fill(phi,eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_trackP_"+vol).fill(trackp,eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_"+vol).fill(tanL,eop);
	Double trackTime = _trkTimeSigma.getFirstElement();
	double trkCluTime=trackTime-cluster.getCalorimeterHits().get(0).getTime()-timeOffset;
	aidaKF.histogram1D(trkpFolder+"trk-cluTime_"+charge+"_"+vol).fill(trkCluTime);
	aidaKF.histogram1D(trkpFolder+"trk-cluTime_"+vol).fill(trkCluTime);
	
	aidaKF.histogram2D(eopFolder+"EoP_vs_trackP_"+charge+"_"+vol).fill(trackp,eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_"+charge+"_"+vol).fill(tanL,eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_phi_"+charge+"_"+vol).fill(phi,eop);
	
	aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda").fill(tanL,eop);
	aidaKF.histogram2D(eopFolder+"EoP_vs_phi").fill(phi,eop);
	
	//	aidaKF.histogram3D(eopFolder+"EoP_vs_tanLambda_phi").fill(tanL,
	//							   phi,
	//							   eop);

	
	if (TriggerModule.inFiducialRegion(cluster)) {
	    
	    aidaKF.histogram1D(eopFolder+"Ecluster_"+vol+"_fid").fill(energy);
	    aidaKF.histogram1D(eopFolder+"EoP_"+vol+"_fid").fill(eop);
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(eopFolder+"EoP_vs_phi_"+vol+"_fid").fill(phi,eop);
		aidaKF.histogram2D(eopFolder+"EoP_vs_trackP_"+vol+"_fid").fill(trackp,eop);
		aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_"+vol+"_fid").fill(tanL,eop);
	    

		aidaKF.histogram2D(eopFolder+"EoP_vs_trackP_"+charge+"_"+vol+"_fid").fill(trackp,eop);
		aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_"+charge+"_"+vol+"_fid").fill(tanL,eop);
		aidaKF.histogram2D(eopFolder+"EoP_vs_phi_"+charge+"_"+vol+"_fid").fill(phi,eop);
		
		aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_fid").fill(tanL,eop);
		aidaKF.histogram2D(eopFolder+"EoP_vs_phi_fid").fill(phi,eop);
	    }
	    //	    aidaKF.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid").fill(tanL,
	    //phi,
	    //eop);

	    
	    
	    // Cluster positions
	    
	    double clusterX  = cluster.getPosition()[0];
	    double clusterY  = cluster.getPosition()[1];
	    TrackState ts_ecal = TrackUtils.getTrackStateAtECal(track);
	    
	    if(ts_ecal == null){
		return;
	    }
	    
	    double[] ts_ecalPos = ts_ecal.getReferencePoint();
	    double trkX = ts_ecalPos[1];
	    double trkY = ts_ecalPos[2];
	    
	    aidaKF.histogram1D(eopFolder+"Xcluster_"+vol+"_fid").fill(clusterX);
	    aidaKF.histogram1D(eopFolder+"Ycluster_"+vol+"_fid").fill(clusterY);
	    
	    aidaKF.histogram1D(eopFolder+"trk_clu_resX_"+vol+"_fid").fill(trkX-clusterX);
	    aidaKF.histogram1D(eopFolder+"trk_clu_resY_"+vol+"_fid").fill(trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsX_"+vol+"_fid").fill(trkX,trkX-clusterX);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsY_"+vol+"_fid").fill(trkY,trkX-clusterX);
	    
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsX_"+vol+"_fid").fill(trkX,trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsY_"+vol+"_fid").fill(trkY,trkY-clusterY);
	    
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vstrkP_"+vol+"_fid").fill(trackp,trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vstrkP_"+vol+"_fid").fill(trackp,trkX-clusterX);
	    
	    aidaKF.histogram2D(eopFolder+"trkY_vs_tanL_"+vol+"_fid").fill(tanL,trkY);
	    
	    aidaKF.histogram1D(eopFolder+"Xcluster_"+charge+"_"+vol+"_fid").fill(clusterX);
	    aidaKF.histogram1D(eopFolder+"Ycluster_"+charge+"_"+vol+"_fid").fill(clusterY);
	    
	    aidaKF.histogram1D(eopFolder+"trk_clu_resX_"+charge+"_"+vol+"_fid").fill(trkX-clusterX);
	    aidaKF.histogram1D(eopFolder+"trk_clu_resY_"+charge+"_"+vol+"_fid").fill(trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsX_"+charge+"_"+vol+"_fid").fill(trkX,trkX-clusterX);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsY_"+charge+"_"+vol+"_fid").fill(trkY,trkX-clusterX);
	    
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsX_"+charge+"_"+vol+"_fid").fill(trkX,trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsY_"+charge+"_"+vol+"_fid").fill(trkY,trkY-clusterY);
		
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vstrkP_"+charge+"_"+vol+"_fid").fill(trackp,trkY-clusterY);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vstrkP_"+charge+"_"+vol+"_fid").fill(trackp,trkX-clusterX);
		
	    aidaKF.histogram2D(eopFolder+"trkY_vs_tanL_"+charge+"_"+vol+"_fid").fill(tanL,trkY);
	    
	    // 
	    
	    // As function of incident angle at ECAL, inclusive and in bin of momentum.
	    
	    


	    
	}
	
	
    }

    
    
    /*
    private void doKFkinks(Track trk, GenericObject kink, Map<HpsSiSensor, Integer> sensorNums) {
        
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
            aidaKF.histogram2D(kinkFolder+"lambda_kink_mod").fill(sensor.getMillepedeId()+spacing,lambda);
            aidaKF.profile1D(kinkFolder+"lambda_kink_mod_p").fill(sensor.getMillepedeId()+spacing,lambda);
            aidaKF.histogram2D(kinkFolder+"phi_kink_mod").fill(sensor.getMillepedeId()+spacing,phi);
            aidaKF.profile1D(kinkFolder+"phi_kink_mod_p").fill(sensor.getMillepedeId()+spacing,phi);
            aidaKF.histogram1D(kinkFolder+"lambda_kink_" + sensor.getName()).fill(lambda);
            aidaKF.histogram1D(kinkFolder+"phi_kink_" + sensor.getName()).fill(phi);
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

            aidaKF.histogram1D(resFolder+"residual_before_KF_" + sensor.getName()).fill(diff.x());
            if (debug)
                System.out.printf("MdiffSensor %s \n", diff.toString());

        }
    }
    */
    private void FillKFTrackPlot(String str, String isTop, String charge, double val) {
        aidaKF.histogram1D(str+isTop).fill(val);
        aidaKF.histogram1D(str+isTop+charge).fill(val);
    }

    private void FillKFTrackPlot(String str, String isTop, String charge, double valX, double valY) {
	if(b_doBig2DPlots){
	    aidaKF.histogram2D(str+isTop).fill(valX,valY);
	    aidaKF.histogram2D(str+isTop+charge).fill(valX,valY);
	}
    }

    /*
    private void FillKFTrackPlot(String str, String isTop, String charge, double valX, double valY, double valZ) {
        aidaKF.histogram3D(str+isTop).fill(valX,valY,valZ);
        aidaKF.histogram3D(str+isTop+charge).fill(valX,valY,valZ);
    }
    */
    

    private void doAllHits(){

	// Map the fitted hits to their corresponding raw hits
	//	    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
	for (Map.Entry<RawTrackerHit, LCRelation> entry : fittedRawTrackerHitMap.entrySet()) {
	    LCRelation fitRth=entry.getValue();
	    RawTrackerHit rth=entry.getKey();
	    HpsSiSensor sensor = (HpsSiSensor) rth.getDetectorElement();
	    double t0 = FittedRawTrackerHit.getT0(fitRth);
	    double amplitude = FittedRawTrackerHit.getAmp(fitRth);
	    double chi2Prob = ShapeFitParameters.getChiProb(FittedRawTrackerHit.getShapeFitParameters(fitRth));
	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_t0_"+sensor.getName()).fill(t0);
	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_amplitude_"+sensor.getName()).fill(amplitude);
	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_chisq_"+sensor.getName()).fill(chi2Prob);
	}
	
	for(TrackerHit cl:  _siClusters){
	    List<RawTrackerHit> rawhits =  cl.getRawHits();
	    HpsSiSensor sensor = (HpsSiSensor) rawhits.get(0).getDetectorElement();
	    //		HpsSiSensor sensor = (HpsSiSensor)((RawTrackerHit) (cl.getRawHits().get(0)).getDetectorElement());
	    aidaKF.histogram1D(hitFolder+"all_cluster_hit_t0_"+sensor.getName()).fill(cl.getTime());
	}
    }

    
    private void doBasicKFtrack(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits) {
        
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
        
        
        FillKFTrackPlot(trkpFolder+"d0",isTop,charge,trackState.getD0());
        FillKFTrackPlot(trkpFolder+"z0",isTop,charge,trackState.getZ0());
        FillKFTrackPlot(trkpFolder+"phi",isTop,charge,trackState.getPhi());
        FillKFTrackPlot(trkpFolder+"tanLambda",isTop,charge,trackState.getTanLambda());
        FillKFTrackPlot(trkpFolder+"p",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==7)
            FillKFTrackPlot(trkpFolder+"p7h",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==6)
            FillKFTrackPlot(trkpFolder+"p6h",isTop,charge,trackp);
        if (trk.getTrackerHits().size()==5)
            FillKFTrackPlot(trkpFolder+"p5h",isTop,charge,trackp);
        
        if (TrackUtils.isHoleTrack(trk)) 
            FillKFTrackPlot(trkpFolder+"p_hole",isTop,charge,trackp);
        else 
            FillKFTrackPlot(trkpFolder+"p_slot",isTop,charge,trackp);
	Double trackTime = _trkTimeSigma.getFirstElement();
	Double trackTimeSD = _trkTimeSigma.getSecondElement();
	
	//fill track time and standard dev
	FillKFTrackPlot(trkpFolder+"trkTime",isTop,charge,trackTime);
	FillKFTrackPlot(trkpFolder+"trkTimeSD",isTop,charge,trackTimeSD);

        //Momentum maps
        FillKFTrackPlot(trkpFolder+"p_vs_phi",isTop,charge,trackState.getPhi(),trackp);
        FillKFTrackPlot(trkpFolder+"p_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackp);
	//        FillKFTrackPlot(trkpFolder+"p_vs_phi_tanLambda",isTop,charge,trackState.getPhi(),trackState.getTanLambda(),trackp);
        
        double tanLambda = trackState.getTanLambda();
        double cosLambda = 1. / (Math.sqrt(1+tanLambda*tanLambda));
        
        FillKFTrackPlot(trkpFolder+"pT_vs_phi",isTop,charge,trackState.getPhi(),trackp*cosLambda);
        FillKFTrackPlot(trkpFolder+"pT_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackp*cosLambda);
        
        
        //if (trk.getTrackerHits().size()==6)
        //    FillKFTrackPlot(trkpFolder+"p_Missing1Hit",isTop,charge,missingHits.get(0),trackp);
        
        //if (missingHits.size()==1 && missingHits.get(0)==7) 
        //    FillKFTrackPlot(trkpFolder+"p_MissingLastLayer",isTop,charge,trackp);
                
	FillKFTrackPlot(trkpFolder+"Chi2",isTop,charge,trk.getChi2());
	FillKFTrackPlot(trkpFolder+"Chi2oNDF",isTop,charge,trk.getChi2() / trk.getNDF());
        FillKFTrackPlot(trkpFolder+"Chi2_vs_p",isTop,charge,trackp,trk.getChi2());

        int nhits = trk.getTrackerHits().size();
       
        aidaKF.histogram1D(trkpFolder+"nHits" + isTop).fill(nhits);
        aidaKF.histogram1D(trkpFolder+"nHits" + isTop+charge).fill(nhits);

	//fill the layers with hit on track
	for(TrackerHit tkh:  trk.getTrackerHits()){
	    List<RawTrackerHit> rawhits =  tkh.getRawHits();
	    int stripLayer = ((HpsSiSensor) ((RawTrackerHit)rawhits.get(0)).getDetectorElement()).getLayerNumber();  
	    FillKFTrackPlot(trkpFolder+"LayersHit",isTop,charge,stripLayer);
	}
        Hep3Vector beamspot = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState, 0));
        if (debug)
            System.out.printf("beamspot %s transformed  \n", beamspot.toString());
        FillKFTrackPlot(trkpFolder+"trk_extr_or_x",isTop,charge,beamspot.x());
        FillKFTrackPlot(trkpFolder+"trk_extr_or_y",isTop,charge,beamspot.y());
        
        //Extrapolation to assumed tgt pos - helix
        Hep3Vector trkTgt = CoordinateTransformations.transformVectorToDetector(TrackUtils.extrapolateHelixToXPlane(trackState,bsZ));
        FillKFTrackPlot(trkpFolder+"trk_extr_bs_x",isTop,charge,trkTgt.x());
        FillKFTrackPlot(trkpFolder+"trk_extr_bs_y",isTop,charge,trkTgt.y());
        
        //Transform z to the beamspot plane
        //Get the PathToPlane
        
        BaseTrackState ts_bs = TrackUtils.getTrackExtrapAtVtxSurfRK(trackState,bFieldMap,0.,bsZ);
        
        
        //Get the track parameters wrt the beamline using helix
        double [] beamLine = new double [] {bsZ,0};
        double [] helixParametersAtBS = TrackUtils.getParametersAtNewRefPoint(beamLine, trackState);

                  
        FillKFTrackPlot(trkpFolder+"trk_extr_bs_x_rk",isTop,charge,ts_bs.getReferencePoint()[1]);
        FillKFTrackPlot(trkpFolder+"trk_extr_bs_y_rk",isTop,charge,ts_bs.getReferencePoint()[2]);

        //Ill defined - should be defined wrt bsX and bsY
        FillKFTrackPlot(trkpFolder+"d0_vs_bs_rk",isTop,charge,ts_bs.getD0());
        FillKFTrackPlot(trkpFolder+"d0_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.D0]);
        
        double s = HelixUtils.PathToXPlane(TrackUtils.getHTF(trackState),bsZ,0.,0).get(0);
        FillKFTrackPlot(trkpFolder+"z0_vs_bs",isTop,charge,trackState.getZ0() + s*trackState.getTanLambda());
        FillKFTrackPlot(trkpFolder+"z0_vs_bs_rk",isTop,charge,ts_bs.getZ0());
        FillKFTrackPlot(trkpFolder+"z0_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.Z0]);
        

        FillKFTrackPlot(trkpFolder+"phi_vs_bs_extrap",isTop,charge,helixParametersAtBS[BaseTrack.PHI]);
        
        //TH2D - Filling
	if(b_doBig2DPlots){
	    FillKFTrackPlot(trkpFolder+"d0_vs_phi",isTop,charge,trackState.getPhi(),trackState.getD0());
	    FillKFTrackPlot(trkpFolder+"d0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getD0());
	    FillKFTrackPlot(trkpFolder+"d0_vs_p",isTop,charge,trackp,trackState.getD0());
	    
	    //Ill defined - should be defined wrt bsX and bsY
	    FillKFTrackPlot(trkpFolder+"d0bs_vs_p",isTop,charge,trackp,helixParametersAtBS[BaseTrack.D0]);
	    
	    FillKFTrackPlot(trkpFolder+"z0_vs_p",isTop,charge,trackp,trackState.getZ0()); 
	    FillKFTrackPlot(trkpFolder+"z0bs_vs_p",isTop,charge,trackp,ts_bs.getZ0()); 
	    
	    //Interesting plot to get a sense where z-vtx is. 
	    //If z0 is referenced to the right BS z location, the slope of <z0> vs tanLambda is 0
	    FillKFTrackPlot(trkpFolder+"z0_vs_tanLambda",isTop,charge,trackState.getTanLambda(),trackState.getZ0());
	    FillKFTrackPlot(trkpFolder+"z0bs_vs_tanLambda",isTop,charge,trackState.getTanLambda(),ts_bs.getZ0());
	}

	if(b_doRawHitPlots){
	    	    	   
	    
	    for(TrackerHit tkh:  trk.getTrackerHits()){
		List<RawTrackerHit> rawhits =  tkh.getRawHits();
		HpsSiSensor sensor = (HpsSiSensor) rawhits.get(0).getDetectorElement();
		aidaKF.histogram1D(hitFolder+"cluster_hit_t0_"+sensor.getName()).fill(tkh.getTime());
		for(RawTrackerHit rth: rawhits){		    
		    //need the rth->fited
		    double t0 = FittedRawTrackerHit.getT0(getFittedHit(rth));
		    double amplitude = FittedRawTrackerHit.getAmp(getFittedHit(rth));
		    double chi2Prob = ShapeFitParameters.getChiProb(FittedRawTrackerHit.getShapeFitParameters(getFittedHit(rth)));
		    aidaKF.histogram1D(hitFolder+"raw_hit_t0_"+sensor.getName()).fill(t0);
		    aidaKF.histogram1D(hitFolder+"raw_hit_amplitude_"+sensor.getName()).fill(amplitude);
		    aidaKF.histogram1D(hitFolder+"raw_hit_chisq_"+sensor.getName()).fill(chi2Prob);

		}
	    }
        }
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
                aidaKF.histogram2D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_"+ibinstr+isTop).fill(trackState.getTanLambda(),z0Corr);
                aidaKF.profile1D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_p_"+ibinstr+isTop).fill(trackState.getTanLambda(),z0Corr);
            }        
        }
    }
    
    private void doKFresiduals(Track trk, Map<HpsSiSensor, TrackerHit> sensorHits, EventHeader event) {
        
        Map<Integer,HpsSiSensor> sensorMPIDs   = new HashMap<Integer,HpsSiSensor>();

        for (HpsSiSensor sensor : sensorHits.keySet()) {
            //Also fill here the sensorMPIDs map
	    if(debug){
		System.out.println(this.getClass().getName()+":: mapping "+sensor.getMillepedeId()+" to " + sensor.getName());
	    }
            sensorMPIDs.put(sensor.getMillepedeId(),sensor);	    
            ITransform3D trans = sensor.getGeometry().getGlobalToLocal();
            
            // position of hit (track crossing the sensor before kf extrapolation)
            // the hit information available on each sensor is meaningful only along the measurement direction,
            // Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            // instead: extract the information of the hit of the track at the sensor position before kf
            TrackState trackState = trk.getTrackStates().get(0);
            Hep3Vector hitTrackPos = TrackStateUtils.getLocationAtSensor(trackState, sensor, bfield);
            
            if (hitTrackPos == null) {
                if (debug) {
                    System.out.printf(this.getClass().getName()+"::doKFresiduals:: hitTrackPos is null to sensor %s\n", sensor.toString());
                }
                continue;
            }
            
            Hep3Vector hitTrackPosSensor = new BasicHep3Vector(hitTrackPos.v());
            trans.transform(hitTrackPosSensor);
            // after the transformation x and y in the sensor frame are reversed
            // This plot is ill defined.
            
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(hitFolder+"hit_u_vs_v_sensor_frame_" + sensor.getName()).fill(hitTrackPosSensor.y(), hitTrackPosSensor.x());
	    }
	    //aidaKF.histogram2D("hit_u_vs_v_sensor_frame_" + sensor.getName()).fill(hitPos.y(), hitPos.x());
            //aidaKF.histogram2D("hit y vs x lab-frame " + sensor.getName()).fill(hitPos.y(), hitPos.x());
            
            
            // position predicted on track after KF
            Hep3Vector extrapPos = null;
            Hep3Vector extrapPosSensor = null;
            extrapPos = TrackUtils.extrapolateTrackPositionToSensor(trk, sensor, sensors, bfield);
            if (extrapPos == null)
                return;
            extrapPosSensor = new BasicHep3Vector(extrapPos.v());
            trans.transform(extrapPosSensor);
            //aidaKF.histogram2D("residual after KF vs u predicted " + sensor.getName()).fill(extrapPosSensor.x(), res);
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
	    }
		// select track charge
	    if(b_doBig2DPlots){
		if(trk.getCharge()>0) {
		    aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_pos_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
		}else if(trk.getCharge()<0) {
		    aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_neg_sensor_frame_" + sensor.getName()).fill(extrapPosSensor.y(), extrapPosSensor.x());
		}
	    }
            // post-KF residual
            Hep3Vector hitPos = new BasicHep3Vector(sensorHits.get(sensor).getPosition());
            Hep3Vector hitPosSensor = new BasicHep3Vector(hitPos.v());
            trans.transform(hitPosSensor);
            Hep3Vector resSensor = VecOp.sub(hitPosSensor, extrapPosSensor);
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(resFolder+"residual_after_KF_vs_v_predicted_" + sensor.getName()).fill(extrapPosSensor.y(), resSensor.x());
		aidaKF.histogram2D(resFolder+"residual_after_KF_vs_u_hit_" + sensor.getName()).fill(hitPosSensor.x(), resSensor.x());
	    }
	    aidaKF.histogram1D(resFolder+"residual_after_KF_" + sensor.getName()).fill(resSensor.x());
		


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

	Double trackTime = _trkTimeSigma.getFirstElement();
	Double trackTimeSD = _trkTimeSigma.getSecondElement();
	/*
        trackTime /= (float)sensorHits.size();
        
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            trackTimeSD += Math.pow(trackTime - sensorHits.get(sensor).getTime(),2);
        }
        
        trackTimeSD = Math.sqrt(trackTimeSD / ((float) sensorHits.size() - 1.));
        */

        
        RelationalTable trackResidualsTable = null;
        if (event.hasCollection(LCRelation.class, trackResidualsRelColName)) {
            trackResidualsTable = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trackresRelation = event.get(LCRelation.class, trackResidualsRelColName);
            for (LCRelation relation : trackresRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    trackResidualsTable.add(relation.getFrom(), relation.getTo());
                }
            }
	    if (debug)
		System.out.println("Loaded track Residuals Table");
        } else {
	    if (debug) {
		System.out.println("null TrackResidualsKF Data Relations.");
	    }
            //Failed finding TrackResidualsKF
            return;
        }
        
        GenericObject trackRes = (GenericObject) trackResidualsTable.from(trk);
        if (trackRes == null) {
	    if (debug)
		System.out.println("null TrackResidualsKF Data.");
            return;
        }
        
	int nres = (trackRes.getNInt()-1);
	if(debug){
	    System.out.println(this.getClass().getName()+":: number entries in trackRes = "+nres);
	}
        String vol = "_top";
        if (trk.getTrackStates().get(0).getTanLambda() < 0)
            vol = "_bottom";
        // get the unbias
        for (int i_hit =0; i_hit < nres ; i_hit+=1) {
            if (trackRes.getIntVal(i_hit)!=-999)  {		
		if(debug){
		    System.out.println(this.getClass().getName()+":: getting residual for ihit = "+i_hit+" trackResValue = "+ trackRes.getIntVal(i_hit));
		}
                //Measured hit
                HpsSiSensor hps_sensor = sensorMPIDs.get(trackRes.getIntVal(i_hit));
		if(debug){
		    System.out.println(this.getClass().getName()+":: sensor for this hit = "+hps_sensor.getName());
		}
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
                aidaKF.histogram1D(resFolder+"uresidual_KF"+vol).fill(trackRes.getDoubleVal(i_hit));
                
                if (trackRes.getIntVal(i_hit) < 9) 
                    //L1L4 
                    aidaKF.histogram1D(resFolder+"uresidual_KF"+vol+"_L1L4").fill(trackRes.getDoubleVal(i_hit));
                else 
                    //L5L7
                    aidaKF.histogram1D(resFolder+"uresidual_KF"+vol+"_L5L7").fill(trackRes.getDoubleVal(i_hit));
                
                
                //Top go from 0 to 20, bottom go from 25 to 45
                int spacing = 0;
                if (vol == "_bottom")
                    spacing = sensors.size()/2 + mod;
		if(b_doBig2DPlots){                
		    aidaKF.histogram2D(resFolder+"uresidual_KF_mod").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
		    aidaKF.histogram2D(resFolder+"uresidual_KF_vs_u_hit_" + sensorName).fill(hitPosSensorG.x(),trackRes.getDoubleVal(i_hit));
		    aidaKF.histogram2D(resFolder+"uresidual_KF_vs_v_pred_" + sensorName).fill(extrapPosSensor.y(),trackRes.getDoubleVal(i_hit));
		}	
		aidaKF.histogram1D(resFolder+"uresidual_KF_" + sensorName).fill(trackRes.getDoubleVal(i_hit));
		aidaKF.profile1D(resFolder+"uresidual_KF_mod_p").fill(trackRes.getIntVal(i_hit)+spacing,trackRes.getDoubleVal(i_hit));
                aidaKF.histogram1D(epullFolder+"ureserror_KF_" + sensorName).fill(trackRes.getFloatVal(i_hit));
                aidaKF.histogram1D(epullFolder+"ures_pull_KF_" + sensorName).fill(trackRes.getDoubleVal(i_hit) / Math.sqrt(trackRes.getFloatVal(i_hit)));
                
                //Get the hit time
                double hitTime = sensorHits.get(hps_sensor).getTime();
                
                //Get the track time (it's the average of hits-on-track time)
                
                double dT_hit_track  =  hitTime - trackTime;
                double dT_hit_sigma  = (hitTime - trackTime) / trackTimeSD;
		if(b_doBig2DPlots){ 
		    aidaKF.histogram2D(resFolder+"uresidual_KF_vs_dT_hit_"+sensorName).fill(dT_hit_track,trackRes.getDoubleVal(i_hit));
		    aidaKF.histogram2D(resFolder+"uresidual_KF_vs_dTs_hit_"+sensorName).fill(dT_hit_sigma,trackRes.getDoubleVal(i_hit));
		}                
                


            }
            else {
                if (debug){
                    System.out.printf("Track refit failed? No biased residual for %d\n", i_hit);
                }
            }
        }
    }//doKFresiduals
    
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
	    //            int hpslayer = (stripLayer + 1 ) / 2;
            LayersOnTrack.add(stripLayer);
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
	    
            aidaKF.histogram1D(eopFolder+"Ecluster"+vol,200,0,6);
            aidaKF.histogram1D(eopFolder+"EoP"+vol,200,0,2);

	    double lmin = 0.;
            double lmax = 0.08;
            if (vol == "_bot") {
                lmin = -0.08;
                lmax = 0.;
            }
	    
            for (String charge : charges) {
                aidaKF.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol,200,0,6,200,0,2);
                aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol,200,lmin,lmax,200,0,2);
                aidaKF.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol,200,-0.2,0.2,200,0,2);
	    }
                        
            aidaKF.histogram1D(eopFolder+"Ecluster"+vol+"_fid",200,0,5);
            aidaKF.histogram1D(eopFolder+"EoP"+vol+"_fid",200,0,2);
            aidaKF.histogram2D(eopFolder+"EoP_vs_trackP"+vol+"_fid",200,0,6,200,0,2);


	    double cxrange = 20;
	    double cyrange = 20;
	    double ecalX = 400;
	  
	    aidaKF.histogram1D(eopFolder+"Xcluster"+vol+"_fid",200,-ecalX,ecalX);
	    aidaKF.histogram1D(eopFolder+"Ycluster"+vol+"_fid",200,-ecalX,ecalX);
            aidaKF.histogram1D(eopFolder+"trk_clu_resX"+vol+"_fid",200,-cxrange,cxrange);
	    aidaKF.histogram1D(eopFolder+"trk_clu_resY"+vol+"_fid",200,-cyrange,cyrange);
	    
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsX"+vol+"_fid",200,-ecalX,ecalX,200,-cxrange,cxrange);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsY"+vol+"_fid",200,-ecalX,ecalX,200,-cxrange,cxrange);
	    
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsX"+vol+"_fid",200,-ecalX,ecalX,200,-cyrange,cyrange);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsY"+vol+"_fid",200,-ecalX,ecalX,200,-cyrange,cyrange);

	    aidaKF.histogram2D(eopFolder+"trk_clu_resY_vstrkP"+vol+"_fid",100,0.,5,200,-cyrange,cyrange);
	    aidaKF.histogram2D(eopFolder+"trk_clu_resX_vstrkP"+vol+"_fid",100,0.,5,200,-cyrange,cyrange);
	    
	    aidaKF.histogram2D(eopFolder+"trkY_vs_tanL"+vol+"_fid",200,-0.2,0.2,200,-100,100);
	    
	    
            for (String charge : charges) {

		  //put the trk-cluster time in trkpFolder
		aidaKF.histogram1D(trkpFolder+"trk-cluTime"+charge+vol,100,-20,20);
		
                aidaKF.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol+"_fid",200,0,6,200,0,2);
                aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol+"_fid",200,0.01,0.08,200,0,2);
                aidaKF.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol+"_fid",200,-0.2,0.2,200,0,2);


		
		aidaKF.histogram1D(eopFolder+"Xcluster"+charge+vol+"_fid",200,-ecalX,ecalX);
		aidaKF.histogram1D(eopFolder+"Ycluster"+charge+vol+"_fid",200,-ecalX,ecalX);
		aidaKF.histogram1D(eopFolder+"trk_clu_resX"+charge+vol+"_fid",200,-cxrange,cxrange);
		aidaKF.histogram1D(eopFolder+"trk_clu_resY"+charge+vol+"_fid",200,-cyrange,cyrange);
		
		aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsX"+charge+vol+"_fid",200,-ecalX,ecalX,200,-cxrange,cxrange);
		aidaKF.histogram2D(eopFolder+"trk_clu_resX_vsY"+charge+vol+"_fid",200,-ecalX,ecalX,200,-cxrange,cxrange);
		
		aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsX"+charge+vol+"_fid",200,-ecalX,ecalX,200,-cyrange,cyrange);
		aidaKF.histogram2D(eopFolder+"trk_clu_resY_vsY"+charge+vol+"_fid",200,-ecalX,ecalX,200,-cyrange,cyrange);
		
		aidaKF.histogram2D(eopFolder+"trk_clu_resY_vstrkP"+charge+vol+"_fid",100,0.,5,200,-cyrange,cyrange);
		aidaKF.histogram2D(eopFolder+"trk_clu_resX_vstrkP"+charge+vol+"_fid",100,0.,5,200,-cyrange,cyrange);
		
		aidaKF.histogram2D(eopFolder+"trkY_vs_tanL"+charge+vol+"_fid",200,-0.2,0.2,200,-100,100);
		
		
		
		
            }
        }
        
        aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda",200,-0.1,0.1,200,0,2);
        aidaKF.histogram2D(eopFolder+"EoP_vs_phi",200,-0.2,0.2,200,0,2);
	//        aidaKF.histogram3D(eopFolder+"EoP_vs_tanLambda_phi",200,-0.08,0.08,200,-0.2,0.2,200,0,2);
	
        aidaKF.histogram2D(eopFolder+"EoP_vs_tanLambda_fid",200,-0.1,0.1,200,0,2);
        aidaKF.histogram2D(eopFolder+"EoP_vs_phi_fid",200,-0.2,0.2,200,0,2);
	//        aidaKF.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid",200,-0.08,0.08,200,-0.2,0.2,200,0,2);
	
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
	    //            aidaKF.histogram1D(resFolder+"bresidual_KF"+vol,nbins, -xmax, xmax);
            aidaKF.histogram1D(resFolder+"uresidual_KF"+vol,nbins, -xmax, xmax);
	    //            aidaKF.histogram1D(resFolder+"bresidual_KF"+vol+"_L1L4",nbins,-xmax,xmax);
            aidaKF.histogram1D(resFolder+"uresidual_KF"+vol+"_L1L4",nbins,-xmax,xmax);
	    //            aidaKF.histogram1D(resFolder+"bresidual_KF"+vol+"_L5L7",nbins,-xmax,xmax);
            aidaKF.histogram1D(resFolder+"uresidual_KF"+vol+"_L5L7",nbins,-xmax,xmax);

        }
        
        //res/kinks TH2D
        //5 empty bins to distinguish between top and bottom
        
	//        aidaKF.histogram2D(resFolder+"bresidual_KF_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5, nbins, -xmax,xmax);
	//        aidaKF.profile1D(resFolder+"bresidual_KF_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
	if(b_doBig2DPlots){
	    aidaKF.histogram2D(resFolder+"uresidual_KF_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5, 400, -0.4,0.4);
	}
        aidaKF.profile1D(resFolder+"uresidual_KF_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
            
        
        //Hits vs channel
	/*
        int nch  = 400;
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL1b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL2b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL3b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL4b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL5b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL6b",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL7b",nch,0,nch,nch,0,nch);
        
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL1t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL2t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL3t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL4t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL5t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL6t",nch,0,nch,nch,0,nch);
        aidaKF.histogram2D(resFolder+"Axial_vs_Stereo_channel_moduleL7t",nch,0,nch,nch,0,nch);
	*/  
        

        for (SiSensor sensor : sensors) {

            HpsSiSensor sens = (HpsSiSensor) sensor.getGeometry().getDetectorElement();
            xmax = 0.5;
            nbins = 250;
            int l = (sens.getLayerNumber() + 1) / 2;
            if (l > 1) xmax = 0.05 + (l - 1) * 0.08;
	    //            aidaKF.histogram1D(resFolder+"residual_before_KF_" + sensor.getName(), nbins, -xmax, xmax);
            
            xmax = 0.250;
            
            if (l >= 6)
                xmax = 0.250;
            aidaKF.histogram1D(resFolder+"residual_after_KF_" + sensor.getName(),  nbins, -xmax, xmax);
	    //            aidaKF.histogram1D(resFolder+"bresidual_KF_" + sensor.getName(), nbins, -xmax, xmax);
            aidaKF.histogram1D(resFolder+"uresidual_KF_" + sensor.getName(), nbins, -xmax, xmax);
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(resFolder+"uresidual_KF_vs_u_hit_" + sensor.getName(),100,-20.0,20.0,100,-0.1,0.1);
		aidaKF.histogram2D(resFolder+"uresidual_KF_vs_v_pred_" + sensor.getName(),300,-60.0,60.0,100,-0.1,0.1);
		aidaKF.histogram2D(resFolder+"uresidual_KF_vs_dT_hit_" + sensor.getName(),100,-10.0,10.0,100,-0.1,0.1);
		aidaKF.histogram2D(resFolder+"uresidual_KF_vs_dTs_hit_" + sensor.getName(),100,-5.0,5.0,100,-0.1,0.1);
	    }
            
	    //            aidaKF.histogram1D(epullFolder+"breserror_KF_" + sensor.getName(), nbins, 0.0, 0.1);
            aidaKF.histogram1D(epullFolder+"ureserror_KF_" + sensor.getName(), nbins, 0.0, 0.2);
	    //            aidaKF.histogram1D(epullFolder+"bres_pull_KF_" + sensor.getName(), nbins, -5, 5);
            aidaKF.histogram1D(epullFolder+"ures_pull_KF_" + sensor.getName(), nbins, -5, 5);
	    if(b_doBig2DPlots){
		aidaKF.histogram2D(resFolder+"residual_after_KF_vs_u_hit_" + sensor.getName(), 100, -20.0, 20.0, 100, -0.04, 0.04);
		aidaKF.histogram2D(resFolder+"residual_after_KF_vs_v_predicted_" + sensor.getName(), 100, -55.0, 55.0, 100, -0.04, 0.04);
		aidaKF.histogram2D(hitFolder+"hit_u_vs_v_sensor_frame_" + sensor.getName(), 300, -60.0, 60.0, 300, -25, 25);
		aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
		aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_pos_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
		aidaKF.histogram2D(hitFolder+"predicted_u_vs_v_neg_sensor_frame_" + sensor.getName(), 100, -60, 60, 100, -25, 25);
	    }
	    aidaKF.histogram1D(hitFolder+"raw_hit_t0_"+sensor.getName(),200, -100, 100.0);
	    aidaKF.histogram1D(hitFolder+"raw_hit_amplitude_"+sensor.getName(),200, 0.0, 4000.0);
	    aidaKF.histogram1D(hitFolder+"raw_hit_chisq_"+sensor.getName(),200, 0.0, 2.0);

	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_t0_"+sensor.getName(),200, -100, 100.0);
	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_amplitude_"+sensor.getName(),200, 0.0, 4000.0);
	    aidaKF.histogram1D(hitFolder+"all_hits_raw_hit_chisq_"+sensor.getName(),200, 0.0, 2.0);


	    aidaKF.histogram1D(hitFolder+"cluster_hit_t0_"+sensor.getName(),200, -100, 100.0);
	    aidaKF.histogram1D(hitFolder+"all_cluster_hit_t0_"+sensor.getName(),200, -100, 100.0);


	    
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
	    //            aidaKF.histogram1D(kinkFolder+"lambda_kink_" + sensor.getName(), 250, -xmax, xmax);
            //aidaKF.histogram1D(kinkFolder+"phi_kink_" + sensor.getName(), 250, -xmax, xmax);
        }
        /*
        aidaKF.histogram2D(kinkFolder+"lambda_kink_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5,nbins,-0.001,0.001);
        aidaKF.profile1D(kinkFolder+"lambda_kink_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
        aidaKF.histogram2D(kinkFolder+"phi_kink_mod",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5   ,nbins,-0.001,0.001);
        aidaKF.profile1D(kinkFolder+"phi_kink_mod_p",mod_2dplot_bins,-0.5,mod_2dplot_bins-0.5);
        */
        List<String> charges = new ArrayList<String>();
        charges.add("");
        charges.add("_pos");
        charges.add("_neg");
        
        int nbins_t = 200;
        
        //For momentum
        int nbins_p = 150;
        double pmax = 4.5;
        
        double z0max = 1;
        double d0max = 5;
        double z0bsmax = 0.2;
        
	aidaKF.histogram1D(trkpFolder+"nTracks",15,0,15);
        for (String vol : volumes) {
            for (String charge : charges) {
                
                
                //TH1Ds
                aidaKF.histogram1D(trkpFolder+"d0"+vol+charge,nbins_t,-5.0,5.0);
                aidaKF.histogram1D(trkpFolder+"z0"+vol+charge,nbins_t,-1.3,1.3);
                aidaKF.histogram1D(trkpFolder+"phi"+vol+charge,nbins_t,-0.06,0.06);
                aidaKF.histogram1D(trkpFolder+"tanLambda"+vol+charge,nbins_t,-0.2,0.2);
                aidaKF.histogram1D(trkpFolder+"trkTime"+vol+charge,nbins_t,-100,100);
                aidaKF.histogram1D(trkpFolder+"trkTimeSD"+vol+charge,nbins_t,0,10);

                aidaKF.histogram1D(trkpFolder+"p"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p7h"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p6h"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p5h"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p_MissingLastLayer"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p_hole"+vol+charge,nbins_p,0.,pmax);
                aidaKF.histogram1D(trkpFolder+"p_slot"+vol+charge,nbins_p,0.,pmax);
                                
                aidaKF.histogram1D(trkpFolder+"Chi2"+vol+charge,nbins_t*2,0,200);
		aidaKF.histogram1D(trkpFolder+"Chi2oNDF"+vol+charge,nbins_t*2,0,50);
                aidaKF.histogram1D(trkpFolder+"nHits"+vol+charge,15,0,15);
                aidaKF.histogram1D(trkpFolder+"LayersHit"+vol+charge,15,0,15);
                aidaKF.histogram1D(trkpFolder+"trk_extr_or_x"+vol+charge,nbins_t,-3,3);
                aidaKF.histogram1D(trkpFolder+"trk_extr_or_y"+vol+charge,nbins_t,-3,3);
                aidaKF.histogram1D(trkpFolder+"trk_extr_bs_x"+vol+charge, 2*nbins_t, -5, 5);
                aidaKF.histogram1D(trkpFolder+"trk_extr_bs_y"+vol+charge, 2*nbins_t, -5, 5);
                aidaKF.histogram1D(trkpFolder+"trk_extr_bs_x_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaKF.histogram1D(trkpFolder+"trk_extr_bs_y_rk"+vol+charge, 2*nbins_t, -3, 3);
                aidaKF.histogram1D(trkpFolder+"d0_vs_bs_rk"+vol+charge, 2*nbins_t, -5, 5);
                aidaKF.histogram1D(trkpFolder+"d0_vs_bs_extrap"+vol+charge, 2*nbins_t, -5, 5);
                aidaKF.histogram1D(trkpFolder+"z0_vs_bs_rk"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaKF.histogram1D(trkpFolder+"z0_vs_bs_extrap"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaKF.histogram1D(trkpFolder+"z0_vs_bs"+vol+charge, 2*nbins_t, -z0bsmax, z0bsmax);
                aidaKF.histogram1D(trkpFolder+"phi_vs_bs_extrap"+vol+charge,2*nbins_t, -0.06,0.06);
                
                
                //TH2Ds
                
		if(b_doBig2DPlots){
		    aidaKF.histogram2D(trkpFolder+"d0_vs_phi"+vol+charge,nbins_t,-0.3,0.3,nbins_t,-5.0,5.0);
		    aidaKF.histogram2D(trkpFolder+"Chi2_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t*2,0,200);
		    //aidaKF.histogram2D("d0_vs_phi_bs"+vol+charge,nbins_t,-5.0,5.0,nbins_t,-0.3,0.3);
		    aidaKF.histogram2D(trkpFolder+"d0_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_t,-5.0,5.0);
		    aidaKF.histogram2D(trkpFolder+"d0_vs_p"+vol+charge,  nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
		    aidaKF.histogram2D(trkpFolder+"d0bs_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
		    aidaKF.histogram2D(trkpFolder+"z0_vs_p"+vol+charge,  nbins_p,0.0,pmax,nbins_t,-5.0,5.0);
		    aidaKF.histogram2D(trkpFolder+"z0bs_vs_p"+vol+charge,nbins_p,0.0,pmax,nbins_t,-z0bsmax,z0bsmax);
		    aidaKF.histogram2D(trkpFolder+"z0_vs_tanLambda"+vol+charge,  nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
		    aidaKF.histogram2D(trkpFolder+"z0bs_vs_tanLambda"+vol+charge,nbins_t,-0.1,0.1,nbins_t,-z0bsmax,z0bsmax);
		    
		    aidaKF.histogram2D(trkpFolder+"p_Missing1Hit"+vol+charge,8,0,8,nbins_p,0.0,pmax);
		    aidaKF.histogram2D(trkpFolder+"p_vs_phi"+vol+charge,   nbins_t,-0.3,0.3, nbins_p,0.,pmax);
		    aidaKF.histogram2D(trkpFolder+"p_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_p,0.,pmax);
		    //                aidaKF.histogram3D(trkpFolder+"p_vs_phi_tanLambda"+vol+charge, 50,-0.3,0.3,50,-0.2,0.2,100,0.,pmax);
		    
		    aidaKF.histogram2D(trkpFolder+"pT_vs_phi"+vol+charge,   nbins_t,-0.3,0.3, nbins_p,0.,pmax);
		    aidaKF.histogram2D(trkpFolder+"pT_vs_tanLambda"+vol+charge,nbins_t,-0.2,0.2,nbins_p,0.,pmax);
		}                

                
                if (b_doDetailPlots) { 
                    //TH2Ds - detail
                    int ibins = 15;
                    double start= -12;
                    double end = -5;
                    double step = (end-start) / (double)ibins;
                    for (int ibin = 0; ibin<ibins;ibin++) {
                        String ibinstr =  String.valueOf(ibin);
                        aidaKF.histogram2D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_"+ibinstr+vol,nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
                        aidaKF.profile1D(trkpDetailFolder+"z0_vs_tanLambda_bsZ_p_"+ibinstr+vol,nbins_t,-0.1,0.1);
                    }
                    //aidaKF.histogram3D("z0_vs_tanLambda_bsZ"+vol,60,-12,-6,nbins_t,-0.1,0.1,nbins_t,-z0max,z0max);
                    //aidaKF.profile2D("z0_vs_tanLambda_bsZ_p"+vol,60,-12,6,nbins_t,-0.1,0.1);
                }
            }//charge loop
        }//vol loop
    }

    private Pair<Double,Double> getTrackTime(Map<HpsSiSensor, TrackerHit> sensorHits){
	double trackTime = 0.;
        double trackTimeSD = 0.;
	for (HpsSiSensor sensor : sensorHits.keySet()) {
	    trackTime += sensorHits.get(sensor).getTime();
	}
	trackTime /= (float)sensorHits.size();
        
        for (HpsSiSensor sensor : sensorHits.keySet()) {
            trackTimeSD += Math.pow(trackTime - sensorHits.get(sensor).getTime(),2);
        }
        
        trackTimeSD = Math.sqrt(trackTimeSD / ((float) sensorHits.size() - 1.));
	return new Pair<Double,Double>(trackTime, trackTimeSD);
    }
    
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aidaKF.saveAs(outputPlots);

		/*
                // remove all KF histograms from heap after they have been written on output file
                String[] type = aidaKF.tree().listObjectNames("/",true);
                for (int i=0; i<type.length; i++){
                    // strip the trailing / from the object name and check if any else
                    String namtyp = type[i].substring(1);
                    if(namtyp.contains("/")) {
                        continue;
                    }else{
                        IManagedObject obj = aidaKF.tree().find(namtyp);
                        if (obj instanceof IBaseHistogram) aidaKF.tree().rm(obj.name()) ;
                    }
                }
		*/
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    
    private void mapFittedRawHits(List<LCRelation> allHits) { 
	
        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
	
        // Loop through all fitted hits and map them to their corresponding raw hits
        for (LCRelation fittedHit : allHits) { 
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
    }


    private LCRelation getFittedHit(RawTrackerHit rawHit) { 
        return fittedRawTrackerHitMap.get(rawHit);
    }
}
