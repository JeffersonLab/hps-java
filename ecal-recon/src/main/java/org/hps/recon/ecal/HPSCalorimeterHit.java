package org.hps.recon.ecal;

import hep.physics.vec.Hep3Vector;

import java.util.Comparator;

import org.hps.conditions.deprecated.EcalConditions;
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
     */
    public HPSCalorimeterHit(double energy, double time, long id, int type) {
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
            IDetectorElementContainer detectorElements = EcalConditions.getSubdetector().getDetectorElement().findDetectorElement(getIdentifier());
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
}
