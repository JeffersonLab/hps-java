package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import java.util.List;
import org.apache.commons.math.stat.StatUtils;
import org.hps.readout.ecal.TriggerData;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.ecal.HPSEcalClusterIC;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalCluster</code> implements the histogram shown to the
 * user
 * in the third tab of the Monitoring Application, when using the Ecal
 * monitoring lcsim file.
 * These histograms shows single-channels distributions:
 * - First sub-tab shows the cluster counts per event (Histogram1D), the number
 * of hits in a cluster (Histogram1D), the cluster centers distribution*
 * (Histogram2D), the maximum cluster energy in an event (Histogram1D)
 * - Second sub-tab shows the energy distribution of the cluster (Histogram1D),
 * and the maximum cluster energy in each event (Histogram1D)
 * - Third sub-tab shows the time distribution of the cluster (Histogram1D),
 * taken from the mean of the times forming the cluster, as well as the RMS
 * (Histogram1D).
 * - Last sub-tab is a "zoom" of the the cluster centers distribution
 * All histograms are updated continously.
 *
 * The cluster center is calculated from the energy center of gravity of the
 * hits forming a cluster.
 *
 * @author Andrea Celentano
 *
 */
public class EcalClusterPlots extends Driver implements Resettable {

    String inputCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();

    IPlotter plotter1, plotter2, plotter3, plotter4, plotter5;
    IHistogram1D clusterCountPlot;
    IHistogram1D clusterSizePlot;
    IHistogram1D clusterEnergyPlot;
    IHistogram1D clusterMaxEnergyPlot;
    IHistogram1D clusterTimes;
    IHistogram1D clusterTimeSigma;
    IHistogram2D edgePlot;
    //mg...12/13/2014
    IHistogram1D  twoclusterTotEnergy;
    IHistogram1D  twoclusterEnergyDiff;
    IHistogram1D  twoclusterEnergyMeanYPos;
    IHistogram1D  twoclusterEnergyMeanXPos;    
    //
    int eventn = 0;
    double maxE = 5000 * ECalUtils.MeV;
    boolean logScale = false;
    boolean hide = false;

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
        System.out.println(this.getClass().getSimpleName() + ":  detectorChanged...making plots");
        // Setup plots.
        aida.tree().cd("/");
        clusterCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Size", 10, -0.5, 9.5);
        clusterEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Energy", 100, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Maximum Cluster Energy In Event", 100, -0.1, maxE);
        edgePlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Cluster center from hits", 93, -23.25, 23.25, 21, -5.25, 5.25);
        clusterTimes = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Time Mean", 400, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Time Sigma", 100, 0, 40);
        //mg..12/13/2014
        twoclusterTotEnergy=aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Two Cluster Energy Sum",100,0,maxE);
        twoclusterEnergyDiff=aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Two Cluster Energy Diff",100,0,maxE/2);
        twoclusterEnergyMeanYPos=aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Two Cluster Energy Weighted Y",100,-100,100);
        twoclusterEnergyMeanXPos=aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Two Cluster Energy Weighted X",100,-250,250);
        
        // Setup the plotter factory.
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal Cluster Plots");

        // Create the plotter regions.
        plotter1 = plotterFactory.create("Cluster Counts");
        plotter1.setTitle("Cluster Counts");
        plotter1.style().dataStyle().errorBarStyle().setVisible(false);
        plotter1.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        plotter1.createRegions(2, 2);
        plotter1.region(0).plot(clusterCountPlot);
        plotter1.region(1).plot(clusterSizePlot);
        plotter1.region(2).plot(edgePlot);
        plotter1.region(3).plot(clusterMaxEnergyPlot);

        plotter2 = plotterFactory.create("Cluster Energies");
        plotter2.createRegions(1, 2);
        plotter2.setTitle("Cluster Energies");
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);
        plotter2.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        if (logScale)
            plotter2.style().yAxisStyle().setParameter("scale", "log");
        plotter2.region(0).plot(clusterEnergyPlot);
        plotter2.region(1).plot(clusterMaxEnergyPlot);

        plotter3 = plotterFactory.create("Cluster Times");
        plotter3.setTitle("Cluster Times");
        plotter3.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(1, 2);
        if (logScale)
            plotter3.style().yAxisStyle().setParameter("scale", "log");
        plotter3.region(0).plot(clusterTimes);
        plotter3.region(1).plot(clusterTimeSigma);

        plotter4 = plotterFactory.create("Cluster Center");
        plotter4.setTitle("Edges");
        plotter4.style().setParameter("hist2DStyle", "colorMap");
        plotter4.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        if (logScale)
            plotter4.style().zAxisStyle().setParameter("scale", "log");
        plotter4.createRegion();
        plotter4.region(0).plot(edgePlot);

         // Create the plotter regions.
        plotter5 = plotterFactory.create("Two Clusters");
        plotter5.setTitle("Two Clusters");
        plotter5.style().dataStyle().errorBarStyle().setVisible(false);
        plotter5.style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
        plotter5.createRegions(2, 2);
        plotter5.region(0).plot(twoclusterTotEnergy);
        plotter5.region(1).plot(twoclusterEnergyDiff);
        plotter5.region(2).plot(twoclusterEnergyMeanXPos);
        plotter5.region(3).plot(twoclusterEnergyMeanYPos);
        
        if (!hide) {
            plotter1.show();
            plotter2.show();
            plotter3.show();
            plotter4.show();
            plotter5.show();
        }
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
        //mg..12/13/2014...put in kludge so that it handles HPSEcalClusterIC clusters as well. 
        //clusters used BaseCluster now and what's below is jusa re-arrangement of what was
        // there before
        List<BaseCluster> clusters;
        if (event.hasCollection(HPSEcalCluster.class, inputCollection))
            clusters = event.get(BaseCluster.class, inputCollection);
        else if (event.hasCollection(HPSEcalClusterIC.class, inputCollection))
            clusters = event.get(BaseCluster.class, inputCollection);
        else {
            clusterCountPlot.fill(0);
            return;
        }

        clusterCountPlot.fill(clusters.size());
        double maxEnergy = 0;
        for (BaseCluster cluster : clusters) {
//
//                if ((botTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] < 0) || (topTrig == 0 && cluster.getEnergy() > 130 && cluster.getPosition()[1] > 0)) {
//                if ((botTrig == 0 && cluster.getPosition()[1] < 0) || (topTrig == 0 && cluster.getPosition()[1] > 0)) {
            clusterEnergyPlot.fill(cluster.getEnergy());
            if (cluster.getEnergy() > maxEnergy)
                maxEnergy = cluster.getEnergy();
            int size = 0;
            double eTOT = 0;
            double[] times = new double[cluster.getCalorimeterHits().size()];
            double[] energies = new double[cluster.getCalorimeterHits().size()];

            double X = 0;
            double Y = 0;
//                    System.out.format("cluster:\n");
            for (CalorimeterHit hit : cluster.getCalorimeterHits())
                if (hit.getRawEnergy() != 0) {
                    energies[size] = hit.getRawEnergy();
                    times[size] = hit.getTime();
                    X += energies[size] * hit.getIdentifierFieldValue("ix");
                    Y += energies[size] * hit.getIdentifierFieldValue("iy");
                    eTOT += energies[size];
                    size++;
//                            System.out.format("x=%d, y=%d, time=%f, energy=%f\n", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getTime(), hit.getRawEnergy());
                }
            if (eTOT > 0) {
                X /= eTOT;
                Y /= eTOT;
                clusterTimes.fill(StatUtils.mean(times, 0, size));
                clusterSizePlot.fill(size); //The number of "hits" in a "cluster"
                clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(times, 0, size)));
                edgePlot.fill(X, Y);
            }
        }
        if (maxEnergy > 0)
            clusterMaxEnergyPlot.fill(maxEnergy);
        //make some interesting 2-cluster plots
        if(clusters.size()==2){
            BaseCluster cl1=clusters.get(0);
            BaseCluster cl2=clusters.get(1);
            double[] p1=cl1.getPosition();
            double[] p2=cl2.getPosition();
            double e1=cl1.getEnergy();
            double e2=cl2.getEnergy();
            twoclusterTotEnergy.fill(e1+e2);
            twoclusterEnergyDiff.fill(Math.abs(e1-e2));            
            twoclusterEnergyMeanXPos.fill((e1*p1[0]+e2*p2[0])/(e1+e2));
            twoclusterEnergyMeanYPos.fill((e1*p1[1]+e2*p2[1])/(e1+e2));
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

    public void setHide(boolean hide)
    {
        this.hide=hide;
    }
}
