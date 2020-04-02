package org.hps.record.daqconfig2019;

/**
 * Class <code>PDECutConfig2019</code> is an implementation of the abstract
 * <code>AbstractCutConfig2019</code> for position-dependent-energy (PED) cuts.
 * The PED is expressed by a 3rd order polynominal, where there are 4 parameters.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class PDECutConfig2019 extends AbstractConfig2019<Double> {
    private static final int parC0 = 0;
    private static final int parC1 = 1;
    private static final int parC2 = 2;
    private static final int parC3 = 3;
    
    /**
     * Instantiates a new <code>PDECutConfig2019</code> object.
     */
    public PDECutConfig2019() { super(4); }
    
    /**
     * Gets PDE parameter C0.
     * @return Returns PDE parameter C0.
     */
    public double getParC0() {
        return getValue(parC0);
    }
    
    /**
     * Sets PED parameter C0.
     * @param value - The new PDE parameter C0.
     */
    void setParC0(double value) {
        setValue(parC0, value);
    }
    
    /**
     * Gets PDE parameter C1.
     * @return Returns PDE parameter C1.
     */
    public double getParC1() {
        return getValue(parC1);
    }
    
    /**
     * Sets PED parameter C1.
     * @param value - The new PDE parameter C1.
     */
    void setParC1(double value) {
        setValue(parC1, value);
    }
    
    /**
     * Gets PDE parameter C2.
     * @return Returns PDE parameter C2.
     */
    public double getParC2() {
        return getValue(parC2);
    }
    
    /**
     * Sets PED parameter C2.
     * @param value - The new PDE parameter C2.
     */
    void setParC2(double value) {
        setValue(parC2, value);
    }
    
    /**
     * Gets PDE parameter C3.
     * @return Returns PDE parameter C3.
     */
    public double getParC3() {
        return getValue(parC3);
    }
    
    /**
     * Sets PED parameter C3.
     * @param value - The new PDE parameter C3.
     */
    void setParC3(double value) {
        setValue(parC3, value);
    }    
}