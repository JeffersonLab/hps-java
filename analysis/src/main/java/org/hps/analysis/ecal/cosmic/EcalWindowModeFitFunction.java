package org.hps.analysis.ecal.cosmic;

import hep.aida.ref.function.AbstractIFunction;

public class EcalWindowModeFitFunction extends AbstractIFunction {

    LandauPdf landauPdf = new LandauPdf();    
      
    public EcalWindowModeFitFunction() {
        this("");
    }
    
    public EcalWindowModeFitFunction(String title) {
        super();                
        this.variableNames = new String[] { "x0" };
        this.parameterNames = new String[] { "mean", "sigma", "norm", "pedestal" };        
        init(title);
    }
            
    @Override
    public double value(double[] v) {
        return this.parameter("pedestal") + this.parameter("norm") * landauPdf.getValue(v[0]);
    }

    @Override
    public void setParameter(String key, double value) throws IllegalArgumentException {
        super.setParameter(key, value);
        if (key.equals("mean")) {
            landauPdf.setMean(value);
        } else if (key.equals("sigma")) {
            landauPdf.setSigma(value);
        }         
    }

    @Override
    public void setParameters(double[] parameters) throws IllegalArgumentException {        
        super.setParameters(parameters);
        landauPdf.setMean(parameters[0]);
        landauPdf.setSigma(parameters[1]);
    }
}
