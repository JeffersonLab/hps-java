package org.hps.users.meeg;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.FADCGenericHit;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TDCData;
import org.hps.record.triggerbank.TDCData.TDCHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class TDCPrintDriver extends Driver {

    private static EcalConditions ecalConditions = null;
    private boolean printADC = false;

    public void setPrintADC(boolean printADC) {
        this.printADC = printADC;
    }

    protected void detectorChanged(Detector detector) {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    @Override
    protected void process(EventHeader event) {
        List<TDCData> allTDCData = new ArrayList<TDCData>();
        List<GenericObject> objects = event.get(GenericObject.class, "FADCGenericHits");
        for (GenericObject object : objects) {
            if (FADCGenericHit.getReadoutMode(object) == 1) { // EventConstants.ECAL_WINDOW_MODE
                System.out.format("Non-ECal raw mode hit, crate %d, slot %d, channel %d\n", FADCGenericHit.getCrate(object), FADCGenericHit.getSlot(object), FADCGenericHit.getChannel(object));
                int[] data = FADCGenericHit.getData(object); //do stuff with data 
                if (printADC) {
                    for (int i = 0; i < data.length; i++) {
                        System.out.println(data[i]);
                    }
                }
            }
        }

        if (event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            for (RawTrackerHit hit : rawHits) {
                int crate = ecalConditions.getChannelCollection().findGeometric(hit.getCellID()).getCrate();
                int slot = ecalConditions.getChannelCollection().findGeometric(hit.getCellID()).getSlot();
                int channel = ecalConditions.getChannelCollection().findGeometric(hit.getCellID()).getChannel();
                System.out.format("Raw mode hit, crate %d, slot %d, channel %d, ix %d, iy %d\n", crate, slot, channel, hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                short[] data = hit.getADCValues(); //do stuff with data 
                if (printADC) {
                    for (int i = 0; i < data.length; i++) {
                        System.out.println(data[i]);
                    }
                }

            }
        }

        List<GenericObject> intDataCollection = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject data : intDataCollection) {
            TDCData tdcData = null;
            if (data instanceof TDCData) {
                tdcData = (TDCData) data;
            } else if (AbstractIntData.getTag(data) == TDCData.BANK_TAG) {
                tdcData = new TDCData(data);
            }
            if (tdcData != null) {
                allTDCData.add(tdcData);
            }
        }
        for (TDCData tdcData : allTDCData) {
            System.out.println("got a TDCData");
            for (TDCHit hit : tdcData.getHits()) {
                System.out.println(hit);
            }
        }

    }
}
