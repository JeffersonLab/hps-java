package kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;

public class HelixTest {   // Main program for testing the Kalman fitting code

	public static void main(String[] args) {
		
		String defaultPath = "C:\\Users\\Robert\\Desktop\\Kalman\\";
		String path;   // Path to where the output histograms should be written
		if (args.length==0) path = defaultPath; else path = args[0];
		
		// Units are Tesla, GeV, mm
		
		int nTrials = 10000;             // The number of test helices to generate for fitting
		boolean verbose = nTrials < 2;

		double B = 1.0; 
		Vec tB = new Vec(0.,0.,1.);
		
		double Q = -1.0;   // charge
		double p = 2.0;    // momentum 
		
		double drho = -2.;
		double drhoSigma = 0.2;
		double dz = 5.0;
		double dzSigma = 0.2;
		double phi0 = 0.03;
		double phi0Sigma = 0.0002;
		double tanl = 0.1;
		double tanlSigma = 0.0002;
		
		double pt = p/Math.sqrt(1.0+tanl*tanl);
		System.out.format("Momentum p=%10.4f GeV, pt=%10.4f GeV\n", p, pt);
		
		double K = Q/pt;
		double kSigma = K*0.02;
		System.out.format("True starting helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
		Vec origin = new Vec(0.,0.,0.);
		double[] param = {drho, phi0, K, dz, tanl};
        Vec helixMCtrue = new Vec(5, param);

        // Tracking instrument description
		int nPlanes = 6;
		Vec tInt = new Vec(0.,1.,0.);                           // Nominal detector plane orientation
		double [] location = {100.,200.,300.,500.,700.,900.};  	// Detector positions in y
		double thickness = 0.3;                                	// Silicon thickness in mm
		double delta = 5.0;         							// Distance between stereo pairs
		double [] stereoAngle = {0.1,0.1,0.1,0.05,0.05,0.05};   // Angles of the stereo layers in radians
		double resolution = 0.012;                              // SSD point resolution, in mm

		double [] thetaR1 = new double [nPlanes];
		double [] phiR1 = new double [nPlanes];
		double [] thetaR2 = new double [nPlanes];
		double [] phiR2 = new double [nPlanes];
		for (int i=0; i<nPlanes; i++) {    // Generate some random misalignment of the detector planes
			double [] gran = gausRan();
			thetaR1[i] = 0.; //Math.abs(gran[0]*0.087);
			phiR1[i] = 0.; //Math.random()*2.0*Math.PI;
			thetaR2[i] = 0.; //Math.abs(gran[1]*0.087);
			phiR2[i] = 0.; //Math.random()*2.0*Math.PI;
		}

		// Generate a (non-physical) non-uniform B field, just for testing
		//double [] Bangle = {0.,0.,0.,0.,0.,0.}; 
		//double [] Bscale = {1.,1.,1.,1.,1.,1.}; 
		double [] Bangle = {0.,0.2,0.4,0.6,0.8,1.0};       // field angle in degrees
		double [] Bscale = {1.0,1.0,0.99,0.98,0.97,0.96}; 
		double [] Bfield = new double [nPlanes];
		Vec [] tBfield = new Vec [nPlanes];
		for (int i=0; i<nPlanes; i++) {
			Bfield[i] = Bscale[i]*B;
			double angle = Bangle[i]*Math.PI/180.;
			tBfield[i] = new Vec(0., Math.sin(angle), Math.cos(angle));
		}
		
		Helix TkInitial = new Helix(new Vec(5,param), origin, Bfield[0], tBfield[0]);

		// Print out a plot of just a simple helix
		File file = new File(path+"helix1.gp");
		file.getParentFile().mkdirs();
		PrintWriter printWriter = null;
		try {
			printWriter = new PrintWriter(file);
		} catch (FileNotFoundException e1) {
			System.out.println("Could not create the gnuplot output file.");
			e1.printStackTrace();
			return;
		}
		//printWriter.format("set xrange [-1900.:100.]\n");
		//printWriter.format("set yrange [-1000.:1000.]\n");
		printWriter.format("set xlabel 'X'\n");
		printWriter.format("set ylabel 'Y'\n");
		printWriter.format("splot '-' u 1:2:3 with lines\n");
		
		if (Q<0.) {
			for (double phi=0.; phi<4*Math.PI; phi=phi + 0.01) {
				Vec r = TkInitial.atPhiGlobal(phi);
				printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
			}
		} else {
			for (double phi=0.; phi>-4*Math.PI; phi=phi - 0.01) {
				Vec r = TkInitial.atPhiGlobal(phi);
				printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
			}				
		}
			
		printWriter.close();
		
		/*
		// Test the seed track fitter using an exact model with no scattering
		Histogram hEdrho2 = new Histogram(100,-10.,0.2,"Seed track drho error","sigmas","track");
		Histogram hEphi02 = new Histogram(100,-10.,0.2,"Seed track phi0 error","sigmas","track");
		Histogram hEk2 = new Histogram(100,-10.,0.2,"Seed track K error","sigmas","track");
		Histogram hEdz2 = new Histogram(100,-10.,0.2,"Seed track dz error","sigmas","track");
		Histogram hEtanl2 = new Histogram(100,-10.,0.2,"Seed track tanl error","sigmas","track");
		Histogram hEa = new Histogram(100,-10.,0.2,"Seed track error on coefficient a","sigmas","track");
		Histogram hEb = new Histogram(100,-10.,0.2,"Seed track error on coefficient b","sigmas","track");
		Histogram hEc = new Histogram(100,-10.,0.2,"Seed track error on coefficient c","sigmas","track");
		Histogram hEd = new Histogram(100,-10.,0.2,"Seed track error on coefficient d","sigmas","track");
		Histogram hEe = new Histogram(100,-10.,0.2,"Seed track error on coefficient e","sigmas","track");
		Histogram hCoefChi2 = new Histogram(50,0.,0.4,"Full chi^2 of linear fit coefficients","chi^2","track");
		double c=2.99793e8;
		double alpha = 1000.0*1.0E9/(c*B);      // Units are Tesla, mm, GeV
		double Radius = alpha/K;
		double xc = (drho + Radius)*Math.cos(phi0);
		double yc = (drho + Radius)*Math.sin(phi0);
		double sgn = -1.0;
		double [] coefs = new double [5];
		coefs[0] = dz - drho*tanl*Math.tan(phi0);
		coefs[1] = tanl/Math.cos(phi0);
		coefs[3] = sgn*yc/Radius;
		coefs[2] = xc + sgn*Radius*(1.0-0.5*coefs[3]*coefs[3]);
		coefs[4] = -sgn/(2.0*Radius);
		double [] circ = parabolaToCircle(alpha,sgn,new Vec(coefs[2],coefs[3],coefs[4]));
		Vec tmp = new Vec(circ[0],circ[1],circ[2]);
		tmp.print("circle params");
		System.out.format("Helix radius = %10.5f, and the center is at %10.6f, %10.6f\n", Radius, xc, yc);
		System.out.format("Polynomial approximation coefficients are %10.6f %10.6f %10.6f %10.6f %10.7f\n",coefs[0],coefs[1],coefs[2],coefs[3],coefs[4]);
		for (int iTrial = 0; iTrial<nTrials; iTrial++) {
			double [] m1 = new double [nPlanes];
			double [] m2 = new double [nPlanes];
			ArrayList<Measurement> measurements = new ArrayList<Measurement>(nPlanes);	
			for (int pln=0; pln<nPlanes; pln++) {
				Vec rInt1 = new Vec(0.,location[pln],0.);
				Plane pInt1 = new Plane(rInt1,tInt);
				
				double xTrue = coefs[2] + (coefs[3] + coefs[4]*rInt1.v[1])*rInt1.v[1];
				double zTrue = coefs[0] + coefs[1]*rInt1.v[1];
				Vec rTrue = new Vec(xTrue, rInt1.v[1], zTrue);
				double [] gran = gausRan();
				m1[pln] = -zTrue + resolution*gran[0];					
			    
				measurements.add(new Measurement(pInt1,rTrue,B,tB,m1[pln],resolution,0.0,thickness));

				Vec rInt2 = new Vec(0.,location[pln]+delta,0.);

			    Plane pInt2 = new Plane(rInt2,tInt);
				RotMatrix R1 = new RotMatrix(pInt2.U(), pInt2.V(), pInt2.T());
				RotMatrix R2 = new RotMatrix(stereoAngle[pln]); 
				RotMatrix R = R2.multiply(R1);
				xTrue = coefs[2] + (coefs[3] + coefs[4]*rInt2.v[1])*rInt2.v[1];
				zTrue = coefs[0] + coefs[1]*rInt2.v[1];
				rTrue = new Vec(xTrue, rInt2.v[1], zTrue);
				m2[pln] = (R.rotate(rTrue.dif(rInt2))).v[1] + resolution*gran[1];
				
				measurements.add(new Measurement(pInt2,rTrue,B,tB,m2[pln],resolution,stereoAngle[pln],thickness));
			}
			if (nTrials == 1) {
				for (Measurement mm:measurements) {
					mm.print(String.format(" polynomial approximation %d",measurements.indexOf(mm)));
				}	
			}
			SeedTrack seed = new SeedTrack(measurements, 12, verbose);
			if (!seed.success) continue;
			if (nTrials==1) {
				seed.print("helix parameters");
				System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
				seed.solution().print("polynomial solution from fit");
				System.out.format("True polynomial coefficients are %10.6f %10.6f %10.6f %10.6f %10.7f\n",coefs[0],coefs[1],coefs[2],coefs[3],coefs[4]);
				seed.solutionCovariance().print("covariance of polynomial fit");
			}
			Vec initialHelix = seed.helixParams();
			Vec seedErrors = seed.errors();
			hEdrho2.entry((initialHelix.v[0]-drho)/seedErrors.v[0]);
			hEphi02.entry((initialHelix.v[1]-phi0)/seedErrors.v[1]);
			hEk2.entry((initialHelix.v[2]-K)/seedErrors.v[2]);
			hEdz2.entry((initialHelix.v[3]-dz)/seedErrors.v[3]);
			hEtanl2.entry((initialHelix.v[4]-tanl)/seedErrors.v[4]);
			Vec fittedCoefs = seed.solution();
			Vec coefErrors = seed.solutionErrors();
			hEa.entry((fittedCoefs.v[0]-coefs[0])/coefErrors.v[0]);
			hEb.entry((fittedCoefs.v[1]-coefs[1])/coefErrors.v[1]);
			hEc.entry((fittedCoefs.v[2]-coefs[2])/coefErrors.v[2]);
			hEd.entry((fittedCoefs.v[3]-coefs[3])/coefErrors.v[3]);
			hEe.entry((fittedCoefs.v[4]-coefs[4])/coefErrors.v[4]);
			Vec trueError = fittedCoefs.dif(new Vec(coefs[0],coefs[1],coefs[2],coefs[3],coefs[4]));
			double coefChi2 = trueError.dot(trueError.leftMultiply(seed.solutionCovariance().invert()));
			hCoefChi2.entry(coefChi2);
		}
		hEdrho2.plot(path+"drhoErrSeed.gp", true, " ", " ");
		hEphi02.plot(path+"phi0ErrSeed.gp", true, " ", " ");
		hEk2.plot(path+"kErrSeed.gp", true, " ", " ");
		hEdz2.plot(path+"dzErrSeed.gp", true, " ", " ");
		hEtanl2.plot(path+"tanlErrSeed.gp", true, " ", " ");
		hEa.plot(path+"aError.gp", true, " ", " ");
		hEb.plot(path+"bError.gp", true, " ", " ");
		hEc.plot(path+"cError.gp", true, " ", " ");
		hEd.plot(path+"dError.gp", true, " ", " ");
		hEe.plot(path+"eError.gp", true, " ", " ");
		hCoefChi2.plot(path+"coefChi2.gp", true, " ", " ");
		*/
		
		PrintWriter printWriter2 = null;  
		if (nTrials == 1) {
			File file2 = new File(path+"helix2.gp");
			file2.getParentFile().mkdirs();
			try {
				printWriter2 = new PrintWriter(file2);
			} catch (FileNotFoundException e1) {
				System.out.println("Could not create the gnuplot output file.");
				e1.printStackTrace();
				return;
			}
			//printWriter2.format("set xrange [-500.:1500]\n");
			//printWriter2.format("set yrange [-1000.:1000.]\n");
			printWriter2.format("set xlabel 'X'\n");
			printWriter2.format("set ylabel 'Y'\n");
			printWriter2.format("$helix << EOD\n");
		}
		
		Histogram hChi2 = new Histogram(80,0.,.5,"Helix fit chi^2 after smoothing","chi^2","tracks");
		Histogram hChi2f = new Histogram(80,0.,.5,"Helix fit chi^2 after filtering","chi^2","tracks");
		Histogram hChi2HelixS = new Histogram(80,0.,0.4,"smoothed chi^2 of helix parameters","chi^2","tracks");
		Histogram hChi2Helix = new Histogram(80,0.,0.4,"filtered chi^2 of helix parameters","chi^2","tracks");
		Histogram hRes = new Histogram(100,-.5,0.01,"detector resolution","mm","hits");
		Histogram hEdrho = new Histogram(100,-10.,0.2,"Filtered helix parameter drho error","sigmas","track");
		Histogram hEphi0 = new Histogram(100,-10.,0.2,"Filtered helix parameter phi0 error","sigmas","track");
		Histogram hEk = new Histogram(100,-10.,0.2,"Filtered helix parameter K error","sigmas","track");
		Histogram hEdz = new Histogram(100,-10.,0.2,"Filtered helix parameter dz error","sigmas","track");
		Histogram hEtanl = new Histogram(100,-10.,0.2,"Filtered helix parameter tanl error","sigmas","track");
		Histogram hEdrhoS = new Histogram(100,-10.,0.2,"Smoothed helix parameter drho error","sigmas","track");
		Histogram hEphi0S = new Histogram(100,-10.,0.2,"Smoothed helix parameter phi0 error","sigmas","track");
		Histogram hEkS = new Histogram(100,-10.,0.2,"Smoothed helix parameter K error","sigmas","track");
		Histogram hEdzS = new Histogram(100,-10.,0.2,"Smoothed helix parameter dz error","sigmas","track");
		Histogram hEtanlS = new Histogram(100,-10.,0.2,"Smoothed helix parameter tanl error","sigmas","track");
		Histogram hResid0 = new Histogram(100,-10.,0.2,"Filtered residual for non-rotated planes","sigmas","hits");
		Histogram hResid1 = new Histogram(100,-10.,0.2,"Filtered residual for rotated planes","sigmas","hits");
		Histogram hResidS0 = new Histogram(100,-10.,0.2,"Smoothed residual for non-rotated planes","sigmas","hits");
		Histogram hResidS1 = new Histogram(100,-10.,0.2,"Smoothed residual for rotated planes","sigmas","hits");

		Instant timestamp = Instant.now();
		System.out.format("Beginning time = %s\n", timestamp.toString());
		LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
		System.out.format("%s %d %d at %d:%d %d.%d seconds\n",  ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());
		
		for (int iTrial = 0; iTrial<nTrials; iTrial++) {
			double [] m1 = new double [nPlanes];
			double [] m2 = new double [nPlanes];
			ArrayList<Measurement> measurements = new ArrayList<Measurement>(nPlanes);
			
			Helix Tk = TkInitial.copy();
	
			for (int pln=0; pln<nPlanes; pln++) {
				if (verbose) System.out.format("Extrapolating to plane #%d\n", pln);
				if (verbose) Tk.print("this plane");
				Vec rInt1 = new Vec(0.,location[pln],0.);
				if (verbose) rInt1.print("  Plane first layer location=");
				//rInt1 = rInt1.sum(new Vec3(-Tk.X0[0],-Tk.X0[1],-Tk.X0[2]));
				
				// Randomly tilt the measurement planes to mimic misalignment
				RotMatrix Rt = new RotMatrix(phiR1[pln],thetaR1[pln],-phiR1[pln]);
			    Plane pInt1 = new Plane(rInt1,Rt.rotate(tInt));
			    double phiInt = Tk.planeIntersect(pInt1);
			    if (Double.isNaN(phiInt)) break;
			    if (verbose) System.out.format("Plane %d, phiInt1= %f\n", pln,phiInt);
				Vec rscat = Tk.atPhiGlobal(phiInt);
				double check = (rscat.dif(pInt1.X()).dot(pInt1.T()));
				//System.out.format("Dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
			    if (nTrials == 1) {
			    	double dPhi = -Q*(phiInt)/20.0;
					for (double phi=0.; phi<Math.abs(phiInt); phi=phi + dPhi) {
						Vec r = Tk.atPhiGlobal(-Q*phi);
						printWriter2.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
					}
			    	//printWriter2.format("%10.6f %10.6f %10.6f\n", rscat.v[0], rscat.v[1], rscat.v[2]);
			    }
			    if (verbose) pInt1.print("first layer");
			    if (verbose) rscat.print("       Gobal intersection point 1 = ");
				RotMatrix R1 = new RotMatrix(pInt1.U(), pInt1.V(), pInt1.T());
				RotMatrix R2 = new RotMatrix(0.); 
				RotMatrix R = R2.multiply(R1);
				if (verbose) R.print("for zero stereo angle");
				Vec rDet = R.rotate(rscat.dif(pInt1.X()));
				if (verbose) rDet.print("       helix intersection in detector frame");
				double [] gran = gausRan();
				m1[pln] = rDet.v[1] + resolution*gran[0];
				hRes.entry(resolution*gran[0]);
				if (verbose) System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1[pln],rDet.v[1]);
				measurements.add(new Measurement(pInt1,rscat,Bfield[pln],tBfield[pln],m1[pln],resolution,0.0,thickness));
				//measurements.add(new Measurement(pInt1,rscat,B,tB,m1[pln],resolution,0.0,thickness));
				
				Vec t1 = null;
				if (verbose) t1= Tk.getMomGlobal(phiInt).unitVec();
				Tk = Tk.randomScat(pInt1, thickness, Bfield[pln], tBfield[pln]); 
				if (verbose) {
					Tk.print("scattered from the first layer of the detector plane");
					Vec t2 = Tk.getMomGlobal(0.).unitVec();
					double scattAng = Math.acos(t1.dot(t2));
					System.out.format("Scattering angle from 1st layer=%10.7f\n", scattAng);
				}
				
				Vec rInt2 = new Vec(0.,location[pln]+delta,0.);
				//rInt2 = rInt2.sum(new Vec(-Tk.X0[0],-Tk.X0[1],-Tk.X0[2]));

				RotMatrix Rt2 = new RotMatrix(phiR2[pln],thetaR2[pln],-phiR2[pln]);
			    Plane pInt2 = new Plane(rInt2,Rt2.rotate(tInt));
			    phiInt = Tk.planeIntersect(pInt2);  
			    if (Double.isNaN(phiInt)) break;
			    if (verbose) System.out.format("Plane %d, phiInt2= %f\n", pln,phiInt);
			    if (nTrials == 1) {
			    	double dPhi = (phiInt)/5.0;
					for (double phi=0.; phi<phiInt; phi=phi + dPhi) {
						Vec r = Tk.atPhiGlobal(phi);
						printWriter2.format(" %10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
					}
			    }
			    rscat = Tk.atPhiGlobal(phiInt);
				check = (rscat.dif(pInt2.X()).dot(pInt2.T()));
				//System.out.format("Dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
			    if (verbose) pInt2.print("Second layer");
			    if (verbose) rscat.print("       Global intersection point 2 = ");
				
				R1 = new RotMatrix(pInt2.U(), pInt2.V(), pInt2.T());
				R2 = new RotMatrix(stereoAngle[pln]); 
				R = R2.multiply(R1);
				if (verbose) R.print(String.format("for stereo angle %10.7f",stereoAngle[pln]));
				Vec rscatRot = R.rotate(rscat.dif(pInt2.X()));
				if (verbose) rscatRot.print("       helix intersection in detector frame");
				m2[pln] = rscatRot.v[1] + resolution*gran[1];
				hRes.entry(resolution*gran[1]);
				if (verbose) System.out.format("       Measurement 2= %10.7f, Truth=%10.7f\n", m2[pln], rscatRot.v[1]);
				measurements.add(new Measurement(pInt2,rscat,Bfield[pln],tBfield[pln],m2[pln],resolution,stereoAngle[pln],thickness));
				//measurements.add(new Measurement(pInt2,rscat,B,tB,m2[pln],resolution,stereoAngle[pln],thickness));
				
			    if (pln != nPlanes-1) Tk = Tk.randomScat(pInt2, thickness, Bfield[pln+1], tBfield[pln+1]);    
			    if (verbose) Tk.print("scattered from the second layer of the measurement plane");
			}
						
			if (verbose) System.out.format("There were %d measurements, as follows:\n", measurements.size());
			if (nTrials == 1) {
				printWriter2.format("EOD\n");
				printWriter2.format("$pnts << EOD\n");
			}
			for (Measurement mm:measurements) {
				//mm.print(String.format(" %d",measurements.indexOf(mm)));
				Vec rmG = mm.measurementGlobal();
				if (nTrials == 1) printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
			}	
			if (nTrials == 1) {
				printWriter2.format("EOD\n");
				printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2, $helix u 1:2:3 with lines lw 3\n");
				printWriter2.close();
			}

			// Create a seed track from the first 3 or 4 layers
			SeedTrack seed = new SeedTrack(measurements, 7, verbose);
			if (!seed.success) return;
			if (nTrials==1) {
				seed.print("helix parameters");
				System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
			}
			Vec initialHelix = seed.helixParams();
			SquareMatrix initialCovariance = seed.covariance();
			
			// Cheating initial "guess" for the helix
			/*
			double [] rn = gausRan();
			double drhoGuess = drho + drhoSigma*rn[0];
			double dzGuess = dz + dzSigma*rn[1];
			rn = gausRan();
			double phi0Guess = phi0 + phi0Sigma*rn[0];
			double tanlGuess = tanl + tanlSigma*rn[1];
			double kGuess = K + kSigma*rn[0];
			Vec initialHelix = new Vec(drhoGuess, phi0Guess, kGuess, dzGuess, tanlGuess);
			SquareMatrix initialCovariance = new SquareMatrix(5);
			initialCovariance.M[0][0] = (drhoSigma*drhoSigma);
			initialCovariance.M[1][1] = (phi0Sigma*phi0Sigma);
			initialCovariance.M[2][2] = (kSigma*kSigma);
			initialCovariance.M[3][3] = (dzSigma*dzSigma);
			initialCovariance.M[4][4] = (tanlSigma*tanlSigma);
			*/
			
			initialCovariance.scale(10000.);   // Blow up the errors on the initial guess
			
			// Run the Kalman fit
			KalmanFit kF = new KalmanFit(measurements, origin, initialHelix, initialCovariance, seed.B(), seed.T(), verbose);
			
			ArrayList<MeasurementSite> sites = kF.sites;
			Iterator<MeasurementSite> itr = sites.iterator();
			while (itr.hasNext()) { 
				MeasurementSite site = itr.next();
				if (site.m.stereo == 0.) {
					hResid0.entry(site.aF.r/Math.sqrt(site.aF.R));
					hResidS0.entry(site.aS.r/Math.sqrt(site.aS.R));
				} else {
					hResid1.entry(site.aF.r/Math.sqrt(site.aF.R));
					hResidS1.entry(site.aS.r/Math.sqrt(site.aS.R));
				}
			}
			
			hChi2.entry(kF.chi2s);
			hChi2f.entry(kF.chi2f);
			
			Vec aF = kF.fittedStateBegin().pivotTransform();
			if (verbose) aF.print("final smoothed helix parameters at the track beginning with pivot at origin");
			Vec aFe = kF.fittedStateBegin().helixErrors(aF);
			SquareMatrix aFC = kF.fittedStateBegin().covariancePivotTransform(aF);
			if (verbose) aFe.print("error estimates on the helix parameters");  
			//aFC.print("helix parameters covariance");
			if (verbose) helixMCtrue.print("MC true helix at the track beginning");
			Vec trueErr = aF.dif(helixMCtrue);
			for (int i=0; i<5; i++) {
				double diff = (trueErr.v[i])/aFe.v[i];
				if (verbose) System.out.format("     Helix parameter %d, error = %10.5f sigma\n", i, diff);	
			}
			hEdrhoS.entry(trueErr.v[0]/aFe.v[0]);
			hEphi0S.entry(trueErr.v[1]/aFe.v[1]);
			hEkS.entry(trueErr.v[2]/aFe.v[2]);
			hEdzS.entry(trueErr.v[3]/aFe.v[3]);
			hEtanlS.entry(trueErr.v[4]/aFe.v[4]);
			double helixChi2 = trueErr.dot(trueErr.leftMultiply(aFC.invert()));
			if (verbose) System.out.format("Full chi^2 of the smoothed helix parameters = %12.4e\n", helixChi2);
			hChi2HelixS.entry(helixChi2);
						
			Vec eF = kF.fittedStateEnd().pivotTransform();
			if (verbose) eF.print("final smoothed helix parameters at the track end");
			Vec eFe = kF.fittedStateEnd().helixErrors(eF);
			if (verbose) eFe.print("errors on the helix parameters");		
			Vec fH = Tk.pivotTransform(new Vec(0.,0.,0.));
			if (verbose) fH.print("MC true helix at the last scatter");		
			hEdrho.entry((eF.v[0]-fH.v[0])/eFe.v[0]);
			hEphi0.entry((eF.v[1]-fH.v[1])/eFe.v[1]);
			hEk.entry((eF.v[2]-fH.v[2])/eFe.v[2]);
			hEdz.entry((eF.v[3]-fH.v[3])/eFe.v[3]);
			hEtanl.entry((eF.v[4]-fH.v[4])/eFe.v[4]);
			trueErr = eF.dif(fH);
			SquareMatrix eFc = kF.fittedStateEnd().covariancePivotTransform(new Vec(0.,0.,0.), eF);
			helixChi2 = trueErr.dot(trueErr.leftMultiply(eFc.invert()));
			if (verbose) System.out.format("Full chi^2 of the filtered helix parameters = %12.4e\n", helixChi2);
			hChi2Helix.entry(helixChi2);
		}
		
		timestamp = Instant.now();
		System.out.format("Ending time = %s\n", timestamp.toString());
		ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
		System.out.format("%s %d %d at %d:%d %d.%d seconds\n",  ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());

		hChi2.plot(path+"chi2s.gp", true, " ", " ");
		hChi2f.plot(path+"chi2f.gp", true, " ", " ");
		hChi2HelixS.plot(path+"chi2helixS.gp", true, " ", " ");
		hChi2Helix.plot(path+"chi2helixF.gp", true, " ", " ");
		hRes.plot(path+"resolution.gp", true, " ", " ");
		hEdrho.plot(path+"drhoError.gp", true, " ", " ");
		hEphi0.plot(path+"phi0Error.gp", true, " ", " ");
		hEk.plot(path+"kError.gp", true, " ", " ");
		hEdz.plot(path+"dzError.gp", true, " ", " ");
		hEtanl.plot(path+"tanlError.gp", true, " ", " ");
		hEdrhoS.plot(path+"drhoErrorS.gp", true, " ", " ");
		hEphi0S.plot(path+"phi0ErrorS.gp", true, " ", " ");
		hEkS.plot(path+"kErrorS.gp", true, " ", " ");
		hEdzS.plot(path+"dzErrorS.gp", true, " ", " ");
		hEtanlS.plot(path+"tanlErrorS.gp", true, " ", " ");
		hResid0.plot(path+"resid0.gp", true, " ", " ");
		hResid1.plot(path+"resid1.gp", true, " ", " ");
		hResidS0.plot(path+"residS0.gp", true, " ", " ");
		hResidS1.plot(path+"residS1.gp", true, " ", " ");

/*		
		// Test matrix code
		SquareMatrix t = new SquareMatrix(5);
		t.M[0][0] = 1.;
		t.M[0][1] = 1.;
		t.M[0][2] = 0.;
		t.M[0][3] = 0.2;
		t.M[0][4] = 1.5;
		t.M[1][0] = 0.;
		t.M[1][1] = 2.;
		t.M[1][2] = 0.;
		t.M[1][3] = 0.8;
		t.M[1][4] = 0.;
		t.M[2][0] = 7.;
		t.M[2][1] = 0.1;
		t.M[2][2] = 3.;
		t.M[2][3] = 0.;
		t.M[2][4] = 3.;
		t.M[3][0] = 0.;
		t.M[3][1] = 2.;
		t.M[3][2] = 0.1;
		t.M[3][3] = 4.;
		t.M[3][4] = 0.7;		
		t.M[4][0] = 0.;
		t.M[4][1] = 4.;
		t.M[4][2] = 0.;
		t.M[4][3] = 1.;
		t.M[4][4] = 5.;
		t.print("test");
		
		SquareMatrix q = t.invert();
		q.print("inverse");
		
		SquareMatrix pr = q.multiply(t);
		pr.print("product");
		*/
	}
	
	static double [] gausRan() {  //Return two gaussian random numbers

		double x1,x2,w;
		double [] gran = new double [2];
		do {
			x1 = 2.0*Math.random() - 1.0;
			x2 = 2.0*Math.random() - 1.0;
			w = x1*x1 + x2*x2;
		} while (w>=1.0);
		w = Math.sqrt((-2.0*Math.log(w))/w);
		gran[0] = x1*w;
		gran[1] = x2*w;  
		
		return gran;
	}	
	
	static double [] parabolaToCircle(double alpha, double sgn, Vec coef) {
		double R = -sgn/(2.0*coef.v[2]);
		double yc = sgn*R*coef.v[1];
		double xc = coef.v[0] - sgn*R*(1.0-0.5*coef.v[1]*coef.v[1]);
		double [] r= new double [3];
		r[1] = Math.atan2(yc, xc);
		if (R<0.) r[1] += Math.PI;
		r[2] = alpha/R;
		r[0] = xc/Math.cos(r[1]) - R;
		return r;		
	} 
	
	public HelixTest() {  
		System.out.format("Unnecessary HelixTest constructor; all the work is done in main\n");
	}

	
}

