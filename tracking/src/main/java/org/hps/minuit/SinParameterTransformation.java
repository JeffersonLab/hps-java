package org.hps.minuit;

import org.apache.commons.math3.util.FastMath;

/**
 *
 * @version $Id: SinParameterTransformation.java 8584 2006-08-10 23:06:37Z duns $
 */
class SinParameterTransformation
{
    double int2ext(double value, double upper, double lower)
        {
            return lower + 0.5*(upper - lower)*(Math.sin(value) + 1.);
        }
    double ext2int(double value, double upper, double lower, MnMachinePrecision prec)
        {
            //double piby2 = 2.*FastMath.atan(1.);
            double piby2 = FastMath.PI / 2.;
            double distnn = 8.*FastMath.sqrt(prec.eps2());
            double vlimhi = piby2 - distnn;
            double vlimlo = -piby2 + distnn;
      
            double yy = 2.*(value - lower)/(upper - lower) - 1.;
            double yy2 = yy*yy;
            if(yy2 > (1. - prec.eps2()))
            {
                if(yy < 0.)
                {
                    return vlimlo;
                } 
                else
                {
                    return vlimhi;
                }
         
            } 
            else
            {
                return FastMath.asin(yy);
            }
        }
    double dInt2Ext(double value, double upper, double lower)
        {
            return 0.5*Math.abs((upper - lower)*FastMath.cos(value));
        }
}
