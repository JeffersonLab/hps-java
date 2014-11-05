package org.hps.evio;

import org.hps.readout.ecal.SSPData;
import org.hps.readout.ecal.TriggerData;

/**
 * Build LCSim events from EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: LCSimTestRunEventBuilder.java,v 1.24 2013/03/01 01:30:25 meeg
 * Exp $
 */
public class LCSimEngRunEventBuilder extends LCSimTestRunEventBuilder {

    public LCSimEngRunEventBuilder() {
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);
        sspCrateBankTag = 0x25;
        sspBankTag = 0xe10c;
//        ecalReader = new ECalEvioReader(0x25, 0x27);
//        svtReader = new SVTEvioReader();
    }

    protected TriggerData makeTriggerData(int[] data) {
        TriggerData triggerData = new SSPData(data);
        time = ((long) triggerData.getTime()) * 4;
        return triggerData;
    }


}
