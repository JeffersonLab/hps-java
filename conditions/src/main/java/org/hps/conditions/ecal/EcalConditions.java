package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.converter.compact.HPSEcalAPI;
import org.lcsim.geometry.Subdetector;

/**
 * This class provides access to all ECAL conditions from the database, including gain, pedestal and bad channel
 * settings, per crystal.
 * <p>
 * Unlike most conditions data types, it does not extend {@link org.hps.conditions.api.ConditionsObject}, because it is
 * a composite object containing data assembled from many other {@link org.hps.conditions.ConditionsObjects} and has a
 * special data converter {@link EcalConditionsConverter}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EcalConditions {

    /**
     * The collection of {@link EcalChannel} objects.
     */
    private EcalChannelCollection channelCollection = new EcalChannelCollection();

    /**
     * Map between channels and their conditions constants.
     */
    private final Map<EcalChannel, EcalChannelConstants> channelConstants = new HashMap<EcalChannel, EcalChannelConstants>();

    /**
     * Map between channels and geometric crystals.
     */
    private EcalCrystalChannelMap crystalMap;

    /**
     * The current ECAL subdetector in the geometry.
     */
    private final Subdetector subdetector;

    /**
     * Class constructor.
     *
     * @param subdetector the ECAL subdetector object
     */
    EcalConditions(final Subdetector subdetector) {
        if (subdetector == null) {
            throw new IllegalArgumentException("The subdetector argument is null.");
        }
        this.subdetector = subdetector;
    }

    /**
     * Set the channel map.
     *
     * @param channelCollection the channel map
     */
    void setChannelCollection(final EcalChannelCollection channelCollection) {
        this.channelCollection = channelCollection;

        // Build the map between crystals and channels.
        this.crystalMap = new EcalCrystalChannelMap((HPSEcalAPI) this.subdetector.getDetectorElement(),
                channelCollection);
    }

    /**
     * Get the map between database IDs and <code>EcalChannel</code> objects.
     *
     * @return the channel map
     */
    public EcalChannelCollection getChannelCollection() {
        return this.channelCollection;
    }

    /**
     * Get the channel information for a geometric crystal.
     *
     * @param crystal the geometric crystal
     * @return the channel information or null if does not exist
     */
    public EcalChannel getChannel(final EcalCrystal crystal) {
        return this.crystalMap.getEcalChannel(crystal);
    }

    /**
     * Get the conditions constants for a specific channel. These will be created if they do not exist for the given
     * channel, BUT only channels in the channel map are accepted as an argument.
     *
     * @param channel the ECAL channel
     * @return the conditions constants for the channel
     * @throws IllegalArgumentException if channel does not exist in the channel map
     */
    public EcalChannelConstants getChannelConstants(final EcalChannel channel) {
        // This channel must come from the map.
        if (!this.channelCollection.contains(channel)) {
            System.err.println("Channel not found in map: " + channel);
            throw new IllegalArgumentException("Channel was not found in map.");
        }
        // If channel has no data yet, then add it.
        if (!this.channelConstants.containsKey(channel)) {
            this.channelConstants.put(channel, new EcalChannelConstants());
        }
        return this.channelConstants.get(channel);
    }

    /**
     * This is just used for a divider length in print output.
     */
    private static final int DIVIDER_SIZE = 91;

    /**
     * Convert this object to a string.
     *
     * @return A string representation of this object.
     */
    // FIXME: The print out from this method looks like a mess.
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append('\n');
        sb.append("Printing ECAL conditions ...");
        sb.append('\n');
        sb.append('\n');

        // Table header:
        sb.append("id");
        sb.append("    ");
        sb.append("crate");
        sb.append("  ");
        sb.append("slot");
        sb.append("   ");
        sb.append("channel");
        sb.append("  ");
        sb.append("x");
        sb.append("      ");
        sb.append("y");
        sb.append("     ");
        sb.append("gain");
        sb.append("       ");
        sb.append("pedestal");
        sb.append("   ");
        sb.append("noise");
        sb.append("      ");
        sb.append("time_shift");
        sb.append(" ");
        sb.append("bad");
        sb.append('\n');
        for (int i = 0; i < DIVIDER_SIZE; i++) {
            sb.append("-");
        }
        sb.append('\n');

        // Loop over all channels.
        for (final EcalChannel channel : this.channelCollection) {

            final EcalChannelConstants constants = getChannelConstants(channel);

            final double gain = constants.getGain().getGain();
            final double pedestal = constants.getCalibration().getPedestal();
            final double noise = constants.getCalibration().getNoise();
            final double timeShift = constants.getTimeShift().getTimeShift();
            final boolean bad = constants.isBadChannel();

            // Channel data.
            sb.append(String.format("%-5d %-6d %-6d %-8d %-6d %-6d", channel.getChannelId(), channel.getCrate(),
                    channel.getSlot(), channel.getChannel(), channel.getX(), channel.getY()));

            // Constants.
            sb.append(String.format("%-10.4f %-10.4f %-10.4f %-11.4f", gain, pedestal, noise, timeShift));

            // Bad channel.
            sb.append(bad);

            sb.append('\n');
        }
        return sb.toString();
    }
}
