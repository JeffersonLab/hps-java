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

public class HelixTest { // Main program for testing the Kalman fitting code

    public static void main(String[] args) {

        String defaultPath = "C:\\Users\\Robert\\Desktop\\Kalman\\";
        String path; // Path to where the output histograms should be written
        if (args.length == 0)
            path = defaultPath;
        else
            path = args[0];

        // Units are Tesla, GeV, mm

        int nTrials = 10000; // The number of test events to generate for fitting
        boolean verbose = nTrials < 2;

        double[] Q = { -1.0, 1.0, 1.0 }; // charge
        double[] p = { 1.0, 1.1, 0.9 }; // momentum

        int nHelices = 1; // Number of helix tracks to simulate
        Vec helixOrigin = new Vec(0., 90., 0.); // Pivot point of initial helices
        double[] drho = { -2., 0., 1. }; // Helix parameters
        double drhoSigma = 0.2;
        double[] dz = { 5.0, 1.0, 4.0 };
        double dzSigma = 0.2;
        double[] phi0 = { 0.03, 0.04, 0.05 };
        double phi0Sigma = 0.0002;
        double[] tanl = { 0.1, 0.12, 0.13 };
        double tanlSigma = 0.0002;

        double[] K = new double[nHelices];
        Vec[] helixMCtrue = new Vec[nHelices];
        for (int i = 0; i < nHelices; i++) {
            double pt = p[i] / Math.sqrt(1.0 + tanl[i] * tanl[i]);
            System.out.format("Momentum p=%10.4f GeV, pt=%10.4f GeV\n", p[i], pt);
            K[i] = Q[i] / pt;
            System.out.format("True starting helix %d is %10.6f %10.6f %10.6f %10.6f %10.6f\n", i, drho[i], phi0[i], K[i], dz[i], tanl[i]);
            double[] param = { drho[i], phi0[i], K[i], dz[i], tanl[i] };
            helixMCtrue[i] = new Vec(5, param);
        }
        double kSigma = K[0] * 0.02;

        // Tracking instrument description
        int nPlanes = 6;
        Vec tInt = new Vec(0., 1., 0.); // Nominal detector plane orientation
        double[] location = { 100., 200., 300., 500., 700., 900. }; // Detector positions in y
        double thickness = 0.00000000003; // Silicon thickness in mm
        double delta = 5.0; // Distance between stereo pairs
        double[] stereoAngle = { 0.1, 0.1, 0.1, 0.05, 0.05, 0.05 }; // Angles of the stereo layers in radians
        double resolution = 0.012; // SSD point resolution, in mm

        double[] thetaR1 = new double[nPlanes];
        double[] phiR1 = new double[nPlanes];
        double[] thetaR2 = new double[nPlanes];
        double[] phiR2 = new double[nPlanes];
        for (int i = 0; i < nPlanes; i++) { // Generate some random misalignment of the detector planes
            double[] gran = gausRan();
            thetaR1[i] = 0.; // Math.abs(gran[0]*0.087);
            phiR1[i] = 0.; // Math.random()*2.0*Math.PI;
            thetaR2[i] = 0.; // Math.abs(gran[1]*0.087);
            phiR2[i] = 0.; // Math.random()*2.0*Math.PI;
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
        if (mapType != "binary")
            fM.writeBinaryFile("C:\\Users\\Robert\\Desktop\\Kalman\\fieldmap.bin");
        Vec Bpivot = fM.getField(helixOrigin);
        Bpivot.print("magnetic field at the initial pivot");
        for (int pln = 0; pln < nPlanes; pln++) {
            Vec bf = fM.getField(new Vec(0., location[pln], 0.));
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
        for (int i = 0; i < nHelices; i++) {
            TkInitial[i] = new Helix(helixMCtrue[i], helixOrigin, helixOrigin, fM);
            TkInitial[i].print(String.format("Initial helix %d", i));
        }

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
            if (i < nHelices - 1)
                printWriter.format(",");
        }
        printWriter.format("\n");
        printWriter.close();

        // Test the multiple scattering matrix

        Histogram hEdrho3 = new Histogram(100, -10., 0.2, "MS drho error", "sigmas", "track");
        Histogram hEphi03 = new Histogram(100, -10., 0.2, "MS phi0 error", "sigmas", "track");
        Histogram hEk3 = new Histogram(100, -10., 0.2, "MS K error", "sigmas", "track");
        Histogram hEdz3 = new Histogram(100, -10., 0.2, "MS dz error", "sigmas", "track");
        Histogram hEtanl3 = new Histogram(100, -10., 0.2, "MS tanl error", "sigmas", "track");
        Histogram htheta = new Histogram(100, 0., 6e-5, "Actual scattering angle", "radians", "track");
        Histogram hphidif = new Histogram(100, -1., 0.02, "phi of scatter", "radians", "track");
        Histogram hScatProj = new Histogram(100, -3.0e-3, 6.e-5, "projected scattering angle", "radians", "track");
        Plane pls = new Plane(new Vec(0., location[1], 0.), new Vec(0., 1., 0.));
        pls.print("for testing the multiple scattering matrix");
        Helix H0 = TkInitial[0].randomScat(pls, 0.);
        H0.print("test helix for the multiple scattering matrix");
        Vec pmom = H0.getMomGlobal(0.);
        Vec t = pmom.unitVec();
        Vec zhat = new Vec(0., 0., 1.);
        Vec uhat = t.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
        Vec vhat = t.cross(uhat);
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

        /*       
        // Test the seed track fitter using an exact model with no scattering
        double Bseed = 1.0;
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
        coefs[0] = dz[0] - drho[0] * tanl[0] * Math.tan(phi0[0]);
        coefs[1] = tanl[0] / Math.cos(phi0[0]);
        coefs[3] = sgn * yc / Radius;
        coefs[2] = xc + sgn * Radius * (1.0 - 0.5 * coefs[3] * coefs[3]);
        coefs[4] = -sgn / (2.0 * Radius);
        double[] circ = parabolaToCircle(alpha, sgn, new Vec(coefs[2], coefs[3], coefs[4]));
        Vec tmp = new Vec(circ[0], circ[1], circ[2]);
        tmp.print("circle params");
        System.out.format("Helix radius = %10.5f, and the center is at %10.6f, %10.6f\n", Radius, xc, yc);
        System.out.format("Polynomial approximation coefficients are %10.6f %10.6f %10.6f %10.6f %10.7f\n", coefs[0], coefs[1], coefs[2],
                                        coefs[3], coefs[4]);
        for (int iTrial = 0; iTrial < nTrials; iTrial++) {
            double[] m1 = new double[nPlanes];
            double[] m2 = new double[nPlanes];
            ArrayList<SiModule> SiModules = new ArrayList<SiModule>(2 * nPlanes);
            for (int pln = 0; pln < nPlanes; pln++) {
                Vec rInt1 = new Vec(0., location[pln], 0.);
                Plane pInt1 = new Plane(rInt1, tInt);
                SiModule thisSi = new SiModule(pln, pInt1, 0., widths[pln], heights[pln], thickness, fM);
                SiModules.add(thisSi);
        
                double xTrue = coefs[2] + (coefs[3] + coefs[4] * rInt1.v[1]) * rInt1.v[1];
                double zTrue = coefs[0] + coefs[1] * rInt1.v[1];
                Vec rTrue = new Vec(xTrue, rInt1.v[1], zTrue);
                double[] gran = gausRan();
                m1[pln] = -zTrue + resolution * gran[0];
        
                thisSi.hits.add(new Measurement(m1[pln], resolution, rTrue, m1[pln]));
        
                Vec rInt2 = new Vec(0., location[pln] + delta, 0.);
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
            SeedTrack seed = new SeedTrack(SiModules, 0, 12, verbose);
            if (!seed.success)
                continue;
            if (nTrials == 1) {
                seed.print("helix parameters");
                System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho, phi0, K, dz, tanl);
                seed.solution().print("polynomial solution from fit");
                System.out.format("True polynomial coefficients are %10.6f %10.6f %10.6f %10.6f %10.7f\n", coefs[0], coefs[1], coefs[2],
                                                coefs[3], coefs[4]);
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
        Histogram hResidS0 = new Histogram(100, -10., 0.2, "Smoothed residual for non-rotated planes", "sigmas", "hits");
        Histogram[] hResidS1 = new Histogram[6];
        for (int i = 0; i < nPlanes; i++) {
            hResidS1[i] = new Histogram(100, -10., 0.2, "Smoothed residual for rotated planes", "sigmas", "hits");
        }

        Instant timestamp = Instant.now();
        System.out.format("Beginning time = %s\n", timestamp.toString());
        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                                        ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        Helix helixBegin = TkInitial[0].copy();
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
                Vec rInt1 = new Vec(0., location[pln], 0.);
                if (verbose)
                    rInt1.print("  Plane first layer location=");

                // Randomly tilt the measurement planes to mimic misalignment
                RotMatrix Rt = new RotMatrix(phiR1[pln], thetaR1[pln], -phiR1[pln]);
                Plane pInt1 = new Plane(rInt1, Rt.rotate(tInt));
                SiModule newModule1 = new SiModule(pln, pInt1, 0., widths[pln], heights[pln], thickness, fM);
                SiModules.add(newModule1);

                Vec rInt2 = new Vec(0., location[pln] + delta, 0.);

                RotMatrix Rt2 = new RotMatrix(phiR2[pln], thetaR2[pln], -phiR2[pln]);
                Plane pInt2 = new Plane(rInt2, Rt2.rotate(tInt));
                SiModule newModule2 = new SiModule(pln, pInt2, stereoAngle[pln], widths[pln], heights[pln], thickness, fM);
                SiModules.add(newModule2);
            }

            // Populate the Si detector planes with hits from helices scattered at each
            // plane
            for (int ih = 0; ih < nHelices; ih++) {
                if (verbose)
                    printWriter2.format("$helix%d << EOD\n", ih);
                for (int pln = 0; pln < nPlanes; pln++) {
                    if (verbose) {
                        System.out.format("Extrapolating to plane #%d\n", pln);
                        Tk[ih].print("this plane");
                    }
                    SiModule thisSi = SiModules.get(2 * pln);
                    double phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt))
                        break;
                    if (verbose)
                        System.out.format("Plane %d, phiInt1= %12.10f\n", pln, phiInt);
                    Vec rscat = Tk[ih].atPhiGlobal(phiInt);
                    if (verbose) {
                        double check = (rscat.dif(thisSi.p.X()).dot(thisSi.p.T()));
                        System.out.format("Dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
                        Tk[ih].atPhi(phiInt).print("local intersection point");
                        Vec xIntGlob = Tk[ih].atPhiGlobal(phiInt);
                        xIntGlob.print("global intersection point");
                        double dPhi = -Q[ih] * (phiInt) / 20.0;
                        for (double phi = 0.; phi < Math.abs(phiInt); phi = phi + dPhi) {
                            Vec r = Tk[ih].atPhiGlobal(-Q[ih] * phi);
                            printWriter2.format("%10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                        }
                        // printWriter2.format("%10.6f %10.6f %10.6f\n", rscat.v[0], rscat.v[1], rscat.v[2]);
                        HelixPlaneIntersect hpi = new HelixPlaneIntersect();
                        Vec Xintersect = hpi.rkIntersect(thisSi.p, Tk[ih].atPhiGlobal(0.), Tk[ih].getMomGlobal(0.), Q[ih], fM);
                        double errX = Xintersect.dif(xIntGlob).mag();
                        System.out.format("Runge-Kutta difference from Helix extrapolation is %12.5e mm for plane %d\n", errX, pln);
                        Xintersect.print("Runge-Kutta intersection");
                    }
                    Vec rDet = thisSi.toLocal(rscat);
                    if (verbose) {
                        thisSi.p.print("first layer");
                        rscat.print("       Gobal intersection point 1");
                        rDet.print("       helix intersection in detector frame");
                    }
                    double[] gran = gausRan(); // !!!
                    m1[pln] = rDet.v[1] + resolution * gran[0];
                    hRes.entry(resolution * gran[0]);
                    if (verbose)
                        System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1[pln], rDet.v[1]);
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
                    Tk[ih] = Tk[ih].randomScat(thisSi.p, thisSi.thickness);
                    Vec t2 = Tk[ih].getMomGlobal(0.);
                    if (verbose) {
                        Tk[ih].print("scattered from the first layer of the detector plane");
                        t2.print("momentum after scatter");
                        double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                        System.out.format("Scattering angle from 1st layer=%10.7f\n", scattAng);
                    }
                    Vec t2Loc = Rtmp.rotate(t2);
                    hsp1theta.entry(Math.asin(t2Loc.v[1]));
                    double phiScat = Math.atan2(t2Loc.v[1], t2Loc.v[0]);
                    hps1.entry(phiScat / Math.PI);

                    // Now for the stereo layer
                    thisSi = SiModules.get(2 * pln + 1);
                    phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt))
                        break;
                    if (verbose) {
                        System.out.format("Plane %d, phiInt2= %f\n", pln, phiInt);
                        double dPhi = (phiInt) / 5.0;
                        for (double phi = 0.; phi < phiInt; phi = phi + dPhi) {
                            Vec r = Tk[ih].atPhiGlobal(phi);
                            printWriter2.format(" %10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                        }
                    }
                    rscat = Tk[ih].atPhiGlobal(phiInt);
                    // check = (rscat.dif(thisSi.p.X()).dot(thisSi.p.T()));
                    // System.out.format("Dot product of vector in plane with plane
                    // direction=%12.8e, should be zero\n", check);
                    if (verbose) {
                        thisSi.p.print("Second layer");
                        rscat.print("       Global intersection point 2");
                    }

                    Vec rscatRot = thisSi.toLocal(rscat);
                    if (verbose) {
                        rscatRot.print("       helix intersection in detector frame");
                    }
                    m2[pln] = rscatRot.v[1] + resolution * gran[1];
                    hRes.entry(resolution * gran[1]);
                    if (verbose)
                        System.out.format("       Measurement 2= %10.7f, Truth=%10.7f\n", m2[pln], rscatRot.v[1]);
                    Measurement thisM2 = new Measurement(m2[pln], resolution, rscat, rscatRot.v[1]);
                    thisSi.addMeasurement(thisM2);
                    if (pln != nPlanes - 1) {
                        t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                        uhat = t1.cross(zhat).unitVec(); // A unit vector u perpendicular to the helix direction
                        vhat = t1.cross(uhat);
                        Rtmp = new RotMatrix(uhat, vhat, t1);
                        Tk[ih] = Tk[ih].randomScat(thisSi.p, thisSi.thickness);
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
                }
                if (verbose)
                    printWriter2.format("EOD\n");
            }

            if (verbose) {
                printWriter2.format("$pnts << EOD\n");
            }
            for (SiModule si : SiModules) {
                Iterator<Measurement> itr = si.hits.iterator();
                while (itr.hasNext()) {
                    Measurement mm = itr.next();
                    Vec rLoc = si.toLocal(mm.rGlobal);
                    Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    if (nTrials == 1)
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                }
            }
            if (verbose) {
                printWriter2.format("EOD\n");
                printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                for (int ih = 0; ih < nHelices; ih++)
                    printWriter2.format(", $helix%d u 1:2:3 with lines lw 3", ih);
                System.out.format("\n");
                printWriter2.close();
            }

            // Create a seed track from the first 3 or 4 layers
            SeedTrack seed = new SeedTrack(SiModules, 0, 7, verbose);
            if (!seed.success)
                return;
            if (verbose) {
                seed.print("helix parameters");
                System.out.format("True helix is %10.6f %10.6f %10.6f %10.6f %10.6f\n", drho[0], phi0[0], K[0], dz[0], tanl[0]);
            }
            Vec initialHelixGuess = seed.helixParams();
            SquareMatrix initialCovariance = seed.covariance();
            double Bstart = seed.B();
            Vec tBstart = seed.T();

            // Cheating initial "guess" for the helix

            double[] rn = gausRan(); // !!!
            double drhoGuess = drho[0] + drhoSigma * rn[0];
            double dzGuess = dz[0] + dzSigma * rn[1];
            rn = gausRan(); // !!!
            double phi0Guess = phi0[0] + phi0Sigma * rn[0];
            double tanlGuess = tanl[0] + tanlSigma * rn[1];
            rn = gausRan();
            double kGuess = K[0] + kSigma * rn[0];
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
                helixOrigin.print("initial pivot guess");
                Bf0.print("B field at pivot");
            }
            Bstart = Bf0.mag();
            tBstart = Bf0.unitVec();

            initialCovariance.scale(10000.); // Blow up the errors on the initial guess

            if (verbose) {
                initialCovariance.print("initial covariance guess");
            }
            // Run the Kalman fit
            KalmanTrackFit kF = new KalmanTrackFit(SiModules, 0, 1, 1, helixOrigin, initialHelixGuess, initialCovariance, Bstart, tBstart,
                                            verbose);

            ArrayList<MeasurementSite> sites = kF.sites;
            Iterator<MeasurementSite> itr = sites.iterator();
            while (itr.hasNext()) {
                MeasurementSite site = itr.next();
                SiModule siM = site.m;
                if (site.m.stereo == 0.) {
                    if (site.filtered)
                        hResid0.entry(site.aF.r / Math.sqrt(site.aF.R));
                    if (site.smoothed)
                        hResidS0.entry(site.aS.r / Math.sqrt(site.aS.R));
                } else {
                    if (site.filtered)
                        hResid1.entry(site.aF.r / Math.sqrt(site.aF.R));
                    if (site.smoothed)
                        hResidS1[siM.Layer].entry(site.aS.r / Math.sqrt(site.aS.R));
                }
            }

            hChi2.entry(kF.chi2s);
            hChi2f.entry(kF.chi2f);

            if (kF.sites.get(kF.initialSite).aS != null && kF.sites.get(kF.finalSite).aS != null) {
                if (verbose) {
                    kF.fittedStateBegin().a.print("fitted helix parameters at the first layer");
                    kF.fittedStateBegin().origin.print("fitted helix origin at first layer");
                    helixBegin.p.print("actual helix parameters at the first layer");
                    helixBegin.origin.print("origin of actual helix");
                    helixBegin.X0.print("pivot of actual helix");
                }
                Vec newPivot = kF.fittedStateBegin().toLocal(helixBegin.origin.sum(helixBegin.X0));
                Vec oF = kF.fittedStateBegin().origin;
                Vec aF = kF.fittedStateBegin().pivotTransform(newPivot);
                if (verbose) {
                    aF.print("final smoothed helix parameters at the track beginning");
                    newPivot.print("final smoothed helix pivot");
                }
                Vec aFe = kF.fittedStateBegin().helixErrors(aF);
                SquareMatrix aFC = kF.fittedStateBegin().covariancePivotTransform(aF);
                if (verbose) {
                    aFe.print("error estimates on the helix parameters");
                    // aFC.print("helix parameters covariance");
                    helixMCtrue[0].print("MC true helix at the track beginning");
                }
                Vec trueErr = aF.dif(helixBegin.p);
                if (verbose) {
                    for (int i = 0; i < 5; i++) {
                        double diff = (trueErr.v[i]) / aFe.v[i];
                        System.out.format("     Helix parameter %d, error = %10.5f sigma\n", i, diff);
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
                if (verbose) {
                    eF.print("final smoothed helix parameters at the track end");
                    newPivot.print("new pivot at the track end");
                }
                Vec eFe = kF.fittedStateEnd().helixErrors(eF);
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
                SquareMatrix eFc = kF.fittedStateEnd().covariancePivotTransform(new Vec(0., 0., 0.), eF);
                helixChi2 = trueErr.dot(trueErr.leftMultiply(eFc.invert()));
                if (verbose)
                    System.out.format("Full chi^2 of the filtered helix parameters = %12.4e\n", helixChi2);
                hChi2Helix.entry(helixChi2);
            }
        }

        timestamp = Instant.now();
        System.out.format("Ending time = %s\n", timestamp.toString());
        ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(),
                                        ldt.getMinute(), ldt.getSecond(), ldt.getNano());

        hps1.plot(path + "phiScat1.gp", true, " ", " ");
        hsp1theta.plot(path + "projScat1.gp", true, " ", " ");
        hps2.plot(path + "phiScat2.gp", true, " ", " ");
        hsp2theta.plot(path + "projScat2.gp", true, " ", " ");
        hChi2.plot(path + "chi2s.gp", true, " ", " ");
        hChi2f.plot(path + "chi2f.gp", true, " ", " ");
        hChi2HelixS.plot(path + "chi2helixS.gp", true, " ", " ");
        hChi2Helix.plot(path + "chi2helixF.gp", true, " ", " ");
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
        hResid0.plot(path + "resid0.gp", true, " ", " ");
        hResid1.plot(path + "resid1.gp", true, " ", " ");
        hResidS0.plot(path + "residS0.gp", true, " ", " ");
        for (int i = 0; i < nPlanes; i++) {
            hResidS1[i].plot(path + String.format("residS1_%d.gp", i), true, " ", " ");
        }

        /*
         * // Test matrix code SquareMatrix t = new SquareMatrix(5); t.M[0][0] = 1.; t.M[0][1] = 1.; t.M[0][2] = 0.; t.M[0][3] = 0.2;
         * t.M[0][4] = 1.5; t.M[1][0] = 0.; t.M[1][1] = 2.; t.M[1][2] = 0.; t.M[1][3] = 0.8; t.M[1][4] = 0.; t.M[2][0] = 7.; t.M[2][1] =
         * 0.1; t.M[2][2] = 3.; t.M[2][3] = 0.; t.M[2][4] = 3.; t.M[3][0] = 0.; t.M[3][1] = 2.; t.M[3][2] = 0.1; t.M[3][3] = 4.; t.M[3][4] =
         * 0.7; t.M[4][0] = 0.; t.M[4][1] = 4.; t.M[4][2] = 0.; t.M[4][3] = 1.; t.M[4][4] = 5.; t.print("test");
         * 
         * SquareMatrix q = t.invert(); q.print("inverse");
         * 
         * SquareMatrix pr = q.multiply(t); pr.print("product");
         */
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
        if (R < 0.)
            r[1] += Math.PI;
        r[2] = alpha / R;
        r[0] = xc / Math.cos(r[1]) - R;
        return r;
    }

    public HelixTest() {
        System.out.format("Unnecessary HelixTest constructor; all the work is done in main\n");
    }

}
