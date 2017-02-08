package org.hps.detector.ecal;

class StatFunUtils {

    /**
     * Function for double to round up the arg. value to 'places' digits after the semicolon 
     * @param value double number to round
     * @param places round up to places
     * @return rounded value 
     */
    public static double round(double value, int places) {
        if (places < 0 || places > 16) {
            throw new IllegalArgumentException();
        }
        long factor = (long) Math.pow(10, places);        
        value = value * factor;
        long tmp = Math.round(value); 
        return (double) tmp / factor;
    }

}
