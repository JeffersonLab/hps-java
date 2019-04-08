package org.hps.conditions.hodoscope;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

@Table(names = {"hodo_channels"})
public final class HodoscopeChannel extends BaseConditionsObject {
    
    public static class HodoscopeChannelCollection extends BaseConditionsObjectCollection<HodoscopeChannel> {
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
}
