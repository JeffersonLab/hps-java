package org.hps.users.spaul;

import java.util.Date;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtMotorPosition;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.evio.SvtEventFlagger;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.HeadBankData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class RewriteFlagsDriver extends Driver {
   @Override 
    public void detectorChanged(Detector detector){
        initialize();
    }

    SvtBiasConstant.SvtBiasConstantCollection svtBiasConstants = null;
    boolean biasGood = false;
    @Override
    public void process(EventHeader event){
        Date eventDate = getEventTimeStamp(event);
        if (eventDate != null) {
            biasGood = false;
            if (svtBiasConstants != null) {
                SvtBiasConstant biasConstant = svtBiasConstants.find(eventDate);
                if (biasConstant != null) {
                    biasGood = true;
                }
            }

            
            
        }
        event.getIntegerParameters().put("svt_bias_good", new int[]{biasGood ? 1 : 0});
        
        
    }
    private Date getEventTimeStamp(EventHeader event) {
        long timestamp = event.getTimeStamp();
        
        
        List<GenericObject> intDataCollection = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject data : intDataCollection) {
            if (AbstractIntData.getTag(data) == HeadBankData.BANK_TAG) {
                Date date = HeadBankData.getDate(data);
                if (date != null) {
                    //System.out.printf("%d %d\n", date.getTime(),timestamp);
                    return date;
                }
            }
        }
        return null;
       // return new Date(timestamp/1000000 + 1457322909477L);
    }
    public void initialize() {
        try {
            svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
        } catch (Exception e) {
            svtBiasConstants = null;
        }
       

    }
}
