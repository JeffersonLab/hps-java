package org.hps.minuit;

class VariableMetricEDMEstimator {

    double estimate(FunctionGradient g, MinimumError e) {
        if (e.invHessian().size() == 1) {
            return 0.5 * g.grad().get(0) * g.grad().get(0) * e.invHessian().get(0, 0);
        }

        double rho = MnUtils.similarity(g.grad(), e.invHessian());
        return 0.5 * rho;
    }
}
