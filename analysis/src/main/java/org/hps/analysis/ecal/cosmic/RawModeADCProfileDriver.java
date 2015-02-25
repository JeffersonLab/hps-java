package org.hps.analysis.ecal.cosmic;

import hep.aida.IAnalysisFactory;
import hep.aida.IProfile1D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * This Driver makes profile plots of the raw mode ADC data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class RawModeADCProfileDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
    Map<EcalChannel, IProfile1D> adcProfiles = new HashMap<EcalChannel, IProfile1D>();       
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();    
    String inputHitsCollectionName = "EcalReadoutHits";
           
    public void setInputHitsCollectionName(String inputHitsCollectionName) {
        this.inputHitsCollectionName = inputHitsCollectionName;
    }
        
    public void detectorChanged(Detector detector) {
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {            
            // Create ADC profile histogram, assuming ADC sample values of 0 to 99, with profile range -0.5 to 99.5, so bins are centered.
            adcProfiles.put(channel, aida.profile1D(inputHitsCollectionName + "/ADC Values : " + String.format("%03d", channel.getChannelId()), 100, -0.5, 99.5));
        }
    }

    public void process(EventHeader event) {                       
        if (event.hasCollection(RawTrackerHit.class, inputHitsCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputHitsCollectionName);
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());                
                if (channel != null) {                    
                    IProfile1D profile = adcProfiles.get(channel);                                        
                    for (int adcIndex = 0; adcIndex < hit.getADCValues().length; adcIndex++) {
                        // Fill the Profile1D with ADC value.
                        profile.fill(adcIndex, hit.getADCValues()[adcIndex]);
                    }                    
                } else {
                    System.err.println("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
        }
    } 
}
