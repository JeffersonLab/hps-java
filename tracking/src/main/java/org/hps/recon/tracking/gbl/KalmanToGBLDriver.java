package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;

//import hep.physics.vec.Hep3Vector;
//import hep.physics.vec.BasicHep3Vector;
//import org.lcsim.event.TrackState;

import org.lcsim.event.Track;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;

import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;

import org.lcsim.constants.Constants;
import org.hps.recon.tracking.TrackResidualsData;
import org.lcsim.event.base.BaseLCRelation;

//import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.hps.recon.tracking.TrackUtils;

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
    private Boolean constrainedBSFit = false;
    private double bfield;
    private Boolean computeGBLResiduals = true;
        
    void setDebug(boolean val) {
        _debug = val;
    }
    

    @Override 
    protected void detectorChanged(Detector detector) {
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        
    }

    @Override
    protected void process(EventHeader event) {

        //Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection =  new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations          = new ArrayList<LCRelation>();


        //Kinks 
        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();
        
        //Get the track collection from the event

        if (!event.hasCollection(Track.class, inputCollectionName)) 
            return;

        List<Track> tracks = event.get(Track.class,inputCollectionName);
        
        if (_debug)
            System.out.println("Found tracks: " + inputCollectionName+" " + tracks.size());


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
            
                        
            //Get the GBLStripClusterData
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
            
            //Get the strip cluster data
            Set<GBLStripClusterData> kfSCDs = kfSCDsRT.allFrom(trk);
            
            
            if (_debug) 
                System.out.println("Kalman Strip Cluster data size: " + kfSCDs.size());
            
                        
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

            /*
            System.out.println("DEBUG::Tom::KalmanToGBLDriver - converted KF track to GBL track with "
                + gbl_fit_trajectory.getNumPoints() + " hits");
             */
            
            
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
            
        }// track loop
        
        if (computeGBLResiduals) {
            event.put(trackResidualsColName,    trackResidualsCollection,  TrackResidualsData.class, 0);
            event.put(trackResidualsRelColName, trackResidualsRelations, LCRelation.class, 0);
            event.put(kinkDataCollectionName, kinkDataCollection, GBLKinkData.class, 0);
            event.put(kinkDataRelationsName, kinkDataRelations, LCRelation.class, 0);
            
        }
    }

    @Override
    protected void startOfData() {
    }

    @Override 
    protected void endOfData() {

    }
    
    static Comparator<GBLStripClusterData>  arcLComparator = new Comparator<GBLStripClusterData>() {
        
        public int compare(GBLStripClusterData strip1, GBLStripClusterData strip2) {
            return strip1.getId() - strip2.getId();
        }
    };
    
}
