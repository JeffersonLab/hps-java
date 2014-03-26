/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IProfile1D;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.HPSFittedRawTrackerHit;
import org.hps.recon.tracking.TrackAnalysis;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class TestAnalysisDriver extends Driver {

    int nevents = 0;
    int naccepted = 0;
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    public String outputPlots = "myplots.aida";
    Map<String, IProfile1D> clsizeMap = new HashMap<String, IProfile1D>();
    String[] detNames = {"Tracker"};
    Integer[] nlayers = {10};

    public void process(
            EventHeader event) {
        nevents++;

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        List<HPSFittedRawTrackerHit> fittedrawHits = event.get(HPSFittedRawTrackerHit.class, "SVTFittedRawTrackerHits");

        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
    
       
       
Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        RelationalTable nearestHitToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        Map<Track, Double> l1Isolation = new HashMap<Track, Double>();
        Map<Track, Double> l1DeltaZ = new HashMap<Track, Double>();
        
        String fittedHitsDir = "FittedHits/";
        for(HPSFittedRawTrackerHit hrth:fittedrawHits){
            double fittedAmp=hrth.getAmp();
            double fittedT0=hrth.getT0();
            String sensorName=hrth.getRawTrackerHit().getDetectorElement().getName();
            aida.cloud1D(fittedHitsDir + sensorName+" fitted Amplitude").fill(fittedAmp);
            aida.cloud1D(fittedHitsDir + sensorName+"fitted T0").fill(fittedT0);
        }
        
        
        /*
         List<Track> tracklist = event.get(Track.class, "MatchedTracks");
        List<HelicalTrackHit> hthits = event.get(HelicalTrackHit.class, "HelicalTrackHits");
        aida.cloud1D("HelicalTrackHits per Event").fill(hthits.size());
        aida.cloud1D("Matched Tracks per Event").fill(tracklist.size());
        String trackdir = "TrackInfo/";
        //  Analyze the tracks in the event
        for (Track track : tracklist) {
            //  Calculate the track pT and cos(theta)
            double d0 = track.getTrackParameter(HelicalTrackFit.dcaIndex);
            double z0 = track.getTrackParameter(HelicalTrackFit.z0Index);
            double phi0 = track.getTrackParameter(HelicalTrackFit.phi0Index);
            double slope = track.getTrackParameter(HelicalTrackFit.slopeIndex);
            double curve = track.getTrackParameter(HelicalTrackFit.curvatureIndex);
            double d0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex));
            double z0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index));
            double phi0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index));
            double slopeErr = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex));
            double curveErr = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex));
             SeedTrack stEle = (SeedTrack) track;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            
            double xoca = ht.x0();
            double yoca = ht.y0();
            double[] poca = {xoca, yoca, z0};
            double mom[] = track.getMomentum();
            double px = mom[0];
            double py = mom[1];
            double pz = mom[2];
            double pperp = Math.sqrt(py * py + pz * pz);
            double pt = Math.sqrt(px * px + py * py);
            double p = Math.sqrt(pt * pt + pz * pz);
            double phi = Math.atan2(py, px);
            double cth = pz / Math.sqrt(pt * pt + pz * pz);
            double sth = pt / Math.sqrt(pt * pt + pz * pz);
            double th = Math.atan2(pt, pz);
             fillTrackInfo(trackdir, "all tracks", track.getChi2(), p, pperp, px, py, pz, phi, cth, d0, xoca, yoca, z0);
        }        
        */
    }
  private void fillTrackInfo(String dir, String species, double chi2,  double p, double pperp, double px, double py, double pz, double phi, double cth, double doca, double xoca, double yoca, double zoca) {
        aida.cloud1D(dir + "total chi^2 for  " + species).fill(chi2);

//                aida.cloud1D(trackdir + "circle chi^2 for  " + species).fill(ht.chisq()[0]);
//                aida.cloud1D(trackdir + "linear chi^2 for  " + species).fill(ht.chisq()[1]       
        aida.cloud1D(dir + "p for  " + species).fill(p);
        aida.cloud1D(dir + "pperp for  " + species).fill(pperp);
        aida.cloud1D(dir + "px for  " + species).fill(px);
        aida.cloud1D(dir + "py for  " + species).fill(py);
        aida.cloud1D(dir + "pz for  " + species).fill(pz);
        aida.cloud1D(dir + "phi for  " + species).fill(phi);
        aida.cloud1D(dir + "cos(theta) for  " + species).fill(cth);
        aida.cloud1D(dir + "DOCA for  " + species).fill(doca);
        aida.cloud1D(dir + "XOCA for  " + species).fill(xoca);
        aida.cloud1D(dir + "YOCA for  " + species).fill(yoca);
        aida.cloud1D(dir + "ZOCA for  " + species).fill(zoca);
        aida.cloud2D(dir + "doca vs xoca for  " + species).fill(xoca, doca);
    }
    public void endOfData() {
   try {
            aida.saveAs(outputPlots);
        } catch (IOException ex) {
            Logger.getLogger(DetailedAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
}
