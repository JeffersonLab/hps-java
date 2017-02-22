package org.hps.users.spaul;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import hep.aida.IHistogram1D;


public class MassHistogramDriver extends Driver{
    AIDA aida = AIDA.defaultInstance();

    private double goodnessPidThreshold = 10;
    private double feeThreshold = 1.750;
    private double pzTotThreshold = 2.760;
    private double radThreshold = 1.750;
    private double trackChi2Threshold = 40;
    private double trackClusterTimeDiffThresholdMean = 55;
    private double trackClusterTimeDiffThresholdAbs = 4.5;
    
    public void setMeanClusterTrackDt(double val){
        trackClusterTimeDiffThresholdMean = val;
    }
    public void setAbsClusterTrackDt(double val){
        trackClusterTimeDiffThresholdAbs = val;
    }
   /* boolean corrD0 = false;
    //correct the track D0 by subtracting 5 mm * phi0.  
    public void setTweakD0(boolean val){
        corrD0 = val;
    }*/
    
    private double posD0Threshold = 1.0;
    private double clusterTimeDiffThreshold = 2;
    
    int nMassBins = 6000;
    double maxMass = .3;
    
    int nCDTbins = 100;
    double maxCDT = 10;
    
    // no cuts except event flags, GBL, clusters in opposite volumes, and tc match chi2 < 10.    
    // if tracks have shared hits, choose the track with the better fit chi2. (may change this criteria)
    
    IHistogram1D massPreliminary = aida.histogram1D("mass_pre", nMassBins, 0, maxMass); 
    IHistogram1D cdtPreliminary = aida.histogram1D("cluster_dt_pre", nCDTbins, -maxCDT, maxCDT); 
    // remove fees
    IHistogram1D massNoFee =  aida.histogram1D("mass_fee_cut", nMassBins, 0, maxMass); 
    IHistogram1D cdtNoFee = aida.histogram1D("cluster_dt_fee_cut", nCDTbins, -maxCDT, maxCDT); 
    IHistogram1D electronPz = aida.histogram1D("electron_pz", 100, 0, 3.0);
    IHistogram1D positronPz = aida.histogram1D("positron_pz", 100, 0, 3.0);
    
    //after a total pz cut
    IHistogram1D totPz = aida.histogram1D("total_pz", 100, 0, 4.0);
    IHistogram1D massPzCut =  aida.histogram1D("mass_pz_cut", nMassBins, 0, maxMass);
    IHistogram1D cdtPzCut  = aida.histogram1D("cluster_dt_pz_cut", nCDTbins, -maxCDT, maxCDT);

    //after a chi2 cut
    IHistogram1D eleTrackChi2 = aida.histogram1D("electron_track_chi2", 200, 0, 100);
    IHistogram1D posTrackChi2 = aida.histogram1D("positron_track_chi2", 200, 0, 100);
    IHistogram1D massTrackChi2Cut =  aida.histogram1D("mass_track_chi2_cut", nMassBins, 0, maxMass);
    IHistogram1D cdtTrackChi2Cut  = aida.histogram1D("cluster_dt_track_chi2_cut", nCDTbins, -maxCDT, maxCDT);
    
     //after a posD0 cut
    IHistogram1D eleD0 = aida.histogram1D("electron_d0", 200, -10, 10);
    IHistogram1D posD0 = aida.histogram1D("positron_d0", 200, -10, 10);
    IHistogram1D massPosD0Cut =  aida.histogram1D("mass_pos_d0_cut", nMassBins, 0, maxMass);
    IHistogram1D cdtPosD0Cut = aida.histogram1D("cluster_dt_pos_d0_cut", nCDTbins, -maxCDT, maxCDT);
    
    //track cluster time cut
    IHistogram1D clusterTrackDt = aida.histogram1D("cluster_track_dt", 200, 0, 100);
    IHistogram1D massClusterTrackDtCut =  aida.histogram1D("mass_tc_dt_cut", nMassBins, 0, maxMass);
    IHistogram1D cdtClusterTrackDtCut  = aida.histogram1D("cluster_dt_tc_dt_cut", nCDTbins, -maxCDT, maxCDT);
    
    //L1 cut:
    IHistogram1D eleL1 = aida.histogram1D("electron_l1", 5, 0, 5);
    IHistogram1D posL1 = aida.histogram1D("positron_l1", 5, 0, 5);
    IHistogram1D eleL2 = aida.histogram1D("electron_l2", 5, 0, 5);
    IHistogram1D posL2 = aida.histogram1D("positron_l2", 5, 0, 5);
    IHistogram1D massL1Cut =  aida.histogram1D("mass_l1l2_cut", nMassBins, 0, maxMass);
    IHistogram1D cdtL1Cut  = aida.histogram1D("cluster_dt_l1l2_cut", nCDTbins, -maxCDT, maxCDT);
    
    //px total
    IHistogram1D totPx = aida.histogram1D("total_px", 200, -.2, .2);
    
    //pt asymmetry
    IHistogram1D ptAsymmetry = aida.histogram1D("pt_asymmetry", 200, -1, 1);
    //pt asymmetry
    IHistogram1D pzAsymmetry = aida.histogram1D("pz_asymmetry", 200, -1, 1);
    IHistogram1D ptpzAsymmetry = aida.histogram1D("pt_over_pz_asymmetry", 200, -1, 1);
    
    
    // all cuts
    IHistogram1D massFinal =  aida.histogram1D("mass_final", nMassBins, 0, maxMass);
    IHistogram1D cdtFinal  = aida.histogram1D("cluster_dt_final", nCDTbins, -maxCDT, maxCDT);
    //after a total pz cut
    IHistogram1D totPzFinal = aida.histogram1D("total_pz_final", 100, 0, 4.0);
    
    /**
     * check if two tracks share hits.  If so, return the track that is "worse"
     * than the other one, by some criteria (for now, just use track fit chi2)
     * @param t1
     * @param t2
     * @return
     */
    Track getWorseTrack(Track t1, Track t2){
        if(t1 == t2)
            return null;
        if(TrackUtils.numberOfSharedHits(t1, t2) == 0)
            return null;
        //for now, just use track fit chi2.  
        if(t1.getChi2() > t2.getChi2())
            return t1;
        return t2;
    }
    
    void preliminaryCleanup(List<ReconstructedParticle> v0s){
      //first get rid of v0s made from matched tracks.  (gbl only).  
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            if(v1.getType()<32)
                trash.add(v1);
        }
        v0s.removeAll(trash);
        trash.clear();
        
        //then, remove v0s where there is a missing cluster, 
        //or both clusters are on opposite sides of the ECal
        
        for(ReconstructedParticle v1 : v0s){
            if(v1.getParticles().get(0).getClusters().size() == 0 
                    || v1.getParticles().get(1).getClusters().size() == 0 )
                trash.add(v1);
            else if(v1.getParticles().get(0).getClusters().get(0).getPosition()[1]
                    *v1.getParticles().get(1).getClusters().get(0).getPosition()[1] > 0)
                trash.add(v1);
            else if(v1.getParticles().get(0).getGoodnessOfPID() > goodnessPidThreshold 
                || v1.getParticles().get(1).getGoodnessOfPID() > goodnessPidThreshold)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    
    public void cleanupDuplicates(List<ReconstructedParticle> v0s){
        
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            Track e1 = v1.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0);
            Track p1 = v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0);
            for(ReconstructedParticle v2 : v0s){
                Track e2 = v2.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0);
                Track p2 = v2.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0);
                Track worse = getWorseTrack(e1, e2);
                if(worse == e1)
                    trash.add(v1);
                else if(worse == e2)
                    trash.add(v2);
                worse = getWorseTrack(p1, p2);
                if(worse == p1)
                    trash.add(v1);
                else if(worse == p2)
                    trash.add(v2);
                
            }
        }
        v0s.removeAll(trash);
    }
    
    public void feeCut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            if(v1.getParticles().get(ReconParticleDriver.ELECTRON).getMomentum().z()>feeThreshold)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    /**
     * combines the radiative cut and the pz max cut.  
     * @param v0s
     */
    public void pzMaxCut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            if(v1.getMomentum().z()>pzTotThreshold)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    public void d0Cut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            double posD0 = TrackUtils.getDoca(v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0));
            
            if(posD0>posD0Threshold)
                trash.add(v1);
            
        }
        v0s.removeAll(trash);
    }
    
    public void clusterTrackDTCut(List<ReconstructedParticle> v0s, EventHeader event) {
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            double trackEleTime = TrackUtils.getTrackTime(v1.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0),TrackUtils.getHitToStripsTable(event),TrackUtils.getHitToRotatedTable(event));
            double trackPosTime = TrackUtils.getTrackTime(v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0),TrackUtils.getHitToStripsTable(event),TrackUtils.getHitToRotatedTable(event));
           
            double clusterEleTime =  v1.getParticles().get(ReconParticleDriver.ELECTRON).getClusters().get(0).getCalorimeterHits().get(0).getTime();
            double clusterPosTime =  v1.getParticles().get(ReconParticleDriver.POSITRON).getClusters().get(0).getCalorimeterHits().get(0).getTime();
            
            if(clusterEleTime - trackEleTime > trackClusterTimeDiffThresholdMean + trackClusterTimeDiffThresholdAbs 
                    || clusterEleTime - trackEleTime < trackClusterTimeDiffThresholdMean - trackClusterTimeDiffThresholdAbs)
                trash.add(v1);
            if(clusterPosTime - trackPosTime > trackClusterTimeDiffThresholdMean + trackClusterTimeDiffThresholdAbs 
                    || clusterPosTime - trackPosTime < trackClusterTimeDiffThresholdMean - trackClusterTimeDiffThresholdAbs)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    
    public void trackChi2Cut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            if(v1.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0).getChi2()>trackChi2Threshold)
                trash.add(v1);
            if(v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0).getChi2()>trackChi2Threshold)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    
    public void clusterDtCut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            
            if(Math.abs(getClusterTimeDiff(v1))>clusterTimeDiffThreshold)
                trash.add(v1);
        }
        v0s.removeAll(trash);
    }
    
    public void L1L2Cut(List<ReconstructedParticle> v0s){
        List<ReconstructedParticle> trash = new ArrayList<ReconstructedParticle>();
        for(ReconstructedParticle v1 : v0s){
            
            if(hasL1(v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0)) == 0)
                trash.add(v1);
            /*if(hasL1(v1.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0)) == 0)
                trash.add(v1);
            if(hasL2(v1.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0)) == 0)
                trash.add(v1);
            if(hasL2(v1.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0)) == 0)
                trash.add(v1);*/
        }
        v0s.removeAll(trash);
    }
    
    @Override
    public void process(EventHeader event){
        
        List<ReconstructedParticle> v0s = event.get(ReconstructedParticle.class, "TargetConstrainedV0Candidates");
        
        preliminaryCleanup(v0s);
        cleanupDuplicates(v0s);
        for(ReconstructedParticle v0 : v0s){
           massPreliminary.fill(v0.getMass());
           cdtPreliminary.fill(getClusterTimeDiff(v0));
        }

        
        //now look at the fee cut
        for(ReconstructedParticle v0 : v0s){
            electronPz.fill(v0.getParticles().get(ReconParticleDriver.ELECTRON).getMomentum().z());
            positronPz.fill(v0.getParticles().get(ReconParticleDriver.POSITRON).getMomentum().z());
            
        }
        feeCut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massNoFee.fill(v0.getMass());
            cdtNoFee.fill(getClusterTimeDiff(v0));
            
        }
        
        //now look at the pz tot cut
        for(ReconstructedParticle v0 : v0s){
            totPz.fill(v0.getMomentum().z());
        }
        pzMaxCut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massPzCut.fill(v0.getMass());
            cdtPzCut.fill(getClusterTimeDiff(v0));
            
        }
        
        //now look at the track chi2 cut
        for(ReconstructedParticle v0 : v0s){
            eleTrackChi2.fill(v0.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0).getChi2());
            posTrackChi2.fill(v0.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0).getChi2());
        }
        trackChi2Cut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massTrackChi2Cut.fill(v0.getMass());
            cdtTrackChi2Cut.fill(getClusterTimeDiff(v0));
            
        }
        
      //now look at the cluster track time difference cut
        for(ReconstructedParticle v0 : v0s){
            double trackEleTime = TrackUtils.getTrackTime(v0.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0),TrackUtils.getHitToStripsTable(event),TrackUtils.getHitToRotatedTable(event));
            double trackPosTime = TrackUtils.getTrackTime(v0.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0),TrackUtils.getHitToStripsTable(event),TrackUtils.getHitToRotatedTable(event));
            double clusterEleTime =  v0.getParticles().get(ReconParticleDriver.ELECTRON).getClusters().get(0).getCalorimeterHits().get(0).getTime();
            double clusterPosTime =  v0.getParticles().get(ReconParticleDriver.POSITRON).getClusters().get(0).getCalorimeterHits().get(0).getTime();
            
            clusterTrackDt.fill(clusterEleTime-trackEleTime);
            clusterTrackDt.fill(clusterPosTime-trackPosTime);
        }
        clusterTrackDTCut(v0s, event);
        for(ReconstructedParticle v0 : v0s){
            massClusterTrackDtCut.fill(v0.getMass());
            cdtClusterTrackDtCut.fill(getClusterTimeDiff(v0));
            
        }
        
        //now do L1 cut.  
        for(ReconstructedParticle v0 : v0s){
            eleL1.fill(hasL1(v0.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0)));
            posL1.fill(hasL1(v0.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0)));
            eleL2.fill(hasL2(v0.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0)));
            posL2.fill(hasL2(v0.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0)));
        
        }
        L1L2Cut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massL1Cut.fill(v0.getMass());
            cdtL1Cut.fill(getClusterTimeDiff(v0));
            
        }
        
        //now look at the positron and electron d0
        for(ReconstructedParticle v0 : v0s){
            eleD0.fill(TrackUtils.getDoca(v0.getParticles().get(ReconParticleDriver.ELECTRON).getTracks().get(0)));
            posD0.fill(TrackUtils.getDoca(v0.getParticles().get(ReconParticleDriver.POSITRON).getTracks().get(0)));
        }
        d0Cut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massPosD0Cut.fill(v0.getMass());
            cdtPosD0Cut.fill(getClusterTimeDiff(v0));
            
        }
      //now look at px total, but don't cut on it.  (I will determine the cut value later on).  
        for(ReconstructedParticle v0 : v0s){
            totPx.fill(v0.getMomentum().x());
        }
        // also look at pt, pz and pt/pz asymmetry:
        for(ReconstructedParticle v0 : v0s){
            ReconstructedParticle pos = v0.getParticles().get(ReconParticleDriver.POSITRON);
            ReconstructedParticle ele = v0.getParticles().get(ReconParticleDriver.ELECTRON);
            
            double tilt = .0305;
            double ptPos = Math.hypot(pos.getMomentum().x()*Math.cos(tilt)-pos.getMomentum().z()*Math.sin(tilt), pos.getMomentum().y());
            double ptEle = Math.hypot(ele.getMomentum().x()*Math.cos(tilt)-ele.getMomentum().z()*Math.sin(tilt), ele.getMomentum().y());
            ptAsymmetry.fill((ptPos-ptEle)/(ptPos+ptEle));
            
            double pzPos = pos.getMomentum().z();
            double pzEle = ele.getMomentum().z();
            pzAsymmetry.fill((pzPos-pzEle)/(pzPos+pzEle));
            
            double ptpzPos = ptPos/pzPos;
            double ptpzEle = ptEle/pzEle;
            ptpzAsymmetry.fill((ptpzPos-ptpzEle)/(ptpzPos+ptpzEle));
            
            totPzFinal.fill(v0.getMomentum().z());
            
        }
        
         
        
        
        
        //now for the final cut:  cluster dt.  
        clusterDtCut(v0s);
        for(ReconstructedParticle v0 : v0s){
            massFinal.fill(v0.getMass());
            cdtFinal.fill(getClusterTimeDiff(v0));
            
            
        }
        
        
        
    }
    
    
    /*private double getD0(Track track){
        double d0 =  TrackUtils.getDoca(track); 
        //make correction due to target being at -5 mm 
        if(corrD0) d0 -= TrackUtils.getPhi0(track)*5;
        return d0;
        
    }*/
    private int hasL1(Track track) {
        for(org.lcsim.event.TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 1)
                return 1;
            }
        return 0;
    }

private int hasL2(Track track) {   
        for(TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 3)
                return 1;
        }
        return 0;
    }
    private double getClusterTimeDiff(ReconstructedParticle v0) {
        
        return v0.getParticles().get(0).getClusters().get(0).getCalorimeterHits().get(0).getTime()
                - v0.getParticles().get(1).getClusters().get(0).getCalorimeterHits().get(0).getTime();
    }
}
