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
    double pMin, pMax;
    

    double beamTiltX = .0295;
    double beamTiltY = -.0008;
    
    double maxChi2 = 50;
    //maximum difference between the reconstructed energy and momentum
    //double maxdE = .5;
    int nMin = 3;

    IHistogram1D theta;
    IHistogram1D thetaInRange;
    IHistogram2D thetaVsPhi;
    IHistogram2D thetaVsPhiInRange;
    IHistogram2D d0VsZ0, d0VsZ0_top, d0VsZ0_bottom;
    IHistogram1D d0;
    IHistogram1D z0;
    IHistogram1D timeHist;
    IHistogram1D chi2Hist;
    
    
    @Override
    public void detectorChanged(Detector det){
        BeamEnergyCollection beamEnergyCollection = 
            this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        eMin = .75*beamEnergy;
        eMax = 1.15*beamEnergy;
        
        pMin = .75*beamEnergy;
        pMax = 1.15*beamEnergy;
        
        seedEnergyCut = .38*beamEnergy; 
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
        thetaVsPhi = aida.histogram2D("theta vs phi", nBinsPhi, 0, .2, 628, -3.14, 3.14);
        thetaVsPhiInRange = aida.histogram2D("theta vs phi in range", nBinsPhi, 0, .2, 628, -3.14, 3.14);
        
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
        clustSize = aida.histogram1D("cluster size", 20, 0, 20);
        
        z0VsTanLambda = aida.histogram2D("z0 vs tan lambda", 100, -5, 5, 100, -.1, .1);
        z0VsChi2 = aida.histogram2D("z0 vs chi2", 100, -5, 5, 100, 0, 100);
        
    }
    
    
    IHistogram2D uxVsUy, uxVsUyInRange;
    
    IHistogram1D pHist;
    IHistogram1D clustSize;
    
    @Override
    public void process(EventHeader event){
    	
    	
    	
    	//removed this criterion from this driver.
    	//instead use a trigger filter driver preceding this driver in the steering file. 
    	//
    	
        /*for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
        {
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
            TIData tid = new TIData(gob);
            if (!tid.isSingle1Trigger())
            {
                return;
            }
        }*/
        
        List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "FinalStateParticles");
        Cleanup.purgeDuplicates(particles);
        for(ReconstructedParticle p : particles){
            processParticle(p);
        }
    }

   
	private void processParticle(ReconstructedParticle part) {
        
        //reject duplicate particles that use seed tracks instead of GBL
        if(!TrackType.isGBL(part.getType()) && part.getTracks().size() >0)
            return;
        
        //reject particles that have no cluster in the Ecal
        if(part.getClusters().size() == 0)
            return;
        Cluster c = part.getClusters().get(0);
        
      //cluster time
        double time = c.getCalorimeterHits().get(0).getTime();
        timeHist.fill(time);
        
        if(!(time>minClustTime && time <maxClustTime))
            return;
        
        
        //find seed hit energy
        double seedEnergy = 0;
        for(CalorimeterHit hit : c.getCalorimeterHits()){
            if(hit.getCorrectedEnergy() > seedEnergy)
                seedEnergy = hit.getCorrectedEnergy();
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
            return;
        
        //cluster energy cut
        if(energy > eMax || energy< eMin)
            return;
        
        clustSize.fill(c.getCalorimeterHits().size());
        if(c.getCalorimeterHits().size() < nMin)
            return;
        
        
        
        charge.fill(part.getCharge());
        if(part.getCharge() != -1)
            return;
        
        if(part.getTracks().size() == 0)
            return;
        
        
        
        

        Hep3Vector p = part.getMomentum();

        double pmag = part.getMomentum().magnitude();
        pHist.fill(pmag);
        if(pmag > pMax || pmag < pMin)
            return;
        
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
        	return;
        if(abs(TrackUtils.getZ0(part.getTracks().get(0)))> z0cut)
        	return;
        
        
        
        chi2Hist.fill(part.getTracks().get(0).getChi2());
        if(part.getTracks().get(0).getChi2()>maxChi2)
            return;
        


/*
        double px = p.x(), pz = p.z();
        double pxtilt = px*cos(beamTiltX)-pz*sin(beamTiltX);
        double py = p.y();
        double pztilt = pz*cos(beamTiltX)+px*sin(beamTiltX);
        
        double pytilt = py*cos(beamTiltY)-pztilt*sin(beamTiltY);
        pztilt = pz*cos(beamTiltY) + pytilt*sin(beamTiltY);
*/
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
        
        uxVsUy.fill(pxtilt/pztilt, pytilt/pztilt);
        thetaVsPhi.fill(theta, phi);
        this.theta.fill(theta);
        if(phi > 0)
            thetaTopOnly.fill(theta);
        else
            thetaBottomOnly.fill(theta);
        if(cb.inRange(theta, phi)){
            thetaVsPhiInRange.fill(theta, phi);
            thetaInRange.fill(theta);
            if(phi > 0)
                thetaTopOnlyInRange.fill(theta);
            else
                thetaBottomOnlyInRange.fill(theta);
            uxVsUyInRange.fill(pxtilt/pztilt, pytilt/pztilt);
        }
    }
    
    public void setBinning(String binning){
        System.out.println("binning scheme:\n" + binning);
        this.cb = new CustomBinning(binning);
    }
    /*
    @Override
    protected void endOfData(){
        createRenormalizationHistogram();
    }
    
    private void createRenormalizationHistogram(){
    	
        //the purpose of this histogram is so that there is a simple histogram
        //that each bin in the theta histograms can be normalized to the 
        //angular range in phi that I am using for the cut inside of that theta bin.
        
        int xbins = thetaVsPhi.xAxis().bins();
        double xmin = thetaVsPhi.xAxis().lowerEdge();
        double xmax = thetaVsPhi.xAxis().upperEdge();
        double ymin = thetaVsPhi.xAxis().lowerEdge();
        double ymax = thetaVsPhi.xAxis().upperEdge();
        
        IHistogram1D phiHist = aida.histogram1D("phiWidth", xbins, xmin, xmax);

        for(int i = 0; i< cb.nTheta; i++){
            double phiWidth = cb.getPhiWidth(i);
            for(int j = 0; j<phiWidth*1000; j++){
                phiHist.fill(cb.thetaMin[i]+.001);
            }
        }

        
    }*/
    
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
	
}
