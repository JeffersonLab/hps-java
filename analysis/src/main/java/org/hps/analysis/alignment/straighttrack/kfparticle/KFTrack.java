package org.hps.analysis.alignment.straighttrack.kfparticle;

import java.util.Arrays;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class KFTrack {
// constructor

    public KFTrack(double[] par, double[] cov, double mass, double chi2, boolean isElectron, int NDF) {
        System.arraycopy(par, 0, fT, 0, 6);
        System.arraycopy(cov, 0, fC, 0, 15);
        fMass = mass;
        fChi2 = chi2;
        fIsElectron = isElectron;
        fNDF = NDF;
    }
//accessor methods

    public double[] GetTrack() {
        return fT;
    }

    public double[] GetCovMatrix() {
        return fC;
    }

    public double GetRefChi2() {
        return fChi2;
    }

    public int GetRefNDF() {
        return fNDF;
    }

    public double GetMass() {
        return fMass;
    }

    public boolean IsElectron() {
        return fIsElectron;
    }

    //TODO add convenience methods for track parameters?
    public String toString() {
        StringBuffer sb = new StringBuffer("KFTrack: ");
        sb.append("chisq: " + fChi2 + " ndf: " + fNDF + " mass: " + fMass + " is" + (!fIsElectron ? " not" : "") + " an electron \n");
        sb.append(Arrays.toString(fT) + "\n");
        sb.append(Arrays.toString(fC) + "\n");
        return sb.toString();
    }

    //TODO provide ENUMS for these parameters
    // Track parameters are:
    // 0 x
    // 1 y
    // 2 x' (dx/dz)
    // 3 y' (dy/dz)
    // 4 q/p
    // 5 z
    private double[] fT = new double[6];
    private double[] fC = new double[15];
    private double fMass;
    private double fChi2;
    private boolean fIsElectron;
    private int fNDF;
}
