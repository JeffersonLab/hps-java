package org.hps.recon.vertexing;

import java.util.HashMap;
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
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.FitResult;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.LineParams;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.LinePlaneProjection;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;

/**
 * Fits a "cascade" (production) vertex where an already-vertexed neutral V0's momentum
 * direction -- treated as a zero-curvature line, carrying its full 6x6 position-momentum
 * joint covariance -- meets a third, curving recoil-electron track. Wraps
 * {@link KalmanVertexFitterGainMatrix#fitCascadeVertex} and packages the result as a
 * 2-daughter ReconstructedParticle (V0, recoil electron) with the production Vertex
 * attached via setStartVertex(...), mirroring the conventions used by
 * {@code HpsReconParticleDriver#makeReconstructedParticle}.
 */
public class CascadeVertexer {

    private static final double ELECTRON_MASS = 0.000511;

    private final double bField;

    public CascadeVertexer(double bField) {
        this.bField = bField;
    }

    /**
     * Fit the production vertex where {@code v0Particle}'s momentum line meets
     * {@code recoilElectron}'s helix.
     *
     * @param v0Particle     an already-fitted V0 whose {@code getStartVertex()} is a
     *                        {@link BilliorVertex} (as produced by BilliorVertexer /
     *                        HpsReconParticleDriver)
     * @param recoilElectron a final-state electron not already a daughter of v0Particle
     * @return the cascade ReconstructedParticle, or null if the fit fails
     */
    public ReconstructedParticle fit(ReconstructedParticle v0Particle, ReconstructedParticle recoilElectron) {
        BilliorVertex v0Vertex = (BilliorVertex) v0Particle.getStartVertex();
        LineParams v0Line = KalmanVertexFitterGainMatrix.lineFromV0(v0Vertex);

        Track recoilTrack = recoilElectron.getTracks().get(0);
        TrackState recoilTs = TrackStateUtils.getTrackStatesAtLocation(recoilTrack, TrackState.AtPerigee).get(0);
        TrackParams recoilParams = trackParamsFromTrack(recoilTrack);

        // recoilParams' raw d0/z0 (and hence the vertex unknown inside fitCascadeVertex) are
        // relative to the recoil track's own reference point, whereas v0Line (built from an
        // already-fit, absolute-frame BilliorVertex) is not -- shift v0Line into that same local
        // frame before fitting, then shift the fitted vertex back to the absolute frame below.
        double[] refPoint = recoilTs.getReferencePoint();
        LineParams v0LineLocal = shiftLine(v0Line, refPoint, -1.0);

        KalmanVertexFitterGainMatrix fitter = new KalmanVertexFitterGainMatrix(bField);
        FitResult result = fitter.fitCascadeVertex(recoilParams, v0LineLocal);
        if (result == null) {
            return null;
        }
        result.vertex = result.vertex.add(MatrixUtils.createRealVector(refPoint));

        // Diagnostic-only target-plane projections (not used by the fit itself): where the V0's
        // own flight line and the recoil track's own helix separately cross the target plane,
        // for comparing against the fitted production vertex -- same convention as the analogous
        // diagnostics added to ThreeTrackVertexer. v0Line is already absolute-frame; recoilParams
        // is not, so its plane crossing must be evaluated at the plane's position in the recoil
        // track's local frame, then shifted back to absolute.
        LinePlaneProjection v0Proj = KalmanVertexFitterGainMatrix.propagateLineToPlane(
                v0Line, fitter.getBeamPosition()[0], 10.0);
        LinePlaneProjection recoilProjLocal = KalmanVertexFitterGainMatrix.propagateTrackToPlane(
                recoilParams, fitter.getBeamPosition()[0] - refPoint[0], 10.0);
        LinePlaneProjection recoilProj = new LinePlaneProjection(
                recoilProjLocal.position.add(MatrixUtils.createRealVector(refPoint)), recoilProjLocal.cov);

        return makeReconstructedParticle(v0Particle, recoilElectron, result, v0Line, v0Proj, recoilProj, v0Vertex);
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

    /**
     * Extract perigee TrackParams (d0, phi0, omega, z0, tanLambda) from a Track's
     * AtPerigee TrackState. Parameter order matches {@code TrackState.getParameters()}
     * exactly (same convention as {@code BilliorTrack(Track)}), so no reordering is needed.
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

    private static ReconstructedParticle makeReconstructedParticle(ReconstructedParticle v0Particle,
            ReconstructedParticle recoilElectron, FitResult result, LineParams v0Line,
            LinePlaneProjection v0Proj, LinePlaneProjection recoilProj, BilliorVertex v0InputVertex) {

        // Tracking frame -> detector frame: det(x,y,z) = trk(y,z,x), same convention used
        // throughout KalmanVertexFitterGainMatrix.fitVertex(...) and BilliorVertexer.
        Hep3Vector vtxPos = new BasicHep3Vector(
                result.vertex.getEntry(1),
                result.vertex.getEntry(2),
                result.vertex.getEntry(0));

        double[] covPacked = new double[6];
        covPacked[0] = result.vertexCov.getEntry(1, 1);
        covPacked[1] = result.vertexCov.getEntry(2, 1);
        covPacked[2] = result.vertexCov.getEntry(2, 2);
        covPacked[3] = result.vertexCov.getEntry(0, 1);
        covPacked[4] = result.vertexCov.getEntry(0, 2);
        covPacked[5] = result.vertexCov.getEntry(0, 0);
        SymmetricMatrix covVtx = new SymmetricMatrix(3, covPacked, true);

        Hep3Vector vtxPosErr = new BasicHep3Vector(
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(1, 1))),
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(2, 2))),
                FastMath.sqrt(FastMath.abs(result.vertexCov.getEntry(0, 0))));

        // Recoil electron's momentum evaluated at the fitted production vertex.
        RealVector pRecoilTrk = result.trackMomenta.get(0).p;
        Hep3Vector pRecoil = new BasicHep3Vector(
                pRecoilTrk.getEntry(1), pRecoilTrk.getEntry(2), pRecoilTrk.getEntry(0));

        // The V0's momentum is constant along its (zero-curvature) line, so it is
        // unchanged by the fit: it is exactly the direction vector carried by v0Line.
        Hep3Vector pV0 = new BasicHep3Vector(v0Line.dy, v0Line.dz, v0Line.dx);

        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        pFitMap.put(0, pV0);
        pFitMap.put(1, pRecoil);

        double v0Mass = v0Particle.getMass();
        double v0PMag = pV0.magnitude();
        double eV0 = FastMath.sqrt(v0PMag * v0PMag + v0Mass * v0Mass);
        double recoilPMag = pRecoil.magnitude();
        double eRecoil = FastMath.sqrt(recoilPMag * recoilPMag + ELECTRON_MASS * ELECTRON_MASS);
        double totalE = eV0 + eRecoil;

        Hep3Vector totalP = VecOp.add(pV0, pRecoil);
        double totalPMag = totalP.magnitude();
        double massSq = totalE * totalE - totalPMag * totalPMag;
        double invMass = massSq > 0 ? FastMath.sqrt(massSq) : -99.0;

        BilliorVertex vtxFit = new BilliorVertex(vtxPos, covVtx, result.chi2, invMass, pFitMap, "CASCADE");
        vtxFit.setPositionError(vtxPosErr);
        vtxFit.setProbability(result.ndf);
        vtxFit.setParameter("ndf", (double) result.ndf);

        // Diagnostic target-plane projections (tracking-frame y,z -> detector-frame x,y, same
        // det(x,y,z)=trk(y,z,x) convention as everywhere else in this method).
        vtxFit.setParameter("v0ProjX", v0Proj.position.getEntry(1));
        vtxFit.setParameter("v0ProjY", v0Proj.position.getEntry(2));
        vtxFit.setParameter("v0ProjXErr", FastMath.sqrt(FastMath.abs(v0Proj.cov.getEntry(1, 1))));
        vtxFit.setParameter("v0ProjYErr", FastMath.sqrt(FastMath.abs(v0Proj.cov.getEntry(2, 2))));
        vtxFit.setParameter("recoilProjX", recoilProj.position.getEntry(1));
        vtxFit.setParameter("recoilProjY", recoilProj.position.getEntry(2));
        vtxFit.setParameter("recoilProjXErr", FastMath.sqrt(FastMath.abs(recoilProj.cov.getEntry(1, 1))));
        vtxFit.setParameter("recoilProjYErr", FastMath.sqrt(FastMath.abs(recoilProj.cov.getEntry(2, 2))));

        // The original two-body Billior V0 fit (position, chi2, mass). Here v0Particle is
        // untouched by this fit, so this duplicates v0Particle.getStartVertex(); stored under
        // the same parameter names as ThreeTrackVertexer's cascade (where it does NOT
        // duplicate v0Particle's startVertex) so CascadeVertexTupleDriver can read both
        // outputs uniformly.
        Hep3Vector v0InputPos = v0InputVertex.getPosition();
        Hep3Vector v0InputPosErr = v0InputVertex.getPositionError();
        vtxFit.setParameter("v0InputVtxX", v0InputPos.x());
        vtxFit.setParameter("v0InputVtxY", v0InputPos.y());
        vtxFit.setParameter("v0InputVtxZ", v0InputPos.z());
        vtxFit.setParameter("v0InputVtxXErr", v0InputPosErr.x());
        vtxFit.setParameter("v0InputVtxYErr", v0InputPosErr.y());
        vtxFit.setParameter("v0InputVtxZErr", v0InputPosErr.z());
        vtxFit.setParameter("v0InputChi2", v0InputVertex.getChi2());
        vtxFit.setParameter("v0InputMass", v0InputVertex.getInvMass());

        ReconstructedParticle candidate = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
        candidate.addParticle(v0Particle);
        candidate.addParticle(recoilElectron);
        ((BaseReconstructedParticle) candidate).setType(recoilElectron.getType());
        ((BaseReconstructedParticle) candidate).setMass(invMass);

        HepLorentzVector fourVector = new BasicHepLorentzVector(totalE, totalP);
        ((BaseReconstructedParticle) candidate).set4Vector(fourVector);

        double particleCharge = v0Particle.getCharge() + recoilElectron.getCharge();
        ((BaseReconstructedParticle) candidate).setCharge(particleCharge);

        vtxFit.setAssociatedParticle(candidate);
        ((BaseReconstructedParticle) candidate).setReferencePoint(vtxFit.getPosition());

        return candidate;
    }
}
