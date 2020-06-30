package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.lcsim.geometry.FieldMap;

// Description of a single silicon-strip module, and a container for its hits
class SiModule {
    int Layer; // Tracker layer number, or a negative integer for a dummy layer added just for stepping in a
               // non-uniform field
    int detector; // Detector number within the layer
    int millipedeID; // ID used by millipede for alignment
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

    private boolean verbose;
    private Logger logger;

    SiModule(int Layer, Plane p, double stereo, double width, double height, double thickness, org.lcsim.geometry.FieldMap Bfield) {
        // for backwards-compatibility with old stand-alone development code: assume axial
        // layers have stereo angle=0
        this(Layer, p, stereo != 0.0, width, height, thickness, Bfield, 0, 0);
    }

    SiModule(int Layer, Plane p, boolean isStereo, double width, double height, double thickness,
            org.lcsim.geometry.FieldMap Bfield) {
        this(Layer, p, isStereo, width, height, thickness, Bfield, 0, 0);
    }

    SiModule(int Layer, Plane p, boolean isStereo, double width, double height, double thickness,
            org.lcsim.geometry.FieldMap Bfield, int detector) {
        this(Layer, p, isStereo, width, height, thickness, Bfield, detector, 0);
    }

    SiModule(int Layer, Plane p, boolean isStereo, double width, double height, double thickness,
            org.lcsim.geometry.FieldMap Bfield, int detector, int millipedeID) {
        logger = Logger.getLogger(SiModule.class.getName());
        verbose = (logger.getLevel()==Level.FINE);
        this.millipedeID = millipedeID;
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
        System.out.format("%s",this.toString(s));
    }
    
    String toString(String s) {
        String str = String.format(
                "Si module %s, Layer=%2d, Detector=%2d, Millipede=%d, stereo=%b, thickness=%8.4f mm, x extents=%10.6f %10.6f, y extents=%10.6f %10.6f\n",
                s, Layer, detector, millipedeID, isStereo, thickness, xExtent[0], xExtent[1], yExtent[0], yExtent[1]);
        if (isStereo) {
            str = str + String.format("This is a stereo detector layer");
        } else {
            str = str + String.format("This is an axial detector layer");
        }
        str = str + p.X().toString("origin of Si layer coordinates in the global system");
        Vec Bf = KalmanInterface.getField(p.X(), Bfield);
        Vec tBf = Bf.unitVec();
        str = str + String.format("      At this origin, B=%10.6f Tesla with direction = %10.7f %10.7f %10.7f\n",Bf.mag(),tBf.v[0],tBf.v[1],tBf.v[2]);
        str = str + R.toString("from detector coordinates to global coordinates");
        str = str + String.format("List of measurements for Si module %s:\n", s);
        Iterator<Measurement> itr = hits.iterator();
        while (itr.hasNext()) {
            Measurement m = itr.next();
            str = str + m.toString(" ");
        }
        return str;
    }
    public String toString() {
        String str = String.format("Si Module: Lyr=%2d Det=%2d Mpd=%d stereo=%b pnt=%8.3f %8.3f %8.3f t=%7.3f %7.3f %7.3f\n",
                Layer, detector, millipedeID, isStereo, p.X().v[0], p.X().v[1], p.X().v[2], p.T().v[0], p.T().v[1], p.T().v[2]);
        return str;
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
