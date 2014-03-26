package org.hps.monitoring.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.evio.TriggerData;
import org.hps.util.Redrawable;
import org.hps.util.Resettable;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TriggerPlots extends Driver implements Resettable, Redrawable {

    int eventRefreshRate = 10000;
    int eventn = 0;
    //AIDAFrame plotterFrame;
    String inputCollection = "EcalCalHits";
    String clusterCollection = "EcalClusters";
    double clusterEnergyCut = 1280.0;
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter, plotter2, plotter3, plotter4, plotter5, plotter6;
    IHistogram1D topHitTimePlot, botHitTimePlot, orHitTimePlot;
    IHistogram1D topTrigTimePlot, botTrigTimePlot, orTrigTimePlot;
    IHistogram2D topTimePlot2D, botTimePlot2D, orTimePlot2D;
    IHistogram2D topClusters, botClusters, pairClusters;
    IHistogram2D noTopClusters, noBotClusters;
    IHistogram1D topClusTimePlot, botClusTimePlot, orClusTimePlot;
    IHistogram2D topClusTime2D, botClusTime2D, orClusTime2D;
    IHistogram1D topClusTimeDiff, botClusTimeDiff, orClusTimeDiff;
    IHistogram2D trigType;
    IHistogram1D simTrigTop, simTrigBot, simTrigAnd;
    IHistogram1D toptrig_cl_ecal_e_tag, toptrig_cl_ecal_emax_tag, toptrig_cl_ecal_n_tag, toptrig_cl_ecal_e_probe, toptrig_cl_ecal_e_probe_trig, toptrigtag_cl_ecal_e_probe, toptrigtag_cl_ecal_e_probe_trig;
    IHistogram1D bottrig_cl_ecal_e_tag, bottrig_cl_ecal_emax_tag, bottrig_cl_ecal_n_tag, bottrig_cl_ecal_e_probe, bottrig_cl_ecal_e_probe_trig, bottrigtag_cl_ecal_e_probe, bottrigtag_cl_ecal_e_probe_trig;

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setClusterCollection(String clusterCollection) {
        this.clusterCollection = clusterCollection;
    }

    public void setClusterEnergyCut(double clusterEnergyCut) {
        this.clusterEnergyCut = clusterEnergyCut;
    }

    @Override
    protected void detectorChanged(Detector detector) {

    	//plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS Trigger Plots");
        aida.tree().cd("/");


        plotter = aida.analysisFactory().createPlotterFactory().create("Hit Times");
        plotter.setTitle("Hit Times");
        //plotterFrame.addPlotter(plotter);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(3, 3);

        topHitTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Top", 100, 0, 100 * 4.0);
        botHitTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Bottom", 100, 0, 100 * 4.0);
        orHitTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : First Hit Time, Or", 100, 0, 100 * 4.0);

        topTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Top", 32, 0, 32);
        botTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Bottom", 32, 0, 32);
        orTrigTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Trigger Time, Or", 32, 0, 32);

        topTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Top", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);
        botTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Bottom", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);
        orTimePlot2D = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Time vs. Trig Time, Or", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);

        // Create the plotter regions.
        plotter.region(0).plot(topHitTimePlot);
        plotter.region(1).plot(botHitTimePlot);
        plotter.region(2).plot(orHitTimePlot);
        plotter.region(3).plot(topTrigTimePlot);
        plotter.region(4).plot(botTrigTimePlot);
        plotter.region(5).plot(orTrigTimePlot);
        for (int i = 0; i < 6; i++) {
            plotter.region(i).style().yAxisStyle().setParameter("scale", "log");
        }
        plotter.region(6).plot(topTimePlot2D);
        plotter.region(7).plot(botTimePlot2D);
        plotter.region(8).plot(orTimePlot2D);
        for (int i = 6; i < 9; i++) {
            plotter.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter.region(i).style().zAxisStyle().setParameter("scale", "log");
        }

        plotter2 = aida.analysisFactory().createPlotterFactory().create("Clusters");
        plotter2.setTitle("Clusters");
        //plotterFrame.addPlotter(plotter2);
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);
        plotter2.style().setParameter("hist2DStyle", "colorMap");
        plotter2.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter2.style().zAxisStyle().setParameter("scale", "log");
        plotter2.createRegions(2, 3);

        topClusters = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Clusters, Top Trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter2.region(0).plot(topClusters);
        botClusters = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Clusters, Bot Trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter2.region(1).plot(botClusters);
        pairClusters = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Clusters, Pair Trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter2.region(2).plot(pairClusters);
        noTopClusters = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Clusters, No Top Trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter2.region(3).plot(noTopClusters);
        noBotClusters = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Clusters, No Bot Trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        plotter2.region(4).plot(noBotClusters);

        topClusTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : First Cluster Time, Top", 100, 0, 100 * 4.0);
        botClusTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : First Cluster Time, Bottom", 100, 0, 100 * 4.0);
        orClusTimePlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : First Cluster Time, Or", 100, 0, 100 * 4.0);

        topClusTime2D = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time vs. Trig Time, Top", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);
        botClusTime2D = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time vs. Trig Time, Bottom", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);
        orClusTime2D = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time vs. Trig Time, Or", 101, -4.0, 100 * 4.0, 33, -1 * 4.0, 32 * 4.0);

        topClusTimeDiff = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time - Trig Time, Top", 200, -100 * 4.0, 100 * 4.0);
        botClusTimeDiff = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time - Trig Time, Bottom", 200, -100 * 4.0, 100 * 4.0);
        orClusTimeDiff = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Time - Trig Time, Or", 200, -100 * 4.0, 100 * 4.0);

        plotter3 = aida.analysisFactory().createPlotterFactory().create("Cluster Times");
        plotter3.setTitle("Cluster Times");
        //plotterFrame.addPlotter(plotter3);
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(3, 3);

        plotter3.region(0).plot(topClusTimePlot);
        plotter3.region(1).plot(botClusTimePlot);
        plotter3.region(2).plot(orClusTimePlot);
        for (int i = 0; i < 3; i++) {
            plotter3.region(i).style().yAxisStyle().setParameter("scale", "log");
        }
        plotter3.region(3).plot(topClusTime2D);
        plotter3.region(4).plot(botClusTime2D);
        plotter3.region(5).plot(orClusTime2D);
        for (int i = 3; i < 6; i++) {
            plotter3.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter3.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter3.region(i).style().zAxisStyle().setParameter("scale", "log");
        }
        plotter3.region(6).plot(topClusTimeDiff);
        plotter3.region(7).plot(botClusTimeDiff);
        plotter3.region(8).plot(orClusTimeDiff);
//        for (int i = 6; i < 9; i++) {
//            plotter3.region(i).style().yAxisStyle().setParameter("scale", "log");
//        }

        trigType = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Actual Trigger vs. Simulated Trigger", 4, 0, 4, 4, 0, 4);
        simTrigTop = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Simulated Trigger - Top-Trigger Events", 4, 0, 4);
        simTrigBot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Simulated Trigger - Bottom-Trigger Events", 4, 0, 4);
        simTrigAnd = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollection + " : Simulated Trigger - And-Trigger Events", 4, 0, 4);

        plotter4 = aida.analysisFactory().createPlotterFactory().create("Trigger Types");
        plotter4.setTitle("Trigger Types");
        //plotterFrame.addPlotter(plotter4);
        plotter4.style().dataStyle().errorBarStyle().setVisible(false);
        plotter4.style().yAxisStyle().setParameter("scale", "log");
//        plotter4.style().setParameter("hist2DStyle", "colorMap");
//        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
//        plotter4.style().zAxisStyle().setParameter("scale", "log");
//        plotter4.createRegion();
//        plotter4.region(0).plot(trigType);
        plotter4.createRegions(1, 3);

        plotter4.region(0).plot(simTrigTop);
        plotter4.region(1).plot(simTrigBot);
        plotter4.region(2).plot(simTrigAnd);

        plotter5 = aida.analysisFactory().createPlotterFactory().create("Bottom turn-on");
        plotter5.setTitle("Bottom turn-on");
        //plotterFrame.addPlotter(plotter5);
        plotter5.style().dataStyle().errorBarStyle().setVisible(false);
        plotter5.createRegions(3, 3);

        double plotEnergyRange = 5 * clusterEnergyCut;

        toptrig_cl_ecal_n_tag = aida.histogram1D("toptrig_cl_ecal_n_tag", 7, 0, 7);
        toptrig_cl_ecal_e_probe = aida.histogram1D("toptrig_cl_ecal_e_probe", 200, 0, plotEnergyRange);
        toptrigtag_cl_ecal_e_probe = aida.histogram1D("toptrigtag_cl_ecal_e_probe", 200, 0, plotEnergyRange);
        toptrig_cl_ecal_e_tag = aida.histogram1D("toptrig_cl_ecal_e_tag", 200, 0, plotEnergyRange);
        toptrig_cl_ecal_e_probe_trig = aida.histogram1D("toptrig_cl_ecal_e_probe_trig", 200, 0, plotEnergyRange);
        toptrigtag_cl_ecal_e_probe_trig = aida.histogram1D("toptrigtag_cl_ecal_e_probe_trig", 200, 0, plotEnergyRange);
        toptrig_cl_ecal_emax_tag = aida.histogram1D("toptrig_cl_ecal_emax_tag", 200, 0, plotEnergyRange);
        plotter5.region(0).plot(toptrig_cl_ecal_n_tag);
        plotter5.region(1).plot(toptrig_cl_ecal_e_probe);
        plotter5.region(2).plot(toptrigtag_cl_ecal_e_probe);
        plotter5.region(3).plot(toptrig_cl_ecal_e_tag);
        plotter5.region(4).plot(toptrig_cl_ecal_e_probe_trig);
        plotter5.region(5).plot(toptrigtag_cl_ecal_e_probe_trig);
        plotter5.region(6).plot(toptrig_cl_ecal_emax_tag);

        plotter6 = aida.analysisFactory().createPlotterFactory().create("Top turn-on");
        plotter6.setTitle("Top turn-on");
        //plotterFrame.addPlotter(plotter6);
        plotter6.style().dataStyle().errorBarStyle().setVisible(false);
        plotter6.createRegions(3, 3);

        bottrig_cl_ecal_n_tag = aida.histogram1D("bottrig_cl_ecal_n_tag", 7, 0, 7);
        bottrig_cl_ecal_e_probe = aida.histogram1D("bottrig_cl_ecal_e_probe", 200, 0, plotEnergyRange);
        bottrigtag_cl_ecal_e_probe = aida.histogram1D("bottrigtag_cl_ecal_e_probe", 200, 0, plotEnergyRange);
        bottrig_cl_ecal_e_tag = aida.histogram1D("bottrig_cl_ecal_e_tag", 200, 0, plotEnergyRange);
        bottrig_cl_ecal_e_probe_trig = aida.histogram1D("bottrig_cl_ecal_e_probe_trig", 200, 0, plotEnergyRange);
        bottrigtag_cl_ecal_e_probe_trig = aida.histogram1D("bottrigtag_cl_ecal_e_probe_trig", 200, 0, plotEnergyRange);
        bottrig_cl_ecal_emax_tag = aida.histogram1D("bottrig_cl_ecal_emax_tag", 200, 0, plotEnergyRange);
        plotter6.region(0).plot(bottrig_cl_ecal_n_tag);
        plotter6.region(1).plot(bottrig_cl_ecal_e_probe);
        plotter6.region(2).plot(bottrigtag_cl_ecal_e_probe);
        plotter6.region(3).plot(bottrig_cl_ecal_e_tag);
        plotter6.region(4).plot(bottrig_cl_ecal_e_probe_trig);
        plotter6.region(5).plot(bottrigtag_cl_ecal_e_probe_trig);
        plotter6.region(6).plot(bottrig_cl_ecal_emax_tag);

        //plotterFrame.setVisible(true);
        //plotterFrame.pack();
    }

    @Override
    public void process(EventHeader event) {
        int orTrig = 0;
        int topTrig = 0;
        int botTrig = 0;
        int pairTrig = 0;
        int orTrigTime = -1;
        int topTrigTime = -1;
        int botTrigTime = -1;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                GenericObject triggerData = triggerList.get(0);

                pairTrig = TriggerData.getAndTrig(triggerData);
                orTrig = TriggerData.getOrTrig(triggerData);
                if (orTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & orTrig) != 0) {
                            orTrigTime = i;
                            orTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
                topTrig = TriggerData.getTopTrig(triggerData);
                if (topTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & topTrig) != 0) {
                            topTrigTime = i;
                            topTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
                botTrig = TriggerData.getBotTrig(triggerData);
                if (botTrig != 0) {
                    for (int i = 0; i < 32; i++) {
                        if ((1 << (31 - i) & botTrig) != 0) {
                            botTrigTime = i;
                            botTrigTimePlot.fill(i);
                            break;
                        }
                    }
                }
            }
        }

        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
//            double maxEnergy = 0;
            double topTime = Double.POSITIVE_INFINITY;
            double botTime = Double.POSITIVE_INFINITY;
            double orTime = Double.POSITIVE_INFINITY;
            for (CalorimeterHit hit : hits) {
                if (hit.getTime() < orTime) {
                    orTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") > 0 && hit.getTime() < topTime) {
                    topTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") < 0 && hit.getTime() < botTime) {
                    botTime = hit.getTime();
                }
//                if (hit.getRawEnergy() > maxEnergy) {
//                    maxEnergy = hit.getRawEnergy();
//                }
            }
            if (orTime != Double.POSITIVE_INFINITY) {
                orHitTimePlot.fill(orTime);
                orTimePlot2D.fill(orTime, 4.0 * orTrigTime);
            }
            if (topTime != Double.POSITIVE_INFINITY) {
                topHitTimePlot.fill(topTime);
                topTimePlot2D.fill(topTime, 4.0 * topTrigTime);
            }
            if (botTime != Double.POSITIVE_INFINITY) {
                botHitTimePlot.fill(botTime);
                botTimePlot2D.fill(botTime, 4.0 * botTrigTime);
            }
        }

//        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
//            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);
//if (clusters.size()>1)            
            double topTime = -4.0;
            double botTime = -4.0;
            double orTime = -4.0;
            clusterloop:
            for (HPSEcalCluster cluster : clusters) {
//            for (CalorimeterHit hit : hits) {
                if (cluster.getEnergy() < clusterEnergyCut) {
//                if (hit.getRawEnergy() < clusterEnergyCut) {
                    continue;
                }
                CalorimeterHit hit = cluster.getSeedHit();

                if (orTime < 0 || hit.getTime() < orTime) {
                    orTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") > 0 && (topTime < 0 || hit.getTime() < topTime)) {
                    topTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") < 0 && (botTime < 0 || hit.getTime() < botTime)) {
                    botTime = hit.getTime();
                }

                if (topTrig != 0) {
                    topClusters.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                } else {
                    noTopClusters.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                }
                if (botTrig != 0) {
                    botClusters.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                } else {
                    noBotClusters.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                }
                if (pairTrig != 0) {
                    pairClusters.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                }

//                if ((botTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] < 0) || (topTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] > 0)) {
//                if (botTrig != 0 && topTrig != 0 && cluster.getEnergy() > 130 && cluster.getCalorimeterHits().size() > 1) {
//                    for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
//                        if (hit.getRawEnergy() > 130) {
//                            continue clusterloop;
//                        }
//                    }
//                    botClusters.fill(cluster.getSeedHit().getIdentifierFieldValue("ix"), cluster.getSeedHit().getIdentifierFieldValue("iy"));
//                }
            }
            if (orTime >= 0 || orTrigTime >= 0) {
                orClusTimePlot.fill(orTime);
                orClusTime2D.fill(orTime, 4.0 * orTrigTime);
                if (orTime >= 0 || orTrigTime >= 0) {
                    orClusTimeDiff.fill(orTime - orTrigTime * 4.0);
                }
            }
            if (topTime >= 0 || topTrigTime >= 0) {
                topClusTimePlot.fill(topTime);
                topClusTime2D.fill(topTime, 4.0 * topTrigTime);
                if (topTime >= 0 || topTrigTime >= 0) {
                    topClusTimeDiff.fill(topTime - topTrigTime * 4.0);
                }
            }
            if (botTime >= 0 || botTrigTime >= 0) {
                botClusTimePlot.fill(botTime);
                botClusTime2D.fill(botTime, 4.0 * botTrigTime);
                if (botTime >= 0 || botTrigTime >= 0) {
                    botClusTimeDiff.fill(botTime - botTrigTime * 4.0);
                }
            }

            int trigTypeActual, trigTypeSim;

            if (topTime < 0 && botTime < 0) {
                trigTypeSim = 0;
            } else if (topTime >= 0 && botTime < 0) {
                trigTypeSim = 1;
            } else if (topTime < 0 && botTime >= 0) {
                trigTypeSim = 2;
            } else {
                trigTypeSim = 3;
            }

            if (topTrig == 0 && botTrig == 0) {
                trigTypeActual = 0;
            } else if (topTrig != 0 && botTrig == 0) {
                trigTypeActual = 1;
                simTrigTop.fill(trigTypeSim);
            } else if (topTrig == 0 && botTrig != 0) {
                trigTypeActual = 2;
                simTrigBot.fill(trigTypeSim);
            } else {
                trigTypeActual = 3;
                simTrigAnd.fill(trigTypeSim);
            }

            trigType.fill(trigTypeSim, trigTypeActual);




            if (topTrig != 0) {

                //Find the tag
                double Etag = -999999.9;
                HPSEcalCluster cl_tag = null; //highest-E cluster in top half
                int nTag = 0; //num. clusters in top half

                //Find a probe
                double Eprobe = -999999.9;
                HPSEcalCluster cl_probe = null; //highest-E cluster in bottom half
                int nProbe = 0; //num. clusters in bottom half

                for (HPSEcalCluster cl : clusters) {
                    if (cl.getPosition()[1] > 0) { //top half
                        ++nTag;
                        toptrig_cl_ecal_e_tag.fill(cl.getEnergy());
                        if (cl.getEnergy() > Etag) {
                            Etag = cl.getEnergy();
                            cl_tag = cl;
                        }
                    }
                    if (cl.getPosition()[1] <= 0) { //bottom half
                        ++nProbe;
                        if (cl.getEnergy() > Eprobe) {
                            Eprobe = cl.getEnergy();
                            cl_probe = cl;
                        }
                    }
                }

                toptrig_cl_ecal_n_tag.fill(nTag);

                if (cl_tag != null) {
                    toptrig_cl_ecal_emax_tag.fill(cl_tag.getEnergy());
                    //use only cases where the is a single probe candidate
                    if (nProbe == 1) {
                        toptrig_cl_ecal_e_probe.fill(cl_probe.getEnergy());
                        if (botTrig != 0) {
                            toptrig_cl_ecal_e_probe_trig.fill(cl_probe.getEnergy());
                        }
                        if (cl_tag.getEnergy() > 2.0 * clusterEnergyCut) {
                            toptrigtag_cl_ecal_e_probe.fill(cl_probe.getEnergy());
                            if (botTrig != 0) {
                                toptrigtag_cl_ecal_e_probe_trig.fill(cl_probe.getEnergy());
                            }
                        }
                    }
                } //tag found
            }//topTrigger

            if (botTrig != 0) {

                //Find the tag
                double Etag = -999999.9;
                HPSEcalCluster cl_tag = null; //highest-E cluster in bottom half
                int nTag = 0; //num. clusters in top half

                //Find a probe
                double Eprobe = -999999.9;
                HPSEcalCluster cl_probe = null; //highest-E cluster in top half
                int nProbe = 0; //num. clusters in bottom half

                for (HPSEcalCluster cl : clusters) {
                    if (cl.getPosition()[1] < 0) { //bottom half
                        ++nTag;
                        bottrig_cl_ecal_e_tag.fill(cl.getEnergy());
                        if (cl.getEnergy() > Etag) {
                            Etag = cl.getEnergy();
                            cl_tag = cl;
                        }
                    }
                    if (cl.getPosition()[1] > 0) { //top half
                        ++nProbe;
                        if (cl.getEnergy() > Eprobe) {
                            Eprobe = cl.getEnergy();
                            cl_probe = cl;
                        }
                    }
                }

                bottrig_cl_ecal_n_tag.fill(nTag);

                if (cl_tag != null) {
                    bottrig_cl_ecal_emax_tag.fill(cl_tag.getEnergy());
                    //use only cases where the is a single probe candidate
                    if (nProbe == 1) {
                        bottrig_cl_ecal_e_probe.fill(cl_probe.getEnergy());
                        if (topTrig != 0) {
                            bottrig_cl_ecal_e_probe_trig.fill(cl_probe.getEnergy());
                        }
                        if (cl_tag.getEnergy() > 2.0 * clusterEnergyCut) {
                            bottrigtag_cl_ecal_e_probe.fill(cl_probe.getEnergy());
                            if (topTrig != 0) {
                                bottrigtag_cl_ecal_e_probe_trig.fill(cl_probe.getEnergy());
                            }
                        }
                    }
                } //tag found
            }//botTrigger

        }

        if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
            redraw();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void endOfData() {
        redraw();
        System.out.format("Top trigger bit: \t");
        for (int i = 0; i < 4; i++) {
            System.out.format("%d\t", simTrigTop.binEntries(i));
        }
        System.out.println();
        System.out.format("Bottom trigger bit: \t");
        for (int i = 0; i < 4; i++) {
            System.out.format("%d\t", simTrigBot.binEntries(i));
        }
        System.out.println();
        System.out.format("Both trigger bits: \t");
        for (int i = 0; i < 4; i++) {
            System.out.format("%d\t", simTrigAnd.binEntries(i));
        }
        System.out.println();

        System.out.println("Events where top fired:");
        System.out.format("Bottom fired:\t\t%d\t%d\n", simTrigAnd.binEntries(2) + simTrigAnd.binEntries(3), simTrigAnd.binEntries(0) + simTrigAnd.binEntries(1));
        System.out.format("Bottom didn't fire:\t%d\t%d\n", simTrigTop.binEntries(2) + simTrigTop.binEntries(3), simTrigTop.binEntries(0) + simTrigTop.binEntries(1));
        System.out.println("Events where bottom fired:");
        System.out.format("Top fired:\t\t%d\t%d\n", simTrigAnd.binEntries(1) + simTrigAnd.binEntries(3), simTrigAnd.binEntries(0) + simTrigAnd.binEntries(2));
        System.out.format("Top didn't fire:\t%d\t%d\n", simTrigBot.binEntries(1) + simTrigBot.binEntries(3), simTrigBot.binEntries(0) + simTrigBot.binEntries(2));
        //plotterFrame.dispose();
    }

    @Override
    public void redraw() {
        IHistogram1D heffTop = aida.histogramFactory().divide("bottom turn-on: top tag", toptrig_cl_ecal_e_probe_trig, toptrig_cl_ecal_e_probe);
        plotter5.region(7).clear();
        plotter5.region(7).style().statisticsBoxStyle().setVisible(false);
        plotter5.region(7).plot(heffTop);
        IHistogram1D heffTop2 = aida.histogramFactory().divide("bottom turn-on: top tag > " + 2.0 * clusterEnergyCut, toptrigtag_cl_ecal_e_probe_trig, toptrigtag_cl_ecal_e_probe);
        plotter5.region(8).clear();
        plotter5.region(8).style().statisticsBoxStyle().setVisible(false);
        plotter5.region(8).plot(heffTop2);

        IHistogram1D heffBot = aida.histogramFactory().divide("top turn-on: bottom tag", bottrig_cl_ecal_e_probe_trig, bottrig_cl_ecal_e_probe);
        plotter6.region(7).clear();
        plotter6.region(7).style().statisticsBoxStyle().setVisible(false);
        plotter6.region(7).plot(heffBot);
        IHistogram1D heffBot2 = aida.histogramFactory().divide("top turn-on: bottom tag > " + 2.0 * clusterEnergyCut, bottrigtag_cl_ecal_e_probe_trig, bottrigtag_cl_ecal_e_probe);
        plotter6.region(8).clear();
        plotter6.region(8).style().statisticsBoxStyle().setVisible(false);
        plotter6.region(8).plot(heffBot2);
    }

    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
}