package org.hps.conditions.svt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.util.Pair;

/**
 * This abstract class provides basic setup information for an SVT sensor channel.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class AbstractSvtChannel extends BaseConditionsObject {

    // TODO: Put constants into their own class
    public static final int MAX_NUMBER_OF_SAMPLES = 6;

    public static abstract class AbstractSvtChannelCollection<T extends AbstractSvtChannel> extends BaseConditionsObjectCollection<T> {

        Map<Integer, T> channelMap = new HashMap<Integer, T>();

        /**
         *  Add a channel of type extending {@link AbstractSvtChannel} to the
         *  channel map
         * 
         *  @param A channel of a type extending {@link AbstractSvtChannel}
         */
        public boolean add(T channel) {

            // If it doesn't exist, add the channel to the channel map
            if (channelMap.containsKey(channel.getChannelID())) {
                throw new IllegalArgumentException("[ " + this.getClass().getSimpleName() + " ]: Channel ID already exists: " + channel.getChannelID());
            }
            channelMap.put(channel.getChannelID(), channel);

            // Add to the collection
            return super.add(channel);                                  
        }

        /**
         *  Find a channel of type extending {@link AbstractSvtChannel} using the
         *  channel ID
         * 
         *  @param channelID
         *  @return An SVT channel of type extending {@link AbstractSvtChannel}
         */
        public T findChannel(int channelID) {
            return channelMap.get(channelID);
        }

        /**
         *  Find the collection of channels of type extending
         *  {@link AbstractSvtChannel} that match a DAQ pair.
         * 
         *  @param pair The DAQ pair.
         *  @return The channels matching the DAQ pair or null if not found.
         */
        public abstract Collection<T> find(Pair<Integer, Integer> pair);

        /**
         *  Convert this object to a human readable string.
         * 
         *  @return This object converted to a string.
         */
        public String toString() {
            StringBuffer buff = new StringBuffer();
            for (T channel : this) {
                buff.append(channel.toString() + '\n');
            }
            return buff.toString();
        }
    }

    /**
     *  Get the channel ID.
     * 
     *  @return The SVT channel ID.
     */
    @Field(names = {"channel_id"})
    public int getChannelID() {
        return getFieldValue("channel_id");
    }

    /**
     *  Get the channel number (0-639). This is different from the ID.
     * 
     *  @return The channel number.
     */
    @Field(names = {"channel"})
    public int getChannel() {
        return getFieldValue("channel");
    }
    
    /**
     *  Set the channel ID.
     * 
     *  @param channelID : The SVT channel ID
     */
    public void setChannelID(int channelID) { 
        this.setFieldValue("channel_id", channelID);
    }
    
    /**
     *  Set the channel number (0-639). This is different from the ID.
     * 
     *  @param channel : The channel number
     */
    public void setChannel(int channel) { 
        this.setFieldValue("channel", channel);
    }
}