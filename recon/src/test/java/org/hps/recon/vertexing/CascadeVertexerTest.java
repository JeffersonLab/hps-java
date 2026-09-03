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
 * Exact-geometry sanity check for {@link CascadeVertexer}: build an already-fitted V0
 * BilliorVertex whose position exactly equals a known point (so its flight line trivially
 * passes through that point regardless of direction) and a recoil-electron track whose helix
 * is constructed to pass through the same point, with the recoil track's AtPerigee reference
 * point set away from the origin (as in real reconstructed tracks) -- verifies the fitted
 * production vertex correctly lands on that point in the absolute tracking/detector frame.
 */
public class CascadeVertexerTest extends TestCase {

    private static final double B_FIELD = 0.5; // Tesla
    private static final double C = 2.99792458e-4;

    public void testFitExactGeometryNonOriginReferencePoint() {
        // Target point, tracking frame (index0=detZ, index1=detX, index2=detY).
        double xV = 5.0, yV = 0.3, zV = -0.2;
        double[] refPoint = {-1.1, 0.0, 0.0};

        Hep3Vector vtxPosDet = new BasicHep3Vector(yV, zV, xV);
        Hep3Vector p1Det = new BasicHep3Vector(0.05, 0.02, 0.3);
        Hep3Vector p2Det = new BasicHep3Vector(0.03, -0.01, 0.25);

        ReconstructedParticle v0Particle = makeV0Particle(vtxPosDet, p1Det, p2Det);

        double pxRecoil = 0.15, pyRecoil = -0.05, pzRecoil = 0.8;
        Track recoilTrack = makeTrackThroughPoint(xV, yV, zV, pxRecoil, pyRecoil, pzRecoil, -1, refPoint);
        ReconstructedParticle recoilElectron = makeElectronParticle(recoilTrack);

        CascadeVertexer vertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle candidate = vertexer.fit(v0Particle, recoilElectron);

        assertNotNull("cascade fit should not be null", candidate);

        BilliorVertex vtx = (BilliorVertex) candidate.getStartVertex();
        Hep3Vector pos = vtx.getPosition();
        System.out.printf("Cascade fitted vertex (det frame): [%.6f, %.6f, %.6f]  "
                + "(truth trk frame: [%.6f, %.6f, %.6f])%n",
                pos.x(), pos.y(), pos.z(), xV, yV, zV);
        System.out.printf("Chi2: %.6f%n", vtx.getChi2());

        // Detector frame: det(x,y,z) = trk(y,z,x)
        assertEquals(yV, pos.x(), 1e-3);
        assertEquals(zV, pos.y(), 1e-3);
        assertEquals(xV, pos.z(), 1e-3);
        assertTrue("chi2 should be small for exactly-consistent geometry, got " + vtx.getChi2(),
                vtx.getChi2() < 1e-2);
    }

    private static ReconstructedParticle makeV0Particle(Hep3Vector vtxPosDet, Hep3Vector p1Det, Hep3Vector p2Det) {
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

        BaseReconstructedParticle v0Particle = new BaseReconstructedParticle();
        v0Particle.setStartVertex(v0Vertex);
        v0Particle.setMass(0.05);
        v0Particle.setCharge(0);
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
