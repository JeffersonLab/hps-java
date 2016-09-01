package org.hps.users.spaul.bh;

public class Polynomial implements IFitResult {
    double p[];
    int k;
    Polynomial(int k){
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
        return y;
    }
    public int paramCount(){return k;}
}
