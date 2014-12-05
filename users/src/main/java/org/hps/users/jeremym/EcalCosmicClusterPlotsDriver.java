package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IProfile1D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Create ADC value plots from the cosmic clusters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCosmicClusterPlotsDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
    Map<EcalChannel, IProfile1D> adcProfiles = new HashMap<EcalChannel, IProfile1D>();
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    String inputClusterCollectionName = "EcalCosmicClusters";
    String rawHitsCollectionName = "EcalCosmicReadoutHits";

    public void setInputHitsCollectionName(String inputClusterCollectionName) {
        this.inputClusterCollectionName = inputClusterCollectionName;
    }
    
    public void setRawHitsCollectionName(String rawHitsCollectionName) {
        this.rawHitsCollectionName = rawHitsCollectionName;
    }

    public void detectorChanged(Detector detector) {
        conditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {
            adcProfiles.put(channel, aida.profile1D(inputClusterCollectionName + "/ADC Values : " + String.format("%03d", channel.getChannelId()), 100, 0, 100));
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(Cluster.class, inputClusterCollectionName)) {
            if (event.hasCollection(RawTrackerHit.class, rawHitsCollectionName)) {
                Map<Long, RawTrackerHit> rawHitMap = createRawHitMap(event.get(RawTrackerHit.class, rawHitsCollectionName));
                List<Cluster> clusters = event.get(Cluster.class, inputClusterCollectionName);
                for (Cluster cluster : clusters) {
                    for (CalorimeterHit calHit : cluster.getCalorimeterHits()) {
                        RawTrackerHit rawHit = rawHitMap.get(calHit.getCellID());
                        EcalChannel channel = channels.findGeometric(rawHit.getCellID());
                        if (channel != null) {
                            IProfile1D profile = adcProfiles.get(channel);
                            for (int adcIndex = 0; adcIndex < rawHit.getADCValues().length; adcIndex++) {
                                // Fill the Profile1D with ADC value.
                                profile.fill(adcIndex, rawHit.getADCValues()[adcIndex]);
                            }
                        } else {
                            throw new RuntimeException("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(rawHit.getCellID())));
                        }
                    }
                }
            } else {
                throw new RuntimeException("Missing raw hit collection: " + rawHitsCollectionName);
            }                       
        }
    }
    
    Map<Long, RawTrackerHit> createRawHitMap(List<RawTrackerHit> rawHits) {
        Map<Long, RawTrackerHit> rawHitMap = new HashMap<Long, RawTrackerHit>();
        for (RawTrackerHit hit : rawHits) {
            rawHitMap.put(hit.getCellID(), hit);
        }
        return rawHitMap;
    }

}
