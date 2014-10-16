package org.hps.recon.tracking.nobfield;

import org.lcsim.fit.helicaltrack.HelicalTrackHit;

/**
 *
 * @author mgraham
 */
public class TrackChecker {

    //define some criteria for keeping/rejecting a track
    
    public TrackChecker() {

    }

    public boolean checkTrack(StraightTrack trk) {

        return true;
    }

    public boolean checkSeed(StraightTrack trk) {

        return true;
    }

}
