package org.hps.recon.tracking.kalman;

/**
 * Description of a 2D plane in 3D space
 */
class Plane { 

    private Vec x; // A point in the plane, at the origin of the local plane's coordinate system
    private Vec t; // Unit vector perpendicular to the plane
    private Vec u; // Local axis always lies in the world system x,y plane
    private Vec v; // Other local axis, t,u,v forms a RH system

    /**
     * Constructor
     * @param point         3-vector point on the plane
     * @param direction     3-vector direction cosines of the plane
     */
    Plane(Vec point, Vec direction) {
        x = point.copy();
        t = direction.copy();
        Vec zhat = new Vec(0., 0., 1.);
        u = t.cross(zhat).unitVec();
        v = t.cross(u);
    }

    /**
     * Copy method
     * @return    A new deep copy of the plane
     */
    Plane copy() {
        return new Plane(x, t);
    }

    /**
     * Alternative constructor
     * @param newX    3-vector point on the plane
     * @param newT    t unit vector perpendicular to the plane
     * @param newU    u unit vector in the plane and in the x,y plane
     * @param newV    v unit vector perpendicular to t and u (t,u,v is a RH system)
     */
    Plane(Vec newX, Vec newT, Vec newU, Vec newV) {
        x = newX;
        t = newT;
        u = newU;
        v = newV;
    }
    
    /**
     * Alternative constuctor
     * @param X        3-vector point on the plane
     * @param T        t unit vector perpendicular to the plane
     * @param U        u unit vector in the plane and in the x,y plane
     */
    Plane(Vec X, Vec T, Vec U) {
        x = X;
        t = T;
        u = U;
        v = t.cross(u);        
    }
    
    /**
     * strange constructor for backward compatibility of SiModule.java
     * @param X        3-vector point on the plane
     * @param T        t unit vector perpendicular to the plane
     * @param theta    angle used to rotate into the t,u,v system
     */
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

    /**
     * Debug printout of a plane instance
     * @param s     Arbitrary string for the user's reference
     */
    void print(String s) {
        System.out.format("%s",this.toString(s));
    }
    
    /**
     * Debug printout to a string of a plane instance
     * @param s     Arbitrary string for the user's reference
     */
    String toString(String s) {
        String str = String.format("Printout of plane %s\n", s);
        str=str+"       point="+x.toString();
        str=str+"       direction="+t.toString()+"\n";
        str=str+"       uhat="+u.toString();
        str=str+"       vhat="+v.toString()+"\n";
        return str;
    }
    
    /**
     * Short string describing the plane
     */
    public String toString() {
        return String.format("pnt %s dir %s", x.toString(), t.toString());
    }

    /**
     * Get u
     * @return    The unit vector in the plane perpendicular to t and the world z axis
     */
    Vec U() { 
        return u;
    }

    /**
     * Get v
     * @return    Another orthogonal unit vector in the plane perp to t and u
     */
    Vec V() { 
        return v;
    }

    /**
     * Get t
     * @return   The unit vector perpendicular to the plane
     */
    Vec T() {
        return t;
    }

    /**
     * Get x
     * @return   The point in the plane
     */
    Vec X() {
        return x;
    }

    /**
     * Convert the plane to a local coordinate system
     * @param R               Rotation matrix from global to local
     * @param origin          Origin of the local system
     * @return                The plane in the local system
     */
    Plane toLocal(RotMatrix R, Vec origin) {
        return new Plane(R.rotate(x.dif(origin)), R.rotate(t), R.rotate(u), R.rotate(v));
    }
}
