/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.kalman;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trffit.HTrack;
import org.lcsim.util.Driver;

/**
 *
 * @author ecfine (ecfine@slac.stanford.edu)
 */


/* Takes Matched Tracks from the Track Reconstruction Driver, and makes TRF
 * HTracks. Adds hits from the original tracks to the new HTrack, and runs a
 * Kalman filter over the HTrack.
 *
 * This only does a forward fit, and multiple scattering is only included at
 * interacting planes, and just by assuming that each plane is .01 radiation
 * lengths. Energy loss is not accounted for. Additionally, the method for
 * constructing hits assumes that every hit occurs at an XY plane, while ideally,
 * there would be a method which checks the lcsim detector geometry and then
 * decides what kind of surface to model the shape as. There may be some methods
 * in the ShapeHelper interface (particularly in TrdHelper) that would be useful
 * for this, but nothing completed. Additionally, to run realistic multiple
 * scattering with non-interacting detector elements, there would need to be a
 * way to find the intercepts between a specific track and the detector elements.
 *
 * Also, magnetic field is just set at 1.0 in each class. It should be taken from
 * the detector geometry. */

public class KalmanFilterDriver extends Driver{
    ShapeDispatcher shapeDis = new ShapeDispatcher();
    TrackUtils trackUtils = new TrackUtils();
    Propagator prop = null;
    FullFitKalman fitk = null;
    KalmanGeom geom = null;
    Detector detector = null;
    HTrack ht = null;
    double bz = 0.5;


    
    public void detectorChanged(Detector det){
        detector = det;
        geom = new KalmanGeom(detector); // new geometry containing detector info
        prop = geom.newPropagator();
        System.out.println("geom field = " + geom.bz + ", trackUtils field = " + trackUtils.bz);
//        trackUtils.setBZ(geom.bz);
        fitk = new FullFitKalman(prop);
//     
    }

    
    public void process(EventHeader event){
        /* Get the tracklist for each event, and then for each track
         * get the starting track parameters and covariance matrix for an
         * outward fit from the seedtracker. */
         if (event.hasItem("MatchedTracks")) {
            List<Track> trklist = (List<Track>) event.get("MatchedTracks");
            System.out.println("number of tracks: " + trklist.size());
            for (int i = 0; i < trklist.size(); i++) {
                /* Start with a HelicalTrackFit, turn it into a VTrack,
                 * turn that into an ETrack, and turn that into an HTrack.
                 * Then add detector hits from the original track. */
                if(trklist.get(i).getTrackerHits().size()<4) {
                    System.out.println("Continue, this track has only " + trklist.get(i).getTrackerHits().size());
                    continue;
                }
                System.out.println("Making tracks...");
                Track track = trklist.get(i);
                HelicalTrackFit helicalTrack = shapeDis.trackToHelix(track);
                VTrack vt = trackUtils.makeVTrack(helicalTrack);
                TrackError initialError = trackUtils.getInitalError(helicalTrack);
                ETrack et = new ETrack (vt, initialError);
                ht = new HTrack(et);

                /* Add hits from original track */
                for (int k = 0; k < track.getTrackerHits().size(); k++) {
                    TrackerHit thit = track.getTrackerHits().get(k);
                    System.out.println("Adding hit...");
                    //ht = geom.addTrackerHit(thit, ht, helicalTrack, vt);
                    // phansson
                    // removing unused arguments to avoid confusion (see function for details)
                    ht = geom.addTrackerHit(thit, ht); 
                }


                
                /* Once we have an HTrack with the ordered list of hits, we pass
                 * this to the Kalman fitter. */
                System.out.println("Running Kalman fit...");
                int fstarf = fitk.fit(ht);
//                 System.out.println("geom field = " + geom.bz + ", trackUtils field = " + trackUtils.bz);
//                List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
//                System.out.println("SimTrackerHits info: ");
//                for(int j = 0; j < rawHits.size(); j++){
//                    List<SimTrackerHit> simHits = rawHits.get(j).getSimTrackerHits();
//                    for(int l = 0; l < simHits.size(); l++){
//                        System.out.println("point = [" + simHits.get(l).getPoint()[0]
//                                + ", " + simHits.get(l).getPoint()[1] +
//                                ", " + simHits.get(l).getPoint()[2] +
//                                "], momentum = [" + simHits.get(l).getMomentum()[0]
//                            + ", " + simHits.get(l).getMomentum()[1]
//                            +", " + simHits.get(l).getMomentum()[2] + "]");
//                    }
//                }
            }


        }
//        } else {
//            System.out.println("No tracks!");
//        }

    }


}
