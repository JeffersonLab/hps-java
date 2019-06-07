package org.hps.conditions.hodoscope;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;

public class HodoscopeConditions {

    /**
     * The collection of {@link EcalChannel} objects.
     */
    private HodoscopeChannelCollection channelCollection = new HodoscopeChannelCollection();

    private final Map<HodoscopeChannel, HodoscopeChannelConstants> channelConstants
            = new HashMap<HodoscopeChannel, HodoscopeChannelConstants>();

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

    public HodoscopeChannelCollection getChannelCollection() {
        return this.channelCollection;
    }
    
    public HodoscopeChannelCollection getChannels() {
//        System.out.println(" ==================== " + (new Throwable()).getStackTrace()[0].toString());
//        System.out.println("Channels are " + channels);
        
        return channels;
    }
}
