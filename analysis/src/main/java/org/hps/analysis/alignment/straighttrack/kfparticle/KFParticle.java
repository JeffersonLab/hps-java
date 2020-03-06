package org.hps.analysis.alignment.straighttrack.kfparticle;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class KFParticle {

    double[] fP = new double[8];  //* Main particle parameters {X,Y,Z,Px,Py,Pz,E,S[=DecayLength/P]}
    double[] fC = new double[36]; //* Lowe-triangle covariance matrix of fP
    int fQ = 0;     //* Particle charge 
    int fNDF = -3;   //* Number of degrees of freedom 
    double fChi2 = 0.;  //* Chi^2

    double fSFromDecay = 0.; //* Distance from decay vertex to current position

    boolean fAtProductionVertex = false; //* Flag shows that the particle error along
    //* its trajectory is taken from production vertex    

    double[] fVtxGuess = new double[3];  //* Guess for the position of the decay vertex 
    //* ( used for linearisation of equations )

    boolean fIsLinearized = false;   //* Flag shows that the guess is present

    int fConstructMethod = 2; //* Determines the method for the particle construction. 
    //* 0 - Energy considered as an independent veriable, fitted independently from momentum, without any constraints on mass
    //* 1 - Energy considered as a dependent variable, calculated from the momentum and mass hypothesis
    //* 2 - Energy considered as an independent variable, fitted independently from momentum, with constraints on mass of daughter particle

    double SumDaughterMass = 0.;  //* sum of the daughter particles masses
    double fMassHypo = -1.;  //* sum of the daughter particles masses

    int fId = -1;                   // id of particle
    List<Integer> fDaughtersIds = new ArrayList<Integer>(); // id of particles it was created from. if size == 1 then this is id of track. TODO use in functions.

    int fPDG = 0; // pdg hypothesis

    //TODO replace this with proper field manager
    double _Bx = 0.;
    double _By = 0.;
    double _Bz = 0.;

    // construct from reconstructed track
    public KFParticle(KFTrack Tr) {
        double[] m = Tr.GetTrack();
        //cng
        // recall that CbmKFTrack parameters are:
        // (x, y, x', y', q/p, z)
        // x' = dx/dz
        // y' = dy/dz
        //cng
        double[] V = Tr.GetCovMatrix();
        //* Fit of vertex track slopes and momentum (a,b,qp) to xyz0 vertex
//  if( z0 ) Tr.Extrapolate( *z0 );
        double a = m[2], b = m[3], qp = m[4];
        //cng
        // a = dx/dz
        // b = dy/dz
        //cng
        //* convert the track to (x,y,px,py,pz,e,t/p) parameterization

        double c2 = 1. / (1. + a * a + b * b);
        double pq = 1. / qp;

        double p2 = pq * pq;
        double pz = sqrt(p2 * c2);
        double px = a * pz;
        double py = b * pz;
        double[] H = {-px * c2, -py * c2, -pz * pq};

        double[] r = new double[6];

        r[0] = m[0]; // x
        r[1] = m[1]; // y
        r[2] = m[5]; // z
        r[3] = px;
        r[4] = py;
        r[5] = pz;

        double cxpz = H[0] * V[3] + H[1] * V[6] + H[2] * V[10];
        double cypz = H[0] * V[4] + H[1] * V[7] + H[2] * V[11];
        double capz = H[0] * V[5] + H[1] * V[8] + H[2] * V[12];
        double cbpz = H[0] * V[8] + H[1] * V[9] + H[2] * V[13];
//  double cqpz = H[0]*V[12] + H[1]*V[13] + H[2]*V[14];
        double cpzpz = H[0] * H[0] * V[5] + H[1] * H[1] * V[9] + H[2] * H[2] * V[14]
                + 2 * (H[0] * H[1] * V[8] + H[0] * H[2] * V[12] + H[1] * H[2] * V[13]);

        double[] C = new double[21];

        C[0] = V[0];
        C[1] = V[1];
        C[2] = V[2];
        C[3] = 0;
        C[4] = 0;
        C[5] = 0;
        C[6] = V[3] * pz + a * cxpz;
        C[7] = V[4] * pz + a * cypz;
        C[8] = 0;
        C[9] = V[5] * pz * pz + 2 * a * pz * capz + a * a * cpzpz;
        C[10] = V[6] * pz + b * cxpz;
        C[11] = V[7] * pz + b * cypz;
        C[12] = 0;
        C[13] = V[8] * pz * pz + a * pz * cbpz + b * pz * capz + a * b * cpzpz;
        C[14] = V[9] * pz * pz + 2 * b * pz * cbpz + b * b * cpzpz;
        C[15] = cxpz;
        C[16] = cypz;
        C[17] = 0;
        C[18] = capz * pz + a * cpzpz;
        C[19] = cbpz * pz + b * cpzpz;
        C[20] = cpzpz;

        int Charge = (qp > 0.) ? 1 : ((qp < 0) ? -1 : 0);

        double mass = Tr.GetMass();

        Create(r, C, Charge, mass);
    }

    // Construction of parent particle from its decay particles
    public KFParticle(KFParticle d1, KFParticle d2) {
        AddDaughter(d1);
        AddDaughter(d2);
    }

    public void AddDaughter(KFParticle Daughter) {
        if (fNDF < -1) { // first daughter . just copy
            fNDF = -1;
            fQ = Daughter.GetQ();
            for (int i = 0; i < 7; i++) {
                fP[i] = Daughter.fP[i];
            }
            for (int i = 0; i < 28; i++) {
                fC[i] = Daughter.fC[i];
            }
            fSFromDecay = 0;
            fMassHypo = Daughter.fMassHypo;
            SumDaughterMass = Daughter.SumDaughterMass;
            return;
        }

        if (fConstructMethod == 0) {
            throw new RuntimeException("AddDaughterWithEnergyFit(Daughter) not implemented");
            //AddDaughterWithEnergyFit(Daughter);
        } else if (fConstructMethod == 1) {
            throw new RuntimeException("AddDaughterWithEnergyCalc(Daughter) not implemented");
            //AddDaughterWithEnergyCalc(Daughter);
        } else if (fConstructMethod == 2) {
            AddDaughterWithEnergyFitMC(Daughter);
        }

        SumDaughterMass += Daughter.SumDaughterMass;
        fMassHypo = -1;
    }

    void AddDaughterWithEnergyFitMC(KFParticle Daughter) {
        //* Energy considered as an independent variable, fitted independently from momentum, without any constraints on mass

        //* Add daughter 
        TransportToDecayVertex();

        double[] b = new double[3];
        int maxIter = 1;

        if (!fIsLinearized) {
            if (fNDF == -1) {
                double[] dsArray = new double[2];
                GetDStoParticle(Daughter, dsArray);
                double ds = dsArray[0];
                double ds1 = dsArray[1];
                TransportToDS(ds);
                double[] m = new double[8];
                double[] mCd = new double[36];
                Daughter.Transport(ds1, m, mCd);
                fVtxGuess[0] = .5 * (fP[0] + m[0]);
                fVtxGuess[1] = .5 * (fP[1] + m[1]);
                fVtxGuess[2] = .5 * (fP[2] + m[2]);
            } else {
                fVtxGuess[0] = fP[0];
                fVtxGuess[1] = fP[1];
                fVtxGuess[2] = fP[2];
            }
            maxIter = 3;
        }

        for (int iter = 0; iter < maxIter; iter++) {

            {
                GetFieldValue(fVtxGuess, b);
                final double kCLight = 0.000299792458;
                b[0] *= kCLight;
                b[1] *= kCLight;
                b[2] *= kCLight;
            }

            //TODO make deep copy here?
            double[] ffP = fP;
            double[] ffC = fC;
            double[] tmpP = new double[8];
            double[] tmpC = new double[36];
            if (fNDF == -1) {
                GetMeasurement(fVtxGuess, tmpP, tmpC);
                ffP = tmpP;
                ffC = tmpC;
            }
            double[] m = new double[8];
            double[] mV = new double[36];

            if (Daughter.fC[35] > 0) {
                Daughter.GetMeasurement(fVtxGuess, m, mV);
            } else {
                for (int i = 0; i < 8; i++) {
                    m[i] = Daughter.fP[i];
                }
                for (int i = 0; i < 36; i++) {
                    mV[i] = Daughter.fC[i];
                }
            }
            //*

            double[] mS = new double[6];
            {
                double[] mSi = {ffC[0] + mV[0],
                    ffC[1] + mV[1], ffC[2] + mV[2],
                    ffC[3] + mV[3], ffC[4] + mV[4], ffC[5] + mV[5]};

                mS[0] = mSi[2] * mSi[5] - mSi[4] * mSi[4];
                mS[1] = mSi[3] * mSi[4] - mSi[1] * mSi[5];
                mS[2] = mSi[0] * mSi[5] - mSi[3] * mSi[3];
                mS[3] = mSi[1] * mSi[4] - mSi[2] * mSi[3];
                mS[4] = mSi[1] * mSi[3] - mSi[0] * mSi[4];
                mS[5] = mSi[0] * mSi[2] - mSi[1] * mSi[1];

                double s = (mSi[0] * mS[0] + mSi[1] * mS[1] + mSi[3] * mS[3]);

                s = (s > 1.E-20) ? 1. / s : 0;
                mS[0] *= s;
                mS[1] *= s;
                mS[2] *= s;
                mS[3] *= s;
                mS[4] *= s;
                mS[5] *= s;
            }
            //* Residual (measured - estimated)

            double[] zeta = {m[0] - ffP[0], m[1] - ffP[1], m[2] - ffP[2]};

            //* CHt = CH'
            double[] mCHt0 = new double[7];
            double[] mCHt1 = new double[7];
            double[] mCHt2 = new double[7];

            mCHt0[0] = ffC[0];
            mCHt1[0] = ffC[1];
            mCHt2[0] = ffC[3];
            mCHt0[1] = ffC[1];
            mCHt1[1] = ffC[2];
            mCHt2[1] = ffC[4];
            mCHt0[2] = ffC[3];
            mCHt1[2] = ffC[4];
            mCHt2[2] = ffC[5];
            mCHt0[3] = ffC[6];
            mCHt1[3] = ffC[7];
            mCHt2[3] = ffC[8];
            mCHt0[4] = ffC[10];
            mCHt1[4] = ffC[11];
            mCHt2[4] = ffC[12];
            mCHt0[5] = ffC[15];
            mCHt1[5] = ffC[16];
            mCHt2[5] = ffC[17];
            mCHt0[6] = ffC[21];
            mCHt1[6] = ffC[22];
            mCHt2[6] = ffC[23];

            //* Kalman gain K = mCH'*S
            double[] k0 = new double[7];
            double[] k1 = new double[7];
            double[] k2 = new double[7];

            for (int i = 0; i < 7; ++i) {
                k0[i] = mCHt0[i] * mS[0] + mCHt1[i] * mS[1] + mCHt2[i] * mS[3];
                k1[i] = mCHt0[i] * mS[1] + mCHt1[i] * mS[2] + mCHt2[i] * mS[4];
                k2[i] = mCHt0[i] * mS[3] + mCHt1[i] * mS[4] + mCHt2[i] * mS[5];
            }

            //* New estimation of the vertex position 
            if (iter < maxIter - 1) {
                for (int i = 0; i < 3; ++i) {
                    fVtxGuess[i] = ffP[i] + k0[i] * zeta[0] + k1[i] * zeta[1] + k2[i] * zeta[2];
                }
                continue;
            }

            // last itearation . update the particle
            //* VHt = VH'
            double[] mVHt0 = new double[7];
            double[] mVHt1 = new double[7];
            double[] mVHt2 = new double[7];

            mVHt0[0] = mV[0];
            mVHt1[0] = mV[1];
            mVHt2[0] = mV[3];
            mVHt0[1] = mV[1];
            mVHt1[1] = mV[2];
            mVHt2[1] = mV[4];
            mVHt0[2] = mV[3];
            mVHt1[2] = mV[4];
            mVHt2[2] = mV[5];
            mVHt0[3] = mV[6];
            mVHt1[3] = mV[7];
            mVHt2[3] = mV[8];
            mVHt0[4] = mV[10];
            mVHt1[4] = mV[11];
            mVHt2[4] = mV[12];
            mVHt0[5] = mV[15];
            mVHt1[5] = mV[16];
            mVHt2[5] = mV[17];
            mVHt0[6] = mV[21];
            mVHt1[6] = mV[22];
            mVHt2[6] = mV[23];

            //* Kalman gain Km = mCH'*S
            double[] km0 = new double[7];
            double[] km1 = new double[7];
            double[] km2 = new double[7];

            for (int i = 0; i < 7; ++i) {
                km0[i] = mVHt0[i] * mS[0] + mVHt1[i] * mS[1] + mVHt2[i] * mS[3];
                km1[i] = mVHt0[i] * mS[1] + mVHt1[i] * mS[2] + mVHt2[i] * mS[4];
                km2[i] = mVHt0[i] * mS[3] + mVHt1[i] * mS[4] + mVHt2[i] * mS[5];
            }

            for (int i = 0; i < 7; ++i) {
                ffP[i] = ffP[i] + k0[i] * zeta[0] + k1[i] * zeta[1] + k2[i] * zeta[2];
            }

            for (int i = 0; i < 7; ++i) {
                m[i] = m[i] - km0[i] * zeta[0] - km1[i] * zeta[1] - km2[i] * zeta[2];
            }

            for (int i = 0, k = 0; i < 7; ++i) {
                for (int j = 0; j <= i; ++j, ++k) {
                    ffC[k] = ffC[k] - (k0[i] * mCHt0[j] + k1[i] * mCHt1[j] + k2[i] * mCHt2[j]);
                }
            }

            for (int i = 0, k = 0; i < 7; ++i) {
                for (int j = 0; j <= i; ++j, ++k) {
                    mV[k] = mV[k] - (km0[i] * mVHt0[j] + km1[i] * mVHt1[j] + km2[i] * mVHt2[j]);
                }
            }

            double[][] mDf = new double[7][7];

            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < 7; ++j) {
                    mDf[i][j] = (km0[i] * mCHt0[j] + km1[i] * mCHt1[j] + km2[i] * mCHt2[j]);
                }
            }

            double[][] mJ1 = new double[7][7];
            double[][] mJ2 = new double[7][7];
            for (int iPar1 = 0; iPar1 < 7; iPar1++) {
                for (int iPar2 = 0; iPar2 < 7; iPar2++) {
                    mJ1[iPar1][iPar2] = 0;
                    mJ2[iPar1][iPar2] = 0;
                }
            }

            double mMassParticle = ffP[6] * ffP[6] - (ffP[3] * ffP[3] + ffP[4] * ffP[4] + ffP[5] * ffP[5]);
            double mMassDaughter = m[6] * m[6] - (m[3] * m[3] + m[4] * m[4] + m[5] * m[5]);
            if (mMassParticle > 0) {
                mMassParticle = sqrt(mMassParticle);
            }
            if (mMassDaughter > 0) {
                mMassDaughter = sqrt(mMassDaughter);
            }

            if (fMassHypo > -0.5) {
                SetMassConstraint(ffP, ffC, mJ1, fMassHypo);
            } else if ((mMassParticle < SumDaughterMass) || (ffP[6] < 0)) {
                SetMassConstraint(ffP, ffC, mJ1, SumDaughterMass);
            }

            if (Daughter.fMassHypo > -0.5) {
                SetMassConstraint(m, mV, mJ2, Daughter.fMassHypo);
            } else if ((mMassDaughter < Daughter.SumDaughterMass) || (m[6] < 0)) {
                SetMassConstraint(m, mV, mJ2, Daughter.SumDaughterMass);
            }

            double[][] mDJ = new double[7][7];

            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 7; j++) {
                    mDJ[i][j] = 0;
                    for (int k = 0; k < 7; k++) {
                        mDJ[i][j] += mDf[i][k] * mJ1[j][k];
                    }
                }
            }

            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < 7; ++j) {
                    mDf[i][j] = 0;
                    for (int l = 0; l < 7; l++) {
                        mDf[i][j] += mJ2[i][l] * mDJ[l][j];
                    }
                }
            }

            //* Add the daughter momentum to the particle momentum
            ffP[3] += m[3];
            ffP[4] += m[4];
            ffP[5] += m[5];
            ffP[6] += m[6];

            ffC[9] += mV[9];
            ffC[13] += mV[13];
            ffC[14] += mV[14];
            ffC[18] += mV[18];
            ffC[19] += mV[19];
            ffC[20] += mV[20];
            ffC[24] += mV[24];
            ffC[25] += mV[25];
            ffC[26] += mV[26];
            ffC[27] += mV[27];

            ffC[6] += mDf[3][0];
            ffC[7] += mDf[3][1];
            ffC[8] += mDf[3][2];
            ffC[10] += mDf[4][0];
            ffC[11] += mDf[4][1];
            ffC[12] += mDf[4][2];
            ffC[15] += mDf[5][0];
            ffC[16] += mDf[5][1];
            ffC[17] += mDf[5][2];
            ffC[21] += mDf[6][0];
            ffC[22] += mDf[6][1];
            ffC[23] += mDf[6][2];

            ffC[9] += mDf[3][3] + mDf[3][3];
            ffC[13] += mDf[4][3] + mDf[3][4];
            ffC[14] += mDf[4][4] + mDf[4][4];
            ffC[18] += mDf[5][3] + mDf[3][5];
            ffC[19] += mDf[5][4] + mDf[4][5];
            ffC[20] += mDf[5][5] + mDf[5][5];
            ffC[24] += mDf[6][3] + mDf[3][6];
            ffC[25] += mDf[6][4] + mDf[4][6];
            ffC[26] += mDf[6][5] + mDf[5][6];
            ffC[27] += mDf[6][6] + mDf[6][6];

            //* New estimation of the vertex position r += K*zeta
            for (int i = 0; i < 7; ++i) {
                fP[i] = ffP[i];
            }

            //* New covariance matrix C -= K*(mCH')'
            for (int i = 0, k = 0; i < 7; ++i) {
                for (int j = 0; j <= i; ++j, ++k) {
                    fC[k] = ffC[k];
                }
            }
            //* Calculate Chi^2 

            fNDF += 2;
            fQ += Daughter.GetQ();
            fSFromDecay = 0;
            fChi2 += (mS[0] * zeta[0] + mS[1] * zeta[1] + mS[3] * zeta[2]) * zeta[0]
                    + (mS[1] * zeta[0] + mS[2] * zeta[1] + mS[4] * zeta[2]) * zeta[1]
                    + (mS[3] * zeta[0] + mS[4] * zeta[1] + mS[5] * zeta[2]) * zeta[2];
        }
    }

    void Create(double[] Param, double[] Cov, int Charge, double mass) {
        // Constructor from "cartesian" track, PID hypothesis should be provided
        //
        // Param[6] = { X, Y, Z, Px, Py, Pz } - position and momentum
        // Cov [21] = lower-triangular part of the covariance matrix:
        //
        //                (  0  .  .  .  .  . )
        //                (  1  2  .  .  .  . )
        //  Cov. matrix = (  3  4  5  .  .  . ) - numbering of covariance elements in Cov[]
        //                (  6  7  8  9  .  . )
        //                ( 10 11 12 13 14  . )
        //                ( 15 16 17 18 19 20 )
        double[] C = new double[21];
        for (int i = 0; i < 21; i++) {
            C[i] = Cov[i];
        }
        Initialize(Param, C, Charge, mass);
    }

    void Initialize(double[] Param, double[] Cov, int Charge, double Mass) {
        // Constructor from "cartesian" track, particle mass hypothesis should be provided
        //
        // Param[6] = { X, Y, Z, Px, Py, Pz } - position and momentum
        // Cov [21] = lower-triangular part of the covariance matrix:
        //
        //                (  0  .  .  .  .  . )
        //                (  1  2  .  .  .  . )
        //  Cov. matrix = (  3  4  5  .  .  . ) - numbering of covariance elements in Cov[]
        //                (  6  7  8  9  .  . )
        //                ( 10 11 12 13 14  . )
        //                ( 15 16 17 18 19 20 )

        for (int i = 0; i < 6; i++) {
            fP[i] = Param[i];
        }
        for (int i = 0; i < 21; i++) {
            fC[i] = Cov[i];
        }

        double energy = sqrt(Mass * Mass + fP[3] * fP[3] + fP[4] * fP[4] + fP[5] * fP[5]);
        fP[6] = energy;
        fP[7] = 0;
        fQ = Charge;
        fNDF = 0;
        fChi2 = 0;
        fAtProductionVertex = false;
        fIsLinearized = false;
        fSFromDecay = 0;

        double energyInv = 1. / energy;
        double h0 = fP[3] * energyInv,
                h1 = fP[4] * energyInv,
                h2 = fP[5] * energyInv;

        fC[21] = h0 * fC[6] + h1 * fC[10] + h2 * fC[15];
        fC[22] = h0 * fC[7] + h1 * fC[11] + h2 * fC[16];
        fC[23] = h0 * fC[8] + h1 * fC[12] + h2 * fC[17];
        fC[24] = h0 * fC[9] + h1 * fC[13] + h2 * fC[18];
        fC[25] = h0 * fC[13] + h1 * fC[14] + h2 * fC[19];
        fC[26] = h0 * fC[18] + h1 * fC[19] + h2 * fC[20];
        fC[27] = (h0 * h0 * fC[9] + h1 * h1 * fC[14] + h2 * h2 * fC[20]
                + 2 * (h0 * h1 * fC[13] + h0 * h2 * fC[18] + h1 * h2 * fC[19]));
        for (int i = 28; i < 36; i++) {
            fC[i] = 0;
        }
        fC[35] = 1.;

        SumDaughterMass = Mass;
        fMassHypo = Mass;
    }

    //*
    //*  ACCESSORS
    //*
    //* Simple accessors 
    public double GetX() {
        return fP[0];
    }

    public double GetY() {
        return fP[1];
    }

    public double GetZ() {
        return fP[2];
    }

    public double GetPx() {
        return fP[3];
    }

    public double GetPy() {
        return fP[4];
    }

    public double GetPz() {
        return fP[5];
    }

    public double GetE() {
        return fP[6];
    }

    public double GetS() {
        return fP[7];
    }

    public int GetQ() {
        return fQ;
    }

    public double GetChi2() {
        return fChi2;
    }

    public int GetNDF() {
        return fNDF;
    }

    // accessors requiring calculations...
    int GetMomentum(double[] pAndError) {
        //* Calculate particle momentum

        double x = fP[3];
        double y = fP[4];
        double z = fP[5];
        double x2 = x * x;
        double y2 = y * y;
        double z2 = z * z;
        double p2 = x2 + y2 + z2;
        pAndError[0] = sqrt(p2);
        pAndError[1] = (x2 * fC[9] + y2 * fC[14] + z2 * fC[20] + 2 * (x * y * fC[13] + x * z * fC[18] + y * z * fC[19]));
        if (pAndError[1] > 1.e-16 && pAndError[0] > 1.e-4) {
            pAndError[1] = sqrt(pAndError[1]) / pAndError[0];
            return 0;
        }
        pAndError[1] = 1.e8;
        return 1;
    }

    int GetPt(double[] ptAndError) {
        //* Calculate particle transverse momentum

        double px = fP[3];
        double py = fP[4];
        double px2 = px * px;
        double py2 = py * py;
        double pt2 = px2 + py2;
        double pt = sqrt(pt2);
        double error = (px2 * fC[9] + py2 * fC[14] + 2 * px * py * fC[13]);
        //TODO check on these conditions
        if (error > 0 && pt > 1.e-4) {
            error = sqrt(error) / pt;
            ptAndError[0] = pt;
            ptAndError[1] = error;
            return 0;
        }
        error = 1.e10;
        ptAndError[0] = pt;
        ptAndError[1] = error;
        return 1;
    }

    int GetPhi(double[] phiAndError) {
        //* Calculate particle polar angle

        double px = fP[3];
        double py = fP[4];
        double px2 = px * px;
        double py2 = py * py;
        double pt2 = px2 + py2;
        double phi = atan2(py, px);
        double error = (py2 * fC[9] + px2 * fC[14] - 2 * px * py * fC[13]);
        if (error > 0 && pt2 > 1.e-4) {
            error = sqrt(error) / pt2;
            phiAndError[0] = phi;
            phiAndError[1] = error;
            return 0;
        }
        error = 1.e10;
        phiAndError[0] = phi;
        phiAndError[1] = error;
        return 1;
    }

    int GetR(double[] rAndError) {
        //* Calculate distance to the origin

        double x = fP[0];
        double y = fP[1];
        double x2 = x * x;
        double y2 = y * y;
        double r = sqrt(x2 + y2);
        double error = (x2 * fC[0] + y2 * fC[2] - 2 * x * y * fC[1]);
        if (error > 0 && r > 1.e-4) {
            error = sqrt(error) / r;
            rAndError[0] = r;
            rAndError[1] = error;
            return 0;
        }
        error = 1.e10;
        rAndError[0] = r;
        rAndError[1] = error;
        return 1;
    }

    int GetMass(double[] mAndError) {
        //* Calculate particle mass

        // s = sigma^2 of m2/2
        double s = (fP[3] * fP[3] * fC[9] + fP[4] * fP[4] * fC[14] + fP[5] * fP[5] * fC[20]
                + fP[6] * fP[6] * fC[27]
                + 2 * (+fP[3] * fP[4] * fC[13] + fP[5] * (fP[3] * fC[18] + fP[4] * fC[19])
                - fP[6] * (fP[3] * fC[24] + fP[4] * fC[25] + fP[5] * fC[26])));
//   double m2 = abs(fP[6]*fP[6] - fP[3]*fP[3] - fP[4]*fP[4] - fP[5]*fP[5]);
//   m  = sqrt(m2);
//   if( m>1.e-10 ){
//     if( s>=0 ){
//       error = sqrt(s)/m;
//       return 0;
//     }
//   }
//   error = 1.e20;
        double m2 = (fP[6] * fP[6] - fP[3] * fP[3] - fP[4] * fP[4] - fP[5] * fP[5]);
        double m = 0.;
        double error = 0.;
        if (m2 < 0.) {
            error = 1.e20;
            m = -sqrt(-m2);
            mAndError[0] = m;
            mAndError[1] = error;
            return 1;
        }

        m = sqrt(m2);
        if (m > 1.e-6) {
            if (s >= 0) {
                error = sqrt(s) / m;
                mAndError[0] = m;
                mAndError[1] = error;
                return 0;
            }
        } else {
            error = 1.e20;
            mAndError[0] = m;
            mAndError[1] = error;
            return 0;
        }
        error = 1.e20;
        mAndError[0] = m;
        mAndError[1] = error;
        return 1;
    }

    int GetDecayLength(double[] lAndError) {
        //* Calculate particle decay length [cm]

        double x = fP[3];
        double y = fP[4];
        double z = fP[5];
        double t = fP[7];
        double x2 = x * x;
        double y2 = y * y;
        double z2 = z * z;
        double p2 = x2 + y2 + z2;
        double l = t * sqrt(p2);
        double error = 0.;
        if (p2 > 1.e-4) {
            error = p2 * fC[35] + t * t / p2 * (x2 * fC[9] + y2 * fC[14] + z2 * fC[20]
                    + 2 * (x * y * fC[13] + x * z * fC[18] + y * z * fC[19]))
                    + 2 * t * (x * fC[31] + y * fC[32] + z * fC[33]);
            error = sqrt(abs(error));
            lAndError[0] = l;
            lAndError[1] = error;
            return 0;
        }
        error = 1.e20;
        lAndError[0] = l;
        lAndError[1] = error;
        return 1;
    }

    int GetDecayLengthXY(double[] lAndError) {
        //* Calculate particle decay length in XY projection [cm]

        double x = fP[3];
        double y = fP[4];
        double t = fP[7];
        double x2 = x * x;
        double y2 = y * y;
        double pt2 = x2 + y2;
        double l = t * sqrt(pt2);
        double error = 0.;
        if (pt2 > 1.e-4) {
            error = pt2 * fC[35] + t * t / pt2 * (x2 * fC[9] + y2 * fC[14] + 2 * x * y * fC[13])
                    + 2 * t * (x * fC[31] + y * fC[32]);
            error = sqrt(abs(error));
            lAndError[0] = l;
            lAndError[1] = error;
            return 0;
        }
        error = 1.e20;
        lAndError[0] = l;
        lAndError[1] = error;
        return 1;
    }

    int GetLifeTime(double[] tauCAndError) {
        //* Calculate particle decay time [s]

        double[] mAndError = new double[2];
        GetMass(mAndError);
        double m = mAndError[0];
        double dm = mAndError[1];
        double cTM = (-fP[3] * fC[31] - fP[4] * fC[32] - fP[5] * fC[33] + fP[6] * fC[34]);
        double tauC = fP[7] * m;
        double error = m * m * fC[35] + 2 * fP[7] * cTM + fP[7] * fP[7] * dm * dm;
        if (error > 0) {
            error = sqrt(error);
            tauCAndError[0] = tauC;
            tauCAndError[1] = error;
            return 0;
        }
        error = 1.e20;
        tauCAndError[0] = tauC;
        tauCAndError[1] = error;
        return 1;
    }

    public void TransportToDecayVertex() {
        //* Transport the particle to its decay vertex 

        if (fSFromDecay != 0) {
            TransportToDS(-fSFromDecay);
        }
        if (fAtProductionVertex) {
            Convert(false);
        }
        fAtProductionVertex = false;
    }

    public double GetP() {
        double[] parAndErr = new double[2];
        if (GetMomentum(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating Momentum");
        } else {
            return parAndErr[0];
        }
    }

    public double GetPt() {
        double[] parAndErr = new double[2];
        if (GetPt(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating transverse momentum");
        } else {
            return parAndErr[0];
        }
    }

    public double GetPhi() {
        double[] parAndErr = new double[2];
        if (GetPhi(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating phi");
        } else {
            return parAndErr[0];
        }
    }

    public double GetMomentum() {
        double[] parAndErr = new double[2];
        if (GetMomentum(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating Momentum");
        } else {
            return parAndErr[0];
        }
    }

    public double GetMass() {
        double[] parAndErr = new double[2];
        if (GetMass(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating mass");
        } else {
            return parAndErr[0];
        }
    }

    public double GetDecayLength() {
        double[] parAndErr = new double[2];
        if (GetDecayLength(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating decay length");
        } else {
            return parAndErr[0];
        }
    }

    public double GetDecayLengthXY() {
        double[] parAndErr = new double[2];
        if (GetDecayLengthXY(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating length XY");
        } else {
            return parAndErr[0];
        }
    }

    public double GetLifeTime() {
        double[] parAndErr = new double[2];
        if (GetLifeTime(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating lifetime");
        } else {
            return parAndErr[0];
        }
    }

    public double GetR() {
        double[] parAndErr = new double[2];
        if (GetR(parAndErr) != 0) {
            throw new RuntimeException("KFParticle: Problem calculating radius");
        } else {
            return parAndErr[0];
        }
    }

    public void TransportToDS(double dS) {
        //* Transport the particle on dS parameter (SignedPath/Momentum) 

        Transport(dS, fP, fC);
        fSFromDecay += dS;
    }

    public void Transport(double dS, double[] P, double[] C) {
        TransportCBM(dS, P, C);
    }

    void TransportCBM(double dS, double[] P, double[] C) {
        //* Transport the particle on dS, output to P[],C[], for CBM field

        if (fQ == 0) {
            TransportLine(dS, P, C);
            return;
        }

        final double kCLight = 0.000299792458;

        double c = fQ * kCLight;

        // construct coefficients 
        double px = fP[3],
                py = fP[4],
                pz = fP[5];

        double sx = 0, sy = 0, sz = 0, syy = 0, syz = 0, syyy = 0, ssx = 0, ssy = 0, ssz = 0, ssyy = 0, ssyz = 0, ssyyy = 0;

        { // get field integrals

            double[][] fld = new double[3][3];
            double[] p0 = new double[3];
            double[] p1 = new double[3];
            double[] p2 = new double[3];

            // line track approximation
            p0[0] = fP[0];
            p0[1] = fP[1];
            p0[2] = fP[2];

            p2[0] = fP[0] + px * dS;
            p2[1] = fP[1] + py * dS;
            p2[2] = fP[2] + pz * dS;

            p1[0] = 0.5 * (p0[0] + p2[0]);
            p1[1] = 0.5 * (p0[1] + p2[1]);
            p1[2] = 0.5 * (p0[2] + p2[2]);

            // first order track approximation
            {
                GetFieldValue(p0, fld[0]);
                GetFieldValue(p1, fld[1]);
                GetFieldValue(p2, fld[2]);

                double ssy1 = (7 * fld[0][1] + 6 * fld[1][1] - fld[2][1]) * c * dS * dS / 96.;
                double ssy2 = (fld[0][1] + 2 * fld[1][1]) * c * dS * dS / 6.;

                p1[0] -= ssy1 * pz;
                p1[2] += ssy1 * px;
                p2[0] -= ssy2 * pz;
                p2[2] += ssy2 * px;
            }

            GetFieldValue(p0, fld[0]);
            GetFieldValue(p1, fld[1]);
            GetFieldValue(p2, fld[2]);

            sx = c * (fld[0][0] + 4 * fld[1][0] + fld[2][0]) * dS / 6.;
            sy = c * (fld[0][1] + 4 * fld[1][1] + fld[2][1]) * dS / 6.;
            sz = c * (fld[0][2] + 4 * fld[1][2] + fld[2][2]) * dS / 6.;

            ssx = c * (fld[0][0] + 2 * fld[1][0]) * dS * dS / 6.;
            ssy = c * (fld[0][1] + 2 * fld[1][1]) * dS * dS / 6.;
            ssz = c * (fld[0][2] + 2 * fld[1][2]) * dS * dS / 6.;

            double[][] c2 = {{5, -4, -1}, {44, 80, -4}, {11, 44, 5}}; // /=360.    
            double[][] cc2 = {{38, 8, -4}, {148, 208, -20}, {3, 36, 3}}; // /=2520.
            for (int n = 0; n < 3; n++) {
                for (int m = 0; m < 3; m++) {
                    syz += c2[n][m] * fld[n][1] * fld[m][2];
                    ssyz += cc2[n][m] * fld[n][1] * fld[m][2];
                }
            }

            syz *= c * c * dS * dS / 360.;
            ssyz *= c * c * dS * dS * dS / 2520.;

            syy = c * (fld[0][1] + 4 * fld[1][1] + fld[2][1]) * dS;
            syyy = syy * syy * syy / 1296;
            syy = syy * syy / 72;

            ssyy = (fld[0][1] * (38 * fld[0][1] + 156 * fld[1][1] - fld[2][1])
                    + fld[1][1] * (208 * fld[1][1] + 16 * fld[2][1])
                    + fld[2][1] * (3 * fld[2][1])) * dS * dS * dS * c * c / 2520.;
            ssyyy
                    = (fld[0][1] * (fld[0][1] * (85 * fld[0][1] + 526 * fld[1][1] - 7 * fld[2][1])
                    + fld[1][1] * (1376 * fld[1][1] + 84 * fld[2][1])
                    + fld[2][1] * (19 * fld[2][1]))
                    + fld[1][1] * (fld[1][1] * (1376 * fld[1][1] + 256 * fld[2][1])
                    + fld[2][1] * (62 * fld[2][1]))
                    + fld[2][1] * fld[2][1] * (3 * fld[2][1])) * dS * dS * dS * dS * c * c * c / 90720.;

        }

        double[][] mJ = new double[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                mJ[i][j] = 0;
            }
        }

        mJ[0][0] = 1;
        mJ[0][1] = 0;
        mJ[0][2] = 0;
        mJ[0][3] = dS - ssyy;
        mJ[0][4] = ssx;
        mJ[0][5] = ssyyy - ssy;
        mJ[1][0] = 0;
        mJ[1][1] = 1;
        mJ[1][2] = 0;
        mJ[1][3] = -ssz;
        mJ[1][4] = dS;
        mJ[1][5] = ssx + ssyz;
        mJ[2][0] = 0;
        mJ[2][1] = 0;
        mJ[2][2] = 1;
        mJ[2][3] = ssy - ssyyy;
        mJ[2][4] = -ssx;
        mJ[2][5] = dS - ssyy;

        mJ[3][0] = 0;
        mJ[3][1] = 0;
        mJ[3][2] = 0;
        mJ[3][3] = 1 - syy;
        mJ[3][4] = sx;
        mJ[3][5] = syyy - sy;
        mJ[4][0] = 0;
        mJ[4][1] = 0;
        mJ[4][2] = 0;
        mJ[4][3] = -sz;
        mJ[4][4] = 1;
        mJ[4][5] = sx + syz;
        mJ[5][0] = 0;
        mJ[5][1] = 0;
        mJ[5][2] = 0;
        mJ[5][3] = sy - syyy;
        mJ[5][4] = -sx;
        mJ[5][5] = 1 - syy;
        mJ[6][6] = mJ[7][7] = 1;

        P[0] = fP[0] + mJ[0][3] * px + mJ[0][4] * py + mJ[0][5] * pz;
        P[1] = fP[1] + mJ[1][3] * px + mJ[1][4] * py + mJ[1][5] * pz;
        P[2] = fP[2] + mJ[2][3] * px + mJ[2][4] * py + mJ[2][5] * pz;
        P[3] = mJ[3][3] * px + mJ[3][4] * py + mJ[3][5] * pz;
        P[4] = mJ[4][3] * px + mJ[4][4] * py + mJ[4][5] * pz;
        P[5] = mJ[5][3] * px + mJ[5][4] * py + mJ[5][5] * pz;
        P[6] = fP[6];
        P[7] = fP[7];

        MultQSQt(mJ, fC, C);

    }

    //TODO implement connection to real field manager
    void GetFieldValue(double[] xyz, double[] B) {
        B[0] = _Bx;
        B[1] = _By;
        B[2] = _Bz;
    }

    public void TransportLine(double dS, double[] P, double[] C) {
        //* Transport the particle as a straight line

        P[0] = fP[0] + dS * fP[3];
        P[1] = fP[1] + dS * fP[4];
        P[2] = fP[2] + dS * fP[5];
        P[3] = fP[3];
        P[4] = fP[4];
        P[5] = fP[5];
        P[6] = fP[6];
        P[7] = fP[7];

        double c6 = fC[6] + dS * fC[9];
        double c11 = fC[11] + dS * fC[14];
        double c17 = fC[17] + dS * fC[20];
        double sc13 = dS * fC[13];
        double sc18 = dS * fC[18];
        double sc19 = dS * fC[19];

        C[0] = fC[0] + dS * (fC[6] + c6);
        C[2] = fC[2] + dS * (fC[11] + c11);
        C[5] = fC[5] + dS * (fC[17] + c17);

        C[7] = fC[7] + sc13;
        C[8] = fC[8] + sc18;
        C[9] = fC[9];

        C[12] = fC[12] + sc19;

        C[1] = fC[1] + dS * (fC[10] + C[7]);
        C[3] = fC[3] + dS * (fC[15] + C[8]);
        C[4] = fC[4] + dS * (fC[16] + C[12]);
        C[6] = c6;

        C[10] = fC[10] + sc13;
        C[11] = c11;

        C[13] = fC[13];
        C[14] = fC[14];
        C[15] = fC[15] + sc18;
        C[16] = fC[16] + sc19;
        C[17] = c17;

        C[18] = fC[18];
        C[19] = fC[19];
        C[20] = fC[20];
        C[21] = fC[21] + dS * fC[24];
        C[22] = fC[22] + dS * fC[25];
        C[23] = fC[23] + dS * fC[26];

        C[24] = fC[24];
        C[25] = fC[25];
        C[26] = fC[26];
        C[27] = fC[27];
        C[28] = fC[28] + dS * fC[31];
        C[29] = fC[29] + dS * fC[32];
        C[30] = fC[30] + dS * fC[33];

        C[31] = fC[31];
        C[32] = fC[32];
        C[33] = fC[33];
        C[34] = fC[34];
        C[35] = fC[35];
    }

    void Convert(boolean ToProduction) {
        //* Tricky function - convert the particle error along its trajectory to 
        //* the value which corresponds to its production/decay vertex
        //* It is done by combination of the error of decay length with the position errors

        double[] fld = new double[3];
        {
            GetFieldValue(fP, fld);
            final double kCLight = fQ * 0.000299792458;
            fld[0] *= kCLight;
            fld[1] *= kCLight;
            fld[2] *= kCLight;
        }

        double[] h = new double[6];

        h[0] = fP[3];
        h[1] = fP[4];
        h[2] = fP[5];
        if (ToProduction) {
            h[0] = -h[0];
            h[1] = -h[1];
            h[2] = -h[2];
        }
        h[3] = h[1] * fld[2] - h[2] * fld[1];
        h[4] = h[2] * fld[0] - h[0] * fld[2];
        h[5] = h[0] * fld[1] - h[1] * fld[0];

        double c;

        c = fC[28] + h[0] * fC[35];
        fC[0] += h[0] * (c + fC[28]);
        fC[28] = c;

        fC[1] += h[1] * fC[28] + h[0] * fC[29];
        c = fC[29] + h[1] * fC[35];
        fC[2] += h[1] * (c + fC[29]);
        fC[29] = c;

        fC[3] += h[2] * fC[28] + h[0] * fC[30];
        fC[4] += h[2] * fC[29] + h[1] * fC[30];
        c = fC[30] + h[2] * fC[35];
        fC[5] += h[2] * (c + fC[30]);
        fC[30] = c;

        fC[6] += h[3] * fC[28] + h[0] * fC[31];
        fC[7] += h[3] * fC[29] + h[1] * fC[31];
        fC[8] += h[3] * fC[30] + h[2] * fC[31];
        c = fC[31] + h[3] * fC[35];
        fC[9] += h[3] * (c + fC[31]);
        fC[31] = c;

        fC[10] += h[4] * fC[28] + h[0] * fC[32];
        fC[11] += h[4] * fC[29] + h[1] * fC[32];
        fC[12] += h[4] * fC[30] + h[2] * fC[32];
        fC[13] += h[4] * fC[31] + h[3] * fC[32];
        c = fC[32] + h[4] * fC[35];
        fC[14] += h[4] * (c + fC[32]);
        fC[32] = c;

        fC[15] += h[5] * fC[28] + h[0] * fC[33];
        fC[16] += h[5] * fC[29] + h[1] * fC[33];
        fC[17] += h[5] * fC[30] + h[2] * fC[33];
        fC[18] += h[5] * fC[31] + h[3] * fC[33];
        fC[19] += h[5] * fC[32] + h[4] * fC[33];
        c = fC[33] + h[5] * fC[35];
        fC[20] += h[5] * (c + fC[33]);
        fC[33] = c;

        fC[21] += h[0] * fC[34];
        fC[22] += h[1] * fC[34];
        fC[23] += h[2] * fC[34];
        fC[24] += h[3] * fC[34];
        fC[25] += h[4] * fC[34];
        fC[26] += h[5] * fC[34];
    }

    public void setBField(double Bx, double By, double Bz) {
        _Bx = Bx;
        _By = By;
        _Bz = Bz;
    }

    void MultQSQt(double[][] Q, double[] S, double[] SOut) {
        //* Matrix multiplication Q*S*Q^T, Q - square matrix, S - symmetric
        final int kN = 8;
        double[] mA = new double[kN * kN];

        for (int i = 0, ij = 0; i < kN; i++) {
            for (int j = 0; j < kN; j++, ++ij) {
                mA[ij] = 0;
                for (int k = 0; k < kN; ++k) {
                    mA[ij] += S[(k <= i) ? i * (i + 1) / 2 + k : k * (k + 1) / 2 + i] * Q[j][k];
                }
            }
        }

        for (int i = 0; i < kN; i++) {
            for (int j = 0; j <= i; j++) {
                int ij = (j <= i) ? i * (i + 1) / 2 + j : j * (j + 1) / 2 + i;
                SOut[ij] = 0;
                for (int k = 0; k < kN; k++) {
                    SOut[ij] += Q[i][k] * mA[k * kN + j];
                }
            }
        }
    }

    public void SetMassConstraint(double[] mP, double[] mC, double[][] mJ, double mass) {
        //* Set nonlinear mass constraint (Mass) on the state vector mP with a covariance matrix mC.

        final double energy2 = mP[6] * mP[6];
        final double p2 = mP[3] * mP[3] + mP[4] * mP[4] + mP[5] * mP[5];
        final double mass2 = mass * mass;

        final double a = energy2 - p2 + 2. * mass2;
        final double b = -2. * (energy2 + p2);
        final double c = energy2 - p2 - mass2;

        double lambda = 0;
        if (abs(b) > 1.e-10) {
            lambda = -c / b;
        }

        double d = 4. * energy2 * p2 - mass2 * (energy2 - p2 - 2. * mass2);
        if (d >= 0 && abs(a) > 1.e-10) {
            lambda = (energy2 + p2 - sqrt(d)) / a;
        }

        if (mP[6] < 0) //If energy < 0 we need a lambda < 0
        {
            lambda = -1000000.; //Empirical, a better solution should be found
        }
        int iIter = 0;
        for (iIter = 0; iIter < 100; iIter++) {
            double lambda2 = lambda * lambda;
            double lambda4 = lambda2 * lambda2;

            double lambda0 = lambda;

            double f = -mass2 * lambda4 + a * lambda2 + b * lambda + c;
            double df = -4. * mass2 * lambda2 * lambda + 2. * a * lambda + b;
            if (abs(df) < 1.e-10) {
                break;
            }
            lambda -= f / df;
            if (abs(lambda0 - lambda) < 1.e-8) {
                break;
            }
        }

        final double lpi = 1. / (1. + lambda);
        final double lmi = 1. / (1. - lambda);
        final double lp2i = lpi * lpi;
        final double lm2i = lmi * lmi;

        double lambda2 = lambda * lambda;

        double dfl = -4. * mass2 * lambda2 * lambda + 2. * a * lambda + b;
        double[] dfx = new double[7];
        dfx[0] = -2. * (1. + lambda) * (1. + lambda) * mP[3];
        dfx[1] = -2. * (1. + lambda) * (1. + lambda) * mP[4];
        dfx[2] = -2. * (1. + lambda) * (1. + lambda) * mP[5];
        dfx[3] = 2. * (1. - lambda) * (1. - lambda) * mP[6];
        double[] dlx = {1, 1, 1, 1};
        if (abs(dfl) > 1.e-10) {
            for (int i = 0; i < 4; i++) {
                dlx[i] = -dfx[i] / dfl;
            }
        }

        double[] dxx = {mP[3] * lm2i, mP[4] * lm2i, mP[5] * lm2i, -mP[6] * lp2i};

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                mJ[i][j] = 0;
            }
        }
        mJ[0][0] = 1.;
        mJ[1][1] = 1.;
        mJ[2][2] = 1.;

        for (int i = 3; i < 7; i++) {
            for (int j = 3; j < 7; j++) {
                mJ[i][j] = dlx[j - 3] * dxx[i - 3];
            }
        }

        for (int i = 3; i < 6; i++) {
            mJ[i][i] += lmi;
        }
        mJ[6][6] += lpi;

        double[][] mCJ = new double[7][7];

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                mCJ[i][j] = 0;
                for (int k = 0; k < 7; k++) {
                    mCJ[i][j] += mC[IJ(i, k)] * mJ[j][k];
                }
            }
        }

        for (int i = 0; i < 7; ++i) {
            for (int j = 0; j <= i; ++j) {
                mC[IJ(i, j)] = 0;
                for (int l = 0; l < 7; l++) {
                    mC[IJ(i, j)] += mJ[i][l] * mCJ[l][j];
                }
            }
        }

        mP[3] *= lmi;
        mP[4] *= lmi;
        mP[5] *= lmi;
        mP[6] *= lpi;
    }

    static int IJ(int i, int j) {
        return (j <= i) ? i * (i + 1) / 2 + j : j * (j + 1) / 2 + i;
    }

    void GetMeasurement(double[] XYZ, double[] m, double[] V) {
        //* Get additional covariances V used during measurement

        double[] b = new double[3];
        GetFieldValue(XYZ, b);
        final double kCLight = 0.000299792458;
        b[0] *= kCLight;
        b[1] *= kCLight;
        b[2] *= kCLight;
// std::cout << "   DStoPoint "<< (GetDStoPoint(XYZ)) << std::endl;
        Transport(GetDStoPoint(XYZ), m, V);
//std::cout << "x " << XYZ[0] << " " << m[0] <<"   y " << XYZ[1] << " " << m[1] <<"   z " << XYZ[2] << " " << m[2] <<std::endl;
        double sigmaS = GetSCorrection(m, XYZ);

        double[] h = new double[6];

        h[0] = m[3] * sigmaS;
        h[1] = m[4] * sigmaS;
        h[2] = m[5] * sigmaS;
        h[3] = (h[1] * b[2] - h[2] * b[1]) * GetQ();
        h[4] = (h[2] * b[0] - h[0] * b[2]) * GetQ();
        h[5] = (h[0] * b[1] - h[1] * b[0]) * GetQ();

        V[0] += h[0] * h[0];
        V[1] += h[1] * h[0];
        V[2] += h[1] * h[1];
        V[3] += h[2] * h[0];
        V[4] += h[2] * h[1];
        V[5] += h[2] * h[2];

        V[6] += h[3] * h[0];
        V[7] += h[3] * h[1];
        V[8] += h[3] * h[2];
        V[9] += h[3] * h[3];

        V[10] += h[4] * h[0];
        V[11] += h[4] * h[1];
        V[12] += h[4] * h[2];
        V[13] += h[4] * h[3];
        V[14] += h[4] * h[4];

        V[15] += h[5] * h[0];
        V[16] += h[5] * h[1];
        V[17] += h[5] * h[2];
        V[18] += h[5] * h[3];
        V[19] += h[5] * h[4];
        V[20] += h[5] * h[5];
    }

    double GetSCorrection(double[] Part, double[] XYZ) {
        //* Get big enough correction for S error to let the particle Part be fitted to XYZ point

        double[] d = {XYZ[0] - Part[0], XYZ[1] - Part[1], XYZ[2] - Part[2]};
        double p2 = Part[3] * Part[3] + Part[4] * Part[4] + Part[5] * Part[5];
//  double sigmaS = (p2>1.e-4) ? ( 10.1+3.*sqrt( d[0]*d[0]+d[1]*d[1]+d[2]*d[2]) )/sqrt(p2) : 1.;
        double sigmaS = (p2 > 1.e-4) ? (0.1 + 10. * sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])) / sqrt(p2) : 1.;
        return sigmaS;
    }

    public double GetDStoPoint(double[] xyz) {
        return GetDStoPointCBM(xyz);
    }

    double GetDStoPointCBM(double[] xyz) {
        //* Transport the particle on dS, output to P[],C[], for CBM field

        double dS = 0;
        //Linear approximation

        double[] fld = new double[3];
        GetFieldValue(fP, fld);
// 
//   GetDStoParticleBy(fld[1],p,dS,dS1);
        dS = GetDStoPointLine(xyz);

        if (fQ == 0) {
            return dS;
        }

        dS = GetDStoPointBy(fld[1], xyz);

//  const double &px   = fP[3], &py   = fP[4], &pz   = fP[5];
        // construct coefficients 
// int NIt;
//   for(int i=0; i<100; i++)
//   {
// //std::cout << "  dS temp "<<dS << std::endl;
//     double x[3], dx[3], ddx[3];
// 
//     GetDSIter(*this, dS, x, dx, ddx);
// 
//     double dr[3] = { x[0] - xyz[0], x[1] - xyz[1], x[2] - xyz[2] };
// 
//     double dS0 = dS;
// 
//     double f  = dx[0] * dr[0] + dx[1] * dr[1] + dx[2] * dr[2];
//     double df  = ddx[0] * dr[0] + ddx[1] * dr[1] + ddx[2] * dr[2] + dx[0]*dx[0] + dx[1]*dx[1] + dx[2]*dx[2];
// 
//     dS = dS - f/df;
//     NIt++;
//     if(fabs(dS - dS0) < fabs(dS)*0.001) break;
//   }
//std::cout << "NIt  " << NIt <<"  dS  "<<  dS << std::endl;
        return dS;
    }

    public double GetDStoPointLine(double[] xyz) {
        //* Get dS to a certain space point without field

        double p2 = fP[3] * fP[3] + fP[4] * fP[4] + fP[5] * fP[5];
        if (p2 < 1.e-4) {
            p2 = 1;
        }
        return (fP[3] * (xyz[0] - fP[0]) + fP[4] * (xyz[1] - fP[1]) + fP[5] * (xyz[2] - fP[2])) / p2;
    }

    double GetDStoPointBy(double B, double[] xyz) {

        //* Get dS to a certain space point for Bz field
        final double kCLight = 0.000299792458;
        double bq = B * fQ * kCLight;
        double pt2 = fP[3] * fP[3] + fP[5] * fP[5];
        if (pt2 < 1.e-4) {
            return 0;
        }
        double dx = xyz[0] - fP[0];
        double dy = -xyz[2] + fP[2];
        double a = dx * fP[3] - dy * fP[5];
        double dS;

        if (abs(bq) < 1.e-8) {
            dS = a / pt2;
        } else {
            dS = atan2(bq * a, pt2 + bq * (dy * fP[3] + dx * fP[5])) / bq;
        }

        return dS;
    }

//TODO eliminate these nested calls...
    public void GetDStoParticle(KFParticle p, double[] dsArray) {
        GetDStoParticleXY(p, dsArray);
    }

    void GetDStoParticleXY(KFParticle p, double[] dsArray) {
        GetDStoParticleCBM(p, dsArray);

    }

    void GetDStoParticleCBM(KFParticle p, double[] dsArray) {
        //* Transport the particle on dS, output to P[],C[], for CBM field

        GetDStoParticleLine(p, dsArray);
        if (fQ == 0) {
            return;
        }

        double[] fld = new double[3];
        GetFieldValue(fP, fld);

        double[] fld1 = new double[3];
        GetFieldValue(p.fP, fld1);

        GetDStoParticleBy((fld[1] + fld1[1]) / 2, p, dsArray);

//   construct coefficients 
// int NIt;
//   for(int i=0; i<10; i++)
//   {
// /*std::cout << "  dS temp "<<dS <<  "  dS1 temp "<< dS1 << std::endl;*/
//     double x[3],  dx[3],  ddx[3];
//     double x1[3], dx1[3], ddx1[3];
// 
//     GetDSIter(*this, dS, x, dx, ddx);
//     GetDSIter(p, dS1, x1, dx1, ddx1);
// 
//     double dr[3] = { x[0] - x1[0], x[1] - x1[1], x[2] - x1[2] };
// 
//     double dS0 = dS;
//     double dS10 = dS1;
// 
//     double f    = dx[0]  * dr[0] + dx[1]  * dr[1] + dx[2]  * dr[2];
//     double dfs  = ddx[0] * dr[0] + ddx[1] * dr[1] + ddx[2] * dr[2] + dx[0]*dx[0] + dx[1]*dx[1] + dx[2]*dx[2];
//     double dfs1 = -ddx[0] * ddx1[0] - ddx[1] * ddx1[1] - ddx[2] * ddx1[2];
// 
//     double f1    = dx1[0]  * dr[0] + dx1[1]  * dr[1] + dx1[2]  * dr[2];
//     double df1s1 = ddx1[0] * dr[0] + ddx1[1] * dr[1] + ddx1[2] * dr[2] - dx1[0]*dx1[0] - dx1[1]*dx1[1] - dx1[2]*dx1[2];
//     double df1s  = -dfs1;
// 
//     double det = dfs*df1s1 - dfs1*df1s;
//     if(fabs(det) < 1.e-8) det = 1.e-8;
// 
//     double DS  = dfs1*f1 - f*df1s1;
//     double DS1 = f*df1s - f1*dfs;
// 
//     dS  += DS/det;
//     dS1 += DS1/det;
//     NIt++;
// /*    if( (fabs(dS - dS0) < fabs(dS)*0.001) && (fabs(dS1 - dS10) < fabs(dS1)*0.001)) break;*/
//   }
//std::cout << "NIt  " << NIt <<"  dS  "<<  dS << "  dS1  "<<  dS1 << std::endl;
    }

    void GetDStoParticleLine(KFParticle p, double[] dsArray) {
        double p12 = fP[3] * fP[3] + fP[4] * fP[4] + fP[5] * fP[5];
        double p22 = p.fP[3] * p.fP[3] + p.fP[4] * p.fP[4] + p.fP[5] * p.fP[5];
        double p1p2 = fP[3] * p.fP[3] + fP[4] * p.fP[4] + fP[5] * p.fP[5];

        double drp1 = fP[3] * (p.fP[0] - fP[0]) + fP[4] * (p.fP[1] - fP[1]) + fP[5] * (p.fP[2] - fP[2]);
        double drp2 = p.fP[3] * (p.fP[0] - fP[0]) + p.fP[4] * (p.fP[1] - fP[1]) + p.fP[5] * (p.fP[2] - fP[2]);

        double detp = p1p2 * p1p2 - p12 * p22;
        if (abs(detp) < 1.e-4) {
            detp = 1; //TODO correct!!!
        }
        dsArray[0] = (drp2 * p1p2 - drp1 * p22) / detp;
        dsArray[1] = (drp2 * p12 - drp1 * p1p2) / detp;
    }

    void GetDStoParticleBy(double B, KFParticle p, double[] dsArray) {
        //cng this seems NOT to work if B=0.
        if (B == 0.) {
            return;
        }
        //* Get dS to another particle for Bz field
        double px = fP[3];
        double py = -fP[5];
        double pz = fP[4];

        double px1 = p.fP[3];
        double py1 = -p.fP[5];
        double pz1 = p.fP[4];

        final double kCLight = 0.000299792458;

        double bq = B * fQ * kCLight;
        double bq1 = B * p.fQ * kCLight;
        double s = 0, ds = 0, s1 = 0, ds1 = 0;

        if (abs(bq) > 1.e-8 || abs(bq1) > 1.e-8) {

            double dx = (p.fP[0] - fP[0]);
            double dy = (-p.fP[2] + fP[2]);
            double d2 = (dx * dx + dy * dy);

            double p2 = (px * px + py * py);
            double p21 = (px1 * px1 + py1 * py1);

            if (abs(p2) < 1.e-8 || abs(p21) < 1.e-8) {
                dsArray[0] = 0.;
                dsArray[1] = 0.;
                return;
            }

            double a = (px * py1 - py * px1);
            double b = (px * px1 + py * py1);

            double ldx = bq * bq1 * dx - bq1 * py + bq * py1;
            double ldy = bq * bq1 * dy + bq1 * px - bq * px1;
            double l2 = ldx * ldx + ldy * ldy;

            double cS = bq1 * p2 + bq * bq1 * (dy * px - dx * py) - bq * b;
            double cS1 = bq * p21 - bq * bq1 * (dy * px1 - dx * py1) - bq1 * b;

            double ca = bq * bq * bq1 * d2 + 2 * (cS + bq * bq * (py1 * dx - px1 * dy));
            double ca1 = bq * bq1 * bq1 * d2 + 2 * (cS1 - bq1 * bq1 * (py * dx - px * dy));

            double sa = 4 * l2 * p2 - ca * ca;
            double sa1 = 4 * l2 * p21 - ca1 * ca1;

            if (sa < 0) {
                sa = 0;
            }
            if (sa1 < 0) {
                sa1 = 0;
            }

            if (abs(bq) > 1.e-8) {
                s = atan2(bq * (bq1 * (dx * px + dy * py) + a), cS) / bq;
                ds = atan2(sqrt(sa), ca) / bq;
            } else {
                s = ((dx * px + dy * py) + (py * px1 - px * py1) / bq1) / p2;
                ds = s * s - (d2 - 2 * (px1 * dy - py1 * dx) / bq1) / p2;
                if (ds < 0) {
                    ds = 0;
                }
                ds = sqrt(ds);
            }

            if (abs(bq1) > 1.e-8) {
                s1 = atan2(-bq1 * (bq * (dx * px1 + dy * py1) + a), cS1) / bq1;
                ds1 = atan2(sqrt(sa1), ca1) / bq1;
            } else {
                s1 = (-(dx * px1 + dy * py1) + (py * px1 - px * py1) / bq) / p21;
                ds1 = s1 * s1 - (d2 + 2 * (px * dy - py * dx) / bq) / p21;
                if (ds1 < 0) {
                    ds1 = 0;
                }
                ds1 = sqrt(ds1);
            }
        }
        double[] ss = new double[2];
        double[] ss1 = new double[2];
        double[][] g = new double[2][5];
        double[][] g1 = new double[2][5];

        ss[0] = s + ds;
        ss[1] = s - ds;
        ss1[0] = s1 + ds1;
        ss1[1] = s1 - ds1;

        for (int i = 0; i < 2; i++) {
            double bs = bq * ss[i];
            double c = cos(bs), sss = sin(bs);
            double cB, sB;
            if (abs(bq) > 1.e-8) {
                cB = (1 - c) / bq;
                sB = sss / bq;
            } else {
                final double kOvSqr6 = 1. / sqrt(6.);
                sB = (1. - bs * kOvSqr6) * (1. + bs * kOvSqr6) * ss[i];
                cB = .5 * sB * bs;
            }
            g[i][0] = fP[0] + sB * px + cB * py;
            g[i][1] = -fP[2] - cB * px + sB * py;
            g[i][2] = fP[1] + ss[i] * pz;
            g[i][3] = +c * px + sss * py;
            g[i][4] = -sss * px + c * py;

            bs = bq1 * ss1[i];
            c = cos(bs);
            sss = sin(bs);
            if (abs(bq1) > 1.e-8) {
                cB = (1 - c) / bq1;
                sB = sss / bq1;
            } else {
                final double kOvSqr6 = 1. / sqrt(6.);
                sB = (1. - bs * kOvSqr6) * (1. + bs * kOvSqr6) * ss1[i];
                cB = .5 * sB * bs;
            }

            g1[i][0] = p.fP[0] + sB * px1 + cB * py1;
            g1[i][1] = -p.fP[2] - cB * px1 + sB * py1;
            g1[i][2] = p.fP[1] + ss1[i] * pz1;
            g1[i][3] = +c * px1 + sss * py1;
            g1[i][4] = -sss * px1 + c * py1;
        }

        int i = 0, i1 = 0;

        double dMin = 1.e10;
        for (int j = 0; j < 2; j++) {
            for (int j1 = 0; j1 < 2; j1++) {
                double xx = g[j][0] - g1[j1][0];
                double yy = g[j][1] - g1[j1][1];
                double zz = g[j][2] - g1[j1][2];
                double d = xx * xx + yy * yy + zz * zz;
                if (d < dMin) {
                    dMin = d;
                    i = j;
                    i1 = j1;
                }
            }
        }

        dsArray[0] = ss[i];
        dsArray[1] = ss1[i1];
    }

} // end of KFParticle.java
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

////---------------------------------------------------------------------------------
//// The KFParticleBase class
//// .
//// @author  S.Gorbunov, I.Kisel, I.Kulakov, M.Zyzak
//// @version 1.0
//// @since   13.05.07
//// 
//// Class to reconstruct and store the decayed particle parameters.
//// The method is described in CBM-SOFT note 2007-003, 
//// ``Reconstruction of decayed particles based on the Kalman filter'', 
//// http://www.gsi.de/documents/DOC-2007-May-14-1.pdf
////
//// This class describes general mathematics which is used by KFParticle class
//// 
////  -= Copyright &copy ALICE HLT and CBM L1 Groups =-
////_________________________________________________________________________________
//
//
//#ifndef KFPARTICLEBASE_H
//#define KFPARTICLEBASE_H
//
//#include <vector>
//
//class KFParticleBase  {
//  
// public:
//
//  //*
//  //* ABSTRACT METHODS HAVE TO BE DEFINED IN USER CLASS 
//  //* 
//
//  //* Virtual method to access the magnetic field
//
//  virtual void GetFieldValue(const double xyz[], double B[]) const = 0;
//  
//  //* Virtual methods needed for particle transportation 
//  //* One can use particular implementations for collider (only Bz component) 
//  //* geometry and for fixed-target (CBM-like) geometry which are provided below 
//  //* in TRANSPORT section
// 
//  //* Get dS to xyz[] space point 
//
//  virtual double GetDStoPoint( const double xyz[] ) const = 0;
//
//  //* Get dS to other particle p (dSp for particle p also returned) 
//
//  virtual void GetDStoParticle( const KFParticleBase &p, 
//                                double &DS, double &DSp ) const = 0;
//  
//  //* Transport on dS value along trajectory, output to P,C
//
//  virtual void Transport( double dS, double P[], double C[] ) const = 0;
//
//
//
//  //*
//  //*  INITIALIZATION
//  //*
//
//  //* Constructor 
//
//  KFParticleBase();
//
//  //* Destructor 
//
//  virtual ~KFParticleBase() { ; }
//
// //* Initialisation from "cartesian" coordinates ( X Y Z Px Py Pz )
// //* Parameters, covariance matrix, charge, and mass hypothesis should be provided 
//
//  void Initialize( const double Param[], const double Cov[], int Charge, double Mass );
//
//  //* Initialise covariance matrix and set current parameters to 0.0 
//
//  void Initialize();
//
//  //* Set decay vertex parameters for linearisation 
//
//  void SetVtxGuess( double x, double y, double z );
//
//  //* Set consruction method
//
//  void SetConstructMethod(int m) {fConstructMethod = m;}
//
//  //* Set and get mass hypothesis of the particle
//  void SetMassHypo(double m) { fMassHypo = m;}
//  const double& GetMassHypo() const { return fMassHypo; }
//
//  //* Returns the sum of masses of the daughters
//  const double& GetSumDaughterMass() const {return SumDaughterMass;}
//
//  //*
//  //*  ACCESSORS
//  //*
//
//  //* Simple accessors 
//
//  double GetX    () const { return fP[0]; }
//  double GetY    () const { return fP[1]; }
//  double GetZ    () const { return fP[2]; }
//  double GetPx   () const { return fP[3]; }
//  double GetPy   () const { return fP[4]; }
//  double GetPz   () const { return fP[5]; }
//  double GetE    () const { return fP[6]; }
//  double GetS    () const { return fP[7]; }
//  int    GetQ    () const { return fQ;    }
//  double GetChi2 () const { return fChi2; }
//  int    GetNDF  () const { return fNDF;  }
//
//  const double& X    () const { return fP[0]; }
//  const double& Y    () const { return fP[1]; }
//  const double& Z    () const { return fP[2]; }
//  const double& Px   () const { return fP[3]; }
//  const double& Py   () const { return fP[4]; }
//  const double& Pz   () const { return fP[5]; }
//  const double& E    () const { return fP[6]; }
//  const double& S    () const { return fP[7]; }
//  const int   & Q    () const { return fQ;    }
//  const double& Chi2 () const { return fChi2; }
//  const int   & NDF  () const { return fNDF;  }
//  
//  double GetParameter ( int i )        const { return fP[i];       }
//  double GetCovariance( int i )        const { return fC[i];       }
//  double GetCovariance( int i, int j ) const { return fC[IJ(i,j)]; }
//
//  //* Accessors with calculations( &value, &estimated sigma )
//  //* error flag returned (0 means no error during calculations) 
//
//  int GetMomentum    ( double &P, double &SigmaP ) const ;
//  int GetPt          ( double &Pt, double &SigmaPt ) const ;
//  int GetEta         ( double &Eta, double &SigmaEta ) const ;
//  int GetPhi         ( double &Phi, double &SigmaPhi ) const ;
//  int GetMass        ( double &M, double &SigmaM ) const ;
//  int GetDecayLength ( double &L, double &SigmaL ) const ;
//  int GetDecayLengthXY ( double &L, double &SigmaL ) const ;
//  int GetLifeTime    ( double &T, double &SigmaT ) const ;
//  int GetR           ( double &R, double &SigmaR ) const ;
//
//  //*
//  //*  MODIFIERS
//  //*
//  
//  double & X    () { return fP[0]; }
//  double & Y    () { return fP[1]; }
//  double & Z    () { return fP[2]; }
//  double & Px   () { return fP[3]; }
//  double & Py   () { return fP[4]; }
//  double & Pz   () { return fP[5]; }
//  double & E    () { return fP[6]; }
//  double & S    () { return fP[7]; }
//  int    & Q    () { return fQ;    }
//  double & Chi2 () { return fChi2; }
//  int    & NDF  () { return fNDF;  }
//
//  double & Parameter ( int i )        { return fP[i];       }
//  double & Covariance( int i )        { return fC[i];       }
//  double & Covariance( int i, int j ) { return fC[IJ(i,j)]; }
//
//
//  //* 
//  //* CONSTRUCTION OF THE PARTICLE BY ITS DAUGHTERS AND MOTHER
//  //* USING THE KALMAN FILTER METHOD
//  //*
//
//
//  //* Simple way to add daughter ex. D0+= Pion; 
//
//  void operator +=( const KFParticleBase &Daughter );  
//
//  //* Add daughter track to the particle 
//
//  void AddDaughter( const KFParticleBase &Daughter );
//
//  void AddDaughterWithEnergyFit( const KFParticleBase &Daughter );
//  void AddDaughterWithEnergyCalc( const KFParticleBase &Daughter );
//  void AddDaughterWithEnergyFitMC( const KFParticleBase &Daughter ); //with mass constrained
//
//  //* Set production vertex 
//
//  void SetProductionVertex( const KFParticleBase &Vtx );
//
//  //* Set mass constraint 
//
//  void SetNonlinearMassConstraint( double Mass );
//  void SetMassConstraint( double Mass, double SigmaMass = 0 );
//
//  //* Set no decay length for resonances
//
//  void SetNoDecayLength();
//
//
//  //* Everything in one go  
//
//  void Construct( const KFParticleBase *vDaughters[], int nDaughters, 
//                 const KFParticleBase *ProdVtx=0,   double Mass=-1, bool IsConstrained=0  );
//
//
//  //*
//  //*                   TRANSPORT
//  //* 
//  //*  ( main transportation parameter is S = SignedPath/Momentum )
//  //*  ( parameters of decay & production vertices are stored locally )
//  //*
//
//
//  //* Transport the particle to its decay vertex 
//
//  void TransportToDecayVertex();
//
//  //* Transport the particle to its production vertex 
//
//  void TransportToProductionVertex();
//
//  //* Transport the particle on dS parameter (SignedPath/Momentum) 
//
//  void TransportToDS( double dS );
//
//  //* Particular extrapolators one can use 
//
//  double GetDStoPointBz( double Bz, const double xyz[] ) const;
//  double GetDStoPointBy( double By, const double xyz[] ) const;
//
//  void GetDStoParticleBz( double Bz, const KFParticleBase &p, double &dS, double &dS1 ) const ;
//  void GetDStoParticleBy( double B, const KFParticleBase &p, double &dS, double &dS1 ) const ;
//
//  double GetDStoPointCBM( const double xyz[] ) const;
//  void GetDStoParticleCBM( const KFParticleBase &p, double &dS, double &dS1 ) const ;
//
//  void TransportBz( double Bz, double dS, double P[], double C[] ) const;
//  void TransportCBM( double dS, double P[], double C[] ) const;  
//
//
//  //* 
//  //* OTHER UTILITIES
//  //*
//
//  //* Calculate distance from another object [cm]
//
//  double GetDistanceFromVertex( const double vtx[] ) const;
//  double GetDistanceFromVertex( const KFParticleBase &Vtx ) const;
//  double GetDistanceFromParticle( const KFParticleBase &p ) const;
//
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//  //* v = [xyz], Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix
//
//  double GetDeviationFromVertex( const double v[], 
//                                   const double Cv[]=0 ) const;
//  double GetDeviationFromVertex( const KFParticleBase &Vtx ) const;
//  double GetDeviationFromParticle( const KFParticleBase &p ) const;  
//
//  //* Subtract the particle from the vertex  
//
//  void SubtractFromVertex( KFParticleBase &Vtx ) const;
//
//  //* Special method for creating gammas
//
//  void ConstructGammaBz( const KFParticleBase &daughter1,
//                         const KFParticleBase &daughter2, double Bz  );
//
//  //* return parameters for the Armenteros-Podolanski plot
//  static void GetArmenterosPodolanski(KFParticleBase& positive, KFParticleBase& negative, double QtAlfa[2] );
//
//  //* Rotates the KFParticle object around OZ axis, OZ axis is set by the vertex position
//  void RotateXY(double angle, double Vtx[3]);
//
//  int Id() const { return fId; };
//  int NDaughters() const { return fDaughtersIds.size(); };
//  const std::vector<int>& DaughterIds() const { return fDaughtersIds; };
//  
//  void SetId( int id ){ fId = id; }; // should be always used (manualy)
//  void AddDaughterId( int id ){ fDaughtersIds.push_back(id); };
//
//  void SetPDG ( int pdg ) { fPDG = pdg; }
//  int GetPDG () const { return fPDG; }
//
// protected:
//
//  static int IJ( int i, int j ){ 
//    return ( j<=i ) ? i*(i+1)/2+j :j*(j+1)/2+i;
//  }
//
//  double & Cij( int i, int j ){ return fC[IJ(i,j)]; }
//
//  void Convert( bool ToProduction );
//  void TransportLine( double S, double P[], double C[] ) const ;
//  double GetDStoPointLine( const double xyz[] ) const;
//  void GetDStoParticleLine( const KFParticleBase &p, double &dS, double &dS1 ) const ;
//
//  void GetDSIter(const KFParticleBase &p, double const &dS, double x[3], double dx[3], double ddx[3]) const;
//
//  static bool InvertSym3( const double A[], double Ainv[] );
//
//  static void MultQSQt( const double Q[], const double S[], 
//                        double SOut[] );
//
//  static double GetSCorrection( const double Part[], const double XYZ[] );
//
//  void GetMeasurement( const double XYZ[], double m[], double V[] ) const ;
//
//  //* Mass constraint function. is needed for the nonlinear mass constraint and a fit with mass constraint
//  void SetMassConstraint( double[] mP, double[] mC, double mJ[7][7], double mass );
//
//  double fP[8];  //* Main particle parameters {X,Y,Z,Px,Py,Pz,E,S[=DecayLength/P]}
//  double fC[36]; //* Low-triangle covariance matrix of fP
//  int    fQ;     //* Particle charge 
//  int    fNDF;   //* Number of degrees of freedom 
//  double fChi2;  //* Chi^2
//
//  double fSFromDecay; //* Distance from decay vertex to current position
//
//  bool fAtProductionVertex; //* Flag shows that the particle error along
//                              //* its trajectory is taken from production vertex    
//
//  double fVtxGuess[3];  //* Guess for the position of the decay vertex 
//                          //* ( used for linearisation of equations )
//
//  bool fIsLinearized;   //* Flag shows that the guess is present
//
//  int fConstructMethod; //* Determines the method for the particle construction. 
//  //* 0 - Energy considered as an independent veriable, fitted independently from momentum, without any constraints on mass
//  //* 1 - Energy considered as a dependent variable, calculated from the momentum and mass hypothesis
//  //* 2 - Energy considered as an independent variable, fitted independently from momentum, with constraints on mass of daughter particle
//
//  double SumDaughterMass;  //* sum of the daughter particles masses
//  double fMassHypo;  //* sum of the daughter particles masse
//
//  int fId;                   // id of particle
//  std::vector<int> fDaughtersIds; // id of particles it created from. if size == 1 then this is id of track. TODO use in functions. why unsigned short int doesn't work???
//
//  int fPDG; // pdg hypothesis
//
//};
//
//#endif 
////---------------------------------------------------------------------------------
//// Implementation of the KFParticleBase class
//// .
//// @author  S.Gorbunov, I.Kisel, I.Kulakov, M.Zyzak
//// @version 1.0
//// @since   13.05.07
//// 
//// Class to reconstruct and store the decayed particle parameters.
//// The method is described in CBM-SOFT note 2007-003, 
//// ``Reconstruction of decayed particles based on the Kalman filter'', 
//// http://www.gsi.de/documents/DOC-2007-May-14-1.pdf
////
//// This class describes general mathematics which is used by KFParticle class
//// 
////  -= Copyright &copy ALICE HLT and CBM L1 Groups =-
////_________________________________________________________________________________
//
//
//#include "KFParticleBase.h"
//
//#include <iostream>
//#include <cmath>
//
//KFParticleBase() :fQ(0), fNDF(-3), fChi2(0), fSFromDecay(0), fAtProductionVertex(0), fIsLinearized(0),
//  fConstructMethod(2), SumDaughterMass(0), fMassHypo(-1), fId(-1), fDaughtersIds(), fPDG(0)
//{ 
//  //* Constructor 
//
//  Initialize();
//}
//
//void Initialize( const double Param[], const double Cov[], int Charge, double Mass )
//{
//  // Constructor from "cartesian" track, particle mass hypothesis should be provided
//  //
//  // Param[6] = { X, Y, Z, Px, Py, Pz } - position and momentum
//  // Cov [21] = lower-triangular part of the covariance matrix:
//  //
//  //                (  0  .  .  .  .  . )
//  //                (  1  2  .  .  .  . )
//  //  Cov. matrix = (  3  4  5  .  .  . ) - numbering of covariance elements in Cov[]
//  //                (  6  7  8  9  .  . )
//  //                ( 10 11 12 13 14  . )
//  //                ( 15 16 17 18 19 20 )
//
//
//  for( int i=0; i<6 ; i++ ) fP[i] = Param[i];
//  for( int i=0; i<21; i++ ) fC[i] = Cov[i];
//
//  double energy = sqrt( Mass*Mass + fP[3]*fP[3] + fP[4]*fP[4] + fP[5]*fP[5]);
//  fP[6] = energy;
//  fP[7] = 0;
//  fQ = Charge;
//  fNDF = 0;
//  fChi2 = 0;
//  fAtProductionVertex = 0;
//  fIsLinearized = 0;
//  fSFromDecay = 0;
//
//  double energyInv = 1./energy;
//  double 
//    h0 = fP[3]*energyInv,
//    h1 = fP[4]*energyInv,
//    h2 = fP[5]*energyInv;
//
//  fC[21] = h0*fC[ 6] + h1*fC[10] + h2*fC[15];
//  fC[22] = h0*fC[ 7] + h1*fC[11] + h2*fC[16];
//  fC[23] = h0*fC[ 8] + h1*fC[12] + h2*fC[17];
//  fC[24] = h0*fC[ 9] + h1*fC[13] + h2*fC[18];
//  fC[25] = h0*fC[13] + h1*fC[14] + h2*fC[19];
//  fC[26] = h0*fC[18] + h1*fC[19] + h2*fC[20];
//  fC[27] = ( h0*h0*fC[ 9] + h1*h1*fC[14] + h2*h2*fC[20] 
//            + 2*(h0*h1*fC[13] + h0*h2*fC[18] + h1*h2*fC[19] ) );
//  for( int i=28; i<36; i++ ) fC[i] = 0;
//  fC[35] = 1.;
//
//  SumDaughterMass = Mass;
//  fMassHypo = Mass;
//}
//
//void Initialize()
//{
//  //* Initialise covariance matrix and set current parameters to 0.0 
//
//  for( int i=0; i<8; i++) fP[i] = 0;
//  for(int i=0;i<36;++i) fC[i]=0.;
//  fC[0] = fC[2] = fC[5] = 100.;
//  fC[35] = 1.;
//  fNDF  = -3;
//  fChi2 =  0.;
//  fQ = 0;
//  fSFromDecay = 0;
//  fAtProductionVertex = 0;
//  fVtxGuess[0]=fVtxGuess[1]=fVtxGuess[2]=0.;
//  fIsLinearized = 0;
//  SumDaughterMass = 0;
//  fMassHypo = -1;
//}
//
//void SetVtxGuess( double x, double y, double z )
//{
//  //* Set decay vertex parameters for linearisation 
//
//  fVtxGuess[0] = x;
//  fVtxGuess[1] = y;
//  fVtxGuess[2] = z;
//  fIsLinearized = 1;
//}
//
//int GetMomentum( double &p, double &error )  const 
//{
//  //* Calculate particle momentum
//
//  double x = fP[3];
//  double y = fP[4];
//  double z = fP[5];
//  double x2 = x*x;
//  double y2 = y*y;
//  double z2 = z*z;
//  double p2 = x2+y2+z2;
//  p = sqrt(p2);
//  error = (x2*fC[9]+y2*fC[14]+z2*fC[20] + 2*(x*y*fC[13]+x*z*fC[18]+y*z*fC[19]) );
//  if( error>1.e-16 && p>1.e-4 ){
//    error = sqrt(error)/p;
//    return 0;
//  }
//  error = 1.e8;
//  return 1;
//}
//
//int GetPt( double &pt, double &error )  const 
//{
//  //* Calculate particle transverse momentum
//
//  double px = fP[3];
//  double py = fP[4];
//  double px2 = px*px;
//  double py2 = py*py;
//  double pt2 = px2+py2;
//  pt = sqrt(pt2);
//  error = (px2*fC[9] + py2*fC[14] + 2*px*py*fC[13] );
//  if( error>0 && pt>1.e-4 ){
//    error = sqrt(error)/pt;
//    return 0;
//  }
//  error = 1.e10;
//  return 1;
//}
//
//int GetEta( double &eta, double &error )  const 
//{
//  //* Calculate particle pseudorapidity
//
//  double px = fP[3];
//  double py = fP[4];
//  double pz = fP[5];
//  double pt2 = px*px + py*py;
//  double p2 = pt2 + pz*pz;
//  double p = sqrt(p2);
//  double a = p + pz;
//  double b = p - pz;
//  eta = 1.e10;
//  if( b > 1.e-8 ){
//    double c = a/b;
//    if( c>1.e-8 ) eta = 0.5*log(c);
//  }
//  double h3 = -px*pz;
//  double h4 = -py*pz;  
//  double pt4 = pt2*pt2;
//  double p2pt4 = p2*pt4;
//  error = (h3*h3*fC[9] + h4*h4*fC[14] + pt4*fC[20] + 2*( h3*(h4*fC[13] + fC[18]*pt2) + pt2*h4*fC[19] ) );
//
//  if( error>0 && p2pt4>1.e-10 ){
//    error = sqrt(error/p2pt4);
//    return 0;
//  }
//
//  error = 1.e10;
//  return 1;
//}
//
//int GetPhi( double &phi, double &error )  const 
//{
//  //* Calculate particle polar angle
//
//  double px = fP[3];
//  double py = fP[4];
//  double px2 = px*px;
//  double py2 = py*py;
//  double pt2 = px2 + py2;
//  phi = atan2(py,px);
//  error = (py2*fC[9] + px2*fC[14] - 2*px*py*fC[13] );
//  if( error>0 && pt2>1.e-4 ){
//    error = sqrt(error)/pt2;
//    return 0;
//  }
//  error = 1.e10;
//  return 1;
//}
//
//int GetR( double &r, double &error )  const 
//{
//  //* Calculate distance to the origin
//
//  double x = fP[0];
//  double y = fP[1];
//  double x2 = x*x;
//  double y2 = y*y;
//  r = sqrt(x2 + y2);
//  error = (x2*fC[0] + y2*fC[2] - 2*x*y*fC[1] );
//  if( error>0 && r>1.e-4 ){
//    error = sqrt(error)/r;
//    return 0;
//  }
//  error = 1.e10;
//  return 1;
//}
//
//int GetMass( double &m, double &error ) const 
//{
//  //* Calculate particle mass
//  
//  // s = sigma^2 of m2/2
//
//  double s = (  fP[3]*fP[3]*fC[9] + fP[4]*fP[4]*fC[14] + fP[5]*fP[5]*fC[20] 
//                 + fP[6]*fP[6]*fC[27] 
//+2*( + fP[3]*fP[4]*fC[13] + fP[5]*(fP[3]*fC[18] + fP[4]*fC[19]) 
// - fP[6]*( fP[3]*fC[24] + fP[4]*fC[25] + fP[5]*fC[26] )   )
// ); 
////   double m2 = abs(fP[6]*fP[6] - fP[3]*fP[3] - fP[4]*fP[4] - fP[5]*fP[5]);
////   m  = sqrt(m2);
////   if( m>1.e-10 ){
////     if( s>=0 ){
////       error = sqrt(s)/m;
////       return 0;
////     }
////   }
////   error = 1.e20;
//  double m2 = (fP[6]*fP[6] - fP[3]*fP[3] - fP[4]*fP[4] - fP[5]*fP[5]);
//
//  if(m2<0.)
//  {
//    error = 1.e20;
//    m = -sqrt(-m2);
//    return 1;
//  }
//
//  m  = sqrt(m2);
//  if( m>1.e-6 ){
//    if( s >= 0 ) {
//      error = sqrt(s)/m;
//      return 0;
//    }
//  }
//  else {
//    error = 1.e20;
//    return 0;
//  }
//  error = 1.e20;
//
//  return 1;
//}
//
//
//int GetDecayLength( double &l, double &error ) const 
//{
//  //* Calculate particle decay length [cm]
//
//  double x = fP[3];
//  double y = fP[4];
//  double z = fP[5];
//  double t = fP[7];
//  double x2 = x*x;
//  double y2 = y*y;
//  double z2 = z*z;
//  double p2 = x2+y2+z2;
//  l = t*sqrt(p2);
//  if( p2>1.e-4){
//    error = p2*fC[35] + t*t/p2*(x2*fC[9]+y2*fC[14]+z2*fC[20]
//+ 2*(x*y*fC[13]+x*z*fC[18]+y*z*fC[19]) )
//  + 2*t*(x*fC[31]+y*fC[32]+z*fC[33]);
//    error = sqrt(abs(error));
//    return 0;
//  }
//  error = 1.e20;
//  return 1;
//}
//
//int GetDecayLengthXY( double &l, double &error ) const 
//{
//  //* Calculate particle decay length in XY projection [cm]
//
//  double x = fP[3];
//  double y = fP[4];
//  double t = fP[7];
//  double x2 = x*x;
//  double y2 = y*y;
//  double pt2 = x2+y2;
//  l = t*sqrt(pt2);
//  if( pt2>1.e-4){
//    error = pt2*fC[35] + t*t/pt2*(x2*fC[9]+y2*fC[14] + 2*x*y*fC[13] )
//      + 2*t*(x*fC[31]+y*fC[32]);
//    error = sqrt(abs(error));
//    return 0;
//  }
//  error = 1.e20;
//  return 1;
//}
//
//
//int GetLifeTime( double &tauC, double &error ) const 
//{
//  //* Calculate particle decay time [s]
//
//  double m, dm;
//  GetMass( m, dm );
//  double cTM = (-fP[3]*fC[31] - fP[4]*fC[32] - fP[5]*fC[33] + fP[6]*fC[34]);
//  tauC = fP[7]*m;
//  error = m*m*fC[35] + 2*fP[7]*cTM + fP[7]*fP[7]*dm*dm;
//  if( error > 0 ){
//    error = sqrt( error );
//    return 0;
//  }
//  error = 1.e20;
//  return 1;
//}
//
//
//void operator +=( const KFParticleBase &Daughter )
//{
//  //* Add daughter via operator+=
//
//  AddDaughter( Daughter );
//}
//  
//double GetSCorrection( const double Part[], const double XYZ[] ) 
//{
//  //* Get big enough correction for S error to let the particle Part be fitted to XYZ point
//  
//  double d[3] = { XYZ[0]-Part[0], XYZ[1]-Part[1], XYZ[2]-Part[2] };
//  double p2 = Part[3]*Part[3]+Part[4]*Part[4]+Part[5]*Part[5];
////  double sigmaS = (p2>1.e-4) ? ( 10.1+3.*sqrt( d[0]*d[0]+d[1]*d[1]+d[2]*d[2]) )/sqrt(p2) : 1.;
//  double sigmaS = (p2>1.e-4) ? ( 0.1+10.*sqrt( d[0]*d[0]+d[1]*d[1]+d[2]*d[2]) )/sqrt(p2) : 1.;
//  return sigmaS;
//}
//
//void GetMeasurement( const double XYZ[], double m[], double V[] ) const
//{
//  //* Get additional covariances V used during measurement
//
//  double b[3];
//  GetFieldValue( XYZ, b );
//  const double kCLight =  0.000299792458;
//  b[0]*=kCLight; b[1]*=kCLight; b[2]*=kCLight;
//// std::cout << "   DStoPoint "<< (GetDStoPoint(XYZ)) << std::endl;
//  Transport( GetDStoPoint(XYZ), m, V );
////std::cout << "x " << XYZ[0] << " " << m[0] <<"   y " << XYZ[1] << " " << m[1] <<"   z " << XYZ[2] << " " << m[2] <<std::endl;
//  double sigmaS = GetSCorrection( m, XYZ );
//
//  double h[6];
//
//  h[0] = m[3]*sigmaS;
//  h[1] = m[4]*sigmaS;
//  h[2] = m[5]*sigmaS;
//  h[3] = ( h[1]*b[2]-h[2]*b[1] )*GetQ();
//  h[4] = ( h[2]*b[0]-h[0]*b[2] )*GetQ();
//  h[5] = ( h[0]*b[1]-h[1]*b[0] )*GetQ();
//    
//  V[ 0]+= h[0]*h[0];
//  V[ 1]+= h[1]*h[0];
//  V[ 2]+= h[1]*h[1];
//  V[ 3]+= h[2]*h[0];
//  V[ 4]+= h[2]*h[1];
//  V[ 5]+= h[2]*h[2];
//
//  V[ 6]+= h[3]*h[0];
//  V[ 7]+= h[3]*h[1];
//  V[ 8]+= h[3]*h[2];
//  V[ 9]+= h[3]*h[3];
//
//  V[10]+= h[4]*h[0];
//  V[11]+= h[4]*h[1];
//  V[12]+= h[4]*h[2];
//  V[13]+= h[4]*h[3];
//  V[14]+= h[4]*h[4];
//
//  V[15]+= h[5]*h[0];
//  V[16]+= h[5]*h[1];
//  V[17]+= h[5]*h[2];
//  V[18]+= h[5]*h[3];
//  V[19]+= h[5]*h[4];
//  V[20]+= h[5]*h[5];
//}
//
//void AddDaughter( const KFParticleBase &Daughter )
//{
//  if( fNDF<-1 ){ // first daughter . just copy
//    fNDF   = -1;
//    fQ     =  Daughter.GetQ();
//    for( int i=0; i<7; i++) fP[i] = Daughter.fP[i];
//    for( int i=0; i<28; i++) fC[i] = Daughter.fC[i];
//    fSFromDecay = 0;
//    fMassHypo = Daughter.fMassHypo;
//    SumDaughterMass = Daughter.SumDaughterMass;
//    return;
//  }
//
//  if(fConstructMethod == 0)
//    AddDaughterWithEnergyFit(Daughter);
//  else if(fConstructMethod == 1)
//    AddDaughterWithEnergyCalc(Daughter);
//  else if(fConstructMethod == 2)
//    AddDaughterWithEnergyFitMC(Daughter);
//
//  SumDaughterMass += Daughter.SumDaughterMass;
//  fMassHypo = -1;
//}
//
//void AddDaughterWithEnergyFit( const KFParticleBase &Daughter )
//{
//  //* Energy considered as an independent veriable, fitted independently from momentum, without any constraints on mass
//
//  //* Add daughter 
//
//  TransportToDecayVertex();
//
//  double b[3]; 
//  int maxIter = 1;
//
//  if( !fIsLinearized ){
//    if( fNDF==-1 ){
//      double ds, ds1;
//      GetDStoParticle(Daughter, ds, ds1);      
//      TransportToDS( ds );
//      double m[8];
//      double mCd[36];       
//      Daughter.Transport( ds1, m, mCd );    
//      fVtxGuess[0] = .5*( fP[0] + m[0] );
//      fVtxGuess[1] = .5*( fP[1] + m[1] );
//      fVtxGuess[2] = .5*( fP[2] + m[2] );
//    } else {
//      fVtxGuess[0] = fP[0];
//      fVtxGuess[1] = fP[1];
//      fVtxGuess[2] = fP[2]; 
//    }
//    maxIter = 3;
//  }
//
//  for( int iter=0; iter<maxIter; iter++ ){
//
//    {
//      GetFieldValue( fVtxGuess, b );
//      const double kCLight =  0.000299792458;
//      b[0]*=kCLight; b[1]*=kCLight; b[2]*=kCLight;
//    }
//
//    double[] ffP = fP, *ffC = fC, tmpP[8], tmpC[36];
//    if( fNDF==-1 ){            
//      GetMeasurement( fVtxGuess, tmpP, tmpC );
//      ffP = tmpP;
//      ffC = tmpC;
//    }
//
//    double m[8], mV[36];
//
//    if( Daughter.fC[35]>0 ){
//      Daughter.GetMeasurement( fVtxGuess, m, mV );
//    } else {
//      for( int i=0; i<8; i++ ) m[i] = Daughter.fP[i];
//      for( int i=0; i<36; i++ ) mV[i] = Daughter.fC[i];
//    }
//    //*
//
//    double mS[6];
//    {
//      double mSi[6] = { ffC[0]+mV[0], 
//ffC[1]+mV[1], ffC[2]+mV[2], 
//ffC[3]+mV[3], ffC[4]+mV[4], ffC[5]+mV[5] };
//
//      mS[0] = mSi[2]*mSi[5] - mSi[4]*mSi[4];
//      mS[1] = mSi[3]*mSi[4] - mSi[1]*mSi[5];
//      mS[2] = mSi[0]*mSi[5] - mSi[3]*mSi[3];
//      mS[3] = mSi[1]*mSi[4] - mSi[2]*mSi[3];
//      mS[4] = mSi[1]*mSi[3] - mSi[0]*mSi[4];
//      mS[5] = mSi[0]*mSi[2] - mSi[1]*mSi[1];
//
//      double s = ( mSi[0]*mS[0] + mSi[1]*mS[1] + mSi[3]*mS[3] );      
//      s = ( abs(s) > 1.E-20 )  ?1./s :0;  
//      mS[0]*=s;
//      mS[1]*=s;
//      mS[2]*=s;
//      mS[3]*=s;
//      mS[4]*=s;
//      mS[5]*=s;
//    }
//    //* Residual (measured - estimated)
//
//    double zeta[3] = { m[0]-ffP[0], m[1]-ffP[1], m[2]-ffP[2] };    
//
//    //* CHt = CH' - D'
//
//    double mCHt0[7], mCHt1[7], mCHt2[7];
//
//    mCHt0[0]=ffC[ 0] ;       mCHt1[0]=ffC[ 1] ;       mCHt2[0]=ffC[ 3] ;
//    mCHt0[1]=ffC[ 1] ;       mCHt1[1]=ffC[ 2] ;       mCHt2[1]=ffC[ 4] ;
//    mCHt0[2]=ffC[ 3] ;       mCHt1[2]=ffC[ 4] ;       mCHt2[2]=ffC[ 5] ;
//    mCHt0[3]=ffC[ 6]-mV[ 6]; mCHt1[3]=ffC[ 7]-mV[ 7]; mCHt2[3]=ffC[ 8]-mV[ 8];
//    mCHt0[4]=ffC[10]-mV[10]; mCHt1[4]=ffC[11]-mV[11]; mCHt2[4]=ffC[12]-mV[12];
//    mCHt0[5]=ffC[15]-mV[15]; mCHt1[5]=ffC[16]-mV[16]; mCHt2[5]=ffC[17]-mV[17];
//    mCHt0[6]=ffC[21]-mV[21]; mCHt1[6]=ffC[22]-mV[22]; mCHt2[6]=ffC[23]-mV[23];
//  
//    //* Kalman gain K = mCH'*S
//    
//    double k0[7], k1[7], k2[7];
//    
//    for(int i=0;i<7;++i){
//      k0[i] = mCHt0[i]*mS[0] + mCHt1[i]*mS[1] + mCHt2[i]*mS[3];
//      k1[i] = mCHt0[i]*mS[1] + mCHt1[i]*mS[2] + mCHt2[i]*mS[4];
//      k2[i] = mCHt0[i]*mS[3] + mCHt1[i]*mS[4] + mCHt2[i]*mS[5];
//    }
//
//   //* New estimation of the vertex position 
//
//    if( iter<maxIter-1 ){
//      for(int i=0; i<3; ++i) 
//fVtxGuess[i]= ffP[i] + k0[i]*zeta[0]+k1[i]*zeta[1]+k2[i]*zeta[2];
//      continue;
//    }
//
//    // last itearation . update the particle
//
//    //* Add the daughter momentum to the particle momentum
//    
//    ffP[ 3] += m[ 3];
//    ffP[ 4] += m[ 4];
//    ffP[ 5] += m[ 5];
//    ffP[ 6] += m[ 6];
//  
//    ffC[ 9] += mV[ 9];
//    ffC[13] += mV[13];
//    ffC[14] += mV[14];
//    ffC[18] += mV[18];
//    ffC[19] += mV[19];
//    ffC[20] += mV[20];
//    ffC[24] += mV[24];
//    ffC[25] += mV[25];
//    ffC[26] += mV[26];
//    ffC[27] += mV[27];
//    
// 
//   //* New estimation of the vertex position r += K*zeta
//    
//    for(int i=0;i<7;++i) 
//      fP[i] = ffP[i] + k0[i]*zeta[0] + k1[i]*zeta[1] + k2[i]*zeta[2];
//    
//    //* New covariance matrix C -= K*(mCH')'
//
//    for(int i=0, k=0;i<7;++i){
//      for(int j=0;j<=i;++j,++k){
//fC[k] = ffC[k] - (k0[i]*mCHt0[j] + k1[i]*mCHt1[j] + k2[i]*mCHt2[j] );
//      }
//    }
//  
//    //* Calculate Chi^2 
//
//    fNDF  += 2;
//    fQ    +=  Daughter.GetQ();
//    fSFromDecay = 0;    
//    fChi2 += (mS[0]*zeta[0] + mS[1]*zeta[1] + mS[3]*zeta[2])*zeta[0]
//      +      (mS[1]*zeta[0] + mS[2]*zeta[1] + mS[4]*zeta[2])*zeta[1]
//      +      (mS[3]*zeta[0] + mS[4]*zeta[1] + mS[5]*zeta[2])*zeta[2];     
//
//  }
//}
//
//void AddDaughterWithEnergyCalc( const KFParticleBase &Daughter )
//{
//  //* Energy considered as a dependent variable, calculated from the momentum and mass hypothesis
//
//  //* Add daughter 
//
//  TransportToDecayVertex();
//
//  double b[3]; 
//  int maxIter = 1;
//
//  if( !fIsLinearized ){
//    if( fNDF==-1 ){
//      double ds, ds1;
//      GetDStoParticle(Daughter, ds, ds1);      
//      TransportToDS( ds );
//      double m[8];
//      double mCd[36];       
//      Daughter.Transport( ds1, m, mCd );    
//      fVtxGuess[0] = .5*( fP[0] + m[0] );
//      fVtxGuess[1] = .5*( fP[1] + m[1] );
//      fVtxGuess[2] = .5*( fP[2] + m[2] );
//    } else {
//      fVtxGuess[0] = fP[0];
//      fVtxGuess[1] = fP[1];
//      fVtxGuess[2] = fP[2]; 
//    }
//    maxIter = 3;
//  }
//
//  for( int iter=0; iter<maxIter; iter++ ){
//
//    {
//      GetFieldValue( fVtxGuess, b );
//      const double kCLight =  0.000299792458;
//      b[0]*=kCLight; b[1]*=kCLight; b[2]*=kCLight;
//    }
//
//    double[] ffP = fP, *ffC = fC, tmpP[8], tmpC[36];
//    if( fNDF==-1 ){            
//      GetMeasurement( fVtxGuess, tmpP, tmpC );
//      ffP = tmpP;
//      ffC = tmpC;
//    }
//
//    double m[8], mV[36];
//
//    if( Daughter.fC[35]>0 ){
//      Daughter.GetMeasurement( fVtxGuess, m, mV );
//    } else {
//      for( int i=0; i<8; i++ ) m[i] = Daughter.fP[i];
//      for( int i=0; i<36; i++ ) mV[i] = Daughter.fC[i];
//    }
//
//    double massMf2 = m[6]*m[6] - (m[3]*m[3] + m[4]*m[4] + m[5]*m[5]);
//    double massRf2 = fP[6]*fP[6] - (fP[3]*fP[3] + fP[4]*fP[4] + fP[5]*fP[5]);
//
//    //*
//
//    double mS[6];
//    {
//      double mSi[6] = { ffC[0]+mV[0], 
//ffC[1]+mV[1], ffC[2]+mV[2], 
//ffC[3]+mV[3], ffC[4]+mV[4], ffC[5]+mV[5] };
//
//      mS[0] = mSi[2]*mSi[5] - mSi[4]*mSi[4];
//      mS[1] = mSi[3]*mSi[4] - mSi[1]*mSi[5];
//      mS[2] = mSi[0]*mSi[5] - mSi[3]*mSi[3];
//      mS[3] = mSi[1]*mSi[4] - mSi[2]*mSi[3];
//      mS[4] = mSi[1]*mSi[3] - mSi[0]*mSi[4];
//mS[5] = mSi[0]*mSi[2] - mSi[1]*mSi[1]; 
//
//      double s = ( mSi[0]*mS[0] + mSi[1]*mS[1] + mSi[3]*mS[3] );      
//
//s = ( s > 1.E-20 )  ?1./s :0;
//      mS[0]*=s;
//      mS[1]*=s;
//      mS[2]*=s;
//      mS[3]*=s;
//      mS[4]*=s;
//      mS[5]*=s;
//    }
//
//    //* Residual (measured - estimated)
//
//    double zeta[3] = { m[0]-ffP[0], m[1]-ffP[1], m[2]-ffP[2] };    
//
//    //* CHt = CH' - D'
//
//    double mCHt0[6], mCHt1[6], mCHt2[6];
//
//    mCHt0[0]=ffC[ 0] ;       mCHt1[0]=ffC[ 1] ;       mCHt2[0]=ffC[ 3] ;
//    mCHt0[1]=ffC[ 1] ;       mCHt1[1]=ffC[ 2] ;       mCHt2[1]=ffC[ 4] ;
//    mCHt0[2]=ffC[ 3] ;       mCHt1[2]=ffC[ 4] ;       mCHt2[2]=ffC[ 5] ;
//    mCHt0[3]=ffC[ 6]-mV[ 6]; mCHt1[3]=ffC[ 7]-mV[ 7]; mCHt2[3]=ffC[ 8]-mV[ 8];
//    mCHt0[4]=ffC[10]-mV[10]; mCHt1[4]=ffC[11]-mV[11]; mCHt2[4]=ffC[12]-mV[12];
//    mCHt0[5]=ffC[15]-mV[15]; mCHt1[5]=ffC[16]-mV[16]; mCHt2[5]=ffC[17]-mV[17];
//
//    //* Kalman gain K = mCH'*S
//
//    double k0[6], k1[6], k2[6];
//
//    for(int i=0;i<6;++i){
//      k0[i] = mCHt0[i]*mS[0] + mCHt1[i]*mS[1] + mCHt2[i]*mS[3];
//      k1[i] = mCHt0[i]*mS[1] + mCHt1[i]*mS[2] + mCHt2[i]*mS[4];
//      k2[i] = mCHt0[i]*mS[3] + mCHt1[i]*mS[4] + mCHt2[i]*mS[5];
//    }
//
//   //* New estimation of the vertex position 
//
//    if( iter<maxIter-1 ){
//      for(int i=0; i<3; ++i) 
//fVtxGuess[i]= ffP[i] + k0[i]*zeta[0]+k1[i]*zeta[1]+k2[i]*zeta[2];
//      continue;
//    }
//
//   //* find mf and mVf - optimum value of the measurement and its covariance matrix
//    //* mVHt = V*H'
//    double mVHt0[6], mVHt1[6], mVHt2[6];
//
//    mVHt0[0]= mV[ 0] ; mVHt1[0]= mV[ 1] ; mVHt2[0]= mV[ 3] ;
//    mVHt0[1]= mV[ 1] ; mVHt1[1]= mV[ 2] ; mVHt2[1]= mV[ 4] ;
//    mVHt0[2]= mV[ 3] ; mVHt1[2]= mV[ 4] ; mVHt2[2]= mV[ 5] ;
//    mVHt0[3]= mV[ 6] ; mVHt1[3]= mV[ 7] ; mVHt2[3]= mV[ 8] ;
//    mVHt0[4]= mV[10] ; mVHt1[4]= mV[11] ; mVHt2[4]= mV[12] ;
//    mVHt0[5]= mV[15] ; mVHt1[5]= mV[16] ; mVHt2[5]= mV[17] ;
//
//    //* Kalman gain Km = mCH'*S
//
//    double km0[6], km1[6], km2[6];
//
//    for(int i=0;i<6;++i){
//      km0[i] = mVHt0[i]*mS[0] + mVHt1[i]*mS[1] + mVHt2[i]*mS[3];
//      km1[i] = mVHt0[i]*mS[1] + mVHt1[i]*mS[2] + mVHt2[i]*mS[4];
//      km2[i] = mVHt0[i]*mS[3] + mVHt1[i]*mS[4] + mVHt2[i]*mS[5];
//    }
//
//    double mf[7] = { m[0], m[1], m[2], m[3], m[4], m[5], m[6] };
//
//    for(int i=0;i<6;++i) 
//      mf[i] = mf[i] - km0[i]*zeta[0] - km1[i]*zeta[1] - km2[i]*zeta[2];
//
//    double energyMf = sqrt( massMf2 + (mf[3]*mf[3] + mf[4]*mf[4] + mf[5]*mf[5]) );
//
//    double mVf[28];
//    for(int iC=0; iC<28; iC++)
//      mVf[iC] = mV[iC];
//
//    //* hmf = d(energyMf)/d(mf)
//    double hmf[7];
//    if( abs(energyMf) < 1.e-10) hmf[3] = 0; else hmf[3] = mf[3]/energyMf;
//    if( abs(energyMf) < 1.e-10) hmf[4] = 0; else hmf[4] = mf[4]/energyMf;
//    if( abs(energyMf) < 1.e-10) hmf[5] = 0; else hmf[5] = mf[5]/energyMf;
////    if( abs(energyMf) < 1.e-10) hmf[6] = 0; else hmf[6] = mf[6]/energyMf;
//    hmf[6] = 0;
//
//    for(int i=0, k=0;i<6;++i){
//      for(int j=0;j<=i;++j,++k){
//        mVf[k] = mVf[k] - (km0[i]*mVHt0[j] + km1[i]*mVHt1[j] + km2[i]*mVHt2[j] );
//      }
//    }
//    double mVf24 = mVf[24], mVf25 = mVf[25], mVf26 = mVf[26];
//    mVf[21] = mVf[6 ]*hmf[3] + mVf[10]*hmf[4] + mVf[15]*hmf[5] + mVf[21]*hmf[6];
//    mVf[22] = mVf[7 ]*hmf[3] + mVf[11]*hmf[4] + mVf[16]*hmf[5] + mVf[22]*hmf[6];
//    mVf[23] = mVf[8 ]*hmf[3] + mVf[12]*hmf[4] + mVf[17]*hmf[5] + mVf[23]*hmf[6];
//    mVf[24] = mVf[9 ]*hmf[3] + mVf[13]*hmf[4] + mVf[18]*hmf[5] + mVf[24]*hmf[6];
//    mVf[25] = mVf[13]*hmf[3] + mVf[14]*hmf[4] + mVf[19]*hmf[5] + mVf[25]*hmf[6];
//    mVf[26] = mVf[18]*hmf[3] + mVf[19]*hmf[4] + mVf[20]*hmf[5] + mVf[26]*hmf[6];
//    mVf[27] = mVf[24]*hmf[3] + mVf[25]*hmf[4] + mVf[26]*hmf[5] + (mVf24*hmf[3] + mVf25*hmf[4] + mVf26*hmf[5] + mVf[27]*hmf[6])*hmf[6]; //here mVf[] are already modified
//
//    mf[6] = energyMf;
//
//    //* find rf and mCf - optimum value of the measurement and its covariance matrix
//
//    //* mCCHt = C*H'
//    double mCCHt0[6], mCCHt1[6], mCCHt2[6];
//
//    mCCHt0[0]=ffC[ 0]; mCCHt1[0]=ffC[ 1]; mCCHt2[0]=ffC[ 3];
//    mCCHt0[1]=ffC[ 1]; mCCHt1[1]=ffC[ 2]; mCCHt2[1]=ffC[ 4];
//    mCCHt0[2]=ffC[ 3]; mCCHt1[2]=ffC[ 4]; mCCHt2[2]=ffC[ 5];
//    mCCHt0[3]=ffC[ 6]; mCCHt1[3]=ffC[ 7]; mCCHt2[3]=ffC[ 8];
//    mCCHt0[4]=ffC[10]; mCCHt1[4]=ffC[11]; mCCHt2[4]=ffC[12];
//    mCCHt0[5]=ffC[15]; mCCHt1[5]=ffC[16]; mCCHt2[5]=ffC[17];
//
//    //* Kalman gain Krf = mCH'*S
//
//    double krf0[6], krf1[6], krf2[6];
//
//    for(int i=0;i<6;++i){
//      krf0[i] = mCCHt0[i]*mS[0] + mCCHt1[i]*mS[1] + mCCHt2[i]*mS[3];
//      krf1[i] = mCCHt0[i]*mS[1] + mCCHt1[i]*mS[2] + mCCHt2[i]*mS[4];
//      krf2[i] = mCCHt0[i]*mS[3] + mCCHt1[i]*mS[4] + mCCHt2[i]*mS[5];
//    }
//    double rf[7] = { ffP[0], ffP[1], ffP[2], ffP[3], ffP[4], ffP[5], ffP[6] };
//
//    for(int i=0;i<6;++i) 
//      rf[i] = rf[i] + krf0[i]*zeta[0] + krf1[i]*zeta[1] + krf2[i]*zeta[2];
//
//    double energyRf = sqrt( massRf2 + (rf[3]*rf[3] + rf[4]*rf[4] + rf[5]*rf[5]) );
//
//    double mCf[28];
//    for(int iC=0; iC<28; iC++)
//      mCf[iC] = ffC[iC];
//    //* hrf = d(Erf)/d(rf)
//    double hrf[7];
//    if( abs(energyRf) < 1.e-10) hrf[3] = 0; else hrf[3] = rf[3]/energyRf;
//    if( abs(energyRf) < 1.e-10) hrf[4] = 0; else hrf[4] = rf[4]/energyRf;
//    if( abs(energyRf) < 1.e-10) hrf[5] = 0; else hrf[5] = rf[5]/energyRf;
////    if( abs(energyRf) < 1.e-10) hrf[6] = 0; else hrf[6] = rf[6]/energyRf;
//    hrf[6] = 0;
//
//    for(int i=0, k=0;i<6;++i){
//      for(int j=0;j<=i;++j,++k){
//        mCf[k] = mCf[k] - (krf0[i]*mCCHt0[j] + krf1[i]*mCCHt1[j] + krf2[i]*mCCHt2[j] );
//      }
//    }
//    double mCf24 = mCf[24], mCf25 = mCf[25], mCf26 = mCf[26];
//    mCf[21] = mCf[6 ]*hrf[3] + mCf[10]*hrf[4] + mCf[15]*hrf[5] + mCf[21]*hrf[6];
//    mCf[22] = mCf[7 ]*hrf[3] + mCf[11]*hrf[4] + mCf[16]*hrf[5] + mCf[22]*hrf[6];
//    mCf[23] = mCf[8 ]*hrf[3] + mCf[12]*hrf[4] + mCf[17]*hrf[5] + mCf[23]*hrf[6];
//    mCf[24] = mCf[9 ]*hrf[3] + mCf[13]*hrf[4] + mCf[18]*hrf[5] + mCf[24]*hrf[6];
//    mCf[25] = mCf[13]*hrf[3] + mCf[14]*hrf[4] + mCf[19]*hrf[5] + mCf[25]*hrf[6];
//    mCf[26] = mCf[18]*hrf[3] + mCf[19]*hrf[4] + mCf[20]*hrf[5] + mCf[26]*hrf[6];
//    mCf[27] = mCf[24]*hrf[3] + mCf[25]*hrf[4] + mCf[26]*hrf[5] + (mCf24*hrf[3] + mCf25*hrf[4] + mCf26*hrf[5] + mCf[27]*hrf[6])*hrf[6]; //here mCf[] are already modified
//
//    for(int iC=21; iC<28; iC++)
//    {
//      ffC[iC] = mCf[iC];
//      mV[iC]  = mVf[iC];
//    }
//
//    fP[6] = energyRf + energyMf;
//    rf[6] = energyRf;
//
//    //double Dvv[3][3]; do not need this
//    double mDvp[3][3];
//    double mDpv[3][3];
//    double mDpp[3][3];
//    double mDe[7];
//
//    for(int i=0; i<3; i++)
//    {
//      for(int j=0; j<3; j++)
//      {
//        mDvp[i][j] = km0[i+3]*mCCHt0[j] + km1[i+3]*mCCHt1[j] + km2[i+3]*mCCHt2[j];
//        mDpv[i][j] = km0[i]*mCCHt0[j+3] + km1[i]*mCCHt1[j+3] + km2[i]*mCCHt2[j+3];
//        mDpp[i][j] = km0[i+3]*mCCHt0[j+3] + km1[i+3]*mCCHt1[j+3] + km2[i+3]*mCCHt2[j+3];
//      }
//    }
//
//    mDe[0] = hmf[3]*mDvp[0][0] + hmf[4]*mDvp[1][0] + hmf[5]*mDvp[2][0];
//    mDe[1] = hmf[3]*mDvp[0][1] + hmf[4]*mDvp[1][1] + hmf[5]*mDvp[2][1];
//    mDe[2] = hmf[3]*mDvp[0][2] + hmf[4]*mDvp[1][2] + hmf[5]*mDvp[2][2];
//    mDe[3] = hmf[3]*mDpp[0][0] + hmf[4]*mDpp[1][0] + hmf[5]*mDpp[2][0];
//    mDe[4] = hmf[3]*mDpp[0][1] + hmf[4]*mDpp[1][1] + hmf[5]*mDpp[2][1];
//    mDe[5] = hmf[3]*mDpp[0][2] + hmf[4]*mDpp[1][2] + hmf[5]*mDpp[2][2];
//    mDe[6] = 2*(mDe[3]*hrf[3] + mDe[4]*hrf[4] + mDe[5]*hrf[5]);
//
//    // last itearation . update the particle
//
//    //* Add the daughter momentum to the particle momentum
//
//    ffP[ 3] += m[ 3];
//    ffP[ 4] += m[ 4];
//    ffP[ 5] += m[ 5];
//
//    ffC[ 9] += mV[ 9];
//    ffC[13] += mV[13];
//    ffC[14] += mV[14];
//    ffC[18] += mV[18];
//    ffC[19] += mV[19];
//    ffC[20] += mV[20];
//    ffC[24] += mV[24];
//    ffC[25] += mV[25];
//    ffC[26] += mV[26];
//    ffC[27] += mV[27];
//
//    ffC[21] += mDe[0];
//    ffC[22] += mDe[1];
//    ffC[23] += mDe[2];
//    ffC[24] += mDe[3];
//    ffC[25] += mDe[4];
//    ffC[26] += mDe[5];
//    ffC[27] += mDe[6];
//
//   //* New estimation of the vertex position r += K*zeta
//
//    for(int i=0;i<6;++i) 
//      fP[i] = ffP[i] + k0[i]*zeta[0] + k1[i]*zeta[1] + k2[i]*zeta[2];
//
//    //* New covariance matrix C -= K*(mCH')'
//
//    for(int i=0, k=0;i<6;++i){
//      for(int j=0;j<=i;++j,++k){
//fC[k] = ffC[k] - (k0[i]*mCHt0[j] + k1[i]*mCHt1[j] + k2[i]*mCHt2[j] );
//      }
//    }
//
//    for(int i=21; i<28; i++) fC[i] = ffC[i];
//
//    //* Calculate Chi^2 
//
//    fNDF  += 2;
//    fQ    +=  Daughter.GetQ();
//    fSFromDecay = 0;    
//    fChi2 += (mS[0]*zeta[0] + mS[1]*zeta[1] + mS[3]*zeta[2])*zeta[0]
//      +      (mS[1]*zeta[0] + mS[2]*zeta[1] + mS[4]*zeta[2])*zeta[1]
//      +      (mS[3]*zeta[0] + mS[4]*zeta[1] + mS[5]*zeta[2])*zeta[2];     
//  }
//}
//
//void AddDaughterWithEnergyFitMC( const KFParticleBase &Daughter )
//{
//  //* Energy considered as an independent variable, fitted independently from momentum, without any constraints on mass
//
//  //* Add daughter 
//
//  TransportToDecayVertex();
//
//  double b[3]; 
//  int maxIter = 1;
//
//  if( !fIsLinearized ){
//    if( fNDF==-1 ){
//      double ds, ds1;
//      GetDStoParticle(Daughter, ds, ds1);      
//      TransportToDS( ds );
//      double m[8];
//      double mCd[36];       
//      Daughter.Transport( ds1, m, mCd );    
//      fVtxGuess[0] = .5*( fP[0] + m[0] );
//      fVtxGuess[1] = .5*( fP[1] + m[1] );
//      fVtxGuess[2] = .5*( fP[2] + m[2] );
//    } else {
//      fVtxGuess[0] = fP[0];
//      fVtxGuess[1] = fP[1];
//      fVtxGuess[2] = fP[2]; 
//    }
//    maxIter = 3;
//  }
//
//  for( int iter=0; iter<maxIter; iter++ ){
//
//    {
//      GetFieldValue( fVtxGuess, b );
//      const double kCLight =  0.000299792458;
//      b[0]*=kCLight; b[1]*=kCLight; b[2]*=kCLight;
//    }
//
//    double[] ffP = fP, *ffC = fC, tmpP[8], tmpC[36];
//    if( fNDF==-1 ){            
//      GetMeasurement( fVtxGuess, tmpP, tmpC );
//      ffP = tmpP;
//      ffC = tmpC;
//    }
//    double m[8], mV[36];
//
//    if( Daughter.fC[35]>0 ){
//      Daughter.GetMeasurement( fVtxGuess, m, mV );
//    } else {
//      for( int i=0; i<8; i++ ) m[i] = Daughter.fP[i];
//      for( int i=0; i<36; i++ ) mV[i] = Daughter.fC[i];
//    }
//    //*
//
//    double mS[6];
//    {
//      double mSi[6] = { ffC[0]+mV[0], 
//  ffC[1]+mV[1], ffC[2]+mV[2], 
//  ffC[3]+mV[3], ffC[4]+mV[4], ffC[5]+mV[5] };
//     
//      mS[0] = mSi[2]*mSi[5] - mSi[4]*mSi[4];
//      mS[1] = mSi[3]*mSi[4] - mSi[1]*mSi[5];
//      mS[2] = mSi[0]*mSi[5] - mSi[3]*mSi[3];
//      mS[3] = mSi[1]*mSi[4] - mSi[2]*mSi[3];
//      mS[4] = mSi[1]*mSi[3] - mSi[0]*mSi[4];
//      mS[5] = mSi[0]*mSi[2] - mSi[1]*mSi[1]; 
//      
//      double s = ( mSi[0]*mS[0] + mSi[1]*mS[1] + mSi[3]*mS[3] );      
//
//      s = ( s > 1.E-20 )  ?1./s :0;  
//      mS[0]*=s;
//      mS[1]*=s;
//      mS[2]*=s;
//      mS[3]*=s;
//      mS[4]*=s;
//      mS[5]*=s;
//    }
//    //* Residual (measured - estimated)
//    
//    double zeta[3] = { m[0]-ffP[0], m[1]-ffP[1], m[2]-ffP[2] };    
//
//    
//    //* CHt = CH'
//    
//    double mCHt0[7], mCHt1[7], mCHt2[7];
//    
//    mCHt0[0]=ffC[ 0] ; mCHt1[0]=ffC[ 1] ; mCHt2[0]=ffC[ 3] ;
//    mCHt0[1]=ffC[ 1] ; mCHt1[1]=ffC[ 2] ; mCHt2[1]=ffC[ 4] ;
//    mCHt0[2]=ffC[ 3] ; mCHt1[2]=ffC[ 4] ; mCHt2[2]=ffC[ 5] ;
//    mCHt0[3]=ffC[ 6] ; mCHt1[3]=ffC[ 7] ; mCHt2[3]=ffC[ 8] ;
//    mCHt0[4]=ffC[10] ; mCHt1[4]=ffC[11] ; mCHt2[4]=ffC[12] ;
//    mCHt0[5]=ffC[15] ; mCHt1[5]=ffC[16] ; mCHt2[5]=ffC[17] ;
//    mCHt0[6]=ffC[21] ; mCHt1[6]=ffC[22] ; mCHt2[6]=ffC[23] ;
//  
//    //* Kalman gain K = mCH'*S
//    
//    double k0[7], k1[7], k2[7];
//    
//    for(int i=0;i<7;++i){
//      k0[i] = mCHt0[i]*mS[0] + mCHt1[i]*mS[1] + mCHt2[i]*mS[3];
//      k1[i] = mCHt0[i]*mS[1] + mCHt1[i]*mS[2] + mCHt2[i]*mS[4];
//      k2[i] = mCHt0[i]*mS[3] + mCHt1[i]*mS[4] + mCHt2[i]*mS[5];
//    }
//
//   //* New estimation of the vertex position 
//
//    if( iter<maxIter-1 ){
//      for(int i=0; i<3; ++i) 
//fVtxGuess[i]= ffP[i] + k0[i]*zeta[0]+k1[i]*zeta[1]+k2[i]*zeta[2];
//      continue;
//    }
//
//    // last itearation . update the particle
//
//    //* VHt = VH'
//    
//    double mVHt0[7], mVHt1[7], mVHt2[7];
//    
//    mVHt0[0]=mV[ 0] ; mVHt1[0]=mV[ 1] ; mVHt2[0]=mV[ 3] ;
//    mVHt0[1]=mV[ 1] ; mVHt1[1]=mV[ 2] ; mVHt2[1]=mV[ 4] ;
//    mVHt0[2]=mV[ 3] ; mVHt1[2]=mV[ 4] ; mVHt2[2]=mV[ 5] ;
//    mVHt0[3]=mV[ 6] ; mVHt1[3]=mV[ 7] ; mVHt2[3]=mV[ 8] ;
//    mVHt0[4]=mV[10] ; mVHt1[4]=mV[11] ; mVHt2[4]=mV[12] ;
//    mVHt0[5]=mV[15] ; mVHt1[5]=mV[16] ; mVHt2[5]=mV[17] ;
//    mVHt0[6]=mV[21] ; mVHt1[6]=mV[22] ; mVHt2[6]=mV[23] ;
//  
//    //* Kalman gain Km = mCH'*S
//    
//    double km0[7], km1[7], km2[7];
//    
//    for(int i=0;i<7;++i){
//      km0[i] = mVHt0[i]*mS[0] + mVHt1[i]*mS[1] + mVHt2[i]*mS[3];
//      km1[i] = mVHt0[i]*mS[1] + mVHt1[i]*mS[2] + mVHt2[i]*mS[4];
//      km2[i] = mVHt0[i]*mS[3] + mVHt1[i]*mS[4] + mVHt2[i]*mS[5];
//    }
//
//    for(int i=0;i<7;++i) 
//      ffP[i] = ffP[i] + k0[i]*zeta[0] + k1[i]*zeta[1] + k2[i]*zeta[2];
//
//    for(int i=0;i<7;++i) 
//      m[i] = m[i] - km0[i]*zeta[0] - km1[i]*zeta[1] - km2[i]*zeta[2];
//
//    for(int i=0, k=0;i<7;++i){
//      for(int j=0;j<=i;++j,++k){
//ffC[k] = ffC[k] - (k0[i]*mCHt0[j] + k1[i]*mCHt1[j] + k2[i]*mCHt2[j] );
//      }
//    }
//
//    for(int i=0, k=0;i<7;++i){
//      for(int j=0;j<=i;++j,++k){
//mV[k] = mV[k] - (km0[i]*mVHt0[j] + km1[i]*mVHt1[j] + km2[i]*mVHt2[j] );
//      }
//    }
//
//    double mDf[7][7];
//
//    for(int i=0;i<7;++i){
//      for(int j=0;j<7;++j){
//mDf[i][j] = (km0[i]*mCHt0[j] + km1[i]*mCHt1[j] + km2[i]*mCHt2[j] );
//      }
//    }
//
//    double mJ1[7][7], mJ2[7][7];
//    for(int iPar1=0; iPar1<7; iPar1++)
//    {
//      for(int iPar2=0; iPar2<7; iPar2++)
//      {
//        mJ1[iPar1][iPar2] = 0;
//        mJ2[iPar1][iPar2] = 0;
//      }
//    }
//
//    double mMassParticle  = ffP[6]*ffP[6] - (ffP[3]*ffP[3] + ffP[4]*ffP[4] + ffP[5]*ffP[5]);
//    double mMassDaughter  = m[6]*m[6] - (m[3]*m[3] + m[4]*m[4] + m[5]*m[5]);
//    if(mMassParticle > 0) mMassParticle = sqrt(mMassParticle);
//    if(mMassDaughter > 0) mMassDaughter = sqrt(mMassDaughter);
//
//    if( fMassHypo > -0.5)
//      SetMassConstraint(ffP,ffC,mJ1,fMassHypo);
//    else if((mMassParticle < SumDaughterMass) || (ffP[6]<0) )
//      SetMassConstraint(ffP,ffC,mJ1,SumDaughterMass);
//
//    if(Daughter.fMassHypo > -0.5)
//      SetMassConstraint(m,mV,mJ2,Daughter.fMassHypo);
//    else if((mMassDaughter < Daughter.SumDaughterMass) || (m[6] < 0) )
//      SetMassConstraint(m,mV,mJ2,Daughter.SumDaughterMass);
//
//    double mDJ[7][7];
//
//    for(int i=0; i<7; i++) {
//      for(int j=0; j<7; j++) {
//        mDJ[i][j] = 0;
//        for(int k=0; k<7; k++) {
//          mDJ[i][j] += mDf[i][k]*mJ1[j][k];
//        }
//      }
//    }
//
//    for(int i=0; i<7; ++i){
//      for(int j=0; j<7; ++j){
//        mDf[i][j]=0;
//        for(int l=0; l<7; l++){
//          mDf[i][j] += mJ2[i][l]*mDJ[l][j];
//        }
//      }
//    }
//
//    //* Add the daughter momentum to the particle momentum
//
//    ffP[ 3] += m[ 3];
//    ffP[ 4] += m[ 4];
//    ffP[ 5] += m[ 5];
//    ffP[ 6] += m[ 6];
//
//    ffC[ 9] += mV[ 9];
//    ffC[13] += mV[13];
//    ffC[14] += mV[14];
//    ffC[18] += mV[18];
//    ffC[19] += mV[19];
//    ffC[20] += mV[20];
//    ffC[24] += mV[24];
//    ffC[25] += mV[25];
//    ffC[26] += mV[26];
//    ffC[27] += mV[27];
//
//    ffC[6 ] += mDf[3][0]; ffC[7 ] += mDf[3][1]; ffC[8 ] += mDf[3][2];
//    ffC[10] += mDf[4][0]; ffC[11] += mDf[4][1]; ffC[12] += mDf[4][2];
//    ffC[15] += mDf[5][0]; ffC[16] += mDf[5][1]; ffC[17] += mDf[5][2];
//    ffC[21] += mDf[6][0]; ffC[22] += mDf[6][1]; ffC[23] += mDf[6][2];
//
//    ffC[9 ] += mDf[3][3] + mDf[3][3];
//    ffC[13] += mDf[4][3] + mDf[3][4]; ffC[14] += mDf[4][4] + mDf[4][4];
//    ffC[18] += mDf[5][3] + mDf[3][5]; ffC[19] += mDf[5][4] + mDf[4][5]; ffC[20] += mDf[5][5] + mDf[5][5];
//    ffC[24] += mDf[6][3] + mDf[3][6]; ffC[25] += mDf[6][4] + mDf[4][6]; ffC[26] += mDf[6][5] + mDf[5][6]; ffC[27] += mDf[6][6] + mDf[6][6];
//
//   //* New estimation of the vertex position r += K*zeta
//
//    for(int i=0;i<7;++i) 
//      fP[i] = ffP[i];
//
//    //* New covariance matrix C -= K*(mCH')'
//
//    for(int i=0, k=0;i<7;++i){
//      for(int j=0;j<=i;++j,++k){
//        fC[k] = ffC[k];
//      }
//    }
//    //* Calculate Chi^2 
//
//    fNDF  += 2;
//    fQ    +=  Daughter.GetQ();
//    fSFromDecay = 0;    
//    fChi2 += (mS[0]*zeta[0] + mS[1]*zeta[1] + mS[3]*zeta[2])*zeta[0]
//      +      (mS[1]*zeta[0] + mS[2]*zeta[1] + mS[4]*zeta[2])*zeta[1]
//      +      (mS[3]*zeta[0] + mS[4]*zeta[1] + mS[5]*zeta[2])*zeta[2];
//  }
//}
//
//void SetProductionVertex( const KFParticleBase &Vtx )
//{
//  //* Set production vertex for the particle, when the particle was not used in the vertex fit
//
//  const double[] m = Vtx.fP, *mV = Vtx.fC;
//
//  bool noS = ( fC[35]<=0 ); // no decay length allowed
//
//  if( noS ){ 
//    TransportToDecayVertex();
//    fP[7] = 0;
//    fC[28] = fC[29] = fC[30] = fC[31] = fC[32] = fC[33] = fC[34] = fC[35] = 0;
//  } else {
//    TransportToDS( GetDStoPoint( m ) );
//    fP[7] = -fSFromDecay;
//    fC[28] = fC[29] = fC[30] = fC[31] = fC[32] = fC[33] = fC[34] = 0;
//    fC[35] = 0.1;
//
//    Convert(1);
//  }
//
//  double mAi[6];
//
//  InvertSym3( fC, mAi );
//
//  double mB[5][3];
//
//  mB[0][0] = fC[ 6]*mAi[0] + fC[ 7]*mAi[1] + fC[ 8]*mAi[3];
//  mB[0][1] = fC[ 6]*mAi[1] + fC[ 7]*mAi[2] + fC[ 8]*mAi[4];
//  mB[0][2] = fC[ 6]*mAi[3] + fC[ 7]*mAi[4] + fC[ 8]*mAi[5];
//
//  mB[1][0] = fC[10]*mAi[0] + fC[11]*mAi[1] + fC[12]*mAi[3];
//  mB[1][1] = fC[10]*mAi[1] + fC[11]*mAi[2] + fC[12]*mAi[4];
//  mB[1][2] = fC[10]*mAi[3] + fC[11]*mAi[4] + fC[12]*mAi[5];
//
//  mB[2][0] = fC[15]*mAi[0] + fC[16]*mAi[1] + fC[17]*mAi[3];
//  mB[2][1] = fC[15]*mAi[1] + fC[16]*mAi[2] + fC[17]*mAi[4];
//  mB[2][2] = fC[15]*mAi[3] + fC[16]*mAi[4] + fC[17]*mAi[5];
//
//  mB[3][0] = fC[21]*mAi[0] + fC[22]*mAi[1] + fC[23]*mAi[3];
//  mB[3][1] = fC[21]*mAi[1] + fC[22]*mAi[2] + fC[23]*mAi[4];
//  mB[3][2] = fC[21]*mAi[3] + fC[22]*mAi[4] + fC[23]*mAi[5];
//
//  mB[4][0] = fC[28]*mAi[0] + fC[29]*mAi[1] + fC[30]*mAi[3];
//  mB[4][1] = fC[28]*mAi[1] + fC[29]*mAi[2] + fC[30]*mAi[4];
//  mB[4][2] = fC[28]*mAi[3] + fC[29]*mAi[4] + fC[30]*mAi[5];
//
//  double z[3] = { m[0]-fP[0], m[1]-fP[1], m[2]-fP[2] };
//
//  {
//    double mAVi[6] = { fC[0]-mV[0], fC[1]-mV[1], fC[2]-mV[2], 
//fC[3]-mV[3], fC[4]-mV[4], fC[5]-mV[5] };
//    
//    if( !InvertSym3( mAVi, mAVi ) ){
//
//      double dChi2 = ( +(mAVi[0]*z[0] + mAVi[1]*z[1] + mAVi[3]*z[2])*z[0]
// +(mAVi[1]*z[0] + mAVi[2]*z[1] + mAVi[4]*z[2])*z[1]
// +(mAVi[3]*z[0] + mAVi[4]*z[1] + mAVi[5]*z[2])*z[2] );
//      
//      // Take Abs(dChi2) here. Negative value of 'det' or 'dChi2' shows that the particle 
//      // was not used in the production vertex fit
//      
//      fChi2+= abs( dChi2 );
//    }
//    fNDF  += 2;
//  }
//  
//  fP[0] = m[0];
//  fP[1] = m[1];
//  fP[2] = m[2];
//  fP[3]+= mB[0][0]*z[0] + mB[0][1]*z[1] + mB[0][2]*z[2];
//  fP[4]+= mB[1][0]*z[0] + mB[1][1]*z[1] + mB[1][2]*z[2];
//  fP[5]+= mB[2][0]*z[0] + mB[2][1]*z[1] + mB[2][2]*z[2];
//  fP[6]+= mB[3][0]*z[0] + mB[3][1]*z[1] + mB[3][2]*z[2];
//  fP[7]+= mB[4][0]*z[0] + mB[4][1]*z[1] + mB[4][2]*z[2];
//  
//  double d0, d1, d2;
//
//  fC[0] = mV[0];
//  fC[1] = mV[1];
//  fC[2] = mV[2];
//  fC[3] = mV[3];
//  fC[4] = mV[4];
//  fC[5] = mV[5];
//
//  d0= mB[0][0]*mV[0] + mB[0][1]*mV[1] + mB[0][2]*mV[3] - fC[ 6];
//  d1= mB[0][0]*mV[1] + mB[0][1]*mV[2] + mB[0][2]*mV[4] - fC[ 7];
//  d2= mB[0][0]*mV[3] + mB[0][1]*mV[4] + mB[0][2]*mV[5] - fC[ 8];
//
//  fC[ 6]+= d0;
//  fC[ 7]+= d1;
//  fC[ 8]+= d2;
//  fC[ 9]+= d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//
//  d0= mB[1][0]*mV[0] + mB[1][1]*mV[1] + mB[1][2]*mV[3] - fC[10];
//  d1= mB[1][0]*mV[1] + mB[1][1]*mV[2] + mB[1][2]*mV[4] - fC[11];
//  d2= mB[1][0]*mV[3] + mB[1][1]*mV[4] + mB[1][2]*mV[5] - fC[12];
//
//  fC[10]+= d0;
//  fC[11]+= d1;
//  fC[12]+= d2;
//  fC[13]+= d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//  fC[14]+= d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//
//  d0= mB[2][0]*mV[0] + mB[2][1]*mV[1] + mB[2][2]*mV[3] - fC[15];
//  d1= mB[2][0]*mV[1] + mB[2][1]*mV[2] + mB[2][2]*mV[4] - fC[16];
//  d2= mB[2][0]*mV[3] + mB[2][1]*mV[4] + mB[2][2]*mV[5] - fC[17];
//
//  fC[15]+= d0;
//  fC[16]+= d1;
//  fC[17]+= d2;
//  fC[18]+= d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//  fC[19]+= d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//  fC[20]+= d0*mB[2][0] + d1*mB[2][1] + d2*mB[2][2];
//
//  d0= mB[3][0]*mV[0] + mB[3][1]*mV[1] + mB[3][2]*mV[3] - fC[21];
//  d1= mB[3][0]*mV[1] + mB[3][1]*mV[2] + mB[3][2]*mV[4] - fC[22];
//  d2= mB[3][0]*mV[3] + mB[3][1]*mV[4] + mB[3][2]*mV[5] - fC[23];
//
//  fC[21]+= d0;
//  fC[22]+= d1;
//  fC[23]+= d2;
//  fC[24]+= d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//  fC[25]+= d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//  fC[26]+= d0*mB[2][0] + d1*mB[2][1] + d2*mB[2][2];
//  fC[27]+= d0*mB[3][0] + d1*mB[3][1] + d2*mB[3][2];
//
//  d0= mB[4][0]*mV[0] + mB[4][1]*mV[1] + mB[4][2]*mV[3] - fC[28];
//  d1= mB[4][0]*mV[1] + mB[4][1]*mV[2] + mB[4][2]*mV[4] - fC[29];
//  d2= mB[4][0]*mV[3] + mB[4][1]*mV[4] + mB[4][2]*mV[5] - fC[30];
//
//  fC[28]+= d0;
//  fC[29]+= d1;
//  fC[30]+= d2;
//  fC[31]+= d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//  fC[32]+= d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//  fC[33]+= d0*mB[2][0] + d1*mB[2][1] + d2*mB[2][2];
//  fC[34]+= d0*mB[3][0] + d1*mB[3][1] + d2*mB[3][2];
//  fC[35]+= d0*mB[4][0] + d1*mB[4][1] + d2*mB[4][2];
//  
//  if( noS ){ 
//    fP[7] = 0;
//    fC[28] = fC[29] = fC[30] = fC[31] = fC[32] = fC[33] = fC[34] = fC[35] = 0;
//  } else {
//    TransportToDS( fP[7] );
//    Convert(0);
//  }
//
//  fSFromDecay = 0;
//}
//
//void SetMassConstraint( double[] mP, double[] mC, double mJ[7][7], double mass )
//{
//  //* Set nonlinear mass constraint (Mass) on the state vector mP with a covariance matrix mC.
//  
//  const double energy2 = mP[6]*mP[6], p2 = mP[3]*mP[3]+mP[4]*mP[4]+mP[5]*mP[5], mass2 = mass*mass;
//
//  const double a = energy2 - p2 + 2.*mass2;
//  const double b = -2.*(energy2 + p2);
//  const double c = energy2 - p2 - mass2;
//
//  double lambda = 0;
//  if(abs(b) > 1.e-10) lambda = -c / b;
//
//  double d = 4.*energy2*p2 - mass2*(energy2-p2-2.*mass2);
//  if(d>=0 && abs(a) > 1.e-10) lambda = (energy2 + p2 - sqrt(d))/a;
//
//  if(mP[6] < 0) //If energy < 0 we need a lambda < 0
//    lambda = -1000000.; //Empirical, a better solution should be found
//
//  int iIter=0;
//  for(iIter=0; iIter<100; iIter++)
//  {
//    double lambda2 = lambda*lambda;
//    double lambda4 = lambda2*lambda2;
//
//    double lambda0 = lambda;
//
//    double f  = -mass2 * lambda4 + a*lambda2 + b*lambda + c;
//    double df = -4.*mass2 * lambda2*lambda + 2.*a*lambda + b;
//    if(abs(df) < 1.e-10) break;
//    lambda -= f/df;
//    if(abs(lambda0 - lambda) < 1.e-8) break;
//  }
//
//  const double lpi = 1./(1. + lambda);
//  const double lmi = 1./(1. - lambda);
//  const double lp2i = lpi*lpi;
//  const double lm2i = lmi*lmi;
//
//  double lambda2 = lambda*lambda;
//
//  double dfl  = -4.*mass2 * lambda2*lambda + 2.*a*lambda + b;
//  double dfx[7] = {0};//,0,0,0};
//  dfx[0] = -2.*(1. + lambda)*(1. + lambda)*mP[3];
//  dfx[1] = -2.*(1. + lambda)*(1. + lambda)*mP[4];
//  dfx[2] = -2.*(1. + lambda)*(1. + lambda)*mP[5];
//  dfx[3] = 2.*(1. - lambda)*(1. - lambda)*mP[6];
//  double dlx[4] = {1,1,1,1};
//  if(abs(dfl) > 1.e-10 )
//  {
//    for(int i=0; i<4; i++)
//      dlx[i] = -dfx[i] / dfl;
//  }
//
//  double dxx[4] = {mP[3]*lm2i, mP[4]*lm2i, mP[5]*lm2i, -mP[6]*lp2i};
//
//  for(int i=0; i<7; i++)
//    for(int j=0; j<7; j++)
//      mJ[i][j]=0;
//  mJ[0][0] = 1.;
//  mJ[1][1] = 1.;
//  mJ[2][2] = 1.;
//
//  for(int i=3; i<7; i++)
//    for(int j=3; j<7; j++)
//      mJ[i][j] = dlx[j-3]*dxx[i-3];
//
//  for(int i=3; i<6; i++)
//    mJ[i][i] += lmi;
//  mJ[6][6] += lpi;
//
//  double mCJ[7][7];
//
//  for(int i=0; i<7; i++) {
//    for(int j=0; j<7; j++) {
//      mCJ[i][j] = 0;
//      for(int k=0; k<7; k++) {
//        mCJ[i][j] += mC[IJ(i,k)]*mJ[j][k];
//      }
//    }
//  }
//
//  for(int i=0; i<7; ++i){
//    for(int j=0; j<=i; ++j){
//      mC[IJ(i,j)]=0;
//      for(int l=0; l<7; l++){
//        mC[IJ(i,j)] += mJ[i][l]*mCJ[l][j];
//      }
//    }
//  }
//
//  mP[3] *= lmi;
//  mP[4] *= lmi;
//  mP[5] *= lmi;
//  mP[6] *= lpi;
//}
//
//void SetNonlinearMassConstraint( double mass )
//{
//  //* Set nonlinear mass constraint (mass)
//
//  double mJ[7][7];
//  SetMassConstraint( fP, fC, mJ, mass );
//  fMassHypo = mass;
//  SumDaughterMass = mass;
//}
//
//void SetMassConstraint( double Mass, double SigmaMass )
//{  
//  //* Set hard( SigmaMass=0 ) or soft (SigmaMass>0) mass constraint 
//
//  fMassHypo = Mass;
//  SumDaughterMass = Mass;
//
//  double m2 = Mass*Mass;            // measurement, weighted by Mass 
//  double s2 = m2*SigmaMass*SigmaMass; // sigma^2
//
//  double p2 = fP[3]*fP[3] + fP[4]*fP[4] + fP[5]*fP[5]; 
//  double e0 = sqrt(m2+p2);
//
//  double mH[8];
//  mH[0] = mH[1] = mH[2] = 0.;
//  mH[3] = -2*fP[3]; 
//  mH[4] = -2*fP[4]; 
//  mH[5] = -2*fP[5]; 
//  mH[6] =  2*fP[6];//e0;
//  mH[7] = 0; 
//
//  double zeta = e0*e0 - e0*fP[6];
//  zeta = m2 - (fP[6]*fP[6]-p2);
//  
//  double mCHt[8], s2_est=0;
//  for( int i=0; i<8; ++i ){
//    mCHt[i] = 0.0;
//    for (int j=0;j<8;++j) mCHt[i] += Cij(i,j)*mH[j];
//    s2_est += mH[i]*mCHt[i];
//  }
//  
//  if( s2_est<1.e-20 ) return; // calculated mass error is already 0, 
//                              // the particle can not be constrained on mass
//
//  double w2 = 1./( s2 + s2_est );
//  fChi2 += zeta*zeta*w2;
//  fNDF  += 1;
//  for( int i=0, ii=0; i<8; ++i ){
//    double ki = mCHt[i]*w2;
//    fP[i]+= ki*zeta;
//    for(int j=0;j<=i;++j) fC[ii++] -= ki*mCHt[j];    
//  }
//}
//
//
//void SetNoDecayLength()
//{  
//  //* Set no decay length for resonances
//
//  TransportToDecayVertex();
//
//  double h[8];
//  h[0] = h[1] = h[2] = h[3] = h[4] = h[5] = h[6] = 0;
//  h[7] = 1; 
//
//  double zeta = 0 - fP[7];
//  for(int i=0;i<8;++i) zeta -= h[i]*(fP[i]-fP[i]);
//  
//  double s = fC[35];   
//  if( s>1.e-20 ){
//    s = 1./s;
//    fChi2 += zeta*zeta*s;
//    fNDF  += 1;
//    for( int i=0, ii=0; i<7; ++i ){
//      double ki = fC[28+i]*s;
//      fP[i]+= ki*zeta;
//      for(int j=0;j<=i;++j) fC[ii++] -= ki*fC[28+j];    
//    }
//  }
//  fP[7] = 0;
//  fC[28] = fC[29] = fC[30] = fC[31] = fC[32] = fC[33] = fC[34] = fC[35] = 0;
//}
//
//
//void Construct( const KFParticleBase* vDaughters[], int nDaughters,
//   const KFParticleBase *Parent,  double Mass, bool IsConstrained         )
//{ 
//  //* Full reconstruction in one go
//
//  int maxIter = 1;
//  bool wasLinearized = fIsLinearized;
//  if( !fIsLinearized || IsConstrained ){
//    //fVtxGuess[0] = fVtxGuess[1] = fVtxGuess[2] = 0;  //!!!!
//    double ds=0., ds1=0.;
//    double P[8], C[36];
//    vDaughters[0].GetDStoParticle(*vDaughters[1], ds, ds1);
//    vDaughters[0].Transport(ds,P,C);
//    fVtxGuess[0] = P[0];
//    fVtxGuess[1] = P[1];
//    fVtxGuess[2] = P[2];
///*    fVtxGuess[0] = GetX();
//    fVtxGuess[1] = GetY();
//    fVtxGuess[2] = GetZ();*/
//
//    fIsLinearized = 1;
//    maxIter = 3;
//  }
//
//  double constraintC[6];
//
//  if( IsConstrained ){
//    for(int i=0;i<6;++i) constraintC[i]=fC[i];
//  } else {
//    for(int i=0;i<6;++i) constraintC[i]=0.;
////    constraintC[0] = constraintC[2] = constraintC[5] = 100.;
//    constraintC[0] = 100.*vDaughters[0].fC[0];
//    constraintC[2] = 100.*vDaughters[0].fC[2];
//    constraintC[5] = 100.*vDaughters[0].fC[5];
//  }
//
//
//  for( int iter=0; iter<maxIter; iter++ ){
//    fAtProductionVertex = 0;
//    fSFromDecay = 0;
//    fP[0] = fVtxGuess[0];
//    fP[1] = fVtxGuess[1];
//    fP[2] = fVtxGuess[2];
//    fP[3] = 0;
//    fP[4] = 0;
//    fP[5] = 0;
//    fP[6] = 0;
//    fP[7] = 0;
//    SumDaughterMass = 0;
//
//    for(int i=0;i<6; ++i) fC[i]=constraintC[i];
//    for(int i=6;i<36;++i) fC[i]=0.;
//    fC[35] = 1.;
//    
//    fNDF  = IsConstrained ?0 :-3;
//    fChi2 =  0.;
//    fQ = 0;
//
//    for( int itr =0; itr<nDaughters; itr++ ){
//      AddDaughter( *vDaughters[itr] );    
//    }
//    if( iter<maxIter-1){
//      for( int i=0; i<3; i++ ) fVtxGuess[i] = fP[i];  
//    }
//  }
//  fIsLinearized = wasLinearized;    
//
//  if( Mass>=0 ) SetMassConstraint( Mass );
//  if( Parent ) SetProductionVertex( *Parent );
//}
//
//
//void Convert( bool ToProduction )
//{
//  //* Tricky function - convert the particle error along its trajectory to 
//  //* the value which corresponds to its production/decay vertex
//  //* It is done by combination of the error of decay length with the position errors
//
//  double fld[3];
//  {
//    GetFieldValue( fP, fld );
//    const double kCLight =  fQ*0.000299792458;
//    fld[0]*=kCLight; fld[1]*=kCLight; fld[2]*=kCLight;
//  }
//
//  double h[6];
//  
//  h[0] = fP[3];
//  h[1] = fP[4];
//  h[2] = fP[5];
//  if( ToProduction ){ h[0]=-h[0]; h[1]=-h[1]; h[2]=-h[2]; } 
//  h[3] = h[1]*fld[2]-h[2]*fld[1];
//  h[4] = h[2]*fld[0]-h[0]*fld[2];
//  h[5] = h[0]*fld[1]-h[1]*fld[0];
//  
//  double c;
//
//  c = fC[28]+h[0]*fC[35];
//  fC[ 0]+= h[0]*(c+fC[28]);
//  fC[28] = c;
//
//  fC[ 1]+= h[1]*fC[28] + h[0]*fC[29];
//  c = fC[29]+h[1]*fC[35];
//  fC[ 2]+= h[1]*(c+fC[29]);
//  fC[29] = c;
//
//  fC[ 3]+= h[2]*fC[28] + h[0]*fC[30];
//  fC[ 4]+= h[2]*fC[29] + h[1]*fC[30];
//  c = fC[30]+h[2]*fC[35];
//  fC[ 5]+= h[2]*(c+fC[30]);
//  fC[30] = c;
//
//  fC[ 6]+= h[3]*fC[28] + h[0]*fC[31];
//  fC[ 7]+= h[3]*fC[29] + h[1]*fC[31];
//  fC[ 8]+= h[3]*fC[30] + h[2]*fC[31];
//  c = fC[31]+h[3]*fC[35];
//  fC[ 9]+= h[3]*(c+fC[31]);
//  fC[31] = c;
//  
//  fC[10]+= h[4]*fC[28] + h[0]*fC[32];
//  fC[11]+= h[4]*fC[29] + h[1]*fC[32];
//  fC[12]+= h[4]*fC[30] + h[2]*fC[32];
//  fC[13]+= h[4]*fC[31] + h[3]*fC[32];
//  c = fC[32]+h[4]*fC[35];
//  fC[14]+= h[4]*(c+fC[32]);
//  fC[32] = c;
//  
//  fC[15]+= h[5]*fC[28] + h[0]*fC[33];
//  fC[16]+= h[5]*fC[29] + h[1]*fC[33];
//  fC[17]+= h[5]*fC[30] + h[2]*fC[33];
//  fC[18]+= h[5]*fC[31] + h[3]*fC[33];
//  fC[19]+= h[5]*fC[32] + h[4]*fC[33];
//  c = fC[33]+h[5]*fC[35];
//  fC[20]+= h[5]*(c+fC[33]);
//  fC[33] = c;
//
//  fC[21]+= h[0]*fC[34];
//  fC[22]+= h[1]*fC[34];
//  fC[23]+= h[2]*fC[34];
//  fC[24]+= h[3]*fC[34];
//  fC[25]+= h[4]*fC[34];
//  fC[26]+= h[5]*fC[34];
//}
//
//
//void TransportToDecayVertex()
//{
//  //* Transport the particle to its decay vertex 
//
//  if( fSFromDecay != 0 ) TransportToDS( -fSFromDecay );
//  if( fAtProductionVertex ) Convert(0);
//  fAtProductionVertex = 0;
//}
//
//void TransportToProductionVertex()
//{
//  //* Transport the particle to its production vertex 
//  
//  if( fSFromDecay != -fP[7] ) TransportToDS( -fSFromDecay-fP[7] );
//  if( !fAtProductionVertex ) Convert( 1 );
//  fAtProductionVertex = 1;
//}
//
//
//void TransportToDS( double dS )
//{ 
//  //* Transport the particle on dS parameter (SignedPath/Momentum) 
// 
//  Transport( dS, fP, fC );
//  fSFromDecay+= dS;
//}
//
//
//double GetDStoPointLine( const double xyz[] ) const 
//{
//  //* Get dS to a certain space point without field
//
//  double p2 = fP[3]*fP[3] + fP[4]*fP[4] + fP[5]*fP[5];  
//  if( p2<1.e-4 ) p2 = 1;
//  return ( fP[3]*(xyz[0]-fP[0]) + fP[4]*(xyz[1]-fP[1]) + fP[5]*(xyz[2]-fP[2]) )/p2;
//}
//
//
//double GetDStoPointBz( double B, const double xyz[] ) 
//  const
//{ 
//  
//  //* Get dS to a certain space point for Bz field
//  const double kCLight = 0.000299792458;
//  double bq = B*fQ*kCLight;
//  double pt2 = fP[3]*fP[3] + fP[4]*fP[4];
//  if( pt2<1.e-4 ) return 0;
//  double dx = xyz[0] - fP[0];
//  double dy = xyz[1] - fP[1]; 
//  double a = dx*fP[3]+dy*fP[4];
//  double dS;
//
//  if( abs(bq)<1.e-8 ) dS = a/pt2;  
//  else dS =  atan2( bq*a, pt2 + bq*(dy*fP[3] -dx*fP[4]) )/bq;
//
//  if(0){
//
//    double px = fP[3];
//    double py = fP[4];
//    double pz = fP[5];
//    double ss[2], g[2][5];
//  
//    ss[0] = dS;
//    ss[1] = -dS;
//    for( int i=0; i<2; i++){
//      double bs = bq*ss[i];
//      double c = cos(bs), s = sin(bs);
//      double cB,sB;
//      if( abs(bq)>1.e-8){
//cB= (1-c)/bq;     
//sB= s/bq;  
//      }else{
//const double kOvSqr6 = 1./sqrt(6.);
//sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*ss[i];
//cB = .5*sB*bs;
//      }
//      g[i][0] = fP[0] + sB*px + cB*py;
//      g[i][1] = fP[1] - cB*px + sB*py;
//      g[i][2] = fP[2] + ss[i]*pz;
//      g[i][3] =       + c*px + s*py;
//      g[i][4] =       - s*px + c*py;      
//    }
//
//    int i=0;
//  
//    double dMin = 1.e10;
//    for( int j=0; j<2; j++){
//      double xx = g[j][0]-xyz[0];
//      double yy = g[j][1]-xyz[1];
//      double zz = g[j][2]-xyz[2];
//      double d = xx*xx + yy*yy + zz*zz;
//      if( d<dMin ){
//dMin = d;
//i = j;
//      }
//    }
//
//    dS = ss[i];
//
//    double x= g[i][0], y= g[i][1], z= g[i][2], ppx= g[i][3], ppy= g[i][4];      
//    double ddx = x-xyz[0];
//    double ddy = y-xyz[1];
//    double ddz = z-xyz[2];
//    double c = ddx*ppx  + ddy*ppy  + ddz*pz ;
//    double pp2 = ppx*ppx + ppy*ppy + pz*pz;    
//    if( abs(pp2)>1.e-8 ){
//      dS+=c/pp2;
//    }
//  }
//  return dS;
//}
//
//double GetDStoPointBy( double B, const double xyz[] ) 
//  const
//{ 
//  
//  //* Get dS to a certain space point for Bz field
//  const double kCLight = 0.000299792458;
//  double bq = B*fQ*kCLight;
//  double pt2 = fP[3]*fP[3] + fP[5]*fP[5];
//  if( pt2<1.e-4 ) return 0;
//  double dx = xyz[0] - fP[0];
//  double dy = -xyz[2] + fP[2]; 
//  double a = dx*fP[3]-dy*fP[5];
//  double dS;
//
//  if( abs(bq)<1.e-8 ) dS = a/pt2;  
//  else dS =  atan2( bq*a, pt2 + bq*(dy*fP[3] +dx*fP[5]) )/bq;
//
//  return dS;
//}
//
//void GetDStoParticleBz( double B, const KFParticleBase &p, 
//   double &DS, double &DS1 ) 
//  const
//{ 
//  //* Get dS to another particle for Bz field
//  double px = fP[3];
//  double py = fP[4];
//  double pz = fP[5];
//
//  double px1 = p.fP[3];
//  double py1 = p.fP[4];
//  double pz1 = p.fP[5];
//
//  const double kCLight = 0.000299792458;
//
//  double bq = B*fQ*kCLight;
//  double bq1 = B*p.fQ*kCLight;
//  double s=0, ds=0, s1=0, ds1=0;
//
//  if( abs(bq)>1.e-8 || abs(bq1)>1.e-8 ){
//
//    double dx = (p.fP[0] - fP[0]);
//    double dy = (p.fP[1] - fP[1]);
//    double d2 = (dx*dx+dy*dy);
//
//    double p2  = (px *px  + py *py); 
//    double p21 = (px1*px1 + py1*py1);
//
//    if( abs(p2) < 1.e-8 || abs(p21) < 1.e-8 )
//    {
//      DS=0.;
//      DS1=0.;
//      return;
//    }
//
//    double a = (px*py1 - py*px1);
//    double b = (px*px1 + py*py1);
//
//    double ldx = bq*bq1*dx - bq1*py + bq*py1 ;
//    double ldy = bq*bq1*dy + bq1*px - bq*px1 ;
//    double l2 = ldx*ldx + ldy*ldy;
//    
//    double cS = bq1*p2 + bq*bq1*(dy* px - dx* py) -  bq*b;
//    double cS1= bq*p21 - bq*bq1*(dy*px1 - dx*py1) - bq1*b;
//
//    double ca  = bq*bq*bq1*d2  +2*( cS + bq*bq*(py1*dx-px1*dy)) ;
//    double ca1 = bq*bq1*bq1*d2 +2*( cS1 - bq1*bq1*(py*dx-px*dy)) ;  
//
//    double sa = 4*l2*p2 - ca*ca;
//    double sa1 = 4*l2*p21 - ca1*ca1;
//
//    if(sa<0) sa=0;
//    if(sa1<0)sa1=0;
//
//    if( abs(bq)>1.e-8){
//      s  = atan2(   bq*( bq1*(dx*px +dy*py) + a ) , cS )/bq;
//      ds = atan2(sqrt(sa),ca)/bq;
//    } else {
//      s = ( (dx*px + dy*py) + (py*px1-px*py1)/bq1)/p2;
//      ds = s*s - (d2-2*(px1*dy-py1*dx)/bq1)/p2; 
//      if( ds<0 ) ds = 0;
//      ds = sqrt(ds);   
//    }
//
//    if( abs(bq1)>1.e-8){
//      s1 = atan2( -bq1*( bq*(dx*px1+dy*py1) + a), cS1 )/bq1;
//      ds1 = atan2(sqrt(sa1),ca1)/bq1;  
//    } else {
//      s1 = (-(dx*px1 + dy*py1) + (py*px1-px*py1)/bq)/p21;
//      ds1 = s1*s1 - (d2+2*(px*dy-py*dx)/bq)/p21; 
//      if( ds1<0 ) ds1 = 0;
//      ds1 = sqrt(ds1);
//    }
//  }
//  double ss[2], ss1[2], g[2][5],g1[2][5];
//  
//  ss[0] = s + ds;
//  ss[1] = s - ds;
//  ss1[0] = s1 + ds1;
//  ss1[1] = s1 - ds1;
//
//  for( int i=0; i<2; i++){
//    double bs = bq*ss[i];
//    double c = cos(bs), sss = sin(bs);
//    double cB,sB;
//    if( abs(bq)>1.e-8){
//      cB= (1-c)/bq;     
//      sB= sss/bq;  
//    }else{
//      const double kOvSqr6 = 1./sqrt(6.);
//      sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*ss[i];
//      cB = .5*sB*bs;
//    }
//    g[i][0] = fP[0] + sB*px + cB*py;
//    g[i][1] = fP[1] - cB*px + sB*py;
//    g[i][2] = fP[2] + ss[i]*pz;
//    g[i][3] =       + c*px + sss*py;
//    g[i][4] =       - sss*px + c*py;
//
//    bs = bq1*ss1[i];  
//    c =  cos(bs); sss = sin(bs);
//    if( abs(bq1)>1.e-8){
//      cB= (1-c)/bq1;   
//      sB= sss/bq1;  
//    }else{
//      const double kOvSqr6 = 1./sqrt(6.);
//      sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*ss1[i];
//      cB = .5*sB*bs;
//    }
//      
//    g1[i][0] = p.fP[0] + sB*px1 + cB*py1;
//    g1[i][1] = p.fP[1] - cB*px1 + sB*py1;
//    g1[i][2] = p.fP[2] + ss1[i]*pz1;
//    g1[i][3] =         + c*px1 + sss*py1;
//    g1[i][4] =         - sss*px1 + c*py1;
//  }
//
//  int i=0, i1=0;
//  
//  double dMin = 1.e10;
//  for( int j=0; j<2; j++){
//    for( int j1=0; j1<2; j1++){
//      double xx = g[j][0]-g1[j1][0];
//      double yy = g[j][1]-g1[j1][1];
//      double zz = g[j][2]-g1[j1][2];
//      double d = xx*xx + yy*yy + zz*zz;
//      if( d<dMin ){
//dMin = d;
//i = j;
//i1 = j1;
//      }
//    }
//  }  
//
//  DS = ss[i];
//  DS1 = ss1[i1];
//  if(0){
//    double x= g[i][0], y= g[i][1], z= g[i][2], ppx= g[i][3], ppy= g[i][4];  
//    double x1=g1[i1][0], y1= g1[i1][1], z1= g1[i1][2], ppx1= g1[i1][3], ppy1= g1[i1][4];  
//    double dx = x1-x;
//    double dy = y1-y;
//    double dz = z1-z;
//    double a = ppx*ppx1 + ppy*ppy1 + pz*pz1;
//    double b = dx*ppx1 + dy*ppy1 + dz*pz1;
//    double c = dx*ppx  + dy*ppy  + dz*pz ;
//    double pp2 = ppx*ppx + ppy*ppy + pz*pz;
//    double pp21= ppx1*ppx1 + ppy1*ppy1 + pz1*pz1;
//    double det = pp2*pp21 - a*a;    
//    if( abs(det)>1.e-8 ){
//      DS+=(a*b-pp21*c)/det;
//      DS1+=(a*c-pp2*b)/det;
//    }
//  }
//}
//
//void GetDStoParticleBy( double B, const KFParticleBase &p, 
//                                        double &DS, double &DS1 ) const
//{ 
////cng this seems NOT to work if B=0.
//if(B==0.) return;
//  //* Get dS to another particle for Bz field
//  double px = fP[3];
//  double py = -fP[5];
//  double pz = fP[4];
//
//  double px1 = p.fP[3];
//  double py1 = -p.fP[5];
//  double pz1 = p.fP[4];
//
//  const double kCLight = 0.000299792458;
//
//  double bq = B*fQ*kCLight;
//  double bq1 = B*p.fQ*kCLight;
//  double s=0, ds=0, s1=0, ds1=0;
//
//  if( abs(bq)>1.e-8 || abs(bq1)>1.e-8 ){
//
//    double dx = (p.fP[0] - fP[0]);
//    double dy = (-p.fP[2] + fP[2]);
//    double d2 = (dx*dx+dy*dy);
//
//    double p2  = (px *px  + py *py); 
//    double p21 = (px1*px1 + py1*py1);
//
//    if( abs(p2) < 1.e-8 || abs(p21) < 1.e-8 )
//    {
//      DS=0.;
//      DS1=0.;
//      return;
//    }
//
//    double a = (px*py1 - py*px1);
//    double b = (px*px1 + py*py1);
//
//    double ldx = bq*bq1*dx - bq1*py + bq*py1 ;
//    double ldy = bq*bq1*dy + bq1*px - bq*px1 ;
//    double l2 = ldx*ldx + ldy*ldy;
//    
//    double cS = bq1*p2 + bq*bq1*(dy* px - dx* py) -  bq*b;
//    double cS1= bq*p21 - bq*bq1*(dy*px1 - dx*py1) - bq1*b;
//
//    double ca  = bq*bq*bq1*d2  +2*( cS + bq*bq*(py1*dx-px1*dy)) ;
//    double ca1 = bq*bq1*bq1*d2 +2*( cS1 - bq1*bq1*(py*dx-px*dy)) ;  
//
//    double sa = 4*l2*p2 - ca*ca;
//    double sa1 = 4*l2*p21 - ca1*ca1;
//
//    if(sa<0) sa=0;
//    if(sa1<0)sa1=0;
//
//    if( abs(bq)>1.e-8){
//      s  = atan2(   bq*( bq1*(dx*px +dy*py) + a ) , cS )/bq;
//      ds = atan2(sqrt(sa),ca)/bq;
//    } else {
//      s = ( (dx*px + dy*py) + (py*px1-px*py1)/bq1)/p2;
//      ds = s*s - (d2-2*(px1*dy-py1*dx)/bq1)/p2; 
//      if( ds<0 ) ds = 0;
//      ds = sqrt(ds);   
//    }
//
//    if( abs(bq1)>1.e-8){
//      s1 = atan2( -bq1*( bq*(dx*px1+dy*py1) + a), cS1 )/bq1;
//      ds1 = atan2(sqrt(sa1),ca1)/bq1;  
//    } else {
//      s1 = (-(dx*px1 + dy*py1) + (py*px1-px*py1)/bq)/p21;
//      ds1 = s1*s1 - (d2+2*(px*dy-py*dx)/bq)/p21; 
//      if( ds1<0 ) ds1 = 0;
//      ds1 = sqrt(ds1);
//    }
//  }
//  double ss[2], ss1[2], g[2][5],g1[2][5];
//  
//  ss[0] = s + ds;
//  ss[1] = s - ds;
//  ss1[0] = s1 + ds1;
//  ss1[1] = s1 - ds1;
//
//  for( int i=0; i<2; i++){
//    double bs = bq*ss[i];
//    double c = cos(bs), sss = sin(bs);
//    double cB,sB;
//    if( abs(bq)>1.e-8){
//      cB= (1-c)/bq;     
//      sB= sss/bq;  
//    }else{
//      const double kOvSqr6 = 1./sqrt(6.);
//      sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*ss[i];
//      cB = .5*sB*bs;
//    }
//    g[i][0] = fP[0] + sB*px + cB*py;
//    g[i][1] = -fP[2] - cB*px + sB*py;
//    g[i][2] = fP[1] + ss[i]*pz;
//    g[i][3] =       + c*px + sss*py;
//    g[i][4] =       - sss*px + c*py;
//
//    bs = bq1*ss1[i];  
//    c =  cos(bs); sss = sin(bs);
//    if( abs(bq1)>1.e-8){
//      cB= (1-c)/bq1;   
//      sB= sss/bq1;  
//    }else{
//      const double kOvSqr6 = 1./sqrt(6.);
//      sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*ss1[i];
//      cB = .5*sB*bs;
//    }
//      
//    g1[i][0] = p.fP[0] + sB*px1 + cB*py1;
//    g1[i][1] = -p.fP[2] - cB*px1 + sB*py1;
//    g1[i][2] = p.fP[1] + ss1[i]*pz1;
//    g1[i][3] =         + c*px1 + sss*py1;
//    g1[i][4] =         - sss*px1 + c*py1;
//  }
//
//  int i=0, i1=0;
//  
//  double dMin = 1.e10;
//  for( int j=0; j<2; j++){
//    for( int j1=0; j1<2; j1++){
//      double xx = g[j][0]-g1[j1][0];
//      double yy = g[j][1]-g1[j1][1];
//      double zz = g[j][2]-g1[j1][2];
//      double d = xx*xx + yy*yy + zz*zz;
//      if( d<dMin ){
//dMin = d;
//i = j;
//i1 = j1;
//      }
//    }
//  }  
//
//  DS = ss[i];
//  DS1 = ss1[i1];
//}
//
//void GetDSIter(const KFParticleBase &p, double const &dS, double x[3], double dx[3], double ddx[3]) const
//{
//  double ssx=0, ssy=0, ssz=0, ssyy=0, ssyz=0, ssyyy=0;
//  double dssx=0, dssy=0, dssz=0, dssyy=0, dssyz=0, dssyyy=0; // d(...)/dS
//  double ddssx=0, ddssy=0, ddssz=0, ddssyy=0, ddssyz=0, ddssyyy=0; // d2(...)/dS2
//  double Kssx=0, Kssy=0, Kssz=0, Kssyy=0, Kssyz=0, Kssyyy=0;
//
//  const double kCLight = 0.000299792458;
//  double c = fQ*kCLight;
//
//  // get field integrals
//
//  double fld[3][3];   
//  double p0[3], p1[3], p2[3];
//
//  // line track approximation
//
//  p0[0] = p.fP[0];
//  p0[1] = p.fP[1];
//  p0[2] = p.fP[2];
//
//  p2[0] = p.fP[0] + p.fP[3]*dS;
//  p2[1] = p.fP[1] + p.fP[4]*dS;
//  p2[2] = p.fP[2] + p.fP[5]*dS;
//
//  p1[0] = 0.5*(p0[0]+p2[0]);
//  p1[1] = 0.5*(p0[1]+p2[1]);
//  p1[2] = 0.5*(p0[2]+p2[2]);
//
//  // first order track approximation
//  {
//    GetFieldValue( p0, fld[0] );
//    GetFieldValue( p1, fld[1] );
//    GetFieldValue( p2, fld[2] );
//
//    double ssy1 = ( 7*fld[0][1] + 6*fld[1][1]-fld[2][1] )*c*dS*dS/96.;
//    double ssy2 = (   fld[0][1] + 2*fld[1][1]         )*c*dS*dS/6.;
//
//    p1[0] -= ssy1*p.fP[5];
//    p1[2] += ssy1*p.fP[3];
//    p2[0] -= ssy2*p.fP[5];
//    p2[2] += ssy2*p.fP[3];
//  }
//
//  GetFieldValue( p0, fld[0] );
//  GetFieldValue( p1, fld[1] );
//  GetFieldValue( p2, fld[2] );
//
//  Kssx = c*( fld[0][0] + 2*fld[1][0]) / 6.;
//  Kssy = c*( fld[0][1] + 2*fld[1][1]) / 6.;
//  Kssz = c*( fld[0][2] + 2*fld[1][2]) / 6.;
//
////  double c2[3][3]    =   { {  5, -4, -1},{  44,  80,  -4},{ 11, 44, 5} }; // /=360.    
//  double cc2[3][3]    =   { { 38,  8, -4},{ 148, 208, -20},{  3, 36, 3} }; // /=2520.
//  for(int n=0; n<3; n++)
//    for(int m=0; m<3; m++) 
//    {
//      Kssyz += cc2[n][m]*fld[n][1]*fld[m][2];
//    }
//  Kssyz *= c*c / 2520.;
//
//  Kssyy = ( fld[0][1]*( 38*fld[0][1] + 156*fld[1][1]  -   fld[2][1] )+
//            fld[1][1]*(              208*fld[1][1]  +16*fld[2][1] )+
//            fld[2][1]*(                             3*fld[2][1] )  
//          )*c*c/2520.;
//  Kssyyy = ( fld[0][1]*( fld[0][1]*( 85*fld[0][1] + 526*fld[1][1]  - 7*fld[2][1] )+
//             fld[1][1]*(             1376*fld[1][1]  +84*fld[2][1] )+
//             fld[2][1]*(                            19*fld[2][1] )  )+
//             fld[1][1]*( fld[1][1]*(             1376*fld[1][1] +256*fld[2][1] )+
//             fld[2][1]*(                            62*fld[2][1] )  )+
//             fld[2][1]*fld[2][1]  *(                             3*fld[2][1] )
//           )*c*c*c/90720.;
//
//  ssx = Kssx * dS*dS;
//  ssy = Kssy * dS*dS;
//  ssz = Kssz * dS*dS;
//  ssyz = Kssyz * dS*dS*dS;
//  ssyy = Kssyy * dS*dS*dS;
//  ssyyy = Kssyyy * dS*dS*dS*dS;
//
//  dssx = 2.*Kssx * dS;
//  dssy = 2.*Kssy * dS;
//  dssz = 2.*Kssz * dS;
//  dssyz = 3.*Kssyz * dS*dS;
//  dssyy = 3.*Kssyy * dS*dS;
//  dssyyy = 4.*Kssyyy * dS*dS*dS;
//
//  ddssx = 2.*Kssx;
//  ddssy = 2.*Kssy;
//  ddssz = 2.*Kssz;
//  ddssyz = 2.*3.*Kssyz * dS;
//  ddssyy = 2.*3.*Kssyy * dS;
//  ddssyyy = 3.*4.*Kssyyy * dS*dS;
//
//  double mJ[3][3];
//  for( int iJ=0; iJ<3; iJ++ ) for( int jJ=0; jJ<3; jJ++) mJ[iJ][jJ]=0;
//
//  mJ[0][0]=dS-ssyy;   mJ[0][1]=ssx;  mJ[0][2]=ssyyy-ssy;
//  mJ[1][0]=-ssz;      mJ[1][1]=dS;   mJ[1][2]=ssx+ssyz;
//  mJ[2][0]=ssy-ssyyy; mJ[2][1]=-ssx; mJ[2][2]=dS-ssyy;
//
//  x[0] = p.fP[0] + mJ[0][0]*p.fP[3] + mJ[0][1]*p.fP[4] + mJ[0][2]*p.fP[5];
//  x[1] = p.fP[1] + mJ[1][0]*p.fP[3] + mJ[1][1]*p.fP[4] + mJ[1][2]*p.fP[5];
//  x[2] = p.fP[2] + mJ[2][0]*p.fP[3] + mJ[2][1]*p.fP[4] + mJ[2][2]*p.fP[5];
//
//  mJ[0][0]= 1-dssyy;      mJ[0][1]= dssx;  mJ[0][2]= dssyyy-dssy;
//  mJ[1][0]= -dssz;        mJ[1][1]= 1;     mJ[1][2]= dssx+dssyz;
//  mJ[2][0]= dssy-dssyyy;  mJ[2][1]= -dssx; mJ[2][2]= 1-dssyy;
//
//  dx[0] = p.fP[0] + mJ[0][0]*p.fP[3] + mJ[0][1]*p.fP[4] + mJ[0][2]*p.fP[5];
//  dx[1] = p.fP[1] + mJ[1][0]*p.fP[3] + mJ[1][1]*p.fP[4] + mJ[1][2]*p.fP[5];
//  dx[2] = p.fP[2] + mJ[2][0]*p.fP[3] + mJ[2][1]*p.fP[4] + mJ[2][2]*p.fP[5];
//
//  mJ[0][0]= -ddssyy;        mJ[0][1]= ddssx;  mJ[0][2]= ddssyyy-ddssy;
//  mJ[1][0]= -ddssz;         mJ[1][1]= 0;      mJ[1][2]= ddssx+ddssyz;
//  mJ[2][0]= ddssy-ddssyyy;  mJ[2][1]= -ddssx; mJ[2][2]= -ddssyy;
//
//  ddx[0] = p.fP[0] + mJ[0][0]*p.fP[3] + mJ[0][1]*p.fP[4] + mJ[0][2]*p.fP[5];
//  ddx[1] = p.fP[1] + mJ[1][0]*p.fP[3] + mJ[1][1]*p.fP[4] + mJ[1][2]*p.fP[5];
//  ddx[2] = p.fP[2] + mJ[2][0]*p.fP[3] + mJ[2][1]*p.fP[4] + mJ[2][2]*p.fP[5];
//}
//
//double GetDStoPointCBM( const double xyz[] ) const
//{
//  //* Transport the particle on dS, output to P[],C[], for CBM field
//
//  double dS = 0;
//  //Linear approximation
//
//  double fld[3];
//  GetFieldValue( fP, fld );
//// 
////   GetDStoParticleBy(fld[1],p,dS,dS1);
//  dS = GetDStoPointLine( xyz );
//
//  if( fQ==0 ){
//    return dS;
//  }
//
//  dS = GetDStoPointBy( fld[1],xyz );
//
////  const double &px   = fP[3], &py   = fP[4], &pz   = fP[5];
//
//
//  // construct coefficients 
//// int NIt;
////   for(int i=0; i<100; i++)
////   {
//// //std::cout << "  dS temp "<<dS << std::endl;
////     double x[3], dx[3], ddx[3];
//// 
////     GetDSIter(*this, dS, x, dx, ddx);
//// 
////     double dr[3] = { x[0] - xyz[0], x[1] - xyz[1], x[2] - xyz[2] };
//// 
////     double dS0 = dS;
//// 
////     double f  = dx[0] * dr[0] + dx[1] * dr[1] + dx[2] * dr[2];
////     double df  = ddx[0] * dr[0] + ddx[1] * dr[1] + ddx[2] * dr[2] + dx[0]*dx[0] + dx[1]*dx[1] + dx[2]*dx[2];
//// 
////     dS = dS - f/df;
////     NIt++;
////     if(fabs(dS - dS0) < fabs(dS)*0.001) break;
////   }
////std::cout << "NIt  " << NIt <<"  dS  "<<  dS << std::endl;
//  return dS;
//}
//
//void GetDStoParticleLine( const KFParticleBase &p, double &dS, double &dS1 ) const
//{
//  double p12 = fP[3]*fP[3] + fP[4]*fP[4] + fP[5]*fP[5];
//  double p22 = p.fP[3]*p.fP[3] + p.fP[4]*p.fP[4] + p.fP[5]*p.fP[5];
//  double p1p2 = fP[3]*p.fP[3] + fP[4]*p.fP[4] + fP[5]*p.fP[5];
//
//  double drp1 = fP[3]*(p.fP[0]-fP[0]) + fP[4]*(p.fP[1]-fP[1]) + fP[5]*(p.fP[2]-fP[2]);
//  double drp2 = p.fP[3]*(p.fP[0]-fP[0]) + p.fP[4]*(p.fP[1]-fP[1]) + p.fP[5]*(p.fP[2]-fP[2]);
//
//  double detp =  p1p2*p1p2 - p12*p22;
//  if( fabs(detp)<1.e-4 ) detp = 1; //TODO correct!!!
//
//  dS  = (drp2*p1p2 - drp1*p22) /detp;
//  dS1 = (drp2*p12  - drp1*p1p2)/detp;
//}
//
//void GetDStoParticleCBM( const KFParticleBase &p, double &dS, double &dS1 ) const
//{
//  //* Transport the particle on dS, output to P[],C[], for CBM field
//
//  GetDStoParticleLine( p, dS, dS1 );
//  if( fQ==0 ){
//    return;
//  }
//
//  double fld[3];
//  GetFieldValue( fP, fld );
//
//  double fld1[3];
//  GetFieldValue( p.fP, fld1 );
//
//
//  GetDStoParticleBy((fld[1]+fld1[1])/2,p,dS,dS1);
//
////   construct coefficients 
//// int NIt;
////   for(int i=0; i<10; i++)
////   {
//// /*std::cout << "  dS temp "<<dS <<  "  dS1 temp "<< dS1 << std::endl;*/
////     double x[3],  dx[3],  ddx[3];
////     double x1[3], dx1[3], ddx1[3];
//// 
////     GetDSIter(*this, dS, x, dx, ddx);
////     GetDSIter(p, dS1, x1, dx1, ddx1);
//// 
////     double dr[3] = { x[0] - x1[0], x[1] - x1[1], x[2] - x1[2] };
//// 
////     double dS0 = dS;
////     double dS10 = dS1;
//// 
////     double f    = dx[0]  * dr[0] + dx[1]  * dr[1] + dx[2]  * dr[2];
////     double dfs  = ddx[0] * dr[0] + ddx[1] * dr[1] + ddx[2] * dr[2] + dx[0]*dx[0] + dx[1]*dx[1] + dx[2]*dx[2];
////     double dfs1 = -ddx[0] * ddx1[0] - ddx[1] * ddx1[1] - ddx[2] * ddx1[2];
//// 
////     double f1    = dx1[0]  * dr[0] + dx1[1]  * dr[1] + dx1[2]  * dr[2];
////     double df1s1 = ddx1[0] * dr[0] + ddx1[1] * dr[1] + ddx1[2] * dr[2] - dx1[0]*dx1[0] - dx1[1]*dx1[1] - dx1[2]*dx1[2];
////     double df1s  = -dfs1;
//// 
////     double det = dfs*df1s1 - dfs1*df1s;
////     if(fabs(det) < 1.e-8) det = 1.e-8;
//// 
////     double DS  = dfs1*f1 - f*df1s1;
////     double DS1 = f*df1s - f1*dfs;
//// 
////     dS  += DS/det;
////     dS1 += DS1/det;
////     NIt++;
//// /*    if( (fabs(dS - dS0) < fabs(dS)*0.001) && (fabs(dS1 - dS10) < fabs(dS1)*0.001)) break;*/
////   }
////std::cout << "NIt  " << NIt <<"  dS  "<<  dS << "  dS1  "<<  dS1 << std::endl;
//}
//
//void TransportCBM( double dS, 
// double P[], double C[] ) const
//{  
//  //* Transport the particle on dS, output to P[],C[], for CBM field
// 
//  if( fQ==0 ){
//    TransportLine( dS, P, C );
//    return;
//  }
//
//  const double kCLight = 0.000299792458;
//
//  double c = fQ*kCLight;
//
//  // construct coefficients 
//
//  double 
//    px   = fP[3],
//    py   = fP[4],
//    pz   = fP[5];
//      
//  double sx=0, sy=0, sz=0, syy=0, syz=0, syyy=0, ssx=0, ssy=0, ssz=0, ssyy=0, ssyz=0, ssyyy=0;
//
//  { // get field integrals
//
//    double fld[3][3];   
//    double p0[3], p1[3], p2[3];
//
//    // line track approximation
//
//    p0[0] = fP[0];
//    p0[1] = fP[1];
//    p0[2] = fP[2];
//  
//    p2[0] = fP[0] + px*dS;
//    p2[1] = fP[1] + py*dS;
//    p2[2] = fP[2] + pz*dS;
//  
//    p1[0] = 0.5*(p0[0]+p2[0]);
//    p1[1] = 0.5*(p0[1]+p2[1]);
//    p1[2] = 0.5*(p0[2]+p2[2]);
//
//    // first order track approximation
//    {
//      GetFieldValue( p0, fld[0] );
//      GetFieldValue( p1, fld[1] );
//      GetFieldValue( p2, fld[2] );
//
//      double ssy1 = ( 7*fld[0][1] + 6*fld[1][1]-fld[2][1] )*c*dS*dS/96.;
//      double ssy2 = (   fld[0][1] + 2*fld[1][1]         )*c*dS*dS/6.;
//
//      p1[0] -= ssy1*pz;
//      p1[2] += ssy1*px;
//      p2[0] -= ssy2*pz;
//      p2[2] += ssy2*px;   
//    }
//
//    GetFieldValue( p0, fld[0] );
//    GetFieldValue( p1, fld[1] );
//    GetFieldValue( p2, fld[2] );
//    
//    sx = c*( fld[0][0] + 4*fld[1][0] + fld[2][0] )*dS/6.;
//    sy = c*( fld[0][1] + 4*fld[1][1] + fld[2][1] )*dS/6.;
//    sz = c*( fld[0][2] + 4*fld[1][2] + fld[2][2] )*dS/6.;
//
//    ssx = c*( fld[0][0] + 2*fld[1][0])*dS*dS/6.;
//    ssy = c*( fld[0][1] + 2*fld[1][1])*dS*dS/6.;
//    ssz = c*( fld[0][2] + 2*fld[1][2])*dS*dS/6.;
//
//    double c2[3][3]    =   { {  5, -4, -1},{  44,  80,  -4},{ 11, 44, 5} }; // /=360.    
//    double cc2[3][3]    =   { { 38,  8, -4},{ 148, 208, -20},{  3, 36, 3} }; // /=2520.
//    for(int n=0; n<3; n++)
//      for(int m=0; m<3; m++) 
//{
//  syz += c2[n][m]*fld[n][1]*fld[m][2];
//  ssyz += cc2[n][m]*fld[n][1]*fld[m][2];
//}
// 
//    syz  *= c*c*dS*dS/360.;
//    ssyz  *= c*c*dS*dS*dS/2520.;
//    
//    syy  = c*( fld[0][1] + 4*fld[1][1] + fld[2][1] )*dS;
//    syyy = syy*syy*syy / 1296;
//    syy  = syy*syy/72;
//
//    ssyy = ( fld[0][1]*( 38*fld[0][1] + 156*fld[1][1]  -   fld[2][1] )+
//    fld[1][1]*(              208*fld[1][1]  +16*fld[2][1] )+
//    fld[2][1]*(                             3*fld[2][1] )  
//    )*dS*dS*dS*c*c/2520.;
//    ssyyy = 
//      (
//       fld[0][1]*( fld[0][1]*( 85*fld[0][1] + 526*fld[1][1]  - 7*fld[2][1] )+
// fld[1][1]*(             1376*fld[1][1]  +84*fld[2][1] )+
// fld[2][1]*(                            19*fld[2][1] )  )+
//       fld[1][1]*( fld[1][1]*(             1376*fld[1][1] +256*fld[2][1] )+
// fld[2][1]*(                            62*fld[2][1] )  )+
//       fld[2][1]*fld[2][1]  *(                             3*fld[2][1] )       
//       )*dS*dS*dS*dS*c*c*c/90720.;    
// 
//  }
//
//  double mJ[8][8];
//  for( int i=0; i<8; i++ ) for( int j=0; j<8; j++) mJ[i][j]=0;
//
//  mJ[0][0]=1; mJ[0][1]=0; mJ[0][2]=0; mJ[0][3]=dS-ssyy;  mJ[0][4]=ssx;  mJ[0][5]=ssyyy-ssy;
//  mJ[1][0]=0; mJ[1][1]=1; mJ[1][2]=0; mJ[1][3]=-ssz;     mJ[1][4]=dS;  mJ[1][5]=ssx+ssyz;
//  mJ[2][0]=0; mJ[2][1]=0; mJ[2][2]=1; mJ[2][3]=ssy-ssyyy; mJ[2][4]=-ssx; mJ[2][5]=dS-ssyy;
//  
//  mJ[3][0]=0; mJ[3][1]=0; mJ[3][2]=0; mJ[3][3]=1-syy;   mJ[3][4]=sx;  mJ[3][5]=syyy-sy;
//  mJ[4][0]=0; mJ[4][1]=0; mJ[4][2]=0; mJ[4][3]=-sz;     mJ[4][4]=1;   mJ[4][5]=sx+syz;
//  mJ[5][0]=0; mJ[5][1]=0; mJ[5][2]=0; mJ[5][3]=sy-syyy; mJ[5][4]=-sx; mJ[5][5]=1-syy;
//  mJ[6][6] = mJ[7][7] = 1;
//  
//  P[0] = fP[0] + mJ[0][3]*px + mJ[0][4]*py + mJ[0][5]*pz;
//  P[1] = fP[1] + mJ[1][3]*px + mJ[1][4]*py + mJ[1][5]*pz;
//  P[2] = fP[2] + mJ[2][3]*px + mJ[2][4]*py + mJ[2][5]*pz;
//  P[3] =        mJ[3][3]*px + mJ[3][4]*py + mJ[3][5]*pz;
//  P[4] =        mJ[4][3]*px + mJ[4][4]*py + mJ[4][5]*pz;
//  P[5] =        mJ[5][3]*px + mJ[5][4]*py + mJ[5][5]*pz;
//  P[6] = fP[6];
//  P[7] = fP[7];
//
//  MultQSQt( mJ[0], fC, C);
//
//}
//
//
//void TransportBz( double b, double t,
//     double p[], double e[] ) const 
//{ 
//  //* Transport the particle on dS, output to P[],C[], for Bz field
// 
//  const double kCLight = 0.000299792458;
//  b = b*fQ*kCLight;
//  double bs= b*t;
//  double s = sin(bs), c = cos(bs);
//  double sB, cB;
//  if( abs(bs)>1.e-10){
//    sB= s/b;
//    cB= (1-c)/b;
//  }else{
//    const double kOvSqr6 = 1./sqrt(6.);
//    sB = (1.-bs*kOvSqr6)*(1.+bs*kOvSqr6)*t;
//    cB = .5*sB*bs;
//  }
//  
//  double px = fP[3];
//  double py = fP[4];
//  double pz = fP[5];
//
//  p[0] = fP[0] + sB*px + cB*py;
//  p[1] = fP[1] - cB*px + sB*py;
//  p[2] = fP[2] +  t*pz;
//  p[3] =          c*px + s*py;
//  p[4] =         -s*px + c*py;
//  p[5] = fP[5];
//  p[6] = fP[6];
//  p[7] = fP[7];
//
//  /* 
//  double mJ[8][8] = { {1,0,0,   sB, cB,  0, 0, 0 },
//{0,1,0,  -cB, sB,  0, 0, 0 },
//{0,0,1,    0,  0,  t, 0, 0 },
//{0,0,0,    c,  s,  0, 0, 0 },
//{0,0,0,   -s,  c,  0, 0, 0 },
//{0,0,0,    0,  0,  1, 0, 0 },
//{0,0,0,    0,  0,  0, 1, 0 },
//{0,0,0,    0,  0,  0, 0, 1 }  };
//  double mA[8][8];
//  for( int k=0,i=0; i<8; i++)
//    for( int j=0; j<=i; j++, k++ ) mA[i][j] = mA[j][i] = fC[k]; 
//
//  double mJC[8][8];
//  for( int i=0; i<8; i++ )
//    for( int j=0; j<8; j++ ){
//      mJC[i][j]=0;
//      for( int k=0; k<8; k++ ) mJC[i][j]+=mJ[i][k]*mA[k][j];
//    }
//  
//  for( int k=0,i=0; i<8; i++)
//    for( int j=0; j<=i; j++, k++ ){
//      e[k] = 0;
//      for( int l=0; l<8; l++ ) e[k]+=mJC[i][l]*mJ[j][l];
//    }
//  
//  return;
//  */
//
//  double 
//    c6=fC[6], c7=fC[7], c8=fC[8], c17=fC[17], c18=fC[18],
//    c24 = fC[24], c31 = fC[31];
//
//  double 
//    cBC13 = cB*fC[13],
//    mJC13 = c7 - cB*fC[9] + sB*fC[13],
//    mJC14 = fC[11] - cBC13 + sB*fC[14],
//    mJC23 = c8 + t*c18,
//    mJC24 = fC[12] + t*fC[19],
//    mJC33 = c*fC[9] + s*fC[13],
//    mJC34 = c*fC[13] + s*fC[14],
//    mJC43 = -s*fC[9] + c*fC[13],
//    mJC44 = -s*fC[13] + c*fC[14];
//
//
//  e[0]= fC[0] + 2*(sB*c6 + cB*fC[10]) + (sB*fC[9] + 2*cBC13)*sB + cB*cB*fC[14];
//  e[1]= fC[1] - cB*c6 + sB*fC[10] + mJC13*sB + mJC14*cB;
//  e[2]= fC[2] - cB*c7 + sB*fC[11] - mJC13*cB + mJC14*sB;
//  e[3]= fC[3] + t*fC[15] + mJC23*sB + mJC24*cB;
//  e[4]= fC[4] + t*fC[16] - mJC23*cB + mJC24*sB;
//
//  e[15]= fC[15] + c18*sB + fC[19]*cB;
//  e[16]= fC[16] - c18*cB + fC[19]*sB;
//  e[17]= c17 + fC[20]*t;
//  e[18]= c18*c + fC[19]*s;
//  e[19]= -c18*s + fC[19]*c;
//
//  e[5]= fC[5] + (c17 + e[17] )*t;
//
//  e[6]= c*c6 + s*fC[10] + mJC33*sB + mJC34*cB;
//  e[7]= c*c7 + s*fC[11] - mJC33*cB + mJC34*sB;
//  e[8]= c*c8 + s*fC[12] + e[18]*t;
//  e[9]= mJC33*c + mJC34*s;
//  e[10]= -s*c6 + c*fC[10] + mJC43*sB + mJC44*cB;
//
//    
//  e[11]= -s*c7 + c*fC[11] - mJC43*cB + mJC44*sB;
//  e[12]= -s*c8 + c*fC[12] + e[19]*t;
//  e[13]= mJC43*c + mJC44*s;
//  e[14]= -mJC43*s + mJC44*c;
//  e[20]= fC[20];
//  e[21]= fC[21] + fC[25]*cB + c24*sB;
//  e[22]= fC[22] - c24*cB + fC[25]*sB;
//  e[23]= fC[23] + fC[26]*t;
//  e[24]= c*c24 + s*fC[25];
//  e[25]= c*fC[25] - c24*s;
//  e[26]= fC[26];
//  e[27]= fC[27];
//  e[28]= fC[28] + fC[32]*cB + c31*sB;
//  e[29]= fC[29] - c31*cB + fC[32]*sB;
//  e[30]= fC[30] + fC[33]*t;
//  e[31]= c*c31 + s*fC[32];
//  e[32]= c*fC[32] - s*c31;
//  e[33]= fC[33];
//  e[34]= fC[34];
//  e[35]= fC[35];     
//}
//
//
//double GetDistanceFromVertex( const KFParticleBase &Vtx ) const
//{
//  //* Calculate distance from vertex [cm]
//
//  return GetDistanceFromVertex( Vtx.fP );
//}
//
//double GetDistanceFromVertex( const double vtx[] ) const
//{
//  //* Calculate distance from vertex [cm]
//
//  double mP[8], mC[36];  
//  Transport( GetDStoPoint(vtx), mP, mC );
//  double d[3]={ vtx[0]-mP[0], vtx[1]-mP[1], vtx[2]-mP[2]};
//  return sqrt( d[0]*d[0]+d[1]*d[1]+d[2]*d[2] );
//}
//
//double GetDistanceFromParticle( const KFParticleBase &p ) 
//  const
//{ 
//  //* Calculate distance to other particle [cm]
//
//  double dS, dS1;
//  GetDStoParticle( p, dS, dS1 );   
//  double mP[8], mC[36], mP1[8], mC1[36];
//  Transport( dS, mP, mC ); 
//  p.Transport( dS1, mP1, mC1 ); 
//  double dx = mP[0]-mP1[0]; 
//  double dy = mP[1]-mP1[1]; 
//  double dz = mP[2]-mP1[2]; 
//  return sqrt(dx*dx+dy*dy+dz*dz);
//}
//
//double GetDeviationFromVertex( const KFParticleBase &Vtx ) const
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//
//  return GetDeviationFromVertex( Vtx.fP, Vtx.fC );
//}
//
//
//double GetDeviationFromVertex( const double v[], const double Cv[] ) const
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//  //* v = [xyz], Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix
//
//  double mP[8];
//  double mC[36];
//  
//  Transport( GetDStoPoint(v), mP, mC );  
//
//  double d[3]={ v[0]-mP[0], v[1]-mP[1], v[2]-mP[2]};
//
//  double sigmaS = .1+10.*sqrt( (d[0]*d[0]+d[1]*d[1]+d[2]*d[2])/
// (mP[3]*mP[3]+mP[4]*mP[4]+mP[5]*mP[5])  );
//
//   
//  double h[3] = { mP[3]*sigmaS, mP[4]*sigmaS, mP[5]*sigmaS };       
//  
//  double mSi[6] = 
//    { mC[0] +h[0]*h[0], 
//      mC[1] +h[1]*h[0], mC[2] +h[1]*h[1], 
//      mC[3] +h[2]*h[0], mC[4] +h[2]*h[1], mC[5] +h[2]*h[2] };
//
//  if( Cv ){
//    mSi[0]+=Cv[0];
//    mSi[1]+=Cv[1];
//    mSi[2]+=Cv[2];
//    mSi[3]+=Cv[3];
//    mSi[4]+=Cv[4];
//    mSi[5]+=Cv[5];
//  }
//  
//  double mS[6];
//
//  mS[0] = mSi[2]*mSi[5] - mSi[4]*mSi[4];
//  mS[1] = mSi[3]*mSi[4] - mSi[1]*mSi[5];
//  mS[2] = mSi[0]*mSi[5] - mSi[3]*mSi[3];
//  mS[3] = mSi[1]*mSi[4] - mSi[2]*mSi[3];
//  mS[4] = mSi[1]*mSi[3] - mSi[0]*mSi[4];
//  mS[5] = mSi[0]*mSi[2] - mSi[1]*mSi[1]; 
//      
//  double s = ( mSi[0]*mS[0] + mSi[1]*mS[1] + mSi[3]*mS[3] );
//  s = ( s > 1.E-20 )  ?1./s :0;  
//
//  return sqrt( abs(s*( ( mS[0]*d[0] + mS[1]*d[1] + mS[3]*d[2])*d[0]
//   +(mS[1]*d[0] + mS[2]*d[1] + mS[4]*d[2])*d[1]
//   +(mS[3]*d[0] + mS[4]*d[1] + mS[5]*d[2])*d[2] ))/2);
//}
//
//
//double GetDeviationFromParticle( const KFParticleBase &p ) 
//  const
//{ 
//  //* Calculate sqrt(Chi2/ndf) deviation from other particle
//
//  double dS, dS1;
//  GetDStoParticle( p, dS, dS1 );   
//  double mP1[8], mC1[36];
//  p.Transport( dS1, mP1, mC1 ); 
//
//  double d[3]={ fP[0]-mP1[0], fP[1]-mP1[1], fP[2]-mP1[2]};
//
//  double sigmaS = .1+10.*sqrt( (d[0]*d[0]+d[1]*d[1]+d[2]*d[2])/
//(mP1[3]*mP1[3]+mP1[4]*mP1[4]+mP1[5]*mP1[5])  );
//
//  double h[3] = { mP1[3]*sigmaS, mP1[4]*sigmaS, mP1[5]*sigmaS };       
//  
//  mC1[0] +=h[0]*h[0];
//  mC1[1] +=h[1]*h[0]; 
//  mC1[2] +=h[1]*h[1]; 
//  mC1[3] +=h[2]*h[0]; 
//  mC1[4] +=h[2]*h[1];
//  mC1[5] +=h[2]*h[2];
//
//  return GetDeviationFromVertex( mP1, mC1 )*sqrt(2./1.);
//}
//
//
//
//void SubtractFromVertex(  KFParticleBase &Vtx ) const
//{
//  //* Subtract the particle from the vertex  
//
//  double fld[3];  
//  {
//    GetFieldValue( Vtx.fP, fld );
//    const double kCLight =  0.000299792458;
//    fld[0]*=kCLight; fld[1]*=kCLight; fld[2]*=kCLight;
//  }
//
//  double m[8];
//  double mCm[36];
//
//  if( Vtx.fIsLinearized ){
//    GetMeasurement( Vtx.fVtxGuess, m, mCm );
//  } else {
//    GetMeasurement( Vtx.fP, m, mCm );
//  }
//
//  double mV[6];
//
//  mV[ 0] = mCm[ 0];
//  mV[ 1] = mCm[ 1];
//  mV[ 2] = mCm[ 2];
//  mV[ 3] = mCm[ 3];
//  mV[ 4] = mCm[ 4];
//  mV[ 5] = mCm[ 5];
//     
//  //* 
//    
//  double mS[6];
//  {
//    double mSi[6] = { mV[0]-Vtx.fC[0], 
//mV[1]-Vtx.fC[1], mV[2]-Vtx.fC[2], 
//mV[3]-Vtx.fC[3], mV[4]-Vtx.fC[4], mV[5]-Vtx.fC[5] };
//    
//    mS[0] = mSi[2]*mSi[5] - mSi[4]*mSi[4];
//    mS[1] = mSi[3]*mSi[4] - mSi[1]*mSi[5];
//    mS[2] = mSi[0]*mSi[5] - mSi[3]*mSi[3];
//    mS[3] = mSi[1]*mSi[4] - mSi[2]*mSi[3];
//    mS[4] = mSi[1]*mSi[3] - mSi[0]*mSi[4];
//    mS[5] = mSi[0]*mSi[2] - mSi[1]*mSi[1]; 
//    
//    double s = ( mSi[0]*mS[0] + mSi[1]*mS[1] + mSi[3]*mS[3] );
//    s = ( s > 1.E-20 )  ?1./s :0;  
//    mS[0]*=s;
//    mS[1]*=s;
//    mS[2]*=s;
//    mS[3]*=s;
//    mS[4]*=s;
//    mS[5]*=s;
//  }
//    
//  //* Residual (measured - estimated)
//    
//  double zeta[3] = { m[0]-Vtx.fP[0], m[1]-Vtx.fP[1], m[2]-Vtx.fP[2] };
//        
//  //* mCHt = mCH' - D'
//    
//  double mCHt0[3], mCHt1[3], mCHt2[3];
//    
//  mCHt0[0]=Vtx.fC[ 0] ;      mCHt1[0]=Vtx.fC[ 1] ;      mCHt2[0]=Vtx.fC[ 3] ;
//  mCHt0[1]=Vtx.fC[ 1] ;      mCHt1[1]=Vtx.fC[ 2] ;      mCHt2[1]=Vtx.fC[ 4] ;
//  mCHt0[2]=Vtx.fC[ 3] ;      mCHt1[2]=Vtx.fC[ 4] ;      mCHt2[2]=Vtx.fC[ 5] ;
//  
//  //* Kalman gain K = mCH'*S
//    
//  double k0[3], k1[3], k2[3];
//    
//  for(int i=0;i<3;++i){
//    k0[i] = mCHt0[i]*mS[0] + mCHt1[i]*mS[1] + mCHt2[i]*mS[3];
//    k1[i] = mCHt0[i]*mS[1] + mCHt1[i]*mS[2] + mCHt2[i]*mS[4];
//    k2[i] = mCHt0[i]*mS[3] + mCHt1[i]*mS[4] + mCHt2[i]*mS[5];
//  }
//    
//  //* New estimation of the vertex position r += K*zeta
//    
//  double dChi2 = -(mS[0]*zeta[0] + mS[1]*zeta[1] + mS[3]*zeta[2])*zeta[0]
//    +      (mS[1]*zeta[0] + mS[2]*zeta[1] + mS[4]*zeta[2])*zeta[1]
//    +      (mS[3]*zeta[0] + mS[4]*zeta[1] + mS[5]*zeta[2])*zeta[2];
//
//  if( Vtx.fChi2 - dChi2 < 0 ) return;
//
//  for(int i=0;i<3;++i) 
//    Vtx.fP[i] -= k0[i]*zeta[0] + k1[i]*zeta[1] + k2[i]*zeta[2];       
//    
//  //* New covariance matrix C -= K*(mCH')'
//    
//  for(int i=0, k=0;i<3;++i){
//    for(int j=0;j<=i;++j,++k) 
//      Vtx.fC[k] += k0[i]*mCHt0[j] + k1[i]*mCHt1[j] + k2[i]*mCHt2[j];
//  }
//    
//  //* Calculate Chi^2 
//
//  Vtx.fNDF  -= 2;
//  Vtx.fChi2 -= dChi2;
//}
//
//void TransportLine( double dS, 
//       double P[], double C[] ) const 
//{
//  //* Transport the particle as a straight line
//
//  P[0] = fP[0] + dS*fP[3];
//  P[1] = fP[1] + dS*fP[4];
//  P[2] = fP[2] + dS*fP[5];
//  P[3] = fP[3];
//  P[4] = fP[4];
//  P[5] = fP[5];
//  P[6] = fP[6];
//  P[7] = fP[7];
// 
//  double c6  = fC[ 6] + dS*fC[ 9];
//  double c11 = fC[11] + dS*fC[14];
//  double c17 = fC[17] + dS*fC[20];
//  double sc13 = dS*fC[13];
//  double sc18 = dS*fC[18];
//  double sc19 = dS*fC[19];
//
//  C[ 0] = fC[ 0] + dS*( fC[ 6] + c6  );
//  C[ 2] = fC[ 2] + dS*( fC[11] + c11 );
//  C[ 5] = fC[ 5] + dS*( fC[17] + c17 );
//
//  C[ 7] = fC[ 7] + sc13;
//  C[ 8] = fC[ 8] + sc18;
//  C[ 9] = fC[ 9];
//
//  C[12] = fC[12] + sc19;
//
//  C[ 1] = fC[ 1] + dS*( fC[10] + C[ 7] );
//  C[ 3] = fC[ 3] + dS*( fC[15] + C[ 8] );
//  C[ 4] = fC[ 4] + dS*( fC[16] + C[12] ); 
//  C[ 6] = c6;
//
//  C[10] = fC[10] + sc13;
//  C[11] = c11;
//
//  C[13] = fC[13];
//  C[14] = fC[14];
//  C[15] = fC[15] + sc18;
//  C[16] = fC[16] + sc19;
//  C[17] = c17;
//  
//  C[18] = fC[18];
//  C[19] = fC[19];
//  C[20] = fC[20];
//  C[21] = fC[21] + dS*fC[24];
//  C[22] = fC[22] + dS*fC[25];
//  C[23] = fC[23] + dS*fC[26];
//
//  C[24] = fC[24];
//  C[25] = fC[25];
//  C[26] = fC[26];
//  C[27] = fC[27];
//  C[28] = fC[28] + dS*fC[31];
//  C[29] = fC[29] + dS*fC[32];
//  C[30] = fC[30] + dS*fC[33];
//
//  C[31] = fC[31];
//  C[32] = fC[32];
//  C[33] = fC[33];
//  C[34] = fC[34];
//  C[35] = fC[35]; 
//}
//
//void ConstructGammaBz( const KFParticleBase &daughter1,
//  const KFParticleBase &daughter2, double Bz  )
//{ 
//  //* Create gamma
//  
//  const KFParticleBase *daughters[2] = { &daughter1, &daughter2};
//
//  double v0[3];
//  
//  if( !fIsLinearized ){
//    double ds, ds1;
//    double m[8];
//    double mCd[36];       
//    daughter1.GetDStoParticle(daughter2, ds, ds1);      
//    daughter1.Transport( ds, m, mCd );
//    fP[0] = m[0];
//    fP[1] = m[1];
//    fP[2] = m[2];
//    daughter2.Transport( ds1, m, mCd );
//    fP[0] = .5*( fP[0] + m[0] );
//    fP[1] = .5*( fP[1] + m[1] );
//    fP[2] = .5*( fP[2] + m[2] );
//  } else {
//    fP[0] = fVtxGuess[0];
//    fP[1] = fVtxGuess[1];
//    fP[2] = fVtxGuess[2];
//  }
//
//  double daughterP[2][8], daughterC[2][36];
//  double vtxMom[2][3];
//
//  int nIter = fIsLinearized ?1 :2;
//
//  for( int iter=0; iter<nIter; iter++){
//
//    v0[0] = fP[0];
//    v0[1] = fP[1];
//    v0[2] = fP[2];
//    
//    fAtProductionVertex = 0;
//    fSFromDecay = 0;
//    fP[0] = v0[0];
//    fP[1] = v0[1];
//    fP[2] = v0[2];
//    fP[3] = 0;
//    fP[4] = 0;
//    fP[5] = 0;
//    fP[6] = 0;
//    fP[7] = 0;
//
//  
//    // fit daughters to the vertex guess  
//    
//    {  
//      for( int id=0; id<2; id++ ){
//
//double[] p = daughterP[id];
//double[] mC = daughterC[id];
//
//daughters[id].GetMeasurement( v0, p, mC );
//
//double mAi[6];
//InvertSym3(mC, mAi );
//
//double mB[3][3];
//
//mB[0][0] = mC[ 6]*mAi[0] + mC[ 7]*mAi[1] + mC[ 8]*mAi[3];
//mB[0][1] = mC[ 6]*mAi[1] + mC[ 7]*mAi[2] + mC[ 8]*mAi[4];
//mB[0][2] = mC[ 6]*mAi[3] + mC[ 7]*mAi[4] + mC[ 8]*mAi[5];
//
//mB[1][0] = mC[10]*mAi[0] + mC[11]*mAi[1] + mC[12]*mAi[3];
//mB[1][1] = mC[10]*mAi[1] + mC[11]*mAi[2] + mC[12]*mAi[4];
//mB[1][2] = mC[10]*mAi[3] + mC[11]*mAi[4] + mC[12]*mAi[5];
//
//mB[2][0] = mC[15]*mAi[0] + mC[16]*mAi[1] + mC[17]*mAi[3];
//mB[2][1] = mC[15]*mAi[1] + mC[16]*mAi[2] + mC[17]*mAi[4];
//mB[2][2] = mC[15]*mAi[3] + mC[16]*mAi[4] + mC[17]*mAi[5];
//
//double z[3] = { v0[0]-p[0], v0[1]-p[1], v0[2]-p[2] };
//
//vtxMom[id][0] = p[3] + mB[0][0]*z[0] + mB[0][1]*z[1] + mB[0][2]*z[2];
//vtxMom[id][1] = p[4] + mB[1][0]*z[0] + mB[1][1]*z[1] + mB[1][2]*z[2];
//vtxMom[id][2] = p[5] + mB[2][0]*z[0] + mB[2][1]*z[1] + mB[2][2]*z[2];
//
//daughters[id].Transport( daughters[id].GetDStoPoint(v0), p, mC );
//      
//      }      
//      
//    } // fit daughters to guess
//
//    
//    // fit new vertex
//    {
//      
//      double mpx0 =  vtxMom[0][0]+vtxMom[1][0];
//      double mpy0 =  vtxMom[0][1]+vtxMom[1][1];
//      double mpt0 = sqrt(mpx0*mpx0 + mpy0*mpy0);
//      // double a0 = atan2(mpy0,mpx0);
//      
//      double ca0 = mpx0/mpt0;
//      double sa0 = mpy0/mpt0;
//      double r[3] = { v0[0], v0[1], v0[2] };
//      double mC[3][3] = {{1000., 0 ,   0  },
// {0,  1000.,   0  },
// {0,     0, 1000. } };
//      double chi2=0;
//      
//      for( int id=0; id<2; id++ ){
//const double kCLight = 0.000299792458;
//double q = Bz*daughters[id].GetQ()*kCLight;
//double px0 = vtxMom[id][0];
//double py0 = vtxMom[id][1];
//double pz0 = vtxMom[id][2];
//double pt0 = sqrt(px0*px0+py0*py0);
//double mG[3][6], mB[3], mH[3][3];
//// r = {vx,vy,vz};
//// m = {x,y,z,Px,Py,Pz};
//// V = daughter.C
//// G*m + B = H*r;
//// q*x + Py - q*vx - sin(a)*Pt = 0
//// q*y - Px - q*vy + cos(a)*Pt = 0
//// (Px*cos(a) + Py*sin(a) ) (vz -z) - Pz( cos(a)*(vx-x) + sin(a)*(vy-y)) = 0
//
//mG[0][0] = q;
//mG[0][1] = 0;
//mG[0][2] = 0;
//mG[0][3] =   -sa0*px0/pt0;
//mG[0][4] = 1 -sa0*py0/pt0;
//mG[0][5] = 0;
//mH[0][0] = q;
//mH[0][1] = 0;
//mH[0][2] = 0;      
//mB[0] = py0 - sa0*pt0 - mG[0][3]*px0 - mG[0][4]*py0 ;
//
//// q*y - Px - q*vy + cos(a)*Pt = 0
//
//mG[1][0] = 0;
//mG[1][1] = q;
//mG[1][2] = 0;
//mG[1][3] = -1 + ca0*px0/pt0;
//mG[1][4] =    + ca0*py0/pt0;
//mG[1][5] = 0;      
//mH[1][0] = 0;
//mH[1][1] = q;
//mH[1][2] = 0;      
//mB[1] = -px0 + ca0*pt0 - mG[1][3]*px0 - mG[1][4]*py0 ;
//
//// (Px*cos(a) + Py*sin(a) ) (z -vz) - Pz( cos(a)*(x-vx) + sin(a)*(y-vy)) = 0
//      
//mG[2][0] = -pz0*ca0;
//mG[2][1] = -pz0*sa0;
//mG[2][2] =  px0*ca0 + py0*sa0;
//mG[2][3] = 0;
//mG[2][4] = 0;
//mG[2][5] = 0;
//
//mH[2][0] = mG[2][0];
//mH[2][1] = mG[2][1];
//mH[2][2] = mG[2][2];
//
//mB[2] = 0;
//
//// fit the vertex
//
//// V = GVGt
//
//double mGV[3][6];
//double mV[6];
//double m[3];
//for( int i=0; i<3; i++ ){
//  m[i] = mB[i];
//  for( int k=0; k<6; k++ ) m[i]+=mG[i][k]*daughterP[id][k];
//}
//for( int i=0; i<3; i++ ){
//  for( int j=0; j<6; j++ ){
//    mGV[i][j] = 0;
//    for( int k=0; k<6; k++ ) mGV[i][j]+=mG[i][k]*daughterC[id][ IJ(k,j) ];
//  }
//}
//for( int i=0, k=0; i<3; i++ ){
//  for( int j=0; j<=i; j++,k++ ){
//    mV[k] = 0;
//    for( int l=0; l<6; l++ ) mV[k]+=mGV[i][l]*mG[j][l];
//  }
//}
//
//      
////* CHt
//
//double mCHt[3][3];
//double mHCHt[6];
//double mHr[3];
//for( int i=0; i<3; i++ ){  
//  mHr[i] = 0;
//  for( int k=0; k<3; k++ ) mHr[i]+= mH[i][k]*r[k];
//}
//      
//for( int i=0; i<3; i++ ){
//  for( int j=0; j<3; j++){
//    mCHt[i][j] = 0;
//    for( int k=0; k<3; k++ ) mCHt[i][j]+= mC[i][k]*mH[j][k];
//  }
//}
//
//for( int i=0, k=0; i<3; i++ ){
//  for( int j=0; j<=i; j++, k++ ){
//    mHCHt[k] = 0;
//    for( int l=0; l<3; l++ ) mHCHt[k]+= mH[i][l]*mCHt[l][j];
//  }
//}
//      
//double mS[6] = { mHCHt[0]+mV[0], 
//   mHCHt[1]+mV[1], mHCHt[2]+mV[2], 
//   mHCHt[3]+mV[3], mHCHt[4]+mV[4], mHCHt[5]+mV[5]    };
//      
//
//InvertSym3(mS,mS);
//
////* Residual (measured - estimated)
//    
//double zeta[3] = { m[0]-mHr[0], m[1]-mHr[1], m[2]-mHr[2] };
//            
////* Kalman gain K = mCH'*S
//    
//double k[3][3];
//      
//for(int i=0;i<3;++i){
//  k[i][0] = mCHt[i][0]*mS[0] + mCHt[i][1]*mS[1] + mCHt[i][2]*mS[3];
//  k[i][1] = mCHt[i][0]*mS[1] + mCHt[i][1]*mS[2] + mCHt[i][2]*mS[4];
//  k[i][2] = mCHt[i][0]*mS[3] + mCHt[i][1]*mS[4] + mCHt[i][2]*mS[5];
//}
//
////* New estimation of the vertex position r += K*zeta
//    
//for(int i=0;i<3;++i) 
//  r[i] = r[i] + k[i][0]*zeta[0] + k[i][1]*zeta[1] + k[i][2]*zeta[2];
//      
////* New covariance matrix C -= K*(mCH')'
//
//for(int i=0;i<3;++i){
//  for(int j=0;j<=i;++j){
//    mC[i][j] = mC[i][j] - (k[i][0]*mCHt[j][0] + k[i][1]*mCHt[j][1] + k[i][2]*mCHt[j][2]);
//    mC[j][i] = mC[i][j];
//  }
//}
//
////* Calculate Chi^2 
//
//chi2 += ( ( mS[0]*zeta[0] + mS[1]*zeta[1] + mS[3]*zeta[2] )*zeta[0]
//  +(mS[1]*zeta[0] + mS[2]*zeta[1] + mS[4]*zeta[2] )*zeta[1]
//  +(mS[3]*zeta[0] + mS[4]*zeta[1] + mS[5]*zeta[2] )*zeta[2]  );  
//      }
//    
//      // store vertex
//    
//      fNDF  = 2;
//      fChi2 = chi2;
//      for( int i=0; i<3; i++ ) fP[i] = r[i];
//      for( int i=0,k=0; i<3; i++ ){
//for( int j=0; j<=i; j++,k++ ){
//  fC[k] = mC[i][j];
//}
//      }
//    }
//
//  } // iterations
//
//  // now fit daughters to the vertex
//  
//  fQ     =  0;
//  fSFromDecay = 0;    
//
//  for(int i=3;i<8;++i) fP[i]=0.;
//  for(int i=6;i<35;++i) fC[i]=0.;
//  fC[35] = 100.;
//
//  for( int id=0; id<2; id++ ){
//
//    double[] p = daughterP[id];
//    double[] mC = daughterC[id];      
//    daughters[id].GetMeasurement( v0, p, mC );
//
//    const double[] m = fP, *mV = fC;
//    
//    double mAi[6];
//    InvertSym3(mC, mAi );
//
//    double mB[4][3];
//
//    mB[0][0] = mC[ 6]*mAi[0] + mC[ 7]*mAi[1] + mC[ 8]*mAi[3];
//    mB[0][1] = mC[ 6]*mAi[1] + mC[ 7]*mAi[2] + mC[ 8]*mAi[4];
//    mB[0][2] = mC[ 6]*mAi[3] + mC[ 7]*mAi[4] + mC[ 8]*mAi[5];
//    
//    mB[1][0] = mC[10]*mAi[0] + mC[11]*mAi[1] + mC[12]*mAi[3];
//    mB[1][1] = mC[10]*mAi[1] + mC[11]*mAi[2] + mC[12]*mAi[4];
//    mB[1][2] = mC[10]*mAi[3] + mC[11]*mAi[4] + mC[12]*mAi[5];
//    
//    mB[2][0] = mC[15]*mAi[0] + mC[16]*mAi[1] + mC[17]*mAi[3];
//    mB[2][1] = mC[15]*mAi[1] + mC[16]*mAi[2] + mC[17]*mAi[4];
//    mB[2][2] = mC[15]*mAi[3] + mC[16]*mAi[4] + mC[17]*mAi[5];
//    
//    mB[3][0] = mC[21]*mAi[0] + mC[22]*mAi[1] + mC[23]*mAi[3];
//    mB[3][1] = mC[21]*mAi[1] + mC[22]*mAi[2] + mC[23]*mAi[4];
//    mB[3][2] = mC[21]*mAi[3] + mC[22]*mAi[4] + mC[23]*mAi[5];    
//
//
//    double z[3] = { m[0]-p[0], m[1]-p[1], m[2]-p[2] };
//
////     {
////       double mAV[6] = { mC[0]-mV[0], mC[1]-mV[1], mC[2]-mV[2], 
////   mC[3]-mV[3], mC[4]-mV[4], mC[5]-mV[5] };
////       
////       double mAVi[6];
////       if( !InvertSym3(mAV, mAVi) ){
//// double dChi2 = ( +(mAVi[0]*z[0] + mAVi[1]*z[1] + mAVi[3]*z[2])*z[0]
////    +(mAVi[1]*z[0] + mAVi[2]*z[1] + mAVi[4]*z[2])*z[1]
////    +(mAVi[3]*z[0] + mAVi[4]*z[1] + mAVi[5]*z[2])*z[2] );
//// fChi2+= abs( dChi2 );
////       }
////       fNDF  += 2;
////     }
//
//    //* Add the daughter momentum to the particle momentum
// 
//    fP[3]+= p[3] + mB[0][0]*z[0] + mB[0][1]*z[1] + mB[0][2]*z[2];
//    fP[4]+= p[4] + mB[1][0]*z[0] + mB[1][1]*z[1] + mB[1][2]*z[2];
//    fP[5]+= p[5] + mB[2][0]*z[0] + mB[2][1]*z[1] + mB[2][2]*z[2];
//    fP[6]+= p[6] + mB[3][0]*z[0] + mB[3][1]*z[1] + mB[3][2]*z[2];
//  
//    double d0, d1, d2;
//   
//    d0= mB[0][0]*mV[0] + mB[0][1]*mV[1] + mB[0][2]*mV[3] - mC[ 6];
//    d1= mB[0][0]*mV[1] + mB[0][1]*mV[2] + mB[0][2]*mV[4] - mC[ 7];
//    d2= mB[0][0]*mV[3] + mB[0][1]*mV[4] + mB[0][2]*mV[5] - mC[ 8];
//
//    //fC[6]+= mC[ 6] + d0;
//    //fC[7]+= mC[ 7] + d1;
//    //fC[8]+= mC[ 8] + d2;
//    fC[9]+= mC[ 9] + d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//
//    d0= mB[1][0]*mV[0] + mB[1][1]*mV[1] + mB[1][2]*mV[3] - mC[10];
//    d1= mB[1][0]*mV[1] + mB[1][1]*mV[2] + mB[1][2]*mV[4] - mC[11];
//    d2= mB[1][0]*mV[3] + mB[1][1]*mV[4] + mB[1][2]*mV[5] - mC[12];
//
//    //fC[10]+= mC[10]+ d0;
//    //fC[11]+= mC[11]+ d1;
//    //fC[12]+= mC[12]+ d2;
//    fC[13]+= mC[13]+ d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//    fC[14]+= mC[14]+ d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//
//    d0= mB[2][0]*mV[0] + mB[2][1]*mV[1] + mB[2][2]*mV[3] - mC[15];
//    d1= mB[2][0]*mV[1] + mB[2][1]*mV[2] + mB[2][2]*mV[4] - mC[16];
//    d2= mB[2][0]*mV[3] + mB[2][1]*mV[4] + mB[2][2]*mV[5] - mC[17];
//
//    //fC[15]+= mC[15]+ d0;
//    //fC[16]+= mC[16]+ d1;
//    //fC[17]+= mC[17]+ d2;
//    fC[18]+= mC[18]+ d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//    fC[19]+= mC[19]+ d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//    fC[20]+= mC[20]+ d0*mB[2][0] + d1*mB[2][1] + d2*mB[2][2];
//
//    d0= mB[3][0]*mV[0] + mB[3][1]*mV[1] + mB[3][2]*mV[3] - mC[21];
//    d1= mB[3][0]*mV[1] + mB[3][1]*mV[2] + mB[3][2]*mV[4] - mC[22];
//    d2= mB[3][0]*mV[3] + mB[3][1]*mV[4] + mB[3][2]*mV[5] - mC[23];
//
//    //fC[21]+= mC[21] + d0;
//    //fC[22]+= mC[22] + d1;
//    //fC[23]+= mC[23] + d2;
//    fC[24]+= mC[24] + d0*mB[0][0] + d1*mB[0][1] + d2*mB[0][2];
//    fC[25]+= mC[25] + d0*mB[1][0] + d1*mB[1][1] + d2*mB[1][2];
//    fC[26]+= mC[26] + d0*mB[2][0] + d1*mB[2][1] + d2*mB[2][2];
//    fC[27]+= mC[27] + d0*mB[3][0] + d1*mB[3][1] + d2*mB[3][2];
//  }
//
////  SetMassConstraint(0,0);
//  SetNonlinearMassConstraint(0);
//}
//
//void GetArmenterosPodolanski(KFParticleBase& positive, KFParticleBase& negative, double QtAlfa[2] )
//{
//// example:
////       KFParticle PosParticle(...)
////       KFParticle NegParticle(...)
////       Gamma.ConstructGamma(PosParticle, NegParticle);
////       double VertexGamma[3] = {Gamma.GetX(), Gamma.GetY(), Gamma.GetZ()};
////       PosParticle.TransportToPoint(VertexGamma);
////       NegParticle.TransportToPoint(VertexGamma);
////       double armenterosQtAlfa[2] = {0.};
////       GetArmenterosPodolanski(PosParticle, NegParticle, armenterosQtAlfa );
//
//  double alpha = 0., qt = 0.;
//  double spx = positive.GetPx() + negative.GetPx();
//  double spy = positive.GetPy() + negative.GetPy();
//  double spz = positive.GetPz() + negative.GetPz();
//  double sp  = sqrt(spx*spx + spy*spy + spz*spz);
//  if( sp == 0.0) return;
//  double pn, pp, pln, plp;
//
//  pn = sqrt(negative.GetPx()*negative.GetPx() + negative.GetPy()*negative.GetPy() + negative.GetPz()*negative.GetPz());
//  pp = sqrt(positive.GetPx()*positive.GetPx() + positive.GetPy()*positive.GetPy() + positive.GetPz()*positive.GetPz());
//  pln  = (negative.GetPx()*spx+negative.GetPy()*spy+negative.GetPz()*spz)/sp;
//  plp  = (positive.GetPx()*spx+positive.GetPy()*spy+positive.GetPz()*spz)/sp;
//
//  if( pn == 0.0) return;
//  double ptm  = (1.-((pln/pn)*(pln/pn)));
//  qt= (ptm>=0.)?  pn*sqrt(ptm) :0;
//  alpha = (plp-pln)/(plp+pln);
//
//  QtAlfa[0] = qt;
//  QtAlfa[1] = alpha;
//}
//
//void RotateXY(double angle, double Vtx[3])
//{
//  // Rotates the KFParticle object around OZ axis, OZ axis is set by the vertex position
//  // double angle - angle of rotation in XY plane in [rad]
//  // double Vtx[3] - position of the vertex in [cm]
//
//  // Before rotation the center of the coordinat system should be moved to the vertex position; move back after rotation
//  X() = X() - Vtx[0];
//  Y() = Y() - Vtx[1];
//  Z() = Z() - Vtx[2];
//
//  // Rotate the kf particle
//  double c = cos(angle);
//  double s = sin(angle);
//
//  double mA[8][ 8];
//  for( int i=0; i<8; i++ ){
//    for( int j=0; j<8; j++){
//      mA[i][j] = 0;
//    }
//  }
//  for( int i=0; i<8; i++ ){
//    mA[i][i] = 1;
//  }
//  mA[0][0] =  c;  mA[0][1] = s;
//  mA[1][0] = -s;  mA[1][1] = c;
//  mA[3][3] =  c;  mA[3][4] = s;
//  mA[4][3] = -s;  mA[4][4] = c;
//
//  double mAC[8][8];
//  double mAp[8];
//
//  for( int i=0; i<8; i++ ){
//    mAp[i] = 0;
//    for( int k=0; k<8; k++){
//      mAp[i]+=mA[i][k] * fP[k];
//    }
//  }
//
//  for( int i=0; i<8; i++){
//    fP[i] = mAp[i];
//  }
//
//  for( int i=0; i<8; i++ ){
//    for( int j=0; j<8; j++ ){
//      mAC[i][j] = 0;
//      for( int k=0; k<8; k++ ){
//        mAC[i][j]+= mA[i][k] * GetCovariance(k,j);
//      }
//    }
//  }
//
//  for( int i=0; i<8; i++ ){
//    for( int j=0; j<=i; j++ ){
//      double xx = 0;
//      for( int k=0; k<8; k++){
//        xx+= mAC[i][k]*mA[j][k];
//      }
//      Covariance(i,j) = xx;
//    }
//  }
//
//  X() = GetX() + Vtx[0];
//  Y() = GetY() + Vtx[1];
//  Z() = GetZ() + Vtx[2];
//}
//
//bool InvertSym3( const double A[], double Ai[] )
//{
//  //* Invert symmetric matric stored in low-triagonal form 
//
//  bool ret = 0;
//  double a0 = A[0], a1 = A[1], a2 = A[2], a3 = A[3];
//
//  Ai[0] = a2*A[5] - A[4]*A[4];
//  Ai[1] = a3*A[4] - a1*A[5];
//  Ai[3] = a1*A[4] - a2*a3;
//  double det = (a0*Ai[0] + a1*Ai[1] + a3*Ai[3]);
//  if( fabs(det)>1.e-20 ) det = 1./det;    
//  else{ 
//    det = 0;
//    ret = 1;
//  }
//  Ai[0] *= det;
//  Ai[1] *= det;
//  Ai[3] *= det;
//  Ai[2] = ( a0*A[5] - a3*a3 )*det;
//  Ai[4] = ( a1*a3 - a0*A[4] )*det;
//  Ai[5] = ( a0*a2 - a1*a1 )*det;
//  return ret;
//}
//
//void MultQSQt( const double Q[], const double S[], double SOut[] )
//{
//  //* Matrix multiplication Q*S*Q^T, Q - square matrix, S - symmetric
//
//  const int kN= 8;
//  double mA[kN*kN];
//  
//  for( int i=0, ij=0; i<kN; i++ ){
//    for( int j=0; j<kN; j++, ++ij ){
//      mA[ij] = 0 ;
//      for( int k=0; k<kN; ++k ) mA[ij]+= S[( k<=i ) ? i*(i+1)/2+k :k*(k+1)/2+i] * Q[ j*kN+k];
//    }
//  }
//    
//  for( int i=0; i<kN; i++ ){
//    for( int j=0; j<=i; j++ ){
//      int ij = ( j<=i ) ? i*(i+1)/2+j :j*(j+1)/2+i;
//      SOut[ij] = 0 ;
//      for( int k=0; k<kN; k++ )  SOut[ij] += Q[ i*kN+k ] * mA[ k*kN+j ];
//    }
//  }
//}
//
//
//// 72-charachters line to define the printer border
////3456789012345678901234567890123456789012345678901234567890123456789012
//
//
//
////---------------------------------------------------------------------------------
//// The KFParticle class
//// .
//// @author  S.Gorbunov, I.Kisel
//// @version 1.0
//// @since   13.05.07
//// 
//// Class to reconstruct and store the decayed particle parameters.
//// The method is described in CBM-SOFT note 2007-003, 
//// ``Reconstruction of decayed particles based on the Kalman filter'', 
//// http://www.gsi.de/documents/DOC-2007-May-14-1.pdf
////
//// This class is ALICE interface to general mathematics in KFParticleBase
//// 
////  -= Copyright &copy ALICE HLT and CBM L1 Groups =-
////_________________________________________________________________________________
//
//#define NonhomogeneousField
////#define HomogeneousField
//
//#ifndef KFPARTICLE_H
//#define KFPARTICLE_H
//
//#include "KFParticleBase.h"
//
//#ifdef HomogeneousField
//class AliVTrack;
//class AliVVertex;
//#endif
//
//#ifdef NonhomogeneousField
//#include "CbmKFTrack.h"
////#include "CbmKF.h"
////class CbmKFTrackInterface;
////class CbmKFVertexInterface;
//#endif
//
//#include "MagneticFieldManager.h"
//
//class KFParticle :public KFParticleBase
//{
//  
// public:
//
//  //*
//  //*  INITIALIZATION
//  //*
//
//  //* Set magnetic field for all particles
//#ifdef HomogeneousField
//  static void SetField( double Bz );
//#endif
//  //* Constructor (empty)
//
//  KFParticle():KFParticleBase(){ ; }
//
//  //* Destructor (empty)
//
//  ~KFParticle(){ ; }
//
//  //* Construction of mother particle by its 2-3-4 daughters
//
//  KFParticle( const KFParticle &d1, const KFParticle &d2, bool gamma = false );
//
//  KFParticle( const KFParticle &d1, const KFParticle &d2, 
// const KFParticle &d3 );
//
//  KFParticle( const KFParticle &d1, const KFParticle &d2, 
// const KFParticle &d3, const KFParticle &d4 );
// 
// //* Initialisation from "cartesian" coordinates ( X Y Z Px Py Pz )
// //* Parameters, covariance matrix, charge and PID hypothesis should be provided 
//
//  void Create( const double Param[], const double Cov[], int Charge, double mass /*int PID*/ );
//
//#ifdef HomogeneousField
// //* Initialisation from ALICE track, PID hypothesis shoould be provided 
//
//  KFParticle( const AliVTrack &track, int PID );
//
//  //* Initialisation from VVertex 
//
//  KFParticle( const AliVVertex &vertex );
//#endif
//
//#ifdef NonhomogeneousField
//  KFParticle( CbmKFTrack* Track, double[] z0=0 );
//  //KFParticle( CbmKFVertexInterface &vertex );
//#endif
//
//  void CleanDaughtersId() { fDaughtersIds.clear(); }
//  void SetNDaughters( int n ) { fDaughtersIds.reserve(n); }
//  void AddDaughter( int id ){ fDaughtersIds.push_back(id); }
//
//
//  //* Initialise covariance matrix and set current parameters to 0.0 
//
//  void Initialize();
//
//  //* Set decay vertex parameters for linearisation 
//
//  void SetVtxGuess( double x, double y, double z );
//
//  //*
//  //*  ACCESSORS
//  //*
//
//  //* Simple accessors 
//
//  double GetX    () const ; //* x of current position
//  double GetY    () const ; //* y of current position
//  double GetZ    () const ; //* z of current position
//  double GetPx   () const ; //* x-compoment of 3-momentum
//  double GetPy   () const ; //* y-compoment of 3-momentum
//  double GetPz   () const ; //* z-compoment of 3-momentum
//  double GetE    () const ; //* energy
//  double GetS    () const ; //* decay length / momentum
//  int    GetQ    () const ; //* charge
//  double GetChi2 () const ; //* chi^2
//  int    GetNDF  () const ; //* Number of Degrees of Freedom
//
//  bool GetAtProductionVertex() const { return fAtProductionVertex; }
//  void SetAtProductionVertex(bool b) { fAtProductionVertex = b; }
//
//#ifdef NonhomogeneousField
//  float* GetFieldCoeff() { return fieldRegion; }
//  void SetFieldCoeff(float c, int i) { fieldRegion[i] = c; }
//#endif
//
//  const double& X    () const { return fP[0]; }
//  const double& Y    () const { return fP[1]; }
//  const double& Z    () const { return fP[2]; }
//  const double& Px   () const { return fP[3]; }
//  const double& Py   () const { return fP[4]; }
//  const double& Pz   () const { return fP[5]; }
//  const double& E    () const { return fP[6]; }
//  const double& S    () const { return fP[7]; }
//  const int   & Q    () const { return fQ;    }
//  const double& Chi2 () const { return fChi2; }
//  const int   & NDF  () const { return fNDF;  }
//  
//  double GetParameter ( int i ) const ;
//  double GetCovariance( int i ) const ;
//  double GetCovariance( int i, int j ) const ;
//
//  //* Accessors with calculations, value returned w/o error flag
//  
//  double GetP           () const; //* momentum
//  double GetPt          () const; //* transverse momentum
//  double GetEta         () const; //* pseudorapidity
//  double GetPhi         () const; //* phi
//  double GetMomentum    () const; //* momentum (same as GetP() )
//  double GetMass        () const; //* mass
//  double GetDecayLength () const; //* decay length
//  double GetDecayLengthXY () const; //* decay length in XY
//  double GetLifeTime    () const; //* life time
//  double GetR           () const; //* distance to the origin
//
//  //* Accessors to estimated errors
//
//  double GetErrX           () const ; //* x of current position 
//  double GetErrY           () const ; //* y of current position
//  double GetErrZ           () const ; //* z of current position
//  double GetErrPx          () const ; //* x-compoment of 3-momentum
//  double GetErrPy          () const ; //* y-compoment of 3-momentum
//  double GetErrPz          () const ; //* z-compoment of 3-momentum
//  double GetErrE           () const ; //* energy
//  double GetErrS           () const ; //* decay length / momentum
//  double GetErrP           () const ; //* momentum
//  double GetErrPt          () const ; //* transverse momentum
//  double GetErrEta         () const ; //* pseudorapidity
//  double GetErrPhi         () const ; //* phi
//  double GetErrMomentum    () const ; //* momentum
//  double GetErrMass        () const ; //* mass
//  double GetErrDecayLength () const ; //* decay length
//  double GetErrDecayLengthXY () const ; //* decay length in XY
//  double GetErrLifeTime    () const ; //* life time
//  double GetErrR           () const ; //* distance to the origin
//
//  //* Accessors with calculations( &value, &estimated sigma )
//  //* error flag returned (0 means no error during calculations) 
//
//  int GetP           ( double &P, double &SigmaP ) const ;   //* momentum
//  int GetPt          ( double &Pt, double &SigmaPt ) const ; //* transverse momentum
//  int GetEta         ( double &Eta, double &SigmaEta ) const ; //* pseudorapidity
//  int GetPhi         ( double &Phi, double &SigmaPhi ) const ; //* phi
//  int GetMomentum    ( double &P, double &SigmaP ) const ;   //* momentum
//  int GetMass        ( double &M, double &SigmaM ) const ;   //* mass
//  int GetDecayLength ( double &L, double &SigmaL ) const ;   //* decay length
//  int GetDecayLengthXY ( double &L, double &SigmaL ) const ;   //* decay length in XY
//  int GetLifeTime    ( double &T, double &SigmaT ) const ;   //* life time
//  int GetR           ( double &R, double &SigmaR ) const ; //* R
//  double GetRapidity() const { return 0.5*log((fP[6] + fP[5])/(fP[6] - fP[5])); }
//  double GetTheta()    const { return atan2(GetPt(),fP[5]); }
//
//
//  //*
//  //*  MODIFIERS
//  //*
//  
//  double & X    () ;
//  double & Y    () ;
//  double & Z    () ;
//  double & Px   () ;
//  double & Py   () ;
//  double & Pz   () ;
//  double & E    () ;
//  double & S    () ;
//  int    & Q    () ;
//  double & Chi2 () ;
//  int    & NDF  () ;
//
//  double & Parameter ( int i ) ;
//  double & Covariance( int i ) ;
//  double & Covariance( int i, int j ) ;
//  double[]  Parameters () ;
//  double[]  CovarianceMatrix() ;
//
//  //* 
//  //* CONSTRUCTION OF THE PARTICLE BY ITS DAUGHTERS AND MOTHER
//  //* USING THE KALMAN FILTER METHOD
//  //*
//
//
//  //* Add daughter to the particle 
//
//  void AddDaughter( const KFParticle &Daughter );
//
//  //* Add daughter via += operator: ex.{ D0; D0+=Pion; D0+= Kaon; }
//
//  void operator +=( const KFParticle &Daughter );  
//
//  //* Set production vertex 
//
//  void SetProductionVertex( const KFParticle &Vtx );
//
//  //* Set mass constraint 
//
//  void SetMassConstraint( double Mass, double SigmaMass = 0  );
//  
//  //* Set no decay length for resonances
//
//  void SetNoDecayLength();
//
//  //* Everything in one go  
//
//  void Construct( const KFParticle *vDaughters[], int nDaughters, 
//  const KFParticle *ProdVtx=0,   double Mass=-1, bool IsConstrained=0  );
//
//  //*
//  //*                   TRANSPORT
//  //* 
//  //*  ( main transportation parameter is S = SignedPath/Momentum )
//  //*  ( parameters of decay & production vertices are stored locally )
//  //*
//
//  //* Transport the particle to its decay vertex 
//
//  void TransportToDecayVertex();
//
//  //* Transport the particle to its production vertex 
//
//  void TransportToProductionVertex();
//
//  //* Transport the particle close to xyz[] point 
//
//  void TransportToPoint( const double xyz[] );
//
//  //* Transport the particle close to VVertex  
//#ifdef HomogeneousField
//  void TransportToVertex( const AliVVertex &v );
//#endif
//  //* Transport the particle close to another particle p 
//
//  void TransportToParticle( const KFParticle &p );
//
//  //* Transport the particle on dS parameter (SignedPath/Momentum) 
//
//  void TransportToDS( double dS );
//
//  //* Get dS to a certain space point 
//
//  double GetDStoPoint( const double xyz[] ) const ;
//  
//  //* Get dS to other particle p (dSp for particle p also returned) 
//
//  void GetDStoParticle( const KFParticle &p, 
//double &DS, double &DSp ) const ;
//  
//  //* Get dS to other particle p in XY-plane
//
//  void GetDStoParticleXY( const KFParticleBase &p, 
//  double &DS, double &DSp ) const ;
//  
//  //* 
//  //* OTHER UTILITIES
//  //*
//
//
//  //* Calculate distance from another object [cm]
//
//  double GetDistanceFromVertex( const double vtx[] ) const ;
//  double GetDistanceFromVertex( const KFParticle &Vtx ) const ;
//#ifdef HomogeneousField
//  double GetDistanceFromVertex( const AliVVertex &Vtx ) const ;
//#endif
//  double GetDistanceFromParticle( const KFParticle &p ) const ;
//
//  //* Calculate sqrt(Chi2/ndf) deviation from another object
//  //* ( v = [xyz]-vertex, Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix )
//
//  double GetDeviationFromVertex( const double v[], const double Cv[]=0 ) const ;
//  double GetDeviationFromVertex( const KFParticle &Vtx ) const ;
//#ifdef HomogeneousField
//  double GetDeviationFromVertex( const AliVVertex &Vtx ) const ;
//#endif
//  double GetDeviationFromParticle( const KFParticle &p ) const ;
// 
//  //* Calculate distance from another object [cm] in XY-plane
//
//  bool GetDistanceFromVertexXY( const double vtx[], double &val, double &err ) const ;
//  bool GetDistanceFromVertexXY( const double vtx[], const double Cv[], double &val, double &err ) const ;
//  bool GetDistanceFromVertexXY( const KFParticle &Vtx, double &val, double &err ) const ;
//#ifdef HomogeneousField
//  bool GetDistanceFromVertexXY( const AliVVertex &Vtx, double &val, double &err ) const ;
//#endif
//
//  double GetDistanceFromVertexXY( const double vtx[] ) const ;
//  double GetDistanceFromVertexXY( const KFParticle &Vtx ) const ;
//#ifdef HomogeneousField
//  double GetDistanceFromVertexXY( const AliVVertex &Vtx ) const ;
//#endif
//  double GetDistanceFromParticleXY( const KFParticle &p ) const ;
//
//  //* Calculate sqrt(Chi2/ndf) deviation from another object in XY plane
//  //* ( v = [xyz]-vertex, Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix )
//
//  double GetDeviationFromVertexXY( const double v[], const double Cv[]=0 ) const ;
//  double GetDeviationFromVertexXY( const KFParticle &Vtx ) const ;
//#ifdef HomogeneousField
//  double GetDeviationFromVertexXY( const AliVVertex &Vtx ) const ;
//#endif
//  double GetDeviationFromParticleXY( const KFParticle &p ) const ;
//
//  //* Calculate opennig angle between two particles
//
//  double GetAngle  ( const KFParticle &p ) const ;
//  double GetAngleXY( const KFParticle &p ) const ;
//  double GetAngleRZ( const KFParticle &p ) const ;
//
//  //* Subtract the particle from the vertex  
//
//  void SubtractFromVertex( KFParticle &v ) const ;
//
//  //* Special method for creating gammas
//
//  void ConstructGamma( const KFParticle &daughter1,
//       const KFParticle &daughter2  );
//
//    // * Pseudo Proper Time of decay = (r*pt) / |pt| * M/|pt|
//    // @primVertex - primary vertex
//    // @mass - mass of the mother particle (in the case of "Hb . JPsi" it would be JPsi mass)
//    // @*timeErr2 - squared error of the decay time. If timeErr2 = 0 it isn't calculated
//  double GetPseudoProperDecayTime( const KFParticle &primVertex, const double& mass, double* timeErr2 = 0 ) const;
//
// protected: 
//  
//  //*
//  //*  INTERNAL STUFF
//  //* 
//
//  //* Method to access ALICE field 
//#ifdef HomogeneousField
//  static double GetFieldAlice();
//#endif
//  //* Other methods required by the abstract KFParticleBase class 
//  
//  void GetFieldValue( const double xyz[], double B[] ) const ;
//  void GetDStoParticle( const KFParticleBase &p, double &DS, double &DSp )const ;
//  void Transport( double dS, double P[], double C[] ) const ;
//  static void GetExternalTrackParam( const KFParticleBase &p, double &X, double &Alpha, double P[5]  ) ;
//
//  //void GetDStoParticleALICE( const KFParticleBase &p, double &DS, double &DS1 ) const;
//
//
// private:
//#ifdef HomogeneousField
//  static double fgBz;  //* Bz compoment of the magnetic field
//#endif
//#ifdef NonhomogeneousField
//  float fieldRegion[10];
//#endif
//
//};
//
//
//
////---------------------------------------------------------------------
////
////     Inline implementation of the KFParticle methods
////
////---------------------------------------------------------------------
//
//#ifdef HomogeneousField
//inline void SetField( double Bz )
//{ 
//  fgBz = Bz;
//}
//#endif
//
//inline KFParticle( const KFParticle &d1, 
//                               const KFParticle &d2, 
//                               const KFParticle &d3 )
//{
//  KFParticle mother;
//  mother+= d1;
//  mother+= d2;
//  mother+= d3;
//  *this = mother;
//}
//
//inline KFParticle( const KFParticle &d1, 
//                               const KFParticle &d2, 
//                               const KFParticle &d3, 
//                               const KFParticle &d4 )
//{
//  KFParticle mother;
//  mother+= d1;
//  mother+= d2;
//  mother+= d3;
//  mother+= d4;
//  *this = mother;
//}
//
//
//inline void Initialize()
//{ 
//  Initialize(); 
//}
//
//inline void SetVtxGuess( double x, double y, double z )
//{
//  SetVtxGuess(x,y,z);
//}  
//
//inline double GetX    () const 
//{ 
//  return GetX();    
//}
//
//inline double GetY    () const 
//{ 
//  return GetY();    
//}
//
//inline double GetZ    () const 
//{ 
//  return GetZ();    
//}
//
//inline double GetPx   () const 
//{ 
//  return GetPx();   
//}
//
//inline double GetPy   () const 
//{ 
//  return GetPy();   
//}
//
//inline double GetPz   () const 
//{ 
//  return GetPz();   
//}
//
//inline double GetE    () const 
//{ 
//  return GetE();    
//}
//
//inline double GetS    () const 
//{ 
//  return GetS();    
//}
//
//inline int    GetQ    () const 
//{ 
//  return GetQ();    
//}
//
//inline double GetChi2 () const 
//{ 
//  return GetChi2(); 
//}
//
//inline int    GetNDF  () const 
//{ 
//  return GetNDF();  
//}
//
//inline double GetParameter ( int i ) const 
//{ 
//  return GetParameter(i);  
//}
//
//inline double GetCovariance( int i ) const 
//{ 
//  return GetCovariance(i); 
//}
//
//inline double GetCovariance( int i, int j ) const 
//{ 
//  return GetCovariance(i,j);
//}
//
//
//inline double GetP    () const
//{
//  double par, err;
//  if( GetMomentum( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetPt   () const
//{
//  double par, err;
//  if( GetPt( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetEta   () const
//{
//  double par, err;
//  if( GetEta( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetPhi   () const
//{
//  double par, err;
//  if( GetPhi( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetMomentum    () const
//{
//  double par, err;
//  if( GetMomentum( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetMass        () const
//{
//  double par, err;
//  if( GetMass( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetDecayLength () const
//{
//  double par, err;
//  if( GetDecayLength( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetDecayLengthXY () const
//{
//  double par, err;
//  if( GetDecayLengthXY( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetLifeTime    () const
//{
//  double par, err;
//  if( GetLifeTime( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetR   () const
//{
//  double par, err;
//  if( GetR( par, err ) ) return 0;
//  else return par;
//}
//
//inline double GetErrX           () const 
//{
//  return sqrt(abs( GetCovariance(0,0) ));
//}
//
//inline double GetErrY           () const 
//{
//  return sqrt(abs( GetCovariance(1,1) ));
//}
//
//inline double GetErrZ           () const 
//{
//  return sqrt(abs( GetCovariance(2,2) ));
//}
//
//inline double GetErrPx          () const 
//{
//  return sqrt(abs( GetCovariance(3,3) ));
//}
//
//inline double GetErrPy          () const 
//{
//  return sqrt(abs( GetCovariance(4,4) ));
//}
//
//inline double GetErrPz          () const 
//{
//  return sqrt(abs( GetCovariance(5,5) ));
//}
//
//inline double GetErrE           () const 
//{
//  return sqrt(abs( GetCovariance(6,6) ));
//}
//
//inline double GetErrS           () const 
//{
//  return sqrt(abs( GetCovariance(7,7) ));
//}
//
//inline double GetErrP    () const
//{
//  double par, err;
//  if( GetMomentum( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrPt    () const
//{
//  double par, err;
//  if( GetPt( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrEta    () const
//{
//  double par, err;
//  if( GetEta( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrPhi    () const
//{
//  double par, err;
//  if( GetPhi( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrMomentum    () const
//{
//  double par, err;
//  if( GetMomentum( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrMass        () const
//{
//  double par, err;
//  if( GetMass( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrDecayLength () const
//{
//  double par, err;
//  if( GetDecayLength( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrDecayLengthXY () const
//{
//  double par, err;
//  if( GetDecayLengthXY( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrLifeTime    () const
//{
//  double par, err;
//  if( GetLifeTime( par, err ) ) return 1.e10;
//  else return err;
//}
//
//inline double GetErrR    () const
//{
//  double par, err;
//  if( GetR( par, err ) ) return 1.e10;
//  else return err;
//}
//
//
//inline int GetP( double &P, double &SigmaP ) const 
//{
//  return GetMomentum( P, SigmaP );
//}
//
//inline int GetPt( double &Pt, double &SigmaPt ) const 
//{
//  return GetPt( Pt, SigmaPt );
//}
//
//inline int GetEta( double &Eta, double &SigmaEta ) const 
//{
//  return GetEta( Eta, SigmaEta );
//}
//
//inline int GetPhi( double &Phi, double &SigmaPhi ) const 
//{
//  return GetPhi( Phi, SigmaPhi );
//}
//
//inline int GetMomentum( double &P, double &SigmaP ) const 
//{
//  return GetMomentum( P, SigmaP );
//}
//
//inline int GetMass( double &M, double &SigmaM ) const 
//{
//  return GetMass( M, SigmaM );
//}
//
//inline int GetDecayLength( double &L, double &SigmaL ) const 
//{
//  return GetDecayLength( L, SigmaL );
//}
//
//inline int GetDecayLengthXY( double &L, double &SigmaL ) const 
//{
//  return GetDecayLengthXY( L, SigmaL );
//}
//
//inline int GetLifeTime( double &T, double &SigmaT ) const 
//{
//  return GetLifeTime( T, SigmaT );
//}
//
//inline int GetR( double &R, double &SigmaR ) const 
//{
//  return GetR( R, SigmaR );
//}
//
//inline double & X() 
//{ 
//  return X();    
//}
//
//inline double & Y()
//{ 
//  return Y();    
//}
//
//inline double & Z() 
//{ 
//  return Z();    
//}
//
//inline double & Px() 
//{ 
//  return Px();   
//}
//
//inline double & Py() 
//{ 
//  return Py();   
//}
//
//inline double & Pz() 
//{ 
//  return Pz();   
//}
//
//inline double & E() 
//{ 
//  return E();    
//}
//
//inline double & S() 
//{ 
//  return S();    
//}
//
//inline int    & Q() 
//{ 
//  return Q();    
//}
//
//inline double & Chi2() 
//{ 
//  return Chi2(); 
//}
//
//inline int    & NDF() 
//{ 
//  return NDF();  
//}
//
//inline double & Parameter ( int i )        
//{ 
//  return Parameter(i);
//}
//
//inline double & Covariance( int i )        
//{ 
//  return Covariance(i);
//}
//
//inline double & Covariance( int i, int j ) 
//{ 
//  return Covariance(i,j); 
//}
//
//inline double[]  Parameters ()
//{
//  return fP;
//}
//
//inline double[]  CovarianceMatrix()
//{
//  return fC;
//}
//
//
//inline void operator +=( const KFParticle &Daughter )
//{
//  operator +=( Daughter );
//}
//  
//
//inline void AddDaughter( const KFParticle &Daughter )
//{
//  AddDaughter( Daughter );
//}
//
//inline void SetProductionVertex( const KFParticle &Vtx )
//{
//  SetProductionVertex( Vtx );
//}
//
//inline void SetMassConstraint( double Mass, double SigmaMass )
//{
//  SetMassConstraint( Mass, SigmaMass );
//}
//    
//inline void SetNoDecayLength()
//{
//  SetNoDecayLength();
//}
//
//inline void Construct( const KFParticle *vDaughters[], int nDaughters, 
//       const KFParticle *ProdVtx,   double Mass, bool IsConstrained  )
//{    
//  Construct( ( const KFParticleBase**)vDaughters, nDaughters, 
// ( const KFParticleBase*)ProdVtx, Mass, IsConstrained );
//}
//
//inline void TransportToDecayVertex()
//{ 
//  TransportToDecayVertex(); 
//}
//
//inline void TransportToProductionVertex()
//{
//  TransportToProductionVertex();
//}
//
//inline void TransportToPoint( const double xyz[] )
//{ 
//  TransportToDS( GetDStoPoint(xyz) );
//}
//#ifdef HomogeneousField
//inline void TransportToVertex( const AliVVertex &v )
//{       
//  TransportToPoint( KFParticle(v).fP );
//}
//#endif
//inline void TransportToParticle( const KFParticle &p )
//{ 
//  double dS, dSp;
//  GetDStoParticle( p, dS, dSp );
//  TransportToDS( dS );
//}
//
//inline void TransportToDS( double dS )
//{
//  TransportToDS( dS );
//} 
//
//inline double GetDStoPoint( const double xyz[] ) const 
//{
//#ifdef HomogeneousField
//  return GetDStoPointBz( GetFieldAlice(), xyz );
//#endif
//#ifdef NonhomogeneousField
//  return GetDStoPointCBM( xyz );
//#endif
//}
//
//  
//inline void GetDStoParticle( const KFParticle &p, 
//    double &DS, double &DSp ) const 
//{
//  GetDStoParticleXY( p, DS, DSp );
//}
//
//
//inline double GetDistanceFromVertex( const double vtx[] ) const
//{
//  return GetDistanceFromVertex( vtx );
//}
//
//inline double GetDeviationFromVertex( const double v[], 
//       const double Cv[] ) const
//{
//  return GetDeviationFromVertex( v, Cv);
//}
//
//inline double GetDistanceFromVertex( const KFParticle &Vtx ) const
//{
//  return GetDistanceFromVertex( Vtx );
//}
//
//inline double GetDeviationFromVertex( const KFParticle &Vtx ) const
//{
//  return GetDeviationFromVertex( Vtx );
//}
//#ifdef HomogeneousField
//inline double GetDistanceFromVertex( const AliVVertex &Vtx ) const
//{
//  return GetDistanceFromVertex( KFParticle(Vtx) );
//}
//
//inline double GetDeviationFromVertex( const AliVVertex &Vtx ) const
//{
//  return GetDeviationFromVertex( KFParticle(Vtx) );
//}
//#endif
//inline double GetDistanceFromParticle( const KFParticle &p ) const 
//{
//  return GetDistanceFromParticle( p );
//}
//
//inline double GetDeviationFromParticle( const KFParticle &p ) const 
//{
//  return GetDeviationFromParticle( p );
//}
//
//inline void SubtractFromVertex( KFParticle &v ) const 
//{
//  SubtractFromVertex( v );
//}
//
//#ifdef HomogeneousField
//inline double GetFieldAlice()
//{ 
//  return fgBz; 
//}
//#endif
//
//#ifdef HomogeneousField
//inline void GetFieldValue( const double[]  /*xyz*/, double B[] ) const 
//{    
//  B[0] = B[1] = 0;
//  B[2] = GetFieldAlice();
//}
//#endif
//
//#ifdef NonhomogeneousField
//inline void GetFieldValue( const double xyz[], double B[] ) const 
//{
////TODO Fix this
//  MagneticFieldManager::GetFieldValue(xyz,B);
//  //FairField *MF = CbmKF::Instance().GetMagneticField();
//  //MF.GetFieldValue( xyz, B );
//}
//#endif
//
//inline void GetDStoParticle( const KFParticleBase &p, 
//    double &DS, double &DSp )const
//{
//  GetDStoParticleXY( p, DS, DSp );
//}
//
//inline void GetDStoParticleXY( const KFParticleBase &p, 
//       double &DS, double &DSp ) const
//{ 
//#ifdef HomogeneousField
//  GetDStoParticleBz( GetFieldAlice(), p, DS, DSp ) ;
//#endif
//#ifdef NonhomogeneousField
//  GetDStoParticleCBM( p, DS, DSp ) ;
//#endif
//  //GetDStoParticleALICE( p, DS, DSp ) ;
//}
//
//inline void Transport( double dS, double P[], double C[] ) const 
//{
//#ifdef HomogeneousField
//  TransportBz( GetFieldAlice(), dS, P, C );
//#endif
//#ifdef NonhomogeneousField
//  TransportCBM( dS, P, C );
//#endif
//}
//
//inline void ConstructGamma( const KFParticle &daughter1,
//   const KFParticle &daughter2  )
//{
//#ifdef HomogeneousField
//  ConstructGammaBz( daughter1, daughter2, GetFieldAlice() );
//#endif
//}
//
//#endif 
//
//
////----------------------------------------------------------------------------
//// Implementation of the KFParticle class
//// .
//// @author  S.Gorbunov, I.Kisel, I.Kulakov, M.Zyzak
//// @version 1.0
//// @since   13.05.07
//// 
//// Class to reconstruct and store the decayed particle parameters.
//// The method is described in CBM-SOFT note 2007-003, 
//// ``Reconstruction of decayed particles based on the Kalman filter'', 
//// http://www.gsi.de/documents/DOC-2007-May-14-1.pdf
////
//// This class is ALICE interface to general mathematics in KFParticleCore
//// 
////  -= Copyright &copy ALICE HLT and CBM L1 Groups =-
////____________________________________________________________________________
//
//
//#include "KFParticle.h"
// 
//
//#ifdef HomogeneousField
//#include "AliVTrack.h"
//#include "AliVVertex.h"
//#endif
//
//#ifdef NonhomogeneousField
////#include "CbmKFTrack.h"
////#include "CbmKFVertex.h"
//#endif
//
//
//#ifdef HomogeneousField
//double fgBz = -5.;  //* Bz compoment of the magnetic field
//#endif
//
//KFParticle( const KFParticle &d1, const KFParticle &d2, bool gamma )
//{
//  if (!gamma) {
//    KFParticle mother;
//    mother+= d1;
//    mother+= d2;
//    *this = mother;
//  } else
//    ConstructGamma(d1, d2);
//}
//
//void Create( const double Param[], const double Cov[], int Charge, double mass /*int PID*/ )
//{
//  // Constructor from "cartesian" track, PID hypothesis should be provided
//  //
//  // Param[6] = { X, Y, Z, Px, Py, Pz } - position and momentum
//  // Cov [21] = lower-triangular part of the covariance matrix:
//  //
//  //                (  0  .  .  .  .  . )
//  //                (  1  2  .  .  .  . )
//  //  Cov. matrix = (  3  4  5  .  .  . ) - numbering of covariance elements in Cov[]
//  //                (  6  7  8  9  .  . )
//  //                ( 10 11 12 13 14  . )
//  //                ( 15 16 17 18 19 20 )
//  double C[21];
//  for( int i=0; i<21; i++ ) C[i] = Cov[i];
//  
////  TParticlePDG* particlePDG = TDatabasePDG::Instance().GetParticle(PID);
////  double mass = (particlePDG) ? particlePDG.Mass() :0.13957;
//  
//  Initialize( Param, C, Charge, mass );
//}
//
//#ifdef NonhomogeneousField
//KFParticle( CbmKFTrack* Tr, double[] z0)
//{
//
//  double[] m = Tr.GetTrack();
//  //cng
//  // recall that CbmKFTrack parameters are:
//  // (x, y, x', y', q/p, z)
//  // x' = dx/dz
//  // y' = dy/dz
//  //cng
//  double[] V = Tr.GetCovMatrix();
//
//  //* Fit of vertex track slopes and momentum (a,b,qp) to xyz0 vertex
//
////  if( z0 ) Tr.Extrapolate( *z0 );
//
//  double a = m[2], b = m[3], qp = m[4];
//  //cng
//  // a = dx/dz
//  // b = dy/dz
//  //cng
//  //* convert the track to (x,y,px,py,pz,e,t/p) parameterization
//
//  double c2 = 1./(1. + a*a + b*b);
//  double pq = 1./qp;
//
//  double p2 = pq*pq;
//  double pz = sqrt(p2*c2);
//  double px = a*pz;
//  double py = b*pz;
//  double H[3] = { -px*c2, -py*c2, -pz*pq };
//
//  double r[6];
//
//  r[0] = m[0]; // x
//  r[1] = m[1]; // y
//  r[2] = m[5]; // z
//  r[3] = px;
//  r[4] = py;
//  r[5] = pz;
//
//  double cxpz = H[0]*V[ 3] + H[1]*V[ 6] + H[2]*V[10];
//  double cypz = H[0]*V[ 4] + H[1]*V[ 7] + H[2]*V[11];
//  double capz = H[0]*V[ 5] + H[1]*V[ 8] + H[2]*V[12];
//  double cbpz = H[0]*V[ 8] + H[1]*V[ 9] + H[2]*V[13];
////  double cqpz = H[0]*V[12] + H[1]*V[13] + H[2]*V[14];
//  double cpzpz = H[0]*H[0]*V[5] +H[1]*H[1]*V[9] + H[2]*H[2]*V[14] 
//    + 2*( H[0]*H[1]*V[8] +H[0]*H[2]*V[12] +H[1]*H[2]*V[13]);
//
//  double C[21];
//
//  C[ 0] = V[0];
//  C[ 1] = V[1];
//  C[ 2] = V[2];
//  C[ 3] = 0;
//  C[ 4] = 0;
//  C[ 5] = 0;
//  C[ 6] = V[3]*pz + a*cxpz; 
//  C[ 7] = V[4]*pz + a*cypz; 
//  C[ 8] = 0;
//  C[ 9] = V[5]*pz*pz + 2*a*pz*capz + a*a*cpzpz;
//  C[10] = V[6]*pz+b*cxpz; 
//  C[11] = V[7]*pz+b*cypz; 
//  C[12] = 0;
//  C[13] = V[8]*pz*pz + a*pz*cbpz + b*pz*capz + a*b*cpzpz;
//  C[14] = V[9]*pz*pz + 2*b*pz*cbpz + b*b*cpzpz;
//  C[15] = cxpz; 
//  C[16] = cypz; 
//  C[17] = 0;
//  C[18] = capz*pz + a*cpzpz;
//  C[19] = cbpz*pz + b*cpzpz;
//  C[20] = cpzpz;
//
//  double Charge = (qp>0.) ?1 :( (qp<0) ?-1 :0);
//  
//  double mass = Tr.GetMass();
//
//  Create(r,C,Charge,mass);
//}
//
////KFParticle( CbmKFVertexInterface &vertex )
////{
////  // Constructor from CBM KF vertex
////  fP[0] = vertex.GetRefX();
////  fP[1] = vertex.GetRefY();
////  fP[2] = vertex.GetRefZ();
////  for( int i=0; i<6; i++)
////    fC[i] = vertex.GetCovMatrix( )[i];
////  fChi2 = vertex.GetRefChi2();
////  fNDF = vertex.GetRefNDF();//2*vertex.GetNContributors() - 3;
////  fQ = 0;
////  fAtProductionVertex = 0;
////  fIsLinearized = 0;
////  fSFromDecay = 0;
////}
//
//#endif
//
//#ifdef HomogeneousField
//KFParticle( const AliVTrack &track, int PID )
//{
//  // Constructor from ALICE track, PID hypothesis should be provided
//
//  track.XvYvZv(fP);
//  track.PxPyPz(fP+3);
//  fQ = track.Charge();
//  track.GetCovarianceXYZPxPyPz( fC );
//
//  TParticlePDG* particlePDG = TDatabasePDG::Instance().GetParticle(PID);
//  double mass = (particlePDG) ? particlePDG.Mass() :0.13957;
//
//  Create(fP,fC,fQ,mass);
//}
//
//KFParticle( const AliVVertex &vertex )
//{
//  // Constructor from ALICE vertex
//
//  vertex.GetXYZ( fP );
//  vertex.GetCovarianceMatrix( fC );  
//  fChi2 = vertex.GetChi2();
//  fNDF = 2*vertex.GetNContributors() - 3;
//  fQ = 0;
//  fAtProductionVertex = 0;
//  fIsLinearized = 0;
//  fSFromDecay = 0;
//}
//
//void GetExternalTrackParam( const KFParticleBase &p, double &X, double &Alpha, double P[5] ) 
//{
//  // Conversion to AliExternalTrackParam parameterization
//
//  double cosA = p.GetPx(), sinA = p.GetPy(); 
//  double pt = sqrt(cosA*cosA + sinA*sinA);
//  double pti = 0;
//  if( pt<1.e-4 ){
//    cosA = 1;
//    sinA = 0;
//  } else {
//    pti = 1./pt;
//    cosA*=pti;
//    sinA*=pti;
//  }
//  Alpha = atan2(sinA,cosA);  
//  X   = p.GetX()*cosA + p.GetY()*sinA;   
//  P[0]= p.GetY()*cosA - p.GetX()*sinA;
//  P[1]= p.GetZ();
//  P[2]= 0;
//  P[3]= p.GetPz()*pti;
//  P[4]= p.GetQ()*pti;
//}
//
//#endif
//
//
//bool GetDistanceFromVertexXY( const double vtx[], const double Cv[], double &val, double &err ) const
//{
//  //* Calculate DCA distance from vertex (transverse impact parameter) in XY
//  //* v = [xy], Cv=[Cxx,Cxy,Cyy ]-covariance matrix
//
//  bool ret = 0;
//  
//  double mP[8];
//  double mC[36];
//  
//  Transport( GetDStoPoint(vtx), mP, mC );  
//
//  double dx = mP[0] - vtx[0];
//  double dy = mP[1] - vtx[1];
//  double px = mP[3];
//  double py = mP[4];
//  double pt = sqrt(px*px + py*py);
//  double ex=0, ey=0;
//  if( pt<1.e-4 ){
//    ret = 1;
//    pt = 1.;
//    val = 1.e4;
//  } else{
//    ex = px/pt;
//    ey = py/pt;
//    val = dy*ex - dx*ey;
//  }
//
//  double h0 = -ey;
//  double h1 = ex;
//  double h3 = (dy*ey + dx*ex)*ey/pt;
//  double h4 = -(dy*ey + dx*ex)*ex/pt;
//  
//  err = 
//    h0*(h0*GetCovariance(0,0) + h1*GetCovariance(0,1) + h3*GetCovariance(0,3) + h4*GetCovariance(0,4) ) +
//    h1*(h0*GetCovariance(1,0) + h1*GetCovariance(1,1) + h3*GetCovariance(1,3) + h4*GetCovariance(1,4) ) +
//    h3*(h0*GetCovariance(3,0) + h1*GetCovariance(3,1) + h3*GetCovariance(3,3) + h4*GetCovariance(3,4) ) +
//    h4*(h0*GetCovariance(4,0) + h1*GetCovariance(4,1) + h3*GetCovariance(4,3) + h4*GetCovariance(4,4) );
//
//  if( Cv ){
//    err+= h0*(h0*Cv[0] + h1*Cv[1] ) + h1*(h0*Cv[1] + h1*Cv[2] ); 
//  }
//
//  err = sqrt(abs(err));
//
//  return ret;
//}
//
//bool GetDistanceFromVertexXY( const double vtx[], double &val, double &err ) const
//{
//  return GetDistanceFromVertexXY( vtx, 0, val, err );
//}
//
//
//bool GetDistanceFromVertexXY( const KFParticle &Vtx, double &val, double &err ) const 
//{
//  //* Calculate distance from vertex [cm] in XY-plane
//
//  return GetDistanceFromVertexXY( Vtx.fP, Vtx.fC, val, err );
//}
//
//#ifdef HomogeneousField
//bool GetDistanceFromVertexXY( const AliVVertex &Vtx, double &val, double &err ) const 
//{
//  //* Calculate distance from vertex [cm] in XY-plane
//
//  return GetDistanceFromVertexXY( KFParticle(Vtx), val, err );
//}
//#endif
//
//double GetDistanceFromVertexXY( const double vtx[] ) const
//{
//  //* Calculate distance from vertex [cm] in XY-plane
//  double val, err;
//  GetDistanceFromVertexXY( vtx, 0, val, err );
//  return val;
//}
//
//double GetDistanceFromVertexXY( const KFParticle &Vtx ) const 
//{
//  //* Calculate distance from vertex [cm] in XY-plane
//
//  return GetDistanceFromVertexXY( Vtx.fP );
//}
//
//#ifdef HomogeneousField
//double GetDistanceFromVertexXY( const AliVVertex &Vtx ) const 
//{
//  //* Calculate distance from vertex [cm] in XY-plane
//
//  return GetDistanceFromVertexXY( KFParticle(Vtx).fP );
//}
//#endif
//
//double GetDistanceFromParticleXY( const KFParticle &p ) const 
//{
//  //* Calculate distance to other particle [cm]
//
//  double dS, dS1;
//  GetDStoParticleXY( p, dS, dS1 );   
//  double mP[8], mC[36], mP1[8], mC1[36];
//  Transport( dS, mP, mC ); 
//  p.Transport( dS1, mP1, mC1 ); 
//  double dx = mP[0]-mP1[0]; 
//  double dy = mP[1]-mP1[1]; 
//  return sqrt(dx*dx+dy*dy);
//}
//
//double GetDeviationFromParticleXY( const KFParticle &p ) const 
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from other particle
//
//  double dS, dS1;
//  GetDStoParticleXY( p, dS, dS1 );   
//  double mP1[8], mC1[36];
//  p.Transport( dS1, mP1, mC1 ); 
//
//  double d[2]={ fP[0]-mP1[0], fP[1]-mP1[1] };
//
//  double sigmaS = .1+10.*sqrt( (d[0]*d[0]+d[1]*d[1] )/
//(mP1[3]*mP1[3]+mP1[4]*mP1[4] )  );
//
//  double h[2] = { mP1[3]*sigmaS, mP1[4]*sigmaS };       
//  
//  mC1[0] +=h[0]*h[0];
//  mC1[1] +=h[1]*h[0]; 
//  mC1[2] +=h[1]*h[1]; 
//
//  return GetDeviationFromVertexXY( mP1, mC1 )*sqrt(2./1.);
//}
//
//
//double GetDeviationFromVertexXY( const double vtx[], const double Cv[] ) const 
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//  //* v = [xyz], Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix
//
//  double val, err;
//  bool problem = GetDistanceFromVertexXY( vtx, Cv, val, err );
//  if( problem || err<1.e-20 ) return 1.e4;
//  else return val/err;
//}
//
//
//double GetDeviationFromVertexXY( const KFParticle &Vtx ) const  
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//  //* v = [xyz], Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix
//
//  return GetDeviationFromVertexXY( Vtx.fP, Vtx.fC );
//}
//
//#ifdef HomogeneousField
//double GetDeviationFromVertexXY( const AliVVertex &Vtx ) const 
//{
//  //* Calculate sqrt(Chi2/ndf) deviation from vertex
//  //* v = [xyz], Cv=[Cxx,Cxy,Cyy,Cxz,Cyz,Czz]-covariance matrix
//
//  KFParticle v(Vtx);
//  return GetDeviationFromVertexXY( v.fP, v.fC );
//}
//#endif
//
//double GetAngle  ( const KFParticle &p ) const 
//{
//  //* Calculate the opening angle between two particles
//
//  double dS, dS1;
//  GetDStoParticle( p, dS, dS1 );   
//  double mP[8], mC[36], mP1[8], mC1[36];
//  Transport( dS, mP, mC ); 
//  p.Transport( dS1, mP1, mC1 ); 
//  double n = sqrt( mP[3]*mP[3] + mP[4]*mP[4] + mP[5]*mP[5] );
//  double n1= sqrt( mP1[3]*mP1[3] + mP1[4]*mP1[4] + mP1[5]*mP1[5] );
//  n*=n1;
//  double a = 0;
//  double PI = acos(-1.);
//  if( n>1.e-8 ) a = ( mP[3]*mP1[3] + mP[4]*mP1[4] + mP[5]*mP1[5] )/n;
//  if (abs(a)<1.) a = acos(a);
//  else a = (a>=0) ?0 :PI;
//  return a;
//}
//
//double GetAngleXY( const KFParticle &p ) const 
//{
//  //* Calculate the opening angle between two particles in XY plane
//
//  double dS, dS1;
//  GetDStoParticleXY( p, dS, dS1 );   
//  double mP[8], mC[36], mP1[8], mC1[36];
//  Transport( dS, mP, mC ); 
//  p.Transport( dS1, mP1, mC1 ); 
//  double n = sqrt( mP[3]*mP[3] + mP[4]*mP[4] );
//  double n1= sqrt( mP1[3]*mP1[3] + mP1[4]*mP1[4] );
//  n*=n1;
//  double a = 0;
//  double PI = acos(-1.);
//  if( n>1.e-8 ) a = ( mP[3]*mP1[3] + mP[4]*mP1[4] )/n;
//  if (abs(a)<1.) a = acos(a);
//  else a = (a>=0) ?0 :PI;
//  return a;
//}
//
//double GetAngleRZ( const KFParticle &p ) const 
//{
//  //* Calculate the opening angle between two particles in RZ plane
//
//  double dS, dS1;
//  GetDStoParticle( p, dS, dS1 );   
//  double mP[8], mC[36], mP1[8], mC1[36];
//  Transport( dS, mP, mC ); 
//  p.Transport( dS1, mP1, mC1 ); 
//  double nr = sqrt( mP[3]*mP[3] + mP[4]*mP[4] );
//  double n1r= sqrt( mP1[3]*mP1[3] + mP1[4]*mP1[4]  );
//  double n = sqrt( nr*nr + mP[5]*mP[5] );
//  double n1= sqrt( n1r*n1r + mP1[5]*mP1[5] );
//  n*=n1;
//  double a = 0;
//  double PI = acos(-1.);
//  if( n>1.e-8 ) a = ( nr*n1r +mP[5]*mP1[5])/n; 
//  if (abs(a)<1.) a = acos(a);
//  else a = (a>=0) ?0 :PI;
//  return a;
//}
//
//
///*
//
//#include "AliExternalTrackParam.h"
//
//void GetDStoParticleALICE( const KFParticleBase &p, 
//   double &DS, double &DS1 ) 
//  const
//{ 
//  DS = DS1 = 0;   
//  double x1, a1, x2, a2;
//  double par1[5], par2[5], cov[15];
//  for(int i=0; i<15; i++) cov[i] = 0;
//  cov[0] = cov[2] = cov[5] = cov[9] = cov[14] = .001;
//
//  GetExternalTrackParam( *this, x1, a1, par1 );
//  GetExternalTrackParam( p, x2, a2, par2 );
//
//  AliExternalTrackParam t1(x1,a1, par1, cov);
//  AliExternalTrackParam t2(x2,a2, par2, cov);
//
//  double xe1=0, xe2=0;
//  t1.GetDCA( &t2, -GetFieldAlice(), xe1, xe2 );
//  t1.PropagateTo( xe1, -GetFieldAlice() );
//  t2.PropagateTo( xe2, -GetFieldAlice() );
//
//  double xyz1[3], xyz2[3];
//  t1.GetXYZ( xyz1 );
//  t2.GetXYZ( xyz2 );
//  
//  DS = GetDStoPoint( xyz1 );
//  DS1 = p.GetDStoPoint( xyz2 );
//
//  return;
//}
//*/
//
//  // * Pseudo Proper Time of decay = (r*pt) / |pt| * M/|pt|
//double GetPseudoProperDecayTime( const KFParticle &pV, const double& mass, double* timeErr2 ) const
//{ // TODO optimize with respect to time and stability
//  const double ipt2 = 1/( Px()*Px() + Py()*Py() );
//  const double mipt2 = mass*ipt2;
//  const double dx = X() - pV.X();
//  const double dy = Y() - pV.Y();
//
//  if ( timeErr2 ) {
//      // -- calculate error = sigma(f(r)) = f'Cf'
//      // r = {x,y,px,py,x_pV,y_pV}
//      // df/dr = { px*m/pt^2,
//      //     py*m/pt^2,
//      //    ( x - x_pV )*m*(1/pt^2 - 2(px/pt^2)^2),
//      //    ( y - y_pV )*m*(1/pt^2 - 2(py/pt^2)^2),
//      //     -px*m/pt^2,
//      //     -py*m/pt^2 }
//    const double f0 = Px()*mipt2;
//    const double f1 = Py()*mipt2;
//    const double mipt2derivative = mipt2*(1-2*Px()*Px()*ipt2);
//    const double f2 = dx*mipt2derivative;
//    const double f3 = -dy*mipt2derivative;
//    const double f4 = -f0;
//    const double f5 = -f1;
//
//    const double& mC00 =    GetCovariance(0,0);
//    const double& mC10 =    GetCovariance(0,1);
//    const double& mC11 =    GetCovariance(1,1);
//    const double& mC20 =    GetCovariance(3,0);
//    const double& mC21 =    GetCovariance(3,1);
//    const double& mC22 =    GetCovariance(3,3);
//    const double& mC30 =    GetCovariance(4,0);
//    const double& mC31 =    GetCovariance(4,1);
//    const double& mC32 =    GetCovariance(4,3);
//    const double& mC33 =    GetCovariance(4,4);
//    const double& mC44 = pV.GetCovariance(0,0);
//    const double& mC54 = pV.GetCovariance(1,0);
//    const double& mC55 = pV.GetCovariance(1,1);
//
//    *timeErr2 =
//      f5*mC55*f5 +
//      f5*mC54*f4 +
//      f4*mC44*f4 +
//      f3*mC33*f3 +
//      f3*mC32*f2 +
//      f3*mC31*f1 +
//      f3*mC30*f0 +
//      f2*mC22*f2 +
//      f2*mC21*f1 +
//      f2*mC20*f0 +
//      f1*mC11*f1 +
//      f1*mC10*f0 +
//      f0*mC00*f0;
//  }
//  return ( dx*Px() + dy*Py() )*mipt2;
//}
//
