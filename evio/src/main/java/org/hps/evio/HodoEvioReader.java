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
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.DaqId;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
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
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOConstants;


/**
 *
 * @author rafopar
 */
public class HodoEvioReader extends EvioReader {

    //private static HodoConditions hodoConditions = null;
    private static EcalConditions hodoConditions = null;           // <<=============== This should be Hodo Conditions
    private static IIdentifierHelper helper = null;

    private static final String readoutName = "HodoHits";
    private static final String subdetectorName = "Hodo";    
    
    private Subdetector subDetector;

    private static final String genericHitCollectionName = "FADCGenericHits";
    private List<FADCGenericHit> genericHits;
    
    private final Map<List<Integer>, Integer> genericHitCount = new HashMap<List<Integer>, Integer>();   
    
    private static final Logger LOGGER = Logger.getLogger(EcalEvioReader.class.getPackage().getName());   
    
    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        return true; // Temporrary, just for not givin syntax error
    }
 
     private BaseRawTrackerHit makeECalRawHit(int time, long id, CompositeData cdata, int nSamples) {
        short[] adcValues = new short[nSamples];
        for (int i = 0; i < nSamples; i++) {
            adcValues[i] = cdata.getShort();
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

                if (id == null) {
                    FADCGenericHit hit = makeGenericRawHit(EventConstants.ECAL_RAW_MODE, crate, slot, channel, cdata, nSamples);
                    processUnrecognizedChannel(hit);
                } else {
                    BaseRawTrackerHit hit = makeECalRawHit(0, id, cdata, nSamples);
                    hits.add(hit);
                }
            }
        }
        return hits;
    }
   
       private Long daqToGeometryId(int crate, short slot, short channel) {
        DaqId daqId = new DaqId(new int[]{crate, slot, channel});
        EcalChannel ecalChannel = hodoConditions.getChannelCollection().findChannel(daqId);
        if (ecalChannel == null) {
            return null;
        }
        int ix = ecalChannel.getX();
        int iy = ecalChannel.getY();
        GeometryId geometryId = new GeometryId(helper, new int[]{subDetector.getSystemID(), ix, iy});
        Long id = geometryId.encode();
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
      
       
}
