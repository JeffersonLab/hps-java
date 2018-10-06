package org.hps.util;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.geometry.FieldMap;
import static org.lcsim.constants.Constants.fieldConversion;

public class RK4integrator {
    private double h;
    private double h2;
    private double alpha;
    private FieldMap fM;
    
    public RK4integrator(double Q, double dx, FieldMap fM) {
        alpha = Q * fieldConversion; // Q is the charge in units of the proton charge
        h = dx; // Step size in mm
        h2 = h / 2.0;
        this.fM = fM; // Magnetic field map
    }
    
    public void setQ(int input) {
        setAlpha(input * fieldConversion);
    }
    
    public void setAlpha(double input) {
        alpha = input;
    }
    
    public void setFieldmap(FieldMap input) {
        fM = input;
    }
    
    public void setStepSize(double input) {
        h = input;
        h2 = h / 2.0;
    }
    
    public Hep3Vector integrationPosition(Hep3Vector r0, Hep3Vector p0, double s) {
        return integrate(r0, p0, s).getFirstElement();
    }
    
    public Hep3Vector integrationMomentum(Hep3Vector r0, Hep3Vector p0, double s) {
        return integrate(r0, p0, s).getFirstElement();
    }
    
    public Pair<Hep3Vector, Hep3Vector> integrate(Hep3Vector r0, Hep3Vector p0, double s) {
        // r0 is the initial point in mm
        // p0 is the initial momentum in GeV/c
        // s is the distance to propagate (approximate to distance dx)
        Hep3Vector r = new BasicHep3Vector(r0.v());
        Hep3Vector p = new BasicHep3Vector(p0.v());

        int nStep = (int) (s / h) + 1;
        for (int step = 0; step < nStep; step++) {

            Pair<Hep3Vector, Hep3Vector> k1 = f(r, p);
            Hep3Vector r1 = VecOp.add(r, VecOp.mult(h2, k1.getFirstElement()));
            Hep3Vector p1 = VecOp.add(p, VecOp.mult(h2, k1.getSecondElement()));
            Pair<Hep3Vector, Hep3Vector> k2 = f(r1, p1);
            Hep3Vector r2 = VecOp.add(r, VecOp.mult(h2, k2.getFirstElement()));;
            Hep3Vector p2 = VecOp.add(p, VecOp.mult(h2, k2.getSecondElement()));
            Pair<Hep3Vector, Hep3Vector> k3 = f(r2, p2);
            Hep3Vector r3 = VecOp.add(r, VecOp.mult(h, k3.getFirstElement()));;
            Hep3Vector p3 = VecOp.add(p, VecOp.mult(h, k3.getSecondElement()));
            Pair<Hep3Vector, Hep3Vector> k4 = f(r3, p3);
            
            Hep3Vector rtemp = VecOp.add(VecOp.mult(1.0/6.0, k1.getFirstElement()), VecOp.mult(1.0/3.0, k2.getFirstElement()));
            rtemp = VecOp.add(rtemp, VecOp.mult(1.0/3.0, k3.getFirstElement()));
            rtemp = VecOp.add(rtemp, VecOp.mult(1.0/6.0, k4.getFirstElement()));
            rtemp = VecOp.mult(h, rtemp);
            r = VecOp.add(r, rtemp);
            
            Hep3Vector ptemp = VecOp.add(VecOp.mult(1.0/6.0, k1.getSecondElement()), VecOp.mult(1.0/3.0, k2.getSecondElement()));
            ptemp = VecOp.add(ptemp, VecOp.mult(1.0/3.0, k3.getSecondElement()));
            ptemp = VecOp.add(ptemp, VecOp.mult(1.0/6.0, k4.getSecondElement()));
            ptemp = VecOp.mult(h, ptemp);
            p = VecOp.add(p, ptemp);

        }

        return new Pair<Hep3Vector, Hep3Vector> (r,p);
    }
    
    private Pair<Hep3Vector, Hep3Vector> f(Hep3Vector x, Hep3Vector p) { // Return all the derivatives

        Hep3Vector pNorm = VecOp.mult(1.0 / p.magnitude(), p);
        Hep3Vector d = VecOp.mult(alpha, VecOp.cross(pNorm, fM.getField(x)));

        return new Pair<Hep3Vector, Hep3Vector> (pNorm, d);
    }

}
