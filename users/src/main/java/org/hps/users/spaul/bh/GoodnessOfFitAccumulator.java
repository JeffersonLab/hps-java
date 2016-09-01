package org.hps.users.spaul.bh;

public class GoodnessOfFitAccumulator{
    private double logLikelinessSum = 0;
    private double chi2Sum = 0;
    /**
     * Accumulate a data point
     * @param h the measured value
     * @param f the fit value
     */
    public void accumulate(double h, double f) {
        logLikelinessSum+=2*(f-h+h*Math.log(h/f));
        chi2Sum+= (f-h)*(f-h)/h;
        total_dof ++;
    } 
    /**
     *@return -2*log(L)
     */
    public double getLogLikeliness() {
        return logLikelinessSum;
    }
    /**
     *@return Pearson's chi^2
     */
    public double getChi2() {
        return chi2Sum;
    }
    /**
     *@return the number of degrees of freedom
     */
    public double getDOF(){
        return total_dof;
    }
    /**
     * @return the number of parameters in the fit
     */
    public double getFitParameterCount(){
        return fitParamCount;
    }

    /**
     * resets the sums
     */
    public void reset(){
        logLikelinessSum =0;
        chi2Sum = 0;
        total_dof = -fitParamCount;
    }
    private int fitParamCount;
    private int total_dof;
    /**
     * Constructs an instance of GoodnessOfFit.
     * @param fitParamCount the number of parameters in the fit
     */
    public GoodnessOfFitAccumulator(int fitParamCount){
        this.fitParamCount = fitParamCount;
        this.total_dof = -fitParamCount;
    }

}
