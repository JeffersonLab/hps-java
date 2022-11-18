package org.hps.recon.tracking.gbl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.Comparator;

//import hep.physics.vec.Hep3Vector;
//import hep.physics.vec.BasicHep3Vector;
//import org.lcsim.event.TrackState;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;

import org.lcsim.geometry.Detector;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.constants.Constants;
import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.TrackResidualsData;
import org.hps.recon.tracking.TrackStateUtils;
import org.lcsim.event.base.BaseLCRelation;

//import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackingReconstructionPlots;

/**
 * A Driver which refits Kalman Tracks using GBL
 * in order to get the alignment derivatives
 */

public class KalmanToGBLDriver extends Driver {
    
    String derFolder = "/gbl_derivatives/";
    private HpsGblTrajectoryCreator _hpsGblTrajCreator;
    private String inputCollectionName = "KalmanFullTracks";
    private String trackResidualsColName = "TrackResidualsKFtoGBL";
    private String trackResidualsRelColName = "TrackResidualsKFtoGBLRelations";
    private String kinkDataCollectionName = GBLKinkData.DATA_COLLECTION;
    private String kinkDataRelationsName  = GBLKinkData.DATA_RELATION_COLLECTION;
    private Boolean _debug = false;
    private Boolean _analysis = false;
    private Boolean constrainedBSFit = false;
    private double bfield;
    private Boolean computeGBLResiduals = true;
    public AIDA aida;
    private IHistogram1D hGBLurt, hGBLurb, hKFurt, hKFurb, hGBLrt, hGBLrb;
    private IHistogram1D hDelPhi0, hDelTanL, hDelOmega, hDelD0, hDelZ0;
    private IHistogramFactory ahf;
        
    public void setDebug(boolean val) {
        _debug = val;
    }
    
    public void setAnalysis(boolean val) {
        _analysis = val;
    }
    

    void definePlots() {
        if (aida == null) aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        ahf = aida.histogramFactory();
        hGBLurt = aida.histogram1D("GBL unbiased residuals top detector", 100, -0.25, 0.25);
        hGBLurb = aida.histogram1D("GBL unbiased residuals bottom detector", 100, -0.25, 0.25);
        hGBLrt = aida.histogram1D("GBL biased residuals top detector", 100, -1., 1.);
        hGBLrb = aida.histogram1D("GBL biased residuals bottom detector", 100, -1., 1.);
        hKFurt = aida.histogram1D("KF unbiased residuals top detector", 100, -1., 1.);
        hKFurb = aida.histogram1D("KF unbiased residuals bottom detector", 100, -1., 1.);
        hDelPhi0 = aida.histogram1D("KF minus GBL phi0", 100, -0.02, 0.02);
        hDelD0 = aida.histogram1D("KF minus GBL D0", 100, -5., 5.);
        hDelOmega = aida.histogram1D("KF minus GBL Omega", 100, -3.E-4, 3.E-4);
        hDelTanL = aida.histogram1D("KF minus GBL tanLamba", 100, -0.01, 0.01);
        hDelZ0 = aida.histogram1D("KF minus GBL Z0", 100, -0.1, 0.1);
    }
    
    @Override 
    protected void detectorChanged(Detector detector) {
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        
        if (_analysis) definePlots();
    }

    @Override
    protected void process(EventHeader event) {

        //Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection =  new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations          = new ArrayList<LCRelation>();


        //Kinks 
        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();
        
        List<Track> GBLtracks = null;
        if (_analysis) {
            if (event.hasCollection(Track.class, "GBLTracks")) {
                GBLtracks = event.get(Track.class, "GBLTracks");
                if (_debug) {
                    System.out.format("KalmanToGBLDriver: number of input GBL tracks = %d\n", GBLtracks.size());
                }
            }
        }
        
        //Get the track collection from the event

        if (!event.hasCollection(Track.class, inputCollectionName)) {
            if (_debug) System.out.format("KalmanToGBLDriver: collection %s not found\n", inputCollectionName);
            return;
        }

        List<Track> tracks = event.get(Track.class,inputCollectionName);
        
        String residualsKFcollectionName = "KFUnbiasRes";
        List<TrackResidualsData> residualsKF = null;
        if (event.hasCollection(TrackResidualsData.class, residualsKFcollectionName)) {
            if (_debug) System.out.format("KalmanToGBLDriver: collection %s found\n", residualsKFcollectionName);
            residualsKF = event.get(TrackResidualsData.class, residualsKFcollectionName);
        }
        String residualsRelationsKFcollectionName = "KFUnbiasResRelations";
        List<LCRelation> residualsRelKF = null;
        RelationalTable kfResidsRT = null;
        if (event.hasCollection(LCRelation.class, residualsRelationsKFcollectionName)) {
            if (_debug) System.out.format("KalmanToGBLDriver: collection %s found\n", residualsRelationsKFcollectionName);
            residualsRelKF = event.get(LCRelation.class, residualsRelationsKFcollectionName);
            kfResidsRT = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            for (LCRelation relation : residualsRelKF) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) { 
                    kfResidsRT.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        
        if (_debug) System.out.println("KalmanToGBLDriver, found tracks: " + inputCollectionName+" " + tracks.size());

        RelationalTable kfSCDsRT = null;
        List<LCRelation> kfSCDRelation = new ArrayList<LCRelation>();       
        if (event.hasCollection(LCRelation.class,"KFGBLStripClusterDataRelations")) {
            kfSCDsRT = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            kfSCDRelation = event.get(LCRelation.class,"KFGBLStripClusterDataRelations");
            for (LCRelation relation : kfSCDRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) { 
                    kfSCDsRT.add(relation.getFrom(), relation.getTo());
                }
            }   
        } else {
            System.out.println("null KFGBLStripCluster Data Relations."); 
            return;
        }
        
        List<Track> newGBLtks = null;
        if (_analysis) newGBLtks = new ArrayList<Track>(tracks.size());
        
        //Loop on Kalman Tracks        
        for (Track trk : tracks ) {
            
            //Remove tracks with less than 10 hits
            //if (trk.getTrackerHits().size() < 10)
            //    continue;
            
            //Hep3Vector momentum = new BasicHep3Vector(trk.getTrackStates().get(0).getMomentum());
            
            //Remove tracks where there is high mis-tracking rate
            //TrackState trackState = trk.getTrackStates().get(0);
            //if (Math.abs(trackState.getTanLambda()) < 0.02)
            //    continue;                                 

            //Get the strip cluster data
            Set<GBLStripClusterData> kfSCDs = kfSCDsRT.allFrom(trk);
            
            if (_debug) System.out.println("Kalman Strip Cluster data size: " + kfSCDs.size());            
                        
            //Convert the set to a list for sorting it

            List<GBLStripClusterData> list_kfSCDs = new ArrayList<GBLStripClusterData>(kfSCDs);
            
            //Sort the list by cluster ID (that's the millepede index, which should correspond to a monotonous increase in arcLength of the points)
            
            Collections.sort(list_kfSCDs, arcLComparator);
            
            
            //for (GBLStripClusterData kfSCD : list_kfSCDs)  {
            //    System.out.println("KalmanToGBLDriver: Found strip with id/layer " + kfSCD.getId());
            //    System.out.println("KalmanToGBLDriver: and s3D " + kfSCD.getPath3D());
            //}
                    
            double bfac = Constants.fieldConversion * bfield;

            //I'm using the OLD way to refit for the moment.
            
            FittedGblTrajectory fitGbl_traj = HpsGblRefitter.fit(list_kfSCDs, bfac, false);
            GblTrajectory gbl_fit_trajectory =  fitGbl_traj.get_traj();
            if (_debug) System.out.format("KalmanToGBLDriver: track %d fit with %d points\n", tracks.indexOf(trk), gbl_fit_trajectory.getNumPoints());
            
            
            // Compute the residuals
            if (computeGBLResiduals) { 
                
                TrackResidualsData resData  = GblUtils.computeGblResiduals(trk, fitGbl_traj);
                trackResidualsCollection.add(resData);
                
                //For the moment I associate the residuals to the old KF track (They should be associated to the GBL Track)
                trackResidualsRelations.add(new BaseLCRelation(resData,trk));
                
                // Get the kinks
                GBLKinkData kinkData = fitGbl_traj.getKinks();
                kinkDataCollection.add(kinkData);
                kinkDataRelations.add(new BaseLCRelation(kinkData,trk));
                                      
            }//computeGBLResiduals
            
                      
            // Get the derivatives
            
            /*
            for (GblData gbldata : gbl_fit_trajectory.getTrajData()) {
            float vals[] = new float[2];
                List<Integer> indLocal = new ArrayList<Integer>();
                List<Double> derLocal = new ArrayList<Double>();
                List<Integer> labGlobal = new ArrayList<Integer>();
                List<Double> derGlobal = new ArrayList<Double>();
                
                gbldata.getAllData(vals, indLocal, derLocal, labGlobal, derGlobal);
                
                //Measurement
                if  (labGlobal.size() >=6 ) { 
                    for (int itag = 3; itag<=5; itag++) {
                        String derTag = String.valueOf(labGlobal.get(itag));
                        aidaGBL.histogram1D(derFolder+derTag).fill(derGlobal.get(itag));
                    }
                }
            }
            */
            
            // Create a GBL track from the fitted trajectory
            if (_analysis) {
                int trackType = 57;    // randomly chosen track type
                TrackState atIP = TrackStateUtils.getTrackStateAtIP(trk);
                HelicalTrackFit htkft = TrackUtils.getHTF(atIP);
                Pair<Track, GBLKinkData> GBLtkrPair = MakeGblTracks.makeCorrectedTrack(fitGbl_traj, htkft, trk.getTrackerHits(), trackType, bfield, true);
                newGBLtks.add(GBLtkrPair.getFirst());
            }
            
        }// track loop
        
        if (computeGBLResiduals) {
            event.put(trackResidualsColName,    trackResidualsCollection,  TrackResidualsData.class, 0);
            event.put(trackResidualsRelColName, trackResidualsRelations, LCRelation.class, 0);
            event.put(kinkDataCollectionName, kinkDataCollection, GBLKinkData.class, 0);
            event.put(kinkDataRelationsName, kinkDataRelations, LCRelation.class, 0);
            
        }
        
        // Analysis added by R. Johnson for debugging etc
        if (_debug) {
            if (residualsKF == null) System.out.format("KalmanToGBLDriver: residualsKF is null\n");
            if (residualsRelKF == null) System.out.format("KalmanToGBLDriver: residualsRelKF is null\n");
        }
        if (_analysis && residualsKF != null && residualsRelKF != null) {

            RelationalTable gblResidsRT = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            for (LCRelation relation : trackResidualsRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) { 
                    gblResidsRT.add(relation.getFrom(), relation.getTo());
                }
            }
            for (Track trk : tracks ) {
                Track trkGBL = newGBLtks.get(tracks.indexOf(trk));
                Set<TrackResidualsData> gblResids = gblResidsRT.allTo(trk);
                TrackState atIP_GBL = TrackStateUtils.getTrackStateAtIP(trkGBL);
                TrackState atIP_KF = TrackStateUtils.getTrackStateAtIP(trk);
                hDelPhi0.fill(atIP_KF.getPhi() - atIP_GBL.getPhi());
                hDelOmega.fill(atIP_KF.getOmega() - atIP_GBL.getOmega());
                hDelTanL.fill(atIP_KF.getTanLambda() - atIP_GBL.getTanLambda());
                hDelD0.fill(atIP_KF.getD0() - atIP_GBL.getD0());
                hDelZ0.fill(atIP_KF.getZ0() - atIP_GBL.getZ0());
                if (_debug) {                    
                    System.out.println("GBL Track state: " + atIP_GBL.toString());
                    System.out.println("KF Track state:  " + atIP_KF.toString());
                    System.out.format("Track %d has %d set of GBL residuals:\n", tracks.indexOf(trk), gblResids.size());
                }
                for (TrackResidualsData resData : gblResids) {
                    int vol = resData.getIntVal(resData.getNInt()-1);
                    if (_debug) System.out.format("  GBL residuals for tracker volume %d\n", vol);
                    for (int i=0; i<resData.getNDouble(); ++i) {
                        if (i%2 == 0) {
                            if (vol == 0) hGBLurt.fill(resData.getDoubleVal(i));
                            else hGBLurb.fill(resData.getDoubleVal(i));
                            if (_debug) System.out.format("   Layer %d, biased residual   = %12.6e +- %12.6e\n", resData.getIntVal(i), resData.getDoubleVal(i), resData.getFloatVal(i));
                        } else {
                            if (vol == 0) hGBLrt.fill(resData.getDoubleVal(i));
                            else hGBLrb.fill(resData.getDoubleVal(i));
                            if (_debug) System.out.format("   Layer %d, unbiased residual = %12.6e +- %12.6e\n", resData.getIntVal(i), resData.getDoubleVal(i), resData.getFloatVal(i));
                        }
                    }
                }
                Set<TrackResidualsData> kfResids = kfResidsRT.allTo(trk);
                if (_debug) System.out.format("Kalman track %d has %d set of residuals:\n", tracks.indexOf(trk), kfResids.size());
                for (TrackResidualsData resData : kfResids) {
                    int vol = resData.getIntVal(resData.getNInt()-1);
                    if (_debug) System.out.format("  KF residuals for tracker volume %d\n", vol);
                    for (int i=0; i<resData.getNDouble(); ++i) {
                        if (vol == 0) hKFurt.fill(resData.getDoubleVal(i));
                        else hKFurb.fill(resData.getDoubleVal(i));
                        if (_debug) System.out.format("   Layer %d, unbiased residual = %12.6e +- %12.6e\n", resData.getIntVal(i), resData.getDoubleVal(i), resData.getFloatVal(i));
                    }
                }
            }
        }
    }

    @Override
    protected void startOfData() {
    }

    @Override 
    protected void endOfData() {
        if (_analysis) {
            try {
                System.out.println("KalmanToGBLDriver: Outputting the histogram plots now.");
                aida.saveAs("KalmanToGBLDriverPlots.root");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    static Comparator<GBLStripClusterData>  arcLComparator = new Comparator<GBLStripClusterData>() {
        
        public int compare(GBLStripClusterData strip1, GBLStripClusterData strip2) {
            return strip1.getId() - strip2.getId();
        }
    };
    
}
