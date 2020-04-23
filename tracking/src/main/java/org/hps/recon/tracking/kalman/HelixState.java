package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.util.Pair;

class HelixState {
    Vec a;                      // Helix parameters 
    Vec X0;                     // Pivot point of helix
    RotMatrix Rot;              // Rotation from the global coordinates to the local field coordinates 
    Vec origin;                 // Origin of the local field coordinates in the global system.
    SquareMatrix C;             // Helix covariance matrix 
    double B;           // Magnetic field magnitude at origin
    private Vec tB;             // Magnetic field direction at origin
    double alpha;       // Conversion from 1/K to radius R
    private double c;           // Speed of light
    private Logger logger;
    private HelixPlaneIntersect hpi;
    private boolean verbose;
    
    HelixState(Vec a, Vec X0, Vec origin, SquareMatrix C, double B, Vec tB) {
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = logger.getLevel()==Level.FINEST;
        this.a = a;
        this.X0 = X0;
        this.origin = origin;
        this.C = C;
        this.B = B;
        this.tB = tB;
        hpi = new HelixPlaneIntersect();
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1.0e12 / (c * B); // Convert from pt in GeV to curvature in mm
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(tB).unitVec();
        Vec v = tB.cross(u);
        Rot = new RotMatrix(u, v, tB);
    }
    
    HelixState(double B, Vec tB, Vec origin) {
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = logger.getLevel()==Level.FINEST;
        this.origin = origin;
        this.B = B;
        this.tB = tB;
        hpi = new HelixPlaneIntersect();
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1.0e12 / (c * B); // Convert from pt in GeV to curvature in mm
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(tB).unitVec();
        Vec v = tB.cross(u);
        Rot = new RotMatrix(u, v, tB);
    }
    
    HelixState() {
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = logger.getLevel()==Level.FINEST;
        hpi = new HelixPlaneIntersect();
        c = 2.99793e8; // Speed of light in m/s        
    }
    
    HelixState copy() {
        return new HelixState(a.copy(), X0.copy(), origin.copy(), C.copy(), B, tB.copy());
    }
    
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
    
    String toString(String s) {
        String str;
        str = String.format("HelixState %s: helix parameters=%s,  pivot=%s\n", s, a.toString(), X0.toString());
        str = str + String.format("   Origin=%s,  B=%10.6f in direction %s\n", origin.toString(), B, tB.toString());
        str = str + C.toString("covariance");
        str = str + Rot.toString("from global coordinates to field coordinates");
        return str;
    }
    
    // Returns a point on the helix at the angle phi
    // Warning: the point returned is in the B-Field reference frame
    Vec atPhi(double phi) {
        return atPhi(X0, a, phi, alpha);
    }

    static Vec atPhi(Vec X0, Vec a, double phi, double alpha) {
        double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha / a.v[2]) * Math.cos(a.v[1] + phi);
        double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha / a.v[2]) * Math.sin(a.v[1] + phi);
        double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        return new Vec(x, y, z);
    }

    // Returns the particle momentum at the helix angle phi
    // Warning! This is returned in the B-Field coordinate system.
    Vec getMom(double phi) {
        return getMom(phi, a);
    }
    
    static Vec getMom(double phi, Vec a) {
        double px = -Math.sin(a.v[1] + phi) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1] + phi) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        return new Vec(px, py, pz);
    }

    // Calculate the phi angle to propagate on helix to the intersection with a
    // measurement plane
    double planeIntersect(Plane pIn) { // pIn is assumed to be defined in the global reference frame
        Plane p = pIn.toLocal(Rot, origin); // Transform the plane into the B-field local reference frame
        return hpi.planeIntersect(a, X0, alpha, p);
    }
    
    // Return errors on the helix parameters at the present pivot point
    Vec helixErrors() {
        return new Vec(Math.sqrt(C.M[0][0]), Math.sqrt(C.M[1][1]), Math.sqrt(C.M[2][2]), Math.sqrt(C.M[3][3]), Math.sqrt(C.M[4][4]));
    }
    
    // Derivative matrix for the pivot transform (without energy loss or field rotations)
    SquareMatrix makeF(Vec aP) {
        return makeF(aP, a, alpha);
    }
    
    static SquareMatrix makeF(Vec aP, Vec a, double alpha) {
        double[][] f = new double[5][5];
        f[0][0] = Math.cos(aP.v[1] - a.v[1]);
        f[0][1] = (a.v[0] + alpha / a.v[2]) * Math.sin(aP.v[1] - a.v[1]);
        f[0][2] = (alpha / (a.v[2] * a.v[2])) * (1.0 - Math.cos(aP.v[1] - a.v[1]));
        f[1][0] = -Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[1][1] = (a.v[0] + alpha / a.v[2]) * Math.cos(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[1][2] = (alpha / (a.v[2] * a.v[2])) * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[2][2] = 1.0;
        f[3][0] = (alpha / a.v[2]) * a.v[4] * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[3][1] = (alpha / a.v[2]) * a.v[4] * (1.0 - (a.v[0] + alpha / a.v[2]) * Math.cos(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]));
        f[3][2] = (alpha / (a.v[2] * a.v[2])) * a.v[4]
                * (aP.v[1] - a.v[1] - (alpha / a.v[2]) * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]));
        f[3][3] = 1.0;
        f[3][4] = -(alpha / a.v[2]) * (aP.v[1] - a.v[1]);
        f[4][4] = 1.0;

        return new SquareMatrix(5, f);
    }
    
    // Momentum at the start of the given helix (point closest to the pivot)
    static Vec aTOp(Vec a) {
        double px = -Math.sin(a.v[1]) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1]) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        return new Vec(px, py, pz);
    }

    // Transform from momentum at helix starting point back to the helix parameters
    // drho and dz are not modified
    static Vec pTOa(Vec p, double drho, double dz, double Q) {
        double phi0 = Math.atan2(-p.v[0], p.v[1]);
        double K = Q / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        double tanl = p.v[2] / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);

        return new Vec(drho, phi0, K, dz, tanl);
    }

    // To transform a space point from global to local field coordinates, first subtract
    // <origin> and then rotate by <Rot>.
    Vec toLocal(Vec xGlobal) {
        Vec xLocal = Rot.rotate(xGlobal.dif(origin));
        return xLocal;
    }
    
    // To transform a space point from local field coordinates to global coordinates, first rotate by
    // the inverse of <Rot> and then add the <origin>.
    Vec toGlobal(Vec xLocal) {
        Vec xGlobal = Rot.inverseRotate(xLocal).sum(origin);
        return xGlobal;
    }
    
    // Transformation of helix parameters from one B-field frame to another, by rotation R
    // Warning: the pivot point has to be transformed too! Here we assume that the new pivot point
    // will be on the helix at phi=0, so drho and dz will always be returned as zero. Therefore, before
    // calling this routine, make sure that the current pivot point is on the helix (drho=dz=0)
    static Vec rotateHelix(Vec a, RotMatrix R, SquareMatrix fRot) {
        // The rotation is easily applied to the momentum vector, so first we transform from helix parameters
        // to momentum, apply the rotation, and then transform back to helix parameters.
        // The values for fRot, the corresponding derivative matrix, are also calculated and returned.

        boolean verbose = false;

        Vec phlx = aTOp(a); // Momentum at point closest to helix
        Vec p_prime = R.rotate(phlx); // Momentum in new coordinate system
        if (verbose) {
            a.print("input helix, in rotateHelix");
            R.print("in rotateHelix");
            phlx.print("momentum vector in rotateHelix");
            p_prime.print("rotated momentum vector in rotateHelix");
            System.out.format("in rotateHelix: p=%10.7f,  prot=%10.7f\n", phlx.mag(), p_prime.mag());
        }

        SquareMatrix dpda = new SquareMatrix(3);
        dpda.M[0][0] = -Math.cos(a.v[1]) / Math.abs(a.v[2]);
        dpda.M[0][1] = Math.sin(a.v[1]) / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[1][0] = -Math.sin(a.v[1]) / Math.abs(a.v[2]);
        dpda.M[1][1] = -Math.cos(a.v[1]) / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[2][1] = -a.v[4] / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[2][2] = 1. / Math.abs(a.v[2]);

        SquareMatrix dprimedp = new SquareMatrix(3);
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                dprimedp.M[i][j] = R.M[i][j]; 
            } 
        }

        double Q = Math.signum(a.v[2]);
        SquareMatrix dadp = new SquareMatrix(3);
        double pt2 = p_prime.v[0] * p_prime.v[0] + p_prime.v[1] * p_prime.v[1];
        double pt = Math.sqrt(pt2);
        dadp.M[0][0] = -p_prime.v[1] / pt2;
        dadp.M[0][1] = p_prime.v[0] / pt2;
        dadp.M[1][0] = -Q * p_prime.v[0] / (pt2 * pt);
        dadp.M[1][1] = -Q * p_prime.v[1] / (pt2 * pt);
        dadp.M[2][0] = -p_prime.v[0] * p_prime.v[2] / (pt2 * pt);
        dadp.M[2][1] = -p_prime.v[1] * p_prime.v[2] / (pt2 * pt);
        dadp.M[2][2] = 1.0 / (pt);

        SquareMatrix prod = dadp.multiply(dprimedp.multiply(dpda));

        fRot.M[0][0] = 1.0;
        fRot.M[1][1] = prod.M[0][0];
        fRot.M[1][2] = prod.M[0][1];
        fRot.M[1][4] = prod.M[0][2];
        fRot.M[2][1] = prod.M[1][0];
        fRot.M[2][2] = prod.M[1][1];
        fRot.M[2][4] = prod.M[1][2];
        fRot.M[3][3] = 1.0;
        fRot.M[4][1] = prod.M[2][0];
        fRot.M[4][2] = prod.M[2][1];
        fRot.M[4][4] = prod.M[2][2];

        /*
         * if (verbose) { // derivative test Vec da = a.scale(0.005); Vec apda =
         * a.sum(da); Vec ap = pTOa(R.rotate(aTOp(a)), a); Vec apdap =
         * pTOa(R.rotate(aTOp(apda)), apda); Vec dap = apdap.dif(ap); Vec dap2 =
         * da.leftMultiply(fRot);
         * System.out.format("StateVector:rotateHelix: derivative test:\n");
         * dap.print("actual difference in helix parameters");
         * dap2.print("diff in helix params from derivatives"); }
         */
        // The parameters drho and dz are assumed not to change in the rotation. This will be correct
        // if the rotation is about the pivot point, and the pivot is on the helix, in which case
        // dz and drho are both zero. That is not necessarily valid if dz and drho are not zero. If the
        // rotation were only about the z axis it would be, but any other sort of rotation will mix dz and drho.
        //if (Math.abs(a.v[0]) > 0.001 || Math.abs(a.v[3]) > 0.001) {
        //    System.out.format("StateVector.rotateHelix: warning, dz=%10.5f and drho=%10.5f are not zero.\n",a.v[3],a.v[0]);
        //}
        Vec aNew = pTOa(p_prime, a.v[0], a.v[3], Q);
        if (verbose) { aNew.print("rotated helix in rotateHelix"); }
        return aNew;
    }
    
    // Transform the helix to a pivot back at the global origin
    Vec pivotTransform() {
        Vec pivot = origin.scale(-1.0);
        return pivotTransform(pivot);
    }

    // Pivot transform of the state vector, from the current pivot to the pivot in
    // the argument (specified in local coordinates)
    Vec pivotTransform(Vec pivot) {
        return pivotTransform(pivot, a, X0, alpha, 0.);
    }

    // Pivot transform including energy loss just before
    Vec pivotTransform(Vec pivot, double deltaEoE) {
        return pivotTransform(pivot, a, X0, alpha, deltaEoE);
    }

    static Vec pivotTransform(Vec pivot, Vec a, Vec X0, double alpha, double deltaEoE) {
        double K = a.v[2] * (1.0 - deltaEoE); // Lose energy before propagating
        double xC = X0.v[0] + (a.v[0] + alpha / K) * Math.cos(a.v[1]); // Center of the helix circle
        double yC = X0.v[1] + (a.v[0] + alpha / K) * Math.sin(a.v[1]);
        // if (verbose) System.out.format("pivotTransform center=%13.10f, %13.10f\n",
        // xC, yC);

        // Predicted state vector
        double[] aP = new double[5];
        aP[2] = K;
        aP[4] = a.v[4];
        if (K > 0) {
            aP[1] = Math.atan2(yC - pivot.v[1], xC - pivot.v[0]);
        } else {
            aP[1] = Math.atan2(pivot.v[1] - yC, pivot.v[0] - xC);
        }
        aP[0] = (xC - pivot.v[0]) * Math.cos(aP[1]) + (yC - pivot.v[1]) * Math.sin(aP[1]) - alpha / K;
        aP[3] = X0.v[2] - pivot.v[2] + a.v[3] - (alpha / K) * (aP[1] - a.v[1]) * a.v[4];

        // xC = pivot.v[0] + (aP[0]+alpha/aP[2])*Math.cos(aP[1]);
        // yC = pivot.v[1] + (aP[0]+alpha/aP[2])*Math.sin(aP[1]);

        return new Vec(5, aP);
    }
    
    Vec propagateRungeKutta(Plane pln, org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, double XL) {
        ArrayList<Double> yScat = new ArrayList<Double>();
        RotMatrix originRot = new RotMatrix();
        return propagateRungeKutta(pln, fM, newCovariance, yScat, originRot, XL);
    }
    
    // Propagate a helix by Runge-Kutta integration to an arbitrary plane
    Vec propagateRungeKutta(Plane pln, org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, ArrayList<Double> yScat, RotMatrix RotOut, double XL) {
        // pln = plane to where the extrapolation is taking place in global coordinates.  
        // The origin of pln will be the new helix pivot point in global coordinates and the origin of the B-field system.
        // fM  = HPS field map
        // newCovariance = output helix covariance at the new pivot
        // return value = helix parameters at the new pivot. These helix parameters are valid in the B-field coordinate system with
        //                origin at the pivot point and z axis in the direction of the B-field at the pivot.
        // yScat = input array of y values where scattering in silicon will take place. Only those between the start and finish points
        //         will be used, so including extras will just waste a bit of CPU time
        // Rot   = rotation from global coordinates to the B-field system
        // XL    = silicon thickness in radiation lengths
        //boolean verbose = true;
        
        // Take the B-field reference frame to be at the position X of the plane. The returned helix will be in this frame.
        Vec B = KalmanInterface.getField(pln.X(), fM);
        double Bmag = B.mag();
        double alphaOrigin = 1.0e12 / (c * Bmag);
        Vec tB = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec uB = yhat.cross(tB).unitVec();
        Vec vB = tB.cross(uB);
        RotMatrix originRot = new RotMatrix(uB, vB, tB); // Rotation from the global system into the B-field system at the plane
        for (int i=0; i<3; ++i) {
            for (int j=0; j<3; ++j) {
                RotOut.M[i][j] = originRot.M[i][j];
            }
        }
        
        // Target plane after transforming to the B-field coordinate system 
        Plane originPlane = pln.toLocal(originRot, pln.X());    
        
        // Point and momentum on the helix in the B-field system near the tracking layer from where we extrapolate
        Vec xLocal = atPhi(0.);
        Vec pLocal = getMom(0.);

        // Starting position and momentum in the B-field system
        Vec X0origin = originRot.rotate((Rot.inverseRotate(xLocal).sum(origin)).dif(pln.X()));
        Vec P0origin = originRot.rotate(Rot.inverseRotate(pLocal));
        double Q = Math.signum(a.v[2]);

        Vec pInt = new Vec(3);
        Vec Xplane = hpi.rkIntersect(originPlane, X0origin, P0origin, Q, fM, pInt); // RK propagation to the target plane
        Vec helixAtIntersect = pTOa(pInt, 0., 0., Q); // Helix with pivot at Xplane
        Vec helixAtOrigin = pivotTransform(originPlane.X(), helixAtIntersect, Xplane, alpha, 0.);
        if (verbose) {
            System.out.format("\nStateVector.propagateRungeKutta, Q=%8.1f, origin=%10.5f %10.5f %10.5f:\n", Q, origin.v[0], origin.v[1],
                    origin.v[2]);
            System.out.format("    At final plane B=%10.5f, t=%10.6f %10.6f %10.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
            System.out.format("    alpha=%10.6f,  alpha at final plane=%10.6f\n", alpha, alphaOrigin);
            Rot.invert().print("from local system at layer 1 to global");
            originRot.print("to Bfield system at final plane");
            originPlane.print("at final plane in B-field system");
            origin.print("origin of local detector system in global coordinates");
            X0.print("helix pivot");
            a.print("local helix parameters at layer 1");
            xLocal.print("point on helix, local at layer 1");
            pLocal.print("helix momentum, local at layer 1");
            X0origin.print("point on helix, origin system, at layer 1");
            P0origin.print("helix momentum, origin system global at layer 1");
            Xplane.print("RK helix intersection with final plane");
            Vec Xglob = originRot.inverseRotate(Xplane);
            Xglob.print("intersection with final plane in global coordinates");
            pInt.print("RK momentum at helix intersection");
            helixAtIntersect.print("helix at final-plane intersection");
            helixAtOrigin.print("helix with pivot at final plane");
            pln.X().print("origin of final field system in global system");
            Rot.print("rotation matrix from global to final field system");
        }

        // The covariance matrix is transformed assuming a sequence of pivot transforms (not Runge Kutta)
        double stepSize = 25.0;
        // Step from the origin of this StateVector to pln.X(), both in global coordinates
        Vec transHelix = new Vec(5);
        if (!helixStepper(stepSize, yScat, XL, newCovariance, transHelix, pln.X(), fM)) {
            for (int i=0; i<5; ++i) {
                for (int j=0; j<5; ++j) {
                    newCovariance.M[i][j] = this.C.M[i][j];
                }
            }
        } else if (verbose) {
            transHelix.print("helixStepper helix at final plane");
            C.print("original covariance");
            System.out.println("    Errors: ");
            for (int i = 0; i < 5; ++i) { System.out.format(" %10.7f", Math.sqrt(C.M[i][i])); }
            System.out.println("\n");
            newCovariance.print("transformed covariance");
            System.out.println("    Errors: ");
            for (int i = 0; i < 5; ++i) { System.out.format(" %10.7f", Math.sqrt(newCovariance.M[i][i])); }
            System.out.println("\n");
        }
               
        return helixAtOrigin;
    }
    
    // Propagate a helix by Runge-Kutta integration to an x,z plane containing the origin.
    Vec propagateRungeKutta(org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, double XL) {
      
        // Plane in the B-field coordinate system at the origin
        Plane originPlane = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.)); 
 
        return propagateRungeKutta(originPlane, fM, newCovariance, XL);
    }

    static double projMSangle(double p, double XL) {
        if (XL <= 0.) return 0.;
        return (0.0136 / Math.abs(p)) * Math.sqrt(XL) * (1.0 + 0.038 * Math.log(XL));
    }

    boolean helixStepper(double maxStep, ArrayList<Double> yScat, double XL, SquareMatrix Covariance, Vec finalHelix, Vec newOrigin, org.lcsim.geometry.FieldMap fM) {
        // The old and new origin points are in global coordinates. The old helix and old pivot are defined
        // in a coordinate system aligned with the field and centered at the old origin. The returned
        // helix will be in a coordinate system aligned with the local field at the new origin, and the
        // new pivot point will be at the new origin, in global coordinates, although to use it one should transform to the field frame, which
        // is defined with its origin at newOrigin and aligned there with the local field.        
        // All scattering layers are assumed to be of the same thickness XL, in radiation lengths
        // We assume that the starting StateVector is at a layer with a hit, in which case the Kalman filter has already accounted for
        // multiple scattering at that layer.       
                
        //boolean verbose = true;
        
        double tol = 1.0;  // Tolerance in mm to determine whether a location is on a scattering plane. 
        if (maxStep < tol) tol = maxStep/2.0;
        int nSteps = (int)(Math.abs(newOrigin.v[1] - this.origin.v[1])/maxStep);
        if (nSteps < 1) nSteps = 1;
        ArrayList<Pair<Double,Boolean>> stepPnts = new ArrayList<Pair<Double,Boolean>>(nSteps+yScat.size());
        double yDistance = newOrigin.v[1] - this.origin.v[1];
        double yStep = yDistance/(double)nSteps;
        
        //stepPnts.add(new Pair<Double,Boolean>(this.origin.v[1],false));
        double yNext = this.origin.v[1];
        double dir = Math.signum(yStep);
        for (int i=0; i<nSteps; ++i) {
            double yLast = yNext;
            yNext += yStep;
            boolean XLnext = false;
            for (double y : yScat) {
                if (dir*y > dir*yLast + tol && dir*y < dir*yNext - tol) { // Add intermediate scattering layer
                    Pair<Double,Boolean> newLayer = new Pair<Double,Boolean>(y,true);
                    stepPnts.add(newLayer);
                } else if (y >= yNext - tol && y <= yNext + tol) {
                    XLnext = true;
                }
            }
            stepPnts.add(new Pair<Double,Boolean>(yNext,XLnext));         
        }
        if (dir > 0.) {
            Collections.sort(stepPnts, HelixState.pairComparator);
        } else {
            Collections.sort(stepPnts, HelixState.pairComparator.reversed());
        }
        
        double localAlpha = alpha;

        RotMatrix RM = Rot;
        Vec newHelix = this.a.copy();
        Vec Pivot = this.X0.copy();
        Vec Origin = this.origin.copy(); // In global coordinates
        SquareMatrix fRot = new SquareMatrix(5);
        SquareMatrix Cov = this.C;
        Vec yhat = new Vec(0., 1., 0.);
        if (verbose) {
            System.out.format("Entering helixStepper for %d steps, B=%10.7f, B direction=%10.7f %10.7f %10.7f\n", 
                    stepPnts.size(), B, RM.M[2][0],RM.M[2][1], RM.M[2][2]);
            for (Pair<Double,Boolean> step : stepPnts) {
                double y = step.getFirstElement();
                boolean ifScat = step.getSecondElement();
                System.out.format("  stepping layer at y=%10.5f, scatter = %b\n", y, ifScat);
            }
            this.origin.print("old origin");
            this.X0.print("old pivot");
            this.a.print("old helix");
            Cov.print("old helix covariance");
            newOrigin.print("new origin");
            RM.print("to transform to the local field frame");
            Plane pln = new Plane(newOrigin, yhat);
            Plane plnLocal = pln.toLocal(RM, Origin);
            double dphi = hpi.planeIntersect(newHelix, Pivot, localAlpha, plnLocal); // Find the helix intersection with the plane
            Vec newPivot = atPhi(Pivot, newHelix, dphi, localAlpha);
            newPivot.print("new pivot in origin plane");
            Vec newHelix0 = pivotTransform(newPivot, newHelix, Pivot, localAlpha, 0.);
            newHelix0.print("Pivot transform to final plane in a single step");
        }
        for (int step = 0; step < stepPnts.size(); ++step) {
            Pair<Double, Boolean> thisStep = stepPnts.get(step);
            double yInt = thisStep.getFirstElement();
            boolean thisXL = thisStep.getSecondElement();
            Plane pln = new Plane(new Vec(0., yInt, 0.), yhat); // Make a plane in global coordinates, perpendicular to the y axis
            Plane plnLocal = pln.toLocal(RM, Origin); // Transform the plane to local coordinates
            double dphi = hpi.planeIntersect(newHelix, Pivot, localAlpha, plnLocal); // Find the helix intersection with the plane
            if (Double.isNaN(dphi)) {
                logger.log(Level.WARNING, String.format("No intersection with the plane at step=%d\n", step));
                return false;
            }
            Vec newPivot = atPhi(Pivot, newHelix, dphi, localAlpha);
            Vec newHelixPivoted = pivotTransform(newPivot, newHelix, Pivot, localAlpha, 0.); // Transform the helix pivot to the intersection point
            if (verbose) {
                System.out.format("Step %d, y=%8.3f, dphi=%10.7f, alpha=%10.7e\n", step, yInt, dphi, localAlpha);
                pln.print("to intersect, in global coordinate");
                plnLocal.print("to intersect, in local field coordinates");
                Pivot.print("old pivot");
                newPivot.print("new pivot at intersection of helix with plane");
                // pln.print("target");
                // plnLocal.print("transformed to local coordinates");
                newHelix.print("new helix after pivot transform");
                dphi = hpi.planeIntersect(newHelixPivoted, newPivot, localAlpha, plnLocal);
                System.out.format("    New delta-phi=%13.10f; should be zero!\n", dphi);
                Vec newPoint = atPhi(newPivot, newHelixPivoted, dphi, localAlpha);
                newPoint.print("new point of intersection, should be same as the old");
                Cov.print("covariance before transform");
            }
            SquareMatrix F = makeF(newHelixPivoted, newHelix, localAlpha);
            newHelix = newHelixPivoted;

            // Rotate the helix into the field system at the new origin
            Origin = RM.inverseRotate(newPivot).sum(Origin); // Make a new coordinate system with origin in global coords at the intersection point
            Vec Bfield = null;
            if (step == stepPnts.size()-1) {   // At the last step define the local field at the point provided by the calling routine
                Bfield = KalmanInterface.getField(newOrigin, fM);
            } else {
                Bfield = KalmanInterface.getField(Origin, fM);
            }     
            double Bmag = Bfield.mag();
            Vec tB = Bfield.unitVec(Bmag); // Local field at the new origin
            Vec uB = yhat.cross(tB).unitVec();
            Vec vB = tB.cross(uB);
            RotMatrix RMnew = new RotMatrix(uB, vB, tB);
            localAlpha = 1.0e12 / c / Bmag;
            RotMatrix deltaRM = RMnew.multiply(RM.invert());    // New coordinate system is rotated to align with local field
            newHelix = rotateHelix(newHelix, deltaRM, fRot);    // Rotate the helix into the new local field coordinate system
            SquareMatrix Ft = F.multiply(fRot);              
            Cov = Cov.similarity(Ft);                           // Here we propagate the covariance matrix
            // Add in multiple scattering if we are here passing through a plane with material
            SquareMatrix Q = null;
            if (thisXL) {                
                Vec pMom = getMom(0.,newHelix);
                double momentum = pMom.mag();
                double ct = pMom.v[1]/momentum;
                double sigmaMS = projMSangle(momentum, XL/ct);
                Q = this.getQ(sigmaMS);
                Cov = Cov.sum(Q);
            }

            // In the next step the pivot will be right at the origin of the local system, except for the last step
            Pivot.v[0] = 0.;
            Pivot.v[1] = 0.;
            Pivot.v[2] = 0.;
            if (verbose) {
                System.out.format("  helixStepper after step %d, B=%10.7f", step, Bmag);
                RMnew.print("new rotation to field frame");
                deltaRM.print("rotation to field system");
                Origin.print("intermediate origin in global system");
                newHelix.print("new helix after rotation");
                Ft.print("transform matrix");
                Cov.print("covariance after rotation");
                if (Q != null) Q.print("multiple scattering matrix");
            }
            RM = RMnew;
        }
        // Finally, move the pivot to the requested point in the final plane
        Vec newOriginLocal = new Vec(0.,0.,0.);
        Vec oldPivot = RM.rotate(Origin.dif(newOrigin));
        Vec finalHx = pivotTransform(newOriginLocal, newHelix, oldPivot, localAlpha, 0.);
        SquareMatrix F = makeF(finalHx, newHelix, localAlpha);
        Cov = Cov.similarity(F);
        if (verbose) {
            Origin.print("helixStepper final origin and pivot on helix");
            oldPivot.print("  final origin in final local coordinate system");
            RM.print("helixStepper final rotation to field system");
            newOriginLocal.print("final origin on helix in local coordinates");
            newHelix.print("helix before pivot transform");
            finalHx.print("final helix");
            Cov.print("transformed covariance");
        }
        for (int i = 0; i < 5; ++i) { 
            finalHelix.v[i] = finalHx.v[i];
            for (int j = 0; j < 5; ++j) { 
                Covariance.M[i][j] = Cov.M[i][j]; 
            } 
        }
        return true;
    }
    
    // Transform a helix from one pivot to another through a non-uniform B field in several steps
    // Deprecated original version without scattering planes
    Vec helixStepper(int nSteps, SquareMatrix Covariance, Vec newOrigin, org.lcsim.geometry.FieldMap fM) {
        double maxStep = Math.abs(newOrigin.v[1]-origin.v[1])/(double)nSteps;
        ArrayList<Double> yScats = new ArrayList<Double>();
        Vec transHelix = new Vec(5);
        double XL = 0.;
        helixStepper(maxStep, yScats, XL, Covariance, transHelix, newOrigin, fM);
        return transHelix;
    }

    // Comparator function for sorting pairs in helixStepper by y
    static Comparator<Pair<Double,Boolean>> pairComparator = new Comparator<Pair<Double,Boolean>>() {
        public int compare(Pair<Double,Boolean> p1, Pair<Double,Boolean> p2) {
            if (p1.getFirstElement() < p2.getFirstElement()) {
                return -1;
            } else {
                return +1;
            }
        }
    };

    // Multiple scattering matrix; assume a single thin scattering layer at the
    // beginning of the helix propagation
    SquareMatrix getQ(double sigmaMS) {
        double[][] q = new double[5][5];

        double V = sigmaMS * sigmaMS;
        q[1][1] = V * (1.0 + a.v[4] * a.v[4]);
        q[4][4] = V * (1.0 + a.v[4] * a.v[4]) * (1.0 + a.v[4] * a.v[4]);
        // All other elements are zero

        return new SquareMatrix(5, q);
    }
}
