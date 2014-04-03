package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.ICloud1D;
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
    ICloud1D ecalEnergyPlot;
    double maxEnergy = 0.0;
    
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
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL Conditions Test");
        IPlotter plotter = plotterFactory.create("ECAL Hits");
        ecalEnergyPlot = histogramFactory.createCloud1D("ADC * Gain [GeV]");        
        plotter.createRegion();
        plotter.region(0).plot(ecalEnergyPlot);
        plotter.show();        
    }
       
    public void process(EventHeader event) {
        if (event.hasCollection(RawCalorimeterHit.class, collectionName)) {
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, collectionName);
            for (RawCalorimeterHit hit : hits) {
                EcalChannelConstants channelConstants = findChannelConstants(hit.getCellID());
                double gain = channelConstants.getGain().getGain();
                double energy = this.calculateEnergy(hit, gain);
                ecalEnergyPlot.fill(energy);
            }
        }
    }    
    
    // Get energy in MeV from ADC counts and gain.
    private double calculateEnergy(RawCalorimeterHit hit, double gain) {
        int adc = hit.getAmplitude();
        return gain * adc * ECalUtils.MeV;
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
}
