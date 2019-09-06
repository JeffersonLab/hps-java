package org.hps.analysis.alignment.straighttrack;

/**
 *
 * @author Norman Graf
 */
public class Hit {
    
    private double[] _uvm; //measurements (local coordinates)
    private double[] _wt; //weights
    
    public Hit(double[] u, double[] w)
    {
        _uvm = u;
        _wt = w;
    }
    
    public double[] uvm()
    {
        return _uvm;
    }
    
    public double[] wt()
    {
        return _wt;
    }
    
    public String toString()
    {
        return new StringBuffer("Hit: "+_uvm[0]).toString();
    }
}
