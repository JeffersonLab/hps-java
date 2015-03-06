package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.converter.compact.HPSEcalAPI;
import org.lcsim.geometry.Subdetector;

/**
 * This class provides access to all ECAL conditions from the database,
 * including gain, pedestal and bad channel settings, per crystal.
 * 
 * Unlike most conditions data types, it does not extend
 * {@link org.hps.conditions.api.ConditionsObject}, because it is a composite object
 * containing data assembled from many other
 * {@link org.hps.conditions.ConditionsObjects}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalConditions {

    // Channel collection.
    EcalChannelCollection channelCollection = new EcalChannelCollection();

    // Map between channels and their conditions data.
    Map<EcalChannel, EcalChannelConstants> channelConstants = new HashMap<EcalChannel, EcalChannelConstants>();

    // Map between geometry stations and channel.
    EcalCrystalChannelMap crystalMap;
    
    // Reference to the current ECAL subdetector in the geometry.
    Subdetector subdetector;
    
    /**
     * Class constructor, which is package protected.
     */
    EcalConditions(Subdetector subdetector) {
        if (subdetector == null) {
            throw new IllegalArgumentException("The subdetector argument is null.");
        }
        this.subdetector = subdetector;        
    }

    /**
     * Set the channel map.
     * @param channels The channel map.
     */
    void setChannelCollection(EcalChannelCollection channelCollection) {
        this.channelCollection = channelCollection;
        
        // Build the map between crystals and channels.
        crystalMap = new EcalCrystalChannelMap((HPSEcalAPI)subdetector.getDetectorElement(), channelCollection);
    }

    /**
     * Get the map between database IDs and <code>EcalChannel</code> objects.
     * @return The channel map.
     */
    public EcalChannelCollection getChannelCollection() {
        return channelCollection;
    }
    
    /**
     * Get the channel information for a geometric crystal.
     * @param crystal The geometric crystal.
     * @return The channel information or null if does not exist.
     */
    public EcalChannel getChannel(EcalCrystal crystal) {
        return crystalMap.getEcalChannel(crystal);
    }
        
    /**
     * Get the conditions constants for a specific channel. These will be
     * created if they do not exist for the given channel, BUT only channels in
     * the channel map are accepted as an argument.
     * @param channel The ECAL channel.
     * @return The conditions constants for the channel.
     * @throws IllegalArgumentException if channel does not exist in the channel
     *             map.
     */
    public EcalChannelConstants getChannelConstants(EcalChannel channel) {
        // This channel must come from the map.
        if (!channelCollection.contains(channel)) {
            System.err.println("Channel not found in map: " + channel);
            throw new IllegalArgumentException("Channel was not found in map.");
        }
        // If channel has no data yet, then add it.
        if (!channelConstants.containsKey(channel))
            channelConstants.put(channel, new EcalChannelConstants());
        return channelConstants.get(channel);
    }

    /**
     * Convert this object to a string.
     * @return A string representation of this object.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();

        buff.append('\n');
        buff.append("Printing ECAL conditions ...");
        buff.append('\n');
        buff.append('\n');

        // Table header:
        buff.append("id");
        buff.append("    ");
        buff.append("crate");
        buff.append("  ");
        buff.append("slot");
        buff.append("   ");
        buff.append("channel");
        buff.append("  ");
        buff.append("x");
        buff.append("      ");
        buff.append("y");
        buff.append("     ");
        buff.append("gain");
        buff.append("       ");
        buff.append("pedestal");
        buff.append("   ");
        buff.append("noise");
        buff.append("      ");
        buff.append("time_shift");
        buff.append(" ");
        buff.append("bad");
        buff.append('\n');
        for (int i = 0; i < 91; i++) {
            buff.append("-");
        }
        buff.append('\n');

        // Loop over all channels.
        for (EcalChannel channel : channelCollection) {

            EcalChannelConstants constants = getChannelConstants(channel);

            double gain = constants.getGain().getGain();
            double pedestal = constants.getCalibration().getPedestal();
            double noise = constants.getCalibration().getNoise();
            double timeShift = constants.getTimeShift().getTimeShift();
            boolean bad = constants.isBadChannel();

            // Channel data.
            buff.append(String.format("%-5d %-6d %-6d %-8d %-6d %-6d", channel.getChannelId(), channel.getCrate(), channel.getSlot(), channel.getChannel(), channel.getX(), channel.getY()));

            // Constants.
            buff.append(String.format("%-10.4f %-10.4f %-10.4f %-11.4f", gain, pedestal, noise, timeShift));

            // Bad channel.
            buff.append(bad);

            buff.append('\n');
        }
        return buff.toString();
    }
}