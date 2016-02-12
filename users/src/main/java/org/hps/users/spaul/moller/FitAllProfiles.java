package org.hps.users.spaul.moller;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;
import hep.aida.ITree;

import java.io.IOException;

public class FitAllProfiles {
	static IAnalysisFactory af = IAnalysisFactory.create();
	static IFitFactory ff = af.createFitFactory();
	
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		
		ITree tree = af.createTreeFactory().create(arg[0]); 
		IPlotter p = af.createPlotterFactory().create();
		p.createRegions(3,2);
		plotAndFit(p, 0, (IHistogram1D)tree.find("pypz"));
		plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz"));
		plotAndFit(p, 2, (IProfile1D)tree.find("pypz vs diff"));
		plotAndFit(p, 3, (IProfile1D)tree.find("pxpz vs diff"));
		
		p.show();
	}
	public static void plotAndFit(IPlotter p, int r, IHistogram1D h){
		p.region(r).plot(h);
		IFitResult fit = ff.createFitter().fit(h, "g");
		IFunction func = fit.fittedFunction();
		System.out.println("\n" + h.title());
		String names[] = func.parameterNames();
		double params[] = func.parameters();
		for(int i = 0; i< names.length; i++){
			System.out.printf("%s: %f\n", names[i], params[i]);
		}
		p.region(r).plot(func);
		
	}
	public static void plotAndFit(IPlotter p, int r, IProfile1D h){
		p.region(r).plot(h);
		IFitResult fit = ff.createFitter().fit(h, "p4");
		IFunction func = fit.fittedFunction();
		System.out.println("\n" + h.title());
		String names[] = func.parameterNames();
		double params[] = func.parameters();
		for(int i = 0; i< names.length; i++){
			System.out.printf("%s: %f\t", names[i], params[i]);
		}
		p.region(r).plot(func);
		
	}
}
