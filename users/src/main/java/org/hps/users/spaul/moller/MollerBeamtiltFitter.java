package org.hps.users.spaul.moller;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;

import java.io.IOException;

import org.hps.users.spaul.StyleUtil;

public class MollerBeamtiltFitter {
	static IAnalysisFactory af = IAnalysisFactory.create();
	static IFitFactory ff = af.createFitFactory();
	static IPlotterFactory pf = af.createPlotterFactory();
	
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		
		ITree tree = af.createTreeFactory().create(arg[0]); 
		

		IPlotter p;
		
		p = pf.create();
		p.createRegions(2, 1);

		plotAndFit(p, 0, (IHistogram1D)tree.find("pxpz"), "ux", "ux");
		plotAndFit(p, 1, (IHistogram1D)tree.find("pypz"), "uy", "uy");
		StyleUtil.setSize(p, 1000, 500);
		p.show();
		
	}
	public static void plotAndFit(IPlotter p, int r, IHistogram1D h, String title, String xAxis){
		p.region(r).plot(h);
		h.setTitle(title);
		
		double xmin = h.mean()-2*h.rms();
		double xmax = h.mean()+2*h.rms();
		String range = String.format("range=\"(%f,%f)\"", xmin, xmax);
		//range = "";
		IFitResult fit = ff.createFitter().fit(h, "g", range);
		IFunction func = fit.fittedFunction();
		System.out.println("\n" + h.title());
		String names[] = func.parameterNames();
		double params[] = func.parameters();
		for(int i = 0; i< names.length; i++){
			System.out.printf("%s: %f\t", names[i], params[i]);
		}
		//System.out.println(Arrays.toString(fit.);
		IPlotterStyle style = p.region(r).style();
		style.dataStyle().outlineStyle().setColor("blue");
		p.region(r).plot(func, style);
		StyleUtil.noFillHistogramBars(p.region(r));
		StyleUtil.stylize(p.region(r), title, xAxis, "#");
		p.region(r).style().statisticsBoxStyle().setVisible(true);
		p.region(r).style().legendBoxStyle().setVisible(false);
		//System.out.println(Arrays.toString(p.region(r).style().statisticsBoxStyle().availableParameters()));
		//p.show();
	}
}
