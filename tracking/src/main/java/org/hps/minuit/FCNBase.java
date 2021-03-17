package org.hps.minuit;

/**
 * User function base class, has to be implemented by the user.
 *
 */
public interface FCNBase {

    /**
     * Returns the value of the function with the given parameters.
     */
    double valueOf(double[] par);
}
