package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.util.Pair;

/**
 * This class represents SVT channel setup information, including FEB ID, FEB
 * Hybrid ID, and channel numbers.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtChannel extends AbstractSvtChannel {

    public static class SvtChannelCollection extends AbstractSvtChannel.AbstractSvtChannelCollection<SvtChannel> {

        /**
         * Find channels that match a DAQ pair (FEB ID, FEB Hybrid ID).
         * @param pair The DAQ pair.
         * @return The channels matching the DAQ pair or null if not found.
         */
        @Override
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