package org.hps.users.meeg;

import java.util.List;
import org.hps.recon.ecal.FADCGenericHit;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TDCData;
import org.hps.recon.ecal.triggerbank.TDCData.TDCHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class TDCPrintDriver extends Driver {

    @Override
    protected void process(EventHeader event) {
        TDCData tdcData = null;
        List<GenericObject> objects = event.get(GenericObject.class, "FADCGenericHits");
        for (GenericObject object : objects) {
            if (FADCGenericHit.getReadoutMode(object) == 1) { // EventConstants.ECAL_WINDOW_MODE
                System.out.format("Raw mode hit, crate %d, slot %d, channel %d\n", FADCGenericHit.getCrate(object), FADCGenericHit.getSlot(object), FADCGenericHit.getChannel(object));
                int[] data = FADCGenericHit.getData(object); //do stuff with data 
                for (int i = 0; i < data.length; i++) {
                    System.out.println(data[i]);
                }
            }
        }

        List<GenericObject> intDataCollection = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject data : intDataCollection) {
            if (data instanceof TDCData) {
                tdcData = (TDCData) data;
            } else if (AbstractIntData.getTag(data) == TDCData.BANK_TAG) {
                tdcData = new TDCData(data);
            }
        }
        
        for (TDCHit hit : tdcData.getHits()) {
            System.out.println(hit);
        }

    }
}
