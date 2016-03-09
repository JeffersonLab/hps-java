package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: EcalConverterDriver.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class EcalConverterDriver extends Driver {
    
    Detector detector = null;

    String rawCollectionName;
    String ecalReadoutName = "EcalHits";
    String ecalCollectionName = "EcalCorrectedHits";
    int flags;
    double scale = 1.0;
//    double pedestal = 0.0;
    double period = 4.0;
    double dt = 0.0;

    public EcalConverterDriver() {
        flags = 0;
        flags += 1 << LCIOConstants.CHBIT_LONG; //store position
        flags += 1 << LCIOConstants.RCHBIT_ID1; //store cell ID
    }

//    public void setPedestal(double pedestal) {
//        this.pedestal = pedestal;
//    }
    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        this.detector = detector;
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
            // Get the list of ECal hits.
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);

            ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

            for (RawCalorimeterHit hit : hits) {
                newHits.add(HitDtoA(hit));
            }

            event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }
    }

//    private int AtoD(double amplitude, long cellID) {
//        return (int) Math.round(amplitude / scale);
//    }

    private double DtoA(int amplitude, long cellID) {
        return scale * amplitude;
    }

    private CalorimeterHit HitDtoA(RawCalorimeterHit hit) {
        double energy = DtoA(hit.getAmplitude(), hit.getCellID());
        return CalorimeterHitUtilities.create(energy, period * hit.getTimeStamp() + dt, hit.getCellID());
    }

//    private RawCalorimeterHit HitAtoD(CalorimeterHit hit) {
//        return new HPSFADCCalorimeterHit(hit.getCellID(), AtoD(hit.getRawEnergy(), hit.getCellID()), (int) Math.round(hit.getTime() / period), 0);
//    }
}
