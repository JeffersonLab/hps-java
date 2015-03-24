package org.hps.record.epics;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is an EVIO event processor that will read EPICS events (event tag 31) 
 * and turn them into {@link EpicsScalarData} objects.
 *   
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EpicsEvioProcessor extends EvioEventProcessor {

    EpicsScalarData data;
    
    public void process(EvioEvent evio) {
        
        if (evio.getHeader().getTag() != EvioEventConstants.EPICS_EVENT_TAG) {
            // Just silently skip these events because otherwise too many error messages might print.
            return;
        }

        // Find the bank with the EPICS information.
        BaseStructure epicsBank = null;
        BaseStructure topBank = evio.getChildrenList().get(0);
        for (BaseStructure childBank : topBank.getChildrenList()) {
            if (childBank.getHeader().getTag() == EvioEventConstants.EPICS_BANK_TAG) {
                epicsBank = childBank;
                break;
            }
        }

        if (epicsBank != null) {
            String epicsData = epicsBank.getStringData()[0]; 
            data = new EpicsScalarData();
            data.fromString(epicsData);
            
            //System.out.println("found EVIO data bank ...");
            //System.out.println(data.toString());
        } 
    }
    
    public EpicsScalarData getEpicsScalarData() {
        return data;
    }
}