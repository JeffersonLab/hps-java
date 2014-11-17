package org.hps.conditions.ecal;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractIdentifier;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;

/**
 * This class encapsulates all the information about a single ECal channel,
 * corresponding to one physical crystal in the detector.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalChannel extends AbstractConditionsObject {

    /**
     * The <code>DaqId</code> is the combination of crate, slot and channel that
     * specify the channel's DAQ configuration.
     */
    public static final class DaqId extends AbstractIdentifier {

        private int crate = -1;
        private int slot = -1;
        private int channel = -1;

        public DaqId(int values[]) {
            crate = values[0];
            slot = values[1];
            channel = values[2];
        }

        @Override
        public long encode() {
            // from Sho's code
            return (((long) crate) << 32) | ((long) slot << 16) | (long) channel;
        }

        @Override
        public boolean isValid() {
            return crate != -1 && slot != -1 && channel != -1;
        }
    }

    /**
     * The <code>GeometryId</code> contains the x and y indices of the crystal
     * in the LCSIM-based geometry representation.
     */
    public static final class GeometryId extends AbstractIdentifier {

        private int system = -1;
        private int x = Integer.MAX_VALUE;
        private int y = Integer.MAX_VALUE;

        private IIdentifierHelper helper;

        public GeometryId(IIdentifierHelper helper, int[] values) {
            this.helper = helper;
            system = values[0];
            x = values[1];
            y = values[2];
        }

        @Override
        public long encode() {
            IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
            expId.setValue(helper.getFieldIndex("system"), system);
            expId.setValue(helper.getFieldIndex("ix"), x);
            expId.setValue(helper.getFieldIndex("iy"), y);
            return helper.pack(expId).getValue();
        }

        @Override
        public boolean isValid() {
            return system != -1 && x != Integer.MAX_VALUE && y != Integer.MAX_VALUE;
        }
    }

    /**
     * The <code>ChannelId</code> is a unique number identifying the channel
     * within its conditions collection. The channels in the database are given
     * sequential channel IDs from 1 to N in semi-arbitrary order. The channel
     * ID is generally the number used to connect other conditions objects such
     * as {@link EcalGain} or {@link EcalCalibration} to the appropriate crystal
     * in the calorimeter.
     */
    public static final class ChannelId extends AbstractIdentifier {

        private int id = -1;

        public ChannelId(int[] values) {
            id = values[0];
        }

        @Override
        public long encode() {
            return id;
        }

        @Override
        public boolean isValid() {
            return id != -1;
        }
    }

    DaqId createDaqId() {
        return new DaqId(new int[] { getCrate(), getSlot(), getChannel() });
    }

    GeometryId createGeometryId(IIdentifierHelper helper, int system) {
        return new GeometryId(helper, new int[] { system, getX(), getY() });
    }

    ChannelId createChannelId() {
        return new ChannelId(new int[] { this.getChannelId() });
    }

    /**
     * A collection of {@link EcalChannel} objects.
     */
    public static class EcalChannelCollection extends ConditionsObjectCollection<EcalChannel> {

        // Identifier maps for fast lookup.
        Map<Long, EcalChannel> daqMap = new HashMap<Long, EcalChannel>();
        Map<Long, EcalChannel> geometryMap = new HashMap<Long, EcalChannel>();
        Map<Long, EcalChannel> channelMap = new HashMap<Long, EcalChannel>();

        /**
         * Add an <code>EcalChannel</code> to the collection and cache its ID
         * information. The GeometryId must be created later as it requires
         * access to the Detector API.
         */
        @Override
        public void add(EcalChannel channel) throws ConditionsObjectException {
            super.add(channel);
            DaqId daqId = channel.createDaqId();
            if (daqId.isValid())
                daqMap.put(daqId.encode(), channel);
            ChannelId channelId = channel.createChannelId();
            if (channelId.isValid())
                channelMap.put(channelId.encode(), channel);
        }

        /**
         * Build the map of geometry IDs.
         * @param helper The identifier helper of the subdetector.
         * @param system The system ID of the subdetector.
         */
        void buildGeometryMap(IIdentifierHelper helper, int system) {
            for (EcalChannel channel : this.objects) {
                GeometryId geometryId = channel.createGeometryId(helper, system);
                geometryMap.put(geometryId.encode(), channel);
            }
        }

        /**
         * Find a channel by using DAQ information.
         * @param daqId The DAQ ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(DaqId daqId) {
            return daqMap.get(daqId.encode());
        }

        /**
         * Find a channel by using its physical ID information.
         * @param geometryId The geometric ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(GeometryId geometryId) {
            return geometryMap.get(geometryId.encode());
        }

        /**
         * Find a channel by its channel ID.
         * @param channelId The channel ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(ChannelId channelId) {
            return channelMap.get(channelId.encode());
        }

        /**
         * Find a channel by its encoded geometric ID.
         * @param id The encoded geometric ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findGeometric(long id) {
            return geometryMap.get(id);
        }

        /**
         * Find a channel by its encoded channel ID.
         * @param id The encoded channel ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(long id) {
            return channelMap.get(id);
        }

        /**
         * Find a channel by its encoded DAQ ID.
         * @param id The encoded DAQ ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findDaq(long id) {
            return daqMap.get(id);
        }
    }

    /**
     * Get the crate number of the channel.
     * @return The crate number.
     */
    public int getCrate() {
        return getFieldValue("crate");
    }

    /**
     * Get the slot number of the channel.
     * @return The slot number.
     */
    public int getSlot() {
        return getFieldValue("slot");
    }

    /**
     * Get the channel number of the channel.
     * @return The channel number.
     */
    public int getChannel() {
        return getFieldValue("channel");
    }

    /**
     * Get the x value of the channel.
     * @return The x value.
     */
    public int getX() {
        return getFieldValue("x");
    }

    /**
     * Get the y value of the channel.
     * @return The y value.
     */
    public int getY() {
        return getFieldValue("y");
    }

    /**
     * Get the ID of the channel.
     * @return The ID of the channel.
     */
    public int getChannelId() {
        return getFieldValue("channel_id");
    }

    /**
     * Implementation of equals.
     * @return True if objects are equal.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof EcalChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        EcalChannel c = (EcalChannel) o;
        return c.getChannelId() == getChannelId() && c.getCrate() == getCrate() && c.getSlot() == getSlot() && c.getChannel() == getChannel() && c.getX() == getX() && c.getY() == getY();
    }
}