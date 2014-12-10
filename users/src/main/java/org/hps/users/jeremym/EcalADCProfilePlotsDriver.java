package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IProfile1D;
import hep.aida.ref.fitter.FitResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalADCProfilePlotsDriver extends Driver {

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
        conditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {            
            adcProfiles.put(channel, aida.profile1D(inputHitsCollectionName + "/ : ADC Values : " + String.format("%03d", channel.getChannelId()), 100, 0, 100));
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
