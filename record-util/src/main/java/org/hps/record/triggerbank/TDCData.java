package org.hps.record.triggerbank;

import java.util.ArrayList;
import java.util.List;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.GenericObject;

/**
 * Data from CAEN V1190 TDC.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class TDCData extends AbstractIntData {

    public static final int BANK_TAG = 0xe107;
    private final List<TDCHit> hits = new ArrayList<TDCHit>();

    public TDCData(int[] bank) {
        super(bank);
        decodeData();
    }

    public TDCData(GenericObject tiData) {
        super(tiData, BANK_TAG);
        decodeData();
    }

    @Override
    public int getTag() {
        return BANK_TAG;
    }

    @Override
    protected final void decodeData() {
        for (int i = 0; i < bank.length; i++) {
            hits.add(new TDCHit(bank[i]));
        }
    }

    public List<TDCHit> getHits() {
        return hits;
    }

    public class TDCHit {

        private final int tdcSlot, edge, tdcChannel, time;
        private int dscCrate, dscSlot, dscChannel;
        private int fadcCrate;
        private boolean isECal;
        private int x = 0, y = 0;

        public TDCHit(int data) {
            tdcSlot = (data >> 27) & 0x1F; // bits 31:27
            edge = (data >> 26) & 1; // bits 26:26
            tdcChannel = (data >> 19) & 0x7F; // bits 25:19
            time = data & 0x7FFFF; // bits 18:00
            tdc2dsc();
        }

        public int getTdcSlot() {
            return tdcSlot;
        }

        public int getEdge() {
            return edge;
        }

        public int getTdcChannel() {
            return tdcChannel;
        }

        public int getTime() {
            return time;
        }

        public int getDscCrate() {
            return dscCrate;
        }

        public int getDscSlot() {
            return dscSlot;
        }

        public int getDscChannel() {
            return dscChannel;
        }

        public int getFadcCrate() {
            return fadcCrate;
        }

        public boolean isECal() {
            return isECal;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        private void tdc2dsc() {
            if (tdcSlot < 10 || tdcSlot > 13 || tdcChannel < 0 || tdcChannel > 127) {
                throw new RuntimeException("Invalid TDC channel: " + tdcSlot + " " + tdcChannel);
            }
            int tdcSlot0 = 10;
            int dscSlot0 = tdcSlot < 12 ? 3 : 6;
            dscCrate = tdcChannel < 64 ? 46 : 58;
            fadcCrate = tdcChannel < 64 ? 2 : 1;
            dscSlot = (tdcChannel % 64) / 16 + 4 * (tdcSlot - tdcSlot0) + dscSlot0;
            dscChannel = tdcChannel % 16;

            EcalConditions ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
            EcalChannel.DaqId daqId = new EcalChannel.DaqId(new int[]{fadcCrate, dscSlot, dscChannel}); // DSC slot/channel mapping mirrors FADC mapping
            EcalChannel ecalChannel = ecalConditions.getChannelCollection().findChannel(daqId);
            if (ecalChannel == null) {
                isECal = false;
            } else {
                isECal = true;
                x = ecalChannel.getX();
                y = ecalChannel.getY();
            }
        }

        @Override
        public String toString() {
            return String.format("slot %d, edge %d, ch %d, time %d; DSC crate %d, slot %d, ch %d, isECal %b, ix %d, iy %d", tdcSlot, edge, tdcChannel, time, dscCrate, dscSlot, dscChannel, isECal, x, y);
        }
    }
}
