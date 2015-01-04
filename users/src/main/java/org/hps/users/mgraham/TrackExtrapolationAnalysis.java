package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.Map;

import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**

 @author mgraham
 */
public class TrackExtrapolationAnalysis extends Driver {

    //private AIDAFrame extrapFrame;
    private AIDA aida = AIDA.defaultInstance();
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    IPlotter extrap0;
    IPlotter extrap1;
    IPlotter extrap2;
    IPlotter extrap3;
    IPlotter extrap4;
    IPlotter extrap5;
    IPlotter extrap6;
    IPlotter extrap7;
    IPlotter extrap8;
    IPlotter extrap9;
    IPlotter extrap10;
    IPlotter extrap11;
    IPlotter extrap12;
    IPlotter extrap13;
    IAnalysisFactory fac = aida.analysisFactory();

    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");
        //extrapFrame = new AIDAFrame();
        //extrapFrame.setTitle("Extrapolation Debugging Plots");
        makeExtrapolationPlots();

        //extrapFrame.pack();
        //extrapFrame.setVisible(true);

    }

    public TrackExtrapolationAnalysis() {
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");


        List<Track> tracks = event.get(Track.class, trackCollectionName);
        for (Track trk : tracks) {
            double trackP = trk.getPX();
            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HelixConverter converter = new HelixConverter(0);
            StraightLineTrack slt = converter.Convert(ht);

            Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trk);
            Hep3Vector extendAtConverter = TrackUtils.extrapolateTrack(trk,BeamlineConstants.HARP_POSITION_TESTRUN);


            int isTop = -1;
            if (trk.getTrackerHits().get(0).getPosition()[2] > 0)
                isTop = 0;//make plot look pretty
            int charge = trk.getCharge();
            if (charge > 0)
                charge = 0;//make plot look pretty
//            System.out.println("Charge = " + charge + "; isTop = " + isTop);

            HPSTrack hpstrk=null;
                hpstrk = new HPSTrack(ht);
//            Hep3Vector posAtConv = hpstrk.getPositionAtZ(zAtConverter, -101, -100, 0.1);
            Hep3Vector posAtConv = hpstrk.getPositionAtZMap(100,BeamlineConstants.HARP_POSITION_TESTRUN , 5.0)[0];
            double useThisx=posAtConv.x();
            double useThisy=posAtConv.y();
            
//             double useThisx=extendAtConverter.y();
//            double useThisy=extendAtConverter.z();
            
            aida.histogram1D("New X (mm) @ Converter").fill(useThisx);  //this is in the JLAB frame already
            aida.histogram1D("New Y (mm) @ Converter").fill(useThisy);
            if (isTop == 0) {
                aida.histogram1D("Top X (mm) @ Converter").fill(useThisx);  //this is in the JLAB frame already
                aida.histogram1D("Top Y (mm) @ Converter").fill(useThisy);
            } else {
                aida.histogram1D("Bottom X (mm) @ Converter").fill(useThisx);  //this is in the JLAB frame already
                aida.histogram1D("Bottom Y (mm) @ Converter").fill(useThisy);
            }
            if (charge < 0) {
                aida.histogram1D("Negative X (mm) @ Converter").fill(useThisx);  //this is in the JLAB frame already
                aida.histogram1D("Negative Y (mm) @ Converter").fill(useThisy);
            }
//            Hep3Vector posAtConvShort = hpstrk.getPositionAtZ(zAtConverter, -0.1, 0, 0.01);
            Hep3Vector posAtConvShort = hpstrk.getPositionAtZMap(0,BeamlineConstants.HARP_POSITION_TESTRUN, 5.0)[0];
            aida.histogram2D("Extrapolated X: short vs long fringe").fill(posAtConvShort.x(), posAtConv.x());
            aida.histogram2D("Extrapolated Y: short vs long fringe").fill(posAtConvShort.y(), posAtConv.y());

            aida.histogram2D("Extrapolated X: HPS vs SLT").fill(posAtConv.x(), slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]);
            aida.histogram2D("Extrapolated Y: HPS vs SLT").fill(posAtConv.y(), slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]);

            aida.histogram2D("Extrapolated X: Extend vs SLT").fill(extendAtConverter.y(), slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]);
            aida.histogram2D("Extrapolated Y: Extend vs SLT").fill(extendAtConverter.z(), slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]);

            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalCollectionName);
            HPSEcalCluster clust = findClosestCluster(posAtEcal, clusters);

            if (clust != null) {
 //               System.out.println("Cluster Position = ("+clust.getPosition()[0]+","+clust.getPosition()[1]+","+clust.getPosition()[2]+")");
                double minFringe=BeamlineConstants.DIPOLE_EDGE_TESTRUN-10;
                double maxFringe=BeamlineConstants.DIPOLE_EDGE_TESTRUN+50;
//                Hep3Vector posAtEcalHPS = hpstrk.getPositionAtZ(zCluster, minFringe,maxFringe, 0.1);
//                Hep3Vector posAtEcalHPS = hpstrk.getPositionAtZMap(750,zCluster, 5.0);
                double zCluster=clust.getPosition()[2];
 //               double zCluster=1450.0;
                 Hep3Vector posAtEcalHPS = hpstrk.getPositionAtZMap(750,zCluster, 5.0)[0];
                Hep3Vector posAtEcalExtend= TrackUtils.extrapolateTrack(trk,zCluster);
                aida.histogram2D("ECal Extrapolation X :  HPS vs Extend").fill( posAtEcalExtend.y(),posAtEcalHPS.x()-posAtEcalExtend.y());
                aida.histogram2D("ECal Extrapolation Y :  HPS vs Extend").fill( posAtEcalExtend.z(),posAtEcalHPS.y()-posAtEcalExtend.z());
                double dX = posAtEcalHPS.x() - clust.getPosition()[0];
                double dY = posAtEcalHPS.y() - clust.getPosition()[1];
//                double dX = posAtEcalExtend.y() - clust.getPosition()[0];
//                double dY = posAtEcalExtend.z() - clust.getPosition()[1];
                
                
                aida.histogram2D("ECal Extrapolation HPS vs Cluster X").fill( clust.getPosition()[0],posAtEcalHPS.x() - clust.getPosition()[0]);
                aida.histogram2D("ECal Extrapolation HPS vs Cluster Y").fill( clust.getPosition()[1],posAtEcalHPS.y() - clust.getPosition()[1]);

                if (isTop == 0) {
                    if (charge < 0) {
                        aida.histogram1D("Track - Cluster X:  Top -ive").fill(dX);
                        aida.histogram1D("Track - Cluster Y:  Top -ive").fill(dY);
                    } else {
                        aida.histogram1D("Track - Cluster X:  Top +ive").fill(dX);
                        aida.histogram1D("Track - Cluster Y:  Top +ive").fill(dY);
                    }
                } else {
                    if (charge < 0) {
                        aida.histogram1D("Track - Cluster X:  Bottom -ive").fill(dX);
                        aida.histogram1D("Track - Cluster Y:  Bottom -ive").fill(dY);
                    } else {
                        aida.histogram1D("Track - Cluster X:  Bottom +ive").fill(dX);
                        aida.histogram1D("Track - Cluster Y:  Bottom +ive").fill(dY);
                    }
                }

            }
            double zmin = 0;
            double zmax = 500.0;
            int nstep = 100;
            Map<Integer, Double[]> traj = hpstrk.trackTrajectory(zmin, zmax, nstep);
            Map<Integer, Double[]> dir = hpstrk.trackDirection(zmin, zmax, nstep);
            for (int i = 0; i < nstep; i++) {
                Double[] xyz = traj.get(i);
                Double[] dxdyz = dir.get(i);
//                System.out.println("z = " + xyz[2]
//                        + "  (x,y) = (" + xyz[0] + "," + xyz[1] + ")"
//                        + "  (dx,dy) = (" + dxdyz[0] + "," + dxdyz[1] + ")");
                if (i == 0) {
                    aida.histogram2D("Track @ 0: X vs dX").fill(xyz[0], dxdyz[0]);
                    aida.histogram2D("Track @ 0: Y vs dY").fill(xyz[1], dxdyz[1]);
                    if (isTop == 0) {
                        if (charge < 0) {
                            aida.histogram2D("X vs dX:  Top -ive").fill(xyz[0], dxdyz[0]);
                            aida.histogram2D("X vs P:  Top -ive").fill(xyz[0], trackP);
                            aida.histogram2D("dX vs P:  Top -ive").fill(dxdyz[0], trackP);
                        } else {
                            aida.histogram2D("X vs dX:  Top +ive").fill(xyz[0], dxdyz[0]);
                            aida.histogram2D("X vs P:  Top +ive").fill(xyz[0], trackP);
                            aida.histogram2D("dX vs P:  Top +ive").fill(dxdyz[0], trackP);
                        }
                    } else {
                        if (charge < 0) {
                            aida.histogram2D("X vs dX:  Bottom -ive").fill(xyz[0], dxdyz[0]);
                            aida.histogram2D("X vs P:  Bottom -ive").fill(xyz[0], trackP);
                            aida.histogram2D("dX vs P:  Bottom -ive").fill(dxdyz[0], trackP);
                        } else {
                            aida.histogram2D("X vs dX:  Bottom +ive").fill(xyz[0], dxdyz[0]);
                            aida.histogram2D("X vs P:  Bottom +ive").fill(xyz[0], trackP);
                            aida.histogram2D("dX vs P:  Bottom +ive").fill(dxdyz[0], trackP);
                        }
                    }
                }
                aida.histogram2D("Track Trajectory:  Z vs X").fill(xyz[2], xyz[0]);
                aida.histogram2D("Track Trajectory:  Z vs Y").fill(xyz[2], xyz[1]);
                aida.histogram2D("Track Direction:  Z vs dX").fill(dxdyz[2], dxdyz[0]);
                aida.histogram2D("Track Direction:  Z vs dY").fill(dxdyz[2], dxdyz[1]);
            }

        }

    }

    private void makeExtrapolationPlots() {

        extrap0 = fac.createPlotterFactory().create("HPS Tracking Plots");
        extrap0.setTitle("New Track extrapolation");
        //extrapFrame.addPlotter(extrap0);
        IPlotterStyle style0 = extrap0.style();
        style0.dataStyle().fillStyle().setColor("yellow");
        style0.dataStyle().errorBarStyle().setVisible(false);
        extrap0.createRegions(2, 4);
        IHistogram1D xAtConverterNew = aida.histogram1D("New X (mm) @ Converter", 50, -20, 80);
        IHistogram1D yAtConverterNew = aida.histogram1D("New Y (mm) @ Converter", 50, -20, 20);
        IHistogram1D xAtCollNew = aida.histogram1D("Top X (mm) @ Converter", 50, -20, 80);
        IHistogram1D yAtCollNew = aida.histogram1D("Top Y (mm) @ Converter", 50, -20, 20);
        IHistogram1D xAtEcalNew = aida.histogram1D("Bottom X (mm) @ Converter", 50, -20, 80);
        IHistogram1D yAtEcalNew = aida.histogram1D("Bottom Y (mm) @ Converter", 50, -20, 20);
        IHistogram1D xAtEcal2New = aida.histogram1D("Negative X (mm) @ Converter", 50, -20, 80);
        IHistogram1D yAtEcal2New = aida.histogram1D("Negative Y (mm) @ Converter", 50, -20, 20);

        extrap0.region(0).plot(xAtConverterNew);
        extrap0.region(4).plot(yAtConverterNew);
        extrap0.region(1).plot(xAtCollNew);
        extrap0.region(5).plot(yAtCollNew);
        extrap0.region(2).plot(xAtEcalNew);
        extrap0.region(6).plot(yAtEcalNew);
        extrap0.region(3).plot(xAtEcal2New);
        extrap0.region(7).plot(yAtEcal2New);

        extrap1 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap1.setTitle("HPSTrack Plots");
        //extrapFrame.addPlotter(extrap1);
        set2DStyle(extrap1.style());
        extrap1.createRegions(1, 2);

//        IHistogram2D quadrants = aida.histogram2D("Charge vs Slope", 2, -1, 1, 2, -1, 1);
        IHistogram2D plot11 = aida.histogram2D("Extrapolated X: short vs long fringe", 50, -50, 50, 50, -50, 50);
        IHistogram2D plot12 = aida.histogram2D("Extrapolated Y: short vs long fringe", 50, -20, 20, 50, -20, 20);
        extrap1.region(0).plot(plot11);
        extrap1.region(1).plot(plot12);


        extrap2 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap2.setTitle("HPSTrack vs SLT Plots");
        //extrapFrame.addPlotter(extrap2);
        set2DStyle(extrap2.style());
        extrap2.createRegions(1, 2);

//        IHistogram2D quadrants = aida.histogram2D("Charge vs Slope", 2, -1, 1, 2, -1, 1);
        IHistogram2D plot21 = aida.histogram2D("Extrapolated X: HPS vs SLT", 50, -50, 50, 50, -50, 50);
        IHistogram2D plot22 = aida.histogram2D("Extrapolated Y: HPS vs SLT", 50, -20, 20, 50, -20, 20);
        extrap2.region(0).plot(plot21);
        extrap2.region(1).plot(plot22);


        extrap3 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap3.setTitle("Extend vs SLT Plots");
        //extrapFrame.addPlotter(extrap3);
        set2DStyle(extrap3.style());
        extrap3.createRegions(1, 2);

        IHistogram2D plot31 = aida.histogram2D("Extrapolated X: Extend vs SLT", 50, -50, 50, 50, -50, 50);
        IHistogram2D plot32 = aida.histogram2D("Extrapolated Y: Extend vs SLT", 50, -20, 20, 50, -20, 20);
        extrap3.region(0).plot(plot31);
        extrap3.region(1).plot(plot32);


        extrap4 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap4.setTitle("Nominal Track Trajectory");
        //extrapFrame.addPlotter(extrap4);
        set2DStyle(extrap4.style());
        extrap4.createRegions(1, 2);

        IHistogram2D plot41 = aida.histogram2D("Track Trajectory:  Z vs X", 100, 0, 500, 100, -100, 100);
        IHistogram2D plot42 = aida.histogram2D("Track Trajectory:  Z vs Y", 100, 0, 500, 100, -75, 75);
        extrap4.region(0).plot(plot41);
        extrap4.region(1).plot(plot42);

        extrap5 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap5.setTitle("Nominal Direction");
        //extrapFrame.addPlotter(extrap5);
        set2DStyle(extrap5.style());
        extrap5.createRegions(1, 2);

        IHistogram2D plot51 = aida.histogram2D("Track Direction:  Z vs dX", 100, 0, 500, 100, -0.1, 0.1);
        IHistogram2D plot52 = aida.histogram2D("Track Direction:  Z vs dY", 100, 0, 500, 100, -0.1, 0.1);
        extrap5.region(0).plot(plot51);
        extrap5.region(1).plot(plot52);



        extrap7 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap7.setTitle("Track @ 0");
        //extrapFrame.addPlotter(extrap7);
        set2DStyle(extrap7.style());

        extrap7.createRegions(1, 2);

        IHistogram2D plot71 = aida.histogram2D("Track @ 0: X vs dX", 50, -50, 50, 50, -0.04, 0.04);
        IHistogram2D plot72 = aida.histogram2D("Track @ 0: Y vs dY", 50, -50, 50, 50, -0.04, 0.04);
        extrap7.region(0).plot(plot71);
        extrap7.region(1).plot(plot72);


        extrap8 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap8.setTitle("Track X vs dX");
        //extrapFrame.addPlotter(extrap8);
        set2DStyle(extrap8.style());

        extrap8.createRegions(2, 2);

        IHistogram2D plot81 = aida.histogram2D("X vs dX:  Top -ive", 50, -50, 50, 50, -0.04, 0.04);
        IHistogram2D plot82 = aida.histogram2D("X vs dX:  Bottom -ive", 50, -50, 50, 50, -0.04, 0.04);
        IHistogram2D plot83 = aida.histogram2D("X vs dX:  Top +ive", 50, -50, 50, 50, -0.04, 0.04);
        IHistogram2D plot84 = aida.histogram2D("X vs dX:  Bottom +ive", 50, -50, 50, 50, -0.04, 0.04);
        extrap8.region(0).plot(plot81);
        extrap8.region(1).plot(plot82);
        extrap8.region(2).plot(plot83);
        extrap8.region(3).plot(plot84);

        extrap9 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap9.setTitle("Track X vs P");
        //extrapFrame.addPlotter(extrap9);
        set2DStyle(extrap9.style());

        extrap9.createRegions(2, 2);

        IHistogram2D plot91 = aida.histogram2D("X vs P:  Top -ive", 50, -50, 50, 50, 0.5, 2.0);
        IHistogram2D plot92 = aida.histogram2D("X vs P:  Bottom -ive", 50, -50, 50, 50, 0.5, 2.0);
        IHistogram2D plot93 = aida.histogram2D("X vs P:  Top +ive", 50, -50, 50, 50, 0.5, 2.0);
        IHistogram2D plot94 = aida.histogram2D("X vs P:  Bottom +ive", 50, -50, 50, 50, 0.5, 2.0);
        extrap9.region(0).plot(plot91);
        extrap9.region(1).plot(plot92);
        extrap9.region(2).plot(plot93);
        extrap9.region(3).plot(plot94);

        extrap10 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap10.setTitle("Track dX vs P");
        //extrapFrame.addPlotter(extrap10);
        set2DStyle(extrap10.style());

        extrap10.createRegions(2, 2);

        IHistogram2D plot101 = aida.histogram2D("dX vs P:  Top -ive", 50, -0.04, 0.04, 50, 0.5, 2.0);
        IHistogram2D plot102 = aida.histogram2D("dX vs P:  Bottom -ive", 50, -0.04, 0.04, 50, 0.5, 2.0);
        IHistogram2D plot103 = aida.histogram2D("dX vs P:  Top +ive", 50, -0.04, 0.04, 50, 0.5, 2.0);
        IHistogram2D plot104 = aida.histogram2D("dX vs P:  Bottom +ive", 50, -0.04, 0.04, 50, 0.5, 2.0);
        extrap10.region(0).plot(plot101);
        extrap10.region(1).plot(plot102);
        extrap10.region(2).plot(plot103);
        extrap10.region(3).plot(plot104);



        extrap6 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap6.setTitle("@ ECal HPS vs Extend");
        //extrapFrame.addPlotter(extrap6);
        set2DStyle(extrap6.style());

        extrap6.createRegions(1, 2);

        IHistogram2D plot61 = aida.histogram2D("ECal Extrapolation X :  HPS vs Extend", 50, -350, 350, 50, -50, 50);
        IHistogram2D plot62 = aida.histogram2D("ECal Extrapolation Y :  HPS vs Extend", 50, -100, 100, 50, -20, 20);
        extrap6.region(0).plot(plot61);
        extrap6.region(1).plot(plot62);

        extrap11 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap11.setTitle("@ ECal HPS vs Cluster");
        //extrapFrame.addPlotter(extrap11);
        set2DStyle(extrap11.style());

        extrap11.createRegions(1, 2);

        IHistogram2D plot111 = aida.histogram2D("ECal Extrapolation HPS vs Cluster X", 50, -350, 350, 50, -50, 50);
        IHistogram2D plot112 = aida.histogram2D("ECal Extrapolation HPS vs Cluster Y", 50, -100, 100, 50, -20, 20);
        extrap11.region(0).plot(plot111);
        extrap11.region(1).plot(plot112);



        extrap12 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap12.setTitle("Cluster Resid X");
        //extrapFrame.addPlotter(extrap12);
        set1DStyle(extrap10.style());

        extrap12.createRegions(2, 2);

        IHistogram1D plot121 = aida.histogram1D("Track - Cluster X:  Top -ive", 50, -50, 50);
        IHistogram1D plot122 = aida.histogram1D("Track - Cluster X:  Bottom -ive", 50, -50, 50);
        IHistogram1D plot123 = aida.histogram1D("Track - Cluster X:  Top +ive", 50, -50, 50);
        IHistogram1D plot124 = aida.histogram1D("Track - Cluster X:  Bottom +ive", 50, -50, 50);
        extrap12.region(0).plot(plot121);
        extrap12.region(1).plot(plot122);
        extrap12.region(2).plot(plot123);
        extrap12.region(3).plot(plot124);


        extrap13 = fac.createPlotterFactory().create("Extrapolation Debug Plots");
        extrap13.setTitle("Cluster Resid Y");
        //extrapFrame.addPlotter(extrap13);
        set1DStyle(extrap10.style());

        extrap13.createRegions(2, 2);

        IHistogram1D plot131 = aida.histogram1D("Track - Cluster Y:  Top -ive", 50, -20, 20);
        IHistogram1D plot132 = aida.histogram1D("Track - Cluster Y:  Bottom -ive", 50, -20, 20);
        IHistogram1D plot133 = aida.histogram1D("Track - Cluster Y:  Top +ive", 50, -20, 20);
        IHistogram1D plot134 = aida.histogram1D("Track - Cluster Y:  Bottom +ive", 50, -20, 20);
        extrap13.region(0).plot(plot131);
        extrap13.region(1).plot(plot132);
        extrap13.region(2).plot(plot133);
        extrap13.region(3).plot(plot134);






    }

    private void set2DStyle(IPlotterStyle style) {
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        style.statisticsBoxStyle().setVisible(false);
    }

    private void set1DStyle(IPlotterStyle style) {
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        style.statisticsBoxStyle().setVisible(true);
    }

    private HPSEcalCluster findClosestCluster(Hep3Vector posonhelix, List<HPSEcalCluster> clusters) {
        HPSEcalCluster closest = null;
        double minDist = 9999;
        for (HPSEcalCluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
//            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.y(), 2) + Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            if (dist < minDist && clEne > 50) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }
}
