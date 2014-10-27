package org.hps.conditions.svt;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;

public final class TestRunSvtConditionsConverter extends AbstractSvtConditionsConverter<TestRunSvtConditions> {

   
    public TestRunSvtConditionsConverter(){
        this.conditions = new TestRunSvtConditions(); 
    }

    /**
     * Create and return an {@link TestRunSvtConditions} object
     * 
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    @Override
    public TestRunSvtConditions getData(ConditionsManager manager, String name){
        
        DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;
       
    	// Get the channel map from the conditions database
        TestRunSvtChannelCollection channels = this.getCollection(TestRunSvtChannelCollection.class, dbConditionsManager);

        //System.out.println("Test run channels: " + channels.toString());
        
        // Create the SVT conditions object to use to encapsulate SVT condition collections
        conditions.setChannelMap(channels);
        
        // Get the DAQ map from the conditions database
        TestRunSvtDaqMappingCollection daqMap = this.getCollection(TestRunSvtDaqMappingCollection.class, dbConditionsManager);
        conditions.setDaqMap(daqMap);
        
        conditions = super.getData(manager, name);

        return conditions;
    }
   
   
    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
	@Override
	public Class<TestRunSvtConditions> getType() {
		return TestRunSvtConditions.class;
	}
}
