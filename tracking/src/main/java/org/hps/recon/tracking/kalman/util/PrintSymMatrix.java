package org.hps.recon.tracking.kalman.util;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.TrackError;

import Jama.Matrix;

/**
 * 
 * Print a square matrix, assumed to be symmetric, as sqrt(diagonal elements)
 * and correlation coefficients.
 *
 *@author $Author: jeremy $
 *@version $Id: PrintSymMatrix.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */


public class  PrintSymMatrix{

    public PrintSymMatrix(){
    }

    public void Print( TrackError e){
	Print (e.getMatrix());
    }

    public void Print( ETrack tre){
    	System.out.printf ( "Params: %14.8e %14.8e %14.8e %14.8e %14.8e\n",
			    tre.vector(0),
			    tre.vector(1),
			    tre.vector(2),
			    tre.vector(3),
			    tre.vector(4)
			    );
	Print (tre.error().getMatrix());
    }

    public void Print( Matrix eta, Matrix m){
    	System.out.printf ( "Params: %14.8e %14.8e %14.8e %14.8e %14.8e\n",
			    eta.get(0,0),
			    eta.get(1,0),
			    eta.get(2,0),
			    eta.get(3,0),
			    eta.get(4,0)
			    );
	Print (m);
    }


    public void Print( Matrix m){
	int dim = m.getColumnDimension();
	if ( m.getRowDimension() != dim ) return;
	
	double[] diag = new double[dim];
	System.out.printf ( "Diag: " );
	for ( int i=0; i<dim; ++i ){
	    if ( m.get(i,i) >= 0 ){
		diag[i] = Math.sqrt(m.get(i,i));
	    }else{
		diag[i] = -1.;
	    }
	    System.out.printf ( " %14.8e", diag[i] );
	}
	System.out.printf("\n");

	for ( int i=0; i<dim; ++i ){
	    for ( int j=i+1; j<dim; ++j ){
		double rho = -2.;
		if ( diag[i] >0. && diag[j] >0. ){
		    rho = m.get(i,j)/diag[i]/diag[j];
		}
		System.out.printf ("%22.16f", rho );
	    }
	    System.out.printf("\n");
	}
    }	
}
