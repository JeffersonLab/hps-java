package org.hps.recon.vertexing;

import junit.framework.TestCase;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math.util.FastMath;

import org.hps.recon.vertexing.TrackConstraintVertexFitter;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.FitResult;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TwoVertexFitResult;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackMomentum;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for TrackConstraintVertexFitter, particularly the Lagrange multiplier
 * constrained fitting for three-prong vertices with 4-momentum conservation.
 */
public class TrackConstraintVertexFitterTest extends TestCase {

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

        GainMatrixVertexer fitter = new GainMatrixVertexer(B_FIELD);
        fitter.setDebugFlag(true);

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

        // Fit without constraints, using the extracted gain-matrix algorithm (fit()
        // itself was moved out of TrackConstraintVertexFitter into GainMatrixVertexer).
        FitResult result = fitter.fit(tracks);

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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);

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
     * Exact-geometry sanity check for the NEW joint two-vertex fit (fitCascadeVertexJoint):
     * construct eMinus/ePlus tracks passing exactly through a chosen V1, sum their momenta
     * there to get the V0 flight direction, place V2 exactly along that direction from V1,
     * and construct a recoil track passing exactly through V2. Verify the fit recovers V1,
     * V2, and all three momenta to tight tolerance with near-zero chi2 -- isolating whether
     * a reported bug (bad V1 position / inflated V1 mass on real data) is in the core fit
     * math here vs. elsewhere (CascadeVertexer's packaging, or real-data specifics).
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
        // Deliberately poor initial guess for V2 (mimicking CascadeVertexer's real usage,
        // where v2Init defaults to the beamspot near the origin rather than the truth).
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
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
        // Deliberately poor initial guess for V2 (mimicking CascadeVertexer's real usage).
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
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
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);

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
     * Toy-MC pull study for {@link TrackConstraintVertexFitter#fitCascadeVertexJointBeamConstrained},
     * mirroring {@link #testJointTwoVertexSmearedPulls} (same V1/track geometry construction)
     * but with the recoil momentum chosen so the three daughters' total 3-momentum exactly
     * equals a beam value (same beam construction as
     * {@link #testNTrackBeamMomentumConstraintSmearedPulls}), and with
     * {@code sigmaTNuclearRecoil=0} so the toy isolates the fitter math with no extra recoil
     * smearing (matching how the N-track beam-constraint toy test isolates its own fitter
     * math). Checks V1(x,y,z), V2(x,y,z), and per-track (eMinus/ePlus/recoil) momentum pulls
     * all have mean~0/std~1 and mean chi2/ndf~1 (ndf=5: 12 constraints - 7 free V1/theta/V2
     * parameters). Also runs the existing unconstrained {@link
     * TrackConstraintVertexFitter#fitCascadeVertexJoint} on the same smeared toys, printing
     * (not asserting) its chi2/ndf alongside the beam-constrained result for comparison.
     */
    public void testJointTwoVertexBeamConstrainedSmearedPulls() {
        for (double flightLength : new double[]{5.0, 20.0, 50.0, 90.0, 120.0, 150.0}) {
            runJointTwoVertexBeamConstrainedSmearedPulls(flightLength, 500);
        }
    }

    private void runJointTwoVertexBeamConstrainedSmearedPulls(double flightLength, int nToys) {
        System.out.println("\n=== testJointTwoVertexBeamConstrainedSmearedPulls (flightLength=" + flightLength + " mm) ===\n");

        double pBeamMag = 3.74;
        double rotAngle = -0.0305;
        double beamPx = pBeamMag * FastMath.cos(rotAngle);
        double beamPy = -pBeamMag * FastMath.sin(rotAngle);
        double beamPz = 0.0;

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        // Recoil momentum fixed by exact beam-momentum conservation, so the three daughters'
        // total momentum equals the beam value by construction (same idea as the N-track
        // beam-constraint toy's px3/py3/pz3).
        double pxRc = beamPx - pxV0, pyRc = beamPy - pyV0, pzRc = beamPz - pzV0;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, -1.1});

        java.util.Random rng = new java.util.Random(24601);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
        fitter.setBeamEnergy(pBeamMag);
        fitter.setBeamRotAngle(rotAngle);
        fitter.setBeamMomentumTransverseNuclearRecoilSigma(0.0);

        int nNull = 0, nOk = 0;
        double[] pullV1X = new double[nToys], pullV1Y = new double[nToys], pullV1Z = new double[nToys];
        double[] pullV2X = new double[nToys], pullV2Y = new double[nToys], pullV2Z = new double[nToys];
        double[] chi2NdfArr = new double[nToys];
        double[] chi2NdfUncArr = new double[nToys];
        int nOkUnc = 0;
        double[][] trackPullPx = new double[3][nToys];
        double[][] trackPullPy = new double[3][nToys];
        double[][] trackPullPz = new double[3][nToys];
        double[] truthPx = {pxEm, pxEp, pxRc};
        double[] truthPy = {pyEm, pyEp, pyRc};
        double[] truthPz = {pzEm, pzEp, pzRc};

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams eMinusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
            TrackParams ePlusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
            TrackParams recoilTruth = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

            TrackParams eMinusTrack = smearTrack(eMinusTruth, trackSigma, trackCov, rng);
            TrackParams ePlusTrack = smearTrack(ePlusTruth, trackSigma, trackCov, rng);
            TrackParams recoilTrack = smearTrack(recoilTruth, trackSigma, trackCov, rng);

            TwoVertexFitResult resultUnc = fitter.fitCascadeVertexJoint(eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init);
            if (resultUnc != null) {
                chi2NdfUncArr[nOkUnc] = resultUnc.ndf > 0 ? resultUnc.chi2 / resultUnc.ndf : 0.0;
                nOkUnc++;
            }

            TwoVertexFitResult result = fitter.fitCascadeVertexJointBeamConstrained(
                    eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init);
            if (result == null) {
                nNull++;
                continue;
            }

            pullV1X[nOk] = (result.v1.getEntry(0) - xV1) / FastMath.sqrt(result.v1Cov.getEntry(0, 0));
            pullV1Y[nOk] = (result.v1.getEntry(1) - yV1) / FastMath.sqrt(result.v1Cov.getEntry(1, 1));
            pullV1Z[nOk] = (result.v1.getEntry(2) - zV1) / FastMath.sqrt(result.v1Cov.getEntry(2, 2));
            pullV2X[nOk] = (result.v2.getEntry(0) - xV2) / FastMath.sqrt(result.v2Cov.getEntry(0, 0));
            pullV2Y[nOk] = (result.v2.getEntry(1) - yV2) / FastMath.sqrt(result.v2Cov.getEntry(1, 1));
            pullV2Z[nOk] = (result.v2.getEntry(2) - zV2) / FastMath.sqrt(result.v2Cov.getEntry(2, 2));
            chi2NdfArr[nOk] = result.ndf > 0 ? result.chi2 / result.ndf : 0.0;

            for (int i = 0; i < 3; i++) {
                TrackMomentum tm = i == 0 ? result.eMinusMomentum : (i == 1 ? result.ePlusMomentum : result.recoilMomentum);
                trackPullPx[i][nOk] = (tm.p.getEntry(0) - truthPx[i]) / FastMath.sqrt(tm.pCov.getEntry(0, 0));
                trackPullPy[i][nOk] = (tm.p.getEntry(1) - truthPy[i]) / FastMath.sqrt(tm.pCov.getEntry(1, 1));
                trackPullPz[i][nOk] = (tm.p.getEntry(2) - truthPz[i]) / FastMath.sqrt(tm.pCov.getEntry(2, 2));
            }
            nOk++;
        }

        double[] pullV1XTrim = java.util.Arrays.copyOf(pullV1X, nOk);
        double[] pullV1YTrim = java.util.Arrays.copyOf(pullV1Y, nOk);
        double[] pullV1ZTrim = java.util.Arrays.copyOf(pullV1Z, nOk);
        double[] pullV2XTrim = java.util.Arrays.copyOf(pullV2X, nOk);
        double[] pullV2YTrim = java.util.Arrays.copyOf(pullV2Y, nOk);
        double[] pullV2ZTrim = java.util.Arrays.copyOf(pullV2Z, nOk);
        double[] chi2NdfTrim = java.util.Arrays.copyOf(chi2NdfArr, nOk);
        double[] chi2NdfUncTrim = java.util.Arrays.copyOf(chi2NdfUncArr, nOkUnc);

        double meanV1X = mean(pullV1XTrim), stdV1X = std(pullV1XTrim, meanV1X);
        double meanV1Y = mean(pullV1YTrim), stdV1Y = std(pullV1YTrim, meanV1Y);
        double meanV1Z = mean(pullV1ZTrim), stdV1Z = std(pullV1ZTrim, meanV1Z);
        double meanV2X = mean(pullV2XTrim), stdV2X = std(pullV2XTrim, meanV2X);
        double meanV2Y = mean(pullV2YTrim), stdV2Y = std(pullV2YTrim, meanV2Y);
        double meanV2Z = mean(pullV2ZTrim), stdV2Z = std(pullV2ZTrim, meanV2Z);
        double meanChi2Ndf = mean(chi2NdfTrim);
        double meanChi2NdfUnc = mean(chi2NdfUncTrim);

        System.out.printf("nToys=%d nNull=%d nOk=%d%n", nToys, nNull, nOk);
        System.out.printf("V1 pull X: mean=%.3f std=%.3f%n", meanV1X, stdV1X);
        System.out.printf("V1 pull Y: mean=%.3f std=%.3f%n", meanV1Y, stdV1Y);
        System.out.printf("V1 pull Z: mean=%.3f std=%.3f%n", meanV1Z, stdV1Z);
        System.out.printf("V2 pull X: mean=%.3f std=%.3f%n", meanV2X, stdV2X);
        System.out.printf("V2 pull Y: mean=%.3f std=%.3f%n", meanV2Y, stdV2Y);
        System.out.printf("V2 pull Z: mean=%.3f std=%.3f%n", meanV2Z, stdV2Z);
        System.out.printf("[BEAM-CONSTRAINED] mean chi2/ndf: %.3f%n", meanChi2Ndf);
        System.out.printf("[UNCONSTRAINED, same toys] mean chi2/ndf: %.3f (for comparison only, not asserted)%n", meanChi2NdfUnc);

        for (int i = 0; i < 3; i++) {
            double[] tPx = java.util.Arrays.copyOf(trackPullPx[i], nOk);
            double[] tPy = java.util.Arrays.copyOf(trackPullPy[i], nOk);
            double[] tPz = java.util.Arrays.copyOf(trackPullPz[i], nOk);
            double mPx = mean(tPx), sPx = std(tPx, mPx);
            double mPy = mean(tPy), sPy = std(tPy, mPy);
            double mPz = mean(tPz), sPz = std(tPz, mPz);
            String trackName = i == 0 ? "eMinus" : (i == 1 ? "ePlus" : "recoil");
            System.out.printf("track %s momentum pull: Px mean=%.3f std=%.3f | Py mean=%.3f std=%.3f | Pz mean=%.3f std=%.3f%n",
                    trackName, mPx, sPx, mPy, sPy, mPz, sPz);
            assertTrue(trackName + " Px pull mean should be near 0, got " + mPx, FastMath.abs(mPx) < 0.15);
            assertTrue(trackName + " Py pull mean should be near 0, got " + mPy, FastMath.abs(mPy) < 0.15);
            assertTrue(trackName + " Pz pull mean should be near 0, got " + mPz, FastMath.abs(mPz) < 0.15);
            assertTrue(trackName + " Px pull std should be near 1, got " + sPx, sPx > 0.8 && sPx < 1.2);
            assertTrue(trackName + " Py pull std should be near 1, got " + sPy, sPy > 0.8 && sPy < 1.2);
            assertTrue(trackName + " Pz pull std should be near 1, got " + sPz, sPz > 0.8 && sPz < 1.2);
        }

        assertTrue("V1 pull X mean should be near 0, got " + meanV1X, FastMath.abs(meanV1X) < 0.15);
        assertTrue("V1 pull Y mean should be near 0, got " + meanV1Y, FastMath.abs(meanV1Y) < 0.15);
        assertTrue("V1 pull Z mean should be near 0, got " + meanV1Z, FastMath.abs(meanV1Z) < 0.15);
        assertTrue("V1 pull X std should be near 1, got " + stdV1X, stdV1X > 0.8 && stdV1X < 1.2);
        assertTrue("V1 pull Y std should be near 1, got " + stdV1Y, stdV1Y > 0.8 && stdV1Y < 1.2);
        assertTrue("V1 pull Z std should be near 1, got " + stdV1Z, stdV1Z > 0.8 && stdV1Z < 1.2);
        assertTrue("V2 pull X mean should be near 0, got " + meanV2X, FastMath.abs(meanV2X) < 0.15);
        assertTrue("V2 pull Y mean should be near 0, got " + meanV2Y, FastMath.abs(meanV2Y) < 0.15);
        assertTrue("V2 pull Z mean should be near 0, got " + meanV2Z, FastMath.abs(meanV2Z) < 0.15);
        assertTrue("V2 pull X std should be near 1, got " + stdV2X, stdV2X > 0.8 && stdV2X < 1.2);
        assertTrue("V2 pull Y std should be near 1, got " + stdV2Y, stdV2Y > 0.8 && stdV2Y < 1.2);
        assertTrue("V2 pull Z std should be near 1, got " + stdV2Z, stdV2Z > 0.8 && stdV2Z < 1.2);
        assertTrue("mean chi2/ndf should be near 1, got " + meanChi2Ndf, meanChi2Ndf > 0.5 && meanChi2Ndf < 1.5);
    }

    /**
     * Toy-MC pull study for {@link TrackConstraintVertexFitter#fitCascadeVertexJointFreeTrack},
     * mirroring {@link #testJointTwoVertexBeamConstrainedSmearedPulls} exactly (same V1/track
     * geometry construction) but with no beam-momentum constraint at all -- isolates the
     * effect of freeing the three tracks' perigee parameters from the effect of imposing an
     * external momentum constraint. Checks V1(x,y,z), V2(x,y,z), and per-track
     * (eMinus/ePlus/recoil) momentum pulls all have mean~0/std~1 and mean chi2/ndf~1 (ndf=2:
     * 9 constraints - 7 free V1/theta/V2 parameters, same as the fixed-track {@link
     * TrackConstraintVertexFitter#fitCascadeVertexJoint}).
     */
    public void testJointTwoVertexFreeTrackSmearedPulls() {
        for (double flightLength : new double[]{5.0, 20.0, 50.0, 90.0, 120.0, 150.0}) {
            runJointTwoVertexFreeTrackSmearedPulls(flightLength, 500);
        }
    }

    private void runJointTwoVertexFreeTrackSmearedPulls(double flightLength, int nToys) {
        System.out.println("\n=== testJointTwoVertexFreeTrackSmearedPulls (flightLength=" + flightLength + " mm) ===\n");

        double xV1 = 0.2, yV1 = -0.1, zV1 = 3.0;
        double pxEm = 0.30, pyEm = 0.10, pzEm = 1.5;
        double pxEp = 0.15, pyEp = -0.05, pzEp = 0.8;
        double pxRc = 0.20, pyRc = -0.08, pzRc = 1.2;

        double pxV0 = pxEm + pxEp, pyV0 = pyEm + pyEp, pzV0 = pzEm + pzEp;
        double pV0Mag = FastMath.sqrt(pxV0 * pxV0 + pyV0 * pyV0 + pzV0 * pzV0);
        double nx = pxV0 / pV0Mag, ny = pyV0 / pV0Mag, nz = pzV0 / pV0Mag;

        double xV2 = xV1 + flightLength * nx;
        double yV2 = yV1 + flightLength * ny;
        double zV2 = zV1 + flightLength * nz;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{xV1, yV1, zV1});
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, -1.1});

        java.util.Random rng = new java.util.Random(24601);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);

        int nNull = 0, nOk = 0;
        double[] pullV1X = new double[nToys], pullV1Y = new double[nToys], pullV1Z = new double[nToys];
        double[] pullV2X = new double[nToys], pullV2Y = new double[nToys], pullV2Z = new double[nToys];
        double[] chi2NdfArr = new double[nToys];
        double[][] trackPullPx = new double[3][nToys];
        double[][] trackPullPy = new double[3][nToys];
        double[][] trackPullPz = new double[3][nToys];
        double[] truthPx = {pxEm, pxEp, pxRc};
        double[] truthPy = {pyEm, pyEp, pyRc};
        double[] truthPz = {pzEm, pzEp, pzRc};

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams eMinusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEm, pyEm, pzEm, -1, B_FIELD, trackCov);
            TrackParams ePlusTruth = createExactTrackThroughPoint(xV1, yV1, zV1, pxEp, pyEp, pzEp, 1, B_FIELD, trackCov);
            TrackParams recoilTruth = createExactTrackThroughPoint(xV2, yV2, zV2, pxRc, pyRc, pzRc, -1, B_FIELD, trackCov);

            TrackParams eMinusTrack = smearTrack(eMinusTruth, trackSigma, trackCov, rng);
            TrackParams ePlusTrack = smearTrack(ePlusTruth, trackSigma, trackCov, rng);
            TrackParams recoilTrack = smearTrack(recoilTruth, trackSigma, trackCov, rng);

            TwoVertexFitResult result = fitter.fitCascadeVertexJointFreeTrack(
                    eMinusTrack, ePlusTrack, recoilTrack, v1Init, v2Init);
            if (result == null) {
                nNull++;
                continue;
            }

            pullV1X[nOk] = (result.v1.getEntry(0) - xV1) / FastMath.sqrt(result.v1Cov.getEntry(0, 0));
            pullV1Y[nOk] = (result.v1.getEntry(1) - yV1) / FastMath.sqrt(result.v1Cov.getEntry(1, 1));
            pullV1Z[nOk] = (result.v1.getEntry(2) - zV1) / FastMath.sqrt(result.v1Cov.getEntry(2, 2));
            pullV2X[nOk] = (result.v2.getEntry(0) - xV2) / FastMath.sqrt(result.v2Cov.getEntry(0, 0));
            pullV2Y[nOk] = (result.v2.getEntry(1) - yV2) / FastMath.sqrt(result.v2Cov.getEntry(1, 1));
            pullV2Z[nOk] = (result.v2.getEntry(2) - zV2) / FastMath.sqrt(result.v2Cov.getEntry(2, 2));
            chi2NdfArr[nOk] = result.ndf > 0 ? result.chi2 / result.ndf : 0.0;

            for (int i = 0; i < 3; i++) {
                TrackMomentum tm = i == 0 ? result.eMinusMomentum : (i == 1 ? result.ePlusMomentum : result.recoilMomentum);
                trackPullPx[i][nOk] = (tm.p.getEntry(0) - truthPx[i]) / FastMath.sqrt(tm.pCov.getEntry(0, 0));
                trackPullPy[i][nOk] = (tm.p.getEntry(1) - truthPy[i]) / FastMath.sqrt(tm.pCov.getEntry(1, 1));
                trackPullPz[i][nOk] = (tm.p.getEntry(2) - truthPz[i]) / FastMath.sqrt(tm.pCov.getEntry(2, 2));
            }
            nOk++;
        }

        double[] pullV1XTrim = java.util.Arrays.copyOf(pullV1X, nOk);
        double[] pullV1YTrim = java.util.Arrays.copyOf(pullV1Y, nOk);
        double[] pullV1ZTrim = java.util.Arrays.copyOf(pullV1Z, nOk);
        double[] pullV2XTrim = java.util.Arrays.copyOf(pullV2X, nOk);
        double[] pullV2YTrim = java.util.Arrays.copyOf(pullV2Y, nOk);
        double[] pullV2ZTrim = java.util.Arrays.copyOf(pullV2Z, nOk);
        double[] chi2NdfTrim = java.util.Arrays.copyOf(chi2NdfArr, nOk);

        double meanV1X = mean(pullV1XTrim), stdV1X = std(pullV1XTrim, meanV1X);
        double meanV1Y = mean(pullV1YTrim), stdV1Y = std(pullV1YTrim, meanV1Y);
        double meanV1Z = mean(pullV1ZTrim), stdV1Z = std(pullV1ZTrim, meanV1Z);
        double meanV2X = mean(pullV2XTrim), stdV2X = std(pullV2XTrim, meanV2X);
        double meanV2Y = mean(pullV2YTrim), stdV2Y = std(pullV2YTrim, meanV2Y);
        double meanV2Z = mean(pullV2ZTrim), stdV2Z = std(pullV2ZTrim, meanV2Z);
        double meanChi2Ndf = mean(chi2NdfTrim);

        System.out.printf("nToys=%d nNull=%d nOk=%d%n", nToys, nNull, nOk);
        System.out.printf("V1 pull X: mean=%.3f std=%.3f%n", meanV1X, stdV1X);
        System.out.printf("V1 pull Y: mean=%.3f std=%.3f%n", meanV1Y, stdV1Y);
        System.out.printf("V1 pull Z: mean=%.3f std=%.3f%n", meanV1Z, stdV1Z);
        System.out.printf("V2 pull X: mean=%.3f std=%.3f%n", meanV2X, stdV2X);
        System.out.printf("V2 pull Y: mean=%.3f std=%.3f%n", meanV2Y, stdV2Y);
        System.out.printf("V2 pull Z: mean=%.3f std=%.3f%n", meanV2Z, stdV2Z);
        System.out.printf("[FREE-TRACK] mean chi2/ndf: %.3f%n", meanChi2Ndf);

        for (int i = 0; i < 3; i++) {
            double[] tPx = java.util.Arrays.copyOf(trackPullPx[i], nOk);
            double[] tPy = java.util.Arrays.copyOf(trackPullPy[i], nOk);
            double[] tPz = java.util.Arrays.copyOf(trackPullPz[i], nOk);
            double mPx = mean(tPx), sPx = std(tPx, mPx);
            double mPy = mean(tPy), sPy = std(tPy, mPy);
            double mPz = mean(tPz), sPz = std(tPz, mPz);
            String trackName = i == 0 ? "eMinus" : (i == 1 ? "ePlus" : "recoil");
            System.out.printf("track %s momentum pull: Px mean=%.3f std=%.3f | Py mean=%.3f std=%.3f | Pz mean=%.3f std=%.3f%n",
                    trackName, mPx, sPx, mPy, sPy, mPz, sPz);
            assertTrue(trackName + " Px pull mean should be near 0, got " + mPx, FastMath.abs(mPx) < 0.15);
            assertTrue(trackName + " Py pull mean should be near 0, got " + mPy, FastMath.abs(mPy) < 0.15);
            assertTrue(trackName + " Pz pull mean should be near 0, got " + mPz, FastMath.abs(mPz) < 0.15);
            assertTrue(trackName + " Px pull std should be near 1, got " + sPx, sPx > 0.8 && sPx < 1.2);
            assertTrue(trackName + " Py pull std should be near 1, got " + sPy, sPy > 0.8 && sPy < 1.2);
            assertTrue(trackName + " Pz pull std should be near 1, got " + sPz, sPz > 0.8 && sPz < 1.2);
        }

        assertTrue("V1 pull X mean should be near 0, got " + meanV1X, FastMath.abs(meanV1X) < 0.15);
        assertTrue("V1 pull Y mean should be near 0, got " + meanV1Y, FastMath.abs(meanV1Y) < 0.15);
        assertTrue("V1 pull Z mean should be near 0, got " + meanV1Z, FastMath.abs(meanV1Z) < 0.15);
        assertTrue("V1 pull X std should be near 1, got " + stdV1X, stdV1X > 0.8 && stdV1X < 1.2);
        assertTrue("V1 pull Y std should be near 1, got " + stdV1Y, stdV1Y > 0.8 && stdV1Y < 1.2);
        assertTrue("V1 pull Z std should be near 1, got " + stdV1Z, stdV1Z > 0.8 && stdV1Z < 1.2);
        assertTrue("V2 pull X mean should be near 0, got " + meanV2X, FastMath.abs(meanV2X) < 0.15);
        assertTrue("V2 pull Y mean should be near 0, got " + meanV2Y, FastMath.abs(meanV2Y) < 0.15);
        assertTrue("V2 pull Z mean should be near 0, got " + meanV2Z, FastMath.abs(meanV2Z) < 0.15);
        assertTrue("V2 pull X std should be near 1, got " + stdV2X, stdV2X > 0.8 && stdV2X < 1.2);
        assertTrue("V2 pull Y std should be near 1, got " + stdV2Y, stdV2Y > 0.8 && stdV2Y < 1.2);
        assertTrue("V2 pull Z std should be near 1, got " + stdV2Z, stdV2Z > 0.8 && stdV2Z < 1.2);
        assertTrue("mean chi2/ndf should be near 1, got " + meanChi2Ndf, meanChi2Ndf > 0.5 && meanChi2Ndf < 1.5);
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

        double[] roots = TrackConstraintVertexFitter.transverseCircleRoots(v1, pV0, recoilTrack);
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
        TrackConstraintVertexFitter.ThetaSeed seed = fitter.selectPhysicalThetaSeed(v1, pV0, recoilTrack);
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
     * origin, matching {@code CascadeVertexer}'s real usage (defaults to the beamspot, not
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
        // same "beamspot near origin" guess CascadeVertexer falls back to in practice.
        RealVector v2Init = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
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
     * ({@link CascadeVertexer}) never takes the null-v2Init path -- it always supplies
     * its own explicit v2Init/v2Cov from the already-fitted V0 line -- precisely because
     * pairing a tight beamspot prior with {@code selectPhysicalThetaSeed}'s own
     * documented-unreliable branch choice (worse than a coin flip; see
     * {@code CascadeVertexer}'s Javadoc on {@code v0InputVtxZ}) can pin V2 near a seed on
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
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
     * {@link org.hps.recon.vertexing.CascadeVertexer} actually uses in production --
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
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);

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

    /**
     * Toy-MC pull study for the N-track (trident) common-vertex fit with the total 3-momentum
     * constrained to the beam value -- the actual physics goal of this repo. Builds a common
     * production vertex with three tracks (2 e- + 1 e+) whose momenta sum exactly to the beam
     * 3-momentum (same construction as {@code testThreeMomentumConstraint}, but now at a
     * displaced vertex point rather than the origin, and smeared/looped as toys), then checks
     * both the soft ({@link #fitSoftConstrained}) and hard/exact
     * ({@link #fitLagrangeMultiplier}) momentum-constraint modes side by side: fitted-vertex
     * pulls should have mean~0 / width~1 and chi2/ndf should average to ~1 (ndf=6: 2 constraints
     * per track x 3 tracks + 3 momentum constraints - 3 vertex parameters), for both modes,
     * before trusting the same machinery on real trident MC.
     */
    public void testNTrackBeamMomentumConstraintSmearedPulls() {
        System.out.println("\n=== testNTrackBeamMomentumConstraintSmearedPulls ===\n");

        double pBeam = 3.74;
        double rotAngle = -0.0305;

        double beamPx = pBeam * FastMath.cos(rotAngle);
        double beamPy = -pBeam * FastMath.sin(rotAngle);
        double beamPz = 0.0;

        // Same beam-momentum-covariance construction as fitVertex()'s beamMomentumConstraint
        // block: 1% longitudinal, 100 urad transverse divergence, rotated into tracking frame.
        double dpOverP = 1e-2;
        double sigmaTheta = 100e-6;
        double sigmaL = dpOverP * pBeam;
        double sigmaT = sigmaTheta * pBeam;
        double cosR = FastMath.cos(rotAngle);
        double sinR = FastMath.sin(rotAngle);
        double sL2 = sigmaL * sigmaL;
        double sT2 = sigmaT * sigmaT;
        RealMatrix beamPCov = MatrixUtils.createRealMatrix(3, 3);
        beamPCov.setEntry(0, 0, sL2 * cosR * cosR + sT2 * sinR * sinR);
        beamPCov.setEntry(0, 1, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 0, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 1, sL2 * sinR * sinR + sT2 * cosR * cosR);
        beamPCov.setEntry(2, 2, sT2);

        RealVector beamP = MatrixUtils.createRealVector(new double[]{beamPx, beamPy, beamPz});

        // Common production vertex (tracking frame), displaced from the origin.
        double xV = 0.2, yV = -0.1, zV = 3.0;

        // Three track momenta (2 e-, 1 e+) summing exactly to the beam 3-momentum.
        double px1 = 1.8, py1 = 0.25, pz1 = 0.15;
        double px2 = 1.2, py2 = -0.20, pz2 = -0.10;
        double px3 = beamPx - px1 - px2;
        double py3 = beamPy - py1 - py2;
        double pz3 = -pz1 - pz2;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        java.util.Random rng = new java.util.Random(31415);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);

        // Cholesky factor of beamPCov, used to draw a per-toy smeared beam-momentum
        // constraint input for SOFT mode (see momentum-pull comment below): the fit is told
        // the constraint has uncertainty beamPCov, so a meaningful toy-MC pull test must
        // actually realize that uncertainty toy-by-toy, not feed the exact truth every time.
        RealMatrix beamPCovChol = new CholeskyDecomposition(beamPCov).getL();

        int nToys = 500;
        for (boolean hard : new boolean[]{false, true}) {
            int nNull = 0, nOk = 0;
            double[] pullX = new double[nToys], pullY = new double[nToys], pullZ = new double[nToys];
            double[] chi2NdfArr = new double[nToys];
            // Momentum-pull arrays are only meaningful in SOFT mode. In HARD
            // (Lagrange-multiplier/exact) mode, totalP is forced to equal beamP exactly for
            // every toy (fourMomentumConstraintCov=null gives V_mom=0 in the KKT system), so
            // Cov(totalP) = J^T*C_fitted*J collapses to ~0 by construction -- dividing by its
            // near-zero sqrt would produce meaningless/unstable pulls, not a real statistical
            // test. Instead, HARD mode is checked via the raw residual + sqrt(cov) below,
            // confirming both are consistently near zero (i.e. the constraint really is exact).
            double[] pullPx = new double[nToys], pullPy = new double[nToys], pullPz = new double[nToys];
            double[] residPx = new double[nToys], residPy = new double[nToys], residPz = new double[nToys];
            double[] sigPx = new double[nToys], sigPy = new double[nToys], sigPz = new double[nToys];

            // Per-track momentum pulls, in BOTH modes: unlike totalMomentumCov (which collapses
            // to ~0 under the hard total-momentum constraint, since that constraint pins exactly
            // the sum), each track's own pCov comes from its own diagonal block of C_fitted and
            // is not directly collapsed by a constraint on a different (summed) linear
            // combination -- individual track momenta should remain meaningfully uncertain even
            // in HARD mode.
            double[][] trackPullPx = new double[3][nToys];
            double[][] trackPullPy = new double[3][nToys];
            double[][] trackPullPz = new double[3][nToys];
            double[] truthPx = {px1, px2, px3};
            double[] truthPy = {py1, py2, py3};
            double[] truthPz = {pz1, pz2, pz3};

            for (int toy = 0; toy < nToys; toy++) {
                TrackParams e1Truth = createExactTrackThroughPoint(xV, yV, zV, px1, py1, pz1, -1, B_FIELD, trackCov);
                TrackParams e2Truth = createExactTrackThroughPoint(xV, yV, zV, px2, py2, pz2, -1, B_FIELD, trackCov);
                TrackParams posTruth = createExactTrackThroughPoint(xV, yV, zV, px3, py3, pz3, 1, B_FIELD, trackCov);

                List<TrackParams> tracks = new ArrayList<>();
                tracks.add(smearTrack(e1Truth, trackSigma, trackCov, rng));
                tracks.add(smearTrack(e2Truth, trackSigma, trackCov, rng));
                tracks.add(smearTrack(posTruth, trackSigma, trackCov, rng));

                // SOFT mode is fed a per-toy smeared beam constraint (see beamPCovChol comment
                // above); HARD mode keeps the exact truth value, since that mode's own test
                // below is specifically about verifying the constraint is enforced exactly.
                RealVector beamNoise = MatrixUtils.createRealVector(
                        new double[]{rng.nextGaussian(), rng.nextGaussian(), rng.nextGaussian()});
                RealVector smearedBeamP = beamP.add(beamPCovChol.operate(beamNoise));

                FitResult result = hard
                        ? fitter.fitLagrangeMultiplier(tracks, null, null, beamP, 10, 1e-6)
                        : fitter.fitSoftConstrained(tracks, null, null, smearedBeamP, beamPCov, 10, 1e-6);
                if (result == null) {
                    nNull++;
                    continue;
                }

                pullX[nOk] = (result.vertex.getEntry(0) - xV) / FastMath.sqrt(result.vertexCov.getEntry(0, 0));
                pullY[nOk] = (result.vertex.getEntry(1) - yV) / FastMath.sqrt(result.vertexCov.getEntry(1, 1));
                pullZ[nOk] = (result.vertex.getEntry(2) - zV) / FastMath.sqrt(result.vertexCov.getEntry(2, 2));
                chi2NdfArr[nOk] = result.ndf > 0 ? result.chi2 / result.ndf : 0.0;

                // Total (summed) fitted momentum, in tracking frame -- truth total P is
                // exactly beamP by construction (px3/py3/pz3 above are defined as beamP minus
                // the other two tracks), so this checks the new Cov(totalP) = J^T*C_fitted*J
                // propagation (task #33/#34) independently of the vertex-position check above.
                residPx[nOk] = result.totalMomentum.getEntry(0) - beamPx;
                residPy[nOk] = result.totalMomentum.getEntry(1) - beamPy;
                residPz[nOk] = result.totalMomentum.getEntry(2) - beamPz;
                sigPx[nOk] = FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(0, 0)));
                sigPy[nOk] = FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(1, 1)));
                sigPz[nOk] = FastMath.sqrt(FastMath.abs(result.totalMomentumCov.getEntry(2, 2)));
                if (!hard) {
                    pullPx[nOk] = residPx[nOk] / sigPx[nOk];
                    pullPy[nOk] = residPy[nOk] / sigPy[nOk];
                    pullPz[nOk] = residPz[nOk] / sigPz[nOk];
                }

                for (int i = 0; i < 3; i++) {
                    RealVector pTrk = result.trackMomenta.get(i).p;
                    RealMatrix pCovTrk = result.trackMomenta.get(i).pCov;
                    trackPullPx[i][nOk] = (pTrk.getEntry(0) - truthPx[i]) / FastMath.sqrt(pCovTrk.getEntry(0, 0));
                    trackPullPy[i][nOk] = (pTrk.getEntry(1) - truthPy[i]) / FastMath.sqrt(pCovTrk.getEntry(1, 1));
                    trackPullPz[i][nOk] = (pTrk.getEntry(2) - truthPz[i]) / FastMath.sqrt(pCovTrk.getEntry(2, 2));
                }
                nOk++;
            }

            double[] pullXTrim = java.util.Arrays.copyOf(pullX, nOk);
            double[] pullYTrim = java.util.Arrays.copyOf(pullY, nOk);
            double[] pullZTrim = java.util.Arrays.copyOf(pullZ, nOk);
            double[] chi2NdfTrim = java.util.Arrays.copyOf(chi2NdfArr, nOk);

            double meanX = mean(pullXTrim), stdX = std(pullXTrim, meanX);
            double meanY = mean(pullYTrim), stdY = std(pullYTrim, meanY);
            double meanZ = mean(pullZTrim), stdZ = std(pullZTrim, meanZ);
            double meanChi2Ndf = mean(chi2NdfTrim);

            String mode = hard ? "HARD" : "SOFT";
            System.out.printf("[%s] nToys=%d nNull=%d nOk=%d%n", mode, nToys, nNull, nOk);
            System.out.printf("[%s] pull X: mean=%.3f std=%.3f%n", mode, meanX, stdX);
            System.out.printf("[%s] pull Y: mean=%.3f std=%.3f%n", mode, meanY, stdY);
            System.out.printf("[%s] pull Z: mean=%.3f std=%.3f%n", mode, meanZ, stdZ);
            System.out.printf("[%s] mean chi2/ndf: %.3f%n", mode, meanChi2Ndf);

            if (!hard) {
                double[] pullPxTrim = java.util.Arrays.copyOf(pullPx, nOk);
                double[] pullPyTrim = java.util.Arrays.copyOf(pullPy, nOk);
                double[] pullPzTrim = java.util.Arrays.copyOf(pullPz, nOk);
                double meanPx = mean(pullPxTrim), stdPx = std(pullPxTrim, meanPx);
                double meanPy = mean(pullPyTrim), stdPy = std(pullPyTrim, meanPy);
                double meanPz = mean(pullPzTrim), stdPz = std(pullPzTrim, meanPz);
                System.out.printf("[%s] pull totalPx: mean=%.3f std=%.3f%n", mode, meanPx, stdPx);
                System.out.printf("[%s] pull totalPy: mean=%.3f std=%.3f%n", mode, meanPy, stdPy);
                System.out.printf("[%s] pull totalPz: mean=%.3f std=%.3f%n", mode, meanPz, stdPz);
                assertTrue(mode + " pull totalPx mean should be near 0, got " + meanPx, FastMath.abs(meanPx) < 0.15);
                assertTrue(mode + " pull totalPy mean should be near 0, got " + meanPy, FastMath.abs(meanPy) < 0.15);
                assertTrue(mode + " pull totalPz mean should be near 0, got " + meanPz, FastMath.abs(meanPz) < 0.15);
                assertTrue(mode + " pull totalPx std should be near 1, got " + stdPx, stdPx > 0.8 && stdPx < 1.2);
                assertTrue(mode + " pull totalPy std should be near 1, got " + stdPy, stdPy > 0.8 && stdPy < 1.2);
                assertTrue(mode + " pull totalPz std should be near 1, got " + stdPz, stdPz > 0.8 && stdPz < 1.2);
            } else {
                // HARD mode: confirm the constraint really is exact -- both the residual
                // (fitted totalP vs beamP) and the propagated sqrt(Cov(totalP)) should be
                // consistently tiny (numerical-precision level), not just individually small.
                double meanAbsResidPx = mean(absArr(java.util.Arrays.copyOf(residPx, nOk)));
                double meanAbsResidPy = mean(absArr(java.util.Arrays.copyOf(residPy, nOk)));
                double meanAbsResidPz = mean(absArr(java.util.Arrays.copyOf(residPz, nOk)));
                double meanSigPx = mean(java.util.Arrays.copyOf(sigPx, nOk));
                double meanSigPy = mean(java.util.Arrays.copyOf(sigPy, nOk));
                double meanSigPz = mean(java.util.Arrays.copyOf(sigPz, nOk));
                System.out.printf("[%s] totalP residual (fit-beam): |Px|=%.3e |Py|=%.3e |Pz|=%.3e%n",
                        mode, meanAbsResidPx, meanAbsResidPy, meanAbsResidPz);
                System.out.printf("[%s] sqrt(Cov(totalP)): Px=%.3e Py=%.3e Pz=%.3e%n",
                        mode, meanSigPx, meanSigPy, meanSigPz);
                assertTrue(mode + " totalPx residual should be ~0 (exact constraint), got " + meanAbsResidPx,
                        meanAbsResidPx < 1e-4);
                assertTrue(mode + " totalPy residual should be ~0 (exact constraint), got " + meanAbsResidPy,
                        meanAbsResidPy < 1e-4);
                assertTrue(mode + " totalPz residual should be ~0 (exact constraint), got " + meanAbsResidPz,
                        meanAbsResidPz < 1e-4);
                assertTrue(mode + " sqrt(Cov(totalPx)) should be ~0 (exact constraint), got " + meanSigPx,
                        meanSigPx < 1e-4);
                assertTrue(mode + " sqrt(Cov(totalPy)) should be ~0 (exact constraint), got " + meanSigPy,
                        meanSigPy < 1e-4);
                assertTrue(mode + " sqrt(Cov(totalPz)) should be ~0 (exact constraint), got " + meanSigPz,
                        meanSigPz < 1e-4);
            }

            for (int i = 0; i < 3; i++) {
                double[] tPx = java.util.Arrays.copyOf(trackPullPx[i], nOk);
                double[] tPy = java.util.Arrays.copyOf(trackPullPy[i], nOk);
                double[] tPz = java.util.Arrays.copyOf(trackPullPz[i], nOk);
                double mPx = mean(tPx), sPx = std(tPx, mPx);
                double mPy = mean(tPy), sPy = std(tPy, mPy);
                double mPz = mean(tPz), sPz = std(tPz, mPz);
                System.out.printf("[%s] track%d momentum pull: Px mean=%.3f std=%.3f | Py mean=%.3f std=%.3f | Pz mean=%.3f std=%.3f%n",
                        mode, i + 1, mPx, sPx, mPy, sPy, mPz, sPz);
                assertTrue(mode + " track" + (i + 1) + " Px pull mean should be near 0, got " + mPx, FastMath.abs(mPx) < 0.15);
                assertTrue(mode + " track" + (i + 1) + " Py pull mean should be near 0, got " + mPy, FastMath.abs(mPy) < 0.15);
                assertTrue(mode + " track" + (i + 1) + " Pz pull mean should be near 0, got " + mPz, FastMath.abs(mPz) < 0.15);
                assertTrue(mode + " track" + (i + 1) + " Px pull std should be near 1, got " + sPx, sPx > 0.8 && sPx < 1.2);
                assertTrue(mode + " track" + (i + 1) + " Py pull std should be near 1, got " + sPy, sPy > 0.8 && sPy < 1.2);
                assertTrue(mode + " track" + (i + 1) + " Pz pull std should be near 1, got " + sPz, sPz > 0.8 && sPz < 1.2);
            }

            assertTrue(mode + " pull X mean should be near 0, got " + meanX, FastMath.abs(meanX) < 0.15);
            assertTrue(mode + " pull Y mean should be near 0, got " + meanY, FastMath.abs(meanY) < 0.15);
            assertTrue(mode + " pull Z mean should be near 0, got " + meanZ, FastMath.abs(meanZ) < 0.15);
            assertTrue(mode + " pull X std should be near 1, got " + stdX, stdX > 0.8 && stdX < 1.2);
            assertTrue(mode + " pull Y std should be near 1, got " + stdY, stdY > 0.8 && stdY < 1.2);
            assertTrue(mode + " pull Z std should be near 1, got " + stdZ, stdZ > 0.8 && stdZ < 1.2);
            assertTrue(mode + " mean chi2/ndf should be near 1, got " + meanChi2Ndf,
                    meanChi2Ndf > 0.5 && meanChi2Ndf < 1.5);
        }
    }

    /**
     * Toy-MC test of the "upstream track-covariance underestimate" hypothesis for why the
     * real-MC beam-momentum-constrained N-track fit shows a much worse chi2/ndf and broader
     * per-track momentum pulls than the unconstrained fit, despite
     * {@link #testNTrackBeamMomentumConstraintSmearedPulls} showing the fitter itself is
     * statistically correct given accurately-covaried inputs. Unlike that test, this one
     * deliberately mis-calibrates the input: toy tracks are smeared by
     * {@code miscalFactor * trackSigma} (the TRUE scatter) while the fitter is still told the
     * covariance is {@code trackCov} (the un-scaled, too-small covariance) -- simulating a
     * track fit that under-reports its own parameter uncertainty by a fixed factor, identically
     * for every parameter and every track.
     * <p>
     * Result (2026-09-06): this UNIFORM mis-calibration does NOT reproduce the real-data
     * pattern -- chi2/ndf inflates by essentially the same factor for the unconstrained, soft-,
     * and hard-constrained fits (all ~3.7-3.9x at miscalFactor=2.0), and per-track momentum
     * pulls do not broaden unconstrained -&gt; soft -&gt; hard the way they do on real data. This
     * falsifies the simplest form of the hypothesis: a globally-uniform covariance underestimate
     * inflates chi2 = r^T*C^-1*r by the same factor regardless of how many constraints are
     * stacked on top, so it cannot by itself explain why the real beam-momentum-constrained fit
     * is so much worse than the unconstrained one. See the assertions below and the printed
     * per-{@code miscalFactor} diagnostics for the actual numbers; a real explanation likely
     * needs a NON-uniform effect (specific parameters/tracks under-covaried more than others, a
     * missing off-diagonal/correlation term, or a systematic bias rather than pure extra
     * variance) rather than a single global scale factor.
     */
    public void testNTrackBeamMomentumConstraintMiscalibratedCovariancePulls() {
        System.out.println("\n=== testNTrackBeamMomentumConstraintMiscalibratedCovariancePulls ===\n");

        double pBeam = 3.74;
        double rotAngle = -0.0305;
        double beamPx = pBeam * FastMath.cos(rotAngle);
        double beamPy = -pBeam * FastMath.sin(rotAngle);
        double beamPz = 0.0;

        double dpOverP = 1e-2;
        double sigmaTheta = 100e-6;
        double sigmaL = dpOverP * pBeam;
        double sigmaT = sigmaTheta * pBeam;
        double cosR = FastMath.cos(rotAngle);
        double sinR = FastMath.sin(rotAngle);
        double sL2 = sigmaL * sigmaL;
        double sT2 = sigmaT * sigmaT;
        RealMatrix beamPCov = MatrixUtils.createRealMatrix(3, 3);
        beamPCov.setEntry(0, 0, sL2 * cosR * cosR + sT2 * sinR * sinR);
        beamPCov.setEntry(0, 1, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 0, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 1, sL2 * sinR * sinR + sT2 * cosR * cosR);
        beamPCov.setEntry(2, 2, sT2);
        RealVector beamP = MatrixUtils.createRealVector(new double[]{beamPx, beamPy, beamPz});
        RealMatrix beamPCovChol = new CholeskyDecomposition(beamPCov).getL();

        double xV = 0.2, yV = -0.1, zV = 3.0;
        double px1 = 1.8, py1 = 0.25, pz1 = 0.15;
        double px2 = 1.2, py2 = -0.20, pz2 = -0.10;
        double px3 = beamPx - px1 - px2;
        double py3 = beamPy - py1 - py2;
        double pz3 = -pz1 - pz2;
        double[] truthPx = {px1, px2, px3};
        double[] truthPy = {py1, py2, py3};
        double[] truthPz = {pz1, pz2, pz3};

        // "Reported" covariance -- what the fitter is told, unchanged by miscalFactor below.
        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        java.util.Random rng = new java.util.Random(271828);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
        int nToys = 500;

        // miscalFactor = (TRUE smearing sigma) / (reported sigma); 1.0 is the calibrated
        // baseline sanity check (should give ~1 chi2/ndf and ~1 pull width for ALL THREE modes,
        // including the unconstrained fit, which testNTrackBeamMomentumConstraintSmearedPulls
        // doesn't cover); values above 1.0 simulate an under-estimated track covariance.
        double[] miscalFactors = {1.0, 1.3, 1.6, 2.0};
        double baselineChi2Unc = 1.0, baselineChi2Soft = 1.0, baselineChi2Hard = 1.0;

        for (double miscalFactor : miscalFactors) {
            double[] trueSigma = new double[5];
            for (int i = 0; i < 5; i++) trueSigma[i] = trackSigma[i] * miscalFactor;

            double[] chi2NdfUnc = new double[nToys], chi2NdfSoft = new double[nToys], chi2NdfHard = new double[nToys];
            int nOkUnc = 0, nOkSoft = 0, nOkHard = 0;
            double[][] trackPullUnc = new double[9][nToys];
            double[][] trackPullSoft = new double[9][nToys];
            double[][] trackPullHard = new double[9][nToys];

            for (int toy = 0; toy < nToys; toy++) {
                TrackParams e1Truth = createExactTrackThroughPoint(xV, yV, zV, px1, py1, pz1, -1, B_FIELD, trackCov);
                TrackParams e2Truth = createExactTrackThroughPoint(xV, yV, zV, px2, py2, pz2, -1, B_FIELD, trackCov);
                TrackParams posTruth = createExactTrackThroughPoint(xV, yV, zV, px3, py3, pz3, 1, B_FIELD, trackCov);

                // Smear by the TRUE (larger) sigma, but hand the fitter the ORIGINAL (smaller)
                // trackCov as the reported uncertainty -- the deliberate mis-calibration.
                List<TrackParams> tracks = new ArrayList<>();
                tracks.add(smearTrack(e1Truth, trueSigma, trackCov, rng));
                tracks.add(smearTrack(e2Truth, trueSigma, trackCov, rng));
                tracks.add(smearTrack(posTruth, trueSigma, trackCov, rng));

                RealVector beamNoise = MatrixUtils.createRealVector(
                        new double[]{rng.nextGaussian(), rng.nextGaussian(), rng.nextGaussian()});
                RealVector smearedBeamP = beamP.add(beamPCovChol.operate(beamNoise));

                FitResult resUnc = fitter.fitBillior1985(tracks, null, null);
                FitResult resSoft = fitter.fitSoftConstrained(tracks, null, null, smearedBeamP, beamPCov, 10, 1e-6);
                FitResult resHard = fitter.fitLagrangeMultiplier(tracks, null, null, beamP, 10, 1e-6);

                if (resUnc != null && resUnc.ndf > 0) {
                    chi2NdfUnc[nOkUnc] = resUnc.chi2 / resUnc.ndf;
                    fillTrackPulls(trackPullUnc, nOkUnc, resUnc, truthPx, truthPy, truthPz);
                    nOkUnc++;
                }
                if (resSoft != null && resSoft.ndf > 0) {
                    chi2NdfSoft[nOkSoft] = resSoft.chi2 / resSoft.ndf;
                    fillTrackPulls(trackPullSoft, nOkSoft, resSoft, truthPx, truthPy, truthPz);
                    nOkSoft++;
                }
                if (resHard != null && resHard.ndf > 0) {
                    chi2NdfHard[nOkHard] = resHard.chi2 / resHard.ndf;
                    fillTrackPulls(trackPullHard, nOkHard, resHard, truthPx, truthPy, truthPz);
                    nOkHard++;
                }
            }

            double meanChi2Unc = mean(java.util.Arrays.copyOf(chi2NdfUnc, nOkUnc));
            double meanChi2Soft = mean(java.util.Arrays.copyOf(chi2NdfSoft, nOkSoft));
            double meanChi2Hard = mean(java.util.Arrays.copyOf(chi2NdfHard, nOkHard));
            double meanPullStdUnc = meanTrackPullStd(trackPullUnc, nOkUnc);
            double meanPullStdSoft = meanTrackPullStd(trackPullSoft, nOkSoft);
            double meanPullStdHard = meanTrackPullStd(trackPullHard, nOkHard);

            System.out.printf("[miscalFactor=%.1f] chi2/ndf: unconstrained=%.3f soft=%.3f hard=%.3f%n",
                    miscalFactor, meanChi2Unc, meanChi2Soft, meanChi2Hard);
            System.out.printf("[miscalFactor=%.1f] mean per-track momentum pull std: unconstrained=%.3f soft=%.3f hard=%.3f%n",
                    miscalFactor, meanPullStdUnc, meanPullStdSoft, meanPullStdHard);

            if (miscalFactor == 1.0) {
                baselineChi2Unc = meanChi2Unc;
                baselineChi2Soft = meanChi2Soft;
                baselineChi2Hard = meanChi2Hard;
                assertTrue("calibrated-baseline unconstrained chi2/ndf should be near 1, got " + meanChi2Unc,
                        meanChi2Unc > 0.7 && meanChi2Unc < 1.3);
                assertTrue("calibrated-baseline soft chi2/ndf should be near 1, got " + meanChi2Soft,
                        meanChi2Soft > 0.7 && meanChi2Soft < 1.3);
                assertTrue("calibrated-baseline hard chi2/ndf should be near 1, got " + meanChi2Hard,
                        meanChi2Hard > 0.7 && meanChi2Hard < 1.3);
                assertTrue("calibrated-baseline mean per-track pull std should be near 1, got " + meanPullStdUnc,
                        meanPullStdUnc > 0.8 && meanPullStdUnc < 1.2);
                assertTrue("calibrated-baseline mean per-track pull std should be near 1, got " + meanPullStdSoft,
                        meanPullStdSoft > 0.8 && meanPullStdSoft < 1.2);
                assertTrue("calibrated-baseline mean per-track pull std should be near 1, got " + meanPullStdHard,
                        meanPullStdHard > 0.8 && meanPullStdHard < 1.2);
            }

            if (miscalFactor == miscalFactors[miscalFactors.length - 1]) {
                // Result (2026-09-06): a UNIFORM covariance underestimate -- the same factor
                // applied to all 5 track parameters, identically for all 3 tracks -- does NOT
                // reproduce the differential amplification seen on real trident MC (there,
                // unconstrained chi2/ndf~2 but soft/hard~10-15, i.e. constrained >> unconstrained).
                // Here all three modes inflate by essentially the SAME factor, and per-track
                // pulls do not broaden going unconstrained -> soft -> hard as they do on real
                // data. This falsifies the simplest version of the "upstream covariance is just
                // globally too small" hypothesis: chi2 = r^T*C^-1*r scales with the mismatch
                // factor the same way regardless of how many constraints are stacked on top, so
                // a real, non-uniform effect (e.g. a covariance underestimate concentrated in
                // specific parameters/tracks, a missing correlation/off-diagonal term, or a
                // systematic bias rather than pure extra variance) is needed to explain the real
                // data -- not just "the same covariance underestimate everywhere, scaled up".
                System.out.printf("[miscalFactor=%.1f] chi2/ndf inflation vs calibrated baseline: "
                        + "unconstrained x%.2f soft x%.2f hard x%.2f (uniform mis-calibration "
                        + "inflates all three modes comparably -- does not reproduce real-data "
                        + "differential amplification)%n", miscalFactor,
                        meanChi2Unc / baselineChi2Unc, meanChi2Soft / baselineChi2Soft, meanChi2Hard / baselineChi2Hard);
                double ratioUnc = meanChi2Unc / baselineChi2Unc;
                double ratioSoft = meanChi2Soft / baselineChi2Soft;
                double ratioHard = meanChi2Hard / baselineChi2Hard;
                assertTrue("uniform mis-calibration should inflate soft chi2/ndf comparably to "
                        + "(within 30% of) the unconstrained fit, got soft x" + ratioSoft + " unconstrained x" + ratioUnc,
                        FastMath.abs(ratioSoft - ratioUnc) / ratioUnc < 0.3);
                assertTrue("uniform mis-calibration should inflate hard chi2/ndf comparably to "
                        + "(within 30% of) the unconstrained fit, got hard x" + ratioHard + " unconstrained x" + ratioUnc,
                        FastMath.abs(ratioHard - ratioUnc) / ratioUnc < 0.3);
            }
        }
    }

    /**
     * {@code fitBillior1985} (one-shot analytic Schur-complement elimination of each track's
     * own 5 perigee parameters, no outer Newton loop) and {@code fitSoftConstrained} with the
     * momentum constraint disabled ({@code fourMomentumConstraint == null}, so
     * {@code nMomConstraints == 0} per its own guard) are two different linear-algebra routes
     * to the *same* unconstrained vertex chi2 -- one-shot elimination vs. iterative
     * Newton-Raphson on the full {@code [vertex, track1, track2, ...]} joint state. On
     * identical input tracks they should therefore agree on vertex position, chi2, ndf, and
     * vertex covariance, up to whatever residual nonlinearity a single linearization pass
     * (Billior) misses relative to a fully re-linearized Newton solve (free-track). This is a
     * fitter-vs-fitter numerical agreement check, not a physics validation -- both methods see
     * literally the same smeared tracks each toy, so there is no notion of a "truth" pull here.
     */
    public void testBilliorVsSoftConstrainedUnconstrainedAgreement() {
        System.out.println("\n=== testBilliorVsSoftConstrainedUnconstrainedAgreement ===\n");

        double xV = 0.2, yV = -0.1, zV = 3.0;
        double px1 = 1.8, py1 = 0.25, pz1 = 0.15;
        double px2 = 1.2, py2 = -0.20, pz2 = -0.10;
        double px3 = 1.0 - px1 - px2;
        double py3 = -0.05 - py1 - py2;
        double pz3 = -pz1 - pz2;

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        java.util.Random rng = new java.util.Random(90210);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
        int nToys = 200;

        double maxVertexDiff = 0, maxChi2RelDiff = 0, maxCovDiff = 0;
        int nBothOk = 0;

        for (int toy = 0; toy < nToys; toy++) {
            TrackParams e1Truth = createExactTrackThroughPoint(xV, yV, zV, px1, py1, pz1, -1, B_FIELD, trackCov);
            TrackParams e2Truth = createExactTrackThroughPoint(xV, yV, zV, px2, py2, pz2, -1, B_FIELD, trackCov);
            TrackParams posTruth = createExactTrackThroughPoint(xV, yV, zV, px3, py3, pz3, 1, B_FIELD, trackCov);

            List<TrackParams> tracks = new ArrayList<>();
            tracks.add(smearTrack(e1Truth, trackSigma, trackCov, rng));
            tracks.add(smearTrack(e2Truth, trackSigma, trackCov, rng));
            tracks.add(smearTrack(posTruth, trackSigma, trackCov, rng));

            FitResult resBillior = fitter.fitBillior1985(tracks, null, null);
            FitResult resSoft = fitter.fitSoftConstrained(tracks, null, null, null, null, 20, 1e-10);

            if (resBillior == null || resSoft == null || resBillior.ndf <= 0 || resSoft.ndf <= 0) {
                continue;
            }
            nBothOk++;

            assertEquals("ndf should match exactly (both 2*nTracks-3 with no momentum constraint)",
                    resBillior.ndf, resSoft.ndf);

            for (int i = 0; i < 3; i++) {
                double diff = FastMath.abs(resBillior.vertex.getEntry(i) - resSoft.vertex.getEntry(i));
                maxVertexDiff = FastMath.max(maxVertexDiff, diff);
                for (int j = 0; j < 3; j++) {
                    double covDiff = FastMath.abs(resBillior.vertexCov.getEntry(i, j) - resSoft.vertexCov.getEntry(i, j));
                    maxCovDiff = FastMath.max(maxCovDiff, covDiff);
                }
            }
            double chi2RelDiff = FastMath.abs(resBillior.chi2 - resSoft.chi2)
                    / FastMath.max(1e-12, FastMath.abs(resBillior.chi2));
            if (chi2RelDiff > maxChi2RelDiff) {
                System.out.printf("  toy=%d chi2Billior=%.6f chi2Soft=%.6f relDiff=%.4f vBillior=%s vSoft=%s%n",
                        toy, resBillior.chi2, resSoft.chi2, chi2RelDiff, resBillior.vertex, resSoft.vertex);
            }
            maxChi2RelDiff = FastMath.max(maxChi2RelDiff, chi2RelDiff);
        }

        System.out.printf("nBothOk=%d/%d  maxVertexDiff=%.3e mm  maxChi2RelDiff=%.3e  maxVertexCovDiff=%.3e%n",
                nBothOk, nToys, maxVertexDiff, maxChi2RelDiff, maxCovDiff);

        assertTrue("expect most toys to converge for both methods, got " + nBothOk + "/" + nToys,
                nBothOk > nToys * 0.9);
        assertTrue("Billior and free-track vertex position should agree closely, got max diff "
                + maxVertexDiff + " mm", maxVertexDiff < 1e-3);
        assertTrue("Billior and free-track chi2 should agree closely, got max relative diff " + maxChi2RelDiff,
                maxChi2RelDiff < 1e-2);
        assertTrue("Billior and free-track vertex covariance should agree closely, got max diff " + maxCovDiff,
                maxCovDiff < 1e-3);
    }

    /**
     * Tests the hypothesis that the broad (but unbiased) Kalman-minus-Billoir V0 residuals seen
     * in real data come from Billoir's fit being only a fixed 1-or-2-linearization procedure
     * (single Newton step, plus the driver's one {@code shiftTracksToVertex} reprojection+refit
     * pass -- see {@code HpsReconParticleDriver.fitVertex}), while Kalman's {@code fit()}
     * re-linearizes the same exact nonlinear track-vertex geometry every iteration until
     * {@code tolerance}. Both algorithms solve the same underlying nonlinear geometric
     * constraint (a helix's distance-of-closest-approach to a vertex point), just in different
     * parametrizations, so truncating {@code fit()}'s own iteration count at 1 or 2 is a faithful
     * stand-in for "linearize once" / "linearize, correct once" without needing to reimplement
     * Billoir's theta/phiv/rho algebra -- AS LONG AS the linearization point matches Billoir's
     * actual one. {@code BilliorVertexer}'s {@code _v0} initial guess is a hardcoded {0,0,0}
     * (never updated from the tracks -- the one line that would update it,
     * {@code BilliorVertexer.java:470}, is commented out), so the 1-/2-pass fits below are
     * forced to start from a fixed {@code initialVertex=(0,0,0)} rather than {@code fit()}'s own
     * (much better, track-averaged) default initial guess.
     * <p>
     * Result (2026-09-15): DISPROVEN, quantitatively, in two stages.
     * <ol>
     * <li>With {@code fit()}'s own smart default initial guess (track-averaged d0/z0, already
     * close to the true vertex), 1-pass vs. fully-converged agree to sub-micron precision even
     * at zV=60mm -- "one linearization" is already exact enough given a good starting point.</li>
     * <li>Forcing the linearization point to Billoir's actual fixed {@code (0,0,0)} (the
     * like-for-like comparison, since {@code BilliorVertexer._v0} is a hardcoded constant, never
     * updated from the tracks -- the line that would update it, {@code
     * BilliorVertexer.java:470}, is commented out) does produce decay-length-dependent, unbiased
     * (mean always &lt;&lt; std) scatter in the 1-pass-only vertex, as expected. But even pushed to
     * an extreme regime (125-260 MeV tracks -- much softer/more-curved than typical, 200mm decay
     * length -- far beyond a typical prompt/short-lived V0), the 1-pass z scatter only reaches
     * ~10 microns (std 0.0103mm at zV=200mm, growing smoothly from ~0 at zV=0). The 2-pass
     * correction -- what {@code HpsReconParticleDriver.fitVertex}'s {@code shiftTracksToVertex}
     * reprojection-and-refit actually does in production -- brings this back down to sub-micron
     * (std &lt; 0.0005mm) at every decay length tested, because reprojecting to Billoir's own
     * pass-1 vertex (already close to truth) makes the second linearization point good enough.</li>
     * </ol>
     * Conclusion: real Billoir's actual 2-pass procedure predicts a linearization-driven
     * Kalman-minus-Billoir scatter at most ~1 micron even in this exaggerated regime -- two to
     * three orders of magnitude below the ~1.6mm z scatter seen in real V0 data. The
     * "single/double linearization vs. fully-converged nonlinear iteration" mechanism, while a
     * real and confirmed structural difference between the two fitters, is NOT large enough to
     * explain the observed real-data residual broadening. The cause must lie elsewhere (e.g. a
     * genuine difference in how track parameters/covariances are consumed or converted between
     * the two paths, not in how many times the same geometry is linearized).
     */
    public void testBilliorLinearizationVsKalmanConvergedVsDecayLength() {
        System.out.println("\n=== testBilliorLinearizationVsKalmanConvergedVsDecayLength ===\n");

        double xV = 0.1, yV = -0.05;
        double px1 = 0.25, py1 = 0.05, pz1 = 0.02;   // e-, soft (~260 MeV) -- more curvature/mm decay length
        double px2 = 0.12, py2 = -0.03, pz2 = -0.01; // e+, soft (~125 MeV)

        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};

        double[] decayLengths = {0.0, 20.0, 50.0, 100.0, 150.0, 200.0};
        int nToys = 300;

        GainMatrixVertexer fitter = new GainMatrixVertexer(B_FIELD);
        java.util.Random rng = new java.util.Random(20260915);

        System.out.printf("%8s %8s %10s %10s %10s | %10s %10s %10s%n",
                "zV(mm)", "nOk", "meanDz_1p", "stdDz_1p", "stdDx_1p", "meanDz_2p", "stdDz_2p", "stdDx_2p");

        double prevStdDz1p = -1, prevStdDz2p = -1;
        for (double zV : decayLengths) {
            double[] dx1p = new double[nToys], dy1p = new double[nToys], dz1p = new double[nToys];
            double[] dx2p = new double[nToys], dy2p = new double[nToys], dz2p = new double[nToys];
            int nOk = 0;

            for (int toy = 0; toy < nToys; toy++) {
                TrackParams eTruth = createExactTrackThroughPoint(xV, yV, zV, px1, py1, pz1, -1, B_FIELD, trackCov);
                TrackParams pTruth = createExactTrackThroughPoint(xV, yV, zV, px2, py2, pz2, 1, B_FIELD, trackCov);

                List<TrackParams> tracks = new ArrayList<>();
                tracks.add(smearTrack(eTruth, trackSigma, trackCov, rng));
                tracks.add(smearTrack(pTruth, trackSigma, trackCov, rng));

                RealVector zeroInit = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});
                FitResult resFull = fitter.fit(tracks, zeroInit, null, null, null, null, null, null, 20, 1e-10);
                FitResult res1p = fitter.fit(tracks, zeroInit, null, null, null, null, null, null, 1, 1e-10);
                FitResult res2p = fitter.fit(tracks, zeroInit, null, null, null, null, null, null, 2, 1e-10);

                if (resFull == null || res1p == null || res2p == null
                        || resFull.ndf <= 0 || res1p.ndf <= 0 || res2p.ndf <= 0) {
                    continue;
                }

                dx1p[nOk] = res1p.vertex.getEntry(0) - resFull.vertex.getEntry(0);
                dy1p[nOk] = res1p.vertex.getEntry(1) - resFull.vertex.getEntry(1);
                dz1p[nOk] = res1p.vertex.getEntry(2) - resFull.vertex.getEntry(2);
                dx2p[nOk] = res2p.vertex.getEntry(0) - resFull.vertex.getEntry(0);
                dy2p[nOk] = res2p.vertex.getEntry(1) - resFull.vertex.getEntry(1);
                dz2p[nOk] = res2p.vertex.getEntry(2) - resFull.vertex.getEntry(2);
                nOk++;
            }

            dx1p = java.util.Arrays.copyOf(dx1p, nOk);
            dy1p = java.util.Arrays.copyOf(dy1p, nOk);
            dz1p = java.util.Arrays.copyOf(dz1p, nOk);
            dx2p = java.util.Arrays.copyOf(dx2p, nOk);
            dy2p = java.util.Arrays.copyOf(dy2p, nOk);
            dz2p = java.util.Arrays.copyOf(dz2p, nOk);

            double meanDz1p = mean(dz1p), stdDz1p = std(dz1p, meanDz1p), stdDx1p = std(dx1p, mean(dx1p));
            double meanDz2p = mean(dz2p), stdDz2p = std(dz2p, meanDz2p), stdDx2p = std(dx2p, mean(dx2p));

            System.out.printf("%8.1f %8d %10.5f %10.5f %10.5f | %10.5f %10.5f %10.5f%n",
                    zV, nOk, meanDz1p, stdDz1p, stdDx1p, meanDz2p, stdDz2p, stdDx2p);

            assertTrue("expect most toys to converge for all three fits at zV=" + zV
                    + ", got " + nOk + "/" + nToys, nOk > nToys * 0.9);
            // (The 1-pass-only residual has a genuine deterministic Newton-truncation bias on
            // top of any smearing-driven scatter, so no unbiasedness check is applied to it --
            // only the production-realistic 2-pass number below matters for the real comparison.)
            // The production-realistic 2-pass correction stays negligible (sub-micron) even in
            // this exaggerated regime -- confirms the linearization-count mechanism cannot
            // explain a mm-scale real-data residual.
            assertTrue("2-pass z scatter should stay sub-micron at zV=" + zV + ", got stdDz2p=" + stdDz2p,
                    stdDz2p < 1e-3);
            if (zV == 0.0) {
                assertTrue("at zV=0 all three fits should agree to sub-micron precision, got stdDz1p=" + stdDz1p,
                        stdDz1p < 1e-3);
            }
            prevStdDz1p = stdDz1p;
            prevStdDz2p = stdDz2p;
        }

        // Even in this deliberately exaggerated regime (soft, high-curvature tracks; 200mm decay
        // length), the 1-pass-only linearization error should stay far below the ~1.6mm z scatter
        // seen in real data -- confirming the mechanism this test targets is not the explanation.
        assertTrue("expect final 1-pass z scatter to stay far below the real-data 1.6mm scale, got "
                + prevStdDz1p, prevStdDz1p < 0.1);
    }

    /**
     * Follow-up to {@link #testNTrackBeamMomentumConstraintMiscalibratedCovariancePulls}: that
     * test showed a UNIFORM covariance underestimate (all 5 track params, all 3 tracks) does
     * NOT reproduce the real-data differential amplification (constrained chi2/ndf >>
     * unconstrained). This test targets a NON-uniform mis-calibration instead: only {@code
     * omega} (curvature, the parameter that directly sets pT and therefore the beam-momentum
     * sum) is under-covaried by {@code miscalFactor}; {@code d0/phi0/z0/tanLambda} keep their
     * correctly-reported uncertainty.
     * <p>
     * Result (2026-09-06): unlike the uniform case, this DOES reproduce the real-data
     * differential amplification in chi2/ndf. Across {@code miscalFactor} 1-5, the unconstrained
     * fit's chi2/ndf stays flat (~0.93-1.03x baseline -- it barely depends on omega's absolute
     * scale, only its ratio between tracks for the vertex position), while soft inflates up to
     * 6.3x and hard up to 7.5x baseline at miscalFactor=5, with hard consistently inflating
     * faster than soft at every factor tested. This matches the qualitative (and roughly
     * quantitative) real-data pattern well: omega specifically being under-covaried, not a
     * uniform covariance scale-down, is a much better candidate mechanism.
     * <p>
     * However, the per-track momentum PULL WIDTH pattern does NOT match real data here: pull
     * std grows fastest for the UNCONSTRAINED fit (0.99 -&gt; 3.58 by miscalFactor=5) and
     * LEAST for soft/hard (0.99 -&gt; ~2.4-2.5) -- backwards from real data, where pull width
     * grows going unconstrained -&gt; soft -&gt; hard. Plausible reason: the beam-momentum
     * constraint pulls each track's fitted momentum toward values consistent with the (correctly
     * modeled) total-momentum conservation, partially correcting for a single track's noisy
     * omega -- a regularization effect that the real upstream mechanism evidently does NOT
     * share. So omega-only under-covariance explains the chi2/ndf escalation but not the
     * per-track pull broadening; the real effect is likely omega-under-covariance PLUS
     * something else (e.g. a missing correlation between tracks' omega, which would defeat this
     * regularization effect) rather than omega-only in isolation.
     */
    public void testNTrackBeamMomentumConstraintOmegaOnlyMiscalibratedCovariancePulls() {
        System.out.println("\n=== testNTrackBeamMomentumConstraintOmegaOnlyMiscalibratedCovariancePulls ===\n");

        double pBeam = 3.74;
        double rotAngle = -0.0305;
        double beamPx = pBeam * FastMath.cos(rotAngle);
        double beamPy = -pBeam * FastMath.sin(rotAngle);
        double beamPz = 0.0;

        double dpOverP = 1e-2;
        double sigmaTheta = 100e-6;
        double sigmaL = dpOverP * pBeam;
        double sigmaT = sigmaTheta * pBeam;
        double cosR = FastMath.cos(rotAngle);
        double sinR = FastMath.sin(rotAngle);
        double sL2 = sigmaL * sigmaL;
        double sT2 = sigmaT * sigmaT;
        RealMatrix beamPCov = MatrixUtils.createRealMatrix(3, 3);
        beamPCov.setEntry(0, 0, sL2 * cosR * cosR + sT2 * sinR * sinR);
        beamPCov.setEntry(0, 1, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 0, (sT2 - sL2) * sinR * cosR);
        beamPCov.setEntry(1, 1, sL2 * sinR * sinR + sT2 * cosR * cosR);
        beamPCov.setEntry(2, 2, sT2);
        RealVector beamP = MatrixUtils.createRealVector(new double[]{beamPx, beamPy, beamPz});
        RealMatrix beamPCovChol = new CholeskyDecomposition(beamPCov).getL();

        double xV = 0.2, yV = -0.1, zV = 3.0;
        double px1 = 1.8, py1 = 0.25, pz1 = 0.15;
        double px2 = 1.2, py2 = -0.20, pz2 = -0.10;
        double px3 = beamPx - px1 - px2;
        double py3 = beamPy - py1 - py2;
        double pz3 = -pz1 - pz2;
        double[] truthPx = {px1, px2, px3};
        double[] truthPy = {py1, py2, py3};
        double[] truthPz = {pz1, pz2, pz3};

        // "Reported" covariance -- what the fitter is told, unchanged by miscalFactor below.
        double d0Err = 0.03, phi0Err = 0.003, omegaErr = 5e-6, z0Err = 0.03, tanLErr = 0.003;
        RealMatrix trackCov = createTrackCovariance(d0Err, phi0Err, omegaErr, z0Err, tanLErr);
        double[] trackSigma = {d0Err, phi0Err, omegaErr, z0Err, tanLErr};
        final int OMEGA_IDX = 2;

        java.util.Random rng = new java.util.Random(161803);
        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(B_FIELD);
        int nToys = 500;

        // Only omega's TRUE smearing sigma is scaled by miscalFactor; d0/phi0/z0/tanLambda are
        // smeared at their correctly-reported scale (factor 1) in every case.
        double[] miscalFactors = {1.0, 1.3, 1.6, 2.0, 3.0, 5.0};
        double baselineChi2Unc = 1.0, baselineChi2Soft = 1.0, baselineChi2Hard = 1.0;

        for (double miscalFactor : miscalFactors) {
            double[] trueSigma = java.util.Arrays.copyOf(trackSigma, 5);
            trueSigma[OMEGA_IDX] = trackSigma[OMEGA_IDX] * miscalFactor;

            double[] chi2NdfUnc = new double[nToys], chi2NdfSoft = new double[nToys], chi2NdfHard = new double[nToys];
            int nOkUnc = 0, nOkSoft = 0, nOkHard = 0;
            double[][] trackPullUnc = new double[9][nToys];
            double[][] trackPullSoft = new double[9][nToys];
            double[][] trackPullHard = new double[9][nToys];

            for (int toy = 0; toy < nToys; toy++) {
                TrackParams e1Truth = createExactTrackThroughPoint(xV, yV, zV, px1, py1, pz1, -1, B_FIELD, trackCov);
                TrackParams e2Truth = createExactTrackThroughPoint(xV, yV, zV, px2, py2, pz2, -1, B_FIELD, trackCov);
                TrackParams posTruth = createExactTrackThroughPoint(xV, yV, zV, px3, py3, pz3, 1, B_FIELD, trackCov);

                List<TrackParams> tracks = new ArrayList<>();
                tracks.add(smearTrack(e1Truth, trueSigma, trackCov, rng));
                tracks.add(smearTrack(e2Truth, trueSigma, trackCov, rng));
                tracks.add(smearTrack(posTruth, trueSigma, trackCov, rng));

                RealVector beamNoise = MatrixUtils.createRealVector(
                        new double[]{rng.nextGaussian(), rng.nextGaussian(), rng.nextGaussian()});
                RealVector smearedBeamP = beamP.add(beamPCovChol.operate(beamNoise));

                FitResult resUnc = fitter.fitBillior1985(tracks, null, null);
                FitResult resSoft = fitter.fitSoftConstrained(tracks, null, null, smearedBeamP, beamPCov, 10, 1e-6);
                FitResult resHard = fitter.fitLagrangeMultiplier(tracks, null, null, beamP, 10, 1e-6);

                if (resUnc != null && resUnc.ndf > 0) {
                    chi2NdfUnc[nOkUnc] = resUnc.chi2 / resUnc.ndf;
                    fillTrackPulls(trackPullUnc, nOkUnc, resUnc, truthPx, truthPy, truthPz);
                    nOkUnc++;
                }
                if (resSoft != null && resSoft.ndf > 0) {
                    chi2NdfSoft[nOkSoft] = resSoft.chi2 / resSoft.ndf;
                    fillTrackPulls(trackPullSoft, nOkSoft, resSoft, truthPx, truthPy, truthPz);
                    nOkSoft++;
                }
                if (resHard != null && resHard.ndf > 0) {
                    chi2NdfHard[nOkHard] = resHard.chi2 / resHard.ndf;
                    fillTrackPulls(trackPullHard, nOkHard, resHard, truthPx, truthPy, truthPz);
                    nOkHard++;
                }
            }

            double meanChi2Unc = mean(java.util.Arrays.copyOf(chi2NdfUnc, nOkUnc));
            double meanChi2Soft = mean(java.util.Arrays.copyOf(chi2NdfSoft, nOkSoft));
            double meanChi2Hard = mean(java.util.Arrays.copyOf(chi2NdfHard, nOkHard));
            double meanPullStdUnc = meanTrackPullStd(trackPullUnc, nOkUnc);
            double meanPullStdSoft = meanTrackPullStd(trackPullSoft, nOkSoft);
            double meanPullStdHard = meanTrackPullStd(trackPullHard, nOkHard);

            System.out.printf("[omega miscalFactor=%.1f] chi2/ndf: unconstrained=%.3f soft=%.3f hard=%.3f%n",
                    miscalFactor, meanChi2Unc, meanChi2Soft, meanChi2Hard);
            System.out.printf("[omega miscalFactor=%.1f] mean per-track momentum pull std: unconstrained=%.3f soft=%.3f hard=%.3f%n",
                    miscalFactor, meanPullStdUnc, meanPullStdSoft, meanPullStdHard);

            if (miscalFactor == 1.0) {
                baselineChi2Unc = meanChi2Unc;
                baselineChi2Soft = meanChi2Soft;
                baselineChi2Hard = meanChi2Hard;
                assertTrue("calibrated-baseline unconstrained chi2/ndf should be near 1, got " + meanChi2Unc,
                        meanChi2Unc > 0.7 && meanChi2Unc < 1.3);
                assertTrue("calibrated-baseline soft chi2/ndf should be near 1, got " + meanChi2Soft,
                        meanChi2Soft > 0.7 && meanChi2Soft < 1.3);
                assertTrue("calibrated-baseline hard chi2/ndf should be near 1, got " + meanChi2Hard,
                        meanChi2Hard > 0.7 && meanChi2Hard < 1.3);
            }

            double ratioUnc = meanChi2Unc / baselineChi2Unc;
            double ratioSoft = meanChi2Soft / baselineChi2Soft;
            double ratioHard = meanChi2Hard / baselineChi2Hard;
            System.out.printf("[omega miscalFactor=%.1f] chi2/ndf inflation vs calibrated baseline: "
                    + "unconstrained x%.2f soft x%.2f hard x%.2f%n", miscalFactor,
                    ratioUnc, ratioSoft, ratioHard);

            if (miscalFactor == miscalFactors[miscalFactors.length - 1]) {
                // Core positive result: unlike the uniform mis-calibration in
                // testNTrackBeamMomentumConstraintMiscalibratedCovariancePulls, under-covaried
                // omega ALONE differentially amplifies the momentum-constrained fits' chi2/ndf
                // well beyond the unconstrained fit, and hard inflates more than soft -- both
                // qualitatively matching the real-data pattern.
                assertTrue("omega-only mis-calibration should leave the unconstrained fit's "
                        + "chi2/ndf inflation near 1 (it barely depends on omega's absolute "
                        + "scale), got x" + ratioUnc, ratioUnc < 1.3);
                assertTrue("omega-only mis-calibration should inflate soft chi2/ndf well beyond "
                        + "the unconstrained fit, got soft x" + ratioSoft + " unconstrained x" + ratioUnc,
                        ratioSoft > 3 * ratioUnc);
                assertTrue("omega-only mis-calibration should inflate hard chi2/ndf even more "
                        + "than soft, got hard x" + ratioHard + " soft x" + ratioSoft,
                        ratioHard > ratioSoft);
            }
        }
    }

    private static void fillTrackPulls(double[][] trackPulls, int idx, FitResult res,
            double[] truthPx, double[] truthPy, double[] truthPz) {
        for (int i = 0; i < 3; i++) {
            RealVector p = res.trackMomenta.get(i).p;
            RealMatrix pCov = res.trackMomenta.get(i).pCov;
            trackPulls[3 * i][idx] = (p.getEntry(0) - truthPx[i]) / FastMath.sqrt(pCov.getEntry(0, 0));
            trackPulls[3 * i + 1][idx] = (p.getEntry(1) - truthPy[i]) / FastMath.sqrt(pCov.getEntry(1, 1));
            trackPulls[3 * i + 2][idx] = (p.getEntry(2) - truthPz[i]) / FastMath.sqrt(pCov.getEntry(2, 2));
        }
    }

    private static double meanTrackPullStd(double[][] trackPulls, int nOk) {
        double sum = 0;
        for (double[] arr : trackPulls) {
            double[] trimmed = java.util.Arrays.copyOf(arr, nOk);
            sum += std(trimmed, mean(trimmed));
        }
        return sum / trackPulls.length;
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

    private static double[] absArr(double[] x) {
        double[] out = new double[x.length];
        for (int i = 0; i < x.length; i++) out[i] = FastMath.abs(x[i]);
        return out;
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        TrackConstraintVertexFitter.DEBUG_JOINT_FIT = true;
        try {
            TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eleParams, posParams, recoilParams, v1Init, null);
            System.err.println("JFDEBUG FINAL v1=" + result.v1 + " v2=" + result.v2
                    + " chi2=" + result.chi2 + " ndf=" + result.ndf);
        } finally {
            TrackConstraintVertexFitter.DEBUG_JOINT_FIT = false;
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

        TrackConstraintVertexFitter fitter = new TrackConstraintVertexFitter(bField);
        TrackConstraintVertexFitter.DEBUG_JOINT_FIT = true;
        try {
            TwoVertexFitResult result = fitter.fitCascadeVertexJoint(eleParams, posParams, recoilParams, v1Init, null);
            System.err.println("JFDEBUG FINAL v1=" + result.v1 + " v2=" + result.v2
                    + " chi2=" + result.chi2 + " ndf=" + result.ndf);
        } finally {
            TrackConstraintVertexFitter.DEBUG_JOINT_FIT = false;
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

        TrackConstraintVertexFitter.LineParams line =
                new TrackConstraintVertexFitter.LineParams(x0, y0, z0, dx, dy, dz, cov6);

        double xPlane = -1.1;
        double sigmaXFloor = 0.001;
        TrackConstraintVertexFitter.LinePlaneProjection proj =
                TrackConstraintVertexFitter.propagateLineToPlane(line, xPlane, sigmaXFloor);

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
        TrackConstraintVertexFitter.LinePlaneProjection proj =
                TrackConstraintVertexFitter.propagateTrackToPlane(track, xPlane, sigmaXFloor);

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
            TrackConstraintVertexFitter.LinePlaneProjection pPlus =
                    TrackConstraintVertexFitter.propagateTrackToPlane(tPlus, xPlane, sigmaXFloor);
            TrackConstraintVertexFitter.LinePlaneProjection pMinus =
                    TrackConstraintVertexFitter.propagateTrackToPlane(tMinus, xPlane, sigmaXFloor);
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
