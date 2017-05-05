package org.hps.monitoring.ecal.plots;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;

/**
 * Basic ECal monitoring plots that work on Test Run data.
 */
public class BasicMonitoringPlotsDriver extends Driver {
    
    private String calHitsCollectionName = "EcalCalHits"; 
    private String rawHitsCollectionName = "EcalReadoutHits";
    private String clustersCollectionName = "EcalClusters";
    
    private IHistogram1D calHitEnergyH1D;
    private IHistogram1D clusterEnergyH1D;
    private IHistogram1D rawHitAmplitudeH1D;
    private IHistogram2D calHitEnergyMapH2D;
    
    public BasicMonitoringPlotsDriver() {       
    }
    
    public void startOfData() {     
        
        IAnalysisFactory.create().createHistogramFactory(null);
        IPlotterFactory plotFactory = IAnalysisFactory.create().createPlotterFactory("ECAL Monitoring");
        IHistogramFactory histogramFactory = IAnalysisFactory.create().createHistogramFactory(null);
        
        calHitEnergyH1D = histogramFactory.createHistogram1D(calHitsCollectionName + ": Energy", calHitsCollectionName + ": Energy", 200, 0.0, 2.0);
        calHitEnergyH1D.annotation().addItem("xAxisLabel", "GeV");
        calHitEnergyH1D.annotation().addItem("yAxisLabel", "Count");
        IPlotter plotter = plotFactory.create("CalorimeterHits");
        plotter.createRegion();
        plotter.style().gridStyle().setVisible(false);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.region(0).plot(calHitEnergyH1D);
        plotter.show();
        
        rawHitAmplitudeH1D = histogramFactory.createHistogram1D(rawHitsCollectionName + ": Amplitude", rawHitsCollectionName + ": Amplitude", 150, 0.0, 15000.0);
        rawHitAmplitudeH1D.annotation().addItem("xAxisLabel", "ADC Value");
        rawHitAmplitudeH1D.annotation().addItem("yAxisLabel", "Count");
        plotter = plotFactory.create("RawCalorimeterHits");
        plotter.createRegion();
        plotter.style().gridStyle().setVisible(false);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.region(0).plot(rawHitAmplitudeH1D);
        plotter.show();
        
        clusterEnergyH1D = histogramFactory.createHistogram1D(clustersCollectionName + ": Energy", clustersCollectionName + ": Energy", 100, 0.0, 3.0);
        clusterEnergyH1D.annotation().addItem("xAxisLabel", "GeV");
        clusterEnergyH1D.annotation().addItem("yAxisLabel", "Count");
        plotter = plotFactory.create("Clusters");
        plotter.createRegion();
        plotter.style().gridStyle().setVisible(false);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.region(0).plot(clusterEnergyH1D);
        plotter.show();
        
        calHitEnergyMapH2D = histogramFactory.createHistogram2D(calHitsCollectionName + ": Energy Map", calHitsCollectionName + ": Energy Map", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter = plotFactory.create("CalorimeterHit Energy Map");
        plotter.createRegion();
        plotter.style().setParameter("hist2DStyle", "colorMap");
        plotter.style().gridStyle().setVisible(false);
        plotter.region(0).plot(calHitEnergyMapH2D);
        plotter.show();
    }
    
    public void process(EventHeader event) {
        
        if (event.hasCollection(CalorimeterHit.class, calHitsCollectionName)) {
            for (CalorimeterHit hit : event.get(CalorimeterHit.class, calHitsCollectionName)) {
                calHitEnergyH1D.fill(hit.getCorrectedEnergy());
                calHitEnergyMapH2D.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getCorrectedEnergy());
            }
        }
        
        if (event.hasCollection(Cluster.class, clustersCollectionName)) {
            for (Cluster cluster : event.get(Cluster.class, clustersCollectionName)) {
                clusterEnergyH1D.fill(cluster.getEnergy());
            }
        }
        
        if (event.hasCollection(RawCalorimeterHit.class, rawHitsCollectionName)) {
            for (RawCalorimeterHit hit : event.get(RawCalorimeterHit.class, rawHitsCollectionName)) {
                rawHitAmplitudeH1D.fill(hit.getAmplitude());
            }
        }
    }
}