package org.hps.recon.tracking.gbl;

import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.Vector;

import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;

public class FrameToFrameDers {

    private Matrix RotDa;
    private Matrix RotDb;
    private Matrix RotDc;
    private boolean debug;

    public FrameToFrameDers() {

        RotDa = new Matrix(3, 3);
        RotDb = new Matrix(3, 3);
        RotDc = new Matrix(3, 3);

        RotDa.set(1, 2, 1);
        RotDa.set(2, 1, -1);

        RotDb.set(0, 2, -1);
        RotDb.set(2, 0, 1);

        RotDc.set(0, 1, 1);
        RotDc.set(1, 0, -1);

        debug = false;
    }

    // Method to convert Hep3Matrices in Jama Matrices
    public Matrix getDerivative(Hep3Matrix objectRot, Hep3Matrix composeRot, Hep3Vector objectPos, Hep3Vector composePos) {

        Vector oPos = new Vector(3);
        Vector cPos = new Vector(3);

        for (int i = 0; i < 3; i++) {
            oPos.set(i, (objectPos.v())[i]);
            cPos.set(i, (composePos.v())[i]);
        }

        // The matrices returned by the getLocalToGlobal are transposed. First fill them, then transpose them.
        Matrix oRot = new Matrix(3, 3);
        Matrix cRot = new Matrix(3, 3);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {

                oRot.set(i, j, objectRot.e(i, j));
                cRot.set(i, j, composeRot.e(i, j));
            }// j
        }// i

        oRot.transposeInPlace();
        cRot.transposeInPlace();

        if (debug) {
            System.out.println("PF::FrameToFrame Check Derivatives!");
            System.out.println("oPos");
            oPos.print(3, 6);
            System.out.println("cPos");
            cPos.print(3, 6);
            System.out.println("oRot");
            oRot.print(3, 6);
            System.out.println("cRot");
            cRot.print(3, 6);
        }

        return getDerivative(oRot, cRot, oPos, cPos);
    }

    public Matrix getDerivative(Matrix objectRot, Matrix composeRot, Vector objectPos, Vector composePos) {
        return getDerivative(objectRot, composeRot, composePos.minus(objectPos));
    }

    private Matrix getDerivative(Matrix objectRot, Matrix composeRot, Vector posVec) {

        Matrix derivative = new Matrix(6, 6);

        Matrix derivAA = new Matrix(3, 3);
        Matrix derivAB = new Matrix(3, 3);
        Matrix derivBB = new Matrix(3, 3);

        derivAA = derivativePosPos(objectRot, composeRot);
        derivAB = derivativePosRot(objectRot, composeRot, posVec);
        derivBB = derivativeRotRot(objectRot, composeRot);

        derivative.set(0, 0, derivAA.get(0, 0));
        derivative.set(0, 1, derivAA.get(0, 1));
        derivative.set(0, 2, derivAA.get(0, 2));
        derivative.set(0, 3, derivAB.get(0, 0));
        derivative.set(0, 4, derivAB.get(0, 1));
        derivative.set(0, 5, derivAB.get(0, 2));
        derivative.set(1, 0, derivAA.get(1, 0));
        derivative.set(1, 1, derivAA.get(1, 1));
        derivative.set(1, 2, derivAA.get(1, 2));
        derivative.set(1, 3, derivAB.get(1, 0));
        derivative.set(1, 4, derivAB.get(1, 1));
        derivative.set(1, 5, derivAB.get(1, 2));
        derivative.set(2, 0, derivAA.get(2, 0));
        derivative.set(2, 1, derivAA.get(2, 1));
        derivative.set(2, 2, derivAA.get(2, 2));
        derivative.set(2, 3, derivAB.get(2, 0));
        derivative.set(2, 4, derivAB.get(2, 1));
        derivative.set(2, 5, derivAB.get(2, 2));
        derivative.set(3, 0, 0);
        derivative.set(3, 1, 0);
        derivative.set(3, 2, 0);
        derivative.set(3, 3, derivBB.get(0, 0));
        derivative.set(3, 4, derivBB.get(0, 1));
        derivative.set(3, 5, derivBB.get(0, 2));
        derivative.set(4, 0, 0);
        derivative.set(4, 1, 0);
        derivative.set(4, 2, 0);
        derivative.set(4, 3, derivBB.get(1, 0));
        derivative.set(4, 4, derivBB.get(1, 1));
        derivative.set(4, 5, derivBB.get(1, 2));
        derivative.set(5, 0, 0);
        derivative.set(5, 1, 0);
        derivative.set(5, 2, 0);
        derivative.set(5, 3, derivBB.get(2, 0));
        derivative.set(5, 4, derivBB.get(2, 1));
        derivative.set(5, 5, derivBB.get(2, 2));

        if (debug) {
            System.out.println("PF::C Matrix");
            derivative.print(6, 6);
        }

        return derivative;
    }

    // Gets linear approximated euler Angles
    private Vector linearEulerAngles(Matrix rotDelta) {

        Matrix eulerAB = new Matrix(3, 3);
        Vector aB = new Vector(3);

        eulerAB.set(0, 1, 1);
        eulerAB.set(1, 0, -1);
        aB.set(2, 1);

        Matrix eulerC = new Matrix(3, 3);
        Vector C = new Vector(3);
        eulerC.set(2, 0, 1);
        C.set(1, 1);

        Vector eulerAngles = new Vector(3);
        eulerAngles = ((eulerAB.times(rotDelta)).times(aB)).plus((eulerC.times(rotDelta)).times(C));
        return eulerAngles;
    }

    // Calculates the derivative DPos/DPos
    private Matrix derivativePosPos(Matrix RotDet, Matrix RotRot) {
        return RotDet.times((RotRot.transpose()));
    }

    // Calculate the derivative DPos/DRot
    private Matrix derivativePosRot(Matrix RotDet, Matrix RotRot, Vector S) {

        Vector dEulerA = new Vector(3);
        Vector dEulerB = new Vector(3);
        Vector dEulerC = new Vector(3);

        Vector tmp = new Vector(3);

        // dEulerA = RotDet * (RotRot.T() * RotDa * RotRot * S);

        tmp = (RotRot.transpose()).times(RotDa.times(RotRot.times(S)));
        dEulerA = RotDet.times(tmp);

        tmp = (RotRot.transpose()).times(RotDb.times(RotRot.times(S)));
        dEulerB = RotDet.times(tmp);

        tmp = (RotRot.transpose()).times(RotDc.times(RotRot.times(S)));
        dEulerC = RotDet.times(tmp);

        Matrix eulerDeriv = new Matrix(3, 3);

        eulerDeriv.set(0, 0, dEulerA.get(0));
        eulerDeriv.set(1, 0, dEulerA.get(1));
        eulerDeriv.set(2, 0, dEulerA.get(2));
        eulerDeriv.set(0, 1, dEulerB.get(0));
        eulerDeriv.set(1, 1, dEulerB.get(1));
        eulerDeriv.set(2, 1, dEulerB.get(2));
        eulerDeriv.set(0, 2, dEulerC.get(0));
        eulerDeriv.set(1, 2, dEulerC.get(1));
        eulerDeriv.set(2, 2, dEulerC.get(2));

        return eulerDeriv;

    }

    // Calculates the derivative DRot/DRot (where I failed (PF) )
    private Matrix derivativeRotRot(Matrix RotDet, Matrix RotRot) {

        Vector dEulerA = new Vector(3);
        Vector dEulerB = new Vector(3);
        Vector dEulerC = new Vector(3);

        Matrix RotDet_T = RotDet.transpose();
        Matrix RotRot_T = RotRot.transpose();

        dEulerA = linearEulerAngles(RotDet.times(RotRot_T.times(RotDa.times(RotRot.times(RotDet_T)))));
        dEulerB = linearEulerAngles(RotDet.times(RotRot_T.times(RotDb.times(RotRot.times(RotDet_T)))));
        dEulerC = linearEulerAngles(RotDet.times(RotRot_T.times(RotDc.times(RotRot.times(RotDet_T)))));

        Matrix eulerDeriv = new Matrix(3, 3);

        eulerDeriv.set(0, 0, dEulerA.get(0));
        eulerDeriv.set(1, 0, dEulerA.get(1));
        eulerDeriv.set(2, 0, dEulerA.get(2));
        eulerDeriv.set(0, 1, dEulerB.get(0));
        eulerDeriv.set(1, 1, dEulerB.get(1));
        eulerDeriv.set(2, 1, dEulerB.get(2));
        eulerDeriv.set(0, 2, dEulerC.get(0));
        eulerDeriv.set(1, 2, dEulerC.get(1));
        eulerDeriv.set(2, 2, dEulerC.get(2));

        return eulerDeriv;
    }
}
