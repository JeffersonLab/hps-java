package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Iterator;

//import org.lcsim.geometry.FieldMap;

// Description of a single silicon-strip module, and a container for its hits
class SiModule {
    int Layer; // Tracker layer number, or a negative integer for a dummy layer added just for stepping in a
               // non-uniform field
    int detector; // Detector number within the layer
    ArrayList<Measurement> hits; // Hits ordered by coordinate value, from minimum to maximum
    Plane p; // Orientation and offset of the detector measurement plane in global coordinates 
             // The offset should be the location of the center of the detector in global
             // coordinates
    double[] xExtent; // Plus and minus limits on the detector active area in the x direction (along
                      // the strips)
    double[] yExtent; // Plus and minus limits on the detector active area in the y direction
                      // (perpendicular to the strips)
    RotMatrix R; // Rotation from the detector coordinates to global coordinates (not field coordinates)
    RotMatrix Rinv; // Rotation from global (not field) coordinates to detector coordinates (transpose of R)
                    // The local coordinate system is u, v, t where t is more-or-less the beam direction (y-global)
                    // and v is the measurement direction.
    double thickness; // Silicon thickness in mm (should be 0 for a dummy layer!)
    org.lcsim.geometry.FieldMap Bfield;
    boolean isStereo;

    boolean verbose = false;

    void setVerbose(boolean input) { 
        verbose = input;
    }
    SiModule(int Layer, Plane p, double stereo, double width, double height, double thickness, org.lcsim.geometry.FieldMap Bfield) {
        // for backwards-compatibility with old stand-alone development code: assume axial
        // layers have stereo angle=0
        this(Layer, p, stereo != 0.0, width, height, thickness, Bfield, 0);
    }

    SiModule(int Layer, Plane p, boolean isStereo, double width, double height, double thickness,
            org.lcsim.geometry.FieldMap Bfield) {
        this(Layer, p, isStereo, width, height, thickness, Bfield, 0);
    }

    SiModule(int Layer, Plane p, boolean isStereo, double width, double height, double thickness,
            org.lcsim.geometry.FieldMap Bfield, int detector) {
        
        if (verbose) { 
            System.out.format("SiModule constructor called with layer = %d, detector module = %d, y=%8.2f\n", Layer, detector, p.X().v[1]);
            p.print("of SiModule");
        }
        Vec BOnAxis = KalmanInterface.getField(new Vec(0.,p.X().v[1],0.), Bfield);
        Vec BatCenter = KalmanInterface.getField(p.X(), Bfield);
        if (verbose) {
            BOnAxis.print("B field on axis");
            BatCenter.print("B at detector center");
        }
        this.Layer = Layer;
        this.detector = detector;
        this.Bfield = Bfield;
        this.p = p;
        this.isStereo = isStereo;
        this.thickness = thickness;
        xExtent = new double[2];
        xExtent[0] = -width / 2.0;
        xExtent[1] = width / 2.0;
        yExtent = new double[2];
        yExtent[0] = -height / 2.0;
        yExtent[1] = height / 2.0;
        RotMatrix R1 = new RotMatrix(p.U(), p.V(), p.T());
        Rinv = R1; // This goes from global to local
        R = Rinv.invert(); // This goes from local to global
        hits = new ArrayList<Measurement>();
    }

    void print(String s) {
        System.out.format(
                "Si module %s, Layer=%2d, Detector=%2d, stereo=%b, thickness=%8.4f mm, x extents=%10.6f %10.6f, y extents=%10.6f %10.6f\n",
                s, Layer, detector, isStereo, thickness, xExtent[0], xExtent[1], yExtent[0], yExtent[1]);
        if (isStereo) {
            System.out.format("This is a stereo detector layer");
        } else {
            System.out.format("This is an axial detector layer");
        }
        p.X().print("origin of Si layer coordinates in the global system");
        Vec Bf = KalmanInterface.getField(p.X(), Bfield);
        Vec tBf = Bf.unitVec();
        System.out.format("      At this origin, B=%10.6f Tesla with direction = %10.7f %10.7f %10.7f\n",Bf.mag(),tBf.v[0],tBf.v[1],tBf.v[2]);
        R.print("from detector coordinates to global coordinates");
        System.out.format("List of measurements for Si module %s:\n", s);
        Iterator<Measurement> itr = hits.iterator();
        while (itr.hasNext()) {
            Measurement m = itr.next();
            m.print(" ");
        }
    }

    // Delete all the existing hits
    void reset() {
        hits = new ArrayList<Measurement>();
    }

    void addMeasurement(Measurement m) {
        if (hits.size() == 0) hits.add(m);
        else {
            boolean added = false;
            for (int i = hits.size() - 1; i >= 0; i--) { // Keep the measurements ordered by coordinate value
                if (m.v > hits.get(i).v) {
                    hits.add(i, m);
                    added = true;
                    break;
                }
            }
            if (!added) hits.add(m);
        }
    }

    Vec toGlobal(Vec vLocal) { // Convert a position vector from local detector coordinates to global
        return p.X().sum(R.rotate(vLocal));
    }

    Vec toLocal(Vec vGlobal) { // Convert a position vector from global coordinates to local detector
        return Rinv.rotate(vGlobal.dif(p.X()));
    }

}
