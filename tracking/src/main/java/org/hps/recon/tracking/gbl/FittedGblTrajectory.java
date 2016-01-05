package org.hps.recon.tracking.gbl;

import hep.physics.vec.Hep3Vector;

import java.util.Map;

import org.hps.recon.tracking.gbl.FittedGblTrajectory.GBLPOINT;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.event.Track;

/**
 * A class that collects information about a fitted GBL trajectory. 
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * 
 */
public class FittedGblTrajectory {

    public enum GBLPOINT {
        IP(0), LAST(1), VERTEX(2);
        private int numVal;
        GBLPOINT(int numVal) {
            this.numVal = numVal;
        }
        public int getNumVal() {
            return numVal;
        }
        public String toString() {
            String s;
            switch (numVal) {
                case 0: s = "VERTEX";
                        break;
                case 1: s = "LAST";
                        break;
                default:
                    s = "";
            }
            if( s.isEmpty() )
                throw new RuntimeException("This value " + Integer.toString(numVal) +  " is not valid.");
            return "GblPoint: " + s;
        }
        
    }
    
    public static enum GBLPARIDX {
        QOVERP(0),YTPRIME(1),XTPRIME(2),XT(3),YT(4);
        private int _value;
        private GBLPARIDX(int value) {
            _value = value;
        }
        public int getValue() {
            return _value;
        }
    };
    
    private GblTrajectory _traj;
    private double _chi2;
    private double _lost;
    private int _ndf;
    private Track _seed = null;
    private Map<Integer, Double> pathLengthMap = null;

    /**
     * Default constructor.
     * 
     * @param traj
     * @param chi2
     * @param ndf
     * @param lost
     */
    public FittedGblTrajectory(GblTrajectory traj, double chi2, int ndf, double lost) {
        _traj = traj;
        _chi2 = chi2;
        _ndf = ndf;
        _lost = lost;
    }
    
    /**
     * Find the index (or label) of the GBL point on the trajectory from the {@link GBLPOINT}.
     * @param point
     * @return
     */
    public int getPointIndex(GBLPOINT point) {
        int gblPointIndex;
        if (point.compareTo(GBLPOINT.IP) == 0)
            gblPointIndex = 1;
        else if (point.compareTo(GBLPOINT.LAST) == 0)
            gblPointIndex = _traj.getNumPoints();
        else 
            throw new RuntimeException("This GBL point " + point.toString() + "( " + point.name() + ") is not valid");
        return gblPointIndex;
    }
    
    
    /**
     * Find the corrections and covariance matrix for a particular {@link GBLPOINT}
     * @param point
     * @param locPar
     * @param locCov
     */
    public void getResults(GBLPOINT point, Vector locPar, SymMatrix locCov) {
        
        // find the GBL point index
        int gblPointIndex = getPointIndex(point);
        
        // Get the result from the trajectory
        int ok = _traj.getResults(gblPointIndex, locPar, locCov);
        
        // check that the fit was ok
        if( ok != 0)
            throw new RuntimeException("Trying to extract GBL corrections for fit that failed!?");
    }
    
    /**
     * Find the path length to this point.
     * @param point - {@link GBLPOINT} point
     * @return path length
     */
    public double getPathLength(GBLPOINT point) {
        int gblPointIndex = getPointIndex(point);
        return getPathLength(gblPointIndex);
    }
    
    /**
     * Find the path length to this point.
     * @param iLabel - GBL point index
     * @return path length
     */
    public double getPathLength(int iLabel) {
        if( !this.pathLengthMap.containsKey(iLabel) ) 
            throw new RuntimeException("This iLabel " + iLabel + " doesn't exists in the path length map.");
        return this.pathLengthMap.get(iLabel);
    }
    
    public void set_seed(Track seed) {
        _seed = seed;
    }
    public Track get_seed() {
        return _seed;
    }
    public GblTrajectory get_traj() {
        return _traj;
    }
    public double get_chi2() {
        return _chi2;
    }
    public double get_lost() {
        return _lost;
    }
    public int get_ndf() {
        return _ndf;
    }

    public void setPathLengthMap(Map<Integer, Double> pathLengthMap) {
        this.pathLengthMap = pathLengthMap;
    }
    
    public Map<Integer, Double> getPathLengthMap() {
        if (this.pathLengthMap == null)
            throw new RuntimeException("No path length map has been set on this trajectory!");
        return this.pathLengthMap;
    }

    

    
}