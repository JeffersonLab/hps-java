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
 * Configuration is left hard-coded; hopefully we'll never need this again.
 *
 * @author baltzell
 */
public class StuckFADCBitDriver extends Driver {
    
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

    private int nOldBad = 0;
    private int nNewBad = 0;

    private final Map<EcalChannel,EcalChannel> swaps = new HashMap<>();
 
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
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.INFO, "Old Bad:  {0}", nOldBad);
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.INFO, "New Bad:  {0}", nNewBad);
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

        if (!analyze(oldHits)) {
            nOldBad++;
        }

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

        if (!analyze(event.get(HIT_CLASS, HIT_COLLECTION_NAME))) {
            nNewBad++;
        }
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
        if (!StuckFADCBit.equals(stuck,unstuck) && !swaps.containsKey(stuck)) {
            swaps.put(stuck,unstuck);
            String msg = String.format("Unsticking FADC Bit:  %s -> %s",
                    StuckFADCBit.toString(stuck),StuckFADCBit.toString(unstuck));
            Logger.getLogger(this.getClass().getCanonicalName()).warning(msg);
        }
    }

    /**
     * Get channel with the stuck bit unstuck.
     * @param channel
     * @return 
     */
    private EcalChannel unStick(EcalChannel channel) {
        EcalChannel newChan = StuckFADCBit.unstick(ecalConditions, channel, STUCK_BIT); 
        register(channel, newChan);
        return newChan;
    }

    /**
     * Get channel with the stuck bit unstuck.
     * @param channel
     * @return 
     */
    private EcalChannel toggle(EcalChannel channel) {
        EcalChannel newChan = StuckFADCBit.toggle(ecalConditions, channel, STUCK_BIT); 
        register(channel, newChan);
        return newChan;
    }
    
    /**
     * Get hit with the stuck bit unstuck. 
     * @param hit
     * @return 
     */
    private RawTrackerHit unStick(RawTrackerHit hit) {
        EcalChannel newChan = unStick(getChannel(hit));
        return makeECalRawHit(0, newChan, hit.getADCValues());
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
     * Get hits with the stuck bit unstuck, if appropriate based on pedestal.
     * @param hits
     * @return 
     */
    private List<RawTrackerHit> fix(List<RawTrackerHit> hits) {
        List<RawTrackerHit> ret = new ArrayList<>();
        for (RawTrackerHit hit : hits) {
            ret.add(fix(hit));
        }
        return ret;
    }
    
    /**
     * Unstick any FADC bits if appropriate.  Use channel ordering if possible,
     * otherwise pedestals, to determine their stuckness.
     * @param hits
     * @return 
     */
    private List<RawTrackerHit> fixSafe(List<RawTrackerHit> hits) {

        List<RawTrackerHit> ret = new ArrayList<>(hits.size());
        boolean[] stuckCandidate = new boolean[hits.size()];
        for (int ii=0; ii<stuckCandidate.length; ii++) stuckCandidate[ii] = true;

        // first address "easy" cases:
        for (int i1=0; i1<hits.size(); i1++){

            RawTrackerHit h1 = hits.get(i1);
            EcalChannel c1 = getChannel(h1);

            ret.add(hits.get(i1));

            // unaffected crate/slot or already marked as unstuck: 
            if (!isAffected(c1) || !stuckCandidate[i1]) {
                stuckCandidate[i1] = false;
                continue;
            }
                
            for (int i2=i1+1; i2<hits.size(); i2++) {

                RawTrackerHit h2 = hits.get(i2);
                EcalChannel c2 = getChannel(hits.get(i2));

                // two identical channels in the same event, unstick one of them
                // based on ordering of the hits in the event:
                if (StuckFADCBit.equals(c1,c2)) {
                    if (STUCK_BIT.state == 0) {
                        hits.set(i2, unStick(h2));
                    }
                    else {
                        ret.set(i1, unStick(h1));
                    }
                    stuckCandidate[i1] = false;
                    stuckCandidate[i2] = false;
                    break;
                }

                // two non-identical partner channels in the same event,
                // mark both as unstuck (this should never happen):
                else if (arePartners(c1,c2)) {
                    stuckCandidate[i1] = false;
                    stuckCandidate[i2] = false;
                    break;
                }
            }
        }

        // then use pedestal to determine stuckness:
        for (int ii=0; ii<ret.size(); ii++) {
            if (stuckCandidate[ii]) {
                ret.set(ii, fix(ret.get(ii)));
            }
        }

        return ret;
    }
 
    /**
     * Check for identical hits.
     * @param hits
     * @return 
     */
    private boolean analyze(List<RawTrackerHit> hits) {
        for (int i1=0; i1<hits.size(); i1++){
            EcalChannel c1 = getChannel(hits.get(i1));
            for (int i2=i1+1; i2<hits.size(); i2++) {
                EcalChannel c2 = getChannel(hits.get(i2));
                if (StuckFADCBit.equals(c1,c2)) {
                    return false;
                }
            }
        }
        return true;
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
