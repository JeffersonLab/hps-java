package org.hps.readout.ecal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * Performs readout of ECal hits. No time evolution - this just integrates all
 * hits in a cycle.
 */
public class SimpleEcalReadoutDriver extends EcalReadoutDriver<CalorimeterHit> {

    //buffer for deposited energy

    Map<Long, Double> eDepMap = null;

    public SimpleEcalReadoutDriver() {
        hitClass = CalorimeterHit.class;
    }

    @Override
    protected void readHits(List<CalorimeterHit> hits) {
        for (Long cellID : eDepMap.keySet()) {
//            int ix = dec.getValue("ix");
//            int iy = dec.getValue("iy");
//            //temporary hack to disable crystals and flip X coordinate
//            int side = dec.getValue("side");
//            if (iy == 1 && ix*side >= -10 && ix*side <= -2)
//                continue;
            if (eDepMap.get(cellID) > threshold) {
                //HPSCalorimeterHit h = new HPSCalorimeterHit(eDepMap.get(cellID), readoutTime(), cellID, hitType);                
                CalorimeterHit h = new BaseCalorimeterHit(eDepMap.get(cellID), eDepMap.get(cellID), 0, readoutTime(), cellID, null, hitType, null);                
                hits.add(h);
            }
        }
        //reset hit integration
        eDepMap = new HashMap<Long, Double>();
    }

    @Override
    protected void putHits(List<CalorimeterHit> hits) {
        //fill the readout buffers
        for (CalorimeterHit hit : hits) {
            Double eDep = eDepMap.get(hit.getCellID());
            if (eDep == null) {
                eDepMap.put(hit.getCellID(), hit.getRawEnergy());
            } else {
                eDepMap.put(hit.getCellID(), eDep + hit.getRawEnergy());
            }
        }
    }

    @Override
    protected void initReadout() {
        //initialize buffers
        eDepMap = new HashMap<Long, Double>();
    }
}
