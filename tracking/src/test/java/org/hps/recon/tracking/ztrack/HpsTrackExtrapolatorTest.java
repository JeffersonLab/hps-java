package org.hps.recon.tracking.ztrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.lit.DetectorTrackPropagator;
import org.hps.util.Pair;
import org.hps.util.RK4integrator;
import org.lcsim.geometry.Detector;
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
 * @author ngraf
 */
public class HpsTrackExtrapolatorTest extends TestCase {

    FieldMap _bFieldMap;
    TrfField _trfField;
    HpsMagField _zTrackField;

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

    public void testIt() throws Exception {
        setupBfields();
        setupZTrack();
        setupTrf();
        setupHps();
        for (int i = 0; i < zPlanes.length; ++i) {
            //trf
            VTrack trfv1 = new VTrack(trfv0);
            PropStat pstat = prop.vecDirProp(trfv1, planes.get(i), trfDir);
//            System.out.println(" ending: " + trfv1);
//            System.out.println(pstat);
            SurfZPlane zp = (SurfZPlane) trfv1.surface();
            TrackVector trfTv = trfv1.vector();
            System.out.println("trf    extrap to z= " + 10. * zp.z() + " " + 10. * trfTv.get(0) + " " + 10. * trfTv.get(1) + " " + 10. * trfTv.get(2) + " " + 10. * trfTv.get(3) + " " + 10. * trfTv.get(4));

            //hps
            Hep3Vector hpsExtrap = extrapolateToZ(hpscurrentPosition, hpscurrentMomentum, q, zPlanes[i], false, _bFieldMap);
            System.out.println("hps    extrap to z= " + hpsExtrap.z() + " " + hpsExtrap.x() + " " + hpsExtrap.y());

            //ztrack
            ztrackExtrapolateToZ(zTrackParam, zTrackParOut, zPlanes[i]);
            System.out.println("ztrack extrap to z= " + zTrackParOut.GetZ() + " " + zTrackParOut.GetX() + " " + zTrackParOut.GetY() + " " + zTrackParOut.GetTx() + " " + zTrackParOut.GetTy() + " " + zTrackParOut.GetQp());

            System.out.println("");
        }
    }

    public void setupBfields() throws Exception {
        String hpsDetectorName = "HPS-EngRun2015-Nominal-v2-fieldmap";
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        _bFieldMap = det.getFieldMap();
        _trfField = new TrfField(_bFieldMap);
        _zTrackField = new HpsMagField(_bFieldMap);
    }

    public void setupZTrack() {
        zTrackParam = new ZTrackParam();
        zTrackParam.SetX(x);
        zTrackParam.SetY(y);
        zTrackParam.SetZ(z);
        zTrackParam.SetTx(tx);
        zTrackParam.SetTy(ty);
        zTrackParam.SetQp(qP);

        extrap = new RK4TrackExtrapolator(_zTrackField);
    }

    public void setupTrf() {
        //NOTE that trf uses cm as default
        prop = new PropZZRK(_trfField);
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
    }

    public void setupHps() {
        hpscurrentPosition = new BasicHep3Vector(x, y, z);
        hpscurrentMomentum = new BasicHep3Vector(px, py, pz);
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

    public void ztrackExtrapolateToZ(ZTrackParam zTrackParam, ZTrackParam zTrackParOut, double zOut) {
        double zIn = zTrackParam.GetZ();
        double dz = zOut - zIn;
        if (abs(dz) < DefaultSettings.MINIMUM_PROPAGATION_DISTANCE) {
            return;
        }
        boolean downstream = dz > 0;
        int numSteps = (int) (abs(dz) / DefaultSettings.MAXIMUM_NAVIGATION_DISTANCE);
        double stepSize;
        if (numSteps == 0) {
            stepSize = abs(dz);
        } else {
            stepSize = DetectorTrackPropagator.MAXIMUM_PROPAGATION_STEP_SIZE;
        }
        double zCurrent = zIn;
        // Loop over steps + one additional to get to zOut
        ZTrackParam zTrackParamCurrent = new ZTrackParam(zTrackParam);
        for (int iStep = 0; iStep < numSteps + 1; ++iStep) {
            if (zCurrent == zOut) {
                break;
            }
            // Update current z position
            if (iStep != numSteps) {
                zCurrent = (downstream) ? zCurrent + stepSize : zCurrent - stepSize;
            } else {
                zCurrent = zOut;
            }
            // extrapolate
            extrap.Extrapolate(zTrackParamCurrent, zTrackParOut, zCurrent, null);
            //update
            zTrackParamCurrent = new ZTrackParam(zTrackParOut);
        }

    }

}
