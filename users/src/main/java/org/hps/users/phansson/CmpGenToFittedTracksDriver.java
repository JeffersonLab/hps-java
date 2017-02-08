package org.hps.users.phansson;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.users.phansson.testrun.TrigRateDriver;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class CmpGenToFittedTracksDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    int totalTracks=0;
    int totalTracksProcessed=0;
    private String outputPlotFileName="";
    private boolean hideFrame = false;
    private boolean _debug = false;
    
    //private AIDAFrame plotterFrame;
    ICloud1D _h_chi2;
    ICloud1D _h_ntracksdiff;
    ICloud1D _h_d0_diff;
    ICloud2D _h_d0xy_diff;
    ICloud1D _h_z0_diff;
    ICloud1D _h_phi0_diff;
    ICloud1D _h_R_diff;
    ICloud1D _h_slope_diff;
    IPlotter _plotter_trackparamdiff;
    IPlotter _plotter_others;
     
       
    
    
    public void setDebug(boolean v) {
        this._debug = v;
    }
    
    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }
    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    
    public CmpGenToFittedTracksDriver() {
    
    }
    

    
    @Override
    public void detectorChanged(Detector detector) {
        
        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        Hep3Vector _bfield = new BasicHep3Vector(0,0,detector.getFieldMap().getField(IP).y());

        makePlots();
       
        
    }
    
    
    
    @Override
    public void process(EventHeader event) {

        List<HelicalTrackFit> tracklistgen = new ArrayList<HelicalTrackFit>();
        if(event.hasCollection(HelicalTrackFit.class,"MCParticle_HelicalTrackFit")) {        
            tracklistgen = event.get(HelicalTrackFit.class, "MCParticle_HelicalTrackFit");
             if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Number of generated Tracks = " + tracklistgen.size());
             }
        }
        
        List<Track> tracklist = new ArrayList<Track>();
        if(event.hasCollection(Track.class,"MatchedTracks")) {        
            tracklist = event.get(Track.class, "MatchedTracks");
             if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Number of Tracks = " + tracklist.size());
             }
        }
        
        _h_ntracksdiff.fill(tracklistgen.size()-tracklist.size());
       
        if(tracklistgen.size()!=tracklist.size()) {
            if(this._debug) System.out.println(this.getClass().getSimpleName() + ": tracklistgen.size() = " + tracklistgen.size() + " tracklist.size()=" + tracklist.size());
            return;
        }
        for (HelicalTrackFit trkgen : tracklistgen) {
            for (Track track : tracklist) {
                SeedTrack st = (SeedTrack) track;              
                SeedCandidate seed = st.getSeedCandidate();
                HelicalTrackFit trk = seed.getHelix();
                List<TrackerHit> hitsOnTrack = track.getTrackerHits();
                //if(Math.signum(trkgen.R())!=Math.signum(trk.R())) continue;
                this._h_d0_diff.fill(trkgen.dca()-trk.dca());
                this._h_d0xy_diff.fill(trkgen.dca(),trk.dca());
                this._h_R_diff.fill(trkgen.R()-trk.R());
                this._h_phi0_diff.fill(trkgen.phi0()-trk.phi0());
                this._h_z0_diff.fill(trkgen.z0()-trk.z0());
                this._h_slope_diff.fill(trkgen.slope()-trk.slope());
                this._h_chi2.fill(trk.chisqtot());
            }
            totalTracks++;
            totalTracksProcessed++;
            
        }

        
        
        
    }

    public void endOfData() {
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
        
         
        _h_chi2 = aida.cloud1D("chi2");
        _h_ntracksdiff = aida.cloud1D("ntracksdiff");
        _h_R_diff = aida.cloud1D("R_diff");
        _h_d0_diff = aida.cloud1D("d0_diff");
        _h_d0xy_diff = aida.cloud2D("d0xy_diff");
        _h_phi0_diff = aida.cloud1D("phi0_diff");
        _h_slope_diff = aida.cloud1D("slope_diff");
        _h_z0_diff = aida.cloud1D("z0_diff");
        _plotter_trackparamdiff = aida.analysisFactory().createPlotterFactory().create();
        _plotter_trackparamdiff.setTitle("Params diff");
        _plotter_trackparamdiff.createRegions(2,3);
        _plotter_trackparamdiff.region(0).plot(_h_R_diff);
        _plotter_trackparamdiff.region(1).plot(_h_d0_diff);
        _plotter_trackparamdiff.region(2).plot(_h_phi0_diff);
        _plotter_trackparamdiff.region(3).plot(_h_z0_diff);
        _plotter_trackparamdiff.region(4).plot(_h_slope_diff);
        _plotter_trackparamdiff.region(5).plot(_h_d0xy_diff);
        
        this._plotter_others = aida.analysisFactory().createPlotterFactory().create();
        this._plotter_others.setTitle("Other");
        this._plotter_others.createRegions(1,2);
        this._plotter_others.region(0).plot(this._h_ntracksdiff);
        this._plotter_others.region(1).plot(this._h_chi2);
        
        
        
        
        
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("Compare Generated and Fitted Tracks");
        //plotterFrame.addPlotter(_plotter_trackparamdiff);
        //plotterFrame.addPlotter(_plotter_others);
        //plotterFrame.pack();
        //plotterFrame.setVisible(!hideFrame);
        
    }
    
    
}
