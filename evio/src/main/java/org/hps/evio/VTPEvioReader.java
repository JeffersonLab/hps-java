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

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        boolean foundVTP = false;

        for (BaseStructure bank : event.getChildrenList()) {
            BaseStructureHeader header = bank.getHeader();
            int rocID = header.getTag();
            
            System.out.println("rocID = " + rocID);
        }


        return foundVTP;
    }

}
