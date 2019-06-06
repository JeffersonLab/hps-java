package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmVertex {

    /**
     * Position coordinates [cm] *
     */
    double fX, fY, fZ;

    /**
     * Chi2 of vertex fit *
     */
    double fChi2;

    /**
     * Number of degrees of freedom of vertex fit *
     */
    int fNDF;

    /**
     * Number of tracks used for the vertex fit *
     */
    int fNTracks;

    /**
     * Covariance matrix for x, y, and z stored in an array. The * sequence is
     * a[0,0], a[0,1], a[0,2], a[1,1], a[1,2], a[2,2]
   *
     */
    double[] fCovMatrix = new double[6];

    double GetRefX() {
        return fX;
    }

    double GetRefY() {
        return fY;
    }

    double GetRefZ() {
        return fZ;
    }

    double[] GetCovMatrix() {
        return fCovMatrix;
    }    /// Array[6] of covariance matrix

    double GetRefChi2() {
        return fChi2;
    }      /// Chi^2 after fit

    int GetRefNDF() {
        return fNDF;
    }       /// Number of Degrees of Freedom after fit

    int GetRefNTracks() {
        return fNTracks;
    }   /// Number of tracks used during fit

    void setCovMatrix(double[] cov) {
        System.arraycopy(cov, 0, fCovMatrix, 0, 6);
    }

    void setX(double x) {
        fX = x;
    }

    void setY(double y) {
        fY = y;
    }

    void setZ(double z) {
        fZ = z;
    }

    void setChi2(double chi2) {
        fChi2 = chi2;
    }

    void setNTracks(int nTracks) {
        fNTracks = nTracks;
    }

    void setfNDF(int nDF) {
        fNDF = nDF;
    }

    void incrementChi2(double chi2) {
        fChi2 += chi2;
    }

    void incrementNTracks(int nTracks) {
        fNTracks += nTracks;
    }

    void incrementfNDF(int nDF) {
        fNDF += nDF;
    }

}
