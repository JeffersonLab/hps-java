package org.hps.users.spaul.moller;

import java.io.IOException;
import java.util.Arrays;

import org.hps.users.spaul.StyleUtil;

import hep.aida.*;

public class FitMollerPyPzGraphs {
	static IAnalysisFactory af = IAnalysisFactory.create();
	static IFitFactory ff = af.createFitFactory();
	
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		
		ITree tree = af.createTreeFactory().create(arg[0]); 
		/*IPlotter p = af.createPlotterFactory().create();
		p.createRegions(4,2);
		plotAndFit(p, 0, (IHistogram1D)tree.find("pypz"), "uy all", "uy");
		plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz"), "ux all", "ux");
		plotAndFit(p, 2, (IHistogram1D)tree.find("pypz bot"), "uy (bot)", "uy");
		plotAndFit(p, 3, (IHistogram1D)tree.find("pxpz bot"), "ux (bot)", "ux");
		plotAndFit(p, 4, (IHistogram1D)tree.find("pypz mid"), "uy (mid)", "uy");
		plotAndFit(p, 5, (IHistogram1D)tree.find("pxpz mid"), "ux (mid)", "ux");
		plotAndFit(p, 6, (IHistogram1D)tree.find("pypz top"), "uy (top)", "uy");
		plotAndFit(p, 7, (IHistogram1D)tree.find("pxpz top"), "ux (top)", "ux");

		StyleUtil.setSize(p, 1000, 500);
		p.show();*/
		

		IPlotter p;
		p = af.createPlotterFactory().create();
		p.region(0).plot((IHistogram1D) tree.find("diff"));
		StyleUtil.stylize(p.region(0), "pz top - pz bottom (GeV)", "#");
		p.show();
		
		p = af.createPlotterFactory().create();
		p.createRegions(2, 1);
		plotAndFit(p, 0, (IHistogram1D)tree.find("pypz bot"), "bot", "uy");
		plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz bot"), "bot", "ux");
		plotAndFit(p, 0, (IHistogram1D)tree.find("pypz mid"), "mid", "uy");
		plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz mid"), "mid", "ux");
		plotAndFit(p, 0, (IHistogram1D)tree.find("pypz top"), "top", "uy");
		plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz top"), "top", "ux");
		StyleUtil.setSize(p, 1000, 500);
		p.show();
		
	}
	public static void plotAndFit(IPlotter p, int r, IHistogram1D h, String title, String xAxis){
		p.region(r).plot(h);
		h.setTitle(title);
		StyleUtil.noFillHistogramBars(p.region(r));
		IFitResult fit = ff.createFitter().fit(h, "g");
		StyleUtil.stylize(p.region(r), title, xAxis, "#");
		IFunction func = fit.fittedFunction();
		System.out.println("\n" + h.title());
		String names[] = func.parameterNames();
		double params[] = func.parameters();
		for(int i = 0; i< names.length; i++){
			System.out.printf("%s: %f\t", names[i], params[i]);
		}
		//System.out.println(Arrays.toString(fit.);
		
		p.region(r).plot(func);
		StyleUtil.stylize(p.region(r), title, xAxis, "#");
		p.region(r).style().statisticsBoxStyle().setVisible(true);
		p.region(r).style().legendBoxStyle().setVisible(false);
		//System.out.println(Arrays.toString(p.region(r).style().statisticsBoxStyle().availableParameters()));
		//p.show();
	}
	
	
}
