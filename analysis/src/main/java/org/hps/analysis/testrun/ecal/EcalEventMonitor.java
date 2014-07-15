package org.hps.analysis.testrun.ecal;

import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBox;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalEventMonitor extends Driver implements ItemListener {

    String inputCollectionName = "EcalCalHits";
    String clusterCollectionName = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IHistogram2D hitPlot;
    IHistogram2D clusterPlot;
    int eventRefreshRate = 1;
    int eventn = 0;
    //private AIDAFrame plotterFrame;
    private JCheckBox logScaleBox;

    public EcalEventMonitor() {
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    public void setInputCollectionName(String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    protected void detectorChanged(Detector detector) {
        // Setup plots.
        aida.tree().cd("/");
        hitPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollectionName + " : Pedestal-Subtracted ADC Value", 47, -23.5, 23.5, 11, -5.5, 5.5);

        clusterPlot = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollectionName + " : Energy", 47, -23.5, 23.5, 11, -5.5, 5.5);

        String title = "HPS ECal Event Monitor";
        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory().create();
        plotter.setTitle(title);
        //plotterFrame = new AIDAFrame();
        //plotterFrame.addPlotter(plotter);
        //plotterFrame.setVisible(true);
        //plotterFrame.setTitle(title);

        // Create the plotter regions.
        plotter.createRegions(1, 2);
        plotter.style().statisticsBoxStyle().setVisible(false);

        for (int i = 0; i < plotter.numberOfRegions(); i++) {
            IPlotterStyle style = plotter.region(i).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            style.zAxisStyle().setParameter("scale", "lin");
        }

        plotter.region(0).plot(hitPlot);
        ((PlotterRegion) plotter.region(0)).getPlot().setAllowUserInteraction(false);
        ((PlotterRegion) plotter.region(0)).getPlot().setAllowPopupMenus(false);

        plotter.region(1).plot(clusterPlot);
        ((PlotterRegion) plotter.region(1)).getPlot().setAllowUserInteraction(false);
        ((PlotterRegion) plotter.region(1)).getPlot().setAllowPopupMenus(false);

        logScaleBox = new JCheckBox("log scale");
        logScaleBox.addItemListener(this);
        //plotterFrame.getControlsPanel().add(logScaleBox);

        //plotterFrame.pack();
    }

    public void process(EventHeader event) {
        if (++eventn % eventRefreshRate != 0) {
            return;
        }
        hitPlot.reset();
        clusterPlot.reset();
        if (event.hasCollection(CalorimeterHit.class, inputCollectionName)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollectionName);
            for (CalorimeterHit hit : hits) {
                hitPlot.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getRawEnergy());
            }
        }
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
            for (HPSEcalCluster cluster : clusters) {
                CalorimeterHit seedHit = cluster.getSeedHit();
                clusterPlot.fill(seedHit.getIdentifierFieldValue("ix"), seedHit.getIdentifierFieldValue("iy"), cluster.getEnergy());
            }
        }
    }

    public void endOfData() {
    	//if (plotterFrame != null) {
        //    plotterFrame.dispose();
        //}
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == logScaleBox) {
            for (int i = 0; i < plotter.numberOfRegions(); i++) {
                IPlotterStyle style = plotter.region(i).style();
                if (ie.getStateChange() == ItemEvent.DESELECTED) {
                    style.zAxisStyle().setParameter("scale", "lin");
                } else {
                    style.zAxisStyle().setParameter("scale", "log");
                }
            }
        }
    }
}