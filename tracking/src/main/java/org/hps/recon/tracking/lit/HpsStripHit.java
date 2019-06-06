package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsStripHit implements Comparable {

    private double _u;
    private double _du;
    private DetectorPlane _plane;

    public HpsStripHit(double u, double du, DetectorPlane plane) {
        _u = u;
        _du = du;
        _plane = plane;
    }

    public double u() {
        return _u;
    }

    public double du() {
        return _du;
    }

    public double phi() {
        return _plane.phi();
    }

    public DetectorPlane plane() {
        return _plane;
    }

    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        DetectorPlane that = ((HpsStripHit) o).plane();
        return _plane.compareTo(that);
    }

    public String toString() {
        return "HpsStripHit : " + _u + " " + _du + " " + _plane.name() + " z0=" + _plane.position().z();
    }

}
