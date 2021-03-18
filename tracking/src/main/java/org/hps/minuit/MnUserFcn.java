package org.hps.minuit;

/**
 *
 */
class MnUserFcn extends MnFcn {

    MnUserFcn(FCNBase fcn, double errDef, MnUserTransformation trafo) {
        super(fcn, errDef);
        theTransform = trafo;
    }

    double valueOf(MnAlgebraicVector v) {
        return super.valueOf(theTransform.transform(v));
    }
    private MnUserTransformation theTransform;
}
