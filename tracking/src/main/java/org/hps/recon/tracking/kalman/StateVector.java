package org.hps.recon.tracking.kalman;

import java.util.logging.Logger;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
/**
 * 
 * Helix state vector (projected, filtered, or smoothed) for the Kalman filter
 * @author Robert Johnson
 *
 */
class StateVector {

    int kUp;            // Last site index for which information is used in this state vector
    int kLow;           // Index of the site for the present pivot (lower index on a in the formalism)
    HelixState helix;   // Class that includes the helix parameters, pivot, coordinate rotation, etc.
    double mPred;       // Filtered or smoothed predicted measurement at site kLow (filled in MeasurementSite.java)
    double r;           // Predicted, filtered, or smoothed residual at site kLow
    double R;           // Covariance of residual
    final static private boolean debug = false;
    DMatrixRMaj F;     // Propagator matrix to propagate from this site to the next site
    private static Logger logger;
    private DMatrixRMaj K;      // Kalman gain matrix
    
    // Working arrays for efficiency, to avoid creating temporary working space over and over
    private static DMatrixRMaj tempV;
    private static DMatrixRMaj tempV2;
    private static DMatrixRMaj tempM;
    private static DMatrixRMaj tempA;
    private static DMatrixRMaj Cinv;
    private static DMatrixRMaj Q;      // Multiple scattering matrix, zero except (1,1) and (4,4)
    private static DMatrixRMaj U;      // Unit matrix
    private static LinearSolverDense<DMatrixRMaj> solver;
    private static boolean initialized;

    // Constructor for the initial state vector used to start the Kalman filter.
    StateVector(int site, Vec helixParams, DMatrixRMaj Cov, Vec pivot, double B, Vec tB, Vec origin) {
        // Here tB is the B field direction, while B is the magnitude       
        if (debug) System.out.format("StateVector: constructing an initial state vector\n");
        helix = new HelixState(helixParams, pivot, origin, Cov, B, tB);
        kLow = site;
        kUp = kLow;
        if (!initialized) {  // Initialize the static working arrays on the first call
            logger = Logger.getLogger(StateVector.class.getName());
            tempV = new DMatrixRMaj(5,1);
            tempV2 = new DMatrixRMaj(5,1);
            tempM = new DMatrixRMaj(5,5);
            tempA = new DMatrixRMaj(5,5);
            Q = new DMatrixRMaj(5,5);
            U = CommonOps_DDRM.identity(5,5);
            Cinv = new DMatrixRMaj(5,5);
            solver = LinearSolverFactory_DDRM.symmPosDef(5);
            initialized = true;
        }
    }

    // Constructor for a new blank state vector with a new B field
    StateVector(int site, double B, Vec tB, Vec origin) {
        helix = new HelixState(B, tB, origin);
        kLow = site;
        if (!initialized) {  // Initialize the static working arrays on the first call
            logger = Logger.getLogger(StateVector.class.getName());
            tempV = new DMatrixRMaj(5,1);
            tempV2 = new DMatrixRMaj(5,1);
            tempM = new DMatrixRMaj(5,5);
            tempA = new DMatrixRMaj(5,5);
            Q = new DMatrixRMaj(5,5);
            U = CommonOps_DDRM.identity(5,5);
            Cinv = new DMatrixRMaj(5,5);
            solver = LinearSolverFactory_DDRM.symmPosDef(5);
            initialized = true;
        }
    }

    // Constructor for a new completely blank state vector
    StateVector(int site) {
        kLow = site;
        if (!initialized) {  // Initialize the static working arrays on the first call
            logger = Logger.getLogger(StateVector.class.getName());
            tempV = new DMatrixRMaj(5,1);
            tempV2 = new DMatrixRMaj(5,1);
            tempM = new DMatrixRMaj(5,5);
            tempA = new DMatrixRMaj(5,5);
            Q = new DMatrixRMaj(5,5);
            U = CommonOps_DDRM.identity(5,5);
            Cinv = new DMatrixRMaj(5,5);
            solver = LinearSolverFactory_DDRM.symmPosDef(5);
            initialized = true;
        }
    }

    StateVector copy() {
        StateVector q = new StateVector(kLow);
        q.helix = (HelixState)helix.copy();      // Deep copy
        q.kUp = kUp;
        q.F = F;  // Don't deep copy the F matrix
        q.mPred = mPred;
        q.R = R;
        q.r = r;
        q.K = K;
        return q;
    }

    // Debug printout of the state vector
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
        
    String toString(String s) {
        String str = String.format(">>>Dump of state vector %s %d  %d\n", s, kUp, kLow);
        str = str + helix.toString(" ");
        if (F != null) str = str + "Propagator matrix: " + F.toString();
        double sigmas;
        if (R > 0.) {
            sigmas = r / Math.sqrt(R);
        } else {
            sigmas = 0.;
        }
        str = str + String.format("Predicted measurement=%10.6f, residual=%10.7f, covariance of residual=%12.4e, std. devs. = %12.4e\n", mPred, r, R,
                sigmas);
        str = str + String.format("End of dump of state vector %s %d  %d<<<\n", s, kUp, kLow);
        return str;
    }

    // Create a predicted state vector by propagating a given helix to a measurement site
    StateVector predict(int newSite, Vec pivot, double B, Vec t, Vec originPrime, double XL, double deltaE) {
        // newSite = index of the new site
        // pivot = pivot point of the new site in the local coordinates of this state vector (i.e. coordinates of the old site)
        // B and t = magnitude and direction of the magnetic field at the pivot point, in global coordinates
        // XL = thickness of the scattering material
        // deltaE = energy loss in the scattering material
        // originPrime = origin of the detector coordinates at the new site in global coordinates

        // This constructs a new blank state vector with pivot and helix parameters undefined as yet
        StateVector aPrime = new StateVector(newSite, B, t, originPrime);
        aPrime.kUp = kUp;
        aPrime.helix.X0 = pivot; // pivot before helix rotation, in coordinate system of the previous site

        double E = helix.a.v[2] * Math.sqrt(1.0 + helix.a.v[4] * helix.a.v[4]);
        double deltaEoE = deltaE / E;

        // Transform helix in old coordinate system to new pivot point lying on the next detector plane
        if (deltaE == 0.) {
            aPrime.helix.a = this.helix.pivotTransform(pivot);
        } else {
            aPrime.helix.a = this.helix.pivotTransform(pivot, deltaEoE);
        }

        if (debug) { // drho and dz are indeed always zero here
            aPrime.helix.a.print("StateVector predict: pivot transformed helix; should have zero drho and dz");
            helix.a.print("old helix");
            pivot.print("new pivot");
            helix.X0.print("old pivot");
        }

        F = new DMatrixRMaj(5,5);
        this.helix.makeF(aPrime.helix.a, F); // Calculate derivatives of the pivot transform
        if (deltaE != 0.) {
            double factor = 1.0 - deltaEoE;
            for (int i = 0; i < 5; i++) F.unsafe_set(i, 2, F.unsafe_get(i,2)*factor);  
        }

        // Transform to the coordinate system of the field at the new site
        // First, transform the pivot point to the new system
        aPrime.helix.X0 = aPrime.helix.toLocal(this.helix.toGlobal(aPrime.helix.X0));

        // Calculate the matrix for the net rotation from the old site coordinates to the new site coordinates
        RotMatrix Rt = aPrime.helix.Rot.multiply(this.helix.Rot.invert());
        if (debug) {
            aPrime.helix.Rot.print("aPrime rotation matrix");
            this.helix.Rot.print("this rotation matrix");
            Rt.print("rotation from old local frame to new local frame");
            aPrime.helix.a.print("StateVector:predict helix before rotation");
        }

        // Rotate the helix parameters here. 
        // dz and drho will remain unchanged at zero
        // phi0 and tanl(lambda) change, as does kappa (1/pt). However, |p| should be unchanged by the rotation.
        // This call to rotateHelix also calculates the derivative matrix fRot
        aPrime.helix.a = HelixState.rotateHelix(aPrime.helix.a, Rt, tempM);
        if (debug) {
            aPrime.helix.a.print("StateVector:predict helix after rotation");
            System.out.println("fRot from StateVector:predict");
            tempM.print();
        }
        CommonOps_DDRM.mult(tempM, F, tempA);

        // Test the derivatives
        /*
        if (debug) {
            double daRel[] = { 0.01, 0.03, -0.02, 0.05, -0.01 };
            StateVector aPda = this.Copy();
            for (int i = 0; i < 5; i++) {
                aPda.a.v[i] = a.v[i] * (1.0 + daRel[i]);
            }
            Vec da = aPda.a.dif(a);
            StateVector aPrimeNew = this.Copy();
            aPrimeNew.a = aPda.pivotTransform(pivot);
            RotMatrix RtTmp = Rot.invert().multiply(aPrime.Rot);
            SquareMatrix fRotTmp = new SquareMatrix(5);
            aPrimeNew.a = rotateHelix(aPrimeNew.a, RtTmp, fRotTmp);
            for (int i = 0; i < 5; i++) {
                double deltaExact = aPrimeNew.a.v[i] - aPrime.a.v[i];
                double delta = 0.;
                for (int j = 0; j < 5; j++) {
                    delta += F.M[i][j] * da.v[j];
                }
                System.out.format("Test of F: Helix parameter %d, deltaExact=%10.8f, delta=%10.8f\n", i, deltaExact,
                        delta);
            }
        }
        */

        aPrime.kLow = newSite;
        aPrime.kUp = kUp;

        // Add the multiple scattering contribution for the silicon layer
        // sigmaMS is the rms of the projected scattering angle
        if (XL == 0.) {
            Cinv.set(this.helix.C);
            if (debug) System.out.format("StateVector.predict: XL=%9.6f\n", XL);
        } else {
            double momentum = (1.0 / helix.a.v[2]) * Math.sqrt(1.0 + helix.a.v[4] * helix.a.v[4]);
            double sigmaMS = HelixState.projMSangle(momentum, XL);
            if (debug) System.out.format("StateVector.predict: momentum=%12.5e, XL=%9.6f sigmaMS=%12.5e\n", momentum, XL, sigmaMS);
            this.helix.getQ(sigmaMS, Q);
            CommonOps_DDRM.add(this.helix.C, Q, Cinv);
        }

        // Now propagate the multiple scattering matrix and covariance matrix to the new site
        CommonOps_DDRM.multTransB(Cinv, tempA, tempM);
        aPrime.helix.C = new DMatrixRMaj(5,5);
        CommonOps_DDRM.mult(tempA, tempM, aPrime.helix.C);

        return aPrime;
    }

    // Create a filtered state vector from a predicted state vector
    StateVector filter(DMatrixRMaj H, double V) {
        // H = prediction matrix (5-vector)
        // V = hit variance (1/sigma^2)

        StateVector aPrime = this.copy();
        aPrime.kUp = kLow;

        CommonOps_DDRM.mult(helix.C, H, tempV);
        double denom = V + CommonOps_DDRM.dot(H, tempV);
        CommonOps_DDRM.scale(1.0/denom, helix.C, tempM);
        K = new DMatrixRMaj(5,1);
        CommonOps_DDRM.mult(tempM, H, K);  //  Kalman gain matrix
        if (debug) {
            System.out.format("StateVector.filter: kLow=%d\n", kLow);
            System.out.format("StateVector.filter: V=%12.4e,  denom=%12.4e\n", V, denom);
            System.out.format("Kalman gain matrix in StateVector.filter: ");
            K.print();
            System.out.format("matrix H in StateVector.filter");
            H.print();
            System.out.format("StateVector.filter: k dot H = %10.7f\n", CommonOps_DDRM.dot(K, H));
            // Alternative calculation of K (sanity check that it gives the same result):
            DMatrixRMaj D = new DMatrixRMaj(5,5);
            CommonOps_DDRM.invert(helix.C, D);
            DMatrixRMaj Dp = new DMatrixRMaj(5,5);
            for (int i=0; i<5; ++i) {
                for (int j=0; j<5; ++j) {
                    Dp.unsafe_set(i, j, H.unsafe_get(i, 0)*H.unsafe_get(j, 0)/V);
                }
            }
            CommonOps_DDRM.addEquals(D, Dp);
            CommonOps_DDRM.invert(D);
            DMatrixRMaj Hs = new DMatrixRMaj(5);
            CommonOps_DDRM.scale(1.0/V, H, Hs);
            DMatrixRMaj Kalt = new DMatrixRMaj(5);
            CommonOps_DDRM.mult(D, Hs, Kalt);
            System.out.println("Alternative Kalman gain matrix:");
            Kalt.print();
        }

        CommonOps_DDRM.scale(r, K, tempV);
        aPrime.helix.a = helix.a.sum(mToVec(tempV));
        directProd(K, H, tempM);
        CommonOps_DDRM.scale(-1.0, tempM);
        CommonOps_DDRM.addEquals(tempM, U);
        CommonOps_DDRM.mult(tempM, helix.C, aPrime.helix.C);

        if (debug) {
            System.out.format("StateVector.filter: compare covariance calculations, original one first:\n");
            // Alternate calculation of the covariance update
            double R = V*(1- CommonOps_DDRM.dot(H, K));
            CommonOps_DDRM.mult(tempM, helix.C, tempV);
            CommonOps_DDRM.multTransB(tempV, tempM, tempA);
            directProd(K, K, tempV);
            DMatrixRMaj tempZ = new DMatrixRMaj(5,5);
            CommonOps_DDRM.add(tempA, R, tempV, tempZ);
            aPrime.helix.C.print();
            tempZ.print();
            System.out.println("filtered covariance (gain-matrix formalism) in StateVector.filter:");
            aPrime.helix.C.print();
            // Alternative calculation of filtered covariance (sanity check that it gives
            // the same result):
            DMatrixRMaj Cinv = new DMatrixRMaj(25);
            CommonOps_DDRM.invert(helix.C, Cinv);
            DMatrixRMaj Hscaled = new DMatrixRMaj(5);
            CommonOps_DDRM.scale(1.0/V, H, Hscaled);
            directProd(Hscaled,H,tempM);
            DMatrixRMaj D = new DMatrixRMaj(5);
            CommonOps_DDRM.add(Cinv,tempM,D);
            CommonOps_DDRM.invert(D);
            System.out.println("alternate (weighted-means formalism) filtered covariance in StateVector.filter");
            D.print();
            helix.a.print("predicted helix parameters");
            aPrime.helix.a.print("filtered helix parameters (gain matrix formalism)");
        }

        return aPrime;
    }

    // Modify the state vector by removing the hit information
    Vec inverseFilter(DMatrixRMaj H, double V, DMatrixRMaj Cnew) {
        CommonOps_DDRM.mult(helix.C, H, tempV);
        double denom = -V + CommonOps_DDRM.dot(H, tempV);
        CommonOps_DDRM.scale(1.0/denom, helix.C, tempM);
        DMatrixRMaj Kstar = new DMatrixRMaj(5,1);
        CommonOps_DDRM.mult(tempM, H, Kstar);   // Kalman gain matrix

        CommonOps_DDRM.scale(r, Kstar, tempV);
        Vec aNew = helix.a.sum(mToVec(tempV));
        directProd(Kstar, H, tempM);
        CommonOps_DDRM.scale(-1.0, tempM);
        CommonOps_DDRM.addEquals(tempM, U);
        CommonOps_DDRM.mult(tempM, helix.C, Cnew);
        if (debug) {
            System.out.format("StateVector.inverseFilter: V=%12.4e,  denom=%12.4e\n", V, denom);
            helix.a.print("old helix");
            aNew.print(" new helix in StateVector.inverseFilter");
        }
        return aNew;
    }

    // Create a smoothed state vector from the filtered state vector
    StateVector smooth(StateVector snS, StateVector snP) {
        if (debug) System.out.format("StateVector.smooth of filtered state %d %d, using smoothed state %d %d and predicted state %d %d\n", kLow, kUp,
                    snS.kLow, snS.kUp, snP.kLow, snP.kUp);
        StateVector sS = this.copy();

        // solver.setA defines the input matrix and checks whether it is singular. 
        // A copy is needed because the input gets modified.
        if (!solver.setA(snP.helix.C.copy())) {
            SquareMatrix invrs = KalTrack.mToS(snP.helix.C).fastInvert();
            if (invrs == null) {
                logger.warning("StateVector:smooth, inversion of the covariance matrix failed");
                snP.helix.C.print();
                for (int i=0; i<5; ++i) {      // Fill the inverse with something not too crazy and continue . . .
                    for (int j=0; j<5; ++j) {
                        if (i == j) {
                            Cinv.unsafe_set(i,j,1.0/snP.helix.C.unsafe_get(i,j));
                        } else {
                            Cinv.unsafe_set(i, j, 0.);
                            snP.helix.C.unsafe_set(i, j, 0.);
                        }
                    }
                }          
            } else {
                if (debug) {
                    KalTrack.mToS(snP.helix.C).print("singular covariance?");
                    invrs.print("inverse");
                    invrs.multiply(KalTrack.mToS(snP.helix.C)).print("unit matrix?"); 
                }
                for (int i=0; i<5; ++i) {
                    for (int j=0; j<5; ++j) {
                        Cinv.unsafe_set(i, j, invrs.M[i][j]);
                    }
                }
            }
        } else {
            solver.invert(Cinv);
        }

        CommonOps_DDRM.multTransB(helix.C, sS.F, tempM);
        CommonOps_DDRM.mult(tempM, Cinv, tempA);

        vecToM(snS.helix.a.dif(snP.helix.a), tempV);
        CommonOps_DDRM.mult(tempA, tempV, tempV2);
        sS.helix.a = helix.a.sum(mToVec(tempV2));
        if (debug) {
            System.out.println("StateVector:smooth, inverse of the covariance:");
            Cinv.print("%11.6e");
            CommonOps_DDRM.mult(snP.helix.C, Cinv, tempM);
            System.out.format("Unit matrix?? ");
            tempM.print();
            System.out.format("Predicted helix covariance: ");
            snP.helix.C.print();
            System.out.format("This helix covariance: ");
            helix.C.print();
            System.out.format("Matrix F ");
            sS.F.print();
            System.out.format("tempM ");
            tempM.print();
            System.out.format("tempA ");
            tempA.print();
            System.out.format("Difference of helix parameters tempV: ");
            tempV.print();
            System.out.format("tempV2 ");
            tempV2.print();
            sS.helix.a.print("new helix parameters");
        }

        CommonOps_DDRM.subtract(snS.helix.C, snP.helix.C, tempM);
        CommonOps_DDRM.multTransB(tempM, tempA, Cinv);
        CommonOps_DDRM.mult(tempA, Cinv, tempM);
        CommonOps_DDRM.add(helix.C, tempM, sS.helix.C);
        
        if (debug) sS.print("Smoothed");
        return sS;
    }

    // Return errors on the helix parameters at the global origin
    Vec helixErrors(Vec aPrime) {
        // aPrime are the helix parameters for a pivot at the global origin, assumed
        // already to be calculated by pivotTransform()
        DMatrixRMaj tC = covariancePivotTransform(aPrime);
        return new Vec(Math.sqrt(tC.unsafe_get(0,0)), Math.sqrt(tC.unsafe_get(1,1)), Math.sqrt(tC.unsafe_get(2,2)), 
                Math.sqrt(tC.unsafe_get(3,3)), Math.sqrt(tC.unsafe_get(4,4)));
    }

    // Transform the helix covariance to new pivot point (specified in local coordinates)
    DMatrixRMaj covariancePivotTransform(Vec aP) {
        // aP are the helix parameters for the new pivot point, assumed already to be
        // calculated by pivotTransform()
        // Note that no field rotation is assumed or accounted for here
        DMatrixRMaj mF = new DMatrixRMaj(5,5);
        helix.makeF(aP, mF);
        CommonOps_DDRM.multTransB(helix.C, mF, tempM);
        CommonOps_DDRM.mult(mF, tempM, tempA);
        return tempA;
    }    
    // Go to and from 1D EJML matrix for a vector Vec
    static void vecToM(Vec a, DMatrixRMaj m) {
        for (int i=0; i<a.N; ++i) {
            m.unsafe_set(i, 0, a.v[i]);
        }
    }
    static Vec mToVec(DMatrixRMaj M) {
        return new Vec(M.unsafe_get(0, 0), M.unsafe_get(1, 0), M.unsafe_get(2, 0), M.unsafe_get(3, 0), M.unsafe_get(4, 0));
    }
    // Direct product of two row vectors to make a 2D matrix
    private static void directProd(DMatrixRMaj a, DMatrixRMaj b, DMatrixRMaj c) {
        for (int i=0; i<5; ++i) {
            for (int j=0; j<5; ++j) {
                c.unsafe_set(i, j, a.unsafe_get(i, 0)*b.unsafe_get(j, 0));
            }
        }
    }
/*
    private static SquareMatrix mToS(DMatrixRMaj M) {
        SquareMatrix S= new SquareMatrix(5);
        for (int i=0; i<5; ++i) {
            for (int j=0; j<5; ++j) {
                S.M[i][j] = M.unsafe_get(i, j);
            }
        }
        return S;
    }
*/
}
