package org.hps.conditions.ecal;


import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.config.DevReadOnlyConfiguration;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;

public class EcalDaqTest  extends TestCase {
	   public void setUp() {
	        new DevReadOnlyConfiguration().setup().load("HPS-ECalCommissioning", 0);
	    }
	    
	    public void testEcalDAQ() {
	        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
	        EcalChannelCollection collection = manager.getConditionsData(EcalChannelCollection.class, TableConstants.ECAL_CHANNELS);
	        System.out.println("CH ID \t X \t Y \t crate \t slot \t number \t  ");
	        for (EcalChannel ch : collection) {
	            System.out.println(ch.getChannelId()+"\t"+ch.getX()+" \t"+ch.getY()+ "\t"+ ch.getCrate()+"\t"+ch.getSlot()+"\t"+ ch.getChannel());        
	      
	            System.out.println();
	        }
	    }
	
	
	
}
