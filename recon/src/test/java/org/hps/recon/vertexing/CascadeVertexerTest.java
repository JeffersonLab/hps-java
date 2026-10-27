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
 * Exact-geometry sanity check for {@link CascadeVertexer}: build an e-/e+ pair whose
 * tracks (and already-fitted V0 BilliorVertex) exactly meet at a known V1 point, and a
 * recoil-electron track whose helix
 * passes exactly through a second known point V2 lying on the V0's flight line, with all
 * three tracks' AtPerigee reference point set away from the origin -- verifies the fitted
 * V1/V2 positions correctly land on those points in the absolute tracking/detector frame.
 */
public class CascadeVertexerTest extends TestCase {

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

        CascadeVertexer vertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle cascade = vertexer.fit(v0Particle, recoilElectron);

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

    /**
     * Verify {@link CascadeVertexer#setUseBeamspotConstraintForV2} actually pulls the fitted V2
     * toward a supplied beamspot position/size (via {@link
     * CascadeVertexer#setBeamspotConstraintForV2Params}), and that the default (toggle off)
     * behavior is unaffected -- proving the new opt-in toggle causes zero regression to existing
     * behavior when left at its default.
     */
    public void testFitWithBeamspotConstraintForV2PullsTowardBeamPosition() {
        double[] refPoint = {-1.1, 0.0, 0.0};

        // V2 (production vertex, tracking frame), near the target.
        double x2V = 0.5, y2V = 0.05, z2V = -0.05;
        // V1 (e-/e+ decay vertex, tracking frame), downstream of V2.
        double x1V = 5.0, y1V = 0.3, z1V = -0.2;

        double[] p1 = {0.25, 0.02, -0.01};
        double[] p2 = {0.20, 0.005, -0.005};
        double[] pRecoil = {0.15, -0.02, 0.01};

        Track eleTrack = makeTrackThroughPoint(x1V, y1V, z1V, p1[0], p1[1], p1[2], -1, refPoint);
        Track posTrack = makeTrackThroughPoint(x1V, y1V, z1V, p2[0], p2[1], p2[2], 1, refPoint);
        Hep3Vector v1PosDet = new BasicHep3Vector(y1V, z1V, x1V);
        Hep3Vector p1Det = new BasicHep3Vector(p1[1], p1[2], p1[0]);
        Hep3Vector p2Det = new BasicHep3Vector(p2[1], p2[2], p2[0]);
        ReconstructedParticle v0Particle = makeV0Particle(v1PosDet, p1Det, p2Det, eleTrack, posTrack);
        Track recoilTrack = makeTrackThroughPoint(x2V, y2V, z2V, pRecoil[0], pRecoil[1], pRecoil[2], -1, refPoint);
        ReconstructedParticle recoilElectron = makeElectronParticle(recoilTrack);

        CascadeVertexer defaultVertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle cascadeDefault = defaultVertexer.fit(v0Particle, recoilElectron);
        assertNotNull("default (unconstrained-V2) fit should not be null", cascadeDefault);
        Hep3Vector v2PosDetDefault = ((BilliorVertex) cascadeDefault.getStartVertex()).getPosition();

        // Well-separated from both refPoint and V2's own truth position, so the pull is
        // unambiguous. beamSizeOverride must be tight not just relative to the tracks'
        // individual d0/z0 sigmas, but relative to the recoil track's own (potentially
        // anisotropic) vertex information -- 1e-4 (information ~1e8) safely dominates that,
        // unlike an initially-tried 0.001 (information ~1e6, only comparable to the track's best-
        // constrained direction, which pulled V2 to neither truth nor the beam position; see
        // NTrackVertexerTest's analogous comment). An even tighter 1e-6 (information ~1e12)
        // was also tried but made fitCascadeVertexJointFreeTrackCore's internal matrix inversion
        // numerically singular -- this joint two-vertex fit is more sensitive to ill-conditioning
        // from an extreme prior/track information mismatch than the simpler N-track fit.
        double[] beamPositionOverride = {0.0, 0.0, 0.0};
        double[] beamSizeOverride = {1e-4, 1e-4, 1e-4};
        CascadeVertexer beamspotVertexer = new CascadeVertexer(B_FIELD);
        beamspotVertexer.setUseBeamspotConstraintForV2(true);
        beamspotVertexer.setBeamspotConstraintForV2Params(beamPositionOverride, beamSizeOverride);
        ReconstructedParticle cascadeBS = beamspotVertexer.fit(v0Particle, recoilElectron);
        assertNotNull("beamspot-constrained-V2 fit should not be null", cascadeBS);
        Hep3Vector v2PosDetBS = ((BilliorVertex) cascadeBS.getStartVertex()).getPosition();

        System.out.printf("Default V2 (det frame): [%.6f, %.6f, %.6f]  (truth trk frame: [%.6f, %.6f, %.6f])%n",
                v2PosDetDefault.x(), v2PosDetDefault.y(), v2PosDetDefault.z(), x2V, y2V, z2V);
        System.out.printf("Beamspot-constrained V2 (det frame): [%.6f, %.6f, %.6f]  "
                + "(beam position trk frame: [%.6f, %.6f, %.6f], beam size trk frame: [%.6f, %.6f, %.6f])%n",
                v2PosDetBS.x(), v2PosDetBS.y(), v2PosDetBS.z(),
                beamPositionOverride[0], beamPositionOverride[1], beamPositionOverride[2],
                beamSizeOverride[0], beamSizeOverride[1], beamSizeOverride[2]);

        // The default (toggle off) fit should reproduce the existing exact-geometry result,
        // unaffected by the new toggle's existence.
        assertEquals(y2V, v2PosDetDefault.x(), 1e-3);
        assertEquals(z2V, v2PosDetDefault.y(), 1e-3);
        assertEquals(x2V, v2PosDetDefault.z(), 1e-3);

        // The beamspot-constrained fit should land close to the (tight) beamspot prior instead
        // of near truth (det frame: det(x,y,z) = trk(y,z,x), beam position trk (0,0,0) -> det
        // (0,0,0)).
        assertEquals(0.0, v2PosDetBS.x(), 0.01);
        assertEquals(0.0, v2PosDetBS.y(), 0.01);
        assertEquals(0.0, v2PosDetBS.z(), 0.01);

        double pullDistance = distance(v2PosDetBS, v2PosDetDefault);
        assertTrue("beamspot constraint should measurably pull V2 away from the default result, "
                + "got pull distance " + pullDistance, pullDistance > 0.3);
    }

    private static double distance(Hep3Vector a, Hep3Vector b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return FastMath.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Verify {@link CascadeVertexer#fit(ReconstructedParticle, ReconstructedParticle, boolean,
     * boolean, double, double, double)} with {@code beamConstrained=true} AND {@link
     * CascadeVertexer#setUseBeamspotConstraintForV2} both set simultaneously pulls V2 toward
     * the beamspot position AND the total (V0 + recoil) 3-momentum toward the beam-momentum
     * target at the same time, and that the pre-existing beamspot-only and beam-momentum-only
     * paths (each exercised here on fresh {@code CascadeVertexer} instances, since the toggle
     * is instance state) are completely unaffected by the new combined call.
     */
    public void testFitBothConstrainedPullsTowardBeamspotAndBeamMomentum() {
        double[] refPoint = {-1.1, 0.0, 0.0};

        // V2 (production vertex, tracking frame), near the target.
        double x2V = 0.5, y2V = 0.05, z2V = -0.05;
        // V1 (e-/e+ decay vertex, tracking frame), downstream of V2.
        double x1V = 5.0, y1V = 0.3, z1V = -0.2;

        // Same track geometry as testFitExactGeometryNonOriginReferencePoint -- total
        // (e-/e+/recoil) momentum [0.60, 0.005, -0.005] (trk frame) is already dominantly
        // along tracking-X, matching the beam-momentum convention below, so no direction
        // mismatch (see NTrackVertexerTest's analogous comment) is introduced by adding
        // the beam-momentum constraint on top of these tracks.
        double[] p1 = {0.25, 0.02, -0.01};
        double[] p2 = {0.20, 0.005, -0.005};
        double[] pRecoil = {0.15, -0.02, 0.01};

        Track eleTrack = makeTrackThroughPoint(x1V, y1V, z1V, p1[0], p1[1], p1[2], -1, refPoint);
        Track posTrack = makeTrackThroughPoint(x1V, y1V, z1V, p2[0], p2[1], p2[2], 1, refPoint);
        Hep3Vector v1PosDet = new BasicHep3Vector(y1V, z1V, x1V);
        Hep3Vector p1Det = new BasicHep3Vector(p1[1], p1[2], p1[0]);
        Hep3Vector p2Det = new BasicHep3Vector(p2[1], p2[2], p2[0]);
        ReconstructedParticle v0Particle = makeV0Particle(v1PosDet, p1Det, p2Det, eleTrack, posTrack);
        Track recoilTrack = makeTrackThroughPoint(x2V, y2V, z2V, pRecoil[0], pRecoil[1], pRecoil[2], -1, refPoint);
        ReconstructedParticle recoilElectron = makeElectronParticle(recoilTrack);

        CascadeVertexer defaultVertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle cascadeDefault = defaultVertexer.fit(v0Particle, recoilElectron);
        assertNotNull("default (unconstrained) fit should not be null", cascadeDefault);
        Hep3Vector v2PosDetDefault = ((BilliorVertex) cascadeDefault.getStartVertex()).getPosition();
        Hep3Vector totalPDefault = cascadeDefault.getMomentum();

        // Beam momentum target chosen far from the tracks' own (unconstrained) momentum sum
        // [0.60, 0.005, -0.005] (trk frame), so a real pull is unambiguous.
        double beamEnergy = 1.0;
        double beamRotAngle = 0.02;
        double beamPxTrk = beamEnergy * FastMath.cos(beamRotAngle);
        double beamPyTrk = -beamEnergy * FastMath.sin(beamRotAngle);
        // getMomentum() (used throughout below) returns detector frame, det(x,y,z) = trk(y,z,x)
        // -- same convention CascadeVertexer.makeReconstructedParticle itself uses -- so the
        // comparison target must be built in that frame too, not tracking frame (see
        // NTrackVertexerTest's analogous fix for the same pitfall).
        Hep3Vector beamPDet = new BasicHep3Vector(beamPyTrk, 0.0, beamPxTrk);

        // Same beamspot prior as testFitWithBeamspotConstraintForV2PullsTowardBeamPosition, far
        // from the tracks' own (unconstrained) V2 position.
        double[] beamPositionOverride = {0.0, 0.0, 0.0};
        double[] beamSizeOverride = {1e-4, 1e-4, 1e-4};

        CascadeVertexer bothVertexer = new CascadeVertexer(B_FIELD);
        bothVertexer.setUseBeamspotConstraintForV2(true);
        bothVertexer.setBeamspotConstraintForV2Params(beamPositionOverride, beamSizeOverride);
        ReconstructedParticle cascadeBoth = bothVertexer.fit(
                v0Particle, recoilElectron, true, beamEnergy, beamRotAngle, 0.0);
        assertNotNull("both-constrained fit should not be null", cascadeBoth);
        Hep3Vector v2PosDetBoth = ((BilliorVertex) cascadeBoth.getStartVertex()).getPosition();
        Hep3Vector totalPBoth = cascadeBoth.getMomentum();

        System.out.printf("Default V2 (det frame): [%.6f, %.6f, %.6f], total P: [%.6f, %.6f, %.6f]%n",
                v2PosDetDefault.x(), v2PosDetDefault.y(), v2PosDetDefault.z(),
                totalPDefault.x(), totalPDefault.y(), totalPDefault.z());
        System.out.printf("Both-constrained V2 (det frame): [%.6f, %.6f, %.6f], total P: [%.6f, %.6f, %.6f] "
                + "(beam P det frame: [%.6f, %.6f, %.6f])%n",
                v2PosDetBoth.x(), v2PosDetBoth.y(), v2PosDetBoth.z(),
                totalPBoth.x(), totalPBoth.y(), totalPBoth.z(),
                beamPDet.x(), beamPDet.y(), beamPDet.z());

        // Position: the beamspot prior is far tighter than the tracks' own position
        // sensitivity, so the constrained fit's V2 should land very close to the beam position
        // (det frame: det(x,y,z) = trk(y,z,x), beam position trk (0,0,0) -> det (0,0,0)).
        assertEquals(0.0, v2PosDetBoth.x(), 0.01);
        assertEquals(0.0, v2PosDetBoth.y(), 0.01);
        assertEquals(0.0, v2PosDetBoth.z(), 0.01);

        // Momentum: the both-constrained total momentum should land measurably closer to the
        // beam momentum target than the default (unconstrained) fit's total momentum did.
        double distDefaultFromBeam = distance(totalPDefault, beamPDet);
        double distBothFromBeam = distance(totalPBoth, beamPDet);
        assertTrue("both-constrained total momentum should be pulled measurably closer to the "
                + "beam momentum target than the default fit (default dist=" + distDefaultFromBeam
                + ", both dist=" + distBothFromBeam + ")", distBothFromBeam < 0.5 * distDefaultFromBeam);

        // Calling the pre-existing single-constraint paths on the same tracks (fresh instances,
        // since the beamspot toggle is instance state) must be completely unaffected by having
        // exercised the new combined path above.
        CascadeVertexer beamspotOnlyVertexer = new CascadeVertexer(B_FIELD);
        beamspotOnlyVertexer.setUseBeamspotConstraintForV2(true);
        beamspotOnlyVertexer.setBeamspotConstraintForV2Params(beamPositionOverride, beamSizeOverride);
        ReconstructedParticle cascadeBS = beamspotOnlyVertexer.fit(v0Particle, recoilElectron);
        assertNotNull("beamspot-only fit should not be null", cascadeBS);
        Hep3Vector v2PosDetBS = ((BilliorVertex) cascadeBS.getStartVertex()).getPosition();
        assertEquals(0.0, v2PosDetBS.x(), 0.01);
        assertEquals(0.0, v2PosDetBS.y(), 0.01);
        assertEquals(0.0, v2PosDetBS.z(), 0.01);

        CascadeVertexer beamMomOnlyVertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle cascadeBM = beamMomOnlyVertexer.fit(
                v0Particle, recoilElectron, true, beamEnergy, beamRotAngle, 0.0);
        assertNotNull("beam-momentum-only fit should not be null", cascadeBM);
        double distBMFromBeam = distance(cascadeBM.getMomentum(), beamPDet);
        assertTrue("beam-momentum-only total momentum should also be pulled measurably closer "
                + "to the beam momentum target than the default fit (default dist="
                + distDefaultFromBeam + ", beam-mom-only dist=" + distBMFromBeam + ")",
                distBMFromBeam < 0.5 * distDefaultFromBeam);
    }

    /**
     * Shape-only check for {@link CascadeVertexer#placeholderCascade}: given a real,
     * successful cascade candidate, the placeholder should mirror its nested structure
     * (cascade -> [v0Particle, recoilElectron], v0Particle -> [eleDaughter, posDaughter])
     * with the same daughter objects, but sentinel (-9999) chi2/mass/ndf on both vertices.
     */
    public void testPlaceholderCascade() {
        double[] refPoint = {-1.1, 0.0, 0.0};
        double x1V = 5.0, y1V = 0.3, z1V = -0.2;
        double x2V = 0.5, y2V = 0.05, z2V = -0.05;
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

        CascadeVertexer vertexer = new CascadeVertexer(B_FIELD);
        ReconstructedParticle cascade = vertexer.fit(v0Particle, recoilElectron);
        assertNotNull("cascade fit should not be null", cascade);

        ReconstructedParticle placeholder = CascadeVertexer.placeholderCascade(cascade);
        assertNotNull("placeholder cascade should not be null", placeholder);
        assertEquals(2, placeholder.getParticles().size());
        assertSame(recoilElectron.getTracks().get(0), placeholder.getParticles().get(1).getTracks().get(0));

        ReconstructedParticle placeholderV0 = placeholder.getParticles().get(0);
        assertEquals(2, placeholderV0.getParticles().size());
        assertSame(eleTrack, placeholderV0.getParticles().get(0).getTracks().get(0));
        assertSame(posTrack, placeholderV0.getParticles().get(1).getTracks().get(0));

        BilliorVertex placeholderV2Vtx = (BilliorVertex) placeholder.getStartVertex();
        assertEquals(-9999.0, placeholderV2Vtx.getChi2(), 1e-9);
        assertEquals(-9999.0, placeholderV2Vtx.getInvMass(), 1e-9);
        assertEquals(-9999.0, placeholderV2Vtx.getCustomParameters().get("ndf"), 1e-9);

        BilliorVertex placeholderV1Vtx = (BilliorVertex) placeholderV0.getStartVertex();
        assertEquals(-9999.0, placeholderV1Vtx.getChi2(), 1e-9);
        assertEquals(-9999.0, placeholderV1Vtx.getInvMass(), 1e-9);
        assertEquals(-9999.0, placeholderV1Vtx.getCustomParameters().get("ndf"), 1e-9);
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
     * {@code NTrackVertexerTest#makeTrackThroughPoint}.
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
