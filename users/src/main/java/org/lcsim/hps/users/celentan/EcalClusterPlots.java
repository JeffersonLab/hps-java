package org.lcsim.hps.users.celentan;



import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.List;

import org.apache.commons.math.stat.StatUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.evio.TriggerData;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalClusterPlots extends Driver implements Resettable {

	//AIDAFrame plotterFrame;
    String inputCollection = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    
    
    IPlotter plotter1, plotter2, plotter3, plotter4;
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

    	//plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS ECal Cluster Plots");
    	
        // Setup the plotter factory.
    	IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal Cluster Plots");
    	plotter1 = plotterFactory.create("Cluster Counts");
        plotter1.setTitle("Cluster Counts");
        //plotterFrame.addPlotter(plotter);
        plotter1.style().dataStyle().errorBarStyle().setVisible(false);

        // Setup plots.
        aida.tree().cd("/");
        clusterCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Size", 10, -0.5, 9.5);
        clusterEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Energy", 1000, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Maximum Cluster Energy In Event", 1000, -0.1, maxE);      
        edgePlot = aida.histogram2D(detector.getDetectorName() + " : " + inputCollection + " : Cluster center from hits", 93, -23.25, 23.25, 21, -5.25, 5.25);
        clusterTimes = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Time Mean", 400, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(detector.getDetectorName() + " : " + inputCollection + " : Cluster Time Sigma", 100, 0, 40);
        
        
        // Create the plotter regions.
        plotter1.createRegions(2, 2);
        plotter1.region(0).plot(clusterCountPlot);
        plotter1.region(1).plot(clusterSizePlot);
        plotter1.region(2).plot(edgePlot);
        plotter1.region(3).plot(clusterMaxEnergyPlot);

        // Setup the plotter.
        plotter2 = plotterFactory.create("Cluster Energies");
        plotter2.setTitle("Cluster Energies");
        //plotterFrame.addPlotter(plotter2);
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);

        if (logScale) {
            plotter2.style().yAxisStyle().setParameter("scale", "log");
        }

     

        // Create the plotter regions.
        plotter2.createRegions(1, 2);
        plotter2.region(0).plot(clusterEnergyPlot);
        plotter2.region(1).plot(clusterMaxEnergyPlot);

        plotter3 = plotterFactory.create("Cluster Times");
        plotter3.setTitle("Cluster Times");
        //plotterFrame.addPlotter(plotter3);
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(1, 2);
        plotter3.style().yAxisStyle().setParameter("scale", "log");

      
        plotter3.region(0).plot(clusterTimes);
        plotter3.region(1).plot(clusterTimeSigma);

        plotter4 = plotterFactory.create("Cluster Center");
        plotter4.setTitle("Edges");
        //plotterFrame.addPlotter(plotter4);
        plotter4.style().setParameter("hist2DStyle", "colorMap");
        plotter4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter4.style().zAxisStyle().setParameter("scale", "log");
        plotter4.createRegion();

       
      

        //plotterFrame.setVisible(true);
        //plotterFrame.pack();
        
        if (!hide){
        	plotter1.show();
        	plotter2.show();
        	plotter3.show();
        	plotter4.show();
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
                double[] energies = new double[cluster.getCalorimeterHits().size()];
                
                double X = 0;
                double Y = 0;
//                    System.out.format("cluster:\n");
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit.getRawEnergy() != 0) {
                    	energies[size] = hit.getRawEnergy();
                        times[size] = hit.getTime();
                        X += energies[size] * hit.getIdentifierFieldValue("ix");
                        Y += energies[size] * hit.getIdentifierFieldValue("iy");
                        //clusterTimes.fill(hit.getTime());
                        size++;
//                            System.out.format("x=%d, y=%d, time=%f, energy=%f\n", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getTime(), hit.getRawEnergy());
                    }
                }
                X/=size;
                Y/=size;
                clusterTimes.fill(StatUtils.mean(times, 0, size));
                clusterSizePlot.fill(size); //The number of "hits" in a "cluster"
                clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(times, 0, size)));
                edgePlot.fill(X,Y);
            
             
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