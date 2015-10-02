package org.hps.recon.tracking.gbl;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.constants.Constants;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * A class providing various utilities related to GBL
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class GblUtils {

    private GblUtils() {
    }

    public static BasicMatrix gblSimpleJacobianLambdaPhi(double ds, double cosl, double bfac) {
        /*
         Simple jacobian: quadratic in arc length difference.
         using lambda phi as directions
	    
         @param ds: arc length difference
         @type ds: float
         @param cosl: cos(lambda)
         @type cosl: float
         @param bfac: Bz*c
         @type bfac: float
         @return: jacobian to move by 'ds' on trajectory
         @rtype: matrix(float)
         ajac(1,1)= 1.0D0
         ajac(2,2)= 1.0D0
         ajac(3,1)=-DBLE(bfac*ds)
         ajac(3,3)= 1.0D0
         ajac(4,1)=-DBLE(0.5*bfac*ds*ds*cosl)
         ajac(4,3)= DBLE(ds*cosl)
         ajac(4,4)= 1.0D0
         ajac(5,2)= DBLE(ds)
         ajac(5,5)= 1.0D0
         '''
         jac = np.eye(5)
         jac[2, 0] = -bfac * ds
         jac[3, 0] = -0.5 * bfac * ds * ds * cosl
         jac[3, 2] = ds * cosl
         jac[4, 1] = ds  
         return jac
         */
        BasicMatrix mat = unitMatrix(5, 5);
        mat.setElement(2, 0, -bfac * ds);
        mat.setElement(3, 0, -0.5 * bfac * ds * ds * cosl);
        mat.setElement(3, 2, ds * cosl);
        mat.setElement(4, 1, ds);
        return mat;
    }

    public static BasicMatrix unitMatrix(int rows, int cols) {
        BasicMatrix mat = new BasicMatrix(rows, cols);
        for (int row = 0; row != mat.getNRows(); row++) {
            for (int col = 0; col != mat.getNColumns(); col++) {
                if (row != col) {
                    mat.setElement(row, col, 0);
                } else {
                    mat.setElement(row, col, 1);
                }
            }
        }
        return mat;
    }

    public static BasicMatrix zeroMatrix(int rows, int cols) {
        BasicMatrix mat = new BasicMatrix(rows, cols);
        for (int row = 0; row != mat.getNRows(); row++) {
            for (int col = 0; col != mat.getNColumns(); col++) {
                mat.setElement(row, col, 0.);
            }
        }
        return mat;
    }

    public static double estimateScatter(IDetectorElement hitElement, HelicalTrackFit htf, MultipleScattering scattering, double _B) {
        //can be edge case where helix is outside, but close to sensor, so make a new scatter point assuming the helix does pass through the sensor
        MaterialSupervisor.DetectorPlane hitPlane = null;
        if (MaterialSupervisor.class.isInstance(scattering.getMaterialManager())) {
            MaterialSupervisor matSup = (MaterialSupervisor) scattering.getMaterialManager();
            for (MaterialSupervisor.ScatteringDetectorVolume vol : matSup.getMaterialVolumes()) {
                if (vol.getDetectorElement() == hitElement) {
                    hitPlane = (MaterialSupervisor.DetectorPlane) vol;
                    break;
                }
            }
            if (hitPlane == null) {
                throw new RuntimeException("cannot find plane for hit!");
            } else {
                // find scatterlength
                double s_closest = HelixUtils.PathToXPlane(htf, hitPlane.origin().x(), 0., 0).get(0);
                double X0 = hitPlane.getMaterialTraversedInRL(HelixUtils.Direction(htf, s_closest));
                ScatterAngle scatterAngle = new ScatterAngle(s_closest, scattering.msangle(htf.p(Math.abs(_B)), X0));
                return scatterAngle.Angle();
            }
        } else {
            throw new UnsupportedOperationException("Should not happen. This problem is only solved with the MaterialSupervisor.");
        }
    }

    public static FittedGblTrajectory doGBLFit(HelicalTrackFit htf, List<TrackerHit> stripHits, MultipleScattering _scattering, double bfield, int debug) {
        List<GBLStripClusterData> stripData = makeStripData(htf, stripHits, _scattering, bfield, debug);
        double bfac = Constants.fieldConversion * bfield;

        FittedGblTrajectory fit = HpsGblRefitter.fit(stripData, bfac, debug > 0);
        return fit;
    }

    public static List<GBLStripClusterData> makeStripData(HelicalTrackFit htf, List<TrackerHit> stripHits, MultipleScattering _scattering, double _B, int _debug) {
        List<GBLStripClusterData> stripClusterDataList = new ArrayList<GBLStripClusterData>();

        // Find scatter points along the path
        MultipleScattering.ScatterPoints scatters = _scattering.FindHPSScatterPoints(htf);

        if (_debug > 0) {
            System.out.printf("perPar covariance matrix\n%s\n", htf.covariance().toString());
        }

        for (TrackerHit stripHit : stripHits) {
            HelicalTrackStripGbl strip;
            if (stripHit instanceof SiTrackerHitStrip1D) {
                strip = new HelicalTrackStripGbl(makeDigiStrip((SiTrackerHitStrip1D) stripHit), true);
            } else {
                SiTrackerHitStrip1D newHit = new SiTrackerHitStrip1D(stripHit);
                strip = new HelicalTrackStripGbl(makeDigiStrip(newHit), true);
            }

            // find Millepede layer definition from DetectorElement
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stripHit.getRawHits().get(0)).getDetectorElement();

            int millepedeId = sensor.getMillepedeId();

            if (_debug > 0) {
                System.out.printf("layer %d millepede %d (DE=\"%s\", origin %s) \n", strip.layer(), millepedeId, sensor.getName(), strip.origin().toString());
            }

            //Center of the sensor
            Hep3Vector origin = strip.origin();

            //Find intercept point with sensor in tracking frame
            Hep3Vector trkpos = TrackUtils.getHelixPlaneIntercept(htf, strip, Math.abs(_B));
            if (trkpos == null) {
                if (_debug > 0) {
                    System.out.println("Can't find track intercept; use sensor origin");
                }
                trkpos = strip.origin();
            }
            if (_debug > 0) {
                System.out.printf("trkpos at intercept [%.10f %.10f %.10f]\n", trkpos.x(), trkpos.y(), trkpos.z());
            }

            //GBLDATA
            GBLStripClusterData stripData = new GBLStripClusterData(millepedeId);
            //Add to output list
            stripClusterDataList.add(stripData);

            //path length to intercept
            double s = HelixUtils.PathToXPlane(htf, trkpos.x(), 0, 0).get(0);
            double s3D = s / Math.cos(Math.atan(htf.slope()));

            //GBLDATA
            stripData.setPath(s);
            stripData.setPath3D(s3D);

            //GBLDATA
            stripData.setU(strip.u());
            stripData.setV(strip.v());
            stripData.setW(strip.w());

            //Print track direction at intercept
            Hep3Vector tDir = HelixUtils.Direction(htf, s);
            double phi = htf.phi0() - s / htf.R();
            double lambda = Math.atan(htf.slope());

            //GBLDATA
            stripData.setTrackDir(tDir);
            stripData.setTrackPhi(phi);
            stripData.setTrackLambda(lambda);

            //Print residual in measurement system
            // start by find the distance vector between the center and the track position
            Hep3Vector vdiffTrk = VecOp.sub(trkpos, origin);

            // then find the rotation from tracking to measurement frame
            Hep3Matrix trkToStripRot = getTrackToStripRotation(sensor);

            // then rotate that vector into the measurement frame to get the predicted measurement position
            Hep3Vector trkpos_meas = VecOp.mult(trkToStripRot, vdiffTrk);

            //GBLDATA
            stripData.setMeas(strip.umeas());
            stripData.setTrackPos(trkpos_meas);
            stripData.setMeasErr(strip.du());

            if (_debug > 1) {
                System.out.printf("rotation matrix to meas frame\n%s\n", VecOp.toString(trkToStripRot));
                System.out.printf("tPosGlobal %s origin %s\n", trkpos.toString(), origin.toString());
                System.out.printf("tDiff %s\n", vdiffTrk.toString());
                System.out.printf("tPosMeas %s\n", trkpos_meas.toString());
            }

            if (_debug > 0) {
                System.out.printf("layer %d millePedeId %d uRes %.10f\n", strip.layer(), millepedeId, stripData.getMeas() - stripData.getTrackPos().x());
            }

            // find scattering angle
            MultipleScattering.ScatterPoint scatter = scatters.getScatterPoint(((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement());
            double scatAngle;

            if (scatter != null) {
                scatAngle = scatter.getScatterAngle().Angle();
            } else {
                if (_debug > 0) {
                    System.out.printf("WARNING cannot find scatter for detector %s with strip cluster at %s\n", ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement().getName(), strip.origin().toString());
                }
                scatAngle = GblUtils.estimateScatter(sensor, htf, _scattering, _B);
            }

            //GBLDATA
            stripData.setScatterAngle(scatAngle);
        }
        return stripClusterDataList;
    }

    private static Hep3Matrix getTrackToStripRotation(SiSensor sensor) {
        // This function transforms the hit to the sensor coordinates

        // Transform from JLab frame to sensor frame (done through the RawTrackerHit)
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        ITransform3D detToStrip = electrodes.getGlobalToLocal();
        // Get rotation matrix
        Hep3Matrix detToStripMatrix = detToStrip.getRotation().getRotationMatrix();
        // Transformation between the JLAB and tracking coordinate systems
        Hep3Matrix detToTrackMatrix = CoordinateTransformations.getMatrix();

        return VecOp.mult(detToStripMatrix, VecOp.inverse(detToTrackMatrix));
    }

    private static HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {
        SiTrackerHitStrip1D local = h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(new BasicHep3Vector(0., 0., 0.));
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();

        //rotate to tracking frame
        Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(org);
        Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
        Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);

        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

        //don't fill fields we don't use
//        IDetectorElement de = h.getSensor();
//        String det = getName(de);
//        int lyr = getLayer(de);
//        BarrelEndcapFlag be = getBarrelEndcapFlag(de);
        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        HelicalTrackStrip strip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dEdx, time, rawhits, null, -1, null);

        return strip;
    }
}
