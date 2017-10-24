package org.hps.recon.tracking.gbl;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;

/**
 * A class that collects information about a fitted GBL trajectory.
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class FittedGblTrajectory {

    public static Logger LOGGER = Logger.getLogger(FittedGblTrajectory.class.getName());

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
                case 0:
                    s = "VERTEX";
                    break;
                case 1:
                    s = "LAST";
                    break;
                default:
                    s = "";
            }
            if (s.isEmpty())
                throw new RuntimeException("This value " + Integer.toString(numVal) + " is not valid.");
            return "GblPoint: " + s;
        }

    }

    public static enum GBLPARIDX {
        QOVERP(0), YTPRIME(1), XTPRIME(2), XT(3), YT(4);

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
     * 
     * @param point
     * @return the index of the GBL point on the trajectory from the enum
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
     * 
     * @param point
     * @param locPar
     * @param locCov
     */
    public void getResults(GBLPOINT point, Vector locPar, SymMatrix locCov) {

        // find the GBL point index
        int gblPointIndex = getPointIndex(point);

        // get the results
        getResults(gblPointIndex, locPar, locCov);

    }

    /**
     * Find the corrections and covariance matrix for a particular point on the GBL trajectory
     * 
     * @param iLabel
     * @param locPar
     * @param locCov
     */
    public void getResults(int iLabel, Vector locPar, SymMatrix locCov) {

        // Get the result from the trajectory
        int ok = _traj.getResults(iLabel, locPar, locCov);

        // check that the fit was ok
        if (ok != 0)
            throw new RuntimeException("Trying to extract GBL corrections for fit that failed!?");
    }

    /**
     * Find the path length to this point.
     * 
     * @param point - {@link GBLPOINT} point
     * @return path length
     */
    public double getPathLength(GBLPOINT point) {
        int gblPointIndex = getPointIndex(point);
        return getPathLength(gblPointIndex);
    }

    /**
     * Find the path length to this point.
     * 
     * @param iLabel - GBL point index
     * @return path length
     */
    public double getPathLength(int iLabel) {
        if (!this.pathLengthMap.containsKey(iLabel))
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

    /**
     * Get the corrected perigee parameters and covariance matrix for a point on the {@link GblTrajectory}. FIXME the
     * covariance matrix is not properly propagated along the trajectory right now!
     * 
     * @param htf - helix to be corrected
     * @param point - {@link GBLPOINT} on the trajectory
     * @param bfield - magnitude of B-field.
     * @return the corrected perigee parameters and covariance matrix
     */
    public Pair<double[], SymmetricMatrix> getCorrectedPerigeeParameters(HelicalTrackFit htf, GBLPOINT point,
            double bfield) {

        // find the point on the trajectory from the GBLPOINT
        int iLabel = getPointIndex(point);

        return getCorrectedPerigeeParameters(htf, iLabel, bfield);

    }

    /**
     * Get the corrected perigee parameters and covariance matrix for a point on the {@link GblTrajectory}. FIXME the
     * covariance matrix is not properly propagated along the trajectory right now!
     * 
     * @param htf - helix to be corrected
     * @param iLabel - label of the point on the {@link GblTrajectory}
     * @param bfield - magnitude of B-field.
     * @return the corrected perigee parameters
     */
    public Pair<double[], SymmetricMatrix> getCorrectedPerigeeParameters(HelicalTrackFit htf, int iLabel, double bfield) {

        // Get corrections from GBL fit
        Vector locPar = new Vector(5);
        SymMatrix locCov = new SymMatrix(5);

        // Extract the corrections to the track parameters and the covariance matrix from the GBL trajectory
        getResults(iLabel, locPar, locCov);

        // Use the super class to keep track of reference point of the helix
        HpsHelicalTrackFit helicalTrackFit = new HpsHelicalTrackFit(htf);
        double[] refIP = helicalTrackFit.getRefPoint();

        // Calculate new reference point for this point
        // This is the intersection of the helix with the plane
        // The trajectory has this information already in the form of a map between GBL point and path length
        double pathLength = getPathLength(iLabel);
        Hep3Vector refPointVec = HelixUtils.PointOnHelix(helicalTrackFit, pathLength);
        double[] refPoint = new double[] {refPointVec.x(), refPointVec.y()};

        LOGGER.finest("pathLength " + pathLength + " -> refPointVec " + refPointVec.toString());

        // Propagate the helix to new reference point
        double[] helixParametersAtPoint = TrackUtils.getParametersAtNewRefPoint(refPoint, helicalTrackFit);

        // Create a new helix with the new parameters and the new reference point
        HpsHelicalTrackFit helicalTrackFitAtPoint = new HpsHelicalTrackFit(helixParametersAtPoint,
                helicalTrackFit.covariance(), helicalTrackFit.chisq(), helicalTrackFit.ndf(),
                helicalTrackFit.PathMap(), helicalTrackFit.ScatterMap(), refPoint);

        // find the corrected perigee track parameters at this point
        double[] helixParametersAtPointCorrected = GblUtils.getCorrectedPerigeeParameters(locPar,
                helicalTrackFitAtPoint, bfield);

        // create a new helix
        HpsHelicalTrackFit helicalTrackFitAtPointCorrected = new HpsHelicalTrackFit(helixParametersAtPointCorrected,
                helicalTrackFit.covariance(), helicalTrackFit.chisq(), helicalTrackFit.ndf(),
                helicalTrackFit.PathMap(), helicalTrackFit.ScatterMap(), refPoint);

        // change reference point back to the original one
        double[] helixParametersAtIPCorrected = TrackUtils.getParametersAtNewRefPoint(refIP,
                helicalTrackFitAtPointCorrected);

        // create a new helix for the new parameters at the IP reference point
        HpsHelicalTrackFit helicalTrackFitAtIPCorrected = new HpsHelicalTrackFit(helixParametersAtIPCorrected,
                helicalTrackFit.covariance(), helicalTrackFit.chisq(), helicalTrackFit.ndf(),
                helicalTrackFit.PathMap(), helicalTrackFit.ScatterMap(), refIP);

        // Calculate the updated covariance
        Matrix jacobian = GblUtils.getCLToPerigeeJacobian(helicalTrackFit, helicalTrackFitAtIPCorrected, bfield);
        Matrix helixCovariance = jacobian.times(locCov.times(jacobian.transpose()));
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i >= j) {
                    cov.setElement(i, j, helixCovariance.get(i, j));
                }
            }
        }
        LOGGER.finest("corrected helix covariance:\n" + cov);

        double parameters_gbl[] = helicalTrackFitAtIPCorrected.parameters();

        return new Pair<double[], SymmetricMatrix>(parameters_gbl, cov);
    }

    /**
     * Extract kinks across the trajectory.
     * 
     * @return kinks in a {@link GBLKinkData} object.
     */
    public GBLKinkData getKinks() {
        GblTrajectory traj = this._traj;
        // get corrections from GBL fit
        Vector locPar = new Vector(5);
        SymMatrix locCov = new SymMatrix(5);
        float[] lambdaKinks = new float[traj.getNumPoints() - 1];
        double[] phiKinks = new double[traj.getNumPoints() - 1];

        double oldPhi = 0, oldLambda = 0;
        for (int i = 0; i < traj.getNumPoints(); i++) {
            traj.getResults(i + 1, locPar, locCov); // vertex point
            double newPhi = locPar.get(GBLPARIDX.XTPRIME.getValue());
            double newLambda = locPar.get(GBLPARIDX.YTPRIME.getValue());
            if (i > 0) {
                lambdaKinks[i - 1] = (float) (newLambda - oldLambda);
                phiKinks[i - 1] = newPhi - oldPhi;
                // System.out.println("phikink: " + (newPhi - oldPhi));
            }
            oldPhi = newPhi;
            oldLambda = newLambda;
        }

        return new GBLKinkData(lambdaKinks, phiKinks);
    }

}