package org.hps.conditions.hodoscope;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

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
        return this.getFieldValue("Hole");
    }

    @Field(names = {"name"})
    public String getName() {
        return this.getFieldValue("name");
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
        return c.getLayer() == this.getLayer() &&
                c.getX() == this.getX() &&
                c.getY() == this.getY() &&
                c.getTop() == this.getTop() &&
                c.getChannel() == this.getChannel() &
                c.getHole() == this.getHole();                                
    }
}
