package org.hps.recon.tracking.gbl;

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
    private GBLTrackData _t = null;
    public FittedGblTrajectory(GblTrajectory traj, double chi2, int ndf, double lost) {
        _traj = traj;
        _chi2 = chi2;
        _ndf = ndf;
        _lost = lost;
    }
    public void set_track_data(GBLTrackData t) {
       _t  = t;
    }
    public GBLTrackData get_track_data() {
        return _t;
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
    
}