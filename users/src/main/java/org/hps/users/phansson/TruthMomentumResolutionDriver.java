package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.ICloud1D;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.ecal.HPSMCParticlePlotsDriver;
import org.hps.util.AIDAFrame;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.ParticleTypeClassifier;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class TruthMomentumResolutionDriver extends Driver {

   
    private String outputPlotFileName="";
    private boolean hideFrame = false;
    int totalTracks = 0;
    private boolean _debug = false;
    private AIDA aida = AIDA.defaultInstance();
    private AIDAFrame pFrame;
    IAnalysisFactory af = aida.analysisFactory();
    IPlotter pPlotter;
    IPlotter pPlotter2;
    IPlotter pPlotter22;
    IPlotter pPlotter3;    
    IHistogram1D hElectronP;
    IHistogram1D hPositronP;
    IHistogram1D hTrackP;
    IHistogram1D hPosTrackP;
    IHistogram1D hTruthMatchedPosTrackP;
    IHistogram1D hTruthMatchedPosTrackPdiff;
    IHistogram1D[] hTruthMatchedPosTrackPdiffvsP = new IHistogram1D[5];
    IDataPointSet hTruthMatchedPosTrackPres;
    IHistogram1D hNegTrackP;
    IHistogram1D hTruthMatchedNegTrackP;
    IHistogram1D hTruthMatchedNegTrackPdiff;
    IHistogram1D[] hTruthMatchedNegTrackPdiffvsP = new IHistogram1D[5];
    IDataPointSet hTruthMatchedNegTrackPres;
    ICloud1D hTruthMatchedPosTrackPdiffPrev[] = new ICloud1D[5];
    ICloud1D hTruthMatchedNegTrackPdiffPrev[] = new ICloud1D[5];
    ICloud1D hNTracks;
    IHistogram1D trkCountVsEventPlot;

    ICloud1D hNPosTracks;
    ICloud1D hNNegTracks;
    ICloud1D hNPositronsForTrack;
    ICloud1D hNElectronsForTrack;
    ICloud1D hNPositronsForTrackInv;
    ICloud1D hNElectronsForTrackInv;
    
    HashMap<Integer,MCParticle> mc_ele_prev = new HashMap<Integer, MCParticle>();
    HashMap<Integer,MCParticle> mc_pos_prev = new HashMap<Integer, MCParticle>();
    
    
    
    public void setDebug(boolean v) {
        this._debug = v;
    }
    
    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }
    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    
    public TruthMomentumResolutionDriver() {
     
    
    }
    

    
    public void detectorChanged(Detector detector) {
        
     
      
        pFrame = new AIDAFrame();
        pFrame.setTitle("Truth p Plots");
        makePlots();
       
   
        
        pFrame.pack();
        pFrame.setVisible(!hideFrame);

       
    }
    
    public double getMomentum(Track track) {
        double[] p_vec = track.getTrackStates().get(0).getMomentum();
        return Math.sqrt(p_vec[0]*p_vec[0] + p_vec[1]*p_vec[1] +p_vec[2]*p_vec[2]);
    }
    
    
    public void process(EventHeader event) {


        //  Create a map between tracks and the associated MCParticle
        List<Track> tracklist = event.get(Track.class, "MatchedTracks");
        if(_debug) System.out.println("Number of Tracks = " + tracklist.size());
        List<MCParticle> mcparticles = event.get(MCParticle.class).get(0);
        if(_debug) System.out.println("Number of MC particles = " + mcparticles.size());
        List<MCParticle> fsParticles = HPSMCParticlePlotsDriver.makeGenFSParticleList(mcparticles);
        if(_debug) System.out.println("Number of FS MC particles = " + fsParticles.size());
        
        
        
        MCParticle electron=null;
        MCParticle positron=null;
        
        int nele = 0;
        int nposi = 0;
        for(MCParticle fs : fsParticles) {
            if(ParticleTypeClassifier.isElectron(fs.getPDGID())) {
                ++nele;
                if(electron==null) electron = fs;
                else {
                    if(fs.getEnergy()>electron.getEnergy()) {
                        electron = fs;
                    } 
                } 
            } else if(ParticleTypeClassifier.isPositron(fs.getPDGID())) {
                ++nposi;
                if(positron==null) positron = fs;
                else {
                    if(fs.getEnergy()>positron.getEnergy()) {
                        positron = fs;
                    } 
                } 
            }
        }
    
        if(electron!=null) this.hElectronP.fill(electron.getMomentum().magnitude());
        if(positron!=null) this.hPositronP.fill(positron.getMomentum().magnitude());
        int[] ntrks = {0,0,0};

        for (Track trk : tracklist) {
            double p = this.getMomentum(trk);
            this.hTrackP.fill(p);
            if(this.isElectronTrack(trk)) {
                this.hNegTrackP.fill(p);
                if(electron!=null) {
                    this.hTruthMatchedNegTrackP.fill(p);
                    this.hTruthMatchedNegTrackPdiff.fill(p - electron.getMomentum().magnitude());
                    hTruthMatchedNegTrackPdiffvsP[this.getMomentumBin(electron.getMomentum().magnitude())].fill(p - electron.getMomentum().magnitude());   
                    if(_debug) System.out.println("Filling ele for " + mc_ele_prev.size() + " prev ");
                    for(Map.Entry<Integer,MCParticle> prev : mc_ele_prev.entrySet()) {
                        if(_debug) System.out.println("prev " + prev.getKey());
                        this.hTruthMatchedNegTrackPdiffPrev[prev.getKey()].fill(p - prev.getValue().getMomentum().magnitude());
                    }
                }
                ++ntrks[2];
            }
            else {
                this.hPosTrackP.fill(p);
                if(positron!=null) {
                    this.hTruthMatchedPosTrackP.fill(p);
                    this.hTruthMatchedPosTrackPdiff.fill(p - positron.getMomentum().magnitude());
                    hTruthMatchedPosTrackPdiffvsP[this.getMomentumBin(positron.getMomentum().magnitude())].fill(p - positron.getMomentum().magnitude());   
                    if(_debug) System.out.println("Filling pos for " + mc_pos_prev.size() + " prev ");
                    for(Map.Entry<Integer,MCParticle> prev : mc_pos_prev.entrySet()) {
                        if(_debug) System.out.println("prev " + prev.getKey());
                        this.hTruthMatchedPosTrackPdiffPrev[prev.getKey()].fill(p - prev.getValue().getMomentum().magnitude());
                    }
                }
                ++ntrks[1];
            }
            
            ++totalTracks;
            ++ntrks[0];
            //int q = trk.getCharge();
        }
        this.hNTracks.fill(ntrks[0]);
        this.hNPosTracks.fill(ntrks[1]);
        this.hNNegTracks.fill(ntrks[2]);
            
        if(ntrks[1]>0) hNPositronsForTrack.fill(nposi);
        if(ntrks[2]>0) hNElectronsForTrack.fill(nele);
        if(ntrks[2]>0) hNPositronsForTrackInv.fill(nposi);
        if(ntrks[1]>0) hNElectronsForTrackInv.fill(nele);
        
        for(int i=0;i<tracklist.size();++i) trkCountVsEventPlot.fill(event.getEventNumber());
        
        //Save to list of previous truth particles
        if(electron!=null) {
            mc_ele_prev = this.updatePrevMap(mc_ele_prev);
            mc_ele_prev.put(0, electron);
        }
        if(positron!=null) {
            mc_pos_prev = this.updatePrevMap(mc_pos_prev);
            mc_pos_prev.put(0, positron);
        }   

         if(totalTracks%50==0) this.updatePlots();
        
    }

    private int getMomentumBin(double p) {
        int p_bin = -1;
        for(int i=0;i<5;++i) {
            double plow = i/2.0;
            double phigh = (i)/2.0+0.5;
            if(p>=plow && p<phigh) {
                p_bin = i;
                break;
            }
        }
        if(p_bin==-1) p_bin = 4;
        return p_bin;
    }
    
    private boolean isElectronTrack(Track track) {
        //fix confusing sign flip in magnetic field!!
        return track.getCharge()>0 ? true : false;
    }
    
    private HashMap<Integer,MCParticle> updatePrevMap(HashMap<Integer,MCParticle> map) {
        HashMap<Integer,MCParticle> newmap = new HashMap<Integer,MCParticle>();
        for (Map.Entry<Integer, MCParticle> entry : map.entrySet()) {
            if(entry.getKey()<4) {
                if(_debug) System.out.println("Key e = " + entry.getKey() + ", Value = " + entry.getValue());
                newmap.put(entry.getKey()+1, entry.getValue());
            }
        }
        return newmap;
    }
    
    

    private MCParticle getHighestEnergyParticle(int pdgId, List<MCParticle> fsParticles) {
        MCParticle particle = null;
        for(MCParticle fs : fsParticles) {
            int fsPdg = fs.getPDGID();
            if(fsPdg==pdgId) {
                if(particle==null) {
                    particle = fs;
                }
                else {
                    if(fs.getEnergy()>particle.getEnergy()) {
                        particle = fs;
                    } 
                } 
            }
        }
        return particle;
    }


    public void endOfData() {
        this.updatePlots();
        System.out.println("Total Number of Tracks Found = "+totalTracks);
          
        if (outputPlotFileName != "")
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        
    }
    
    private void makePlots() {
    
    
        pPlotter = af.createPlotterFactory().create("Truth p Plots");
        pPlotter.setTitle("Truth p Plots");
        pFrame.addPlotter(pPlotter);
        IPlotterStyle style0 = pPlotter.style();
        //style0.dataStyle().fillStyle().setColor("yellow");
        //style0.dataStyle().errorBarStyle().setVisible(false);
        pPlotter.createRegions(2, 6);       
        hTrackP = aida.histogram1D("Track p", 50, 0,4);
        hPosTrackP = aida.histogram1D("Track p q>0", 50, 0,4);
        hNegTrackP = aida.histogram1D("Track p q<0", 50, 0,4);
        hTruthMatchedPosTrackP = aida.histogram1D("Track p q>0 e+ match", 50, 0,4);
        hTruthMatchedNegTrackP = aida.histogram1D("Track p q<0 e- match", 50, 0,4);
        hTruthMatchedPosTrackPdiff = aida.histogram1D("Track p - p(e+) q>0", 100, -0.2,0.2);
        hTruthMatchedNegTrackPdiff = aida.histogram1D("Track p - p(e-) q<0", 100, -0.2,0.2);
        for(int i=0;i<5;++i) {
              double plow = i/2.0+0.5;
              double phigh = (i+1)/2.0+0.5;
              hTruthMatchedPosTrackPdiffvsP[i] = aida.histogram1D("Track p - p(e+) q>0 p["+plow+","+phigh+"]", 100, -0.2,0.2);
              hTruthMatchedNegTrackPdiffvsP[i] = aida.histogram1D("Track p - p(e-) q<0 p["+plow+","+phigh+"]", 100, -0.2,0.2);
     
        }
        IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(aida.tree());
        hTruthMatchedPosTrackPres = dpsf.create("hTruthMatchedPosTrackPres", "RMS(Track p - p(e+)) q>0 vs P",2);
        hTruthMatchedNegTrackPres = dpsf.create("hTruthMatchedNegTrackPres", "RMS(Track p - p(e-)) q<0 vs P",2);
        hNTracks = aida.cloud1D("Ntrks");
        trkCountVsEventPlot = aida.histogram1D("Number of Tracks vs Event Nr", 501, -0.5, 500.5);
	trkCountVsEventPlot.annotation().addItem("xAxisLabel", "Event Number");
        hNPosTracks = aida.cloud1D("Ntrks q>0");
        hNNegTracks = aida.cloud1D("Ntrks q<0");    
        hElectronP = aida.histogram1D("Electron Momentum", 50, 0,4);
        hPositronP = aida.histogram1D("Positron Momentum", 50, 0,4);      
        hNPositronsForTrack = aida.cloud1D("N positrons given track with q>0");
        hNElectronsForTrack = aida.cloud1D("N electrons given track with q<0");
        hNPositronsForTrackInv = aida.cloud1D("N positrons given track with q<0");
        hNElectronsForTrackInv = aida.cloud1D("N electrons given track with q>0");

        
        pPlotter.region(0).plot(hTrackP);
        pPlotter.region(0).plot(hPosTrackP);
        pPlotter.region(0).plot(hNegTrackP);
        pPlotter.region(6).plot(hNTracks);
        pPlotter.region(6).plot(hNPosTracks);
        pPlotter.region(6).plot(hNNegTracks);     
        pPlotter.region(1).plot(hElectronP);
        pPlotter.region(7).plot(hPositronP);
        pPlotter.region(2).plot(this.hTruthMatchedPosTrackP);
        pPlotter.region(3).plot(trkCountVsEventPlot);
        pPlotter.region(8).plot(this.hTruthMatchedNegTrackP);
        pPlotter.region(4).plot(this.hNPositronsForTrack);
        pPlotter.region(10).plot(this.hNElectronsForTrack);
        pPlotter.region(5).plot(this.hNPositronsForTrackInv);
        pPlotter.region(11).plot(this.hNElectronsForTrackInv);

        
        
        pPlotter2 = af.createPlotterFactory().create("Resolution");
        pPlotter2.setTitle("Resolution");
        pFrame.addPlotter(pPlotter2);
        style0 = pPlotter2.style();
        //style0.dataStyle().fillStyle().setColor("yellow");
        //style0.dataStyle().errorBarStyle().setVisible(false);
        pPlotter2.createRegions(2, 2);
        pPlotter2.region(0).plot(this.hTruthMatchedPosTrackPdiff);
        pPlotter2.region(2).plot(this.hTruthMatchedNegTrackPdiff);
        pPlotter2.region(1).plot(this.hTruthMatchedPosTrackPres);
        pPlotter2.region(3).plot(this.hTruthMatchedNegTrackPres);
        
        
        
        pPlotter22 = af.createPlotterFactory().create("Resolutions");
        pPlotter22.setTitle("Resolutions");
        pFrame.addPlotter(pPlotter22);
        style0 = pPlotter22.style();
        //style0.dataStyle().fillStyle().setColor("yellow");
        //style0.dataStyle().errorBarStyle().setVisible(false);
        pPlotter22.createRegions(2, 5);
        for(int i=0;i<5;++i) {   
            pPlotter22.region(i).plot(this.hTruthMatchedPosTrackPdiffvsP[i]);
            pPlotter22.region(5+i).plot(this.hTruthMatchedNegTrackPdiffvsP[i]);
        }
        
       
        pPlotter3 = af.createPlotterFactory().create("Truth p Plots");
        pPlotter3.setTitle("Prev BS's");
        pFrame.addPlotter(pPlotter3);
        pPlotter3.createRegions(2, 5);
        
        for(int i=0;i<5;++i) {
            hTruthMatchedPosTrackPdiffPrev[i] = aida.cloud1D("Track p - p(e+) q>0 BC=-" + i);
            hTruthMatchedNegTrackPdiffPrev[i] = aida.cloud1D("Track p - p(e-) q<0 BC=-" + i);
            pPlotter3.region(i).plot(hTruthMatchedPosTrackPdiffPrev[i]);
            pPlotter3.region(5+i).plot(hTruthMatchedNegTrackPdiffPrev[i]);
        }
    
    }
   
    
    
    
    
    
 


 void updatePlots() {
     this.hTruthMatchedNegTrackPres.clear();
     this.hTruthMatchedPosTrackPres.clear();
        
     for(int i=0;i<5;++i) {
                       
        double plow = i/2.0+0.5;
        double phigh = (i+1)/2.0+0.5;
        double p = (phigh-plow)/2+plow;
        double rms = hTruthMatchedPosTrackPdiffvsP[i].rms();
        double n = hTruthMatchedPosTrackPdiffvsP[i].entries();
        //aproximation
        double rms_error = n==0? 0 :  Math.sqrt(Math.pow(hTruthMatchedPosTrackPdiffvsP[i].rms(),2)/(2*hTruthMatchedPosTrackPdiffvsP[i].entries()));
        
        this.hTruthMatchedPosTrackPres.addPoint();
        this.hTruthMatchedPosTrackPres.point(i).coordinate(1).setValue(rms/p);
        this.hTruthMatchedPosTrackPres.point(i).coordinate(1).setErrorPlus(rms_error);
        this.hTruthMatchedPosTrackPres.point(i).coordinate(0).setValue(p);
        this.hTruthMatchedPosTrackPres.point(i).coordinate(0).setErrorPlus((phigh-plow)/2);
        
       
        rms = hTruthMatchedPosTrackPdiffvsP[i].rms();
        n = hTruthMatchedPosTrackPdiffvsP[i].entries();
        //aproximation
        rms_error = n==0? 0 :  Math.sqrt(Math.pow(hTruthMatchedPosTrackPdiffvsP[i].rms(),2)/(2*hTruthMatchedPosTrackPdiffvsP[i].entries()));
        
        this.hTruthMatchedNegTrackPres.addPoint();
        this.hTruthMatchedNegTrackPres.point(i).coordinate(1).setValue(rms/p);
        this.hTruthMatchedNegTrackPres.point(i).coordinate(1).setErrorPlus(rms_error);
        this.hTruthMatchedNegTrackPres.point(i).coordinate(0).setValue((phigh-plow)/2+plow);
        this.hTruthMatchedNegTrackPres.point(i).coordinate(0).setErrorPlus((phigh-plow)/2);
        
        
    }
 }
}