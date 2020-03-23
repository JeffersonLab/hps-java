package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Random;

//This is for testing only and is not part of the Kalman fitting code
class HelixTest3 { // Program for testing the Kalman fitting code

    // Coordinate system:
    // z is the B field direction, downward in lab coordinates
    // y is the beam direction
    // x is y cross z

    Random rnd;

    HelixTest3(String path) {

        // Control parameters
        // Units are Tesla, GeV, mm

        int nTrials = 2000; // The number of test events to generate for fitting
        int startLayer = 10; // Where to start the Kalman filtering
        int nIteration = 2; // Number of filter iterations
        int nAxial = 3; // Number of axial layers needed by the linear fit
        int nStereo = 4; // Number of stereo layers needed by the linear fit
        boolean cheat = false; // true to use the true helix parameters (smeared) for the starting guess
        boolean perfect = false;

        boolean rungeKutta = true; // Set true to generate the helix by Runge Kutta integration instead of a
                                   // piecewise helix
        boolean verbose = nTrials < 2;

        // Seed the random number generator
        long rndSeed = -3263009337738135404L;
        rnd = new Random();
        rnd.setSeed(rndSeed);

        Histogram hGaus = new Histogram(100, -4., 0.08, "Normal Distribution 1", "x", "y");
        for (int i = 0; i < 1000000; i++) { hGaus.entry(rnd.nextGaussian()); }

        // Read in the magnetic field map
        String mapType = "binary";
        String mapFile = "C:\\Users\\Robert\\Documents\\GitHub\\hps-java\\fieldmap\\125acm2_3kg_corrected_unfolded_scaled_0.7992_v3.bin";
        FieldMap fM = null;
        FieldMap fMg = null;
        try {
            fM = new FieldMap(mapFile, mapType, true, 21.17, 0., 457.2);
            fMg = new FieldMap(mapFile, mapType, false, 21.17, 0., 457.2);     // for generating tracks
        } catch (IOException e) {
            System.out.format("Could not open or read the field map %s\n", mapFile);
            return;
        }
        if (mapType != "binary") {
            fM.writeBinaryFile("C:\\Users\\Robert\\Documents\\GitHub\\hps-java\\fieldmap\\125acm2_3kg_corrected_unfolded_scaled_0.7992_v2.bin");
        }

        // Tracking instrument description

        double thickness = 0.3; // Silicon thickness in mm
        if (perfect) { thickness = 0.0000000000001; }
        ArrayList<SiModule> SiModules = new ArrayList<SiModule>();
        Plane plnInt;
        SiModule newModule;

        double yStart = 103.69;
        plnInt = new Plane(new Vec(3.4814, yStart, 20.781), new Vec(-0.030928, -0.99952, 0.00056169), -0.100076);
        newModule = new SiModule(1, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(3.7752, 111.75, 20.770), new Vec(0.029092, 0.99957, 0.0031495), 0.000303);
        newModule = new SiModule(2, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(6.6595, 203.81, 22.296), new Vec(-0.029875, -0.99954, 0.0053661), -0.099851);
        newModule = new SiModule(3, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(6.7661, 211.87, 22.281), new Vec(0.028940, 0.99958, 0.0028008), 0.000145);
        newModule = new SiModule(4, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(9.4835, 303.76, 23.796), new Vec(-0.029471, -0.99955, 0.0048642), -0.100012);
        newModule = new SiModule(5, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(9.7121, 311.63, 23.777), new Vec(0.027875, 0.99961, -0.0027053), 0.000106);
        newModule = new SiModule(6, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-35.087, 505.57, 29.328), new Vec(-0.029044, -0.99958, 0.0022785), -0.049060);
        newModule = new SiModule(7, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(65.791, 502.52, 24.294), new Vec(-0.030402, -0.99954, 0.0012687), -0.050671);
        newModule = new SiModule(7, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-34.848, 513.08, 26.824), new Vec(0.030086, 0.99954, -0.0021664), 0.000199);
        newModule = new SiModule(8, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(65.958, 510.03, 26.821), new Vec(0.030452, 0.99954, -0.00060382), 0.000194);
        newModule = new SiModule(8, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-29.010, 705.47, 32.358), new Vec(-0.030508, -0.99953, -0.00048837), -0.050035);
        newModule = new SiModule(9, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(71.778, 702.43, 27.322), new Vec(-0.029627, -0.99956, -0.0015542), -0.050102);
        newModule = new SiModule(9, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-28.846, 713.07, 29.845), new Vec(0.029810, 0.99956, -0.00084633), 0.000172);
        newModule = new SiModule(10, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(72.034, 710.03, 29.845), new Vec(0.030891, 0.99952, 0.00016092), 0.000205);
        newModule = new SiModule(10, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        // Add some dummy in-between planes in this region where the field is changing rapidly
        /*
        plnInt = new Plane(new Vec(0., 730.0, 30.0), new Vec(0., 1.0, 0.));
        newModule = new SiModule(-1, plnInt, false, 0., 900., 900., 0., fM, 0);
        SiModules.add(newModule);       
        plnInt = new Plane(new Vec(0., 770.0, 30.0), new Vec(0., 1.0, 0.));
        newModule = new SiModule(-2, plnInt, false, 0., 900., 900., 0., fM, 0);
        SiModules.add(newModule);
        plnInt = new Plane(new Vec(0., 800.0, 30.0), new Vec(0., 1.0, 0.));
        newModule = new SiModule(-3, plnInt, false, 0., 900., 900., 0., fM, 0);
        SiModules.add(newModule);       
        plnInt = new Plane(new Vec(0., 835.0, 30.0), new Vec(0., 1.0, 0.));
        newModule = new SiModule(-4, plnInt, false, 0., 900., 900., 0., fM, 0);
        SiModules.add(newModule);
        plnInt = new Plane(new Vec(0., 870.0, 30.0), new Vec(0., 1.0, 0.));
        newModule = new SiModule(-5, plnInt, false, 0., 900., 900., 0., fM, 0);
        SiModules.add(newModule);        
        */
        plnInt = new Plane(new Vec(-22.879, 905.35, 35.309), new Vec(-0.029214, -0.99957, 0.0019280), -0.049801);
        newModule = new SiModule(11, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(77.869, 902.35, 30.284), new Vec(-0.029989, -0.99955, -0.00062471), -0.049863);
        newModule = new SiModule(11, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-22.795, 912.89, 32.839), new Vec(0.028266, 0.99960, -0.0014105), 0.000107);
        newModule = new SiModule(12, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(78.097, 909.99, 32.835), new Vec(0.030889, 0.99952, -0.00029751), 0.000071);
        newModule = new SiModule(12, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        int nLayers = 13; // Layer 0 not yet implemented here

        double[] location = new double[nLayers];
        double[] xdet = new double[SiModules.size()];
        double[] ydet = new double[SiModules.size()];
        double[] zdet = new double[SiModules.size()];
        int[] lyr = new int[SiModules.size()];
        for (int i = 0; i < SiModules.size(); i++) {
            SiModule si = SiModules.get(i);
            if (si.Layer >= 0) { location[si.Layer] = si.p.X().v[1]; }
            lyr[i] = si.Layer;
            xdet[i] = si.p.X().v[0];
            ydet[i] = si.p.X().v[1];
            zdet[i] = si.p.X().v[2];
            System.out.format("Si Module %d, Layer=%d, Detector=%d, location=%8.2f, x=%8.2f, y=%8.2f, z=%8.2f\n", i, si.Layer, si.detector,
                    si.p.X().v[1], xdet[i], ydet[i], zdet[i]);
        }
        double resolution = 0.006; // SSD point resolution, in mm

        double Q = -1.0;
        double p = 1.0;

        Vec helixOrigin = new Vec(0., 0., 0.); // Pivot point of initial helix
        Vec Bpivot = fM.getField(helixOrigin);
        Bpivot.print("magnetic field at the initial origin");
        for (int pln = 0; pln < SiModules.size(); pln++) {
            Vec bf = fM.getField(new Vec(xdet[pln], ydet[pln], zdet[pln]));
            System.out.format("Kalman fitting B field at module %d = %10.7f, %10.7f, %10.7f\n", pln, bf.v[0], bf.v[1], bf.v[2]);
            bf = fMg.getField(new Vec(xdet[pln], ydet[pln], zdet[pln]));
            System.out.format("MC generator B field at module %d = %10.7f, %10.7f, %10.7f\n", pln, bf.v[0], bf.v[1], bf.v[2]);
        }
        double Phi = 91. * Math.PI / 180.;
        double Theta = 88. * Math.PI / 180.;
        Vec initialDirection = new Vec(Math.cos(Phi) * Math.sin(Theta), Math.sin(Phi) * Math.sin(Theta), Math.cos(Theta));
        initialDirection.print("initial particle direction");
        double drho;
        double drhoSigma = 0.3; // 0.2 0.3
        double dz;
        double dzSigma = 0.02; // 0.2 0.02
        double phi0;
        double phi0Sigma = 0.01; // 0.0002 0.01
        double tanl;
        double tanlSigma = 0.001; // 0.0002, 0.001
        double K;
        double kError = 1.2; // 0.02 1.2

        Vec helixMCtrue = null;
        Vec momentum = new Vec(p * initialDirection.v[0], p * initialDirection.v[1], p * initialDirection.v[2]);
        momentum.print("initial helix momentum");
        Helix TkInitial = new Helix(Q, helixOrigin, momentum, helixOrigin, fMg, rnd);
        drho = TkInitial.p.v[0];
        phi0 = TkInitial.p.v[1];
        K = TkInitial.p.v[2];
        dz = TkInitial.p.v[3];
        tanl = TkInitial.p.v[4];
        TkInitial.print(String.format("Initial helix"));
        double pt = p / Math.sqrt(1.0 + tanl * tanl);
        System.out.format("Momentum p=%10.4f GeV, pt=%10.4f GeV\n", p, pt);
        System.out.format("True starting helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
        double kSigma = K * kError;

        // Print out a plot of just a simple helix
        File file = new File(path + "helix1.gp");
        file.getParentFile().mkdirs();
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(file);
        } catch (FileNotFoundException e1) {
            System.out.println("Could not create the gnuplot output file.");
            e1.printStackTrace();
            return;
        }
        // printWriter.format("set xrange [-1900.:100.]\n");
        // printWriter.format("set yrange [-1000.:1000.]\n");
        printWriter.format("set xlabel 'X'\n");
        printWriter.format("set ylabel 'Y'\n");
        printWriter.format("$runga << EOD\n");
        RungeKutta4 r4 = new RungeKutta4(Q, 1., fMg);
        Vec r0 = TkInitial.atPhiGlobal(0.);
        Vec p0 = TkInitial.getMomGlobal(0.);
        for (int step = 0; step < 50; step++) {
            double[] res = r4.integrate(r0, p0, 20.);
            r0.v[0] = res[0];
            r0.v[1] = res[1];
            r0.v[2] = res[2];
            p0.v[0] = res[3];
            p0.v[1] = res[4];
            p0.v[2] = res[5];
            printWriter.format("%10.6f %10.6f %10.6f\n", r0.v[0], r0.v[1], r0.v[2]);
        }
        printWriter.format("EOD\n");
        printWriter.format("$helix << EOD\n");

        if (Q < 0.) {
            for (double phi = 0.; phi < 0.02 * Math.PI; phi = phi + 0.001) {
                Vec r = TkInitial.atPhiGlobal(phi);
                printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
            }
        } else {
            for (double phi = 0.; phi > -0.02 * Math.PI; phi = phi - 0.001) {
                Vec r = TkInitial.atPhiGlobal(phi);
                printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
            }
        }

        printWriter.format("EOD\n");
        printWriter.format("splot ");
        printWriter.format("$runga u 1:2:3 with lines lw 3, $helix u 1:2:3 with lines lw 3\n");
        printWriter.close();

        Vec zhat = null;
        Vec uhat = null;
        Vec vhat = null;

        PrintWriter printWriter2 = null; // Prepare for plot of generated helix with hits
        if (nTrials == 1) {
            File file2 = new File(path + "helix2.gp");
            file2.getParentFile().mkdirs();
            try {
                printWriter2 = new PrintWriter(file2);
            } catch (FileNotFoundException e1) {
                System.out.println("Could not create the gnuplot output file.");
                e1.printStackTrace();
                return;
            }
            // printWriter2.format("set xrange [-500.:1500]\n");
            // printWriter2.format("set yrange [-1000.:1000.]\n");
            printWriter2.format("set xlabel 'X'\n");
            printWriter2.format("set ylabel 'Y'\n");
        }
        Histogram hScat = new Histogram(100, 0., 0.0001, "Scattering Angle", "radians", "events");
        Histogram hScatProj = new Histogram(100, -0.5, 0.01, "Projected Scattering Angle", "degrees", "Si planes");
        Histogram hnHit = new Histogram(15, 0., 1., "Number of hits on helix", "hits", "events");
        Histogram hps1 = new Histogram(100, -1., 0.02, "phi of scatter, non-stereo layer", "radians", "track");
        Histogram hsp1theta = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle, non-stereo layer", "pi radians", "track");
        Histogram hps2 = new Histogram(100, -1., 0.02, "phi of scatter, stereo layer", "radians", "track");
        Histogram hsp2theta = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle, stereo layer", "pi radians", "track");
        Histogram hchi2G = new Histogram(80, 0., 2.0, "Helix guess chi^2", "chi^2", "tracks");
        Histogram hChi2 = new Histogram(80, 0., 1.0, "Helix fit chi^2 after smoothing", "chi^2", "tracks");
        Histogram hChi2Alt = new Histogram(80, 0., 3.0, "Fit chi^2 after smoothing from MC true locations", "chi^2", "tracks");
        Histogram hChi2p = new Histogram(80, 0., 1.0, "Fit chi^2 using only hit errors", "chi^2", "tracks");
        Histogram hReducedErr = new Histogram(100, 0., .0002, "Reduced error in Layer 4", "mm", "tracks");
        Histogram hChi2f = new Histogram(80, 0., 1.0, "Helix fit chi^2 after filtering", "chi^2", "tracks");
        Histogram hXscat = new Histogram(100, -1., 0.02, "X scattering angles", "degrees", "Si planes");
        Histogram hZscat = new Histogram(100, -0.5, 0.01, "Z scattering angles", "degrees", "Si planes");
        Histogram hChi2HelixS = new Histogram(80, 0., 0.4, "smoothed chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hChi2Helix = new Histogram(80, 0., 0.4, "filtered chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hChi2Guess = new Histogram(80, 0., 2.0, "chi^2 of guess helix parameters", "chi^2", "tracks");
        Histogram hChi2Origin = new Histogram(80, 0., .5, "Helix fit chi^2 at the origin", "chi^2", "tracks");
        Histogram hRes = new Histogram(100, -.25, 0.005, "detector resolution", "mm", "hits");
        Histogram hEdrho = new Histogram(100, -10., 0.2, "Filtered helix parameter drho error", "sigmas", "track");
        Histogram hEphi0 = new Histogram(100, -10., 0.2, "Filtered helix parameter phi0 error", "sigmas", "track");
        Histogram hEk = new Histogram(100, -10., 0.2, "Filtered helix parameter K error", "sigmas", "track");
        Histogram hEdz = new Histogram(100, -10., 0.2, "Filtered helix parameter dz error", "sigmas", "track");
        Histogram hEtanl = new Histogram(100, -10., 0.2, "Filtered helix parameter tanl error", "sigmas", "track");
        Histogram hEdrhoS = new Histogram(100, -10., 0.2, "Smoothed helix parameter drho error", "sigmas", "track");
        Histogram hEphi0S = new Histogram(100, -10., 0.2, "Smoothed helix parameter phi0 error", "sigmas", "track");
        Histogram hEkS = new Histogram(100, -10., 0.2, "Smoothed helix parameter K error", "sigmas", "track");
        Histogram hEdzS = new Histogram(100, -10., 0.2, "Smoothed helix parameter dz error", "sigmas", "track");
        Histogram hEtanlS = new Histogram(100, -10., 0.2, "Smoothed helix parameter tanl error", "sigmas", "track");
        Histogram hEdrhoO = new Histogram(100, -10., 0.2, "Origin helix parameter drho error", "sigmas", "track");
        Histogram hEphi0O = new Histogram(100, -10., 0.2, "Origin helix parameter phi0 error", "sigmas", "track");
        Histogram hEkO = new Histogram(100, -10., 0.2, "Origin helix parameter K error", "sigmas", "track");
        Histogram hEdzO = new Histogram(100, -10., 0.2, "Origin helix parameter dz error", "sigmas", "track");
        Histogram hEtanlO = new Histogram(100, -10., 0.2, "Origin helix parameter tanl error", "sigmas", "track");
        Histogram hResid0 = new Histogram(100, -10., 0.2, "Filtered residual for axial planes", "sigmas", "hits");
        Histogram hResid1 = new Histogram(100, -10., 0.2, "Filtered residual for stereo planes", "sigmas", "hits");
        Histogram hEdrhoG = new Histogram(100, -40., 0.8, "Helix guess drho error", "sigmas", "track");
        Histogram hEphi0G = new Histogram(100, -40., 0.8, "Helix guess phi0 error", "sigmas", "track");
        Histogram hEkG = new Histogram(100, -40., 0.8, "Helix guess K error", "sigmas", "track");
        Histogram hEdzG = new Histogram(100, -40., 0.8, "Helix guess dz error", "sigmas", "track");
        Histogram hEtanlG = new Histogram(100, -80., 1.6, "Helix guess tanl error", "sigmas", "track");
        Histogram hEdrhoG1 = new Histogram(100, -4., 0.08, "Helix guess drho error", "mm", "track");
        Histogram hEphi0G1 = new Histogram(100, -0.05, 0.001, "Helix guess phi0 error", "radians", "track");
        Histogram hEkG1 = new Histogram(100, -10., 0.2, "Helix guess K error", "1/GeV", "track");
        Histogram hEdzG1 = new Histogram(100, -0.50, 0.01, "Helix guess dz error", "mm", "track");
        Histogram hEtanlG1 = new Histogram(100, -0.005, 0.0001, "Helix guess tanl error", "tan(lambda) error", "track");
        Histogram[] hResidS0 = new Histogram[nLayers];
        Histogram[] hResidS2 = new Histogram[nLayers];
        Histogram[] hResidS4 = new Histogram[nLayers];
        Histogram[] hResidX = new Histogram[nLayers];
        Histogram[] hResidZ = new Histogram[nLayers];
        for (int i = 0; i < nLayers; i++) {
            hResidS0[i] = new Histogram(100, -10., 0.2, String.format("Smoothed fit residual for plane %d", i), "sigmas", "hits");
            hResidS2[i] = new Histogram(100, -0.02, 0.0004, String.format("Smoothed fit residual for plane %d", i), "mm", "hits");
            hResidS4[i] = new Histogram(100, -0.1, 0.002, String.format("Smoothed true residual for plane %d", i), "mm", "hits");
            hResidX[i] = new Histogram(100, -0.8, 0.016, String.format("True residual in global X for plane %d", i), "mm", "hits");
            hResidZ[i] = new Histogram(100, -0.1, 0.002, String.format("True residual in global Z for plane %d", i), "mm", "hits");
        }

        Instant timestamp = Instant.now();
        System.out.format("Beginning time = %s\n", timestamp.toString());
        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        // Extrapolate the helix from the origin to the first detector layer
        SiModule si1 = SiModules.get(0);
        double phi1 = TkInitial.planeIntersect(si1.p);
        if (Double.isNaN(phi1)) {
            if (verbose) System.out.format("Oops! No intersection found with initial plane");
            return;
        }
        Vec p1 = new Vec(3);
        HelixPlaneIntersect hpi1 = new HelixPlaneIntersect();
        Vec pivotBegin = hpi1.rkIntersect(si1.p, TkInitial.atPhiGlobal(0.), TkInitial.getMomGlobal(0.), Q, fMg, p1);
        Helix helixBegin = new Helix(Q, pivotBegin, p1, pivotBegin, fMg, rnd);

        Vec[] helixSaved = new Vec[SiModules.size()];
        Vec[] pivotSaved = new Vec[SiModules.size()];
        TkInitial.print("TkInitial: initial helix at the origin");
        helixBegin.print("helixBegin: starting helix at layer 1");
        Helix TkEnd = helixBegin;
        for (int iTrial = 0; iTrial < nTrials; iTrial++) {
            Helix Tk = helixBegin.copy();
            if (verbose) { Tk.print("copied initial helix"); }

            // Populate the Si detector planes with hits from the helix scattered at each
            // plane
            HelixPlaneIntersect hpi = new HelixPlaneIntersect();
            if (verbose) { printWriter2.format("$helix << EOD\n"); }
            for (int icm = 0; icm < SiModules.size(); icm++) {
                SiModule thisSi = SiModules.get(icm);
                if (thisSi.Layer < 0) { continue; }
                thisSi.reset();
                int pln = thisSi.Layer;
                int det = thisSi.detector;
                if (verbose) {
                    System.out.format("Extrapolating to plane #%d, detector %d\n", pln, det);
                    Tk.print("this plane");
                }
                double phiInt = Tk.planeIntersect(thisSi.p);
                if (Double.isNaN(phiInt)) {
                    if (verbose) System.out.format("Plane %d, no intersection found", pln);
                    break;
                }
                if (verbose) { System.out.format("Plane %d, phiInt1= %12.10f\n", pln, phiInt); }
                Vec rscat = new Vec(3);
                Vec pInt = new Vec(3);
                if (rungeKutta) {
                    rscat = hpi.rkIntersect(thisSi.p, Tk.atPhiGlobal(0.), Tk.getMomGlobal(0.), Q, fMg, pInt);
                } else {
                    rscat = Tk.atPhiGlobal(phiInt);
                    pInt = Tk.getMomGlobal(phiInt);
                }
                if (verbose) {
                    double check = (rscat.dif(thisSi.p.X()).dot(thisSi.p.T()));
                    System.out.format("Dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
                    Tk.atPhi(phiInt).print("local intersection point of helix");
                    Vec xIntGlob = Tk.atPhiGlobal(phiInt);
                    xIntGlob.print("global intersection point of helix");
                    double dPhi = Math.abs(phiInt) / 20.0;
                    for (double phi = 0.; phi < Math.abs(phiInt); phi = phi + dPhi) {
                        Vec r = Tk.atPhiGlobal(-Q * phi);
                        printWriter2.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                    }
                    // printWriter2.format("%10.6f %10.6f %10.6f\n", rscat.v[0], rscat.v[1],
                    // rscat.v[2]);
                    if (rungeKutta) {
                        double errX = rscat.dif(xIntGlob).mag();
                        System.out.format("Runge-Kutta difference from Helix extrapolation is %12.5e mm for plane %d\n", errX, pln);
                        rscat.print("Runge-Kutta intersection point");
                        pInt.print("Runge-Kutta momentum at intersection point");
                    }
                }
                Vec rDet = thisSi.toLocal(rscat);
                if (verbose) {
                    thisSi.p.print(String.format("layer %d, detector %d", pln, det));
                    // thisSi.Rinv.print("SiModule Rinv");
                    rscat.print("       Gobal intersection point 1");
                    rDet.print("       helix intersection in detector frame");
                }
                // Check whether the intersection is within the bounds of the detector
                // if (verbose) {
                // System.out.format("Check boundardies of detector %d in layer %d\n",det,pln);
                // System.out.format(" X: %10.6f is within %8.4f to %8.4f?\n",
                // rDet.v[0],thisSi.xExtent[0],thisSi.xExtent[1]);
                // System.out.format(" Y: %10.6f is within %8.4f to %8.4f?\n",
                // rDet.v[1],thisSi.yExtent[0],thisSi.yExtent[1]);
                // }
                if (rDet.v[0] > thisSi.xExtent[1] || rDet.v[0] < thisSi.xExtent[0] || rDet.v[1] > thisSi.yExtent[1]
                        || rDet.v[1] < thisSi.yExtent[0]) {
                    if (verbose) { System.out.format("     Intersection point is outside of the detector %d in layer %d\n", det, pln); }
                    continue;
                }
                // if (thisSi.Layer == 7 || thisSi.Layer == 8) continue; // !!!!!!!!!!!!

                double[] gran = new double[2];
                if (perfect) {
                    gran[0] = 0.;
                    gran[1] = 0.;
                } else {
                    //gran = gausRan();
                    gran[0] = rnd.nextGaussian();
                    gran[1] = rnd.nextGaussian();
                }
                double smear = resolution * gran[0];
                double m1 = rDet.v[1] + smear;
                hRes.entry(smear);
                if (verbose) { System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1, rDet.v[1]); }
                Measurement thisM1 = new Measurement(m1, resolution, 0., rscat, rDet.v[1]);
                thisSi.addMeasurement(thisM1);

                if (icm + 1 < SiModules.size()) {
                    Vec t1 = Tk.getMomGlobal(phiInt).unitVec();
                    if (verbose) {
                        Tk.getMom(phiInt).print("helix local momentum before scatter");
                        Tk.getMomGlobal(phiInt).print("helix global momentum before scatter");
                    }
                    zhat = new Vec(0., 0., 1.);
                    uhat = t1.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
                    vhat = t1.cross(uhat);
                    RotMatrix Rtmp = new RotMatrix(uhat, vhat, t1);
                    // Tk.origin.print("origin before scatter");
                    // Tk.X0.print("X0 before scatter");
                    Tk = Tk.randomScat(thisSi.p, rscat, pInt, thisSi.thickness);
                    TkEnd = Tk;
                    helixSaved[icm] = Tk.p.copy();
                    pivotSaved[icm] = Tk.R.inverseRotate(Tk.X0).sum(Tk.origin);
                    // Tk.origin.print("origin after scatter");
                    // Tk.X0.print("X0 after scatter");
                    // Tk.R.invert().print("R inverted");
                    // pivotSaved[icm].print("saved pivot");
                    // System.out.format("%d location=%10.5f\n", icm, location[icm]);
                    Vec t2 = Tk.getMomGlobal(0.).unitVec();
                    double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                    double scattAngProj = Math.atan2(t1.v[0], t1.v[1]) - Math.atan2(t2.v[0], t2.v[1]);
                    hScatProj.entry(scattAngProj * 180. / Math.PI);
                    hScat.entry(scattAng);
                    if (verbose) {
                        Tk.print("scattered from the first layer of the detector plane");
                        Vec p2 = Tk.getMomGlobal(0.);
                        p2.print("momentum after scatter");
                        System.out.format("Scattering angle from 1st layer of thickness %10.5f = %10.7f; p=%10.7f\n", thickness, scattAng,
                                p2.mag());
                    }
                    Vec t2Loc = Rtmp.rotate(t2);

                    double phiScat = Math.atan2(t2Loc.v[1], t2Loc.v[0]);
                    if (thisSi.isStereo) {
                        hps2.entry(phiScat / Math.PI);
                        hsp2theta.entry(Math.asin(t2Loc.v[1]));
                    } else {
                        hps1.entry(phiScat / Math.PI);
                        hsp1theta.entry(Math.asin(t2Loc.v[1]));
                    }
                } else {
                    TkEnd = Tk;
                    if (verbose) { TkEnd.print("TkEnd"); }
                }
            }
            if (verbose) printWriter2.format("EOD\n");

            if (verbose) {
                printWriter2.format("$pnts << EOD\n");
                System.out.format("\n\n ******* Printing out the list of Si modules: ********\n");
                for (SiModule si : SiModules) {
                    si.print("in list");
                    for (Measurement mm : si.hits) {
                        Vec rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector
                                                           // frame
                        Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                }
                printWriter2.format("EOD\n");
                printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                printWriter2.format(", $helix u 1:2:3 with lines lw 3\n");
                printWriter2.close();
            }

            int nHits = 0;
            for (SiModule siM : SiModules) {
                if (siM.hits.size() > 0) nHits++;           
            }
            hnHit.entry(nHits);

            // Create a seed track from the first 3 or 4 layers
            int nA = 0;
            int nS = 0;
            int frstLyr = 0;
            ArrayList<int[]> hitList = new ArrayList<int[]>(nAxial + nStereo);
            for (int i = SiModules.size() - 1; i >= 0; i--) {
                SiModule si = SiModules.get(i);
                if (si.Layer > startLayer) continue;
                if (si.hits.isEmpty()) continue;
                if (nA < nAxial) {
                    if (!si.isStereo) {
                        int[] ht = new int[2];
                        ht[0] = i;
                        ht[1] = 0;
                        hitList.add(ht);
                        nA++;
                        frstLyr = si.Layer;
                    }
                } else {
                    if (nS >= nStereo) break;
                }
                if (nS < nStereo) {
                    if (si.isStereo) {
                        int[] ht = new int[2];
                        ht[0] = i;
                        ht[1] = 0;
                        hitList.add(ht);
                        nS++;
                        frstLyr = si.Layer;
                    }
                } else {
                    if (nA >= nStereo) break;
                }
            }
            if (nS < nStereo || nA < nAxial) {
                System.out.format("Not enough hits for SeedTrack. nA=%d  nS=%d\n", nA, nS);
                continue;
            }

            SeedTrack seed = new SeedTrack(SiModules, location[frstLyr], hitList, verbose);
            if (!seed.success) {
                System.out.format("Failed to make a seed track\n");
                continue;
            }
            if (verbose) {
                seed.print("helix parameters");
                System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
            }
            hchi2G.entry(seed.chi2);
            Vec initialHelixGuess = seed.helixParams();
            SquareMatrix initialCovariance = seed.covariance();
            Vec GuessErrors = seed.errors();

            // For comparison, get the true helix in the B field frame at the first layer of
            // the linear fit
            // Then transform it to the same pivot used by the linear fit. Here we have to
            // ignore details about
            // coordinate system rotation, because the linear fit is only done in the global
            // frame assuming constant field

            helixMCtrue = helixSaved[frstLyr];
            Vec pivotOnAxis = new Vec(0., location[frstLyr], 0.);
            Vec Bpiv = fMg.getField(pivotOnAxis);
            double alpha = 1.0e12 / (2.99793e8 * Bpiv.mag());
            Vec helixTrueTrans = StateVector.pivotTransform(pivotOnAxis, helixMCtrue, pivotSaved[frstLyr], alpha, 0.);
            Vec gErrVec = initialHelixGuess.dif(helixTrueTrans);
            // new Vec(initialHelixGuess.v[0] - drho, initialHelixGuess.v[1] - phi0,
            // initialHelixGuess.v[2] - K,
            // initialHelixGuess.v[3] - dz, initialHelixGuess.v[4] - tanl);
            double[] gErr = new double[5];
            for (int i = 0; i < 5; i++) { gErr[i] = gErrVec.v[i] / GuessErrors.v[i]; }
            if (verbose) {
                // helixMCtrue.print("MC true helix at this layer");
                // pivotSaved[frstLyr].print("old pivot");
                // pivotOnAxis.print("new pivot");
                // Bpiv.print("field at old pivot");
                System.out.format("Comparing linear helix fit with true at layer %d, alpha=%10.5f\n", frstLyr, alpha);
                System.out.format("Guess drho=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[0],
                        helixTrueTrans.v[0], GuessErrors.v[0], gErr[0]);
                System.out.format("Guess phi0=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[1],
                        helixTrueTrans.v[1], GuessErrors.v[1], gErr[1]);
                System.out.format("Guess K=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[2],
                        helixTrueTrans.v[2], GuessErrors.v[2], gErr[2]);
                System.out.format("Guess dz=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[3],
                        helixTrueTrans.v[3], GuessErrors.v[3], gErr[3]);
                System.out.format("Guess tanl=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[4],
                        helixTrueTrans.v[4], GuessErrors.v[4], gErr[4]);
            }
            hEdrhoG.entry(gErr[0]);
            hEphi0G.entry(gErr[1]);
            hEkG.entry(gErr[2]);
            hEdzG.entry(gErr[3]);
            hEtanlG.entry(gErr[4]);
            hEdrhoG1.entry(gErrVec.v[0]);
            hEphi0G1.entry(gErrVec.v[1]);
            hEkG1.entry(gErrVec.v[2]);
            hEdzG1.entry(gErrVec.v[3]);
            hEtanlG1.entry(gErrVec.v[4]);
            double guessHelixChi2 = gErrVec.dot(gErrVec.leftMultiply(initialCovariance.invert()));
            hChi2Guess.entry(guessHelixChi2);

            // double Bstart = seed.B();
            // Vec tBstart = new Vec(0., 0., 1.);

            // Cheating initial "guess" for the helix

            double[] rn = new double[2];
            if (perfect) {
                rn[0] = 0.;
                rn[1] = 0.;
            } else {
                //rn = gausRan();
                rn[0] = rnd.nextGaussian();
                rn[1] = rnd.nextGaussian();
            }
            double drhoGuess = drho + drhoSigma * rn[0];
            double dzGuess = dz + dzSigma * rn[1];
            if (!perfect) {
                //rn = gausRan();
                rn[0] = rnd.nextGaussian();
                rn[1] = rnd.nextGaussian();
            }
            double phi0Guess = phi0 + phi0Sigma * rn[0];
            double tanlGuess = tanl + tanlSigma * rn[1];
            if (!perfect) {
                //rn = gausRan();
                rn[0] = rnd.nextGaussian();
            }
            double kGuess = K + kSigma * rn[0];

            if (cheat) {
                initialHelixGuess = new Vec(drhoGuess, phi0Guess, kGuess, dzGuess, tanlGuess);
                initialCovariance = new SquareMatrix(5);
                initialCovariance.M[0][0] = (drhoSigma * drhoSigma);
                initialCovariance.M[1][1] = (phi0Sigma * phi0Sigma);
                initialCovariance.M[2][2] = (kSigma * kSigma);
                initialCovariance.M[3][3] = (dzSigma * dzSigma);
                initialCovariance.M[4][4] = (tanlSigma * tanlSigma);

                Vec Bf0 = fMg.getField(helixOrigin);
                if (verbose) {
                    initialHelixGuess.print("initial helix guess");
                    System.out.format("True helix: %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
                    helixOrigin.print("initial pivot guess");
                    Bf0.print("B field at pivot");
                }
            }

            initialCovariance.scale(1000.); // Blow up the errors on the initial guess

            if (verbose) { initialCovariance.print("initial covariance guess"); }
            // Run the Kalman fit
            KalmanTrackFit2 kF = new KalmanTrackFit2(iTrial, SiModules, startLayer, nIteration, new Vec(0., location[frstLyr], 0.),
                    initialHelixGuess, initialCovariance, fM, verbose);
            if (!kF.success) { continue; }
            KalTrack KalmanTrack = kF.tkr;
            KalmanTrack.originHelix();
            if (verbose) KalmanTrack.print("KalmanTrack");

            double chi2s = 0.;
            ArrayList<MeasurementSite> sites = kF.sites;
            for (MeasurementSite site : sites) {
                if (site.m.hits.size() > 0) {
                    SiModule siM = site.m;
                    if (site.m.Layer >= 0) {
                        if (site.filtered) {
                            if (siM.isStereo) hResid0.entry(site.aF.r / Math.sqrt(site.aF.R));
                            else hResid1.entry(site.aF.r / Math.sqrt(site.aF.R));
                        }
                        if (site.smoothed) {
                            if (site.m.Layer == 4) hReducedErr.entry(Math.sqrt(site.aS.R));
                            chi2s += Math.pow(site.aS.mPred - site.m.hits.get(site.hitID).vTrue, 2) / site.aS.R;
                            hResidS0[siM.Layer].entry(site.aS.r / Math.sqrt(site.aS.R));
                            hResidS2[siM.Layer].entry(site.aS.r);
                            if (site.hitID >= 0) { hResidS4[siM.Layer].entry(site.m.hits.get(site.hitID).vTrue - site.aS.mPred); }
                        }
                    }
                }
            }
            for (MeasurementSite site : KalmanTrack.interceptVects.keySet()) {
                Vec loc = KalmanTrack.interceptVects.get(site);
                SiModule siM = site.m;
                if (siM.Layer < 0) continue;
                if (site.hitID < 0) { System.out.format("Missing hit ID on site with layer=%d", siM.Layer); }
                Vec locMC = site.m.hits.get(site.hitID).rGlobal;
                hResidX[siM.Layer].entry(loc.v[0] - locMC.v[0]);
                hResidZ[siM.Layer].entry(loc.v[2] - locMC.v[2]);
            }
            hChi2Alt.entry(chi2s);
            hChi2.entry(kF.chi2s);
            hChi2f.entry(kF.chi2f);
            double chi2prm = KalmanTrack.chi2prime();
            hChi2p.entry(chi2prm);

            if (verbose) {
                System.out.format("HelixTest3: initial-site=%d, final-site=%d, # sites=%d, chi^2s=%10.4f chi^2true=%10.4f chi^2_prm=%10.4f\n",
                        kF.initialSite, kF.finalSite, kF.sites.size(), kF.chi2s, chi2s, chi2prm);
            }
            if (kF.sites.size() > 0) {
                for (MeasurementSite site : kF.sites) {
                    hXscat.entry(site.scatX() * 180. / Math.PI);
                    hZscat.entry(site.scatZ() * 180. / Math.PI);
                }
                if (kF.sites.get(kF.initialSite).aS != null && kF.sites.get(kF.finalSite).aS != null) {
                    if (verbose) {
                        KalmanTrack.plot(path);
                        kF.fittedStateBegin().a.print("fitted helix parameters at the first layer");
                        kF.fittedStateBegin().origin.print("fitted helix origin at first layer");
                        helixBegin.p.print("actual helix parameters at the first layer");
                        helixBegin.origin.print("origin of actual helix");
                        helixBegin.X0.print("pivot of actual helix");
                    }
                    Vec newPivot = kF.fittedStateBegin().toLocal(helixBegin.origin.sum(helixBegin.X0));
                    Vec aF = kF.fittedStateBegin().pivotTransform(newPivot);
                    // now rotate to the original field frame
                    SquareMatrix fRot = new SquareMatrix(5);
                    RotMatrix Rcombo = helixBegin.R.multiply(kF.fittedStateBegin().Rot.invert());
                    aF = StateVector.rotateHelix(aF, Rcombo, fRot);
                    if (verbose) {
                        Rcombo.print("Rcombo, into the frame of the true helix");
                        aF.print("final smoothed helix parameters at the track beginning");
                        newPivot.print("final smoothed helix pivot in local coordinates");
                    }
                    Vec aFe = new Vec(5);
                    SquareMatrix aFC = kF.fittedStateBegin().covariancePivotTransform(aF);
                    aFC = aFC.similarity(fRot);
                    for (int i = 0; i < 5; i++) { aFe.v[i] = Math.sqrt(Math.max(0., aFC.M[i][i])); }
                    if (verbose) { aFe.print("error estimates on the smoothed helix parameters"); }
                    Vec trueErr = aF.dif(helixBegin.p);
                    if (verbose) {
                        for (int i = 0; i < 5; i++) {
                            double diff = (trueErr.v[i]) / aFe.v[i];
                            System.out.format("     Helix parameter %d after smoothing, error = %10.5f sigma\n", i, diff);
                        }
                    }
                    hEdrhoS.entry(trueErr.v[0] / aFe.v[0]);
                    hEphi0S.entry(trueErr.v[1] / aFe.v[1]);
                    hEkS.entry(trueErr.v[2] / aFe.v[2]);
                    hEdzS.entry(trueErr.v[3] / aFe.v[3]);
                    hEtanlS.entry(trueErr.v[4] / aFe.v[4]);
                    double helixChi2 = trueErr.dot(trueErr.leftMultiply(aFC.invert()));
                    hChi2HelixS.entry(helixChi2);
                    if (verbose) {
                        System.out.format("Full chi^2 of the smoothed helix parameters = %12.4e\n", helixChi2);
                        TkEnd.print("MC true helix at the last detector plane");
                        kF.fittedStateEnd().print("fitted state at the last detector plane");
                    }
                    newPivot = kF.fittedStateEnd().toLocal(TkEnd.R.inverseRotate(TkEnd.X0).sum(TkEnd.origin));
                    Vec eF = kF.fittedStateEnd().pivotTransform(newPivot);
                    // Rcombo = TkEnd.R.multiply(kF.fittedStateEnd().Rot.invert());
                    // eF = kF.fittedStateEnd().rotateHelix(eF, Rcombo, fRot);
                    if (verbose) {
                        eF.print("final smoothed helix parameters at the track end");
                        newPivot.print("new pivot at the track end");
                    }
                    SquareMatrix eFc = kF.fittedStateEnd().covariancePivotTransform(eF);
                    // eFc = eFc.similarity(fRot);
                    Vec eFe = new Vec(5);
                    for (int i = 0; i < 5; i++) { eFe.v[i] = Math.sqrt(Math.max(0., eFc.M[i][i])); }
                    Vec fH = TkEnd.p;
                    trueErr = eF.dif(fH);
                    if (verbose) {
                        eFe.print("errors on the helix parameters");
                        fH.print("MC true helix parameters at the last detector plane");
                        for (int i = 0; i < 5; i++) {
                            double diff = (trueErr.v[i]) / aFe.v[i];
                            System.out.format("     Helix parameter %d, error = %10.5f sigma\n", i, diff);
                        }
                    }
                    hEdrho.entry(trueErr.v[0] / eFe.v[0]);
                    hEphi0.entry(trueErr.v[1] / eFe.v[1]);
                    hEk.entry(trueErr.v[2] / eFe.v[2]);
                    hEdz.entry(trueErr.v[3] / eFe.v[3]);
                    hEtanl.entry(trueErr.v[4] / eFe.v[4]);
                    trueErr = eF.dif(fH);
                    helixChi2 = trueErr.dot(trueErr.leftMultiply(eFc.invert()));
                    if (verbose) { System.out.format("Full chi^2 of the filtered helix parameters = %12.4e\n", helixChi2); }
                    hChi2Helix.entry(helixChi2);

                    // Study the fitted helix extrapolated back to the origin
                    double[] hP = KalmanTrack.originHelixParms();
                    double[] hErr = new double[5];
                    for (int i = 0; i < 5; ++i) { hErr[i] = (hP[i] - TkInitial.p.v[i]); }
                    hEdrhoO.entry(hErr[0] / KalmanTrack.helixErr(0));
                    hEphi0O.entry(hErr[1] / KalmanTrack.helixErr(1));
                    hEkO.entry(hErr[2] / KalmanTrack.helixErr(2));
                    hEdzO.entry(hErr[3] / KalmanTrack.helixErr(3));
                    hEtanlO.entry(hErr[4] / KalmanTrack.helixErr(4));
                    double[] originPnt = KalmanTrack.originX();
                    double[] originMom = KalmanTrack.originP();
                    double[][] xCov = KalmanTrack.originXcov();
                    double[][] pCov = KalmanTrack.originPcov();
                    double xErr[] = new double[3];
                    double pErr[] = new double[3];
                    for (int i = 0; i < 3; ++i) {
                        xErr[i] = (originPnt[i] - helixOrigin.v[i]) / Math.sqrt(xCov[i][i]);
                        pErr[i] = (originMom[i] - momentum.v[i]) / Math.sqrt(pCov[i][i]);
                    }
                    Vec oHerr = new Vec(5, hErr);
                    SquareMatrix oCov = new SquareMatrix(5, KalmanTrack.originCovariance());
                    SquareMatrix corr = new SquareMatrix(5);
                    for (int i = 0; i < 5; ++i) {
                        for (int j = 0; j < 5; ++j) { corr.M[i][j] = oCov.M[i][j] / KalmanTrack.helixErr(i) / KalmanTrack.helixErr(j); }
                    }
                    double oHelixChi2 = oHerr.dot(oHerr.leftMultiply(oCov.invert()));
                    hChi2Origin.entry(oHelixChi2);
                    if (verbose) {
                        helixOrigin.print("MC True particle origin");
                        System.out.format("Fitted particle origin=%10.6f %10.6f %10.6f\n", originPnt[0], originPnt[1], originPnt[2]);
                        momentum.print("MC True particle initial momentum");
                        System.out.format("Fitted particle momentum=%10.6f %10.6f %10.6f\n", originMom[0], originMom[1], originMom[2]);
                        // new SquareMatrix(3, pCov).print("momentum covariance");
                        System.out.format("Vertex and momentum errors in x,y,z\n");
                        for (int i = 0; i < 3; ++i) {
                            System.out.format("%d pnt: %10.5f %10.5f %8.3f    ", i, originPnt[i], helixOrigin.v[i], xErr[i]);
                            System.out.format("mom: %10.5f %10.5f %8.3f\n", originMom[i], momentum.v[i], pErr[i]);
                        }
                        System.out.format("Helix parameter errors.  Full Chi^2 = %12.5e\n", oHelixChi2);
                        for (int i = 0; i < 5; ++i) {
                            double e = hErr[i] / KalmanTrack.helixErr(i);
                            System.out.format("%d helix: %10.6f %10.6f %9.4f %8.3f %8.3f\n", i, hP[i], TkInitial.p.v[i],
                                    KalmanTrack.helixErr(i), hErr[i], e);
                        }
                    }
                }
            }
        }

        long grdef = rnd.nextLong();
        System.out.format("New seed = %d\n", grdef);
        timestamp = Instant.now();
        System.out.format("Ending time = %s\n", timestamp.toString());
        ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        hGaus.plot(path + "Gaussian.gp", true, "gaus", " ");
        hReducedErr.plot(path + "ReducedErr.gp", true, " ", " ");
        hXscat.plot(path + "XscatAng.gp", true, " ", " ");
        hZscat.plot(path + "ZscatAng.gp", true, " ", " ");
        hScat.plot(path + "scatAng.gp", true, " ", " ");
        hScatProj.plot(path + "scatAngProj.gp", true, " ", " ");
        hchi2G.plot(path + "chi2G.gp", true, " ", " ");
        hnHit.plot(path + "nHits.gp", true, " ", " ");
        hps1.plot(path + "phiScat1.gp", true, " ", " ");
        hsp1theta.plot(path + "projScat1.gp", true, " ", " ");
        hps2.plot(path + "phiScat2.gp", true, " ", " ");
        hsp2theta.plot(path + "projScat2.gp", true, " ", " ");
        hChi2.plot(path + "chi2s.gp", true, " ", " ");
        hChi2Alt.plot(path + "chi2sAlt.gp", true, " ", " ");
        hChi2p.plot(path + "chi2prm.gp", true, " ", " ");
        hChi2f.plot(path + "chi2f.gp", true, " ", " ");
        hChi2HelixS.plot(path + "chi2helixS.gp", true, " ", " ");
        hChi2Origin.plot(path + "chi2helixO.gp", true, " ", " ");
        hChi2Helix.plot(path + "chi2helixF.gp", true, " ", " ");
        hChi2Guess.plot(path + "chi2HelixGuess.gp", true, " ", " ");
        hRes.plot(path + "resolution.gp", true, " ", " ");
        hEdrho.plot(path + "drhoError.gp", true, " ", " ");
        hEphi0.plot(path + "phi0Error.gp", true, " ", " ");
        hEk.plot(path + "kError.gp", true, " ", " ");
        hEdz.plot(path + "dzError.gp", true, " ", " ");
        hEtanl.plot(path + "tanlError.gp", true, " ", " ");
        hEdrhoS.plot(path + "drhoErrorS.gp", true, "gaus", " ");
        hEphi0S.plot(path + "phi0ErrorS.gp", true, "gaus", " ");
        hEkS.plot(path + "kErrorS.gp", true, "gaus", " ");
        hEdzS.plot(path + "dzErrorS.gp", true, "gaus", " ");
        hEtanlS.plot(path + "tanlErrorS.gp", true, "gaus", " ");
        hEdrhoO.plot(path + "drhoErrorO.gp", true, "gaus", " ");
        hEphi0O.plot(path + "phi0ErrorO.gp", true, "gaus", " ");
        hEkO.plot(path + "kErrorO.gp", true, "gaus", " ");
        hEdzO.plot(path + "dzErrorO.gp", true, "gaus", " ");
        hEtanlO.plot(path + "tanlErrorO.gp", true, "gaus", " ");
        hEdrhoG.plot(path + "drhoErrorG.gp", true, " ", " ");
        hEphi0G.plot(path + "phi0ErrorG.gp", true, " ", " ");
        hEkG.plot(path + "kErrorG.gp", true, " ", " ");
        hEdzG.plot(path + "dzErrorG.gp", true, " ", " ");
        hEtanlG.plot(path + "tanlErrorG.gp", true, " ", " ");
        hEdrhoG1.plot(path + "drhoErrorG1.gp", true, " ", " ");
        hEphi0G1.plot(path + "phi0ErrorG1.gp", true, " ", " ");
        hEkG1.plot(path + "kErrorG1.gp", true, " ", " ");
        hEdzG1.plot(path + "dzErrorG1.gp", true, " ", " ");
        hEtanlG1.plot(path + "tanlErrorG1.gp", true, " ", " ");
        hResid0.plot(path + "resid0.gp", true, " ", " ");
        hResid1.plot(path + "resid1.gp", true, " ", " ");
        for (int i = 0; i < nLayers; i++) {
            hResidS0[i].plot(path + String.format("residS0_%d.gp", i), true, "gaus", " ");
            hResidS2[i].plot(path + String.format("residS2_%d.gp", i), true, "gaus", " ");
            hResidS4[i].plot(path + String.format("residS4_%d.gp", i), true, "gaus", " ");
            hResidX[i].plot(path + String.format("residX_%d.gp", i), true, "gaus", " ");
            hResidZ[i].plot(path + String.format("residZ_%d.gp", i), true, "gaus", " ");
        }
    }
    /*
    double[] gausRan() { // Return two gaussian random numbers
    
        double x1, x2, w;
        double[] gran = new double[2];
        do {
            x1 = 2.0 * Math.random() - 1.0;
            x2 = 2.0 * Math.random() - 1.0;
            //x1 = 2.0 * rnd.nextDouble() - 1.0;
            //x2 = 2.0 * rnd.nextDouble() - 1.0;
            w = x1 * x1 + x2 * x2;
        } while (w >= 1.0);
        w = Math.sqrt((-2.0 * Math.log(w)) / w);
        gran[0] = x1 * w;
        gran[1] = x2 * w;
    
        return gran;
    }
    */
}
