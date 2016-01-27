package org.hps.users.spaul.feecc;

import hep.aida.*;


import java.io.File;
import java.io.IOException;

import org.hps.users.spaul.StyleUtil;

//arg[0] should point to the sums directory,
// with files that are named runnum.aida and one file total.aida
//arg[1] is the binning scheme file.  
//arg[2] is the luminosity (corrected for prescaling, blinding etc.) in barns^-1
//arg[3] is the scale factor for the mott scattering (Z^2*alpha^2)/(4*E^2) in barns
//arg[4] is recoil parameter (2E/M) in Mott scattering
//arg[5] is the radius (in fm) for calculating predicted form factors

public class ExtractFormFactors {
	static double Na = 6.022e23;
	static double hbarc = .1973269788;
	static double alpha = 1/137.;
	static double q = 1.602e-19;
	static double amu = .931454;
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		IAnalysisFactory af = IAnalysisFactory.create();
		ITreeFactory tf = af.createTreeFactory();

		String[] inpaths = new String[]{"theta", "theta top", "theta bottom"};
		String[] outpaths = new String[]{"form factors", "top only", "bottom only"};
		
		for(int config = 0; config < 3; config ++){
			ITree treeNew = af.createTreeFactory().create();



			IDataPointSetFactory dpsf = af.createDataPointSetFactory(treeNew);

			IPlotter p = af.createPlotterFactory().create(outpaths[config]);


			int Nsets = arg.length/10;
			dpsMeasured = new IDataPointSet[Nsets];

			for(int set = 0; set< Nsets; set++){
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

				double luminosity = Q*1e-9/q*aDensity/(M/Na)*1e-24;  //nuclei per barn times # of beam electrons
				
				double scale = Math.pow((Z*alpha*hbarc)/(2*E), 2)/100; //this should be in barns
				
				if(config != 0) //one half of the detector only
					scale /= 2;  
				double recoil = 2*E/(M*amu);


				//double luminosity = Double.parseDouble(arg[2]);
				//double scale = Double.parseDouble(arg[3]);
				//double recoil = Double.parseDouble(arg[4]);
				//double r = Double.parseDouble(arg[5]);

				ITree tree0 = tf.create(inputFile, "xml");
				//IHistogram1D hist_iso = (IHistogram1D) tree0.find("theta isolated ");
				IHistogram1D hist = (IHistogram1D) tree0.find(inpaths[config]);
				System.out.println(inpaths[config] + " " + title + " " + hist.binHeight(0));

				dpsMeasured[set] = dpsf.create(title, 2);
				//IDataPointSet dps_iso = dpsf.create("F(Q)^2 (measured isolated)", 2);

				if(set == 0)
					dpsTheory = dpsf.create("theoretical", 2);

				int nBins = hist.axis().bins();

				for(int i = 0; i< nBins; i++){
					double thmax = cb.thetaMax[i];
					double thmin = cb.thetaMin[i];
					double n = hist.binHeight(i);
					//double n_iso = hist_iso.binHeight(i);

					//scale = Math.pow(74/137.*.2/(2*1.057), 2);
					double mottInt = MottIntegral.mottIntegral(recoil, scale, i, cb);
					double F2 =n*prescale/luminosity/mottInt;
					double dF2 = Math.sqrt(n)*prescale/luminosity/mottInt;

					//double F2_iso =n_iso*prescale/luminosity/mottInt;
					//double dF2_iso = Math.sqrt(n_iso)*prescale/luminosity/mottInt;


					IDataPoint dp = dpsMeasured[set].addPoint();

					double trigStuff = Math.pow(Math.sin((thmax+thmin)/4),2);
					double Q2 = 4*E*E*trigStuff/(1+recoil*trigStuff);
					double dQ2 = E*E*(thmax+thmin)*(thmax-thmin)*2;
					dp.coordinate(0).setValue(Q2);
					dp.coordinate(0).setErrorMinus(dQ2);
					dp.coordinate(0).setErrorPlus(dQ2);
					dp.coordinate(1).setValue(F2);
					dp.coordinate(1).setErrorMinus(dF2);
					dp.coordinate(1).setErrorPlus(dF2);

					/*dp = dps_iso.addPoint();
				dp.coordinate(0).setValue(Q2);
				dp.coordinate(0).setErrorMinus(dQ2);
				dp.coordinate(0).setErrorPlus(dQ2);
				dp.coordinate(1).setValue(F2_iso);
				dp.coordinate(1).setErrorMinus(dF2_iso);
				dp.coordinate(1).setErrorPlus(dF2_iso);
					 */
					double F_calc = 0;
					if(Z == 74){
						//double R = 5.373; 
						double R = 6.87;
						F_calc = F_calc(Q2, R);
					} else if(Z== 6){
						double rp = 0.8786;
						double a = 1.64;
						F_calc = F_calc_alt(Q2, 6,a, Math.sqrt(a*a*(1-1/12.)+rp*rp) );
					}

					if(set == 0){
						dp = dpsTheory.addPoint();
						dp.coordinate(0).setValue(Q2);
						//double F_calc = 1-r*r*Q2/(6*hbarc*hbarc);
						dp.coordinate(1).setValue(F_calc*F_calc);
						dp.coordinate(1).setErrorMinus(0);
						dp.coordinate(1).setErrorPlus(0);

					}
					//System.out.println(i + "\t" + thmin+ "\t" + thmax + "\t"+ F_calc*F_calc + "\t" + F2 + "\t" + dF2);







				}

				p.region(0).plot(dpsMeasured[set]);

				//p.region(0).plot(dps_iso);
			}
			p.region(0).plot(dpsTheory);
			StyleUtil.stylize(p.region(0), "Form Factors", "Q^2 (GeV^2)", "|F(Q^2)|^2");
			p.show();
		}

	}
	static IDataPointSet dpsMeasured[];
	static IDataPointSet dpsTheory;

	static double F_calc(double Q2, double r){
		double x = Math.sqrt(Q2*r*r/(hbarc*hbarc));
		double F_calc = 3/(x*x*x)*(Math.sin(x)-x*Math.cos(x));
		return F_calc;
	}

	static double F_calc_alt(double Q2, double Z, double a, double b){
		return (1-(Z-2)/(6*Z)*a*a*Q2/(hbarc*hbarc))*Math.exp(-1/4.*b*b*Q2/(hbarc*hbarc));
	}


	void temp_test(){
		
	}

}
