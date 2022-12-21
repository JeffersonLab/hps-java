package org.hps.recon.tracking.kalman;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

/**
 * This is for driving stand-alone testing only and is not part of the Kalman fitting code
 */
public class TestMain {

    public static void main(String[] args) {

        String defaultPath = "C:\\Users\\rjohn\\Desktop\\Kalman\\";
        String path; // Path to where the output histograms should be written
        if (args.length == 0) {
            path = defaultPath;
        } else {
            path = args[0];
        }
        System.out.format("TestMain: standalone test of Kalman fitter code\n");
        
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.symmPosDef(3);
        
        double [][] M = new double[3][3];
        M[0][0] = 1.;
        M[1][1] = 2.;
        M[2][2] = 3.;
        M[0][1] = 0.1;
        M[0][2] = 0.2;
        M[1][2] = 0.4;
        M[1][0] = M[0][1];
        M[2][0] = M[0][2];
        M[2][1] = M[1][2];
        SquareMatrix S = new SquareMatrix(3,M);
        S.print("input matrix");
        DMatrixRMaj mat = new DMatrixRMaj(M);
        DMatrixRMaj matInv = new DMatrixRMaj(3,3);
        DMatrixRMaj res = new DMatrixRMaj(3,3);
        SquareMatrix T = new SquareMatrix(3);
        for (int i=0; i<3; ++i) {
            for (int j=0; j<3; ++j) {
                T.M[i][j] = res.unsafe_get(i, j);
            }
        }
        T.print("res matrix");
        DMatrixRMaj temp = new DMatrixRMaj(3);
        temp.set(mat);
        if (solver.setA(temp)) {
            solver.invert(matInv);
            CommonOps_DDRM.mult(mat,matInv,res);
            mat.print();
            matInv.print();
            res.print();
        }
        HelixTest3 t1 = new HelixTest3(path);
        //PatRecTest t1 = new PatRecTest(path);
    }

    public TestMain() {

    }

}
