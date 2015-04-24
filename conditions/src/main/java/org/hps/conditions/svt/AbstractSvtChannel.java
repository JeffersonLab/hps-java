package org.hps.conditions.svt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.database.Field;
import org.hps.util.Pair;

/**
 * This abstract class provides basic setup information for an SVT sensor channel.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public abstract class AbstractSvtChannel extends BaseConditionsObject {

    /**
     * Default number of samples to read out.
     */
    // TODO: Put constants into their own class.
    public static final int MAX_NUMBER_OF_SAMPLES = 6;

    /**
     * Collection implementation for {@link AbstractSvtChannel}.
     *
     * @param <T> A type extending AbstractSvtChannel
     */
    @SuppressWarnings("serial")
    public abstract static class AbstractSvtChannelCollection<T extends AbstractSvtChannel> extends
            BaseConditionsObjectCollection<T> {

        /**
         * Map of channel number to object.
         */
        private final Map<Integer, T> channelMap = new HashMap<Integer, T>();

        /**
         * Add a channel of type extending {@link AbstractSvtChannel} to the channel map.
         *
         * @param channel channel of a type extending {@link AbstractSvtChannel}
         */
        @Override
        public final boolean add(final T channel) throws ConditionsObjectException {

            // If it doesn't exist, add the channel to the channel map
            if (this.channelMap.containsKey(channel.getChannelID())) {
                throw new IllegalArgumentException("[ " + this.getClass().getSimpleName()
                        + " ]: Channel ID already exists: " + channel.getChannelID());
            }
            this.channelMap.put(channel.getChannelID(), channel);

            // Add to the collection
            return super.add(channel);
        }

        /**
         * Find a channel of type extending {@link AbstractSvtChannel} using the channel ID.
         *
         * @param channelID the channel ID
         * @return an SVT channel of type extending {@link AbstractSvtChannel}
         */
        public final T findChannel(final int channelID) {
            return this.channelMap.get(channelID);
        }

        /**
         * Find the collection of channels of type extending {@link AbstractSvtChannel} that match a DAQ pair (FEB ID
         * and FEB Hybrid ID).
         *
         * @param pair the DAQ pair
         * @return the channels matching the DAQ pair or null if not found
         */
        public abstract Collection<T> find(final Pair<Integer, Integer> pair);

        /**
         * Convert this object to a human readable string.
         *
         * @return This object converted to a string.
         */
        @Override
        public final String toString() {
            final StringBuffer buff = new StringBuffer();
            for (final T channel : this) {
                buff.append(channel.toString() + '\n');
            }
            return buff.toString();
        }
    }

    /**
     * Get the channel ID.
     *
     * @return the SVT channel ID
     */
    @Field(names = {"channel_id"})
    public final int getChannelID() {
        return getFieldValue("channel_id");
    }

    /**
     * Get the channel number (0-639). This is different from the ID.
     *
     * @return the channel number
     */
    @Field(names = {"channel"})
    public final int getChannel() {
        return getFieldValue("channel");
    }

    /**
     * Set the channel ID.
     *
     * @param channelID the SVT channel ID
     */
    public final void setChannelID(final int channelID) {
        this.setFieldValue("channel_id", channelID);
    }

    /**
     * Set the channel number (0-639). This is different from the ID.
     *
     * @param channel the channel number
     */
    public final void setChannel(final int channel) {
        this.setFieldValue("channel", channel);
    }
}
