/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.EvioEvent;
//import org.jlab.coda.jevio.EvioException;

import org.lcsim.event.EventHeader;

/**
 *
 * @author rafopar
 */
public class VTPEvioReader extends EvioReader {

    private static final int topRocID = 60011;
    private static final int botRocID = 60012;

    public VTPEvioReader() {
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        boolean foundVTP = false;

        for (BaseStructure bank : event.getChildrenList()) {
            BaseStructureHeader header = bank.getHeader();
            int rocID = header.getTag();

            if (rocID == topRocID || rocID == botRocID) {

                System.out.println("rocID = " + rocID);
                System.out.println("Children count is " + bank.getChildCount());

                for (BaseStructure childBank : bank.getChildrenList()) {

                    System.out.println("Hashcode = " + childBank.hashCode());
                    System.out.println("To String =" + childBank.toString());

                    System.out.println("childBank Header Tag = " + childBank.getHeader().getTag() );

                    int[] vals = childBank.getIntData();

                    System.out.println("Length of vals " + vals.length);

                    for (int ind = 0; ind < vals.length; ind++) {
                        System.out.println("Value [" + ind + "] = " + vals[ind]);
                    }
                }

                foundVTP = true;
            }
        }

        return foundVTP;
    }

}
