package org.hps.users.spaul.bh;

import java.util.Arrays;

public class PolynomialPlusGaussian implements IFitResult{

    /**
     * The square root of two pi
     */
    static final double sqrt2pi = Math.sqrt(2*Math.PI);
    
    
    //parameters of the fit
    double p[];
    int k;
    double mean, sigma, N;
    
    PolynomialPlusGaussian(int k){
        this.p = new double[k];
        this.k = k;
    }
    public double get(double m){
        double y = 0;
        double powM = 1;
        for(int i = 0; i<k; i++){
            y += powM*p[i];
            powM *= m;
        }
        return y + N*Math.exp(-Math.pow((m-mean)/sigma,2)/2)
            /(sqrt2pi*sigma);
    }
    public int paramCount(){
        return k + 3;
    }
    public String toString(){
        return "p + g " + Arrays.toString(p) + " " + mean + " " + sigma + " " + N; 
    }
}
