package org.hps.recon.tracking.nobfield;

import org.lcsim.fit.helicaltrack.HelicalTrackHit;

/**
 *
 * @author mgraham
 */
public class HitOnTrackChecker {

    //define some criteria for adding this hit to  a track prior to track fit
    private double _maxDeltaX;
    private double _maxDeltaY;

    public HitOnTrackChecker() {

    }

    public boolean checkNewHit(StraightTrack trk, HelicalTrackHit newhit) {

        return true;
    }

    public void setDeltaX(double del) {
        _maxDeltaX = del;
    }

    public void setDeltaY(double del) {
        _maxDeltaY = del;
    }

}
