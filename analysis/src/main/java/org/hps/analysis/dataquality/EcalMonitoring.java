package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math.stat.StatUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.ecal.HPSEcalClusterIC;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

/**
 *
 * @author mgraham on Mar 28, 2014...just added empty (almost) file into svn May
 * 14, 2014 put some DQM template stuff in...ECal-ers should really fill in the
 * guts
 *
 * mg...12/16/2014: added a bunch of plots to use with engineering run data;
 * mostly copied from online monitoring
 */
public class EcalMonitoring extends DataQualityMonitor {

    String readoutHitCollectionName = "EcalReadoutHits";//these are in ADC counts
    String calibratedHitCollectionName = "EcalCalHits";//these are in energy
    String clusterCollectionName = "EcalClusters";

    //ecal hit plots
    IHistogram1D hitCountPlot;
    IHistogram1D hitTimePlot;
    IHistogram1D hitEnergyPlot;
    IHistogram1D fiducialHitCountPlot;
    IHistogram1D fiducialEnergyPlot;
    //  ecal cluster plots
    IHistogram1D clusterCountPlot;
    IHistogram1D clusterSizePlot;
    IHistogram1D clusterEnergyPlot;
    IHistogram1D clusterTimes;
    IHistogram1D clusterTimeSigma;
    //mg...12/13/2014
    IHistogram1D twoclusterTotEnergy;
    IHistogram1D twoclusterEnergyAsymmetry;
//    IHistogram1D twoclusterEnergyMeanYPos;
//    IHistogram1D twoclusterEnergyMeanXPos;
    //mg...12/14/2014
    IHistogram1D fiducialClusterCountPlot;
    IHistogram1D fiducialClusterEnergyPlot;
    IHistogram1D fiducialClusterSizePlot;
    IHistogram2D fiducialenergyVsY;
    IHistogram2D fiducialenergyVsX;
    IHistogram2D energyVsY;
    IHistogram2D energyVsX;

    private Map<String, Double> monitoredQuantityMap = new HashMap<>();
    String[] ecalQuantNames = {"Good", "Stuff", "For", "ECAL"};
    double maxE = 2.5;
    private final String plotHitsDir = "EcalHits/";
    private final String plotClustersDir = "EcalClusters/";

    protected void detectorChanged(Detector detector) {
        System.out.println("EcalMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        // Setup hit plots.
        hitCountPlot = aida.histogram1D(plotHitsDir + "Hit Count In Event", 40, -0.5, 39.5);
        hitTimePlot = aida.histogram1D(plotHitsDir + "Hit Time", 50, 0 * 4.0, 50 * 4.0);
        hitEnergyPlot = aida.histogram1D(plotHitsDir + "Hit Energy", 100, -0.1, maxE);
        fiducialHitCountPlot = aida.histogram1D(plotHitsDir + "Hit Count with Fiducial Cut", 10, -0.5, 9.5);
        fiducialEnergyPlot = aida.histogram1D(plotHitsDir + "Hit Energy with Fiducial Cut", 100, -0.1, maxE);

        // Setup cluster plots
        clusterCountPlot = aida.histogram1D(plotClustersDir + "Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(plotClustersDir + "Cluster Size", 10, -0.5, 9.5);
        clusterEnergyPlot = aida.histogram1D(plotClustersDir + "Cluster Energy", 100, -0.1, maxE);
        clusterTimes = aida.histogram1D(plotClustersDir + "Cluster Time Mean", 200, 0, 4.0 * 50);
        clusterTimeSigma = aida.histogram1D(plotClustersDir + "Cluster Time Sigma", 100, 0, 10);
        twoclusterTotEnergy = aida.histogram1D(plotClustersDir + "Two Cluster Energy Sum", 100, 0, maxE);
        twoclusterEnergyAsymmetry = aida.histogram1D(plotClustersDir + "Two Cluster Energy Asymmetry", 100, 0, 1.0);
        energyVsX = aida.histogram2D(plotClustersDir + "Energy vs X", 50, 0, 1.6, 50, .0, 200.0);
        energyVsY = aida.histogram2D(plotClustersDir + "Energy vs Y", 50, 0, 1.6, 50, 20.0, 85.0);

        fiducialClusterCountPlot = aida.histogram1D(plotClustersDir + "Cluster Count with Fiducal Cut", 10, -0.5, 9.5);
        fiducialClusterSizePlot = aida.histogram1D(plotClustersDir + "Cluster Size with Fiducal Cut", 10, -0.5, 9.5);
        fiducialClusterEnergyPlot = aida.histogram1D(plotClustersDir + "Cluster Energy with Fiducal Cut", 100, -0.1, maxE);
        fiducialenergyVsY = aida.histogram2D(plotClustersDir + "Energy vs Y with Fiducial Cuts", 50, 0, 1.6, 50, 45.0, 85.0);
        fiducialenergyVsX = aida.histogram2D(plotClustersDir + "Energy vs X with Fiducial Cuts", 50, 0, 1.6, 50, 0.0, 200.0);

    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        List<CalorimeterHit> hits;
        if (event.hasCollection(CalorimeterHit.class, calibratedHitCollectionName))
            hits = event.get(CalorimeterHit.class, calibratedHitCollectionName);
        else
            return; //this might be a non-data event

        hitCountPlot.fill(hits.size());
        int fidHitCount = 0;
        for (CalorimeterHit hit : hits) {

            hitEnergyPlot.fill(hit.getCorrectedEnergy());
            hitTimePlot.fill(hit.getTime());
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            if (Math.abs(iy) > 2) {
                fidHitCount++;
                fiducialEnergyPlot.fill(hit.getCorrectedEnergy());
            }
            fiducialHitCountPlot.fill(fidHitCount);
        }

        List<Cluster> clusters;
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName))
            clusters = event.get(Cluster.class, clusterCollectionName);
        else if (event.hasCollection(HPSEcalClusterIC.class, clusterCollectionName))
            clusters = event.get(Cluster.class, clusterCollectionName);
        else if(event.hasCollection(Cluster.class, clusterCollectionName))
            clusters = event.get(Cluster.class, clusterCollectionName);
        else {
            clusterCountPlot.fill(0);
            return;
        }

        clusterCountPlot.fill(clusters.size());

        int fidcnt = 0;
        for (Cluster cluster : clusters) {
            clusterEnergyPlot.fill(cluster.getEnergy());
            double[] times = new double[cluster.getCalorimeterHits().size()];
            double[] energies = new double[cluster.getCalorimeterHits().size()];
            CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            if (cluster.getCalorimeterHits().size() > 1) {
                energyVsX.fill(cluster.getEnergy(), Math.abs(cluster.getPosition()[0]));
                energyVsY.fill(cluster.getEnergy(), Math.abs(cluster.getPosition()[1]));
            }
            if (Math.abs(iy) > 2&&cluster.getCalorimeterHits().size()>1) {
                fidcnt++;
                fiducialClusterSizePlot.fill(cluster.getCalorimeterHits().size());
                fiducialClusterEnergyPlot.fill(cluster.getEnergy());
                if (cluster.getCalorimeterHits().size() > 1)
                    fiducialenergyVsY.fill(cluster.getEnergy(), Math.abs(cluster.getPosition()[1]));
            }

            int size = 0;
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                energies[size] = hit.getCorrectedEnergy();
                times[size] = hit.getTime();
                size++;
            }
            clusterTimes.fill(StatUtils.mean(times, 0, size));
            clusterSizePlot.fill(size); //The number of "hits" in a "cluster"
            clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(times, 0, size)));

        }
        fiducialClusterCountPlot.fill(fidcnt);
        //make some interesting 2-cluster plots
        if (clusters.size() == 2) {
            Cluster cl1 = clusters.get(0);
            Cluster cl2 = clusters.get(1);
            double[] p1 = cl1.getPosition();
            double[] p2 = cl2.getPosition();
            double e1 = cl1.getEnergy();
            double e2 = cl2.getEnergy();
            twoclusterTotEnergy.fill(e1 + e2);
            twoclusterEnergyAsymmetry.fill(Math.abs(e1 - e2)/(e1+e2));
        }
        

    }

    @Override
    public void dumpDQMData() {
        System.out.println("EcalMonitoring::endOfData filling DQM database");
    }

    @Override
    public void printDQMData() {
        System.out.println("EcalMonitoring::printDQMData");

        System.out.println("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {
    }

    @Override
    public void printDQMStrings() {

    }

}
