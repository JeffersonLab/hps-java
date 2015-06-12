package org.hps.users.celentan;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.lcsim.event.GenericObject;

public class DummyDriverRaw extends Driver {
    @Override
    public void detectorChanged(Detector detector) {
        System.out.println("Ecal event display detector changed");
    }
    
    @Override
    public void process(EventHeader event) {
        double orTrigTime,topTrigTime,botTrigTime;
        System.out.println("1");
        if(event.hasCollection(GenericObject.class, "TriggerBank")) {
            System.out.println("2");
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                System.out.println("3");
                GenericObject triggerData = triggerList.get(0);
                if(triggerData instanceof SSPData){
                    // TODO: TOP, BOTTOM, OR, and AND triggers were only
                    // used by the test run data and are not supported by
                    // SSP data any longer.
                    System.out.println("4");
                    orTrigTime  = 0; //((SSPData)triggerData).getOrTrig();
                    topTrigTime = 0; //((SSPData)triggerData).getTopTrig();
                    botTrigTime = 0; //((SSPData)triggerData).getBotTrig();      
                    System.out.println(orTrigTime + " " + topTrigTime + " " + botTrigTime);
                }
                else if (AbstractIntData.getTag(triggerData)==SSPData.BANK_TAG){
                    // TODO: TOP, BOTTOM, OR, and AND triggers were only
                    // used by the test run data and are not supported by
                    // SSP data any longer.
                    //SSPData mData=new SSPData(triggerData);
                    orTrigTime  = 0; //(mData).getOrTrig();
                    topTrigTime = 0; //(mData).getTopTrig();
                    botTrigTime = 0; //(mData).getBotTrig();      
                    System.out.println(orTrigTime + " " + topTrigTime + " " + botTrigTime);
                }
            }//end if triggerList isEmpty
        }
    }
}
