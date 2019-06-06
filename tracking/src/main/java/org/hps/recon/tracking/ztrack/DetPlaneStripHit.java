package org.hps.recon.tracking.ztrack;

import static java.lang.Math.sin;
import static java.lang.Math.cos;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class DetPlaneStripHit extends Hit implements Comparable {

    public DetPlaneStripHit(DetectorPlane p) {
        SetHitType(HitType.STRIPHIT);
        _detPlane = p;
    }

    public DetPlaneStripHit(DetectorPlane p, double u, double du) {
        SetHitType(HitType.STRIPHIT);
        _detPlane = p;
        fU = u;
        fDu = du;
        SetPhi(p.phi());
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

    public double GetZ() {
        return _detPlane.GetZpos();
    }

    public DetectorPlane GetPlane() {
        return _detPlane;
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

    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        DetectorPlane that = ((DetPlaneStripHit) o).GetPlane();
        return _detPlane.compareTo(that);
    }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("DetPlaneStripHit: pos=(" + GetU() + "," + GetZ()
                + ") err=(" + GetDu() + "," + GetDz() + ") "
                + " phi=" + GetPhi()
                + " cosPhi=" + GetCosPhi() + " sinPhi=" + GetSinPhi()
                + " refId=" + GetRefId()
                + " hitType=" + GetType()
                + " stationGroup=" + GetStationGroup()
                + " station=" + GetStation()
                + " substation=" + GetSubstation()
                + " module=" + GetModule()
                + " det plane=" + _detPlane
                + "\n");
        return ss.toString();
    }
    double fU; // U measurement of the hit in [cm].
    double fDu; // U measurement error in [cm].
    double fPhi; // Strip rotation angle in [rad].
    double fCosPhi; // Cosine of strip rotation angle.
    double fSinPhi; // Sine of strip rotation angle.
    int fSegment; // Up or down segment of straw tube.
    DetectorPlane _detPlane; // the detector plane at which this hit is defined.
}
