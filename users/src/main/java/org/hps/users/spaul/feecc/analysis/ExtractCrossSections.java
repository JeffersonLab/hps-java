package org.hps.users.spaul.feecc.analysis;

import hep.aida.*;

import java.io.File;
import java.io.IOException;

//arg[0] should point to the sums directory,
// with files that are named runnum.aida and one file total.aida
public class ExtractCrossSections {
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		IAnalysisFactory af = IAnalysisFactory.create();
		 ITreeFactory tf = af.createTreeFactory();
		
		boolean isFirst = true;
		int nBins =0;
		for(File f : new File(arg[0]).listFiles()){
			String[] split = f.getAbsolutePath().split("[\\./]");
			String a = split[split.length-2];
			if(a.equals("total"))
				continue;
			int runNumber = Integer.parseInt(a);

			 ITree tree0 = tf.create(f.getAbsolutePath(), "xml");
			IHistogram1D theta = (IHistogram1D) tree0.find("theta");
			
			if(isFirst){
				nBins = theta.axis().bins();
				isFirst = false;
			}
			double beamCharge = getBeamCharge(runNumber);
			System.out.println(runNumber + "\t");
			for(int i = 0; i< nBins; i++)
				System.out.print(theta.binHeight(i)/beamCharge + "\t");
			System.out.println();
		}
	}

	private static double getBeamCharge(int runNumber) {
		//placeholder until we have the beam charges reliably in the database.
		return 1;  
	}
}
