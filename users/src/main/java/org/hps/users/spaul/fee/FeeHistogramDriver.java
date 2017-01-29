package org.hps.users.spaul.fee;

import java.util.ArrayList;
import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import static java.lang.Math.*;


public class FeeHistogramDriver extends Driver{
    private static final double amu = 0.931494095;
    private static final double  Mcarbon = amu*12;
    private static final double Mtungsten = amu*183.84;
    boolean fidEcal(Cluster c){
        return c.getPosition()[0] > -262.74 && c.getPosition()[0] < 347.7 && Math.abs(c.getPosition()[1])>33.54 
                && Math.abs(c.getPosition()[1])<75.18 
                && !(c.getPosition()[0]>-106.66 && c.getPosition()[0] < 42.17 && Math.abs(c.getPosition()[1])<47.17);
    }
    
    
    
    public void setD0Cut(double val){
        d0cut = val;
    }
    public void setZ0Cut(double val){
        z0cut = val;
    }
    double d0cut = 1;
    double z0cut = .5;

    //ssp cuts
    double _sspTriggerTime = 60;
    double _sspNmin = 3;
    double _sspMinEnergy = 1.3;
    double _sspMaxEnergy = 2.6;

    //determined by beam energy
    double eMin, eMax, beamEnergy, seedEnergyCut;

    boolean _useSSPcut = false;
    public void setUseSSPcut(boolean val){
        this._useSSPcut = val;
    }
    public void setSspClusterTime(double val){
        this._sspTriggerTime = val;
    }
    public void setSspNmin(int val){
        this._sspNmin = val;
    }
    public void setSspMinEnergy(double val){
        this._sspMinEnergy = val;
    }
    public void setSspMaxEnergy(double val){
        this._sspMaxEnergy = val;
    }


    public void setEMin(double val){
        this.eMin = val;
    }

    public void setEMax(double val){
        this.eMax = val;
    }
    
    public void setPMin(double val){
        this.pMin = val;
    }

    public void setPMax(double val){
        this.pMax = val;
    }

    
    public double getSeedEnergyCutFrac() {
        return seedEnergyCutFrac;
    }

    public void setSeedEnergyCutFrac(double seedEnergyCut) {
        this.seedEnergyCutFrac = seedEnergyCut;
    }

    double pMin, pMax;


    double beamTiltX = .0295;
    double beamTiltY = -.0008;

    double maxChi2 = 30;
    //maximum difference between the reconstructed energy and momentum
    //double maxdE = .5;
    int nMin = 3;

    IHistogram2D ixiySSPseed;
    IHistogram2D ixiySSPall;
    IHistogram1D nSSP;
    IHistogram1D theta;
    IHistogram1D thetaInRange;
    IHistogram2D[] thetaVsPhi;
    IHistogram2D[] thetaVsPhiNoTrackQualityCuts;
    IHistogram2D[] thetaVsPhiInRange;
    IHistogram2D d0VsZ0, d0VsZ0_top, d0VsZ0_bottom;
    IHistogram1D d0;
    IHistogram1D z0;
    IHistogram2D chi2_vs_theta;
    IHistogram2D nFidTopVsBot;
    IHistogram2D nClustTopVsBot;
    IHistogram1D timeHist;
    IHistogram1D chi2Hist;
    private double seedEnergyCutFrac = 0;


    @Override
    public void detectorChanged(Detector det){
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        eMin = .85*beamEnergy;
        eMax = 1.15*beamEnergy;

        pMin = .75*beamEnergy;
        pMax = 1.25*beamEnergy;

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
    private IHistogram2D thetaVsEnergy;
    private IHistogram2D thetaVsEnergyFid;
    private IHistogram2D twoFeeClustX_tb;
    //private IHistogram1D clusterEnergyFidEcal;
    private IHistogram2D twoFeeClustX_bb;
    private IHistogram2D twoFeeClustX_tt;
    private IHistogram2D feeClustXY;
    
    private IHistogram1D twoFeeClustDX_bb;
    private IHistogram1D twoFeeClustDX_tb;
    private IHistogram1D twoFeeClustDX_tt;

    private IHistogram1D twoFeeTrackDphi_bb;
    private IHistogram1D twoFeeTrackDphi_tb;
    private IHistogram1D twoFeeTrackDphi_tt;
    private IHistogram1D w_carbon;
    private IHistogram1D w_tungsten;
    
    void setupHistograms(){

        this.nSSP =  aida.histogram1D("ssp clusters count", 10, 0, 10);
        this.nSSPpass =  aida.histogram1D("ssp clusters count pass", 10, 0, 10);
        this.ixiySSPseed = aida.histogram2D("ssp clusters seed", 50, -25, 25, 11, -5, 6);
        this.ixiySSPall = aida.histogram2D("ssp clusters all", 50, -25, 25, 11, -5, 6);
        this.sspEnergy = aida.histogram1D("ssp clusters energy", 100, 0, 1.5*beamEnergy);
        this.sspTime = aida.histogram1D("ssp clusters time", 200, 0, 200);

        energy = aida.histogram1D("cluster energy", 100, 0, 1.5*beamEnergy);
        energyTop = aida.histogram1D("cluster energy top", 100, 0, 1.5*beamEnergy);
        energyBottom = aida.histogram1D("cluster energy bottom", 100, 0, 1.5*beamEnergy);

        //clusterEnergyFidEcal = aida.histogram1D("cluster energy fid ecal", 100, 0, 1.5*beamEnergy);

        clusterEnergyFidTrack = aida.histogram1D("cluster energy fid track", 100, 0, 1.5*beamEnergy);

        seedEnergy = aida.histogram1D("seed energy", 100, 0, beamEnergy);
        seedEnergyTop = aida.histogram1D("seed energy top", 100, 0, beamEnergy);
        seedEnergyBottom = aida.histogram1D("seed energy bottom", 100, 0, beamEnergy);

        clusterVsSeedEnergy = aida.histogram2D("cluster vs seed energy", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);
        clusterVsSeedEnergyTop = aida.histogram2D("cluster vs seed energy top", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);
        clusterVsSeedEnergyBottom = aida.histogram2D("cluster vs seed energy bottom", 100, 0, 1.5*beamEnergy, 100, 0, beamEnergy);

        this.nSigma = aida.histogram1D("nSigma", 200, 0, 10);

        int nBinsPhi = 400;

        String[] regionNames = {"", " r1", " r2", " r3", " r4"};

        thetaVsPhi = new IHistogram2D[regionNames.length];
        thetaVsPhiInRange= new IHistogram2D[regionNames.length];
        thetaVsPhiNoTrackQualityCuts = new IHistogram2D[regionNames.length];
        thetaVsPhiChi2Cut= new IHistogram2D[regionNames.length];
        thetaVsPhiTrackExtrapCut = new IHistogram2D[regionNames.length];
        thetaVsPhiMomentumCut = new IHistogram2D[regionNames.length];	
        thetaVsPhi6 = new IHistogram2D[regionNames.length];
        for(int i = 0; i< regionNames.length; i++){
            thetaVsPhi[i] = aida.histogram2D("theta vs phi" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhiInRange[i] = aida.histogram2D("theta vs phi in range" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhiNoTrackQualityCuts[i] = aida.histogram2D("theta vs phi no track quality cuts" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhiChi2Cut[i] = aida.histogram2D("theta vs phi chi2 cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhiTrackExtrapCut[i] = aida.histogram2D("theta vs phi track extrap cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhiMomentumCut[i] = aida.histogram2D("theta vs phi chi2 momentum cut" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);
            thetaVsPhi6[i] = aida.histogram2D("theta vs phi 6 hits" + regionNames[i], nBinsPhi, 0, .2, 628, -3.14, 3.14);

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
        d0 = aida.histogram1D("d0", 200, -3, 3);
        z0 = aida.histogram1D("z0", 200, -1.5, 1.5);

        d0VsZ0_top = aida.histogram2D("d0 vs z0 top", 100, -5, 5, 100, -5, 5);
        d0VsZ0_bottom = aida.histogram2D("d0 vs z0 bottom", 100, -5, 5, 100, -5, 5);


        pHist = aida.histogram1D("momentum", 100, 0, 1.5*beamEnergy);

        charge = aida.histogram1D("charge", 6, -3, 3);


        timeHist = aida.histogram1D("cluster time", 400, 0, 400);
        chi2Hist = aida.histogram1D("chi2", 200, 0, 50);
        chi2RedHist = aida.histogram1D("chi2 red", 200, 0, 15);
        clustSize = aida.histogram1D("cluster size", 20, 0, 20);
        clustSizeGTP = aida.histogram1D("cluster size gtp (>55% beam energy)", 20, 0, 20);

        z0VsTanLambda = aida.histogram2D("z0 vs tan lambda", 100, -5, 5, 100, -.1, .1);
        z0VsChi2 = aida.histogram2D("z0 vs chi2", 100, -5, 5, 100, 0, 100);
        nPassCutsPerEvent = aida.histogram1D("n pass cuts", 10, 0, 10);
        nPassCutsPerEventFiducial = aida.histogram1D("n pass cuts fiducial", 10, 0, 10);
        thetaVsMomentum = aida.histogram2D("theta vs momentum", 100, 0, .2, 100, 0, 1.2*beamEnergy);
        thetaVsMomentumFid = aida.histogram2D("theta vs momentum fiducial", 100, 0, .2, 100, 0, 1.2*beamEnergy);
        thetaVsEnergy = aida.histogram2D("theta vs energy", 100, 0, .2, 100, 0, 1.2*beamEnergy);
        thetaVsEnergyFid = aida.histogram2D("theta vs energy fiducial", 100, 0, .2, 100, 0, 1.2*beamEnergy);


        dxdyAtEcal = aida.histogram2D("dxdy at ecal", 200, -20, 20, 200, -20, 20);

        dxAtEcal = aida.histogram1D("dx at ecal", 200, -20, 20);
        dyAtEcal = aida.histogram1D("dy at ecal", 200, -20, 20);

        dxVsNcols = aida.histogram2D("dx at ecal vs ncols", 200,-20, 20, 5, 0,5);
        dyVsNrows = aida.histogram2D("dy at ecal vs nrows", 200,-20, 20, 5, 0,5);
        chi2_vs_theta = aida.histogram2D("chi2 vs theta", 100, 0,  50, 100, 0, .2);
        nFidTopVsBot = aida.histogram2D("n fid top vs bottom", 5, 0, 5, 5, 0, 5);
        nClustTopVsBot = aida.histogram2D("n fee clust top vs bottom", 5, 0, 5, 5, 0, 5);


        twoFeeClustX_tb = aida.histogram2D("2 cluster x top vs bot", 140, -300, 400, 140, -300, 400);
        twoFeeClustX_bb = aida.histogram2D("2 cluster x bot vs bot", 140, -300, 400, 140, -300, 400);
        twoFeeClustX_tt = aida.histogram2D("2 cluster x top vs top", 140, -300, 400, 140, -300, 400);

        twoFeeClustDX_tb = aida.histogram1D("2 cluster dx top vs bot", 280, -700, 700);
        twoFeeClustDX_bb = aida.histogram1D("2 cluster dx bot vs bot", 280, -700, 700);
        twoFeeClustDX_tt = aida.histogram1D("2 cluster dx top vs top", 280, -700, 700);

        twoFeeTrackDphi_tb = aida.histogram1D("2 track dphi top vs bot", 200, -.3, .3);
        twoFeeTrackDphi_bb = aida.histogram1D("2 track dphi bot vs bot", 200, -.3, .3);
        twoFeeTrackDphi_tt = aida.histogram1D("2 track dphi top vs top", 200, -.3, .3);

        
        feeClustXY = aida.histogram2D("fee clust xy", 140, -300, 400, 100, -100, 100);
        w_carbon = aida.histogram1D("w carbon", 100, 9, 13);
        
        w_tungsten = aida.histogram1D("w tungsten", 100, 160, 180);
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
    private IHistogram1D sspTime;
    private IHistogram1D sspEnergy;
    private IHistogram1D nSSPpass;
    private IHistogram1D dxAtEcal;
    private IHistogram1D dyAtEcal;
    private IHistogram2D dxdyAtEcal;
    private IHistogram1D clusterEnergyFidTrack;
    private IHistogram2D dxVsNcols;
    private IHistogram2D dyVsNrows;
    private IHistogram1D nSigma;
    private IHistogram2D[] thetaVsPhi6;
    private double nSigmaCut = 4;
    private boolean useLowestPrescale = true;

    public void setNSigmaCut(double val){
        this.nSigmaCut = val;
    }

    @Override
    public void process(EventHeader event){

        //find the ssp cluster with the most energy, and its center column

        //int col = -999;
        //int row = -999;
        int nfidTop = 0, nfidBot = 0;
        //int nfeeClustTop = 0, nfeeClustBot = 0;
        
        List<Track> feeTracksTop = new ArrayList();        
        List<Track> feeTracksBot = new ArrayList();

        List<SSPCluster> goodSSPclusters = new ArrayList();
        for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
        {
            if (!(AbstractIntData.getTag(gob) == SSPData.BANK_TAG)) continue;
            SSPData sspd = new SSPData(gob);
            //SSPCluster max = null;


            for (SSPCluster  c: sspd.getClusters()){
                sspEnergy.fill(c.getEnergy());
                sspTime.fill(c.getTime());
                ixiySSPall.fill(c.getXIndex(), c.getYIndex());
                if(c.getEnergy() > _sspMinEnergy && c.getEnergy() < _sspMaxEnergy && c.getHitCount() >= _sspNmin && c.getTime() == _sspTriggerTime){
                    goodSSPclusters.add(c);
                    ixiySSPseed.fill(c.getXIndex(), c.getYIndex());
                    nSSPpass.fill(sspd.getClusters().size());
                }
            }
            nSSP.fill(sspd.getClusters().size());
        }

        int region = getRegion(goodSSPclusters);


        fillHistogramsGTP(event);

        //int col = getColumn(event);

        List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "FinalStateParticles");
        List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
        Cleanup.purgeParticlesWithSameCluster(particles, clusters);
        int nPassFidCuts = 0, nPassCuts = 0;


        List<Cluster> feeClustersTop = new ArrayList();
        List<Cluster> feeClustersBot = new ArrayList();

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


            //find seed hit energy and column number
            double seedEnergy = 0;
            int seedcol = 0;
            int seedrow = 0;
            CalorimeterHit seedHit = null;
            for(CalorimeterHit hit : c.getCalorimeterHits()){
                if(hit.getCorrectedEnergy() > seedEnergy){
                    seedHit = hit;
                    seedEnergy = hit.getCorrectedEnergy();
                    seedcol = hit.getIdentifierFieldValue("ix");
                    seedrow = hit.getIdentifierFieldValue("iy");
                }
            }
            //make sure this is the hit that caused the trigger
            int col = -999;
            int row = -999;
            double delta = 2;
            if(_useSSPcut){
                boolean found = false;
                for(SSPCluster sspc : goodSSPclusters){
                    int coli = sspc.getXIndex();
                    int rowi = sspc.getYIndex();

                    int dx = seedcol - coli;
                    int dy = seedrow - rowi;
                    double deltai =Math.hypot(dx, dy);
                    if(deltai <= delta && seedrow*rowi >0){
                        found = true;
                        col = coli;
                        row = rowi;
                        delta = deltai;
                    }
                }
                if(!found)
                    continue;
            }






            //first check if the cluster is in the fiducial part of the Ecal:
            if(!fidEcal(c))
                continue;



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

            if(c.getPosition()[1] >0){
                feeClustersTop.add(c);
            } else{
                feeClustersBot.add(c);
            }


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
            if(useLowestPrescale)
                fillRegion(thetaVsPhiNoTrackQualityCuts, theta, phi, region);
            else
                fill(thetaVsPhiNoTrackQualityCuts,theta, phi, col);

            double dx = getDx(part);
            double dy = getDy(part);
            dxAtEcal.fill(dx);
            dyAtEcal.fill(dy);
            dxdyAtEcal.fill(dx, dy);

            dxVsNcols.fill(dx, getNcols(c));
            dyVsNrows.fill(dy, getNrows(c));

            nSigma.fill(part.getGoodnessOfPID());
            //if(abs(dx)>trackExtrapCutX || abs(dy)> trackExtrapCutY)
            if(part.getGoodnessOfPID() > nSigmaCut )
                continue;
            if(useLowestPrescale)
                fillRegion(thetaVsPhiTrackExtrapCut, theta, phi, region);
            else
                fill(thetaVsPhiTrackExtrapCut,theta, phi, col);

            Track track = part.getTracks().get(0);

            chi2_vs_theta.fill(track.getChi2(), theta);
            chi2Hist.fill(track.getChi2());
            chi2RedHist.fill(track.getChi2()/(2*track.getTrackerHits().size()-5));

            if(track.getChi2()>maxChi2)
                continue;
            if(useLowestPrescale)
                fillRegion(thetaVsPhiChi2Cut, theta, phi, region);
            else
                fill(thetaVsPhiChi2Cut,theta, phi, col);



            double pmag = part.getMomentum().magnitude();
            pHist.fill(pmag);
            if(pmag > pMax || pmag < pMin)
                continue;
            if(useLowestPrescale)
                fillRegion(thetaVsPhiMomentumCut, theta, phi, region);
            else
                fill(thetaVsPhiMomentumCut,theta, phi, col);

            double d = TrackUtils.getDoca(track);
            d0.fill(d);
            double z = TrackUtils.getZ0(track);
            z0.fill(z);
            d0VsZ0.fill(d, z);

            if(p.y() > 0)
                d0VsZ0_top.fill(d,z);
            else
                d0VsZ0_bottom.fill(d,z);

            z0VsTanLambda.fill(z, TrackUtils.getTanLambda(track));
            z0VsChi2.fill(z, track.getChi2());

            if(cb.inRange(theta, phi)){
                if(phi>0)
                    feeTracksTop.add(track);
                else
                    feeTracksBot.add(track);
            }
            
            double wc = W(pz, beamEnergy, Mcarbon, theta);
            w_carbon.fill(wc);
          //  System.out.println("wc " + wc);
            double ww = W(pz, beamEnergy, Mtungsten, theta);
            w_tungsten.fill(ww);
            //System.out.println("ww " + ww);

            if(abs(d)> d0cut)
                continue;


            if(abs(z)> z0cut)
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
            if(useLowestPrescale)
                fillRegion(thetaVsPhi, theta, phi, region);
            else
                fill(thetaVsPhi,theta, phi, col);

            if(track.getTrackerHits().size() == 6){
                if(useLowestPrescale )
                    fillRegion(thetaVsPhi6, theta, phi, region);
                else
                    fill(thetaVsPhi6,theta, phi, col);
            }

            this.theta.fill(theta);
            if(phi > 0)
                thetaTopOnly.fill(theta);
            else
                thetaBottomOnly.fill(theta);
            nPassCuts++;
            thetaVsMomentum.fill(theta, pz);
            thetaVsEnergy.fill(theta, part.getEnergy());
            if(cb.inRange(theta, phi)){
                fill(thetaVsPhiInRange, theta, phi, col);
                thetaInRange.fill(theta);
                if(phi > 0)
                {
                    nfidTop++;
                    thetaTopOnlyInRange.fill(theta);
                }
                else{
                    nfidBot++;
                    thetaBottomOnlyInRange.fill(theta);
                }
                uxVsUyInRange.fill(pxtilt/pztilt, pytilt/pztilt);
                nPassFidCuts++;
                thetaVsMomentumFid.fill(theta, pz);
                thetaVsEnergyFid.fill(theta, part.getEnergy());
                clusterEnergyFidTrack.fill(part.getEnergy());
            }
        }
        nPassCutsPerEvent.fill(nPassCuts);
        nPassCutsPerEventFiducial.fill(nPassFidCuts);
        nFidTopVsBot.fill(nfidTop, nfidBot);
        nClustTopVsBot.fill(feeClustersTop.size(), feeClustersBot.size());

        for(Cluster c1 : feeClustersTop){
            for(Cluster c2 : feeClustersBot){

                twoFeeClustX_tb.fill(c1.getPosition()[0], c2.getPosition()[0]);
                twoFeeClustDX_tb.fill(c1.getPosition()[0] -c2.getPosition()[0]);
            }
        }

        for(int i = 0; i< feeClustersTop.size()-1; i++){
            for(int j = i+1; j<feeClustersTop.size(); j++){
                Cluster c1 = feeClustersTop.get(i);
                Cluster c2 = feeClustersTop.get(j);
                twoFeeClustX_tt.fill(c1.getPosition()[0], c2.getPosition()[0]);
                twoFeeClustDX_tt.fill(c1.getPosition()[0]- c2.getPosition()[0]);
            }
        }

        for(int i = 0; i< feeClustersBot.size()-1; i++){
            for(int j = i+1; j<feeClustersBot.size(); j++){
                Cluster c1 = feeClustersBot.get(i);
                Cluster c2 = feeClustersBot.get(j);
                twoFeeClustX_bb.fill(c1.getPosition()[0], c2.getPosition()[0]);
                twoFeeClustDX_bb.fill(c1.getPosition()[0]- c2.getPosition()[0]);
            }
        }
        //now for two-tracks
        for(Track c1 : feeTracksTop){
            for(Track c2 : feeTracksBot){

                twoFeeTrackDphi_tb.fill(TrackUtils.getPhi0(c1) -TrackUtils.getPhi0(c2));
            }
        }

        for(int i = 0; i< feeTracksTop.size()-1; i++){
            for(int j = i+1; j<feeTracksTop.size(); j++){
                Track c1 = feeTracksTop.get(i);
                Track c2 = feeTracksTop.get(j);
               twoFeeTrackDphi_tt.fill(TrackUtils.getPhi0(c1) -TrackUtils.getPhi0(c2));
            }
        }

        for(int i = 0; i< feeTracksBot.size()-1; i++){
            for(int j = i+1; j<feeTracksBot.size(); j++){
                Track c1 = feeTracksBot.get(i);
                Track c2 = feeTracksBot.get(j);
                twoFeeTrackDphi_bb.fill(TrackUtils.getPhi0(c1) -TrackUtils.getPhi0(c2));
            }
        }
        
        
        
        for(Cluster c1 : feeClustersBot){
            feeClustXY.fill(c1.getPosition()[0], c1.getPosition()[1]);
        }
        for(Cluster c1 : feeClustersTop){
            feeClustXY.fill(c1.getPosition()[0], c1.getPosition()[1]);
        }

    }
    private double W(double ep, double e, double m, double theta) {
        
        return Math.sqrt(2*(e-ep)*m+m*m-4*e*ep*Math.pow(Math.sin(theta/2),2));
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

    int getNcols(Cluster c){
        long a = 0;
        for(CalorimeterHit hit : c.getCalorimeterHits()){
            a|= (1 <<(hit.getIdentifierFieldValue("ix") + 23));
        }
        int tot = 0;
        for(int i = 0; i<47; i++){
            tot += a & 1;
            a >>=1;
        }
        return tot;
    }

    int getNrows(Cluster c){
        int a = 0;
        for(CalorimeterHit hit : c.getCalorimeterHits()){
            a|= (1 <<(hit.getIdentifierFieldValue("iy") + 5));
        }
        int tot = 0;
        for(int i = 0; i<11; i++){
            tot += a & 1;
            a >>=1;
        }
        return tot;
    }

    private int getRegion(List<SSPCluster> clust){
        int ret = 5;

        for(SSPCluster c : clust){
            int region;
            int col = c.getXIndex();
            if(col <= -14 || col>=6)
                region = 1;
            else if (col <= -10 || col >= 2)
                region = 2;
            else if (col <= -8 || col >= -3)
                region = 3;
            else 
                region = 4;
            if(region < ret)
                ret = region;
        }
        return ret;
    }

    private void fill(IHistogram2D[] thetaVsPhiHist, double theta,
            double phi, int col) {
        thetaVsPhiHist[0].fill(theta, phi);
        if(col <= -14 || col>=6)
            thetaVsPhiHist[1].fill(theta, phi);
        else if (col <= -10 || col >= 2)
            thetaVsPhiHist[2].fill(theta, phi);
        else if (col <= -8 || col >= -3)
            thetaVsPhiHist[3].fill(theta, phi);
        else 
            thetaVsPhiHist[4].fill(theta, phi);

    }

    private void fillRegion(IHistogram2D[] thetaVsPhiHist, double theta,
            double phi, int region) {
        thetaVsPhiHist[0].fill(theta, phi);
        if(region <= 4 && region >=1)
            thetaVsPhiHist[region].fill(theta, phi);

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
