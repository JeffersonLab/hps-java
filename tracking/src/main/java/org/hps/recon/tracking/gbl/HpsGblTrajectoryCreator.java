package org.hps.recon.tracking.gbl;

import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.recon.tracking.gbl.matrix.Matrix;
//import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.geometry.compact.converter.MilleParameter;


public class HpsGblTrajectoryCreator {

    private final static Logger LOGGER = Logger.getLogger(HpsGblRefitter.class.getPackage().getName());
    private boolean _debug = false;
    
    public void setDebug(boolean debug) {
        _debug = debug;
    }

    public HpsGblTrajectoryCreator() {
        // System.out.println("level " + LOGGER.getLevel().toString());
    }

    public List<GblPointJna> MakeGblPointsList(List<GBLStripClusterData> hits, GBLBeamSpotPoint bs, double bfac, Map<Integer, Integer> sensorMap, Map<Integer, Double> pathLengthMap) {
        
        
        // Save the association between strip cluster and label, and between label and path length
        //Map<Integer, Double> pathLengthMap = new HashMap<Integer, Double>();
        //Map<Integer, Integer> sensorMap = new HashMap<Integer, Integer>();
        List<GblPointJna> listOfPoints = new ArrayList<GblPointJna>();
        
        

        int iLabel = 0;
        double s = 0.;
        Matrix jacPointToPoint = new Matrix(5,5);
        jacPointToPoint.UnitMatrix();
                
        if (bs != null) {

            double cosL_bs = 1. / Math.sqrt(1. + bs._tanLambda*bs._tanLambda); 
            //double s = 0.;
            s = bs._arcLength / cosL_bs;
                        
            //start trajectory at the beamspot
            jacPointToPoint = gblSimpleJacobianLambdaPhi(bs._arcLength / cosL_bs, cosL_bs, abs(bfac));
        
        
            GblPointJna bs_point = new GblPointJna(jacPointToPoint);
            bs_point.addMeasurement(bs._projL2m,bs._aResidual,bs._aPrecision,0.);
            listOfPoints.add(bs_point);
            iLabel=listOfPoints.size();
            pathLengthMap.put(iLabel, s);
        
            //jacPointToPoint.UnitMatrix(); 
            //jacPointToPoint = gblSimpleJacobianLambdaPhi(0. - bs._arcLength / cosL_bs, cosL_bs, abs(bfac));
            //GblPointJna ref_point = new GblPointJna(jacPointToPoint);
            //listOfPoints.add(ref_point);
            //iLabel = listOfPoints.size();
            //pathLengthMap.put(iLabel,0.);
            
        } else {
            
            GblPointJna ref_point = new GblPointJna(jacPointToPoint);
            listOfPoints.add(ref_point);
            iLabel = listOfPoints.size();
            pathLengthMap.put(iLabel,0.);
        }
        
        s = 0.;
        
        
        // Loop over strips
        int n_strips = hits.size();
        for (int istrip = 0; istrip != n_strips; ++istrip) {
            GBLStripClusterData strip = hits.get(istrip);
            // MG--9/18/2015--beamspot has Id=666/667...don't include it in the GBL fit
            if (strip.getId() > 99) {
                continue;
            }
            if (_debug) {
                System.out.println("HpsGblFitter: Processing strip " + istrip + " with id/layer " + strip.getId());
            }
            // Path length step for this cluster
            double step = strip.getPath3D() - s;
            if (_debug) {
                System.out.println("HpsGblFitter: " + "Path length step " + step + " from " + s + " to " + strip.getPath3D());
            }
            
            if (strip.getScatterOnly() == 1 && _debug) {
                System.out.println("This is scatter only on sensor: MPID " + strip.getId());
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
            if (_debug) {
                System.out.println("HpsGblFitter: " + "mDir");
                mDir.print(4, 6);
            }
            Matrix mDirT = mDir.copy().transpose();

            if (_debug) {
                System.out.println("HpsGblFitter: " + "mDirT");
                mDirT.print(4, 6);
            }

            // Track direction
            double sinLambda = sin(strip.getTrackLambda());// ->GetLambda());
            double cosLambda = sqrt(1.0 - sinLambda * sinLambda);
            double sinPhi = sin(strip.getTrackPhi());// ->GetPhi());
            double cosPhi = sqrt(1.0 - sinPhi * sinPhi);

            if (_debug) {
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

            if (_debug) {
                System.out.println("HpsGblFitter: " + "uvDir");
                uvDir.print(6, 4);
            }

            // projection from measurement to local (curvilinear uv) directions (duv/dm)
            Matrix proM2l = uvDir.times(mDirT);

            // projection from local (uv) to measurement directions (dm/duv)
            Matrix proL2m = proM2l.copy();
            
            //Invertible
            if (strip.getScatterOnly() == 0)
                proL2m = proL2m.inverse();

            if (_debug) {
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

            if (_debug) {
                System.out.println("HpsGblFitter: " + "meas: ");
                meas.print(4, 6);
                System.out.println("HpsGblFitter: " + "measErr:");
                measErr.print(4, 6);
                System.out.println("HpsGblFitter: " + "measPrec:");
                measPrec.print(4, 6);
            }

            // Find the Jacobian to be able to propagate the covariance matrix to this strip position
            jacPointToPoint = gblSimpleJacobianLambdaPhi(step, cosLambda, abs(bfac));
            if (_debug) {
                System.out.println("HpsGblFitter: " + "jacPointToPoint to extrapolate to this point:");
                jacPointToPoint.print(4, 6);
            }

            GblPointJna point = new GblPointJna(jacPointToPoint);
            // Add measurement to the point

            if (strip.getScatterOnly()==0)
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
            if (_debug) {
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
            
            //Add derivatives only to measurements
            if (strip.getScatterOnly()==0) {
                // calculate and add derivatives to point
                GlobalDers glDers = new GlobalDers(strip.getId(), meas.get(0), vmeas, wmeas, tDirMeas, strip.getTrackPos(), normalMeas);
                
                // TODO find a more robust way to get half.
                boolean isTop = Math.sin(strip.getTrackLambda()) > 0;
                
                // Get the list of millepede parameters
                List<MilleParameter> milleParameters = glDers.getDers(isTop);

                // need to make vector and matrices for interface
                List<Integer> labGlobal = new ArrayList<Integer>();
                
                //The Matrix addDer is a single row vector of the size of the milleParameters (6)
                Matrix addDer = new Matrix(1, milleParameters.size());
                if (_debug) {
                    System.out.println("PF::Derivatives Informations");
                    System.out.printf("MilleParameters size %d\n", milleParameters.size());
                }
                
                for (int i = 0; i < milleParameters.size(); ++i) {
                    labGlobal.add(milleParameters.get(i).getId());
                    addDer.set(0, i, milleParameters.get(i).getValue());
                }
                if (_debug) {
                    System.out.println("PF::Print the lables and the derivatives");
                    System.out.println(labGlobal.toString());
                    addDer.print(6,6);
                }
                
                point.addGlobals(labGlobal, addDer);
                
                //            String logders = "";
                //            for (int i = 0; i < milleParameters.size(); ++i) {
                //                logders += labGlobal.get(i) + "\t" + addDer.get(0, i) + "\n";
                //            }
                //            LOGGER.info("\n" + logders);
                
                //LOGGER.info("uRes " + strip.getId() + " uRes " + uRes + " pred (" + strip.getTrackPos().x() + "," + strip.getTrackPos().y() + "," + strip.getTrackPos().z() + ") s(3D) " + strip.getPath3D());
            }
            // go to next point
            s += step;
            
        } // strips
        
        /*
        // create the trajectory
        GblTrajectoryJna traj = null;
        
        boolean constrainedFit = false;
        
        try {
            
            if (!constrainedFit) {
                //Unconstrained fit
                traj = new GblTrajectoryJna(listOfPoints,1,1,1); 
            }
            
            else {
                
                //Seed constrained fit
                
                SymMatrix seedPrecision = new SymMatrix(5);
                seedPrecision.set(0,0,1000000.);
                traj = new GblTrajectoryJna(listOfPoints,1,seedPrecision,1,1,1);
                
                
            }
                        
            if (traj.isValid() == 0 ) {
                System.out.println("HpsGblTrajectoryCreator " + " Invalid GblTrajectory -> skip");
                return null;
            }
            
            // print the trajectory
            
            if (_debug) {
                System.out.println("HpsGblTrajectoryCreator::Gbl Trajectory ");
                traj.printTrajectory(1);
                //traj.printData();
                traj.printPoints(4);
            }
            
            
            return traj;
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("HpsGblFitter: Invalid GblTrajectory -> skip"); 
            return null;
        }
        */

        return listOfPoints;
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
}
