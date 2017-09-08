package kalman;

import java.util.ArrayList;
import java.util.Iterator;

// Fit a line/parabola approximation to a helix to a set of measurement points.
// The line and parabola are fit simultaneously in order to handle properly the stereo layers.
// The pivot of the helix is assumed to be the origin of the coordinate system, but the
// coordinate system in which the helix is described may be rotated slightly with respect to
// the global coordinates, in order to optimize its alignment with the field.
class SeedTrack {                     		
	boolean success;                   		
	private double drho, phi0, K, dz, tanl; // Helix parameters derived from the line/parabola fits
	private RotMatrix Rot;                  // Orthogonal transformation from global to helix coordinates
	private SquareMatrix C;                 // Covariance of helix parameters
	private boolean verbose;				// Set true to generate lots of debug printout
	private double alpha;					// Conversion constant from 1/pt to helix radius
    private double R, xc, yc;				// Radius and center of the "helix"
    private Vec sol;						// Fitted polynomial coefficients
    private SquareMatrix Csol;				// Covariance matrix of the fitted polynomial coefficients
    private double Bavg;                    // Average B field
    private Vec tavg;                       // Average direction for the B field
	
	void print(String s) {
		if (success) {
			System.out.format("Seed track %s: B=%10.7f helix= %10.6f, %10.6f, %10.6f, %10.6f, %10.6f\n", s, Bavg, drho, phi0, K, dz, tanl);
			tavg.print("seed track field direction");
			C.print("seed track covariance");
		} else {
			System.out.format("Seed track %s fit unsuccessful.\n", s);
		}
	}
	
	SeedTrack(
		ArrayList<Measurement> data,   // List of measurement data points
		int Npnt,                      // Number of points to use (starting with the first)
	    boolean verbose                // Set true for lots of debug printout
		) {
		
		this.verbose = verbose;
		
		// Fit a straight line in the non-bending plane and a parabola in the bending plane
		
		double [] x = new double[data.size()];   // Global x coordinates of measurements (bending plane)
		double [] y = new double[data.size()];   // Global y coordinates of measurements (along beam direction)
		double [] z = new double[data.size()];   // Global z coordinates of measurements (along B field direction)
		double [] v = new double[data.size()];   // Measurement value (i.e. position of the hit strip)
		double [] s = new double[data.size()];   // Uncertainty in the measurement (spatial resolution of the SSD)
		double [] t = new double[data.size()];	 // Stereo angle
		int N = 0;
		int Nbending = 0;
		int Nnonbending = 0;
		if (verbose) System.out.format("Entering SeedTrack: Npnt=%d\n", Npnt);
		
		// First find the average field 
		Vec Bvec = new Vec(0.,0.,0.);
		Iterator<Measurement> itr = data.iterator();
		while (itr.hasNext() && N < Npnt) {     
			Measurement m = itr.next();
			Bvec = Bvec.sum(m.t.scale(m.B));
			N++;
		}
		Bvec = Bvec.scale(1.0/(double)N);
		Bavg = Bvec.mag();
		tavg = Bvec.unitVec();
		double c = 2.99793e8;             // Speed of light in m/s
		alpha = 1000.0*1.0e9/(c*Bavg);    // Convert from pt in GeV to curvature in mm
		Vec yhat = new Vec(0., 1.0, 0.);
		Vec uhat = yhat.cross(tavg).unitVec();
		Vec vhat = tavg.cross(uhat);
		Rot = new RotMatrix(uhat,vhat,tavg);
		
		N = 0;
		itr = data.iterator();
		while (itr.hasNext() && N < Npnt) {     
			Measurement m = itr.next();
			if (m.stereo == 0.) Nnonbending ++; else Nbending++;
			Vec pnt = new Vec(0., m.v, 0.);
			pnt = m.toGlobal(pnt);
			if (verbose) {
				System.out.format("Measurement %d = %10.7f\n", N, m.v);
				pnt.print("point global");
			}
			pnt = Rot.rotate(pnt);  // Rotate coordinate system to align with the average field
			x[N] = pnt.v[0];
			y[N] = pnt.v[1];
			z[N] = pnt.v[2];
			v[N] = z[N]*Math.cos(m.stereo) + x[N]*Math.sin(m.stereo);
			s[N] = m.sigma;
			t[N] = m.stereo;
			N++;
		}
		if (Nnonbending < 2) {
			System.out.format("SeedTrack: not enough points in the non-bending plane; N=%d\n", Nnonbending);
			success = false;
			return;
		}
		if (Nbending < 3) {
			System.out.format("SeedTrack: not enough points in the bending plane; N=%d\n", Nbending);
			success = false;
			return;
		}
		if (verbose) {
			System.out.format("SeedTrack: data in global coordinates: y, z, x, m, sigma, theta\n");
			for (int i=0; i<N; i++) {
				System.out.format("%d  %10.6f   %10.6f   %10.6f   %10.6f   %10.6f   %8.5f\n", i, y[i], z[i], x[i], v[i], s[i], t[i]);
			}
		}
		LinearHelixFit fit = new LinearHelixFit(N,y,v,s,t,verbose);
		if (verbose) {
			fit.print(N,y,v,s,t);
		}
		
		// Now, derive the helix parameters and covariance from the two fits
		double sgn = -1.0;         // Careful with this sign--->selects the correct half of the circle!
		sol = fit.solution();
		Vec coef = new Vec(sol.v[2], sol.v[3], sol.v[4]);   // Parabola coefficients
		
		double [] circleParams = parabolaToCircle(sgn, coef);
		if (verbose) System.out.format("     R=%10.6f, xc=%10.6f, yc=%10.6f\n", R, xc, yc); 
		phi0 = circleParams[1];
		K = circleParams[2];
		drho = circleParams[0];
		if (verbose) System.out.format("      phi0=%10.7f,  K=%10.6f,   drho=%10.6f\n", phi0, K, drho);
		double dphi0da = (2.0*coef.v[2]/coef.v[1])*square(Math.sin(phi0));
		double dphi0db = Math.sin(phi0)*Math.cos(phi0)/coef.v[1] - square(Math.sin(phi0));
		double dphi0dc = (2.0*coef.v[0]/coef.v[1])*square(Math.sin(phi0));
		SquareMatrix D = new SquareMatrix(5);
		double temp = xc*Math.tan(phi0)/Math.cos(phi0);

		double slope = sol.v[1];
		double intercept = sol.v[0];
		tanl = slope*Math.cos(phi0);
		dz = intercept + drho*tanl*Math.tan(phi0);		
		
		// Some derivatives to transform the covariance from line and parabola coefficients to helix parameters
		D.M[0][2] = 1.0/Math.cos(phi0) + temp*dphi0da;
		D.M[0][3] = -(coef.v[1]/(2.0*coef.v[2]))/Math.cos(phi0) + temp*dphi0db;
		D.M[0][4] = -(sgn+(1.0-0.5*coef.v[1]*coef.v[1])/Math.cos(phi0))/(2.0*coef.v[2]*coef.v[2]) + temp*dphi0dc;
		D.M[1][2] = dphi0da;
		D.M[1][3] = dphi0db;
		D.M[1][4] = dphi0dc;
		D.M[2][4] = -2.0*alpha*sgn;
		D.M[3][0] = 1.0;
		D.M[3][1] = drho*Math.sin(phi0);
		D.M[4][1] = Math.cos(phi0);
		Csol = fit.covariance();
		C = Csol.similarity(D);  // Covariance of helix parameters
		if (verbose) D.print("line/parabola to helix derivatives");


		// Note that the non-bending plane is assumed to be y,z (B field in z direction), and the track is assumed to start out more-or-less
		// in the y direction, so that phi0 should be close to zero.  phi0 at 90 degrees will give trouble here!
		success = true;
	}
	
	private double square(double x) {
		return x*x;
	}
	
	Vec helixParams() {  // Return the fitted helix parameters
		return new Vec(drho, phi0, K, dz, tanl);
	}
	
	SquareMatrix covariance() {  // Return covariance matrix of the fitted helix parameters
		return C;
	}
	
	Vec solution() {   // Return the 5 polynomial coefficients
		return sol;
	}
	
	Vec T() {   // Return the average field direction
		return tavg;
	}
	
	double B() {  // Return the average field
		return Bavg;
	}
	
	SquareMatrix solutionCovariance() {  // Return covariance of the polynomial coefficients
		return Csol;
	}
	
	Vec solutionErrors() {  // Return errors on the polynomial coefficients (for testing)
		return new Vec(Math.sqrt(Csol.M[0][0]),Math.sqrt(Csol.M[1][1]),Math.sqrt(Csol.M[2][2]),Math.sqrt(Csol.M[3][3]),Math.sqrt(Csol.M[4][4]));
	}
	
	Vec errors() {  // Return errors on the helix parameters
		return new Vec(Math.sqrt(C.M[0][0]),Math.sqrt(C.M[1][1]),Math.sqrt(C.M[2][2]),Math.sqrt(C.M[3][3]),Math.sqrt(C.M[4][4]));
	}
	
	private double [] parabolaToCircle(double sgn, Vec coef) {  // Utility to convert from parabola coefficients to circle (i.e. helix) parameters drho, phi0, and K
		R = -sgn/(2.0*coef.v[2]);
		yc = sgn*R*coef.v[1];
		xc = coef.v[0] - sgn*R*(1.0-0.5*coef.v[1]*coef.v[1]);
		double [] r= new double [3];
		r[1] = Math.atan2(yc, xc);
		if (R<0.) r[1] += Math.PI;
		r[2] = alpha/R;
		r[0] = xc/Math.cos(r[1]) - R;
		if (verbose) System.out.format("parabolaToCircle:     R=%10.6f, xc=%10.6f, yc=%10.6f, drho=%10.7f, phi0=%10.7f, K=%10.7f\n", R, xc, yc, r[0], r[1], r[2]); 
		//double phi02 = Math.atan(-coef.v[1]/(2.0*coef.v[0]*coef.v[2] + (1.0-coef.v[1]*coef.v[1])));
		//System.out.format("phi02 = %10.7f\n", phi02);
		return r;		
	}
}
