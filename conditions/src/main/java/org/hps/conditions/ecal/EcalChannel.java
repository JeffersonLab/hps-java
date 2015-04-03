package org.hps.conditions.ecal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.api.AbstractIdentifier;
import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.AbstractConditionsObjectConverter;
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
 * This class encapsulates all the information about a single ECal channel,
 * corresponding to one physical crystal in the detector.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"ecal_channels", "test_run_ecal_channels"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED, converter = EcalChannel.EcalChannelConverter.class)
public final class EcalChannel extends BaseConditionsObject {

    /**
     * Customized converter for this object.
     */
    public static final class EcalChannelConverter extends AbstractConditionsObjectConverter<EcalChannelCollection> {

        /**
         * Create an {@link EcalChannel} collection.
         * @param conditionsManager The conditions manager.
         * @param name The name of the conditions data table.
         * @return The collection of ECAL channel objects.
         */
        @Override
        public EcalChannelCollection getData(ConditionsManager conditionsManager, String name) {
            final EcalChannelCollection collection = super.getData(conditionsManager, name);
            final Subdetector ecal = DatabaseConditionsManager.getInstance().getEcalSubdetector();
            collection.buildGeometryMap(ecal.getDetectorElement().getIdentifierHelper(), ecal.getSystemID());
            return collection;
        }

        /**
         * Get the type this converter handles.
         * @return The type this converter handles.
         */
        @Override
        public Class<EcalChannelCollection> getType() {
            return EcalChannelCollection.class;
        }
    }
    
    /**
     * The <code>DaqId</code> is the combination of crate, slot and channel that
     * specify the channel's DAQ configuration.
     */
    public static final class DaqId extends AbstractIdentifier {

        /**
         * The DAQ crate number.
         */
        private int crate = -1;
        
        /**
         * The DAQ slot number. 
         */
        private int slot = -1;
        
        /**
         * The DAQ channel number.
         */
        private int channel = -1;

        /**
         * Create a DAQ ID from an array of values.
         * @param values The list of values (crate, slot, channel).
         */
        public DaqId(final int values[]) {
            crate = values[0];
            slot = values[1];
            channel = values[2];
        }

        /**
         * Encode this ID into a long value.
         * @return The encoded long value.
         */
        @Override
        public long encode() {
            // from Sho's code
            return (((long) crate) << 32) | ((long) slot << 16) | (long) channel;
        }

        /**
         * Check if the values look valid.
         * @return True if ID's values are valid.
         */
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
         * The helper for using identifiers.
         */
        private IIdentifierHelper helper;

        /**
         * Create a geometry ID.
         * @param helper The ID helper.
         * @param values The list of values (system, x, y).
         */
        public GeometryId(final IIdentifierHelper helper, final int[] values) {
            this.helper = helper;
            system = values[0];
            x = values[1];
            y = values[2];
        }

        /**
         * Encode this ID as a long using the ID helper.
         * @return The encoded long value.
         */
        @Override
        public long encode() {
            IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
            expId.setValue(helper.getFieldIndex("system"), system);
            expId.setValue(helper.getFieldIndex("ix"), x);
            expId.setValue(helper.getFieldIndex("iy"), y);
            return helper.pack(expId).getValue();
        }

        /**
         * True if ID's values look valid.
         * @return True if ID is valid.
         */
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

        /**
         * The channel ID value.
         */
        private int id = -1;

        /**
         * Create a channel ID.
         * @param values The values (size 0 with single int value).
         */
        public ChannelId(int[] values) {
            id = values[0];
        }

        /**
         * Encode as long value (just returns the int value).
         * @return The ID's value.
         */
        @Override
        public long encode() {
            return id;
        }

        /**
         * True if ID looks valid.
         * @return True if ID looks valid.
         */
        @Override
        public boolean isValid() {
            return id != -1;
        }
    }

    /**
     * Create a {@link DaqId} for this ECAL channel. 
     * @return The DAQ Id for this ECAL channel.
     */
    DaqId createDaqId() {
        return new DaqId(new int[] { getCrate(), getSlot(), getChannel() });
    }

    /**
     * Create a {@link GeometryId} for this ECAL channel.
     * @param helper The ID helper.
     * @param system The subdetector system ID.
     * @return The geometry ID.
     */
    GeometryId createGeometryId(IIdentifierHelper helper, int system) {
        return new GeometryId(helper, new int[] { system, getX(), getY() });
    }

    /**
     * Create a channel ID for this ECAL channel.
     * @return The channel ID.
     */
    ChannelId createChannelId() {
        return new ChannelId(new int[] { this.getChannelId() });
    }

    /**
     * A collection of {@link EcalChannel} objects.
     */
    public static class EcalChannelCollection extends BaseConditionsObjectCollection<EcalChannel> {

        /**
         * Map of {@link DaqId} to channel object.
         */
        private Map<Long, EcalChannel> daqMap = new HashMap<Long, EcalChannel>();
        
        /**
         * Map of {@link GeometryId} to channel object.
         */
        private Map<Long, EcalChannel> geometryMap = new HashMap<Long, EcalChannel>();
        
        /**
         * Map of {@link ChannelId} to channel object.
         */
        Map<Long, EcalChannel> channelMap = new HashMap<Long, EcalChannel>();

        /**
         * Add an <code>EcalChannel</code> to the collection and cache its ID
         * information. The GeometryId must be created later as it requires
         * access to the Detector API.
         * 
         * @param channel The ECAL channel object.
         * @return True if object was added successfully.
         */
        @Override
        public boolean add(final EcalChannel channel)  {
            super.add(channel);
            final DaqId daqId = channel.createDaqId();
            if (daqId.isValid()) {
                daqMap.put(daqId.encode(), channel);
            }
            final ChannelId channelId = channel.createChannelId();
            if (channelId.isValid()) {
                channelMap.put(channelId.encode(), channel);
            }
            return true;
        }

        /**
         * Build the map of {@link GeometryId} objects.
         * @param helper The identifier helper of the subdetector.
         * @param system The system ID of the subdetector.
         */
        void buildGeometryMap(final IIdentifierHelper helper, final int system) {
            for (EcalChannel channel : this) {
                GeometryId geometryId = channel.createGeometryId(helper, system);
                geometryMap.put(geometryId.encode(), channel);
            }
        }

        /**
         * Find a channel by using DAQ information.
         * @param daqId The DAQ ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(final DaqId daqId) {
            return daqMap.get(daqId.encode());
        }

        /**
         * Find a channel by using its physical ID information.
         * @param geometryId The geometric ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(final GeometryId geometryId) {
            return geometryMap.get(geometryId.encode());
        }

        /**
         * Find a channel by its channel ID.
         * @param channelId The channel ID object.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(final ChannelId channelId) {
            return channelMap.get(channelId.encode());
        }

        /**
         * Find a channel by its encoded geometric ID.
         * @param id The encoded geometric ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findGeometric(final long id) {
            return geometryMap.get(id);
        }

        /**
         * Find a channel by its encoded channel ID.
         * @param id The encoded channel ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findChannel(final long id) {
            return channelMap.get(id);
        }

        /**
         * Find a channel by its encoded DAQ ID.
         * @param id The encoded DAQ ID.
         * @return The matching channel or null if does not exist.
         */
        public EcalChannel findDaq(final long id) {
            return daqMap.get(id);
        }        

        /**
         * Sort collection and return but do not sort in place.
         * @return The sorted copy of the collection.
         */
        public BaseConditionsObjectCollection<EcalChannel> sorted() {
            return sorted(new ChannelIdComparator());
        }

        /**
         * Comparison of ECAL channel objects.
         */
        class ChannelIdComparator implements Comparator<EcalChannel> {
            /**
             * Compare two ECAL channel objects using their channel ID.
             * @param c1 The first object.
             * @param c2 The second object.
             * @return -1, 0, or 1 if first channel is less than, equal to or greater than second.
             */
            public int compare(EcalChannel c1, EcalChannel c2) {
                if (c1.getChannelId() < c2.getChannelId()) {
                    return -1;
                } else if (c1.getChannelId() > c2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Get the crate number of the channel.
     * @return The crate number.
     *
     */
    @Field(names = {"crate"})
    public int getCrate() {
        return getFieldValue("crate");
    }

    /**
     * Get the slot number of the channel.
     * @return The slot number.
     */
    @Field(names = {"slot"})
    public int getSlot() {
        return getFieldValue("slot");
    }

    /**
     * Get the channel number of the channel.
     * @return The channel number.
     */
    @Field(names = {"channel"})
    public int getChannel() {
        return getFieldValue("channel");
    }

    /**
     * Get the x value of the channel.
     * @return The x value.
     */
    @Field(names = {"x"})
    public int getX() {
        return getFieldValue("x");
    }

    /**
     * Get the y value of the channel.
     * @return The y value.
     */
    @Field(names = {"y"})
    public int getY() {
        return getFieldValue("y");
    }

    /**
     * Get the ID of the channel.
     * @return The ID of the channel.
     */
    @Field(names = {"channel_id"})
    public int getChannelId() {
        return getFieldValue("channel_id");
    }

    /**
     * Implementation of equals.
     * @param o The object to compare equality to.
     * @return True if objects are equal.
     */
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
}
