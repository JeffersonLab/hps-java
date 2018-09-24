package org.hps.analysis.alignment.straighttrack;

/**
 *
 * @author Norman Graf
 */
public class ImpactPoint {

    private double _ti; //parameter at impact point
    private double[] _q;//local  u,v,w coordinates
    private double[] _r;//global x,y,z coordinates

    public ImpactPoint(double ti, double[] q, double[] r) {
        _ti = ti;
        _q = q;
        _r = r;
    }

    public double ti() {
        return _ti;
    }

    public double[] q() {
        return _q;
    }

    public double[] r() {
        return _r;
    }
}
