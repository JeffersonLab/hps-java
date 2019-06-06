package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class PixelHit extends Hit {

    public PixelHit() {
        SetHitType(HitType.PIXELHIT);
    }

    /*
     * Getters
     */
    double GetX() {
        return fX;
    }

    double GetY() {
        return fY;
    }

    double GetDx() {
        return fDx;
    }

    double GetDy() {
        return fDy;
    }

    double GetDxy() {
        return fDxy;
    }

    /*
     * Setters
     */
    void SetX(double x) {
        fX = x;
    }

    void SetY(double y) {
        fY = y;
    }

    void SetDx(double dx) {
        fDx = dx;
    }

    void SetDy(double dy) {
        fDy = dy;
    }

    void SetDxy(double dxy) {
        fDxy = dxy;
    }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("PixelHit: pos=(" + GetX() + "," + GetY() + "," + GetZ()
                + ") err=(" + GetDx() + "," + GetDy() + "," + GetDz() + ") "
                + " dxy=" + GetDxy()
                + " refId=" + GetRefId()
                + " hitType=" + GetType()
                //         + " detId=" + GetSystem()
                + " stationGroup=" + GetStationGroup()
                + " station=" + GetStation()
                + " substation=" + GetSubstation()
                + " module=" + GetModule() + "\n");
        return ss.toString();
    }
    double fX, fY; // X and Y hit positions in [cm].
    double fDx, fDy; // X and Y hit position errors in [cm].
    double fDxy; // XY covariance.    
}
