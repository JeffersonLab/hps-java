package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsObjectException;
import org.hps.util.Pair;

/**
 * This class represents SVT channel setup information, including FEB ID, 
 * FEB Hybrid ID, and channel numbers.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtChannel extends AbstractConditionsObject {

	public static final int MAX_NUMBER_OF_SAMPLES = 6;

	public static class SvtChannelCollection extends ConditionsObjectCollection<SvtChannel> {

        Map<Integer, SvtChannel> channelMap = new HashMap<Integer, SvtChannel>();

        public void add(SvtChannel channel) {
            // Add to map.
            if (channelMap.containsKey(channel.getChannelID())) {
                throw new IllegalArgumentException("Channel ID already exists: " + channel.getChannelID());
            }
            channelMap.put(channel.getChannelID(), channel);

            // Add to collection.
            try {
                super.add(channel);
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }

        public SvtChannel findChannel(int channelId) {
            return channelMap.get(channelId);
        }

        /**
         * Find channels that match a DAQ pair (FEB ID, FEB Hybrid ID).
         * @param pair The DAQ pair.
         * @return The channels matching the DAQ pair or null if not found.
         */
        public Collection<SvtChannel> find(Pair<Integer, Integer> pair) {
            List<SvtChannel> channels = new ArrayList<SvtChannel>();
            int febID = pair.getFirstElement();
            int febHybridID = pair.getSecondElement();
            for (SvtChannel channel : this.getObjects()) {
                if (channel.getFebID() == febID && channel.getFebHybridID() == febHybridID) {
                    channels.add(channel);
                }
            }
            return channels;
        }

        /**
         * Convert this object to a human readable string.
         * @return This object converted to a string.
         */
        public String toString() {
            StringBuffer buff = new StringBuffer();
            for (SvtChannel channel : this.getObjects()) {
                buff.append(channel.toString() + '\n');
            }
            return buff.toString();
        }
    }

    /**
     * Get the channel ID.
     * @return The channel ID.
     */
    public int getChannelID() {
        return getFieldValue("channel_id");
    }

    /**
     * Get the FEB ID.
     * @return The FEB ID.
     */
    public int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the FEB hybrid ID.
     * @return The FEB hybrid ID.
     */
    public int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     * Get the channel number. This is different from the ID.
     * @return The channel number.
     */
    public int getChannel() {
        return getFieldValue("channel");
    }

    /**
     * Convert this object to a human readable string.
     * @return This object as a string.
     */
    public String toString() {
        return "channel_id: " + getChannelID() + ", feb_id: " + getFebID() + ", feb_hybrid_id: " + getFebHybridID() + ", channel: " + getChannel();
    }

    /**
     * Implementation of equals.
     * @return True if the object equals this one; false if not.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof SvtChannel))
            return false;
        if (o == this)
            return true;
        SvtChannel channel = (SvtChannel) o;
        return getChannelID() == channel.getChannelID() && getFebID() == channel.getFebID() && getFebHybridID() == channel.getFebHybridID() && getChannel() == channel.getChannel();
    }
}