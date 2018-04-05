package kalman;

import java.util.ArrayList;
import java.util.Iterator;

// Description of a single silicon-strip module, and a container for its hits
public class SiModule {
    int Layer; // Tracker layer number, or -1 for a dummy layer added just for stepping in a non-uniform field
    ArrayList<Measurement> hits; // Hits ordered by coordinate value, from minimum to maximum
    Plane p; // Orientation and offset of the detector measurement plane in global
             // coordinates (NOT rotated by the stereo angle)
             // The offset should be the location of the center of the detector in global coordinates
    double[] xExtent; // Plus and minus limits on the detector active area in the x direction (along the strips)
    double[] yExtent; // Plus and minus limits on the detector active area in the y direction (perpendicular to the strips)
    RotMatrix R; // Rotation from the detector coordinates to global coordinates (not field coordinates)
    RotMatrix Rinv; // Rotation from global (not field) coordinates to detector coordinates (transpose of R)
    double stereo; // Stereo angle of the detectors in radians
    double thickness; // Silicon thickness in mm (should be 0 for a dummy layer!)
    FieldMap Bfield;

    public SiModule(int Layer, Plane p, double stereo, double width, double height, double thickness, FieldMap Bfield) {
        this.Layer = Layer;
        this.Bfield = Bfield;
        this.p = p;
        this.stereo = stereo;
        this.thickness = thickness;
        xExtent = new double[2];
        xExtent[0] = -width / 2.0;
        xExtent[1] = width / 2.0;
        yExtent = new double[2];
        yExtent[0] = -height / 2.0;
        yExtent[1] = height / 2.0;
        RotMatrix R1 = new RotMatrix(p.U(), p.V(), p.T()); // Rotation into the detector plane
        RotMatrix R2 = new RotMatrix(stereo); // Rotation by stereo angle in detector plane
        Rinv = R2.multiply(R1);
        R = Rinv.invert();
        hits = new ArrayList<Measurement>();
    }

    public void print(String s) {
        System.out.format("Si module %s, Layer=%2d, stereo angle=%8.4f, thickness=%8.4f mm, x extents=%10.6f %10.6f, y extents=%10.6f %10.6f\n", s, Layer,
                                        stereo, thickness, xExtent[0], xExtent[1], yExtent[0], yExtent[1]);
        p.X().print("origin of Si layer coordinates in the global system");
        R.print("from detector coordinates to global coordinates");
        System.out.format("List of measurements for Si module %s:\n", s);
        Iterator<Measurement> itr = hits.iterator();
        while (itr.hasNext()) {
            Measurement m = itr.next();
            m.print(" ");
            Vec Bf = Bfield.getField(m.rGlobal);
            Vec tBf = Bf.unitVec();
            System.out.format("            At the MC true location, B=%10.6f Tesla with direction = %10.7f %10.7f %10.7f\n", Bf.mag(), tBf.v[0], tBf.v[1],
                                            tBf.v[2]);
        }
    }

    public void addMeasurement(Measurement m) {
        if (hits.size() == 0)
            hits.add(m);
        else {
            boolean added = false;
            for (int i = hits.size() - 1; i >= 0; i--) { // Keep the measurements ordered by coordinate value
                if (m.v > hits.get(i).v) {
                    hits.add(i, m);
                    added = true;
                    break;
                }
            }
            if (!added)
                hits.add(m);
        }
    }

    Vec toGlobal(Vec vLocal) { // Convert a position vector from local detector coordinates to global
        return p.X().sum(R.rotate(vLocal));
    }

    Vec toLocal(Vec vGlobal) { // Convert a position vector from global coordinates to local detector
        return Rinv.rotate(vGlobal.dif(p.X()));
    }

}
