package org.hps.recon.tracking;

import org.lcsim.event.GenericObject;

/**
 *
 * @author Matt Graham
 */
public class ShapeFitParameters implements GenericObject {

    private double _t0 = Double.NaN;
    private double _t0Err = Double.NaN;
    private double _amp = Double.NaN;
    private double _ampErr = Double.NaN;
//    private double _tp = Double.NaN;
//    private double _tpErr = Double.NaN;
    private double _chiProb = Double.NaN;

    public ShapeFitParameters() {
    }

    public ShapeFitParameters(double t0, double amplitude) {
        _t0 = t0;
        _amp = amplitude;
    }
    
    public ShapeFitParameters(GenericObject parameters) { 
        setT0(getT0(parameters)); 
        setT0Err(getT0Err(parameters)); 
        setAmp(getAmp(parameters)); 
        setAmpErr(getAmpErr(parameters)); 
        setChiProb(getChiProb(parameters)); 
    }

    public void setT0(double t0) {
        _t0 = t0;
    }

    public void setAmp(double amp) {
        _amp = amp;
    }

//    public void setTp(double tp) {
//        _tp = tp;
//    }

    public void setAmpErr(double _ampErr) {
        this._ampErr = _ampErr;
    }

    public void setT0Err(double _t0Err) {
        this._t0Err = _t0Err;
    }

//    public void setTpErr(double _tpErr) {
//        this._tpErr = _tpErr;
//    }

    public void setChiProb(double _chiProb) {
        this._chiProb = _chiProb;
    }

    public double getT0() {
        return _t0;
    }

    public double getAmp() {
        return _amp;
    }

//    public double getTp() {
//        return _tp;
//    }

    public double getT0Err() {
        return _t0Err;
    }

    public double getAmpErr() {
        return _ampErr;
    }

//    public double getTpErr() {
//        return _tpErr;
//    }

    public double getChiProb() {
        return _chiProb;
    }

    public static double getT0(GenericObject object) {
        return object.getDoubleVal(0);
    }

    public static double getT0Err(GenericObject object) {
        return object.getDoubleVal(1);
    }

    public static double getAmp(GenericObject object) {
        return object.getDoubleVal(2);
    }

    public static double getAmpErr(GenericObject object) {
        return object.getDoubleVal(3);
    }

//    public static double getTp(GenericObject object) {
//        return object.getDoubleVal(4);
//    }
//
//    public static double getTpErr(GenericObject object) {
//        return object.getDoubleVal(5);
//    }

    public static double getChiProb(GenericObject object) {
        return object.getDoubleVal(4);
    }

    @Override
    public int getNInt() {
        return 0;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 5;
    }

    @Override
    public int getIntVal(int index) {
        throw new UnsupportedOperationException("No int values in " + this.getClass().getSimpleName());
    }

    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No int values in " + this.getClass().getSimpleName());
    }

    @Override
    public double getDoubleVal(int index) {
        switch (index) {
            case 0:
                return _t0;
            case 1:
                return _t0Err;
            case 2:
                return _amp;
            case 3:
                return _ampErr;
//            case 4:
//                return _tp;
//            case 5:
//                return _tpErr;
            case 4:
                return _chiProb;
            default:
                throw new UnsupportedOperationException("Only 5 double values in " + this.getClass().getSimpleName());
        }

    }

    @Override
    public boolean isFixedSize() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("chiprob=%f\tA=%f\tAerr=%f\tT0=%f\tT0err=%f", _chiProb, _amp, _ampErr, _t0, _t0Err);
    }
}
