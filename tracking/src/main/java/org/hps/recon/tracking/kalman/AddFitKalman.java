package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfutil.Assert;

import Jama.Matrix;
// Fit tracks using Kalman filter.

/**
 *AddFitKalman  uses a Kalman filter to update the track fit.
 *
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */
public class AddFitKalman extends AddFitter {

    boolean _DEBUG = false;
    // Maximum allowed hit dimension.
    private static final int MAXDIM = 3;
    // Maximum number of track vectors.
    private static final int MAXVECTOR = 1;
    // Maximum number of track errors.
    private static final int MAXERROR = 1;

    //private:  // nested classes
    // The nested class Box holds the vectors, matrices and symmetric
    // matrices needed for adding a hit in the main class.
    class Box {
        /*
        private:  // typedefs
        typedef Ptr<TrfVector,AutoPolicy>   VectorPtr;
        typedef Ptr<TrfSMatrix,AutoPolicy>  SMatrixPtr;
        typedef Ptr<TrfMatrix,AutoPolicy>   MatrixPtr;
        typedef vector<VectorPtr>           VectorList;
        typedef vector<SMatrixPtr>          SMatrixList;
        typedef vector<MatrixPtr>           MatrixList;
         */

        // enums
        // number of vectors
        private static final int NVECTOR = 2;
        // number of errors
        private static final int NERROR = 3;
        // number of vectors
        private static final int NDERIV = 2;
        // number of gains
        private static final int NGAIN = 2;
        // attributes
        // dimension of the vector, matrix, etc
        private int _size;
        // array of vectors
        List _vectors;
        // array of error matrices
        List _errors;
        // array of derivatives (Nx5 matrices)
        List _derivs;
        // array of gains (5xN matrices)
        List _gains;

        // methods
        // constructor
        public Box(int size) {
            _size = size;
            _vectors = new ArrayList();
            _errors = new ArrayList();
            _derivs = new ArrayList();
            _gains = new ArrayList();

            int icnt;
            // Track vectors
            for (icnt = 0; icnt < NVECTOR; ++icnt)
                _vectors.add(new Matrix(size, 1));
            // Track errors
            for (icnt = 0; icnt < NERROR; ++icnt)
                _errors.add(new Matrix(size, size));
            // Track derivatives
            for (icnt = 0; icnt < NDERIV; ++icnt)
                _derivs.add(new Matrix(size, 5));
            // Gains
            for (icnt = 0; icnt < NDERIV; ++icnt)
                _gains.add(new Matrix(5, size));

        }

        // return the dimension
        public int get_size() {
            return _size;
        }
        // fetch a vector

        public Matrix get_vector(int ivec) {
            return (Matrix) _vectors.get(ivec);
        }
        // fetch an error

        public Matrix get_error(int ierr) {
            return (Matrix) _errors.get(ierr);
        }
        // fetch a derivative

        public Matrix get_deriv(int ider) {
            return (Matrix) _derivs.get(ider);
        }
        // fetch a gain

        public Matrix get_gain(int igai) {
            return (Matrix) _gains.get(igai);
        }
    } // end of Box inner class

    // static methods
    //
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String typeName() {
        return "AddFitKalman";
    }

    //
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String staticType() {
        return typeName();
    }
    // attributes
    // Array of boxes for each supported size.
    private List _boxes;
    // Track vectors.
    private List _tvectors;
    // Track errors.
    private List _terrors;

    //methods
    //
    /**
     *Construct a default instance.
     * Allocate space needed for hits of dimension from 1 to MAXDIM.
     *
     */
    public AddFitKalman() {
        _boxes = new ArrayList();
        _tvectors = new ArrayList();
        _terrors = new ArrayList();

        // Create boxes for hit containers.
        for (int dim = 1; dim < MAXDIM; ++dim)
            _boxes.add(new Box(dim));

        int icnt;

        for (icnt = 0; icnt < MAXVECTOR; ++icnt)
            _tvectors.add(new Matrix(5, 1));

        for (icnt = 0; icnt < MAXERROR; ++icnt)
            _terrors.add(new Matrix(5, 5));

    }

    //
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public String type() {
        return staticType();
    }

    //
    /**
     *Add a hit and fit with the new hit.
     * Use a Kalman filter to add a hit to a track.
     * The hit is updated with the input track.
     * Note: We make direct use of the underlying vector and matrix
     *       classes here.  It will probably be neccessary to modify
     *       this routine if these are changed.
     *
     * @param   tre The ETrack to update.
     * @param   chsq The chi-square for the fit.
     * @param   hit The Hit to add to the track.
     * @return  0 if successful.
     */
    public int addHitFit(ETrack tre, double chsq, Hit hit) {
        // Update the hit with the input track.
        hit.update(tre);

        // Fetch hit size.
        int dim = hit.size();
        Assert.assertTrue(dim <= MAXDIM);

        // Fetch the box holding the needed hit containers.
        // The chice of boxes depends on the size of the hit.
        Box box = (Box) _boxes.get(dim - 1);
        Assert.assertTrue(box.get_size() == dim);

        // Fetch the hit containers.
        Matrix diff = box.get_vector(0);
        Matrix hit_res = box.get_vector(1);
        Matrix hit_err = box.get_error(0);
        Matrix hit_err_tot = box.get_error(1);
        Matrix hit_res_err = box.get_error(2);
        Matrix dhit_dtrk = box.get_deriv(0);
        Matrix new_dhit_dtrk = box.get_deriv(1);
        Matrix trk_err_dhit_dtrk = box.get_gain(0);
        Matrix gain = box.get_gain(1);

        // Fetch the track containers.
        Matrix new_vec = (Matrix) _tvectors.get(0);
        Matrix new_err = (Matrix) _terrors.get(0);

        hit_err = hit.measuredError().matrix();
        //System.out.println("hit_err= \n"+hit_err);

        // Fetch track prediction of hit.
        diff = hit.differenceVector().matrix();
        //System.out.println("diff= \n"+diff);
        dhit_dtrk = hit.dHitdTrack().matrix();
        //System.out.println("dhit_dtrk= \n"+dhit_dtrk);
        // Fetch track info.
        Matrix trk_vec = tre.vector().matrix();
        Matrix trk_err = tre.error().getMatrix(); //need to fix this!

        //System.out.println("trk_vec= \n"+trk_vec);
        //System.out.println("trk_err= \n"+trk_err);

        // Build gain matrix.
        hit_err_tot = hit.predictedError().matrix().plus(
                hit_err);
        //System.out.println("hit_err_tot= \n"+hit_err_tot);
        //if ( invert(hit_err_tot)!=0 ) return 3;
        hit_err_tot = hit_err_tot.inverse();
        //System.out.println("hit_err_tot inverse= \n"+hit_err_tot);
        trk_err_dhit_dtrk = trk_err.times(dhit_dtrk.transpose());
        gain = trk_err_dhit_dtrk.times(hit_err_tot);
        //System.out.println("trk_err_dhit_dtrk= \n"+trk_err_dhit_dtrk);
        //System.out.println("gain= \n"+gain);

        //  if ( get_debug() ) {
        //System.out.println("\n");
        //System.out.println("      trk_vec: " + "\n"+       trk_vec + "\n");
        //System.out.println("      trk_err: " + "\n"+       trk_err + "\n");
        //System.out.println("    dhit_dtrk: " + "\n"+     dhit_dtrk + "\n");
        //  }

        // We need to return dhit_dtrk to its original state for the
        // next call.
        // dhit_dtrk = dhit_dtrk.transpose(); //need to check this!
        dhit_dtrk.transpose();
        // Build new track vector.
        new_vec = trk_vec.minus(gain.times(diff));
        //System.out.println("new_vec= \n"+new_vec);

        // Build new error;
        new_err = trk_err.minus(trk_err_dhit_dtrk.times(hit_err_tot.times(trk_err_dhit_dtrk.transpose())));
        //System.out.println("new_err= \n"+new_err);
        // Check the error.
        {
            int nbad = 0;
            for (int i = 0; i < 5; ++i) {
                if (new_err.get(i, i) < 0.0)
                    ++nbad;
                double eii = new_err.get(i, i);
                for (int j = 0; j < i; ++j) {
                    double ejj = new_err.get(j, j);
                    double eij = new_err.get(j, i);
                    if (Math.abs(eij * eij) >= eii * ejj)
                        ++nbad;
                }
            }
            if (nbad > 0)
                return 5;
        }

        // Create track vector with new values.
        tre.setVectorAndKeepDirection(new TrackVector(new_vec));
        tre.setError(new TrackError(new_err));

        // Calculate residual vector.

        // Update the hit with the new track.
        //System.out.println("update the hit");
        hit.update(tre);

        hit_res = hit.differenceVector().matrix();
        new_dhit_dtrk = hit.dHitdTrack().matrix();
        //System.out.println("new_dhit_dtrk= \n"+new_dhit_dtrk);

        // Calculate residual covariance and invert.
        hit_res_err = hit_err.minus(dhit_dtrk.times(new_err.times(dhit_dtrk.transpose())));
        //		System.out.println("hit_res_err= \n"+hit_res_err);
        hit_res_err = hit_res_err.inverse();
        //System.out.println("hit_res_err inverse= \n"+hit_res_err);
        // Update chi-square.
        // result should be 1x1 matrix, so should be able to do the following
        //System.out.println( " hr*hre*hrT=\n"+(hit_res.transpose()).times(hit_res_err.times(hit_res)) );
        double dchsq = (hit_res.transpose()).times(hit_res_err.times(hit_res)).get(0, 0);
        //System.out.println("chsq= "+chsq+", dchsq= "+dchsq);
        chsq = chsq + dchsq;
        setChisquared(chsq);

        if (_DEBUG) {
            System.out.println("         gain: " + "\n" + gain + "\n");
            System.out.println("      new_vec: " + "\n" + new_vec + "\n");
            System.out.println("      new_err: " + "\n" + new_err + "\n");
            System.out.println("new_dhit_dtrk: " + "\n" + new_dhit_dtrk + "\n");
            System.out.println("      hit_res: " + "\n" + hit_res + "\n");
            System.out.println("  hit_res_err: " + "\n" + hit_res_err + "\n");
            System.out.println(" dchsq & chsq: " + "\n" + dchsq + " " + chsq + "\n");
        }

        return 0;

    }

    /**
     *output stream
     *
     * @return The String representation of this instance.
     */
    public String toString() {
        return getClass().getName();
    }
}
