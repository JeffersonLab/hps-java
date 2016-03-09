package org.hps.users.spaul.feecc;

public abstract class FormFactor {

    abstract double getFormFactor(double q2);

    double getFormFactorSquared(double q2){
        return Math.pow(getFormFactor(q2), 2);
    }
    static double hbarc = .197;
    static FormFactor carbon = new FormFactor(){

        @Override
        public double getFormFactor(double Q2) {
            double Z= 6;
            double rp = 0.8786;
            double a = 1.64;
            double b = Math.sqrt(a*a*(1-1/12.)+rp*rp);
            return (1-(Z-2)/(6*Z)*a*a*Q2/(hbarc*hbarc))*Math.exp(-1/4.*b*b*Q2/(hbarc*hbarc));
        }

    };
    static FormFactor tungsten = new FormFactor(){

        @Override
        public double getFormFactor(double Q2) {
            double Z= 74;
            double r = 6.87;
            double x = Math.sqrt(Q2*r*r/(hbarc*hbarc));
            double F_calc = 3/(x*x*x)*(Math.sin(x)-x*Math.cos(x));
            return F_calc;
        }
        
    };
    static FormFactor get(int Z){
        if(Z == 6)
            return carbon;
        if(Z == 74)
            return tungsten;
        System.err.println("Form Factor for " + Z + " not implemented");
        return null;
    }
}
