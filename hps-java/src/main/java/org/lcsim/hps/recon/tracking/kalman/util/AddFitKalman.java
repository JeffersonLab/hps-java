package org.lcsim.hps.recon.tracking.kalman.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Hit;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trffit.AddFitter;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.util.aida.AIDA;

import Jama.Matrix;


// Fit tracks using Kalman filter.

/**
 * Copied from org.lcsim.recon.tracking.trffit.AddFitKalman
 * and added debug printout.  
 *
 * Original author was Norman Graf.
 * 
 * AddFitKalman  uses a Kalman filter to update the track fit.
 *
 * 
 *@author $Author: jeremy $
 *@version $Id: AddFitKalman.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 */

public class AddFitKalman extends AddFitter
{

    private AIDA aida = AIDA.defaultInstance();
    
    
    // Maximum allowed hit dimension.
    private static final int MAXDIM = 3;
    
    // Maximum number of track vectors.
    private static final int MAXVECTOR = 1;
    
    // Maximum number of track errors.
    private static final int MAXERROR = 1;
    
    //private:  // nested classes
    
    // The nested class Box holds the vectors, matrices and symmetric
    // matrices needed for adding a hit in the main class.
    class Box
    {
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
        private static final int  NVECTOR = 2 ;
        // number of errors
        private static final int NERROR = 3 ;
        // number of vectors
        private static final int  NDERIV = 2 ;
        // number of gains
        private static final int  NGAIN = 2 ;
        
        // attributes
        // dimension of the vector, matrix, etc
        private int _size;
        // array of vectors
        List  _vectors;
        // array of error matrices
        List _errors;
        // array of derivatives (Nx5 matrices)
        List _derivs;
        // array of gains (5xN matrices)
        List _gains;
        
        // methods
        // constructor
        public Box(int size)
        {
            _size= size;
            _vectors = new ArrayList();
            _errors = new ArrayList();
            _derivs = new ArrayList();
            _gains = new ArrayList();
            
            int icnt;
            // Track vectors
            for ( icnt=0; icnt<NVECTOR; ++icnt )
                _vectors.add(  new Matrix(size,1) );
            // Track errors
            for ( icnt=0; icnt<NERROR; ++icnt )
                _errors.add(  new Matrix(size,size) );
            // Track derivatives
            for ( icnt=0; icnt<NDERIV; ++icnt )
                _derivs.add(  new Matrix(size,5) );
            // Gains
            for ( icnt=0; icnt<NDERIV; ++icnt )
                _gains.add(  new Matrix(5,size) );
            
        }
        
        // return the dimension
        public int get_size()
        { return _size;
        }
        // fetch a vector
        public Matrix get_vector(int ivec)
        {
            return (Matrix) _vectors.get(ivec);
        }
        // fetch an error
        public Matrix get_error(int ierr)
        {
            return (Matrix) _errors.get(ierr);
        }
        // fetch a derivative
        public Matrix get_deriv(int ider)
        {
            return (Matrix) _derivs.get(ider);
        }
        // fetch a gain
        public Matrix get_gain(int igai)
        {
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
    public static String typeName()
    { return "AddFitKalman";
    }
    
    //
    
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public static String staticType()
    { return typeName();
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
    
    int AddFitKalmanDebugLevel = 0;
    
    
    /**
     *Construct a default instance.
     * Allocate space needed for hits of dimension from 1 to MAXDIM.
     *
     */
    public AddFitKalman()
    {
        _boxes = new ArrayList();
        _tvectors = new ArrayList();
        _terrors = new ArrayList();
        
        // Create boxes for hit containers.
        for ( int dim=1; dim<MAXDIM; ++dim )
            _boxes.add( new Box(dim) );
        
        int icnt;
        
        for ( icnt=0; icnt<MAXVECTOR; ++icnt )
            _tvectors.add(  new Matrix(5, 1) );
        
        for ( icnt=0; icnt<MAXERROR; ++icnt )
            _terrors.add(  new Matrix(5,5) );

	try{
	    ToyConfig config = ToyConfig.getInstance();
	    AddFitKalmanDebugLevel = config.getInt( "AddFitKalmanDebugLevel", 
						    AddFitKalmanDebugLevel );
	    

	} catch (ToyConfigException e){
            System.out.println (e.getMessage() );
            System.out.println ("Stopping now." );
            System.exit(-1);
        }
	
        
    }
    
    //
    
    /**
     *Return a String representation of the class' the type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' the type name.
     */
    public String type()
    { return staticType();
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
    public int addHitFit(ETrack tre, double chsq,  Hit hit)
    {

        // Update the hit with the input track.
        hit.update(tre);
        
        // Fetch hit size.
        int dim = hit.size();
        Assert.assertTrue( dim <= MAXDIM );
        
        // Fetch the box holding the needed hit containers.
        // The chice of boxes depends on the size of the hit.
        Box box = (Box)_boxes.get(dim-1);
        Assert.assertTrue( box.get_size() == dim );
        
        // Fetch the hit containers.
        Matrix diff    = box.get_vector(0);
        Matrix hit_res = box.get_vector(1);
        Matrix hit_err     = box.get_error(0);
        Matrix hit_err_tot = box.get_error(1);
        Matrix hit_res_err = box.get_error(2);
        Matrix dhit_dtrk         = box.get_deriv(0);
        Matrix new_dhit_dtrk     = box.get_deriv(1);
        Matrix trk_err_dhit_dtrk = box.get_gain(0);
        Matrix gain              = box.get_gain(1);
        Matrix gain2             = gain.copy();

        // Fetch the track containers.
        Matrix new_vec = (Matrix) _tvectors.get(0);
        Matrix new_err = (Matrix)  _terrors.get(0);
        
        hit_err = hit.measuredError().matrix();
	Matrix Vm    = hit_err.copy();
	Matrix VmInv = Vm.inverse();
        //System.out.println("hit_err= \n"+hit_err);
        
        // Fetch track prediction of hit.
        diff = hit.differenceVector().matrix();
        //System.out.println("diff= \n"+diff);
        dhit_dtrk = hit.dHitdTrack().matrix();
	Matrix D = dhit_dtrk.copy();
        //System.out.println("dhit_dtrk= \n"+dhit_dtrk);
        // Fetch track info.
        Matrix trk_vec = tre.vector().matrix();
        Matrix trk_err = tre.error().getMatrix(); //need to fix this!
        Matrix V       = tre.error().getMatrix();
	VTUtil vtu = new VTUtil( tre );
        
        //System.out.println("trk_vec= \n"+trk_vec);
        //System.out.println("trk_err= \n"+trk_err);
        
        // Build gain matrix.
        hit_err_tot = hit.predictedError().matrix().plus(
                hit_err);
        //System.out.println("hit_err_tot= \n"+hit_err_tot);
        //if ( invert(hit_err_tot)!=0 ) return 3;
	Matrix hetot  = new Matrix(2,2);
	Matrix hetoti = new Matrix(2,2);
	hetot = hit.predictedError().matrix().plus(hit_err);
	hetoti = hetot.inverse();
        hit_err_tot= hit_err_tot.inverse();

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

	Matrix Vnew = new_err.copy();
	D = D.transpose();
	gain2 = Vnew.times(D.times(VmInv));
	Matrix etap = trk_vec.minus(gain2.times(diff));

        //System.out.println("new_err= \n"+new_err);
        // Check the error.
        if ( AddFitKalmanDebugLevel > 0 ) {
            int nbad = 0;
            for ( int i=0; i<5; ++i )
            {
                if ( new_err.get(i,i) < 0.0 ) {
		    if ( nbad == 0 ) System.out.println("");
		    System.out.println ( "Bad on: " + i + " " + new_err.get(i,i) );
		    ++nbad;
		}
                double eii = new_err.get(i,i);
                for ( int j=0; j<i; ++j )
                {
                    double ejj = new_err.get(j,j);
                    double eij = new_err.get(j,i);
                    if ( Math.abs(eij*eij) >= eii*ejj  ) {
			double delta = -Math.abs(eij*eij) + eii*ejj;
			if ( nbad == 0 ) System.out.println("");
			System.out.println( "Bad off: " + i + " " 
					    + j + " "
					    + eii + " "
					    + ejj + " "
					    + eij + " "
					    + delta
					    );
			++nbad;
		    }
                }
            }
	    if ( nbad > 0 ){
		PrintSymMatrix psm = new PrintSymMatrix();
		System.out.println ("Illegal cov in addfitkalman: " 
				    + RKDebug.Instance().getTrack() + " "
				    + RKDebug.Instance().getPropDir() + " "
				    );
		System.out.println ("Surface:   " + tre.surface() );
		
		System.out.println ("old_err \n" );
		psm.Print(tre.error());

		System.out.println ("Predicted error: " );
		psm.Print(hit.predictedError().matrix() );

		System.out.println ("Measured error: " );
		psm.Print(hit.measuredError().matrix() );

		System.out.println ("Total error: " );
		psm.Print( hetot );

		System.out.println ("Inverse: " );
		psm.Print( hetoti );
		
		psm.Print( hetoti.times(hetot) );


		System.out.println ("Derivs: \n" + hit.dHitdTrack() );
		
		System.out.println ("new_err \n" );
		psm.Print(new_err);
	    }
            if ( nbad > 0 ) return 5;
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
        double dchsq = (hit_res.transpose()).times(hit_res_err.times(hit_res)).get(0,0);
        //System.out.println("chsq= "+chsq+", dchsq= "+dchsq);

	Matrix dEta = etap.minus(trk_vec);
	Matrix dM   = hit.differenceVector().matrix();
	Matrix VInv = V.inverse();

	/*
	System.out.printf ( "Dim1: (%d,%d) (%d,%d) (%d,%d)\n",
			    dEta.getRowDimension(),  dEta.getColumnDimension(),
			    dM.getRowDimension(),    dM.getColumnDimension(),
			    VmInv.getRowDimension(), VmInv.getColumnDimension() 
			    );
	*/
	double dch1 = ((dM.transpose()).times(VmInv.times(dM))).get(0,0);
	double dch2 = ((dEta.transpose()).times(VInv.times(dEta))).get(0,0);
	double dch  = dch1 + dch2;

	double ddd = dch-dchsq;

	if ( Math.abs(ddd) > 0.01 ){
	    if ( RKDebug.Instance().getPropDir() == PropDir.BACKWARD ){
		aida.cloud1D("/Bugs/Chisq/Back: Delta Chisq: ").fill(dch-dchsq);
		aida.cloud2D("/Bugs/Chisq/Back: Chisq TRF vs CLEO").fill(dch,dchsq);
		aida.cloud1D("/Bugs/Chisq/radius of bad chisq").fill( vtu.r() );
		aida.cloud1D("/Bugs/Chisq/z of bad chisq").fill( vtu.z() );
		aida.cloud2D("/Bugs/Chisq/Back: Chisq TRF vs r").fill( vtu.r(), dchsq );
		aida.cloud2D("/Bugs/Chisq/Back: Chisq TRF vs z").fill( vtu.z(), dchsq );
	    } else {
		aida.cloud1D("/Bugs/Chisq/Forw: Delta Chisq: ").fill(dch-dchsq);
		aida.cloud2D("/Bugs/Chisq/Forw: Chisq TRF vs CLEO").fill(dch,dchsq);
	    }
	} else {
	    if ( RKDebug.Instance().getPropDir() == PropDir.BACKWARD ){
		aida.cloud1D("/Bugs/Chisq/Back: Small Delta Chisq: ").fill(dch-dchsq);
	    } else {
		aida.cloud1D("/Bugs/Chisq/Forw: Small Delta Chisq: ").fill(dch-dchsq);
	    }
	}

	dchsq = dch;


	/*
	System.out.println ("Chitest: " + dchsq + " " + dch + " | " + dch1 + " " + dch2 );
	for ( int i=0; i<5; ++i ){
	    System.out.printf ("Eta  %3d %12.7f %12.7f %12.7f %12.7f %12.7f  | %12.7f \n", i,
			       trk_vec.get(i,0),
			       new_vec.get(i,0),
			       etap.get(i,0),
			       (new_vec.minus(etap)).get(i,0),
			       Math.sqrt(new_err.get(i,i)),
			       dEta.get(i,0)
			       );
		    
	}

	if ( Vm.getRowDimension() == 1){
	    Hack1D ( trk_vec, V, Vm, new_vec, new_err );
	} else if ( Vm.getRowDimension() == 2){
	    Hack2D ( trk_vec, V, Vm, diff, new_vec, new_err );
	}
	*/

	// Warn about bad chisquared contribution, ignoring small problems that might
	// well be just round off error.

	if ( dchsq <   -0.001 ){
	    /*
	    System.out.println ("\ndchisq: " 
				+ RKDebug.Instance().getTrack() + " "
				+ RKDebug.Instance().getPropDir() + " "
				+ dchsq + " "
				+ tre.surface() + " "
				+ RKDebug.Instance().getRKTrack().cz() + " "
				);
	    */
	    if ( RKDebug.Instance().getPropDir() == PropDir.BACKWARD ){
		aida.histogram1D( "/Bugs/Backward: cz for tracks with bad dchisquared",100,-1.,1.)
		    .fill( RKDebug.Instance().getRKTrack().cz() );
		aida.histogram1D( "/Bugs/Backward: bad dchisquared",100,-20.,0.).fill( dchsq );
		aida.cloud2D( "/Bugs/Backward: dch vs cz for tracks with bad dchisquared",-1)
		    .fill( RKDebug.Instance().getRKTrack().cz(), dchsq );
	    }else{
		aida.histogram1D( "/Bugs/Forward: cz for tracks with bad dchisquared",100,-1.,1.)
		    .fill( RKDebug.Instance().getRKTrack().cz() );
		aida.histogram1D( "/BugsForward: bad dchisquared",100,-20.,0.).fill( dchsq );
		aida.cloud2D( "/Bugs/Forward: dch vs cz for tracks with bad dchisquared",-1)
		    .fill( RKDebug.Instance().getRKTrack().cz(), dchsq );
	    }
	}

        chsq = chsq + dchsq;
        setChisquared(chsq);
        
        //  if ( get_debug() ) {
        //System.out.println("         gain: " + "\n"+          gain + "\n");
        //System.out.println("      new_vec: " + "\n"+       new_vec + "\n");
        //System.out.println("      new_err: " + "\n"+       new_err + "\n");
        //System.out.println("new_dhit_dtrk: " + "\n"+ new_dhit_dtrk + "\n");
        //System.out.println("      hit_res: " + "\n"+       hit_res + "\n");
        //System.out.println("  hit_res_err: " + "\n"+   hit_res_err + "\n");
        //System.out.println(" dchsq & chsq: " + "\n"+ dchsq + " " + chsq + "\n");
        //  }
        
        return 0;
        
    }
    
    
    /**
     *output stream
     *
     * @return The String representation of this instance.
     */
    public String toString()
    {
        return getClass().getName();
    }

    /**
     * Perform matrix operations as BigDecimal, specialized to a 1D measurement.
     *
     * 
     */
    private void Hack1D ( Matrix etain, 
			  Matrix Vin, 
			  Matrix Vm, 
			  Matrix etat,
			  Matrix Vt ){

	BigDecimal sigsq  =  new BigDecimal(  Vm.get(0,0), MathContext.DECIMAL128 );
	BigDecimal V00    =  new BigDecimal( Vin.get(0,0), MathContext.DECIMAL128 );
	BigDecimal denom  = sigsq.add(V00);
	
	BigDecimal[] Vi0 = { new BigDecimal( Vin.get(0,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(1,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(2,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(3,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(4,0), MathContext.DECIMAL128 ) };

	Matrix Vp = new Matrix(5,5);
	for ( int i=0; i<5; ++i ){
	    for ( int j=i; j<5; ++j ){
		BigDecimal Vij = new BigDecimal( Vin.get(i,j), MathContext.DECIMAL128 );
		BigDecimal numer = Vi0[i].multiply(Vi0[j]);
		BigDecimal ratio = numer.divide(denom,MathContext.DECIMAL128);
		BigDecimal Vijnew = Vij.subtract(ratio);
		Vp.set(i,j,Vijnew.doubleValue() );
	    }
	}
	System.out.println ("Hack 1D");
	PrintSymMatrix psm = new PrintSymMatrix();
	psm.Print(Vp);
    }

    /**
     * Perform matrix operations as BigDecimal, specialized to a 2D measurement.
     *
     * 
     */
    private void Hack2D ( Matrix etain, 
			  Matrix Vin, 
			  Matrix Vm,
			  Matrix dM,
			  Matrix etat,
			  Matrix Vt ){

	BigDecimal DTVD00 = new BigDecimal( Vin.get(0,0), MathContext.DECIMAL128 );
	BigDecimal DTVD11 = new BigDecimal( Vin.get(1,1), MathContext.DECIMAL128 );
	BigDecimal DTVD01 = new BigDecimal( Vin.get(0,1), MathContext.DECIMAL128 );

	BigDecimal sigsqx  =  new BigDecimal(  Vm.get(0,0), MathContext.DECIMAL128 );
	BigDecimal sigsqy  =  new BigDecimal(  Vm.get(1,1), MathContext.DECIMAL128 );

	BigDecimal Sum00 = DTVD00.add(sigsqx);
	BigDecimal Sum11 = DTVD11.add(sigsqy);
	BigDecimal Sum01 = DTVD01;

	BigDecimal Disc  = (Sum00.multiply(Sum11)).subtract(Sum01.multiply(Sum01));

	BigDecimal U00 = Sum11.divide(Disc,MathContext.DECIMAL128);
	BigDecimal U11 = Sum00.divide(Disc,MathContext.DECIMAL128);
	BigDecimal U01 = (Sum01.negate()).divide(Disc,MathContext.DECIMAL128);
	BigDecimal U10 = U01;


	
	BigDecimal[] Vi0 = { new BigDecimal( Vin.get(0,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(1,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(2,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(3,0), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(4,0), MathContext.DECIMAL128 ) };

	BigDecimal[] Vi1 = { new BigDecimal( Vin.get(0,1), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(1,1), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(2,1), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(3,1), MathContext.DECIMAL128 ),
			     new BigDecimal( Vin.get(4,1), MathContext.DECIMAL128 ) };

	// New covariance matrix, both as Matrix and as 2D array of BigDecimal.
	Matrix Vp = new Matrix(5,5);
	BigDecimal BigZero = new BigDecimal ( 0., MathContext.DECIMAL128 );
	BigDecimal[][] VpBig = { { BigZero, BigZero, BigZero, BigZero, BigZero},
				 { BigZero, BigZero, BigZero, BigZero, BigZero},
				 { BigZero, BigZero, BigZero, BigZero, BigZero},
				 { BigZero, BigZero, BigZero, BigZero, BigZero},
				 { BigZero, BigZero, BigZero, BigZero, BigZero} };


	for ( int i=0; i<5; ++i ){
	    for ( int j=i; j<5; ++j ){

		BigDecimal a = U00.multiply(Vi0[j]);
		BigDecimal b = U01.multiply(Vi1[j]);
		BigDecimal c = U01.multiply(Vi0[j]);
		BigDecimal d = U11.multiply(Vi1[j]);

		BigDecimal s1 = Vi0[i].multiply( a.add(b));
		BigDecimal s2 = Vi1[i].multiply( c.add(d));

		BigDecimal Vij = new BigDecimal( Vin.get(i,j), MathContext.DECIMAL128 );
		BigDecimal Vijnew = Vij.subtract( s1.add(s2) );

		Vp.set(i,j,Vijnew.doubleValue() );
		VpBig[i][j] = Vijnew;
		double denom = (Vp.get(i,j)+Vt.get(i,j))/2.;
		if ( denom != 0. ){
		    double frac = (Vp.get(i,j)-Vt.get(i,j))/denom;
		    aida.cloud1D( "/Bugs/AddFit/Frac Vdiff " + i + " " + j ).fill(frac);
		}
		if ( i != j ){
		    Vp.set(j,i,Vp.get(i,j));
		    VpBig[j][i] = Vijnew;
		}
	    }
	}

	// Predicted-measured vector as BigDecimal.
	BigDecimal dM0 = new BigDecimal( dM.get(0,0), MathContext.DECIMAL128);
	BigDecimal dM1 = new BigDecimal( dM.get(1,0), MathContext.DECIMAL128);

	// New track parameters, both as Matrix and as 2D array of BigDecimal.
	BigDecimal[] etanew = { BigZero, BigZero, BigZero, BigZero, BigZero };
	Matrix etap = new Matrix(5,1);
	for ( int i=0; i<5; ++i ){
	    BigDecimal rx = VpBig[i][0].divide(sigsqx,MathContext.DECIMAL128);
	    BigDecimal ry = VpBig[i][1].divide(sigsqy,MathContext.DECIMAL128);
	    BigDecimal deta = (rx.multiply(dM0)).add(ry.multiply(dM1));
	    BigDecimal etai = new BigDecimal( etain.get(i,0),MathContext.DECIMAL128);
	    etanew[i] = etai.subtract(deta);
	    etap.set(i,0,etanew[i].doubleValue());
	    aida.cloud1D("/Bugs/AddFit/Diff eta " + i).fill(etap.get(i,0)-etat.get(i,0));
	    double denom = (etap.get(i,0)+etat.get(i,0))/2.;
	    if ( denom != 0. ){
		double frac = (etap.get(i,0)-etat.get(i,0))/denom;
		aida.cloud1D("/Bugs/AddFit/Frac Diff eta " + i).fill(frac);
	    }

	}

	System.out.println ("Hack 2D " + dM0 + " " + dM1 );
	PrintSymMatrix psm = new PrintSymMatrix();
	psm.Print(etap, Vp);


    }
    
}

