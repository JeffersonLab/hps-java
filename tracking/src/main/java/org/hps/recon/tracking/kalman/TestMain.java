package org.hps.recon.tracking.kalman;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

//This is for testing only and is not part of the Kalman fitting code
public class TestMain {

    public static void main(String[] args) {

        String defaultPath = "C:\\Users\\Robert\\Desktop\\Kalman\\";
        String path; // Path to where the output histograms should be written
        if (args.length == 0) {
            path = defaultPath;
        } else {
            path = args[0];
        }
        System.out.format("TestMain: standalone test of Kalman fitter code\n");

        //HelixTest3 t1 = new HelixTest3(path);
        PatRecTest t1 = new PatRecTest(path);
    }

    public TestMain() {

    }

}
