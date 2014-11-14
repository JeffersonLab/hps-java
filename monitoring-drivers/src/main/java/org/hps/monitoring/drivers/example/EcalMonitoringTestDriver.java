package org.hps.monitoring.drivers.example;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;

/**
 * This Driver just plots the number of RawTrackerHit objects in the 'EcalReadoutHits'
 * collection and the FADC values as a basic test that ECAL raw data is accessible to 
 * the monitoring app.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalMonitoringTestDriver extends Driver {
    
    static IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);    
    IHistogram1D nHitsH1D = histogramFactory.createHistogram1D("EcalReadoutHits : Number of Hits", 443, 0, 443.);   
    IHistogram1D adcValuesH1D = histogramFactory.createHistogram1D("EcalReadoutHits : ADC Values", 300, 0, 300.);
        
    public void startOfData() {        
        IPlotter plotter = IAnalysisFactory.create().createPlotterFactory("ECAL Test").create("ECAL Test");
        plotter.createRegions(2);
        plotter.region(0).plot(nHitsH1D);
        plotter.region(1).plot(adcValuesH1D);
        plotter.show();
    }
    
    public void process(EventHeader event) {               
        if (event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            System.out.println("EcalReadoutHits has " + hits.size() + " hits");
            nHitsH1D.fill(hits.size());
            for (RawTrackerHit hit : hits) {
                for (short adcValue : hit.getADCValues()) {
                    adcValuesH1D.fill(adcValue);
                }
            }
        } else {
            // For testing purposes, consider it a fatal error if collection is missing.
            throw new RuntimeException("No RawTrackerHit collection was found with name 'EcalReadoutHits'.");
        }
    }
}
