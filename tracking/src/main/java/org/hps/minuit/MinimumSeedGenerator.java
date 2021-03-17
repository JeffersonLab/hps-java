package org.hps.minuit;

/**
 * base class for seed generators (starting values); the seed generator prepares
 * initial starting values from the input (MnUserParameterState) for the
 * minimization;
 *
 */
interface MinimumSeedGenerator {

    MinimumSeed generate(MnFcn fcn, GradientCalculator calc, MnUserParameterState user, MnStrategy stra);
}
