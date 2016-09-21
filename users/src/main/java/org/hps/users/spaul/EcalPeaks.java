package org.hps.users.spaul;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;

public class EcalPeaks extends Driver {
	private static final Logger LOGGER = Logger.getLogger(EcalPeaks.class.getPackage().getName());

	private String clusterCollection = "EcalClustersCorr";
	private String particleCollection = "FinalStateParticles";
	
	protected Double beamEnergy;
    public void setBeamEnergy(double e){
    this.beamEnergy = e;
    }
    public double getBeamEnergy(){
    return this.beamEnergy;
    }
    @Override
    protected void detectorChanged(Detector detector){
        BeamEnergyCollection beamEnergyCollection = 
            this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        if(beamEnergy== null && beamEnergyCollection != null && beamEnergyCollection.size() != 0)
            beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        else{
            LOGGER.log(Level.WARNING, "warning:  beam energy not found.  Using a 6.6 GeV as the default energy");
            beamEnergy = 6.6;
        }
        
        oneClusterPeak = aida.histogram1D("one_cluster_peak", 200, 0, 
        		1.5*beamEnergy);
        twoClusterPeak = aida.histogram1D("two_cluster_peak", 200, 0, 
        		1.5*beamEnergy);
        oneClusterPeakTrackAssist = aida.histogram1D("one_cluster_peak_ta", 200, 0, 
        		1.5*beamEnergy);
        twoClusterPeakTrackAssist = aida.histogram1D("two_cluster_peak_ta", 200, 0, 
        		1.5*beamEnergy);
        pairClusterEnergyDiffCut = beamEnergy*.5;
        pairClusterFeeCut = beamEnergy*.8;
       
    }
    
    private IHistogram1D oneClusterPeak, twoClusterPeak,
    oneClusterPeakTrackAssist,
    twoClusterPeakTrackAssist, timestamps;
	AIDA aida = AIDA.defaultInstance();

	private double pairClusterTimeCut = 2;
	private double pairClusterEnergyDiffCut;
	private double pairClusterFeeCut;
	private boolean firstEvent = true;
	
	//private long firsttimestamp = 0;
	
	
	@Override
	public void process(EventHeader event){
		int runNumber = event.getRunNumber();
		
		long timestamp = event.getTimeStamp();
		//System.out.println(timestamp);
		
		if(firstEvent){
			long firsttimestamp = timestamp;
			double n_minutes = 5; //anything higher than this, and there was probably a beam trip during this file.
			timestamps = aida.histogram1D("timestamps", 200, firsttimestamp, firsttimestamp+n_minutes*60000);
		}
		timestamps.fill(timestamp);
		firstEvent = false;
		boolean isSingle1 = false;
		boolean isPair1 = false;
		for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
		{
			if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
			TIData tid = new TIData(gob);
			if (tid.isPair1Trigger()) isPair1 = true;
			if (tid.isSingle1Trigger()) isSingle1 = true;
		}
		
		if(!isSingle1 && !isPair1)
			return;

		List<Cluster> clusters = event.get(Cluster.class, clusterCollection);
		List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, particleCollection);
		if(isSingle1)
			processSingle1(clusters, particles, event);
		if(isPair1)
			processPair1(clusters, particles, event);
	}
	
	
	
	private void processSingle1(List<Cluster> clusters, List<ReconstructedParticle> particles, EventHeader event) {
		

		fill1ClusterPeak(clusters, oneClusterPeak);
		//first filter out any events that don't have a track with at least 90\% of the beam energy.  
		//This will significantly reduce the size of the left-side tail of the Ecal measured energy distribution.
		ReconstructedParticle feeFound = null;
		for(ReconstructedParticle p: particles){
			if(p.getMomentum().z() > .9*beamEnergy && p.getTracks().size() != 0){
				feeFound = p;
				break;
			}
		}
		if(feeFound == null)
			return;
		fill1ClusterPeak(clusters, oneClusterPeakTrackAssist);
		
		
	}
	
	void fill1ClusterPeak(List<Cluster> clusters, IHistogram1D oneClusterPeak){
		for(Cluster c1 : clusters){
			if(c1.getSize() < 3)
				continue;
			if(isEdge(c1))
				continue;
			oneClusterPeak.fill(c1.getEnergy());
				
		}
	}
	private boolean isEdge(Cluster c){
		CalorimeterHit seed = c.getCalorimeterHits().get(0);
		int ix = seed.getIdentifierFieldValue("ix");
		int iy = seed.getIdentifierFieldValue("iy");
		return isEdge(ix, iy);
			
	}
	private boolean isEdge(int ix, int iy){
        if(iy == 5 || iy == 1 || iy == -1 || iy == -5)
            return true;
        if(ix == -23 || ix == 23)
            return true;
        if((iy == 2 || iy == -2) && (ix >=-11 && ix <= -1))
            return true;
        return false;
    }
	private void processPair1(List<Cluster> clusters, List<ReconstructedParticle> particles, EventHeader event) {
		
		fill2ClusterPeak(clusters, twoClusterPeak);
		
		//first, filter out undeserirable events using the SVT.  
		boolean found = false;
		for(ReconstructedParticle top : particles){
			if(top.getMomentum().y() < 0)
				continue;
			if(top.getMomentum().z() > .9*beamEnergy)
				continue;
			if(top.getTracks().size() == 0)
				continue;
			for(ReconstructedParticle bot : particles){
				if(bot.getMomentum().y()>0)
					continue;
				if(bot.getMomentum().z() > .9*beamEnergy)
					continue;
				if(bot.getTracks().size() == 0)
					continue;
				double pztot = top.getMomentum().z() + bot.getMomentum().z();
				if(pztot > .8*beamEnergy && pztot < 1.2*beamEnergy){
					found = true;
					break;
				}
			}
		}
		if(! found)
			return;
		
		fill2ClusterPeak(clusters, twoClusterPeakTrackAssist);
		
	}
	
	void fill2ClusterPeak(List<Cluster> clusters, IHistogram1D twoClusterPeak){
		for(Cluster top : clusters){
			//make sure it is not a FEE.
			if(top.getEnergy() > pairClusterFeeCut)
				continue;
			//nor an edge crystal
			if(isEdge(top))
				continue;
			if(top.getPosition()[1]< 0)
				continue;
			for(Cluster bot: clusters){
				//make sure it's not a FEE
				if(bot.getEnergy() > pairClusterFeeCut)
					continue;
				//nor an edge crystal
				if(isEdge(bot))
					continue;
				//make sure it's on the opposite side of the detector
				if(bot.getPosition()[1]>0)
					continue;
				//cluster time cut
				if(Math.abs(top.getCalorimeterHits().get(0).getTime()-bot.getCalorimeterHits().get(0).getTime())>pairClusterTimeCut )
					continue;
				
				
				twoClusterPeak.fill(bot.getEnergy()+ top.getEnergy());
			}
		}
	}
}
