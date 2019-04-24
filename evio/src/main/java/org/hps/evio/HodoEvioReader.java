/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
//import org.hps.conditions.ecal.EcalChannel;
//import org.hps.conditions.ecal.EcalChannel.DaqId;
//import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.FADCGenericHit;
import org.hps.recon.ecal.HitExtraData;
import org.hps.recon.ecal.HitExtraData.Mode7Data;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
//import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
//import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;
//import org.lcsim.lcio.LCIOConstants;


/**
 *
 * @author rafopar
 */
public class HodoEvioReader extends EvioReader {

    private int bankTag = 0;
    private Class hitClass = BaseRawCalorimeterHit.class;    
    
    //private static HodoConditions hodoConditions = null;
    private static EcalConditions hodoConditions = null;           // <<=============== This should be Hodo Conditions
    private static IIdentifierHelper helper = null;

    private static final String readoutName = "HodoHits";
    private static final String subdetectorName = "Hodo";

    private Subdetector subDetector;

    private static final String genericHitCollectionName = "FADCGenericHits";
    private List<FADCGenericHit> genericHits;
    
    private static final String extraDataRelationsName = "HodoReadoutExtraDataRelations";
    private List<LCRelation> extraDataRelations;

    private static final String extraDataCollectionName = "HodoReadoutExtraData";
    private List<HitExtraData> extraDataList;
    

    private final Map<List<Integer>, Integer> genericHitCount = new HashMap<List<Integer>, Integer>();

    private static final Logger LOGGER = Logger.getLogger(HodoEvioReader.class.getPackage().getName());

    private int topBankTag, botBankTag;

    public HodoEvioReader(int topBankTag, int botBankTag) {
        this.topBankTag = topBankTag;
        this.botBankTag = botBankTag;
        hitCollectionName = "HodoReadoutHits";
    }

    public void setTopBankTag(int topBankTag) {
        this.topBankTag = topBankTag;
    }

    public void setBotBankTag(int botBankTag) {
        this.botBankTag = botBankTag;
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        System.out.println("Kuku: MakeHits of the HodoReader");
        
        boolean foundHits = false;
        List<Object> hits = new ArrayList<Object>();
        genericHits = new ArrayList<FADCGenericHit>();
        extraDataList = new ArrayList<HitExtraData>();
        extraDataRelations = new ArrayList<LCRelation>();
        int flags = 0;
        for (BaseStructure bank : event.getChildrenList()) {
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
                        System.out.println("Hodo bank tag: " + header.getTag() + "; childCount: " + bank.getChildCount());
                    }
                    try {
                        for (BaseStructure slotBank : bank.getChildrenList()) {
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
                                        LOGGER.info(String.format("Hodo format tag: 0x%x\n", bankTag));
                                    }
                                    switch (slotBank.getHeader().getTag()) {
                                        case EventConstants.FADC_MODE1_BANK_TAG:
                                            hits.addAll(makeWindowHits(cdata, crate));
                                            hitClass = RawTrackerHit.class;
                                            flags = 0;
                                            break;
                                        /*case EventConstants.ECAL_PULSE_BANK_TAG:
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
                                            hitClass = RawCalorimeterHit.class;
                                            flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                            break;*/
                                        default:
                                            throw new RuntimeException("Unsupported Hodo format - bank tag " + slotBank.getHeader().getTag());
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
        if (!extraDataList.isEmpty()) {
            lcsimEvent.put(extraDataCollectionName, extraDataList, Mode7Data.class, 0);
            lcsimEvent.put(extraDataRelationsName, extraDataRelations, LCRelation.class, 0);
        }
//        for (Object hit : hits) {
//            System.out.println(((RawTrackerHit) hit).getIDDecoder().getIDDescription().toString());
//        }

        return foundHits;
        
        
        
        
  //      return true; // Temporrary, just for not givin syntax error
    }

    private BaseRawTrackerHit makeHodoRawHit(int time, long id, CompositeData cdata, int nSamples) {
        
        
//        System.out.println("time = " + time );
//        System.out.println("id = " + id );
//        System.out.println("cdata.getStrings() = " +  cdata.getStrings());
//        System.out.println("nSamples = " + nSamples );
//        System.out.println("Short is " + cdata.getShort());
        
        short[] adcValues = new short[nSamples];
        for (int i = 0; i < nSamples; i++) {
            adcValues[i] = cdata.getShort();
            //System.out.println("ADC["+i+"] = " + adcValues[i]);
        }
        return new BaseRawTrackerHit( // need to use the complicated constructor, simhit collection can't be null
                time,
                id,
                adcValues,
                new ArrayList<SimTrackerHit>(),
                subDetector.getDetectorElement().findDetectorElement(new Identifier(id)).get(0));
    }

    private static FADCGenericHit makeGenericRawHit(int mode, int crate, short slot, short channel, CompositeData cdata, int nSamples) {
        int[] adcValues = new int[nSamples];
        for (int i = 0; i < nSamples; i++) {
            adcValues[i] = cdata.getShort();
        }
        return new FADCGenericHit(mode, crate, slot, channel, adcValues);
    }

    private List<BaseRawTrackerHit> makeWindowHits(CompositeData cdata, int crate) {
        List<BaseRawTrackerHit> hits = new ArrayList<BaseRawTrackerHit>();
//        if (debug) {
//            int n = cdata.getNValues().size();
//            for (int i = 0; i < n; i++) {
//                System.out.println("cdata.N[" + i + "]=" + cdata.getNValues().get(i));
//            }
//            int ni = cdata.getItems().size();
//            for (int i = 0; i < ni; i++) {
//                System.out.println("cdata.type[" + i + "]=" + cdata.getTypes().get(i));
//            }
//        }
        

        while (cdata.index() + 1 < cdata.getItems().size()) {
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();

//            System.out.println("slot = " + slot);
//            System.out.println("trigger = " + trigger);
//            System.out.println("timestamp = " + timestamp);
//            System.out.println("nchannels = " + nchannels);
//            
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

                //System.out.println("id = " + id);
                if (id == null) {
                    FADCGenericHit hit = makeGenericRawHit(EventConstants.HODO_RAW_MODE, crate, slot, channel, cdata, nSamples);
                    processUnrecognizedChannel(hit);
                } else {
                    BaseRawTrackerHit hit = makeHodoRawHit(0, id, cdata, nSamples);
                    hits.add(hit);
                }
            }
        }
        return hits;
    }

    private Long daqToGeometryId(int crate, short slot, short channel) {

//        DaqId daqId = new DaqId(new int[]{crate, slot, channel});
//        EcalChannel ecalChannel = hodoConditions.getChannelCollection().findChannel(daqId);
//        if (ecalChannel == null) {
//            return null;
//        }
//        int ix = ecalChannel.getX();
//        int iy = ecalChannel.getY();
//        GeometryId geometryId = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy});
//        Long id = geometryId.encode();
        // A temporaty code to analyze Hodo EEL Data, until the conditions DB will be fixed
        int tmp = (slot - 3) * 16 + channel;
        long id = tmp + 1;

        return id;
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

        // Lowered the log level on these.  Otherwise they print too much. --JM
        if (count < 10) {
            LOGGER.finer(String.format("Crate %d, slot %d, channel %d not found in map", hit.getCrate(), hit.getSlot(), hit.getChannel()));
        } else if (count == 10) {
            LOGGER.fine(String.format("Crate %d, slot %d, channel %d not found in map: silencing further warnings for this channel", hit.getCrate(), hit.getSlot(), hit.getChannel()));
        }
    }

    
    void initialize() {
        
        subDetector = DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector(subdetectorName);

        // ECAL combined conditions object.
        //ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();

        helper = subDetector.getDetectorElement().getIdentifierHelper();
    }
    
    
}
