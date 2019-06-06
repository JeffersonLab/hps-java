package org.hps.recon.tracking.ztrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.hps.util.Pair;
import org.hps.util.RK4integrator;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.TrackSurfaceDirection;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfzp.PropZZRK;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class ExtrapolatorTest extends TestCase {

    double bX = 0.0;
    double bY = -0.24;
    double bZ = 0.0;
    double[] zPlanes = {
        87.921,
        96.106,
        187.92,
        196.11,
        288.04,
        296.03,
        488.23,
        495.70,
        688.20,
        695.89,
        888.54,
        895.98
    };
    double q = -1;
    Hep3Vector pos = new BasicHep3Vector(-0.29023, 0.37964, 0.0000);
    Hep3Vector mom = new BasicHep3Vector(0.019211, -0.076617, 2.2437);
    double x = pos.x();
    double y = pos.y();
    double z = pos.z();
    double px = mom.x();
    double py = mom.y();
    double pz = mom.z();
    double p = mom.magnitude();
    double tx = px / pz;
    double ty = py / pz;
    double qP = q / p;
    ConstantMagneticField zTrackField = new ConstantMagneticField(bX, bY, bZ);
    org.lcsim.recon.tracking.magfield.ConstantMagneticField trfField = new org.lcsim.recon.tracking.magfield.ConstantMagneticField(bX, bY, bZ);
    FieldMap hpsField = new HpsConstantMagneticField(bX, bY, bZ);
    // zTrack stuff
    ZTrackParam zTrackParam;
    ZTrackParam zTrackParOut = new ZTrackParam();
    RK4TrackExtrapolator extrap;

    // trf stuff
    PropZZRK prop;
    List<SurfZPlane> planes;
    TrackVector vec1;
    PropDir trfDir;
    VTrack trfv0;

    //hps stuff
    Hep3Vector hpscurrentPosition;
    Hep3Vector hpscurrentMomentum;

    public void testIt() {
        ztrackConst();
        trfConst();
        hpsConst();
    }

    public void testAll() {
        //TODO insert some assertions to test agreement.
        testIt();
        for (int i = 0; i < zPlanes.length; ++i) {
            //ztrack
            extrap.Extrapolate(zTrackParam, zTrackParOut, zPlanes[i], null);
            System.out.println("ztrack extrap to z= " + zTrackParOut.GetZ() + " " + zTrackParOut.GetX() + " " + zTrackParOut.GetY() + " " + zTrackParOut.GetTx() + " " + zTrackParOut.GetTy() + " " + zTrackParOut.GetQp());
            //trf
            VTrack trfv1 = new VTrack(trfv0);
            PropStat pstat = prop.vecDirProp(trfv1, planes.get(i), trfDir);
//            System.out.println(" ending: " + trfv1);
//            System.out.println(pstat);
            SurfZPlane zp = (SurfZPlane) trfv1.surface();
            TrackVector trfTv = trfv1.vector();
            System.out.println("trf    extrap to z= " + 10.*zp.z() + " " + 10.*trfTv.get(0) + " " + 10.*trfTv.get(1) + " " + 10.*trfTv.get(2) + " " + 10.*trfTv.get(3) + " " + 10.*trfTv.get(4));

            //hps
            Hep3Vector hpsExtrap = extrapolateToZ(hpscurrentPosition, hpscurrentMomentum, q, zPlanes[i], false, hpsField);
            System.out.println("hps    extrap to z= " + hpsExtrap.z() + " " + hpsExtrap.x() + " " + hpsExtrap.y());
            System.out.println("");
        }

    }

    public void ztrackConst() {
        System.out.println("testing it!");

        zTrackParam = new ZTrackParam();
        zTrackParam.SetX(x);
        zTrackParam.SetY(y);
        zTrackParam.SetZ(z);
        zTrackParam.SetTx(tx);
        zTrackParam.SetTy(ty);
        zTrackParam.SetQp(qP);

        extrap = new RK4TrackExtrapolator(zTrackField);

//        for (int i = 0; i < zPlanes.length; ++i) {
//            //ztrack
//            extrap.Extrapolate(zTrackParam, zTrackParOut, zPlanes[i], null);
//            System.out.println("extrap to z= " + zPlanes[i] + " " + zTrackParOut.GetX() + " " + zTrackParOut.GetY() + " " + zTrackParOut.GetTx() + " " + zTrackParOut.GetTy() + " " + zTrackParOut.GetQp());
//        }
    }

    public void trfConst() {
        //NOTE that trf uses cm as default
        prop = new PropZZRK(trfField);
        planes = new ArrayList<SurfZPlane>();
        for (int i = 0; i < zPlanes.length; ++i) {
            planes.add(new SurfZPlane(zPlanes[i] / 10.));
        }
        vec1 = new TrackVector();
        vec1.set(0, x / 10.);    // x
        vec1.set(1, y / 10.);    // y
        vec1.set(2, tx);   // dx/dz
        vec1.set(3, ty);   // dy/dz
        vec1.set(4, qP);   // q/p
        // create a VTrack at the origin.

        SurfZPlane zp0 = new SurfZPlane(z);
        TrackSurfaceDirection tdir = TrackSurfaceDirection.TSD_FORWARD;
        trfv0 = new VTrack(zp0.newPureSurface(), vec1, tdir);

        VTrack trfv1 = new VTrack(trfv0);
//        System.out.println(" starting: " + trfv1);
        trfDir = PropDir.FORWARD;
//        for (int i = 0; i < zPlanes.length; ++i) {
//            PropStat pstat = prop.vecDirProp(trfv1, planes.get(i), trfDir);
//            System.out.println(" ending: " + trfv1);
//            System.out.println(pstat);
//        }
    }

    public void hpsConst() {

        hpscurrentPosition = new BasicHep3Vector(x, y, z);
        hpscurrentMomentum = new BasicHep3Vector(px, py, pz);

//        for (int i = 0; i < zPlanes.length; ++i) {
//            Hep3Vector extrap = extrapolateToZ(hpscurrentPosition, hpscurrentMomentum, q, zPlanes[i], false, hpsField);
//            System.out.println("extrap " + extrap);
//        }
    }

    public Hep3Vector extrapolateToZ(Hep3Vector currentPosition, Hep3Vector currentMomentum, double q, double z, boolean debug, FieldMap bFieldMap) {
        double distanceZ = z - currentPosition.z();
        double distance = distanceZ / VecOp.cosTheta(currentMomentum);
        double epsilon = 1;
        RK4integrator RKint = new RK4integrator(q, epsilon, bFieldMap);
        Pair<Hep3Vector, Hep3Vector> RKresults = RKint.integrate(currentPosition, currentMomentum, distance);
        // System.out.printf("RKpos %s \n", RKresults.getFirstElement().toString());
        Hep3Vector momExtrap = RKresults.getSecondElement();
        double dz = z - RKresults.getFirstElement().z();
        Hep3Vector delta = new BasicHep3Vector(dz * momExtrap.x() / momExtrap.z(), dz * momExtrap.y() / momExtrap.z(), dz);
        Hep3Vector finalPos = VecOp.add(delta, RKresults.getFirstElement());
        return finalPos;
    }

    class HpsConstantMagneticField implements FieldMap {

        double[] _field;
        Hep3Vector _fieldVec;

        public HpsConstantMagneticField(double bx, double by, double bz) {
            _field = new double[]{bx, by, bz};
            _fieldVec = new BasicHep3Vector(bx, by, bz);
        }

        /**
         * Get the field magnitude and direction at a particular point.
         *
         * @param position The position at which the field is requested
         * @param b The field (the object is passed by reference and set to the
         * correct field)
         */
        public void getField(double[] position, double[] b) {
            b[0] = _field[0];
            b[1] = _field[1];
            b[2] = _field[2];
        }

        /**
         * Get the field magnitude and direction at a particular point. This
         * method requires allocation of a new object on each call, and should
         * therefore not be used if it may be called many times.
         *
         * @param position The position at which the field is requested
         * @return The field.
         */
        public double[] getField(double[] position) {
            return _field;
        }

        /**
         * Get the field magnitude and direction at a particular point.
         *
         * @param position The position at which the field is requested.
         * @param field The field. If not <code>null</code> this is passed by
         * reference and set to the correct field
         * @return The field. This will be the same object passed as field,
         * unless field was <code>null</code>
         */
        public Hep3Vector getField(Hep3Vector position, BasicHep3Vector field) {
            return null;
        }

        /**
         * Get the field magnitude and direction at a particular point.
         * Equivalent to <code>getField(position,null)</code>.
         */
        public Hep3Vector getField(Hep3Vector position) {
            return _fieldVec;
        }
    }
}
