/**
 * 
 */
package org.hps.analysis.trigger;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.AnalysisFactory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>, Matt Solt <mrsolt@slac.stanford.edu>
 *
 */
public class TriggerTurnOnDriver extends Driver {

    private static Logger logger = LogUtil.create(TriggerTurnOnDriver.class, new BasicLogFormatter(), Level.INFO);
    private String triggerBankCollectionName = "TriggerBank";
    private String ecalClusterCollectionName = "EcalClusters";
    IPlotter plotter;
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D clusterE_Random;
    IHistogram1D clusterE_RandomSingles1;
    IHistogram1D trigEff;
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
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        //plotterFrame.addPlotter(plotter);

        clusterE_Random = aida.histogram1D("Cluster E rndm", 50, 0., 1.5);
        clusterE_RandomSingles1 = aida.histogram1D("Cluster E rndm+singles1", 50, 0., 1.5);
        trigEff = aida.histogram1D("trigEff", 50, 0., 1.5);

        plotter.region(0).plot(clusterE_Random);

        plotter.region(1).plot(clusterE_RandomSingles1);
        
        plotter.region(2).plot(trigEff);

        if(showPlots) plotter.show();
        
    }
    
    @Override
    protected void process(EventHeader event) {

        // Get the list of trigger banks from the event
        List<GenericObject> triggerBanks = event.get(GenericObject.class, triggerBankCollectionName);

        boolean isRandomTriggerEvent = false;
        boolean isSingles1TriggerEvent = false;

        // Loop through the collection of banks and get the TI banks.
        for (GenericObject triggerBank : triggerBanks) {

            // If the bank contains TI data, process it
            if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {

                TIData tiData = new TIData(triggerBank);

                if(tiData.isPulserTrigger()) {
                    isRandomTriggerEvent = true;
                } else if(tiData.isSingle1Trigger()) {
                    isSingles1TriggerEvent = true;
                }
            }
        }


        if(isRandomTriggerEvent) {

            logger.info("Random trigger fired");

            // find offline ecal clusters -> denominator
            // count how often the singles1trigger fired for a given offline cluster (vs E, x, y)

            if(event.hasCollection(Cluster.class , ecalClusterCollectionName)) {


                List<Cluster> clusters = event.get(Cluster.class, ecalClusterCollectionName);

                for(Cluster cluster : clusters) {

                    clusterE_Random.fill(cluster.getEnergy());
                    nEventsProcessed++;

                }


            }

        } else if(isSingles1TriggerEvent) {

            logger.info("Singles1 trigger fired");
        }
        
        
        if (isRandomTriggerEvent && isSingles1TriggerEvent) {
            
            logger.info("Eureka. They both fired.");
            if(event.hasCollection(Cluster.class , ecalClusterCollectionName)) {


                List<Cluster> clusters = event.get(Cluster.class, ecalClusterCollectionName);

                for(Cluster cluster : clusters) {

                    clusterE_RandomSingles1.fill(cluster.getEnergy());

                }


            }
            
            
        }

        
        if(nEventsProcessed % 10 == 0 ) {
            trigEff = aida.histogramFactory().divide("trigEff", clusterE_RandomSingles1, clusterE_Random);
        }


    }
    
    @Override
    protected void endOfData() {
        

    }

}
