package org.hps.conditions.hodoscope;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

import junit.framework.TestCase;

public class HodoscopeConditionsTest extends TestCase {
    
    private static final String DETECTOR = "HPS-HodoscopeTest-v1";
    private static final int RUN = 1000000;
    
    public void testHodoscopeConditions() {
        
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        try {
            mgr.setDetector(DETECTOR, RUN);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        final HodoscopeChannelCollection channels = 
                mgr.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        System.out.println("Printing Hodoscope channels ...");
        for (HodoscopeChannel channel : channels) {
            System.out.println(channel);
        }
        
    }

}
