package org.hps.users.spaul.fee;

import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import static java.lang.Math.*;


public class FeeHistogramDriver extends Driver{

	double d0cut = 1;
	double z0cut = .5;

	//determined by beam energy
	double eMin, eMax, beamEnergy, seedEnergyCut;
	public double getSeedEnergyCutFrac() {
		return seedEnergyCutFrac;
	}

	public void setSeedEnergyCutFrac(double seedEnergyCut) {
		this.seedEnergyCutFrac = seedEnergyCut;
	}

	double pMin, pMax;


	double beamTiltX = .0295;
	double beamTiltY = -.0008;

	double maxChi2 = 50;
	//maximum difference between the reconstructed energy and momentum
	//double maxdE = .5;
	int nMin = 3;

	IHistogram1D theta;
	IHistogram1D thetaInRange;
	IHistogram2D[] thetaVsPhi;
	IHistogram2D[] thetaVsPhiNoTrackQualityCuts;
	IHistogram2D[] thetaVsPhiInRange;
	IHistogram2D d0VsZ0, d0VsZ0_top, d0VsZ0_bottom;
	IHistogram1D d0;
	IHistogram1D z0;
	IHistogram1D timeHist;
	IHistogram1D chi2Hist;
	private double seedEnergyCutFrac = .38;


	@Override
	public void detectorChanged(Detector det){
		BeamEnergyCollection beamEnergyCollection = 
			this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
		beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
		eMin = .75*beamEnergy;
		eMax = 1.15*beamEnergy;

		pMin = .75*beamEnergy;
		pMax = 1.15*beamEnergy;

		seedEnergyCut = seedEnergyCutFrac *beamEnergy; 
		setupHistograms();

	}

	AIDA aida = AIDA.defaultInstance();


	private IHistogram1D thetaTopOnly;


	private IHistogram1D thetaTopOnlyInRange;


	private IHistogram1D thetaBottomOnlyInRange;


	private IHistogram1D energy, seedEnergy, energyTop, seedEnergyTop, energyBottom, seedEnergyBottom;
	private IHistogram2D clusterVsSeedEnergy, clusterVsSeedEnergyTop, clusterVsSeedEnergyBottom;

	private IHistogram1D thetaBottomOnly;

	private IHistogram1D charge;
	private IHistogram1D clustSizeGTP;


	void setupHistograms(){


		energy = aida.histogram1D("cluster energy", 100, 0, 1.5*beamEnergy);
		energyTop = aida.histogram1D("cluster energy top", 100, 0, 1.5*beamEnergy);
		energyBottom = aida.histogram1D("cluster energy bottom", 100, 0, 1.5*beamEnergy);

		seedEnergy = aida.histogram1D("seed energy", 100, 0, beamEnergy);
		seedEnergyTop = aida.histogram1D("seed energy top", 100, 0, beamEnergy);
		seedEnergyBottom = aida.histogram1D("seed energy bottom", 100, 0, beamEnergy);

		clusterVsSeedEnergy = aida.histogram2D("cluster vs seed energy", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);
		clusterVsSeedEnergyTop = aida.histogram2D("cluster vs seed energy top", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);
		clusterVsSeedEnergyBottom = aida.histogram2D("cluster vs seed energy bottom", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);


		int nBinsPhi = 400;
		
		String[] regionNames = {"", " r1", " r2", " r3", " r4"};
		
		thetaVsPhi = new IHistogram2D[regionNames.length];
		thetaVsPhiInRange= new IHistogram2D[regionNames.length];
		thetaVsPhiNoTrackQualityCuts = new IHistogram2D[regionNames.length];
		thetaVsPhiChi2Cut= new IHistogram2D[regionNames.length];
		thetaVsPhiTrackExtrapCut = new IHistogram2D[regionNames.length];
		thetaVsPhiMomentumCut = new IHistogram2D[regionNames.length];		
		for(int i = 0; i< regionNames.length; i++){
			thetaVsPhi[i] = aida.histogram2D("theta vs phi" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
			thetaVsPhiInRange[i] = aida.histogram2D("theta vs phi in range" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
			thetaVsPhiNoTrackQualityCuts[i] = aida.histogram2D("theta vs phi no track quality cuts" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
			thetaVsPhiChi2Cut[i] = aida.histogram2D("theta vs phi chi2 cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
			thetaVsPhiTrackExtrapCut[i] = aida.histogram2D("theta vs phi track extrap cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
			thetaVsPhiMomentumCut[i] = aida.histogram2D("theta vs phi chi2 momentum cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
		}
		theta = aida.histogram1D("theta", nBinsPhi, 0, .2);
		thetaInRange = aida.histogram1D("theta in range", nBinsPhi, 0, .2);

		thetaTopOnly = aida.histogram1D("theta top", nBinsPhi, 0, .2);
		thetaTopOnlyInRange = aida.histogram1D("theta top in range", nBinsPhi, 0, .2);

		thetaBottomOnly = aida.histogram1D("theta bottom", nBinsPhi, 0, .2);
		thetaBottomOnlyInRange = aida.histogram1D("theta bottom in range", nBinsPhi, 0, .2);
		uxVsUy = aida.histogram2D("ux vs uy", 300, -.16, .24, 300, -.2, .2);
		uxVsUyInRange = aida.histogram2D("ux vs uy in range", 300, -.16, .24, 300, -.2, .2);
		d0VsZ0 = aida.histogram2D("d0 vs z0", 100, -5, 5, 100, -5, 5);
		d0 = aida.histogram1D("d0", 100, -5, 5);
		z0 = aida.histogram1D("z0", 100, -5, 5);

		d0VsZ0_top = aida.histogram2D("d0 vs z0 top", 100, -5, 5, 100, -5, 5);
		d0VsZ0_bottom = aida.histogram2D("d0 vs z0 bottom", 100, -5, 5, 100, -5, 5);


		pHist = aida.histogram1D("momentum", 100, 0, 1.5*beamEnergy);

		charge = aida.histogram1D("charge", 6, -3, 3);


		timeHist = aida.histogram1D("cluster time", 400, 0, 400);
		chi2Hist = aida.histogram1D("chi2", 200, 0, 200);
		chi2RedHist = aida.histogram1D("chi2 red", 200, 0, 40);
		clustSize = aida.histogram1D("cluster size", 20, 0, 20);
		clustSizeGTP = aida.histogram1D("cluster size gtp (>55% beam energy)", 20, 0, 20);

		z0VsTanLambda = aida.histogram2D("z0 vs tan lambda", 100, -5, 5, 100, -.1, .1);
		z0VsChi2 = aida.histogram2D("z0 vs chi2", 100, -5, 5, 100, 0, 100);
		nPassCutsPerEvent = aida.histogram1D("n pass cuts", 10, 0, 10);
		nPassCutsPerEventFiducial = aida.histogram1D("n pass cuts fiducial", 10, 0, 10);
		thetaVsMomentum = aida.histogram2D("theta vs energy", 100, 0, .2, 100, 0, 1.2*beamEnergy);
		thetaVsMomentumFid = aida.histogram2D("theta vs energy fiducial", 100, 0, .2, 100, 0, 1.2*beamEnergy);
	}


	IHistogram2D uxVsUy, uxVsUyInRange;

	IHistogram1D pHist;
	IHistogram1D clustSize;

	IHistogram1D nPassCutsPerEvent;
	IHistogram1D nPassCutsPerEventFiducial;
	private IHistogram1D chi2RedHist;
	private IHistogram2D thetaVsPhiChi2Cut[];
	private IHistogram2D thetaVsPhiMomentumCut[];
	private IHistogram2D thetaVsPhiTrackExtrapCut[];
	@Override
	public void process(EventHeader event){
		
		fillHistogramsGTP(event);
		
		//int col = getColumn(event);

		List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "FinalStateParticles");
		Cleanup.purgeParticlesWithSameCluster(particles);
		int nPassFidCuts = 0, nPassCuts = 0;
		for(ReconstructedParticle part : particles){

			
			//reject duplicate particles that use seed tracks instead of GBL
			if(!TrackType.isGBL(part.getType()) && part.getTracks().size() >0)
				continue;

			//reject particles that have no cluster in the Ecal
			if(part.getClusters().size() == 0)
				continue;
			Cluster c = part.getClusters().get(0);

			//cluster time
			double time = c.getCalorimeterHits().get(0).getTime();
			timeHist.fill(time);

			if(!(time>minClustTime && time <maxClustTime))
				continue;


			//find seed hit energy
			double seedEnergy = 0;
			int col = 0;
			for(CalorimeterHit hit : c.getCalorimeterHits()){
				if(hit.getCorrectedEnergy() > seedEnergy){
					seedEnergy = hit.getCorrectedEnergy();
					col = hit.getIdentifierFieldValue("ix");
				}
			}
			double energy = c.getEnergy();


			//these are the histograms of seed and cluster energy before
			//the cuts on these variables 
			this.seedEnergy.fill(seedEnergy);
			this.energy.fill(energy);
			this.clusterVsSeedEnergy.fill(energy, seedEnergy);

			if(part.getMomentum().y()> 0){
				this.seedEnergyTop.fill(seedEnergy);
				this.energyTop.fill(energy);
				this.clusterVsSeedEnergyTop.fill(energy, seedEnergy);
			} else {
				this.seedEnergyBottom.fill(seedEnergy);
				this.energyBottom.fill(energy);
				this.clusterVsSeedEnergyBottom.fill(energy, seedEnergy);
			}


			//seed energy cut
			if(seedEnergy < seedEnergyCut)
				continue;

			//cluster energy cut
			if(energy > eMax || energy< eMin)
				continue;

			clustSize.fill(c.getCalorimeterHits().size());
			if(c.getCalorimeterHits().size() < nMin)
				continue;



			charge.fill(part.getCharge());
			if(part.getCharge() != -1)
				continue;

			if(part.getTracks().size() == 0)
				continue;


			Hep3Vector p = part.getMomentum();
			double px = p.x(), py = p.y(), pz = p.z();

			double cx = cos(beamTiltX);
			double sy = sin(beamTiltY);
			double cy = cos(beamTiltY);
			double sx = sin(beamTiltX);

			double pxtilt = px*cx              -pz*sx;
			double pytilt = -py*sx*sy + py*cy  -pz*sy*cx;
			double pztilt = px*cy*sx  + py*sy  +pz*cy*cx;


			double theta = atan(hypot(pxtilt, pytilt)/pztilt);
			double phi =atan2(pytilt, pxtilt);
			fill(thetaVsPhiNoTrackQualityCuts,theta, phi, col);

			if(abs(getDx(part))>trackExtrapCut || abs(getDy(part))> trackExtrapCut)
				continue;
			fill(thetaVsPhiTrackExtrapCut, theta, phi, col);
			
			chi2Hist.fill(part.getTracks().get(0).getChi2());
			chi2RedHist.fill(part.getTracks().get(0).getChi2()/(2*part.getTracks().get(0).getTrackerHits().size()-5));

			if(part.getTracks().get(0).getChi2()>maxChi2)
				continue;
			fill(thetaVsPhiChi2Cut, theta, phi, col);
			
			double pmag = part.getMomentum().magnitude();
			pHist.fill(pmag);
			if(pmag > pMax || pmag < pMin)
				continue;
			fill(thetaVsPhiMomentumCut, theta, phi, col);

			double d = TrackUtils.getDoca(part.getTracks().get(0));
			d0.fill(d);
			double z = TrackUtils.getZ0(part.getTracks().get(0));
			z0.fill(z);
			d0VsZ0.fill(d, z);

			if(p.y() > 0)
				d0VsZ0_top.fill(d,z);
			else
				d0VsZ0_bottom.fill(d,z);

			z0VsTanLambda.fill(z, TrackUtils.getTanLambda(part.getTracks().get(0)));
			z0VsChi2.fill(z, part.getTracks().get(0).getChi2());

			if(abs(TrackUtils.getDoca(part.getTracks().get(0)))> d0cut)
				continue;
			if(abs(TrackUtils.getZ0(part.getTracks().get(0)))> z0cut)
				continue;



			



			/*
        double px = p.x(), pz = p.z();
        double pxtilt = px*cos(beamTiltX)-pz*sin(beamTiltX);
        double py = p.y();
        double pztilt = pz*cos(beamTiltX)+px*sin(beamTiltX);

        double pytilt = py*cos(beamTiltY)-pztilt*sin(beamTiltY);
        pztilt = pz*cos(beamTiltY) + pytilt*sin(beamTiltY);
			 */
			

			uxVsUy.fill(pxtilt/pztilt, pytilt/pztilt);
			fill(thetaVsPhi, theta, phi, col);
			this.theta.fill(theta);
			if(phi > 0)
				thetaTopOnly.fill(theta);
			else
				thetaBottomOnly.fill(theta);
			nPassCuts++;
			thetaVsMomentum.fill(theta, pz);
			if(cb.inRange(theta, phi)){
				fill(thetaVsPhiInRange, theta, phi, col);
				thetaInRange.fill(theta);
				if(phi > 0)
					thetaTopOnlyInRange.fill(theta);
				else
					thetaBottomOnlyInRange.fill(theta);
				uxVsUyInRange.fill(pxtilt/pztilt, pytilt/pztilt);
				nPassFidCuts++;
				thetaVsMomentumFid.fill(theta, pz);
			}
		}
		nPassCutsPerEvent.fill(nPassCuts);
		nPassCutsPerEventFiducial.fill(nPassFidCuts);
	}
	//returns the column of the seed hit of the trigger cluster in the GTP
	/*private int getColumn(EventHeader event) {
	    for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
	    {
	      if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
	      TIData tid = new TIData(gob);
	      
	    }
		
		
		List<Cluster> l = event.get(Cluster.class, "EcalClusters");
		double maxClusterEnergy = 0;
		int col = 0;
		for(Cluster c : l){
			if(c.getEnergy()>maxClusterEnergy){
				maxClusterEnergy = c.getEnergy();
				double maxHitEnergy = 0;
				for(CalorimeterHit hit : c.getCalorimeterHits()){
					if(hit.getCorrectedEnergy() > maxHitEnergy){
						col = hit.getIdentifierFieldValue("ix");
						maxHitEnergy = hit.getCorrectedEnergy();
					}
				}
			}
		}
		
		return col;
	}*/

	private void fill(IHistogram2D[] thetaVsPhiHist, double theta,
			double phi, int col) {
		thetaVsPhiHist[0].fill(theta, phi);
		if(col <= -13 || col>=6)
			thetaVsPhiHist[1].fill(theta, phi);
		else if (col <= -9 || col >= 2)
			thetaVsPhiHist[2].fill(theta, phi);
		else if (col <= -7 || col >= -2)
			thetaVsPhiHist[3].fill(theta, phi);
		else 
			thetaVsPhiHist[4].fill(theta, phi);
		
	}

	private void fillHistogramsGTP(EventHeader event) {
		if(event.hasCollection(Cluster.class, "EcalClusters"))
		for(Cluster c : event.get(Cluster.class, "EcalClusters")){
			double energy = c.getEnergy();
			if(energy>beamEnergy*.55)
				clustSizeGTP.fill(c.getSize());
		}
	}

	public void setBinning(String binning){
		System.out.println("binning scheme:\n" + binning);
		this.cb = new CustomBinning(binning);
	}
	
	CustomBinning cb;
	double minClustTime = 40;
	double maxClustTime = 50;


	public double getMinClustTime() {
		return minClustTime;
	}



	public void setMinClustTime(double minClustTime) {
		this.minClustTime = minClustTime;
	}



	public double getMaxClustTime() {
		return maxClustTime;
	}



	public void setMaxClustTime(double maxClustTime) {
		this.maxClustTime = maxClustTime;
	}

	IHistogram2D z0VsTanLambda;
	IHistogram2D z0VsChi2;
	IHistogram2D thetaVsMomentumFid;
	IHistogram2D thetaVsMomentum;
	
	public double trackExtrapCut = 10;
	public static double getDx(ReconstructedParticle p){
		double xc = p.getClusters().get(0).getPosition()[0];
		double xt = TrackUtils.getTrackPositionAtEcal(p.getTracks().get(0)).x();
		return xt-xc;
	}

	public static double getDy(ReconstructedParticle p) {
		double yc = p.getClusters().get(0).getPosition()[1];
		double yt = TrackUtils.getTrackPositionAtEcal(p.getTracks().get(0)).y();
		return yt-yc;
	}
}
