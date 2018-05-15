package kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;

//This is for testing only and is not part of the Kalman fitting code
public class HelixTest { // Program for testing the Kalman fitting code

    // Coordinate system:
    // z is the B field direction, downward in lab coordinates
    // y is the beam direction
    // x is y cross z

    public HelixTest(String path) {

        // Units are Tesla, GeV, mm

        int nTrials = 10000; // The number of test events to generate for fitting
        int startModule = 5; // Where to start the Kalman filtering
        int nIteration = 2; // Number of filter iterations
        int numbLayers = 6; // Number of layers to use for the linear fit
        boolean cheat = false; // true to use the true helix parameters (smeared) for the starting guess
        boolean perfect = false;
        double thickness = 0.00000000003; // Silicon thickness in mm
        if (perfect) {
            thickness = 0.0000000000001;
        }
        boolean rungeKutta = true; // Set true to generate the helix by Runge Kutta integration instead of a piecewise helix
        boolean verbose = nTrials < 2;

        // Tracking instrument description
        int nPlanes = 6;
        Vec tInt = new Vec(0., 1., 0.); // Nominal detector plane orientation
        double[] location = { 100., 200., 300., 500., 700., 900. }; // Detector positions in y (origin of detector local system)
        double[] xdet = { 50., 55., 60., 65., 70., 75. }; // x coordinate of origin of the detector local system
        double[] zdet = { 5., -5., 0., 10., 12., 2. }; // z coordinate (along B) of origin of the detector local system
        double delta = 5.0; // Distance between stereo pairs
        double[] stereoAngle = { 0.1, 0.1, 0.1, 0.05, 0.05, 0.05 }; // Angles of the stereo layers in radians
        double resolution = 0.012; // SSD point resolution, in mm

        double[] Q = { 1.0, 1.0, 1.0 }; // charge
        double[] p = { 1.0, 1.1, 0.9 }; // momentum

        int nHelices = 1; // Number of helix tracks to simulate
        Vec helixOrigin = new Vec(0., location[0], 0.); // Pivot point of initial helices
        double Phi = 91. * Math.PI / 180.;
        double Theta = 85. * Math.PI / 180.;
        Vec initialDirection = new Vec(Math.cos(Phi) * Math.sin(Theta), Math.sin(Phi) * Math.sin(Theta), Math.cos(Theta));
        initialDirection.print("initial particle direction");
        double[] drho = new double[nHelices]; // { -0., 0., 1. }; // Helix parameters
        double drhoSigma = 0.3; // 0.2 0.3
        double[] dz = new double[nHelices]; // { 5.0, 1.0, 4.0 };
        double dzSigma = 0.02; // 0.2 0.02
        double[] phi0 = new double[nHelices]; // { 0.0, 0.04, 0.05 };
        double phi0Sigma = 0.01; // 0.0002 0.01
        double[] tanl = new double[nHelices]; // { 0.1, 0.12, 0.13 };
        double tanlSigma = 0.001; // 0.0002, 0.001
        double[] K = new double[nHelices];
        double kError = 1.2; // 0.02 1.2

        double[] thetaR1 = new double[nPlanes];
        double[] phiR1 = new double[nPlanes];
        double[] thetaR2 = new double[nPlanes];
        double[] phiR2 = new double[nPlanes];
        for (int i = 0; i < nPlanes; i++) { // Generate some random misalignment of the detector planes
            double[] gran = gausRan();
            thetaR1[i] = Math.abs(gran[0] * 0.5 * 0.01745);
            phiR1[i] = Math.random() * 2.0 * Math.PI;
            thetaR2[i] = Math.abs(gran[1] * 0.5 * 0.01745);
            phiR2[i] = Math.random() * 2.0 * Math.PI;
        }

        double[] heights = { 100., 100., 100., 100., 100., 100. };
        double[] widths = { 150., 150., 150., 300., 300., 300. };

        String mapType = "binary";
        // String mapFile =
        // "C:\\Users\\Robert\\Desktop\\Kalman\\125acm2_3kg_corrected_unfolded_scaled_0.7992.dat";
        String mapFile = "C:\\Users\\Robert\\Desktop\\Kalman\\fieldmap.bin";
        FieldMap fM = null;
        try {
            fM = new FieldMap(mapFile, mapType, 21.17, 0., 457.2);
        } catch (IOException e) {
            System.out.format("Could not open or read the field map %s\n", mapFile);
            return;
        }
        if (mapType != "binary") {
            fM.writeBinaryFile("C:\\Users\\Robert\\Desktop\\Kalman\\fieldmap.bin");
        }
        Vec Bpivot = fM.getField(helixOrigin);
        Bpivot.print("magnetic field at the initial pivot");
        for (int pln = 0; pln < nPlanes; pln++) {
            Vec bf = fM.getField(new Vec(xdet[pln], location[pln], zdet[pln]));
            System.out.format("B field at plane %d = %10.7f, %10.7f, %10.7f\n", pln, bf.v[0], bf.v[1], bf.v[2]);
        }
        File file = new File(path + "field.gp");
        file.getParentFile().mkdirs();
        PrintWriter printWriter3 = null;
        try {
            printWriter3 = new PrintWriter(file);
        } catch (FileNotFoundException e1) {
            System.out.println("Could not create the gnuplot output file.");
            e1.printStackTrace();
            return;
        }
        // printWriter.format("set xrange [-1900.:100.]\n");
        // printWriter.format("set yrange [-1000.:1000.]\n");
        printWriter3.format("set xlabel 'Y'\n");
        printWriter3.format("set ylabel 'B'\n");
        printWriter3.format("$field1 << EOD\n");
        for (int i = 0; i < 200; i++) {
            double y = ((location[5] - 0.) / 200.) * (double) i;
            Vec bf = fM.getField(new Vec(0., y, 0.));
            printWriter3.format("  %10.6f %10.6f\n", y, bf.v[2]);
        }
        printWriter3.format("EOD\n");
        printWriter3.format("$field2 << EOD\n");
        for (int i = 0; i < 200; i++) {
            double y = ((location[5] - 0.) / 200.) * (double) i;
            Vec bf = fM.getField(new Vec(150., y, 0.));
            printWriter3.format("  %10.6f %10.6f\n", y, bf.v[2]);
        }
        printWriter3.format("EOD\n");
        printWriter3.format("$field3 << EOD\n");
        for (int i = 0; i < 200; i++) {
            double y = ((location[5] - 0.) / 200.) * (double) i;
            Vec bf = fM.getField(new Vec(-150., y, 0.));
            printWriter3.format("  %10.6f %10.6f\n", y, bf.v[2]);
        }
        printWriter3.format("EOD\n");
        printWriter3.format("plot $field1 with lines lw 1, $field2 with lines lw 1, $field3 with lines lw 1\n");
        printWriter3.close();

        Helix[] TkInitial = new Helix[nHelices];
        Vec[] helixMCtrue = new Vec[nHelices];
        for (int i = 0; i < nHelices; i++) {
            Vec momentum = new Vec(p[i] * initialDirection.v[0], p[i] * initialDirection.v[1], p[i] * initialDirection.v[2]);
            momentum.print("initial helix momentum");
            TkInitial[i] = new Helix(Q[i], helixOrigin, momentum, helixOrigin, fM);
            drho[i] = TkInitial[i].p.v[0];
            phi0[i] = TkInitial[i].p.v[1];
            K[i] = TkInitial[i].p.v[2];
            dz[i] = TkInitial[i].p.v[3];
            tanl[i] = TkInitial[i].p.v[4];
            TkInitial[i].print(String.format("Initial helix %d", i));
            double pt = p[i] / Math.sqrt(1.0 + tanl[i] * tanl[i]);
            System.out.format("Momentum p=%10.4f GeV, pt=%10.4f GeV\n", p[i], pt);
            System.out.format("True starting helix %d is %10.6f %10.6f %10.6f %10.6f %10.6f\n", i, drho[i], phi0[i], K[i], dz[i], tanl[i]);
            helixMCtrue[i] = TkInitial[i].p.copy();
        }
        double kSigma = K[0] * kError;

        // Print out a plot of just a simple helix
        file = new File(path + "helix1.gp");
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
        for (int i = 0; i < nHelices; i++) {
            printWriter.format("$runga%d << EOD\n", i);
            RungeKutta4 r4 = new RungeKutta4(Q[i], 1., fM);
            Vec r0 = TkInitial[i].atPhiGlobal(0.);
            Vec p0 = TkInitial[i].getMomGlobal(0.);
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
            printWriter.format("$helix%d << EOD\n", i);

            if (Q[i] < 0.) {
                for (double phi = 0.; phi < 0.02 * Math.PI; phi = phi + 0.001) {
                    Vec r = TkInitial[i].atPhiGlobal(phi);
                    printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                }
            } else {
                for (double phi = 0.; phi > -0.02 * Math.PI; phi = phi - 0.001) {
                    Vec r = TkInitial[i].atPhiGlobal(phi);
                    printWriter.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                }
            }

            printWriter.format("EOD\n");
        }
        printWriter.format("splot ");
        for (int i = 0; i < nHelices; i++) {
            printWriter.format("$runga%d u 1:2:3 with lines lw 3, $helix%d u 1:2:3 with lines lw 3", i, i);
            if (i < nHelices - 1) printWriter.format(",");
        }
        printWriter.format("\n");
        printWriter.close();

        Vec zhat = null;
        Vec uhat = null;
        Vec vhat = null;

        // Test extrapolation of the helix from layer 5 to 6

        Histogram hXe = new Histogram(100, -0.1, 0.002, "X extrapolation", "x", "trial");
        Histogram hZe = new Histogram(100, -0.1, 0.002, "Z extrapolation", "z", "trial");
        Helix tkH = TkInitial[0].copy();
        Plane pl = new Plane(new Vec(xdet[4], location[4], zdet[4]), new Vec(0., 1., 0.));
        Plane pl6 = new Plane(new Vec(xdet[5], location[5], zdet[5]), new Vec(0., 1., 0.));
        double phiInt5 = tkH.planeIntersect(pl);
        Vec x1 = tkH.atPhiGlobal(phiInt5);
        x1.print("point on plane 5");
        Vec ctr = null;
        for (int itr = 0; itr < 10000; itr++) {
            double[] grn = { 0., 0. };
            if (itr != 0) {
                grn = gausRan();
            }
            double x = x1.v[0] + grn[0] * resolution;
            double z = x1.v[2] + grn[1] * resolution;
            Vec pvt = new Vec(x, x1.v[1], z);
            // pvt.print("new pivot");
            Vec hNew = tkH.pivotTransform(pvt);
            // hNew.print("new helix parameters");
            hNew.v[0] = 0.;
            hNew.v[3] = 0.;
            Helix tkHnew = new Helix(hNew, pvt, pl.X(), fM);
            double phiInt6 = tkHnew.planeIntersect(pl6);
            Vec pnt6 = tkHnew.atPhiGlobal(phiInt6);
            // pnt6.print("point at plane 6");
            if (itr == 0) {
                ctr = pnt6.copy();
            }
            hXe.entry(pnt6.v[0] - ctr.v[0]);
            hZe.entry(pnt6.v[2] - ctr.v[2]);
        }
        hXe.plot(path + "Xe.gp", true, " ", " ");
        hZe.plot(path + "Ze.gp", true, " ", " ");

        // Test the multiple scattering matrix
        /*
        Histogram hEdrho3 = new Histogram(100, -10., 0.2, "MS drho error", "sigmas", "track");
        Histogram hEphi03 = new Histogram(100, -10., 0.2, "MS phi0 error", "sigmas", "track");
        Histogram hEk3 = new Histogram(100, -10., 0.2, "MS K error", "sigmas", "track");
        Histogram hEdz3 = new Histogram(100, -10., 0.2, "MS dz error", "sigmas", "track");
        Histogram hEtanl3 = new Histogram(100, -10., 0.2, "MS tanl error", "sigmas", "track");
        Histogram htheta = new Histogram(100, 0., 6e-5, "Actual scattering angle", "radians", "track");
        Histogram hphidif = new Histogram(100, -1., 0.02, "phi of scatter", "radians", "track");
        Histogram hScatProj = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle", "radians", "track");
        Plane pls = new Plane(new Vec(xdet[1], location[1], zdet[1]), new Vec(0., 1., 0.));
        pls.print("for testing the multiple scattering matrix");
        Helix H0 = TkInitial[0].randomScat(pls, 0.);
        H0.print("test helix for the multiple scattering matrix");
        Vec pmom = H0.getMomGlobal(0.);
        Vec t = pmom.unitVec();
        zhat = new Vec(0., 0., 1.);
        uhat = t.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
        vhat = t.cross(uhat);
        RotMatrix R = new RotMatrix(uhat, vhat, t);  
        double ct = Math.abs(pls.T().dot(t));
        double rho = 2.329; // Density of silicon in g/cm^2
        double radLen = (21.82 / rho) * 10.0;
        double sigmaMS = Math.sqrt((thickness / radLen) / ct) * (0.0136 / pmom.mag()) * (1.0 + 0.038 * Math.log((thickness / radLen) / ct));
        System.out.format("Multiple scattering sigma=%12.5f\n", sigmaMS);
        double V = sigmaMS * sigmaMS;
        double[][] q = new double[5][5];
        q[1][1] = V * (1.0 + H0.p.v[4] * H0.p.v[4]);
        q[2][2] = 0.; // V*(a.v[2]*a.v[2]*a.v[4]*a.v[4]); // These commented terms would be relevant
                      // for a scatter halfway in between planes
        q[2][4] = 0.; // V*(a.v[2]*a.v[4]*(1.0+a.v[4]*a.v[4]));
        q[4][2] = 0.; // q[2][4];
        q[4][4] = V * (1.0 + H0.p.v[4] * H0.p.v[4]) * (1.0 + H0.p.v[4] * H0.p.v[4]);
        // All other elements are zero
        SquareMatrix QMS = new SquareMatrix(5, q);
        System.out.format("     Sigma-phi0=%12.5e,  Sigma-tanl=%12.5e\n", Math.sqrt(q[1][1]), Math.sqrt(q[4][4]));
        QMS.print("test MS matrix");
        for (int i = 0; i < 10000; i++) {
            Helix H1 = TkInitial[0].randomScat(pls, thickness);
            Vec t1 = H1.getMomGlobal(0.).unitVec();
            Vec t1Loc = R.rotate(t1);
            double ct12 = t1.dot(t);
            htheta.entry(Math.acos(ct12));
            hScatProj.entry(Math.asin(t1Loc.v[0]));
            double phiScat = Math.atan2(t1Loc.v[1], t1Loc.v[0]);
            hphidif.entry(phiScat / Math.PI);
            hEdrho3.entry(H0.p.v[0] - H1.p.v[0]);
            hEphi03.entry((H0.p.v[1] - H1.p.v[1]) / Math.sqrt(q[1][1]));
            hEk3.entry(H0.p.v[2] - H1.p.v[2]);
            hEdz3.entry(H0.p.v[3] - H1.p.v[3]);
            hEtanl3.entry((H0.p.v[4] - H1.p.v[4]) / Math.sqrt(q[4][4]));
        }
        hEdrho3.plot(path + "drhoErrMS.gp", true, " ", " ");
        hEphi03.plot(path + "phi0ErrMS.gp", true, " ", " ");
        hEk3.plot(path + "kErrMS.gp", true, " ", " ");
        hEdz3.plot(path + "dzErrMS.gp", true, " ", " ");
        hEtanl3.plot(path + "tanlErrMS.gp", true, " ", " ");
        htheta.plot(path + "thetaScat.gp", true, " ", " ");
        hphidif.plot(path + "phiscat.gp", true, " ", " ");
        hScatProj.plot(path + "projScat.gp", true, " ", " ");
        */
        /*
        // Test the seed track fitter using an exact model with no scattering
        // First find the average field
        Vec Bvec = new Vec(0., 0., 0.);
        for (int pln = 0; pln < nPlanes; pln++) {
            Vec thisB = fM.getField(new Vec(xdet[pln], location[pln], zdet[pln]));
            Bvec = Bvec.sum(thisB);
            thisB = fM.getField(new Vec(xdet[pln], location[pln] + delta, zdet[pln]));
            Bvec = Bvec.sum(thisB);
        }
        double sF = 1.0 / ((double) 2 * nPlanes);
        Bvec = Bvec.scale(sF);
        double Bseed = Bvec.mag();
        System.out.format("B field averaged over all layers = %12.5e\n", Bseed);
        Histogram hEdrho2 = new Histogram(100, -10., 0.2, "Seed track drho error", "sigmas", "track");
        Histogram hEphi02 = new Histogram(100, -10., 0.2, "Seed track phi0 error", "sigmas", "track");
        Histogram hEk2 = new Histogram(100, -10., 0.2, "Seed track K error", "sigmas", "track");
        Histogram hEdz2 = new Histogram(100, -10., 0.2, "Seed track dz error", "sigmas", "track");
        Histogram hEtanl2 = new Histogram(100, -10., 0.2, "Seed track tanl error", "sigmas", "track");
        Histogram hEa = new Histogram(100, -10., 0.2, "Seed track error on coefficient a", "sigmas", "track");
        Histogram hEb = new Histogram(100, -10., 0.2, "Seed track error on coefficient b", "sigmas", "track");
        Histogram hEc = new Histogram(100, -10., 0.2, "Seed track error on coefficient c", "sigmas", "track");
        Histogram hEd = new Histogram(100, -10., 0.2, "Seed track error on coefficient d", "sigmas", "track");
        Histogram hEe = new Histogram(100, -10., 0.2, "Seed track error on coefficient e", "sigmas", "track");
        Histogram hCoefChi2 = new Histogram(50, 0., 0.4, "Full chi^2 of linear fit coefficients", "chi^2", "track");
        double c = 2.99793e8;
        double alpha = 1000.0 * 1.0E9 / (c * Bseed);
        // Units are Tesla, mm, GeV
        double Radius = alpha / K[0];
        double xc = (drho[0] + Radius) * Math.cos(phi0[0]);
        double yc = (drho[0] + Radius) * Math.sin(phi0[0]);
        double sgn = -1.0;
        double[] coefs = new double[5];
        System.out.format("True starting helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho[0], phi0[0], K[0], dz[0], tanl[0]);
        coefs[0] = dz[0] - drho[0] * tanl[0] * Math.tan(phi0[0]);
        coefs[1] = tanl[0] / Math.cos(phi0[0]);
        coefs[3] = sgn * yc / Radius;
        coefs[2] = xc + sgn * Radius * (1.0 - 0.5 * coefs[3] * coefs[3]);
        coefs[4] = -sgn / (2.0 * Radius);
        double[] circ = parabolaToCircle(alpha, sgn, new Vec(coefs[2], coefs[3], coefs[4]));
        Vec tmp = new Vec(circ[0], circ[1], circ[2]);
        tmp.print("circle params");
        System.out.format("Helix radius = %10.5f, and the center is at %10.6f, %10.6f, alpha=%12.5e, K=%12.5e, B=%12.5e\n", Radius, xc, yc,
                                        alpha, K[0], Bseed);
        System.out.format("Polynomial approximation coefficients are %10.6f %10.6f %10.6f %10.6f %10.7f\n", coefs[0], coefs[1], coefs[2],
                                        coefs[3], coefs[4]);
        for (int iTrial = 0; iTrial < nTrials; iTrial++) {
            double[] m1 = new double[nPlanes];
            double[] m2 = new double[nPlanes];
            ArrayList<SiModule> SiModules = new ArrayList<SiModule>(2 * nPlanes);
            for (int pln = 0; pln < nPlanes; pln++) {
                Vec rInt1 = new Vec(xdet[pln], location[pln], zdet[pln]);
                Plane pInt1 = new Plane(rInt1, tInt);
                SiModule thisSi = new SiModule(pln, pInt1, 0., widths[pln], heights[pln], thickness, fM);
                SiModules.add(thisSi);
        
                double xTrue = coefs[2] + (coefs[3] + coefs[4] * rInt1.v[1]) * rInt1.v[1];
                double zTrue = coefs[0] + coefs[1] * rInt1.v[1];
                Vec rTrue = new Vec(xTrue, rInt1.v[1], zTrue);
                double[] gran = gausRan();
                m1[pln] = thisSi.toLocal(rTrue).v[1] + resolution * gran[0];
                thisSi.hits.add(new Measurement(m1[pln], resolution, rTrue, m1[pln]));
        
                Vec rInt2 = new Vec(xdet[pln], location[pln] + delta, zdet[pln]);
                Plane pInt2 = new Plane(rInt2, tInt);
                thisSi = new SiModule(pln, pInt2, stereoAngle[pln], widths[pln], heights[pln], thickness, fM);
                SiModules.add(thisSi);
                xTrue = coefs[2] + (coefs[3] + coefs[4] * rInt2.v[1]) * rInt2.v[1];
                zTrue = coefs[0] + coefs[1] * rInt2.v[1];
                rTrue = new Vec(xTrue, rInt2.v[1], zTrue);
                m2[pln] = thisSi.toLocal(rTrue).v[1] + resolution * gran[1];
        
                thisSi.hits.add(new Measurement(m2[pln], resolution, rTrue, m2[pln]));
            }
            if (nTrials == 1) {
                for (SiModule mm : SiModules) {
                    mm.print(String.format(" polynomial approximation %d", SiModules.indexOf(mm)));
                }
            }
            SeedTrack seed = new SeedTrack(SiModules, 0., 0, 12, verbose);
            if (!seed.success) {
                continue;
            }
            if (nTrials == 1) {
                seed.print("helix parameters");
                System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho[0], phi0[0], K[0], dz[0], tanl[0]);
                seed.solution().print("polynomial solution from fit");
                // System.out.format("True polynomial coefficients are %10.4ef %10.4e %10.4e %10.4e %10.4e\n", coefs[0], coefs[1], coefs[2],
                // coefs[3], coefs[4]);
                seed.solutionCovariance().print("covariance of polynomial fit");
            }
            Vec initialHelix = seed.helixParams();
            Vec seedErrors = seed.errors();
            hEdrho2.entry((initialHelix.v[0] - drho[0]) / seedErrors.v[0]);
            hEphi02.entry((initialHelix.v[1] - phi0[0]) / seedErrors.v[1]);
            hEk2.entry((initialHelix.v[2] - K[0]) / seedErrors.v[2]);
            hEdz2.entry((initialHelix.v[3] - dz[0]) / seedErrors.v[3]);
            hEtanl2.entry((initialHelix.v[4] - tanl[0]) / seedErrors.v[4]);
            Vec fittedCoefs = seed.solution();
            Vec coefErrors = seed.solutionErrors();
            hEa.entry((fittedCoefs.v[0] - coefs[0]) / coefErrors.v[0]);
            hEb.entry((fittedCoefs.v[1] - coefs[1]) / coefErrors.v[1]);
            hEc.entry((fittedCoefs.v[2] - coefs[2]) / coefErrors.v[2]);
            hEd.entry((fittedCoefs.v[3] - coefs[3]) / coefErrors.v[3]);
            hEe.entry((fittedCoefs.v[4] - coefs[4]) / coefErrors.v[4]);
            Vec trueError = fittedCoefs.dif(new Vec(coefs[0], coefs[1], coefs[2], coefs[3], coefs[4]));
            double coefChi2 = trueError.dot(trueError.leftMultiply(seed.solutionCovariance().invert()));
            hCoefChi2.entry(coefChi2);
        }
        hEdrho2.plot(path + "drhoErrSeed.gp", true, " ", " ");
        hEphi02.plot(path + "phi0ErrSeed.gp", true, " ", " ");
        hEk2.plot(path + "kErrSeed.gp", true, " ", " ");
        hEdz2.plot(path + "dzErrSeed.gp", true, " ", " ");
        hEtanl2.plot(path + "tanlErrSeed.gp", true, " ", " ");
        hEa.plot(path + "aError.gp", true, " ", " ");
        hEb.plot(path + "bError.gp", true, " ", " ");
        hEc.plot(path + "cError.gp", true, " ", " ");
        hEd.plot(path + "dError.gp", true, " ", " ");
        hEe.plot(path + "eError.gp", true, " ", " ");
        hCoefChi2.plot(path + "coefChi2.gp", true, " ", " ");
        */
        PrintWriter printWriter2 = null;
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

        Histogram hps1 = new Histogram(100, -1., 0.02, "phi of scatter, non-stereo layer", "radians", "track");
        Histogram hsp1theta = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle, non-stereo layer", "pi radians", "track");
        Histogram hps2 = new Histogram(100, -1., 0.02, "phi of scatter, stereo layer", "radians", "track");
        Histogram hsp2theta = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle, stereo layer", "pi radians", "track");
        Histogram hChi2 = new Histogram(80, 0., .5, "Helix fit chi^2 after smoothing", "chi^2", "tracks");
        Histogram hChi2f = new Histogram(80, 0., .5, "Helix fit chi^2 after filtering", "chi^2", "tracks");
        Histogram hChi2HelixS = new Histogram(80, 0., 0.4, "smoothed chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hChi2Helix = new Histogram(80, 0., 0.4, "filtered chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hChi2Guess = new Histogram(80, 0., 0.4, "chi^2 of guess helix parameters", "chi^2", "tracks");
        Histogram hRes = new Histogram(100, -.5, 0.01, "detector resolution", "mm", "hits");
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
        Histogram hResid0 = new Histogram(100, -10., 0.2, "Filtered residual for non-rotated planes", "sigmas", "hits");
        Histogram hResid1 = new Histogram(100, -10., 0.2, "Filtered residual for rotated planes", "sigmas", "hits");
        Histogram hEdrhoG = new Histogram(100, -10., 0.2, "Helix guess drho error", "sigmas", "track");
        Histogram hEphi0G = new Histogram(100, -10., 0.2, "Helix guess phi0 error", "sigmas", "track");
        Histogram hEkG = new Histogram(100, -10., 0.2, "Helix guess K error", "sigmas", "track");
        Histogram hEdzG = new Histogram(100, -10., 0.2, "Helix guess dz error", "sigmas", "track");
        Histogram hEtanlG = new Histogram(100, -25., 0.5, "Helix guess tanl error", "sigmas", "track");
        Histogram hEdrhoG1 = new Histogram(100, -2., 0.04, "Helix guess drho error", "mm", "track");
        Histogram hEphi0G1 = new Histogram(100, -0.05, 0.001, "Helix guess phi0 error", "radians", "track");
        Histogram hEkG1 = new Histogram(100, -10., 0.2, "Helix guess K error", "1/GeV", "track");
        Histogram hEdzG1 = new Histogram(100, -0.25, 0.005, "Helix guess dz error", "mm", "track");
        Histogram hEtanlG1 = new Histogram(100, -0.005, 0.0001, "Helix guess tanl error", "tan(lambda) error", "track");
        Histogram[] hResidS0 = new Histogram[6];
        Histogram[] hResidS1 = new Histogram[6];
        Histogram[] hResidS2 = new Histogram[6];
        Histogram[] hResidS3 = new Histogram[6];
        Histogram[] hResidS4 = new Histogram[6];
        Histogram[] hResidS5 = new Histogram[6];
        for (int i = 0; i < nPlanes; i++) {
            hResidS0[i] = new Histogram(100, -10., 0.2, String.format("Smoothed fit residual for non-rotated plane %d", i), "sigmas", "hits");
            hResidS1[i] = new Histogram(100, -10., 0.2, String.format("Smoothed fit residual for rotated plane %d", i), "sigmas", "hits");
            hResidS2[i] = new Histogram(100, -0.1, 0.002, String.format("Smoothed fit residual for non-rotated plane %d", i), "mm", "hits");
            hResidS3[i] = new Histogram(100, -0.1, 0.002, String.format("Smoothed fit residual for rotated plane %d", i), "mm", "hits");
            hResidS4[i] = new Histogram(100, -0.1, 0.002, String.format("Smoothed true residual for non-rotated plane %d", i), "mm", "hits");
            hResidS5[i] = new Histogram(100, -0.1, 0.002, String.format("Smoothed true residual for rotated planes %d", i), "mm", "hits");
        }

        Instant timestamp = Instant.now();
        System.out.format("Beginning time = %s\n", timestamp.toString());
        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(),
                                        ldt.getSecond(), ldt.getNano());

        Vec[] helixSaved = new Vec[2 * nPlanes];
        Helix helixBegin = TkInitial[0].copy();
        helixBegin.print("helixBegin");
        TkInitial[0].print("TkInitial");
        Helix TkEnd = null;
        for (int iTrial = 0; iTrial < nTrials; iTrial++) {
            double[] m1 = new double[nPlanes];
            double[] m2 = new double[nPlanes];

            Helix[] Tk = new Helix[nHelices];
            for (int i = 0; i < nHelices; i++) {
                Tk[i] = TkInitial[i].copy();
                if (verbose) {
                    Tk[i].print("copied initial helix");
                }
            }

            // Make an array of Si detector planes
            ArrayList<SiModule> SiModules = new ArrayList<SiModule>(2 * nPlanes);
            for (int pln = 0; pln < nPlanes; pln++) {
                Vec rInt1 = new Vec(xdet[pln], location[pln], zdet[pln]);
                if (verbose) {
                    rInt1.print("  Plane first layer location=");
                }

                // Randomly tilt the measurement planes to mimic misalignment
                RotMatrix Rt = new RotMatrix(phiR1[pln], thetaR1[pln], -phiR1[pln]);
                Plane pInt1 = new Plane(rInt1, Rt.rotate(tInt));
                SiModule newModule1 = new SiModule(pln, pInt1, 0., widths[pln], heights[pln], thickness, fM);
                SiModules.add(newModule1);
                // newModule1.R.multiply(newModule1.Rinv).print("unit matrix 1?");

                Vec rInt2 = new Vec(xdet[pln], location[pln] + delta, zdet[pln]);

                RotMatrix Rt2 = new RotMatrix(phiR2[pln], thetaR2[pln], -phiR2[pln]);
                Plane pInt2 = new Plane(rInt2, Rt2.rotate(tInt));
                SiModule newModule2 = new SiModule(pln, pInt2, stereoAngle[pln], widths[pln], heights[pln], thickness, fM);
                SiModules.add(newModule2);
                // newModule2.R.multiply(newModule2.Rinv).print("unit matrix 2?");
            }

            // Populate the Si detector planes with hits from helices scattered at each plane
            for (int ih = 0; ih < nHelices; ih++) {
                HelixPlaneIntersect hpi = new HelixPlaneIntersect();
                if (verbose) {
                    printWriter2.format("$helix%d << EOD\n", ih);
                }
                for (int pln = 0; pln < nPlanes; pln++) {
                    if (verbose) {
                        System.out.format("Extrapolating to plane #%d\n", pln);
                        Tk[ih].print("this plane");
                    }
                    SiModule thisSi = SiModules.get(2 * pln);
                    double phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt)) break;
                    if (verbose) {
                        System.out.format("Plane %d, phiInt1= %12.10f\n", pln, phiInt);
                    }
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
                        for (double phi = 0.; phi < Math.abs(phiInt); phi = phi + dPhi) {
                            Vec r = Tk[ih].atPhiGlobal(-Q[ih] * phi);
                            printWriter2.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
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
                        thisSi.p.print("first layer");
                        // thisSi.Rinv.print("SiModule Rinv");
                        rscat.print("       Gobal intersection point 1");
                        rDet.print("       helix intersection in detector frame");
                    }
                    double[] gran = new double[2];
                    if (perfect) {
                        gran[0] = 0.;
                        gran[1] = 0.;
                    } else {
                        gran = gausRan();
                    }
                    m1[pln] = rDet.v[1] + resolution * gran[0];
                    hRes.entry(resolution * gran[0]);
                    if (verbose) {
                        System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1[pln], rDet.v[1]);
                    }
                    Measurement thisM1 = new Measurement(m1[pln], resolution, rscat, rDet.v[1]);
                    thisSi.addMeasurement(thisM1);

                    Vec t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                    if (verbose) {
                        Tk[ih].getMom(phiInt).print("helix local momentum before scatter");
                        Tk[ih].getMomGlobal(phiInt).print("helix global momentum before scatter");
                    }
                    zhat = new Vec(0., 0., 1.);
                    uhat = t1.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
                    vhat = t1.cross(uhat);
                    RotMatrix Rtmp = new RotMatrix(uhat, vhat, t1);
                    Tk[ih] = Tk[ih].randomScat(thisSi.p, rscat, pInt, thisSi.thickness);
                    helixSaved[2 * pln] = Tk[ih].p.copy();
                    Vec t2 = Tk[ih].getMomGlobal(0.).unitVec();
                    if (verbose) {
                        Tk[ih].print("scattered from the first layer of the detector plane");
                        Vec p2 = Tk[ih].getMomGlobal(0.);
                        p2.print("momentum after scatter");
                        double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                        System.out.format("Scattering angle from 1st layer of thickness %10.5f = %10.7f; p=%10.7f\n", thickness, scattAng, p2.mag());
                    }
                    Vec t2Loc = Rtmp.rotate(t2);
                    hsp1theta.entry(Math.asin(t2Loc.v[1]));
                    double phiScat = Math.atan2(t2Loc.v[1], t2Loc.v[0]);
                    hps1.entry(phiScat / Math.PI);

                    // Now for the stereo layer
                    thisSi = SiModules.get(2 * pln + 1);
                    phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt)) break;
                    if (verbose) {
                        System.out.format("Plane %d, phiInt2= %f\n", pln, phiInt);
                        double dPhi = (phiInt) / 5.0;
                        for (double phi = 0.; phi < phiInt; phi = phi + dPhi) {
                            Vec r = Tk[ih].atPhiGlobal(phi);
                            printWriter2.format(" %10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                        }
                    }
                    if (rungeKutta) {
                        rscat = hpi.rkIntersect(thisSi.p, Tk[ih].atPhiGlobal(0.), Tk[ih].getMomGlobal(0.), Q[ih], fM, pInt);
                    } else {
                        rscat = Tk[ih].atPhiGlobal(phiInt);
                        pInt = Tk[ih].getMomGlobal(phiInt);
                    }
                    // check = (rscat.dif(thisSi.p.X()).dot(thisSi.p.T()));
                    // System.out.format("Dot product of vector in plane with plane
                    // direction=%12.8e, should be zero\n", check);
                    if (verbose) {
                        thisSi.p.print("Second layer");
                        rscat.print("       Global intersection point 2");
                        if (rungeKutta) {
                            Vec rIntTmp = Tk[ih].atPhiGlobal(phiInt);
                            double errX = rscat.dif(rIntTmp).mag();
                            System.out.format("Runge-Kutta difference from Helix extrapolation is %12.5e mm for plane %d stereo\n", errX, pln);
                        }
                    }

                    Vec rscatRot = thisSi.toLocal(rscat);
                    if (verbose) {
                        rscatRot.print("       helix intersection in detector frame");
                    }
                    m2[pln] = rscatRot.v[1] + resolution * gran[1];
                    hRes.entry(resolution * gran[1]);
                    if (verbose) System.out.format("       Measurement 2= %10.7f, Truth=%10.7f\n", m2[pln], rscatRot.v[1]);
                    Measurement thisM2 = new Measurement(m2[pln], resolution, rscat, rscatRot.v[1]);
                    thisSi.addMeasurement(thisM2);
                    if (pln != nPlanes - 1) {
                        t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                        uhat = t1.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
                        vhat = t1.cross(uhat);
                        Rtmp = new RotMatrix(uhat, vhat, t1);
                        Tk[ih] = Tk[ih].randomScat(thisSi.p, rscat, pInt, thisSi.thickness);
                        t2 = Tk[ih].getMomGlobal(0.).unitVec();
                        if (verbose) {
                            Tk[ih].print("scattered from the second layer of the measurement plane");
                            double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                            System.out.format("Scattering angle from 2nd layer=%10.7f\n", scattAng);
                        }
                        t2Loc = Rtmp.rotate(t2);
                        hsp2theta.entry(Math.asin(t2Loc.v[1]));
                        phiScat = Math.atan2(t2Loc.v[1], t2Loc.v[0]);
                        hps2.entry(phiScat / Math.PI);
                    } else {
                        TkEnd = Tk[ih];
                    }
                    helixSaved[2 * pln + 1] = Tk[ih].p.copy();
                }
                if (verbose) printWriter2.format("EOD\n");
            }

            if (verbose) {
                printWriter2.format("$pnts << EOD\n");
                System.out.format("\n\n ******* Printing out the list of Si modules: ********\n");
                for (SiModule si : SiModules) {
                    si.print("in list");
                    Iterator<Measurement> itr = si.hits.iterator();
                    while (itr.hasNext()) {
                        Measurement mm = itr.next();
                        Vec rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                        Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                    printWriter2.format("EOD\n");
                    printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                    for (int ih = 0; ih < nHelices; ih++) {
                        printWriter2.format(", $helix%d u 1:2:3 with lines lw 3", ih);
                    }
                    printWriter2.format("\n");
                    printWriter2.close();
                }
            }

            // Create a seed track from the first 3 or 4 layers
            int frstLyr = Math.max(startModule - numbLayers + 1, 0);
            ArrayList<int[]> hitList = new ArrayList<int[]>();
            for (int i = 0; i < numbLayers; i++) {
                int[] ht = new int[2];
                ht[0] = frstLyr + i;
                ht[1] = 0;
                hitList.add(ht);
            }
            SeedTrack seed = new SeedTrack(SiModules, location[frstLyr / 2], hitList, verbose);
            if (!seed.success) {
                System.out.format("Failed to make a seed track\n");
                continue;
            }
            if (verbose) {
                seed.print("helix parameters");
                System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho[0], phi0[0], K[0], dz[0], tanl[0]);
            }
            Vec initialHelixGuess = seed.helixParams();
            SquareMatrix initialCovariance = seed.covariance();
            Vec GuessErrors = seed.errors();

            // For comparison, find the true helix in the B field frame at the first layer of the linear fit

            Vec gErrVec = initialHelixGuess.dif(helixMCtrue[0]);
            // new Vec(initialHelixGuess.v[0] - drho[0], initialHelixGuess.v[1] - phi0[0], initialHelixGuess.v[2] - K[0],
            // initialHelixGuess.v[3] - dz[0], initialHelixGuess.v[4] - tanl[0]);
            double[] gErr = new double[5];
            for (int i = 0; i < 5; i++) {
                gErr[i] = gErrVec.v[i] / GuessErrors.v[i];
            }
            if (verbose) {
                System.out.format("Comparing linear helix fit with true at layer %d\n", frstLyr);
                System.out.format("Guess drho=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[0], helixMCtrue[0].v[0],
                                                GuessErrors.v[0], gErr[0]);
                System.out.format("Guess phi0=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[1], helixMCtrue[0].v[1],
                                                GuessErrors.v[1], gErr[1]);
                System.out.format("Guess K=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[2], helixMCtrue[0].v[2],
                                                GuessErrors.v[2], gErr[2]);
                System.out.format("Guess dz=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[3], helixMCtrue[0].v[3],
                                                GuessErrors.v[3], gErr[3]);
                System.out.format("Guess tanl=%12.5e, true=%12.5e, uncertainty=%12.5e, sigmas=%12.5e\n", initialHelixGuess.v[4], helixMCtrue[0].v[4],
                                                GuessErrors.v[4], gErr[4]);
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

            //double Bstart = seed.B();
            //Vec tBstart = new Vec(0., 0., 1.);

            // Cheating initial "guess" for the helix

            double[] rn = new double[2];
            if (perfect) {
                rn[0] = 0.;
                rn[1] = 0.;
            } else {
                rn = gausRan();
            }
            double drhoGuess = drho[0] + drhoSigma * rn[0];
            double dzGuess = dz[0] + dzSigma * rn[1];
            if (!perfect) {
                rn = gausRan();
            }
            double phi0Guess = phi0[0] + phi0Sigma * rn[0];
            double tanlGuess = tanl[0] + tanlSigma * rn[1];
            if (!perfect) {
                rn = gausRan();
            }
            double kGuess = K[0] + kSigma * rn[0];

            if (cheat) {
                initialHelixGuess = new Vec(drhoGuess, phi0Guess, kGuess, dzGuess, tanlGuess);
                initialCovariance = new SquareMatrix(5);
                initialCovariance.M[0][0] = (drhoSigma * drhoSigma);
                initialCovariance.M[1][1] = (phi0Sigma * phi0Sigma);
                initialCovariance.M[2][2] = (kSigma * kSigma);
                initialCovariance.M[3][3] = (dzSigma * dzSigma);
                initialCovariance.M[4][4] = (tanlSigma * tanlSigma);

                Vec Bf0 = fM.getField(helixOrigin);
                if (verbose) {
                    initialHelixGuess.print("initial helix guess");
                    System.out.format("True helix: %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho[0], phi0[0], K[0], dz[0], tanl[0]);
                    helixOrigin.print("initial pivot guess");
                    Bf0.print("B field at pivot");
                }
                //Bstart = Bf0.mag();
                //tBstart = Bf0.unitVec();
            }

            initialCovariance.scale(1000.); // Blow up the errors on the initial guess

            if (verbose) {
                initialCovariance.print("initial covariance guess");
            }
            // Run the Kalman fit
            KalmanTrackFit2 kF = new KalmanTrackFit2(SiModules, startModule, nIteration, new Vec(0., location[frstLyr / 2], 0.), initialHelixGuess,
                                            initialCovariance, fM, verbose);
            if (!kF.success) {
                continue;
            }

            ArrayList<MeasurementSite> sites = kF.sites;
            Iterator<MeasurementSite> itr = sites.iterator();
            while (itr.hasNext()) {
                MeasurementSite site = itr.next();
                SiModule siM = site.m;
                if (site.m.Layer >= 0) {
                    if (site.m.stereo == 0.) {
                        if (site.filtered) hResid0.entry(site.aF.r / Math.sqrt(site.aF.R));
                        if (site.smoothed) {
                            hResidS0[siM.Layer].entry(site.aS.r / Math.sqrt(site.aS.R));
                            hResidS2[siM.Layer].entry(site.aS.r);
                            if (site.hitID >= 0) {
                                hResidS4[siM.Layer].entry(site.aS.mPred - site.m.hits.get(site.hitID).vTrue);
                            }
                        }
                    } else {
                        if (site.filtered) hResid1.entry(site.aF.r / Math.sqrt(site.aF.R));
                        if (site.smoothed) {
                            hResidS1[siM.Layer].entry(site.aS.r / Math.sqrt(site.aS.R));
                            hResidS3[siM.Layer].entry(site.aS.r);
                            if (site.hitID >= 0) {
                                hResidS5[siM.Layer].entry(site.aS.mPred - site.m.hits.get(site.hitID).vTrue);
                            }
                        }
                    }
                }
            }

            hChi2.entry(kF.chi2s);
            hChi2f.entry(kF.chi2f);

            if (verbose) {
                System.out.format("HelixTest: initial-site=%d, final-site=%d, # sites=%d\n", kF.initialSite, kF.finalSite, kF.sites.size());
            }
            if (kF.sites.size() > 0) {
                if (kF.sites.get(kF.initialSite).aS != null && kF.sites.get(kF.finalSite).aS != null) {
                    if (verbose) {
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
                    aF = kF.fittedStateBegin().rotateHelix(aF, Rcombo, fRot);
                    if (verbose) {
                        aF.print("final smoothed helix parameters at the track beginning");
                        newPivot.print("final smoothed helix pivot in local coordinates");
                    }
                    Vec aFe = new Vec(5); // .fittedStateBegin().helixErrors(aF);
                    SquareMatrix aFC = kF.fittedStateBegin().covariancePivotTransform(aF);
                    aFC = aFC.similarity(fRot);
                    for (int i = 0; i < 5; i++) {
                        aFe.v[i] = Math.sqrt(Math.max(0., aFC.M[i][i]));
                    }
                    if (verbose) {
                        aFe.print("error estimates on the smoothed helix parameters");
                        // aFC.print("helix parameters covariance");
                        helixMCtrue[0].print("MC true helix at the track beginning");
                    }
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
                    for (int i = 0; i < 5; i++) {
                        eFe.v[i] = Math.sqrt(Math.max(0., eFc.M[i][i]));
                    }
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
                    if (verbose) System.out.format("Full chi^2 of the filtered helix parameters = %12.4e\n", helixChi2);
                    hChi2Helix.entry(helixChi2);
                }
            }
        }

        timestamp = Instant.now();
        System.out.format("Ending time = %s\n", timestamp.toString());
        ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(),
                                        ldt.getSecond(), ldt.getNano());

        hps1.plot(path + "phiScat1.gp", true, " ", " ");
        hsp1theta.plot(path + "projScat1.gp", true, " ", " ");
        hps2.plot(path + "phiScat2.gp", true, " ", " ");
        hsp2theta.plot(path + "projScat2.gp", true, " ", " ");
        hChi2.plot(path + "chi2s.gp", true, " ", " ");
        hChi2f.plot(path + "chi2f.gp", true, " ", " ");
        hChi2HelixS.plot(path + "chi2helixS.gp", true, " ", " ");
        hChi2Helix.plot(path + "chi2helixF.gp", true, " ", " ");
        hChi2Guess.plot(path + "chi2HelixGuess.gp", true, " ", " ");
        hRes.plot(path + "resolution.gp", true, " ", " ");
        hEdrho.plot(path + "drhoError.gp", true, " ", " ");
        hEphi0.plot(path + "phi0Error.gp", true, " ", " ");
        hEk.plot(path + "kError.gp", true, " ", " ");
        hEdz.plot(path + "dzError.gp", true, " ", " ");
        hEtanl.plot(path + "tanlError.gp", true, " ", " ");
        hEdrhoS.plot(path + "drhoErrorS.gp", true, " ", " ");
        hEphi0S.plot(path + "phi0ErrorS.gp", true, " ", " ");
        hEkS.plot(path + "kErrorS.gp", true, " ", " ");
        hEdzS.plot(path + "dzErrorS.gp", true, " ", " ");
        hEtanlS.plot(path + "tanlErrorS.gp", true, " ", " ");
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
        for (int i = 0; i < nPlanes; i++) {
            hResidS0[i].plot(path + String.format("residS0_%d.gp", i), true, " ", " ");
            hResidS1[i].plot(path + String.format("residS1_%d.gp", i), true, " ", " ");
            hResidS2[i].plot(path + String.format("residS2_%d.gp", i), true, " ", " ");
            hResidS3[i].plot(path + String.format("residS3_%d.gp", i), true, " ", " ");
            hResidS4[i].plot(path + String.format("residS4_%d.gp", i), true, " ", " ");
            hResidS5[i].plot(path + String.format("residS5_%d.gp", i), true, " ", " ");
        }
    }

    static double[] gausRan() { // Return two gaussian random numbers

        double x1, x2, w;
        double[] gran = new double[2];
        do {
            x1 = 2.0 * Math.random() - 1.0;
            x2 = 2.0 * Math.random() - 1.0;
            w = x1 * x1 + x2 * x2;
        } while (w >= 1.0);
        w = Math.sqrt((-2.0 * Math.log(w)) / w);
        gran[0] = x1 * w;
        gran[1] = x2 * w;

        return gran;
    }

    static double[] parabolaToCircle(double alpha, double sgn, Vec coef) {
        double R = -sgn / (2.0 * coef.v[2]);
        double yc = sgn * R * coef.v[1];
        double xc = coef.v[0] - sgn * R * (1.0 - 0.5 * coef.v[1] * coef.v[1]);
        double[] r = new double[3];
        r[1] = Math.atan2(yc, xc);
        if (R < 0.) r[1] += Math.PI;
        r[2] = alpha / R;
        r[0] = xc / Math.cos(r[1]) - R;
        return r;
    }

}
