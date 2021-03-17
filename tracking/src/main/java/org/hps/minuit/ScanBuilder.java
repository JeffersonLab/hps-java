package org.hps.minuit;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a minimization using the simplex method of Nelder and Mead (ref.
 * Comp. J. 7, 308 (1965)).
 *
 */
class ScanBuilder implements MinimumBuilder {

    public FunctionMinimum minimum(MnFcn mfcn, GradientCalculator gc, MinimumSeed seed, MnStrategy stra, int maxfcn, double toler) {
        MnAlgebraicVector x = seed.parameters().vec().clone();
        MnUserParameterState upst = new MnUserParameterState(seed.state(), mfcn.errorDef(), seed.trafo());
        MnParameterScan scan = new MnParameterScan(mfcn.fcn(), upst.parameters(), seed.fval());
        double amin = scan.fval();
        int n = seed.trafo().variableParameters();
        MnAlgebraicVector dirin = new MnAlgebraicVector(n);
        for (int i = 0; i < n; i++) {
            int ext = seed.trafo().extOfInt(i);
            scan.scan(ext);
            if (scan.fval() < amin) {
                amin = scan.fval();
                x.set(i, seed.trafo().ext2int(ext, scan.parameters().value(ext)));
            }
            dirin.set(i, Math.sqrt(2. * mfcn.errorDef() * seed.error().invHessian().get(i, i)));
        }

        MinimumParameters mp = new MinimumParameters(x, dirin, amin);
        MinimumState st = new MinimumState(mp, 0., mfcn.numOfCalls());

        List<MinimumState> states = new ArrayList<MinimumState>(1);
        states.add(st);
        return new FunctionMinimum(seed, states, mfcn.errorDef());
    }

}
