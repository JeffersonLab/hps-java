package org.hps.analysis.ecal.cosmic;

import hep.aida.ref.function.AbstractIFunction;

/**
 * <p>
 * This class implements a function for fitting a signal plus pedestal
 * in non-pedestal subtracted raw mode ADC data.
 * <p>
 * It has four function parameters:
 * <ul>
 * <li><b>mean</b> - the mean of the Landau function
 * <li><b>sigma</b> - the width of the Landau
 * <li><b>norm</b> - the normalization parameter
 * <li><b>pedestal</b> - the pedestal constant
 * </ul> 
 * <p>
 * The class is designed to be used with the AIDA fitting API. 
 * 
 */
public class LandauFitFunction extends AbstractIFunction {

    // This is the backing function used to get the Landau PDF values.
    LandauPdf landauPdf = new LandauPdf();    
      
    /**
     * No argument constructor.
     */
    public LandauFitFunction() {
        this("");
    }
    
    /**
     * Constructor with function title.
     * The no arg constructor uses this one.
     * @param title The title of the function.
     */
    public LandauFitFunction(String title) {
        super();                
        this.variableNames = new String[] { "x0" };
        this.parameterNames = new String[] { "mean", "sigma", "norm", "pedestal" };        
        init(title);
    }

    /**
     * Get the Y value of the Landau function at X.
     * @value v The input X value (array of length 1).
     */
    @Override
    public double value(double[] v) {
        return this.parameter("pedestal") + this.parameter("norm") * landauPdf.getValue(v[0]);
    }

    /**
     * Set a parameter value on the function.
     * If these are mean or sigma values, they are pushed to the {@link #landauPdf} object.
     */
    @Override
    public void setParameter(String key, double value) throws IllegalArgumentException {
        super.setParameter(key, value);
        if (key.equals("mean")) {
            landauPdf.setMean(value);
        } else if (key.equals("sigma")) {
            landauPdf.setSigma(value);
        }         
    }

    /**
     * Set all parameters at once.
     * The mean and sigma are pushed to the {@link #landauPdf} object.
     */
    @Override
    public void setParameters(double[] parameters) throws IllegalArgumentException {        
        super.setParameters(parameters);
        landauPdf.setMean(parameters[0]);
        landauPdf.setSigma(parameters[1]);
    }
}
