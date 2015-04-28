package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class represents SVT channel setup information, including FEB ID, FEB Hybrid ID, and channel numbers.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = {"svt_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtChannel extends AbstractSvtChannel {

    /**
     * Maximum channel number.
     */
    private static final int MAX_CHANNEL = 639;

    /**
     * Minimum channel number.
     */
    private static final int MIN_CHANNEL = 0;

    /**
     * Maximum FEB Hybrid ID.
     */
    private static final int MAX_FEB_HYBRID_ID = 3;

    /**
     * Minimum FEB hybrid ID.
     */
    private static final int MIN_FEB_HYBRID_ID = 0;

    /**
     * Maximum FEB ID.
     */
    private static final int MAX_FEB_ID = 9;

    /**
     * Minimum FEB ID.
     */
    private static final int MIN_FEB_ID = 0;

    /**
     * Default constructor.
     */
    public SvtChannel() {
    }

    /**
     * Fully qualified constructor.
     *
     * @param channelID the SVT channel ID
     * @param febID the Front End Board (FEB) ID (0-9)
     * @param febHybridID the hybrid ID (0-3)
     * @param channel the channel number (0-639)
     */
    public SvtChannel(final int channelID, final int febID, final int febHybridID, final int channel) {
        if (!this.isValidFeb(febID) || !this.isValidFebHybridID(febHybridID) || !this.isValidPhysicalChannel(channel)) {
            throw new RuntimeException("Invalid FEB ID, FEB hybrid ID or physical channel number is being used.");
        }
        this.setChannelID(channelID);
        this.setFebID(febID);
        this.setFebHybridID(febHybridID);
        this.setChannel(channel);
    }

    /**
     * Collection implementation for {@link SvtChannel}.
     */
    @SuppressWarnings("serial")
    public static class SvtChannelCollection extends AbstractSvtChannel.AbstractSvtChannelCollection<SvtChannel> {
        /**
         * Find channels that match a DAQ pair (FEB ID, FEB Hybrid ID).
         *
         * @param pair the DAQ pair consisting of a FEB ID and FEB Hybrid ID
         * @return the channels matching the DAQ pair or null if not found
         */
        @Override
        public Collection<SvtChannel> find(final Pair<Integer, Integer> pair) {
            final List<SvtChannel> channels = new ArrayList<SvtChannel>();
            final int febID = pair.getFirstElement();
            final int febHybridID = pair.getSecondElement();
            for (final SvtChannel channel : this) {
                if (channel.getFebID() == febID && channel.getFebHybridID() == febHybridID) {
                    channels.add(channel);
                }
            }
            return channels;
        }

        /**
         * Get the SVT channel ID associated with a given FEB ID/Hybrid ID/physical channel.
         *
         * @param febID : The FEB ID
         * @param febHybridID : The FEB hybrid ID
         * @param channel : The physical channel number
         * @return The SVT channel ID
         * @throws {@link RuntimeException} if the channel ID can't be found
         */
        public final int findChannelID(final int febID, final int febHybridID, final int channel) {
            for (final SvtChannel svtChannel : this) {
                if (svtChannel.getFebID() == febID && svtChannel.getFebHybridID() == febHybridID
                        && svtChannel.getChannel() == channel) {
                    return svtChannel.getChannelID();
                }
            }
            // throw new RuntimeException("Channel ID couldn't be found");
            return -1;
        }
    }

    /**
     * Get the FEB ID associated with this SVT channel ID.
     *
     * @return The FEB ID.
     */
    @Field(names = {"feb_id"})
    public Integer getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the FEB hybrid ID associated with this SVT channel ID.
     *
     * @return The FEB hybrid ID.
     */
    @Field(names = {"feb_hybrid_id"})
    public Integer getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     * Set the FEB ID associated with this SVT channel ID.
     *
     * @param febID the FEB ID
     */
    public void setFebID(final int febID) {
        this.setFieldValue("feb_id", febID);
    }

    /**
     * Set the FEB hybrid ID associated with this SVT channel ID.
     *
     * @param febHybridID : The FEB hybrid ID
     */
    public void setFebHybridID(final int febHybridID) {
        this.setFieldValue("feb_hybrid_id", febHybridID);
    }

    /**
     * Checks if a FEB ID is valid.
     *
     * @param febID the Front End Board (FEB) ID
     * @return <code>true</code> if the FEB ID lies within the range 0-9
     */
    public boolean isValidFeb(final int febID) {
        return febID >= MIN_FEB_ID && febID <= MAX_FEB_ID ? true : false;
    }

    /**
     * Checks if a Front End Board hybrid ID is valid.
     *
     * @param febHybridID the hybrid ID
     * @return <code>true</code> if the hybrid ID lies within the range 0-3
     */
    public boolean isValidFebHybridID(final int febHybridID) {
        return febHybridID >= MIN_FEB_HYBRID_ID && febHybridID <= MAX_FEB_HYBRID_ID ? true : false;
    }

    /**
     * Checks if a physical channel number is valid.
     *
     * @param channel the physical channel number
     * @return <code>true</code> if the channel number lies within the range 0-639
     */
    public boolean isValidPhysicalChannel(final int channel) {
        return channel >= MIN_CHANNEL && channel <= MAX_CHANNEL ? true : false;
    }

    /**
     * Implementation of equals.
     *
     * @return <code>true</code> if the object equals this one
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof SvtChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        final SvtChannel channel = (SvtChannel) o;
        return getChannelID() == channel.getChannelID() && getFebID() == channel.getFebID()
                && getFebHybridID() == channel.getFebHybridID() && getChannel() == channel.getChannel();
    }
}
