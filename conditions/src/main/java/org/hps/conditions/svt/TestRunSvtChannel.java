package org.hps.conditions.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * The implementation of {@link AbstractSvtChannel} for Test Run conditions.
 *
 * @author Omar Moreno, UCSC
 */
@Table(names = {"test_run_svt_channels"})
public final class TestRunSvtChannel extends AbstractSvtChannel {

    /**
     * Concrete collection implementation for {@link TestRunSvtChannel} objects.
     */
    @SuppressWarnings("serial")
    public static class TestRunSvtChannelCollection extends
            AbstractSvtChannel.AbstractSvtChannelCollection<TestRunSvtChannel> {

        /**
         * Find a collection of channels by their DAQ pair assignment.
         *
         * @param pair the DAQ pair (FEB ID and FEB Hybrid ID)
         * @return the collection of channels
         */
        @Override
        public Collection<TestRunSvtChannel> find(final Pair<Integer, Integer> pair) {
            final List<TestRunSvtChannel> channels = new ArrayList<TestRunSvtChannel>();
            final int fpga = pair.getFirstElement();
            final int hybrid = pair.getSecondElement();
            for (final TestRunSvtChannel channel : this) {
                if (channel.getFpgaID() == fpga && channel.getHybridID() == hybrid) {
                    channels.add(channel);
                }
            }
            return channels;
        }
    }

    /**
     * Implementation of equals.
     *
     * @param o the other object
     * @return <code>true</code> if the object equals this one; false if not.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof TestRunSvtChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        final TestRunSvtChannel channel = (TestRunSvtChannel) o;
        return this.getChannelID() == channel.getChannelID() && this.getFpgaID() == channel.getFpgaID()
                && this.getHybridID() == channel.getHybridID() && this.getChannel() == channel.getChannel();
    }

    /**
     * Get the FPGA ID.
     *
     * @return the FPGA ID
     */
    @Field(names = {"fpga"})
    public Integer getFpgaID() {
        return this.getFieldValue("fpga");
    }

    /**
     * Get the hybrid ID.
     *
     * @return the hybrid ID
     */
    @Field(names = {"hybrid"})
    public Integer getHybridID() {
        return this.getFieldValue("hybrid");
    }
}
