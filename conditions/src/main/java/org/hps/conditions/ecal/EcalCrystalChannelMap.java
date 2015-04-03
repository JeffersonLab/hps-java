package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.converter.compact.HPSEcalAPI;

/**
 * This is a convenience utility for associating the geometric crystal
 * objects with the conditions system channel information and vice versa. 
 * 
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class EcalCrystalChannelMap {

    /**
     * Map of crystal to channel.
     */
    Map<EcalCrystal, EcalChannel> crystalMap = new HashMap<EcalCrystal, EcalChannel>();
    
    /**
     * Map of channel to crystal.
     */
    Map<EcalChannel, EcalCrystal> channelMap = new HashMap<EcalChannel, EcalCrystal>();

    /**
     * Creates the map between crystals and channels.
     * @param api The ECAL API.
     * @param channels The list of channels.
     */
    EcalCrystalChannelMap(final HPSEcalAPI api, final EcalChannelCollection channels) {

        // Map crystals to channels.
        for (EcalCrystal crystal : api.getCrystals()) {
            final EcalChannel channel = channels.findGeometric(crystal.getIdentifier().getValue());
            if (channel == null) {
                throw new RuntimeException("ECAL channel was not found for ID: " + crystal.getExpandedIdentifier());
            }
            crystalMap.put(crystal, channel);
        }

        // Map channels to crystals.
        for (EcalChannel channel : channels) {
            final EcalCrystal crystal = api.getCrystal(channel.getX(), channel.getY());
            if (crystal == null) {
                throw new RuntimeException("ECAl crystal was not found for channel X Y: " 
                        + channel.getX() + " " + channel.getY());
            }
            channelMap.put(channel, crystal);
        }
    }

    /**
     * Get a channel from a crystal.
     * @param crystal The geometry object.
     * @return The channel information or null if does not exist.
     */
    EcalChannel getEcalChannel(final EcalCrystal crystal) {
        return crystalMap.get(crystal);
    }    

    /**
     * Get a channel from a crystal.
     * @param crystal The geometry object.
     * @return The channel information or null if does not exist.
     */
    EcalCrystal getEcalCrystal(final EcalChannel channel) {
        return channelMap.get(channel);
    } 
}