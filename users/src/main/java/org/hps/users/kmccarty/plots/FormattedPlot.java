package org.hps.users.kmccarty.plots;

public abstract class FormattedPlot {
    private final String xAxis;
    private final String yAxis;
    private final String plotName;
    
    public FormattedPlot(String xAxis, String yAxis, String plotName) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.plotName = plotName;
    }
    
    public String getPlotName() {
        return plotName;
    }
    
    public String getXAxisName() {
        return xAxis;
    }
    
    public String getYAxisName() {
        return yAxis;
    }
}