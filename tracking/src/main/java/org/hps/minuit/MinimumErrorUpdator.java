package org.hps.minuit;

/**
 *
 */
interface MinimumErrorUpdator {

    MinimumError update(MinimumState state, MinimumParameters par, FunctionGradient grad);
}
