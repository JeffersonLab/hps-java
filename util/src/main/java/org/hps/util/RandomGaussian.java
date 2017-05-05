package org.hps.util;

import java.util.Random;

public class RandomGaussian {

    private static Random randNumberGenerator;
        
    /**
     * Class shouldn't be instantiated by anyone 
     */
    private RandomGaussian(){
    }

    /**
     * Generates a Gaussian distributed number with given mean and standard deviation
     *
     * @param mean : Mean of the distribution
     * @param sigma : Standard deviation of the distribution
     * @return Gaussian distributed number
     */
    public static double getGaussian(double mean, double sigma){
            if(randNumberGenerator == null) randNumberGenerator = new Random();
            return mean + randNumberGenerator.nextGaussian()*sigma;
    }
}
