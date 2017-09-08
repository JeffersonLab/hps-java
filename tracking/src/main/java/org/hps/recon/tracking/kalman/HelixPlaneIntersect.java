package kalman;

class HelixPlaneIntersect {  // Calculates intersection of a helix with an arbitrary plane

	Plane p;
	Vec a;
	Vec X0;
	private double alpha;
	
	HelixPlaneIntersect(double alpha) {
		this.alpha = alpha;
	}

	double rtSafe(double xGuess, double x1, double x2, double xacc) { // Safe Newton-Raphson zero finding from Numerical Recipes in C
		double df, dx, dxold, f, fh, fl;
		double temp, xh, xl, rts;
		int MAXIT = 100;
		
		if (xGuess <= x1 || xGuess >= x2) {
			System.out.format("ZeroF.rtsafe: initial guess needs to be bracketed\n");
			return xGuess;
		}
		fl = S(x1);
		fh = S(x2);
		if ((fl>0.0 && fh>0.0) || (fl<0.0 && fh<0.0)) {
			System.out.format("ZeroFind.rtsafe: root is not bracketed in zero finding, fl=%12.5e, fh=%12.5e, alpha=%10.6f\n",fl, fh, alpha);
			p.print("internal plane");
			a.print("internal helix parameters");
			X0.print("internal pivot");
			return xGuess;
		}
		if (fl == 0.) return x1;
		if (fh == 0.) return x2;
		if (fl < 0.0) {
			xl = x1;
			xh = x2;
		} else {
			xh = x1;
			xl = x2;
		}
		rts = xGuess;
		dxold = Math.abs(x2-x1);
		dx = dxold;
		f = S(rts);
		df = dSdPhi(rts);
		for (int j=1; j<=MAXIT; j++) {
			if ((((rts-xh)*df-f)*((rts-xl)*df-f) > 0.0) || (Math.abs(2.0*f) > Math.abs(dxold*df))) {
				dxold = dx;
				dx = 0.5*(xh-xl);    // Use bisection if the Newton-Raphson method is going bonkers
				rts = xl + dx;
				if (xl == rts) return rts;
			} else {
				dxold = dx;
				dx = f/df;          // Newton-Raphson method
				temp = rts;
				rts -= dx;
				if (temp == rts) return rts;
			}
			if (Math.abs(dx) < xacc) {
				//System.out.format("ZeroFind.rtSafe: solution converged in %d iterations.\n", j);
				return rts;
			}
			f = S(rts);
			df = dSdPhi(rts);
			if (f < 0.0) {
				xl = rts;
			} else {
				xh = rts;
			}
		}
		System.out.format("ZeroFind.rtsafe: maximum number of iterations exceeded.\n");
		return rts;
	}

	private double dSdPhi(double phi) {
		Vec dXdPhi = new Vec((alpha/a.v[2])*Math.sin(a.v[1]+phi), -(alpha/a.v[2])*Math.cos(a.v[1]+phi), -(alpha/a.v[2])*a.v[4]);
		return p.T().dot(dXdPhi);		
	}

	private double S(double phi) {
		return (atPhi(phi).dif(p.X())).dot(p.T());		
	}	

	private Vec atPhi(double phi) {  // point on the helix at the angle phi
		double x= X0.v[0] + (a.v[0] + (alpha/a.v[2]))*Math.cos(a.v[1]) - (alpha/a.v[2])*Math.cos(a.v[1]+phi);
		double y= X0.v[1] + (a.v[0] + (alpha/a.v[2]))*Math.sin(a.v[1]) - (alpha/a.v[2])*Math.sin(a.v[1]+phi);
		double z= X0.v[2] + a.v[3] - (alpha/a.v[2])*phi*a.v[4];
		return new Vec(x,y,z);
	}		
	
}
