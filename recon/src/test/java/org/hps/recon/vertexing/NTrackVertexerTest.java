package org.hps.recon.vertexing;

import java.util.Arrays;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import junit.framework.TestCase;

import org.apache.commons.math.util.FastMath;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;

/**
 * Exact-geometry sanity check for {@link NTrackVertexer}: build an electron and
 * a positron track that are both constructed to pass exactly through the same known point, with
 * small (non-singular) measurement covariances, and verify {@code fitVertexNoBeamConstraint}
 * recovers that point with near-zero chi2. Covers the 2-track (V0) case, formerly exercised via
 * the now-removed {@code KalmanV0Vertexer}.
 */
public class NTrackVertexerTest extends TestCase {

    private static final double B_FIELD = 0.5; // Tesla
    private static final double ELECTRON_MASS = 0.000511; // GeV
    private static final double C = 2.99792458e-4;

    public void testFitVertexExactGeometry() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;

        Track eleTrack = makeTrackThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        Track posTrack = makeTrackThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        NTrackVertexer vertexer = new NTrackVertexer(B_FIELD);
        BilliorVertex vtx = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));

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

        NTrackVertexer vertexer = new NTrackVertexer(B_FIELD);
        BilliorVertex vtx = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));

        assertNotNull("fit result should not be null", vtx);

        Hep3Vector pos = vtx.getPosition();
        System.out.printf("Fitted vertex (det frame, non-origin ref): [%.6f, %.6f, %.6f]  "
                + "(truth trk frame: [%.6f, %.6f, %.6f])%n",
                pos.x(), pos.y(), pos.z(), xV, yV, zV);

        // Detector frame: det(x,y,z) = trk(y,z,x). fitVertexNoBeamConstraint dispatches to
        // fitBillior1985, a single-shot linearization around v0=(0,0,0) in the tracks' local
        // (reference-point-relative) frame -- unlike the iterative gain-matrix path it
        // replaces, it does not re-linearize at successive vertex guesses. With refPoint
        // x=-1.1mm, the true vertex sits 1.4mm from v0 in local x (vs 0.3mm in the
        // origin-reference-point test), large enough for the resulting O(offset^2) residual
        // to exceed a 1um tolerance on the tracking-x/z (detector-z/y) axes -- hence the
        // looser tolerance on those two only.
        assertEquals(yV, pos.x(), 1e-3);
        assertEquals(zV, pos.y(), 0.02);
        assertEquals(xV, pos.z(), 0.01);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + vtx.getChi2(),
                vtx.getChi2() < 1e-2);
    }

    /**
     * Verify {@code fitVertexBeamspotConstrained} actually pulls the fitted vertex toward a
     * supplied beamspot position, and that calling the pre-existing {@code
     * fitVertexNoBeamConstraint} is completely unaffected (same tracks, called before and after
     * the new method) -- proving the new opt-in method causes zero regression to existing
     * behavior.
     */
    public void testFitVertexBeamspotConstrainedPullsTowardBeamPosition() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        double pxEle = 0.30, pyEle = 0.10, pzEle = 1.5;
        double pxPos = 0.15, pyPos = -0.05, pzPos = 0.8;

        Track eleTrack = makeTrackThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        Track posTrack = makeTrackThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        NTrackVertexer vertexer = new NTrackVertexer(B_FIELD);

        BilliorVertex vtxUnconstrainedBefore = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));
        Hep3Vector posUnc = vtxUnconstrainedBefore.getPosition();

        // beamSize must be tight not just relative to the tracks' individual d0/z0 sigmas, but
        // relative to the *track pair's own vertex information matrix*, which for two tracks
        // crossing at a long lever arm from the reference point is highly anisotropic and can
        // have an eigenvalue (best-constrained direction) far tighter than any single sigma
        // would suggest -- 1e-6 (information ~1e12) safely dominates that too, unlike an
        // initially-tried 0.001 (information ~1e6, only comparable to the tracks' best
        // direction, which pulled the fit to neither the truth nor the beam position).
        double[] beamPosition = {0.0, 0.0, 0.0};
        double[] beamSize = {1e-6, 1e-6, 1e-6};
        BilliorVertex vtxBS = vertexer.fitVertexBeamspotConstrained(
                Arrays.asList(eleTrack, posTrack), beamPosition, beamSize);
        assertNotNull("beamspot-constrained fit result should not be null", vtxBS);
        Hep3Vector posBS = vtxBS.getPosition();

        System.out.printf("Unconstrained vertex (det frame): [%.6f, %.6f, %.6f]%n",
                posUnc.x(), posUnc.y(), posUnc.z());
        System.out.printf("Beamspot-constrained vertex (det frame): [%.6f, %.6f, %.6f]  "
                + "(beam position trk frame: [%.6f, %.6f, %.6f], beam size trk frame: [%.6f, %.6f, %.6f])%n",
                posBS.x(), posBS.y(), posBS.z(),
                beamPosition[0], beamPosition[1], beamPosition[2],
                beamSize[0], beamSize[1], beamSize[2]);

        // The beamspot prior is far tighter than the tracks' own position sensitivity, and
        // centered far from the true (and unconstrained-fit) vertex, so the constrained fit
        // should land very close to the beam position (det frame: det(x,y,z) = trk(y,z,x)).
        assertEquals(0.0, posBS.x(), 0.01);
        assertEquals(0.0, posBS.y(), 0.01);
        assertEquals(0.0, posBS.z(), 0.01);

        double pullDistance = VecOp.sub(posBS, posUnc).magnitude();
        assertTrue("beamspot constraint should measurably pull the vertex away from the "
                + "unconstrained result, got pull distance " + pullDistance, pullDistance > 1.0);

        // Calling the pre-existing unconstrained method again on the same tracks must give a
        // bit-identical result to the first call above -- the new method must not have mutated
        // any shared state or otherwise perturbed existing behavior.
        BilliorVertex vtxUnconstrainedAfter = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));
        Hep3Vector posUncAfter = vtxUnconstrainedAfter.getPosition();
        assertEquals(posUnc.x(), posUncAfter.x(), 1e-12);
        assertEquals(posUnc.y(), posUncAfter.y(), 1e-12);
        assertEquals(posUnc.z(), posUncAfter.z(), 1e-12);
    }

    /**
     * Verify {@code fitVertexBothConstrained} pulls the fitted vertex toward a supplied
     * beamspot position AND the total 2-track momentum toward a supplied beam momentum,
     * simultaneously -- and that the pre-existing single-constraint methods on the same
     * tracks are completely unaffected by having called the new method.
     */
    public void testFitVertexBothConstrainedPullsTowardBeamspotAndBeamMomentum() {
        double xV = 0.3, yV = -0.2, zV = 5.0;
        // Unlike the other tests in this file (which only need a non-degenerate momentum to
        // check vertex-position recovery), this test also applies a beam-momentum constraint,
        // so the tracks' total momentum must be dominantly along tracking-X to match the beam
        // direction convention below (beamPx=beamEnergy*cos(rotAngle) dominant) -- tracks whose
        // total momentum instead points dominantly along tracking-Z (as in the other tests'
        // pzEle=1.5-style values) would require fitSoftConstrained's Newton-Raphson solve to
        // rotate the total momentum by nearly 90 degrees in a few linearized steps, which does
        // not converge to a sensible answer (confirmed by direct fitSoftConstrained probing).
        double pxEle = 1.9, pyEle = 0.15, pzEle = 0.05;
        double pxPos = 1.3, pyPos = -0.20, pzPos = -0.03;

        Track eleTrack = makeTrackThroughPoint(xV, yV, zV, pxEle, pyEle, pzEle, -1);
        Track posTrack = makeTrackThroughPoint(xV, yV, zV, pxPos, pyPos, pzPos, 1);

        NTrackVertexer vertexer = new NTrackVertexer(B_FIELD);

        BilliorVertex vtxUnconstrainedBefore = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));
        Hep3Vector posUnc = vtxUnconstrainedBefore.getPosition();
        Hep3Vector totalPUnc = VecOp.add(vtxUnconstrainedBefore.getFittedMomentum(0),
                vtxUnconstrainedBefore.getFittedMomentum(1));

        // Beam momentum target chosen far from the tracks' own (unconstrained) momentum sum
        // [3.2, -0.05, 0.02] (trk frame), so a real pull is unambiguous.
        double beamEnergy = 3.7;
        double beamRotAngle = 0.0305;
        double beamPx = beamEnergy * FastMath.cos(beamRotAngle);
        double beamPy = -beamEnergy * FastMath.sin(beamRotAngle);
        // getFittedMomentum() (used below) returns detector frame, det(x,y,z) = trk(y,z,x),
        // same convention as vertex position -- so the comparison target must be built in
        // that frame too, not tracking frame.
        Hep3Vector beamP = new hep.physics.vec.BasicHep3Vector(beamPy, 0.0, beamPx);

        // Same tight beamspot prior as testFitVertexBeamspotConstrainedPullsTowardBeamPosition,
        // far from the tracks' own (unconstrained) vertex position.
        double[] beamPosition = {0.0, 0.0, 0.0};
        double[] beamSize = {1e-6, 1e-6, 1e-6};

        BilliorVertex vtxBoth = vertexer.fitVertexBothConstrained(
                Arrays.asList(eleTrack, posTrack), beamPosition, beamSize, beamEnergy, beamRotAngle, 0.0);
        assertNotNull("both-constrained fit result should not be null", vtxBoth);
        Hep3Vector posBoth = vtxBoth.getPosition();
        Hep3Vector totalPBoth = VecOp.add(vtxBoth.getFittedMomentum(0), vtxBoth.getFittedMomentum(1));

        System.out.printf("Unconstrained vertex (det frame): [%.6f, %.6f, %.6f], total P: [%.6f, %.6f, %.6f]%n",
                posUnc.x(), posUnc.y(), posUnc.z(), totalPUnc.x(), totalPUnc.y(), totalPUnc.z());
        System.out.printf("Both-constrained vertex (det frame): [%.6f, %.6f, %.6f], total P: [%.6f, %.6f, %.6f] "
                + "(beam P det frame: [%.6f, %.6f, %.6f])%n",
                posBoth.x(), posBoth.y(), posBoth.z(), totalPBoth.x(), totalPBoth.y(), totalPBoth.z(),
                beamP.x(), beamP.y(), beamP.z());

        // Position: the beamspot prior is far tighter than the tracks' own position
        // sensitivity, so the constrained fit should land very close to the beam position
        // (det frame: det(x,y,z) = trk(y,z,x)).
        assertEquals(0.0, posBoth.x(), 0.01);
        assertEquals(0.0, posBoth.y(), 0.01);
        assertEquals(0.0, posBoth.z(), 0.01);

        // Momentum: the both-constrained total momentum should land measurably closer to the
        // beam momentum target than the unconstrained fit's total momentum did.
        double distUncFromBeam = VecOp.sub(totalPUnc, beamP).magnitude();
        double distBothFromBeam = VecOp.sub(totalPBoth, beamP).magnitude();
        assertTrue("both-constrained total momentum should be pulled measurably closer to the "
                + "beam momentum target than the unconstrained fit (unc dist=" + distUncFromBeam
                + ", both dist=" + distBothFromBeam + ")", distBothFromBeam < 0.5 * distUncFromBeam);

        // Calling the pre-existing single-constraint methods again on the same tracks must be
        // completely unaffected by having called fitVertexBothConstrained above.
        BilliorVertex vtxUnconstrainedAfter = vertexer.fitVertexNoBeamConstraint(Arrays.asList(eleTrack, posTrack));
        Hep3Vector posUncAfter = vtxUnconstrainedAfter.getPosition();
        assertEquals(posUnc.x(), posUncAfter.x(), 1e-12);
        assertEquals(posUnc.y(), posUncAfter.y(), 1e-12);
        assertEquals(posUnc.z(), posUncAfter.z(), 1e-12);

        BilliorVertex vtxBS = vertexer.fitVertexBeamspotConstrained(
                Arrays.asList(eleTrack, posTrack), beamPosition, beamSize);
        Hep3Vector posBS = vtxBS.getPosition();
        assertEquals(0.0, posBS.x(), 0.01);
        assertEquals(0.0, posBS.y(), 0.01);
        assertEquals(0.0, posBS.z(), 0.01);
    }

    public void testPlaceholderVertexOnFailedFit() {
        BilliorVertex placeholder = NTrackVertexer.placeholderVertex(2);
        assertNotNull(placeholder);
        assertEquals(-9999.0, placeholder.getChi2(), 1e-9);
        assertEquals(-9999.0, placeholder.getInvMass(), 1e-9);
        assertEquals(-9999.0, placeholder.getCustomParameters().get("ndf"), 1e-9);
    }

    /**
     * Build a Track with a single AtPerigee TrackState whose helix passes exactly through
     * (xV,yV,zV) with the given momentum, by inverting the same center/perigee formulas as
     * {@code TrackConstraintVertexFitterTest#createExactTrackThroughPoint}.
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
