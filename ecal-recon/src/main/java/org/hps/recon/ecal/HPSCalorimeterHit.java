package org.hps.recon.ecal;

import hep.physics.vec.Hep3Vector;

import java.util.Comparator;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsUtil;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.geometry.Detector;

/**
 * An implementation of CalorimeterHit, with a constructor that sets rawEnergy
 * for use in ECalReadout
 *
 * @author Sho Uemura
 * @version $Id: HPSCalorimeterHit.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class HPSCalorimeterHit extends BaseCalorimeterHit {

    Detector detector = null;    
    static EcalConditions ecalConditions = null;
    static IIdentifierHelper helper = null;
    static EcalChannelCollection channels = null; 

    /**
     * Fully qualified constructor that sets rawEnergy
     *
     * @param energy   Raw energy for this cell
     * @param position Global Cartesian coordinate for this cell
     * @param time     Time of energy deposition
     * @param id       Cell ID
     * @param type     Type
     * WARNING: setDetector(detector° must be called after initialization
     */
    public HPSCalorimeterHit(double energy, double time, long id, int type) {
        this.rawEnergy = energy;
//      if (position != null) {
//          this.positionVec = new BasicHep3Vector(position);
//      } else {
//          positionVec = null;
//      }
      this.time = time;
      this.id = id;
      this.type = type;
    }
    
    /**
     * Fully qualified constructor that sets rawEnergy
     *
     * @param energy   Raw energy for this cell
     * @param position Global Cartesian coordinate for this cell
     * @param time     Time of energy deposition
     * @param id       Cell ID
     * @param type     Type
     * WARNING: setDetector(detector) must be called after initialization
     */
    public HPSCalorimeterHit(CalorimeterHit hit) {
        this.rawEnergy = hit.getRawEnergy();
//      if (position != null) {
//          this.positionVec = new BasicHep3Vector(position);
//      } else {
//          positionVec = null;
//      }
      this.time = hit.getTime();
      this.id = hit.getCellID();
      this.type = hit.getType();
    }
    
    /**
     * Fully qualified constructor that sets rawEnergy
     *
     * @param energy   Raw energy for this cell
     * @param position Global Cartesian coordinate for this cell
     * @param time     Time of energy deposition
     * @param id       Cell ID
     * @param type     Type
     */
    public void setParameters(double energy, double time, long id, int type) {
        this.rawEnergy = energy;
//        if (position != null) {
//            this.positionVec = new BasicHep3Vector(position);
//        } else {
//            positionVec = null;
//        }
        this.time = time;
        this.id = id;
        this.type = type;
    }

    @Override
    public IDetectorElement getDetectorElement() {
        if (de == null) {
//            findDetectorElementByPosition();
            IDetectorElementContainer detectorElements = detector.getDetectorElement().findDetectorElement(getIdentifier());
            if (detectorElements.size() != 1) {
                throw new RuntimeException("Expected exactly one DetectorElement matching ID " + getIdentifier() + ", got " + detectorElements.size());
            } else {
                de = detectorElements.get(0);
            }
        }
        // setupDetectorElement();
        return de;
    }

    @Override
    public double[] getPosition() {
        return getPositionVec().v();
    }

    @Override
    public Hep3Vector getPositionVec() {
        return this.getDetectorElement().getGeometry().getPosition();
    }

    static class TimeComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            return Double.compare(o1.getTime(), o2.getTime());
        }
    }
    
    /** 
     * Must be set when an object HPSCalorimeterHit is created.
     * @param detector (long)
     */
    public void setDetector(Detector detector) {
        this.detector = detector;
        
        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        
        // List of channels.
        channels = ecalConditions.getChannelCollection();
        
        // ID helper.
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
        
//        System.out.println("You are now using the database conditions for HPSCalorimeterHit.");
    }
    
    
}
