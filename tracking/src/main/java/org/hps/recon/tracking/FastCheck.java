package org.hps.recon.tracking;

import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrack3DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.threepointcircle.CircleFit;
import org.lcsim.fit.threepointcircle.ThreePointCircleFitter;
import org.lcsim.fit.twopointcircle.TwoPointCircleFit;
import org.lcsim.fit.twopointcircle.TwoPointCircleFitter;
import org.lcsim.fit.twopointcircle.TwoPointLineFit;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
//import org.lcsim.recon.tracking.seedtracker.Sector;
//import org.lcsim.recon.tracking.seedtracker.SectorManager;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;

public class FastCheck extends org.lcsim.recon.tracking.seedtracker.FastCheck {

    private double _RMin;
    private double _dMax;
    private double _z0Max;
    private double _nsig;
    private TwoPointCircleFitter _cfit2;
    private ThreePointCircleFitter _cfit3;
    private static double twopi = 2. * Math.PI;
    private double _eps = 1.0e-6;
    private boolean _skipchecks = false;

    public FastCheck(SeedStrategy strategy, double bfield) {
        super(strategy, bfield, null);

        // Calculate the minimum radius of curvature, maximum DCA and Maximum z0
        _RMin = strategy.getMinPT() / (Constants.fieldConversion * bfield);
        _dMax = strategy.getMaxDCA();
        _z0Max = strategy.getMaxZ0();
        _nsig = Math.sqrt(strategy.getMaxChisq());

        // Instantiate the two point circle fitter for this minimum radius,
        // maximum DCA
        _cfit2 = new TwoPointCircleFitter(_RMin);

        // Instantiate the three point circle fitter
        _cfit3 = new ThreePointCircleFitter();
    }

    @Override
    public boolean TwoPointCircleCheck(HelicalTrackHit hit1, HelicalTrackHit hit2, SeedCandidate seed) {
        if (_skipchecks)
            return true;

        // Initialize the hit coordinates for an unknown track direction
        CorrectHitPosition(hit1, seed);
        CorrectHitPosition(hit2, seed);

        // Check that hits are outside the maximum DCA
        if (hit1.r() < _dMax || hit2.r() < _dMax)
            return false;

        // Try to find a circle passing through the 2 hits and the maximum DCA
        boolean success = false;
        try {
            success = _cfit2.FitCircle(hit1, hit2, _dMax);
        } catch (Exception x) {
        }

        // Check for success
        if (!success)
            return false;

        // Initialize the minimum/maximum arc lengths
        double s1min = 1.0e99;
        double s1max = -1.0e99;
        double s2min = 1.0e99;
        double s2max = -1.0e99;

        // Loop over the circle fits and find the min/max arc lengths
        for (TwoPointCircleFit fit : _cfit2.getCircleFits()) {
            double s1 = fit.s1();
            double s2 = fit.s2();
            if (s1 < s1min)
                s1min = s1;
            if (s1 > s1max)
                s1max = s1;
            if (s2 < s2min)
                s2min = s2;
            if (s2 > s2max)
                s2max = s2;
        }

        // If we are consistent with a straight-line fit, update the minimum s1,
        // s2
        TwoPointLineFit lfit = _cfit2.getLineFit();
        if (lfit != null) {

            // Find the distance from the DCA to the maximum DCA circle
            double x0 = lfit.x0();
            double y0 = lfit.y0();
            double s0 = 0.;
            double s0sq = _dMax * _dMax - (x0 * x0 + y0 * y0);
            if (s0sq > _eps * _eps)
                s0 = Math.sqrt(s0sq);

            // Update the minimum arc length to the distance from the DCA to the
            // hit
            s1min = lfit.s1() - s0;
            s2min = lfit.s2() - s0;
        }

        // Calculate the allowed variation in hit r and z (not 1 sigma errors!)
        double dr1 = Math.max(_nsig * hit1.dr(), _dMax);
        double dr2 = Math.max(_nsig * hit2.dr(), _dMax);
        double dz1 = dz(hit1);
        double dz2 = dz(hit2);

        // Now check for consistent hits in the s-z plane
        // First expand z ranges by hit z uncertainty
        double z1min = hit1.z() - dz1;
        double z1max = hit1.z() + dz1;
        double z2min = hit2.z() - dz2;
        double z2max = hit2.z() + dz2;

        // Expand s ranges by hit r uncertainty (r ~ s for r << R_curvature)
        s1min = Math.max(0., s1min - dr1);
        s1max = s1max + dr1;
        s2min = Math.max(0., s2min - dr2);
        s2max = s2max + dr2;

        // Check the z0 limits using the min/max path lengths
        return checkz0(s1min, s1max, z1min, z1max, s2min, s2max, z2min, z2max);

    }

    @Override
    public boolean ThreePointHelixCheck(HelicalTrackHit hit1, HelicalTrackHit hit2, HelicalTrackHit hit3) {

        if (_skipchecks)
            return true;

        // Setup for a 3 point circle fit
        double p[][] = new double[3][2];
        double[] pos;
        double z[] = new double[3];
        double dztot = 0.;
        int indx;
        HelicalTrackHit hit;
        boolean zfirst = true;

        // While not terribly elegant, code for speed
        // Use calls that give uncorrected position and error
        // Get the relevant variables for hit 1
        indx = 0;
        hit = hit1;
        pos = hit.getPosition();
        p[indx][0] = pos[0];
        p[indx][1] = pos[1];
        z[indx] = pos[2];

        if (hit instanceof HelicalTrack3DHit)
            dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
        else {
            zfirst = false;
            if (hit instanceof HelicalTrack2DHit)
                dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
            else
                dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
        }

        // Get the relevant variables for hit 2
        indx = 1;
        hit = hit2;
        pos = hit.getPosition();
        p[indx][0] = pos[0];
        p[indx][1] = pos[1];
        z[indx] = pos[2];

        if (hit instanceof HelicalTrack3DHit)
            dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
        else {
            zfirst = false;
            if (hit instanceof HelicalTrack2DHit)
                dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
            else
                dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
        }

        // Get the relevant variables for hit 3
        indx = 2;
        hit = hit3;
        pos = hit.getPosition();
        p[indx][0] = pos[0];
        p[indx][1] = pos[1];
        z[indx] = pos[2];

        if (hit instanceof HelicalTrack3DHit)
            dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
        else {
            zfirst = false;
            if (hit instanceof HelicalTrack2DHit)
                dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
            else
                dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
        }

        // Add multiple scattering error here - for now, just set it to 1 mm
        dztot += 1.;

        // Unless the three hits are all pixel hits, do the circle checks first
        if (!zfirst) {
            if (!TwoPointCircleCheck(hit1, hit3, null))
                return false;
            if (!TwoPointCircleCheck(hit2, hit3, null))
                return false;
        }

        // Do the 3 point circle fit and check for success
        boolean success = _cfit3.fit(p[0], p[1], p[2]);
        if (!success)
            return false;

        // Retrieve the circle parameters
        CircleFit circle = _cfit3.getFit();
        double xc = circle.x0();
        double yc = circle.y0();
        double rc = Math.sqrt(xc * xc + yc * yc);
        double rcurv = circle.radius();

        // Find the point of closest approach
        double x0 = xc * (1. - rcurv / rc);
        double y0 = yc * (1. - rcurv / rc);

        // Find the x-y arc lengths to the hits and the smallest arc length
        double phi0 = Math.atan2(y0 - yc, x0 - xc);
        double[] dphi = new double[3];
        double dphimin = 999.;

        for (int i = 0; i < 3; i++) {
            // Find the angle between the hits and the DCA under the assumption
            // that |dphi| < pi
            dphi[i] = Math.atan2(p[i][1] - yc, p[i][0] - xc) - phi0;
            if (dphi[i] > Math.PI)
                dphi[i] -= twopi;
            if (dphi[i] < -Math.PI)
                dphi[i] += twopi;
            if (Math.abs(dphi[i]) < Math.abs(dphimin))
                dphimin = dphi[i];
        }

        // Use the hit closest to the DCA to determine the circle "direction"
        boolean cw = dphimin < 0.;

        // Find the arc lengths to the hits
        double[] s = new double[3];
        for (int i = 0; i < 3; i++) {

            // Arc set to be positive if they have the same sign as dphimin
            if (cw)
                s[i] = -dphi[i] * rcurv;
            else
                s[i] = dphi[i] * rcurv;

            // Treat the case where a point has dphi opposite in sign to dphimin
            // as an incoming looper hit
            if (s[i] < 0.)
                s[i] += twopi * rcurv;
        }

        // Order the arc lengths and z info by increasing arc length
        for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
                if (s[j] < s[i]) {
                    double temp = s[i];
                    s[i] = s[j];
                    s[j] = temp;
                    temp = z[i];
                    z[i] = z[j];
                    z[j] = temp;
                }
            }
        }

        // Predict the middle z and see if it is consistent with the
        // measurements
        double slope = (z[2] - z[0]) / (s[2] - s[0]);
        double z0 = z[0] - s[0] * slope;
        double zpred = z0 + s[1] * slope;
        if (Math.abs(zpred - z[1]) > dztot)
            return false;

        // If we haven't already done the circle checks, do them now
        if (zfirst) {
            if (!TwoPointCircleCheck(hit1, hit3, null))
                return false;
            if (!TwoPointCircleCheck(hit2, hit3, null))
                return false;
        }

        // Passed all checks - success!
        return true;
    }

    private boolean checkz0(double s1min, double s1max, double zmin1, double zmax1, double s2min, double s2max, double zmin2, double zmax2) {

        double z0[] = new double[2];
        double z1[] = new double[4];
        double z2[] = new double[2];
        double s1[] = new double[4];
        double s2[] = new double[2];

        // Set limits on z0
        z0[0] = -_z0Max;
        z0[1] = _z0Max;

        // Set corners of allowed region for s1, z1
        z1[0] = zmin1;
        z1[1] = zmin1;
        z1[2] = zmax1;
        z1[3] = zmax1;
        s1[0] = s1min;
        s1[1] = s1max;
        s1[2] = s1min;
        s1[3] = s1max;

        // Set limits on z2, s2
        z2[0] = zmin2;
        z2[1] = zmax2;
        s2[0] = s2min;
        s2[1] = s2max;

        // Initialize min/max of s, z at point 2
        double zmax = -1.0e10;
        double zmin = 1.0e10;
        double smax = -1.0e10;
        double smin = 1.0e10;

        // Loop over z0 limits
        for (int i = 0; i < 2; i++) {

            // Loop over corners of s1, z1
            for (int j = 0; j < 4; j++) {

                // Calculate slope of line in s-z space from z0 limit to point 1
                // corner
                double slope = (z1[j] - z0[i]) / s1[j];

                // Loop over limits on z2, s2
                for (int k = 0; k < 2; k++) {

                    // Calculate extrapolation of s-z line to the point 2 limit
                    double z = z0[i] + s2[k] * slope;
                    double s = (z2[k] - z0[i]) / slope;

                    // Find the min/max values of the extrapolated s, z at point
                    // 2
                    if (z > zmax)
                        zmax = z;
                    if (z < zmin)
                        zmin = z;
                    if (s > smax)
                        smax = s;
                    if (s < smin)
                        smin = s;
                }
            }
        }

        // Check to see if the extrapolated points are consistent with
        // measurements
        boolean checkz0 = (zmin2 <= zmax && zmax2 >= zmin) || (s2min <= smax && s2max >= smin);

        return checkz0;
    }

    private void CorrectHitPosition(HelicalTrackHit hit, SeedCandidate seed) {
        if (hit instanceof HelicalTrackCross) {
            HelicalTrackCross cross = (HelicalTrackCross) hit;
            HelicalTrackFit helix = null;
            if (seed != null)
                helix = seed.getHelix();
            cross.setTrackDirection(helix);
        }
    }

    private double dz(HelicalTrackHit hit) {

        // Axial strip hits: use half strip length
        if (hit instanceof HelicalTrack2DHit) {
            return 0.5 * ((HelicalTrack2DHit) hit).zlen();

            // Otherwise use the z error
        } else {
            return _nsig * Math.sqrt(hit.getCorrectedCovMatrix().diagonal(2));
        }
    }

}
