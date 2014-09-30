package org.hps.evio;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.DaqId;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
//import org.hps.conditions.deprecated.EcalConditions;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOConstants;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ECalEvioReader.java,v 1.23 2013/04/18 20:59:16 meeg Exp $
 */
public class ECalEvioReader extends EvioReader {
    // Names of subdetectors.

    private int bankTag = EventConstants.ECAL_PULSE_INTEGRAL_BANK_TAG;
    private Class hitClass = BaseRawCalorimeterHit.class;

    // FIXME: Hard-coded detector names.
    private static final String readoutName = "EcalHits";
    private static final String subdetectorName = "Ecal";
//    private Detector detector;
    private final Subdetector subDetector;

    private static EcalConditions ecalConditions = null;
    private static IIdentifierHelper helper = null;
    private static EcalChannelCollection channels = null;

    public ECalEvioReader() {
        hitCollectionName = "EcalReadoutHits";

        subDetector =  DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector(subdetectorName);

        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();

        // List of channels.
        channels = ecalConditions.getChannelCollection();

        helper = subDetector.getDetectorElement().getIdentifierHelper();

        System.out.println("You are now using the database conditions for ECalEvioReader.java");
        // ID helper.
//        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {
        boolean foundHits = false;
        List<Object> hits = new ArrayList<Object>();
        hitClass = Object.class;
        int flags = 0;
        for (BaseStructure bank : event.getChildren()) {
            BaseStructureHeader header = bank.getHeader();
            int crateBankTag = header.getTag();
            if (crateBankTag == EventConstants.ECAL_TOP_BANK_TAG || crateBankTag == EventConstants.ECAL_BOTTOM_BANK_TAG) {
                foundHits = true;
                if (bank.getChildCount() > 0) {
                    if (debug) {
                        System.out.println("ECal bank tag: " + header.getTag() + "; childCount: " + bank.getChildCount());
                    }
                    try {
                        for (BaseStructure slotBank : bank.getChildren()) {
                            if (slotBank.getHeader().getTag() == EventConstants.TRIGGER_BANK_TAG) {
                                if (debug) {
                                    int[] data = slotBank.getIntData();
                                    for (int i = 0; i < data.length; i++) {
                                        System.out.format("0x%x\n", data[i]);
                                    }
                                }
                                continue;
                            }
                            for (CompositeData cdata : slotBank.getCompositeData()) {
//                            CompositeData cdata = slotBank.getCompositeData();
                                if (slotBank.getHeader().getTag() != bankTag) {
                                    bankTag = slotBank.getHeader().getTag();
                                    System.out.printf("ECal format tag: 0x%x\n", bankTag);
                                }
                                switch (slotBank.getHeader().getTag()) {
                                    case EventConstants.ECAL_WINDOW_BANK_TAG:
                                        hits.addAll(makeWindowHits(cdata, crateBankTag));
                                        hitClass = RawTrackerHit.class;
                                        flags = 0;
                                        break;
                                    case EventConstants.ECAL_PULSE_BANK_TAG:
                                        hits.addAll(makePulseHits(cdata, crateBankTag));
                                        hitClass = RawTrackerHit.class;
                                        flags = 0;
                                        break;
                                    case EventConstants.ECAL_PULSE_INTEGRAL_BANK_TAG:
                                        hits.addAll(makeIntegralHits(cdata, crateBankTag));
                                        hitClass = BaseRawCalorimeterHit.class;
                                        flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                        break;
                                    default:
                                        throw new RuntimeException("Unsupported ECal format - bank tag " + slotBank.getHeader().getTag());
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

                System.out.println("The long id is: " + id);

                short[] adcValues = new short[nSamples];
                for (int i = 0; i < nSamples; i++) {
                    adcValues[i] = cdata.getShort();
                }
                if (id == null) {
                    System.out.printf("Crate %d, slot %d, channel %d not found in map\n", crate, slot, channel);
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
        EcalChannel ecalChannel = channels.findChannel(daqId);
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
        while (cdata.index() < cdata.getItems().size()) {
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
                        System.out.printf("Crate %d, slot %d, channel %d not found in map\n", crate, slot, channel);
                    } else {
                        hits.add(new BaseRawTrackerHit(pulseNum, id, adcValues, new ArrayList<SimTrackerHit>(), subDetector.getDetectorElement().findDetectorElement(new Identifier(id)).get(0)));
                    }
                }
            }
        }
        return hits;
    }

    private List<BaseRawCalorimeterHit> makeIntegralHits(CompositeData cdata, int crate) {
        List<BaseRawCalorimeterHit> hits = new ArrayList<BaseRawCalorimeterHit>();
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
                        System.out.printf("Crate %d, slot %d, channel %d not found in map\n", crate, slot, channel);
                    } else {
                        hits.add(new BaseRawCalorimeterHit(id, pulseIntegral, pulseTime));
                    }
                }
            }
        }
        return hits;
    }
}
