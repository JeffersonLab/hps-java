package org.hps.users.spaul.feecc;

import hep.aida.*;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepRecord;
import hep.io.stdhep.StdhepWriter;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import static java.lang.Math.*;

public class FEESpectrumGenerator {
	/*************
	 * CONSTANTS *
	 *************/
	static double alpha = 1/137., hbarc = .197, q = 1.6e-19, Na= 6.022e23;
	static double me = .000511;
	static double amu = 931.4941;

	private static double sqrt2 = Math.sqrt(2);

	/***************
	 * OTHER STUFF *
	 ***************/

	static int counter = 0;

	static Random random = new Random();
	static double thetaMax = .1;
	static double thetaMin = .03;
	/**
	 * 
	 * @return a random angle theta, with no form factor.  
	 */
	public static double getRandomThetaBare(){
		double r = random.nextDouble();
		double dTheta = (thetaMax-thetaMin)/4;
		double theta = (thetaMax+thetaMin)/2;
		int nIter = 11;
		for(int i = 0; i< nIter; i++){
			double x = MottIntegral.integral(a, theta);
			double f = (x-xmin)/(xmax-xmin);
			if(f<r){
				theta += dTheta;
			} else if(f> r){
				theta -= dTheta;
			}
			dTheta /= 2;
		}
		return theta;


	}

	static double combine(double theta, double rms){
		return theta + sqrt2 *rms*random.nextGaussian();
	}


	/**
	 * 
	 * @param a recoil factor = 2*E/M
	 * @return a random angle theta, with no form factor.  
	 */
	public static double getRandomThetaWithFormFactor(FormFactor f){
		while(true){
			double theta = getRandomThetaBare();
			double trigstuff = pow(sin(theta/2), 2);
			double Q2 = 4*E*E*trigstuff/(1+a*trigstuff);
			counter ++;
			double f2 = f.getFormFactorSquared(Q2);
			if(f2 > random.nextDouble())
				return theta;
		}
	}



		static double b = -1;
	



	static boolean radCorrections = false;
	static int Nevents;
	static double M, E, Q, aDensity;
	static int Z;
	static double a, xmax, xmin;
	static boolean display;


	private static CustomBinning cb;
	public static void main(String arg[]) throws IOException{
		String filename = arg[0];
		M = Double.parseDouble(arg[1]);
		E = Double.parseDouble(arg[2]);
		thetaMin = Double.parseDouble(arg[3]);
		thetaMax = Double.parseDouble(arg[4]);
		Z = Integer.parseInt(arg[5]);

		String outfile = null;
		aDensity = Double.parseDouble(arg[6]);
		Q = Double.parseDouble(arg[7]);

		radCorrections = Boolean.parseBoolean(arg[8]);

		if(arg.length>9){
			display = true;
			cb = new CustomBinning(new File(arg[9]));
			outfile = arg[10];
		}


		a = 2*E/(M*amu);
		xmin = MottIntegral.integral(a, thetaMin);
		xmax = MottIntegral.integral(a, thetaMax);

		double scale = Math.pow((Z*alpha*hbarc)/(2*E), 2)/100; //this should be in barns
		double sigma = MottIntegral.mottIntegralWithFormFactor(a, scale, thetaMin, thetaMax, FormFactor.get(Z), 1000, E);
		
		double luminosity = Q*1e-9/q*aDensity/(M/Na)*1e-24;
		Nevents = (int)(sigma*luminosity);


		System.out.println(Nevents);

		String title = "fee";
		String comment = "fee";
		StdhepWriter writer = new StdhepWriter(filename, title, comment, Nevents);
		IAnalysisFactory af = null;
		ITree tree = null;

		if(display){
			af = IAnalysisFactory.create();
			tree = af.createTreeFactory().create(outfile,"xml",false,true);
			IHistogramFactory hf = af.createHistogramFactory(tree);
			
			double thetaBins[] = new double[cb.nTheta+1];
			for(int i = 0; i<cb.nTheta; i++){
				thetaBins[i] = cb.thetaMin[i];
			}

			thetaBins[thetaBins.length-1] = cb.thetaMax[cb.nTheta-1];
			
			thetaHist = hf.createHistogram1D("theta", "theta", thetaBins);
			
			EHist = hf.createHistogram1D("energy", 200, 0, 1.3);
			IPlotter p = af.createPlotterFactory().create();
			p.createRegions(2,1);
			p.region(0).plot(thetaHist);
			p.region(1).plot(EHist);
			p.show();
		}

		for(int i = 1; i<= Nevents; i++){
			int nevhep = i; 
			int nhep = 1;
			int[] isthep = {1}; 
			int[] idhep = {11};
			int[] jmohep = {0,0}; 
			int[] jdahep = {0,0};
			double[] phep = randomMomentum();
			double[] vhep = {0,0,0,0};
			
			
			if(phep[3] > .5*E){
				StdhepEvent event = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);
				double smear = .02;
				if(display)
					EHist.fill(phep[3]*(1+random.nextGaussian()*smear));
				writer.writeRecord(event);
			}
			else{ // if the event has enough energy loss,
				  // write it out as a blank event.  This way,
				  // slic doesnt have to deal with low energy electrons
				  // 
				StdhepEvent event = new StdhepEvent(i, 0, 
						new int[0],
						new int[0],
						new int[0],
						new int[0],
						new double[0],
						new double[0]);
				writer.writeRecord(event);
			}
			if(i%1000 == 0)
				System.out.println("event " + i);
		}
		writer.close();
		if(tree != null)
			tree.commit();
		
		/*
		String title = arg[0+10*set];
		String inputFile = arg[1+10*set];
		CustomBinning cb = new CustomBinning(new File(arg[2+10*set]));


		double Q = Double.parseDouble(arg[3+10*set]); //in nC
		double E = Double.parseDouble(arg[4+10*set]); //in GeV
		double aDensity = Double.parseDouble(arg[5+10*set]); //in g/cm^2
		double M = Double.parseDouble(arg[6+10*set]); // in amu
		double Z = Double.parseDouble(arg[7+10*set]); 
		double r = Double.parseDouble(arg[8+10*set]);
		double prescale = Math.pow(2,Double.parseDouble(arg[9+10*set]));
		 */
		if(display){
			ExtractFormFactors.main(new String[]{
					"generated",
					outfile,
					arg[9],
					Double.toString(Q),
					Double.toString(E),
					Double.toString(aDensity),
					Double.toString(M),
					Double.toString(Z),
					"3", //not used
					"0" //no prescale

			});

			displayResiduals();

		}
	}

	private static void displayResiduals() {
		IAnalysisFactory af = IAnalysisFactory.create();
		IDataPointSetFactory dpsf = af.createDataPointSetFactory(af.createTreeFactory().create());
		IDataPointSet residuals = dpsf.create("residuals", 2);
		for(int i = 0; i<ExtractFormFactors.dpsMeasured[0].size(); i++){
			IDataPoint gen = ExtractFormFactors.dpsMeasured[0].point(i);
			IDataPoint the = ExtractFormFactors.dpsTheory.point(i);

			IDataPoint res = residuals.addPoint();
			res.coordinate(0).setValue(gen.coordinate(0).value());
			res.coordinate(1).setValue(gen.coordinate(1).value()-the.coordinate(1).value());
			double err = Math.hypot(gen.coordinate(1).errorMinus(), the.coordinate(1).errorMinus());
			res.coordinate(1).setErrorMinus(err);
			res.coordinate(1).setErrorPlus(err);
		}
		IPlotter p = af.createPlotterFactory().create();
		p.region(0).plot(residuals);
		p.show();

	}



	static IHistogram1D thetaHist = null, EHist;



	private static double[] randomMomentum() {
		double theta = getRandomThetaWithFormFactor(FormFactor.get(Z));
		double phi = random.nextDouble()*2*PI-PI;
		if(thetaHist != null)
			if(cb.inRange(theta, phi))
				thetaHist.fill(theta);
		double trigstuff = pow(sin(theta/2.), 2);
		double Ep = E/(1+a*trigstuff);
		double m = .000511;



		if(radCorrections){

			// L.W. Mo, Y.S. Tsai, Radiative Corrections to Elastic and inelastic ep and $\mu$p scattering
			// Stanford Linear Accelerator Center, Stanford University, Stanford, CA
			// Reviews of Modern Physics  Volume 41 Number 1, January 1969

			//otherwise apply radiative corrections
			double Q2 = 4*E*Ep*trigstuff;

			//effective number of pre and post radiation lengths caused by radiative effects
			double t = 3/4.*(alpha/Math.PI)*(Math.log(Q2/(me*me)-1));

			// rms theta for 
			double theta_rms = MultipleScattering.getThetaRMS(t, E);
			theta = combine(theta, theta_rms);
			
			if(b == -1){
				b = MultipleScattering.b(Z);
			}
			double bt = b*t;
			Ep *= MultipleScattering.getRandomEnergyFraction(bt)*MultipleScattering.getRandomEnergyFraction(bt);
			
		}
		if(Double.isNaN(Ep))
			return null;
		double p = sqrt(Ep*Ep-m*m);
		return new double[]{p*sin(theta)*cos(phi), p*sin(theta)*sin(phi), p*cos(theta), Ep, m};
	}
}
