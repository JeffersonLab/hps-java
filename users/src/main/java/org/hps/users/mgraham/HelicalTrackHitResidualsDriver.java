package org.hps.users.mgraham;

import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.ref.plotter.PlotterRegion;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.EventQuality;
import org.hps.recon.tracking.TrackUtils;
import org.hps.users.phansson.testrun.TrigRateDriver;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class HelicalTrackHitResidualsDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private int totalTracks=0;
    private int totalTracksProcessed=0;
    private String outputPlotFileName="";
    private String trackCollectionName="MatchedTracks";
    private boolean hideFrame = false;
    private boolean _debug = false;
    private boolean _includeMS = true;
    
    //private AIDAFrame plotterFrame;
//    ICloud1D[] _h_resz_track_top = new ICloud1D[5];
//    ICloud1D[] _h_resz_track_bottom = new ICloud1D[5];
//    ICloud1D[] _h_resy_track_top = new ICloud1D[5];
//    ICloud1D[] _h_resy_track_bottom = new ICloud1D[5];
    private IHistogram1D[] _h_resz_track_top = new IHistogram1D[5];
    private IHistogram1D[] _h_resz_track_bottom = new IHistogram1D[5];
    private IHistogram1D[] _h_resy_track_top = new IHistogram1D[5];
    private IHistogram1D[] _h_resy_track_bottom = new IHistogram1D[5];
    private IDataPointSet dps_hth_y_b;
    private IDataPointSet dps_hth_y_t;
    private IDataPointSet dps_hth_z_b;
    private IDataPointSet dps_hth_z_t;
    private IPlotter _plotter_resz_top;
    private IPlotter _plotter_resy_top;
    private IPlotter _plotter_resz_bottom;
    private IPlotter _plotter_resy_bottom;
    private IPlotter _plotter_mean_res;
    
       
    
    
    public void setDebug(boolean v) {
        this._debug = v;
    }
    
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }
    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    
    public void setIncludeMS(boolean inc) {
        this._includeMS = inc;
    }
    
    public HelicalTrackHitResidualsDriver() {
    
    }
    

    
    @Override
    public void detectorChanged(Detector detector) {
        
        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        Hep3Vector _bfield = new BasicHep3Vector(0,0,detector.getFieldMap().getField(IP).y());

        makePlots();
       
        
    }
    
   

    
    @Override
    public void process(EventHeader event) {

        List<Track> tracklist = new ArrayList<Track>();
        if(event.hasCollection(Track.class,this.trackCollectionName)) {        
            tracklist = event.get(Track.class, this.trackCollectionName);
             if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Number of Tracks = " + tracklist.size());
             }
        }

        
        
        for (Track track : tracklist) {
            
            if(!TrackUtils.isGoodTrack(track, tracklist, EventQuality.Quality.MEDIUM)) {
                continue;
            }
            
            SeedTrack st = (SeedTrack) track;
            SeedCandidate seed = st.getSeedCandidate();
            HelicalTrackFit trk = seed.getHelix();
            List<TrackerHit> hitsOnTrack = track.getTrackerHits();
            for(TrackerHit hit : hitsOnTrack) {
                HelicalTrackHit hth = (HelicalTrackHit) hit;
                //HelicalTrackCross htc = (HelicalTrackCross) hth;
                //System.out.printf("%s: getHitMap: hth position before trkdir: (%.3f,%.3f,%.3f)\n",this.getClass().getSimpleName(),hth.x(),hth.y(),hth.z());
                //htc.setTrackDirection(trk);
                Map<String,Double> res_track = TrackUtils.calculateTrackHitResidual(hth, trk, this._includeMS);
                boolean isTop = false;
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hth.getRawHits().get(0)).getDetectorElement();
                //===> if(SvtUtils.getInstance().isTopLayer((SiSensor)((RawTrackerHit)hth.getRawHits().get(0)).getDetectorElement())) {
                if(sensor.isTopLayer()) {
                    isTop = true;
                }
                int layer = hth.Layer();
                if(_debug) System.out.println(this.getClass().getSimpleName() + ": residual for hit at " + hth.toString() + " and layer " + layer);
                if(layer%2==0) {
                    System.out.println(this.getClass().getSimpleName() + ": HTH layer is not odd!" + layer);
                    System.exit(1);
                }
                int layer_idx = (layer-1)/2;
                if(isTop) {
                    this._h_resz_track_top[layer_idx].fill(res_track.get("resz"));
                    this._h_resy_track_top[layer_idx].fill(res_track.get("resy"));
                } else {
                    this._h_resz_track_bottom[layer_idx].fill(res_track.get("resz"));
                    this._h_resy_track_bottom[layer_idx].fill(res_track.get("resy"));
                }

//                if(Math.abs(res_track.get("resy"))>0.02) {
//                    System.out.println(this.getClass().getSimpleName() + ": this has large y res = " + res_track.get("resy"));
//                    System.exit(1);
//                }
                
            }
            
        }
        totalTracks++;
        totalTracksProcessed++;

        

        if(totalTracks%50==0) this.updatePlots();

        
        
    }

    public void endOfData() {
        this.updatePlots();
        //try {
            System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Found = "+totalTracks);
            System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Processed = "+totalTracksProcessed);
//        } catch (IOException ex) {
//            Logger.getLogger(CmpGenToFittedTracksDriver.class.getName()).log(Level.SEVERE, null, ex);
//        }
        if (!"".equals(outputPlotFileName))
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        
    }
    
   
    private void makePlots() {
        
        int nbins = 50;
        double bins_resz_min[] = {-0.15,-0.4,-0.6,-1.0,-1.5};
        double bins_resz_max[] = {0.15,0.4,0.6,1.0,1.5};
        double bins_resy_min[] = {-0.4,-0.6,-1.0,-1.5,-1.8};
        double bins_resy_max[] = {0.4,0.6,1.0,1.5,1.8};
        
        
        _plotter_resz_top = aida.analysisFactory().createPlotterFactory().create();
        _plotter_resz_top.setTitle("res z top");
        _plotter_resz_top.createRegions(5,1);
        _plotter_resy_top = aida.analysisFactory().createPlotterFactory().create();
        _plotter_resy_top.setTitle("res y top");
        _plotter_resy_top.createRegions(5,1);
        for(int i=1;i<6;++i) {
//            _h_resz_track_top[i-1] = aida.cloud1D("h_resz_track_top_layer"+i);
//            _h_resy_track_top[i-1] = aida.cloud1D("h_resy_track_top_layer"+i);
            _h_resz_track_top[i-1] = aida.histogram1D("h_resz_track_top_layer"+i,nbins,bins_resz_min[i-1],bins_resz_max[i-1]);
            _h_resy_track_top[i-1] = aida.histogram1D("h_resy_track_top_layer"+i,nbins,bins_resy_min[i-1],bins_resy_max[i-1]);
            _plotter_resz_top.region(i-1).plot(_h_resz_track_top[i-1]);
            _plotter_resy_top.region(i-1).plot(_h_resy_track_top[i-1]);
        }
        
        _plotter_resz_bottom = aida.analysisFactory().createPlotterFactory().create();
        _plotter_resz_bottom.setTitle("res z bottom");
        _plotter_resz_bottom.createRegions(5,1);
        _plotter_resy_bottom = aida.analysisFactory().createPlotterFactory().create();
        _plotter_resy_bottom.setTitle("res y bottom");
        _plotter_resy_bottom.createRegions(5,1);
        for(int i=1;i<6;++i) {
            _h_resz_track_bottom[i-1] = aida.histogram1D("h_resz_track_bottom_layer"+i,nbins,bins_resz_min[i-1],bins_resz_max[i-1]);
            _h_resy_track_bottom[i-1] = aida.histogram1D("h_resy_track_bottom_layer"+i,nbins,bins_resy_min[i-1],bins_resy_max[i-1]);
//            _h_resz_track_bottom[i-1] = aida.cloud1D("h_resz_track_bottom_layer"+i);
//            _h_resy_track_bottom[i-1] = aida.cloud1D("h_resy_track_bottom_layer"+i);
            _plotter_resz_bottom.region(i-1).plot(_h_resz_track_bottom[i-1]);
            _plotter_resy_bottom.region(i-1).plot(_h_resy_track_bottom[i-1]);
        
        }
        
        _plotter_mean_res = aida.analysisFactory().createPlotterFactory().create();
        _plotter_mean_res.setTitle("Mean res y");
        _plotter_mean_res.createRegions(2,2);
        
        IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(null);
        
        dps_hth_y_b = dpsf.create("dps_hth_y_b", "Mean of y residual bottom",2);
        dps_hth_y_t = dpsf.create("dps_hth_y_t", "Mean of y residual top",2);
        _plotter_mean_res.region(1).plot(dps_hth_y_b);
        _plotter_mean_res.region(0).plot(dps_hth_y_t);

        dps_hth_z_b = dpsf.create("dps_hth_z_b", "Mean of z residual bottom",2);
        dps_hth_z_t = dpsf.create("dps_hth_z_t", "Mean of z residual top",2);
        _plotter_mean_res.region(3).plot(dps_hth_z_b);
        _plotter_mean_res.region(2).plot(dps_hth_z_t);

        ((PlotterRegion)_plotter_mean_res.region(0)).getPlot().setAllowUserInteraction(true);
        ((PlotterRegion) _plotter_mean_res.region(0)).getPlot().setAllowPopupMenus(true);
        ((PlotterRegion)_plotter_mean_res.region(1)).getPlot().setAllowUserInteraction(true);
        ((PlotterRegion) _plotter_mean_res.region(1)).getPlot().setAllowPopupMenus(true);
        
        
        /*
        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("HTH Residuals");
        plotterFrame.addPlotter(_plotter_resz_top);
        plotterFrame.addPlotter(_plotter_resz_bottom);
        plotterFrame.addPlotter(_plotter_resy_top);
        plotterFrame.addPlotter(_plotter_resy_bottom);
        plotterFrame.addPlotter(_plotter_mean_res);
        plotterFrame.pack();
        plotterFrame.setVisible(!hideFrame);
        */
        
    }
    
    
    void updatePlots() {
        dps_hth_y_t.clear();
        dps_hth_z_t.clear();
        dps_hth_z_b.clear();
        dps_hth_z_t.clear();
        
         for(int i=1;i<6;++i) {
                
            double mean = this._h_resy_track_bottom[i-1].mean();
            double stddev = this._h_resy_track_bottom[i-1].rms();
            double N =  this._h_resy_track_bottom[i-1].entries();
            double error = N >0 ? stddev/Math.sqrt(N) : 0; 
            dps_hth_y_b.addPoint();
            dps_hth_y_b.point(i-1).coordinate(1).setValue(mean);
            dps_hth_y_b.point(i-1).coordinate(1).setErrorPlus(error);
            dps_hth_y_b.point(i-1).coordinate(0).setValue(i);
            dps_hth_y_b.point(i-1).coordinate(0).setErrorPlus(0);
            
            mean = this._h_resy_track_top[i-1].mean();
            stddev = this._h_resy_track_top[i-1].rms();
            N =  this._h_resy_track_top[i-1].entries();
            error = N >0 ? stddev/Math.sqrt(N) : 0; 
            dps_hth_y_t.addPoint();
            dps_hth_y_t.point(i-1).coordinate(1).setValue(mean);
            dps_hth_y_t.point(i-1).coordinate(1).setErrorPlus(error);
            dps_hth_y_t.point(i-1).coordinate(0).setValue(i);
            dps_hth_y_t.point(i-1).coordinate(0).setErrorPlus(0);
            
            mean = this._h_resz_track_top[i-1].mean();
            stddev = this._h_resz_track_top[i-1].rms();
            N =  this._h_resz_track_top[i-1].entries();
            error = N >0 ? stddev/Math.sqrt(N) : 0; 
            dps_hth_z_t.addPoint();
            dps_hth_z_t.point(i-1).coordinate(1).setValue(mean);
            dps_hth_z_t.point(i-1).coordinate(1).setErrorPlus(error);
            dps_hth_z_t.point(i-1).coordinate(0).setValue(i);
            dps_hth_z_t.point(i-1).coordinate(0).setErrorPlus(0);

            mean = this._h_resz_track_bottom[i-1].mean();
            stddev = this._h_resz_track_bottom[i-1].rms();
            N =  this._h_resz_track_bottom[i-1].entries();
            error = N >0 ? stddev/Math.sqrt(N) : 0; 
            dps_hth_z_b.addPoint();
            dps_hth_z_b.point(i-1).coordinate(1).setValue(mean);
            dps_hth_z_b.point(i-1).coordinate(1).setErrorPlus(error);
            dps_hth_z_b.point(i-1).coordinate(0).setValue(i);
            dps_hth_z_b.point(i-1).coordinate(0).setErrorPlus(0);
            
            
           
            

         }
        
        
        
        
    }
    
    
}
