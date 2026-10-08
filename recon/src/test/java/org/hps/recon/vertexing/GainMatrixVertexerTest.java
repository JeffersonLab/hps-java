package org.hps.recon.vertexing;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import junit.framework.TestCase;

import org.hps.recon.vertexing.TrackConstraintVertexFitter.FitResult;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;

/**
 * Exact-geometry sanity check for the extracted gain-matrix {@link GainMatrixVertexer}: build
 * an electron and a positron track that are both constructed to pass exactly through the same
 * known point, with small (non-singular) measurement covariances, and verify {@code fit}
 * recovers that point with near-zero chi2. Kept alongside {@link NTrackVertexerTest}'s
 * equivalent Billoir-batch check so the superseded gain-matrix algorithm still has a working
 * regression test even though nothing in {@code src/main} calls it anymore.
 */
public class GainMatrixVertexerTest extends TestCase {

    private static final double B_FIELD = 0.5; // Tesla
    private static final double C = 2.99792458e-4;

    public void testFitVertexExactGeometry() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;

        TrackParams eleTrack = makeTrackParamsThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        TrackParams posTrack = makeTrackParamsThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        GainMatrixVertexer vertexer = new GainMatrixVertexer(B_FIELD);
        FitResult result = vertexer.fit(Arrays.asList(eleTrack, posTrack));

        assertNotNull("fit result should not be null", result);

        RealVector vertex = result.vertex;
        System.out.printf("Fitted vertex (trk frame): [%.6f, %.6f, %.6f]  (truth: [%.6f, %.6f, %.6f])%n",
                vertex.getEntry(0), vertex.getEntry(1), vertex.getEntry(2), xV, yV, zV);
        System.out.printf("Chi2: %.6f%n", result.chi2);

        assertEquals(xV, vertex.getEntry(0), 1e-3);
        assertEquals(yV, vertex.getEntry(1), 1e-3);
        assertEquals(zV, vertex.getEntry(2), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + result.chi2,
                result.chi2 < 1e-2);
    }

    /**
     * Same exact-geometry check, but with an explicit non-origin initial vertex guess, to
     * verify {@code fit} converges to the true point regardless of starting point.
     */
    public void testFitVertexExactGeometryWithInitialVertex() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;

        TrackParams eleTrack = makeTrackParamsThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        TrackParams posTrack = makeTrackParamsThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        GainMatrixVertexer vertexer = new GainMatrixVertexer(B_FIELD);
        RealVector initialVertex = MatrixUtils.createRealVector(new double[]{0.0, 0.0, 0.0});
        FitResult result = vertexer.fit(Arrays.asList(eleTrack, posTrack), initialVertex,
                null, null, null, null, null, null, 20, 1e-10);

        assertNotNull("fit result should not be null", result);

        RealVector vertex = result.vertex;
        assertEquals(xV, vertex.getEntry(0), 1e-3);
        assertEquals(yV, vertex.getEntry(1), 1e-3);
        assertEquals(zV, vertex.getEntry(2), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + result.chi2,
                result.chi2 < 1e-2);
    }

    /**
     * Build track parameters passing exactly through (xV,yV,zV) with the given momentum, by
     * inverting the same center/perigee formulas as
     * {@code TrackConstraintVertexFitterTest#createExactTrackThroughPoint}.
     */
    private static TrackParams makeTrackParamsThroughPoint(double xV, double yV, double zV,
            double px, double py, double pz, int charge) {
        double pT = FastMath.sqrt(px * px + py * py);
        double omega = charge * C * B_FIELD / pT;
        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);
        double phiV = FastMath.atan2(py, px);
        double tanLambda = pz / pT;

        double xc = xV + R * sign * FastMath.sin(phiV);
        double yc = yV - R * sign * FastMath.cos(phiV);

        double A = FastMath.sqrt(xc * xc + yc * yc);
        double phi0 = FastMath.atan2(sign * xc, -sign * yc);
        double d0 = sign * (R - A);

        double dphi = phiV - phi0;
        while (dphi > FastMath.PI) dphi -= 2.0 * FastMath.PI;
        while (dphi < -FastMath.PI) dphi += 2.0 * FastMath.PI;
        double s = -sign * R * dphi;
        double z0 = zV - s * tanLambda;

        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        cov.setEntry(0, 0, 1e-4);
        cov.setEntry(1, 1, 1e-5);
        cov.setEntry(2, 2, 1e-8);
        cov.setEntry(3, 3, 1e-4);
        cov.setEntry(4, 4, 1e-5);

        return new TrackParams(d0, phi0, omega, z0, tanLambda, cov);
    }
}
