package org.hps.minuit;

/**
 *
 */
class MinimumSeed {

    MinimumSeed(MinimumState state, MnUserTransformation trafo) {
        theState = state;
        theTrafo = trafo;
        theValid = true;
    }

    MinimumState state() {
        return theState;
    }

    MinimumParameters parameters() {
        return state().parameters();
    }

    MinimumError error() {
        return state().error();
    }

    FunctionGradient gradient() {
        return state().gradient();
    }

    MnUserTransformation trafo() {
        return theTrafo;
    }

    MnMachinePrecision precision() {
        return theTrafo.precision();
    }

    double fval() {
        return state().fval();
    }

    double edm() {
        return state().edm();
    }

    int nfcn() {
        return state().nfcn();
    }

    boolean isValid() {
        return theValid;
    }

    private MinimumState theState;
    private MnUserTransformation theTrafo;
    private boolean theValid;
}
