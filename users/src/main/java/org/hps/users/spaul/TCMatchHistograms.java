package org.hps.users.spaul;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class TCMatchHistograms extends Driver{
    private IHistogram1D allElectrons;
    private IHistogram1D allPositrons;
    private IHistogram1D fidPositrons;
    private IHistogram1D fidElectrons;
    private IHistogram1D fidTop;
    private IHistogram1D fidBottom;
    
    private IHistogram1D fidTopElec5;
    private IHistogram1D fidBotElec5;
    private IHistogram1D fidBotPosi5;
    private IHistogram1D fidTopPosi5;
    private IHistogram1D fidTopElec6;
    private IHistogram1D fidBotElec6;
    private IHistogram1D fidBotPosi6;
    private IHistogram1D fidTopPosi6;
    
    private IHistogram2D allElectronsVsPz;
    private IHistogram2D allPositronsVsPz;
    private IHistogram2D fidPositronsVsPz;
    private IHistogram2D fidElectronsVsPz;
    private IHistogram2D fidTopVsPz;
    private IHistogram2D fidBottomVsPz;
    
    private IHistogram2D fidTopElec5VsPz;
    private IHistogram2D fidBotElec5VsPz;
    private IHistogram2D fidBotPosi5VsPz;
    private IHistogram2D fidTopPosi5VsPz;
    private IHistogram2D fidTopElec6VsPz;
    private IHistogram2D fidBotElec6VsPz;
    private IHistogram2D fidBotPosi6VsPz;
    private IHistogram2D fidTopPosi6VsPz;
    
    private IHistogram2D allElectronsDxVsPz;
    private IHistogram2D allPositronsDxVsPz;
    private IHistogram2D fidPositronsDxVsPz;
    private IHistogram2D fidElectronsDxVsPz;
    private IHistogram2D fidTopDxVsPz;
    private IHistogram2D fidBottomDxVsPz;
    
    private IHistogram2D fidTopElec5DxVsPz;
    private IHistogram2D fidBotElec5DxVsPz;
    private IHistogram2D fidBotPosi5DxVsPz;
    private IHistogram2D fidTopPosi5DxVsPz;
    private IHistogram2D fidTopElec6DxVsPz;
    private IHistogram2D fidBotElec6DxVsPz;
    private IHistogram2D fidBotPosi6DxVsPz;
    private IHistogram2D fidTopPosi6DxVsPz;
    
    private IHistogram2D allElectronsDyVsPz;
    private IHistogram2D allPositronsDyVsPz;
    private IHistogram2D fidPositronsDyVsPz;
    private IHistogram2D fidElectronsDyVsPz;
    private IHistogram2D fidTopDyVsPz;
    private IHistogram2D fidBottomDyVsPz;
    
    private IHistogram2D fidTopElec5DyVsPz;
    private IHistogram2D fidBotElec5DyVsPz;
    private IHistogram2D fidBotPosi5DyVsPz;
    private IHistogram2D fidTopPosi5DyVsPz;
    private IHistogram2D fidTopElec6DyVsPz;
    private IHistogram2D fidBotElec6DyVsPz;
    private IHistogram2D fidBotPosi6DyVsPz;
    private IHistogram2D fidTopPosi6DyVsPz;
    
    private IHistogram1D nParticles;
    private IHistogram1D nFidParticles;
    TrackClusterMatcher matcher;
    @Override
    protected void detectorChanged(Detector d){
        AIDA aida = AIDA.defaultInstance();

        allElectrons = aida.histogram1D("nsigma/all_electrons", 100, 0, 10);
        allPositrons = aida.histogram1D("nsigma/all_positrons", 100, 0, 10);
        
        fidElectrons = aida.histogram1D("nsigma/fid_electrons", 100, 0, 10);
        fidPositrons = aida.histogram1D("nsigma/fid_positrons", 100, 0, 10);
        
        fidTop = aida.histogram1D("nsigma/fid_top", 100, 0, 10);
        fidBottom = aida.histogram1D("nsigma/fid_bot", 100, 0, 10);
        
        fidTopElec5 = aida.histogram1D("nsigma/fid_top_electrons_5", 100, 0, 10);
        fidBotElec5 = aida.histogram1D("nsigma/fid_bot_electrons_5", 100, 0, 10);
        
        fidTopPosi5 = aida.histogram1D("nsigma/fid_top_positrons_5", 100, 0, 10);
        fidBotPosi5 = aida.histogram1D("nsigma/fid_bot_positrons_5", 100, 0, 10);
        
        fidTopElec6 = aida.histogram1D("nsigma/fid_top_electrons_6", 100, 0, 10);
        fidBotElec6 = aida.histogram1D("nsigma/fid_bot_electrons_6", 100, 0, 10);
        
        fidTopPosi6 = aida.histogram1D("nsigma/fid_top_positrons_6", 100, 0, 10);
        fidBotPosi6 = aida.histogram1D("nsigma/fid_bot_positrons_6", 100, 0, 10);
        
        
        //nsigma vs pz
        allElectronsVsPz = aida.histogram2D("pz_vs_nsigma/all_electrons", 100, 0, 3, 100, 0, 10);
        allPositronsVsPz = aida.histogram2D("pz_vs_nsigma/all_positrons", 100, 0, 3, 100, 0, 10);
        
        fidElectronsVsPz = aida.histogram2D("pz_vs_nsigma/fid_electrons", 100, 0, 3, 100, 0, 10);
        fidPositronsVsPz = aida.histogram2D("pz_vs_nsigma/fid_positrons", 100, 0, 3, 100, 0, 10);
        
        fidTopVsPz = aida.histogram2D("pz_vs_nsigma/fid_top", 100, 0, 3, 100, 0, 10);
        fidBottomVsPz = aida.histogram2D("pz_vs_nsigma/fid_bot", 100, 0, 3, 100, 0, 10);
        
        fidTopElec5VsPz = aida.histogram2D("pz_vs_nsigma/fid_top_electrons_5", 100, 0, 3, 100, 0, 10);
        fidBotElec5VsPz = aida.histogram2D("pz_vs_nsigma/fid_bot_electrons_5", 100, 0, 3, 100, 0, 10);
        
        fidTopPosi5VsPz = aida.histogram2D("pz_vs_nsigma/fid_top_positrons_5", 100, 0, 3, 100, 0, 10);
        fidBotPosi5VsPz = aida.histogram2D("pz_vs_nsigma/fid_bot_positrons_5", 100, 0, 3, 100, 0, 10);
        
        fidTopElec6VsPz = aida.histogram2D("pz_vs_nsigma/fid_top_electrons_6", 100, 0, 3, 100, 0, 10);
        fidBotElec6VsPz = aida.histogram2D("pz_vs_nsigma/fid_bot_electrons_6", 100, 0, 3, 100, 0, 10);
        
        fidTopPosi6VsPz = aida.histogram2D("pz_vs_nsigma/fid_top_positrons_6", 100, 0, 3, 100, 0, 10);
        fidBotPosi6VsPz = aida.histogram2D("pz_vs_nsigma/fid_bot_positrons_6", 100, 0, 3, 100, 0, 10);
        
        //nsigmax vs pz 
        allElectronsDxVsPz = aida.histogram2D("pz_vs_nsigma_x/all_electrons", 100, 0, 3, 100, -10, 10);
        allPositronsDxVsPz = aida.histogram2D("pz_vs_nsigma_x/all_positrons", 100, 0, 3, 100, -10, 10);
        
        fidElectronsDxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_electrons", 100, 0, 3, 100, -10, 10);
        fidPositronsDxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_positrons", 100, 0, 3, 100, -10, 10);
        
        fidTopDxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_top", 100, 0, 3, 100, -10, 10);
        fidBottomDxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_bot", 100, 0, 3, 100, -10, 10);
        
        fidTopElec5DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_top_electrons_5", 100, 0, 3, 100, -10, 10);
        fidBotElec5DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_bot_electrons_5", 100, 0, 3, 100, -10, 10);
        
        fidTopPosi5DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_top_positrons_5", 100, 0, 3, 100, -10, 10);
        fidBotPosi5DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_bot_positrons_5", 100, 0, 3, 100, -10, 10);
        
        fidTopElec6DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_top_electrons_6", 100, 0, 3, 100, -10, 10);
        fidBotElec6DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_bot_electrons_6", 100, 0, 3, 100, -10, 10);
        
        fidTopPosi6DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_top_positrons_6", 100, 0, 3, 100, -10, 10);
        fidBotPosi6DxVsPz = aida.histogram2D("pz_vs_nsigma_x/fid_bot_positrons_6", 100, 0, 3, 100, -10, 10);
        
        //nsigmay vs pz
        allElectronsDyVsPz = aida.histogram2D("pz_vs_nsigma_y/all_electrons", 100, 0, 3, 100, -10, 10);
        allPositronsDyVsPz = aida.histogram2D("pz_vs_nsigma_y/all_positrons", 100, 0, 3, 100, -10, 10);
        
        fidElectronsDyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_electrons", 100, 0, 3, 100, -10, 10);
        fidPositronsDyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_positrons", 100, 0, 3, 100, -10, 10);
        
        fidTopDyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_top", 100, 0, 3, 100, -10, 10);
        fidBottomDyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_bot", 100, 0, 3, 100, -10, 10);
        
        fidTopElec5DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_top_electrons_5", 100, 0, 3, 100, -10, 10);
        fidBotElec5DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_bot_electrons_5", 100, 0, 3, 100, -10, 10);
        
        fidTopPosi5DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_top_positrons_5", 100, 0, 3, 100, -10, 10);
        fidBotPosi5DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_bot_positrons_5", 100, 0, 3, 100, -10, 10);
        
        fidTopElec6DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_top_electrons_6", 100, 0, 3, 100, -10, 10);
        fidBotElec6DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_bot_electrons_6", 100, 0, 3, 100, -10, 10);
        
        fidTopPosi6DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_top_positrons_6", 100, 0, 3, 100, -10, 10);
        fidBotPosi6DyVsPz = aida.histogram2D("pz_vs_nsigma_y/fid_bot_positrons_6", 100, 0, 3, 100, -10, 10);
        
        
        nFidParticles = aida.histogram1D("nparticles_fid", 10, 0, 10);
        nParticles = aida.histogram1D("nparticles", 10, 0, 10);
        matcher = new TrackClusterMatcher();
        //matcher.set
    }
    @Override
    protected void process(EventHeader event){
        int count = 0, fidcount = 0;
        for(ReconstructedParticle p : event.get(ReconstructedParticle.class, "FinalStateParticles")){
            if(p.getClusters().size() == 0 || p.getTracks().size() == 0)
                continue;
            if(p.getType() <32)
                continue;
            if(p.getMomentum().z()< .5)
                continue;
            Cluster c = p.getClusters().get(0);
            Track t = p.getTracks().get(0);
            double nsigma = matcher.getNSigmaPosition(c, t);
            
            double nsigmax = matcher.getNSigmaPositionX(c, t);
            double nsigmay = matcher.getNSigmaPositionY(c, t);
            boolean isTop = TrackUtils.getTanLambda(t)> 0;
            boolean isElec = TrackUtils.getCharge(t) < 0;
            boolean fiducial = c.getPosition()[0] > -262.74 && c.getPosition()[0] < 347.7 && Math.abs(c.getPosition()[1])>33.54 
                    && Math.abs(c.getPosition()[1])<75.18 
                    && !(c.getPosition()[0]>-106.66 && c.getPosition()[0] < 42.17 && Math.abs(c.getPosition()[1])<47.17);
            count++;
            if(fiducial){
                fidcount++;
            }
            
            boolean hasL6 = false;
            for(TrackerHit hit : t.getTrackerHits()){
                if(TrackUtils.getLayer(hit) == 11)
                    hasL6 = true;
            }
            
            double pz =  1.57e-4*Math.abs(TrackUtils.getR(t));
           // System.out.println(nsigma);
            if(isElec){
                allElectrons.fill(nsigma);
                allElectronsVsPz.fill(pz, nsigma);
                allElectronsDxVsPz.fill(pz, nsigmax);
                allElectronsDyVsPz.fill(pz, nsigmay);
                if(fiducial){
                    fidElectrons.fill(nsigma);
                    fidElectronsVsPz.fill(pz, nsigma);
                    fidElectronsDxVsPz.fill(pz, nsigmax);
                    fidElectronsDyVsPz.fill(pz, nsigmay);
                    if(isTop)
                        if(!hasL6){
                            fidTopElec5.fill(nsigma);
                            fidTopElec5VsPz.fill(pz, nsigma);
                            fidTopElec5DxVsPz.fill(pz, nsigmax);
                            fidTopElec5DyVsPz.fill(pz, nsigmay);
                        }
                        else{
                            fidTopElec6.fill(nsigma);
                            fidTopElec6VsPz.fill(pz, nsigma);
                            fidTopElec6DxVsPz.fill(pz,nsigmax);
                            fidTopElec6DyVsPz.fill(pz,nsigmay);
                        }
                    else
                        if(!hasL6){
                            fidBotElec5.fill(nsigma);
                            fidBotElec5VsPz.fill(pz,nsigma);
                            fidBotElec5DxVsPz.fill(pz,nsigmax);
                            fidBotElec5DyVsPz.fill(pz,nsigmay);
                        }
                        else{
                            fidBotElec6.fill(nsigma);
                            fidBotElec6VsPz.fill(pz,nsigma);
                            fidBotElec6DxVsPz.fill(pz,nsigmax);
                            fidBotElec6DyVsPz.fill(pz,nsigmay);
                        }
                }
            } else {
                allPositrons.fill(nsigma);
                allPositronsVsPz.fill(pz, nsigma);
                allPositronsDxVsPz.fill(pz, nsigmax);
                allPositronsDyVsPz.fill(pz, nsigmay);
                if(fiducial){
                    fidPositrons.fill(nsigma);
                    fidPositronsVsPz.fill(pz, nsigma);
                    fidPositronsDxVsPz.fill(pz, nsigmax);
                    fidPositronsDyVsPz.fill(pz, nsigmay);
                    if(isTop)
                        if(!hasL6){
                            fidTopPosi5.fill(nsigma);
                            fidTopPosi5VsPz.fill(pz,nsigma);
                            fidTopPosi5DxVsPz.fill(pz,nsigmax);
                            fidTopPosi5DyVsPz.fill(pz,nsigmay);
                        }
                        else{
                            fidTopPosi6.fill(nsigma);
                            fidTopPosi6VsPz.fill(pz,nsigma);
                            fidTopPosi6DxVsPz.fill(pz,nsigmax);
                            fidTopPosi6DyVsPz.fill(pz,nsigmay);
                        }
                    else
                        if(!hasL6){
                            fidBotPosi5.fill(nsigma);
                            fidBotPosi5VsPz.fill(pz,nsigma);
                            fidBotPosi5DxVsPz.fill(pz,nsigmax);
                            fidBotPosi5DyVsPz.fill(pz,nsigmay);
                        }
                        else{
                            fidBotPosi6.fill(nsigma);
                            fidBotPosi6VsPz.fill(pz,nsigma);
                            fidBotPosi6DxVsPz.fill(pz,nsigmax);
                            fidBotPosi6DyVsPz.fill(pz,nsigmay);
                        }
                }
            }
            
            if(isTop && fiducial){
                fidTop.fill(nsigma);
                fidTopVsPz.fill(pz,nsigma);
                fidTopDxVsPz.fill(pz,nsigmax);
                fidTopDyVsPz.fill(pz,nsigmay);
            }
            else if(!isTop && fiducial){
                fidBottom.fill(nsigma);
                fidBottomVsPz.fill(pz,nsigma);
                fidBottomDxVsPz.fill(pz,nsigmax);
                fidBottomDyVsPz.fill(pz,nsigmay);
            }
            
        }
        nParticles.fill(count);
        nFidParticles.fill(fidcount);
    }
}
