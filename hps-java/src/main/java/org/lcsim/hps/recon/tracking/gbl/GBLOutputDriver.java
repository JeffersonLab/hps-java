package org.lcsim.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.recon.tracking.EventQuality;
import org.lcsim.hps.recon.tracking.FieldMap;
import org.lcsim.hps.recon.tracking.TrackUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
* This driver is used to convert lcio input to a relative unstructured output format used as imput to GBL
* 
* We should port GBL to java.
* 
* @author Per Hansson Adrian <phansson@slac.stanford.edu>
* @version $Id: GBLOutputDriver.java,v 1.9 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
* 
*/
public class GBLOutputDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String[] detNames = {"Tracker"};
    int nevt = 0;
    GBLOutput gbl;
    TruthResiduals truthRes;
    private String gblFile = "gblinput.txt";
    int totalTracks=0;
    int totalTracksProcessed=0;
    private String outputPlotFileName="";
    private boolean hideFrame = false;
    private int _debug = 0;
    private String MCParticleCollectionName = "MCParticle";
    private int iTrack = 0;
    private int iEvent = 0;
    private boolean isMC = true;
    
    public void setDebug(int v) {
        this._debug = v;
    }
    public void setGblFileName(String filename) {
        gblFile = filename;
    }
    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    public void setIsMC(boolean isMC) {
        this.isMC = isMC;
    }

    
    public GBLOutputDriver() {
    }

    
    @Override
    public void detectorChanged(Detector detector) {
        Hep3Vector bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
        System.out.printf("%s: B-field in z %s\n",this.getClass().getSimpleName(),bfield.toString());
        gbl = new GBLOutput(gblFile,bfield);
        gbl.setDebug(_debug);
        gbl.buildModel(detector);
        gbl.setAPrimeEventFlag(false);
        gbl.setXPlaneFlag(false);
        truthRes = new TruthResiduals(bfield);
        truthRes.setDebug(_debug);
        truthRes.setHideFrame(hideFrame);
        FieldMap.printFieldMap();
        
    }
    
    
    
    @Override
    public void process(EventHeader event) {

        
        List<Track> tracklist = null;
        if(event.hasCollection(Track.class,"MatchedTracks")) {        
            tracklist = event.get(Track.class, "MatchedTracks");
             if(_debug>0) {
                System.out.printf("%s: Event %d has %d tracks\n", this.getClass().getSimpleName(),event.getEventNumber(),tracklist.size());
             }
        }


        List<MCParticle> mcParticles = new ArrayList<MCParticle>();
        if(event.hasCollection(MCParticle.class,this.MCParticleCollectionName)) {
        	mcParticles = event.get(MCParticle.class,this.MCParticleCollectionName);
        }

        List<SimTrackerHit> simTrackerHits = new ArrayList<SimTrackerHit>();
        if (event.hasCollection(SimTrackerHit.class, "TrackerHits")) {
        	simTrackerHits = event.getSimTrackerHits("TrackerHits");
        }
        
        if(isMC) {
        	truthRes.processSim(mcParticles, simTrackerHits);
        }
        
        
        List<Track> selected_tracks = new ArrayList<Track>();
        for (Track trk : tracklist) {
            totalTracks++;            
            if(TrackUtils.isGoodTrack(trk, tracklist, EventQuality.Quality.MEDIUM)) {
                if(_debug>0) System.out.printf("%s: Track failed selection\n", this.getClass().getSimpleName());
                selected_tracks.add(trk);
            }
        }


        //gbl.printNewEvent(event.getEventNumber());
        gbl.printNewEvent(iEvent,gbl.get_B().z());
            
        iTrack = 0;
        for (Track trk : selected_tracks) {
            if(_debug>0) System.out.printf("%s: Print GBL output for this track\n", this.getClass().getSimpleName());
            gbl.printTrackID(iTrack);
            gbl.printGBL(trk,mcParticles,simTrackerHits,this.isMC);
            totalTracksProcessed++;
            ++iTrack;
        }
        
        ++iEvent;
        
    }

    @Override
    public void endOfData() {
        gbl.close();
        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Events           = "+iEvent);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks           = "+totalTracks);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Processed = "+totalTracksProcessed);
        
        
    }
    
    
    
}
