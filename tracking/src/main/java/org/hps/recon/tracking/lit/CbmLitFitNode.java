package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitFitNode {

    public CbmLitFitNode() {
        fF = new double[25];
    }

    /*
     * Getters
     */
    public double[] GetF() {
        return fF;
    }

    public CbmLitTrackParam GetPredictedParam() {
        return new CbmLitTrackParam(fPredictedParam);
    }

    public CbmLitTrackParam GetUpdatedParam() {
        return new CbmLitTrackParam(fUpdatedParam);
    }

    public CbmLitTrackParam GetSmoothedParam() {
        return new CbmLitTrackParam(fSmoothedParam);
    }

    public double GetChiSqFiltered() {
        return fChiSqFiltered;
    }

    public double GetChiSqSmoothed() {
        return fChiSqSmoothed;
    }

    /*
     * Setters
     */
    public void SetF(double[] F) {
        System.arraycopy(F, 0, fF, 0, F.length);
    }

    public void SetPredictedParam(CbmLitTrackParam par) {
        fPredictedParam = new CbmLitTrackParam(par);
    }

    public void SetUpdatedParam(CbmLitTrackParam par) {
        fUpdatedParam = new CbmLitTrackParam(par);
    }

    public void SetSmoothedParam(CbmLitTrackParam par) {
        fSmoothedParam = new CbmLitTrackParam(par);
    }

    public void SetChiSqFiltered(double chiSq) {
        fChiSqFiltered = chiSq;
    }

    public void SetChiSqSmoothed(double chiSq) {
        fChiSqSmoothed = chiSq;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FitNode: ChiSqFiltered:" + fChiSqFiltered + ", ChiSqSmoothed: " + fChiSqSmoothed + "\n"
                + " predictedParam " + fPredictedParam + "\n"
                + " updatedParam   " + fUpdatedParam + "\n"
                + " smoothedParam  " + fSmoothedParam + "\n");
        return sb.toString();
    }
    double[] fF; // Transport matrix.
    CbmLitTrackParam fPredictedParam; // Predicted track parameters.
    CbmLitTrackParam fUpdatedParam; // Updated with KF track parameters.
    CbmLitTrackParam fSmoothedParam; // Smoothed track parameters.
    double fChiSqFiltered; // Contribution to chi-square of updated track parameters and hit.
    double fChiSqSmoothed; // Contribution to chi-square of smoothed track parameters and hit.   
}
