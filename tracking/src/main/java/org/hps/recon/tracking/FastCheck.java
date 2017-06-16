package org.hps.recon.tracking;

import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.twopointcircle.TwoPointCircleFit;
import org.lcsim.fit.twopointcircle.TwoPointCircleFitter;
import org.lcsim.fit.twopointcircle.TwoPointLineFit;
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
    }

    public boolean TwoPointCircleCheck(HelicalTrackHit hit1, HelicalTrackHit hit2) {
        if (_skipchecks)
            return true;

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

    @Override
    public boolean CheckHitSeed(HelicalTrackHit hit, SeedCandidate seed) {

        CorrectHitPosition(hit, seed);
        // Check that hits are outside the maximum DCA
        if (hit.r() < _dMax)
            return false;

        if (_skipchecks)
            return true;

        //  Check the hit against each hit in the seed
        for (HelicalTrackHit hit2 : seed.getHits()) {

            // Check that hits are outside the maximum DCA
            if (hit2.r() < _dMax)
                continue;

            CorrectHitPosition(hit2, seed);

            //          if (this._doSectorBinCheck) {
            //          if (!super.zSectorCheck(hit,hit2)) {
            //              return false;
            //          }
            //      }

            if (!TwoPointCircleCheck(hit, hit2))
                return false;
        }

        return true;
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

    private void CorrectHitPosition(HelicalTrackHit hit, SeedCandidate seed) {
        if (hit instanceof HelicalTrackCross) {
            HelicalTrackCross cross = (HelicalTrackCross) hit;
            HelicalTrackFit helix = null;
            if (seed != null)
                helix = seed.getHelix();
            cross.setTrackDirection(helix);
        }
    }

}
