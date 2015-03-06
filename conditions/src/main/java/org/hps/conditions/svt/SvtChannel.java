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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
@Table(names = { "svt_channels" })
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtChannel extends AbstractSvtChannel {

    public static class SvtChannelCollection extends AbstractSvtChannel.AbstractSvtChannelCollection<SvtChannel> {
        /**
         * Find channels that match a DAQ pair (FEB ID, FEB Hybrid ID).
         * 
         * @param pair : The DAQ pair consiting of a FEB ID and FEB Hybrid ID.
         * @return The channels matching the DAQ pair or null if ?not found.
         */
        @Override
        public Collection<SvtChannel> find(Pair<Integer, Integer> pair) {
            List<SvtChannel> channels = new ArrayList<SvtChannel>();
            int febID = pair.getFirstElement();
            int febHybridID = pair.getSecondElement();
            for (SvtChannel channel : this) {
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
        public int findChannelID(int febID, int febHybridID, int channel) {
            for (SvtChannel svtChannel : this) {
                if (svtChannel.getFebID() == febID && svtChannel.getFebHybridID() == febHybridID && svtChannel.getChannel() == channel) {
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
    @Field(names = { "feb_id" })
    public int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the FEB hybrid ID associated with this SVT channel ID.
     * 
     * @return The FEB hybrid ID.
     */
    @Field(names = { "feb_hybrid_id" })
    public int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
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
