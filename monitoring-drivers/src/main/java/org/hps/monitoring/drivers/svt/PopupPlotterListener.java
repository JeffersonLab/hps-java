package org.hps.monitoring.drivers.svt;

import java.awt.event.MouseEvent;

import hep.aida.jfree.plotter.ChartPanelMouseListener;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterFactory;
import hep.aida.jfree.AnalysisFactory;

import hep.aida.IHistogram;

/**
 *  A MouseListener used to pop up a separate window with a plotter in it when 
 *  a region is clicked.  The histogram that is plotted on the region clicked
 *  will also be plotted in the newly created plotter. 
 * 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class PopupPlotterListener extends ChartPanelMouseListener {

    PlotterRegion plotterRegion; 
    Plotter plotter = null; 
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
       
            histogram = ((IHistogram) plotterRegion.getPlottedObjects().get(0));
            plotter = (Plotter) plotterFactory.create(); 
            plotter.createRegion();
            plotter.region(0).setStyle(plotterRegion.style());
            plotter.region(0).plot(histogram);
            plotter.show();
    }
}
