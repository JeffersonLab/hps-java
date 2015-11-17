package org.hps.users.spaul;

import java.util.Arrays;

import hep.aida.IAnalysisFactory;
import hep.aida.IFunction;
import hep.aida.IPlotterRegion;

public class StyleUtil {
	
	public static void stylize(IPlotterRegion r, String title, String lx, String ly, double xmin, double xmax, double ymin, double ymax){
		r.setTitle(title);
		stylize(r, lx, ly);
		r.setXLimits(xmin, xmax);
		r.setYLimits(ymin, ymax);
	}
	public static void stylize(IPlotterRegion r, String title, String lx, String ly){
		r.setTitle(title);
		stylize(r, lx, ly);
	}
	public static void stylize(IPlotterRegion r, String lx, String ly){
		
		r.style().titleStyle().textStyle().setFontSize(22);
		r.style().xAxisStyle().setLabel(lx);
		r.style().xAxisStyle().labelStyle().setFontSize(16);
		r.style().xAxisStyle().tickLabelStyle().setFontSize(14);
		r.style().yAxisStyle().setLabel(ly);
		r.style().yAxisStyle().labelStyle().setFontSize(16);
		r.style().yAxisStyle().tickLabelStyle().setFontSize(14);
		r.style().statisticsBoxStyle().setParameter("backgroundColor", "white");
		r.style().statisticsBoxStyle().boxStyle().backgroundStyle().setColor("White");
		r.style().statisticsBoxStyle().boxStyle().backgroundStyle().setParameter("color", "white");
		r.style().statisticsBoxStyle().boxStyle().backgroundStyle().setOpacity(100);
		System.out.println(Arrays.toString(r.style().dataStyle().availableParameters()));
		r.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
		r.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", "false");
		//r.style().dataStyle().setParameter("showDataInStatisticsBox", "false");
		r.style().setParameter("hist2DStyle", "colorMap");
		//r.style().dataBoxStyle()
	}
	static void addHorizontalLine(IPlotterRegion r, double y, String name){
		IAnalysisFactory af = IAnalysisFactory.create();
		IFunction f = af.createFunctionFactory(af.createTreeFactory().create()).createFunctionByName(name, "p0");
		f.setParameter("p0", y);
		r.plot(f);
	}
	public static void addParabola(IPlotterRegion region, double p0,
			double p1, double p2, String string) {
		IAnalysisFactory af = IAnalysisFactory.create();
		IFunction f = af.createFunctionFactory(af.createTreeFactory().create()).createFunctionByName(string, "p2");
		f.setParameter("p0", p0);
		f.setParameter("p1", p1);
		f.setParameter("p2", p2);
		region.plot(f);
	}
}
