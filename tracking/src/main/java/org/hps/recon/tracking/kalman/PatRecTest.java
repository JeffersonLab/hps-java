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
public class PatRecTest {

    public PatRecTest(String path) {
        // Units are Tesla, GeV, mm

        int nTrials = 10000; // The number of test events to generate for fitting
        boolean MCplot = false; // true to plot MC tracks, otherwise fitted tracks
        boolean perfect = false;
        double thickness = 0.3; // Silicon thickness in mm
        if (perfect) {
            thickness = 0.0000000000001;
        }
        boolean rungeKutta = true; // Set true to generate the helix by Runge Kutta integration instead of a piecewise helix
        boolean verbose = nTrials < 2;

        int nHelices = 2; // Number of helix tracks to simulate
        double[] Q = new double[nHelices]; // charge
        double[] p = new double[nHelices]; // momentum
        Vec helixOrigin = new Vec(0., 0., 0.); // Pivot point of initial helices
        double Phi = 90. * Math.PI / 180.;
        double Theta = 88. * Math.PI / 180.;
        Vec[] initialDirection = new Vec[nHelices];
        for (int i = 0; i < nHelices; i++) {
            double[] gran = gausRan();
            if (gran[0] > 0.)
                Q[i] = 1.0;
            else
                Q[i] = -1.0;
            p[i] = 1.0 + gran[1] * 0.02;
            gran = gausRan();
            double PhiR = Phi + gran[0] * 0.5 * Math.PI / 180.;
            double ThetaR = Theta + gran[1] * 0.5 * Math.PI / 180.;
            initialDirection[i] = new Vec(Math.cos(PhiR) * Math.sin(ThetaR), Math.sin(PhiR) * Math.sin(ThetaR), Math.cos(ThetaR));
            initialDirection[i].print("initial particle direction");
        }
        double[] drho = new double[nHelices]; // { -0., 0., 1. }; // Helix parameters
        double[] dz = new double[nHelices]; // { 5.0, 1.0, 4.0 };
        double[] phi0 = new double[nHelices]; // { 0.0, 0.04, 0.05 };
        double[] tanl = new double[nHelices]; // { 0.1, 0.12, 0.13 };
        double[] K = new double[nHelices];

        // Tracking instrument description
        int nPlanes = 6;
        Vec tInt = new Vec(0., 1., 0.); // Nominal detector plane orientation
        double[] location = { 100., 200., 300., 500., 700., 900. }; // Detector positions in y
        double delta = 5.0; // Distance between stereo pairs
        double[] stereoAngle = { 0.1, 0.1, 0.1, 0.05, 0.05, 0.05 }; // Angles of the stereo layers in radians
        double resolution = 0.012; // SSD point resolution, in mm

        double[] thetaR1 = new double[nPlanes];
        double[] phiR1 = new double[nPlanes];
        double[] thetaR2 = new double[nPlanes];
        double[] phiR2 = new double[nPlanes];
        for (int i = 0; i < nPlanes; i++) { // Generate some random misalignment of the detector planes
            // double[] gran = gausRan();
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
        if (mapType != "binary") {
            fM.writeBinaryFile("C:\\Users\\Robert\\Desktop\\Kalman\\fieldmap.bin");
        }
        Vec Bpivot = fM.getField(helixOrigin);
        helixOrigin.print("initial pivot point");
        Bpivot.print("magnetic field at the initial pivot");
        for (int pln = 0; pln < nPlanes; pln++) {
            Vec bf = fM.getField(new Vec(0., location[pln], 0.));
            System.out.format("B field at plane %d = %10.7f, %10.7f, %10.7f\n", pln, bf.v[0], bf.v[1], bf.v[2]);
        }

        Helix[] TkInitial = new Helix[nHelices];
        Vec[] helixMCtrue = new Vec[nHelices];
        for (int i = 0; i < nHelices; i++) {
            Vec momentum = new Vec(p[i] * initialDirection[i].v[0], p[i] * initialDirection[i].v[1], p[i] * initialDirection[i].v[2]);
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
        // Define histograms
        Histogram hNtracks = new Histogram(10, 0., 1., "Number of tracks found and fitted", "tracks", "events");
        Histogram hNhits = new Histogram(15, 0., 1., "Number of hits per fitted track", "hits", "tracks");
        Histogram hTkChi2 = new Histogram(80, 0., .5, "Track helix fit chi^2 after smoothing", "chi^2", "tracks");
        Histogram hEdrhoS = new Histogram(100, -10., 0.2, "Smoothed helix parameter drho error", "sigmas", "track");
        Histogram hEdrho = new Histogram(100, -2., 0.04, "drho error", "mm", "tracks");
        Histogram hEphi0S = new Histogram(100, -10., 0.2, "Smoothed helix parameter phi0 error", "sigmas", "track");
        Histogram hEphi0 = new Histogram(100, -2., 0.04, "phi0 error", "degrees", "tracks");
        Histogram hEkS = new Histogram(100, -10., 0.2, "Smoothed helix parameter K error", "sigmas", "track");
        Histogram hEk = new Histogram(100, -40., 0.8, "curvature error", "percent", "tracks");
        Histogram hEdzS = new Histogram(100, -10., 0.2, "Smoothed helix parameter dz error", "sigmas", "track");
        Histogram hEdz = new Histogram(100, -1., 0.02, "dz error", "mm", "tracks");
        Histogram hEtanlS = new Histogram(100, -10., 0.2, "Smoothed helix parameter tanl error", "sigmas", "track");
        Histogram hEtanl = new Histogram(100, -.02, 0.0004, "tanl error", " ", "tracks");
        Histogram hChi2HelixS = new Histogram(80, 0., 0.4, "Smoothed chi^2 of helix parameters", "chi^2", "tracks");
        Histogram hXerr = new Histogram(100, -20., 0.4, "error on the vertex x coordinate", "sigmas", "tracks");
        Histogram hYerr = new Histogram(100, -20., 0.4, "error on the vertex y coordinate", "sigmas", "tracks");
        Histogram hZerr = new Histogram(100, -20., 0.4, "error on the vertex z coordinate", "sigmas", "tracks");

        Instant timestamp = Instant.now();
        System.out.format("Beginning time = %s\n", timestamp.toString());
        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(),
                                        ldt.getSecond(), ldt.getNano());

        Vec[] helixSaved = new Vec[2 * nPlanes];
        Helix helixBegin = TkInitial[0].copy();
        helixBegin.print("helixBegin");
        TkInitial[0].print("TkInitial");
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
                if (verbose) {
                    rInt1.print("  Plane first layer location=");
                }

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
                        thisSi.p.print("first layer");
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
                    if (verbose) {
                        System.out.format("       Measurement 1= %10.7f,  Truth=%10.7f\n", m1[pln], rDet.v[1]);
                    }
                    Measurement md = null;
                    for (Measurement mm : thisSi.hits) {
                        if (Math.abs(mm.v - m1[pln]) < 0.04) { // assume hits overlap if less than 40 microns apart
                            md = mm;
                            break;
                        }
                    }
                    if (md != null) {
                        md.v = 0.5 * (md.v + m1[pln]); // overlapping hits
                    } else {
                        Measurement thisM1 = new Measurement(m1[pln], resolution, rscat, rDet.v[1]);
                        thisSi.addMeasurement(thisM1);
                    }

                    Vec t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                    if (verbose) {
                        Tk[ih].getMom(phiInt).print("helix local momentum before scatter");
                        Tk[ih].getMomGlobal(phiInt).print("helix global momentum before scatter");
                    }
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

                    // Now for the stereo layer
                    thisSi = SiModules.get(2 * pln + 1);
                    phiInt = Tk[ih].planeIntersect(thisSi.p);
                    if (Double.isNaN(phiInt)) break;
                    if (verbose) {
                        System.out.format("Plane %d, phiInt2= %f\n", pln, phiInt);
                        double dPhi = (phiInt) / 5.0;
                        int npnts = 0;
                        for (double phi = 0.; phi < phiInt; phi = phi + dPhi) {
                            npnts++;
                            Vec r = Tk[ih].atPhiGlobal(phi);
                            printWriter2.format(" %10.6f %10.6f %10.6f\n", r.v[0], r.v[1], r.v[2]);
                            if (npnts > 1000) break;
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
                    if (verbose) System.out.format("       Measurement 2= %10.7f, Truth=%10.7f\n", m2[pln], rscatRot.v[1]);
                    md = null;
                    for (Measurement mm : thisSi.hits) {
                        if (Math.abs(mm.v - m2[pln]) < 0.04) { // assume hits overlap if less than 40 microns apart
                            md = mm;
                            break;
                        }
                    }
                    if (md != null) {
                        md.v = 0.5 * (md.v + m2[pln]); // overlapping hits
                    } else {
                        Measurement thisM2 = new Measurement(m2[pln], resolution, rscat, rscatRot.v[1]);
                        thisSi.addMeasurement(thisM2);
                    }
                    if (pln != nPlanes - 1) {
                        t1 = Tk[ih].getMomGlobal(phiInt).unitVec();
                        Tk[ih] = Tk[ih].randomScat(thisSi.p, rscat, pInt, thisSi.thickness);
                        t2 = Tk[ih].getMomGlobal(0.).unitVec();
                        if (verbose) {
                            Tk[ih].print("scattered from the second layer of the measurement plane");
                            double scattAng = Math.acos(Math.min(1.0, t1.dot(t2)));
                            System.out.format("Scattering angle from 2nd layer=%10.7f\n", scattAng);
                        }
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
                    for (Measurement mm : si.hits) {
                        Vec rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                        Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                        // printWriter2.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", mm.rGlobal.v[0], mm.rGlobal.v[1], mm.rGlobal.v[2]);
                        System.out.format("   Hit location %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    }
                }
                printWriter2.format("EOD\n");
            }

            KalmanPatRecHPS patRec = new KalmanPatRecHPS(SiModules, verbose);
            if (verbose) {
                for (KalTrack tkr : patRec.TkrList) {
                    printWriter2.format("$tkr%d << EOD\n", tkr.ID);
                    for (MeasurementSite site : tkr.SiteList) {
                        StateVector aS = site.aS;
                        SiModule m = site.m;
                        double phiS = aS.planeIntersect(m.p);
                        if (Double.isNaN(phiS)) continue;
                        Vec rLocal = aS.atPhi(phiS);
                        Vec rGlobal = aS.toGlobal(rLocal);
                        printWriter2.format(" %10.6f %10.6f %10.6f\n", rGlobal.v[0], rGlobal.v[1], rGlobal.v[2]);
                        // Vec rDetector = m.toLocal(rGlobal);
                        // double vPred = rDetector.v[1];
                        // if (site.hitID >= 0) {
                        // System.out.format("vPredPrime=%10.6f, vPred=%10.6f, v=%10.6f\n", vPred, aS.mPred, m.hits.get(site.hitID).v);
                        // }
                    }
                    printWriter2.format("EOD\n");
                }
                printWriter2.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
                if (MCplot) {
                    for (int ih = 0; ih < nHelices; ih++) {
                        printWriter2.format(", $helix%d u 1:2:3 with lines lw 3", ih);
                    }
                } else {
                    for (KalTrack tkr : patRec.TkrList) {
                        printWriter2.format(", $tkr%d u 1:2:3 with lines lw 3", tkr.ID);
                    }
                }
                printWriter2.format("\n");
                printWriter2.close();
            }
            // Analysis of the tracking results
            int nTracks = patRec.TkrList.size();
            hNtracks.entry(nTracks);
            for (KalTrack tkr : patRec.TkrList) {
                hNhits.entry(tkr.nHits);
                hTkChi2.entry(tkr.chi2);
                // Compare with the generated particles
                Vec helixAtOrigin = new Vec(5, tkr.originHelix());
                SquareMatrix Cinv = (new SquareMatrix(5, tkr.originCovariance())).invert();
                double minChi2 = 9999.e12;
                int iBest = -1;
                for (int ih = 0; ih < nHelices; ih++) {
                    Vec trueErr = helixAtOrigin.dif(TkInitial[ih].p);
                    double helixChi2 = trueErr.dot(trueErr.leftMultiply(Cinv));
                    if (helixChi2 < minChi2) {
                        minChi2 = helixChi2;
                        iBest = ih;
                    }
                }
                if (verbose) TkInitial[iBest].print("Best MC track");
                Vec trueErr = helixAtOrigin.dif(TkInitial[iBest].p);
                if (verbose) {
                    for (int i = 0; i < 5; i++) {
                        double diff = (trueErr.v[i]) / tkr.helixErr(i);
                        System.out.format("     Helix parameter %d after smoothing, error = %10.5f sigma\n", i, diff);
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
            }
        }
        timestamp = Instant.now();
        System.out.format("Ending time = %s\n", timestamp.toString());
        ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        System.out.format("%s %d %d at %d:%d %d.%d seconds\n", ldt.getMonth(), ldt.getDayOfMonth(), ldt.getYear(), ldt.getHour(), ldt.getMinute(),
                                        ldt.getSecond(), ldt.getNano());

        hNtracks.plot(path + "nTracks.gp", true, " ", " ");
        hNhits.plot(path + "nHits.gp", true, " ", " ");
        hTkChi2.plot(path + "tkrChi2.gp", true, " ", " ");
        hEdrhoS.plot(path + "drhoErrorS.gp", true, " ", " ");
        hEphi0S.plot(path + "phi0ErrorS.gp", true, " ", " ");
        hEkS.plot(path + "kErrorS.gp", true, " ", " ");
        hEdzS.plot(path + "dzErrorS.gp", true, " ", " ");
        hEtanlS.plot(path + "tanlErrorS.gp", true, " ", " ");
        hEdrho.plot(path + "drhoError.gp", true, " ", " ");
        hEphi0.plot(path + "phi0Error.gp", true, " ", " ");
        hEk.plot(path + "kError.gp", true, " ", " ");
        hEdz.plot(path + "dzError.gp", true, " ", " ");
        hEtanl.plot(path + "tanlError.gp", true, " ", " ");
        hChi2HelixS.plot(path + "chi2helixS.gp", true, " ", " ");
        hXerr.plot(path + "xErr.gp", true, " ", " ");
        hYerr.plot(path + "yErr.gp", true, " ", " ");
        hZerr.plot(path + "zErr.gp", true, " ", " ");
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
