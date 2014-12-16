package org.hps.analysis.ecal.cosmic;

import hep.aida.ref.function.AbstractIFunction;

public class MoyalFitFunction extends AbstractIFunction {

    /**
     * No argument constructor.
     */
    public MoyalFitFunction() {
        this("");
    }
    
    /**
     * Constructor with function title.
     * The no arg constructor uses this one.
     * @param title The title of the function.
     */
    public MoyalFitFunction(String title) {
        super();                
        this.variableNames = new String[] { "x0" };
        this.parameterNames = new String[] { "pedestal", "norm", "mpv", "width" };
        init(title);
    }
    
    @Override
    public double value(double[] x) {
        double a = -(x[0] - this.parameter("mpv")) / this.parameter("width");
        return this.parameter("pedestal") + this.parameter("norm") * (1 / (Math.sqrt(2 * Math.PI) * this.parameter("width"))) * Math.exp(-0.5 * (Math.exp(a) - a));
    }                  
}
