/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.jevio.EvioEvent;
//import org.jlab.coda.jevio.EvioException;

import org.lcsim.event.EventHeader;

/**
 *
 * This code reads VTP banks from the EVIO, and writes it's values as a genericObject.
 * In GenericObject there are two arrays. One represents Trigger words, and
 * the other represents rocIDs. In particular we want to know the rocID of i-th  VTP word,
 * the we should use the i-th value of the array "rocID_vals" (see description in the code).
 * 
 * 
 * 
 * @author rafopar
 */
public class VTPEvioReader extends EvioReader {

    public VTPEvioReader() {
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        boolean foundVTP = false;
        int[] vtp_vals = new int[]{};           // This array contains VTP words (as integer) for the given event
        int[] rocID_vals = new int[]{};         // This array contains rocIDs for the same index VTP word

        // ======== Defining GenericObjects for words, and rocIDs
        VTPGenericObject vtp_generic = new VTPGenericObject();
        VTPGenericObject vtp_generic_rocid = new VTPGenericObject();
        
        
        // ====== In order to write generic object in the file, 1st we should add generic objects
        // ====== in the List, so we will add above genericObjects into the list below
        List<VTPGenericObject> vtp_list = new ArrayList();

        
        // ===== Looping over all banks in the EVIO
        for (BaseStructure bank : event.getChildrenList()) {
            BaseStructureHeader header = bank.getHeader();
            int rocID = header.getTag();  // getting the tag of the bnk
            
            // We want to select banks with tag=rocID_Top or rocID_Bot===== As of Jun-3-2019, these rocIDs are
            // 60011 (Top) and 60012 (Bot)
            if (rocID == EvioEventConstants.VTP_TOP_RocID || rocID == EvioEventConstants.VTP_BOT_RocID ) {

                for (BaseStructure childBank : bank.getChildrenList()) {

                    // ==== Althought all the subbanks here should be VTP, it is worth to check, if
                    // ==== the Bank tag is VTP Bank tag
                    
                    if (childBank.getHeader().getTag() == EvioEventConstants.VTP_BANK_TAG) {
                        int[] vals = childBank.getIntData();

                        // Concatinating arrays "vtp_vals" to the array from this bank
                        vtp_vals = ArrayUtils.addAll(vtp_vals, vals);
                        
                        int len_vals = vals.length;
                        
                        // ==== A temporary array which will contin rocIDs, which by the way has same values for all
                        // ==== elements in banks under the same rocID
                        int[] rocid_values = new int[len_vals];
                        
                        for( int i_val = 0 ; i_val < len_vals; i_val++ ){
                            rocid_values[i_val] = rocID;
                        }
                        
                        // Concatinating array rocID_vals with the rocid_values (which represents rocIDs only for the given bank)
                        rocID_vals = ArrayUtils.addAll(rocID_vals, rocid_values);
                    }
                }

                foundVTP = true;
            }
        }


        // Filling generic objects with corresponding arrays of Integers
        vtp_generic.setValues(vtp_vals);
        vtp_generic_rocid.setValues(rocID_vals);
        
        // Adding generic object to the list
        vtp_list.add(vtp_generic);
        vtp_list.add(vtp_generic_rocid);
        
        // Writing VTP data into the event
        lcsimEvent.put("VTPBank", vtp_list, VTPGenericObject.class, 0);
        
        return foundVTP;
    }

}
