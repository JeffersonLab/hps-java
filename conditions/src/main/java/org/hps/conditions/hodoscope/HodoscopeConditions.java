package org.hps.conditions.hodoscope;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;

public class HodoscopeConditions {

    private final Map<HodoscopeChannel, HodoscopeChannelConstants> channelConstants = 
            new HashMap<HodoscopeChannel, HodoscopeChannelConstants>();
    
    //private HodoscopeChannelCollection channels = null;
    private HodoscopeChannelCollection channels = new HodoscopeChannelCollection();
    
    void setChannelCollection(HodoscopeChannelCollection channels) {
        this.channels = channels;
        
        // Build channel map.
        for (HodoscopeChannel channel : this.channels) {
            channelConstants.put(channel, new HodoscopeChannelConstants());
        }
    }
    
    public HodoscopeChannelConstants getChannelConstants(HodoscopeChannel channel) {
        return channelConstants.get(channel);
    }   
    
    
    public HodoscopeChannelCollection getChannels() {
        return channels;
    }
}
