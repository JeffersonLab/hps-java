package org.hps.conditions.hodoscope;

import java.util.Comparator;

import org.hps.conditions.api.AbstractIdentifier;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;


@Table(names = {"hodo_channels"})
public final class HodoscopeChannel extends BaseConditionsObject {
    
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
    }
    
    @Field(names = {"channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("channel_id");
    }
     
    @Field(names = {"layer"})
    public Integer getLayer() {
        return this.getFieldValue("layer");
    }
    
    @Field(names = {"x"})
    public Integer getX() {
        return this.getFieldValue("x");
    }
    
    @Field(names = {"y"})
    public Integer getY() {
        return this.getFieldValue("y");
    }
    
    @Field(names = {"top"})
    public Integer getTop() {
        return this.getFieldValue("top");
    }
    
    @Field(names = {"channel"})
    public Integer getChannel() {
        return this.getFieldValue("channel");
    }

    @Field(names = {"hole"})
    public Integer getHole() {
        return this.getFieldValue("hole");
    }

    @Field(names = {"name"})
    public String getName() {
        return this.getFieldValue("name");
    }
    
    @Field(names = {"crate"})
    public Integer getCrate() {
        return this.getFieldValue("crate");
    }
    
    @Field(names = {"slot"})
    public Integer getSlot() {
        return this.getFieldValue("slot");
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
        if (!(o instanceof HodoscopeChannel)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        final HodoscopeChannel c = (HodoscopeChannel) o;
        // FIXME: This is overkill!
        return c.getLayer() == this.getLayer() &&
                c.getX() == this.getX() &&
                c.getY() == this.getY() &&
                c.getTop() == this.getTop() &&
                c.getChannel() == this.getChannel() &&
                c.getHole() == this.getHole() &&
                c.getCrate() == this.getCrate() &&
                c.getSlot() == this.getSlot();                                
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
            
            System.out.println("helper.toString " + this.helper.toString());
            System.out.println("==== Kuku1 ==== " + this.helper.hasField("system"));
            expId.setValue(this.helper.getFieldIndex("system"), this.system);
            System.out.println("==== Kuku2 ==== " + this.helper.hasField("ix"));
            expId.setValue(this.helper.getFieldIndex("ix"), this.x);
            System.out.println("==== Kuku3 ==== " + this.helper.hasField("iy"));
            expId.setValue(this.helper.getFieldIndex("iy"), this.y);
            System.out.println("==== Kuku4 ==== " + this.helper.hasField("layer"));
            expId.setValue(this.helper.getFieldIndex("layer"), this.layer);
            System.out.println("==== Kuku5 ==== " + this.helper.hasField("hole"));
//            //this.helper.hasField("hole");
//            expId.setValue(this.helper.getFieldIndex("hole"), this.hole);
//            System.out.println("==== Kuku6 ==== " + this.helper.hasField("hole"));            
//            
//            System.out.println("Kuku in the method encode");
            
            
            
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
