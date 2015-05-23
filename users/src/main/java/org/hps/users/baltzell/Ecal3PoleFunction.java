package org.hps.users.baltzell;

import hep.aida.ref.function.AbstractIFunction;


public class Ecal3PoleFunction extends AbstractIFunction {
 
    protected double pedestal=0;
    protected double time0=0;
    protected double integral=0;
    protected double width=0;
    public boolean debug=false;
   
    public Ecal3PoleFunction() {
        this("");
    }
    public Ecal3PoleFunction(String title) {
        super();
        this.variableNames = new String[] { "time" };
        this.parameterNames = new String[] { "pedestal","time0","integral","width" };
        init(title);
    }
   
    void setDebug(boolean debug) { this.debug=debug; }
    
    @Override
    public double value(double[] v) {
        final double time = v[0];
        if (time <= time0) return pedestal;
        return pedestal 
             + integral / Math.pow(width,3) / 2 
             * Math.pow(time-time0,2)
             * Math.exp(-(time-time0)/width);
    }
   
    @Override
    public void setParameters(double[] pars) throws IllegalArgumentException {        
        super.setParameters(pars);
        pedestal=pars[0];
        time0=pars[1];
        integral=pars[2];
        width=pars[3];
        if (debug) System.err.println(String.format("%8.2f %8.2f %8.2f %8.2f",pars[0],pars[1],pars[2],pars[3]));
    }

    @Override
    public void setParameter(String key,double value) throws IllegalArgumentException{
        super.setParameter(key,value);
        if      (key.equals("pedestal"))  pedestal=value;
        else if (key.equals("time0"))     time0=value;
        else if (key.equals("integral"))  integral=value;
        else if (key.equals("width"))     width=value;
    }
    
}
