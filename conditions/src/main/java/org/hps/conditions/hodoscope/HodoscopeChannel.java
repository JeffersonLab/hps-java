package org.hps.conditions.hodoscope;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.conditions.api.AbstractIdentifier;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.AbstractConditionsObjectConverter;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

@Table(names = {"hodo_channels"})
@Converter(converter = HodoscopeChannel.HodoscopeChannelConverter.class)
public final class HodoscopeChannel extends BaseConditionsObject {

    private static Logger LOGGER = Logger.getLogger(HodoscopeChannel.class.getPackage().getName());

    /**
     * Specifies a scintillator that is positioned on the top half of the
     * hodoscope.
     */
    public static final int TOP = 1;
    /**
     * Specifies a scintillator that is positioned on the bottom half of the
     * hodoscope.
     */
    public static final int BOTTOM = -1;
    /**
     * Specifies a scintillator that is positioned on the first hodoscope layer
     * - i.e. the layer closest to the target.
     */
    public static final int LAYER_1 = 0;
    /**
     * Specifies a scintillator that is positioned on the second hodoscope layer
     * - i.e. the layer farthest from the target.
     */
    public static final int LAYER_2 = 1;
    /**
     * Specifies the scintillator fiber optic bundle hole that is closest to the
     * calorimeter center.
     */
    public static final int HOLE_LOW_X = -1;
    /**
     * Specifies the scintillator fiber optic bundle hole that is closest to the
     * positron-side of the calorimeter.
     */
    public static final int HOLE_HIGH_X = 1;
    /**
     * Specifies that a scintillator has only one fiber optic bundle hole.
     */
    public static final int HOLE_NULL = 0;

    public static class HodoscopeChannelCollection extends BaseConditionsObjectCollection<HodoscopeChannel> {

        class ChannelIdComparator implements Comparator<HodoscopeChannel> {

            public int compare(final HodoscopeChannel c1, final HodoscopeChannel c2) {
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
        private final Map<Long, HodoscopeChannel> channelMap = new HashMap<Long, HodoscopeChannel>();

        /**
         * Map of {@link #DaqId} to channel object.
         */
        private final Map<Long, HodoscopeChannel> daqMap = new HashMap<Long, HodoscopeChannel>();

        /**
         * Map of {@link #GeometryId} to channel object.
         */
        private final Map<Long, HodoscopeChannel> geometryMap = new HashMap<Long, HodoscopeChannel>();

        /**
         * Build the map of {@link #GeometryId} objects.
         *
         * @param helper the ID helper of the subdetector
         * @param system the system ID of the subdetector
         */
        void buildGeometryMap(final IIdentifierHelper helper, final int system) {
            for (final HodoscopeChannel channel : this) {
                final GeometryId geometryId = channel.createGeometryId(helper, system);
                this.geometryMap.put(geometryId.encode(), channel);
            }
        }

        public HodoscopeChannel findChannel(int channelId) {
            HodoscopeChannel foundIt = null;
            for (HodoscopeChannel channel : this) {
                if (channel.getChannelId() == channelId) {
                    foundIt = channel;
                    break;
                }
            }
            return foundIt;
        }

        // TODO: Implement a fast lookup method using bit-packed DAQ ID similar to EcalChannel.
        public HodoscopeChannel findChannel(int crate, int slot, int channel) {
            HodoscopeChannel foundIt = null;
            for (HodoscopeChannel c : this) {
                if (c.getCrate() == crate && c.getSlot() == slot && c.getChannel() == channel) {
                    foundIt = c;
                    break;
                }
            }
            return foundIt;
        }

        public HodoscopeChannel findChannel(int ix, int iy, int layer, int hole) {
            HodoscopeChannel foundIt = null;
            
            for (HodoscopeChannel c : this) {
                if( c.getIX() == ix && c.getIY() == iy && c.getLayer() == layer && c.getHole() == hole ){
                    foundIt = c;
                    break;
                }
            }
            
            return foundIt;
        }

        /**
         * Find a channel by its encoded geometric ID.
         *
         * @param id the encoded geometric ID
         * @return the matching channel or <code>null</code> if does not exist
         */
        public HodoscopeChannel findGeometric(final long id) {
            return this.geometryMap.get(id);
        }

    }

    /**
     * Create a {@link #GeometryId} for this Hodo channel.
     *
     * @param helper the ID helper
     * @param system the subdetector system ID
     * @return the geometry ID
     */
    GeometryId createGeometryId(final IIdentifierHelper helper, final int system) {
        return new GeometryId(helper, new int[]{system, this.getIX(), this.getIY(), this.getLayer(), this.getHole()});
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof HodoscopeChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        final HodoscopeChannel c = (HodoscopeChannel) o;
        // FIXME: This is overkill!
        return c.getLayer() == this.getLayer()
                && c.getIX() == this.getIX()
                && c.getIY() == this.getIY()
                && c.getTop() == this.getTop()
                && c.getChannel() == this.getChannel()
                && c.getHole() == this.getHole()
                && c.getCrate() == this.getCrate()
                && c.getSlot() == this.getSlot();
    }

    /**
     * Indicates whether or not this scintillator is located on the bottom half
     * of the hodoscope.
     *
     * @return Returns <code>true</code> if the scintillator is on the bottom
     * half and <code>false</code> otherwise.
     */
    public boolean isBottom() {
        return getIY().intValue() == BOTTOM;
    }

    /**
     * Indicates whether or not this scintillator is located on layer 1 of the
     * hodoscope.
     *
     * @return Returns <code>true</code> if the scintillator is on layer 1 and
     * <code>false</code> otherwise.
     */
    public boolean isLayer1() {
        return getLayer().intValue() == LAYER_1;
    }

    /**
     * Indicates whether or not this scintillator is located on layer 2 of the
     * hodoscope.
     *
     * @return Returns <code>true</code> if the scintillator is on layer 2 and
     * <code>false</code> otherwise.
     */
    public boolean isLayer2() {
        return getLayer().intValue() == LAYER_2;
    }

    /**
     * Indicates whether or not this scintillator channel belongs to a
     * scintillator with multiple FADC channel.
     *
     * @return Returns <code>true</code> if the channel belongs to a
     * multi-channel scintillator and otherwise returns <code>false</code>.
     */
    public boolean isMultiHoleScintillator() {
        return getHole().intValue() == HOLE_NULL;
    }

    /**
     * Indicates whether or not this scintillator channel belongs to a
     * scintillator with only one FADC channel.
     *
     * @return Returns <code>true</code> if the channel belongs to a
     * single-channel scintillator and otherwise returns <code>false</code>.
     */
    public boolean isSingleHoleScintillator() {
        return getHole().intValue() == HOLE_NULL;
    }

    /**
     * Indicates whether or not this scintillator is located on the top half of
     * the hodoscope.
     *
     * @return Returns <code>true</code> if the scintillator is on the top half
     * and <code>false</code> otherwise.
     */
    public boolean isTop() {
        return getIY().intValue() == TOP;
    }

    @Field(names = {"channel"})
    public Integer getChannel() {
        return this.getFieldValue("channel");
    }

    @Field(names = {"channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("channel_id");
    }

    @Field(names = {"crate"})
    public Integer getCrate() {
        return this.getFieldValue("crate");
    }

    /**
     * Gets the hole number for the channel. For scintillators that only have a
     * single hole, this will be <code>0</code>. It will otherwise be either
     * <code>-1</code> or <code>1</code>.
     * <br/><br/>
     * Static integer representation of the allowed values can be found in {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#HOLE_LOW_X
     * HOLE_LOW_X}, {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#HOLE_HIGH_X
     * HOLE_HIGH_X}, and {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#HOLE_NULL
     * HOLE_NULL}.
     *
     * @return Returns the hole number for the scintillator.
     */
    @Field(names = {"hole"})
    public Integer getHole() {
        return this.getFieldValue("hole");
    }

    /**
     * Gets the x-index for the scintllator. This value ranges from
     * <code>0</code> to <code>4</code>, with higher values representing
     * scintillators closer to the positron-side of the calorimeter.
     *
     * @return Returns the x-index as an <code>int</code>.
     */
    @Field(names = {"x"})
    public Integer getIX() {
        return this.getFieldValue("x");
    }

    /**
     * Gets the y-index for the scintllator. This value is either
     * <code>-1</code> or <code>1</code>, with the former indicating the bottom
     * half of the hodoscope and the latter indicating the top half of the
     * hodoscope.
     * <br/><br/>
     * Static integer representation of the allowed values can be found in {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#TOP TOP} and null null     {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#BOTTOM BOTTOM}.
     *
     * @return Returns the y-index as an <code>int</code>.
     */
    @Field(names = {"y"})
    public Integer getIY() {
        return this.getFieldValue("y");
    }

    /**
     * Gets the layer number (also referred to as z-index) of the scintillator.
     * This value is either <code>0</code> for layer 1 or <code>1</code> for
     * layer 2. Static integer representation of the allowed values can be found
     * in {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#LAYER_1 LAYER_1} and {@link
     * org.hps.conditions.hodoscope.HodoscopeChannel#LAYER_2
     * LAYER_2}.
     *
     * @return Returns the hodoscope layer.
     */
    @Field(names = {"layer"})
    public Integer getLayer() {
        return this.getFieldValue("layer");
    }

    @Field(names = {"name"})
    public String getName() {
        return this.getFieldValue("name");
    }

    @Field(names = {"slot"})
    public Integer getSlot() {
        return this.getFieldValue("slot");
    }

    /**
     * Specifies whether or not the scintillator is positioned on the top half
     * of the calorimeter or not. A value of <code>0</code> means that it is
     * not, and a value of <code>1</code> means that it is.
     *
     * @return Returns either <code>0</code> for <code>false</code> or
     * <code>1</code> for <code>true</code>.
     */
    @Field(names = {"top"})
    public Integer getTop() {
        return this.getFieldValue("top");
    }

    /**
     * Customized converter for this object.
     */
    public static final class HodoscopeChannelConverter extends AbstractConditionsObjectConverter<HodoscopeChannelCollection> {

        /**
         * Create an {@link EcalChannel} collection.
         *
         * @param conditionsManager the conditions manager
         * @param name the name of the conditions data table
         * @return the collection of ECAL channel objects
         */
        @Override
        public HodoscopeChannelCollection getData(final ConditionsManager conditionsManager, final String name) {
            final HodoscopeChannelCollection collection = super.getData(conditionsManager, name);
            DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
            // FIXME: Ugly method call to get Ecal subdetector object!
            final Subdetector hodo
                    = mgr.getCachedConditions(Detector.class, "compact.xml").getCachedData().getSubdetector("Hodoscope");
            if (hodo != null) {
                if (hodo.getDetectorElement() != null) {
                    collection.buildGeometryMap(hodo.getDetectorElement().getIdentifierHelper(), hodo.getSystemID());
                } else {
                    // This can happen when not running with the detector-framework jar in the classpath.
                    throw new IllegalStateException("The ECal subdetector's detector element is not setup.");
                }
            } else {
                // FIXME: Probably this should be a fatal error.
                LOGGER.warning("ECal subdetector is not accessible so geometry map was not initialized.");
            }
            return collection;
        }

        /**
         * Get the type this converter handles.
         *
         * @return the type this converter handles
         */
        @Override
        public Class<HodoscopeChannelCollection> getType() {
            return HodoscopeChannelCollection.class;
        }
    }

    /**
     * The <code>GeometryId</code> contains the x and y indices of the crystal
     * in the LCSIM-based geometry representation.
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
         * The Tiles's X index.
         */
        private int x = Integer.MAX_VALUE;

        /**
         * The Tile's Y index.
         */
        private int y = Integer.MAX_VALUE;

        /**
         * The Layer.
         */
        private int layer = Integer.MAX_VALUE;

        /**
         * The Hole.
         */
        private int hole = Integer.MAX_VALUE;

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
            this.layer = values[3];
            this.hole = values[4];
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
            expId.setValue(this.helper.getFieldIndex("barrel"), 1); // Hard-coded flag to fix ID matching. --JM
            expId.setValue(this.helper.getFieldIndex("ix"), this.x);
            expId.setValue(this.helper.getFieldIndex("iy"), this.y);
            expId.setValue(this.helper.getFieldIndex("layer"), this.layer);
            expId.setValue(this.helper.getFieldIndex("hole"), this.hole);

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

}
