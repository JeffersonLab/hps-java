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
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getEcalConditions();
        
        aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            //aida.histogram1D(getHistoName(cc),200,0.5,1.3);
        	aida.histogram1D(getHistoName(cc),200,0.6,2.8);	
        }

    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat,cc.getChannelId());
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
        		
        		//if ((seedE/clusE > 0.6) && seedE >0.45 && time>30 && time <70){
        		if ((seedE/clusE > 0.6) && seedE >0.6 && time>30 && time <70){
        	
        			EcalChannel cc = findChannel(seed);
        			aida.histogram1D(getHistoName(cc)).fill(clusE);
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
