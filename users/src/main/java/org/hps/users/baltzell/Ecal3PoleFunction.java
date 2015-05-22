package org.hps.users.baltzell;

import hep.aida.ref.function.AbstractIFunction;


public class Ecal3PoleFunction extends AbstractIFunction {
 
    protected double pedestal=0;
    protected double time0=0;
    protected double amplitude=0;
    protected double shape=0;
   
    public Ecal3PoleFunction() {
        this("");
    }
    public Ecal3PoleFunction(String title) {
        super();
        this.variableNames = new String[] { "time" };
        this.parameterNames = new String[] { "pedestal","time0","amplitude","shape" };
        init(title);
    }
    
    @Override
    public double value(double[] v) {
        final double time = v[0];
        if (time <= time0) return pedestal;
        return pedestal 
             + amplitude 
             * Math.pow(time-time0,2)
             / (2*shape)
             * Math.exp(-(time-time0)/shape);
    }
   
    @Override
    public void setParameters(double[] pars) throws IllegalArgumentException {        
        super.setParameters(pars);
        pedestal=pars[0];
        time0=pars[1];
        amplitude=pars[2];
        shape=pars[3];
        System.err.println(String.format("%8.2f %8.2f %8.2f %8.2f",pars[0],pars[1],pars[2],pars[3]));
    }

    @Override
    public void setParameter(String key,double value) throws IllegalArgumentException{
        super.setParameter(key,value);
        if      (key.equals("pedestal"))  pedestal=value;
        else if (key.equals("time0"))     time0=value;
        else if (key.equals("amplitude")) amplitude=value;
        else if (key.equals("shape"))     shape=value;
    }
}
