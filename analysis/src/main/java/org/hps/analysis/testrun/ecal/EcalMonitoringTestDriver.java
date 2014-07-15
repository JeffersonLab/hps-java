package org.hps.monitoring.drivers.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This is very simple Driver that plots the amplitude from RawCalorimeterHits
 * in the ECAL as a basic test that the monitoring system works.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class EcalMonitoringTestDriver extends Driver {
       
    static final String collectionName = "EcalReadoutHits";
    AIDA aida = AIDA.defaultInstance();
    IHistogram1D amplitudePlot;    
    
    public void startOfData() {
        amplitudePlot = aida.histogram1D("Amplitude", 300, 0., 30000);
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL Plots");
        IPlotter plotter = plotterFactory.create("Amplitude");
        plotter.createRegion();
        plotter.region(0).plot(amplitudePlot);
        plotter.show();
    }
    
    public void process(EventHeader event) {
        if (event.hasCollection(RawCalorimeterHit.class, collectionName)) {
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, collectionName);
            for (RawCalorimeterHit hit : hits) {
                amplitudePlot.fill(hit.getAmplitude());
            }        
        }
    }
}
