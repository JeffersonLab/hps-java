package org.hps.detector.ecal;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.converter.compact.HPSEcalAPI;

/**
 * This is a convenience utility for associating the geometric crystal objects with the conditions system channel
 * information and vice versa.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EcalCrystalChannelMap {

    /**
     * Map of channel to crystal.
     */
    private final Map<EcalChannel, EcalCrystal> channelMap = new HashMap<EcalChannel, EcalCrystal>();

    /**
     * Map of crystal to channel.
     */
    private final Map<EcalCrystal, EcalChannel> crystalMap = new HashMap<EcalCrystal, EcalChannel>();

    /**
     * Creates the map between crystals and channels.
     *
     * @param api the ECAL geometry API
     * @param channels the list of channels
     */
    EcalCrystalChannelMap(final HPSEcalAPI api, final EcalChannelCollection channels) {

        // Map crystals to channels.
        for (final EcalCrystal crystal : api.getCrystals()) {
            final EcalChannel channel = channels.findGeometric(crystal.getIdentifier().getValue());
            if (channel == null) {
                throw new RuntimeException("ECAL channel was not found for ID: " + crystal.getExpandedIdentifier());
            }
            this.crystalMap.put(crystal, channel);
        }

        // Map channels to crystals.
        for (final EcalChannel channel : channels) {
            final EcalCrystal crystal = api.getCrystal(channel.getX(), channel.getY());
            if (crystal == null) {
                throw new RuntimeException("ECAl crystal was not found for channel X Y: " + channel.getX() + " "
                        + channel.getY());
            }
            this.channelMap.put(channel, crystal);
        }
    }

    /**
     * Get a channel from a crystal.
     *
     * @param crystal the crystal's geometry object
     * @return the channel information or <code>null</code> if does not exist
     */
    EcalChannel getEcalChannel(final EcalCrystal crystal) {
        return this.crystalMap.get(crystal);
    }

    /**
     * Get a crystal from a channel.
     *
     * @param channel the ECAL channel object
     * @return the crystal's geometry object or <code>null</code> if does not exist
     */
    EcalCrystal getEcalCrystal(final EcalChannel channel) {
        return this.channelMap.get(channel);
    }
}
