package org.hps.evio;

import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.DaqId;
import org.hps.conditions.ecal.EcalConditions;
import org.jlab.coda.jevio.CompositeData;
import org.lcsim.event.RawTrackerHit;

/**
 * Tools to resolve stuck FADC bit in 2019 run, based on comparing
 * measured and expected pedestal values.
 * @author baltzell
 */
public class StuckFADCBit {
   
    static final int N_PEDESTAL_SAMPLES = 7;
 
    public static class Stuck {
        int index;
        int state;
        public Stuck(int bitIndex, int bitState) {
            index = bitIndex;
            state = bitState;
        }
        public int unStick(int value) {
            if (state == 0) {
                // set the bit:
                return value | (1<<index);
            }
            else {
                // unset the bit:
                return value & ~(1<<index);
            }
        }
        public int toggle(int value) {
            return value ^ (1<<index);
        }
        public boolean isStuck(int value) {
            return ((value >> index) & 1) == state;
        }
        public boolean arePartners(int x, int y) {
            // the stuck bit:
            int bx = x & (1<<index);
            int by = y & (1<<index);
            // all the others:
            int ox = x & ~(1<<index);
            int oy = y & ~(1<<index);
            return ox==oy && bx!=by;
        }
        public boolean arePartners(EcalChannel x, EcalChannel y) {
            if (x.getCrate() != y.getCrate()) {
                return false;
            }
            if (x.getSlot() != y.getSlot()) {
                return false;
            }
            return arePartners(x.getChannel(), y.getChannel());
        }
    }

    /**
     * Get the measured pedestal for a given readout.
     * @param cdata FADC samples
     * @return the average of the first samples
     */
    public static float getPedestal(CompositeData cdata) {
        final CompositeData tmp = (CompositeData)cdata.clone();
        final int maxSamples = tmp.getNValue();
        float pedestal = 0;
        int nSamples = 0;
        for (int ii=0; ii<N_PEDESTAL_SAMPLES && ii<maxSamples; ii++) {
            pedestal += cdata.getShort();
            nSamples++;
        }
        return pedestal/nSamples;
    }

    /**
     * Get the measured pedestal for a given readout.
     * @param hit
     * @return the average of the first samples
     */
    public static float getPedestal(RawTrackerHit hit) {
        float pedestal = 0;
        int nSamples = 0;
        for (int ii=0; ii<N_PEDESTAL_SAMPLES && ii<hit.getADCValues().length; ii++) {
            pedestal += hit.getADCValues()[ii];
            nSamples++;
        }
        return pedestal/nSamples;
    }

    /**
     * Get the conditions database pedestal for a given channel.
     * @param condi
     * @param daqId
     * @return the pedestal
     */
    public static double getPedestal(EcalConditions condi, DaqId daqId) {
        return condi.getChannelConstants(
               condi.getChannelCollection().findChannel(daqId)).getCalibration().getPedestal();
    }

    /**
     * Get the conditions database pedestal. 
     * @param condi
     * @param c
     * @return 
     */
    public static double getPedestal(EcalConditions condi, EcalChannel c) {
        return getPedestal(condi,new DaqId(new int[]{c.getCrate(),c.getSlot(),c.getChannel()}));
    }

    /**
     * Just format a channel as crate/slot/channel
     * @param c
     * @return
     */
    public static String toString(EcalChannel c) {
        return String.format("%d/%d/%d(%4s)", c.getCrate(), c.getSlot(), c.getChannel(),
                Integer.toBinaryString(c.getChannel())).replace(' ','0');
    }

    /**
     * 
     * @param condi
     * @param crate
     * @param slot
     * @param channel
     * @return 
     */
    public static EcalChannel getChannel(EcalConditions condi, int crate, int slot, int channel) {
        return condi.getChannelCollection().findChannel(new DaqId(new int[]{crate,slot,channel}));
    }

    /**
     * Compare two EcalChannel objects based on crate/slot/channel.
     * Shouldn't EcalChannel provide this itself?
     * @param c1
     * @param c2
     * @return
     */
    public static boolean equals(EcalChannel c1, EcalChannel c2) {
        if (c1.getCrate() != c2.getCrate()) {
            return false;
        }
        if (c1.getSlot() != c2.getSlot()) {
            return false;
        }
        if (c1.getChannel() != c2.getChannel()) {
            return false;
        }
        return true;
    }

    /**
     * Get the channel after unsetting the given bit. 
     * @param condi
     * @param channel
     * @param stuck
     * @return 
     */
    public static EcalChannel unstick(EcalConditions condi, EcalChannel channel, Stuck stuck) {
        final int unstuckChannel = stuck.unStick(channel.getChannel());
        return getChannel(condi, channel.getCrate(), channel.getSlot(), unstuckChannel);
    }

    /**
     * Get the channel after toggling the given bit. 
     * @param condi
     * @param channel
     * @param stuck
     * @return 
     */
    public static EcalChannel toggle(EcalConditions condi, EcalChannel channel, Stuck stuck) {
        final int unstuckChannel = stuck.toggle(channel.getChannel());
        return getChannel(condi, channel.getCrate(), channel.getSlot(), unstuckChannel);
    }

    /**
     * Get the corrected channel.
     * @param condi
     * @param channel
     * @param cdata
     * @param stuck
     * @return 
     */
    public static EcalChannel fix(EcalConditions condi, EcalChannel channel, CompositeData cdata, Stuck stuck) {
        EcalChannel ret = channel;
        if (stuck.isStuck(ret.getChannel())) {
            final EcalChannel newChan = unstick(condi, ret, stuck);
            final double oldPed = getPedestal(condi, ret);
            final double newPed = getPedestal(condi, newChan);
            final double measPed = getPedestal(cdata);
            if (Math.abs(newPed-measPed) < Math.abs(oldPed-measPed)) {
                ret = newChan;
            }
        }
        return ret;
    }

    /**
     * Get the corrected channel.
     * @param condi
     * @param hit
     * @param stuck
     * @return 
     */
    public static EcalChannel fix(EcalConditions condi, RawTrackerHit hit, Stuck stuck) {
        EcalChannel ret = condi.getChannelCollection().findGeometric(hit.getCellID());
        if (stuck.isStuck(ret.getChannel())) {
            final EcalChannel newChan = unstick(condi, ret, stuck);
            final double oldPed = getPedestal(condi, ret);
            final double newPed = getPedestal(condi, newChan);
            final double measPed = getPedestal(hit);
            if (Math.abs(newPed-measPed) < Math.abs(oldPed-measPed)) {
                ret = newChan;
            }
        }
        return ret;
    }

    /**
     * Get the corrected channel. 
     * @param condi
     * @param daqId
     * @param cdata
     * @param stuck
     * @return 
     */
    public static EcalChannel fix(EcalConditions condi, DaqId daqId, CompositeData cdata, Stuck stuck) {
        return fix(condi,condi.getChannelCollection().findChannel(daqId), cdata, stuck);
    }

}
