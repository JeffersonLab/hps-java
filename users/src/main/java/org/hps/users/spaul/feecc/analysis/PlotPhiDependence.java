package org.hps.users.spaul.feecc.analysis;

import hep.aida.*;

import org.hps.users.spaul.feecc.CustomBinning;
import org.hps.users.spaul.StyleUtil;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

//arg[0] should point to the sums directory,
// with files that are named runnum.aida and one file total.aida
public class PlotPhiDependence {
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		IAnalysisFactory af = IAnalysisFactory.create();
		ITreeFactory tf = af.createTreeFactory();
		IHistogramFactory hf = af.createHistogramFactory(tf.create());

		ITree tree0 = tf.create(arg[0], "xml");
		IHistogram2D hist = (IHistogram2D) tree0.find("theta vs phi alt");

		int nBinsTheta = hist.xAxis().bins();

		for(int i = 0; i< nBinsTheta; i++){
			IPlotter p = af.createPlotterFactory().create();
			IHistogram1D proj = hf.sliceY("bin " + i, hist, i);
			p.region(0).plot(proj);
			StyleUtil.stylize(p.region(0),"Phi Dependence", "phi", "# of hits");
			System.out.println(Arrays.toString(p.region(0).style().dataStyle().markerStyle().availableParameters()));
			p.region(0).style().dataStyle().setParameter("showHistogramBars", "false");
			
			p.show();
		}
		
	}




}
