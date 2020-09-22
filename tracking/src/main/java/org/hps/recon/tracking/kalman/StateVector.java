package org.hps.recon.tracking.kalman;

import java.util.logging.Logger;

//State vector (projected, filtered, or smoothed) for the Kalman filter
class StateVector {

    int kUp;            // Last site index for which information is used in this state vector
    int kLow;           // Index of the site for the present pivot (lower index on a in the formalism)
    HelixState helix;   // Class that includes the helix parameters, pivot, coordinate rotation, etc.
    double mPred;       // Filtered or smoothed predicted measurement at site kLow (filled in MeasurementSite.java)
    double r;           // Predicted, filtered, or smoothed residual at site kLow
    double R;           // Covariance of residual
    final private boolean verbose;
    SquareMatrix F;     // Propagator matrix to propagate from this site to the next site
    private Logger logger;

    // Constructor for the initial state vector used to start the Kalman filter.
    StateVector(int site, Vec helixParams, SquareMatrix Cov, Vec pivot, double B, Vec tB, Vec origin) {
        // Here tB is the B field direction, while B is the magnitude
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = false;
        if (verbose) System.out.format("StateVector: constructing an initial state vector\n");
        helix = new HelixState(helixParams, pivot, origin, Cov, B, tB);
        kLow = site;
        kUp = kLow;
    }

    // Constructor for a new blank state vector with a new B field
    StateVector(int site, double B, Vec tB, Vec origin) {
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = false;
        helix = new HelixState(B, tB, origin);
        kLow = site;
    }

    // Constructor for a new completely blank state vector
    StateVector(int site) {
        kLow = site;
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = false;
        helix = new HelixState();
    }

    // Deep copy of the state vector
    StateVector copy() {
        StateVector q = new StateVector(kLow);
        q.helix = helix.copy();
        q.kUp = kUp;
        if (F != null) q.F = F.copy();
        q.mPred = mPred;
        q.R = R;
        q.r = r;
        return q;
    }

    // Debug printout of the state vector
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
        
    String toString(String s) {
        String str = String.format(">>>Dump of state vector %s %d  %d\n", s, kUp, kLow);
        str = str + helix.toString(" ");
        if (F != null) str = str + F.toString("for the propagator");
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
        if (verbose) { // drho and dz are indeed always zero here
            aPrime.helix.a.print("StateVector predict: pivot transformed helix; should have zero drho and dz");
            helix.a.print("old helix");
            pivot.print("new pivot");
            helix.X0.print("old pivot");
        }

        F = this.helix.makeF(aPrime.helix.a); // Calculate derivatives of the pivot transform
        if (deltaE != 0.) {
            double factor = 1.0 - deltaEoE;
            for (int i = 0; i < 5; i++) { F.M[i][2] *= factor; }
        }

        // Transform to the coordinate system of the field at the new site
        // First, transform the pivot point to the new system
        aPrime.helix.X0 = aPrime.helix.toLocal(this.helix.toGlobal(aPrime.helix.X0));

        // Calculate the matrix for the net rotation from the old site coordinates to the new site coordinates
        RotMatrix Rt = aPrime.helix.Rot.multiply(this.helix.Rot.invert());
        SquareMatrix fRot = new SquareMatrix(5);
        if (verbose) {
            aPrime.helix.Rot.print("aPrime rotation matrix");
            this.helix.Rot.print("this rotation matrix");
            Rt.print("rotation from old local frame to new local frame");
            aPrime.helix.a.print("StateVector:predict helix before rotation");
        }

        // Rotate the helix parameters here. 
        // dz and drho will remain unchanged at zero
        // phi0 and tanl(lambda) change, as does kappa (1/pt). However, |p| should be unchanged by the rotation.
        // This call to rotateHelix also calculates the derivative matrix fRot
        aPrime.helix.a = HelixState.rotateHelix(aPrime.helix.a, Rt, fRot);
        if (verbose) {
            aPrime.helix.a.print("StateVector:predict helix after rotation");
            fRot.print("fRot from StateVector:predict");
        }
        F = fRot.multiply(F);

        // Test the derivatives
        /*
        if (verbose) {
            double daRel[] = { 0.01, 0.03, -0.02, 0.05, -0.01 };
            StateVector aPda = copy();
            for (int i = 0; i < 5; i++) {
                aPda.a.v[i] = a.v[i] * (1.0 + daRel[i]);
            }
            Vec da = aPda.a.dif(a);
            StateVector aPrimeNew = copy();
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
        SquareMatrix Ctot;
        if (XL == 0.) {
            Ctot = this.helix.C;
            if (verbose) System.out.format("StateVector.predict: XL=%9.6f", XL);
        } else {
            double momentum = (1.0 / helix.a.v[2]) * Math.sqrt(1.0 + helix.a.v[4] * helix.a.v[4]);
            double sigmaMS = HelixState.projMSangle(momentum, XL);
            if (verbose) System.out.format("StateVector.predict: momentum=%12.5e, XL=%9.6f sigmaMS=%12.5e", momentum, XL, sigmaMS);
            Ctot = this.helix.C.sum(this.helix.getQ(sigmaMS));
        }

        // Now propagate the multiple scattering matrix and covariance matrix to the new site
        aPrime.helix.C = Ctot.similarity(F);

        // Temporary test of fine stepping the pivot transform (for uniform field)
        // Verified that taking multiple steps gives the same result for both the helix parameters
        // and the covariance matrix compared to making a single step, for uniform field.
        /*
        int Ntr = 5;
        Vec delta = (pivot.dif(X0)).scale(1.0/(double)Ntr);
        Vec oldPivot = X0;
        Vec oldHelix = a;
        System.out.format("Fine stepping test:\n");
        pivot.print("target new pivot");
        delta.print("delta");
        oldPivot.print("oldPivot");
        oldHelix.print("oldHelix");
        SquareMatrix oldC = Ctot;
        oldC.print("old C");
        for (int itr=0; itr<Ntr; itr++) {
            Vec newPivot = oldPivot.sum(delta);
            Vec newHelix = StateVector.pivotTransform(newPivot, oldHelix, oldPivot, alpha, 0.);
            SquareMatrix Ftmp = StateVector.makeF(newHelix, oldHelix, alpha);
            SquareMatrix newC = oldC.similarity(Ftmp);
            System.out.format("Fine stepping test itr=%d\n", itr);
            newPivot.print("newPivot");
            newHelix.print("newHelix");
            newC.print("newC");
            oldPivot = newPivot;
            oldHelix = newHelix;
            oldC = newC;
        }
        aPrime.a.print("transformed helix");
        aPrime.C.print("transformed covariance");
        */
        return aPrime;
    }

    // Create a filtered state vector from a predicted state vector
    StateVector filter(Vec H, double V) {
        // H = prediction matrix (5-vector)
        // V = hit variance (1/sigma^2)

        StateVector aPrime = copy();
        aPrime.kUp = kLow;

        double denom = V + H.dot(H.leftMultiply(helix.C));
        Vec K = H.leftMultiply(helix.C).scale(1.0 / denom); // Kalman gain matrix
        if (verbose) {
            System.out.format("StateVector.filter: kLow=%d\n", kLow);
            System.out.format("StateVector.filter: V=%12.4e,  denom=%12.4e\n", V, denom);
            K.print("Kalman gain matrix in StateVector.filter");
            H.print("matrix H in StateVector.filter");
            System.out.format("StateVector.filter: k dot H = %10.7f\n", K.dot(H));
            // Alternative calculation of K (sanity check that it gives the same result):
            SquareMatrix D = helix.C.invert().sum(H.scale(1.0 / V).product(H));
            Vec Kalt = H.scale(1.0 / V).leftMultiply(D.invert());
            Kalt.print("alternate Kalman gain matrix");
        }

        aPrime.helix.a = helix.a.sum(K.scale(r));
        SquareMatrix U = new SquareMatrix(5, 1.0);
        aPrime.helix.C = (U.dif(K.product(H))).multiply(helix.C);

        if (verbose) {
            aPrime.helix.C.print("filtered covariance (gain-matrix formalism) in StateVector.filter");
            // Alternative calculation of filtered covariance (sanity check that it gives
            // the same result):
            SquareMatrix D = helix.C.invert().sum(H.scale(1.0 / V).product(H));
            SquareMatrix Calt = D.invert();
            Calt.print("alternate (weighted-means formalism) filtered covariance in StateVector.filter");
            aPrime.helix.C.multiply(D).print("unit matrix??");
            helix.a.print("predicted helix parameters");
            aPrime.helix.a.print("filtered helix parameters (gain matrix formalism)");
        }
        //double R = (1 - H.dot(K))*V;
        //System.out.format("StateVector.filter: R=%10.8f\n", R);

        return aPrime;
    }

    // Modify the state vector by removing the hit information
    Vec inverseFilter(Vec H, double V, SquareMatrix Cnew) {
        double denom = -V + H.dot(H.leftMultiply(helix.C));
        Vec Kstar = H.leftMultiply(helix.C).scale(1.0 / denom); // Kalman gain matrix

        Vec aNew = helix.a.sum(Kstar.scale(r));
        SquareMatrix U = new SquareMatrix(5, 1.0);
        SquareMatrix Cstar = (U.dif(Kstar.product(H))).multiply(helix.C);
        if (verbose) {
            System.out.format("StateVector.inverseFilter: V=%12.4e,  denom=%12.4e\n", V, denom);
            Kstar.print("Kalman gain matrix in StateVector.inverseFilter");
            H.print("matrix H in StateVector.inverseFilter");
            helix.a.print("old helix");
            aNew.print(" new helix in StateVector.inverseFilter");
            helix.C.print("old covariance");
            Cnew.print(" new covariance in StateVector.inverseFilter");
        }
        Cnew.M = Cstar.M;
        return aNew;
    }

    // Create a smoothed state vector from the filtered state vector
    StateVector smooth(StateVector snS, StateVector snP) {
        if (verbose) System.out.format("StateVector.smooth of filtered state %d %d, using smoothed state %d %d and predicted state %d %d", kLow, kUp,
                    snS.kLow, snS.kUp, snP.kLow, snP.kUp);
        StateVector sS = this.copy();

        SquareMatrix CnInv = snP.helix.C.invert();
        SquareMatrix A = (helix.C.multiply(sS.F.transpose())).multiply(CnInv);

        Vec diff = snS.helix.a.dif(snP.helix.a);
        sS.helix.a = helix.a.sum(diff.leftMultiply(A));

        SquareMatrix Cdiff = snS.helix.C.dif(snP.helix.C);
        sS.helix.C = helix.C.sum(Cdiff.similarity(A));

        return sS;
    }

    // Return errors on the helix parameters at the global origin
    Vec helixErrors(Vec aPrime) {
        // aPrime are the helix parameters for a pivot at the global origin, assumed
        // already to be calculated by pivotTransform()
        SquareMatrix tC = covariancePivotTransform(aPrime);
        return new Vec(Math.sqrt(tC.M[0][0]), Math.sqrt(tC.M[1][1]), Math.sqrt(tC.M[2][2]), Math.sqrt(tC.M[3][3]), Math.sqrt(tC.M[4][4]));
    }

    // Transform the helix covariance to new pivot point (specified in local
    // coordinates)
    SquareMatrix covariancePivotTransform(Vec aP) {
        // aP are the helix parameters for the new pivot point, assumed already to be
        // calculated by pivotTransform()
        // Note that no field rotation is assumed or accounted for here
        SquareMatrix mF = helix.makeF(aP);
        return helix.C.similarity(mF);
    }    
    
}
