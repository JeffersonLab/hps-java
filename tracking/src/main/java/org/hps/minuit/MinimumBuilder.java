package org.hps.minuit;

/**
 *
 */
interface MinimumBuilder {

    FunctionMinimum minimum(MnFcn fcn, GradientCalculator gc, MinimumSeed seed, MnStrategy strategy, int maxfcn, double toler);
}
