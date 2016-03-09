package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import org.hps.monitoring.ecal.eventdisplay.ui.PDataEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.Viewer;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalEvent;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalListener;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/*
 * Display histograms created by org.hps.recon.ecal.EcalPedestalCalculator
 * 
 * When user clicks on crystal in Kyle's event viewer, the corresponding channel's
 * pedestal histogram is drawn.
 * 
 * @version $Id: EcalPedestalViewer.java,v 0.1 2015/02/20 00:00:00
 * @author <baltzell@jlab.org>
 */
public class EcalPedestalViewer extends Driver implements CrystalListener, ActionListener {

    // this has to match the one in EcalPedstalCalculator:
    private String histoNameFormat = "Ecal/Pedestals/Mode7/ped%03d";
    
    private AIDA aida = AIDA.defaultInstance(); 
    private IPlotter plotter;
    private IPlotterFactory plotterFactory;
    private IPlotterStyle pstyle;
    private PEventViewer viewer;

    static final String[] colors={"red","black","blue","green","yellow","pink","cyan","magenta","brown"};
    static final int nRows=3;
    static final int nColumns=3;
    private int theRegion=0;
    
    @Override
    public void detectorChanged(Detector detector) {
        plotterFactory = aida.analysisFactory().createPlotterFactory("ECal Peds");
        plotter = plotterFactory.create("ECal Peds");
        plotter.createRegions(nColumns,nRows);
        // Plot dummmy histos, else null plotter regions later:
        for (int ii=0; ii<nColumns*nRows; ii++) {
            plotter.region(ii).plot(aida.histogram1D("ASDF"+ii,100,11e9,11e11));
        }
        plotter.show();
        
        pstyle=plotterFactory.createPlotterStyle();
        pstyle.xAxisStyle().labelStyle().setBold(true);
        pstyle.yAxisStyle().labelStyle().setBold(true);
        pstyle.xAxisStyle().tickLabelStyle().setBold(true);
        pstyle.yAxisStyle().tickLabelStyle().setBold(true);
        pstyle.xAxisStyle().lineStyle().setColor("black");
        pstyle.yAxisStyle().lineStyle().setColor("black");
        pstyle.xAxisStyle().lineStyle().setThickness(2);
        pstyle.yAxisStyle().lineStyle().setThickness(2);
        pstyle.dataStyle().errorBarStyle().setThickness(0);
        pstyle.legendBoxStyle().setVisible(false);
    }
    
    @Override
    public void startOfData() {
        File config = new File("ecal-mapping-config.csv");
        if(config.exists() && config.canRead()) {
            try { viewer = new PDataEventViewer(config.getAbsolutePath()); }
            catch (IOException e) { viewer = new PEventViewer(); }
        } else { viewer = new PEventViewer(); }
        viewer.addCrystalListener(this);
        viewer.setVisible(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) { }
    
    @Override
    public void crystalActivated(CrystalEvent e) { }
    
    @Override
    public void crystalDeactivated(CrystalEvent e) { }

    @Override
    public void crystalClicked(CrystalEvent e) {
        aida.tree().cd("/");
        Point ecalPoint = Viewer.toEcalPoint(e.getCrystalID());
        if (ecalPoint.x == 0 || ecalPoint.y == 0) return;
        if (EcalMonitoringUtilities.isInHole(ecalPoint.y,ecalPoint.x)) return;
        final int cid=EcalMonitoringUtilities.getChannelIdFromRowColumn(ecalPoint.y,ecalPoint.x);
        IHistogram1D hist=aida.histogram1D(String.format(histoNameFormat,cid));
        if (hist==null) {
            System.err.println("Running the Driver?");
        } else {
            hist.setTitle(String.format("(%d,%d)",ecalPoint.x,ecalPoint.y));
            pstyle.dataStyle().lineStyle().setParameter("color", colors[theRegion%colors.length]);
            plotter.region(theRegion).clear();
            plotter.region(theRegion).plot(hist,pstyle);
            plotter.region(theRegion).refresh();
            theRegion=(theRegion+1)%(nColumns*nRows);
        }
    }
    
    
}
