package org.hps.monitoring.drivers.svt;

import java.awt.event.MouseEvent;

import hep.aida.jfree.plotter.ChartPanelMouseListener;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterFactory;
import hep.aida.jfree.AnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IPlotterStyle;

/**
 * A MouseListener used to pop up a separate window with a plotter in it when 
 * a region is clicked.  The histogram that is plotted on the region clicked
 * will also be plotted in the newly created plotter. 
 */
public class PopupPlotterListener extends ChartPanelMouseListener {

    private PlotterRegion plotterRegion = null; 
    private static Plotter plotter = null; 
    IHistogram histogram;
    PlotterFactory plotterFactory = (PlotterFactory) AnalysisFactory.create().createPlotterFactory(); 
    
    /**
     *  Constructor
     * 
     *  @param plotterRegion : The plotter region that has been selected
     */
    public PopupPlotterListener(PlotterRegion plotterRegion) {
        super(plotterRegion);
        this.plotterRegion = plotterRegion;
    }

    /**
     * 
     */
    @Override
    public void mouseClicked(MouseEvent e) {
     
            if (plotter == null) {
                plotter = (Plotter) plotterFactory.create();
                plotter.createRegion(0);
            } else {  
                ((PlotterRegion) plotter.region(0)).clear();
            }
            
            histogram = ((IHistogram) plotterRegion.getPlottedObjects().get(0));
            plotter.region(0).plot(histogram, this.createStyle());
            plotter.show();
    }
    
    /**
     *  Create a plotter style.
     * 
     * @return plotter style
     */
    IPlotterStyle createStyle() {
        
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);
        style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
        style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        return style;
    }
}
