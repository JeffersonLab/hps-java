/**
 * 
 */
package org.hps.analysis.trigger.util;

import java.util.Date;
import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.HeadBankData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * Class with only static utility methods.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class TriggerDataUtils {

    public static Date getEventTimeStamp(EventHeader event, String collectionName) {
        List<GenericObject> intDataCollection = event.get(GenericObject.class, collectionName);
        for (GenericObject data : intDataCollection) {
            if (AbstractIntData.getTag(data) == HeadBankData.BANK_TAG) {
                Date date = HeadBankData.getDate(data);
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    
    /**
     * Private constructor to avoid instantiation of the 
     */
    private TriggerDataUtils() {}

}
