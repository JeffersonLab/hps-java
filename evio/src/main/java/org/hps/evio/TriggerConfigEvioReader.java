package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.daqconfig.EvioDAQParser;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.hps.record.daqconfig2019.EvioDAQParser2019;

/*
 * Search for a configuration bank in EvioEvent, and, if found, create an instance of
 * EvioDAQParser, pass it the string data for parsing, and put it in EventHeader.
 * 
 * 
 * As of Feb 27, 2015 (run 4043):
 * 
 * The error in string termination in one of the EVIO banks that made it unparseable 
 * by JEVIO is now fixed.  
 * 
 * But the banks are still being written in physics events under their crate's bank.
 * This should change soon.
 * 
 *  
 * @author <baltzell@jlab.org>
 * 
 *  * <code>EvioDAQParser2019</code> is parser for 2019 trigger configuration banks.
 *  
 * @author Tongtong Cao <caot@jlab.org>
 */
public class TriggerConfigEvioReader {

    int nDAQConfigErrors = 0;
    
    public void getDAQConfig(EvioEvent evioEvent, EventHeader lcsimEvent) {
        if (lcsimEvent.getRunNumber() < 4043) {
            if (nDAQConfigErrors++ < 2) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                        "EVIO DAQ CONFIG format not good for runs before #4043.  "
                        + "Ignoring it, and only printing this twice.");
            }
            return;
        }
        else if (lcsimEvent.getRunNumber() >= 4043 && lcsimEvent.getRunNumber() < 9000) {
            List<EvioDAQParser> trigconf = new ArrayList<EvioDAQParser>();
            for (BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getChildCount() <= 0)
                    continue;
                int crate = bank.getHeader().getTag();
                for (BaseStructure subBank : bank.getChildrenList()) {
                    if (subBank.getHeader().getTag() == EvioDAQParser.BANK_TAG) {
                        if (subBank.getStringData() == null) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                                    "JEVIO can't parse DAQ Config bank.  Event Number "
                                            + evioEvent.getEventNumber());
                            return;
                        }
                        if (trigconf.size() == 0)
                            trigconf.add(new EvioDAQParser());
                        trigconf.get(0).parse(crate,lcsimEvent.getRunNumber(),subBank.getStringData());
                    }
                }
            }
            if (trigconf.size() > 0) {
                lcsimEvent.put("TriggerConfig",trigconf,EvioDAQParser.class,0);
            }
        }
        else {
            List<EvioDAQParser2019> trigconf = new ArrayList<EvioDAQParser2019>();
            for (BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getChildCount() <= 0)
                    continue;
                int crate = bank.getHeader().getTag();
                for (BaseStructure subBank : bank.getChildrenList()) {
                    if (subBank.getHeader().getTag() == EvioDAQParser2019.BANK_TAG) {
                        if (subBank.getStringData() == null) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                                    "JEVIO can't parse DAQ Config bank.  Event Number "
                                            + evioEvent.getEventNumber());
                            return;
                        }
                        if (trigconf.size() == 0)
                            trigconf.add(new EvioDAQParser2019());
                        trigconf.get(0).parse(crate,lcsimEvent.getRunNumber(),subBank.getStringData());
                    }
                }
            }
            if (trigconf.size() > 0) {
                lcsimEvent.put("TriggerConfig",trigconf,EvioDAQParser2019.class,0);
            }
        }
    }

}
