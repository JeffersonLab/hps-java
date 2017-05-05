package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.HashSet;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver plots the number of hits per event with at least one ADC value at or above 1 to 6 sigma.
 */
// TODO: Add plot of hist.fill(nSigmaHits / nTotalHits) to get percentage by event.  (X axis = 0 - 1.0 w/ 100 bins, Y axis = nEvents)
public class EcalADCThresholdPlotsDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
    static int N_CRYSTALS = 442;

    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();

    @Override
    public void detectorChanged(Detector detector) {
        
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();        
        channels = conditions.getChannelCollection();                                       
    }
    
    @Override
    public void startOfData() {
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("ECAL ADC Threshold Cuts");
        IPlotter plotter = plotterFactory.create("ECAL ADC Threshold Cuts");
        plotter.createRegion();
        
        IPlotterStyle style = plotterFactory.createPlotterStyle();
        style.dataStyle().lineStyle().setVisible(true);
        style.dataStyle().fillStyle().setVisible(false);
        style.legendBoxStyle().setVisible(true);
        style.yAxisStyle().setScaling("log");
        
        style.dataStyle().lineStyle().setColor("blue");
        plotter.region(0).plot(aida.histogram1D("Hits Over 1 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        style.dataStyle().lineStyle().setColor("red");
        plotter.region(0).plot(aida.histogram1D("Hits Over 2 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        style.dataStyle().lineStyle().setColor("green");
        plotter.region(0).plot(aida.histogram1D("Hits Over 3 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        style.dataStyle().lineStyle().setColor("purple");
        plotter.region(0).plot(aida.histogram1D("Hits Over 4 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        style.dataStyle().lineStyle().setColor("grey");
        plotter.region(0).plot(aida.histogram1D("Hits Over 5 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        style.dataStyle().lineStyle().setColor("yellow");
        plotter.region(0).plot(aida.histogram1D("Hits Over 6 Sigma", N_CRYSTALS, 0, N_CRYSTALS), style);
        
        plotter.show();
    }
    
    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            HashSet<RawTrackerHit> sigma1Hits = new HashSet<RawTrackerHit>();
            HashSet<RawTrackerHit> sigma2Hits = new HashSet<RawTrackerHit>();
            HashSet<RawTrackerHit> sigma3Hits = new HashSet<RawTrackerHit>();
            HashSet<RawTrackerHit> sigma4Hits = new HashSet<RawTrackerHit>();
            HashSet<RawTrackerHit> sigma5Hits = new HashSet<RawTrackerHit>();
            HashSet<RawTrackerHit> sigma6Hits = new HashSet<RawTrackerHit>();
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                double pedestal = conditions.getChannelConstants(channel).getCalibration().getPedestal();
                double noise = conditions.getChannelConstants(channel).getCalibration().getNoise();
                for (short adcValue : hit.getADCValues()) {
                    if ((double)adcValue >= (pedestal + noise)) {
                        sigma1Hits.add(hit);
                    }
                    if ((double)adcValue >= (pedestal + noise * 2)) {
                        sigma2Hits.add(hit);
                    }                    
                    if ((double)adcValue >= (pedestal + noise * 3)) {
                        sigma3Hits.add(hit);
                    } 
                    if ((double)adcValue >= (pedestal + noise * 4)) {
                        sigma4Hits.add(hit);
                    } 
                    if ((double)adcValue >= (pedestal + noise * 5)) {
                        sigma5Hits.add(hit);
                    }
                    if ((double)adcValue >= (pedestal + noise * 6)) {
                        sigma6Hits.add(hit);
                    }
                }                
            }
            if (sigma1Hits.size() > 0)
                aida.histogram1D("Hits Over 1 Sigma").fill(sigma1Hits.size());
            if (sigma2Hits.size() > 0)
                aida.histogram1D("Hits Over 2 Sigma").fill(sigma2Hits.size());
            if (sigma3Hits.size() > 0)
                aida.histogram1D("Hits Over 3 Sigma").fill(sigma3Hits.size());
            if (sigma4Hits.size() > 0)
                aida.histogram1D("Hits Over 4 Sigma").fill(sigma4Hits.size());
            if (sigma5Hits.size() > 0)
                aida.histogram1D("Hits Over 5 Sigma").fill(sigma5Hits.size());
            if (sigma6Hits.size() > 0)
                aida.histogram1D("Hits Over 6 Sigma").fill(sigma6Hits.size());
        } 
    }
}