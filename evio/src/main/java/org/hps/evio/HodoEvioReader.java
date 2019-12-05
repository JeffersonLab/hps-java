/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.GeometryId;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOConstants;

/**
 *
 * This class is similar to the ECalEvioReader. It was copied and modified to
 * work with Hodoscope data.
 *
 * @author rafopar
 */
public class HodoEvioReader extends EvioReader {

    private int bankTag = 0;
    private Class<?> hitClass = BaseRawCalorimeterHit.class;

    private HodoscopeChannelCollection hodoChannels = null;
    private IIdentifierHelper helper = null;

    private static final String readoutName = "HodoHits";
    private static final String subdetectorName = "Hodoscope";

    private Subdetector subDetector;

    //private static final Logger LOGGER = Logger.getLogger(HodoEvioReader.class.getPackage().getName());
    private static final Logger LOGGER = Logger.getLogger(HodoEvioReader.class.getCanonicalName());

    private int topBankTag, botBankTag;

    public HodoEvioReader(int topBankTag, int botBankTag) {
        this.topBankTag = topBankTag;
        this.botBankTag = botBankTag;
        this.hitCollectionName = "HodoReadoutHits";
    }

    public void setTopBankTag(int topBankTag) {
        this.topBankTag = topBankTag;
    }

    public void setBotBankTag(int botBankTag) {
        this.botBankTag = botBankTag;
    }

    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        LOGGER.entering(HodoEvioReader.class.getName(), "makeHits", lcsimEvent.getEventNumber());
        
        boolean foundHits = false;
        List<Object> hits = new ArrayList<Object>();

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
                    LOGGER.fine("Hodo bank tag: " + header.getTag() + "; childCount: " + bank.getChildCount());
                    /*
                    if (debug) {
                        System.out.println("Hodo bank tag: " + header.getTag() + "; childCount: " + bank.getChildCount());
                    }
                    */
                    try {
                        for (BaseStructure slotBank : bank.getChildrenList()) {
                            if (slotBank.getCompositeData() != null) { //skip SSP and TI banks, if any
                                for (CompositeData cdata : slotBank.getCompositeData()) {
                                    // We are reading the same data again. JEVIO remembers that the index was advanced already, so we need to set it to zero again.
                                    cdata.index(0);
                                    if (slotBank.getHeader().getTag() != bankTag) {
                                        bankTag = slotBank.getHeader().getTag();
                                        LOGGER.info(String.format("Hodo format tag: 0x%x\n", bankTag));
                                    }
                                    switch (slotBank.getHeader().getTag()) {
                                        case EventConstants.FADC_MODE1_BANK_TAG:
                                            LOGGER.fine("Making window hits");
                                            hits.addAll(makeWindowHits(cdata, crate));
                                            hitClass = RawTrackerHit.class;
                                            flags = 0;
                                            break;
                                        case EventConstants.FADC_PULSE_BANK_TAG:
                                            LOGGER.fine("Making pulse hits");
                                            hits.addAll(makePulseHits(cdata, crate));
                                            hitClass = RawTrackerHit.class;
                                            flags = 0;
                                            break;
                                        case EventConstants.FADC_PULSE_INTEGRAL_BANK_TAG:
                                            LOGGER.fine("Making pulse integral hits");
                                            hits.addAll(makeIntegralHitsMode3(cdata, crate));
                                            hitClass = RawCalorimeterHit.class;
                                            flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                            break;
                                        case EventConstants.FADC_PULSE_INTEGRAL_HIGHRESTDC_BANK_TAG:
                                            LOGGER.fine("Making pulse integral high res TDC hits");
                                            hits.addAll(makeIntegralHitsMode7(cdata, crate));
                                            hitClass = RawCalorimeterHit.class;
                                            flags = (1 << LCIOConstants.RCHBIT_TIME); //store timestamp
                                            break;
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

        LOGGER.fine("Adding " + hits.size() + " Hodoscope hits to " + readoutName);
        
        lcsimEvent.put(this.hitCollectionName, hits, hitClass, flags, readoutName);

        return foundHits;
    }

    private BaseRawTrackerHit makeHodoRawHit(int time, ArrayList<Long> id, CompositeData cdata, int nSamples) {

        short[] adcValues = new short[nSamples];
        for (int i = 0; i < nSamples; i++) {
            adcValues[i] = cdata.getShort();
            //System.out.println("ADC["+i+"] = " + adcValues[i]);
        }

        Long id_hit = id.get(0);
        Long id_det = id.get(1);

        IDetectorElementContainer srch = subDetector.getDetectorElement().findDetectorElement(new Identifier(id_det));
        if (srch.size() == 0) {
            throw new RuntimeException("No detector element was found for hit ID: " + helper.unpack(new Identifier(id_det)));
        }
        return new BaseRawTrackerHit(
                time,
                id_hit,
                adcValues,
                new ArrayList<SimTrackerHit>(),
                srch.get(0)
        );
    }

    private List<BaseRawTrackerHit> makeWindowHits(CompositeData cdata, int crate) {
        List<BaseRawTrackerHit> hits = new ArrayList<BaseRawTrackerHit>();
        
        while (cdata.index() + 1 < cdata.getItems().size()) {
            int index = cdata.index();
            short slot = cdata.getByte();
            int trigger = cdata.getInt();
            long timestamp = cdata.getLong();
            int nchannels = cdata.getNValue();
            LOGGER.finest("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            /*
            if (debug) {
                System.out.println("slot#=" + slot + "; trigger=" + trigger + "; timestamp=" + timestamp + "; nchannels=" + nchannels);
            }
            */
            for (int j = 0; j < nchannels; j++) {
                short channel = cdata.getByte();
                int nSamples = cdata.getNValue();

                LOGGER.finest("  channel=" + channel + "; nSamples=" + nSamples);
                /*if (debug) {
                    System.out.println("  channel=" + channel + "; nSamples=" + nSamples);
                }*/

                ArrayList<Long> ids = daqToGeometryId(crate, slot, channel);

                LOGGER.finest("The long ids are: " + ids);
                /*
                if (debug) {
                    System.out.println("The long ids are: " + ids);
                }*/

                if (ids != null) {  // We found an actual Hodoscope channel.
                    BaseRawTrackerHit hit = makeHodoRawHit(0, ids, cdata, nSamples);
                    hits.add(hit);
                } else {        // Not a hodoscope hit, so wind forward  -- MWH.
                    cdata.index(cdata.index() + nSamples);  // Wind the pointer forward by nSamples.
                }
            }
        }
        return hits;
    }

    private ArrayList<Long> daqToGeometryId(int crate, short slot, short channel) {

        // ====================== Rafo =======================
        // Unlike to the ECal case, where each detector element is readout with a single channel
        // here for the hodoscope, there are detector elements (tiles) that are readout with two different channels
        // 
        // The code below, previously was assigning the hole value to 0, 
        // That way you would get an iD, which is ok for the detector element identification, howevere
        // This id is not correct, when one wants to knwo which PMT channel is actually this hit belongs to.
        // In particular during the recon, I was getting an error, that it can not find a cellID
        
        // Now as a possible solution (Possibly not the best solution), this method will return two IDs
        // The 1st one, is clauclulated taking into account the hit, while the 2nd one is calculated with 
        // the "hole=0" assumption.
        
        // Further depending on the intention, one can use ids.get(0) if one wants to check the hit ID, or ids.get(1), if onw
        // wants to identify the detector element.
        
        
        ArrayList<Long> iDs = new ArrayList<>();

        if (hodoChannels == null) {  // The hodoChannels were not initialized, probably data that did not have a Hodoscope.  -- MWH.
            iDs = null;
            return iDs;
        }
        HodoscopeChannel hodoChannel = hodoChannels.findChannel(crate, slot, channel);

        if (hodoChannel == null) {
            iDs = null;
            return iDs;
        }
        int ix = hodoChannel.getIX();
        int iy = hodoChannel.getIY();
        int ilayer = hodoChannel.getLayer();
        int ihole = hodoChannel.getHole();


        /* The 'hole' ID value is set to zero to match with the pixel detector elements in the geometry. --JM */
        //int ihole = 0; 
        
        // === Getting the ID which will be used to identify the actuall signal.  Rafo ====
        GeometryId geometryId_withHole = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy, ilayer, ihole});
        Long id_with_hole = geometryId_withHole.encode();

        iDs.add(id_with_hole);

        // === Getting the ID which will be used to identify the detector element, NOTE: hole value is set to 0.  Rafo ====
        GeometryId geometryId_NoHole = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy, ilayer, 0});
        Long id_No_hole = geometryId_NoHole.encode();

        iDs.add(id_No_hole);

        return iDs;
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
                ArrayList<Long> ids = daqToGeometryId(crate, slot, channel);
                for (int k = 0; k < npulses; k++) {
                    short pulseNum = cdata.getByte();
                    int sampleCount = cdata.getNValue();

                    if (ids == null) {
                        cdata.index(cdata.index() + sampleCount);
                    } else {
                        BaseRawTrackerHit hit = makeHodoRawHit(pulseNum, ids, cdata, sampleCount);
                        hits.add(hit);
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
                ArrayList<Long> ids = daqToGeometryId(crate, slot, channel);
                for (int k = 0; k < npulses; k++) {
                    short pulseTime = cdata.getShort();
                    int pulseIntegral = cdata.getInt();
                    if (debug) {
                        System.out.println("    pulseTime=" + pulseTime + "; pulseIntegral=" + pulseIntegral);
                    }
                    if (ids != null) {
                        hits.add(new BaseRawCalorimeterHit(ids.get(0), pulseIntegral, pulseTime));
                    }
                }
            }
        }
        return hits;
    }

    private List<RawCalorimeterHit> makeIntegralHitsMode7(CompositeData cdata, int crate) {
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
                ArrayList<Long> ids = daqToGeometryId(crate, slot, channel);
                for (int k = 0; k < npulses; k++) {
                    short pulseTime = cdata.getShort();
                    int pulseIntegral = cdata.getInt();
                    short amplLow = cdata.getShort();
                    short amplHigh = cdata.getShort();
                    if (debug) {
                        System.out.println("    pulseTime=" + pulseTime + "; pulseIntegral=" + pulseIntegral + "; amplLow=" + amplLow + "; amplHigh=" + amplHigh);
                    }
                    if (ids != null) {
                        RawCalorimeterHit hit = new BaseRawCalorimeterHit(ids.get(0), pulseIntegral, pulseTime);
                        hits.add(hit);
                    }
                }
            }
        }
        return hits;
    }

    /**
     * If this is called, then a check was already made in the builder for the "hodo_channels" conditions 
     * collection.  Should either the subdetector or "hodo_channels" collection be missing, then something
     * has gone wrong, and a fatal exception will be thrown.
     */
    void initialize() {

        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        Detector det = mgr.getDetectorObject();
        
        if (det.getSubdetectorNames().contains(subdetectorName)) {
            subDetector = DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector(subdetectorName);
            helper = subDetector.getDetectorElement().getIdentifierHelper();
        } else {
            // Fatal error if no Hodoscope subdetector is defined.
            LOGGER.severe("No Hodoscope subdetector was found in the detector description!");
            throw new RuntimeException("Hodoscope subdetect was not found.");
        }

        if (mgr.hasConditionsRecord("hodo_channels")) {
            hodoChannels = mgr.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        } else {
            // Fatal error if no Hodoscope channels are defined.
            LOGGER.severe("No HodoscopeChannelCollection found for run " + mgr.getRun() + " in the conditions database!");
            throw new RuntimeException("The Hodoscope channels collection was not found.");
        }
    }
}
