package org.hps.evio.epics;

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

    public void process(EvioEvent evio) {
        
        if (evio.getHeader().getTag() != EvioEventConstants.EPICS_EVENT_TAG) {
            // Just silently skip these events because otherwise too many error messages might print.
            return;
        }

        System.out.println("Epics EVIO event " + evio.getEventNumber());
        System.out.println("Dumping EPICS event ...");
        System.out.println(evio.toXML());

        // Find the bank with the EPICS information.
        BaseStructure epicsBank = null;
        BaseStructure topBank = evio.getChildrenList().get(0);
        System.out.println("got top bank: " + topBank.getHeader().getTag());
        for (BaseStructure childBank : topBank.getChildrenList()) {
            System.out.println("found child bank tag: " + childBank.getHeader().getTag());
            if (childBank.getHeader().getTag() == EvioEventConstants.EPICS_BANK_TAG) {
                System.out.println("found EPICS bank tag: " + childBank.getHeader().getTag());
                epicsBank = childBank;
                break;
            }
        }

        if (epicsBank != null) {
            System.out.println("found EPICS bank with tag " + epicsBank.getHeader().getTag());
            String epicsData = epicsBank.getStringData()[0];
            System.out.println("dumping EPICS string data ...");
            System.out.println(epicsData);

            EpicsScalarData data = new EpicsScalarData();
            data.fromString(epicsData);

            System.out.println("parsed EPICS data ...");
            System.out.println(data.toString());
        } else {
            System.out.println("did not find EPICS data bank in event");
        }
    }
}