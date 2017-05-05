package org.hps.detector.ecal;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.solids.Trd;

/**
 * This class implements the geometry API for ECal crystals in the HPS experiment.
 */
public final class EcalCrystal extends DetectorElement {
    
    private Hep3Vector positionFront;

    /**
     * Class constructor.
     * @param name The name of the DetectorElement.
     * @param parent The parent component.
     * @param path The physical path.
     * @param id The component's ID.
     */
    public EcalCrystal(String name, IDetectorElement parent, String path, IIdentifier id) {
        super(name, parent, path, id);
    }
    
    /**
     * Get the X index of this crystal.
     * @return The X index of this crystal.
     */
    public int getX() {
        return getIdentifierHelper().getValue(getIdentifier(), "ix");
    }
    
    /**
     * Get the Y index of this crystal.
     * @return The Y index of this crystal.
     */
    public int getY() {
        return getIdentifierHelper().getValue(getIdentifier(), "iy");
    }
               
    /**
     * Get the global center position of the XY plane in the front of the crystal.
     * This is used in the reconstruction clustering algorithm for determining the 
     * "corrected" hit position, so it is best to cache it once used, for performance 
     * purposes.
     * @return The center position of the XY plane in the front of the crystal.
     */
    public Hep3Vector getPositionFront() {
        if (positionFront == null) {
            IGeometryInfo geom = getGeometry();
            double[] p = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()), 
                            (Hep3Vector) new BasicHep3Vector(0, 0, -1 * ((Trd) geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();
            positionFront = new BasicHep3Vector(p[0], p[1], p[2]);
        }
        return positionFront;
    }
    
}
