package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;

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

import org.lcsim.util.aida.AIDA;
import java.io.IOException;
/**
 * A Driver which refits Kalman Tracks using GBL.
 *
 */

public class KalmanToGBLDriver extends Driver {
    
    private AIDA aidaGBL; 
    String derFolder = "/gbl_derivatives/";
    private HpsGblTrajectoryCreator _hpsGblTrajCreator;
    private String inputCollectionName = "KalmanFullTracks";
    private String trackResidualsColName = "TrackResidualsKFtoGBL";
    private String trackResidualsRelColName = "TrackResidualsKFtoGBLRelations";
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
        
        if (aidaGBL == null)
            aidaGBL = AIDA.defaultInstance();
        
        aidaGBL.tree().cd("/");
        setupPlots();
        
    }

    @Override
    protected void process(EventHeader event) {

        //Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection =  new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations          = new ArrayList<LCRelation>();
                
        //Get the track collection from the event

        if (!event.hasCollection(Track.class, inputCollectionName)) 
            return;

        List<Track> tracks = event.get(Track.class,inputCollectionName);
        
        if (_debug)
            System.out.println("Found tracks: " + inputCollectionName+" " + tracks.size());


        //Loop on Kalman Tracks 
        
        for (Track trk : tracks ) {

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

            //OLD way to refit. I'll use this for the moment.

            FittedGblTrajectory fitGbl_traj = HpsGblRefitter.fit(list_kfSCDs, bfac, false);
            GblTrajectory gbl_fit_trajectory =  fitGbl_traj.get_traj();
            
            
            // Compute the residuals
            if (computeGBLResiduals) { 
                
                TrackResidualsData resData  = GblUtils.computeGblResiduals(trk, fitGbl_traj);
                trackResidualsCollection.add(resData);
                
                //For the moment I associate the residuals to the old KF track (They should be associated to the GBL Track)
                trackResidualsRelations.add(new BaseLCRelation(resData,trk));
                
            }//computeGBLResiduals
            
            
            //kinkDataCollection.add(newTrack.getSecond());
            //kinkDataRelations.add(new BaseLCRelation(newTrack.getSecond(), gblTrk));

            // Get the derivatives
            
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
            
            

            
        } // track loop
        
        if (computeGBLResiduals) {
            event.put(trackResidualsColName,    trackResidualsCollection,  TrackResidualsData.class, 0);
            event.put(trackResidualsRelColName, trackResidualsRelations, LCRelation.class, 0);
        }
        
    }

    @Override
    protected void startOfData() {
    }

    @Override 
    protected void endOfData() {

        //Save the plots?
        
        try {
            aidaGBL.saveAs("KalmanToGBLDriverplots.root");
        }
        catch (IOException ex) {
        }
        
    }


    private void setupPlots() {
                
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
        int nbins = 500;
        List<Integer> minAxis = new ArrayList<Integer>();
        minAxis.add(-5);
        minAxis.add(-2);
        minAxis.add(-50);

        List<Integer> maxAxis = new ArrayList<Integer>();
        maxAxis.add(5);
        maxAxis.add(2);
        maxAxis.add(50);
        
        //Only rotations around w
        for (int ivol = 1; ivol<=2; ivol++) {
            for (int itype = 2; itype<=2;itype++) {
                for (int iaxis = 1; iaxis<=3; iaxis++) {
                    for (int is=0; is<=20; is++){
                        String derTag = String.valueOf(ivol*10000 + itype*1000 + iaxis*100 + is);
                        aidaGBL.histogram1D(derFolder+derTag,nbins,minAxis.get(iaxis-1),maxAxis.get(iaxis-1));
                    }//isensor
                }//iaxis
            }//itype
        }//ivol

        //Local derivatives 
        //wrt q/p 
        aidaGBL.histogram1D(derFolder+"df_dqop",nbins, -5,5);
        
        
        //d0 and z0 wrt beamspot

        aidaGBL.histogram1D("d0_vs_bs",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs",nbins,-0.200,0.200);

        aidaGBL.histogram1D("d0_vs_bs_meas",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_meas",nbins,-0.200,0.200);
        
        aidaGBL.histogram1D("d0_vs_bs_refit",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_refit",nbins,-0.200,0.200);


        aidaGBL.histogram1D("d0_vs_bs_refit_lcsim",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_refit_lcsim",nbins,-0.200,0.200);

        aidaGBL.histogram1D("d0_vs_bs_BSC_lcsim",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_BSC_lcsim",nbins,-0.200,0.200);
        
        
    }//setupPlots


    
    static Comparator<GBLStripClusterData>  arcLComparator = new Comparator<GBLStripClusterData>() {
        
        public int compare(GBLStripClusterData strip1, GBLStripClusterData strip2) {
            return strip1.getId() - strip2.getId();
        }
    };
    
}
