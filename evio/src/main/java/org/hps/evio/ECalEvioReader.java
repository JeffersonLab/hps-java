package org.hps.evio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.DaqId;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.FADCGenericHit;
import org.hps.recon.ecal.HPSRawCalorimeterHit;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.log.LogUtil;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ECalEvioReader.java,v 1.23 2013/04/18 20:59:16 meeg Exp $
 */
public class ECalEvioReader extends EvioReader {
    // Names of subdetectors.

    private int bankTag = 0;
    private Class hitClass = BaseRawCalorimeterHit.class;

    // FIXME: Hard-coded detector names.
    private static final String readoutName = "EcalHits";
    private static final String subdetectorName = "Ecal";
//    private Detector detector;
    private Subdetector subDetector;

    private static final String genericHitCollectionName = "FADCGenericHits";
    private List<FADCGenericHit> genericHits;

    private static EcalConditions ecalConditions = null;
    private static IIdentifierHelper helper = null;

    private int topBankTag, botBankTag;

    private final Map<List<Integer>, Integer> genericHitCount = new HashMap<List<Integer>, Integer>();

    private static Logger logger = LogUtil.create(ECalEvioReader.class);

    public ECalEvioReader(int topBankTag, int botBankTag) {
        this.topBankTag = topBankTag;
        this.botBankTag = botBankTag;
        hitCollectionName = "EcalReadoutHits";

        logger.setLevel(Level.FINE);
        //System.out.println("You are now using the database conditions for ECalEvioReader.java");
        // ID helper.
//        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
    }

    public void setTopBankTag(int topBankTag) {
        this.topBankTag = topBankTag;
    }

    public void setBotBankTag(int botBankTag) {
        this.botBankTag = botBankTag;
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {
        boolean foundHits = false;
        List<Object> hits = new ArrayList<Object>();
        genericHits = new ArrayList<FADCGenericHit>();
        hitClass = Object.class;
        int flags = 0;
        for (BaseStructure bank : event.getChildren()) {
            BaseStructureHeader header = bank.getHeader();
            int crateBankTag = header.getTag();
            int crate = 0;
            if (crateBankTag == topBankTag) {
                crate = 1;
            } else if (crateBankTag == botBankTag) {
                crate = 2;
            }
            if (crateBankTag == topBankTag || crateBankTag == botBankTag) {
                foundHits = true;
                if (bank.getChildCount() > 0) {
                    if (debug) {
                        System.out.println("ECal bank tag: " + header.getTag() + "; childCount: " + bank.getChildCount());
                    }
                    try {
                        for (BaseStructure slotBank : bank.getChildren()) {
//                            if (isTriggerBank(slotBank.getHeader().getTag(), lcsimEvent.getRunNumber()) != 0) {
//                                if (debug) {
//                                    int[] data = slotBank.getIntData();
//                                    for (int i = 0; i < data.length; i++) {
//                                        System.out.format("0x%x\n", data[i]);
//                                    }
//                                }
//                                continue;
//                            }
                            if (slotBank.getCompositeData() != null) { //skip SSP and TI banks, if any
                                for (CompositeData cdata : slotBank.getCompositeData()) {
//                            CompositeData cdata = slotBank.getCompositeData();
                                    if (slotBank.getHeader().getTag() != bankTag) {
                                        bankTag = slotBank.getHeader().getTag();
                                        System.out.printf("ECal format tag: 0x%x\n", bankTag);
                                    }
                                    switch (slotBank.getHeader().getTag()) {
                                        case EventConstants.ECAL_WINDOW_BANK_TAG:
                                            hits.addAll(makeWindowHits(cdata, crate));
                                            hitClass = RawTrackerHit.class;
                                            flags = 0;
                                            break;
                                        case EventConstants.ECAL_PULSE_BANK_TAG:
                                            hits.addAll(makePulseHits(cdata, crate));
                                            hitClass = RawTrackerHit.class;
                                            flags = 0;
                                            break;
                                        case EventConstants.ECAL_PULSE_INTEGRAL_BANK_TAG:
                                            hits.addAll(makeIntegralHitsMode3(cdata, crate));
                                            hitClass = RawCalorimeterHit.class;
                                            flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                            break;
                                        case EventConstants.ECAL_PULSE_INTEGRAL_HIGHRESTDC_BANK_TAG:
                                            hits.addAll(makeIntegralHitsMode7(cdata, crate));
                                            hitClass = HPSRawCalorimeterHit.class;
                                            flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                            break;
                                        default:
                                            throw new RuntimeException("Unsupported ECal format - bank tag " + slotBank.getHeader().getTag());
                                    }
                                }
                            }
                        }
                    } catch (EvioException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
//        String readoutName = ;
        lcsimEvent.put(hitCollectionName, hits, hitClass, flags, readoutName);
        lcsimEvent.put(genericHitCollectionName, genericHits, FADCGenericHit.class, 0);
//        for (Object hit : hits) {
//            System.out.println(((RawTrackerHit) hit).getIDDecoder().getIDDescription().toString());
//        }

        return foundHits;
    }

    private List<BaseRawTrackerHit> makeWindowHits(CompositeData cdata, int crate) {
        List<BaseRawTrackerHit> hits = new ArrayList<BaseRawTrackerHit>();
        if (debug) {
            int n = cdata.getNValues().size();
            for (int i = 0; i < n; i++) {
                System.out.println("cdata.N[" + i + "]=" + cdata.getNValues().get(i));
            }
            int ni = cdata.getItems().size();
            for (int i = 0; i < ni; i++) {
                System.out.println("cdata.type[" + i + "]=" + cdata.getTypes().get(i));
            }
        }
        while (cdata.index() + 1 < cdata.getItems().size()) {
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();
            if (debug) {
                System.out.println("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            }
            for (int j = 0; j < nchannels; j++) {
                short channel = cdata.getByte();
                int nSamples = cdata.getNValue();
                if (debug) {
                    System.out.println("  channel=" + channel + "; nSamples=" + nSamples);
                }

                Long id = daqToGeometryId(crate, slot, channel);
//                Long id = EcalConditions.daqToPhysicalID(crate, slot, channel);

                if (debug) {
                    System.out.println("The long id is: " + id);
                }

                short[] adcValues = new short[nSamples];
                for (int i = 0; i < nSamples; i++) {
                    adcValues[i] = cdata.getShort();
                }
                if (id == null) {
                    int[] data = new int[adcValues.length];
                    for (int i = 0; i < adcValues.length; i++) {
                        data[i] = adcValues[i];
                    }
                    processUnrecognizedChannel(new FADCGenericHit(EventConstants.ECAL_WINDOW_MODE, crate, slot, channel, data));
                } else {
                    hits.add(new BaseRawTrackerHit(
                            0,
                            id,
                            adcValues,
                            new ArrayList<SimTrackerHit>(),
                            subDetector
                            .getDetectorElement().findDetectorElement(new Identifier(id)).get(0)));
                }
            }
        }
        return hits;
    }

    private Long daqToGeometryId(int crate, short slot, short channel) {
        DaqId daqId = new DaqId(new int[]{crate, slot, channel});
        EcalChannel ecalChannel = ecalConditions.getChannelCollection().findChannel(daqId);
        if (ecalChannel == null) {
            return null;
        }
        int ix = ecalChannel.getX();
        int iy = ecalChannel.getY();
        GeometryId geometryId = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy});
        Long id = geometryId.encode();
        return id;
    }

    private List<BaseRawTrackerHit> makePulseHits(CompositeData cdata, int crate) {
        List<BaseRawTrackerHit> hits = new ArrayList<BaseRawTrackerHit>();
        if (debug) {
            int n = cdata.getNValues().size();
            for (int i = 0; i < n; i++) {
                System.out.println("cdata.N[" + i + "]=" + cdata.getNValues().get(i));
            }
            int ni = cdata.getItems().size();
            for (int i = 0; i < ni; i++) {
                System.out.println("cdata.type[" + i + "]=" + cdata.getTypes().get(i));
            }
        }
        while (cdata.index() + 1 < cdata.getItems().size()) { //the +1 is a hack because sometimes an extra byte gets read (padding)
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();
            if (debug) {
                System.out.println("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            }
            for (int j = 0; j < nchannels; j++) {
                short channel = cdata.getByte();
                int npulses = cdata.getNValue();
                if (debug) {
                    System.out.println("  channel=" + channel + "; npulses=" + npulses);
                }
                Long id = daqToGeometryId(crate, slot, channel);
                for (int k = 0; k < npulses; k++) {
                    short pulseNum = cdata.getByte();
                    int sampleCount = cdata.getNValue();
                    short[] adcValues = new short[sampleCount];
                    for (int i = 0; i < sampleCount; i++) {
                        adcValues[i] = cdata.getShort();
                    }
                    if (id == null) {
                        int[] data = new int[adcValues.length];
                        for (int i = 0; i < adcValues.length; i++) {
                            data[i] = adcValues[i];
                        }
                        processUnrecognizedChannel(new FADCGenericHit(EventConstants.ECAL_PULSE_MODE, crate, slot, channel, data));
                    } else {
                        hits.add(new BaseRawTrackerHit(pulseNum, id, adcValues, new ArrayList<SimTrackerHit>(), subDetector.getDetectorElement().findDetectorElement(new Identifier(id)).get(0)));
                    }
                }
            }
        }
        return hits;
    }

    private List<RawCalorimeterHit> makeIntegralHitsMode3(CompositeData cdata, int crate) {
        List<RawCalorimeterHit> hits = new ArrayList<RawCalorimeterHit>();
        if (debug) {
            int n = cdata.getNValues().size();
            for (int i = 0; i < n; i++) {
                System.out.println("cdata.N[" + i + "]=" + cdata.getNValues().get(i));
            }
            int ni = cdata.getItems().size();
            for (int i = 0; i < ni; i++) {
                System.out.println("cdata.type[" + i + "]=" + cdata.getTypes().get(i));
            }
        }
        while (cdata.index() + 1 < cdata.getItems().size()) { //the +1 is a hack because sometimes an extra byte gets read (padding)
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();
            if (debug) {
                System.out.println("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            }
            for (int j = 0; j < nchannels; j++) {
                short channel = cdata.getByte();
                int npulses = cdata.getNValue();
                if (debug) {
                    System.out.println("  channel=" + channel + "; npulses=" + npulses);
                }
                Long id = daqToGeometryId(crate, slot, channel);

                for (int k = 0; k < npulses; k++) {
                    short pulseTime = cdata.getShort();
                    int pulseIntegral = cdata.getInt();
                    if (debug) {
                        System.out.println("    pulseTime=" + pulseTime + "; pulseIntegral=" + pulseIntegral);
                    }
                    if (id == null) {
                        int[] data = {pulseIntegral, pulseTime};
                        processUnrecognizedChannel(new FADCGenericHit(EventConstants.ECAL_PULSE_INTEGRAL_MODE, crate, slot, channel, data));
                    } else {
                        hits.add(new BaseRawCalorimeterHit(id, pulseIntegral,pulseTime));
                    }
                }
            }
        }
        return hits;
    }

    
    
    private List<HPSRawCalorimeterHit> makeIntegralHitsMode7(CompositeData cdata, int crate) {
        List<HPSRawCalorimeterHit> hits = new ArrayList<HPSRawCalorimeterHit>();
        if (debug) {
            int n = cdata.getNValues().size();
            for (int i = 0; i < n; i++) {
                System.out.println("cdata.N[" + i + "]=" + cdata.getNValues().get(i));
            }
            int ni = cdata.getItems().size();
            for (int i = 0; i < ni; i++) {
                System.out.println("cdata.type[" + i + "]=" + cdata.getTypes().get(i));
            }
        }
        while (cdata.index() + 1 < cdata.getItems().size()) { //the +1 is a hack because sometimes an extra byte gets read (padding)
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();
            if (debug) {
                System.out.println("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            }
            for (int j = 0; j < nchannels; j++) {
                short channel = cdata.getByte();
                int npulses = cdata.getNValue();
                if (debug) {
                    System.out.println("  channel=" + channel + "; npulses=" + npulses);
                }
                Long id = daqToGeometryId(crate, slot, channel);

                for (int k = 0; k < npulses; k++) {
                    short pulseTime = cdata.getShort();
                    int pulseIntegral = cdata.getInt();
                    short amplLow = cdata.getShort();
                    short amplHigh = cdata.getShort();
                    if (debug) {
                        System.out.println("    pulseTime=" + pulseTime + "; pulseIntegral=" + pulseIntegral + "; amplLow=" + amplLow + "; amplHigh=" + amplHigh);
                    }
                    if (id == null) {
                        int[] data = {pulseIntegral, pulseTime, amplLow, amplHigh};
                        processUnrecognizedChannel(new FADCGenericHit(EventConstants.ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE, crate, slot, channel, data));
                    } else {
                        hits.add(new HPSRawCalorimeterHit(id, pulseIntegral,pulseTime,0,amplLow,amplHigh,EventConstants.ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE));
                    }
                }
            }
        }
        return hits;
    }
    
    
    
    
    
    private void processUnrecognizedChannel(FADCGenericHit hit) {
        genericHits.add(hit);

        List<Integer> channelAddress = Arrays.asList(hit.getCrate(), hit.getSlot(), hit.getChannel());
        Integer count = genericHitCount.get(channelAddress);
        if (count == null) {
            count = 0;
        }
        count++;
        genericHitCount.put(channelAddress, count);

        if (count < 10) {
            logger.info(String.format("Crate %d, slot %d, channel %d not found in map", hit.getCrate(), hit.getSlot(), hit.getChannel()));
        } else if (count == 10) {
            logger.warning(String.format("Crate %d, slot %d, channel %d not found in map: silencing further warnings for this channel", hit.getCrate(), hit.getSlot(), hit.getChannel()));
        }
    }

    void initialize() {
        subDetector = DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector(subdetectorName);

        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();

        helper = subDetector.getDetectorElement().getIdentifierHelper();
    }
}
