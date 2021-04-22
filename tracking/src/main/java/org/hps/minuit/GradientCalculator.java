package org.hps.minuit;

/**
 *
 */
interface GradientCalculator {

    FunctionGradient gradient(MinimumParameters par);

    FunctionGradient gradient(MinimumParameters par, FunctionGradient grad);
}
