package org.hps.recon.vertexing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseReconstructedParticle;

import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.LineParams;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.LinePlaneProjection;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TwoVertexFitResult;

/**
 * Fits a hierarchical two-vertex cascade: V1, the e-/e+ decay vertex, and V2, the
 * production vertex where the neutral V0 (e-+e+) meets the recoil electron, linked by a
 * straight-line-flight collinearity constraint. Unlike a common-vertex fit of all three
 * tracks, this lets V1, V2, and all three track momenta move jointly within their
 * covariances. Wraps {@link KalmanVertexFitterGainMatrix#fitCascadeVertexJoint} and
 * packages the result as a 2-daughter cascade ReconstructedParticle (inner V0 particle at
 * V1, recoil electron) nested exactly like {@link CascadeVertexer}'s output, so existing
 * consumers of that output shape (e.g. {@code CascadeVertexTupleDriver}) work unchanged.
 */
public class ThreeTrackVertexer {

    private static final double ELECTRON_MASS = 0.000511;

    private final double bField;

    // When true, V2's tracking-index-0 (beam-direction/target-z) coordinate is held fixed
    // at the target position instead of fit freely -- see fit() below. Default false keeps
    // today's fully-free-V2 behavior unchanged.
    private boolean fixV2BeamCoordinate = false;

    public ThreeTrackVertexer(double bField) {
        this.bField = bField;
    }

    public void setFixV2BeamCoordinate(boolean fixV2BeamCoordinate) {
        this.fixV2BeamCoordinate = fixV2BeamCoordinate;
    }

    /**
     * Fit the joint two-vertex cascade for the V0's two daughters and a recoil electron.
     *
     * @param v0Particle     an already-fitted V0 whose daughters are the e-/e+ tracks and
     *                        whose {@code getStartVertex()} is a {@link BilliorVertex}
     * @param recoilElectron a final-state electron not already a daughter of v0Particle
     * @param truthMatched   whether this (v0Particle, recoilElectron) combination is the
     *                        MC-truth-matched signal pairing; gates the BADFIT_DEBUG print
     *                        below so it isn't flooded by wrong-pairing combinatorics that
     *                        are expected to fit badly. Callers without truth info should
     *                        pass {@code true} to preserve the original always-print behavior.
     * @return the cascade ReconstructedParticle (V0 particle + recoil electron), or null
     *         if the fit fails
     */
    public ReconstructedParticle fit(ReconstructedParticle v0Particle, ReconstructedParticle recoilElectron, boolean truthMatched) {
        List<ReconstructedParticle> v0Daughters = v0Particle.getParticles();
        ReconstructedParticle eleDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(0) : v0Daughters.get(1);
        ReconstructedParticle posDaughter = v0Daughters.get(0).getCharge() < 0 ? v0Daughters.get(1) : v0Daughters.get(0);

        TrackParams eleParams = trackParamsFromTrack(eleDaughter.getTracks().get(0));
        TrackParams posParams = trackParamsFromTrack(posDaughter.getTracks().get(0));
        TrackParams recoilParams = trackParamsFromTrack(recoilElectron.getTracks().get(0));

        // V1's initial guess and prior covariance come directly from the V0's own already-
        // fitted vertex (lineFromV0 does the detector -> tracking frame transform and
        // assembles the full 6x6 position-momentum joint covariance, same utility already
        // used by CascadeVertexer). V2's initial guess and prior covariance come from
        // propagating that same line to the target plane, so a correctly-reconstructed
        // candidate starts the fit right next to the true answer instead of needing the
        // Newton iteration to discover the V1/V2 branch from a flat/beamspot-only prior.
        BilliorVertex v0Vertex = (BilliorVertex) v0Particle.getStartVertex();
        LineParams v0Line = KalmanVertexFitterGainMatrix.lineFromV0(v0Vertex);

        // eleParams/posParams/recoilParams above are raw perigee params, i.e. relative to
        // their tracks' own reference point (assumed shared across all three, true for
        // AtPerigee states from the tracking reconstruction), whereas v0Line (built from an
        // already-fit, absolute-frame BilliorVertex) is not -- shift v0Line into that same
        // local frame before deriving any fit inputs from it, then shift the fitted V1/V2
        // back to the absolute frame below. The original v0Line is kept, unshifted, for the
        // diagnostic target-plane projection stored in the output (v0Proj below).
        double[] refPoint = TrackStateUtils.getTrackStatesAtLocation(
                eleDaughter.getTracks().get(0), TrackState.AtPerigee).get(0).getReferencePoint();
        LineParams v0LineLocal = shiftLine(v0Line, refPoint, -1.0);

        RealVector v1Init = MatrixUtils.createRealVector(new double[]{v0LineLocal.x0, v0LineLocal.y0, v0LineLocal.z0});
        RealMatrix v1Cov = v0LineLocal.cov.getSubMatrix(0, 2, 0, 2);

        // sigmaXFloor is deliberately weak (matches the flat variance=100 -> sigma=10 prior
        // used for V1/theta elsewhere in this class), NOT fitter.getBeamSize()[0] (a
        // disconnected, hardcoded 1-micron default never intended for this purpose): the
        // along-beam coordinate here is fixed to exactly xPlane by construction (see
        // propagateLineToPlane), and priorC is rebuilt from this covariance every outer
        // iteration, so a tight floor would permanently pin V2 to the target plane regardless
        // of what the recoil track and geometric constraint say -- only a weak floor lets
        // those actually determine where along the beam direction V2 ends up.
        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        LinePlaneProjection v0Proj = KalmanVertexFitterGainMatrix.propagateLineToPlane(
                v0Line, fitter.getBeamPosition()[0], 10.0);
        LinePlaneProjection v2Proj = KalmanVertexFitterGainMatrix.propagateLineToPlane(
                v0LineLocal, fitter.getBeamPosition()[0] - refPoint[0], 10.0);

        // Diagnostic-only (not used by the fit itself): where the recoil track's own helix,
        // independent of the V0, crosses the same target plane -- lets the tuple compare the
        // fitted V2 against each daughter's unconstrained target-plane projection. recoilParams
        // is local-frame, so the plane must be evaluated at its position in that same local
        // frame, then the result shifted back to absolute.
        LinePlaneProjection recoilProjLocal = KalmanVertexFitterGainMatrix.propagateTrackToPlane(
                recoilParams, fitter.getBeamPosition()[0] - refPoint[0], 10.0);
        LinePlaneProjection recoilProj = new LinePlaneProjection(
                recoilProjLocal.position.add(MatrixUtils.createRealVector(refPoint)), recoilProjLocal.cov);

        RealVector pV0Init = fitter.computeMomentumAtVertex(eleParams, v1Init)
                .add(fitter.computeMomentumAtVertex(posParams, v1Init));
        double[] roots = KalmanVertexFitterGainMatrix.transverseCircleRoots(v1Init, pV0Init, recoilParams);

        // v2FixedX is only used when fixV2BeamCoordinate is set; it's exactly the same
        // target-plane value v2Proj was itself propagated to (in the same local frame as
        // v1Init/v2Proj.position, since it's compared directly against the tracks' own raw
        // params inside the fitter), so the fixed coordinate is self-consistent with the
        // free-V2 fit's own target-plane prior.
        double v2FixedX = fitter.getBeamPosition()[0] - refPoint[0];

        TwoVertexFitResult result;
        if (roots == null) {
            result = fixV2BeamCoordinate
                    ? fitter.fitCascadeVertexJointFixedV2X(
                            eleParams, posParams, recoilParams, v1Init, v2Proj.position, v2FixedX, v1Cov, v2Proj.cov)
                    : fitter.fitCascadeVertexJoint(
                            eleParams, posParams, recoilParams, v1Init, v2Proj.position, v1Cov, v2Proj.cov);
        } else {
            // The recoil track's transverse (bending-plane) circle generically crosses the
            // V0's flight line at two points -- a genuine branch ambiguity. Neither chi2 nor
            // fitCascadeVertexJoint's own selectPhysicalThetaSeed (a vertical/dip-consistency
            // check) can tell the branches apart: both are computed entirely from this same
            // line/circle system, so both inherit its bias toward whichever branch has the
            // shorter theta -- a shorter arc length along the recoil helix structurally
            // yields a smaller residual in any metric evaluated on this system, regardless
            // of which branch is geometrically correct. Measured on real signal MC (task #27,
            // 1795 truth-matched double-root candidates): chi2 picks the truth-closer branch
            // only 26.5% of the time, and selectPhysicalThetaSeed only 34.7% -- both worse
            // than a coin flip. Break the tie with a discriminator that's genuinely external
            // to the ambiguity instead: fit both branches to convergence and keep whichever
            // one's V1 lands closest to the original two-track V0 vertex fit's own z. That
            // fit only ever involves the e-/e+ pair, never the recoil track, so it can't
            // inherit this bias -- measured to pick the truth-closer branch 99.1% of the time.
            // v0Vertex.getPosition() is absolute-frame; branchResult.v1 (below) is still
            // local-frame at this point (shifted back to absolute only after the branch is
            // picked), so shift the comparison target into that same local frame instead.
            double v0InputDetZLocal = v0Vertex.getPosition().z() - refPoint[0];
            TwoVertexFitResult best = null;
            double bestZDiff = Double.POSITIVE_INFINITY;
            for (double theta : roots) {
                RealVector v2Forced = v1Init.subtract(pV0Init.mapMultiply(theta));
                TwoVertexFitResult branchResult = fixV2BeamCoordinate
                        ? fitter.fitCascadeVertexJointFixedV2XForcedBranch(eleParams, posParams, recoilParams,
                                v1Init, theta, v2Forced, v2FixedX, v1Cov, v2Proj.cov, 60, 1.0e-8)
                        : fitter.fitCascadeVertexJointForcedBranch(
                                eleParams, posParams, recoilParams, v1Init, theta, v2Forced, v1Cov, v2Proj.cov, 60, 1.0e-8);
                if (branchResult == null) {
                    continue;
                }
                double v1DetZ = branchResult.v1.getEntry(0);
                double zDiff = FastMath.abs(v1DetZ - v0InputDetZLocal);
                if (zDiff < bestZDiff) {
                    bestZDiff = zDiff;
                    best = branchResult;
                }
            }
            result = best;
        }
        if (result == null) {
            return null;
        }
        RealVector refPointVec = MatrixUtils.createRealVector(refPoint);
        result.v1 = result.v1.add(refPointVec);
        result.v2 = result.v2.add(refPointVec);
        if (truthMatched && result.chi2 / result.ndf > 1000) {
            System.out.println("BADFIT_DEBUG bField=" + bField);
            System.out.println("BADFIT_DEBUG v1Init=" + v1Init);
            System.out.println("BADFIT_DEBUG v1Cov=" + v1Cov);
            System.out.println("BADFIT_DEBUG v2Init=" + v2Proj.position);
            System.out.println("BADFIT_DEBUG v2Cov=" + v2Proj.cov);
            System.out.println("BADFIT_DEBUG eleParams=" + dumpTrackParams(eleParams));
            System.out.println("BADFIT_DEBUG posParams=" + dumpTrackParams(posParams));
            System.out.println("BADFIT_DEBUG recoilParams=" + dumpTrackParams(recoilParams));
            System.out.println("BADFIT_DEBUG chi2=" + result.chi2 + " ndf=" + result.ndf);
            System.out.println("BADFIT_DEBUG v1=" + result.v1 + " v2=" + result.v2);
        }

        return makeReconstructedParticle(result, eleDaughter, posDaughter, recoilElectron, v0Proj, recoilProj, v0Vertex);
    }

    /**
     * Shift a line's point (x0,y0,z0) by {@code sign * refPoint} (direction unchanged; a pure
     * translation does not affect the line's covariance).
     */
    private static LineParams shiftLine(LineParams line, double[] refPoint, double sign) {
        return new LineParams(
                line.x0 + sign * refPoint[0], line.y0 + sign * refPoint[1], line.z0 + sign * refPoint[2],
                line.dx, line.dy, line.dz, line.cov);
    }

    private static String dumpTrackParams(TrackParams tp) {
        double[] a = tp.toArray();
        StringBuilder sb = new StringBuilder();
        sb.append("d0=").append(a[0]).append(" phi0=").append(a[1]).append(" omega=").append(a[2])
                .append(" z0=").append(a[3]).append(" tanLambda=").append(a[4]).append(" cov=[");
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                sb.append(tp.cov.getEntry(i, j));
                if (j < 4) {
                    sb.append(",");
                }
            }
            if (i < 4) {
                sb.append(";");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) from a Track's
     * AtPerigee TrackState. Duplicated from {@link CascadeVertexer}'s private helper of
     * the same name rather than shared, matching that class's own convention.
     */
    private static TrackParams trackParamsFromTrack(Track track) {
        TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
        double[] par = ts.getParameters();
        SymmetricMatrix sm = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setEntry(i, j, sm.e(i, j));
            }
        }
        return new TrackParams(par[0], par[1], par[2], par[3], par[4], cov);
    }

    private static ReconstructedParticle makeReconstructedParticle(TwoVertexFitResult result,
            ReconstructedParticle eleDaughter, ReconstructedParticle posDaughter, ReconstructedParticle recoilElectron,
            LinePlaneProjection v0Proj, LinePlaneProjection recoilProj, BilliorVertex v0InputVertex) {

        // Tracking frame -> detector frame: det(x,y,z) = trk(y,z,x), same convention used
        // throughout KalmanVertexFitterGainMatrix.fitVertex(...) and CascadeVertexer.
        Hep3Vector v1PosDet = new BasicHep3Vector(
                result.v1.getEntry(1), result.v1.getEntry(2), result.v1.getEntry(0));
        Hep3Vector v2PosDet = new BasicHep3Vector(
                result.v2.getEntry(1), result.v2.getEntry(2), result.v2.getEntry(0));

        SymmetricMatrix v1CovDet = packCov(result.v1Cov);
        SymmetricMatrix v2CovDet = packCov(result.v2Cov);

        Hep3Vector v1PosErr = new BasicHep3Vector(
                FastMath.sqrt(FastMath.abs(result.v1Cov.getEntry(1, 1))),
                FastMath.sqrt(FastMath.abs(result.v1Cov.getEntry(2, 2))),
                FastMath.sqrt(FastMath.abs(result.v1Cov.getEntry(0, 0))));
        Hep3Vector v2PosErr = new BasicHep3Vector(
                FastMath.sqrt(FastMath.abs(result.v2Cov.getEntry(1, 1))),
                FastMath.sqrt(FastMath.abs(result.v2Cov.getEntry(2, 2))),
                FastMath.sqrt(FastMath.abs(result.v2Cov.getEntry(0, 0))));

        Hep3Vector pEle = toDetFrame(result.eMinusMomentum.p);
        Hep3Vector pPos = toDetFrame(result.ePlusMomentum.p);
        Hep3Vector pRecoil = toDetFrame(result.recoilMomentum.p);
        Hep3Vector pV0 = VecOp.add(pEle, pPos);

        // Inner V0 particle at V1: e- and e+ daughters, mirroring HpsReconParticleDriver's
        // V0-particle construction style.
        double eEleMag = pEle.magnitude();
        double ePosMag = pPos.magnitude();
        double eEle = FastMath.sqrt(eEleMag * eEleMag + ELECTRON_MASS * ELECTRON_MASS);
        double ePos = FastMath.sqrt(ePosMag * ePosMag + ELECTRON_MASS * ELECTRON_MASS);
        double eV0 = eEle + ePos;
        double v0PMag = pV0.magnitude();
        double v0MassSq = eV0 * eV0 - v0PMag * v0PMag;
        double v0Mass = v0MassSq > 0 ? FastMath.sqrt(v0MassSq) : -99.0;

        Map<Integer, Hep3Vector> v0FitMap = new HashMap<Integer, Hep3Vector>();
        v0FitMap.put(0, pEle);
        v0FitMap.put(1, pPos);

        // The fit is fully coupled (V1, V2, and all three track momenta adjust jointly),
        // so there is no clean way to partition chi2/ndf between the two vertices --
        // report the same joint values on both, as CascadeVertexTupleDriver only reads
        // v0Vtx.getInvMass()/getChi2() (not its ndf) from the inner vertex.
        BilliorVertex v1Vtx = new BilliorVertex(v1PosDet, v1CovDet, result.chi2, v0Mass, v0FitMap, "TWOVERTEX");
        v1Vtx.setPositionError(v1PosErr);
        v1Vtx.setProbability(result.ndf);
        v1Vtx.setParameter("ndf", (double) result.ndf);

        ReconstructedParticle v0Particle = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) v0Particle).setStartVertex(v1Vtx);
        v0Particle.addParticle(eleDaughter);
        v0Particle.addParticle(posDaughter);
        ((BaseReconstructedParticle) v0Particle).setType(eleDaughter.getType());
        ((BaseReconstructedParticle) v0Particle).setMass(v0Mass);
        HepLorentzVector v0FourVector = new BasicHepLorentzVector(eV0, pV0);
        ((BaseReconstructedParticle) v0Particle).set4Vector(v0FourVector);
        double v0Charge = eleDaughter.getCharge() + posDaughter.getCharge();
        ((BaseReconstructedParticle) v0Particle).setCharge(v0Charge);
        v1Vtx.setAssociatedParticle(v0Particle);
        ((BaseReconstructedParticle) v0Particle).setReferencePoint(v1Vtx.getPosition());

        // Outer cascade particle at V2: V0 particle + recoil electron, identical nesting
        // to CascadeVertexer's output.
        double recoilPMag = pRecoil.magnitude();
        double eRecoil = FastMath.sqrt(recoilPMag * recoilPMag + ELECTRON_MASS * ELECTRON_MASS);
        double totalE = eV0 + eRecoil;
        Hep3Vector totalP = VecOp.add(pV0, pRecoil);
        double totalPMag = totalP.magnitude();
        double massSq = totalE * totalE - totalPMag * totalPMag;
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        Map<Integer, Hep3Vector> cascadeFitMap = new HashMap<Integer, Hep3Vector>();
        cascadeFitMap.put(0, pV0);
        cascadeFitMap.put(1, pRecoil);

        BilliorVertex v2Vtx = new BilliorVertex(v2PosDet, v2CovDet, result.chi2, invMass, cascadeFitMap, "TWOVERTEX");
        v2Vtx.setPositionError(v2PosErr);
        v2Vtx.setProbability(result.ndf);
        v2Vtx.setParameter("ndf", (double) result.ndf);

        // Diagnostic target-plane projections (tracking-frame y,z -> detector-frame x,y, same
        // det(x,y,z)=trk(y,z,x) convention as everywhere else in this method), independent of
        // the fit result itself: where the V0's own flight line and the recoil track's own
        // helix separately cross the target plane, for comparing against the fitted V2.
        v2Vtx.setParameter("v0ProjX", v0Proj.position.getEntry(1));
        v2Vtx.setParameter("v0ProjY", v0Proj.position.getEntry(2));
        v2Vtx.setParameter("v0ProjXErr", FastMath.sqrt(FastMath.abs(v0Proj.cov.getEntry(1, 1))));
        v2Vtx.setParameter("v0ProjYErr", FastMath.sqrt(FastMath.abs(v0Proj.cov.getEntry(2, 2))));
        v2Vtx.setParameter("recoilProjX", recoilProj.position.getEntry(1));
        v2Vtx.setParameter("recoilProjY", recoilProj.position.getEntry(2));
        v2Vtx.setParameter("recoilProjXErr", FastMath.sqrt(FastMath.abs(recoilProj.cov.getEntry(1, 1))));
        v2Vtx.setParameter("recoilProjYErr", FastMath.sqrt(FastMath.abs(recoilProj.cov.getEntry(2, 2))));

        // The original two-body Billior V0 fit (position, chi2, mass), from before the joint
        // V1/V2 fit above replaced it -- kept here since v0Particle's own startVertex is now
        // the joint-fit V1, not this original fit, so it would otherwise be lost.
        Hep3Vector v0InputPos = v0InputVertex.getPosition();
        Hep3Vector v0InputPosErr = v0InputVertex.getPositionError();
        v2Vtx.setParameter("v0InputVtxX", v0InputPos.x());
        v2Vtx.setParameter("v0InputVtxY", v0InputPos.y());
        v2Vtx.setParameter("v0InputVtxZ", v0InputPos.z());
        v2Vtx.setParameter("v0InputVtxXErr", v0InputPosErr.x());
        v2Vtx.setParameter("v0InputVtxYErr", v0InputPosErr.y());
        v2Vtx.setParameter("v0InputVtxZErr", v0InputPosErr.z());
        v2Vtx.setParameter("v0InputChi2", v0InputVertex.getChi2());
        v2Vtx.setParameter("v0InputMass", v0InputVertex.getInvMass());

        ReconstructedParticle cascade = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) cascade).setStartVertex(v2Vtx);
        cascade.addParticle(v0Particle);
        cascade.addParticle(recoilElectron);
        ((BaseReconstructedParticle) cascade).setType(recoilElectron.getType());
        ((BaseReconstructedParticle) cascade).setMass(invMass);
        HepLorentzVector fourVector = new BasicHepLorentzVector(totalE, totalP);
        ((BaseReconstructedParticle) cascade).set4Vector(fourVector);
        double particleCharge = v0Charge + recoilElectron.getCharge();
        ((BaseReconstructedParticle) cascade).setCharge(particleCharge);
        v2Vtx.setAssociatedParticle(cascade);
        ((BaseReconstructedParticle) cascade).setReferencePoint(v2Vtx.getPosition());

        return cascade;
    }

    private static Hep3Vector toDetFrame(RealVector pTrk) {
        return new BasicHep3Vector(pTrk.getEntry(1), pTrk.getEntry(2), pTrk.getEntry(0));
    }

    private static SymmetricMatrix packCov(RealMatrix covTrk) {
        double[] covPacked = new double[6];
        covPacked[0] = covTrk.getEntry(1, 1);
        covPacked[1] = covTrk.getEntry(2, 1);
        covPacked[2] = covTrk.getEntry(2, 2);
        covPacked[3] = covTrk.getEntry(0, 1);
        covPacked[4] = covTrk.getEntry(0, 2);
        covPacked[5] = covTrk.getEntry(0, 0);
        return new SymmetricMatrix(3, covPacked, true);
    }
}
