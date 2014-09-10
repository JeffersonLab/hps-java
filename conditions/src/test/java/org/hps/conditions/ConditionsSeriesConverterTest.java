package org.hps.conditions;

import junit.framework.TestCase;

import org.hps.conditions.config.TestRunReadOnlyConfiguration;
import org.hps.conditions.svt.SvtBadChannel;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;


public class ConditionsSeriesConverterTest extends TestCase {
    
    DatabaseConditionsManager conditionsManager;

    public void setUp() {
        new TestRunReadOnlyConfiguration(true);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }
    
    public void testConditionsSeries() {
        
        ConditionsSeries<SvtBadChannelCollection> series = 
                conditionsManager.getConditionsSeries(TableConstants.SVT_BAD_CHANNELS);
        
        for (SvtBadChannelCollection collection : series) {
            System.out.println("collection " + collection.getCollectionId() + " has " + collection.getObjects().size() + " objects");
            System.out.println("conditions record ...");
            System.out.println(collection.getConditionsRecord().toString());
            for (SvtBadChannel badChannel : collection) {
                System.out.println("  channel #" + badChannel.getChannelId());
            }
        }
    }

}
