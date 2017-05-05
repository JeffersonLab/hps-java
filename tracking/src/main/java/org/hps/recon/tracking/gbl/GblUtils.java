package org.hps.recon.tracking.gbl;

import java.util.logging.Logger;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.constants.Constants;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * A class with only static utilities related to GBL.
 */
public class GblUtils {
    
    public static Logger LOGGER = Logger.getLogger(GblUtils.class.getName());
            
    /**
     * Private constructor to avoid instantiation.
     */
    private GblUtils() {
    }
    
    /**
     * 
     * Store local curvilinear track parameters. 
     * 
     *
     */
    public static class ClParams {
    
        private BasicMatrix _params = new BasicMatrix(1, 5);
    
        public ClParams(HelicalTrackFit htf, double B) {
    
            if (htf == null) {
                return;
            }
    
            Hep3Matrix perToClPrj = getPerToClPrj(htf);
    
            double d0 = -1 * htf.dca(); //sign convention for curvilinear frame
            double z0 = htf.z0();
            Hep3Vector vecPer = new BasicHep3Vector(0., d0, z0);
            //System.out.printf("%s: vecPer=%s\n",this.getClass().getSimpleName(),vecPer.toString());
    
            Hep3Vector vecCl = VecOp.mult(perToClPrj, vecPer);
            //System.out.printf("%s: vecCl=%s\n",this.getClass().getSimpleName(),vecCl.toString());
            double xT = vecCl.x();
            double yT = vecCl.y();
            //double zT = vecCl.z();
    
            double lambda = Math.atan(htf.slope());
            double q = Math.signum(htf.R());
            double qOverP = q / htf.p(Math.abs(B));
            double phi = htf.phi0();
    
            _params.setElement(0, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), qOverP);
            _params.setElement(0, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), lambda);
            _params.setElement(0, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), phi);
            _params.setElement(0, FittedGblTrajectory.GBLPARIDX.XT.getValue(), xT);
            _params.setElement(0, FittedGblTrajectory.GBLPARIDX.YT.getValue(), yT);
        }
    
        public BasicMatrix getParams() {
            return _params;
        }
    
        double getQoverP() {
            return _params.e(0, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue());
        }
    
        double getLambda() {
            return _params.e(0, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue());
        }
    
        double getPhi() {
            return _params.e(0, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue());
        }
    
        double getXt() {
            return _params.e(0, FittedGblTrajectory.GBLPARIDX.XT.getValue());
        }
    
        double getYt() {
            return _params.e(0, FittedGblTrajectory.GBLPARIDX.YT.getValue());
        }
    
    }





    /**
     * 
     * Store perigee track parameters. 
     * 
     *
     */
    public static class PerigeeParams {
    
        private final BasicMatrix _params;
    
        public PerigeeParams(HelicalTrackFit htf, double B) {
            _params = GblUtils.getPerParVector(htf, B);
        }
    
        public PerigeeParams(double kappa, double theta, double phi, double d0, double z0) {
            this._params = GblUtils.getPerParVector(kappa, theta, phi, d0, z0);
        }
    
        public BasicMatrix getParams() {
            return _params;
        }
    
        public double getKappa() {
            return _params.e(0, 0);
        }
    
        public double getTheta() {
            return _params.e(0, 1);
        }
    
        public double getPhi() {
            return _params.e(0, 2);
        }
    
        public double getD0() {
            return _params.e(0, 3);
        }
    
        public double getZ0() {
            return _params.e(0, 4);
        }
    }
    
    /**
     * Get corrected perigee parameters. 
     * @param locPar - GBL local curvilinear corrections
     * @param helicalTrackFit - helix
     * @param bfield - B-field strength
     * @return corrected parameters
     */
    public static double[] getCorrectedPerigeeParameters(Vector locPar, HelicalTrackFit helicalTrackFit, double bfield) {
                
        // Explicitly assign corrections to local variables
        double qOverPCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.QOVERP.getValue());
        double xTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue());
        double yTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue());
        double xTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XT.getValue());
        double yTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YT.getValue());
        
        // Get helix parameters
        double qOverP = helicalTrackFit.curvature() / (Constants.fieldConversion * Math.abs(bfield) * Math.sqrt(1 + Math.pow(helicalTrackFit.slope(), 2)));
        double d0 = -1.0 * helicalTrackFit.dca(); // correct for different sign convention of d0 in perigee frame
        double z0 = helicalTrackFit.z0();
        double phi0 = helicalTrackFit.phi0();
        double lambda = Math.atan(helicalTrackFit.slope());
        
        // calculate new d0 and z0
        Hep3Matrix perToClPrj = GblUtils.getPerToClPrj(helicalTrackFit);

        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        Hep3Vector corrPer = VecOp.mult(clToPerPrj, new BasicHep3Vector(xTCorr, yTCorr, 0.0));

        //d0
        double d0_corr = corrPer.y();
        double dca_gbl = -1.0 * (d0 + d0_corr);

        //z0
        double z0_corr = corrPer.z();
        double z0_gbl = z0 + z0_corr;

        //calculate new slope
        double lambda_gbl = lambda + yTPrimeCorr;
        double slope_gbl = Math.tan(lambda_gbl);

        // calculate new curvature
        double qOverP_gbl = qOverP + qOverPCorr;
        double C_gbl = Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl / Math.cos(lambda_gbl);

        //calculate new phi0
        double phi0_gbl = phi0 + xTPrimeCorr - corrPer.x() * C_gbl;

        LOGGER.info("qOverP=" + qOverP + " qOverPCorr=" + qOverPCorr + " qOverP_gbl=" + qOverP_gbl + " ==> pGbl=" + 1.0 / qOverP_gbl + " C_gbl=" + C_gbl);

        LOGGER.info(String.format("corrected helix: d0=%f, z0=%f, omega=%f, tanlambda=%f, phi0=%f, p=%f", dca_gbl, z0_gbl, C_gbl, slope_gbl, phi0_gbl, Math.abs(1 / qOverP_gbl)));
        
        double parameters_gbl[] = new double[5];
        parameters_gbl[HelicalTrackFit.dcaIndex] = dca_gbl;
        parameters_gbl[HelicalTrackFit.phi0Index] = phi0_gbl;
        parameters_gbl[HelicalTrackFit.curvatureIndex] = C_gbl;
        parameters_gbl[HelicalTrackFit.z0Index] = z0_gbl;
        parameters_gbl[HelicalTrackFit.slopeIndex] = slope_gbl;
        
        return parameters_gbl;       
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

    /**
     * Calculate the Jacobian from Curvilinear to Perigee frame. 
     * @param helicalTrackFit - original helix
     * @param helicalTrackFitAtIPCorrected - corrected helix at this point
     * @param bfield - magnitude of B-field
     * @return the Jacobian matrix from Curvilinear to Perigee frame
     */
    public static Matrix getCLToPerigeeJacobian(HelicalTrackFit helicalTrackFit, HpsHelicalTrackFit helicalTrackFitAtIPCorrected, double bfield) {
        
        /*
         * This part is taken from:
         // Strandlie, Wittek, NIMA 566, 2006
         Matrix covariance_gbl = new Matrix(5, 5);
         //helpers
         double Bz = -Constants.fieldConversion * Math.abs(bfield); // TODO sign convention and should it be it scaled from Telsa?
         double p = Math.abs(1 / qOverP_gbl);
         double q = Math.signum(qOverP_gbl);
         double tanLambda = Math.tan(lambda_gbl);
         double cosLambda = Math.cos(lambda_gbl);
         //        Hep3Vector B = new BasicHep3Vector(0, 0, Bz); // TODO sign convention?
         Hep3Vector H = new BasicHep3Vector(0, 0, 1);
         Hep3Vector T = HelixUtils.Direction(helix, 0.);
         Hep3Vector HcrossT = VecOp.cross(H, T);
         double alpha = HcrossT.magnitude(); // this should be Bvec cross TrackDir/|B|
         double Q = Bz * q / p;
         Hep3Vector Z = new BasicHep3Vector(0, 0, 1);
         Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
         Hep3Vector K = Z;
         Hep3Vector U = VecOp.mult(-1, J);
         Hep3Vector V = VecOp.cross(T, U);
         Hep3Vector I = VecOp.cross(J, K);
         Hep3Vector N = VecOp.mult(1 / alpha, VecOp.cross(H, T));
         double UdotI = VecOp.dot(U, I);
         double NdotV = VecOp.dot(N, V);
         double NdotU = VecOp.dot(N, U);
         double TdotI = VecOp.dot(T, I);
         double VdotI = VecOp.dot(V, I);
         double VdotK = VecOp.dot(V, K);
         covariance_gbl.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), VdotK / TdotI);
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1);
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XT.getValue(), -alpha * Q * UdotI * NdotU / (cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -alpha * Q * VdotI * NdotU / (cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), -1 * Bz / cosLambda);
         //        covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 0);
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1 * q * Bz * tanLambda / (p * cosLambda));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), q * Bz * alpha * Q * tanLambda * UdotI * NdotV / (p * cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), q * Bz * alpha * Q * tanLambda * VdotI * NdotV / (p * cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -1 / TdotI);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), alpha * Q * UdotI * NdotV / TdotI);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), alpha * Q * VdotI * NdotV / TdotI);

         covariance_gbl.print(15, 13);
         */
        
        // Sho's magic below
        
        // Use projection matrix
        //TODO should this not be the corrected helix?
        Hep3Matrix perToClPrj = getPerToClPrj(helicalTrackFit);
        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        double C_gbl = helicalTrackFitAtIPCorrected.curvature();
        double lambda_gbl = Math.atan(helicalTrackFitAtIPCorrected.slope());
        double qOverP_gbl = helicalTrackFitAtIPCorrected.curvature() / (Constants.fieldConversion * Math.abs(bfield) * Math.sqrt(1 + Math.pow(helicalTrackFitAtIPCorrected.slope(), 2)));
        
        Matrix jacobian = new Matrix(5, 5);
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), -clToPerPrj.e(1, 0));
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -clToPerPrj.e(1, 1));
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1.0);
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(0, 1) * C_gbl);
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), Constants.fieldConversion * Math.abs(bfield) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl * Math.tan(lambda_gbl) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.XT.getValue(), clToPerPrj.e(2, 0));
        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(2, 1));
        jacobian.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), Math.pow(Math.cos(lambda_gbl), -2.0));
        
        return jacobian;
    }

    /**
     * Computes the projection matrix from the perigee XY plane variables dca
     * and z0 into the curvilinear xT,yT,zT frame (U,V,T) with reference point (0,0,0) 
     * for the perigee frame.
     *
     * @param htf input helix to find the track direction
     * @return 3x3 projection matrix
     */
    static Hep3Matrix getPerToClPrj(HelicalTrackFit htf) {
        Hep3Vector Z = new BasicHep3Vector(0, 0, 1);
        Hep3Vector T = HelixUtils.Direction(htf, 0.);
        Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
        Hep3Vector K = Z;
        Hep3Vector U = VecOp.mult(-1, J);
        Hep3Vector V = VecOp.cross(T, U);
        Hep3Vector I = VecOp.cross(J, K);
    
        BasicHep3Matrix trans = new BasicHep3Matrix();
        trans.setElement(0, 0, VecOp.dot(I, U));
        trans.setElement(0, 1, VecOp.dot(J, U));
        trans.setElement(0, 2, VecOp.dot(K, U));
        trans.setElement(1, 0, VecOp.dot(I, V));
        trans.setElement(1, 1, VecOp.dot(J, V));
        trans.setElement(1, 2, VecOp.dot(K, V));
        trans.setElement(2, 0, VecOp.dot(I, T));
        trans.setElement(2, 1, VecOp.dot(J, T));
        trans.setElement(2, 2, VecOp.dot(K, T));
        return trans;
    
        /*
         Hep3Vector B = new BasicHep3Vector(0, 0, 1); // TODO sign convention?
         Hep3Vector H = VecOp.mult(1 / bfield, B);
         Hep3Vector T = HelixUtils.Direction(helix, 0.);
         Hep3Vector HcrossT = VecOp.cross(H, T);
         double alpha = HcrossT.magnitude(); // this should be Bvec cross TrackDir/|B|
         double Q = Math.abs(bfield) * q / p;
         Hep3Vector Z = new BasicHep3Vector(0, 0, 1);
         Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
         Hep3Vector K = Z;
         Hep3Vector U = VecOp.mult(-1, J);
         Hep3Vector V = VecOp.cross(T, U);
         Hep3Vector I = VecOp.cross(J, K);
         Hep3Vector N = VecOp.mult(1 / alpha, VecOp.cross(H, T)); //-cross(T,H)/alpha = -cross(T,Z) = -J
         double UdotI = VecOp.dot(U, I); // 0,0
         double NdotV = VecOp.dot(N, V); // 1,1?
         double NdotU = VecOp.dot(N, U); // 0,1?
         double TdotI = VecOp.dot(T, I); // 2,0
         double VdotI = VecOp.dot(V, I); // 1,0
         double VdotK = VecOp.dot(V, K); // 1,2
         */
    }

    private static BasicMatrix getPerParVector(double kappa, double theta, double phi, double d0, double z0) {
        BasicMatrix perPar = new BasicMatrix(1, 5);
        perPar.setElement(0, 0, kappa);
        perPar.setElement(0, 1, theta);
        perPar.setElement(0, 2, phi);
        perPar.setElement(0, 3, d0);
        perPar.setElement(0, 4, z0);
        return perPar;
    }

    private static BasicMatrix getPerParVector(HelicalTrackFit htf, double B) {
        if (htf != null) {
            double kappa = -1.0 * Math.signum(B) / htf.R();
            double theta = Math.PI / 2.0 - Math.atan(htf.slope());
            return getPerParVector(kappa, theta, htf.phi0(), htf.dca(), htf.z0());
        }
        return new BasicMatrix(1, 5);
    }
}
