package org.hps.users.kmccarty.plots;

import org.hps.users.kmccarty.PlotsFormatter.ColorStyle;

import hep.aida.IHistogram1D;

public class FormattedPlot1D extends FormattedPlot {
	private final ColorStyle style;
	private final IHistogram1D plot;
	
	public FormattedPlot1D(IHistogram1D plot, ColorStyle style, String xAxis, String yAxis, String plotName) {
		super(xAxis, yAxis, plotName);
		this.plot = plot;
		this.style = style;
	}
	
	public IHistogram1D getPlot() {
		return plot;
	}
	
	public ColorStyle getColorStyle() {
		return style;
	}
}
