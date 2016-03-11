package org.hps.recon.tracking.axial;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import java.util.List;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;

/**
 * Encapsulate 2D hit info needed by HelicalTrackFitter. 
 * This class is explicitly for HPS where the length of the
 * sensors are (mostly) along the detector 
 * y-dimension ( == HelicalTrackFit x-dimension);
 * Copied/Modified from org.lcsim.recon.tracking.helicaltrack.HelicalTrack2DHit.java
 * @author Matt Graham <mgraham@slac.stanford.edu>
 */
public class HelicalTrack2DHit  extends HelicalTrackHit {
    private double _axmin;//min value along the bend-direction..
    private double _axmax;//max value along the bend-direction..
    private static int _type = 2;
    private Hep3Vector _axialDirection;//the direction along the strip length
    
    /**
     * Create a HelicalTrack2DHit from the associated TrackerHit and HelicalTrackStrip.
     * @param pos location of the strip center
     * @param cov covariance matrix for the hit
     * @param dEdx deposited energy
     * @param time hit time
     * @param rawhits list of raw hits
     * @param detname detector name
     * @param layer layer number
     * @param beflag
     */
    public HelicalTrack2DHit(Hep3Vector pos, SymmetricMatrix cov, double dEdx, double time,
            List rawhits, String detname, int layer, BarrelEndcapFlag beflag, double zmin, double zmax, Hep3Vector axDir) {
        super(pos, cov, dEdx, time, _type, rawhits, detname, layer, beflag);
        _axmin = zmin;
        _axmax = zmax;        
        _axialDirection=axDir;
    }
    
    /**
     * Return the minimum z coordinate.
     * @return minimum z coordinate
     */
    public double axmin() {
        return _axmin;
    }
    
    /**
     * Return the maximum z coordinate.
     * @return maximum z coordinate
     */
    public double axmax() {
        return _axmax;
    }

    /**
     * Return the length of the strip along the z direction.
     * @return strip length
     */
    public double axlen() {
        return _axmax - _axmin;
    }
    
    public Hep3Vector axialDirection(){
        return _axialDirection;
    }
}