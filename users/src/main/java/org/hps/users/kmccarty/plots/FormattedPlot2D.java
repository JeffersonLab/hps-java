package org.hps.users.kmccarty.plots;

import hep.aida.IHistogram2D;

public class FormattedPlot2D extends FormattedPlot {
	private final IHistogram2D plot;
	private final boolean logarithmic;
	
	public FormattedPlot2D(IHistogram2D plot, boolean logarithmic, String xAxis, String yAxis, String plotName) {
		super(xAxis, yAxis, plotName);
		this.plot = plot;
		this.logarithmic = logarithmic;
	}
	
	public IHistogram2D getPlot() {
		return plot;
	}
	
	public boolean isLogarithmic() {
		return logarithmic;
	}
}