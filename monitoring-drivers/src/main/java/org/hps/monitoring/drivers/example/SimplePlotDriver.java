package org.hps.monitoring.drivers.example;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SimplePlotDriver extends Driver {
    
    static String ecalCollectionName = "EcalReadoutHits";
    static String svtCollectionName = "SVTRawTrackerHits";
    
    AIDA aida = AIDA.defaultInstance();
    IHistogram1D svtHitsPlot;
    IHistogram1D ecalHitsPlot;
    IHistogram1D ecalEnergyPlot;
    
    public void startOfData() {
        ecalHitsPlot = aida.histogram1D("ECAL Hits per Event", 20, 0., 20.);
        svtHitsPlot = aida.histogram1D("SVT Hits per Event", 200, 0., 200.);
        
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("Monitoring Test Plots");
        
        IPlotter plotter = plotterFactory.create("ECAL");
        plotter.createRegion();
        plotter.region(0).plot(ecalHitsPlot);
        plotter.show();
        
        plotter = plotterFactory.create("SVT");
        plotter.createRegion();
        plotter.region(0).plot(svtHitsPlot);
        plotter.show();
    }
    
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, svtCollectionName))            
            svtHitsPlot.fill(event.get(RawTrackerHit.class, svtCollectionName).size());
        if (event.hasCollection(RawCalorimeterHit.class,  ecalCollectionName))
            ecalHitsPlot.fill(event.get(RawCalorimeterHit.class, ecalCollectionName).size());
    }
}