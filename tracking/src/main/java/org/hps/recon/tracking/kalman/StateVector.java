package kalman;

//State vector (projected, filtered, or smoothed) for the Kalman filter
class StateVector {      

	int kUp;         // Last site index for which information is used in this state vector
	int kLow;        // Index of the site for the present pivot (lower index on a in the formalism)
	Vec a;           // Helix parameters at this site, relevant only in the local site coordinates
	Vec X0;          // Pivot point of this site; reference point for these helix parameters, in local site coordinates
	RotMatrix Rot;   // Rotation from the global coordinates to the local site coordinates aligned with B field on z axis
	Vec origin;      // Origin of the local site coordinates.
	SquareMatrix C;  // Helix covariance matrix at this site
	double mPred;    // Filtered or smoothed predicted measurement at site kLow
	double r;        // Filtered or smoothed residual at site kLow
	double R;        // Covariance of residual
	boolean verbose;
	private SquareMatrix F;  // Propagator matrix to propagate from this site to the next site
	private double B;        // Field magnitude
	private double alpha;    // Conversion from 1/K to radius R
	private HelixPlaneIntersect hpi;
	
	// Constructor for the initial state vector used to start the Kalman filter.
	StateVector(int site, Vec helixParams,  
			SquareMatrix Cov, Vec pivot, double B, Vec t, Vec origin, boolean verbose) {  // Creates an initial unpropagated state vector at a given site
		if (verbose) System.out.format("StateVector: constructing an initial state vector\n");
		this.verbose = verbose;
		a = helixParams.copy();
		X0 = pivot.copy();
		this.origin = origin.copy();
		this.B = B;
		double c = 2.99793e8;             // Speed of light in m/s
		alpha = 1000.0*1.0e9/(c*B);       // Convert from pt in GeV to curvature in mm
		if (verbose) System.out.format("Creating state vector with alpha=%12.4e\n", alpha);
		kLow = site;
		kUp = kLow;
		C = Cov.copy();
		hpi = new HelixPlaneIntersect(alpha);
		Vec yhat = new Vec(0., 1.0, 0.);
		Vec u = yhat.cross(t).unitVec();
		Vec v = t.cross(u);
		Rot = new RotMatrix(u,v,t);
	}

	// Constructor for a new blank state vector with a new B field
	StateVector(int site, double B, Vec t, Vec origin, boolean verbose) { 
		//System.out.format("Creating state vector with alpha=%12.4e\n", alpha);
		kLow = site;
		double c = 2.99793e8;             // Speed of light in m/s
		alpha = 1000.0*1.0e9/(c*B);       // Convert from pt in GeV to curvature in mm
		this.B = B;
		hpi = new HelixPlaneIntersect(alpha);
		this.verbose = verbose;
		this.origin = origin.copy();
		Vec yhat = new Vec(0., 1.0, 0.);
		Vec u = yhat.cross(t).unitVec();
		Vec v = t.cross(u);
		Rot = new RotMatrix(u,v,t);
	}
	
	// Constructor for a new completely blank state vector
	StateVector(int site, boolean verbose) { 
		kLow = site;
		this.verbose = verbose;
	}
	
	// Deep copy of the state vector
	StateVector copy() { 
		StateVector q = new StateVector(kLow, verbose);
		q.B = B;
		q.alpha = alpha;
		q.Rot = Rot.copy();
		q.kUp = kUp;
		q.a = a.copy();
		q.C = C.copy();
		if (F != null) q.F = F.copy();
		q.X0 = X0.copy();
		q.origin = origin.copy();
		q.mPred = mPred;
		q.R = R;
		q.r = r;
		q.hpi = new HelixPlaneIntersect(alpha);
		return q;
	}
	
	// Debug printout of the state vector
	void print(String s) {
		System.out.format(">>>Dump of state vector %s %d  %d, B=%10.7f Tesla\n", s, kUp, kLow, B);
		origin.print("origin of local coordinates");
		Rot.print("from global to field coordinates");
		X0.print("pivot point in local cooordinates");
		a.print("helix parameters");
		helixErrors().print("helix parameter errors");
		C.print("for the helix covariance");
		if (F != null) F.print("for the propagator");
		System.out.format("Predicted measurement=%10.6f, residual=%10.7f, covariance of residual=%12.4e\n", mPred, r, R);
		System.out.format("End of dump of state vector %s %d  %d<<<\n", s, kUp, kLow);
	}
	
	// To transform a space point from global to local coordinates, first subtract <origin> and then rotate by <Rot>.
	Vec toLocal(Vec xGlobal) {
		Vec xLocal = Rot.rotate(xGlobal.dif(origin));
		return xLocal;
	}
	
	// To transform a space point from local to global coordinates, first rotate by the inverse of <Rot> and then add the <origin>.
	Vec toGlobal(Vec xLocal) {
		Vec xGlobal = Rot.inverseRotate(xLocal).sum(origin);
		return xGlobal;
	}
		
	// Create a predicted state vector by propagating a given helix to a measurement site
	StateVector predict(int newSite, Vec pivot, double B, Vec t, Vec origin, double XL, double deltaE) { // Create predicted state vector for the next site
		// newSite = index of the new site
		// pivot = pivot point of the new site in the local coordinates of this state vector
		// alpha = constant to transform from pt to radius of curvature
		// XL = thickness of the scattering material
		// deltaE = energy loss in the scattering material
		// Put the origin of the local coordinates of the new state vector at the pivot point (makes drho and dz zero)
		StateVector aPrime = new StateVector(newSite, B, t, origin, verbose);
		aPrime.X0 = new Vec(0.,0.,0.);

		double E = a.v[2]*Math.sqrt(1.0+ a.v[4]*a.v[4]);
		double deltaEoE = deltaE/E;
		if (deltaE == 0.) {
			aPrime.a = pivotTransform(pivot);
		} else {			
			aPrime.a = pivotTransform(pivot, deltaEoE);
		}
		//if (verbose) aPrime.a.print("pivot transformed helix; should have zero drho and dz");
		
		F = makeF(aPrime.a);  // Derivatives of propagator from this site to the prime (next) site where we are making the prediction
		if (deltaE != 0.) {
			double factor = 1.0 - deltaEoE;
			for (int i=0; i<5; i++) {
				F.M[i][2] *= factor;
			}
		}

		// Transform to the coordinate system of the field at the new site
		RotMatrix Rt = Rot.invert().multiply(aPrime.Rot);
		SquareMatrix fRot = new SquareMatrix(5);
		if (verbose) {
			Rt.print("rotation from old local frame to new local frame");
			aPrime.a.print("StateVector:predict helix before rotation");
		}
		aPrime.a = rotateHelix(aPrime.a, Rt, fRot);         // Derivative matrix fRot gets filled in here
		if (verbose) {
			aPrime.a.print("StateVector:predict helix after rotation");
			fRot.print("fRot from StateVector:predict");
		}
		F = fRot.multiply(F);
		
		// Test the derivatives
		/*
		if (verbose) {
			double daRel[] = {0.01,0.03,-0.02,0.05,-0.01};
			StateVector aPda = copy();
			for (int i=0; i<5; i++) aPda.a.v[i] = a.v[i]*(1.0+daRel[i]);
			Vec da = aPda.a.dif(a);
			StateVector aPrimeNew = copy();
			aPrimeNew.a = aPda.pivotTransform(pivot);
			RotMatrix RtTmp = Rot.invert().multiply(aPrime.Rot);
			SquareMatrix fRotTmp = new SquareMatrix(5);
			aPrimeNew.a = rotateHelix(aPrimeNew.a, RtTmp, fRotTmp);
			for (int i=0; i<5; i++) {
				double deltaExact = aPrimeNew.a.v[i] - aPrime.a.v[i];
				double delta = 0.;
				for (int j=0; j<5; j++) {
					delta += aPrimeNew.F.M[i][j]*da.v[j];
				}
				System.out.format("Test of F: Helix parameter %d, deltaExact=%10.8f, delta=%10.8f\n", i, deltaExact, delta);
			}
		}
		*/
						
		aPrime.kLow = newSite;
		aPrime.kUp = kUp;

		// Add the multiple scattering contribution for the silicon layer
		double sigmaMS;
		if (XL == 0.) sigmaMS = 0.;
		else {
			double momentum = (1.0/a.v[2])*Math.sqrt(1.0+a.v[4]*a.v[4]);
			sigmaMS = (0.0136/momentum)*Math.sqrt(XL)*(1.0+0.038*Math.log(XL));
		}
		SquareMatrix Ctot = C.sum(getQ(sigmaMS));
		
		// Now propagate the multiple scattering matrix and covariance matrix to the new site		
		aPrime.C = Ctot.similarity(F);
		
		return aPrime;
	}
	
	// Create a filtered state vector from a predicted state vector
	StateVector filter(Vec H, double V) {

		StateVector aPrime = copy();
		aPrime.kUp = kLow;

		double denom = V + H.dot(H.leftMultiply(C));
		if (verbose) System.out.format("StateVector.filter: V=%12.4e,  denom=%12.4e\n", V, denom);
		Vec K = H.leftMultiply(C).scale(1.0/denom);    // Kalman gain matrix
		if (verbose) {
			K.print("Kalman gain matrix in StateVector.filter");
			H.print("matrix H in StateVector.filter");
			System.out.format("k dot H = %10.7f\n", K.dot(H));
		}
		
		SquareMatrix D = C.invert().sum(H.scale(1.0/V).product(H));
		if (verbose) {
			// Alternative calculation of K (sanity check that it gives the same result):
			Vec Kalt = H.scale(1.0/V).leftMultiply(D.invert());
			Kalt.print("alternate Kalman gain matrix");
		}
		
		aPrime.a = a.sum(K.scale(r));

		aPrime.C = D.invert();
		if (verbose) {
			aPrime.C.print("filtered covariance");
			SquareMatrix Calt = (C.unit(5).dif(K.product(H))).multiply(C);
			Calt.print("alternate filtered covariance");
			aPrime.C.multiply(D).print("unit matrix??");
		}
		
		return aPrime;
	}
	
	// Create a smoothed state vector from the filtered state vector
	StateVector smooth(StateVector snS, StateVector snP) {
		StateVector sS = copy();

		SquareMatrix CnInv = snP.C.invert();
		SquareMatrix A = (C.multiply(F.transpose())).multiply(CnInv);
		
		Vec diff = snS.a.dif(snP.a);
		sS.a = a.sum(diff.leftMultiply(A));
		
		SquareMatrix Cdiff = snS.C.dif(snP.C);
		sS.C = C.sum(Cdiff.similarity(A));
		
		return sS;
	}

	// Returns a point on the helix at the angle phi
	Vec atPhi(double phi) {  
		double x= X0.v[0] + (a.v[0] + (alpha/a.v[2]))*Math.cos(a.v[1]) - (alpha/a.v[2])*Math.cos(a.v[1]+phi);
		double y= X0.v[1] + (a.v[0] + (alpha/a.v[2]))*Math.sin(a.v[1]) - (alpha/a.v[2])*Math.sin(a.v[1]+phi);
		double z= X0.v[2] + a.v[3] - (alpha/a.v[2])*phi*a.v[4];
		return new Vec(x,y,z);
	}	

	// Returns the particle momentum at the helix angle phi
	Vec getMom(double phi) {  
		double px = -Math.sin(a.v[1]+phi)/Math.abs(a.v[2]);
		double py = Math.cos(a.v[1]+phi)/Math.abs(a.v[2]);
		double pz = a.v[4]/Math.abs(a.v[2]);
		return new Vec(px,py,pz);
	}	

	// Calculate the phi angle to propagate on helix to the intersection with a measurement plane
	double planeIntersect(Plane pIn) { 			// pIn is assumed to be defined in the global reference frame									
		Plane p = pIn.toLocal(Rot, origin);     // Transform the plane into the B-field local reference frame
		/*
		if (verbose) {
			System.out.format("StateVector.planeIntersect:\n");
			pIn.print("of measurement global");
			p.print("of measurement local");
			a.print("helix parameters");
			X0.print("pivot");
			Rot.print("from global to local coordinates");
			origin.print("origin of local coordinates");
			System.out.format(" alpha=%10.7f, radius=%10.5f\n", alpha, alpha/a.v[2]);
		}
		*/
		//Take as a starting guess the solution for the case that the plane orientation is exactly y-hat.
		double arg = (a.v[2]/alpha)*((a.v[0]+(alpha/a.v[2]))*Math.sin(a.v[1])-(p.X().v[1]-X0.v[1]));
		double phi0= -a.v[1] + Math.asin(arg);  
		//if (verbose) System.out.format("  StateVector.planeIntersect: arg=%10.7f, phi=%10.7f\n", arg, phi0);
		if (Double.isNaN(phi0) || p.T().v[1] == 1.0) return phi0;

		hpi.a = a;
		hpi.X0 = X0;
		hpi.p = p;		
		double dphi = 0.1;
		double accuracy = 0.0000001;
		double phi = hpi.rtSafe(phi0, phi0-dphi, phi0+dphi, accuracy);  // Iterative solution for a general plane orientation	
		//if (verbose) System.out.format("StateVector.planeIntersect: phi0=%10.7f, phi=%10.7f\n", phi0, phi);
		return phi;
	}	
	
	// Multiple scattering matrix; assume a single thin scattering layer at the beginning of the helix propagation
	private SquareMatrix getQ(double sigmaMS) {  
		double [][] q = new double [5][5];
		
		double V = sigmaMS*sigmaMS;
		q[1][1] = V*(1.0 + a.v[4]*a.v[4]);
		q[2][2] = 0.; //V*(a.v[2]*a.v[2]*a.v[4]*a.v[4]);    // These commented terms would be relevant for a scatter halfway in between planes
		q[2][4] = 0.; //V*(a.v[2]*a.v[4]*(1.0+a.v[4]*a.v[4]));
		q[4][2] = 0.; //q[2][4];
		q[4][4] = V*(1.0+a.v[4]*a.v[4])*(1.0+a.v[4]*a.v[4]);
		// All other elements are zero
		
		return new SquareMatrix(5, q);
	}

	// Return errors on the helix parameters at the global origin
	Vec helixErrors(Vec aPrime) {  
		// aPrime are the helix parameters for a pivot at the global origin, assumed already to be calculated by pivotTransform()
		SquareMatrix tC = covariancePivotTransform(aPrime);
		return new Vec(Math.sqrt(tC.M[0][0]), Math.sqrt(tC.M[1][1]),Math.sqrt(tC.M[2][2]),Math.sqrt(tC.M[3][3]),Math.sqrt(tC.M[4][4]));
	}
	
	// Return errors on the helix parameters at the present pivot point
	Vec helixErrors() {  
		return new Vec(Math.sqrt(C.M[0][0]), Math.sqrt(C.M[1][1]),Math.sqrt(C.M[2][2]),Math.sqrt(C.M[3][3]),Math.sqrt(C.M[4][4]));
	}

	// Transform the helix covariance to new pivot point (specified in local coordinates)
	SquareMatrix covariancePivotTransform(Vec pivot, Vec aP) {  
		// aP are the helix parameters for the new pivot point, assumed already to be calculated by pivotTransform()
		SquareMatrix mF = makeF(aP);	
		return C.similarity(mF);
	}
	
	// Transform the helix covariance to a pivot point at the origin
	SquareMatrix covariancePivotTransform(Vec aP) {
		// aP are the helix parameters for a pivot point at the origin, assumed already to be calculated by pivotTransform()
		Vec pivot = origin.scale(-1.0);
		return covariancePivotTransform(pivot, aP);
	}
	
	// Transform the helix to a pivot back at the global origin
	Vec pivotTransform() {  
		Vec pivot = origin.scale(-1.0);
		return pivotTransform(pivot);
	}
	
	// Pivot transform of the state vector, from the current pivot to the pivot in the argument (specified in local coordinates)
	Vec pivotTransform(Vec pivot) {  
		double xC = X0.v[0] + (a.v[0]+alpha/a.v[2])*Math.cos(a.v[1]);            // Center of the helix circle
		double yC = X0.v[1] + (a.v[0]+alpha/a.v[2])*Math.sin(a.v[1]);
		//if (verbose) System.out.format("pivotTransform center=%10.6f, %10.6f\n", xC, yC);
		
		// Predicted state vector
		double [] aP = new double [5];
		aP[2] = a.v[2];
		aP[4] = a.v[4];
		if (a.v[2]>0) {
			aP[1] = Math.atan2(yC-pivot.v[1], xC-pivot.v[0]);		
		} else {
			aP[1] = Math.atan2(pivot.v[1]-yC, pivot.v[0]-xC);		
		}
		aP[0] = (xC-pivot.v[0])*Math.cos(aP[1]) + (yC-pivot.v[1])*Math.sin(aP[1])-alpha/a.v[2];
		aP[3] = X0.v[2] - pivot.v[2] + a.v[3] - (alpha/a.v[2])*(aP[1]-a.v[1])*a.v[4];
		
		//xC =  pivot[0] + (aP[0]+alpha/aP[2])*Math.cos(aP[1]);
		//yC = pivot[1] + (aP[0]+alpha/aP[2])*Math.sin(aP[1]);
		//if (verbose) System.out.format("pivotTransform new center=%10.6f, %10.6f\n", xC, yC);
		
		return new Vec(5, aP);	
	}
	
	// Pivot transform including energy loss just before
	Vec pivotTransform(Vec pivot, double deltaEoE) {  
		double K = a.v[2]*(1.0-deltaEoE);                                 // Lose energy before propagating
		double xC = X0.v[0] + (a.v[0]+alpha/K)*Math.cos(a.v[1]);            // Center of the helix circle
		double yC = X0.v[1] + (a.v[0]+alpha/K)*Math.sin(a.v[1]);
		//if (verbose) System.out.format("pivotTransform center=%10.6f, %10.6f\n", xC, yC);
		
		// Predicted state vector
		double [] aP = new double [5];
		aP[2] = K;
		aP[4] = a.v[4];
		if (K>0) {
			aP[1] = Math.atan2(yC-pivot.v[1], xC-pivot.v[0]);		
		} else {
			aP[1] = Math.atan2(pivot.v[1]-yC, pivot.v[0]-xC);		
		}
		aP[0] = (xC-pivot.v[0])*Math.cos(aP[1]) + (yC-pivot.v[1])*Math.sin(aP[1])-alpha/K;
		aP[3] = X0.v[2] - pivot.v[2] + a.v[3] - (alpha/K)*(aP[1]-a.v[1])*a.v[4];
		
		//xC =  pivot[0] + (aP[0]+alpha/aP[2])*Math.cos(aP[1]);
		//yC = pivot[1] + (aP[0]+alpha/aP[2])*Math.sin(aP[1]);
		//if (verbose) System.out.format("pivotTransform new center=%10.6f, %10.6f\n", xC, yC);
		
		return new Vec(5, aP);	
	}	
	
	// Derivative matrix for the pivot transform (without energy loss or field rotations)
	private SquareMatrix makeF(Vec aP) {    
		double [][] f = new double [5][5];
		f[0][0] = Math.cos(aP.v[1]-a.v[1]);
		f[0][1] = (a.v[0]+alpha/a.v[2])*Math.sin(aP.v[1]-a.v[1]);
		f[0][2] = (alpha/(a.v[2]*a.v[2]))*(1.0-Math.cos(aP.v[1]-a.v[1]));
		f[1][0] = -Math.sin(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]);
		f[1][1] = (a.v[0]+alpha/a.v[2])*Math.cos(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]);
		f[1][2] = (alpha/(a.v[2]*a.v[2]))*Math.sin(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]);
		f[2][2] = 1.0;
		f[3][0] = (alpha/a.v[2])*a.v[4]*Math.sin(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]);
		f[3][1] = (alpha/a.v[2])*a.v[4]*(1.0-(a.v[0]+alpha/a.v[2])*Math.cos(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]));
		f[3][2] = (alpha/(a.v[2]*a.v[2]))*a.v[4]*(aP.v[1]-a.v[1] - (alpha/a.v[2])*Math.sin(aP.v[1]-a.v[1])/(aP.v[0]+alpha/a.v[2]));
		f[3][3] = 1.0;
		f[3][4] = -(alpha/a.v[2])*(aP.v[1]-a.v[1]);
		f[4][4] = 1.0;
		
		return new SquareMatrix(5, f);
	}

	// Momentum at the start of the given helix (point closest to the pivot)
	Vec aTOp(Vec a) {  
		double px = -Math.sin(a.v[1])/Math.abs(a.v[2]);
		double py = Math.cos(a.v[1])/Math.abs(a.v[2]);
		double pz = a.v[4]/Math.abs(a.v[2]);
		return new Vec(px,py,pz);
	}

	// Transform from momentum at helix starting point back to the helix parameters
	Vec pTOa(Vec p, Vec a) {  
		double Q = Math.signum(a.v[2]);
		double phi0 = Math.atan2(-p.v[0], p.v[1]);
		double K = Q/Math.sqrt(p.v[0]*p.v[0] + p.v[1]*p.v[1]);
		double tanl = p.v[2]/Math.sqrt(p.v[0]*p.v[0] + p.v[1]*p.v[1]);
		if (verbose) {
			System.out.format("StateVector pTOa: Q=%5.1f phi0=%10.7f K=%10.6f tanl=%10.7f\n", Q,phi0,K,tanl);
			p.print("input momentum vector");
		}
		// Note: the following only makes sense when a.v[0] and a.v[3] (drho and dz) are both zero, i.e. pivot is on the helix
		return new Vec(a.v[0],phi0,K,a.v[3],tanl);
	}

	// Transformation of a helix from one B-field frame to another, by rotation R
	Vec rotateHelix(Vec a, RotMatrix R, SquareMatrix fRot) {
		// The rotation is easily applied to the momentum vector, so first we transform from helix parameters
		// to momentum, apply the rotation, and then transform back to helix parameters.
		// The values for fRot, the corresponding derivative matrix, are also calculated and returned.
		Vec p_prime = R.rotate(aTOp(a));

		SquareMatrix dpda = new SquareMatrix(3);
		dpda.M[0][0] = -Math.cos(a.v[1])/Math.abs(a.v[2]);
		dpda.M[0][1] = Math.sin(a.v[1])/(a.v[2]*Math.abs(a.v[2]));
		dpda.M[1][0] = -Math.sin(a.v[1])/Math.abs(a.v[2]);
		dpda.M[1][1] = -Math.cos(a.v[1])/(a.v[2]*Math.abs(a.v[2]));
		dpda.M[2][1] = -a.v[4]/(a.v[2]*Math.abs(a.v[2]));
		dpda.M[2][2] = 1./Math.abs(a.v[2]);
		
		SquareMatrix dprimedp = new SquareMatrix(3);
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				dprimedp.M[i][j] = R.M[i][j];      
			}
		}
		
		double Q = Math.signum(a.v[2]);		
		SquareMatrix dadp = new SquareMatrix(3);
		double pt2 = p_prime.v[0]*p_prime.v[0] + p_prime.v[1]*p_prime.v[1];
		double pt = Math.sqrt(pt2);
		dadp.M[0][0] = -p_prime.v[1]/pt2;
		dadp.M[0][1] = p_prime.v[0]/pt2;
		dadp.M[1][0] = -Q*p_prime.v[0]/(pt2*pt);
		dadp.M[1][1] = -Q*p_prime.v[1]/(pt2*pt);
		dadp.M[2][0] = -p_prime.v[0]*p_prime.v[2]/(pt2*pt);
		dadp.M[2][1] = -p_prime.v[1]*p_prime.v[2]/(pt2*pt);
		dadp.M[2][2] = 1.0/(pt);
		
		SquareMatrix prod = dadp.multiply(dprimedp.multiply(dpda));
		
		fRot.M[0][0] = 1.0;
		fRot.M[1][1] = prod.M[0][0];
		fRot.M[1][2] = prod.M[0][1];
		fRot.M[1][4] = prod.M[0][2];
		fRot.M[2][1] = prod.M[1][0];
		fRot.M[2][2] = prod.M[1][1];
		fRot.M[2][4] = prod.M[1][2];
		fRot.M[3][3] = 1.0;
		fRot.M[4][1] = prod.M[2][0];
		fRot.M[4][2] = prod.M[2][1];
		fRot.M[4][4] = prod.M[2][2];
/*
		if (verbose) { // derivative test
			Vec da = a.scale(0.005);
			Vec apda = a.sum(da);
			Vec ap = pTOa(R.rotate(aTOp(a)),a);
			Vec apdap = pTOa(R.rotate(aTOp(apda)),apda);
			Vec dap = apdap.dif(ap);
			Vec dap2 = da.leftMultiply(fRot);
			System.out.format("StateVector:rotateHelix: derivative test:\n");
			dap.print("actual difference in helix parameters");
			dap2.print("diff in helix params from derivatives");
		}		
*/
		return pTOa(p_prime, a);
	}
	
}
