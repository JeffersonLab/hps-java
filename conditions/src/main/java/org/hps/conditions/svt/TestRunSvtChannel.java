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
 * The implementation of {@link AbstractSvtChannel} for Test Run conditions.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = {"test_run_svt_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
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
         * @param the DAQ pair (FEB ID and FEB Hybrid ID)
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
     * Get the FPGA ID.
     *
     * @return the FPGA ID
     */
    @Field(names = {"fpga"})
    public Integer getFpgaID() {
        return getFieldValue("fpga");
    }

    /**
     * Get the hybrid ID.
     *
     * @return the hybrid ID
     */
    @Field(names = {"hybrid"})
    public Integer getHybridID() {
        return getFieldValue("hybrid");
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
        return getChannelID() == channel.getChannelID() && getFpgaID() == channel.getFpgaID()
                && getHybridID() == channel.getHybridID() && getChannel() == channel.getChannel();
    }
}
