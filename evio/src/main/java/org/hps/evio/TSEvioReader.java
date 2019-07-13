package org.hps.evio;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 *
 * This code reads TS banks from the EVIO, and write its header tag and its
 * values into a GenericObject. In the GenericObject, the header tag is stored
 * as the first integer, and contents of the bank are the remaining integer
 * primitives.
 *
 * 
 * 
 * 
 * @author tongtong
 */
public class TSEvioReader extends EvioReader {
    public TSEvioReader() {
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {
        boolean foundTS = false;
        int[] ts = new int[] {}; // This array contains tag and data for TS bank of the given event

        // ======== Defining GenericObjects for an integer array
        TSGenericObject ts_generic = new TSGenericObject();

        // ====== In order to write generic object in the file, 1st we should add
        // generic objects
        // ====== in the List, so we will add above genericObjects into the list below
        List<TSGenericObject> ts_list = new ArrayList();

        // ===== Looping over all banks in the EVIO
        for (BaseStructure bank : event.getChildrenList()) {
            BaseStructureHeader header = bank.getHeader();
            int rocID = header.getTag(); // getting the tag of the bnk
            // We want to select banks with tag = TS_RocID
            if (rocID == EvioEventConstants.TS_RocID) {
                for (BaseStructure childBank : bank.getChildrenList()) {
                    // check if the Bank tag is TS Bank tag
                    if (childBank.getHeader().getTag() == EvioEventConstants.TS_BANK_TAG) {
                        ts = ArrayUtils.addAll(ts, EvioEventConstants.TS_BANK_TAG);
                        int[] vals = childBank.getIntData();
                        ts = ArrayUtils.addAll(ts, vals);
                        foundTS = true;
                        break;                                                  // No need to look further, we found the bank.
                    }
                }

                break;                                                         // There is only one TS_RocID bank per event. If we found it, we are done.
            }
        }

        // Filling the generic objects with the integer array
        ts_generic.setValues(ts);
        
        // Adding the generic object to the list
        ts_list.add(ts_generic);

        // Writing TS data into the event
        lcsimEvent.put("TSBank", ts_list, TSGenericObject.class, 0);
        return foundTS;

    }

}
