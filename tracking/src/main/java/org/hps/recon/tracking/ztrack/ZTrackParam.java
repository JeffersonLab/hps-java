package org.hps.recon.tracking.ztrack;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class ZTrackParam {
//TODO Add enums for the track parameters
    //TODO define the track parameters

    /**
     * \brief Constructor.
     */
    public ZTrackParam() {
        fQp = 1.;
        fCovMatrix[0] = 9999.; //x
        fCovMatrix[5] = 9999.; //y
        fCovMatrix[9] = 9999.; //tx
        fCovMatrix[12] = 9999.; //ty
        fCovMatrix[14] = 9999.; //q/p
    }

    public ZTrackParam(double[] pos, double[] mom, int sign) {
        SetX(pos[0]);
        SetY(pos[1]);
        SetZ(pos[2]);
        SetTx(mom[0] / mom[2]);
        SetTy(mom[1] / mom[2]);
        double mag = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2]);
        SetQp(sign / mag);
        double p = (abs(fQp) != 0.) ? 1. / abs(fQp) : 1.e20;
        double pz = sqrt(p * p / (fTx * fTx + fTy * fTy + 1));
        double px = fTx * pz;
        double py = fTy * pz;
    }

    // copy constructor
    public ZTrackParam(ZTrackParam p) {
        SetStateVector(p.GetStateVector());
        SetZ(p.GetZ());
        SetCovMatrix(p.GetCovMatrix());
    }

    public void copyFrom(ZTrackParam p) {
        SetStateVector(p.GetStateVector());
        SetZ(p.GetZ());
        SetCovMatrix(p.GetCovMatrix());
    }

    /*
     * Getters
     */
    public double GetX() {
        return fX;
    }

    public double GetY() {
        return fY;
    }

    public double GetZ() {
        return fZ;
    }

    public double GetTx() {
        return fTx;
    }

    public double GetTy() {
        return fTy;
    }

    public double GetQp() {
        return fQp;
    }

    public double GetCovariance(int index) {
        return fCovMatrix[index];
    }

    public double[] GetCovMatrix() {
        double[] tmp = new double[15];
        System.arraycopy(fCovMatrix, 0, tmp, 0, 15);
        return tmp;
    }

    /*
     * Setters
     */
    public void SetX(double x) {
        fX = x;
    }

    public void SetY(double y) {
        fY = y;
    }

    public void SetZ(double z) {
        fZ = z;
    }

    public void SetTx(double tx) {
        fTx = tx;
    }

    public void SetTy(double ty) {
        fTy = ty;
    }

    public void SetQp(double qp) {
        fQp = qp;
    }

    public void SetCovMatrix(final double[] C) {
        System.arraycopy(C, 0, fCovMatrix, 0, 15);
    }

    public void SetCovariance(int index, double cov) {
        fCovMatrix[index] = cov;
    }

    /**
     * \brief Return direction cosines. \param[out] nx Output direction cosine
     * for OX axis. \param[out] ny Output direction cosine for OY axis.
     * \param[out] nz Output direction cosine for OZ axis.
     */
    public void GetDirCos(double[] dirCos) {
        double p = (abs(fQp) != 0.) ? 1. / abs(fQp) : 1.e20;
        double pz = sqrt(p * p / (fTx * fTx + fTy * fTy + 1));
        double px = fTx * pz;
        double py = fTy * pz;
        dirCos[0] = px / p;
        dirCos[1] = py / p;
        dirCos[2] = pz / p;
//      TVector3 unit = TVector3(px, py, pz).Unit();
//      nx = unit.X();
//      ny = unit.Y();
//      nz = unit.Z();
    }

    public void GetMomentum(double[] mom) {
        double p = (abs(fQp) != 0.) ? 1. / abs(fQp) : 1.e20;
        double pz = sqrt(p * p / (fTx * fTx + fTy * fTy + 1));
        double px = fTx * pz;
        double py = fTy * pz;
        mom[0] = px;
        mom[1] = py;
        mom[2] = pz;
    }

    /**
     * \brief Return state vector as vector. \return State vector as vector.
     */
    public double[] GetStateVector() {
        double[] state = new double[5];
        state[0] = GetX();
        state[1] = GetY();
        state[2] = GetTx();
        state[3] = GetTy();
        state[4] = GetQp();
        return state;
    }

    /**
     * \brief Set parameters from vector. \param[in] x State vector.
     */
    public void SetStateVector(final double[] x) {
        SetX(x[0]);
        SetY(x[1]);
        SetTx(x[2]);
        SetTy(x[3]);
        SetQp(x[4]);
    }

    /**
     * \brief Return string representation of class. \return String
     * representation of class.
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("ZTrackParam: pos=(" + fX + "," + fY + "," + fZ
                + ") tx=" + fTx + " ty=" + fTy + " qp=" + fQp);
        // ss << "cov: ";
        // for (Int_t i = 0; i < 15; i++) ss << fCovMatrix[i] << " ";
        // ss << endl;
//      ss.precision(3);
        ss.append(" cov: x=" + fCovMatrix[0] + " y=" + fCovMatrix[5]
                + " tx=" + fCovMatrix[9] + " ty=" + fCovMatrix[12]
                + " q/p=" + fCovMatrix[14] + "\n");
        return ss.toString();
    }
    double fX, fY, fZ; // X, Y, Z coordinates in [cm]
    double fTx, fTy; // Slopes: tx=dx/dz, ty=dy/dz
    double fQp; // Q/p: Q is a charge (+/-1), p is momentum in [GeV/c]

    /*
     * Covariance matrix. Upper triangle symmetric matrix. a[0,0..4], a[1,1..4],
     * a[2,2..4], a[3,3..4], a[4,4]
     */
    double[] fCovMatrix = new double[15];
}
