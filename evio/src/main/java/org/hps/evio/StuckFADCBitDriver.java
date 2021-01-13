package org.hps.evio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.DaqId;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;

/**
 * Modify FADC waveform collection to fix stuck channel bits during 2019 run.
 * 
 * Configuration is left hard-coded at the top; hopefully we'll never need this again.
 *
 * WARNING: While this is generic to which crate/slot/channel and the channel's
 * bit/state is stuck, this will need modification if more than one combination
 * is affected.
 * 
 * @author baltzell
 */
public class StuckFADCBitDriver extends Driver {
   
    // if true, switches the fixer to pedestal-only and prints metrics that
    // may give additional info on efficiency, based on order-based denominator:
    private final boolean VALIDATE = false;
    
    private static final int RUN_MIN = 10651;
    private static final int RUN_MAX = 10678;
    private static final Set<CrateSlot> STUCK_SLOTS = new HashSet<>(Arrays.asList(new CrateSlot(1,3)));
    private static final StuckFADCBit.Stuck STUCK_BIT = new StuckFADCBit.Stuck(1,0);
    
    private static final String READOUT_NAME= "EcalHits";
    private static final String SUBDETECTOR_NAME = "Ecal";

    private static final Class HIT_CLASS = RawTrackerHit.class;
    private static final String HIT_COLLECTION_NAME = "EcalReadoutHits";

    private Subdetector subDetector = null;
    private IIdentifierHelper helper = null;
    private EcalConditions ecalConditions = null;

    private boolean printedFirst = false;
    private long nFixOrderIdentical = 0;
    private long nFixOrderAdjacent = 0;
    private long nFixOrderUnique = 0;
    private long nFixPedestal=0;
    private long nAvertPedestal=0;
    private int nOldBad = 0;
    private int nNewBad = 0;

    private final Map<EcalChannel,EcalChannel> swapRegister = new HashMap<>();

    public static class CrateSlot {
        public int crate,slot;
        public CrateSlot(int crate, int slot) {
            this.crate = crate;
            this.slot = slot;
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof CrateSlot) {
                return ((CrateSlot)o).crate==this.crate && ((CrateSlot)o).slot==this.slot;
            }
            return false;
        }
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + this.crate;
            hash = 37 * hash + this.slot;
            return hash;
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
        subDetector = DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector(SUBDETECTOR_NAME);
        helper = subDetector.getDetectorElement().getIdentifierHelper();
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    @Override
    public void endOfData() {
        final String s = this.getClass().getCanonicalName();
        Logger.getLogger(s).log(Level.INFO, "Old Bad:  {0}", nOldBad);
        Logger.getLogger(s).log(Level.INFO, "New Bad:  {0}", nNewBad);
        Logger.getLogger(s).log(Level.INFO, "Fixed Identical:  {0}",nFixOrderIdentical);
        Logger.getLogger(s).log(Level.INFO, "Fixed Adjacent:   {0}",nFixOrderAdjacent);
        Logger.getLogger(s).log(Level.INFO, "Fixed Unique:     {0}",nFixOrderUnique);
        Logger.getLogger(s).log(Level.INFO, "Fixed Pedestal:   {0}",nFixPedestal);
        Logger.getLogger(s).log(Level.INFO, "Avert Pedestal:   {0}",nAvertPedestal);
    }

    /**
     * Replace all affected hits with fixed ones.
     * @param event 
     */
    @Override
    public void process(EventHeader event) {

        if (!event.hasCollection(HIT_CLASS, HIT_COLLECTION_NAME)) return;

        if (event.getRunNumber() < RUN_MIN || event.getRunNumber() > RUN_MAX) return;

        List<RawTrackerHit> oldHits = event.get(HIT_CLASS, HIT_COLLECTION_NAME);

        nOldBad += analyze(event, oldHits, true);

        List<RawTrackerHit> newHits;

        if (VALIDATE) {
            newHits = fix(oldHits);
            analyze(oldHits,newHits);
        }
        else {
            newHits = fixSafe(oldHits);
        }

        event.remove(HIT_COLLECTION_NAME);
        event.put(HIT_COLLECTION_NAME, newHits, HIT_CLASS, 0, READOUT_NAME);

        nNewBad += analyze(event, event.get(HIT_CLASS, HIT_COLLECTION_NAME), false);
    }

    /**
     * Query whether this channel is potentially affected by stuck FADC bit
     * @param channel
     * @return
     */
    private boolean isAffected(EcalChannel channel) {
        return STUCK_SLOTS.contains(new CrateSlot(channel.getCrate(), channel.getSlot()));
    }

    /**
     * Get whether the two channels are stuck-bit partners, i.e. they differ only
     * by the stuck channel bit.
     * @param c1
     * @param c2
     * @return 
     */
    private boolean arePartners(EcalChannel c1, EcalChannel c2) {
        boolean ret = false;
        if (c1.getCrate() == c2.getCrate()) {
            if (c1.getSlot() == c2.getSlot()) {
                ret = STUCK_BIT.arePartners(c1.getChannel(), c2.getChannel());
            }
        }
        return ret;
    }

    /**
     * Get the EcalChannel object corresponding to a hit.
     * @param hit
     * @return
     */
    private EcalChannel getChannel(RawTrackerHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    }

    /**
     * Register the swapping, log if first time. 
     * @param stuck
     * @param unstuck 
     */
    private void register(EcalChannel stuck, EcalChannel unstuck) {
        if (!StuckFADCBit.equals(stuck,unstuck) && !swapRegister.containsKey(stuck)) {
            swapRegister.put(stuck,unstuck);
            String msg = String.format("Unsticking FADC Bit:  %s -> %s",
                    StuckFADCBit.toString(stuck),StuckFADCBit.toString(unstuck));
            Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
        }
        if (!STUCK_BIT.isStuck(stuck.getChannel())) {
            throw new RuntimeException();
        }
    }

    /**
     * Get channel with the stuck bit unstuck.
     * @param channel
     * @return 
     */
    private EcalChannel unStick(EcalChannel channel) {
        EcalChannel newChan = StuckFADCBit.unStick(ecalConditions, channel, STUCK_BIT); 
        register(channel, newChan);
        return newChan;
    }

    /**
     * Get channel with the stuck bit unstuck.
     * @param channel
     * @return 
     */
    private EcalChannel toggle(EcalChannel channel) {
        return StuckFADCBit.toggle(ecalConditions, channel, STUCK_BIT); 
    }
    
    /**
     * Get hit with the stuck bit unstuck. 
     * @param hit
     * @return 
     */
    private RawTrackerHit unStick(RawTrackerHit hit) {
        return makeECalRawHit(0, unStick(getChannel(hit)), hit.getADCValues());
    }
    
    /**
     * Get hit with the stuck bit unstuck, if appropriate based on pedestal.
     * @param hit
     * @return 
     */
    private RawTrackerHit fix(RawTrackerHit hit) {
        RawTrackerHit ret = hit;
        EcalChannel oldChan = getChannel(hit);
        if (isAffected(oldChan)) {
            EcalChannel newChan = StuckFADCBit.fix(ecalConditions, hit, STUCK_BIT);
            register(oldChan, newChan);
            ret = makeECalRawHit(0, newChan, hit.getADCValues());
        }
        return ret;
    }

    /**
     * Get hits with the stuck bit unstuck, based only on pedestal.
     * @param hits
     * @return 
     */
    private List<RawTrackerHit> fix(List<RawTrackerHit> hits) {
        List<RawTrackerHit> ret = new ArrayList<>();
        for (RawTrackerHit hit : hits) ret.add(fix(hit));
        return ret;
    }
    
    /**
     * Unstick any FADC bits if appropriate.  Use channel ordering if possible,
     * otherwise pedestals, to determine their stuckness.  This appears complicated
     * because it's written to be generic to what bit and state is the stuck one.
     * Should be pulled apart into smaller routines, which would probably require
     * moving stuckCandidate up in scope, or extending the hit class to attach
     * a stuckness to it.
     * @param hits
     * @return 
     */
    private List<RawTrackerHit> fixSafe(List<RawTrackerHit> hits) {

        List<RawTrackerHit> ret = new ArrayList<>(hits.size());

        // This array designates hits that are still candidates for having a
        // stuck bit.  We'll progressively mark them as non-candidates and 
        // (sometimes) unstick their bits below.
        boolean[] stuckCandidate = new boolean[hits.size()];
        for (int ii=0; ii<stuckCandidate.length; ii++) stuckCandidate[ii] = true;

        // First address "easy" cases:
        for (int i1=0; i1<hits.size(); i1++){

            EcalChannel c1 = getChannel(hits.get(i1));
            ret.add(hits.get(i1));

            // Unaffected crate/slot or bit is not in stuck state: 
            if (!isAffected(c1) || !stuckCandidate[i1] || !STUCK_BIT.isStuck(c1)) {
                stuckCandidate[i1] = false;
                continue;
            }

            for (int i2=i1+1; i2<hits.size(); i2++) {

                EcalChannel c2 = getChannel(hits.get(i2));

                // Two identical channels, unstick one based on ordering:
                if (StuckFADCBit.equals(c1,c2)) {
                    if (STUCK_BIT.state == 0) {
                        hits.set(i2, unStick(hits.get(i2)));
                    }
                    else {
                        ret.set(i1, unStick(hits.get(i1)));
                    }
                    stuckCandidate[i1] = false;
                    stuckCandidate[i2] = false;
                    nFixOrderIdentical++;
                    break;
                }

                // Two non-identical stuck-bit-partner channels in the same
                // event, mark both as unstuck just in case (although this
                // should never happen if the bit was really stuck):
                else if (arePartners(c1,c2)) {
                    stuckCandidate[i1] = false;
                    stuckCandidate[i2] = false;
                    break;
                }
            }
        }

        // Check for two consecutive, non-identical channels that are out of
        // order but can be unambiguously resolved by unsticking only one of
        // their bits:
        for (int i1=0; i1<ret.size()-1; i1++) {
            final int i2 = i1+1;
            EcalChannel c1 = getChannel(ret.get(i1));
            EcalChannel c2 = getChannel(ret.get(i2));
            if (!isAffected(c1) || !isAffected(c2)) continue;
            if (c1.getChannel() < c2.getChannel()) continue;
            EcalChannel c1new = StuckFADCBit.unStick(ecalConditions, c1, STUCK_BIT); 
            EcalChannel c2new = StuckFADCBit.unStick(ecalConditions, c2, STUCK_BIT);
            if (stuckCandidate[i1] && stuckCandidate[i2]) {
                if (STUCK_BIT.state==0) {
                    if (c1.getChannel() < c2new.getChannel()) {
                        stuckCandidate[i2] = false;
                        ret.set(i2, unStick(ret.get(i2)));
                        nFixOrderAdjacent++;
                    }
                }
                else if (c1new.getChannel() < c2.getChannel()) {
                    stuckCandidate[i1] = false;
                    ret.set(i1, unStick(ret.get(i1)));
                    nFixOrderAdjacent++;
                }
            }
            else if (stuckCandidate[i1]) {
                if (c1new.getChannel() < c2.getChannel()) {
                    stuckCandidate[i1] = false;
                    ret.set(i1, unStick(ret.get(i1)));
                    nFixOrderAdjacent++;
                }
            }
            else if (stuckCandidate[i2]) {
                if (c1.getChannel() < c2new.getChannel()) {
                    stuckCandidate[i2] = false;
                    ret.set(i2, unStick(ret.get(i2)));
                    nFixOrderAdjacent++;
                }
            }
        }

        // Then address non-identical, non-consecutive channels that are out of
        // order, if one has stuck bit in the stuck state and the other is not
        // a stuck candidate.  In hindsight, this may have been all that was
        // necessary before using pedestals, given the particular bit that we
        // know to be stuck, and is ineffective after the above "simpler" fixes.
        boolean fixed = true;
        while (fixed) {
            fixed = false;
            for (int i1=0; i1<ret.size(); i1++) {
                EcalChannel c1 = getChannel(ret.get(i1));
                if (!isAffected(c1)) continue;
                for (int i2=i1+1; i2<ret.size(); i2++) {
                    EcalChannel c2 = getChannel(ret.get(i2));
                    if (!isAffected(c2)) continue;
                    if (!STUCK_BIT.isStuck(c1.getChannel())) continue;
                    if (stuckCandidate[i1] && !stuckCandidate[i2]) {
                        if (c1.getChannel() >= c2.getChannel()) {
                            if (STUCK_BIT.unStick(c1.getChannel()) < c2.getChannel()) {
                                ret.set(i1, unStick(ret.get(i1)));
                                stuckCandidate[i1] = false;
                                i1--;
                                fixed = true;
                                nFixOrderUnique++;
                                break;
                            }
                        }
                    }
                }
            }
            for (int i1=0; i1<ret.size(); i1++) {
                EcalChannel c1 = getChannel(ret.get(i1));
                if (!isAffected(c1)) continue;
                for (int i2=i1+1; i2<ret.size(); i2++) {
                    EcalChannel c2 = getChannel(ret.get(i2));
                    if (!isAffected(c2)) continue;
                    if (!STUCK_BIT.isStuck(c2.getChannel())) continue;
                    if (stuckCandidate[i2] && !stuckCandidate[i1]) {
                        if (c1.getChannel() >= c2.getChannel()) {
                            if (STUCK_BIT.unStick(c2.getChannel()) > c1.getChannel()) {
                                ret.set(i2, unStick(ret.get(i2)));
                                stuckCandidate[i2] = false;
                                fixed = true;
                                nFixOrderUnique++;
                            }
                        }
                    }
                }
            }
        }

        // All ordering-only logic has been done.  Here we register which of the
        // remaining stuck candidates the pedestal algorithm would want to swap:
        boolean[] pedSwap = new boolean[ret.size()];
        for (int ii=0; ii<ret.size(); ii++) {
            pedSwap[ii] = false;
            if (!stuckCandidate[ii]) continue;
            if (!StuckFADCBit.equals(getChannel(fix(ret.get(ii))), getChannel(ret.get(ii)))) {
                pedSwap[ii] = true;
            }
        }

        // Veto any pedestal-based swaps if inconsistent with channel ordering:
        for (int i1=0; i1<ret.size(); i1++) {
            if (!isAffected(getChannel(ret.get(i1)))) continue;
            final int c1old = getChannel(ret.get(i1)).getChannel();
            final int c1new = STUCK_BIT.unStick(getChannel(ret.get(i1)).getChannel());
            for (int i2=i1+1; i2<ret.size(); i2++) {
                if (!isAffected(getChannel(ret.get(i2)))) continue;
                final int c2old = getChannel(ret.get(i2)).getChannel();
                final int c2new = STUCK_BIT.unStick(getChannel(ret.get(i2)).getChannel());
                if (pedSwap[i1] && pedSwap[i2]) {
                    if (c1new < c2new) {
                       // swapping both is valid, which cannot be prioritized
                       // over only swapping one of them, so let them both be
                    }
                    else if (c1new>c2old && c1old<c2new) {
                        // swapping only the 1st is invalid, veto the 1st:
                        pedSwap[i1] = false;
                        nAvertPedestal++;
                        i1--;
                        break;
                    }
                    else if (c1new<c2old && c1old>c2new) {
                        // swapping only the 2nd is invalid, veto the 2nd:
                        pedSwap[i2] = false;
                        nAvertPedestal++;
                    }
                }
                else if (pedSwap[i1]) {
                    if (c1new > c2old) {
                        pedSwap[i1] = false;
                        nAvertPedestal++;
                        i1--;
                        break;
                    }
                }
                else if (pedSwap[i2]) {
                    if (c1old > c2new) {
                        pedSwap[i2] = false;
                        nAvertPedestal++;
                    }
                }
            }
        }

        // finally, fix remaining stuck bit candidates based only on pedestal:
        for (int ii=0; ii<ret.size(); ii++) {
            if (pedSwap[ii]) {
                ret.set(ii, fix(ret.get(ii)));
                nFixPedestal++;
            }
        }
        
        return ret;
    }

    /**
     * Just a convenience method for uniformly logging the first instance of a
     * known stuck bit.
     * @param event 
     */
    private void printRunEvent(EventHeader event) {
        String msg = String.format("FIRST DEFINITELY-STUCK BIT @ RUN/EVENT:  %d/%d",
                event.getRunNumber(),event.getEventNumber());
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING,msg);
    }
 
    /**
     * Check for identical or out-of-order hits.
     * @param hits
     * @return 
     */
    private int analyze(EventHeader event, List<RawTrackerHit> hits, boolean print) {
        int ret = 0;
        for (int i1=0; i1<hits.size(); i1++){
            EcalChannel c1 = getChannel(hits.get(i1));
            for (int i2=i1+1; i2<hits.size(); i2++) {
                EcalChannel c2 = getChannel(hits.get(i2));
                if (StuckFADCBit.equals(c1,c2)) {
                    ret++;
                    if (print && !printedFirst) {
                        printedFirst = true;
                        printRunEvent(event);
                    }
                }
                else if (isAffected(c1) && isAffected(c2)) {
                    if (c1.getChannel() > c2.getChannel()) {
                        ret++;
                        if (print) {
                            if (!printedFirst) {
                                printedFirst = true;
                                printRunEvent(event);
                            }
                        }
                        //else {
                        //    for (int ii=0; ii<hits.size(); ii++) {
                        //        if (!isAffected(getChannel(hits.get(ii)))) continue;
                        //        final String msg = String.format("%d %d/%d %s",
                        //                ii,event.getRunNumber(),event.getEventNumber(),
                        //                StuckFADCBit.toString(getChannel(hits.get(ii))));
                        //        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE,msg);
                        //    }
                        //}
                    }
                }
            }
        }
        return ret;
    }
 
    /**
     * For cases where two channels in the same event are identical due to 
     * stuck bit, after the correction the 1st one should have had the stuck bit
     * unset.  Here we test for cases where the pedestal-based correction gets
     * it wrong by unsticking the 2nd hit's bit.  We also check for identical
     * channels when the stuck bit isn't set prior to correction.
     * @param oldHits
     * @param newHits
     * @return 
     */
    private boolean analyze(List<RawTrackerHit> oldHits, List<RawTrackerHit> newHits) {
        boolean ret = true;
        for (int i1=0; i1<oldHits.size(); i1++) {
            EcalChannel c1old = getChannel(oldHits.get(i1));
            EcalChannel c1new = getChannel(newHits.get(i1));
            if (!isAffected(c1old)) continue;
            for (int i2=i1+1; i2<oldHits.size(); i2++) {
                EcalChannel c2old = getChannel(oldHits.get(i2));
                EcalChannel c2new = getChannel(newHits.get(i2));
                if (!isAffected(c2old)) continue;
                // if unfixed, never should have bit partners:
                if (STUCK_BIT.arePartners(c1old, c2old)) {
                    String msg = "ERROR(A) "+StuckFADCBit.toString(c1old)+" -> "+StuckFADCBit.toString(c2old);
                    Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                    ret = false;
                }
                if (StuckFADCBit.equals(c1old,c2old)) {
                    // if unfixed, they both should have the stuck bit stuck:
                    if (!STUCK_BIT.isStuck(c1old.getChannel())) {
                        String msg = "ERROR(4) "+StuckFADCBit.toString(c1old)+" -> "+StuckFADCBit.toString(c2old);
                        Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                        ret = false;
                    }
                    // if fixed, the 1st should have the stuck bit unset:
                    if ((c1new.getChannel()&(1<<STUCK_BIT.index)) != 0) {
                        String msg = "ERROR(2) "+StuckFADCBit.toString(c1new)+" -> "+StuckFADCBit.toString(c2new);
                        Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                        ret = false;
                    }
                    // if fixed, the 2nd should have the stuck bit set:
                    if ((c2new.getChannel()&(1<<STUCK_BIT.index)) == 0) {
                        String msg = "ERROR(3) "+StuckFADCBit.toString(c1new)+" -> "+StuckFADCBit.toString(c2new);
                        Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                        ret = false;
                    }
                }
                // if properly fixed, there should never be identical hits:
                if (StuckFADCBit.equals(c1new,c2new)) {
                    String msg = "ERROR(5) "+StuckFADCBit.toString(c1new)+" -> "+StuckFADCBit.toString(c2new);
                    Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                    ret = false;
                    EcalChannel cUnStuck = toggle(c1new);
                    double measPed1 = StuckFADCBit.getPedestal(newHits.get(i1));
                    double measPed2 = StuckFADCBit.getPedestal(newHits.get(i2));
                    double dbPed1 = StuckFADCBit.getPedestal(ecalConditions, c1new);
                    double dbPed2 = StuckFADCBit.getPedestal(ecalConditions, cUnStuck);
                    msg  = String.format(" %s/db=%.1f/m=%.2f",StuckFADCBit.toString(c1new),dbPed1,measPed1);
                    msg += String.format("  %s/db=%.1f/m=%.1f",StuckFADCBit.toString(cUnStuck),dbPed2,measPed2);
                    Logger.getLogger(this.getClass().getCanonicalName()).info(msg);
                }
            }
        }
        return ret;
    }

    /**
     * Copied from EcalEvioReader
     * @param time
     * @param channel
     * @param samples
     * @return 
     */
    private BaseRawTrackerHit makeECalRawHit(int time, EcalChannel channel, short[] samples) {
        long daqId = daqToGeometryId(channel.getCrate(),channel.getSlot(),channel.getChannel());
        return new BaseRawTrackerHit(
                time,
                daqId,
                samples,
                new ArrayList<SimTrackerHit>(),
                subDetector.getDetectorElement().findDetectorElement(new Identifier(daqId)).get(0));
    }

    /**
     * Copied from EcalEvioReader
     * @param crate
     * @param slot
     * @param channel
     * @return 
     */
    private Long daqToGeometryId(int crate, int slot, int channel) {
        DaqId daqId = new DaqId(new int[]{crate, slot, channel});
        EcalChannel ecalChannel = ecalConditions.getChannelCollection().findChannel(daqId);
        if (ecalChannel == null) return null;
        int ix = ecalChannel.getX();
        int iy = ecalChannel.getY();
        GeometryId geometryId = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy});
        Long id = geometryId.encode();
        return id;
    }

}
