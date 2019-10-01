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

    public String toString() {
        StringBuffer sb = new StringBuffer("ImpactPoint: \n");
        sb.append("Global: " + _r[0] + " " + _r[1] + " " + _r[2]+"\n");
        sb.append("Local: " + _q[0] + " " + _q[1] + " " + _q[2]+"\n");
        return sb.toString();
    }
}
