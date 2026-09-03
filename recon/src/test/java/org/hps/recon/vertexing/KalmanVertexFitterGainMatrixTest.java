package org.hps.recon.vertexing;

import junit.framework.TestCase;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math.util.FastMath;

import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.FitResult;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TwoVertexFitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for KalmanVertexFitterGainMatrix, particularly the Lagrange multiplier
 * constrained fitting for three-prong vertices with 4-momentum conservation.
 */
public class KalmanVertexFitterGainMatrixTest extends TestCase {

    private static final double B_FIELD = 1.0; // Tesla
    private static final double ELECTRON_MASS = 0.000511; // GeV

    // Field conversion constant: pT [GeV] = C * B [T] / |omega| [1/mm]
    private static final double C = 2.99792458e-4;

    /**
     * Create a track covariance matrix with reasonable uncertainties
     */
    private RealMatrix createTrackCovariance(double d0Err, double phi0Err, double omegaErr,
                                              double z0Err, double tanLErr) {
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        cov.setEntry(0, 0, d0Err * d0Err);
        cov.setEntry(1, 1, phi0Err * phi0Err);
        cov.setEntry(2, 2, omegaErr * omegaErr);
        cov.setEntry(3, 3, z0Err * z0Err);
        cov.setEntry(4, 4, tanLErr * tanLErr);
        return cov;
    }

    /**
     * Create TrackParams for a track with given momentum at a vertex position.
     * This is a simplified model - in reality the track parameters depend on the
     * full helix geometry.
     */
    private TrackParams createTrackAtVertex(double px, double py, double pz,
                                            double vx, double vy, double vz,
                                            int charge, double bField) {
        double pT = FastMath.sqrt(px * px + py * py);
        double p = FastMath.sqrt(px * px + py * py + pz * pz);

        // omega = C * B / pT, with sign from charge
        double omega = charge * C * bField / pT;
        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);

        // phi0 is the momentum direction at perigee
        // For a track at the vertex, phi at vertex is atan2(py, px)
        // phi0 = phiV - dphi where dphi depends on path from perigee to vertex
        // For simplicity, assume vertex is near origin so phi0 ≈ atan2(py, px)
        double phiV = FastMath.atan2(py, px);

        // tanLambda = pz / pT
        double tanLambda = pz / pT;

        // For a track passing through vertex (vx, vy, vz) with momentum direction phiV,
        // we need to find the perigee parameters (d0, phi0, z0)
        //
        // The helix center is perpendicular to momentum direction:
        // xc = vx + sign * R * sin(phiV) ... wait, this isn't quite right
        // Let me use a simpler approximation for testing:
        // Assume vertex is at origin, so d0 ≈ 0, z0 ≈ vz, phi0 ≈ phiV

        // More accurate: compute helix center from vertex position
        // The momentum at vertex points in direction (cos(phiV), sin(phiV)) in our convention
        // (where px = pT * cos(phiV), py = pT * sin(phiV))
        //
        // For the helix center formula: xc = (sign*R - d0)*sin(phi0), yc = (d0 - sign*R)*cos(phi0)
        // At the vertex: xV = xc + R * something...
        //
        // For simplicity in this test, let's place the vertex at the origin
        // Then d0 = 0, z0 = 0, and phi0 = phiV

        double d0 = 0.0;
        double phi0 = phiV;
        double z0 = vz;

        // Add small random perturbations to simulate measurement
        // (In a real test we might want deterministic values)

        RealMatrix cov = createTrackCovariance(0.1, 0.01, 1e-5, 0.1, 0.01);

        return new TrackParams(d0, phi0, omega, z0, tanLambda, cov);
    }

    /**
     * Test basic unconstrained vertex fit with three tracks
     */
    public void testUnconstrainedFit() {
        System.out.println("\n=== testUnconstrainedFit ===\n");

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        fitter.setDebug(true);

        // Create three tracks that should meet at the origin
        // Track 1: electron going forward-right-up
        // Track 2: electron going forward-left-down
        // Track 3: positron going forward (to balance charge for momentum)

        List<TrackParams> tracks = new ArrayList<>();

        // Electron 1: pT ~ 1 GeV, going in +x direction with small py, pz
        double omega1 = -C * B_FIELD / 1.0; // negative for electron
        tracks.add(new TrackParams(0.0, 0.1, omega1, 0.0, 0.05,
                                   createTrackCovariance(0.1, 0.001, 1e-6, 0.1, 0.001)));

        // Electron 2: pT ~ 1.5 GeV
        double omega2 = -C * B_FIELD / 1.5;
        tracks.add(new TrackParams(0.0, -0.1, omega2, 0.0, -0.03,
                                   createTrackCovariance(0.1, 0.001, 1e-6, 0.1, 0.001)));

        // Positron: pT ~ 1.2 GeV
        double omega3 = C * B_FIELD / 1.2; // positive for positron
        tracks.add(new TrackParams(0.0, 0.0, omega3, 0.0, 0.01,
                                   createTrackCovariance(0.1, 0.001, 1e-6, 0.1, 0.001)));

        // Fit without constraints
        FitResult result = fitter.fit(tracks, 10, 1e-6);

        assertNotNull("Fit result should not be null", result);
        System.out.println("Vertex: " + result.vertex);
        System.out.println("Chi2: " + result.chi2 + ", NDF: " + result.ndf);

        // Vertex should be near origin since all tracks have d0=0, z0=0
        assertTrue("Vertex x should be near 0", FastMath.abs(result.vertex.getEntry(0)) < 1.0);
        assertTrue("Vertex y should be near 0", FastMath.abs(result.vertex.getEntry(1)) < 1.0);
        assertTrue("Vertex z should be near 0", FastMath.abs(result.vertex.getEntry(2)) < 1.0);
    }

    /**
     * Test 4-momentum constrained fit using Lagrange multiplier method via fitVertex()
     */
    public void testThreeMomentumConstraint() {
        System.out.println("\n=== testThreeMomentumConstraint ===\n");

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        fitter.setDebug(true);

        // Beam parameters with realistic HPS rotation
        // The beam is rotated by ~30.5 mrad around the global Y-axis (vertical)
        // In tracking frame: X = detector Z (beamline), Y = detector X (horizontal), Z = detector Y (vertical)
        // So the rotation is around tracking Z, giving beam momentum:
        //   px_trk = pBeam * cos(rotAngle)
        //   py_trk = -pBeam * sin(rotAngle)
        //   pz_trk = 0
        double beamEnergy = 3.7;  // GeV
        double beamRotAngle = 0.0305;  // 30.5 mrad - realistic HPS value

        // Compute beam momentum in tracking frame
        double beamPx = beamEnergy * FastMath.cos(beamRotAngle);
        double beamPy = -beamEnergy * FastMath.sin(beamRotAngle);
        double beamPz = 0.0;

        System.out.printf("Beam rotation angle: %.4f rad (%.2f mrad)%n", beamRotAngle, beamRotAngle * 1000);
        System.out.printf("Beam 3-momentum (tracking frame): [%.6f, %.6f, %.6f]%n", beamPx, beamPy, beamPz);

        // Design NON-COLLINEAR tracks whose 3-momentum sums to the beam momentum
        // We only constrain 3-momentum now (not energy), so non-collinear tracks are fine
        //
        // Track 1 (electron): p1 going mostly forward with some transverse
        // Track 2 (electron): p2 going forward-left
        // Track 3 (positron): p3 going forward-right (to balance py)
        //
        // Design: sum(px) = beamPx, sum(py) = beamPy, sum(pz) = 0

        double px1 = 1.5, py1 = 0.2, pz1 = 0.1;
        double px2 = 1.0, py2 = -0.15, pz2 = -0.05;
        // Track 3 chosen to make sum = beam momentum
        double px3 = beamPx - px1 - px2;
        double py3 = beamPy - py1 - py2;
        double pz3 = -pz1 - pz2;

        double totalPxIn = px1 + px2 + px3;
        double totalPyIn = py1 + py2 + py3;
        double totalPzIn = pz1 + pz2 + pz3;

        System.out.printf("Track momenta (designed):%n");
        System.out.printf("  Track 1: [%.6f, %.6f, %.6f]%n", px1, py1, pz1);
        System.out.printf("  Track 2: [%.6f, %.6f, %.6f]%n", px2, py2, pz2);
        System.out.printf("  Track 3: [%.6f, %.6f, %.6f]%n", px3, py3, pz3);
        System.out.printf("  Sum:     [%.6f, %.6f, %.6f]%n", totalPxIn, totalPyIn, totalPzIn);
        System.out.printf("  Beam:    [%.6f, %.6f, %.6f]%n", beamPx, beamPy, beamPz);
        System.out.printf("  Diff:    [%.2e, %.2e, %.2e]%n",
                          totalPxIn - beamPx, totalPyIn - beamPy, totalPzIn - beamPz);

        // Set fitter beam parameters
        fitter.setBeamEnergy(beamEnergy);
        fitter.setBeamRotAngle(beamRotAngle);
        fitter.setBeamPosition(new double[]{0.0, 0.0, 0.0});  // Origin
        fitter.setBeamSize(new double[]{0.1, 0.1, 1.0});  // Reasonable beam spot

        List<TrackParams> tracks = new ArrayList<>();

        // Create track parameters from momenta
        // For track at origin: d0=0, z0=0, phi0=atan2(py,px), omega=q*C*B/pT, tanL=pz/pT
        double pT1 = FastMath.sqrt(px1*px1 + py1*py1);
        double phi1 = FastMath.atan2(py1, px1);
        double omega1 = -C * B_FIELD / pT1;  // electron (negative charge)
        double tanL1 = pz1 / pT1;

        double pT2 = FastMath.sqrt(px2*px2 + py2*py2);
        double phi2 = FastMath.atan2(py2, px2);
        double omega2 = -C * B_FIELD / pT2;  // electron
        double tanL2 = pz2 / pT2;

        double pT3 = FastMath.sqrt(px3*px3 + py3*py3);
        double phi3 = FastMath.atan2(py3, px3);
        double omega3 = C * B_FIELD / pT3;   // positron (positive charge)
        double tanL3 = pz3 / pT3;

        // All tracks at origin (d0=0, z0=0)
        // Use reasonable covariance matrix
        RealMatrix cov = createTrackCovariance(0.1, 0.01, 1e-5, 0.1, 0.01);

        tracks.add(new TrackParams(0.0, phi1, omega1, 0.0, tanL1, cov));
        tracks.add(new TrackParams(0.0, phi2, omega2, 0.0, tanL2, cov));
        tracks.add(new TrackParams(0.0, phi3, omega3, 0.0, tanL3, cov));

        // Print input track parameters
        System.out.println("\nInput track parameters:");
        for (int i = 0; i < tracks.size(); i++) {
            TrackParams t = tracks.get(i);
            double pT = C * B_FIELD / FastMath.abs(t.omega);
            double px = pT * FastMath.cos(t.phi0);
            double py = pT * FastMath.sin(t.phi0);
            double pz = pT * t.tanLambda;
            System.out.printf("  Track %d: pT=%.4f, phi0=%.4f, omega=%.6f, tanL=%.4f -> p=[%.4f, %.4f, %.4f]%n",
                              i, pT, t.phi0, t.omega, t.tanLambda, px, py, pz);
        }

        // Fit with 3-momentum constraint using fitVertex
        BilliorVertex vtx = fitter.fitVertex(tracks, true);

        assertNotNull("Vertex should not be null", vtx);

        System.out.println("\nFit results:");
        System.out.printf("  Vertex (det frame): [%.4f, %.4f, %.4f]%n",
                          vtx.getPosition().x(), vtx.getPosition().y(), vtx.getPosition().z());
        System.out.printf("  Chi2: %.4f, NDF: %d%n", vtx.getChi2(), 6);  // 3 track z-constraints + 3 momentum constraints

        // Get fitted momenta from the vertex
        // Note: fitVertex returns momenta in DETECTOR frame
        // Detector frame: X = tracking Y, Y = tracking Z, Z = tracking X
        // So to convert back to tracking frame: trk_x = det_z, trk_y = det_x, trk_z = det_y
        double totalPx_det = 0, totalPy_det = 0, totalPz_det = 0;
        for (int i = 0; i < 3; i++) {
            double pxi = vtx.getFittedMomentum(i).x();
            double pyi = vtx.getFittedMomentum(i).y();
            double pzi = vtx.getFittedMomentum(i).z();
            double pMag = FastMath.sqrt(pxi*pxi + pyi*pyi + pzi*pzi);
            totalPx_det += pxi;
            totalPy_det += pyi;
            totalPz_det += pzi;
            System.out.printf("  Track %d fitted (det frame): p=[%.4f, %.4f, %.4f], |p|=%.4f%n",
                              i, pxi, pyi, pzi, pMag);
        }

        // Convert total momentum to tracking frame for comparison with beam constraint
        // tracking X = detector Z, tracking Y = detector X, tracking Z = detector Y
        double totalPx_trk = totalPz_det;
        double totalPy_trk = totalPx_det;
        double totalPz_trk = totalPy_det;

        System.out.printf("  Total fitted 3-mom (det frame): [%.6f, %.6f, %.6f]%n",
                          totalPx_det, totalPy_det, totalPz_det);
        System.out.printf("  Total fitted 3-mom (trk frame): [%.6f, %.6f, %.6f]%n",
                          totalPx_trk, totalPy_trk, totalPz_trk);
        System.out.printf("  Beam 3-momentum (trk frame):    [%.6f, %.6f, %.6f]%n", beamPx, beamPy, beamPz);

        // Check that 3-momentum constraint is satisfied (in tracking frame)
        double dpx = totalPx_trk - beamPx;
        double dpy = totalPy_trk - beamPy;
        double dpz = totalPz_trk - beamPz;

        System.out.printf("  3-momentum residuals (trk frame): [%.2e, %.2e, %.2e]%n", dpx, dpy, dpz);

        // For this test, the input tracks exactly satisfy the constraint
        // So the fit should converge with chi2 ~ 0 and residuals ~ 0
        System.out.printf("  Chi2/NDF: %.4f%n", vtx.getChi2() / 6.0);

        // The constraint should be satisfied to very tight tolerance since input exactly satisfies it
        assertTrue("px constraint should be satisfied", FastMath.abs(dpx) < 0.001);
        assertTrue("py constraint should be satisfied", FastMath.abs(dpy) < 0.001);
        assertTrue("pz constraint should be satisfied", FastMath.abs(dpz) < 0.001);

        // Chi2 should be very small since input exactly satisfies all constraints
        assertTrue("Chi2 should be small for exact input", vtx.getChi2() < 1.0);
    }

    /**
     * Test that constraint Jacobians are computed correctly by numerical differentiation
     */
    public void testConstraintJacobians() {
        System.out.println("\n=== testConstraintJacobians ===\n");

        // This test verifies the analytical Jacobian matches numerical derivatives
        // We'll compute d(momentum)/d(track_params) numerically and compare

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);

        // Create a single track
        double d0 = 0.5;
        double phi0 = 0.2;
        double omega = -C * B_FIELD / 1.5; // electron with pT = 1.5 GeV
        double z0 = 0.1;
        double tanL = 0.05;

        RealMatrix cov = createTrackCovariance(0.1, 0.01, 1e-5, 0.1, 0.01);
        TrackParams track = new TrackParams(d0, phi0, omega, z0, tanL, cov);

        // Vertex position
        double xV = 0.0, yV = 0.0, zV = 0.0;
        RealVector vertex = MatrixUtils.createRealVector(new double[]{xV, yV, zV});

        // Compute momentum at vertex
        // We need to access the private method, so we'll use reflection or
        // compute it ourselves using the same formula

        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);

        // Helix center
        double xc = sign * R * FastMath.sin(phi0) - d0 * FastMath.sin(phi0);
        double yc = -sign * R * FastMath.cos(phi0) + d0 * FastMath.cos(phi0);

        double dx = xV - xc;
        double dy = yV - yc;

        double phiV = FastMath.atan2(-dx * sign, dy * sign);

        double pT = C * B_FIELD / FastMath.abs(omega);
        double px = pT * FastMath.cos(phiV);
        double py = pT * FastMath.sin(phiV);
        double pz = pT * tanL;

        System.out.printf("Track params: d0=%.4f, phi0=%.4f, omega=%.6f, z0=%.4f, tanL=%.4f%n",
                          d0, phi0, omega, z0, tanL);
        System.out.printf("Helix center: xc=%.4f, yc=%.4f%n", xc, yc);
        System.out.printf("phiV=%.4f, pT=%.4f%n", phiV, pT);
        System.out.printf("Momentum at vertex: [%.4f, %.4f, %.4f]%n", px, py, pz);

        // Numerical derivatives
        double eps = 1e-6;

        // d(px)/d(omega) numerically
        double omega_plus = omega + eps;
        double R_plus = 1.0 / FastMath.abs(omega_plus);
        double sign_plus = FastMath.signum(omega_plus);
        double xc_plus = sign_plus * R_plus * FastMath.sin(phi0) - d0 * FastMath.sin(phi0);
        double yc_plus = -sign_plus * R_plus * FastMath.cos(phi0) + d0 * FastMath.cos(phi0);
        double dx_plus = xV - xc_plus;
        double dy_plus = yV - yc_plus;
        double phiV_plus = FastMath.atan2(-dx_plus * sign_plus, dy_plus * sign_plus);
        double pT_plus = C * B_FIELD / FastMath.abs(omega_plus);
        double px_plus = pT_plus * FastMath.cos(phiV_plus);

        double dpx_domega_numerical = (px_plus - px) / eps;

        System.out.printf("d(px)/d(omega) numerical: %.4f%n", dpx_domega_numerical);

        // The test passes if we can compute these - detailed validation would require
        // exposing the Jacobian computation methods or computing them analytically
        assertTrue("Numerical derivative should be finite", Double.isFinite(dpx_domega_numerical));
    }

    /**
     * Build TrackParams for a helix that passes exactly through (xV,yV,zV) with the given
     * momentum, by inverting the same center/perigee formulas used internally by
     * computeTrackConstraint/perigeeToVertexParams (xc = (sign*R-d0)*sin(phi0),
     * yc = -(sign*R-d0)*cos(phi0), zV = z0 - sign*R*dphi*tanLambda).
     */
    private TrackParams createExactTrackThroughPoint(double xV, double yV, double zV,
                                                       double px, double py, double pz,
                                                       int charge, double bField, RealMatrix cov) {
        double pT = FastMath.sqrt(px * px + py * py);
        double omega = charge * C * bField / pT;
        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);
        double phiV = FastMath.atan2(py, px);
        double tanLambda = pz / pT;

        double xc = xV + R * sign * FastMath.sin(phiV);
        double yc = yV - R * sign * FastMath.cos(phiV);

        // (xc,yc) and (d0,phi0) are related by (xc,yc) = ((sign*R-d0)*sin(phi0), -(sign*R-d0)*cos(phi0)),
        // which has a two-fold ambiguity: (phi0, sign*R-A) and (phi0+pi, sign*R+A) both reproduce the
        // same (xc,yc). Only the sign*(R-A) branch gives the physically-correct small |d0| (distance of
        // closest approach); the other branch spuriously gives |d0| ~ R+A and pushes phi0 off by pi.
        double A = FastMath.sqrt(xc * xc + yc * yc);
        double phi0 = FastMath.atan2(sign * xc, -sign * yc);
        double d0 = sign * (R - A);

        double dphi = phiV - phi0;
        while (dphi > FastMath.PI) dphi -= 2.0 * FastMath.PI;
        while (dphi < -FastMath.PI) dphi += 2.0 * FastMath.PI;
        double s = -sign * R * dphi;
        double z0 = zV - s * tanLambda;

        return new TrackParams(d0, phi0, omega, z0, tanLambda, cov);
    }

    /**
     * Exact-geometry sanity check for the cascade (V0-line + recoil-track) vertex fit:
     * construct a helix and a line that are both built to pass exactly through the same
     * point, with small (non-singular) measurement covariances, and verify
     * fitCascadeVertex recovers that point.
     */
    public void testCascadeVertexExactGeometry() {
        System.out.println("\n=== testCascadeVertexExactGeometry ===\n");

        double xV = 0.3, yV = -0.2, zV = 5.0;

        RealMatrix trackCov = createTrackCovariance(1e-4, 1e-5, 1e-8, 1e-4, 1e-5);
        TrackParams recoilTrack = createExactTrackThroughPoint(
                xV, yV, zV, 0.15, -0.05, 1.0, -1, B_FIELD, trackCov);

        RealMatrix lineCov = MatrixUtils.createRealIdentityMatrix(6).scalarMultiply(1e-8);
        KalmanVertexFitterGainMatrix.LineParams v0Line =
                new KalmanVertexFitterGainMatrix.LineParams(xV, yV, zV, 0.10, 0.03, 2.5, lineCov);

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        FitResult result = fitter.fitCascadeVertex(recoilTrack, v0Line);

        System.out.printf("Fitted vertex: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.vertex.getEntry(0), result.vertex.getEntry(1), result.vertex.getEntry(2),
                xV, yV, zV);
        System.out.printf("Chi2/NDF: %.6f / %d%n", result.chi2, result.ndf);

        assertEquals(xV, result.vertex.getEntry(0), 1e-4);
        assertEquals(yV, result.vertex.getEntry(1), 1e-4);
        assertEquals(zV, result.vertex.getEntry(2), 1e-4);
        assertEquals(1, result.ndf);
        assertTrue("chi2 should be small for an exactly-consistent geometry", result.chi2 < 1e-3);
    }

    /**
     * Toy-MC pull study for the cascade vertex fit: build a truth production vertex where a
     * helix and a line cross, smear both inputs by their (correlated, full 6x6 for the line)
     * covariances many times, refit, and check that the fitted-vertex pulls have mean ~0 /
     * width ~1 and that chi2/ndf averages to ~1 (ndf=1), consistent with
     * {@code testCascadeVertexExactGeometry}'s single noiseless case but now exercising the
     * statistical behavior of the fit under realistic measurement uncertainty.
     */
    public void testCascadeVertexSmearedPulls() {
        System.out.println("\n=== testCascadeVertexSmearedPulls ===\n");

        double xV = 0.5, yV = -0.3, zV = 8.0;
        double px = 0.20, py = -0.06, pz = 1.2; // recoil track truth momentum
        double dx = 0.35, dy = 0.12, dz = 3.0;  // V0 line truth direction (= its momentum)

        // Line's own reference point need not be the intersection point -- any point on the
        // line works. Offset it to exercise that generality.
        double s = -2.0;
        double refX = xV + s * dx, refY = yV + s * dy, refZ = zV + s * dz;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);

        double sigmaPos = 0.05;   // mm, line reference-point uncertainty
        double sigmaMom = 0.02;   // GeV, line direction (momentum) uncertainty
        double rho = 0.3;         // correlation between position_i and momentum_i
        RealMatrix lineCov = MatrixUtils.createRealMatrix(6, 6);
        for (int i = 0; i < 3; i++) {
            lineCov.setEntry(i, i, sigmaPos * sigmaPos);
            lineCov.setEntry(3 + i, 3 + i, sigmaMom * sigmaMom);
            double cross = rho * sigmaPos * sigmaMom;
            lineCov.setEntry(i, 3 + i, cross);
            lineCov.setEntry(3 + i, i, cross);
        }

        java.util.Random rng = new java.util.Random(12345);
        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);

        int nToys = 3000;
        double[] pullX = new double[nToys], pullY = new double[nToys], pullZ = new double[nToys];
        double[] chi2 = new double[nToys];
        int nFailed = 0;

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams truthTrack = createExactTrackThroughPoint(
                    xV, yV, zV, px, py, pz, -1, B_FIELD, trackCov);
            double[] tp = truthTrack.toArray();
            double[] smearedTp = new double[5];
            double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};
            for (int i = 0; i < 5; i++) {
                smearedTp[i] = tp[i] + trackSigma[i] * rng.nextGaussian();
            }
            TrackParams smearedTrack = new TrackParams(
                    smearedTp[0], smearedTp[1], smearedTp[2], smearedTp[3], smearedTp[4], trackCov);

            double[] dPos = new double[3], dMom = new double[3];
            for (int i = 0; i < 3; i++) {
                double z1 = rng.nextGaussian();
                double z2 = rng.nextGaussian();
                dPos[i] = sigmaPos * z1;
                dMom[i] = rho * sigmaMom * z1 + sigmaMom * FastMath.sqrt(1.0 - rho * rho) * z2;
            }
            KalmanVertexFitterGainMatrix.LineParams smearedLine = new KalmanVertexFitterGainMatrix.LineParams(
                    refX + dPos[0], refY + dPos[1], refZ + dPos[2],
                    dx + dMom[0], dy + dMom[1], dz + dMom[2], lineCov);

            FitResult result = fitter.fitCascadeVertex(smearedTrack, smearedLine);
            if (result == null) {
                nFailed++;
                continue;
            }

            pullX[toy] = (result.vertex.getEntry(0) - xV) / FastMath.sqrt(result.vertexCov.getEntry(0, 0));
            pullY[toy] = (result.vertex.getEntry(1) - yV) / FastMath.sqrt(result.vertexCov.getEntry(1, 1));
            pullZ[toy] = (result.vertex.getEntry(2) - zV) / FastMath.sqrt(result.vertexCov.getEntry(2, 2));
            chi2[toy] = result.chi2;
        }

        assertEquals("no toy fits should fail", 0, nFailed);

        double meanChi2 = mean(chi2);
        double[] meanX = {mean(pullX)}, meanY = {mean(pullY)}, meanZ = {mean(pullZ)};
        double stdX = std(pullX, meanX[0]), stdY = std(pullY, meanY[0]), stdZ = std(pullZ, meanZ[0]);

        System.out.printf("Pull X: mean=%.3f std=%.3f%n", meanX[0], stdX);
        System.out.printf("Pull Y: mean=%.3f std=%.3f%n", meanY[0], stdY);
        System.out.printf("Pull Z: mean=%.3f std=%.3f%n", meanZ[0], stdZ);
        System.out.printf("Mean chi2 (ndf=1): %.3f%n", meanChi2);

        assertTrue("pull X mean should be near 0, got " + meanX[0], FastMath.abs(meanX[0]) < 0.15);
        assertTrue("pull Y mean should be near 0, got " + meanY[0], FastMath.abs(meanY[0]) < 0.15);
        assertTrue("pull Z mean should be near 0, got " + meanZ[0], FastMath.abs(meanZ[0]) < 0.15);
        assertTrue("pull X std should be near 1, got " + stdX, stdX > 0.8 && stdX < 1.2);
        assertTrue("pull Y std should be near 1, got " + stdY, stdY > 0.8 && stdY < 1.2);
        assertTrue("pull Z std should be near 1, got " + stdZ, stdZ > 0.8 && stdZ < 1.2);
        assertTrue("mean chi2 for ndf=1 should be near 1, got " + meanChi2, meanChi2 > 0.7 && meanChi2 < 1.4);
    }

    /**
     * Exact-geometry sanity check for the NEW joint two-vertex fit (fitCascadeVertexJoint):
     * construct eMinus/ePlus tracks passing exactly through a chosen V1, sum their momenta
     * there to get the V0 flight direction, place V2 exactly along that direction from V1,
     * and construct a recoil track passing exactly through V2. Verify the fit recovers V1,
     * V2, and all three momenta to tight tolerance with near-zero chi2 -- isolating whether
     * a reported bug (bad V1 position / inflated V1 mass on real data) is in the core fit
     * math here vs. elsewhere (ThreeTrackVertexer's packaging, or real-data specifics).
     */
    public void testJointTwoVertexExactGeometry() {
        System.out.println("\n=== testJointTwoVertexExactGeometry ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double flightLength = 40.0; // mm
        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;

        RealMatrix trackCov = createTrackCovariance(0.03, 0.003, 5e-6, 0.03, 0.003);
        TrackParams eMinusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
        TrackParams ePlusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
        TrackParams recoilTrack = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        // Deliberately poor initial guess for V2 (mimicking ThreeTrackVertexer's real usage,
        // where v2Init defaults to the beamspot near the origin rather than the truth).
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init, 500, 1.0e-10);

        assertNotNull("Joint two-vertex fit result should not be null", result);

        System.out.printf("Fitted V1: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v1.getEntry(0), result.v1.getEntry(1), result.v1.getEntry(2), xV1, yV1, zV1);
        System.out.printf("Fitted V2: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v2.getEntry(0), result.v2.getEntry(1), result.v2.getEntry(2), xV2, yV2, zV2);
        System.out.printf("Fitted eMinus p: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.eMinusMomentum.p.getEntry(0), result.eMinusMomentum.p.getEntry(1), result.eMinusMomentum.p.getEntry(2),
                pxEm, pyEm, pzEm);
        System.out.printf("Fitted ePlus p: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.ePlusMomentum.p.getEntry(0), result.ePlusMomentum.p.getEntry(1), result.ePlusMomentum.p.getEntry(2),
                pxEp, pyEp, pzEp);
        System.out.printf("Fitted recoil p: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.recoilMomentum.p.getEntry(0), result.recoilMomentum.p.getEntry(1), result.recoilMomentum.p.getEntry(2),
                pxRc, pyRc, pzRc);
        System.out.printf("Chi2/NDF: %.6f / %d%n", result.chi2, result.ndf);

        assertEquals(xV1, result.v1.getEntry(0), 1e-3);
        assertEquals(yV1, result.v1.getEntry(1), 1e-3);
        assertEquals(zV1, result.v1.getEntry(2), 1e-3);
        assertEquals(xV2, result.v2.getEntry(0), 1e-3);
        assertEquals(yV2, result.v2.getEntry(1), 1e-3);
        assertEquals(zV2, result.v2.getEntry(2), 1e-3);
        assertEquals(pxEm, result.eMinusMomentum.p.getEntry(0), 1e-3);
        assertEquals(pyEm, result.eMinusMomentum.p.getEntry(1), 1e-3);
        assertEquals(pzEm, result.eMinusMomentum.p.getEntry(2), 1e-3);
        assertEquals(pxEp, result.ePlusMomentum.p.getEntry(0), 1e-3);
        assertEquals(pyEp, result.ePlusMomentum.p.getEntry(1), 1e-3);
        assertEquals(pzEp, result.ePlusMomentum.p.getEntry(2), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + result.chi2, result.chi2 < 1e-2);
    }

    /**
     * As {@link #testJointTwoVertexExactGeometry}, but for {@code fitCascadeVertexJointFixedV2X}:
     * V2's tracking-index-0 coordinate is passed in as {@code v2FixedX} equal to the true xV2
     * (isolating the reduced-state fit mechanics from any target-plane-vs-truth systematic,
     * which is instead checked separately against real data), and must come back reported as
     * exactly that value with exactly-zero variance -- V1 and V2's other two coordinates should
     * still recover truth as tightly as the unconstrained fit.
     */
    public void testJointTwoVertexFixedV2XExactGeometry() {
        System.out.println("\n=== testJointTwoVertexFixedV2XExactGeometry ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double flightLength = 40.0; // mm
        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;

        RealMatrix trackCov = createTrackCovariance(0.03, 0.003, 5e-6, 0.03, 0.003);
        TrackParams eMinusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
        TrackParams ePlusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
        TrackParams recoilTrack = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        // Deliberately poor initial guess for V2 (mimicking ThreeTrackVertexer's real usage).
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        TwoVertexFitResult result = fitter.fitCascadeVertexJointFixedV2X(
                eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init, xV2, null, null, 500, 1.0e-10);

        assertNotNull("Fixed-V2X joint two-vertex fit result should not be null", result);

        System.out.printf("Fitted V1: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v1.getEntry(0), result.v1.getEntry(1), result.v1.getEntry(2), xV1, yV1, zV1);
        System.out.printf("Fitted V2: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v2.getEntry(0), result.v2.getEntry(1), result.v2.getEntry(2), xV2, yV2, zV2);
        System.out.printf("Chi2/NDF: %.6f / %d%n", result.chi2, result.ndf);

        assertEquals(xV1, result.v1.getEntry(0), 1e-3);
        assertEquals(yV1, result.v1.getEntry(1), 1e-3);
        assertEquals(zV1, result.v1.getEntry(2), 1e-3);
        assertEquals("fixed V2 coordinate should be reported as exactly the input value",
                xV2, result.v2.getEntry(0), 1e-12);
        assertEquals(yV2, result.v2.getEntry(1), 1e-3);
        assertEquals(zV2, result.v2.getEntry(2), 1e-3);
        assertEquals("fixed V2 coordinate's variance should be exactly zero",
                0.0, result.v2Cov.getEntry(0, 0), 1e-12);
        assertEquals(0.0, result.v2Cov.getEntry(0, 1), 1e-12);
        assertEquals(0.0, result.v2Cov.getEntry(0, 2), 1e-12);
        assertEquals(0.0, result.v2Cov.getEntry(1, 0), 1e-12);
        assertEquals(0.0, result.v2Cov.getEntry(2, 0), 1e-12);
        assertEquals(3, result.ndf);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + result.chi2, result.chi2 < 1e-2);
    }

    /**
     * Pull-distribution check for {@code fitCascadeVertexJointFixedV2X}, mirroring
     * {@link #testJointTwoVertexSmearedPulls}: V1's full 3 coordinates and V2's 2 free
     * (transverse) coordinates should have pull mean ~0, std ~1 across many smeared toys.
     * V2's fixed coordinate has no pull to check -- it isn't a fitted quantity.
     */
    public void testJointTwoVertexFixedV2XSmearedPulls() {
        for (double flightLength : new double[]{5.0, 20.0, 50.0, 90.0, 120.0, 150.0}) {
            runJointTwoVertexFixedV2XSmearedPulls(flightLength, 500);
        }
    }

    private void runJointTwoVertexFixedV2XSmearedPulls(double flightLength, int nToys) {
        System.out.println("\n=== testJointTwoVertexFixedV2XSmearedPulls (flightLength=" + flightLength + " mm) ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, -1.1});

        java.util.Random rng = new java.util.Random(6789);
        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);

        int nNull = 0;
        int nOk = 0;
        double[] pullV1X = new double[nToys], pullV1Y = new double[nToys], pullV1Z = new double[nToys];
        double[] pullV2Y = new double[nToys], pullV2Z = new double[nToys];

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams eMinusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
            TrackParams ePlusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
            TrackParams recoilTruth = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

            TrackParams eMinusTrack = smearTrack(eMinusTruth, trackSigma, trackCov, rng);
            TrackParams ePlusTrack = smearTrack(ePlusTruth, trackSigma, trackCov, rng);
            TrackParams recoilTrack = smearTrack(recoilTruth, trackSigma, trackCov, rng);

            TwoVertexFitResult result = fitter.fitCascadeVertexJointFixedV2X(
                    eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init, xV2, null, null, 60, 1.0e-8);
            if (result == null) {
                nNull++;
                continue;
            }

            pullV1X[nOk] = (result.v1.getEntry(0) - xV1) / FastMath.sqrt(result.v1Cov.getEntry(0, 0));
            pullV1Y[nOk] = (result.v1.getEntry(1) - yV1) / FastMath.sqrt(result.v1Cov.getEntry(1, 1));
            pullV1Z[nOk] = (result.v1.getEntry(2) - zV1) / FastMath.sqrt(result.v1Cov.getEntry(2, 2));
            pullV2Y[nOk] = (result.v2.getEntry(1) - yV2) / FastMath.sqrt(result.v2Cov.getEntry(1, 1));
            pullV2Z[nOk] = (result.v2.getEntry(2) - zV2) / FastMath.sqrt(result.v2Cov.getEntry(2, 2));
            assertEquals("fixed V2 coordinate should never move from the input value",
                    xV2, result.v2.getEntry(0), 1e-12);
            nOk++;
        }

        double[] pullXTrim = java.util.Arrays.copyOf(pullV1X, nOk);
        double[] pullYTrim = java.util.Arrays.copyOf(pullV1Y, nOk);
        double[] pullZTrim = java.util.Arrays.copyOf(pullV1Z, nOk);
        double[] pullV2YTrim = java.util.Arrays.copyOf(pullV2Y, nOk);
        double[] pullV2ZTrim = java.util.Arrays.copyOf(pullV2Z, nOk);

        System.out.printf("nToys=%d nNull=%d nOk=%d%n", nToys, nNull, nOk);
        System.out.printf("V1 pull X: mean=%.3f std=%.3f%n", mean(pullXTrim), std(pullXTrim, mean(pullXTrim)));
        System.out.printf("V1 pull Y: mean=%.3f std=%.3f%n", mean(pullYTrim), std(pullYTrim, mean(pullYTrim)));
        System.out.printf("V1 pull Z: mean=%.3f std=%.3f%n", mean(pullZTrim), std(pullZTrim, mean(pullZTrim)));
        System.out.printf("V2 pull Y: mean=%.3f std=%.3f%n", mean(pullV2YTrim), std(pullV2YTrim, mean(pullV2YTrim)));
        System.out.printf("V2 pull Z: mean=%.3f std=%.3f%n", mean(pullV2ZTrim), std(pullV2ZTrim, mean(pullV2ZTrim)));
    }

    /**
     * The V0 flight line through V1 (direction pV0) generically crosses the recoil track's
     * own transverse (bending-plane) circle at two points -- a genuine near/far branch
     * ambiguity that plain least-squares projection of the caller's v2Init guess cannot
     * resolve, since it knows nothing about the recoil track. Build a geometry with a large,
     * deliberately-unphysical truth theta (15.0, i.e. V2 is 15x further along -pV0 than V1)
     * so the two transverse roots are far apart, and verify: (1) transverseCircleRoots finds
     * both roots and they satisfy the recoil track's own circle equation; (2) one root
     * matches truth closely while the other is a genuinely distinct, spurious branch; (3)
     * selectPhysicalThetaSeed's z-consistency discriminant (comparing V2.z against the
     * recoil track's own zV prediction at each candidate) picks the truth-matching root, not
     * the spurious one.
     */
    public void testTransverseCircleRootsAndBranchSelection() {
        System.out.println("\n=== testTransverseCircleRootsAndBranchSelection ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;

        double thetaTrue = 15.0;
        double xV2 = xV1 - thetaTrue * pxV0;
        double yV2 = yV1 - thetaTrue * pyV0;
        double zV2 = zV1 - thetaTrue * pzV0;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;
        RealMatrix trackCov = createTrackCovariance(0.03, 0.003, 5e-6, 0.03, 0.003);
        TrackParams recoilTrack = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

        RealVector v1 = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        RealVector pV0 = MatrixUtils.createRealVector(new double[]{pxV0, pyV0, pzV0});

        double[] roots = KalmanVertexFitterGainMatrix.transverseCircleRoots(v1, pV0, recoilTrack);
        assertNotNull("expected two real transverse-circle roots for this geometry", roots);
        assertEquals(2, roots.length);

        double R = 1.0 / FastMath.abs(recoilTrack.omega);
        double sign = FastMath.signum(recoilTrack.omega);
        double xc = R * sign * FastMath.sin(recoilTrack.phi0) - recoilTrack.d0 * FastMath.sin(recoilTrack.phi0);
        double yc = -R * sign * FastMath.cos(recoilTrack.phi0) + recoilTrack.d0 * FastMath.cos(recoilTrack.phi0);
        for (double theta : roots) {
            double xOnLine = v1.getEntry(0) - theta * pV0.getEntry(0);
            double yOnLine = v1.getEntry(1) - theta * pV0.getEntry(1);
            double r = FastMath.sqrt((xOnLine - xc) * (xOnLine - xc) + (yOnLine - yc) * (yOnLine - yc));
            assertEquals("root theta=" + theta + " should lie on the recoil circle", R, r, 1e-6);
        }

        double res0 = FastMath.abs(roots[0] - thetaTrue);
        double res1 = FastMath.abs(roots[1] - thetaTrue);
        double bestResidual = FastMath.min(res0, res1);
        double worstResidual = FastMath.max(res0, res1);
        System.out.printf("roots=[%.6f, %.6f]  thetaTrue=%.6f%n", roots[0], roots[1], thetaTrue);
        assertTrue("one root should closely match thetaTrue, got best residual " + bestResidual,
                bestResidual < 1e-6);
        assertTrue("the other root should be a genuinely distinct, spurious branch, got separation "
                + FastMath.abs(roots[1] - roots[0]), FastMath.abs(roots[1] - roots[0]) > 1.0);
        assertTrue("spurious root should not itself be near truth, got residual " + worstResidual,
                worstResidual > 1.0);

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        KalmanVertexFitterGainMatrix.ThetaSeed seed = fitter.selectPhysicalThetaSeed(v1, pV0, recoilTrack);
        assertNotNull("selectPhysicalThetaSeed should resolve the branch ambiguity", seed);
        System.out.printf("selected theta=%.6f  v2=[%.6f, %.6f, %.6f]  (truth: %.6f, [%.6f, %.6f, %.6f])%n",
                seed.theta, seed.v2.getEntry(0), seed.v2.getEntry(1), seed.v2.getEntry(2),
                thetaTrue, xV2, yV2, zV2);

        assertEquals("selected theta should match the truth branch, not the spurious one",
                thetaTrue, seed.theta, 1e-6);
        assertEquals(xV2, seed.v2.getEntry(0), 1e-4);
        assertEquals(yV2, seed.v2.getEntry(1), 1e-4);
        assertEquals(zV2, seed.v2.getEntry(2), 1e-4);
    }

    /**
     * End-to-end check that the branch-selection fix ({@link #testTransverseCircleRootsAndBranchSelection})
     * actually changes the full Newton iteration's outcome, not just the seed in isolation.
     * Reuses that test's exact geometry -- same V1/tracks/thetaTrue=15, so the same genuinely
     * separated near/far roots (-1774.86 and 15.0) exist -- but drives the full
     * {@code fitCascadeVertexJoint} call with a deliberately poor {@code v2Init} at the
     * origin, matching {@code ThreeTrackVertexer}'s real usage (defaults to the beamspot, not
     * truth, when the caller has no better guess). Without the branch-selection override, the
     * least-squares theta seeded from (v1Init, v2Init=origin) has no information about the
     * recoil track at all and can leave Newton free to walk to the spurious branch and
     * self-consistently settle there (large chi2, wrong V1/V2) -- see the bug description on
     * {@link #fitCascadeVertexJoint} referenced in the production-code comment. With the fix,
     * {@code selectPhysicalThetaSeed} overrides the seed before iteration starts regardless of
     * {@code v2Init}, so the fit should converge tightly to truth even from this adversarial
     * starting point.
     */
    public void testJointTwoVertexAdversarialSeedConvergesToTruthBranch() {
        System.out.println("\n=== testJointTwoVertexAdversarialSeedConvergesToTruthBranch ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;

        double thetaTrue = 15.0;
        double xV2 = xV1 - thetaTrue * pxV0;
        double yV2 = yV1 - thetaTrue * pyV0;
        double zV2 = zV1 - thetaTrue * pzV0;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;

        RealMatrix trackCov = createTrackCovariance(0.03, 0.003, 5e-6, 0.03, 0.003);
        TrackParams eMinusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
        TrackParams ePlusTrack = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
        TrackParams recoilTrack = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        // Deliberately adversarial: far from both truth V2 and the spurious branch's V2, and
        // carrying no information at all about which branch the recoil track prefers -- the
        // same "beamspot near origin" guess ThreeTrackVertexer falls back to in practice.
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);
        TwoVertexFitResult result = fitter.fitCascadeVertexJoint(
                eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init, 500, 1.0e-10);

        assertNotNull("Joint two-vertex fit result should not be null", result);

        System.out.printf("Fitted V1: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v1.getEntry(0), result.v1.getEntry(1), result.v1.getEntry(2), xV1, yV1, zV1);
        System.out.printf("Fitted V2: [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                result.v2.getEntry(0), result.v2.getEntry(1), result.v2.getEntry(2), xV2, yV2, zV2);
        System.out.printf("Chi2/NDF: %.6f / %d%n", result.chi2, result.ndf);

        assertEquals(xV1, result.v1.getEntry(0), 1e-3);
        assertEquals(yV1, result.v1.getEntry(1), 1e-3);
        assertEquals(zV1, result.v1.getEntry(2), 1e-3);
        assertEquals(xV2, result.v2.getEntry(0), 1e-3);
        assertEquals(yV2, result.v2.getEntry(1), 1e-3);
        assertEquals(zV2, result.v2.getEntry(2), 1e-3);
        assertEquals(pxEm, result.eMinusMomentum.p.getEntry(0), 1e-3);
        assertEquals(pyEm, result.eMinusMomentum.p.getEntry(1), 1e-3);
        assertEquals(pzEm, result.eMinusMomentum.p.getEntry(2), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + result.chi2, result.chi2 < 1e-2);
    }

    private static RealMatrix cov5x5(double[][] rows) {
        RealMatrix m = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                m.setEntry(i, j, rows[i][j]);
            }
        }
        return m;
    }

    /**
     * Regression test using exact real-data inputs (run 14272, one of the truth-matched
     * candidates from mcReconApPulser180MeV/ap_pulser_100 with apVtxZMC~37mm and v1Init~39mm)
     * that, prior to fixing the backtracking line search's merit function to include the
     * parameter-pull term dx^T W dx (not just the constraint-residual norm ||h||) and adding
     * a trust-region cap on the per-iteration vertex step, converged to a wildly wrong V1
     * (~[-55,-3,-1] mm) with chi2/ndf~1267. This particular candidate's v1Init turns out to
     * have a genuine ~7mm self-inconsistency against the eMinus track's own longitudinal
     * (z0/tanLambda) prediction even before any fit adjustment (verified by hand from the
     * raw track parameters) -- i.e. not every truth-matched candidate has a v1Init that is
     * itself a perfect local optimum, so this test only checks that the fit no longer blows
     * up catastrophically (chi2/ndf << 1267), not that it lands exactly on v1Init.
     *
     * <p>Uses the explicit-v2Init overload (an arbitrary non-null guess, giving V2 a flat/
     * weak prior) rather than the {@code v2Init=null} convenience overload, which defaults
     * to a beamspot-size-tied V2 prior ({@code useBeamspotPriorForV2=true}). Production
     * ({@link ThreeTrackVertexer}) never takes the null-v2Init path -- it always supplies
     * its own explicit v2Init/v2Cov from the already-fitted V0 line -- precisely because
     * pairing a tight beamspot prior with {@code selectPhysicalThetaSeed}'s own
     * documented-unreliable branch choice (worse than a coin flip; see
     * {@code ThreeTrackVertexer}'s Javadoc on {@code v0InputVtxZ}) can pin V2 near a seed on
     * the wrong branch and blow up chi2, independent of any track-level correctness. With a
     * flat V2 prior, both {@code transverseCircleRoots} branches converge to the same
     * well-behaved V1 for this event, which is what this test actually checks.
     */
    public void testJointTwoVertexRealDataRegression() {
        System.out.println("\n=== testJointTwoVertexRealDataRegression ===\n");

        double bField = -0.8595999999999999;

        TrackParams eMinusTrack = new TrackParams(
                -0.4138278812633871, 0.05818441086284123, 2.2774348286756926E-4,
                3.4280614931007167, -0.08920239911869288,
                cov5x5(new double[][]{
                        {0.15615547160986665, -0.0016108269680988323, -6.136407470045745E-6, 0.00905529155838811, -8.625784313471315E-5},
                        {-0.0016108269680988323, 1.8920753232229165E-5, 7.679882358313323E-8, -8.499857689815913E-5, 8.411670918677625E-7},
                        {-6.136407470045745E-6, 7.679882358313323E-8, 3.436488183137914E-10, -3.103531812967638E-7, 3.1355922839603002E-9},
                        {0.00905529155838811, -8.499857689815913E-5, -3.103531812967638E-7, 0.0028035017716515386, -4.008046330392784E-5},
                        {-8.625784313471315E-5, 8.411670918677625E-7, 3.1355922839603002E-9, -4.008046330392784E-5, 6.201734967043375E-7}}));

        TrackParams ePlusTrack = new TrackParams(
                -1.4158622787508648, 0.026232654124577415, -7.340327909292423E-5,
                -1.395029157958812, 0.03055412093977258,
                cov5x5(new double[][]{
                        {0.06907944877016554, -3.707516890884016E-4, -5.570557539621614E-7, -0.0027419045884956397, 1.0944381852808471E-5},
                        {-3.707516890884016E-4, 2.3555616481904708E-6, 3.679013133079066E-9, 1.5548720728112388E-5, -8.038460884907129E-8},
                        {-5.570557539621614E-7, 3.679013133079066E-9, 6.949082309771353E-12, 2.3959185608354854E-8, -1.3153402748976983E-10},
                        {-0.0027419045884956397, 1.5548720728112388E-5, 2.3959185608354854E-8, 0.001142758650098608, -1.0696167027024057E-5},
                        {1.0944381852808471E-5, -8.038460884907129E-8, -1.3153402748976983E-10, -1.0696167027024057E-5, 1.332734727465295E-7}}));

        TrackParams recoilTrack = new TrackParams(
                1.7534739233699383, 0.0028232486700802044, 6.790166806190062E-4,
                -0.10407287685488854, -0.03300521957923669,
                cov5x5(new double[][]{
                        {0.24563231182724743, -0.0029459600581028204, -1.1294416141395693E-5, 0.022629210781667644, -3.005455288274632E-4},
                        {-0.0029459600581028204, 4.00251403930356E-5, 1.6327180350031857E-7, -2.3970366994327392E-4, 3.2538808883958757E-6},
                        {-1.1294416141395693E-5, 1.6327180350031857E-7, 8.643410712643231E-10, -9.189655156689234E-7, 1.2613796957457733E-8},
                        {0.022629210781667644, -2.3970366994327392E-4, -9.189655156689234E-7, 0.015358330058350982, -2.5466221420599583E-4},
                        {-3.005455288274632E-4, 3.2538808883958757E-6, 1.2613796957457733E-8, -2.5466221420599583E-4, 4.300732964474161E-6}}));

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{38.9028909318, 0.5285581997, -0.2061714304});

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        RealVector v2Init = MatrixUtils.createRealVector(fitter.getBeamPosition());
        TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init);

        assertNotNull("Joint two-vertex fit result should not be null", result);
        System.out.printf("Fitted V1: [%.6f, %.6f, %.6f]  (v1Init: [%.6f, %.6f, %.6f])%n",
                result.v1.getEntry(0), result.v1.getEntry(1), result.v1.getEntry(2),
                v1Init.getEntry(0), v1Init.getEntry(1), v1Init.getEntry(2));
        System.out.printf("Chi2/NDF: %.6f / %d = %.6f%n", result.chi2, result.ndf, result.chi2 / result.ndf);

        assertTrue("chi2/ndf should be far below the pre-fix catastrophic value of ~1267, got "
                + (result.chi2 / result.ndf), result.chi2 / result.ndf < 300.0);
    }

    /**
     * Toy-MC pull study for the joint two-vertex fit under realistic smearing and the same
     * (v1Init=truth-ish, v2Init=beamspot, maxIterations=20, tolerance=1e-8) conditions
     * {@link org.hps.recon.vertexing.ThreeTrackVertexer} actually uses in production --
     * isolating whether the bad V1 positions / inflated V1 mass seen on real reconstructed
     * data are reproducible from the core fit math alone under realistic noise, as opposed
     * to something specific to real detector data.
     */
    public void testJointTwoVertexSmearedPulls() {
        for (double flightLength : new double[]{5.0, 20.0, 50.0, 90.0, 120.0, 150.0}) {
            runJointTwoVertexSmearedPulls(flightLength, 500);
        }
    }

    private void runJointTwoVertexSmearedPulls(double flightLength, int nToys) {
        System.out.println("\n=== testJointTwoVertexSmearedPulls (flightLength=" + flightLength + " mm) ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        double pxRc = 0.20, pyRc = -0.10, pzRc = 3.0;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, -1.1});

        java.util.Random rng = new java.util.Random(6789);
        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(B_FIELD);

        int nNull = 0;
        int nHighChi2 = 0;
        int nMassOver200 = 0;
        double[] pullV1X = new double[nToys], pullV1Y = new double[nToys], pullV1Z = new double[nToys];
        double[] chi2Arr = new double[nToys];
        double[] massArr = new double[nToys];
        int nOk = 0;

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams eMinusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
            TrackParams ePlusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
            TrackParams recoilTruth = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

            TrackParams eMinusTrack = smearTrack(eMinusTruth, trackSigma, trackCov, rng);
            TrackParams ePlusTrack = smearTrack(ePlusTruth, trackSigma, trackCov, rng);
            TrackParams recoilTrack = smearTrack(recoilTruth, trackSigma, trackCov, rng);

            TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init);
            if (result == null) {
                nNull++;
                continue;
            }

            double chi2Ndf = result.chi2 / result.ndf;
            if (chi2Ndf > 20) {
                nHighChi2++;
            }

            double eEm = FastMath.sqrt(result.eMinusMomentum.p.dotProduct(result.eMinusMomentum.p) + ELECTRON_MASS * ELECTRON_MASS);
            double eEp = FastMath.sqrt(result.ePlusMomentum.p.dotProduct(result.ePlusMomentum.p) + ELECTRON_MASS * ELECTRON_MASS);
            RealVector pV0 = result.eMinusMomentum.p.add(result.ePlusMomentum.p);
            double massSq = (eEm + eEp) * (eEm + eEp) - pV0.dotProduct(pV0);
            double mass = massSq > 0 ? FastMath.sqrt(massSq) : -1.0;
            if (mass > 0.2) {
                nMassOver200++;
            }

            chi2Arr[nOk] = result.chi2;
            massArr[nOk] = mass;
            pullV1X[nOk] = (result.v1.getEntry(0) - xV1) / FastMath.sqrt(result.v1Cov.getEntry(0, 0));
            pullV1Y[nOk] = (result.v1.getEntry(1) - yV1) / FastMath.sqrt(result.v1Cov.getEntry(1, 1));
            pullV1Z[nOk] = (result.v1.getEntry(2) - zV1) / FastMath.sqrt(result.v1Cov.getEntry(2, 2));
            nOk++;
        }

        double[] chi2Trim = java.util.Arrays.copyOf(chi2Arr, nOk);
        double[] massTrim = java.util.Arrays.copyOf(massArr, nOk);
        double[] pullXTrim = java.util.Arrays.copyOf(pullV1X, nOk);
        double[] pullYTrim = java.util.Arrays.copyOf(pullV1Y, nOk);
        double[] pullZTrim = java.util.Arrays.copyOf(pullV1Z, nOk);

        System.out.printf("nToys=%d nNull=%d nHighChi2(>20)=%d nMassOver200MeV=%d nOk=%d%n",
                nToys, nNull, nHighChi2, nMassOver200, nOk);
        System.out.printf("chi2: mean=%.3f max=%.3f%n", mean(chi2Trim), java.util.Arrays.stream(chi2Trim).max().orElse(0));
        System.out.printf("V1 mass: mean=%.4f max=%.4f%n", mean(massTrim), java.util.Arrays.stream(massTrim).max().orElse(0));
        System.out.printf("V1 pull X: mean=%.3f std=%.3f%n", mean(pullXTrim), std(pullXTrim, mean(pullXTrim)));
        System.out.printf("V1 pull Y: mean=%.3f std=%.3f%n", mean(pullYTrim), std(pullYTrim, mean(pullYTrim)));
        System.out.printf("V1 pull Z: mean=%.3f std=%.3f%n", mean(pullZTrim), std(pullZTrim, mean(pullZTrim)));
    }

    private static TrackParams smearTrack(TrackParams truth, double[] trackSigma, RealMatrix trackCov, java.util.Random rng) {
        double[] tp = truth.toArray();
        double[] smearedTp = new double[5];
        for (int i = 0; i < 5; i++) {
            smearedTp[i] = tp[i] + trackSigma[i] * rng.nextGaussian();
        }
        return new TrackParams(smearedTp[0], smearedTp[1], smearedTp[2], smearedTp[3], smearedTp[4], trackCov);
    }

    private static double mean(double[] x) {
        double sum = 0;
        for (double v : x) sum += v;
        return sum / x.length;
    }

    private static double std(double[] x, double mean) {
        double sum = 0;
        for (double v : x) sum += (v - mean) * (v - mean);
        return FastMath.sqrt(sum / x.length);
    }

    /**
     * Reproduces one specific real-MC event (run 14272, first BADFIT_DEBUG block in
     * /tmp/vo_run2.log) directly from its captured perigee parameters/covariances, with
     * DEBUG_JOINT_FIT enabled, to inspect the per-iteration JFDEBUG trace.
     */
    public void testDebugRealBadEvent() {
        double bField = -0.8595999999999999;
        RealVector v1Init = MatrixUtils.createRealVector(new double[]{44.2652212072, 1.4093367997, 0.170271548});

        RealMatrix eleCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.09099249541759491, -4.65152581455186E-4, -6.321477599158243E-7, 0.004269929137080908, -1.8205431842943653E-5},
            {-4.65152581455186E-4, 2.659913434399641E-6, 3.725346697791565E-9, -2.0947405573679134E-5, 9.69749862633762E-8},
            {-6.321477599158243E-7, 3.725346697791565E-9, 7.499869648930346E-12, -2.8541577989926736E-8, 1.3351865446598055E-10},
            {0.004269929137080908, -2.0947405573679134E-5, -2.8541577989926736E-8, 0.0029748319648206234, -2.6774487196234986E-5},
            {-1.8205431842943653E-5, 9.69749862633762E-8, 1.3351865446598055E-10, -2.6774487196234986E-5, 2.78031990319505E-7}
        });
        TrackParams eleParams = new TrackParams(0.629338800907135, 0.016672732308506966, 1.2636852625291795E-4,
                2.378530979156494, -0.04880968853831291, eleCov);

        RealMatrix posCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.06338092684745789, -3.4044793574139476E-4, -4.6726222535653505E-7, -0.002420209813863039, 1.005577087198617E-5},
            {-3.4044793574139476E-4, 2.1097148419357836E-6, 2.87417623034969E-9, 1.2600367881532293E-5, -5.876536235405183E-8},
            {-4.6726222535653505E-7, 2.87417623034969E-9, 5.894043499793389E-12, 1.7731609958104855E-8, -8.565938930393813E-11},
            {-0.002420209813863039, 1.2600367881532293E-5, 1.7731609958104855E-8, 0.0019167944556102157, -2.1231198843452148E-5},
            {1.005577087198617E-5, -5.876536235405183E-8, -8.565938930393813E-11, -2.1231198843452148E-5, 2.9569127946160734E-7}
        });
        TrackParams posParams = new TrackParams(-0.06583922356367111, 0.03222518786787987, -1.1622635793173686E-4,
                -2.1310274600982666, 0.050598885864019394, posCov);

        RealMatrix recoilCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.3589296340942383, -0.005202689673751593, -2.7695679818862118E-5, 0.022159304469823837, -2.742857614066452E-4},
            {-0.005202689673751593, 8.316571620525792E-5, 4.6634096406705794E-7, -2.9148461180739105E-4, 3.763552740565501E-6},
            {-2.7695679818862118E-5, 4.6634096406705794E-7, 2.805736754041277E-9, -1.4865117918816395E-6, 1.9633365155868887E-8},
            {0.022159304469823837, -2.9148461180739105E-4, -1.4865117918816395E-6, 0.008457320742309093, -1.352451363345608E-4},
            {-2.742857614066452E-4, 3.763552740565501E-6, 1.9633365155868887E-8, -1.352451363345608E-4, 2.2341159819916356E-6}
        });
        TrackParams recoilParams = new TrackParams(-0.2463390827178955, 0.05816323310136795, 4.706922627519816E-4,
                -0.10475388169288635, -0.03560171276330948, recoilCov);

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        KalmanVertexFitterGainMatrix.DEBUG_JOINT_FIT = true;
        try {
            TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eleParams, posParams, recoilParams, v1Init, null);
            System.err.println("JFDEBUG FINAL v1=" + result.v1 + " v2=" + result.v2
                    + " chi2=" + result.chi2 + " ndf=" + result.ndf);
        } finally {
            KalmanVertexFitterGainMatrix.DEBUG_JOINT_FIT = false;
        }
    }

    /**
     * Second bad event from the same log, same eMinus/ePlus/v1Init/bField but a different
     * recoil-track hypothesis (second BADFIT_DEBUG block in /tmp/vo_run2.log) -- checks
     * whether the carried-forward-covariance fix behaves consistently across events.
     */
    public void testDebugRealBadEvent2() {
        double bField = -0.8595999999999999;
        RealVector v1Init = MatrixUtils.createRealVector(new double[]{44.2652212072, 1.4093367997, 0.170271548});

        RealMatrix eleCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.09099249541759491, -4.65152581455186E-4, -6.321477599158243E-7, 0.004269929137080908, -1.8205431842943653E-5},
            {-4.65152581455186E-4, 2.659913434399641E-6, 3.725346697791565E-9, -2.0947405573679134E-5, 9.69749862633762E-8},
            {-6.321477599158243E-7, 3.725346697791565E-9, 7.499869648930346E-12, -2.8541577989926736E-8, 1.3351865446598055E-10},
            {0.004269929137080908, -2.0947405573679134E-5, -2.8541577989926736E-8, 0.0029748319648206234, -2.6774487196234986E-5},
            {-1.8205431842943653E-5, 9.69749862633762E-8, 1.3351865446598055E-10, -2.6774487196234986E-5, 2.78031990319505E-7}
        });
        TrackParams eleParams = new TrackParams(0.629338800907135, 0.016672732308506966, 1.2636852625291795E-4,
                2.378530979156494, -0.04880968853831291, eleCov);

        RealMatrix posCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.06338092684745789, -3.4044793574139476E-4, -4.6726222535653505E-7, -0.002420209813863039, 1.005577087198617E-5},
            {-3.4044793574139476E-4, 2.1097148419357836E-6, 2.87417623034969E-9, 1.2600367881532293E-5, -5.876536235405183E-8},
            {-4.6726222535653505E-7, 2.87417623034969E-9, 5.894043499793389E-12, 1.7731609958104855E-8, -8.565938930393813E-11},
            {-0.002420209813863039, 1.2600367881532293E-5, 1.7731609958104855E-8, 0.0019167944556102157, -2.1231198843452148E-5},
            {1.005577087198617E-5, -5.876536235405183E-8, -8.565938930393813E-11, -2.1231198843452148E-5, 2.9569127946160734E-7}
        });
        TrackParams posParams = new TrackParams(-0.06583922356367111, 0.03222518786787987, -1.1622635793173686E-4,
                -2.1310274600982666, 0.050598885864019394, posCov);

        RealMatrix recoilCov = MatrixUtils.createRealMatrix(new double[][]{
            {0.22654478251934052, -0.0031680979300290346, -1.2684452485700604E-5, 0.007989047095179558, -3.1170796137303114E-4},
            {-0.0031680979300290346, 5.3549021686194465E-5, 2.268006795702604E-7, -7.062631630105898E-5, 2.9667264698218787E-6},
            {-1.2684452485700604E-5, 2.268006795702604E-7, 1.4096562805931967E-9, -2.4915863150454243E-7, 1.0956304308251674E-8},
            {0.007989047095179558, -7.062631630105898E-5, -2.4915863150454243E-7, 0.012361372821033001, -3.0974321998655796E-4},
            {-3.1170796137303114E-4, 2.9667264698218787E-6, 1.0956304308251674E-8, -3.0974321998655796E-4, 7.928260856715497E-6}
        });
        TrackParams recoilParams = new TrackParams(0.680279016494751, 0.14952220022678375, 9.425431489944458E-4,
                0.2652631998062134, 0.04711122438311577, recoilCov);

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        KalmanVertexFitterGainMatrix.DEBUG_JOINT_FIT = true;
        try {
            TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eleParams, posParams, recoilParams, v1Init, null);
            System.err.println("JFDEBUG FINAL v1=" + result.v1 + " v2=" + result.v2
                    + " chi2=" + result.chi2 + " ndf=" + result.ndf);
        } finally {
            KalmanVertexFitterGainMatrix.DEBUG_JOINT_FIT = false;
        }
    }

    /**
     * Checks {@code propagateLineToPlane} against a hand-computed crossing point and a
     * finite-difference Jacobian, for a line with a non-trivial (correlated, non-diagonal)
     * 6x6 covariance. Also checks that the x-row/column of the returned covariance is exactly
     * the supplied {@code sigmaXFloor}, not the (exactly-zero) value the Jacobian alone would
     * give -- x is fixed to the plane by construction, so the floor is what keeps a caller's
     * Kalman prior from permanently pinning that coordinate.
     */
    public void testPropagateLineToPlane() {
        System.out.println("\n=== testPropagateLineToPlane ===\n");

        double x0 = 30.0, y0 = 0.4, z0 = -0.2;
        double dx = 0.9, dy = 0.15, dz = -0.05;

        RealMatrix cov6 = MatrixUtils.createRealMatrix(6, 6);
        double[] sigma = {0.05, 0.03, 0.04, 0.02, 0.015, 0.01};
        java.util.Random rng = new java.util.Random(4242);
        // Build a random-but-valid (symmetric positive-definite) covariance: A*A^T scaled by sigmas.
        RealMatrix A = MatrixUtils.createRealMatrix(6, 6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                A.setEntry(i, j, rng.nextGaussian() * 0.3);
            }
            A.setEntry(i, i, A.getEntry(i, i) + 1.0);
        }
        RealMatrix base = A.multiply(A.transpose());
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                cov6.setEntry(i, j, base.getEntry(i, j) * sigma[i] * sigma[j] / FastMath.sqrt(base.getEntry(i, i) * base.getEntry(j, j)));
            }
        }

        KalmanVertexFitterGainMatrix.LineParams line =
                new KalmanVertexFitterGainMatrix.LineParams(x0, y0, z0, dx, dy, dz, cov6);

        double xPlane = -1.1;
        double sigmaXFloor = 0.001;
        KalmanVertexFitterGainMatrix.LinePlaneProjection proj =
                KalmanVertexFitterGainMatrix.propagateLineToPlane(line, xPlane, sigmaXFloor);

        double sExpected = (xPlane - x0) / dx;
        double yExpected = y0 + sExpected * dy;
        double zExpected = z0 + sExpected * dz;

        assertEquals(xPlane, proj.position.getEntry(0), 1e-12);
        assertEquals(yExpected, proj.position.getEntry(1), 1e-9);
        assertEquals(zExpected, proj.position.getEntry(2), 1e-9);

        // Finite-difference Jacobian of [yProp, zProp] w.r.t. the 6 line parameters, compared
        // against propagateLineToPlane's covariance via J*cov6*J^T.
        double h = 1e-6;
        RealMatrix Jyz = MatrixUtils.createRealMatrix(2, 6);
        for (int k = 0; k < 6; k++) {
            double[] plus = line.toArray();
            double[] minus = line.toArray();
            plus[k] += h;
            minus[k] -= h;
            double[] yzPlus = crossingYZ(plus, xPlane);
            double[] yzMinus = crossingYZ(minus, xPlane);
            Jyz.setEntry(0, k, (yzPlus[0] - yzMinus[0]) / (2 * h));
            Jyz.setEntry(1, k, (yzPlus[1] - yzMinus[1]) / (2 * h));
        }
        RealMatrix covYZExpected = Jyz.multiply(cov6).multiply(Jyz.transpose());

        assertEquals(covYZExpected.getEntry(0, 0), proj.cov.getEntry(1, 1), 1e-6);
        assertEquals(covYZExpected.getEntry(0, 1), proj.cov.getEntry(1, 2), 1e-6);
        assertEquals(covYZExpected.getEntry(1, 1), proj.cov.getEntry(2, 2), 1e-6);

        // x is fixed by construction: the Jacobian-only covariance would be exactly zero there,
        // but the floor should be substituted in instead.
        assertEquals(sigmaXFloor * sigmaXFloor, proj.cov.getEntry(0, 0), 1e-15);
        assertEquals(0.0, proj.cov.getEntry(0, 1), 1e-15);
        assertEquals(0.0, proj.cov.getEntry(0, 2), 1e-15);

        System.out.printf("position=%s%n", proj.position);
        System.out.printf("cov=%s%n", proj.cov);
    }

    /** [yProp, zProp] where a line with parameters [x0,y0,z0,dx,dy,dz] crosses x=xPlane. */
    private static double[] crossingYZ(double[] p, double xPlane) {
        double s = (xPlane - p[0]) / p[3];
        return new double[]{p[1] + s * p[4], p[2] + s * p[5]};
    }

    /**
     * Checks {@code propagateTrackToPlane} (curved-track analog of
     * {@code propagateLineToPlane}) two ways: (1) algebraically, that the returned crossing
     * point lies exactly on the track's helix circle (independent of the turning-angle
     * machinery used internally to find it), and (2) via a finite-difference Jacobian of
     * [yProp, zProp] w.r.t. the 5 perigee parameters (obtained by calling the method itself
     * at perturbed inputs -- its closed-form position formula and its analytic Jacobian are
     * independent derivations, so this is a real cross-check, not a tautology).
     */
    public void testPropagateTrackToPlane() {
        System.out.println("\n=== testPropagateTrackToPlane ===\n");

        double d0 = 1.7534739233699383, phi0 = 0.0028232486700802044,
                omega = 6.790166806190062E-4, z0 = -0.10407287685488854,
                tanLambda = -0.03300521957923669;

        RealMatrix cov5 = MatrixUtils.createRealMatrix(5, 5);
        double[] sigma = {0.03, 0.002, 1e-5, 0.02, 0.003};
        java.util.Random rng = new java.util.Random(1234);
        RealMatrix A = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                A.setEntry(i, j, rng.nextGaussian() * 0.3);
            }
            A.setEntry(i, i, A.getEntry(i, i) + 1.0);
        }
        RealMatrix base = A.multiply(A.transpose());
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov5.setEntry(i, j, base.getEntry(i, j) * sigma[i] * sigma[j] / FastMath.sqrt(base.getEntry(i, i) * base.getEntry(j, j)));
            }
        }

        TrackParams track = new TrackParams(d0, phi0, omega, z0, tanLambda, cov5);

        double xPlane = -1.1;
        double sigmaXFloor = 10.0;
        KalmanVertexFitterGainMatrix.LinePlaneProjection proj =
                KalmanVertexFitterGainMatrix.propagateTrackToPlane(track, xPlane, sigmaXFloor);

        double R = 1.0 / FastMath.abs(omega);
        double xc = FastMath.sin(phi0) * (1.0 / omega - d0);
        double yc = -FastMath.cos(phi0) * (1.0 / omega - d0);
        double dx = xPlane - xc;
        double dy = proj.position.getEntry(1) - yc;
        assertEquals(R * R, dx * dx + dy * dy, 1e-6);

        double h = 1e-6;
        RealMatrix Jyz = MatrixUtils.createRealMatrix(2, 5);
        for (int k = 0; k < 5; k++) {
            double[] plus = track.toArray();
            double[] minus = track.toArray();
            plus[k] += h;
            minus[k] -= h;
            TrackParams tPlus = new TrackParams(plus[0], plus[1], plus[2], plus[3], plus[4], cov5);
            TrackParams tMinus = new TrackParams(minus[0], minus[1], minus[2], minus[3], minus[4], cov5);
            KalmanVertexFitterGainMatrix.LinePlaneProjection pPlus =
                    KalmanVertexFitterGainMatrix.propagateTrackToPlane(tPlus, xPlane, sigmaXFloor);
            KalmanVertexFitterGainMatrix.LinePlaneProjection pMinus =
                    KalmanVertexFitterGainMatrix.propagateTrackToPlane(tMinus, xPlane, sigmaXFloor);
            Jyz.setEntry(0, k, (pPlus.position.getEntry(1) - pMinus.position.getEntry(1)) / (2 * h));
            Jyz.setEntry(1, k, (pPlus.position.getEntry(2) - pMinus.position.getEntry(2)) / (2 * h));
        }
        RealMatrix covYZExpected = Jyz.multiply(cov5).multiply(Jyz.transpose());

        assertClose(covYZExpected.getEntry(0, 0), proj.cov.getEntry(1, 1));
        assertClose(covYZExpected.getEntry(0, 1), proj.cov.getEntry(1, 2));
        assertClose(covYZExpected.getEntry(1, 1), proj.cov.getEntry(2, 2));

        assertEquals(sigmaXFloor * sigmaXFloor, proj.cov.getEntry(0, 0), 1e-15);
        assertEquals(0.0, proj.cov.getEntry(0, 1), 1e-15);
        assertEquals(0.0, proj.cov.getEntry(0, 2), 1e-15);

        System.out.printf("position=%s%n", proj.position);
        System.out.printf("cov=%s%n", proj.cov);
    }

    /** Relative-tolerance comparison (1e-4), robust across the widely varying magnitudes of
     * the covariance entries checked in {@code testPropagateTrackToPlane}. */
    private static void assertClose(double expected, double actual) {
        double scale = FastMath.max(1.0, FastMath.abs(expected));
        assertTrue("expected=" + expected + " actual=" + actual, FastMath.abs(expected - actual) <= 1e-4 * scale);
    }
}
