/**
 * 
 */
package org.hps.analysis.trigger;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.trigger.util.TriggerDecisionCalculator;
import org.hps.analysis.trigger.util.TriggerDecisionCalculator.TriggerType;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>, Matt Solt <mrsolt@slac.stanford.edu>
 *
 */
public class TriggerTurnOnDriver extends Driver {

    private static Logger logger = LogUtil.create(TriggerTurnOnDriver.class, new BasicLogFormatter(), Level.FINE);
    private final String triggerBankCollectionName = "TriggerBank";
    private final String ecalClusterCollectionName = "EcalClustersCorr";
    IPlotter plotter;
    IPlotter plotter2;
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D clusterE_Random;
    IHistogram1D clusterE_RandomSingles1;
    IHistogram1D clusterEOne_Random;
    IHistogram1D clusterEOne_RandomSingles1;
    IHistogram1D clusterE_RandomSingles1_trigEff;
    IHistogram1D clusterEOne_RandomSingles1_trigEff;
    private boolean showPlots = true;
    private int nEventsProcessed = 0;
    
    /**
     * 
     */
    public TriggerTurnOnDriver() {
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();

        plotter = fac.createPlotterFactory().create("Trigger Efficiency");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        clusterE_Random = aida.histogram1D("clusterE_Random", 50, 0., 1.5);
        clusterE_RandomSingles1 = aida.histogram1D("clusterE_RandomSingles1", 50, 0., 1.5);
        plotter.setTitle("Cluster E efficiency");
        plotter.region(0).plot(clusterE_Random);
        plotter.region(1).plot(clusterE_RandomSingles1);
        if(showPlots) plotter.show();
        
        plotter2 = fac.createPlotterFactory().create("Trigger Efficiency One");
        plotter2.createRegions(2, 2);
        clusterEOne_Random = aida.histogram1D("clusterEOne_Random", 50, 0., 1.5);
        clusterEOne_RandomSingles1 = aida.histogram1D("clusterEOne_RandomSingles1", 50, 0., 1.5);
        plotter2.region(0).plot(clusterEOne_Random);
        plotter2.region(1).plot(clusterEOne_RandomSingles1);
        if(showPlots) plotter2.show();
        
    }
    
    @Override
    protected void process(EventHeader event) {

        
        TriggerDecisionCalculator triggerDecisions = new TriggerDecisionCalculator(event);
        
        if(!triggerDecisions.passed(TriggerType.PULSER))
            return;
        
        logger.fine("pulser trigger fired");

        if(triggerDecisions.passed(TriggerType.SINGLES1))
            logger.fine("Singles1 trigger fired");
        
        if(triggerDecisions.passed(TriggerType.SINGLES1_SIM))
            logger.fine("Sim Singles1 trigger fired");
        
        
        List<Cluster> clusters = null;
        Cluster clusterEMax = null;
        
        if(event.hasCollection(Cluster.class , ecalClusterCollectionName)) 
            clusters = event.get(Cluster.class, ecalClusterCollectionName);

        if(clusters != null) {
            for(Cluster cluster : clusters) {
                if(clusterEMax != null) {
                    if(cluster.getEnergy() > clusterEMax.getEnergy()) 
                        clusterEMax = cluster;
                } else {
                    clusterEMax = cluster;
                }
            }
        }

        // fill denominator
        if(clusterEMax!=null) {
            clusterE_Random.fill(clusterEMax.getEnergy());
            if(clusters.size() == 1) {
                clusterEOne_Random.fill(clusterEMax.getEnergy());
            }
        }

        // fill numerator
        if (triggerDecisions.passed(TriggerType.SINGLES1_SIM)) {
            logger.fine("Eureka. They both fired.");
            if(clusterEMax != null) {
                clusterE_RandomSingles1.fill(clusterEMax.getEnergy());
                if(clusters.size() == 1) 
                    clusterEOne_RandomSingles1.fill(clusterEMax.getEnergy());
            }
        }

        nEventsProcessed++;
        

    }
    
    @Override
    protected void endOfData() {
        clusterE_RandomSingles1_trigEff = aida.histogramFactory().divide("trigEff", clusterE_RandomSingles1, clusterE_Random);
        clusterEOne_RandomSingles1_trigEff = aida.histogramFactory().divide("trigEffEone", clusterEOne_RandomSingles1, clusterEOne_Random);
        logger.info("entries in clusterE_RandomSingles1_trigEff: " + Integer.toString(clusterE_RandomSingles1_trigEff.allEntries()));
        plotter.region(2).plot(clusterE_RandomSingles1_trigEff);
        plotter2.region(2).plot(clusterEOne_RandomSingles1_trigEff);
        
    }

}
