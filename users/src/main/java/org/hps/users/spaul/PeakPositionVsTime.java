package org.hps.users.spaul;

import java.util.List;

import hep.aida.IHistogram1D;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.ecal.EcalRawConverter;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class PeakPositionVsTime extends Driver{
	
	
	AIDA aida = AIDA.defaultInstance();
	
	private int skipEvents = 10000;
	private int samplesPerPrint = 1000;
	IHistogram1D radPeak, feePeak;
	public void setSamplesPerPrint(int samplesPerPrint){
		this.samplesPerPrint = samplesPerPrint;
	}
	public void setSkipEvents(int skipEvents){
		this.skipEvents = skipEvents;
	}
	double beamEnergy = 0;
	public void startOfData(){
		
		BeamEnergyCollection beamEnergyCollection = 
            this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        if(beamEnergyCollection != null && beamEnergyCollection.size() != 0)
            beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
		radPeak = aida.histogram1D("radpeak", 200, 0, 1.5*beamEnergy);
		feePeak = aida.histogram1D("feepeak", 200, 0, 1.5*beamEnergy);
		
	}
	
	
	@Override
	public void process(EventHeader event){
		int eventNum = event.getEventNumber();
		if(eventNum < skipEvents)
			return;
		if(eventNum % samplesPerPrint != 0){
			double feePeakValue = getMax(feePeak);
			double radPeakValue = getMax(radPeak);
			long time = event.getTimeStamp();
			System.out.println(time + "\t" + eventNum + "\t" + feePeakValue + "\t" + radPeakValue);
			radPeak.reset();
			feePeak.reset();
		}
		
		
		boolean singles1 = false, pairs1 = false;
		for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
	    {
	      if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
	      TIData tid = new TIData(gob);
	      if (tid.isSingle1Trigger()) singles1 = true;
	      if (tid.isPair1Trigger()) pairs1 = true;
	    }
		
		
		if(! singles1 && ! pairs1)
			return;
		List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
		if(singles1){
			for(Cluster cluster : clusters){
				feePeak.fill(cluster.getEnergy());
			}
		}
		if(pairs1){
			for(Cluster c1 : clusters){
				for(Cluster c2 : clusters){
					double e1 = c1.getEnergy();
					double e2 = c2.getEnergy();
					if(e1 < .8*beamEnergy && e2 < .8*beamEnergy //avoid FEEs
							&& c1.getPosition()[1]*c2.getPosition()[1]<0 //both on opposite halves of detector
							){
						radPeak.fill(e1+e2);
					}
				}
			}
		}
		
	}
	double getMax(IHistogram1D hist){
		int peakBin = 0;
		for(int i = 0; i< hist.axis().bins(); i++){
			if(hist.binHeight(i) > hist.binHeight(peakBin))
				peakBin = i;
		}
		
		
		return (hist.axis().binLowerEdge(peakBin)+hist.axis().binUpperEdge(peakBin))/2.;
	}
}
