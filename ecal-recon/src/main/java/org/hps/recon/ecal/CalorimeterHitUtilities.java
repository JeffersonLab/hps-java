package org.hps.recon.ecal;

import org.hps.detector.ecal.EcalCrystal;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * This is a simple set of utility methods for creating CalorimeterHit objects. 
 */
public final class CalorimeterHitUtilities {

    /**
     * This class is purely static.
     */
    private CalorimeterHitUtilities() {        
    }
    
    public static final CalorimeterHit create(double energy, double time, long id, int type) {
        return create(energy, time, id, type, null);
    }
    
    public static final CalorimeterHit create(double energy, double time, long id) {
        return create(energy, time, id, 0, null);
    }    
    
    public static final CalorimeterHit create(double energy, double time, long id, LCMetaData meta) {
        return create(energy, time, id, 0, meta);
    }    
    
    public static final CalorimeterHit create(double energy, double time, long id, int type, LCMetaData metaData) {
        return new BaseCalorimeterHit(energy, energy, 0, time, id, null, type, metaData);
    }    
    
    public static EcalCrystal findCrystal(CalorimeterHit hit) {
        return (EcalCrystal)hit.getSubdetector().getDetectorElement().findDetectorElement(hit.getIdentifier()).get(0);
    }
    
    public static IGeometryInfo findGeometryInfo(CalorimeterHit hit) {
        return findCrystal(hit).getGeometry();
    }
}
