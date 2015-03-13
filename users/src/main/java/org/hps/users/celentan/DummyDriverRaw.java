package org.hps.users.celentan;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

public class DummyDriverRaw extends Driver{

	  private Detector detector;



@Override
public void detectorChanged(Detector detector) {
	System.out.println("Ecal event display detector changed");
    this.detector = detector;
    
}




@Override
public void process(EventHeader event){
	
	double orTrigTime,topTrigTime,botTrigTime;
	System.out.println("1");
    if (event.hasCollection(GenericObject.class, "TriggerBank")) {
    	System.out.println("2");
    	List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
        if (!triggerList.isEmpty()) {
        	System.out.println("3");
            GenericObject triggerData = triggerList.get(0);
            if (triggerData instanceof SSPData){ 
            	System.out.println("4");
            	orTrigTime=((SSPData)triggerData).getOrTrig();
            	topTrigTime=((SSPData)triggerData).getTopTrig();
            	botTrigTime =((SSPData)triggerData).getBotTrig();      
            	System.out.println(orTrigTime+" "+topTrigTime+" "+botTrigTime);
            }         
            else if (AbstractIntData.getTag(triggerData)==SSPData.BANK_TAG){
            	SSPData mData=new SSPData(triggerData);
            	orTrigTime=(mData).getOrTrig();
            	topTrigTime=(mData).getTopTrig();
            	botTrigTime =(mData).getBotTrig();      
            	System.out.println(orTrigTime+" "+topTrigTime+" "+botTrigTime);
            }
        }//end if triggerList isEmpty
	
}

}
}
