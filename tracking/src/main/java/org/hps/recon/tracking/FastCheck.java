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
import org.lcsim.recon.tracking.seedtracker.Sector;
import org.lcsim.recon.tracking.seedtracker.SectorManager;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.diagnostic.ISeedTrackerDiagnostics;

public class FastCheck extends org.lcsim.recon.tracking.seedtracker.FastCheck {

    public FastCheck(SeedStrategy strategy, double bfield, ISeedTrackerDiagnostics diag) {
        super(strategy, bfield, diag);
        // TODO Auto-generated constructor stub
    }

    private double _dMax;
    private double _nsig;
    private TwoPointCircleFitter _cfit2;
    private ThreePointCircleFitter _cfit3;
    private static double twopi = 2. * Math.PI;
    private double _eps = 1.0e-6;
    private boolean _skipchecks = false;
    private boolean _doSectorBinCheck = false;

    @Override
    public boolean CheckHitSeed(HelicalTrackHit hit, SeedCandidate seed) {

        if (_skipchecks)
            return true;

        // Check the hit against each hit in the seed
        for (HelicalTrackHit hit2 : seed.getHits()) {
            if (this._doSectorBinCheck) {
                if (!zSectorCheck(hit, hit2)) {
                    return false;
                }
            }

            if (!TwoPointCircleCheck(hit, hit2, seed))
                return false;

        }

        return true;
    }

    @Override
    public boolean CheckSector(SeedCandidate seed, Sector sector) {

        if (_skipchecks)
            return true;

        // Get limits on r, phi, and z for hits in this sector
        double rmin = sector.rmin();
        double rmax = sector.rmax();
        double phimin = sector.phimin();
        double phimax = sector.phimax();
        double zmin = sector.zmin();
        double zmax = sector.zmax();

        // Calculate the midpoint and half the span in phi for this layer
        double midphisec = (phimin + phimax) / 2.;
        double dphisec = 0.5 * (phimax - phimin);

        // Check each hit for compatibility with this sector
        for (HelicalTrackHit hit : seed.getHits()) {
            // Adjust the hit position for stereo hits
            CorrectHitPosition(hit, seed);

            // Sectoring
            if (_doSectorBinCheck) {
                if (!zSectorCheck(hit, sector))
                    return false;
            }

            // Calculate the max track angle change between the hit and sector
            // layer
            double dphitrk1 = dphimax(hit.r(), rmin);
            double dphitrk2 = dphimax(hit.r(), rmax);
            double dphitrk = Math.max(dphitrk1, dphitrk2);

            // Calculate the phi dev between the hit and midpoint of the sector
            double dphi = phidif(hit.phi(), midphisec);

            // The maximum dphi is the sum of the track bend and half the sector
            // span
            double dphimx = dphitrk + dphisec;
            if (dphi > dphimx)
                return false;

            double smin1 = smin(rmin);
            double smax1 = smax(rmax);
            double r = hit.r();
            double smin2 = smin(r);
            double smax2 = smax(r);

            // Get the z limits for the hit
            double zlen = 0.;
            if (hit instanceof HelicalTrack2DHit) {
                zlen = ((HelicalTrack2DHit) hit).zlen();
            }
            double zmin2 = hit.z() - 0.5 * zlen;
            double zmax2 = zmin2 + zlen;

            // Check the z0 limits
            boolean zOK = checkz0(smin1, smax1, zmin, zmax, smin2, smax2, zmin2, zmax2);

            if (!zOK)
                return false;

        }
        return true;
    }

    @Override
    public boolean CheckSectorPair(Sector s1, Sector s2) {

        if (_skipchecks)
            return true;

        if (_doSectorBinCheck) {
            if (!zSectorCheck(s1, s2))
                return false;
        }

        // Calculate the maximum change in azimuth
        double dphi1 = dphimax(s1.rmin(), s2.rmax());
        double dphi2 = dphimax(s1.rmax(), s2.rmin());

        // Calculate the angular difference between the midpoints of the 2
        // sectors
        double mid1 = (s1.phimax() + s1.phimin()) / 2.0;
        double mid2 = (s2.phimax() + s2.phimin()) / 2.0;
        double dmid = phidif(mid1, mid2);

        // Calculate the half widths of the 2 sectors
        double wid1 = s1.phimax() - mid1;
        double wid2 = s2.phimax() - mid2;

        // Check that the sectors are compatible in the bend coordinate
        boolean phiOK;

        phiOK = dmid < dphi1 + wid1 + wid2;
        if (!phiOK)
            phiOK = dmid < dphi2 + wid1 + wid2;
        if (!phiOK)
            return false;

        // Get the minimum and maximum path lengths
        double s1min = smin(s1.rmin());
        double s2min = smin(s2.rmin());
        double s1max = smax(s1.rmax());
        double s2max = smax(s2.rmax());

        // Get the minimum and maximum z's
        double z1min = s1.zmin();
        double z2min = s2.zmin();
        double z1max = s1.zmax();
        double z2max = s2.zmax();

        // Check that the sectors are compatible in the non-bend coordinate
        return checkz0(s1min, s1max, z1min, z1max, s2min, s2max, z2min, z2max);
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
        boolean zOK = checkz0(s1min, s1max, z1min, z1max, s2min, s2max, z2min, z2max);

        if (!zOK)
            return false;

        boolean zSectorOK = true;

        if (_doSectorBinCheck) {
            zSectorOK = zSectorCheck(hit1, hit2);
        }

        // Done!
        return zSectorOK;
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
        if (hit.BarrelEndcapFlag() == BarrelEndcapFlag.BARREL) {
            if (hit instanceof HelicalTrack3DHit)
                dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
            else {
                zfirst = false;
                if (hit instanceof HelicalTrack2DHit)
                    dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
                else
                    dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
            }
        } else {
            dztot += hit.dr() * Math.abs(pos[2]) / Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1]);
        }

        // Get the relevant variables for hit 2
        indx = 1;
        hit = hit2;
        pos = hit.getPosition();
        p[indx][0] = pos[0];
        p[indx][1] = pos[1];
        z[indx] = pos[2];
        if (hit.BarrelEndcapFlag() == BarrelEndcapFlag.BARREL) {
            if (hit instanceof HelicalTrack3DHit)
                dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
            else {
                zfirst = false;
                if (hit instanceof HelicalTrack2DHit)
                    dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
                else
                    dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
            }
        } else {
            dztot += hit.dr() * Math.abs(pos[2]) / Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1]);
        }

        // Get the relevant variables for hit 3
        indx = 2;
        hit = hit3;
        pos = hit.getPosition();
        p[indx][0] = pos[0];
        p[indx][1] = pos[1];
        z[indx] = pos[2];
        if (hit.BarrelEndcapFlag() == BarrelEndcapFlag.BARREL) {
            if (hit instanceof HelicalTrack3DHit)
                dztot += _nsig * ((HelicalTrack3DHit) hit).dz();
            else {
                zfirst = false;
                if (hit instanceof HelicalTrack2DHit)
                    dztot += ((HelicalTrack2DHit) hit).zlen() / 2.;
                else
                    dztot += _nsig * Math.sqrt(hit.getCovMatrix()[5]);
            }
        } else {
            dztot += hit.dr() * Math.abs(pos[2]) / Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1]);
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

}
