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
 * This code reads TS banks from the EVIO, and use the 5th word (trigger
 * pattern) to extract trigger flags which are written as a genericObject. In
 * GenericObject there is an integer arrays. Each element of the array
 * corresponds a trigger flag.
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
        int[] trigger_flags = new int[] {}; // This array contains trigger flags for the given event


        
        // ======== Defining GenericObjects for an integer array of trigger flags
        TSGenericObject ts_generic = new TSGenericObject();

        // ====== In order to write generic object in the file, 1st we should add
        // generic objects
        // ====== in the List, so we will add above genericObjects into the list below
        List<TSGenericObject> ts_list = new ArrayList();

        // ===== Looping over all banks in the EVIO
        for (BaseStructure bank : event.getChildrenList()) {
            BaseStructureHeader header = bank.getHeader();
            int rocID = header.getTag(); // getting the tag of the bnk

            // We want to select banks with tag=TS_RocID
            if (rocID == EvioEventConstants.TS_RocID) {

                for (BaseStructure childBank : bank.getChildrenList()) {

                    // ==== Althought all the subbanks here should be TS, it is worth to check, if
                    // ==== the Bank tag is TS Bank tag

                    if (childBank.getHeader().getTag() == EvioEventConstants.TS_BANK_TAG) {
                        int[] vals = childBank.getIntData();

                        ArrayList<Integer> list = new ArrayList<Integer>();
                        // the trigger pattern is in the 5th word
                        for (int i = 0; i < 32; i++) {
                            if (vals[4] % 2 == 1) {
                                list.add(i);
                            }
                            vals[4] = vals[4] / 2;
                        }
                        Integer[] array = (Integer[]) list.toArray(new Integer[0]);
                        int[] triggers = new int[array.length];
                        for (int i = 0; i < array.length; i++) {
                            triggers[i] = array[i].intValue();
                        }
                        trigger_flags = ArrayUtils.addAll(trigger_flags, triggers);
                    }
                }

                foundTS = true;
            }
        }

        // Filling generic objects with corresponding arrays of Integers
        ts_generic.setValues(trigger_flags);

        // Adding generic object to the list
        ts_list.add(ts_generic);

        // Writing TS data into the event
        lcsimEvent.put("TSBank", ts_list, TSGenericObject.class, 0);
        return foundTS;

    }

}
