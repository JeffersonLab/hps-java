package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.converter.compact.HPSEcalAPI;

/**
 * This is a convenience utility for associating the geometric crystal
 * objects with the conditions system channel information.  
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class EcalCrystalChannelMap {
    
    Map<EcalCrystal, EcalChannel> crystalMap = new HashMap<EcalCrystal, EcalChannel>();
    
    /**
     * Creates the map between crystals and channels.
     * @param api The ECAL API.
     * @param channels The list of channels.
     */
    EcalCrystalChannelMap(HPSEcalAPI api, EcalChannelCollection channels) {
        List<EcalCrystal> crystals = api.getCrystals();        
        for (EcalCrystal crystal : crystals) {
            EcalChannel channel = channels.findGeometric(crystal.getIdentifier().getValue());
            if (channel == null) {
                throw new RuntimeException("ECAL channel was not found for ID: " + crystal.getExpandedIdentifier());
            }
            crystalMap.put(crystal, channel);
        }
    }
    
    /**
     * Get a channel from a crystal.
     * @param crystal The geometry object.
     * @return The channel information or null if does not exist.
     */
    EcalChannel getEcalChannel(EcalCrystal crystal) {
        return crystalMap.get(crystal);
    }    
}