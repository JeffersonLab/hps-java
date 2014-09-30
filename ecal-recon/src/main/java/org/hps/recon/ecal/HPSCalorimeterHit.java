package org.hps.recon.ecal;

import hep.physics.vec.Hep3Vector;

import java.util.Comparator;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * An implementation of CalorimeterHit, with a constructor that sets rawEnergy
 * for use in ECalReadout
 *
 * @author Sho Uemura
 * @version $Id: HPSCalorimeterHit.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class HPSCalorimeterHit extends BaseCalorimeterHit {

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
        this.correctedEnergy = energy;
        this.time = time;
        this.id = id;
        this.type = type;
    }
    
//    /**
//     * Fully qualified constructor that sets rawEnergy
//     *
//     * @param energy   Raw energy for this cell
//     * @param position Global Cartesian coordinate for this cell
//     * @param time     Time of energy deposition
//     * @param id       Cell ID
//     * @param type     Type
//     * WARNING: setDetector(detector) must be called after initialization
//     */
//    public HPSCalorimeterHit(CalorimeterHit hit) {
//        this.rawEnergy = hit.getRawEnergy();
////      if (position != null) {
////          this.positionVec = new BasicHep3Vector(position);
////      } else {
////          positionVec = null;
////      }
//      this.time = hit.getTime();
//      this.id = hit.getCellID();
//      this.type = hit.getType();
//    }
    
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
            IDetectorElementContainer detectorElements = getSubdetector().getDetectorElement().findDetectorElement(getIdentifier());
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
        if (positionVec == null) {
            positionVec = this.getDetectorElement().getGeometry().getPosition();
        }
        return super.getPosition();
    }

    @Override
    public Hep3Vector getPositionVec() {
        if (positionVec == null) {
            positionVec = this.getDetectorElement().getGeometry().getPosition();
        }
        return super.getPositionVec();
    }

    static class TimeComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            return Double.compare(o1.getTime(), o2.getTime());
        }
    }
}
