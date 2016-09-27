package org.hps.users.kmccarty.plots;

import hep.aida.IHistogram2D;

public class FormattedPlot2D extends FormattedPlot {
	private final IHistogram2D plot;
	private final boolean logarithmic;
	private final double xAxisRange;
	private final double yAxisRange;
	
	public FormattedPlot2D(IHistogram2D plot, boolean logarithmic, String xAxis, String yAxis, String plotName) {
		super(xAxis, yAxis, plotName);
		this.plot = plot;
		this.xAxisRange = -1;
		this.yAxisRange = -1;
		this.logarithmic = logarithmic;
	}
	
	public FormattedPlot2D(IHistogram2D plot, boolean logarithmic, String xAxis, String yAxis, String plotName, double xAxisRange, double yAxisRange) {
		super(xAxis, yAxis, plotName);
		this.plot = plot;
		this.xAxisRange = xAxisRange;
		this.yAxisRange = yAxisRange;
		this.logarithmic = logarithmic;
	}
	
	public IHistogram2D getPlot() {
		return plot;
	}
	
	public boolean isLogarithmic() {
		return logarithmic;
	}
	
	public boolean definesXAxisRange() {
		return xAxisRange != -1;
	}
	
	public boolean definesYAxisRange() {
		return yAxisRange != -1;
	}
	
	public double getXAxisRange() {
		return xAxisRange;
	}
	
	public double getYAxisRange() {
		return yAxisRange;
	}
}