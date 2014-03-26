package org.hps.users.omoreno;

import hep.aida.ICloud2D;
//--- hep ---//
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.util.aida.AIDA;

public class PlotUtils {

	// Default ctor
	public PlotUtils(){}	

	public static IPlotter setupPlotter(String title, int regionX, int regionY){
	    IPlotter plotter = AIDA.defaultInstance().analysisFactory().createPlotterFactory().create(title);
	    plotter.setTitle(title);
	    
	    if(regionX < 0 || regionY < 0) throw new RuntimeException("Region dimensions need to be greater than 0!");
	    else if(regionX != 0 || regionY != 0) plotter.createRegions(regionX, regionY);
	    
	    plotter.style().statisticsBoxStyle().setVisible(false);
	    plotter.style().dataStyle().errorBarStyle().setVisible(false);
	    plotter.setParameter("plotterWidth", "800");
	    plotter.setParameter("plotterHeight", "800");
	    
	    return plotter;
	    
	}
    
    public static void setup2DRegion(IPlotter plotter, String title, int region, String xTitle, String yTitle, IHistogram2D histo){
    	
    	// Check if the specified region is valid
    	if(region > plotter.numberOfRegions()) 
    		throw new RuntimeException("Region is invalid! " + title + " contains " + plotter.numberOfRegions() + " regions");
    	
		plotter.region(region).style().xAxisStyle().setLabel(xTitle);
		plotter.region(region).style().xAxisStyle().labelStyle().setFontSize(14);
		plotter.region(region).style().yAxisStyle().setLabel(yTitle);
		plotter.region(region).style().yAxisStyle().labelStyle().setFontSize(14);
		plotter.region(region).style().xAxisStyle().setVisible(true);
		plotter.region(region).style().yAxisStyle().setVisible(true);
		plotter.region(region).style().setParameter("hist2DStyle", "colorMap");
    	plotter.region(region).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
    	
    	if(histo != null) plotter.region(region).plot(histo);
    }
   
    public static void setup2DRegion(IPlotter plotter, String title, int region, String xTitle, String yTitle, ICloud2D cloud, IPlotterStyle style){
    	
    	// Check if the specified region is valid
    	if(region > plotter.numberOfRegions()) 
    		throw new RuntimeException("Region is invalid! " + title + " contains " + plotter.numberOfRegions() + " regions");
    	
		plotter.region(region).style().xAxisStyle().setLabel(xTitle);
		plotter.region(region).style().xAxisStyle().labelStyle().setFontSize(14);
		String[] pars = plotter.region(region).style().xAxisStyle().availableParameters();
		plotter.region(region).style().yAxisStyle().setLabel(yTitle);
		plotter.region(region).style().yAxisStyle().labelStyle().setFontSize(14);
		plotter.region(region).style().xAxisStyle().setVisible(true);
		plotter.region(region).style().yAxisStyle().setVisible(true);
		plotter.region(region).style().setParameter("showAsScatterPlot", "true");
    	
    	if(cloud != null) plotter.region(region).plot(cloud, style);
    }
    
    
    public static void setup1DRegion(IPlotter plotter, String title, int region, String xTitle, IHistogram1D histo){
    	
		plotter.region(region).style().xAxisStyle().setLabel(xTitle);
		plotter.region(region).style().xAxisStyle().labelStyle().setFontSize(14);
		plotter.region(region).style().xAxisStyle().setVisible(true);
		plotter.region(region).style().dataStyle().fillStyle().setVisible(false);
		plotter.region(region).style().dataStyle().lineStyle().setThickness(3);
		
		if(histo != null) plotter.region(region).plot(histo);
    }
    
    /**
     * 
     */
    public static int getPlotterRegion(SiSensor sensor) {

        int layer = SvtUtils.getInstance().getLayerNumber(sensor);

        // Compute the sensor's x and y grid coordinates and then translate to region number.
        int ix = (layer - 1) / 2;
        int iy = 0;
        if (!SvtUtils.getInstance().isTopLayer(sensor)) {
            iy += 2;
        }
        if (layer % 2 == 0) {
            iy += 1;
        }
        int region = ix * 4 + iy;
        return region;
    }
    
}
