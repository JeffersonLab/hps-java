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
class PatRecTest {

    Random rnd;

    PatRecTest(String path) {
        // Units are Tesla, GeV, mm

        int nTrials = 100;              // The number of test eventNumbers to generate for pattern recognition and fitting
        int mxPlot = 20;                // Maximum number of single event plots
        int [] eventToPrint = {1,20};  // Range of events to print in detail and plot as an event display
        boolean perfect = false;

        boolean rungeKutta = true;      // Set true to generate the helix by Runge Kutta integration instead of a piecewise helix
        boolean verbose = false;
        boolean noisy = true;

        // Seed the random number generator
        long rndSeed = -3113005327838135103L;
        rnd = new Random();
        rnd.setSeed(rndSeed);

        // Set pattern recognition parameters
        KalmanParams kPar = new KalmanParams();
        
        // Definition of the magnetic field
        String mapType = "binary";
        //String mapFile = "C:\\Users\\Robert\\Documents\\GitHub\\hps-java\\fieldmap\\125acm2_3kg_corrected_unfolded_scaled_0.7992_v3.bin";
        String mapFile = "C:\\Users\\Robert\\Documents\\GitHub\\hps-java\\fieldmap\\209acm2_5kg_corrected_unfolded_scaled_1.04545_v4.bin";
        FieldMap fM = null;
        try {
            fM = new FieldMap(mapFile, mapType, true, 21.17, 0., 457.2);       // for fitting tracks
        } catch (IOException e) {
            System.out.format("Could not open or read the field map %s\n", mapFile);
            return;
        }
        if (mapType != "binary") fM.writeBinaryFile("C:\\Users\\Robert\\Documents\\GitHub\\hps-java\\fieldmap\\fieldmap.bin");

        // Tracking instrument description
        double thickness = 0.32; // Silicon thickness in mm
        if (perfect) { thickness = 0.0000000000001; }
        ArrayList<SiModule> SiModules = new ArrayList<SiModule>();
        Plane plnInt;
        SiModule newModule;

        double yStart = 103.69;
        plnInt = new Plane(new Vec(3.4814, yStart, 20.781), new Vec(-0.030928, -0.99952, 0.00056169), -0.100076);
        newModule = new SiModule(2, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(3.7752, 111.75, 20.770), new Vec(0.029092, 0.99957, 0.0031495), 0.000303);
        newModule = new SiModule(3, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(6.6595, 203.81, 22.296), new Vec(-0.029875, -0.99954, 0.0053661), -0.099851);
        newModule = new SiModule(4, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(6.7661, 211.87, 22.281), new Vec(0.028940, 0.99958, 0.0028008), 0.000145);
        newModule = new SiModule(5, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(9.4835, 303.76, 23.796), new Vec(-0.029471, -0.99955, 0.0048642), -0.100012);
        newModule = new SiModule(6, plnInt, true, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(9.7121, 311.63, 23.777), new Vec(0.027875, 0.99961, -0.0027053), 0.000106);
        newModule = new SiModule(7, plnInt, false, 200., 47.17, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-35.087, 505.57, 29.328), new Vec(-0.029044, -0.99958, 0.0022785), -0.049060);
        newModule = new SiModule(8, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(65.791, 502.52, 24.294), new Vec(-0.030402, -0.99954, 0.0012687), -0.050671);
        newModule = new SiModule(8, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-34.848, 513.08, 26.824), new Vec(0.030086, 0.99954, -0.0021664), 0.000199);
        newModule = new SiModule(9, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(65.958, 510.03, 26.821), new Vec(0.030452, 0.99954, -0.00060382), 0.000194);
        newModule = new SiModule(9, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-29.010, 705.47, 32.358), new Vec(-0.030508, -0.99953, -0.00048837), -0.050035);
        newModule = new SiModule(10, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(71.778, 702.43, 27.322), new Vec(-0.029627, -0.99956, -0.0015542), -0.050102);
        newModule = new SiModule(10, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-28.846, 713.07, 29.845), new Vec(0.029810, 0.99956, -0.00084633), 0.000172);
        newModule = new SiModule(11, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(72.034, 710.03, 29.845), new Vec(0.030891, 0.99952, 0.00016092), 0.000205);
        newModule = new SiModule(11, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-22.879, 905.35, 35.309), new Vec(-0.029214, -0.99957, 0.0019280), -0.049801);
        newModule = new SiModule(12, plnInt, true, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(77.869, 902.35, 30.284), new Vec(-0.029989, -0.99955, -0.00062471), -0.049863);
        newModule = new SiModule(12, plnInt, true, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(-22.795, 912.89, 32.839), new Vec(0.028266, 0.99960, -0.0014105), 0.000107);
        newModule = new SiModule(13, plnInt, false, 100., 40.34, thickness, fM, 0);
        SiModules.add(newModule);

        plnInt = new Plane(new Vec(78.097, 909.99, 32.835), new Vec(0.030889, 0.99952, -0.00029751), 0.000071);
        newModule = new SiModule(13, plnInt, false, 100., 40.34, thickness, fM, 1);
        SiModules.add(newModule);
        
        for (SiModule siM : SiModules) {
            siM.print(" ");
        }

        int nLayers = 14; // Layer 0 not yet implemented here
        double resolution = 0.006; // SSD point resolution, in mm
        double hitEfficiency = 0.98;
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

        int nHelices = 2; // Number of helix tracks to simulate
        double[] Q = new double[nHelices]; // charge
        double[] p = new double[nHelices]; // momentum
        Vec helixOrigin = new Vec(0., 0., 0.); // Pivot point of initial helices
        double Phi = 91. * Math.PI / 180.;
        double Theta = 88.5 * Math.PI / 180.;


        // Define histograms
        Histogram hNtracks = new Histogram(10, 0., 1., "Number of tracks found and fitted", "tracks", "events");
        Histogram hNhits = new Histogram(15, 0., 1., "Number of hits per fitted track", "hits", "tracks");
        Histogram hScatProj = new Histogram(100, -0.01, 0.0002, "Projected Scattering Angle", "radians", "Si planes");
        Histogram hTkChi2 = new Histogram(100, 0., 1.0, "Track helix fit chi^2 after smoothing", "chi^2", "tracks");
        Histogram hEdrhoS = new Histogram(100, -10., 0.2, "Smoothed helix parameter drho error", "sigmas", "track");
        Histogram hEdrho = new Histogram(100, -2., 0.04, "drho error", "mm", "tracks");
        Histogram hEphi0S = new Histogram(100, -10., 0.2, "Smoothed helix parameter phi0 error", "sigmas", "track");
        Histogram hEphi0 = new Histogram(100, -2., 0.04, "phi0 error", "degrees", "tracks");
        Histogram hEkS = new Histogram(100, -10., 0.2, "Smoothed helix parameter K error", "sigmas", "track");
        Histogram hEk = new Histogram(100, -40., 0.8, "curvature error", "percent", "tracks");
        Histogram hEdzS = new Histogram(100, -10., 0.2, "Smoothed helix parameter dz error", "sigmas", "track");
        Histogram hEdz = new Histogram(100, -1., 0.02, "dz error", "mm", "tracks");
        Histogram hEtanlS = new Histogram(100, -10., 0.2, "Smoothed helix parameter tanl error", "sigmas", "track");
        Histogram hEtanl = new Histogram(100, -.01, 0.0002, "tanl error", " ", "tracks");
        Histogram hChi2HelixS = new Histogram(80, 0., 0.4, "Smoothed chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hXerr = new Histogram(100, -20., 0.4, "error on the vertex x coordinate", "sigmas", "tracks");
        Histogram hYerr = new Histogram(100, -20., 0.4, "error on the vertex y coordinate", "sigmas", "tracks");
        Histogram hZerr = new Histogram(100, -20., 0.4, "error on the vertex z coordinate", "sigmas", "tracks");
        Histogram hEdrhoS1 = new Histogram(100, -10., 0.2, "Smoothed helix parameter drho error at lyr1", "sigmas", "track");
        Histogram hEphi0S1 = new Histogram(100, -10., 0.2, "Smoothed helix parameter phi0 error at lyr1", "sigmas", "track");
        Histogram hEkS1 = new Histogram(100, -10., 0.2, "Smoothed helix parameter K error at lyr1", "sigmas", "track");
        Histogram hEdzS1 = new Histogram(100, -10., 0.2, "Smoothed helix parameter dz error at lyr1", "sigmas", "track");
        Histogram hEtanlS1 = new Histogram(100, -10., 0.2, "Smoothed helix parameter tanl error at lyr1", "sigmas", "track");
        Histogram hMomentum = new Histogram(100, 0., 0.05, "Reconstructed track momentum","GeV","tracks");
        Histogram[] hScatXY = new Histogram[nLayers];
        Histogram[] hScatZY = new Histogram[nLayers];
        Histogram[] hResidS0 = new Histogram[nLayers];
        Histogram[] hResidS2 = new Histogram[nLayers];
        for (int i = 2; i < nLayers; i++) {
            hScatXY[i] = new Histogram(100, -0.001, 0.00002, String.format("Scattering angle in xy for layer %d", i), "radians", "tracks");
            hScatZY[i] = new Histogram(100, -0.001, 0.00002, String.format("Scattering angle in xy for layer %d", i), "radians", "tracks");
            hResidS0[i] = new Histogram(100, -10., 0.2, String.format("Smoothed fit residual for plane %d", i), "sigmas", "hits");
            hResidS2[i] = new Histogram(100, -0.02, 0.0004, String.format("Smoothed fit residual for plane %d", i), "mm", "hits");
        }
        
        Instant timestamp = Instant.now();
        System.out.format("Beginning time = %s\n", timestamp.toString());
        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        Helix[] TkSaved = new Helix[nHelices];
        HelixPlaneIntersect hpi = new HelixPlaneIntersect();
        int nPlot = 0;
        for (int eventNumber = 0; eventNumber < nTrials; eventNumber++) {
            if (Math.floorMod(eventNumber, 100)==0) System.out.format("Starting trial event %d\n", eventNumber);
            verbose = (eventNumber >= eventToPrint[0] && eventNumber <= eventToPrint[1]);
            
            Vec[] initialDirection = new Vec[nHelices];
            for (int i = 0; i < nHelices; i++) {
                if (rnd.nextGaussian() > 0.) Q[i] = 1.0;    // Randomize the momentum vectors
                else Q[i] = -1.0;
                p[i] = 2.4 + rnd.nextGaussian() * 0.02;
                double PhiR = Phi + rnd.nextGaussian() * 0.25 * Math.PI / 180.;
                double ThetaR = Theta + rnd.nextGaussian() * 0.25 * Math.PI / 180.;
                initialDirection[i] = new Vec(Math.cos(PhiR) * Math.sin(ThetaR), Math.sin(PhiR) * Math.sin(ThetaR), Math.cos(ThetaR));
                if (verbose) initialDirection[i].print("initial particle direction");
            }
            double[] drho = new double[nHelices]; // { -0., 0., 1. }; // Helix parameters
            double[] dz = new double[nHelices]; // { 5.0, 1.0, 4.0 };
            double[] phi0 = new double[nHelices]; // { 0.0, 0.04, 0.05 };
            double[] tanl = new double[nHelices]; // { 0.1, 0.12, 0.13 };
            double[] K = new double[nHelices];
            Helix[] TkInitial = new Helix[nHelices];
            Vec[] helixMCtrue = new Vec[nHelices];
            for (int i = 0; i < nHelices; i++) {
                Vec momentum = new Vec(p[i] * initialDirection[i].v[0], p[i] * initialDirection[i].v[1], p[i] * initialDirection[i].v[2]);
                if (verbose) momentum.print("initial helix momentum");
                TkInitial[i] = new Helix(Q[i], helixOrigin, momentum, helixOrigin, fM, rnd);
                drho[i] = TkInitial[i].p.v[0];
                phi0[i] = TkInitial[i].p.v[1];
                K[i] = TkInitial[i].p.v[2];
                dz[i] = TkInitial[i].p.v[3];
                tanl[i] = TkInitial[i].p.v[4];
                if (verbose) TkInitial[i].print(String.format("Initial helix %d", i));
                double pt = p[i] / Math.sqrt(1.0 + tanl[i] * tanl[i]);
                if (verbose) {
                    System.out.format("Momentum p=%10.4f GeV, pt=%10.4f GeV\n", p[i], pt);
                    System.out.format("True starting helix %d is %10.6f %10.6f %10.6f %10.6f %10.6f\n", i, drho[i], phi0[i], K[i], dz[i], tanl[i]);
                }
                helixMCtrue[i] = TkInitial[i].p.copy();
            }
            
            // Extrapolate each helix from the origin to the first detector layer
            SiModule si1 = SiModules.get(0);
            int plnBegin = si1.Layer;
            if (verbose) System.out.format("Beginning layer number = %d", plnBegin);
            Helix[] helixBegin = new Helix[nHelices];
            for (int i = 0; i < nHelices; i++) {
                double phi1 = TkInitial[i].planeIntersect(si1.p);
                if (Double.isNaN(phi1)) {
                    System.out.format("Oops! No intersection found with initial plane");
                    return;
                }
                Vec p1 = new Vec(3);
                HelixPlaneIntersect hpi1 = new HelixPlaneIntersect();
                Vec pivotBegin = hpi1.rkIntersect(si1.p, TkInitial[i].atPhiGlobal(0.), TkInitial[i].getMomGlobal(0.), Q[i], fM, p1);
                helixBegin[i] = new Helix(Q[i], pivotBegin, p1, pivotBegin, fM, rnd);
                if (verbose) helixBegin[i].print("helixBegin");
            }
            PrintWriter printWriter2 = null;
            if (verbose) {
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

            Helix[] Tk = new Helix[nHelices];
            for (int i = 0; i < nHelices; i++) {
                Tk[i] = helixBegin[i].copy();
                if (verbose) Tk[i].print("copied beginning helix");
            }

            for (SiModule thisSi : SiModules) { // Zero out the hit lists from previous event
                thisSi.reset();
            }
            // Populate the Si detector planes with noise hits
            if (noisy) {
                for (SiModule thisSi : SiModules) {
                    // Assume a strip every 60 microns
                    double dy = 0.060;
                    double a = 3.0;
                    int nstrips = (int) ((thisSi.yExtent[1] - thisSi.yExtent[0]) / dy);
                    for (int i = 1; i < nstrips - 1; i++) {
                        double ys = thisSi.yExtent[0] + i * dy;
                        double occ = 0.0002 + 0.005 * Math.exp(-(thisSi.yExtent[1] - ys) / a);
                        if (rnd.nextDouble() < occ) {
                            Vec pntGlobal = thisSi.toGlobal(new Vec(0., ys, 0.));
                            Measurement ms = new Measurement(ys, resolution, 0., pntGlobal, 999.);
                            thisSi.addMeasurement(ms);
                        }
                    }
                }
            }

            // Populate the Si detector planes with hits from helices scattered at each plane
            for (int ih = 0; ih < nHelices; ih++) {
                if (verbose) {
                    printWriter2.format("$helix%d << EOD\n", ih);
                    System.out.format("Begin simulation of helix number %d\n", ih);
                }
                for (int icm = 0; icm < SiModules.size(); icm++) {
                    SiModule thisSi = SiModules.get(icm);
                    if (thisSi.Layer < 0) { continue; }
                    int pln = thisSi.Layer;
                    int det = thisSi.detector;
                    if (verbose) {
                        System.out.format("Extrapolating to plane #%d, detector %d\n", pln, det);
                        Tk[ih].print("this plane");
                    }
                    double phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt)) {
                        if (verbose) { System.out.format("Plane %d, detector %d, no intersection found", pln, det); }
                        break;
                    }
                    if (verbose) { System.out.format("Plane %d, phiInt1= %12.10f\n", pln, phiInt); }
                    Vec rscat = new Vec(3);
                    Vec pInt = new Vec(3);
                    if (rungeKutta) {
                        rscat = hpi.rkIntersect(thisSi.p, Tk[ih].atPhiGlobal(0.), Tk[ih].getMomGlobal(0.), Q[ih], fM, pInt);
                    } else {
                        rscat = Tk[ih].atPhiGlobal(phiInt);
                        pInt = Tk[ih].getMomGlobal(phiInt);
                    }
                    if (verbose) {
                        double check = (rscat.dif(thisSi.p.X()).dot(thisSi.p.T()));
                        System.out.format("Dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
                        Tk[ih].atPhi(phiInt).print("local intersection point of helix");
                        Vec xIntGlob = Tk[ih].atPhiGlobal(phiInt);
                        xIntGlob.print("global intersection point of helix");
                        double dPhi = -Q[ih] * (phiInt) / 20.0;
                        int npnts = 0;
                        for (double phi = 0.; phi < Math.abs(phiInt); phi = phi + dPhi) {
                            npnts++;
                            Vec r = Tk[ih].atPhiGlobal(-Q[ih] * phi);
                            printWriter2.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                            if (npnts > 1000) break;
                        }
                        // printWriter2.format("%10.6f %10.6f %10.6f\n", rscat.v[0], rscat.v[1], rscat.v[2]);
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
                        rscat.print("       Gobal intersection point 1");
                        rDet.print("       helix intersection in detector frame");
                    }
                    // Check whether the intersection is within the bounds of the detector
                    if (rDet.v[0] > thisSi.xExtent[1] || rDet.v[0] < thisSi.xExtent[0] || rDet.v[1] > thisSi.yExtent[1]
                            || rDet.v[1] < thisSi.yExtent[0]) {
                        if (verbose) { System.out.format("     Intersection point is outside of the detector %d in layer %d\n", det, pln); }
                        continue;
                    }
                    if (rnd.nextDouble() < hitEfficiency) { // Apply some hit inefficiency
                        double[] gran = new double[2];
                        if (perfect) {
                            gran[0] = 0.;
                            gran[1] = 0.;
                        } else {
                            gran[0] = rnd.nextGaussian();
                            gran[1] = rnd.nextGaussian();
                        }
                        double m1 = rDet.v[1] + resolution * gran[0];
                        if (verbose) { System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1, rDet.v[1]); }
                        Measurement md = null;
                        for (Measurement mm : thisSi.hits) {
                            if (Math.abs(mm.v - m1) < 0.04) { // assume hits overlap if less than 40 microns apart
                                md = mm;
                                break;
                            }
                        }
                        if (md != null) {
                            md.v = 0.5 * (md.v + m1); // overlapping hits, take the average
                            md.sigma *= 1.5; // reduce resolution for overlapping hits
                            md.addMC(ih);
                            if (verbose) { System.out.format("Overlapping with hit at v=%8.4f\n", md.v); }
                        } else {
                            Measurement thisM1 = new Measurement(m1, resolution, 0., rscat, rDet.v[1]);
                            thisM1.addMC(ih);
                            thisSi.addMeasurement(thisM1);
                            if (verbose) { System.out.format("Adding measurement. Size of hit array=%d\n", thisSi.hits.size()); }
                        }
                    }
                    if (icm + 1 < SiModules.size()) {
                        Vec t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                        if (verbose) {
                            Tk[ih].getMom(phiInt).print("helix local momentum before scatter");
                            Tk[ih].getMomGlobal(phiInt).print("helix global momentum before scatter");
                        }
                        // Scatter the helix before going on to the next plane
                        Tk[ih] = Tk[ih].randomScat(thisSi.p, rscat, pInt, thisSi.thickness);
                        if (pln == plnBegin) {
                            TkSaved[ih] = Tk[ih].copy();
                            if (verbose) { System.out.format("Saving helix after scatter in plane %d\n", pln); }
                        }
                        Vec t2 = Tk[ih].getMomGlobal(0.).unitVec();
                        Vec p2 = Tk[ih].getMomGlobal(0.);                       
                        double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                        double scattAngProj = Math.atan2(t1.v[0], t1.v[1]) - Math.atan2(t2.v[0], t2.v[1]);
                        hScatProj.entry(scattAngProj);
                        if (verbose) {
                            Tk[ih].print("scattered from the detector plane");
                            p2.print("momentum after scatter");
                            System.out.format("Scattering angle from 1st layer of thickness %10.5f = %10.7f; p=%10.7f\n", thickness, scattAng,
                                            p2.mag());
                        }
                    }
                }
                if (verbose) { printWriter2.format("EOD\n"); }
            }

            if (verbose) {
                printWriter2.format("$pnts << EOD\n");
                System.out.format("\n\n ******* Printing out the list of Si modules: ********\n");
                for (SiModule si : SiModules) { System.out.format("Layer %d, size of hit list=%d\n", si.Layer, si.hits.size()); }
                for (SiModule si : SiModules) {
                    si.print("in list");
                    for (Measurement mm : si.hits) {
                        Vec rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                        Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        // printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", mm.rGlobal.v[0], mm.rGlobal.v[1], mm.rGlobal.v[2]);
                        System.out.format("   Hit location %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                }
                printWriter2.format("EOD\n");
                printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                for (int ih = 0; ih < nHelices; ih++) { printWriter2.format(", $helix%d u 1:2:3 with lines lw 3", ih); }
                printWriter2.close();
            }

            if (verbose) System.out.format("\n\n ******* PatRecTest: now making the call to KalmanPatRecHPS.\n");
            KalmanPatRecHPS patRec = new KalmanPatRecHPS(SiModules, 0, eventNumber, kPar, verbose);
            if (nPlot < mxPlot && verbose) {
                nPlot++;
                PrintWriter printWriter3 = null;
                String fn = String.format("%shelix3_%d.gp", path, eventNumber);
                System.out.format("Output single event plot to file %s\n", fn);
                File file3 = new File(fn);
                file3.getParentFile().mkdirs();
                try {
                    printWriter3 = new PrintWriter(file3);
                } catch (FileNotFoundException e1) {
                    System.out.println("Could not create the gnuplot output file.");
                    e1.printStackTrace();
                    return;
                }
                // printWriter3.format("set xrange [-500.:1500]\n");
                // printWriter3.format("set yrange [-1000.:1000.]\n");
                printWriter3.format("set title 'Event Number %d'\n", eventNumber);
                printWriter3.format("set xlabel 'X'\n");
                printWriter3.format("set ylabel 'Y'\n");
                double vPos = 0.9;
                for (KalTrack tkr : patRec.TkrList) {
                    double [] a = tkr.originHelixParms();
                    String s = String.format("TB %d Track %d, %d hits, chi^2=%7.1f, a=%8.3f %8.3f %8.3f %8.3f %8.3f", 
                            patRec.topBottom, tkr.ID, tkr.nHits, tkr.chi2, a[0], a[1], a[2], a[3], a[4]);
                    printWriter3.format("set label '%s' at screen 0.1, %2.2f\n", s, vPos);
                    vPos = vPos - 0.03;
                }
                for (KalTrack tkr : patRec.TkrList) {
                    printWriter3.format("$tkr%d << EOD\n", tkr.ID);
                    for (MeasurementSite site : tkr.SiteList) {
                        StateVector aS = site.aS;
                        SiModule m = site.m;
                        double phiS = aS.planeIntersect(m.p);
                        if (Double.isNaN(phiS)) continue;
                        Vec rLocal = aS.atPhi(phiS);
                        Vec rGlobal = aS.toGlobal(rLocal);
                        printWriter3.format(" %10.6f %10.6f %10.6f\n", rGlobal.v[0], rGlobal.v[1], rGlobal.v[2]);
                        // Vec rDetector = m.toLocal(rGlobal);
                        // double vPred = rDetector.v[1];
                        // if (site.hitID >= 0) {
                        // System.out.format("vPredPrime=%10.6f, vPred=%10.6f, v=%10.6f\n", vPred, aS.mPred, m.hits.get(site.hitID).v);
                        // }
                    }
                    printWriter3.format("EOD\n");
                }
                for (KalTrack tkr : patRec.TkrList) {
                    printWriter3.format("$tkp%d << EOD\n", tkr.ID);
                    for (MeasurementSite site : tkr.SiteList) {
                        SiModule m = site.m;
                        int hitID = site.hitID;
                        if (hitID < 0) continue;
                        Measurement mm = m.hits.get(hitID);
                        Vec rLoc = m.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                        Vec rmG = m.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                    printWriter3.format("EOD\n");
                }
                printWriter3.format("$pnts << EOD\n");
                for (SiModule si : SiModules) {
                    for (Measurement mm : si.hits) {
                        if (mm.tracks.size() > 0) continue;
                        Vec rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                        Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                }
                printWriter3.format("EOD\n");
                printWriter3.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkr%d u 1:2:3 with lines lw 3", tkr.ID); }
                for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkp%d u 1:2:3 with points pt 7 ps 2", tkr.ID); }
                printWriter3.format("\n");
                printWriter3.close();
            }
            // Analysis of the tracking results
            int nTracks = patRec.TkrList.size();
            hNtracks.entry(nTracks);
            for (KalTrack tkr : patRec.TkrList) {
                tkr.sortSites(true);
                if (verbose) {
                    System.out.format("Track %d, %d hits, chi^2=%10.5f, 1st layer=%d\n", tkr.ID, tkr.nHits, tkr.chi2,
                            tkr.SiteList.get(0).m.Layer);
                    tkr.print("from KalmanPatRecHPS");
                }
                hNhits.entry(tkr.nHits);
                hTkChi2.entry(tkr.chi2);
                double[] momentum = tkr.originP();
                double pMag = Math.sqrt(momentum[0]*momentum[0]+momentum[1]*momentum[1]+momentum[2]*momentum[2]);
                hMomentum.entry(pMag);
                for (MeasurementSite site : tkr.SiteList) {
                    int layer = site.m.Layer;
                    hScatXY[layer].entry(tkr.scatX(layer));
                    hScatZY[layer].entry(tkr.scatZ(layer));
                    hResidS0[layer].entry(site.aS.r/Math.sqrt(site.aS.R));
                    hResidS2[layer].entry(site.aS.r);
                }
                // Compare with the generated particles
                if (!tkr.originHelix()) continue;
                Vec helixAtOrigin = new Vec(5, tkr.originHelixParms());
                SquareMatrix Cinv = (new SquareMatrix(5, tkr.originCovariance())).invert();
                double minChi2 = 9999.e33;
                int iBest = -1;
                for (int ih = 0; ih < nHelices; ih++) {
                    Vec trueErr = helixAtOrigin.dif(TkInitial[ih].p);
                    double helixChi2 = trueErr.dot(trueErr.leftMultiply(Cinv));
                    if (helixChi2 < minChi2) {
                        minChi2 = helixChi2;
                        iBest = ih;
                    }
                }
                if (iBest == -1) continue;
                Vec trueErr = helixAtOrigin.dif(TkInitial[iBest].p);
                if (verbose) {
                    TkInitial[iBest].print("Best MC track");
                    for (int i = 0; i < 5; i++) {
                        double diff = (trueErr.v[i]) / tkr.helixErr(i);
                        System.out.format("     Helix parameter %d after smoothing, error = %10.5f sigma, true error=%10.7f, estimate=%10.7f\n",
                                i, diff, trueErr.v[i], tkr.helixErr(i));
                    }
                }
                hEdrhoS.entry(trueErr.v[0] / tkr.helixErr(0));
                hEphi0S.entry(trueErr.v[1] / tkr.helixErr(1));
                hEkS.entry(trueErr.v[2] / tkr.helixErr(2));
                hEdzS.entry(trueErr.v[3] / tkr.helixErr(3));
                hEtanlS.entry(trueErr.v[4] / tkr.helixErr(4));
                hEdrho.entry(trueErr.v[0]);
                hEphi0.entry(trueErr.v[1] * 180. / Math.PI);
                hEk.entry(100. * trueErr.v[2] / helixAtOrigin.v[2]);
                hEdz.entry(trueErr.v[3]);
                hEtanl.entry(trueErr.v[4]);
                hChi2HelixS.entry(minChi2);
                double xErr = (tkr.originX()[0] - helixOrigin.v[0]) / Math.sqrt(tkr.originXcov()[0][0]);
                hXerr.entry(xErr);
                double yErr = (tkr.originX()[1] - helixOrigin.v[1]) / Math.sqrt(tkr.originXcov()[1][1]);
                hYerr.entry(yErr);
                double zErr = (tkr.originX()[2] - helixOrigin.v[2]) / Math.sqrt(tkr.originXcov()[2][2]);
                hZerr.entry(zErr);

                // Repeat comparison just after the first tracker plane 
                if (tkr.SiteList.get(0).m.Layer == 2) {
                    StateVector S = tkr.SiteList.get(0).aS;
                    Vec helixAtLayer1 = S.pivotTransform(TkSaved[iBest].X0);
                    trueErr = helixAtLayer1.dif(TkSaved[iBest].p);
                    Vec helErrs = tkr.SiteList.get(0).aS.helixErrors();
                    if (verbose) {
                        helixAtLayer1.print("reconstructed helix at layer 1");
                        S.origin.print("reconstructed helix origin at layer 1");
                        S.X0.print("reconstructed helix pivot at layer 1");
                        TkSaved[iBest].p.print(String.format("generated helix at layer 1 for iBest=%d", iBest));
                        TkSaved[iBest].X0.print("generated helix pivot at layer 1");
                        TkSaved[iBest].origin.print("generated helix origin at layer 1");
                        for (int i = 0; i < 5; i++) {
                            System.out.format("     Helix parameter %d after smoothing, true error=%10.7f, estimate=%10.7f\n", i, trueErr.v[i],
                                    helErrs.v[i]);
                        }
                    }
                    hEdrhoS1.entry(trueErr.v[0] / helErrs.v[0]);
                    hEphi0S1.entry(trueErr.v[1] / helErrs.v[1]);
                    hEkS1.entry(trueErr.v[2] / helErrs.v[2]);
                    hEdzS1.entry(trueErr.v[3] / helErrs.v[3]);
                    hEtanlS1.entry(trueErr.v[4] / helErrs.v[4]);
                }
            }
        }
        timestamp = Instant.now();
        System.out.format("Ending time = %s\n", timestamp.toString());
        ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        hNtracks.plot(path + "nTracks.gp", true, " ", " ");
        hNhits.plot(path + "nHits.gp", true, " ", " ");
        hTkChi2.plot(path + "tkrChi2.gp", true, " ", " ");
        hEdrhoS.plot(path + "drhoErrorS.gp", true, "gaus", " ");
        hEphi0S.plot(path + "phi0ErrorS.gp", true, "gaus", " ");
        hEkS.plot(path + "kErrorS.gp", true, "gaus", " ");
        hEdzS.plot(path + "dzErrorS.gp", true, "gaus", " ");
        hEtanlS.plot(path + "tanlErrorS.gp", true, "gaus", " ");
        hEdrhoS1.plot(path + "drhoErrorS1.gp", true, "gaus", " ");
        hEphi0S1.plot(path + "phi0ErrorS1.gp", true, "gaus", " ");
        hEkS1.plot(path + "kErrorS1.gp", true, "gaus", " ");
        hEdzS1.plot(path + "dzErrorS1.gp", true, "gaus", " ");
        hEtanlS1.plot(path + "tanlErrorS1.gp", true, "gaus", " ");
        hEdrho.plot(path + "drhoError.gp", true, "gaus", " ");
        hEphi0.plot(path + "phi0Error.gp", true, "gaus", " ");
        hEk.plot(path + "kError.gp", true, "gaus", " ");
        hEdz.plot(path + "dzError.gp", true, "gaus", " ");
        hEtanl.plot(path + "tanlError.gp", true, "gaus", " ");
        hChi2HelixS.plot(path + "chi2helixS.gp", true, " ", " ");
        hXerr.plot(path + "xErr.gp", true, "gaus", " ");
        hYerr.plot(path + "yErr.gp", true, "gaus", " ");
        hZerr.plot(path + "zErr.gp", true, "gaus", " ");
        hMomentum.plot(path+"momentum.gp", true, "gaus", " ");
        hScatProj.plot(path+"scatt.gp",true," "," ");
        for (int layer=2; layer<nLayers; ++layer) {
            hScatXY[layer].plot(path + String.format("ScatXY_%d.gp", layer), true, " ", " ");
            hScatZY[layer].plot(path + String.format("ScatZY_%d.gp", layer), true, " ", " ");
            hResidS0[layer].plot(path + String.format("ResidS0_%d.gp", layer), true, " ", " ");
            hResidS2[layer].plot(path + String.format("ResidS2_%d.gp", layer), true, " ", " ");
        }
    }

}
