package org.hps.recon.tracking.kalman;

import java.util.Random;

// Runge-Kutta propagation through the detector, including Gaussian MCS at silicon planes.
// This code is only to help with internal testing of the Kalman package and is not part of the fitting or pattern recognition.
class RKhelix {

    Vec x;    // point on the track
    Vec p;    // momentum of the track at point x
    double Q; // charge of the particle
    
    private org.lcsim.geometry.FieldMap fM;
    private Random rndm;
    private HelixPlaneIntersect hpi;
    private double rho;
    private double radLen;
    
    RKhelix(Vec x, Vec p, double Q, org.lcsim.geometry.FieldMap fM, Random rndm) {
        this.rndm = rndm;
        this.fM = fM;
        this.x = x;
        this.p = p;
        this.Q = Q;
        hpi = new HelixPlaneIntersect();
        rho = 2.329; // Density of silicon in g/cm^2
        radLen = (21.82 / rho) * 10.0; // Radiation length of silicon in millimeters
    }
    
    RKhelix propagateRK(Plane pln) {
        Vec newP = new Vec(3);
        Vec newX = planeIntersect(pln, newP);
        return new RKhelix(newX, newP, Q, fM, rndm);
    }
    
    Vec planeIntersect(Plane pln, Vec pInt) { // phi value where the helix intersects the plane P (given in global coordinates)        
        return hpi.rkIntersect(pln, x, p, Q, fM, pInt);
    }

    void print(String s) {
        System.out.format("RKhelix %s: x=%s, p=%s, Q=%3.1f\n", s, x.toString(), p.toString(), Q);
    }
    
    // Get parameters for the helix passing through the point x.
    // pivotF is the pivot point in the helix field reference system.
    // Input "pivot", the desired pivot point in global coordinates. This will be the origin of the field reference system.
    Vec helixParameters(Vec pivot, Vec pivotF) {
        Vec B = KalmanInterface.getField(pivot, fM);
        double Bmag = B.mag();
        double alpha = 1.0e12 / (2.99793e8 * Bmag);
        RotMatrix Rot = R(pivot);
        // Transform the momentum into the field frame
        Vec pF = Rot.rotate(p);
        Vec helixAtX = HelixState.pTOa(pF, 0., 0., Q);   // Helix with pivot at x in field frame
        // Transform the desired pivot point into the field frame
        Vec pivotTrans = Rot.rotate(pivot.dif(x));
        for (int i=0; i<3; ++i) {
            pivotF.v[i] = pivotTrans.v[i];
        }
        return HelixState.pivotTransform(pivotF, helixAtX, new Vec(0., 0., 0.), alpha, 0.);
    }
    
    RKhelix copy() {        
        return new RKhelix(x.copy(),p.copy(),Q,fM,rndm);
    }
    
    RotMatrix R(Vec position) {
        Vec B = KalmanInterface.getField(position, fM);
        double Bmag = B.mag();
        Vec t = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(t).unitVec();
        Vec v = t.cross(u);
        return new RotMatrix(u, v, t);
    }
    
    RKhelix randomScat(Plane P, double X) { // Produce a new helix scattered randomly in a given plane P

        Vec t = p.unitVec();
        Vec zhat = new Vec(0., 0., 1.);
        Vec uhat = t.cross(zhat).unitVec(); // A unit vector u perpendicular to the momentum
        Vec vhat = t.cross(uhat);
        RotMatrix Rp = new RotMatrix(uhat, vhat, t);
        double ct = Math.abs(P.T().dot(t));
        double theta0;
        
        if (X == 0.) theta0 = 0.;  // Get the scattering angle
        else theta0 = Math.sqrt((X / radLen) / ct) * (0.0136 / p.mag()) * (1.0 + 0.038 * Math.log((X / radLen) / ct));
        double thetaX = rndm.nextGaussian() * theta0;
        double thetaY = rndm.nextGaussian() * theta0;
        double tx = Math.sin(thetaX);
        double ty = Math.sin(thetaY);
        Vec tLoc = new Vec(tx, ty, Math.sqrt(1.0 - tx * tx - ty * ty));
        Vec tnew = Rp.inverseRotate(tLoc);

        double E = p.mag(); // Everything is assumed electron
        double sp = 0.0;    // 0.002; // Estar collision stopping power for electrons in silicon at about a GeV, in GeV cm2/g
        double dEdx = 0.1 * sp * rho; // in GeV/mm
        double eLoss = dEdx * X / ct;
        E = E - eLoss;
        Vec pNew = tnew.scale(E);

        return new RKhelix(x, pNew, Q, fM, rndm); // Create the new helix with new origin and pivot point
    }

}
