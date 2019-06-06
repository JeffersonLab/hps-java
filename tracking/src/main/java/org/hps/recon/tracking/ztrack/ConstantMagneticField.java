package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class ConstantMagneticField implements Field {

    private double[] _fieldValue = new double[3];

    public ConstantMagneticField(double Bx, double By, double Bz) {
        _fieldValue[0] = Bx;
        _fieldValue[1] = By;
        _fieldValue[2] = Bz;
    }

    public ConstantMagneticField(double[] fieldValue) {
        System.arraycopy(fieldValue, 0, _fieldValue, 0, 3);
    }

    public void GetFieldValue(double x, double y, double z, double[] field) {
        System.arraycopy(_fieldValue, 0, field, 0, 3);
    }
}
