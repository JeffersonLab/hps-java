package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class FitNode {

    public FitNode() {
        fF = new double[25];
    }

    /*
     * Getters
     */
    public double[] GetF() {
        return fF;
    }

    public ZTrackParam GetPredictedParam() {
        return new ZTrackParam(fPredictedParam);
    }

    public ZTrackParam GetUpdatedParam() {
        return new ZTrackParam(fUpdatedParam);
    }

    public ZTrackParam GetSmoothedParam() {
        return new ZTrackParam(fSmoothedParam);
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

    public void SetPredictedParam(ZTrackParam par) {
        fPredictedParam = new ZTrackParam(par);
    }

    public void SetUpdatedParam(ZTrackParam par) {
        fUpdatedParam = new ZTrackParam(par);
    }

    public void SetSmoothedParam(ZTrackParam par) {
        fSmoothedParam = new ZTrackParam(par);
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
    ZTrackParam fPredictedParam; // Predicted track parameters.
    ZTrackParam fUpdatedParam; // Updated with KF track parameters.
    ZTrackParam fSmoothedParam; // Smoothed track parameters.
    double fChiSqFiltered; // Contribution to chi-square of updated track parameters and hit.
    double fChiSqSmoothed; // Contribution to chi-square of smoothed track parameters and hit.   
}
