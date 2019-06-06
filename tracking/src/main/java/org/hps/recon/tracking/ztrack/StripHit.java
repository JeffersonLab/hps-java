package org.hps.recon.tracking.ztrack;

import static java.lang.Math.sin;
import static java.lang.Math.cos;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripHit extends Hit {

    public StripHit() {
        SetHitType(HitType.STRIPHIT);
    }

    /*
     * Getters
     */
    public double GetU() {
        return fU;
    }

    public double GetDu() {
        return fDu;
    }

    public double GetPhi() {
        return fPhi;
    }

    public double GetCosPhi() {
        return fCosPhi;
    }

    public double GetSinPhi() {
        return fSinPhi;
    }

    public int GetSegment() {
        return fSegment;
    }

    /*
     * Setters
     */
    void SetU(double u) {
        fU = u;
    }

    void SetDu(double du) {
        fDu = du;
    }

    void SetPhi(double phi) {
        fPhi = phi;
        fCosPhi = cos(phi);
        fSinPhi = sin(phi);
    }

//    void SetCosPhi(double cosPhi)
//    {
//        fCosPhi = cosPhi;
//    }
//
//    void SetSinPhi(double sinPhi)
//    {
//        fSinPhi = sinPhi;
//    }
    void SetSegment(int segment) {
        fSegment = segment;
    }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("StripHit: pos=(" + GetU() + "," + GetZ()
                + ") err=(" + GetDu() + "," + GetDz() + ") "
                + " phi=" + GetPhi()
                + " cosPhi=" + GetCosPhi() + " sinPhi=" + GetSinPhi()
                + " refId=" + GetRefId()
                + " hitType=" + GetType()
                + " stationGroup=" + GetStationGroup()
                + " station=" + GetStation()
                + " substation=" + GetSubstation()
                + " module=" + GetModule() + "\n");
        return ss.toString();
    }
    double fU; // U measurement of the hit in [cm].
    double fDu; // U measurement error in [cm].
    double fPhi; // Strip rotation angle in [rad].
    double fCosPhi; // Cosine of strip rotation angle.
    double fSinPhi; // Sine of strip rotation angle.
    int fSegment; // Up or down segment of straw tube.    
}
