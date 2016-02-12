package org.hps.users.spaul;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

import hep.aida.*;
import hep.aida.ref.plotter.PlotterUtilities;

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
		//	r.style().statisticsBoxStyle().set;
		//debugPrint());
		r.style().legendBoxStyle().textStyle().setFontSize(16);
		r.style().statisticsBoxStyle().textStyle().setFontSize(16);
		
		//r.style().dataStyle().showInLegendBox(false);
		
		r.style().legendBoxStyle().boxStyle().foregroundStyle().setOpacity(1.0);
		r.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
		r.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", "false");
		//debugPrint(r.style().dataStyle().availableParameters()));
		//r.style().dataStyle().setParameter("showDataInStatisticsBox", "false");
		r.style().setParameter("hist2DStyle", "colorMap");
		//r.style().dataBoxStyle()
	}
	public static void addHorizontalLine(IPlotterRegion r, double y, String name){
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
	public static void noFillHistogramBars(IPlotterRegion region) {
		region.style().dataStyle().setParameter("fillHistogramBars", "false");
		region.style().dataStyle().fillStyle().setVisible(false);
		

		region.style().dataStyle().lineStyle().setParameter("colorRotateMethod", "regionOverlayIndex");
		region.style().dataStyle().lineStyle().setParameter("colorRotate", "black, red, green, blue");
		region.style().dataStyle().lineStyle().setParameter("thickness", "3");
		region.style().dataStyle().outlineStyle().setParameter("colorRotateMethod", "regionOverlayIndex");
		//debug = true;
		debugPrint(region.style().dataStyle().outlineStyle().availableParameters());
		region.style().dataStyle().outlineStyle().setParameter("colorRotate", "black, red, green, blue");
		region.style().dataStyle().outlineStyle().setParameter("thickness", "3");
		region.style().dataStyle().errorBarStyle().setVisible(false);
		debugPrint(region.style().dataStyle().lineStyle().availableParameterOptions("colorRotateMethod"));
	}
	public static void setSize(IPlotter p, int width, int height){
		p.setParameter("plotterWidth", width +"");
		p.setParameter("plotterHeight", height +"");
	}
	
	public static void setLog(IPlotterRegion r){

		r.style().yAxisStyle().setParameter("scale", "log");
		r.style().gridStyle().setUnits(100);
		debugPrint(r.style().gridStyle().availableParameters()); 
		
	}
	static boolean debug = false;
	static void debugPrint(String[] stuff){
		if(debug){
			System.out.println(Arrays.toString(stuff));
		}
	}
	public static void main(String arg[]){
		IAnalysisFactory af = IAnalysisFactory.create();
		IHistogramFactory hf = af.createHistogramFactory(af.createTreeFactory().create());
		
		IPlotter p = af.createPlotterFactory().create();
		debugPrint(p.availableParameters());
		p.createRegions(1, 2);
		IHistogram1D h1 = hf.createHistogram1D("blah", 100, -5, 5);
		IHistogram1D h2 = hf.createHistogram1D("bleh", 100, -5, 5);
		Random random = new Random();
		for(int i = 0; i< 100000; i++){
			h1.fill(random.nextGaussian());
			h2.fill(random.nextGaussian()*2);
		}
		hideLegendAndStats(p.region(1));
		noFillHistogramBars(p.region(0));
		stylize(p.region(0), "title", "x axis label", "y axis label");
		stylize(p.region(1), "stuff", "x axis label", "y axis label");
		p.region(0).plot(h1);
		p.region(0).plot(h2);
		

		IHistogram2D h3 = hf.createHistogram2D("blah", 100, -5, 5, 100, -5,5);
		
		for(int i = 0; i< 100000; i++){
			h3.fill(random.nextGaussian(), random.nextGaussian());
		}
		
		p.region(1).plot(h3);
		
		
		
		p.show();
		
		p = af.createPlotterFactory().create();
		debugPrint(p.availableParameters());
		p.createRegions(1, 2);
		
		p.region(0).plot(h1);
		setLog(p.region(0));
		
		
		
		p.show();
	}
	public static void hideLegendAndStats(IPlotterRegion r){
		r.style().statisticsBoxStyle().setVisible(false);
		r.style().legendBoxStyle().setVisible(false);
	}
	public static IPlotterStyle smoothCurveStyle(IPlotterFactory pf) {
		IPlotterStyle style = pf.createPlotterStyle();
		debugPrint(style.dataStyle().availableParameters());
		
		style.dataStyle().markerStyle().setVisible(false);
		
		return style;
	}
	public static void writeToFile(IPlotter plotter, String filename, String filetype){
		//JFrame frame = new JFrame()
		//if(plotter.)
		//plotter.hide();
		//plotter.show();
		//PlotterUtilities.writeToFile(plotter, filename, filetype, null);
		try {
			
			
			//PlotterUtilities.writeToFile(plotter, filename, filetype, null);
			Thread.sleep(1000);
			Component c = PlotterUtilities.componentForPlotter(plotter);
			int width = Integer.parseInt(plotter.parameterValue("plotterWidth"));
			int height = Integer.parseInt(plotter.parameterValue("plotterHeight"));
			if(width <= 0){
				width = 300;
				plotter.setParameter("plotterWidth", Integer.toString(width));
			}
			if(height <= 0){
				height = 300;

				plotter.setParameter("plotterHeight", Integer.toString(height));
			}
			
			c.setSize(width, height);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = image.createGraphics();
			c.paint(graphics2D);
			ImageIO.write(image,filetype, new File(filename));
			Runtime.getRuntime().exec("open " + filename);
			System.out.println("saved");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
