package org.hps.recon.tracking.kalman;

class Plane { // Description of a 2D plane in 3D space

    private Vec x; // A point in the plane, at the origin of the local plane's coordinate system
    private Vec t; // Unit vector perpendicular to the plane
    private Vec u; // Local axis always lies in the world system x,y plane
    private Vec v; // Other local axis, t,u,v forms a RH system

    Plane(Vec point, Vec direction) {
        x = point.copy();
        t = direction.copy();
        Vec zhat = new Vec(0., 0., 1.);
        u = t.cross(zhat).unitVec();
        v = t.cross(u);
    }

    Plane copy() {
        return new Plane(x, t);
    }

    Plane(Vec newX, Vec newT, Vec newU, Vec newV) {
        x = newX;
        t = newT;
        u = newU;
        v = newV;
    }
    
    Plane(Vec X, Vec T, Vec U) {
        x = X;
        t = T;
        u = U;
        v = t.cross(u);        
    }
    
    // strange constructor for backward compatibility of SiModule.java
    Plane(Vec X, Vec T, double theta) {
        x = X;
        Vec zhat = new Vec(0., 0., 1.);
        t = T.copy();
        u = t.cross(zhat).unitVec();
        v = t.cross(u);
        RotMatrix R1 = new RotMatrix(u,v,t);
        RotMatrix R2 = new RotMatrix(theta);
        RotMatrix Rt = R2.multiply(R1);
        for (int i=0; i<3; i++) {
            u.v[i] = Rt.M[0][i];
            v.v[i] = Rt.M[1][i];
            t.v[i] = Rt.M[2][i];
        }
    }

    void print(String s) {
        System.out.format("Printout of plane %s\n", s);
        x.print("       point=");
        t.print("       direction=");
        u.print("       uhat=");
        v.print("       vhat=");
    }

    Vec U() { // unit vector in the plane perpendicular to t and the world z axis
        return u;
    }

    Vec V() { // another orthogonal unit vector in the plane perp to t and u
        return v;
    }

    Vec T() {
        return t;
    }

    Vec X() {
        return x;
    }

    // Convert the plane to a local coordinate system
    Plane toLocal(RotMatrix R, Vec origin) {
        return new Plane(R.rotate(x.dif(origin)), R.rotate(t), R.rotate(u), R.rotate(v));
    }
}
