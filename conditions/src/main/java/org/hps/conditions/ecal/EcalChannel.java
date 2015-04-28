package org.hps.conditions.ecal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.api.AbstractConditionsObjectConverter;
import org.hps.conditions.api.AbstractIdentifier;
import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Subdetector;

/**
 * This class encapsulates all the information about a single ECal channel, corresponding to one physical crystal in the
 * detector.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"ecal_channels", "test_run_ecal_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED, converter = EcalChannel.EcalChannelConverter.class)
public final class EcalChannel extends BaseConditionsObject {

    /**
     * The <code>ChannelId</code> is a unique number identifying the channel within its conditions collection. The
     * channels in the database are given sequential channel IDs from 1 to N in semi-arbitrary order. The channel ID is
     * generally the number used to connect other conditions objects such as {@link EcalGain} or {@link EcalCalibration}
     * to the appropriate crystal in the calorimeter.
     */
    public static final class ChannelId extends AbstractIdentifier {

        /**
         * The channel ID value.
         */
        private int id = -1;

        /**
         * Create a channel ID.
         *
         * @param values the values (size 0 with single int value)
         */
        public ChannelId(final int[] values) {
            this.id = values[0];
        }

        /**
         * Encode as long value (just returns the int value).
         *
         * @return the ID's value encoded as a <code>long</code>
         */
        @Override
        public long encode() {
            return this.id;
        }

        /**
         * Return <code>true</code> if ID is valid
         *
         * @return <code>true</code> if ID is valid
         */
        @Override
        public boolean isValid() {
            return this.id != -1;
        }
    }

    /**
     * The <code>DaqId</code> is the combination of crate, slot and channel that specify the channel's DAQ
     * configuration.
     */
    public static final class DaqId extends AbstractIdentifier {

        /**
         * The DAQ channel number.
         */
        private int channel = -1;

        /**
         * The DAQ crate number.
         */
        private int crate = -1;

        /**
         * The DAQ slot number.
         */
        private int slot = -1;

        /**
         * Create a DAQ ID from an array of values.
         *
         * @param values The list of values (crate, slot, channel).
         */
        public DaqId(final int values[]) {
            this.crate = values[0];
            this.slot = values[1];
            this.channel = values[2];
        }

        /**
         * Encode this ID into a long value.
         *
         * @return The encoded long value.
         */
        @Override
        public long encode() {
            // from ECAL readout sim code
            return (long) this.crate << 32 | (long) this.slot << 16 | this.channel;
        }

        /**
         * Check if the values look valid.
         *
         * @return True if ID's values are valid.
         */
        @Override
        public boolean isValid() {
            return this.crate != -1 && this.slot != -1 && this.channel != -1;
        }
    }

    /**
     * A collection of {@link EcalChannel} objects.
     */
    public static class EcalChannelCollection extends BaseConditionsObjectCollection<EcalChannel> {

        /**
         * Comparison of ECAL channel objects.
         */
        class ChannelIdComparator implements Comparator<EcalChannel> {
            /**
             * Compare two ECAL channel objects using their channel ID.
             *
             * @param c1 the first object
             * @param c2 the second object
             * @return -1, 0, or 1 if first channel is less than, equal to or greater than second
             */
            @Override
            public int compare(final EcalChannel c1, final EcalChannel c2) {
                if (c1.getChannelId() < c2.getChannelId()) {
                    return -1;
                } else if (c1.getChannelId() > c2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        /**
         * Map of {@link #ChannelId} to channel object.
         */
        private final Map<Long, EcalChannel> channelMap = new HashMap<Long, EcalChannel>();

        /**
         * Map of {@link #DaqId} to channel object.
         */
        private final Map<Long, EcalChannel> daqMap = new HashMap<Long, EcalChannel>();

        /**
         * Map of {@link #GeometryId} to channel object.
         */
        private final Map<Long, EcalChannel> geometryMap = new HashMap<Long, EcalChannel>();

        /**
         * Add an <code>EcalChannel</code> to the collection and cache its ID information. The GeometryId must be
         * created later as it requires access to the Detector API.
         *
         * @param channel the ECAL channel object
         * @return <code>true</code> if object was added successfully
         */
        @Override
        public boolean add(final EcalChannel channel) throws ConditionsObjectException {
            super.add(channel);
            final DaqId daqId = channel.createDaqId();
            if (daqId.isValid()) {
                this.daqMap.put(daqId.encode(), channel);
            }
            final ChannelId channelId = channel.createChannelId();
            if (channelId.isValid()) {
                this.channelMap.put(channelId.encode(), channel);
            }
            return true;
        }

        /**
         * Build the map of {@link #GeometryId} objects.
         *
         * @param helper the ID helper of the subdetector
         * @param system the system ID of the subdetector
         */
        void buildGeometryMap(final IIdentifierHelper helper, final int system) {
            for (final EcalChannel channel : this) {
                final GeometryId geometryId = channel.createGeometryId(helper, system);
                this.geometryMap.put(geometryId.encode(), channel);
            }
        }

        /**
         * Find a channel by its channel ID.
         *
         * @param channelId the channel ID object
         * @return the matching channel or <code>null</code> if does not exist
         */
        public EcalChannel findChannel(final ChannelId channelId) {
            return this.channelMap.get(channelId.encode());
        }

        /**
         * Find a channel by using DAQ information.
         *
         * @param daqId the DAQ ID object
         * @return the matching channel or <code>null</code> if does not exist.
         */
        public EcalChannel findChannel(final DaqId daqId) {
            return this.daqMap.get(daqId.encode());
        }

        /**
         * Find a channel by using its physical ID information.
         *
         * @param geometryId the geometric ID object
         * @return the matching channel or <code>null</code> if does not exist
         */
        public EcalChannel findChannel(final GeometryId geometryId) {
            return this.geometryMap.get(geometryId.encode());
        }

        /**
         * Find a channel by its encoded channel ID.
         *
         * @param id the encoded channel ID
         * @return the matching channel or <code>null</code> if does not exist
         */
        public EcalChannel findChannel(final long id) {
            return this.channelMap.get(id);
        }

        /**
         * Find a channel by its encoded DAQ ID.
         *
         * @param id the encoded DAQ ID
         * @return the matching channel or <code>null</code> if does not exist
         */
        public EcalChannel findDaq(final long id) {
            return this.daqMap.get(id);
        }

        /**
         * Find a channel by its encoded geometric ID.
         *
         * @param id the encoded geometric ID
         * @return the matching channel or <code>null</code> if does not exist
         */
        public EcalChannel findGeometric(final long id) {
            return this.geometryMap.get(id);
        }

        /**
         * Sort collection and return but do not sort in place.
         *
         * @return the sorted copy of the collection
         */
        public ConditionsObjectCollection<EcalChannel> sorted() {
            return sorted(new ChannelIdComparator());
        }
    }

    /**
     * Customized converter for this object.
     */
    public static final class EcalChannelConverter extends AbstractConditionsObjectConverter<EcalChannelCollection> {

        /**
         * Create an {@link EcalChannel} collection.
         *
         * @param conditionsManager the conditions manager
         * @param name the name of the conditions data table
         * @return the collection of ECAL channel objects
         */
        @Override
        public EcalChannelCollection getData(final ConditionsManager conditionsManager, final String name) {
            final EcalChannelCollection collection = super.getData(conditionsManager, name);
            final Subdetector ecal = DatabaseConditionsManager.getInstance().getEcalSubdetector();
            if (ecal != null) {
                collection.buildGeometryMap(ecal.getDetectorElement().getIdentifierHelper(), ecal.getSystemID());
            }
            return collection;
        }

        /**
         * Get the type this converter handles.
         *
         * @return the type this converter handles
         */
        @Override
        public Class<EcalChannelCollection> getType() {
            return EcalChannelCollection.class;
        }
    }

    /**
     * The <code>GeometryId</code> contains the x and y indices of the crystal in the LCSIM-based geometry
     * representation.
     */
    public static final class GeometryId extends AbstractIdentifier {

        /**
         * The helper for using identifiers.
         */
        private final IIdentifierHelper helper;

        /**
         * The subdetector system ID.
         */
        private int system = -1;

        /**
         * The crystal's X index.
         */
        private int x = Integer.MAX_VALUE;

        /**
         * The crystal's Y index.
         */
        private int y = Integer.MAX_VALUE;

        /**
         * Create a geometry ID.
         *
         * @param helper the ID helper
         * @param values the list of values (order is system, x, y)
         */
        public GeometryId(final IIdentifierHelper helper, final int[] values) {
            this.helper = helper;
            this.system = values[0];
            this.x = values[1];
            this.y = values[2];
        }

        /**
         * Encode this ID as a long using the ID helper.
         *
         * @return The encoded long value.
         */
        @Override
        public long encode() {
            final IExpandedIdentifier expId = new ExpandedIdentifier(this.helper.getIdentifierDictionary()
                    .getNumberOfFields());
            expId.setValue(this.helper.getFieldIndex("system"), this.system);
            expId.setValue(this.helper.getFieldIndex("ix"), this.x);
            expId.setValue(this.helper.getFieldIndex("iy"), this.y);
            return this.helper.pack(expId).getValue();
        }

        /**
         * Return <code>true</code> if ID is valid
         *
         * @return <code>true</code> if ID is valid
         */
        @Override
        public boolean isValid() {
            return this.system != -1 && this.x != Integer.MAX_VALUE && this.y != Integer.MAX_VALUE;
        }
    }

    /**
     * Create a channel ID for this ECAL channel.
     *
     * @return The channel ID.
     */
    ChannelId createChannelId() {
        return new ChannelId(new int[] {this.getChannelId()});
    }

    /**
     * Create a {@link #DaqId} for this ECAL channel.
     *
     * @return the {@link #DaqId} for this ECAL channel
     */
    DaqId createDaqId() {
        return new DaqId(new int[] {getCrate(), getSlot(), getChannel()});
    }

    /**
     * Create a {@link #GeometryId} for this ECAL channel.
     *
     * @param helper the ID helper
     * @param system the subdetector system ID
     * @return the geometry ID
     */
    GeometryId createGeometryId(final IIdentifierHelper helper, final int system) {
        return new GeometryId(helper, new int[] {system, getX(), getY()});
    }

    /**
     * Implementation of equals.
     *
     * @param o the object to compare equality to
     * @return <code>true</code> if objects are equal
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof EcalChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        final EcalChannel c = (EcalChannel) o;
        return c.getChannelId() == getChannelId() && c.getCrate() == getCrate() && c.getSlot() == getSlot()
                && c.getChannel() == getChannel() && c.getX() == getX() && c.getY() == getY();
    }

    /**
     * Get the channel number of the channel.
     *
     * @return the channel number
     */
    @Field(names = {"channel"})
    public Integer getChannel() {
        return getFieldValue("channel");
    }

    /**
     * Get the ID of the channel.
     *
     * @return the ID of the channel
     */
    @Field(names = {"channel_id"})
    public Integer getChannelId() {
        return getFieldValue("channel_id");
    }

    /**
     * Get the crate number of the channel.
     *
     * @return the crate number
     */
    @Field(names = {"crate"})
    public Integer getCrate() {
        return getFieldValue("crate");
    }

    /**
     * Get the slot number of the channel.
     *
     * @return the slot number
     */
    @Field(names = {"slot"})
    public Integer getSlot() {
        return getFieldValue("slot");
    }

    /**
     * Get the x value of the channel.
     *
     * @return the x value
     */
    @Field(names = {"x"})
    public Integer getX() {
        return getFieldValue("x");
    }

    /**
     * Get the y value of the channel.
     *
     * @return the y value
     */
    @Field(names = {"y"})
    public Integer getY() {
        return getFieldValue("y");
    }
}
