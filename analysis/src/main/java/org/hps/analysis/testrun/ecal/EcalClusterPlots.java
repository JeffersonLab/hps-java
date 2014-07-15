package org.hps.analysis.testrun.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;

import java.util.List;

import org.apache.commons.math.stat.StatUtils;
import org.hps.readout.ecal.TriggerData;
import org.hps.recon.ecal.ECalUtils;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalClusterPlots extends Driver implements Resettable {

	//AIDAFrame plotterFrame;
    String inputCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter, plotter2, plotter3, plotter4;
    IHistogram1D clusterCountPlot;
    IHistogram1D clusterSizePlot;
    IHistogram1D clusterEnergyPlot;
    IHistogram1D clusterMaxEnergyPlot;
    IHistogram1D clusterTimes;
    IHistogram1D clusterTimeSigma;
    IHistogram2D edgePlot;
    int eventn = 0;
    double maxE = 5000 * ECalUtils.MeV;
    boolean logScale = false;

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    @Override
    protected void detectorChanged(Detector detector) {

    	//plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS ECal Cluster Plots");

        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create("Cluster Counts");
        plotter.setTitle("Cluster Counts");
        //plotterFrame.addPlotter(plotter);
        plotter.style().dataStyle().errorBarStyle().setVisible(false);

        // Setup plots.
        aida.tree().cd("/");
        clusterCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Size", 10, -0.5, 9.5);

        // Create the plotter regions.
        plotter.createRegions(1, 2);
        plotter.region(0).plot(clusterCountPlot);
        plotter.region(1).plot(clusterSizePlot);


        // Setup the plotter.
        plotter2 = aida.analysisFactory().createPlotterFactory().create("Cluster Energies");
        plotter2.setTitle("Cluster Energies");
        //plotterFrame.addPlotter(plotter2);
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);

        if (logScale) {
            plotter2.style().yAxisStyle().setParameter("scale", "log");
        }

        clusterEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Energy", 1000, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Maximum Cluster Energy In Event", 1000, -0.1, maxE);


        // Create the plotter regions.
        plotter2.createRegions(1, 2);
        plotter2.region(0).plot(clusterEnergyPlot);
        plotter2.region(1).plot(clusterMaxEnergyPlot);

        plotter3 = aida.analysisFactory().createPlotterFactory().create("Cluster Times");
        plotter3.setTitle("Cluster Times");
        //plotterFrame.addPlotter(plotter3);
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(1, 2);
        plotter3.style().yAxisStyle().setParameter("scale", "log");

        clusterTimes = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Times", 100, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Time Sigma", 100, 0, 50);
        plotter3.region(0).plot(clusterTimes);
        plotter3.region(1).plot(clusterTimeSigma);

        plotter4 = aida.analysisFactory().createPlotterFactory().create("Edges");
        plotter4.setTitle("Edges");
        //plotterFrame.addPlotter(plotter4);
        plotter4.style().setParameter("hist2DStyle", "colorMap");
        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter4.style().zAxisStyle().setParameter("scale", "log");
        plotter4.createRegion();

        edgePlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Pairs Across Crystal Edges", 93, -23.25, 23.25, 21, -5.25, 5.25);
        plotter4.region(0).plot(edgePlot);

        //plotterFrame.setVisible(true);
        //plotterFrame.pack();
    }

    @Override
    public void process(EventHeader event) {
        int orTrig = 0;
        int topTrig = 0;
        int botTrig = 0;
        if (event.hasCollection(TriggerData.class, "TriggerBank")) {
            List<TriggerData> triggerList = event.get(TriggerData.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                TriggerData triggerData = triggerList.get(0);

                orTrig = triggerData.getOrTrig();
                topTrig = triggerData.getTopTrig();
                botTrig = triggerData.getBotTrig();
            }
        }
        if (event.hasCollection(Cluster.class, inputCollection)) {
            List<Cluster> clusters = event.get(Cluster.class, inputCollection);
            clusterCountPlot.fill(clusters.size());
            double maxEnergy = 0;
            for (Cluster cluster : clusters) {
//                if ((botTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] < 0) || (topTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] > 0)) {
//                if ((botTrig == 0 && cluster.getPosition()[1] < 0) || (topTrig == 0 && cluster.getPosition()[1] > 0)) {
                    clusterEnergyPlot.fill(cluster.getEnergy());
                if (cluster.getEnergy() > maxEnergy) {
                    maxEnergy = cluster.getEnergy();
                }
                int size = 0;
                double[] times = new double[cluster.getCalorimeterHits().size()];
//                    System.out.format("cluster:\n");
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit.getRawEnergy() != 0) {
                        times[size] = hit.getTime();
                        clusterTimes.fill(hit.getTime());
                        size++;
//                            System.out.format("x=%d, y=%d, time=%f, energy=%f\n", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getTime(), hit.getRawEnergy());
                    }
                }
                clusterSizePlot.fill(size);
                clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(times, 0, size)));

                List<CalorimeterHit> hits = cluster.getCalorimeterHits();
                for (int i = 0; i < hits.size(); i++) {
                    CalorimeterHit hit1 = hits.get(i);
                    if (hit1.getRawEnergy() == 0) {
                        continue;
                    }
                    int x1 = hit1.getIdentifierFieldValue("ix");
                    int y1 = hit1.getIdentifierFieldValue("iy");
                    for (int j = i + 1; j < hits.size(); j++) {
                        CalorimeterHit hit2 = hits.get(j);
                        if (hit2.getRawEnergy() == 0) {
                            continue;
                        }
                        int x2 = hit2.getIdentifierFieldValue("ix");
                        int y2 = hit2.getIdentifierFieldValue("iy");
                        if ((Math.abs(x1 - x2) <= 1 || x1 * x2 == -1) && (Math.abs(y1 - y2) <= 1)) {
                            if (x1 != x2 || y1 != y2) {
                                edgePlot.fill((x1 + x2) / 2.0, (y1 + y2) / 2.0);
                            }
                        }
                    }
                }
//                }
            }
            clusterMaxEnergyPlot.fill(maxEnergy);
        } else {
            clusterCountPlot.fill(0);
        }
    }

    @Override
    public void reset() {
        clusterCountPlot.reset();
        clusterSizePlot.reset();
        clusterEnergyPlot.reset();
        clusterMaxEnergyPlot.reset();
    }

    @Override
    public void endOfData() {
    	//plotterFrame.dispose();
    }
}