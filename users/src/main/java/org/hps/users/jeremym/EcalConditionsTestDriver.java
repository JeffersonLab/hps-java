package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;

import java.util.List;

import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.ECalUtils;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Test plots using ECAL conditions data such as per channel gain values.
 */
public class EcalConditionsTestDriver extends Driver {
        
    String collectionName = "EcalReadoutHits";
    EcalConditions ecalConditions = null;
    IIdentifierHelper helper = null;
    EcalChannelCollection channels = null;    
    IPlotter plotter;
    AIDA aida = AIDA.defaultInstance();
    
    IHistogram1D ecalRawEnergyPlot;
    IHistogram1D ecalCalibratedEnergyPlot;
    IHistogram1D ecalEnergyDiffPlot;
    IHistogram1D ecalTimePlot;
    
    
    double minRawEnergy = Double.MAX_VALUE;
    double maxRawEnergy = 0.0;
    double minCalibratedEnergy = Double.MAX_VALUE;
    double maxCalibratedEnergy = 0.0;
    double minTime = Double.MAX_VALUE;
    double maxTime = 0;    
    
    // Time window in ADC clocks which is 35*4 ns (from Sho).  
    static int ECAL_TIME_WINDOW = 35;
    
    public void detectorChanged(Detector detector) {
        System.out.println(this.getClass().getName() + ".detectorChanged");
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = ecalConditions.getChannelMap();
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
    }
         
    public void startOfData() {
        
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        ITree tree = analysisFactory.createTreeFactory().create();
        IHistogramFactory histogramFactory = analysisFactory.createHistogramFactory(tree);
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL Raw Hits");
        
        IPlotter plotter = plotterFactory.create("Raw Energy");
        ecalRawEnergyPlot = histogramFactory.createHistogram1D("Raw Hit Energy [GeV]", 60, 0., 15);
        plotter.createRegion();
        plotter.region(0).plot(ecalRawEnergyPlot);
        plotter.show();        
        
        plotter = plotterFactory.create("Calibrated Energy");
        ecalCalibratedEnergyPlot = histogramFactory.createHistogram1D("Calibrated Energy [GeV]", 60, -2, 13);
        plotter.createRegion();
        plotter.region(0).plot(ecalCalibratedEnergyPlot);
        plotter.show();
        
        plotter = plotterFactory.create("Time");
        ecalTimePlot = histogramFactory.createHistogram1D("Hit Time [ns]", 100, 0., 400);
        plotter.createRegion();
        plotter.region(0).plot(ecalTimePlot);
        plotter.show();
        
        plotter = plotterFactory.create("Raw minus Calibrated Energy");
        ecalEnergyDiffPlot = histogramFactory.createHistogram1D("Raw Minus Calibrated Energy [GeV]", 50, -2, 5);
        plotter.createRegion();
        plotter.region(0).plot(ecalEnergyDiffPlot);
        plotter.show();
    }
       
    public void process(EventHeader event) {
        if (event.hasCollection(RawCalorimeterHit.class, collectionName)) {
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, collectionName);
            for (RawCalorimeterHit hit : hits) {
                
                // Get conditions for channel.
                EcalChannelConstants channelConstants = findChannelConstants(hit.getCellID());
                double gain = channelConstants.getGain().getGain();
                double pedestal = channelConstants.getCalibration().getPedestal();                
                
                // Plot raw energy.
                double rawEnergy = calculateRawEnergy(hit, gain);
                if (rawEnergy > this.maxRawEnergy)
                    maxRawEnergy = rawEnergy;
                if (rawEnergy < this.minRawEnergy)
                    minRawEnergy = rawEnergy;
                //System.out.println("energy: " + energy);
                
                ecalRawEnergyPlot.fill(rawEnergy);
                
                // Plot calibrated energy.
                double calibratedEnergy = calculateCalibratedEnergy(hit, gain, pedestal, ECAL_TIME_WINDOW);
                if (calibratedEnergy > this.maxCalibratedEnergy)
                    maxCalibratedEnergy = calibratedEnergy;
                if (calibratedEnergy < this.minCalibratedEnergy)
                    minCalibratedEnergy = calibratedEnergy;
                //System.out.println("calibrated energy: " + calibratedEnergy);
                ecalCalibratedEnergyPlot.fill(calibratedEnergy);
                
                // Plot raw minus calibrated energy.
                ecalEnergyDiffPlot.fill(rawEnergy - calibratedEnergy);
                
                // Plot time.
                //System.out.println("timestamp: " + hit.getTimeStamp());
                double time = calculateTime(hit);
                if (time > this.maxTime)
                    maxTime = time;
                if (time < this.minTime)
                    minTime = time;
                //System.out.println("time: " + time);
                ecalTimePlot.fill(time);
                                
                //System.out.println();
            }
        }
    }    

    // Calculate energy of a raw ECAL hit only using gain and ADC.
    private double calculateRawEnergy(RawCalorimeterHit hit, double gain) {
        return hit.getAmplitude() * gain * ECalUtils.MeV;
    }
  
    // Calculate calibrated energy of a raw ECAL hit, applying pedestal.
    private double calculateCalibratedEnergy(RawCalorimeterHit hit, double gain, double pedestal, int window) {
        double adcSum = hit.getAmplitude() - window * pedestal;
        double energy = gain * adcSum * ECalUtils.MeV;
        return energy;
    }
    
    // Copied and modified from EcalRawConverter in ecal-recon.
    private double calculateTime(RawCalorimeterHit hit) {
        if (hit.getTimeStamp() % 64 != 0) {
            throw new RuntimeException("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = ((double)hit.getTimeStamp()) / 16.0;
        return time;
    }    
   
    // Find ECAL channel constants from hit ID.
    EcalChannelConstants findChannelConstants(long rawId) {
        IIdentifier id = new Identifier(rawId);
        int x = helper.getValue(id, "ix");
        int y = helper.getValue(id, "iy");
        GeometryId geometryId = new GeometryId();
        geometryId.x = x;
        geometryId.y = y;
        EcalChannel channel = channels.findChannel(geometryId);
        return ecalConditions.getChannelConstants(channel);
    }        
    
    public void endOfData() {
        this.ecalConditions = null;
        System.out.println("minRawEnergy: " + this.minRawEnergy);
        System.out.println("maxRawEnergy: " + this.maxRawEnergy);
        System.out.println("minCalibratedEnergy: " + this.minCalibratedEnergy);
        System.out.println("maxCalibratedEnergy: " + this.maxCalibratedEnergy);
        System.out.println("minTime: " + this.minTime);
        System.out.println("maxTime: " + this.maxTime);
    }
}
