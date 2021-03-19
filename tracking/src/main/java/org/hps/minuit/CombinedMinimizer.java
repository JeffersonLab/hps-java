package org.hps.minuit;

/**
 *
 */
class CombinedMinimizer extends ModularFunctionMinimizer {

    MinimumSeedGenerator seedGenerator() {
        return theMinSeedGen;
    }

    MinimumBuilder builder() {
        return theMinBuilder;
    }

    private MnSeedGenerator theMinSeedGen = new MnSeedGenerator();
    private CombinedMinimumBuilder theMinBuilder = new CombinedMinimumBuilder();
}
