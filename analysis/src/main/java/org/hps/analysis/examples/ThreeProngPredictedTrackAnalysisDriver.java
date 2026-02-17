package org.hps.analysis.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Vertex;

import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis driver for three-prong predicted track fits.
 *
 * For each three-prong vertex, we fit with N-1 tracks and predict the Nth track's
 * momentum using momentum conservation. This driver plots the residuals between
 * predicted and actual momenta, angle residuals, and fit quality metrics.
 *
 * Two sets of histograms are produced per particle: one with a vertex probability
 * cut (under particle name) and one without (under particle name + "_NoCut").
 */
public class ThreeProngPredictedTrackAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    private String threeProngPredictedEleColName = "ThreeProngPredictedEle";
    private String threeProngPredictedPosColName = "ThreeProngPredictedPos";
    private String threeProngPredictedRecColName = "ThreeProngPredictedRec";
    private double beamRotationY = -0.0305;

    private IHistogram1D hNCandidatesEle, hNCandidatesPos, hNCandidatesRec;

    /** Holds a complete set of histograms for one particle/cut combination. */
    private static class HistSet {
        IHistogram1D resPx, resPy, resPz, resPtot;
        IHistogram1D predPx, predPy, predPz, predPtot;
        IHistogram1D actPx, actPy, actPz, actPtot;
        IHistogram1D resTanL, resPhi, chi2, prob;
        IHistogram1D vtxX, vtxY, vtxZ;
        IHistogram1D pullPx, pullPy, pullPz, pullPtot;
        IHistogram2D resVsP, resVsChi2;
        // 6 residuals x 3 kinematics: index = r*3+k
        // r: 0=Px,1=Py,2=Pz,3=Ptot,4=tanL,5=phi; k: 0=vsP,1=vsTanL,2=vsPhi
        IHistogram2D[] resVsKin = new IHistogram2D[18];
    }

    private Map<String, HistSet> hists = new HashMap<>();

    public void setThreeProngPredictedEleColName(String s) { threeProngPredictedEleColName = s; }
    public void setThreeProngPredictedPosColName(String s) { threeProngPredictedPosColName = s; }
    public void setThreeProngPredictedRecColName(String s) { threeProngPredictedRecColName = s; }
    public void setBeamRotationY(double angle) { beamRotationY = angle; }

    private Hep3Vector rotateToBeamFrame(Hep3Vector v) {
        double cosTheta = Math.cos(beamRotationY);
        double sinTheta = Math.sin(beamRotationY);
        return new BasicHep3Vector(
                v.x() * cosTheta + v.z() * sinTheta, v.y(), -v.x() * sinTheta + v.z() * cosTheta);
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        aida.tree().mkdir("/ThreeProngPredicted");
        aida.tree().cd("/ThreeProngPredicted");

        hNCandidatesEle = aida.histogram1D("N predicted ele vertices", 20, 0, 20);
        hNCandidatesPos = aida.histogram1D("N predicted pos vertices", 20, 0, 20);
        hNCandidatesRec = aida.histogram1D("N predicted rec vertices", 20, 0, 20);

        for (String p : new String[]{"Electron", "Positron", "Recoil"}) {
            hists.put(p, createHistSet("/ThreeProngPredicted/" + p, p));
            hists.put(p + "_NoCut", createHistSet("/ThreeProngPredicted/" + p + "_NoCut", p + " NoCut"));
        }
    }

    private HistSet createHistSet(String dir, String p) {
        HistSet h = new HistSet();
        double pxR = 0.2, pyR = 0.2;
        double pxResR = 0.1, pyResR = 0.1, pzResR = 0.5;
        double chi2max = 50;

        aida.tree().mkdir(dir);

        aida.tree().mkdir(dir + "/MomentumResiduals");
        aida.tree().cd(dir + "/MomentumResiduals");
        h.resPx = aida.histogram1D(p + " predicted - actual Px", 200, -pxResR, pxResR);
        h.resPy = aida.histogram1D(p + " predicted - actual Py", 200, -pyResR, pyResR);
        h.resPz = aida.histogram1D(p + " predicted - actual Pz", 200, -pzResR, pzResR);
        h.resPtot = aida.histogram1D(p + " predicted - actual |P|", 200, -pzResR, pzResR);

        aida.tree().mkdir(dir + "/PredictedMomentum");
        aida.tree().cd(dir + "/PredictedMomentum");
        h.predPx = aida.histogram1D(p + " predicted Px", 200, -pxR, pxR);
        h.predPy = aida.histogram1D(p + " predicted Py", 200, -pyR, pyR);
        h.predPz = aida.histogram1D(p + " predicted Pz", 200, 0, 5.0);
        h.predPtot = aida.histogram1D(p + " predicted |P|", 200, 0, 5.0);

        aida.tree().mkdir(dir + "/ActualMomentum");
        aida.tree().cd(dir + "/ActualMomentum");
        h.actPx = aida.histogram1D(p + " actual Px", 200, -pxR, pxR);
        h.actPy = aida.histogram1D(p + " actual Py", 200, -pyR, pyR);
        h.actPz = aida.histogram1D(p + " actual Pz", 200, 0, 5.0);
        h.actPtot = aida.histogram1D(p + " actual |P|", 200, 0, 5.0);

        aida.tree().mkdir(dir + "/AngleResiduals");
        aida.tree().cd(dir + "/AngleResiduals");
        h.resTanL = aida.histogram1D(p + " tanLambda residual", 200, -0.05, 0.05);
        h.resPhi = aida.histogram1D(p + " phi residual", 200, -0.1, 0.1);

        aida.tree().mkdir(dir + "/FitQuality");
        aida.tree().cd(dir + "/FitQuality");
        h.chi2 = aida.histogram1D(p + " predicted fit chi2", 200, 0, chi2max);
        h.prob = aida.histogram1D(p + " predicted fit probability", 200, 0, 1.0);

        aida.tree().mkdir(dir + "/Vertex");
        aida.tree().cd(dir + "/Vertex");
        h.vtxX = aida.histogram1D(p + " vertex X", 200, -0.5, 0.5);
        h.vtxY = aida.histogram1D(p + " vertex Y", 200, -0.5, 0.5);
        h.vtxZ = aida.histogram1D(p + " vertex Z", 200, -10.0, 10.0);

        aida.tree().mkdir(dir + "/Pulls");
        aida.tree().cd(dir + "/Pulls");
        h.pullPx = aida.histogram1D(p + " pull Px", 200, -5.0, 5.0);
        h.pullPy = aida.histogram1D(p + " pull Py", 200, -5.0, 5.0);
        h.pullPz = aida.histogram1D(p + " pull Pz", 200, -5.0, 5.0);
        h.pullPtot = aida.histogram1D(p + " pull |P|", 200, -5.0, 5.0);

        aida.tree().mkdir(dir + "/Correlations");
        aida.tree().cd(dir + "/Correlations");
        h.resVsP = aida.histogram2D(p + " |P| residual vs actual |P|", 100, 0, 5.0, 100, -0.5, 0.5);
        h.resVsChi2 = aida.histogram2D(p + " |P| residual vs chi2", 100, 0, chi2max, 100, -0.5, 0.5);

        aida.tree().mkdir(dir + "/ResidualVsKinematics");
        aida.tree().cd(dir + "/ResidualVsKinematics");
        String[] resN = {"Px", "Py", "Pz", "|P|", "tanLambda", "phi"};
        double[] resLo = {-pxResR, -pyResR, -pzResR, -pzResR, -0.05, -0.1};
        double[] resHi = { pxResR,  pyResR,  pzResR,  pzResR,  0.05,  0.1};
        String[] kinN = {"|P|", "tanLambda", "phi"};
        double[] kinLo = {0, -0.15, -0.2};
        double[] kinHi = {5.0, 0.15, 0.2};
        for (int r = 0; r < 6; r++)
            for (int k = 0; k < 3; k++)
                h.resVsKin[r * 3 + k] = aida.histogram2D(
                        p + " " + resN[r] + " residual vs " + kinN[k],
                        100, kinLo[k], kinHi[k], 100, resLo[r], resHi[r]);

        return h;
    }

    @Override
    protected void process(EventHeader event) {
        if (event.hasCollection(Vertex.class, threeProngPredictedEleColName)) {
            List<Vertex> vertices = event.get(Vertex.class, threeProngPredictedEleColName);
            hNCandidatesEle.fill(vertices.size());
            for (Vertex vtx : vertices) fillPredictedHistograms(vtx, "Electron");
        }
        if (event.hasCollection(Vertex.class, threeProngPredictedPosColName)) {
            List<Vertex> vertices = event.get(Vertex.class, threeProngPredictedPosColName);
            hNCandidatesPos.fill(vertices.size());
            for (Vertex vtx : vertices) fillPredictedHistograms(vtx, "Positron");
        }
        if (event.hasCollection(Vertex.class, threeProngPredictedRecColName)) {
            List<Vertex> vertices = event.get(Vertex.class, threeProngPredictedRecColName);
            hNCandidatesRec.fill(vertices.size());
            for (Vertex vtx : vertices) fillPredictedHistograms(vtx, "Recoil");
        }
    }

    private void fillPredictedHistograms(Vertex vtx, String particle) {
        if (vtx == null) return;

        Map<String, Double> params = vtx.getParameters();
        Double predPx = params.get("predictedPx"), predPy = params.get("predictedPy"), predPz = params.get("predictedPz");
        Double actPx = params.get("actualPx"), actPy = params.get("actualPy"), actPz = params.get("actualPz");
        Double resPx = params.get("residualPx"), resPy = params.get("residualPy"), resPz = params.get("residualPz");
        if (predPx == null || actPx == null) return;

        // Covariances
        Double predCovXX = params.get("predictedMomCovXX"), predCovXY = params.get("predictedMomCovXY"),
               predCovXZ = params.get("predictedMomCovXZ"), predCovYY = params.get("predictedMomCovYY"),
               predCovYZ = params.get("predictedMomCovYZ"), predCovZZ = params.get("predictedMomCovZZ");
        Double actCovXX = params.get("actualMomCovXX"), actCovXY = params.get("actualMomCovXY"),
               actCovXZ = params.get("actualMomCovXZ"), actCovYY = params.get("actualMomCovYY"),
               actCovYZ = params.get("actualMomCovYZ"), actCovZZ = params.get("actualMomCovZZ");
        boolean hasCov = (predCovXX != null && actCovXX != null);

        double predPtot = Math.sqrt(predPx * predPx + predPy * predPy + predPz * predPz);
        double actPtot = Math.sqrt(actPx * actPx + actPy * actPy + actPz * actPz);
        double resPtot = predPtot - actPtot;
        double actTanL = actPy / Math.sqrt(actPx * actPx + actPz * actPz);
        double actPhi = Math.atan2(actPx, actPz);
        double predTanL = predPy / Math.sqrt(predPx * predPx + predPz * predPz);
        double resTanL = predTanL - actTanL;
        double resPhi = Math.atan2(predPx, predPz) - actPhi;
        while (resPhi > Math.PI) resPhi -= 2 * Math.PI;
        while (resPhi < -Math.PI) resPhi += 2 * Math.PI;

        // Pulls
        double pullPx = 0, pullPy = 0, pullPz = 0, pullPtot = 0;
        boolean pullsValid = false;
        if (hasCov) {
            double vXX = predCovXX + actCovXX, vYY = predCovYY + actCovYY, vZZ = predCovZZ + actCovZZ;
            if (vXX > 0 && vYY > 0 && vZZ > 0) {
                pullPx = resPx / Math.sqrt(vXX);
                pullPy = resPy / Math.sqrt(vYY);
                pullPz = resPz / Math.sqrt(vZZ);
                double varPredP = predPtot > 0 ? (predPx * predPx * predCovXX + predPy * predPy * predCovYY
                        + predPz * predPz * predCovZZ + 2 * predPx * predPy * predCovXY
                        + 2 * predPx * predPz * predCovXZ + 2 * predPy * predPz * predCovYZ) / (predPtot * predPtot) : 0;
                double varActP = actPtot > 0 ? (actPx * actPx * actCovXX + actPy * actPy * actCovYY
                        + actPz * actPz * actCovZZ + 2 * actPx * actPy * actCovXY
                        + 2 * actPx * actPz * actCovXZ + 2 * actPy * actPz * actCovYZ) / (actPtot * actPtot) : 0;
                if (varPredP + varActP > 0) {
                    pullPtot = resPtot / Math.sqrt(varPredP + varActP);
                    pullsValid = true;
                }
            }
        }

        double chi2Val = vtx.getChi2();
        Double ndfParam = params.get("ndf");
        int ndf = (ndfParam != null) ? ndfParam.intValue() : 6;
        double probVal = org.apache.commons.math3.special.Gamma.regularizedGammaQ(ndf / 2.0, chi2Val / 2.0);
        double vtxX = vtx.getPosition().x(), vtxY = vtx.getPosition().y(), vtxZ = vtx.getPosition().z();
        double[] resVals = {resPx, resPy, resPz, resPtot, resTanL, resPhi};
        double[] kinVals = {actPtot, actTanL, actPhi};

        // Always fill no-cut histograms
        fillHistSet(hists.get(particle + "_NoCut"), predPx, predPy, predPz, predPtot,
                actPx, actPy, actPz, actPtot, resPx, resPy, resPz, resPtot,
                resTanL, resPhi, chi2Val, probVal, vtxX, vtxY, vtxZ,
                pullPx, pullPy, pullPz, pullPtot, pullsValid, resVals, kinVals);

        // Apply vertex probability cut
        double vtxProb = vtx.getProbability();
        System.out.println("ThreeProngPredicted::  vtxProb = " + vtxProb);
        if (vtxProb < 0.01) return;

        // Fill with-cut histograms
        fillHistSet(hists.get(particle), predPx, predPy, predPz, predPtot,
                actPx, actPy, actPz, actPtot, resPx, resPy, resPz, resPtot,
                resTanL, resPhi, chi2Val, probVal, vtxX, vtxY, vtxZ,
                pullPx, pullPy, pullPz, pullPtot, pullsValid, resVals, kinVals);
    }

    private void fillHistSet(HistSet h, double predPx, double predPy, double predPz, double predPtot,
            double actPx, double actPy, double actPz, double actPtot,
            double resPx, double resPy, double resPz, double resPtot,
            double resTanL, double resPhi, double chi2, double prob,
            double vtxX, double vtxY, double vtxZ,
            double pullPx, double pullPy, double pullPz, double pullPtot, boolean pullsValid,
            double[] resVals, double[] kinVals) {
        h.resPx.fill(resPx); h.resPy.fill(resPy); h.resPz.fill(resPz); h.resPtot.fill(resPtot);
        h.predPx.fill(predPx); h.predPy.fill(predPy); h.predPz.fill(predPz); h.predPtot.fill(predPtot);
        h.actPx.fill(actPx); h.actPy.fill(actPy); h.actPz.fill(actPz); h.actPtot.fill(actPtot);
        h.resTanL.fill(resTanL); h.resPhi.fill(resPhi);
        h.chi2.fill(chi2); h.prob.fill(prob);
        h.vtxX.fill(vtxX); h.vtxY.fill(vtxY); h.vtxZ.fill(vtxZ);
        h.resVsP.fill(actPtot, resPtot); h.resVsChi2.fill(chi2, resPtot);
        for (int r = 0; r < 6; r++)
            for (int k = 0; k < 3; k++)
                h.resVsKin[r * 3 + k].fill(kinVals[k], resVals[r]);
        if (pullsValid) {
            h.pullPx.fill(pullPx); h.pullPy.fill(pullPy); h.pullPz.fill(pullPz); h.pullPtot.fill(pullPtot);
        }
    }
}
