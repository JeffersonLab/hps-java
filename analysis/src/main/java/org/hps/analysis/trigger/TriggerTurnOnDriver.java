/**
 * 
 */
package org.hps.analysis.trigger;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.TriggerDecisionCalculator;
import org.hps.analysis.trigger.util.TriggerDecisionCalculator.TriggerType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>, Matt Solt <mrsolt@slac.stanford.edu>
 *
 */
public class TriggerTurnOnDriver extends Driver {

    private static Logger LOGGER = Logger.getLogger(TriggerTurnOnDriver.class.getPackage().getName());
    private final String ecalClusterCollectionName = "EcalClustersCorr";
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter33;
    IPlotter plotter333;

    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D clusterE_Random;
    IHistogram1D clusterE_RandomSingles1;
    IHistogram1D clusterEOne_Random;
    IHistogram1D clusterEOne_RandomSingles1;
    IHistogram1D clusterE_RandomSingles1_trigEff;
    IHistogram1D clusterEOne_RandomSingles1_trigEff;
    IHistogram1D clusterEOne_RandomSingles1_thetaY_trigEff[][] = new IHistogram1D[2][5];
    IHistogram1D clusterEOne_Random_thetaY[][] = new IHistogram1D[2][5];
    IHistogram1D clusterEOne_RandomSingles1_thetaY[][] = new IHistogram1D[2][5];
    
    private boolean showPlots = false;
    private int nEventsProcessed = 0;
    private int nSimSingles1 = 0;
    private int nResultSingles1 = 0;
    private boolean isMC;
    
    public boolean isMC() {
        return isMC;
    }

    public void setIsMC(boolean isMC) {
        this.isMC = isMC;
    }

    /**
     * 
     */
    public TriggerTurnOnDriver() {
    }
    
	public void setShowPlots(boolean showPlots) {
		this.showPlots = showPlots;
	}

    @Override
    protected void detectorChanged(Detector detector) {
        
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();

        plotter = fac.createPlotterFactory().create("Trigger Efficiency");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(1, 3);
        clusterE_Random = aida.histogram1D("clusterE_Random", 50, 0., 1.3);
        clusterE_RandomSingles1 = aida.histogram1D("clusterE_RandomSingles1", 50, 0., 1.3);
        plotter.setTitle("Cluster E efficiency");
        plotter.region(0).plot(clusterE_Random);
        plotter.region(1).plot(clusterE_RandomSingles1);
        if(showPlots) plotter.show();
        
        plotter2 = fac.createPlotterFactory().create("Trigger Efficiency One");
        plotter2.createRegions(1, 3);
        clusterEOne_Random = aida.histogram1D("clusterEOne_Random", 50, 0., 1.3);
        clusterEOne_RandomSingles1 = aida.histogram1D("clusterEOne_RandomSingles1", 50, 0., 1.3);
        plotter2.region(0).plot(clusterEOne_Random);
        plotter2.region(1).plot(clusterEOne_RandomSingles1);
        if(showPlots) plotter2.show();
        
        plotter3 = fac.createPlotterFactory().create("Cluster energy One ThetaY");
        plotter3.createRegions(2, 5);
        plotter33 = fac.createPlotterFactory().create("Cluster energy One ThetaY");
        plotter33.createRegions(2, 5);
        plotter333 = fac.createPlotterFactory().create("Trigger Efficiency One ThetaY");
        plotter333.createRegions(2, 5);
        int r = 0;
        for(int i=0; i<2; ++i) {
            for( int y=1; y<6; ++y) {
                clusterEOne_Random_thetaY[i][y-1] = aida.histogram1D("clusterEOne_Random_thetaY" + y + (i==0?"top":"bottom") , 50, 0., 1.3);
                clusterEOne_RandomSingles1_thetaY[i][y-1] = aida.histogram1D("clusterEOne_RandomSingles1_thetaY" + y + (i==0?"top":"bottom") , 50, 0., 1.3);
                plotter3.region(r).plot(clusterEOne_Random_thetaY[i][y-1]);
                plotter33.region(r).plot(clusterEOne_RandomSingles1_thetaY[i][y-1]);
                r++;
            }
        }
        if(showPlots) plotter3.show();
        if(showPlots) plotter33.show();
        if(showPlots) plotter333.show();
        
    }
    
    @Override
    protected void process(EventHeader event) {

        
        TriggerDecisionCalculator triggerDecisions = new TriggerDecisionCalculator(event);
        
        if(!triggerDecisions.passed(TriggerType.PULSER) && !isMC)
            return;
        
        LOGGER.fine("pulser trigger fired");

        if(triggerDecisions.passed(TriggerType.SINGLES1))
            LOGGER.fine("Singles1 trigger fired");
        
        if(triggerDecisions.passed(TriggerType.SINGLES1_SIM)) {
            LOGGER.fine("Sim Singles1 trigger fired");
            nSimSingles1++;
        }
        
        if(triggerDecisions.passed(TriggerType.SINGLES1_RESULTS)) {
            LOGGER.fine("Results Singles1 trigger fired");
            nResultSingles1++;
        }
        
        
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
                int clusterPosIdy = clusterEMax.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                if( Math.abs(clusterPosIdy) > 5 ) 
                    throw new RuntimeException("invalid crystal position " + clusterPosIdy);
                int half = clusterPosIdy > 0 ? 0 : 1;
                int ypos = Math.abs(clusterPosIdy)-1;
                clusterEOne_Random_thetaY[half][ypos].fill(clusterEMax.getEnergy());
            }
        }



        // fill numerator
        if (triggerDecisions.passed(TriggerType.SINGLES1_SIM)) {
            LOGGER.fine("Eureka. They both fired.");
            if(clusterEMax != null) {
                clusterE_RandomSingles1.fill(clusterEMax.getEnergy());
                if(clusters.size() == 1) {
                    clusterEOne_RandomSingles1.fill(clusterEMax.getEnergy());
                    int clusterPosIdy = clusterEMax.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                    int half = clusterPosIdy > 0 ? 0 : 1;
                    int ypos = Math.abs(clusterPosIdy)-1;
                    clusterEOne_RandomSingles1_thetaY[half][ypos].fill(clusterEMax.getEnergy());
                }
            }
        }

        nEventsProcessed++;
        

    }
    
    @Override
    protected void endOfData() {
        LOGGER.info("Processed " + nEventsProcessed);
        LOGGER.info("nResSingles1 " + nResultSingles1 + " nSimSingles1 " + nSimSingles1);
        clusterE_RandomSingles1_trigEff = aida.histogramFactory().divide("trigEff", clusterE_RandomSingles1, clusterE_Random);
        clusterEOne_RandomSingles1_trigEff = aida.histogramFactory().divide("trigEffEone", clusterEOne_RandomSingles1, clusterEOne_Random);
        int r = 0;
        for(int i=0;i<2;++i) {
            for(int y=0;y<5;++y) {
                clusterEOne_RandomSingles1_thetaY_trigEff[i][y] = aida.histogramFactory().divide("trigEffEone_" + (i==0?"top":"bottom") + "_" + y, clusterEOne_RandomSingles1_thetaY[i][y], clusterEOne_Random_thetaY[i][y]);
                plotter333.region(r).plot(clusterEOne_RandomSingles1_thetaY_trigEff[i][y]);
                r++;
            }
        }
        LOGGER.info("entries in clusterE_RandomSingles1_trigEff: " + Integer.toString(clusterE_RandomSingles1_trigEff.allEntries()));
        plotter.region(2).plot(clusterE_RandomSingles1_trigEff);
        plotter2.region(2).plot(clusterEOne_RandomSingles1_trigEff);
        
    }
    
    private List<SSPCluster> getSingles1SSPClusters(EventHeader event) {
        List<SSPCluster> clusters = new ArrayList<SSPCluster>();
        List<GenericObject> triggerBanks = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject triggerBank : triggerBanks) {
            if(AbstractIntData.getTag(triggerBank) == SSPData.BANK_TAG) {
                SSPData sspBank = new SSPData(triggerBank);
                
                // recompute the decision for singles1
                List<SSPCluster> sspClusters = sspBank.getClusters();
                List<List<SinglesTrigger<SSPCluster>>> singleTriggers = TriggerDecisionCalculator.constructSinglesTriggersFromSSP(sspClusters);
                for(SinglesTrigger<SSPCluster> singleTrigger : singleTriggers.get(1)) {
                    clusters.add( singleTrigger.getTriggerSource() );
                }
            }
        }
        return clusters;
    }
    

}
