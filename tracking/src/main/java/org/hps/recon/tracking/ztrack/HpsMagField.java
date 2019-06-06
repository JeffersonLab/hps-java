package org.hps.recon.tracking.ztrack;

import org.lcsim.geometry.FieldMap;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsMagField implements Field {

    private FieldMap _fieldmap;
    double[] pos = new double[3];

    public HpsMagField(FieldMap map) {
        _fieldmap = map;
    }

    public void GetFieldValue(double x, double y, double z, double[] field) {
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;
        _fieldmap.getField(pos, field);
    }
}
