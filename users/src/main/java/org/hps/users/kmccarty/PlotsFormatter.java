package org.hps.users.kmccarty;

import hep.aida.ref.plotter.PlotterRegion;
import jas.hist.JASHist1DHistogramStyle;
import jas.hist.JASHist2DHistogramStyle;

import java.awt.Color;
import java.awt.Font;

public class PlotsFormatter {
	// Define plot fonts.
	public static final Font BASIC_FONT = new Font("Calibri", Font.PLAIN, 30);
	public static final Font AXIS_FONT  = new Font("Calibri", Font.BOLD,  35);
	public static final Font TITLE_FONT = new Font("Calibri", Font.BOLD,  45);
	
	// Defines the color style options for plot data.
	public static enum ColorStyle {
		 MS_BLUE(new Color( 79, 129, 189), new Color( 36,  64,  97)), MS_ORANGE(new Color(247, 150,  70), new Color(152,  72,   6)),
		  MS_RED(new Color(192,  80,  77), new Color( 99,  36,  35)),      GREY(new Color(166, 166, 166), new Color( 89,  89,  89)),
		MS_GREEN(new Color(155, 187,  89), new Color( 79,  98,  40)),   CRIMSON(new Color(161,   0,   0), new Color(104,   0,   0)),
		    RUST(new Color(161,  80,   0), new Color(105,  80,   0)),    YELLOW(new Color(161, 161,   0), new Color(122, 109,   8)),
		  FOREST(new Color( 65, 102,   0), new Color( 37,  79,   0)),     GREEN(new Color(  7, 132,  70), new Color(  7,  82,  30)),
		    TEAL(new Color(  0, 130, 130), new Color(  0,  90, 100)),  CERULEAN(new Color(  0,  86, 130), new Color(  0,  28,  83)),
		    BLUE(new Color(  0,  33, 203), new Color(  0,   0, 137)),    INDIGO(new Color( 68,  10, 127), new Color(  0,   0,  61)),
		  PURPLE(new Color(106,   0, 106), new Color( 63,   0,  56)),   FUSCHIA(new Color(119,   0,  60), new Color( 60,   0,  60));
		
		private final Color fillColor;
		private final Color lineColor;
		
		private ColorStyle(Color fillColor, Color lineColor) {
			this.fillColor = fillColor;
			this.lineColor = lineColor;
		}
		
		public Color getFillColor() { return fillColor; }
		
		public Color getLineColor() { return lineColor; }
	};
	
	/**
	 * Sets the plot display formatting for 1D plots.
	 * @param region - The plotter region to format.
	 * @param color - The data color settings to use.
	 */
	public static final void setDefault1DStyle(PlotterRegion region, ColorStyle[] color) {
		// Get the names of each plot on in the region.
		String[] dataNames = region.getAllDataNames();
		
		// Check whether this is an overlay plot. Overlay plots contain
		// more than one data name.
		boolean overlay = (dataNames.length > 1 ? true : false);
		
		// Iterate over each plot in the region.
		for(int i = 0; i < dataNames.length; i++) {
			// Set the overlay style if needed.
			if(overlay) {
				// Get the fill style for the current data type.
				JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(dataNames[i]).getStyle();
				
				// Set the histogram style to display thick-lined bars
				// with no fill. The color is set by the "color" argument.
				fillStyle.setHistogramFill(false);
				fillStyle.setHistogramBarLineWidth(3);
				fillStyle.setHistogramBarLineColor(color[i].getFillColor());
				
				// Set the legend text style.
				region.getPlot().getLegend().setFont(new Font("Calibri", Font.PLAIN, 20));
			}
			
			// Otherwise, set the fill style for a single plot.
			else {
				// Get the fill style for the current data type.
				JASHist1DHistogramStyle fillStyle = (JASHist1DHistogramStyle) region.getDataForName(dataNames[i]).getStyle();
				
				// Set the histogram style to display thick-lined bars
				// with a fill color. The colors are defined by the
				// "color" argument.
				fillStyle.setHistogramBarLineWidth(3);
				fillStyle.setHistogramBarColor(color[i].getFillColor());
				fillStyle.setHistogramBarLineColor(color[i].getLineColor());
			}
			
			// Set the statistics box style.
			region.getPlot().getStats().setVisible(true);
			region.getPlot().getStats().setFont(BASIC_FONT);
			
			// Set the title font.
			region.getPlot().getTitleObject().setFont(TITLE_FONT);
			
			// Set the axis tick-mark fonts.
			region.getPlot().getXAxis().setFont(BASIC_FONT);
			region.getPlot().getYAxis().setFont(BASIC_FONT);
			region.getPlot().getXAxis().getLabelObject().setFont(AXIS_FONT);
			region.getPlot().getYAxis().getLabelObject().setFont(AXIS_FONT);
		}
	}
	
	/**
	 * Sets the plot display formatting for 1D plots.
	 * @param region - The plotter region to format.
	 */
	public static final void setDefault2DStyle(PlotterRegion region, boolean logarithmic) {
		// Get the fill style object. 2D plots should never be overlay
		// plots, so there should only ever be one data name.
		JASHist2DHistogramStyle fillStyle = (JASHist2DHistogramStyle) region.getDataForName(region.getAllDataNames()[0]).getStyle();
		
		// Set the fill style for a two-dimensional plot.
		if(logarithmic) { fillStyle.setLogZ(true); }
		fillStyle.setHistStyle(JASHist2DHistogramStyle.STYLE_COLORMAP);
		fillStyle.setColorMapScheme(JASHist2DHistogramStyle.COLORMAP_RAINBOW);
		
		// Make the statistics box invisible.
		region.getPlot().getStats().setVisible(false);
		
		// Set the general plot font (which is also the z-axis font).
		region.getPlot().setFont(BASIC_FONT);
		
		// Set the title font.
		region.getPlot().getTitleObject().setFont(TITLE_FONT);
		
		// Set the axis tick-mark fonts.
		region.getPlot().getXAxis().setFont(BASIC_FONT);
		region.getPlot().getYAxis().setFont(BASIC_FONT);
		region.getPlot().getXAxis().getLabelObject().setFont(AXIS_FONT);
		region.getPlot().getYAxis().getLabelObject().setFont(AXIS_FONT);
	}
}