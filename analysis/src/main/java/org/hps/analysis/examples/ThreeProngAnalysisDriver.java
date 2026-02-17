package org.hps.analysis.examples;

import java.util.List;
import java.util.Map;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.record.StandardCuts;

/**
 * Analysis driver for three-prong (e-e+e-) vertex fit diagnostics.
 * Two histogram sets: with probability cut (/ThreeProng) and without (/ThreeProng_NoCut).
 */
public class ThreeProngAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String threeProngCandidatesColName = "ThreeProngCandidates";
    private String threeProngVerticesColName = "ThreeProngVertices";
    private double beamRotationY = -0.0305;
    protected StandardCuts cuts = new StandardCuts();

    private IHistogram1D hNCandidates;
    private HistSet cutHS, noCutHS;

    private static final String[] PN = {"Ele", "Pos", "Rec"};
    private static final String[] PNFULL = {"Electron", "Positron", "Recoil"};

    private static class HistSet {
        IHistogram1D[] trkP=new IHistogram1D[3], trkPt=new IHistogram1D[3], trkD0=new IHistogram1D[3];
        IHistogram1D[] trkPhi0=new IHistogram1D[3], trkOmega=new IHistogram1D[3], trkZ0=new IHistogram1D[3];
        IHistogram1D[] trkTanL=new IHistogram1D[3], trkChi2=new IHistogram1D[3];
        IHistogram1D[] partP=new IHistogram1D[3];
        IHistogram1D totPIn, totPxIn, totPyIn, totPzIn, sumPMagIn, magPSumIn, pMagDiffIn;
        IHistogram1D vtxX, vtxY, vtxZ, vtxChi2, vtxProb, invMass;
        IHistogram1D[] fitP=new IHistogram1D[3];
        IHistogram1D totPFit, totPxFit, totPyFit, totPzFit, sumPMagFit, magPSumFit, pMagDiffFit;
        IHistogram1D dPhiXZ1, dPhiXZ2, dPhiXZMin;
        IHistogram1D trkDtEP, trkDtER, trkDtPR;
        IHistogram1D clusDtEP, clusDtER, clusDtPR;
        IHistogram1D[] trkClusDt=new IHistogram1D[3], trkMinPosClT=new IHistogram1D[3];
        IHistogram2D vtxXvsZ, p1vP2, p1vP3, p2vP3, invMvChi2;
        IHistogram1D avgZ0h, z0Sph, vtxYmAvgZ0, tanLSph, avgTanLh;
        IHistogram1D[] z0Err=new IHistogram1D[3];
        IHistogram2D vtxYvAvgZ0, vtxYvTanLSp, vtxYvChi2;
        IHistogram1D[] resPx=new IHistogram1D[3], resPy=new IHistogram1D[3];
        IHistogram1D[] resPz=new IHistogram1D[3], resPtot=new IHistogram1D[3];
        IHistogram1D resTotPx, resTotPy, resTotPz, resTotP;
        IHistogram1D[] pullPx=new IHistogram1D[3], pullPy=new IHistogram1D[3];
        IHistogram1D[] pullPz=new IHistogram1D[3], pullPtot=new IHistogram1D[3];
    }

    public void setThreeProngCandidatesColName(String s) { threeProngCandidatesColName = s; }
    public void setThreeProngVerticesColName(String s) { threeProngVerticesColName = s; }
    public void setBeamRotationY(double angle) { beamRotationY = angle; }

    private Hep3Vector rotateToBeamFrame(Hep3Vector v) {
        double c = Math.cos(beamRotationY), s = Math.sin(beamRotationY);
        return new BasicHep3Vector(v.x()*c + v.z()*s, v.y(), -v.x()*s + v.z()*c);
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        aida.tree().mkdir("/ThreeProng");
        aida.tree().cd("/ThreeProng");
        hNCandidates = aida.histogram1D("Number of candidates per event", 20, 0, 20);
        cutHS = createHistSet("/ThreeProng");
        aida.tree().mkdir("/ThreeProng_NoCut");
        noCutHS = createHistSet("/ThreeProng_NoCut");
    }

    private HistSet createHistSet(String b) {
        HistSet h = new HistSet();
        double tr = 10.0, pi2 = 2.0 * Math.PI;

        // Track params per particle
        aida.tree().mkdir(b + "/Input");
        for (int i = 0; i < 3; i++) {
            String d = b + "/Input/" + PN[i];
            aida.tree().mkdir(d); aida.tree().cd(d);
            h.trkP[i] = aida.histogram1D(PN[i]+" momentum", 200, 0, 5.0);
            h.trkPt[i] = aida.histogram1D(PN[i]+" pT", 200, 0, 2.0);
            h.trkD0[i] = aida.histogram1D(PN[i]+" d0", 200, -3.0, 3.0);
            h.trkPhi0[i] = aida.histogram1D(PN[i]+" phi0", 200, -0.2, 0.2);
            h.trkOmega[i] = aida.histogram1D(PN[i]+" omega", 200, -0.01, 0.01);
            h.trkZ0[i] = aida.histogram1D(PN[i]+" z0", 200, -1.0, 1.0);
            h.trkTanL[i] = aida.histogram1D(PN[i]+" tanLambda", 200, -0.2, 0.2);
            h.trkChi2[i] = aida.histogram1D(PN[i]+" track chi2", 200, 0, 100.0);
        }

        String d = b + "/Input/Total";
        aida.tree().mkdir(d); aida.tree().cd(d);
        for (int i = 0; i < 3; i++)
            h.partP[i] = aida.histogram1D(PNFULL[i]+" momentum", 200, 0, 5.0);
        h.totPIn = aida.histogram1D("Total input momentum", 200, 0, 6.0);
        h.totPxIn = aida.histogram1D("Total input Px", 200, -0.5, 0.5);
        h.totPyIn = aida.histogram1D("Total input Py", 200, -0.2, 0.2);
        h.totPzIn = aida.histogram1D("Total input Pz", 200, 0, 6.0);
        h.sumPMagIn = aida.histogram1D("Sum of input |p_i|", 200, 0, 6.0);
        h.magPSumIn = aida.histogram1D("Mag of input sum p", 200, 0, 6.0);
        h.pMagDiffIn = aida.histogram1D("Sum|p_i| - |sum p_i| input", 200, -0.03, 0.03);

        d = b + "/Vertex";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.vtxX = aida.histogram1D("Vertex X", 200, -0.5, 0.5);
        h.vtxY = aida.histogram1D("Vertex Y", 200, -0.5, 0.5);
        h.vtxZ = aida.histogram1D("Vertex Z", 200, -10.0, 10.0);
        h.vtxChi2 = aida.histogram1D("Vertex chi2", 200, 0, 250.0);
        h.vtxProb = aida.histogram1D("Vertex probability", 200, 0, 1.0);
        h.invMass = aida.histogram1D("Invariant mass", 200, 0, 0.5);

        d = b + "/FittedMomenta";
        aida.tree().mkdir(d); aida.tree().cd(d);
        for (int i = 0; i < 3; i++)
            h.fitP[i] = aida.histogram1D("Fitted p"+(i+1), 200, 0, 5.0);
        h.totPFit = aida.histogram1D("Total fitted momentum", 200, 0, 6.0);
        h.totPxFit = aida.histogram1D("Total fitted Px", 200, -0.5, 0.5);
        h.totPyFit = aida.histogram1D("Total fitted Py", 200, -0.2, 0.2);
        h.totPzFit = aida.histogram1D("Total fitted Pz", 200, 0, 6.0);
        h.sumPMagFit = aida.histogram1D("Sum of fitted |p_i|", 200, 0, 6.0);
        h.magPSumFit = aida.histogram1D("Mag of fitted sum p", 200, 0, 6.0);
        h.pMagDiffFit = aida.histogram1D("Sum|p_i| - |sum p_i| fitted", 200, -0.03, 0.03);

        d = b + "/DeltaPhiXZ";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.dPhiXZ1 = aida.histogram1D("DeltaPhiXZ pairing1 (ele+pos vs recoil)", 200, -pi2, pi2);
        h.dPhiXZ2 = aida.histogram1D("DeltaPhiXZ pairing2 (rec+pos vs ele)", 200, -pi2, pi2);
        h.dPhiXZMin = aida.histogram1D("DeltaPhiXZ closest to pi", 200, -pi2, pi2);

        d = b + "/TrackTimeDiff";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.trkDtEP = aida.histogram1D("Track dt ele-pos", 200, -tr, tr);
        h.trkDtER = aida.histogram1D("Track dt ele-rec", 200, -tr, tr);
        h.trkDtPR = aida.histogram1D("Track dt pos-rec", 200, -tr, tr);

        d = b + "/ClusterTimeDiff";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.clusDtEP = aida.histogram1D("Cluster dt ele-pos", 200, -tr, tr);
        h.clusDtER = aida.histogram1D("Cluster dt ele-rec", 200, -tr, tr);
        h.clusDtPR = aida.histogram1D("Cluster dt pos-rec", 200, -tr, tr);

        d = b + "/TrackClusterTimeDiff";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.trkClusDt[0] = aida.histogram1D("Track-Cluster dt ele", 200, -tr, tr);
        h.trkClusDt[1] = aida.histogram1D("Track-Cluster dt pos", 200, -tr, tr);
        h.trkClusDt[2] = aida.histogram1D("Track-Cluster dt rec", 200, -tr, tr);

        d = b + "/TrackMinusPosClusTime";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.trkMinPosClT[0] = aida.histogram1D("Ele track - Pos cluster time", 200, -tr, tr);
        h.trkMinPosClT[1] = aida.histogram1D("Pos track - Pos cluster time", 200, -tr, tr);
        h.trkMinPosClT[2] = aida.histogram1D("Rec track - Pos cluster time", 200, -tr, tr);

        d = b + "/Correlations";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.vtxXvsZ = aida.histogram2D("Vertex X vs Z", 100, -50.0, 50.0, 100, -5.0, 5.0);
        h.p1vP2 = aida.histogram2D("p pos vs p ele", 100, 0, 5.0, 100, 0, 5.0);
        h.p1vP3 = aida.histogram2D("p pos vs p rec", 100, 0, 5.0, 100, 0, 5.0);
        h.p2vP3 = aida.histogram2D("p ele vs p rec", 100, 0, 5.0, 100, 0, 5.0);
        h.invMvChi2 = aida.histogram2D("InvMass vs chi2", 100, 0, 0.5, 100, 0, 100.0);

        d = b + "/ZConstraintDiag";
        aida.tree().mkdir(d); aida.tree().cd(d);
        h.avgZ0h = aida.histogram1D("Average track z0", 200, -1.0, 1.0);
        h.z0Sph = aida.histogram1D("z0 spread (max-min)", 200, 0, 1.0);
        h.z0Err[0] = aida.histogram1D("Electron z0 error", 200, 0, 0.5);
        h.z0Err[1] = aida.histogram1D("Positron z0 error", 200, 0, 0.5);
        h.z0Err[2] = aida.histogram1D("Recoil z0 error", 200, 0, 0.5);
        h.vtxYmAvgZ0 = aida.histogram1D("Fitted vtx Y - avg z0 (mm)", 200, -0.5, 0.5);
        h.vtxYvAvgZ0 = aida.histogram2D("Fitted vtx Y vs avg z0", 100, -1.0, 1.0, 100, -0.5, 0.5);
        h.tanLSph = aida.histogram1D("tanLambda spread (max-min)", 200, 0, 0.2);
        h.avgTanLh = aida.histogram1D("Average tanLambda", 200, -0.1, 0.1);
        h.vtxYvTanLSp = aida.histogram2D("Vtx Y vs tanL spread", 100, 0, 0.2, 100, -0.5, 0.5);
        h.vtxYvChi2 = aida.histogram2D("Vtx Y vs chi2", 100, 0, 100.0, 100, -0.5, 0.5);

        d = b + "/MomentumResiduals";
        aida.tree().mkdir(d); aida.tree().cd(d);
        for (int i = 0; i < 3; i++) {
            h.resPx[i] = aida.histogram1D(PN[i]+" px residual (fitted-input)", 200, -0.5, 0.5);
            h.resPy[i] = aida.histogram1D(PN[i]+" py residual (fitted-input)", 200, -0.2, 0.2);
            h.resPz[i] = aida.histogram1D(PN[i]+" pz residual (fitted-input)", 200, -0.5, 0.5);
            h.resPtot[i] = aida.histogram1D(PN[i]+" ptot residual (fitted-input)", 200, -0.5, 0.5);
        }
        h.resTotPx = aida.histogram1D("Total px residual (sum fitted - beam)", 200, -0.2, 0.2);
        h.resTotPy = aida.histogram1D("Total py residual (sum fitted - beam)", 200, -0.1, 0.1);
        h.resTotPz = aida.histogram1D("Total pz residual (sum fitted - beam)", 200, -0.2, 0.2);
        h.resTotP = aida.histogram1D("Total p residual (sum fitted - beam)", 200, -0.2, 0.2);

        d = b + "/MomentumPulls";
        aida.tree().mkdir(d); aida.tree().cd(d);
        for (int i = 0; i < 3; i++) {
            h.pullPx[i] = aida.histogram1D(PN[i]+" px pull (fitted-input)/sigma", 200, -10.0, 10.0);
            h.pullPy[i] = aida.histogram1D(PN[i]+" py pull (fitted-input)/sigma", 200, -10.0, 10.0);
            h.pullPz[i] = aida.histogram1D(PN[i]+" pz pull (fitted-input)/sigma", 200, -10.0, 10.0);
            h.pullPtot[i] = aida.histogram1D(PN[i]+" ptot pull (fitted-input)/sigma", 200, -10.0, 10.0);
        }
        return h;
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(ReconstructedParticle.class, threeProngCandidatesColName)) {
            hNCandidates.fill(0);
            return;
        }
        List<ReconstructedParticle> candidates = event.get(ReconstructedParticle.class, threeProngCandidatesColName);
        hNCandidates.fill(candidates.size());

        for (ReconstructedParticle candidate : candidates) {
            List<ReconstructedParticle> particles = candidate.getParticles();
            if (particles.size() != 3) continue;
            Vertex vtx = candidate.getStartVertex();
            if (vtx == null) continue;

            ReconstructedParticle[] parts = {particles.get(0), particles.get(1), particles.get(2)};

            // Pre-compute track parameters
            double[] trkPMag = new double[3], trkPtV = new double[3], trkD0V = new double[3];
            double[] trkPhi0V = new double[3], trkOmV = new double[3], trkZ0V = new double[3];
            double[] trkTanLV = new double[3], trkChi2V = new double[3];
            TrackState[] ts = new TrackState[3];
            Track[] trks = new Track[3];
            for (int i = 0; i < 3; i++) {
                trks[i] = parts[i].getTracks().get(0);
                ts[i] = trks[i].getTrackStates().get(0);
                double[] p = ts[i].getParameters();
                double[] mom = ts[i].getMomentum();
                trkPMag[i] = parts[i].getMomentum().magnitude();
                trkPtV[i] = Math.sqrt(mom[0]*mom[0] + mom[1]*mom[1]);
                trkD0V[i] = p[0]; trkPhi0V[i] = p[1]; trkOmV[i] = p[2];
                trkZ0V[i] = p[3]; trkTanLV[i] = p[4];
                trkChi2V[i] = trks[i].getChi2();
            }

            // Z0 diagnostics
            double avgZ0 = (trkZ0V[0] + trkZ0V[1] + trkZ0V[2]) / 3.0;
            double z0Spread = Math.max(trkZ0V[0], Math.max(trkZ0V[1], trkZ0V[2]))
                            - Math.min(trkZ0V[0], Math.min(trkZ0V[1], trkZ0V[2]));
            double[] z0ErrV = new double[3];
            for (int i = 0; i < 3; i++) {
                double[] cov = ts[i].getCovMatrix();
                z0ErrV[i] = (cov != null && cov.length > 9) ? Math.sqrt(Math.abs(cov[9])) : -1;
            }

            // tanLambda diagnostics
            double avgTanL = (trkTanLV[0] + trkTanLV[1] + trkTanLV[2]) / 3.0;
            double tanLSpread = Math.max(trkTanLV[0], Math.max(trkTanLV[1], trkTanLV[2]))
                              - Math.min(trkTanLV[0], Math.min(trkTanLV[1], trkTanLV[2]));

            // Momenta in beam frame
            Hep3Vector[] inMom = new Hep3Vector[3];
            double[] inP = new double[3];
            for (int i = 0; i < 3; i++) {
                inMom[i] = rotateToBeamFrame(parts[i].getMomentum());
                inP[i] = inMom[i].magnitude();
            }
            Hep3Vector totInP = VecOp.add(inMom[0], VecOp.add(inMom[1], inMom[2]));
            double magPSum = totInP.magnitude();
            double sumPMag = inP[0] + inP[1] + inP[2];

            // Delta phi
            double dPhi1 = Math.atan2(inMom[0].y()+inMom[1].y(), inMom[0].x()+inMom[1].x())
                         - Math.atan2(inMom[2].y(), inMom[2].x());
            double dPhi2 = Math.atan2(inMom[2].y()+inMom[1].y(), inMom[2].x()+inMom[1].x())
                         - Math.atan2(inMom[0].y(), inMom[0].x());
            double closestDPhi = Math.abs(dPhi1-Math.PI) < Math.abs(dPhi2-Math.PI) ? dPhi1 : dPhi2;

            // Track times
            double[] trkT = new double[3];
            for (int i = 0; i < 3; i++) trkT[i] = avgHitTime(trks[i]);

            // Cluster times
            boolean[] hasCl = new boolean[3];
            double[] clT = new double[3];
            for (int i = 0; i < 3; i++) {
                hasCl[i] = parts[i].getClusters() != null && !parts[i].getClusters().isEmpty();
                if (hasCl[i]) clT[i] = ClusterUtilities.getSeedHitTime(parts[i].getClusters().get(0));
            }
            double tcOff = cuts.getTrackClusterTimeOffset();

            // Vertex info
            Hep3Vector vPos = vtx.getPosition();
            double vChi2 = vtx.getChi2(), vProb = vtx.getProbability();
            Map<String, Double> vp = vtx.getParameters();
            boolean hasInvM = vp.containsKey("invMass");
            double invM = hasInvM ? vp.get("invMass") : 0;

            // Fitted momenta
            double totFitPx = 0, totFitPy = 0, totFitPz = 0, sumFitPMag = 0;
            double[] fitPMag = new double[3];
            Hep3Vector[] fitMom = new Hep3Vector[3];
            boolean hasFit = vp.containsKey("p1X") && vp.containsKey("p2X") && vp.containsKey("p3X");
            if (hasFit) {
                String[] pl = {"p1", "p2", "p3"};
                for (int i = 0; i < 3; i++) {
                    double px = vp.get(pl[i]+"X"), py = vp.get(pl[i]+"Y"), pz = vp.get(pl[i]+"Z");
                    fitMom[i] = rotateToBeamFrame(new BasicHep3Vector(px, py, pz));
                    fitPMag[i] = fitMom[i].magnitude();
                    sumFitPMag += fitPMag[i];
                    totFitPx += fitMom[i].x(); totFitPy += fitMom[i].y(); totFitPz += fitMom[i].z();
                }
            }
            double totFitP = Math.sqrt(totFitPx*totFitPx + totFitPy*totFitPy + totFitPz*totFitPz);

            // Residuals and pulls
            double[][] resP = new double[3][4];
            double[][] pullP = new double[3][4];
            boolean[] pullOk = new boolean[3];
            double bField = 1.0, beamE = 3.7;
            if (hasFit) {
                for (int i = 0; i < 3; i++) {
                    resP[i][0] = fitMom[i].x() - inMom[i].x();
                    resP[i][1] = fitMom[i].y() - inMom[i].y();
                    resP[i][2] = fitMom[i].z() - inMom[i].z();
                    resP[i][3] = fitMom[i].magnitude() - inMom[i].magnitude();
                    double[] err = computeMomentumErrors(ts[i], bField);
                    pullOk[i] = true;
                    for (int j = 0; j < 4; j++) {
                        if (err[j] > 1e-6) pullP[i][j] = resP[i][j] / err[j];
                        else pullOk[i] = false;
                    }
                }
            }

            // ---- Fill: NoCut always, Cut only if probability passes ----
            HistSet[] sets = {noCutHS, cutHS};
            boolean[] doFill = {true, vProb >= 0.01};

            for (int si = 0; si < 2; si++) {
                if (!doFill[si]) continue;
                HistSet h = sets[si];

                // Track params
                for (int i = 0; i < 3; i++) {
                    h.trkP[i].fill(trkPMag[i]); h.trkPt[i].fill(trkPtV[i]); h.trkD0[i].fill(trkD0V[i]);
                    h.trkPhi0[i].fill(trkPhi0V[i]); h.trkOmega[i].fill(trkOmV[i]); h.trkZ0[i].fill(trkZ0V[i]);
                    h.trkTanL[i].fill(trkTanLV[i]); h.trkChi2[i].fill(trkChi2V[i]);
                }

                // Z0 and tanLambda diagnostics
                h.avgZ0h.fill(avgZ0); h.z0Sph.fill(z0Spread);
                for (int i = 0; i < 3; i++) if (z0ErrV[i] >= 0) h.z0Err[i].fill(z0ErrV[i]);
                h.avgTanLh.fill(avgTanL); h.tanLSph.fill(tanLSpread);

                // Input momenta
                for (int i = 0; i < 3; i++) h.partP[i].fill(inP[i]);
                h.totPIn.fill(magPSum);
                h.totPxIn.fill(totInP.x()); h.totPyIn.fill(totInP.y()); h.totPzIn.fill(totInP.z());
                h.sumPMagIn.fill(sumPMag); h.magPSumIn.fill(magPSum); h.pMagDiffIn.fill(sumPMag - magPSum);

                // Delta phi
                h.dPhiXZ1.fill(dPhi1); h.dPhiXZ2.fill(dPhi2); h.dPhiXZMin.fill(closestDPhi);

                // Track time diffs
                h.trkDtEP.fill(trkT[0]-trkT[1]); h.trkDtER.fill(trkT[0]-trkT[2]); h.trkDtPR.fill(trkT[1]-trkT[2]);

                // Cluster time diffs
                if (hasCl[0] && hasCl[1]) {
                    h.clusDtEP.fill(clT[0]-clT[1]);
                    if (hasCl[2]) { h.clusDtER.fill(clT[0]-clT[2]); h.clusDtPR.fill(clT[1]-clT[2]); }
                } else if (hasCl[1] && hasCl[2]) {
                    h.clusDtPR.fill(clT[1]-clT[2]);
                }

                // Track-cluster time diffs
                for (int i = 0; i < 3; i++) if (hasCl[i]) h.trkClusDt[i].fill(trkT[i]-clT[i]+tcOff);
                if (hasCl[1]) for (int i = 0; i < 3; i++) h.trkMinPosClT[i].fill(trkT[i]-clT[1]+tcOff);

                // Vertex
                h.vtxX.fill(vPos.x()); h.vtxY.fill(vPos.y()); h.vtxZ.fill(vPos.z());
                h.vtxChi2.fill(vChi2); h.vtxProb.fill(vProb);
                h.vtxYmAvgZ0.fill(vPos.y()-avgZ0);
                h.vtxYvAvgZ0.fill(avgZ0, vPos.y());
                h.vtxYvTanLSp.fill(tanLSpread, vPos.y());
                h.vtxYvChi2.fill(vChi2, vPos.y());
                if (hasInvM) { h.invMass.fill(invM); h.invMvChi2.fill(invM, vChi2); }

                // Fitted momenta, residuals, pulls, correlations
                if (hasFit) {
                    for (int i = 0; i < 3; i++) h.fitP[i].fill(fitPMag[i]);
                    h.totPFit.fill(totFitP);
                    h.totPxFit.fill(totFitPx); h.totPyFit.fill(totFitPy); h.totPzFit.fill(totFitPz);
                    h.sumPMagFit.fill(sumFitPMag); h.magPSumFit.fill(totFitP);
                    h.pMagDiffFit.fill(sumFitPMag - totFitP);

                    for (int i = 0; i < 3; i++) {
                        h.resPx[i].fill(resP[i][0]); h.resPy[i].fill(resP[i][1]);
                        h.resPz[i].fill(resP[i][2]); h.resPtot[i].fill(resP[i][3]);
                    }
                    h.resTotPx.fill(totFitPx); h.resTotPy.fill(totFitPy);
                    h.resTotPz.fill(totFitPz - beamE); h.resTotP.fill(totFitP - beamE);

                    for (int i = 0; i < 3; i++) if (pullOk[i]) {
                        h.pullPx[i].fill(pullP[i][0]); h.pullPy[i].fill(pullP[i][1]);
                        h.pullPz[i].fill(pullP[i][2]); h.pullPtot[i].fill(pullP[i][3]);
                    }

                    h.vtxXvsZ.fill(vPos.z(), vPos.x());
                    h.p1vP2.fill(fitPMag[0], fitPMag[1]);
                    h.p1vP3.fill(fitPMag[0], fitPMag[2]);
                    h.p2vP3.fill(fitPMag[1], fitPMag[2]);
                }
            }
        }
    }

    private double avgHitTime(Track track) {
        List<TrackerHit> hits = track.getTrackerHits();
        if (hits == null || hits.isEmpty()) return 0.0;
        double sum = 0.0;
        for (TrackerHit hit : hits) sum += hit.getTime();
        return sum / hits.size();
    }

    /**
     * Compute momentum errors (sigma_px, sigma_py, sigma_pz, sigma_ptot) from track covariance,
     * transformed to the BEAM FRAME.
     */
    private double[] computeMomentumErrors(TrackState ts, double bField) {
        double[] params = ts.getParameters();
        double[] cov = ts.getCovMatrix();
        if (cov == null || cov.length < 15)
            return new double[]{0.1, 0.1, 0.1, 0.1};

        double phi0 = params[1], omega = params[2], tanLambda = params[4];
        double c = 2.99792458e-4;
        double pT = c * bField / Math.abs(omega);
        double px_trk = pT * Math.cos(phi0), py_trk = pT * Math.sin(phi0), pz_trk = pT * tanLambda;

        // Jacobian of (px,py,pz)_tracking w.r.t. (d0,phi0,omega,z0,tanLambda)
        double[][] J = new double[3][5];
        J[0][1] = -py_trk;         J[0][2] = -px_trk / omega;
        J[1][1] = px_trk;          J[1][2] = -py_trk / omega;
        J[2][2] = -pz_trk / omega; J[2][4] = pT;

        // Track covariance (lower triangular)
        double[][] C = new double[5][5];
        for (int i = 0; i < 5; i++)
            for (int j = 0; j <= i; j++) {
                C[i][j] = cov[i*(i+1)/2 + j];
                C[j][i] = C[i][j];
            }

        // pCov_trk = J * C * J^T
        double[][] pCov = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 5; k++)
                    for (int l = 0; l < 5; l++)
                        pCov[i][j] += J[i][k] * C[k][l] * J[j][l];

        // Tracking -> detector: px_det=py_trk, py_det=pz_trk, pz_det=px_trk
        double[][] T = {{0,1,0},{0,0,1},{1,0,0}};
        double[][] pCovD = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    for (int l = 0; l < 3; l++)
                        pCovD[i][j] += T[i][k] * pCov[k][l] * T[j][l];

        // Detector -> beam frame (rotation about y)
        double cosR = Math.cos(beamRotationY), sinR = Math.sin(beamRotationY);
        double[][] R = {{cosR,0,sinR},{0,1,0},{-sinR,0,cosR}};
        double[][] pCovB = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    for (int l = 0; l < 3; l++)
                        pCovB[i][j] += R[i][k] * pCovD[k][l] * R[j][l];

        double sigmaPx = Math.sqrt(Math.abs(pCovB[0][0]));
        double sigmaPy = Math.sqrt(Math.abs(pCovB[1][1]));
        double sigmaPz = Math.sqrt(Math.abs(pCovB[2][2]));

        // ptot error via error propagation
        double px_det = py_trk, py_det = pz_trk, pz_det = px_trk;
        double px_b = px_det*cosR + pz_det*sinR, py_b = py_det, pz_b = -px_det*sinR + pz_det*cosR;
        double ptot = Math.sqrt(px_b*px_b + py_b*py_b + pz_b*pz_b);
        double sigmaPtotSq = 0;
        double[] pV = {px_b, py_b, pz_b};
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                sigmaPtotSq += (pV[i]/ptot) * pCovB[i][j] * (pV[j]/ptot);

        return new double[]{sigmaPx, sigmaPy, sigmaPz, Math.sqrt(Math.abs(sigmaPtotSq))};
    }
}
