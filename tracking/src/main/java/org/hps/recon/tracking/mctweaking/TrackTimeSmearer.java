package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random; 
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.event.ReconstructedParticle;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackDataDriver;
/**
 *
 * @author mgraham created 1/29/24
 * 
 * Extension of SlopeBasedTrackHitKiller to work on Kalman Tracks
 * This driver will remove 1d strip clusters from the
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * collection based on a track-slope efficiency file (obtained from L1/no L1 WAB events)
 * mg...this only works for L1 module at the moment
 * mg...the official hit killing as of 2024 is StripHitKiller.java...
 * mg...this is just included for posterity
 * 
 */
public class TrackTimeSmearer extends Driver {
   
    String trackCollectionName = "KalmanFullTracks";
    boolean debug = false;
    double smearBottom = 0;
    double smearTop = 0;
    Random r=new Random();    


    public void setSmearBottom(double smear) {
        this.smearBottom = smear;
    }

     public void setSmearTop(double smear) {
        this.smearTop = smear;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public TrackTimeSmearer() {
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasItem(trackCollectionName)) {
            System.out.println("TrackTimeSmearer::process No Input Collection Found?? " + trackCollectionName);
            return;
        }

         if (!event.hasCollection(TrackData.class,TrackData.TRACK_DATA_COLLECTION)
                || !event.hasCollection(LCRelation.class, TrackData.TRACK_DATA_RELATION_COLLECTION)) {
            System.out.println("No TrackData");
            return;
        }
              
        List<Track> tracks = event.get(Track.class, trackCollectionName);  

        //System.out.println("Collection Name:  " + TrackData.TRACK_DATA_COLLECTION);
        //System.out.println("Collection Name:  " + TrackData.TRACK_DATA_RELATION_COLLECTION);
 

        //Loop over tracks
        for (Track trk : tracks) {
            double trkTime = TrackData.getTrackTime(TrackData.getTrackData(event, trk));
            int trkVolume = TrackData.getTrackVolume(TrackData.getTrackData(event, trk));
            double smearingPosition = 0;
            if (trkVolume == 0) {smearingPosition = smearTop; }
            else if (trkVolume == 1) {smearingPosition = smearBottom; }
            TrackToSmear trackSmear = new TrackToSmear(trkVolume,smearingPosition);
            double smearAmount=trackSmear.getRandomTimeSmear();
		    double newTime = trkTime+smearAmount;
            if (debug)
			System.out.println("Smearing Track in Volume = " +trkVolume				  
					   +" smearTimeSigma= " + smearingPosition
					   + "  old time = " + trkTime
					   + "  new time = " + newTime
					   + "  amount smeared = " + smearAmount); 
            TrackData trkData = (TrackData)TrackData.getTrackData(event, trk);
            trkData.setTrackTime((float)newTime);
            //System.out.println("TrackTime:  " + trkTime);
            //double totalT0 = 0;
            //int nHits = trk.getTrackerHits().size();
            //for (TrackerHit stripCluster : trk.getTrackerHits()) {
            //    double t0 = stripCluster.getTime();
            //    totalT0 += t0;
            //}
            //double t0_time = totalT0/nHits;
            //System.out.println("TrackTime Test:  " + t0_time);
        }
    }


    public class TrackToSmear {

        int _volume = -1;
        double _smearTimeSigma = -666.0; // units of this is ns

        public TrackToSmear(int volume, double smearTime) {
	        _smearTimeSigma = smearTime;
            _volume = volume;
        }
		
        void setSmearTimeSigma(double smear) {
            this._smearTimeSigma=smear;
        }
	     
        double getSmearTimeSigma() {
            return this._smearTimeSigma;
        }
	       
	    double getRandomTimeSmear(){
	        if(this._smearTimeSigma > 0)		
		    return r.nextGaussian()*this._smearTimeSigma;
	        else
		    return 0.0;
	    }
    }

    @Override
    public void endOfData() {
    }
}
