package org.hps.users.spaul.feecc.analysis;

import hep.aida.*;
import org.hps.users.spaul.feecc.CustomBinning;

import java.io.File;
import java.io.IOException;

//arg[0] should point to the sums directory,
// with files that are named runnum.aida and one file total.aida
//arg[1] is the binning scheme file.  
//arg[2] is an overall normalization scale (2E/Zalpha)*sqrt(cc/counts)
//arg[3] is recoil parameter (2E/M)
public class CountsPerBinVsCharge {
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		IAnalysisFactory af = IAnalysisFactory.create();
		ITreeFactory tf = af.createTreeFactory();
		CustomBinning cb = new CustomBinning(new File(arg[1]));
		double scale = Double.parseDouble(arg[2]);
		double recoil = Double.parseDouble(arg[3]);
		
		ITree tree0 = tf.create(arg[0], "xml");
		IHistogram1D hist = (IHistogram1D) tree0.find("theta");

		int nBins = hist.axis().bins();
		for(int i = 0; i< nBins; i++){
			double thmax = cb.thetaMax[i];
			double thmin = cb.thetaMin[i];
			double n = hist.binHeight(i);
			double mottInt = cb.mottIntegralFactor(recoil, i);
			double F = scale*Math.sqrt(n/mottInt);
			double dF = scale/(2*Math.sqrt(mottInt));
				System.out.println(i + "\t" + thmin+ "\t" + thmax + "\t" + F + "\t" + dF);
		}

	}

	private static double getBeamCharge(int runNumber) {
		//placeholder until we have the beam charges reliably in the database.
		return 1;  
	}
}
