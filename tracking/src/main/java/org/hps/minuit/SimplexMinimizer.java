package org.hps.minuit;

/**
 *
 */
class SimplexMinimizer extends ModularFunctionMinimizer {

    public SimplexMinimizer() {
        theSeedGenerator = new SimplexSeedGenerator();
        theBuilder = new SimplexBuilder();
    }

    public MinimumSeedGenerator seedGenerator() {
        return theSeedGenerator;
    }

    public MinimumBuilder builder() {
        return theBuilder;
    }

    private SimplexSeedGenerator theSeedGenerator;
    private SimplexBuilder theBuilder;
}
