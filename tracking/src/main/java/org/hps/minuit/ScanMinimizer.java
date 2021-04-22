package org.hps.minuit;

/**
 *
 */
class ScanMinimizer extends ModularFunctionMinimizer {

    ScanMinimizer() {
        theSeedGenerator = new SimplexSeedGenerator();
        theBuilder = new ScanBuilder();
    }

    MinimumSeedGenerator seedGenerator() {
        return theSeedGenerator;
    }

    MinimumBuilder builder() {
        return theBuilder;
    }

    private SimplexSeedGenerator theSeedGenerator;
    private ScanBuilder theBuilder;
}
