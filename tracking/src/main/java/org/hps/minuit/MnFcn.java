package org.hps.minuit;

/**
 *
 */
class MnFcn {

    MnFcn(FCNBase fcn, double errorDef) {
        theFCN = fcn;
        theNumCall = 0;
        theErrorDef = errorDef;
    }

    double valueOf(MnAlgebraicVector v) {
        theNumCall++;
        return theFCN.valueOf(v.asArray());
    }

    int numOfCalls() {
        return theNumCall;
    }

    double errorDef() {
        return theErrorDef;
    }

    FCNBase fcn() {
        return theFCN;
    }

    private FCNBase theFCN;
    protected int theNumCall;
    private double theErrorDef;
}
