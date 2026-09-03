package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import junit.framework.TestCase;

import org.apache.commons.math.util.FastMath;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;

/**
 * Exact-geometry sanity check for {@link ThreeTrackVertexer}, mirroring {@link
 * CascadeVertexerTest}: build an e-/e+ pair whose tracks (and already-fitted V0
 * BilliorVertex) exactly meet at a known V1 point, and a recoil-electron track whose helix
 * passes exactly through a second known point V2 lying on the V0's flight line, with all
 * three tracks' AtPerigee reference point set away from the origin -- verifies the fitted
 * V1/V2 positions correctly land on those points in the absolute tracking/detector frame.
 */
public class ThreeTrackVertexerTest extends TestCase {

    private static final double B_FIELD = 0.5; // Tesla
    private static final double C = 2.99792458e-4;

    public void testFitExactGeometryNonOriginReferencePoint() {
        double[] refPoint = {-1.1, 0.0, 0.0};

        // V2 (production vertex, tracking frame), near the target.
        double x2V = 0.5, y2V = 0.05, z2V = -0.05;
        // V1 (e-/e+ decay vertex, tracking frame), downstream of V2.
        double x1V = 5.0, y1V = 0.3, z1V = -0.2;

        // e-/e+ momenta (tracking frame) chosen so p1+p2 is parallel to (V1 - V2), i.e. the
        // V0 flies from V2 to V1 along its own total momentum direction.
        double[] p1 = {0.25, 0.02, -0.01};
        double[] p2 = {0.20, 0.005, -0.005};

        Track eleTrack = makeTrackThroughPoint(x1V, y1V, z1V, p1[0], p1[1], p1[2], -1, refPoint);
        Track posTrack = makeTrackThroughPoint(x1V, y1V, z1V, p2[0], p2[1], p2[2], 1, refPoint);

        Hep3Vector v1PosDet = new BasicHep3Vector(y1V, z1V, x1V);
        Hep3Vector p1Det = new BasicHep3Vector(p1[1], p1[2], p1[0]);
        Hep3Vector p2Det = new BasicHep3Vector(p2[1], p2[2], p2[0]);

        ReconstructedParticle v0Particle = makeV0Particle(v1PosDet, p1Det, p2Det, eleTrack, posTrack);

        double[] pRecoil = {0.15, -0.02, 0.01};
        Track recoilTrack = makeTrackThroughPoint(x2V, y2V, z2V, pRecoil[0], pRecoil[1], pRecoil[2], -1, refPoint);
        ReconstructedParticle recoilElectron = makeElectronParticle(recoilTrack);

        ThreeTrackVertexer vertexer = new ThreeTrackVertexer(B_FIELD);
        ReconstructedParticle cascade = vertexer.fit(v0Particle, recoilElectron, true);

        assertNotNull("three-track fit should not be null", cascade);

        BilliorVertex v2Vtx = (BilliorVertex) cascade.getStartVertex();
        Hep3Vector v2PosDet = v2Vtx.getPosition();
        ReconstructedParticle v0Out = cascade.getParticles().get(0);
        BilliorVertex v1Vtx = (BilliorVertex) v0Out.getStartVertex();
        Hep3Vector v1PosDetFit = v1Vtx.getPosition();

        System.out.printf("Fitted V1 (det frame): [%.6f, %.6f, %.6f]  (truth trk frame: [%.6f, %.6f, %.6f])%n",
                v1PosDetFit.x(), v1PosDetFit.y(), v1PosDetFit.z(), x1V, y1V, z1V);
        System.out.printf("Fitted V2 (det frame): [%.6f, %.6f, %.6f]  (truth trk frame: [%.6f, %.6f, %.6f])%n",
                v2PosDet.x(), v2PosDet.y(), v2PosDet.z(), x2V, y2V, z2V);
        System.out.printf("Chi2: %.6f%n", v2Vtx.getChi2());

        // Detector frame: det(x,y,z) = trk(y,z,x)
        assertEquals(y1V, v1PosDetFit.x(), 1e-3);
        assertEquals(z1V, v1PosDetFit.y(), 1e-3);
        assertEquals(x1V, v1PosDetFit.z(), 1e-3);

        assertEquals(y2V, v2PosDet.x(), 1e-3);
        assertEquals(z2V, v2PosDet.y(), 1e-3);
        assertEquals(x2V, v2PosDet.z(), 1e-3);

        assertTrue("chi2 should be small for exactly-consistent geometry, got " + v2Vtx.getChi2(),
                v2Vtx.getChi2() < 1e-2);
    }

    private static ReconstructedParticle makeV0Particle(Hep3Vector vtxPosDet, Hep3Vector p1Det, Hep3Vector p2Det,
            Track eleTrack, Track posTrack) {
        SymmetricMatrix covVtx = new SymmetricMatrix(3);
        covVtx.setElement(0, 0, 1e-6);
        covVtx.setElement(1, 1, 1e-6);
        covVtx.setElement(2, 2, 1e-6);

        Map<Integer, Hep3Vector> pFitMap = new HashMap<Integer, Hep3Vector>();
        pFitMap.put(0, p1Det);
        pFitMap.put(1, p2Det);

        BilliorVertex v0Vertex = new BilliorVertex(vtxPosDet, covVtx, 0.0, 0.05, pFitMap, "TEST_V0");
        v0Vertex.setPositionError(new BasicHep3Vector(1e-3, 1e-3, 1e-3));

        SymmetricMatrix covP1 = new SymmetricMatrix(3);
        covP1.setElement(0, 0, 1e-6);
        covP1.setElement(1, 1, 1e-6);
        covP1.setElement(2, 2, 1e-6);
        SymmetricMatrix covP2 = new SymmetricMatrix(3);
        covP2.setElement(0, 0, 1e-6);
        covP2.setElement(1, 1, 1e-6);
        covP2.setElement(2, 2, 1e-6);
        SymmetricMatrix covP12 = new SymmetricMatrix(3);
        List<Matrix> covTrkMomList = new ArrayList<Matrix>();
        covTrkMomList.add(covP1);
        covTrkMomList.add(covP2);
        covTrkMomList.add(covP12);
        v0Vertex.setTrackMomentumCovariances(covTrkMomList);

        SymmetricMatrix covVp1 = new SymmetricMatrix(3);
        SymmetricMatrix covVp2 = new SymmetricMatrix(3);
        List<Matrix> covVtxMomList = new ArrayList<Matrix>();
        covVtxMomList.add(covVp1);
        covVtxMomList.add(covVp2);
        v0Vertex.setVertexMomentumCovariance(covVtxMomList);

        BaseReconstructedParticle eleDaughter = new BaseReconstructedParticle();
        eleDaughter.addTrack(eleTrack);
        eleDaughter.setCharge(-1);

        BaseReconstructedParticle posDaughter = new BaseReconstructedParticle();
        posDaughter.addTrack(posTrack);
        posDaughter.setCharge(1);

        BaseReconstructedParticle v0Particle = new BaseReconstructedParticle();
        v0Particle.setStartVertex(v0Vertex);
        v0Particle.setMass(0.05);
        v0Particle.setCharge(0);
        v0Particle.addParticle(eleDaughter);
        v0Particle.addParticle(posDaughter);
        return v0Particle;
    }

    private static ReconstructedParticle makeElectronParticle(Track track) {
        BaseReconstructedParticle particle = new BaseReconstructedParticle();
        particle.addTrack(track);
        particle.setCharge(-1);
        return particle;
    }

    /**
     * Build a Track with a single AtPerigee TrackState whose helix passes exactly through
     * (xV,yV,zV) with the given momentum and reference point, mirroring
     * {@code KalmanV0VertexerTest#makeTrackThroughPoint}.
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
