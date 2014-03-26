package org.hps.monitoring.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.*;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.hps.conditions.deprecated.EcalConditions;

public class EcalEvsX extends Driver {

    String inputCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IHistogram2D EvsXPlot;
    IHistogram1D invMassPlot;
    IHistogram2D clusterPairEnergyPlot;
    IHistogram2D clusterPairPositionPlot;
    Detector detector;
    int eventn = 0;
    double targetZ = 0;

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setTargetZ(double targetZ) {
        this.targetZ = targetZ;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        this.detector = detector;

        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create("HPS ECal E vs X Plot");
        plotter.style().dataStyle().errorBarStyle().setVisible(false);

        // Setup plots.
        aida.tree().cd("/");
//        EvsXPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : E vs X", 50, -350.0, 400.0, 100, -2000, 2000);
        EvsXPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : E vs X", 50, -350.0, 400.0, 100, 0, 2.0);
        invMassPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Photon Pair Mass", 100, 0.0, 0.250);
        clusterPairEnergyPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Pair Energies", 1000, -0.1, 2.0, 1000, -0.1, 2.0);
        clusterPairPositionPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Pair Positions", 50, -350, 350, 50, -350, 350);

        // Create the plotter regions.
        plotter.createRegions(2, 2);
//        plotter.style().statisticsBoxStyle().setVisible(false);
        IPlotterStyle style = plotter.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.statisticsBoxStyle().setVisible(false);
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(1).style().yAxisStyle().setParameter("scale", "log");
        plotter.region(0).plot(EvsXPlot);
        plotter.region(1).plot(invMassPlot);
        plotter.region(2).plot(clusterPairEnergyPlot);
        style = plotter.region(2).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.statisticsBoxStyle().setVisible(false);
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.zAxisStyle().setParameter("scale", "log");
        plotter.region(3).plot(clusterPairPositionPlot);
        style = plotter.region(3).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.statisticsBoxStyle().setVisible(false);
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.zAxisStyle().setParameter("scale", "log");
        plotter.show();
    }

    @Override
    public void process(EventHeader event) {
//        int orTrig = 0;
//        int topTrig = 0;
//        int botTrig = 0;
//        if (event.hasCollection(TriggerData.class, "TriggerBank")) {
//            List<TriggerData> triggerList = event.get(TriggerData.class, "TriggerBank");
//            if (!triggerList.isEmpty()) {
//                TriggerData triggerData = triggerList.get(0);
//
//                orTrig = triggerData.getOrTrig();
//                topTrig = triggerData.getTopTrig();
//                botTrig = triggerData.getBotTrig();
//            }
//        }

        if (event.hasCollection(Cluster.class, inputCollection)) {
            List<Cluster> clusters = event.get(Cluster.class, inputCollection);
            List<Cluster> goodClusters = new ArrayList<Cluster>();
            for (Cluster cluster : clusters) {
                if (true || isGoodCluster(cluster)) {
                    goodClusters.add(cluster);
                }
            }
//            boolean left = false;
//            boolean right = false;
//            for (Cluster cluster : goodClusters) {
//                if (cluster.getPosition()[0] > 0) {
//                    right = true;
//                }
//                if (cluster.getPosition()[0] < 0) {
//                    left = true;
//                }
//            }
//            if (left && right) {
//            if (goodClusters.size()>1) {
//                for (Cluster cluster : goodClusters) {
            for (Cluster cluster : goodClusters) {
//                EvsXPlot.fill(cluster.getPosition()[0], Math.signum(cluster.getPosition()[1])*cluster.getEnergy());
                EvsXPlot.fill(cluster.getPosition()[0], cluster.getEnergy());
            }
//            }
//            if (!event.hasCollection(TrackerHit.class, "HelicalTrackHits") || event.get(TrackerHit.class, "HelicalTrackHits").isEmpty()) {
                for (int i = 0; i < goodClusters.size() - 1; i++) {
                    Cluster clus1 = goodClusters.get(i);
                    double e1 = clus1.getEnergy();
                    double x1 = clus1.getPosition()[0];
                    if (clus1.getPosition()[1] > 0 && x1 > 0) {
                        x1 = 350 - x1;
                    }
                    for (int j = i + 1; j < goodClusters.size(); j++) {
                        Cluster clus2 = goodClusters.get(j);
                        double e2 = clus2.getEnergy();
                        double x2 = clus2.getPosition()[0];
                        if (clus2.getPosition()[1] > 0 && x2 > 0) {
                            x2 = 350 - x2;
                        }
//                    if (clusters.get(i).getPosition()[1] * clusters.get(j).getPosition()[1] > 0) {
//                        continue;
//                    }
                        clusterPairEnergyPlot.fill(Math.max(e1, e2), Math.min(e1, e2));
                        clusterPairPositionPlot.fill(Math.max(x1, x2), Math.min(x1, x2));
//                    double e1e2 = clusters.get(i).getEnergy() * clusters.get(j).getEnergy();
//                    double dx2 = Math.pow(clusters.get(i).getPosition()[0] - clusters.get(j).getPosition()[0], 2) + Math.pow(clusters.get(i).getPosition()[1] - clusters.get(j).getPosition()[1], 2);
//                    invMassPlot.fill(Math.sqrt(e1e2 * dx2 / (135 * 135)));
                        invMassPlot.fill(VecOp.add(clusterAsPhoton(clus1), clusterAsPhoton(clus2)).magnitude());
                    }
                }
//            }

            ++eventn;
        }
    }

    public HepLorentzVector clusterAsPhoton(Cluster cluster) {
        Hep3Vector position = new BasicHep3Vector(cluster.getPosition());
        Hep3Vector direction = VecOp.unit(VecOp.add(position, new BasicHep3Vector(41.27, 0, targetZ)));
        return new BasicHepLorentzVector(cluster.getEnergy(), VecOp.mult(cluster.getEnergy(), direction));
    }

    public boolean isGoodCluster(Cluster cluster) {
        NeighborMap map = ((HPSEcal3) EcalConditions.getSubdetector()).getNeighborMap();
        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            if (map.get(hit.getCellID()).size() > 6) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void endOfData() {
        if (plotter != null) {
            plotter.hide();
            plotter.destroyRegions();
        }
    }
}
