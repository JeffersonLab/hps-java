package org.hps.users.kmccarty.plots;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ref.plotter.PlotterRegion;

import org.hps.users.kmccarty.PlotsFormatter;
import org.hps.users.kmccarty.PlotsFormatter.ColorStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlotFormatModule {
	private String width = "2000";
	private String height = "1200";
	private List<FormattedPlot1D> formattedPlots1D = new ArrayList<FormattedPlot1D>();
	private List<FormattedPlot2D> formattedPlots2D = new ArrayList<FormattedPlot2D>();
	
	public void addPlot1D(FormattedPlot1D plot) {
		formattedPlots1D.add(plot);
	}
	
	public void addPlot2D(FormattedPlot2D plot) {
		formattedPlots2D.add(plot);
	}
	
	public void setDisplayHeight(int height) {
		this.height = "" + height;
	}
	
	public void setDisplayWidth(int width) {
		this.width = "" + width;
	}
	
	public void displayPlots() {
		try { processPlots(null); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public void savePlots(String filePath) throws IOException {
		processPlots(filePath);
	}
	
	private void processPlots(String filePath) throws IOException {
		// Create a plotter factory.
		IAnalysisFactory af = IAnalysisFactory.create();
		IPlotterFactory plotterFactory = af.createPlotterFactory();
		
		// Format and display the 1D plots.
		for(FormattedPlot1D formattedPlot : formattedPlots1D) {
			// Get the plot.
			IHistogram1D plot = formattedPlot.getPlot();
			
			// Create a plotter and plotting region for the plot.
			IPlotter plotter = plotterFactory.create(plot.title());
			plotter.createRegions(1);
			plotter.region(0).plot(plot);
			
			// Format the axis labels.
			PlotterRegion region = (PlotterRegion) plotter.region(0);
			region.getPlot().setTitle(formattedPlot.getPlotName());
			region.getPlot().getXAxis().setLabel(formattedPlot.getXAxisName());
			region.getPlot().getYAxis().setLabel(formattedPlot.getYAxisName());
			
			// Format the fonts and general plot presentation.
			PlotsFormatter.setDefault1DStyle(region, new ColorStyle[] { formattedPlot.getColorStyle() });
			
			// Set the plotter dimensions.
			plotter.setParameter("plotterWidth", width);
			plotter.setParameter("plotterHeight", height);
			
			// If the file path is null, display the plots. Otherwise,
			// save them to the destination folder.
			if(filePath == null) { plotter.show(); }
			else { plotter.writeToFile(filePath + formattedPlot.getPlotName() + ".png"); }
		}
		
		// Format and display the 2D plots.
		for(FormattedPlot2D formattedPlot : formattedPlots2D) {
			// Get the plot.
			IHistogram2D plot = formattedPlot.getPlot();
			
			// Create a plotter and plotting region for the plot.
			IPlotter plotter = plotterFactory.create(formattedPlot.getPlotName());
			plotter.createRegions(1);
			plotter.region(0).plot(plot);
			
			// Format the axis labels.
			PlotterRegion region = (PlotterRegion) plotter.region(0);
			region.getPlot().setTitle(formattedPlot.getPlotName());
			region.getPlot().getXAxis().setLabel(formattedPlot.getXAxisName());
			region.getPlot().getYAxis().setLabel(formattedPlot.getYAxisName());
			
			
			// Format the fonts and general plot presentation.
			PlotsFormatter.setDefault2DStyle(region, formattedPlot.isLogarithmic());
			
			// Set the plotter dimensions.
			plotter.setParameter("plotterWidth", width);
			plotter.setParameter("plotterHeight", height);
			
			// If the file path is null, display the plots. Otherwise,
			// save them to the destination folder.
			if(filePath == null) { plotter.show(); }
			else { plotter.writeToFile(filePath + formattedPlot.getPlotName() + ".png"); }
		}
	}
}
