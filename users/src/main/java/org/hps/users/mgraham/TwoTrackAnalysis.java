package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.deprecated.BeamlineConstants;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.util.Resettable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 *
 * @author mgraham
 */
public class TwoTrackAnalysis extends Driver {

    public String outputTextName = "myevents.txt";
    FileWriter fw;
    PrintWriter pw;
    //private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    IPlotter plotter5;
    IPlotter plotter6;
    IPlotter plotter7;
    IPlotter plotter8;
    IAnalysisFactory fac = aida.analysisFactory();
    private String trackCollectionName = "MatchedTracks";
    private String outputPlots = null;
    private boolean isMC = true;
    private boolean showPlots = false;
    int nevt = 0;

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        //if (showPlots) {
        //    plotterFrame = new AIDAFrame();
        //    plotterFrame.setTitle("HPS Tracking Plots");
        //}
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        //if (showPlots) {
        //    plotterFrame.addPlotter(plotter);
        //}

        IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);

////////////////////////////////////////////////////////////////////

        plotter2 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter2.setTitle("Vertex");

        plotter2.style().dataStyle().fillStyle().setColor("yellow");
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(2, 2);
        //if (showPlots) {
        //    plotterFrame.addPlotter(plotter2);
        //}

        IHistogram1D xvert = aida.histogram1D("XVertex", 40, -30, 50);
        IHistogram1D yvert = aida.histogram1D("YVertex", 40, -35, 30);
        IHistogram1D zvert = aida.histogram1D("ZVertex", 40, -800, -450);
        IHistogram1D dist = aida.histogram1D("Distance btwn Trks @ Solution", 40, 0, 20);
        plotter2.region(0).plot(xvert);
        plotter2.region(1).plot(yvert);
        plotter2.region(2).plot(zvert);
        plotter2.region(3).plot(dist);
        ////////////////////////////////////////////////////////////////////

        plotter4 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter4.setTitle("Vertex w/cut");
        plotter4.style().dataStyle().fillStyle().setColor("yellow");
        plotter4.style().dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(2, 2);
        //if (showPlots) {
        //    plotterFrame.addPlotter(plotter4);
        //}

        IHistogram1D xvertns = aida.histogram1D("XVertex with y cut", 40, -30, 50);
        IHistogram1D yvertns = aida.histogram1D("YVertex with y cut", 40, -35, 30);
        IHistogram1D zvertns = aida.histogram1D("ZVertex with y cut", 40, -800, -450);
        IHistogram1D distns = aida.histogram1D("Distance btwn Trks with y cut", 40, 0, 20);
        plotter4.region(0).plot(xvertns);
        plotter4.region(1).plot(yvertns);
        plotter4.region(2).plot(zvertns);
        plotter4.region(3).plot(distns);
////////////////////////////////////////////////////////////////////

        plotter3 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter3.setTitle("Track @ Converter");

        plotter3.style().dataStyle().fillStyle().setColor("yellow");
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(2, 2);
        //if (showPlots) {
        //    plotterFrame.addPlotter(plotter3);
        //}
        IHistogram1D xAtConvert = aida.histogram1D("X (mm) @ Converter using Map", 50, -50, 50);
        IHistogram1D yAtConvert = aida.histogram1D("Y (mm) @ Converter using Map", 50, -20, 20);
        IHistogram1D xAtConvertSLT = aida.histogram1D("X (mm) @ Converter using SLT", 50, -50, 50);
        IHistogram1D yAtConvertSLT = aida.histogram1D("Y (mm) @ Converter using SLT", 50, -20, 20);
        plotter3.region(0).plot(xAtConvert);
        plotter3.region(1).plot(yAtConvert);
        plotter3.region(2).plot(xAtConvertSLT);
        plotter3.region(3).plot(yAtConvertSLT);

        plotter5 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter5.setTitle("Mass");
        plotter5.style().dataStyle().fillStyle().setColor("yellow");
        plotter5.style().dataStyle().errorBarStyle().setVisible(false);
        //plotter5.createRegions(2, 2);
        //if (showPlots) {
        //    plotterFrame.addPlotter(plotter5);
        //}
        IHistogram1D invMass = aida.histogram1D("Invariant Mass", 50, 0, .4);

        plotter5.region(0).plot(invMass);

        plotter6 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter6.setTitle("correlations");
        plotter6.style().setParameter("hist2DStyle", "colorMap");
        plotter6.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter6.createRegions(1, 3);
        IHistogram2D xy = aida.histogram2D("X v Y", 50, -30, 50, 50, -35, 30);
        IHistogram2D xz = aida.histogram2D("X v Z", 50, -30, 50, 50, -800, -450);
        IHistogram2D yz = aida.histogram2D("Y v Z", 50, -35, 30, 50, -800, -450);
        plotter6.region(0).plot(xy);
        plotter6.region(1).plot(xz);
        plotter6.region(2).plot(yz);



        IHistogram1D trkbins = aida.histogram1D("Track Distributions", 5, -2, 3);
        IHistogram2D twtrkptot = aida.histogram2D("Total P+ vs. P-", 60, 0, 4, 60, 0, 4);
        IHistogram1D sumtrks = aida.histogram1D("Sum of Track's Momentums", 100, -1, 7);
        IHistogram1D perptrks = aida.histogram1D("Perpendicular Momentum", 100, 0, .1);
        IHistogram2D pyppm = aida.histogram2D("Py+ vs. Py-", 60, -.1, .1, 60, -.1, .1);
        IHistogram2D pzppm = aida.histogram2D("Pz+ vs. Pz-", 60, -.1, .1, 60, -.1, .1);
        IHistogram1D px = aida.histogram1D("Two Track X Momentum", 40, 0, 4);
        IHistogram1D py = aida.histogram1D("Two Track Y Momentum", 40, -.1, .1);
        IHistogram1D pz = aida.histogram1D("Two Track Z Momentum", 40, -.1, .1);
        IHistogram1D chi2 = aida.histogram1D("Tracks Chi2", 25, 0, 25.0);
        IHistogram1D bbpx = aida.histogram1D("Big Bump Track Momenta (Px)", 40, 0, 4);
        IHistogram1D bbpy = aida.histogram1D("Big Bump Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D bbpz = aida.histogram1D("Big Bump Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D bbchi2 = aida.histogram1D("Big Bump Tracks Chi2", 25, 0, 25.0);
        IHistogram1D spx = aida.histogram1D("Split Track Momenta (Px)", 40, 0, 4);
        IHistogram1D spy = aida.histogram1D("Split Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D spz = aida.histogram1D("Split Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D schi2 = aida.histogram1D("Split Tracks Chi2", 25, 0, 25.0);
        IHistogram1D bbsumtrks = aida.histogram1D("Big Bump Sum of Track's Momentums", 50, -1, 7);
        IHistogram2D bbpppm = aida.histogram2D("Big Bump P+ vs. P-", 50, 0, 4, 50, 0, 4);
        IHistogram2D lbpppm = aida.histogram2D("Little Bump P+ vs. P-", 50, 0, 4, 50, 0, 4);
        IHistogram1D lbsumtrks = aida.histogram1D("Little Bump Sum of Track's Momentums", 50, -1, 7);
        IHistogram1D lbpx = aida.histogram1D("Little Bump Track Momenta (Px)", 40, 0, 4);
        IHistogram1D lbpy = aida.histogram1D("Little Bump Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D lbpz = aida.histogram1D("Little Bump Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D lbchi2 = aida.histogram1D("Little Bump Tracks Chi2", 25, 0, 25.0);
        IHistogram2D xyemt = aida.histogram2D("X v Y - e- Top", 50, -30, 50, 50, -35, 30);
        IHistogram2D xzemt = aida.histogram2D("X v Z - e- Top", 50, -30, 50, 50, -800, -450);
        IHistogram2D yzemt = aida.histogram2D("Y v Z - e- Top", 50, -35, 30, 50, -800, -450);
        IHistogram1D qbins = aida.histogram1D("Charge Distributions", 5, -2, 3);
        IHistogram1D lbtp = aida.histogram1D("Little Bump Track Parity", 7, 0, 7);
        IHistogram1D bbtp = aida.histogram1D("Big Bump Track Parity", 7, 0, 7);
        IHistogram1D xres = aida.histogram1D("X Res Trk1", 40, -0.25, 0.25);
        IHistogram1D yres = aida.histogram1D("Y Res Trk1", 40, -0.25, 0.25);
        IHistogram1D xres2 = aida.histogram1D("X Res Trk2", 40, -0.25, 0.25);
        IHistogram1D yres2 = aida.histogram1D("Y Res Trk2", 40, -0.25, 0.25);
        IHistogram1D unx1 = aida.histogram1D("Uncert X Trk 1", 50, 0, 10);
        IHistogram1D uny1 = aida.histogram1D("Uncert Y Trk 1", 50, 0, 10);
        IHistogram1D unz1 = aida.histogram1D("Uncert Z Trk 1", 50, 0, 40);
        IHistogram1D unx2 = aida.histogram1D("Uncert X Trk 2", 50, 0, 10);
        IHistogram1D uny2 = aida.histogram1D("Uncert Y Trk 2", 50, 0, 10);
        IHistogram1D unz2 = aida.histogram1D("Uncert Z Trk 2", 50, 0, 40);
        IHistogram2D xyept = aida.histogram2D("X v Y - e+ Top", 50, -30, 50, 50, -35, 30);
        IHistogram2D xzept = aida.histogram2D("X v Z - e+ Top", 50, -30, 50, 50, -800, -450);
        IHistogram2D yzept = aida.histogram2D("Y v Z - e+ Top", 50, -35, 30, 50, -800, -450);
        IHistogram1D three = aida.histogram1D("Three Track Invariant Mass", 50, 0, .4);

        plotter7 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter7.setTitle("correlations e+ Top");
        plotter7.style().setParameter("hist2DStyle", "colorMap");
        plotter7.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter7.createRegions(1, 3);
        plotter7.region(0).plot(xyept);
        plotter7.region(1).plot(xzept);
        plotter7.region(2).plot(yzept);

        plotter8 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter8.setTitle("correlations e- Top");
        plotter8.style().setParameter("hist2DStyle", "colorMap");
        plotter8.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter8.createRegions(1, 3);
        plotter8.region(0).plot(xyemt);
        plotter8.region(1).plot(xzemt);
        plotter8.region(2).plot(yzemt);

        //if (showPlots) {
        //    plotterFrame.pack();
        //    plotterFrame.setVisible(true);
        //}
    }

    public void setIsMC(boolean setit) {
        isMC = setit;
    }

    public void process(EventHeader event) {

        if (nevt == 0) {
            try {
//open things up
                fw = new FileWriter(outputTextName);
                pw = new PrintWriter(fw);
            } catch (IOException ex) {
                Logger.getLogger(TwoTrackAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        nevt++;
        aida.tree().cd("/");
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        System.out.println("Staring TwoTrackAnalysis");
        for (Track trk : tracks) {
            aida.histogram1D("Track Momentum (Px)").fill(trk.getPY());
            aida.histogram1D("Track Momentum (Py)").fill(trk.getPZ());
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getPX());
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HelixConverter converter = new HelixConverter(0);
            StraightLineTrack slt = converter.Convert(ht);
            HPSTrack hpstrack = new HPSTrack(ht);
            Hep3Vector[] trkatconver = hpstrack.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            aida.histogram1D("X (mm) @ Converter using Map").fill(trkatconver[0].x()); // y tracker frame?
            aida.histogram1D("Y (mm) @ Converter using Map").fill(trkatconver[0].y()); // z tracker frame?
            if (slt != null) {
                aida.histogram1D("X (mm) @ Converter using SLT").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]); // y tracker frame?
                aida.histogram1D("Y (mm) @ Converter using SLT").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]); // z tracker frame?
            }
        }

        System.out.println("...checking if two tracks...");

        if (tracks.size() == 2) { //uncert can be used here  && (Ytrue || Ytrue2) && (Xtrue || Xtrue2)
            System.out.println("               ...yes!");

            Track trk1 = tracks.get(0);
            Track trk2 = tracks.get(1);
            int isTrk1Top = -1;
            if (trk1.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTrk1Top = 1;
            }
            int isTrk2Top = -1;
            if (trk2.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTrk2Top = 1;
            }
            boolean topbot = false;
            if ((isTrk1Top + isTrk2Top) == 0) {
                topbot = true;
            }

            SeedTrack stEle1 = (SeedTrack) trk1;
            SeedCandidate seedEle1 = stEle1.getSeedCandidate();
            HelicalTrackFit ht1 = seedEle1.getHelix();
            HelixConverter converter1 = new HelixConverter(0);
            StraightLineTrack slt1 = converter1.Convert(ht1);

            SeedTrack stEle2 = (SeedTrack) trk2;
            SeedCandidate seedEle2 = stEle2.getSeedCandidate();
            HelicalTrackFit ht2 = seedEle2.getHelix();
            HelixConverter converter2 = new HelixConverter(0);
            StraightLineTrack slt2 = converter2.Convert(ht2);

//            HPSTrack hpstrack1 = new HPSTrack(ht1);
//            Hep3Vector[] trkatconver1 = hpstrack1.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION, 1);
//            HPSTrack hpstrack2 = new HPSTrack(ht2);
//            Hep3Vector[] trkatconver2 = hpstrack2.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION, 1);

            HPSTrack hpstrack1 = new HPSTrack(ht1);
            Hep3Vector[] trkatconver1 = {new BasicHep3Vector(), new BasicHep3Vector(0, 0, 0)};
            HPSTrack hpstrack2 = new HPSTrack(ht2);
            Hep3Vector[] trkatconver2 = {new BasicHep3Vector(), new BasicHep3Vector(0, 0, 0)};;
            if (isMC) {
                double[] t1 = slt1.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN);
                double[] t2 = slt2.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN);
                trkatconver1[0] = new BasicHep3Vector(t1[0], t1[1], BeamlineConstants.HARP_POSITION_TESTRUN);
                trkatconver2[0] = new BasicHep3Vector(t2[0], t2[1], BeamlineConstants.HARP_POSITION_TESTRUN);
            } else {
                trkatconver1 = hpstrack1.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
                trkatconver2 = hpstrack2.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            }
            List<TrackerHit> hitsOnTrack1 = trk1.getTrackerHits();
            int layer1;
            double y1 = 0;
            double y2 = 0;
            double z1 = 0;
            double z2 = 0;
            double dely1 = 0;
            double dely2 = 0;
            for (TrackerHit hit : hitsOnTrack1) {
                HelicalTrackHit htc1 = (HelicalTrackHit) hit;
                layer1 = htc1.Layer();
                int y1layer = 0;
                int y2layer = 0;
                if (y1 == 0) {
                    y1 = htc1.getPosition()[2]; //
                    z1 = htc1.getPosition()[0]; // z1 is jlab but the get position refers to hps-tracking
                    y1layer = layer1;
                    SymmetricMatrix ErrorHitOne = htc1.getCorrectedCovMatrix();
                    dely1 = Math.sqrt(ErrorHitOne.diagonal(2)); //y in jlab is z in hps
                } else {

                    if ((layer1 > y1layer) && (y2layer == 0)) {
                        y2 = htc1.getPosition()[2]; //
                        z2 = htc1.getPosition()[0]; // see above comments!
                        y2layer = layer1;
                        SymmetricMatrix ErrorHitTwo = htc1.getCorrectedCovMatrix();
                        dely2 = Math.sqrt(ErrorHitTwo.diagonal(2));
                    }

                }

            }
            List<TrackerHit> hitsOnTrack2 = trk2.getTrackerHits();

            double my1 = 0;
            double my2 = 0;
            double mz1 = 0;
            double mz2 = 0;
            double delymy1 = 0;
            double delymy2 = 0;
            int layer2;
            for (TrackerHit hit : hitsOnTrack2) {
                HelicalTrackHit htc2 = (HelicalTrackHit) hit;
//            if (htc.getPosition()[2] < 0) {

                layer2 = htc2.Layer();
                int my1layer = 0;
                int my2layer = 0;
                if (my1 == 0) {
                    my1 = htc2.getPosition()[2]; //see above comments
                    mz1 = htc2.getPosition()[0];
                    my1layer = layer2;
                    SymmetricMatrix ErrorHitOne = htc2.getCorrectedCovMatrix();
                    delymy1 = Math.sqrt(ErrorHitOne.diagonal(2));
                } else {
                    if ((layer2 > my1layer) && (my2layer == 0)) {
                        my2 = htc2.getPosition()[2];
                        mz2 = htc2.getPosition()[0];
                        my2layer = layer2;
                        SymmetricMatrix ErrorHitTwo = htc2.getCorrectedCovMatrix();
                        delymy2 = Math.sqrt(ErrorHitTwo.diagonal(2));
                    }
                }
            }
            //   double dely = .00001; //mm
            double b1;
            double m1;
            double b2;
            double m2;
            boolean check1 = true;
            if (y1 == 0) {
                check1 = false;
            }
            boolean check2 = true;
            if (my1 == 0) {
                check2 = false;
            }
            boolean check3 = true;
            if (my2 == 0) {
                check3 = false;
            }



            double X1 = -99, Y1 = -99;
            if (slt1 != null) {
                X1 = slt1.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0];
                Y1 = slt1.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1];
            }

            boolean X1cent = false;
            boolean Y1cent = false; //for simulation

            if (11 < X1 && X1 < 29) {
                X1cent = true;
            }
            if (-3.5 < Y1 && Y1 < 3.5) {
                Y1cent = true;
            }

            double X2 = 99, Y2 = 99;
            if (slt2 != null) {
                X2 = slt2.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0];
                Y2 = slt2.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1];
            }

            boolean X2cent = false;
            boolean Y2cent = false; //for simulation
            if (11 < X2 && X2 < 29) {
                X2cent = true;
            }
            if (-3.5 < Y2 && Y2 < 3.5) {
                Y2cent = true;
            }

            int qtrk1 = trk1.getCharge();
            int qtrk2 = trk2.getCharge();
            boolean pm = false;
            if ((qtrk1 + qtrk2) == 0) {
                pm = true;
            }


            if (topbot && pm) {

                double b1p;
                double b2p;
                double m1p;
                double m2p;

                if (check1 && check2 && check3) {
                    if (isTrk1Top == 1) {
                        double zc = -1 * z1 / (z2 - z1);
                        b1 = (zc * (y2 - y1 + .5 * (dely2 + dely1))) + y1 - (.5 * dely1);
                        m1 = (y2 - y1 + .5 * (dely2 + dely1)) / (z2 - z1);
                        m1p = (y2 - y1 - .5 * (dely2 + dely1)) / (z2 - z1);
                        b1p = y1 - (m1p * z1) + (.5 * dely1);
                    } else {
                        double zc = -1 * z1 / (z2 - z1);
                        b1 = (zc * (y2 - y1 - .5 * (dely2 + dely1))) + y1 + (.5 * dely1);
                        m1 = (y2 - y1 - .5 * (dely2 + dely1)) / (z2 - z1);
                        m1p = (y2 - y1 + .5 * (dely2 + dely1)) / (z2 - z1);
                        b1p = y1 - (m1p * z1) - (.5 * dely1);
                    }

                    if (isTrk2Top == 1) {
                        double zc = -1 * mz1 / (mz2 - mz1);
                        b2 = (zc * (my2 - my1 + .5 * (delymy2 + delymy1))) + my1 - (.5 * delymy1);
                        m2 = (my2 - my1 + .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        m2p = (my2 - my1 - .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        b2p = my1 - (m2p * mz1) + (.5 * delymy1);
                    } else {
                        double zc = -1 * mz1 / (mz2 - mz1);
                        b2 = (zc * (my2 - my1 - .5 * (delymy2 + delymy1))) + my1 + (.5 * delymy1);
                        m2 = (my2 - my1 - .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        m2p = (my2 - my1 + .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        b2p = my1 - (m2p * mz1) - (.5 * delymy1);
                    }

                    double zi = (b2 - b1) / (m1 - m2);
                    double zr = Math.abs(zi - BeamlineConstants.HARP_POSITION_TESTRUN);
                    double zs = 2 * zr / 100;
                    //      System.out.println("Closest Possible Z to Tracker");
                    //      System.out.println(zi);


                    List<double[]> Trk1 = new ArrayList<double[]>();
                    for (int i = 0; i < 100; i++) {
                        double z = BeamlineConstants.HARP_POSITION_TESTRUN - zr + (zs * i);
                        double[] posvec = new double[3];
                        if (isMC) {
                            posvec[0] = slt1.getYZAtX(z)[0];
                            posvec[1] = slt1.getYZAtX(z)[1];
                            posvec[2] = z;
                        } else {
                            Hep3Vector[] trk1atz = hpstrack1.getPositionAtZMap(100, z, 1);
                            posvec[0] = trk1atz[0].x();
                            posvec[1] = trk1atz[0].y();
                            posvec[2] = z;
                        }
                        Trk1.add(posvec);
                    }
                    //  System.out.println("Vectors ");

                    //  System.out.println(Trk1);

                    double xbar = 0;
                    double ybar = 0;
                    double zbar = 0;
                    double xsqbar = 0;
                    double ysqbar = 0;
                    double zsqbar = 0;
                    int n = 0;
                    for (double[] inttrk : Trk1) {
                        //      System.out.println(inttrk[0]);
                        //    System.out.println(inttrk[1]);
                        //    System.out.println(inttrk[2]);
                        xbar = xbar + inttrk[0];
                        ybar = ybar + inttrk[1];
                        zbar = zbar + inttrk[2];
                        n = n + 1;
                    }
                    //   System.out.println("n " + n);
                    xbar = xbar / n;
                    ybar = ybar / n;
                    zbar = zbar / n;
                    //    System.out.println("Xbar is " + xbar);
                    //    System.out.println("Ybar is " + ybar);
                    //    System.out.println("Zbar is " + zbar);
                    Matrix d;
                    Matrix A = Matrix.random(n, 3);
                    int j1 = 0;
                    for (double[] inttrk : Trk1) {
                        A.set(j1, 0, inttrk[0] - xbar);
                        A.set(j1, 1, inttrk[1] - ybar);
                        A.set(j1, 2, inttrk[2] - zbar);
                        j1++;
                    }

                    //           System.out.println("Matrix A");
                    //           A.print(9, 6);
                    A.svd();
                    SingularValueDecomposition s = A.svd();
                    Matrix S = s.getS();
                    //         System.out.println("S Matrix");
                    //         S.print(9, 6);
                    Matrix V = s.getV();
                    //         System.out.println("V Matrix");
                    //         V.print(9, 6);
                    d = V.getMatrix(0, 2, 0, 0);
                    double[] dd;
                    dd = new double[3];

                    dd[0] = d.get(0, 0);
                    dd[1] = d.get(1, 0);
                    dd[2] = d.get(2, 0);
                    double nd = Math.sqrt((Math.pow(dd[0], 2)) + (Math.pow(dd[1], 2)) + (Math.pow(dd[2], 2)));

                    for (double[] inttrk : Trk1) {
                        double t1 = (inttrk[2] - zbar) / dd[2];
                        double restrk1[];
                        restrk1 = new double[3];
                        restrk1[0] = xbar + (t1) * dd[0] - inttrk[0];
                        restrk1[1] = ybar + (t1) * dd[1] - inttrk[1];
                        restrk1[2] = zbar + (t1) * dd[2] - inttrk[2];
                        //aida.histogram1D("X Res Trk1").fill(restrk1[0]);
                        //aida.histogram1D("Y Res Trk1").fill(restrk1[1]);
                    }

                    List<double[]> Trk2 = new ArrayList<double[]>();
                    for (int i = 0; i < 100; i++) {
                        double z = BeamlineConstants.HARP_POSITION_TESTRUN - zr + (zs * i);
                        double[] posvec2 = new double[3];

                        if (isMC) {
                            posvec2[0] = slt2.getYZAtX(z)[0];
                            posvec2[1] = slt2.getYZAtX(z)[1];
                            posvec2[2] = z;
                        } else {
                            Hep3Vector[] trk2atz = hpstrack2.getPositionAtZMap(100, z, 1);
                            posvec2[0] = trk2atz[0].x();
                            posvec2[1] = trk2atz[0].y();
                            posvec2[2] = z;
                        }
                        Trk2.add(posvec2);
                        //      System.out.println("Components");
                        //      System.out.println(posvec2[0]);
                        //      System.out.println(posvec2[1]);
                        //      System.out.println(posvec2[2]);
                    }
                    double xbar2 = 0;
                    double ybar2 = 0;
                    double zbar2 = 0;
                    int n2 = 0;
                    for (double[] trk : Trk2) {
                        xbar2 = xbar2 + trk[0];
                        ybar2 = ybar2 + trk[1];
                        zbar2 = zbar2 + trk[2];
                        n2 = n2 + 1;
                    }
                    xbar2 = xbar2 / n2;
                    ybar2 = ybar2 / n2;
                    zbar2 = zbar2 / n2;
                    Matrix d2;
                    Matrix A2 = Matrix.random(n, 3);

                    int j2 = 0;
                    for (double[] inttrk : Trk2) {
                        A2.set(j2, 0, inttrk[0] - xbar2);
                        A2.set(j2, 1, inttrk[1] - ybar2);
                        A2.set(j2, 2, inttrk[2] - zbar2);
                        j2++;
                    }

                    A2.svd();
                    SingularValueDecomposition s2 = A2.svd();
                    Matrix V2 = s2.getV();
                    d2 = V2.getMatrix(0, 2, 0, 0);
                    double[] d22;
                    d22 = new double[3];
                    d22[0] = d2.get(0, 0);
                    d22[1] = d2.get(1, 0);
                    d22[2] = d2.get(2, 0);
                    double nd2 = Math.sqrt((Math.pow(d22[0], 2)) + (Math.pow(d22[1], 2)) + (Math.pow(d22[2], 2)));

                    for (double[] inttrk : Trk2) {
                        double t2 = (inttrk[2] - zbar2) / d22[2];
                        double restrk2[];
                        restrk2 = new double[3];
                        restrk2[0] = xbar2 + (t2) * d22[0] - inttrk[0];
                        restrk2[1] = ybar2 + (t2) * d22[1] - inttrk[1];
                        restrk2[2] = zbar2 + (t2) * d22[2] - inttrk[2];
                        //aida.histogram1D("X Res Trk2").fill(restrk2[0]);
                        //aida.histogram1D("Y Res Trk2").fill(restrk2[1]);

                    }

                    //solution for intersection below!! 

                    //starting with costant matrix b
                    double x11 = Math.pow(nd, 2) - Math.pow(dd[0], 2);
                    double y11 = Math.pow(nd, 2) - Math.pow(dd[1], 2);
                    double z11 = Math.pow(nd, 2) - Math.pow(dd[2], 2);
                    double x22 = Math.pow(nd2, 2) - Math.pow(d22[0], 2);
                    double y22 = Math.pow(nd2, 2) - Math.pow(d22[1], 2);
                    double z22 = Math.pow(nd2, 2) - Math.pow(d22[2], 2);
                    double xy1 = -1 * dd[0] * dd[1];
                    double xz1 = -1 * dd[0] * dd[2];
                    double yz1 = -1 * dd[1] * dd[2];
                    double xy2 = -1 * d22[0] * d22[1];
                    double xz2 = -1 * d22[0] * d22[2];
                    double yz2 = -1 * d22[1] * d22[2];
                    Matrix Intersect = Matrix.random(6, 3);
                    Intersect.set(0, 0, x11);
                    Intersect.set(1, 0, xy1);
                    Intersect.set(2, 0, xz1);
                    Intersect.set(0, 1, xy1);
                    Intersect.set(1, 1, y11);
                    Intersect.set(2, 1, yz1);
                    Intersect.set(0, 2, xz1);
                    Intersect.set(1, 2, yz1);
                    Intersect.set(2, 2, z11);

                    Intersect.set(3, 0, x22);
                    Intersect.set(4, 0, xy2);
                    Intersect.set(5, 0, xz2);
                    Intersect.set(3, 1, xy2);
                    Intersect.set(4, 1, y22);
                    Intersect.set(5, 1, yz2);
                    Intersect.set(3, 2, xz2);
                    Intersect.set(4, 2, yz2);
                    Intersect.set(5, 2, z22);
                    Matrix b = Matrix.random(6, 1);
                    b.set(0, 0, (x11 * xbar) + (xy1 * ybar) + (xz1 * zbar));
                    b.set(1, 0, (y11 * ybar) + (xy1 * xbar) + (yz1 * zbar));
                    b.set(2, 0, (z11 * zbar) + (xz1 * xbar) + (yz1 * ybar));
                    b.set(3, 0, (x22 * xbar2) + (xy2 * ybar2) + (xz2 * zbar2));
                    b.set(4, 0, (y22 * ybar2) + (xy2 * xbar2) + (yz2 * zbar2));
                    b.set(5, 0, (z22 * zbar2) + (xz2 * xbar2) + (yz2 * ybar2));

                    Intersect.svd();
                    SingularValueDecomposition s3 = Intersect.svd();
                    Matrix Vint = s3.getV();
                    Matrix Uint = s3.getU();
                    Matrix Sint = s3.getS();
                    //       System.out.println("S Matrix");
                    //       Sint.print(9, 6);
                    Matrix VT = Vint.transpose();
                    Matrix UT = Uint.transpose();
                    Matrix SI = Sint.inverse();
                    //      System.out.println("Inverted S Matrix");
                    //      SI.print(9, 6);
                    Matrix C1 = VT.times(SI);
                    Matrix C2 = C1.times(UT);
                    Matrix C = C2.times(b);
                    C.print(9, 6);
                    aida.histogram1D("XVertex").fill(C.get(0, 0));
                    aida.histogram1D("YVertex").fill(C.get(1, 0));
                    aida.histogram1D("ZVertex").fill(C.get(2, 0));

                    aida.histogram2D("X v Y").fill(C.get(0, 0), C.get(1, 0));
                    aida.histogram2D("X v Z").fill(C.get(0, 0), C.get(2, 0));
                    aida.histogram2D("Y v Z").fill(C.get(1, 0), C.get(2, 0));

                    double zint = C.get(2, 0);
                    double t1 = (zint - zbar) / dd[2];
                    double t2 = (zint - zbar2) / d22[2];
                    double postrk1[];
                    postrk1 = new double[3];
                    postrk1[0] = xbar + (t1) * dd[0];
                    postrk1[1] = ybar + (t1) * dd[1];
                    postrk1[2] = zbar + (t1) * dd[2];
                    double postrk2[];
                    postrk2 = new double[3];
                    postrk2[0] = xbar2 + (t2) * d22[0];
                    postrk2[1] = ybar2 + (t2) * d22[1];
                    postrk2[2] = zbar2 + (t2) * d22[2];
                    double distance = Math.sqrt(Math.pow(postrk2[0] - postrk1[0], 2) + Math.pow(postrk2[1] - postrk1[1], 2) + Math.pow(postrk2[2] - postrk1[2], 2));
                    //     double distancex = Math.sqrt(Math.pow(postrk2[0] - postrk1[0], 2));
                    //   double distancey = Math.sqrt(Math.pow(postrk2[1] - postrk1[1], 2));
                    aida.histogram1D("Distance btwn Trks @ Solution").fill(distance);

                    double pxE, pyE, pzE;
                    double pxP, pyP, pzP;
                    Hep3Vector[] trkatconvE;
                    Hep3Vector[] trkatconvP;
                    if (trk1.getCharge() > 0) {
                        pxP = trk1.getPX();
                        pyP = trk1.getPY();
                        pzP = trk1.getPZ();
                        pxE = trk2.getPX();
                        pyE = trk2.getPY();
                        pzE = trk2.getPZ();
                        trkatconvP = trkatconver1;
                        trkatconvE = trkatconver2;
                    } else {
                        pxP = trk2.getPX();
                        pyP = trk2.getPY();
                        pzP = trk2.getPZ();
                        pxE = trk1.getPX();
                        pyE = trk1.getPY();
                        pzE = trk1.getPZ();
                        trkatconvP = trkatconver2;
                        trkatconvE = trkatconver1;
                    }
                    double vX = C.get(0, 0);
                    double vY = C.get(1, 0);
                    double vZ = C.get(2, 0);
                    pw.format("%d %5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", nevt, pxE, pyE, pzE,trkatconvE[0].x(),trkatconvE[0].y(),trkatconvE[0].z());
                    pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", pxP, pyP, pzP,trkatconvP[0].x(),trkatconvP[0].y(),trkatconvP[0].z());
                    pw.format("%5.5f %5.5f %5.5f %5.5f ",distance,vX,vY,vZ);
                    pw.println();

                    if (trk1.getPX() > 0.25 && trk2.getPX() > 0.25 && Math.abs(C.get(1, 0)) < 6.0) {
                        aida.histogram1D("XVertex with y cut").fill(C.get(0, 0));
                        aida.histogram1D("YVertex with y cut").fill(C.get(1, 0));
                        aida.histogram1D("ZVertex with y cut").fill(C.get(2, 0));
                        aida.histogram1D("Distance btwn Trks with y cut").fill(distance);
                    }
                    double tt1 = (BeamlineConstants.HARP_POSITION_TESTRUN - zbar) / dd[2]; //target
                    double tt2 = (BeamlineConstants.HARP_POSITION_TESTRUN - zbar2) / d22[2]; //target
                    double postrk1att[];
                    postrk1att = new double[3]; //target
                    postrk1att[0] = xbar + (tt1) * dd[0];
                    postrk1att[1] = ybar + (tt1) * dd[1];
                    postrk1att[2] = zbar + (tt1) * dd[2];
                    double postrk2att[]; //target
                    postrk2att = new double[3];
                    postrk2att[0] = xbar2 + (tt2) * d22[0];
                    postrk2att[1] = ybar2 + (tt2) * d22[1];
                    postrk2att[2] = zbar2 + (tt2) * d22[2];

                    //aida.histogram1D("Trk1 X @ Target").fill(postrk1att[0]);
                    //aida.histogram1D("Trk1 Y @ Target").fill(postrk1att[1]);
                    //aida.histogram1D("Trk2 X @ Target").fill(postrk2att[0]);
                    //aida.histogram1D("Trk2 Y @ Target").fill(postrk2att[1]);
                    double distanceatt = Math.sqrt(Math.pow(postrk2att[0] - postrk1att[0], 2) + Math.pow(postrk2att[1] - postrk1att[1], 2) + Math.pow(postrk2att[2] - postrk1att[2], 2));
                    // double zdiff = postrk2att[2] - postrk1att[2];
                    //aida.histogram1D("Distance btwn Trks @ Target").fill(distanceatt);
                    // aida.histogram1D("Z Diff").fill(zdiff);


                    double uncerty = Math.abs((m1 - m2) * zint + (b1 - b2));
                    //   double uncertx1 = Math.sqrt(uncertx1sq);
                    double uncertz1 = Math.sqrt(zr);
                    //   double uncertx2 = Math.sqrt(uncertx2sq);
                    double uncertz2 = Math.sqrt(zr);
                    //   aida.histogram1D("Uncert X Trk 1").fill(uncertx1);
                    //aida.histogram1D("Uncert Y Trk 1").fill(uncerty);
                    //aida.histogram1D("Uncert Z Trk 1").fill(uncertz1);
                    // aida.histogram1D("Uncert X Trk 2").fill(uncerty2);
                    //aida.histogram1D("Uncert Y Trk 2").fill(uncerty);
                    //aida.histogram1D("Uncert Z Trk 2").fill(uncertz2);
                    boolean yzbump1 = false;
                    if (-18 < C.get(1, 0) && C.get(1, 0) < -12 && -645 < C.get(2, 0) && C.get(2, 0) < -600) {
                        yzbump1 = true;
                    }
                    boolean yzbump2 = false;
                    if (-4 < C.get(1, 0) && C.get(1, 0) < 4 && -720 < C.get(2, 0) && C.get(2, 0) < -605) {
                        yzbump2 = true;
                    }
                    boolean xybump1 = false;
                    if (-1 < C.get(0, 0) && C.get(0, 0) < 11 && -20 < C.get(1, 0) && C.get(1, 0) < -13) {
                        xybump1 = true;
                    }
                    boolean xybump2 = false;
                    if (11 < C.get(0, 0) && C.get(0, 0) < 25 && -4 < C.get(1, 0) && C.get(1, 0) < 3) {
                        xybump2 = true;
                    }
                    int trksparity = 0;
                    if (isTrk1Top == 1 && qtrk1 == 1) { //top e+ will be far right
                        trksparity = 6;
                    }
                    if (isTrk1Top == 1 && qtrk1 == -1) { //top e- will be right of middle two
                        trksparity = 4;
                    }
                    if (isTrk1Top == -1 && qtrk1 == 1) { // bot e+ will be left of middle two
                        trksparity = 2;
                    }
                    if (isTrk1Top == -1 && qtrk1 == -1) { //bot e- will be far left
                        trksparity = 0;
                    }
                    boolean eplustop = false;
                    if ((isTrk1Top == 1 && qtrk1 == 1) || (isTrk2Top == 1 && qtrk2 == 1)) {
                        eplustop = true;
                    }
                    if (yzbump1 && xybump1) {
                        // aida.histogram1D("Little Bump Track Parity").fill(trksparity);
                    }
                    if (yzbump2 && xybump2) {
                        // aida.histogram1D("Big Bump Track Parity").fill(trksparity);
                    }
                    if (eplustop) { //read Little bump as e+ top
                        // aida.histogram1D("Little Bump Track Momenta (Px)").fill(trk1.getPX());
                        // aida.histogram1D("Little Bump Track Momenta (Py)").fill(trk1.getPY());
                        // aida.histogram1D("Little Bump Track Momenta (Pz)").fill(trk1.getPZ());
                        // aida.histogram1D("Little Bump Tracks Chi2").fill(trk1.getChi2());
                        // aida.histogram1D("Little Bump Track Momenta (Px)").fill(trk2.getPX());
                        // aida.histogram1D("Little Bump Track Momenta (Py)").fill(trk2.getPY());
                        // aida.histogram1D("Little Bump Track Momenta (Pz)").fill(trk2.getPZ());
                        // aida.histogram1D("Little Bump Tracks Chi2").fill(trk2.getChi2());
                        // aida.histogram1D("Little Bump Sum of Track's Momentums").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPX() + trk2.getPX()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
                        double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
                        double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
                        double Etrk1 = Math.sqrt(Etrk1sq);
                        double Etrk2 = Math.sqrt(Etrk2sq);
                        double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
                        aida.histogram1D("Invariant Mass").fill(Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));
                        if (qtrk1 == 1) {
                            //       aida.histogram2D("Little Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));
                        } else {
                            //      aida.histogram2D("Little Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
                        }
                        aida.histogram2D("X v Y - e+ Top").fill(C.get(0, 0), C.get(1, 0));
                        aida.histogram2D("X v Z - e+ Top").fill(C.get(0, 0), C.get(2, 0));
                        aida.histogram2D("Y v Z - e+ Top").fill(C.get(1, 0), C.get(2, 0));
                    } else { //read Big bump as e- top
//                        aida.histogram1D("Big Bump Track Momenta (Px)").fill(trk1.getPX());
//                        aida.histogram1D("Big Bump Track Momenta (Py)").fill(trk1.getPY());
//                        aida.histogram1D("Big Bump Track Momenta (Pz)").fill(trk1.getPZ());
//                        aida.histogram1D("Big Bump Tracks Chi2").fill(trk1.getChi2());
//                        aida.histogram1D("Big Bump Track Momenta (Px)").fill(trk2.getPX());
//                        aida.histogram1D("Big Bump Track Momenta (Py)").fill(trk2.getPY());
//                        aida.histogram1D("Big Bump Track Momenta (Pz)").fill(trk2.getPZ());
//                        aida.histogram1D("Big Bump Tracks Chi2").fill(trk2.getChi2());
//                        aida.histogram1D("Big Bump Sum of Track's Momentums").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPX() + trk2.getPX()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
                        double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
                        double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
                        double Etrk1 = Math.sqrt(Etrk1sq);
                        double Etrk2 = Math.sqrt(Etrk2sq);
                        double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
                        aida.histogram1D("Invariant Mass").fill(Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));
                        if (qtrk1 == 1) {
                            //  aida.histogram2D("Big Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));
                        } else {
                            // aida.histogram2D("Big Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
                        }
                        aida.histogram2D("X v Y - e- Top").fill(C.get(0, 0), C.get(1, 0));
                        aida.histogram2D("X v Z - e- Top").fill(C.get(0, 0), C.get(2, 0));
                        aida.histogram2D("Y v Z - e- Top").fill(C.get(1, 0), C.get(2, 0));
                    }
                }
                boolean check4 = true;
                if (my2 == 0) {
                    check4 = false;
                }

//                aida.histogram1D("Track Distributions").fill(isTrk2Top + isTrk1Top);
//                aida.histogram1D("Charge Distributions").fill(qtrk1 + qtrk2);


                if ((isTrk2Top + isTrk1Top) == 0) {
//                    aida.histogram1D("Split Track Momenta (Px)").fill(trk1.getPX());
//                    aida.histogram1D("Split Track Momenta (Py)").fill(trk1.getPY());
//                    aida.histogram1D("Split Track Momenta (Pz)").fill(trk1.getPZ());
//                    aida.histogram1D("Split Tracks Chi2").fill(trk1.getChi2());
//                    aida.histogram1D("Split Track Momenta (Px)").fill(trk2.getPX());
//                    aida.histogram1D("Split Track Momenta (Py)").fill(trk2.getPY());
//                    aida.histogram1D("Split Track Momenta (Pz)").fill(trk2.getPZ());
//                    aida.histogram1D("Split Tracks Chi2").fill(trk2.getChi2());
                    //     aida.histogram1D("Charge Distributions Split Tracks").fill(qtrk1 + qtrk2);
                }

                if ((isTrk2Top + isTrk1Top) == 2) {
//                    aida.histogram1D("Top-Top Track Momenta (Px)").fill(trk1.getPX());
//                    aida.histogram1D("Top-Top Track Momenta (Py)").fill(trk1.getPY());
//                    aida.histogram1D("Top-Top Track Momenta (Pz)").fill(trk1.getPZ());
//                    aida.histogram1D("Top-Top Tracks Chi2").fill(trk1.getChi2());
//                    aida.histogram1D("Top-Top Track Momenta (Px)").fill(trk2.getPX());
//                    aida.histogram1D("Top-Top Track Momenta (Py)").fill(trk2.getPY());
//                    aida.histogram1D("Top-Top Track Momenta (Pz)").fill(trk2.getPZ());
//                    aida.histogram1D("Top-Top Tracks Chi2").fill(trk2.getChi2());
                    //     aida.histogram1D("Charge Distributions Non-Split Tracks").fill(qtrk1 + qtrk2);
                }


//                if ((qtrk1 + qtrk2) == 0) {
//                    aida.histogram1D("Perpendicular Momentum").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
//
//                    if (qtrk1 == 1) {
//                        aida.histogram2D("Py+ vs. Py-").fill(trk1.getPY(), trk2.getPY());
//                        aida.histogram2D("Pz+ vs. Pz-").fill(trk1.getPZ(), trk2.getPZ());
//                        aida.histogram2D("Total P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));
//
//                    } else {
//                        aida.histogram2D("Py+ vs. Py-").fill(trk2.getPY(), trk1.getPY());
//                        aida.histogram2D("Pz+ vs. Pz-").fill(trk2.getPZ(), trk1.getPZ());
//                        aida.histogram2D("Total P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
//                    }
//                }

            }


        }
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setShowPlots(boolean showem) {
        this.showPlots = showem;
    }

    public void setOutputTextName(String output) {
        this.outputTextName = output;
    }

    public void endOfData() {

        pw.close();
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(TwoTrackAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Output");
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TwoTrackAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
