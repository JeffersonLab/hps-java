package org.hps.minuit;

/**
 *
 */
class VariableMetricMinimizer extends ModularFunctionMinimizer {

    public VariableMetricMinimizer() {
        theMinSeedGen = new MnSeedGenerator();
        theMinBuilder = new VariableMetricBuilder();
    }

    public MinimumSeedGenerator seedGenerator() {
        return theMinSeedGen;
    }

    public MinimumBuilder builder() {
        return theMinBuilder;
    }

    private MnSeedGenerator theMinSeedGen;
    private VariableMetricBuilder theMinBuilder;

}
