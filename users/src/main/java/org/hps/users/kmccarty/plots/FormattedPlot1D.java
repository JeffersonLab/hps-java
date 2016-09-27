package org.hps.users.kmccarty.plots;

import java.util.HashSet;
import java.util.Set;

import org.hps.users.kmccarty.plots.PlotsFormatter.ColorStyle;
import org.hps.users.kmccarty.plots.PlotsFormatter.DisplayStyle;

import hep.aida.IHistogram1D;

public class FormattedPlot1D extends FormattedPlot {
	private final double axisMin;
	private final double axisMax;
	private final ColorStyle[] styles;
	private final IHistogram1D[] plots;
	private final DisplayStyle dispStyle;
	
	public FormattedPlot1D(IHistogram1D plot, ColorStyle style, String xAxis, String yAxis, String plotName) {
		this(new IHistogram1D[] { plot }, new ColorStyle[] { style }, new String[] { plotName }, xAxis, yAxis, DisplayStyle.BAR, plotName);
	}
	
	public FormattedPlot1D(IHistogram1D plot, ColorStyle style, String xAxis, String yAxis, DisplayStyle dispStyle, String plotName) {
		this(new IHistogram1D[] { plot }, new ColorStyle[] { style }, new String[] { plotName }, xAxis, yAxis, dispStyle, plotName);
	}
	
	public FormattedPlot1D(IHistogram1D plots[], ColorStyle[] styles, String[] entryNames,
			String xAxis, String yAxis, DisplayStyle dispStyle, String plotName) {
		this(plots, styles, entryNames, xAxis, yAxis, dispStyle, plotName, -1, -1);
	}
	
	public FormattedPlot1D(IHistogram1D plot, ColorStyle style, String xAxis, String yAxis, String plotName, double axisMin, double axisMax) {
		this(new IHistogram1D[] { plot }, new ColorStyle[] { style }, new String[] { plotName },
				xAxis, yAxis, DisplayStyle.BAR, plotName, axisMin, axisMax);
	}
	
	public FormattedPlot1D(IHistogram1D plot, ColorStyle style, String xAxis, String yAxis,
			DisplayStyle dispStyle, String plotName, double axisMin, double axisMax) {
		this(new IHistogram1D[] { plot }, new ColorStyle[] { style }, new String[] { plotName },
				xAxis, yAxis, dispStyle, plotName, axisMin, axisMax);
	}
	
	public FormattedPlot1D(IHistogram1D plots[], ColorStyle[] styles, String[] entryNames, String xAxis,
			String yAxis, DisplayStyle dispStyle, String plotName, double axisMin, double axisMax) {
		// Initialize the object.
		super(xAxis, yAxis, plotName);
		this.plots = plots;
		this.styles = styles;
		this.axisMin = axisMin;
		this.axisMax = axisMax;
		if(dispStyle == null) { this.dispStyle = DisplayStyle.BAR; }
		else { this.dispStyle = dispStyle; }
		
		// Verify that the plot array is valid for a compound plot.
		if(!verifyPlots(plots)) {
			throw new IllegalArgumentException("Plots array invalid; plots must have the same bins!");
		}
		
		// Verify that all color styles are defined and are equal in
		// number to the plots.
		if(styles == null || styles.length != plots.length) {
			throw new IllegalArgumentException("Color style array invalid; a number of color styles equal to plots must be defined.");
		}
		for(int i = 0; i < styles.length; i++) {
			if(styles[i] == null) {
				throw new IllegalArgumentException("Color style array invalid; all color styles must be defined.");
			}
		}
		
		// Set the names of each individual entry plot.
		if(entryNames == null || entryNames.length != plots.length) {
			throw new IllegalArgumentException("When defining multiple plots, a unique entry name must be defined for each.");
		}
		Set<String> entryNameSet = new HashSet<String>(entryNames.length);
		for(int i = 0; i < plots.length; i++) {
			if(entryNames[i] == null || entryNameSet.contains(entryNames[i])) {
				throw new IllegalArgumentException("When defining multiple plots, a unique entry name must be defined for each.");
			}
			entryNameSet.add(entryNames[i]);
			plots[i].setTitle(entryNames[i]);
		}
	}
	
	public IHistogram1D[] getPlots() {
		return plots;
	}
	
	public ColorStyle[] getColorStyle() {
		return styles;
	}
	
	public boolean definesAxisRange() {
		return axisMax != -1;
	}
	
	public double getAxisMin() {
		return axisMin;
	}
	
	public double getAxisMax() {
		return axisMax;
	}
	
	public DisplayStyle getDisplayStyle() {
		return dispStyle;
	}
	
	private static final boolean verifyPlots(IHistogram1D[] plots) {
		// The plot file must be defined.
		if(plots == null) { return false; }
		
		// If there is only one plot, it passes automatically.
		if(plots.length == 1) { return true; }
		
		// All plots must have the same dimensions and bin values.
		int baseBins = plots[0].axis().bins();
		for(IHistogram1D plot : plots) {
			// Check that the plot has the same number of bins.
			if(plot.axis().bins() != baseBins) { return false; }
		}
		
		// Check that each bin in the plots has the same value.
		for(int bin = 0; bin < baseBins; bin++) {
			// Get the value of the first bin in the base plot.
			double baseBinValue = plots[0].axis().binLowerEdge(0);
			
			// Check that all other plots are the same.
			for(IHistogram1D plot : plots) {
				// Check that the plot has the same number of bins.
				if(plot.axis().binLowerEdge(0) != baseBinValue) { return false; }
			}
		}
		
		// Otherwise, the plots are the same.
		return true;
	}
}
