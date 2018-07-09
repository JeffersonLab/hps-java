package org.hps.recon.tracking.gbl;

import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.geometry.compact.converter.MilleParameter;

/**
 * A Driver which refits tracks using GBL. Modeled on the hps-dst code written by Per Hansson and Omar Moreno. Requires
 * the GBL Collections and Relations to be present in the event.
 *
 * @author Norman A Graf, SLAC
 * @author Per Hansson Adrian, SLAC
 * @author Miriam Diamond, SLAC
 */
public class HpsGblRefitter {

    private final static Logger LOGGER = Logger.getLogger(HpsGblRefitter.class.getPackage().getName());
    private boolean _debug = false;

    public void setDebug(boolean debug) {
        _debug = debug;
        MakeGblTracks.setDebug(debug);
    }

    public HpsGblRefitter() {
        MakeGblTracks.setDebug(_debug);
        // System.out.println("level " + LOGGER.getLevel().toString());
    }

    // @Override
    // public void setLogLevel(String logLevel) {
    // logger.setLevel(Level.parse(logLevel));
    // }

    public static FittedGblTrajectory fit(List<GBLStripClusterData> hits, double bfac, boolean debug) {
        // path length along trajectory
        double s = 0.;
        int iLabel;

        // jacobian to transport errors between points along the path
        Matrix jacPointToPoint = new Matrix(5, 5);
        jacPointToPoint.UnitMatrix();
        // Vector of the strip clusters used for the GBL fit
        List<GblPoint> listOfPoints = new ArrayList<GblPoint>();
        // Save the association between strip cluster and label, and between label and path length
        Map<Integer, Double> pathLengthMap = new HashMap<Integer, Double>();
        Map<Integer, Integer> sensorMap = new HashMap<Integer, Integer>();

        // start trajectory at refence point (s=0) - this point has no measurement
        GblPoint ref_point = new GblPoint(jacPointToPoint);
        listOfPoints.add(ref_point);

        // save path length to each point
        iLabel = listOfPoints.size();
        pathLengthMap.put(iLabel, s);

        // Loop over strips
        int n_strips = hits.size();
        for (int istrip = 0; istrip != n_strips; ++istrip) {
            GBLStripClusterData strip = hits.get(istrip);
            // MG--9/18/2015--beamspot has Id=666/667...don't include it in the GBL fit
            if (strip.getId() > 99) {
                continue;
            }
            if (debug) {
                System.out.println("HpsGblFitter: Processing strip " + istrip + " with id/layer " + strip.getId());
            }
            // Path length step for this cluster
            double step = strip.getPath3D() - s;
            if (debug) {
                System.out.println("HpsGblFitter: " + "Path length step " + step + " from " + s + " to " + strip.getPath3D());
            }

            // get measurement frame unit vectors
            Hep3Vector u = strip.getU();
            Hep3Vector v = strip.getV();
            Hep3Vector w = strip.getW();

            // Measurement direction (perpendicular and parallel to strip direction)
            Matrix mDir = new Matrix(2, 3);
            mDir.set(0, 0, u.x());
            mDir.set(0, 1, u.y());
            mDir.set(0, 2, u.z());
            mDir.set(1, 0, v.x());
            mDir.set(1, 1, v.y());
            mDir.set(1, 2, v.z());
            if (debug) {
                System.out.println("HpsGblFitter: " + "mDir");
                mDir.print(4, 6);
            }
            Matrix mDirT = mDir.copy().transpose();

            if (debug) {
                System.out.println("HpsGblFitter: " + "mDirT");
                mDirT.print(4, 6);
            }

            // Track direction
            double sinLambda = sin(strip.getTrackLambda());// ->GetLambda());
            double cosLambda = sqrt(1.0 - sinLambda * sinLambda);
            double sinPhi = sin(strip.getTrackPhi());// ->GetPhi());
            double cosPhi = sqrt(1.0 - sinPhi * sinPhi);

            if (debug) {
                System.out.println("HpsGblFitter: " + "Track direction sinLambda=" + sinLambda + " sinPhi=" + sinPhi);
            }

            // Track direction in curvilinear frame (U,V,T)
            // U = Z x T / |Z x T|, V = T x U
            Matrix uvDir = new Matrix(2, 3);
            uvDir.set(0, 0, -sinPhi);
            uvDir.set(0, 1, cosPhi);
            uvDir.set(0, 2, 0.);
            uvDir.set(1, 0, -sinLambda * cosPhi);
            uvDir.set(1, 1, -sinLambda * sinPhi);
            uvDir.set(1, 2, cosLambda);

            if (debug) {
                System.out.println("HpsGblFitter: " + "uvDir");
                uvDir.print(6, 4);
            }

            // projection from measurement to local (curvilinear uv) directions (duv/dm)
            Matrix proM2l = uvDir.times(mDirT);

            // projection from local (uv) to measurement directions (dm/duv)
            Matrix proL2m = proM2l.copy();
            proL2m = proL2m.inverse();

            if (debug) {
                System.out.println("HpsGblFitter: " + "proM2l:");
                proM2l.print(4, 6);
                System.out.println("HpsGblFitter: " + "proL2m:");
                proL2m.print(4, 6);
                System.out.println("HpsGblFitter: " + "proM2l*proL2m (should be unit matrix):");
                (proM2l.times(proL2m)).print(4, 6);
            }

            // measurement/residual in the measurement system
            // only 1D measurement in u-direction, set strip measurement direction to zero
            Vector meas = new Vector(2);
            // double uRes = strip->GetUmeas() - strip->GetTrackPos().x(); // how can this be correct?
            double uRes = strip.getMeas() - strip.getTrackPos().x();
            meas.set(0, uRes);
            meas.set(1, 0.);
            Vector measErr = new Vector(2);
            measErr.set(0, strip.getMeasErr());
            measErr.set(1, 0.);
            Vector measPrec = new Vector(2);
            measPrec.set(0, 1.0 / (measErr.get(0) * measErr.get(0)));
            measPrec.set(1, 0.);

            if (debug) {
                System.out.println("HpsGblFitter: " + "meas: ");
                meas.print(4, 6);
                System.out.println("HpsGblFitter: " + "measErr:");
                measErr.print(4, 6);
                System.out.println("HpsGblFitter: " + "measPrec:");
                measPrec.print(4, 6);
            }

            // Find the Jacobian to be able to propagate the covariance matrix to this strip position
            jacPointToPoint = gblSimpleJacobianLambdaPhi(step, cosLambda, abs(bfac));
            if (debug) {
                System.out.println("HpsGblFitter: " + "jacPointToPoint to extrapolate to this point:");
                jacPointToPoint.print(4, 6);
            }

            GblPoint point = new GblPoint(jacPointToPoint);
            // Add measurement to the point
            point.addMeasurement(proL2m, meas, measPrec, 0.);
            // Add scatterer in curvilinear frame to the point
            // no direction in this frame
            Vector scat = new Vector(2);
            // Scattering angle in the curvilinear frame
            // Note the cosLambda to correct for the projection in the phi direction
            Vector scatErr = new Vector(2);
            scatErr.set(0, strip.getScatterAngle());
            scatErr.set(1, strip.getScatterAngle() / cosLambda);
            Vector scatPrec = new Vector(2);
            scatPrec.set(0, 1.0 / (scatErr.get(0) * scatErr.get(0)));
            scatPrec.set(1, 1.0 / (scatErr.get(1) * scatErr.get(1)));

            // add scatterer 
            point.addScatterer(scat, scatPrec);
            if (debug) {
                System.out.println("HpsGblFitter: " + "adding scatError to this point:");
                scatErr.print(4, 6);
            }

            // Add this GBL point to list that will be used in fit
            listOfPoints.add(point);
            iLabel = listOfPoints.size();
            // save path length and sensor-number to each point
            pathLengthMap.put(iLabel, s + step);
            sensorMap.put(iLabel, strip.getId());

            // // Calculate global derivatives for this point
            // track direction in tracking/global frame
            Hep3Vector tDirGlobal = new BasicHep3Vector(cosPhi * cosLambda, sinPhi * cosLambda, sinLambda);

            // Cross-check that the input is consistent
            if (VecOp.sub(tDirGlobal, strip.getTrackDirection()).magnitude() > 0.00001) {
                throw new RuntimeException("track directions are inconsistent: " + tDirGlobal.toString() + " and " + strip.getTrackDirection().toString());
            }

            // rotate track direction to measurement frame
            Hep3Vector tDirMeas = new BasicHep3Vector(VecOp.dot(tDirGlobal, u), VecOp.dot(tDirGlobal, v), VecOp.dot(tDirGlobal, w));
            Hep3Vector normalMeas = new BasicHep3Vector(VecOp.dot(w, u), VecOp.dot(w, v), VecOp.dot(w, w));

            // measurements: non-measured directions
            double vmeas = 0.;
            double wmeas = 0.;

            // calculate and add derivatives to point
            GlobalDers glDers = new GlobalDers(strip.getId(), meas.get(0), vmeas, wmeas, tDirMeas, strip.getTrackPos(), normalMeas);

            // TODO find a more robust way to get half.
            boolean isTop = Math.sin(strip.getTrackLambda()) > 0;

            // Get the list of millepede parameters
            List<MilleParameter> milleParameters = glDers.getDers(isTop);

            // need to make vector and matrices for interface
            List<Integer> labGlobal = new ArrayList<Integer>();
            Matrix addDer = new Matrix(1, milleParameters.size());
            for (int i = 0; i < milleParameters.size(); ++i) {
                labGlobal.add(milleParameters.get(i).getId());
                addDer.set(0, i, milleParameters.get(i).getValue());
            }
            point.addGlobals(labGlobal, addDer);
            //            String logders = "";
            //            for (int i = 0; i < milleParameters.size(); ++i) {
            //                logders += labGlobal.get(i) + "\t" + addDer.get(0, i) + "\n";
            //            }
            //            LOGGER.info("\n" + logders);

            LOGGER.info("uRes " + strip.getId() + " uRes " + uRes + " pred (" + strip.getTrackPos().x() + "," + strip.getTrackPos().y() + "," + strip.getTrackPos().z() + ") s(3D) " + strip.getPath3D());

            // go to next point
            s += step;

        } // strips

        // create the trajectory
        GblTrajectory traj = new GblTrajectory(listOfPoints); // ,seedLabel, clSeed);

        if (!traj.isValid()) {
            System.out.println("HpsGblFitter: " + " Invalid GblTrajectory -> skip");
            return null; // 1;//INVALIDTRAJ;
        }

        // print the trajectory
        if (debug) {
            System.out.println("%%%% Gbl Trajectory ");
            traj.printTrajectory(1);
            traj.printData();
            traj.printPoints(4);
        }
        // fit trajectory
        double[] dVals = new double[2];
        int[] iVals = new int[1];
        traj.fit(dVals, iVals, "");
        LOGGER.info("fit result: Chi2=" + dVals[0] + " Ndf=" + iVals[0] + " Lost=" + dVals[1]);

        FittedGblTrajectory fittedTraj = new FittedGblTrajectory(traj, dVals[0], iVals[0], dVals[1]);
        fittedTraj.setPathLengthMap(pathLengthMap);
        fittedTraj.setSensorMap(sensorMap);

        return fittedTraj;
    }

    private static Matrix gblSimpleJacobianLambdaPhi(double ds, double cosl, double bfac) {
        /**
         * Simple jacobian: quadratic in arc length difference. using lambda phi as directions
         *
         * @param ds: arc length difference
         * @type ds: float
         * @param cosl: cos(lambda)
         * @type cosl: float
         * @param bfac: Bz*c
         * @type bfac: float
         * @return: jacobian to move by 'ds' on trajectory
         * @rtype: matrix(float) ajac(1,1)= 1.0D0 ajac(2,2)= 1.0D0 ajac(3,1)=-DBLE(bfac*ds) ajac(3,3)= 1.0D0
         *         ajac(4,1)=-DBLE(0.5*bfac*ds*ds*cosl) ajac(4,3)= DBLE(ds*cosl) ajac(4,4)= 1.0D0 ajac(5,2)= DBLE(ds)
         *         ajac(5,5)= 1.0D0 ''' jac = np.eye(5) jac[2, 0] = -bfac * ds jac[3, 0] = -0.5 * bfac * ds * ds * cosl
         *         jac[3, 2] = ds * cosl jac[4, 1] = ds return jac
         */
        Matrix jac = new Matrix(5, 5);
        jac.UnitMatrix();
        jac.set(2, 0, -bfac * ds);
        jac.set(3, 0, -0.5 * bfac * ds * ds * cosl);
        jac.set(3, 2, ds * cosl);
        jac.set(4, 1, ds);
        return jac;
    }

    private static class GlobalDers {

        private final int _layer;
        private final Hep3Vector _t; // track direction
        private final Hep3Vector _p; // track prediction
        private final Hep3Vector _n; // normal to plane
        private final Matrix _dm_dg; // Global derivaties of the local measurements
        private final Matrix _dr_dm; // Derivatives of residuals w.r.t. measurement
        private final Matrix _dr_dg; // Derivatives of residuals w.r.t. global parameters

        public GlobalDers(int layer, double umeas, double vmeas, double wmeas, Hep3Vector tDir, Hep3Vector tPred, Hep3Vector normal) {
            _layer = layer;
            _t = tDir;
            _p = tPred;
            _n = normal;
            // Derivatives of residuals w.r.t. perturbed measurement
            _dr_dm = getResDers();
            // Derivatives of perturbed measurement w.r.t. global parameters
            _dm_dg = getMeasDers();
            // Calculate, by chain rule, derivatives of residuals w.r.t. global parameters
            _dr_dg = _dr_dm.times(_dm_dg);

        }

        /**
         * Derivative of mt, the perturbed measured coordinate vector m w.r.t. to global parameters:
         * u,v,w,alpha,beta,gamma
         */
        private Matrix getMeasDers() {

            // Derivative of the local measurement for a translation in u
            double dmu_du = 1.;
            double dmv_du = 0.;
            double dmw_du = 0.;
            // Derivative of the local measurement for a translation in v
            double dmu_dv = 0.;
            double dmv_dv = 1.;
            double dmw_dv = 0.;
            // Derivative of the local measurement for a translation in w
            double dmu_dw = 0.;
            double dmv_dw = 0.;
            double dmw_dw = 1.;
            // Derivative of the local measurement for a rotation around u-axis (alpha)
            double dmu_dalpha = 0.;
            double dmv_dalpha = _p.z(); // self.wmeas
            double dmw_dalpha = -1.0 * _p.y(); // -1.0 * self.vmeas
            // Derivative of the local measurement for a rotation around v-axis (beta)
            double dmu_dbeta = -1.0 * _p.z(); // -1.0 * self.wmeas
            double dmv_dbeta = 0.;
            double dmw_dbeta = _p.x(); // self.umeas
            // Derivative of the local measurement for a rotation around w-axis (gamma)
            double dmu_dgamma = _p.y(); // self.vmeas
            double dmv_dgamma = -1.0 * _p.x(); // -1.0 * self.umeas 
            double dmw_dgamma = 0.;
            // put into matrix
            Matrix dm_dg = new Matrix(3, 6);
            dm_dg.set(0, 0, dmu_du);
            dm_dg.set(0, 1, dmu_dv);
            dm_dg.set(0, 2, dmu_dw);
            dm_dg.set(0, 3, dmu_dalpha);
            dm_dg.set(0, 4, dmu_dbeta);
            dm_dg.set(0, 5, dmu_dgamma);
            dm_dg.set(1, 0, dmv_du);
            dm_dg.set(1, 1, dmv_dv);
            dm_dg.set(1, 2, dmv_dw);
            dm_dg.set(1, 3, dmv_dalpha);
            dm_dg.set(1, 4, dmv_dbeta);
            dm_dg.set(1, 5, dmv_dgamma);
            dm_dg.set(2, 0, dmw_du);
            dm_dg.set(2, 1, dmw_dv);
            dm_dg.set(2, 2, dmw_dw);
            dm_dg.set(2, 3, dmw_dalpha);
            dm_dg.set(2, 4, dmw_dbeta);
            dm_dg.set(2, 5, dmw_dgamma);

            return dm_dg;
        }

        /**
         * Derivatives of the local perturbed residual w.r.t. the measurements m (u,v,w)'
         */
        private Matrix getResDers() {
            double tdotn = VecOp.dot(_t, _n);
            Matrix dr_dm = Matrix.identity(3, 3);

            double delta, val;
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    delta = i == j ? 1. : 0.;
                    val = delta - _t.v()[i] * _n.v()[j] / tdotn;
                    dr_dm.set(i, j, val);
                }
            }
            return dr_dm;
        }

        /**
         * Turn derivative matrix into @Milleparameter
         *
         * @param isTop - top or bottom track
         * @return list of @Milleparameters
         */
        private List<MilleParameter> getDers(boolean isTop) {
            int transRot;
            int direction;
            int label;
            double value;
            List<MilleParameter> milleParameters = new ArrayList<MilleParameter>();
            int topBot = isTop == true ? 1 : 2;
            for (int ip = 1; ip < 7; ++ip) {
                if (ip > 3) {
                    transRot = 2;
                    direction = ((ip - 1) % 3) + 1;
                } else {
                    transRot = 1;
                    direction = ip;
                }
                label = topBot * MilleParameter.half_offset + transRot * MilleParameter.type_offset + direction * MilleParameter.dimension_offset + _layer;
                value = _dr_dg.get(0, ip - 1);
                MilleParameter milleParameter = new MilleParameter(label, value, 0.0);
                milleParameters.add(milleParameter);
            }
            return milleParameters;
        }

    }
}
