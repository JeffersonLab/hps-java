package org.hps.recon.ecal.triggerbank;

import java.util.ArrayList;
import java.util.List;
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

        private final int slot, edge, channel, time;

        public TDCHit(int data) {
            slot = (data >> 27) & 0x1F; // bits 31:27
            edge = (data >> 26) & 1; // bits 26:26
            channel = (data >> 19) & 0x7F; // bits 25:19
            time = data & 0x7FFFF; // bits 18:00
        }

        public int getSlot() {
            return slot;
        }

        public int getEdge() {
            return edge;
        }

        public int getChannel() {
            return channel;
        }

        public int getTime() {
            return time;
        }

        @Override
        public String toString() {
            return String.format("slot %d, edge %d, ch %d, time %d", slot, edge, channel, time);
        }
    }
}
