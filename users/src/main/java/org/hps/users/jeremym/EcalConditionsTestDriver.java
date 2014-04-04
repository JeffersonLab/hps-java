package org.hps.users.jeremym;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.ArrayList;
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
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsTestDriver extends Driver {
        
    // Collection info
    String collectionName = "EcalReadoutHits";
    IIdentifierHelper helper = null;    
    
    // ECAL conditions objects
    EcalConditions ecalConditions = null;
    EcalChannelCollection channels = null;    
    
    // AIDA stuff
    AIDA aida = AIDA.defaultInstance();       
    List<IPlotter> plotters;
    IHistogram1D ecalRawEnergyPlot;
    IHistogram1D ecalCalibratedEnergyPlot;
    IHistogram1D ecalEnergyDiffPlot;
    IHistogram1D ecalTimePlot;
        
    // Job summary variables
    double minRawEnergy = Double.MAX_VALUE;
    double maxRawEnergy = 0.0;
    double minCalibratedEnergy = Double.MAX_VALUE;
    double maxCalibratedEnergy = 0.0;
    double minTime = Double.MAX_VALUE;
    double maxTime = 0;
    
    // Time window in ADC clocks which equates to 35*4 ns (from Sho)
    static int ECAL_TIME_WINDOW = 35;
    
    public void detectorChanged(Detector detector) {
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = ecalConditions.getChannelMap();
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
    }
         
    public void startOfData() {
        
        plotters = new ArrayList<IPlotter>();
        
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL Raw Hits");
        
        IPlotter plotter = plotterFactory.create("Raw Energy");
        plotters.add(plotter);
        ecalRawEnergyPlot = aida.histogram1D("Raw Hit Energy [GeV]", 180, 0., 15);
        plotter.createRegion();
        plotter.region(0).plot(ecalRawEnergyPlot);
        plotter.show();        
        
        plotter = plotterFactory.create("Calibrated Energy");
        plotters.add(plotter);
        ecalCalibratedEnergyPlot = aida.histogram1D("Calibrated Energy [GeV]", 180, -2, 13);
        plotter.createRegion();
        plotter.region(0).plot(ecalCalibratedEnergyPlot);
        plotter.show();
        
        plotter = plotterFactory.create("Time");
        plotters.add(plotter);
        ecalTimePlot = aida.histogram1D("Hit Time [ns]", 200, 0., 400);
        plotter.createRegion();
        plotter.region(0).plot(ecalTimePlot);
        plotter.show();
        
        plotter = plotterFactory.create("Raw minus Calibrated Energy");
        plotters.add(plotter);
        ecalEnergyDiffPlot = aida.histogram1D("Raw Minus Calibrated Energy [GeV]", 150, -2, 5);
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
                
                //System.out.println("gain: " + gain);
                //System.out.println("pedestal: " + pedestal);
                
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

    // Calculate energy of a raw ECAL hit, only using gain and ADC.
    private double calculateRawEnergy(RawCalorimeterHit hit, double gain) {
        return hit.getAmplitude() * gain * ECalUtils.MeV;
    }
  
    // Calculate calibrated energy of a raw ECAL hit, applying pedestal.
    private double calculateCalibratedEnergy(RawCalorimeterHit hit, double gain, double pedestal, int window) {
        double adcSum = hit.getAmplitude() - window * pedestal;
        double energy = gain * adcSum * ECalUtils.MeV;
        return energy;
    }
    
    // Calculate the hit time.
    private double calculateTime(RawCalorimeterHit hit) {
        if (hit.getTimeStamp() % 64 != 0) {
            throw new RuntimeException("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = ((double)hit.getTimeStamp()) / 16.0;
        return time;
    }    
   
    // Find the ECAL channel conditions constants from a hit ID.
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
        this.channels = null;
        System.out.println("------- Job Summary -------");
        System.out.println("minRawEnergy: " + this.minRawEnergy);
        System.out.println("maxRawEnergy: " + this.maxRawEnergy);
        System.out.println("minCalibratedEnergy: " + this.minCalibratedEnergy);
        System.out.println("maxCalibratedEnergy: " + this.maxCalibratedEnergy);
        System.out.println("minTime: " + this.minTime);
        System.out.println("maxTime: " + this.maxTime);
        System.out.println("------------------------");
        
        // The plotters must be hidden or the job hangs at the end!
        for (IPlotter plotter : plotters) {
            System.out.println("destroying plotter " + plotter.title());
            plotter.hide();
        }        
    }
}
