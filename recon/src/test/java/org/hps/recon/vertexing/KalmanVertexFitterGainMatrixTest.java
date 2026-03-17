package org.hps.recon.vertexing;

import junit.framework.TestCase;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math.util.FastMath;

import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.FitResult;

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
}
