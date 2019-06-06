package org.hps.recon.tracking.ztrack;

import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.magfield.AbstractMagneticField;
import org.lcsim.recon.tracking.spacegeom.CartesianPointVector;
import org.lcsim.recon.tracking.spacegeom.SpacePoint;
import org.lcsim.recon.tracking.spacegeom.SpacePointTensor;
import org.lcsim.recon.tracking.spacegeom.SpacePointVector;

/**
 *
 * @author ngraf
 */
public class TrfField extends AbstractMagneticField {

    private FieldMap _fieldmap;
    double[] pos = new double[3];
    double[] field = new double[3];

    public TrfField(FieldMap map) {
        _fieldmap = map;
    }

    /**
     *
     * @param sp Cartesian Space Point in cm
     * @return Magnetic Field value in Tesla
     */
    @Override
    public SpacePointVector field(SpacePoint sp) {
        pos[0] = sp.x()*10.; //back to mm
        pos[1] = sp.y()*10.; // back to mm
        pos[2] = sp.z()*10.; // back to mm
        _fieldmap.getField(pos, field);
//        System.out.println("TrfField:");
//        System.out.println("pos   "+Arrays.toString(pos));
//        System.out.println("field "+Arrays.toString(field));
        
        return new CartesianPointVector(sp, field[0], field[1], field[2]);
    }

    @Override
    public SpacePointVector field(SpacePoint sp, SpacePointTensor spt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
