package kalman;

class Helix {         // Create a simple helix oriented along the B field axis for testing the Kalman fit
	Vec p;            // Helix parameters drho, phi0, K, dz, tanl
	Vec X0;           // Pivot point in the B field reference frame
	private double alpha;
	private double B;        // Magnetic field magnitude
	private Vec tB;          // Magnetic field direction in the global system
	private Vec uB;
	private Vec vB;
	private double[] gran;
	private int ngran;
	private double rho;
	private double Q;
	private double radLen;
	private HelixPlaneIntersect hpi;
	private RotMatrix R;      // Transforms from global coordinates to the B field frame.
	                          // All frames are assumed to have the same origin at (0.,0.,0.)

	Helix(Vec HelixParams, Vec origin, double Bfield, Vec tB) {
		this.tB = tB.copy();
		p = HelixParams.copy();
		gran = new double[2];
		B= Bfield;
		Vec yhat = new Vec(0., 1.0, 0.);
		uB = yhat.cross(tB).unitVec();
		vB = tB.cross(uB);
		R = new RotMatrix(uB,vB,tB);
		double c=2.99793e8;
		alpha = 1000.0*1.0E9/(c*Bfield);      // Units are Tesla, mm, GeV
		X0 = origin.copy();
		ngran = 0;       // For keeping track of use of gaussian random numbers
		rho = 2.329;     // Density of silicon in g/cm^2
		Q = Math.signum(p.v[2]);
		radLen = (21.82/rho) * 10.0;  // Radiation length of silicon in millimeters
		//System.out.format("helix.java: new helix with radius = %10.2f mm.\n", alpha/p[2]);
		
		hpi = new HelixPlaneIntersect(alpha);
	}
	
	Helix copy() {
		return new Helix(p, X0, B, tB);
	}
	
	void print(String s) {
		System.out.format("Helix parameters for %s:\n", s);
		System.out.format("    drho=%10.5f\n", p.v[0]);
		System.out.format("    phi0=%10.5f\n", p.v[1]);
		System.out.format("       K=%10.5f\n", p.v[2]);
		System.out.format("      dz=%10.5f\n", p.v[3]);
		System.out.format("   tanlL=%10.5f\n", p.v[4]);
		System.out.format("  Origin=%10.5f, %10.5f, %10.5f\n", X0.v[0], X0.v[1], X0.v[2]);
		System.out.format("  Helix radius=%10.5f\n", alpha/p.v[2]);
		R.print("from global frame to B-field frame");
	}

	Vec pivotTransform(Vec pivot) {
		double xC = X0.v[0] + (p.v[0]+alpha/p.v[2])*Math.cos(p.v[1]);            // Center of the helix circle
		double yC = X0.v[1] + (p.v[0]+alpha/p.v[2])*Math.sin(p.v[1]);
		//System.out.format("pivotTransform center=%10.6f, %10.6f\n", xC, yC);
		
		// Predicted state vector
		double [] aP = new double [5];
		aP[2] = p.v[2];
		aP[4] = p.v[4];
		if (p.v[2]>0) {
			aP[1] = Math.atan2(yC-pivot.v[1], xC-pivot.v[0]);		
		} else {
			aP[1] = Math.atan2(pivot.v[1]-yC, pivot.v[0]-xC);		
		}
		aP[0] = (xC-pivot.v[0])*Math.cos(aP[1]) + (yC-pivot.v[1])*Math.sin(aP[1])-alpha/p.v[2];
		aP[3] = X0.v[2] - pivot.v[2] + p.v[3] - (alpha/p.v[2])*(aP[1]-p.v[1])*p.v[4];
		
		return new Vec(5, aP);	
	}	
	
	double drho() {
		return p.v[0];
	}
	
	double phi0() {
		return p.v[1];
	}
	
	double K() {
		return p.v[2];
	}
	
	double dz() {
		return p.v[3];
	}
	
	double tanl() {
		return p.v[4];
	}

	Vec atPhi(double phi) {  // return the local coordinates on the helix at a particular phi value
		double x= X0.v[0] + (p.v[0] + (alpha/(p.v[2])))*Math.cos(p.v[1]) - (alpha/(p.v[2]))*Math.cos(p.v[1]+phi);
		double y= X0.v[1] + (p.v[0] + (alpha/(p.v[2])))*Math.sin(p.v[1]) - (alpha/(p.v[2]))*Math.sin(p.v[1]+phi);
		double z= X0.v[2] + p.v[3] - (alpha/(p.v[2]))*phi*p.v[4];
		return new Vec(x,y,z);
	}
	
	Vec atPhiGlobal(double phi) {  // return the global coordinates on the helix at a particular phi value
		double x= X0.v[0] + (p.v[0] + (alpha/(p.v[2])))*Math.cos(p.v[1]) - (alpha/(p.v[2]))*Math.cos(p.v[1]+phi);
		double y= X0.v[1] + (p.v[0] + (alpha/(p.v[2])))*Math.sin(p.v[1]) - (alpha/(p.v[2]))*Math.sin(p.v[1]+phi);
		double z= X0.v[2] + p.v[3] - (alpha/(p.v[2]))*phi*p.v[4];
		return R.inverseRotate(new  Vec(x,y,z));
	}
	
	double planeIntersect(Plane Pin) {   // phi value where the helix intersects the plane P (given in global coordinates)
		Plane P= Pin.toLocal(R,new Vec(0.,0.,0.));
		//P.print("from helix.planeIntersect");
		//p.print("helix");
		double arg = (p.v[2]/alpha)*((p.v[0]+(alpha/(p.v[2])))*Math.sin(p.v[1])-(P.X().v[1]-X0.v[1]));
		//System.out.format("  helix.planeIntersect: arg=%10.7f\n", arg);
		double phi0= -p.v[1] + Math.asin(arg);
		if (Double.isNaN(phi0) || P.T().v[2] == 1.0) return phi0;
		
		hpi.a = p;
		hpi.X0 = X0;
		hpi.p = P;
		double deltaPhi = 0.1;
		double accuracy = 0.0000001;
		double phi = hpi.rtSafe(phi0, phi0-deltaPhi, phi0+deltaPhi, accuracy);
		//System.out.format("Helix.planeIntersect: phi0=%10.7f, phi=%10.7f\n", phi0, phi);
		return phi;
	}
	
	Vec getMom(double phi) {   // get the particle momentum vector at a particular phi value
		double px = -Math.sin(p.v[1]+phi)/Math.abs(p.v[2]);
		double py = Math.cos(p.v[1]+phi)/Math.abs(p.v[2]);
		double pz = p.v[4]/Math.abs(p.v[2]);
		return new Vec(px,py,pz);
	}
	
	Vec getMomGlobal(double phi) {   // get the particle momentum vector at a particular phi value
		double px = -Math.sin(p.v[1]+phi)/Math.abs(p.v[2]);
		double py = Math.cos(p.v[1]+phi)/Math.abs(p.v[2]);
		double pz = p.v[4]/Math.abs(p.v[2]);
		return R.inverseRotate(new Vec(px,py,pz));
	}

	private double gausRan() {  //Return a Gaussian random number

		double y,x1,x2,w;
		if (ngran == 0) {
			do {
				x1 = 2.0*Math.random() - 1.0;
				x2 = 2.0*Math.random() - 1.0;
				w = x1*x1 + x2*x2;
			} while (w>=1.0);
			w = Math.sqrt((-2.0*Math.log(w))/w);
			gran[0] = x1*w;
			gran[1] = x2*w;  // Save the second number generated by the algorithm
			y = gran[0];
			ngran = 1;
		} else {
			y = gran[1];
			ngran = 0;
		}
		
		return y;
	}
	
	Helix randomScat(Plane P, double X, double Bnew, Vec tBnew) {  // Produce a new helix scattered randomly in a given plane P
		// X is the thickness of the silicon material in meters
		double phi= planeIntersect(P);  // Here the plane P is assumed to be given in global coordinates
		
		Vec r= atPhiGlobal(phi);
		Vec pmom= getMomGlobal(phi);
		Vec t= pmom.unitVec();
		//System.out.format("randomScat: original direction=%10.7f, %10.7f, %10.7f\n", t.v[0],t.v[1],t.v[2]);
		double ct = Math.abs(P.T().dot(t));
		double theta0 = Math.sqrt(2.0*(X/radLen)/ct)*(0.0136/pmom.mag())*(1.0+0.038*Math.log((X/radLen)/ct));
		double thetaScat = gausRan()*theta0;
		double phiScat = Math.random()*2.0*Math.PI;
		//System.out.format("randomScat: theta0=%10.6f thetaScat=%10.7f phiScat=%10.6f\n",theta0,thetaScat,phiScat);
		Vec zhat = new Vec(0.,0.,1.);
		Vec uhat = t.cross(zhat);    // A unit vector u perpendicular to the helix direction
		RotMatrix R1 = new RotMatrix(t,phiScat);
		uhat = R1.rotate(uhat);       // Rotate the unit vector randomly around the helix direction
		//System.out.format("uhat dot t=%12.8f\n", uhat.dot(t));  //Crosscheck that uhat is still perpendicular to t
		RotMatrix R2 = new RotMatrix(uhat,thetaScat);   // Now rotate the helix direction by theta about the u axis

		Vec yhat = new Vec(0., 1.0, 0.);
		Vec uBnew = (yhat.cross(tBnew)).unitVec();
		Vec vBnew = tBnew.cross(uBnew);
		RotMatrix Rnew = new RotMatrix(uBnew,vBnew,tBnew);  // Also rotate into the frame of the new B field
		Vec tnew = Rnew.rotate(R2.rotate(t));		
		
		//System.out.format("tnew dot tnew= %14.10f\n", tnew.dot(tnew));
		//System.out.format("randomScat: new direction=%10.7f, %10.7f, %10.7f\n", tnew.v[0],tnew.v[1],tnew.v[2]);
		//double check = Math.acos(t.dot(tnew));
		//System.out.format("recalculated scattered angle=%10.7f\n", check);
		double E = pmom.mag();                               // Everything is assumed electron
		double sp = 0.0; //0.002;   // Estar collision stopping power for electrons in silicon at about a GeV, in GeV cm2/g
		double dEdx = 0.1*sp*rho;  // in GeV/mm
		double eLoss = dEdx*X/ct;
		//System.out.format("randomScat: energy=%10.7f,  energy loss=%10.7f\n", E, eLoss);
		E = E - eLoss;
		double tanl = tnew.v[2]/Math.sqrt(1.0-tnew.v[2]*tnew.v[2]);
		double pt = E/Math.sqrt(1.0+tanl*tanl);
		double K = Q/pt;
		double phi0 = Math.atan2(-tnew.v[0], tnew.v[1]);
		Vec H = new Vec(0.,phi0,K,0.,tanl);     // Pivot point is on the helix, at the plane intersection point, so drho and dz are zero
/*		
        // Testing of the pivot transformation equations:
		double [] aP = pivotTransform(r.v, p, B);
		System.out.format("Comparing transformations:\n");
		for (int i=0; i<5; i++) {
			System.out.format("     %d   %10.6f    %10.6f\n", i, H[i], aP[i]);
		}
		
		Matrix5 F = makeF(aP, p, B);  // analytic derivatives

        double [] dP = new double [5];
        double [] ppdP = new double [5];
        for (int i=0; i<5; i++) {
        	dP[i] = p[i]*0.1;
        	ppdP[i] = p[i] + dP[i];
        }
        double [] aPpdP = pivotTransform(r.v, ppdP, B);
        double [] N = new double[5];   // numerical estimates from derivatives
        for (int i=0; i<5; i++) {
        	N[i] = aP[i];
        	for (int j=0; j<5; j++) {
        		N[i] = N[i] + F.M[i][j] * dP[j];
        	}
        }
		System.out.format("Comparing with numerical difference:\n");
		for (int i=0; i<5; i++) {
			System.out.format("     %d   %10.6f    %10.6f\n", i, aPpdP[i], N[i]);
		}
*/		
		return new Helix(H,Rnew.rotate(r),Bnew,tBnew);   // Create the new helix with pivot point specified in the new B-field coordinates
	}
	
	private SquareMatrix makeF(double [] aP, double [] a, double B) { // For testing purposes only
		double alpha = 1./B;
		double [][] f = new double [5][5];
		f[0][0] = Math.cos(aP[1]-a[1]);
		f[0][1] = (a[0]+alpha/a[2])*Math.sin(aP[1]-a[1]);
		f[0][2] = (alpha/(a[2]*a[2]))*(1.0-Math.cos(aP[1]-a[1]));
		f[0][3] = 0.;
		f[0][4] = 0.;
		f[1][0] = -Math.sin(aP[1]-a[1])/(aP[0]+alpha/a[2]);
		f[1][1] = (a[0]+alpha/a[2])*Math.cos(aP[1]-a[1])/(aP[0]+alpha/a[2]);
		f[1][2] = (alpha/(a[2]*a[2]))*Math.sin(aP[1]-a[1])/(aP[0]+alpha/a[2]);
		f[1][3] = 0.;
		f[1][4] = 0.;
		f[2][0] = 0.;
		f[2][1] = 0.;
		f[2][2] = 1.0;
		f[2][3] = 0.;
		f[2][4] = 0.;
		f[3][0] = (alpha/a[2])*a[4]*Math.sin(aP[1]-a[1])/(aP[0]+alpha/a[2]);
		f[3][1] = (alpha/a[2])*a[4]*(1.0-(a[0]+alpha/a[2])*Math.cos(aP[1]-a[1])/(aP[0]+alpha/a[2]));
		f[3][2] = (alpha/(a[2]*a[2]))*a[4]*(aP[1]-a[1] - (alpha/a[2])*Math.sin(aP[1]-a[1])/(aP[0]+alpha/a[2]));
		f[3][3] = 1.0;
		f[3][4] = -(alpha/a[2])*(aP[1]-a[1]);
		f[4][0] = 0.;
		f[4][1] = 0.;
		f[4][2] = 0.;
		f[4][3] = 0.;
		f[4][4] = 1.0;
		return new SquareMatrix(5, f);
	}

	private double [] pivotTransform(double [] pivot, double [] a, double B) {  // For testing purposes only
		double alpha = 1/B;
		double xC = X0.v[0] + (a[0]+alpha/a[2])*Math.cos(a[1]);            // Center of the helix circle
		double yC = X0.v[1] + (a[0]+alpha/a[2])*Math.sin(a[1]);
		
		double [] aP = new double [5];
		aP[2] = a[2];
		aP[4] = a[4];
		if (a[2]>0) {
			aP[1] = Math.atan2(yC-pivot[1], xC-pivot[0]);		
		} else {
			aP[1] = Math.atan2(pivot[1]-yC, pivot[0]-xC);		
		}
		aP[0] = (xC-pivot[0])*Math.cos(aP[1]) + (yC-pivot[1])*Math.sin(aP[1])-alpha/a[2];
		aP[3] = X0.v[2] - pivot[2] + a[3] - (alpha/a[2])*(aP[1]-a[1])*a[4];
		
		return aP;	
	}
}
