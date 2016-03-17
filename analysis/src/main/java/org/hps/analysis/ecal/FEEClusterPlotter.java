package org.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;


/**
*This makes a bunch of plots of the FEE peak per crystal (seed).
*@author Holly Szumila <hvanc001@odu.edu>
*/

public class FEEClusterPlotter extends Driver {
    
    //private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IAnalysisFactory fac = aida.analysisFactory();
    private String inputCollection = "EcalClusters";
    public void setInputCollection(final String inputCollection) {
        this.inputCollection = inputCollection;
    }
    
    private DatabaseConditionsManager conditionsManager = null;
    private EcalConditions ecalConditions = null;
    private String histoNameFormat = "%3d";
    
    private String outputPlots = null;
    
    //Set min energy in histo
    private double minHistoE = 0.5;
    
    //Set max energy in histo
    private double maxHistoE = 1.3;
    
    /**
     * Set the minimum histogram energy
     * @param minHistoE
     */
    void setMinHistoE(double minHistoE) {
        this.minHistoE = minHistoE;
    }
    
    /**
     * Set the maximum histogram energy
     * @param maxHistoE
     */
    void setMaxHistoE(double maxHistoE) {
        this.maxHistoE = maxHistoE;
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getEcalConditions();
        
        aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            aida.histogram1D(getHistoName(cc),200,minHistoE,maxHistoE);
        }

    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat,cc.getChannelId());
    }
    
    
    //Set min seed energy value, default to 2015 run
    private double seedCut = 0.4;
    
    //set min cluster time in window, default to 2015 run
    private double minTime = 30;
    
    //set max cluster time in window, default to 2015 run
    private double maxTime = 070;
    
  //set min number of hits in a cluster in row 1, default to 2015 run
    private int hitCut = 5;
    
    //hit cut is only used in 2016 data, not 2015
    boolean useHitCut = false;
    
    
    /**
     * Set the cut value for seed energy in GeV
     * @param seedCut
     */
    void setSeedCut(double seedCut) {
        this.seedCut = seedCut;
    }
    
    /**
     * Set the min time in window to look for cluster
     * @param minTime
     */
    void setMinTime(double minTime) {
        this.minTime = minTime;
    }
    
    /**
     * Set the max time in window to look for cluster
     * @param maxTime
     */
    void setMaxTime(double maxTime) {
        this.maxTime = maxTime;
    }
    
    /**
     * Set the hit cut value for hits in cluster
     * This cut is used in 2016 running (not 2015)
     * @param hitCut
     */
    void setHitCut(int hitCut) {
        this.hitCut = hitCut;
    }
    
    /**
     * Set the hit cut value for hits in cluster
     * This cut is used in 2016 running (not 2015)
     * @param hitCut
     */
    void setUseHitCut(boolean useHitCut) {
        this.useHitCut = useHitCut;
    }
    
    public void process(EventHeader event) {
        aida.tree().cd("/");

        //only keep singles triggers:
        if (!event.hasCollection(GenericObject.class,"TriggerBank"))
            throw new Driver.NextEventException();
        boolean isSingles=false;
        for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
        {   
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
            TIData tid = new TIData(gob);
            if (tid.isSingle0Trigger()  || tid.isSingle1Trigger())
            {
                isSingles=true;
                break;
            }
        }
        
        if (isSingles){
            List<Cluster> clusters = event.get(Cluster.class, inputCollection);
            for (Cluster clus : clusters) {
                List<CalorimeterHit> hits = clus.getCalorimeterHits();
                CalorimeterHit seed = hits.get(0);
            
                double seedE = seed.getCorrectedEnergy();
                double clusE = clus.getEnergy();
                double time = seed.getTime();
                
                //in 2015, not hit count cut used at all
                if (useHitCut){
                    if (Math.abs(seed.getIdentifierFieldValue("iy"))==1 && (seedE/clusE > 0.6) && seedE >seedCut 
                        && time>minTime && time <maxTime && hits.size()>(hitCut+2) ){
            
                        EcalChannel cc = findChannel(seed);
                        aida.histogram1D(getHistoName(cc)).fill(clusE);
                    }
                    else if (Math.abs(seed.getIdentifierFieldValue("iy"))==1 && (seedE/clusE > 0.6) && seedE >seedCut 
                        && time>minTime && time <maxTime && hits.size()>(hitCut) ){
                        
                        EcalChannel cc = findChannel(seed);
                        aida.histogram1D(getHistoName(cc)).fill(clusE);
                    }
                }
                else {
                    if ((seedE/clusE > 0.6) && seedE >seedCut 
                            && time>minTime && time <maxTime ){
                
                            EcalChannel cc = findChannel(seed);
                            aida.histogram1D(getHistoName(cc)).fill(clusE);
                         
                    }   
                }   
            }
        }
    }
    
    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }
   
    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }

    public EcalChannel findChannel(CalorimeterHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    }
    
    public void endOfData() {
        System.out.println("OutputFile");
        if (outputPlots != null) {
            try {
                aida.saveAs("outputFEEPlots.root");
            } catch (IOException ex) {
                Logger.getLogger(FEEClusterPlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }   
}
