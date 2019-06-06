package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import java.util.Arrays;

/**
 *
 * @author ngraf
 */
public class CbmLitKalmanFilterTest extends TestCase
{

    /**
     * Test of Update method, of class CbmLitKalmanFilter.
     */
    public void testCbmLitKalmanFilter()
    {
        System.out.println("testing");
        SimpleDetector det = new SimpleDetector();
        double[] zPlanes = {1., 2., 3., 4., 5.};
        for (int i = 0; i < zPlanes.length; ++i) {
            CbmLitMaterialInfo m = new CbmLitMaterialInfo();
            double z = i;
            det.addDetectorPlane(m);
        }
        // create a simple magnetic field
        CbmLitField field = new ConstantMagneticField(0., 0., 0.);
        // create an extrapolator...
        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);
        //the Kalman Filter updater
        CbmLitTrackUpdate update = new CbmLitKalmanFilter();

        CbmLitTrackParam parIn = new CbmLitTrackParam();
        CbmLitTrackParam parOut = new CbmLitTrackParam();

        double sigmax = .002;
        double sigmay = .002;
        double sigmaxy = 0.;

        Random ran = new Random();
        // generate an input track
        CbmLitTrackParam par = new CbmLitTrackParam();
        //TODO replace this with a state generator
        double slope = .1;
        double intercept = .5;
        double[] pars = new double[5];
        pars[0] = 0.; //x
        pars[1] = 0.; //y
        pars[2] = 0.; // x'
        pars[3] = 0.1; // y'
        pars[4] = 1.0; // q/p
        par.SetStateVector(pars);
        par.SetZ(0.);
        // also need a starting covariance matrix...
        // upperdiagonal
        double[] cov = new double[15];
        cov[0] = sigmax;
        cov[5] = sigmay;
        cov[9] = 99999.;
        cov[12] = 99999.;
        cov[14] = 99999.;
        par.SetCovMatrix(cov);

        double[] F = new double[25];

        CbmLitTrackParam parHere = new CbmLitTrackParam();
        List<CbmLitHit> hitList = new ArrayList<CbmLitHit>();
        // swim the track to each detector station
        double[] smearY = new double[5];
        double[] Y = new double[5];
        double[] Z = new double[5];
        double[] chisq = new double[2];
        for (int j = 0; j < 5; ++j) {
            //               System.out.println(zPlanes[j]);
            LitStatus stat = extrap.Extrapolate(par, parOut, zPlanes[j], null);
//                System.out.println(j + " : par    " + par);
//                System.out.println(j + " : parOut " + parOut);
//                System.out.println(j + " : " + "x= " + parOut.GetX() + " y= " + parOut.GetY() + " z= " + parOut.GetZ());
            // generate hits by smearing the positions
            // start with strip hits
            CbmLitStripHit hit = new CbmLitStripHit();

            hit.SetDz(0.);
            // smear x and y independently...
            smearY[j] = sigmay * ran.nextGaussian();
            Y[j] = parOut.GetY() + smearY[j];
            Z[j] = parOut.GetZ();
            hit.SetU(parOut.GetY() + smearY[j]);
            hit.SetDu(sigmay);
            hit.SetZ(parOut.GetZ());
            hit.SetPhi(PI / 2.);
            hit.SetHitType(LitHitType.kLITSTRIPHIT);
            hitList.add(hit);

            //propagate the original track parameters to this hit...
            extrap.Extrapolate(par, parHere, hit.GetZ(), F);
            System.out.println("z: " + parHere.GetZ() + " y: " + parHere.GetY() + " Z: " + Z[j] + " smeared y: " + Y[j] + " smearing: " + smearY[j]);
            //System.out.println(hit);
//            update.Update(parHere,parOut,hit,chisq);
//            System.out.println("parHere: "+parHere);
//            System.out.println("parOut : "+parOut);
//            System.out.println("chisq  : "+chisq[0]);
//            
        }

        // done simulating track and detector response
        System.out.println("hitList has " + hitList.size() + " hits in it");

        // least squares fit of straight line in z,y
        double[] params = new double[4]; // intercept, slope, int err, slope err
        double[] sigmaY = new double[5];
        Arrays.fill(sigmaY, sigmay);
        fit(params, Z, Y, sigmaY, 5);

            System.out.println("straight line fit: ");
        System.out.println(Arrays.toString(params));

        double[] params2 = new double[6]; // intercept, slope, int err, slope err, simaab, chisq
        fit2(Z, Y, sigmaY, 5, params2);
        System.out.println("straight line fit: ");
        System.out.println(Arrays.toString(params2));

    }

    public static void fit(double[] parameters, double[] x, double[] y,
                           double[] sigma_y, int num_points)
    {

        double s = 0.0, sx = 0.0, sy = 0.0, sxx = 0.0, sxy = 0.0, del;

        for (int i = 0; i < num_points; i++) {

            s += 1.0 / (sigma_y[i] * sigma_y[i]);
            sx += x[i] / (sigma_y[i] * sigma_y[i]);
            sy += y[i] / (sigma_y[i] * sigma_y[i]);
            sxx += (x[i] * x[i]) / (sigma_y[i] * sigma_y[i]);
            sxy += (x[i] * y[i]) / (sigma_y[i] * sigma_y[i]);
        }
        del = s * sxx - sx * sx;
        // Intercept
        parameters[0] = (sxx * sy - sx * sxy) / del;
        // Slope
        parameters[1] = (s * sxy - sx * sy) / del;
        // Errors  (sd**2) on the:
        // intercept
        parameters[2] = sxx / del;
        // and slope
        parameters[3] = s / del;
    } // fit

    public static boolean fit2(double[] x, double[] y, double[] sigma_y, int n, double[] params)
    {
        double sum, sx, sy, sxx, sxy, syy, det;
        double chisq = 999999.0;
        int i;

        if (n < 2) {
            return false; //too few points, abort
        }

        //initialization
        sum = sx = sy = sxx = sxy = syy = 0.;

        //find sum , sumx ,sumy, sumxx, sumxy
        double[] w = new double[n];
        for (i = 0; i < n; ++i) {
            w[i] = 1 / (sigma_y[i] * sigma_y[i]);
            sum += w[i];
            sx += w[i] * x[i];
            sy += w[i] * y[i];
            sxx += w[i] * x[i] * x[i];
            sxy += w[i] * x[i] * y[i];
            syy += w[i] * y[i] * y[i];
        }

        det = sum * sxx - sx * sx;
        if (Math.abs(det) < 1.0e-20) {
            return false; //Zero determinant, abort
        }
        //compute the best fitted parameters A,B

        double slope = (sum * sxy - sx * sy) / det;
        double intercept = (sy * sxx - sxy * sx) / det;

        //calculate chisq-square
        chisq = 0.0;
        for (i = 0; i < n; ++i) {
            chisq += w[i] * ((y[i]) - slope * (x[i]) - intercept) * ((y[i]) - slope * (x[i]) - intercept);
        }

        double slopeErr = sqrt(sum / det);
        double interceptErr = sqrt(sxx / det);
        double sigab = -sx / det;

        params[0] = intercept;
        params[1] = slope;
        params[2] = interceptErr;
        params[3] = slopeErr;
        params[4] = sigab;
        params[5] = chisq;
        return true;
    }
}
