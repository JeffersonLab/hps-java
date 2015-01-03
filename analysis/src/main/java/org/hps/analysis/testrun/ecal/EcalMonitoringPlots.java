package org.hps.analysis.testrun.ecal;

import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.util.Redrawable;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalMonitoringPlots extends Driver implements Redrawable {

    String inputCollection = "EcalReadoutHits";
    String clusterCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IHistogram2D hitCountFillPlot;
    IHistogram2D hitCountDrawPlot;
    IHistogram2D clusterCountFillPlot;
    IHistogram2D clusterCountDrawPlot;
    int eventRefreshRate = 1;
    int eventn = 0;
    boolean hide = false;

    public EcalMonitoringPlots() {
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setClusterCollection(String clusterCollection) {
        this.clusterCollection = clusterCollection;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    protected void detectorChanged(Detector detector) {
        // Setup the plotter.
        plotter = aida.analysisFactory().createPlotterFactory("Ecal Monitoring Plots").create("HPS ECal Monitoring Plots");
        // Setup plots.
        aida.tree().cd("/");
        hitCountDrawPlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Hit Count", 47, -23.5, 23.5, 11, -5.5, 5.5);
        hitCountFillPlot = makeCopy(hitCountDrawPlot);
        clusterCountDrawPlot = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollection + " : Cluster Center Count", 47, -23.5, 23.5, 11, -5.5, 5.5);
        clusterCountFillPlot = makeCopy(clusterCountDrawPlot);

        // Create the plotter regions.
        plotter.createRegions(1, 2);
        plotter.style().statisticsBoxStyle().setVisible(false);
        IPlotterStyle style = plotter.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(0).plot(hitCountDrawPlot);
        style = plotter.region(1).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(1).plot(clusterCountDrawPlot);
        //if (!hide) {
        //    plotter.show();
        //}
    }

    public void process(EventHeader event) {
        if (event.hasCollection(BaseRawCalorimeterHit.class, inputCollection)) {
            List<BaseRawCalorimeterHit> hits = event.get(BaseRawCalorimeterHit.class, inputCollection);
            for (BaseRawCalorimeterHit hit : hits) {
                hitCountFillPlot.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
            }
        }
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollection);
            for (RawTrackerHit hit : hits) {
                hitCountFillPlot.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
            }
        }
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                hitCountFillPlot.fill(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
            }
        }
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);
//if (clusters.size()>1)            
            for (HPSEcalCluster cluster : clusters) {
                clusterCountFillPlot.fill(cluster.getSeedHit().getIdentifierFieldValue("ix"), cluster.getSeedHit().getIdentifierFieldValue("iy"));
            }
        }
        if (eventRefreshRate > 0 && ++eventn % eventRefreshRate == 0) {
            redraw();
        }
    }

    public void endOfData() {
        plotter.hide();
        plotter.destroyRegions();
    }

    @Override
    public void redraw() {
        hitCountDrawPlot.reset();
        hitCountDrawPlot.add(hitCountFillPlot);
        clusterCountDrawPlot.reset();
        clusterCountDrawPlot.add(clusterCountFillPlot);
    }

    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    private IHistogram2D makeCopy(IHistogram2D hist) {
        return aida.histogram2D(hist.title() + "_copy", hist.xAxis().bins(), hist.xAxis().lowerEdge(), hist.xAxis().upperEdge(), hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
    }
}