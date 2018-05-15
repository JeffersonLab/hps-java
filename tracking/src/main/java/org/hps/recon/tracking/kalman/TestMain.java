package org.hps.recon.tracking.kalman;

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
        HelixTest t1 = new HelixTest(path);
        //PatRecTest t1 = new PatRecTest(path);
    }

    public TestMain() {

    }

}
