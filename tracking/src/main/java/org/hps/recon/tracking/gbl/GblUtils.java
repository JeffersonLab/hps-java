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


import org.lcsim.event.Track;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.TrackResidualsData;

/**
 * A class with only static utilities related to GBL
 */
public class GblUtils {

    public static Logger LOGGER = Logger.getLogger(GblUtils.class.getName());

    /**
     * Private constructor to avoid instantiation.
     */
    private GblUtils() {
    }

    /**
     * Store local curvilinear track parameters.
     * 
     */
    public static class ClParams {

        private BasicMatrix _params = new BasicMatrix(1, 5);

        public ClParams(HelicalTrackFit htf, double B) {

            if (htf == null) {
                return;
            }

            Hep3Matrix perToClPrj = getPerToClPrj(htf);

            double d0 = -1 * htf.dca(); // sign convention for curvilinear frame
            double z0 = htf.z0();
            Hep3Vector vecPer = new BasicHep3Vector(0., d0, z0);
            // System.out.printf("%s: vecPer=%s\n",this.getClass().getSimpleName(),vecPer.toString());

            Hep3Vector vecCl = VecOp.mult(perToClPrj, vecPer);
            // System.out.printf("%s: vecCl=%s\n",this.getClass().getSimpleName(),vecCl.toString());
            double xT = vecCl.x();
            double yT = vecCl.y();
            // double zT = vecCl.z();

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
    
    /** returns the residuals from the trajectory fit
     *  fix this by passing only the trajectory!
     */
    
    public static TrackResidualsData computeGblResiduals(Track trk, FittedGblTrajectory fitGbl_traj) {
        
        GblTrajectory gbl_fit_trajectory = fitGbl_traj.get_traj();
        
        List<Double>  b_residuals = new ArrayList<Double>();
        List<Float>   b_sigmas    = new ArrayList<Float>();
        List<Integer> r_sensors   = new ArrayList<Integer>();
        
        int numData[] = new int[1];
        //System.out.printf("Getting the residuals. Points  on trajectory: %d \n",gbl_fit_trajectory.getNpointsOnTraj());
        //The fitted trajectory has a mapping between the MPID and the ilabel. Use that to get the MPID of the residual.
        Integer[] sensorsFromMapArray = fitGbl_traj.getSensorMap().keySet().toArray(new Integer[0]);
        //System.out.printf("Getting the residuals. Sensors on trajectory: %d \n",sensorsFromMapArray.length);
        
        //System.out.println("Check residuals of the original fit");
        //Looping on all the sensors on track -  to get the biased residuals.
        for (int i_s = 0; i_s < sensorsFromMapArray.length; i_s++) {       
            //Get the point label
            int ilabel = sensorsFromMapArray[i_s];
            //Get the millepede ID
            int mpid = fitGbl_traj.getSensorMap().get(ilabel);
            List<Double> aResiduals   = new ArrayList<Double>();   
            List<Double> aMeasErrors  = new ArrayList<Double>();
            List<Double> aResErrors   = new ArrayList<Double>();  
            List<Double> aDownWeights = new ArrayList<Double>();
            gbl_fit_trajectory.getMeasResults(ilabel,numData,aResiduals,aMeasErrors,aResErrors,aDownWeights); 
            if (numData[0]>1) { 
                System.out.printf("GBLRefitterDriver::WARNING::We have SCT sensors. Residuals dimensions should be <=1\n");
            }
            for (int i=0; i<numData[0];i++) {
                //System.out.printf("KalmanToGBLDriver::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                //System.out.printf("KalmanToGBLDriver::measResults %d %d %d %f %f %f \n",ilabel, i, mpid, aResiduals.get(i),aMeasErrors.get(i),aResErrors.get(i));
                
                r_sensors.add(mpid);
                b_residuals.add(aResiduals.get(i));
                b_sigmas.add(aResErrors.get(i).floatValue());
            }
            //Perform an unbiasing fit for each traj
            
            //System.out.println("Run the unbiased residuals!!!\n");
            //For each sensor create a trajectory 
            GblTrajectory gbl_fit_traj_u = new GblTrajectory(gbl_fit_trajectory.getSingleTrajPoints());
            double[] u_dVals = new double[2];
            int[] u_iVals    = new int[1];
            int[] u_numData  = new int[1]; 
            //Fit it once to have exactly the same starting point of gbl_fit_trajectory.
            gbl_fit_traj_u.fit(u_dVals,u_iVals,"");
            List<Double> u_aResiduals   = new ArrayList<Double>();   
            List<Double> u_aMeasErrors  = new ArrayList<Double>();
            List<Double> u_aResErrors   = new ArrayList<Double>();  
            List<Double> u_aDownWeights = new ArrayList<Double>();
            
            try {
                //Fit removing the measurement
                gbl_fit_traj_u.fit(u_dVals,u_iVals,"",ilabel);
                gbl_fit_traj_u.getMeasResults(ilabel,numData,u_aResiduals,u_aMeasErrors,u_aResErrors,u_aDownWeights); 
                for (int i=0; i<numData[0];i++) {
                    //System.out.printf("Example1::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                    //System.out.printf("Example1::UmeasResults %d %d %d %f %f %f \n",ilabel, i, mpid, u_aResiduals.get(i),u_aMeasErrors.get(i),u_aResErrors.get(i));
                    
                    r_sensors.add(mpid);
                    b_residuals.add(u_aResiduals.get(i));
                    b_sigmas.add(u_aResErrors.get(i).floatValue());
                }
            }
            catch (RuntimeException e){
                //  e.printStackTrack();
                r_sensors.add(-999);
                b_residuals.add(-9999.);
                b_sigmas.add((float)-9999.);
                //System.out.printf("Unbiasing fit fails! For label::%d\n",ilabel);
            }
            
        }//loop on sensors on track
        
        //Set top by default
        int trackerVolume = 0;
        //if tanLamda<0 set bottom
        //System.out.printf("Residuals size %d \n", r_sensors.size());
        
        if (trk.getTrackStates().get(0).getTanLambda() < 0) trackerVolume = 1;
        TrackResidualsData resData  = new TrackResidualsData(trackerVolume,r_sensors,b_residuals,b_sigmas);
        
        return resData;
    }
      
    

    /**
     * Store perigee track parameters.
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
     * 
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
        double qOverP = helicalTrackFit.curvature()
                / (Constants.fieldConversion * Math.abs(bfield) * Math.sqrt(1 + Math.pow(helicalTrackFit.slope(), 2)));
        double d0 = -1.0 * helicalTrackFit.dca(); // correct for different sign convention of d0 in perigee frame
        double z0 = helicalTrackFit.z0();
        double phi0 = helicalTrackFit.phi0();
        double lambda = Math.atan(helicalTrackFit.slope());

        // calculate new d0 and z0
        Hep3Matrix perToClPrj = GblUtils.getPerToClPrj(helicalTrackFit);

        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        Hep3Vector corrPer = VecOp.mult(clToPerPrj, new BasicHep3Vector(xTCorr, yTCorr, 0.0));

        // d0
        double d0_corr = corrPer.y();
        double dca_gbl = -1.0 * (d0 + d0_corr);

        // z0
        double z0_corr = corrPer.z();
        double z0_gbl = z0 + z0_corr;

        // calculate new slope
        double lambda_gbl = lambda + yTPrimeCorr;
        double slope_gbl = Math.tan(lambda_gbl);

        // calculate new curvature
        double qOverP_gbl = qOverP + qOverPCorr;
        double C_gbl = Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl / Math.cos(lambda_gbl);

        // calculate new phi0
        double phi0_gbl = phi0 + xTPrimeCorr - corrPer.x() * C_gbl;

        LOGGER.info("qOverP=" + qOverP + " qOverPCorr=" + qOverPCorr + " qOverP_gbl=" + qOverP_gbl + " ==> pGbl=" + 1.0
                / qOverP_gbl + " C_gbl=" + C_gbl);

        LOGGER.info(String.format("corrected helix: d0=%f, z0=%f, omega=%f, tanlambda=%f, phi0=%f, p=%f", dca_gbl,
                z0_gbl, C_gbl, slope_gbl, phi0_gbl, Math.abs(1 / qOverP_gbl)));

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
         * Simple jacobian: quadratic in arc length difference. using lambda phi as directions
         * @param ds: arc length difference
         * @type ds: float
         * @param cosl: cos(lambda)
         * @type cosl: float
         * @param bfac: Bz*c
         * @type bfac: float
         * @return: jacobian to move by 'ds' on trajectory
         * @rtype: matrix(float) ajac(1,1)= 1.0D0 ajac(2,2)= 1.0D0 ajac(3,1)=-DBLE(bfac*ds) ajac(3,3)= 1.0D0
         * ajac(4,1)=-DBLE(0.5*bfac*ds*ds*cosl) ajac(4,3)= DBLE(ds*cosl) ajac(4,4)= 1.0D0 ajac(5,2)= DBLE(ds) ajac(5,5)=
         * 1.0D0 ''' jac = np.eye(5) jac[2, 0] = -bfac * ds jac[3, 0] = -0.5 * bfac * ds * ds * cosl jac[3, 2] = ds *
         * cosl jac[4, 1] = ds return jac
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

    public static double estimateScatter(IDetectorElement hitElement, HelicalTrackFit htf,
            MultipleScattering scattering, double _B) {
        // can be edge case where helix is outside, but close to sensor, so make a new scatter point assuming the helix
        // does pass through the sensor
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
            throw new UnsupportedOperationException(
                "Should not happen. This problem is only solved with the MaterialSupervisor.");
        }
    }


    /**
     * Form a virtual layer at the target location
     * Use the helix analytical extrapolation to beamspot
     * These computations are in tracking frame
     */
    
    public static GBLBeamSpotPoint gblMakeBsPoint(HelicalTrackFit htf, double [] bsLocation, double[] udir, double [] vdir, double [] error)  {
        //Using the GBL package for prediction and change of reference
        //Need to use -1/R and -d0 as it uses different conventions 
            
        GblSimpleHelix gblHelix = new GblSimpleHelix(-1/htf.R(), htf.phi0(), -htf.dca(), htf.slope(), htf.z0());
        GblHelixPrediction gblHelixPrediction = gblHelix.getPrediction(bsLocation, udir,vdir);

        double prediction [] = new double[2];
        gblHelixPrediction.getMeasPred(prediction);

        gblHelix.delete();
        gblHelixPrediction.delete();
            
        //Curvilinear frame, UV Plane 
        Matrix uvDir_gbl = gblHelixPrediction.getCurvilinearDirs();
            
        //Global to measurement transformation
            
        Matrix mDir = new Matrix(2,3);
            
        mDir.set(0,2,udir[2]);
        mDir.set(1,0,vdir[0]);
        mDir.set(1,1,vdir[1]);

        //measurement to global 
        Matrix mDirT = mDir.copy().transpose();
            
        //measurement to curvilinear frame.
        Matrix proM2l = uvDir_gbl.times(mDirT);

        //Curvilinear to measurement. 
        Matrix proL2m = proM2l.copy().inverse();
            
        //Create the jacobian point to point
        double sArc2D = gblHelixPrediction.getArcLength();
            
        Hep3Vector bsLocation_3v = new BasicHep3Vector(bsLocation[0],bsLocation[1],bsLocation[2]);
        Vector aResidual = new Vector(2);
        //Notice the minus sign. The residual is defined as meas - pred
        aResidual.set(0,-prediction[0]); //Y
        aResidual.set(1,-prediction[1]); //X

        Vector aPrecision = new Vector(2);
        aPrecision.set(0,1./(error[0]*error[0])); 
        aPrecision.set(1,1./(error[1]*error[1])); 
           
        return  new GBLBeamSpotPoint(bsLocation_3v, aResidual,
                                     aPrecision, proL2m,sArc2D,htf.slope(),htf.phi0() - sArc2D/htf.R());
            
    }

    //This method fails with singular matrices. TODO:: Check why that happens
    /**
     * Calculate the Jacobian from Curvilinear to Perigee frame.
     * 
     * @param helicalTrackFit - original helix - unused. Only for backward compatibility.
     * @param helicalTrackFitAtIPCorrected - corrected helix at this point
     * @param bfield - magnitude of B-field
     * @return the Jacobian matrix from Curvilinear to Perigee frame
     */
    
    /*
      public static Matrix getCLToPerigeeJacobian(HelicalTrackFit helicalTrackFit, HpsHelicalTrackFit helicalTrackFitAtIPCorrected, double bfield) {
        
        return getCLToPerigeeJacobian(helicalTrackFitAtIPCorrected, bfield);
    }
    
    */

    
    /**
     * Calculate the Jacobian from Curvilinear to Perigee frame.
     * 
     * @param helicalTrackFitAtIPCorrected - corrected helix at this point
     * @param bfield - magnitude of B-field
     * @return the Jacobian matrix from Curvilinear to Perigee frame
     */
    public static Matrix getCLToPerigeeJacobian(HelicalTrackFit helicalTrackFit, HpsHelicalTrackFit helicalTrackFitAtIPCorrected, double bfield) {
        
        /*
         * This part is taken from: // Strandlie, Wittek, NIMA 566, 2006 Matrix covariance_gbl = new Matrix(5, 5);
         * //helpers double Bz = -Constants.fieldConversion * Math.abs(bfield); // TODO sign convention and should it be
         * it scaled from Telsa? double p = Math.abs(1 / qOverP_gbl); double q = Math.signum(qOverP_gbl); double
         * tanLambda = Math.tan(lambda_gbl); double cosLambda = Math.cos(lambda_gbl); // Hep3Vector B = new
         * BasicHep3Vector(0, 0, Bz); // TODO sign convention? Hep3Vector H = new BasicHep3Vector(0, 0, 1); Hep3Vector T
         * = HelixUtils.Direction(helix, 0.); Hep3Vector HcrossT = VecOp.cross(H, T); double alpha =
         * HcrossT.magnitude(); // this should be Bvec cross TrackDir/|B| double Q = Bz * q / p; Hep3Vector Z = new
         * BasicHep3Vector(0, 0, 1); Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
         * Hep3Vector K = Z; Hep3Vector U = VecOp.mult(-1, J); Hep3Vector V = VecOp.cross(T, U); Hep3Vector I =
         * VecOp.cross(J, K); Hep3Vector N = VecOp.mult(1 / alpha, VecOp.cross(H, T)); double UdotI = VecOp.dot(U, I);
         * double NdotV = VecOp.dot(N, V); double NdotU = VecOp.dot(N, U); double TdotI = VecOp.dot(T, I); double VdotI
         * = VecOp.dot(V, I); double VdotK = VecOp.dot(V, K); covariance_gbl.set(HelicalTrackFit.dcaIndex,
         * FittedGblTrajectory.GBLPARIDX.XT.getValue(), VdotK / TdotI); covariance_gbl.set(HelicalTrackFit.phi0Index,
         * FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1); covariance_gbl.set(HelicalTrackFit.phi0Index,
         * FittedGblTrajectory.GBLPARIDX.XT.getValue(), -alpha * Q * UdotI * NdotU / (cosLambda * TdotI));
         * covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -alpha * Q * VdotI
         * * NdotU / (cosLambda * TdotI)); covariance_gbl.set(HelicalTrackFit.curvatureIndex,
         * FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), -1 * Bz / cosLambda); //
         * covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 0);
         * covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1 * q *
         * Bz * tanLambda / (p * cosLambda)); covariance_gbl.set(HelicalTrackFit.curvatureIndex,
         * FittedGblTrajectory.GBLPARIDX.XT.getValue(), q * Bz * alpha * Q * tanLambda * UdotI * NdotV / (p * cosLambda
         * * TdotI)); covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), q
         * * Bz * alpha * Q * tanLambda * VdotI * NdotV / (p * cosLambda * TdotI));
         * covariance_gbl.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -1 / TdotI);
         * covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1);
         * covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), alpha * Q * UdotI
         * * NdotV / TdotI); covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(),
         * alpha * Q * VdotI * NdotV / TdotI); covariance_gbl.print(15, 13);
         */

        // Use projection matrix
        // TODO should this not be the corrected helix?
        // It's a very small effect, but yes since lambda gets corrected and the Projection depends on lambda (only).
        // PF::08/07/2020
        // However I noticed several cases where the jacobian is singular. Those cases should be investigated
        // But for the moment revert to the old computation, although I think it is not fully correct. 
        
        //Hep3Matrix perToClPrj = getPerToClPrj(helicalTrackFit);
        double tanLambda = helicalTrackFitAtIPCorrected.slope();
        
        
        //Hep3Matrix perToClPrj   = getPerToClPrj(helicalTrackFitAtIPCorrected);
        Hep3Matrix perToClPrj     = getPerToClPrj(helicalTrackFit);
        
        //This has been checked and is equivalent to perToClPrj. In some cases the matrix is indetermined tho. 
        //TODO::Check those cases. For the moment back to the old computation
        
        //Hep3Matrix perToClPrj    = getSimplePerToClPrj(tanLambda);
        
        //System.out.println("PF::DEBUG:: PerTOCl");
        //System.out.println(((BasicHep3Matrix)perToClPrj).toString());
        
        //System.out.println("PF::DEBUG:: PerTOCl V2");
        //System.out.println(((BasicHep3Matrix)SperToClPrj).toString());
        

        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj); //Transpose should work
        double C_gbl = helicalTrackFitAtIPCorrected.curvature();
        //double lambda_gbl = Math.atan(helicalTrackFitAtIPCorrected.slope());
        
        double cosLambda = 1. / Math.sqrt(1. + (tanLambda * tanLambda));
        double babs = Math.abs(bfield);

        //double qOverP_gbl = helicalTrackFitAtIPCorrected.curvature()
        //       / (Constants.fieldConversion * babs * Math.sqrt(1 + Math.pow(
        //              helicalTrackFitAtIPCorrected.slope(), 2)));
        
        double qOverP_gbl = (C_gbl * cosLambda) / (Constants.fieldConversion*babs);
        
        Matrix jacobian = new Matrix(5, 5);
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), -clToPerPrj.e(1, 0));
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -clToPerPrj.e(1, 1));
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1.0);
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(0, 1) * C_gbl);
        //jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(),
        //        Constants.fieldConversion * Math.abs(bfield) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(),
                     Constants.fieldConversion * babs / cosLambda);
        
        //jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(),
        //      Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl * Math.tan(lambda_gbl) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(),
                     Constants.fieldConversion * babs * qOverP_gbl * tanLambda / cosLambda);

        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.XT.getValue(), clToPerPrj.e(2, 0));
        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(2, 1));
        //jacobian.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(),
        //        Math.pow(Math.cos(lambda_gbl), -2.0));
        jacobian.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(),1./(cosLambda*cosLambda));
                
        return jacobian;
    }
    
    /** 
     * Computes the projection matrix from perigee to curvilinear frame, with reference point(0,0,0) for the perigee frame. 
     * The projection matrix only depends on tanLambda.
     *
     * @param tanLambda
     * @return 3x3 projection matrix
     */

    static Hep3Matrix getSimplePerToClPrj(double tanLambda) {
        BasicHep3Matrix trans = new BasicHep3Matrix();
        double cosLambda = 1. / Math.sqrt(1 + tanLambda*tanLambda);
        double sinLambda = Math.sqrt(1. - cosLambda*cosLambda);
        //Only filling non zero entries
        
        trans.setElement(0, 1, -1);
        trans.setElement(1, 0, sinLambda);
        trans.setElement(1, 2, cosLambda);
        trans.setElement(2, 0, -cosLambda);
        trans.setElement(2, 2, sinLambda);
        return trans;
    }
     
    
    /**
     * Computes the projection matrix from the perigee XY plane variables dca and z0 into the curvilinear xT,yT,zT frame
     * (U,V,T) with reference point (0,0,0) for the perigee frame.
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
         * Hep3Vector B = new BasicHep3Vector(0, 0, 1); // TODO sign convention? Hep3Vector H = VecOp.mult(1 / bfield,
         * B); Hep3Vector T = HelixUtils.Direction(helix, 0.); Hep3Vector HcrossT = VecOp.cross(H, T); double alpha =
         * HcrossT.magnitude(); // this should be Bvec cross TrackDir/|B| double Q = Math.abs(bfield) * q / p;
         * Hep3Vector Z = new BasicHep3Vector(0, 0, 1); Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(),
         * VecOp.cross(T, Z)); Hep3Vector K = Z; Hep3Vector U = VecOp.mult(-1, J); Hep3Vector V = VecOp.cross(T, U);
         * Hep3Vector I = VecOp.cross(J, K); Hep3Vector N = VecOp.mult(1 / alpha, VecOp.cross(H, T));
         * //-cross(T,H)/alpha = -cross(T,Z) = -J double UdotI = VecOp.dot(U, I); // 0,0 double NdotV = VecOp.dot(N, V);
         * // 1,1? double NdotU = VecOp.dot(N, U); // 0,1? double TdotI = VecOp.dot(T, I); // 2,0 double VdotI =
         * VecOp.dot(V, I); // 1,0 double VdotK = VecOp.dot(V, K); // 1,2
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
