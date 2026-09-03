package org.hps.recon.vertexing;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import junit.framework.TestCase;

import org.apache.commons.math.util.FastMath;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;

/**
 * Exact-geometry sanity check for {@link KalmanV0Vertexer}, mirroring
 * {@code KalmanVertexFitterGainMatrixTest#testCascadeVertexExactGeometry}: build an electron and
 * a positron track that are both constructed to pass exactly through the same known point, with
 * small (non-singular) measurement covariances, and verify {@code fitVertex} recovers that point
 * with near-zero chi2.
 */
public class KalmanV0VertexerTest extends TestCase {

    private static final double B_FIELD = 0.5; // Tesla
    private static final double ELECTRON_MASS = 0.000511; // GeV
    private static final double C = 2.99792458e-4;

    public void testFitVertexExactGeometry() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;

        Track eleTrack = makeTrackThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        Track posTrack = makeTrackThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        KalmanV0Vertexer vertexer = new KalmanV0Vertexer(B_FIELD);
        BilliorVertex vtx = vertexer.fitVertex(eleTrack, posTrack);

        assertNotNull("fit result should not be null", vtx);

        Hep3Vector pos = vtx.getPosition();
        System.out.printf("Fitted vertex (det frame): [%.6f, %.6f, %.6f]  (truth trk frame: [%.6f, %.6f, %.6f])%n",
                pos.x(), pos.y(), pos.z(), xV, yV, zV);
        System.out.printf("Chi2: %.6f%n", vtx.getChi2());

        // Detector frame: det(x,y,z) = trk(y,z,x)
        assertEquals(yV, pos.x(), 1e-3);
        assertEquals(zV, pos.y(), 1e-3);
        assertEquals(xV, pos.z(), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + vtx.getChi2(),
                vtx.getChi2() < 1e-2);

        double eEle = FastMath.sqrt(pxEle * pxEle + pyEle * pyEle + pzEle * pzEle
                + ELECTRON_MASS * ELECTRON_MASS);
        double ePos = FastMath.sqrt(pxPos * pxPos + pyPos * pyPos + pzPos * pzPos
                + ELECTRON_MASS * ELECTRON_MASS);
        double pxSum = pxEle + pxPos, pySum = pyEle + pyPos, pzSum = pzEle + pzPos;
        double massSqTruth = (eEle + ePos) * (eEle + ePos) - (pxSum * pxSum + pySum * pySum + pzSum * pzSum);
        double massTruth = massSqTruth > 0 ? FastMath.sqrt(massSqTruth) : -1.0;

        assertEquals(massTruth, vtx.getInvMass(), 1e-3);
    }

    /**
     * Same exact-geometry check as {@link #testFitVertexExactGeometry}, but with both tracks'
     * AtPerigee reference point set to the real target position (tracking-frame x = -1.1mm, as
     * used by production reconstruction) instead of the origin, to verify the fitted vertex is
     * correctly shifted back into the absolute tracking/detector frame rather than being left
     * relative to the tracks' reference point.
     */
    public void testFitVertexExactGeometryNonOriginReferencePoint() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;
        double[] refPoint = {-1.1, 0.0, 0.0};

        Track eleTrack = makeTrackThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1, refPoint);
        Track posTrack = makeTrackThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1, refPoint);

        KalmanV0Vertexer vertexer = new KalmanV0Vertexer(B_FIELD);
        BilliorVertex vtx = vertexer.fitVertex(eleTrack, posTrack);

        assertNotNull("fit result should not be null", vtx);

        Hep3Vector pos = vtx.getPosition();
        System.out.printf("Fitted vertex (det frame, non-origin ref): [%.6f, %.6f, %.6f]  "
                + "(truth trk frame: [%.6f, %.6f, %.6f])%n",
                pos.x(), pos.y(), pos.z(), xV, yV, zV);

        // Detector frame: det(x,y,z) = trk(y,z,x)
        assertEquals(yV, pos.x(), 1e-3);
        assertEquals(zV, pos.y(), 1e-3);
        assertEquals(xV, pos.z(), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + vtx.getChi2(),
                vtx.getChi2() < 1e-2);
    }

    public void testPlaceholderVertexOnFailedFit() {
        BilliorVertex placeholder = KalmanV0Vertexer.placeholderVertex();
        assertNotNull(placeholder);
        assertEquals(-9999.0, placeholder.getChi2(), 1e-9);
        assertEquals(-9999.0, placeholder.getInvMass(), 1e-9);
        assertEquals(-9999.0, placeholder.getCustomParameters().get("ndf"), 1e-9);
    }

    /**
     * Build a Track with a single AtPerigee TrackState whose helix passes exactly through
     * (xV,yV,zV) with the given momentum, by inverting the same center/perigee formulas as
     * {@code KalmanVertexFitterGainMatrixTest#createExactTrackThroughPoint}.
     */
    private static Track makeTrackThroughPoint(double xV, double yV, double zV,
            double px, double py, double pz, int charge) {
        return makeTrackThroughPoint(xV, yV, zV, px, py, pz, charge, new double[]{0.0, 0.0, 0.0});
    }

    /**
     * As above, but with the perigee params expressed relative to {@code refPoint} (tracking
     * frame) instead of the origin, and the TrackState's reference point set accordingly.
     */
    private static Track makeTrackThroughPoint(double xV, double yV, double zV,
            double px, double py, double pz, int charge, double[] refPoint) {
        double xVLocal = xV - refPoint[0];
        double yVLocal = yV - refPoint[1];
        double zVLocal = zV - refPoint[2];
        double pT = FastMath.sqrt(px * px + py * py);
        double omega = charge * C * B_FIELD / pT;
        double R = 1.0 / FastMath.abs(omega);
        double sign = FastMath.signum(omega);
        double phiV = FastMath.atan2(py, px);
        double tanLambda = pz / pT;

        double xc = xVLocal + R * sign * FastMath.sin(phiV);
        double yc = yVLocal - R * sign * FastMath.cos(phiV);

        double A = FastMath.sqrt(xc * xc + yc * yc);
        double phi0 = FastMath.atan2(sign * xc, -sign * yc);
        double d0 = sign * (R - A);

        double dphi = phiV - phi0;
        while (dphi > FastMath.PI) dphi -= 2.0 * FastMath.PI;
        while (dphi < -FastMath.PI) dphi += 2.0 * FastMath.PI;
        double s = -sign * R * dphi;
        double z0 = zVLocal - s * tanLambda;

        double[] params = new double[5];
        params[BaseTrack.D0] = d0;
        params[BaseTrack.PHI] = phi0;
        params[BaseTrack.OMEGA] = omega;
        params[BaseTrack.Z0] = z0;
        params[BaseTrack.TANLAMBDA] = tanLambda;

        SymmetricMatrix cov = new SymmetricMatrix(5);
        cov.setElement(0, 0, 1e-4);
        cov.setElement(1, 1, 1e-5);
        cov.setElement(2, 2, 1e-8);
        cov.setElement(3, 3, 1e-4);
        cov.setElement(4, 4, 1e-5);

        BaseTrackState ts = new BaseTrackState(params, refPoint,
                cov.asPackedArray(true), TrackState.AtPerigee, B_FIELD);

        BaseTrack track = new BaseTrack();
        track.getTrackStates().add(ts);
        return track;
    }
}
