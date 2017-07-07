package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.solids.Inside;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * Extention of lcsim class to allow use of local classes. Finds scatter points
 * and magnitude from detector geometry directly.
 *
 * @author Per Hansson <phansson@slac.stanford.edu>
 */
public class MultipleScattering extends org.lcsim.recon.tracking.seedtracker.MultipleScattering {

    private boolean _fixTrackMomentum = false;
    private double _momentum = -99;//dummy
    private static final double inside_tolerance = 1.0;//tolerance for first (approximate) test of track intersection with sensor

    public MultipleScattering(MaterialManager materialmanager) {
        super(materialmanager);
    }

    /**
     * Override lcsim version and select material manager depending on object
     * type. This allows to use a local extension of the material manager in the
     * lcsim track fitting code.
     *
     * @param helix
     * @return a list of ScatterAngle.
     */
    @Override
    public List<ScatterAngle> FindScatters(HelicalTrackFit helix) {
        if (_debug) {
            System.out.printf("\n%s: FindScatters() for helix:\n%s\n", this.getClass().getSimpleName(), helix.toString());
        }

        if (MaterialSupervisor.class.isInstance(this._materialmanager)) {
            if (_debug) {
                System.out.printf("%s: use HPS scattering model", this.getClass().getSimpleName());
            }
            return this.FindHPSScatters(helix);
        } else {
            if (_debug) {
                System.out.printf("%s: use default lcsim material manager to find scatters\n", this.getClass().getSimpleName());
            }
            return super.FindScatters(helix);
        }
    }

    /**
     * Extra interface to keep a function returning the same type as the lcsim
     * version
     *
     * @param helix
     * @return a list of ScatterAngle.
     */
    private List<ScatterAngle> FindHPSScatters(HelicalTrackFit helix) {
        ScatterPoints scatterPoints = this.FindHPSScatterPoints(helix);
        return scatterPoints.getScatterAngleList();
    }

    /**
     * Find scatter points along helix using the local material manager
     *
     * @param helix
     * @return the points of scatter along the helix
     */
    public ScatterPoints FindHPSScatterPoints(HelicalTrackFit helix) {
        if (_debug) {
            System.out.printf("\n%s: FindHPSScatters() for helix:\n%s\n", this.getClass().getSimpleName(), helix.toString());
            System.out.printf("%s: momentum is p=%f,R=%f,B=%f \n", this.getClass().getSimpleName(), helix.p(Math.abs(_bfield)), helix.R(), _bfield);
        }
        //        MG TURN THIS OFF SO IT DOESN'T ABORT STRAIGHT TRACKS
        // Check that B Field is set
        if (_bfield == 0. && !_fixTrackMomentum) {
            throw new RuntimeException("B Field or fixed momentum must be set before calling FindScatters method");
        }

        // Create a new list to contain the mutliple scatters
        // List<ScatterAngle> scatters = new ArrayList<ScatterAngle>();
        ScatterPoints scatters = new ScatterPoints();

        MaterialSupervisor materialSupervisor = (MaterialSupervisor) this._materialmanager;

        List<ScatteringDetectorVolume> materialVols = materialSupervisor.getMaterialVolumes();

        if (_debug) {
            System.out.printf("%s: there are %d detector volumes in the model\n", this.getClass().getSimpleName(), materialVols.size());
        }

        for (ScatteringDetectorVolume vol : materialVols) {

            if (_debug) {
                System.out.printf("\n%s: found detector volume \"%s\"\n", this.getClass().getSimpleName(), vol.getName());
            }

            // find intersection pathpoint with helix
            Hep3Vector pos = getHelixIntersection(helix, vol);

            if (pos != null) {

                if (_debug) {
                    System.out.printf("%s: intersection position %s\n", this.getClass().getSimpleName(), pos.toString());
                }

                // find the track direction at the plane
                double s = HelixUtils.PathToXPlane(helix, pos.x(), 0., 0).get(0);

                if (_debug) {
                    System.out.printf("%s: path length %f\n", this.getClass().getSimpleName(), s);
                }

                Hep3Vector dir = HelixUtils.Direction(helix, s);

                if (_debug) {
                    System.out.printf("%s: track dir %s\n", this.getClass().getSimpleName(), dir.toString());
                }

                // Calculate the material the track will traverse
                double radlen = vol.getMaterialTraversedInRL(dir);

                if (_debug) {
                    System.out.printf("%s: material traversed: %f R.L. (%fmm) \n", this.getClass().getSimpleName(), radlen, vol.getMaterialTraversed(dir));
                }

                double p;
                if (_fixTrackMomentum) {
                    p = _momentum;
                } else {
                    p = helix.p(this._bfield);
                }
                double msangle = this.msangle(p, radlen);

                ScatterAngle scat = new ScatterAngle(s, msangle);

                if (_debug) {
                    System.out.printf("%s: scatter angle %f rad for p %f GeV at path length %f\n", this.getClass().getSimpleName(), scat.Angle(), p, scat.PathLen());
                }

                ScatterPoint scatterPoint = new ScatterPoint(vol.getDetectorElement(), scat);
                scatterPoint.setDirection(dir);
                scatterPoint.setPosition(pos);
                scatters.addPoint(scatterPoint);

            } else if (_debug) {
                System.out.printf("\n%s: helix did not intersect this volume \n", this.getClass().getSimpleName());
            }

        }

        // Sort the multiple scatters by their path length
        Collections.sort(scatters._points);

        if (_debug) {
            System.out.printf("\n%s: found %d scatters for this helix:\n", this.getClass().getSimpleName(), scatters.getPoints().size());
            System.out.printf("%s: %10s %10s\n", this.getClass().getSimpleName(), "s (mm)", "theta(rad)");
            for (ScatterPoint p : scatters.getPoints()) {
                System.out.printf("%s: %10.2f %10f\n", this.getClass().getSimpleName(), p.getScatterAngle().PathLen(), p.getScatterAngle().Angle());
            }
        }
        return scatters;
    }

    public Hep3Vector getHelixIntersection(HelicalTrackFit helix, ScatteringDetectorVolume plane) {

        if (SiStripPlane.class.isInstance(plane)) {
            return getHelixIntersection(helix, (SiStripPlane) plane);
        } else {
            throw new UnsupportedOperationException("This det volume type is not supported yet.");
        }
    }

    /*
     * Returns interception between helix and plane Uses the origin x posiution of the plane and
     * extrapolates linearly to find teh intersection If inside use an iterative "exact" way to
     * determine the final position
     */
    public Hep3Vector getHelixIntersection(HelicalTrackFit helix, SiStripPlane plane) {

        if (_debug) {
            System.out.printf("%s: calculate simple helix intercept\n", this.getClass().getSimpleName());
            System.out.printf("%s: StripSensorPlane:\n", this.getClass().getSimpleName());
            plane.print();
        }

        double s_origin = HelixUtils.PathToXPlane(helix, plane.origin().x(), 0., 0).get(0);

        if (Double.isNaN(s_origin)) {
            if (_debug) {
                System.out.printf("%s: could not extrapolate to XPlane, too large curvature: origin is at %s \n", this.getClass().getSimpleName(), plane.origin().toString());
            }
            return null;
        }

        Hep3Vector pos = HelixUtils.PointOnHelix(helix, s_origin);
        Hep3Vector direction = HelixUtils.Direction(helix, s_origin);

        if (_debug) {
            System.out.printf("%s: position at x=origin is %s with path length %f and direction %s\n", this.getClass().getSimpleName(), pos.toString(), s_origin, direction.toString());
        }

        // Use this approximate position to get a first estimate if the helix intercepted the plane
        // This is only because the real intercept position is an iterative procedure and we'd
        // like to avoid using it if possible
        // Consider the plane as pure x-plane i.e. no rotations
        // -> this is not very general, as it assumes that strips are (mostly) along y -> FIX
        // THIS!?
        // Transformation from tracking to detector frame
        Hep3Vector pos_det = VecOp.mult(VecOp.inverse(CoordinateTransformations.getMatrix()), pos);
        Hep3Vector direction_det = VecOp.mult(VecOp.inverse(CoordinateTransformations.getMatrix()), direction);

        if (_debug) {
            System.out.printf("%s: position in det frame %s and direction %s\n", this.getClass().getSimpleName(), pos_det.toString(), direction_det.toString());
        }

        // Transformation from detector frame to sensor frame
        Hep3Vector pos_sensor = plane.getSensor().getGeometry().getGlobalToLocal().transformed(pos_det);
        Hep3Vector direction_sensor = plane.getSensor().getGeometry().getGlobalToLocal().rotated(direction_det);

        if (_debug) {
            System.out.printf("%s: position in sensor frame %s and direction %s\n", this.getClass().getSimpleName(), pos_sensor.toString(), direction_sensor.toString());
        }

        // find step in w to cross sensor plane
        double delta_w = -1.0 * pos_sensor.z() / direction_sensor.z();

        // find the point where it crossed the plane
        Hep3Vector pos_int = VecOp.add(pos_sensor, VecOp.mult(delta_w, direction_sensor));
        Hep3Vector pos_int_det = plane.getSensor().getGeometry().getLocalToGlobal().transformed(pos_int);
        // find the intercept in the tracking frame
        Hep3Vector pos_int_trk = VecOp.mult(CoordinateTransformations.getMatrix(), pos_int_det);

        if (_debug) {
            System.out.printf("%s: take step %f to get intercept position in sensor frame %s (det: %s trk: %s)\n", this.getClass().getSimpleName(), delta_w, pos_int, pos_int_det.toString(), pos_int_trk.toString());
        }

        boolean isInside = true;
        if (Math.abs(pos_int.x()) > plane.getMeasuredDimension() / 2.0 + inside_tolerance) {
            if (_debug) {
                System.out.printf("%s: intercept is outside in u\n", this.getClass().getSimpleName());
            }
            isInside = false;
        }

        if (Math.abs(pos_int.y()) > plane.getUnmeasuredDimension() / 2.0 + inside_tolerance) {
            if (_debug) {
                System.out.printf("%s: intercept is outside in v\n", this.getClass().getSimpleName());
            }
            isInside = false;
        }

        // Check if it's inside sensor and module and if it contradicts the manual calculation
        // For now: trust manual calculation and output warning if it's outside BOTH sensor AND module 
        if (_debug) {
            // check if it's inside the sensor
            Inside result_inside = plane.getDetectorElement().getGeometry().getPhysicalVolume().getMotherLogicalVolume().getSolid().inside(pos_int);
            Inside result_inside_module = plane.getSensor().getGeometry().getDetectorElement().getParent().getGeometry().inside(pos_int_det);
            System.out.printf("%s: Inside result sensor: %s module: %s\n", this.getClass().getSimpleName(), result_inside.toString(), result_inside_module.toString());

            boolean isInsideSolid = false;
            if (result_inside.equals(Inside.INSIDE) || result_inside.equals(Inside.SURFACE)) {
                isInsideSolid = true;
            }

            boolean isInsideSolidModule = false;
            if (result_inside_module.equals(Inside.INSIDE) || result_inside_module.equals(Inside.SURFACE)) {
                isInsideSolidModule = true;
            }

            if (isInside && !isInsideSolid) {
                System.out.printf("%s: manual calculation says inside sensor, inside solid says outside -> contradiction \n", this.getClass().getSimpleName());
                if (isInsideSolidModule) {
                    System.out.printf("%s: this intercept is outside sensor but inside module\n", this.getClass().getSimpleName());
                } else {
                    System.out.printf("%s: warning: this intercept at %s, in sensor frame %s, (sensor origin at %s ) is outside sensor and module!\n", this.getClass().getSimpleName(), pos_int_trk.toString(), pos_int.toString(), plane.origin().toString());
                }
            }
        }

        if (!isInside) {
            return null;
        }

        if (_debug) {
            System.out.printf("%s: found simple intercept at %s \n", this.getClass().getSimpleName(), pos_int_trk.toString());
        }

        // TODO Catch special cases where the incidental iteration procedure seems to fail 
        if (Math.abs(helix.R()) < 2000 && Math.abs(helix.dca()) > 10.0) {
            if (_debug) {
                System.out.printf("%s: momentum is low (p=%f,R=%f,B=%f) and d0 is big (d0=%f), skip the iterative calculation\n", this.getClass().getSimpleName(), helix.p(Math.abs(_bfield)), helix.R(), _bfield, helix.dca());
            }
            return pos_int_trk;
        }

        if (_debug) {
            System.out.printf("%s: calculate iterative helix intercept\n", this.getClass().getSimpleName());
        }

        Hep3Vector pos_iter_trk = TrackUtils.getHelixPlaneIntercept(helix, plane.normal(), plane.origin(), _bfield, s_origin);

        if (pos_iter_trk == null) {
            System.out.printf("%s: iterative intercept failed for helix \n%s\n at sensor with org=%s, unit w=%s\n", this.getClass().getSimpleName(), helix.toString(), plane.origin().toString(), plane.normal().toString());
            System.out.printf("%s: => use simple intercept pos=%s\n", this.getClass().getSimpleName(), pos_int_trk);
            System.out.printf("helix pos=%s dir=%s\n", pos, direction);
            return pos_int_trk;
        }

        if (_debug) {
            //        if (VecOp.sub(pos_iter_trk, pos_int_trk).magnitude()>1e-4)
            System.out.printf("%s: iterative helix intercept point at %s (diff to approx: %s) \n", this.getClass().getSimpleName(), pos_iter_trk.toString(), VecOp.sub(pos_iter_trk, pos_int_trk).toString());
        }

        // find position in sensor frame
        Hep3Vector pos_iter_sensor = plane.getSensor().getGeometry().getGlobalToLocal().transformed(VecOp.mult(VecOp.inverse(CoordinateTransformations.getMatrix()), pos_iter_trk));

        if (_debug) {
            System.out.printf("%s: found iterative helix intercept in sensor coordinates at %s\n", this.getClass().getSimpleName(), pos_iter_sensor.toString());
        }

        isInside = true;
        if (Math.abs(pos_iter_sensor.x()) > plane.getMeasuredDimension() / 2.0) {
            if (this._debug) {
                System.out.printf("%s: intercept is outside in u\n", this.getClass().getSimpleName());
            }
            isInside = false;
        }

        if (Math.abs(pos_iter_sensor.y()) > plane.getUnmeasuredDimension() / 2.0) {
            if (this._debug) {
                System.out.printf("%s: intercept is outside in v\n", this.getClass().getSimpleName());
            }
            isInside = false;
        }

        if (_debug) {
            Hep3Vector pos_iter_det = VecOp.mult(VecOp.inverse(CoordinateTransformations.getMatrix()), pos_iter_trk);
            Inside result_inside = plane.getDetectorElement().getGeometry().getPhysicalVolume().getMotherLogicalVolume().getSolid().inside(pos_iter_sensor);
            Inside result_inside_module = plane.getSensor().getGeometry().getDetectorElement().getParent().getGeometry().inside(pos_iter_det);
            System.out.printf("%s: Inside result sensor: %s module: %s\n", this.getClass().getSimpleName(), result_inside.toString(), result_inside_module.toString());

            boolean isInsideSolid = false;
            if (result_inside.equals(Inside.INSIDE) || result_inside.equals(Inside.SURFACE)) {
                isInsideSolid = true;
            }

            boolean isInsideSolidModule = false;
            if (result_inside_module.equals(Inside.INSIDE) || result_inside_module.equals(Inside.SURFACE)) {
                isInsideSolidModule = true;
            }

            // Check if it's inside sensor and module and if it contradicts the manual calculation
            // For now: trust manual calculation and output warning if it's outside BOTH sensor AND
            // module -> FIX THIS!?
            if (isInside && !isInsideSolid) {
                System.out.printf("%s: manual iterative calculation says inside sensor, inside solid says outside -> contradiction \n", this.getClass().getSimpleName());
                if (isInsideSolidModule) {
                    System.out.printf("%s: this iterative intercept is outside sensor but inside module\n", this.getClass().getSimpleName());
                } else {
                    System.out.printf("%s: warning: this iterative intercept %s, sensor frame %s, (sensor origin %s ) is outside sensor and module!\n", this.getClass().getSimpleName(), pos_iter_trk.toString(), pos_iter_sensor.toString(), plane.origin().toString());
                }
            }
        }

        if (!isInside) {
            return null;
        }

        if (_debug) {
            System.out.printf("%s: found intercept at %s \n", this.getClass().getSimpleName(), pos_iter_trk.toString());
        }

        return pos_iter_trk;
    }

    @Override
    public void setDebug(boolean debug) {
        _debug = debug;
    }

    public MaterialManager getMaterialManager() {
        // Should be safe to cast here
        return (MaterialManager) _materialmanager;
    }

    public void fixTrackMomentum(double mom) {
        _fixTrackMomentum = true;
        _momentum = mom;
    }

    /**
     * Nested class to encapsulate the scatter angles and which detector element
     * it is related to
     */
    public static class ScatterPoint implements Comparable<ScatterPoint> {

        IDetectorElement _det;
        ScatterAngle _scatterAngle;
        private Hep3Vector trkpos;
        private Hep3Vector dir;

        public ScatterPoint(IDetectorElement det, ScatterAngle scatterAngle) {
            _det = det;
            _scatterAngle = scatterAngle;
        }

        public Hep3Vector getPosition() {
            return trkpos;
        }

        public Hep3Vector getDirection() {
            return dir;
        }

        public void setPosition(Hep3Vector input) {
            trkpos = input;
        }

        public void setDirection(Hep3Vector input) {
            dir = input;
        }

        public IDetectorElement getDet() {
            return _det;
        }

        public ScatterAngle getScatterAngle() {
            return _scatterAngle;
        }

        @Override
        public int compareTo(ScatterPoint p) {
            return p.getScatterAngle().PathLen() > this._scatterAngle.PathLen() ? -1 : 1;
        }
    }

    /**
     * Nested class to encapsulate a list of scatters
     *
     */
    public class ScatterPoints {

        private List<ScatterPoint> _points;

        public ScatterPoints(List<ScatterPoint> _points) {
            this._points = _points;
        }

        private ScatterPoints() {
            _points = new ArrayList<ScatterPoint>();
        }

        public List<ScatterPoint> getPoints() {
            return _points;
        }

        public void addPoint(ScatterPoint point) {
            _points.add(point);
        }

        private List<ScatterAngle> getScatterAngleList() {
            List<ScatterAngle> scatters = new ArrayList<ScatterAngle>();
            for (ScatterPoint p : _points) {
                scatters.add(p._scatterAngle);
            }
            return scatters;
        }

        public ScatterPoint getScatterPoint(IDetectorElement detectorElement) {
            for (ScatterPoint p : _points) {
                if (p.getDet().equals(detectorElement)) {
                    return p;
                }
            }
            return null;
        }

    }

}
