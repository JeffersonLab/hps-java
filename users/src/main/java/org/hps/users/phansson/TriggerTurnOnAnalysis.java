/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.ecal.HPSMCParticlePlotsDriver;
import org.hps.readout.ecal.TriggerData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public class TriggerTurnOnAnalysis extends Driver {
    private boolean _DEBUG = false;
    private String ecalClusterCollectionName = "EcalTriggerClusters";
    private String triggerDataCollectionName = "TriggerStatus";
    
    private int totalEvents = 0;
    
    private List<MCParticle> electrons = null;
    private List<MCParticle> positrons = null;
    
    private IHistogram1D hCountTrig;
    private IHistogram1D hThetay;
    private IHistogram1D hThetayAll;
    private IHistogram2D hThetayvsEAll;
    private IHistogram1D hThetaySmallest;
    private IHistogram1D hThetaySmallestEcut;
    private IHistogram1D hTrigThetaySmallestEcut;
    private IHistogram1D hThetayLargest;
    private IHistogram1D hThetayLargestEcut;
    private IHistogram1D hTrigThetayLargestEcut;
    private IHistogram2D hThetaySmallestvsE;
    private IHistogram2D hTrigThetaySmallestvsE;
    private IHistogram2D hThetayLargestvsE;
    private IHistogram2D hTrigThetayLargestvsE;
    private IHistogram2D hThetayvsThetay;
    private IHistogram2D hTrigThetayvsThetay;
    private IHistogram2D hele1vsele2;
    
    private boolean _hideFrame = false;
    private String _aidaFileName = "trigturnonanalysis.aida";
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    IHistogramFactory hf = aida.histogramFactory();
    IPlotter plotter_count;
    IPlotter plotter_count_1;
    IPlotter plotter_count_2;
    IPlotter plotter_count_3;
    IPlotter plotter_count_4;
    IPlotter plotter_count_11;
    IPlotter plotter_count_22;
    
    
    public void setHideFrame(boolean hide) {
        this._hideFrame = hide;
    }
    
    public void setAidaFileName(String filename) {
        this._aidaFileName = filename;
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        
        
        makePlots();
        
        

    }
    
    @Override
    public void process(EventHeader event) {

        if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": Process event " + event.getEventNumber());

        List<MCParticle> mcparticles = null;
        if(event.hasCollection(MCParticle.class)) {
            mcparticles = event.get(MCParticle.class).get(0);
            if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": Number of MC particles = " + mcparticles.size());

            List<MCParticle> fsParticles = HPSMCParticlePlotsDriver.makeGenFSParticleList(mcparticles);
            if(_DEBUG) System.out.println(this.getClass().getSimpleName()+": Number of FS MC particles = " + fsParticles.size());
            electrons = this.getTriggerCandidates(11, -1, -1, fsParticles);
            positrons = this.getTriggerCandidates(-11, -1, -1, fsParticles);
            if(_DEBUG) {
                System.out.println(this.getClass().getSimpleName()+": " + electrons.size() + " electrons");
                System.out.println(this.getClass().getSimpleName()+": " + positrons.size() + " electrons");
            }
            
            
        }
        else {
            System.out.println(this.getClass().getSimpleName() + ": no MC particles in this event");
        }
        
        

        
        List<TriggerData> trigger_data = null;
        
        if(event.hasCollection(TriggerData.class, triggerDataCollectionName)) {
            trigger_data = event.get(TriggerData.class, triggerDataCollectionName);
            if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": event " + event.getRunNumber() + " has trigger data");
        }
        else {
            if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": event " + event.getRunNumber() + " has no trigger data");
        }
        
        List<Cluster> clusters = null;
        if( event.hasCollection(Cluster.class, ecalClusterCollectionName) ) {
            clusters = event.get(Cluster.class, ecalClusterCollectionName);
            if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": event " + event.getRunNumber() + " has " + clusters.size() + " ecal clusters");
        }
        else {
            if(_DEBUG) System.out.println(this.getClass().getSimpleName() + ": event " + event.getRunNumber() + " has no ecal clusters");
        }
        
        //fill counting histogram
        hCountTrig.fill( trigger_data == null ? 0 : 1);
        
        //find smallest angle of the proposed pair that fired the trigger
        
        // Use electron/positron with highest E
        MCParticle electron = null;
        for(MCParticle e : electrons) {
            hThetayAll.fill( Math.abs(Math.atan(e.getMomentum().y()/e.getMomentum().z())) );
            hThetayvsEAll.fill( Math.abs(Math.atan(e.getMomentum().y()/e.getMomentum().z())) , e.getEnergy() );
            
            if (electron==null) {
                electron = e;
            } else {
                if(e.getEnergy() > electron.getEnergy()) {
                    electron = e;
                }
            }
        }
        if(electrons.size()>=2) {
            hele1vsele2.fill(electrons.get(0).getEnergy(),electrons.get(1).getEnergy());
        }
        MCParticle positron = null;
        for(MCParticle e : positrons) {
            hThetayAll.fill( Math.abs(Math.atan(e.getMomentum().y()/e.getMomentum().z())) );
            hThetayvsEAll.fill( Math.abs(Math.atan(e.getMomentum().y()/e.getMomentum().z())) , e.getEnergy() );
            if (positron==null) {
                positron = e;
            } else {
                if(e.getEnergy() > positron.getEnergy()) {
                    positron = e;
                }
            }
        }

            
        if(electron!=null && positron!=null) {
            double electron_thetay = Math.abs(Math.atan(electron.getMomentum().y()/electron.getMomentum().z()));               
            double positron_thetay = Math.abs(Math.atan(positron.getMomentum().y()/positron.getMomentum().z()));
            double electron_E = electron.getEnergy();
            double positron_E = positron.getEnergy();
            double thetay_smallest = electron_thetay < positron_thetay ? electron_thetay : positron_thetay;
            double E_thetay_smallest = electron_thetay < positron_thetay ? electron.getEnergy() : positron.getEnergy();
            double thetay_largest = electron_thetay < positron_thetay ? positron_thetay : electron_thetay;
            double E_thetay_largest = electron_thetay < positron_thetay ? positron.getEnergy() : electron.getEnergy();
      
            double highest_E = -1;//electron.getEnergy() > positron.getEnergy() ? electron.getEnergy() : positron.getEnergy();
            double highest_E_thetay = -1;//electron.getEnergy() > positron.getEnergy() ? electron_thetay : positron_thetay;

            if(electron_thetay > positron_thetay && electron_E > positron_E) {
                highest_E_thetay = electron_thetay;
                highest_E = electron_E;
            } else if(electron_thetay < positron_thetay && electron_E < positron_E) {
                highest_E_thetay = positron_thetay;
                highest_E = positron_E;
            }
            
            
            hThetay.fill( electron_thetay );
            hThetay.fill( positron_thetay );
            hThetaySmallest.fill( thetay_smallest );
            if(highest_E>0) hThetayLargest.fill( highest_E_thetay );
            hThetaySmallestvsE.fill(thetay_smallest, E_thetay_smallest);
            if(highest_E>0) hThetayLargestvsE.fill(highest_E_thetay, highest_E);
            hThetayvsThetay.fill(thetay_smallest, thetay_largest );
            if(E_thetay_smallest>0.0) {
                hThetaySmallestEcut.fill( thetay_smallest );
                hThetayLargestEcut.fill( highest_E_thetay );
                if(trigger_data!=null) {
                    hTrigThetaySmallestvsE.fill(thetay_smallest, E_thetay_smallest);
                    if(highest_E>0) hTrigThetayLargestvsE.fill(highest_E_thetay, highest_E);
                    hTrigThetayvsThetay.fill(thetay_smallest, thetay_largest );
                    hTrigThetaySmallestEcut.fill( thetay_smallest );
                    if(highest_E>0) hTrigThetayLargestEcut.fill( highest_E_thetay );
                } 
            }
            
            
        }
        if(totalEvents % 500 == 0 && !this._hideFrame) updatePlots(); //plots are updated at end of data anyway
        totalEvents++;
        
    }
    
    MCParticle ele_cand = null;
    MCParticle pos_cand = null;
    
    List<MCParticle> getTriggerCandidates(int pdgid, double E_cut, double thetay_cut, List<MCParticle> fsParticles) {
        List<MCParticle> particles  = new ArrayList<MCParticle>();
        for(MCParticle particle : fsParticles) {
            int pdgID = particle.getPDGID();
            if(pdgid!=pdgID) continue;
            if(E_cut>0 && particle.getEnergy()<E_cut) continue;
            Hep3Vector p = particle.getMomentum();
            double thetay = Math.abs(Math.atan(p.y()/p.z()));
            if(thetay_cut>0 && thetay<thetay_cut) continue;
            particles.add(particle);
        }
        return particles;
   }
    
    @Override
   public void endOfData() {
        updatePlots();
        if (!"".equals(this._aidaFileName))
        try {
            aida.saveAs(this._aidaFileName);
        } catch (IOException ex) {
            Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + this._aidaFileName, ex);
        }
        
        if(this._hideFrame) {
            plotter_count.hide();
            plotter_count_1.hide();
            plotter_count_11.hide();
            plotter_count_2.hide();
            plotter_count_22.hide();
            plotter_count_3.hide();
            plotter_count_4.hide();
        }
        
   }
    
    
   private void makePlots() {
        hCountTrig = aida.histogram1D("hCountTrig",2,0,2);
        hThetay = aida.histogram1D("Theta_y",50,0,0.1);
        hThetayAll = aida.histogram1D("Theta_y all",50,0,0.1);
        
        hThetayvsEAll = aida.histogram2D("Theta_y vs E all",25,0,0.1,20,0,2.);
        hThetaySmallest = aida.histogram1D("Theta_y smallest",50,0,0.05);
        hThetaySmallestEcut = aida.histogram1D("Theta_y smallest Ecut",50,0,0.05);
        hTrigThetaySmallestEcut = aida.histogram1D("Triggered Theta_y smallest Ecut",50,0,0.05);
        hTrigThetaySmallestEcut.annotation().addItem("xAxisLabel", "Theta_y [rad]");
        hThetayLargest = aida.histogram1D("Theta_y largest",50,0,0.05);
        hThetayLargestEcut = aida.histogram1D("Theta_y largest Ecut",50,0,0.05);
        hTrigThetayLargestEcut = aida.histogram1D("Triggered Theta_y largest Ecut",50,0,0.05);
        hTrigThetayLargestEcut.annotation().addItem("xAxisLabel", "Theta_y [rad]");
        hThetaySmallestvsE = aida.histogram2D("Theta_y smallest vs E",25,0,0.1,20,0,2.);
        hTrigThetaySmallestvsE = aida.histogram2D("Triggered Theta_y smallest vs E",25,0,0.1,20,0,2.);
        hThetayLargestvsE = aida.histogram2D("Theta_y largest vs E",25,0,0.1,20,0,2.);
        hTrigThetayLargestvsE = aida.histogram2D("Triggered Theta_y largest vs E",25,0,0.1,20,0,2.);
        hThetayvsThetay = aida.histogram2D("Theta_y for e+e- pair",50,0,0.1,50,0,0.2);
        hTrigThetayvsThetay = aida.histogram2D("Triggered Theta_y for e+e- pair",50,0,0.1,50,0,0.2);
        
        hele1vsele2 = aida.histogram2D("hele1vsele2", 50,0,2,50,0,2);
        
        plotter_count = af.createPlotterFactory().create();
        plotter_count.createRegions(2,2);
        plotter_count.setTitle("Trigger Count");
        plotter_count.style().setParameter("hist2DStyle", "colorMap");
        plotter_count.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_count.region(0).plot(hCountTrig);
        plotter_count.region(1).plot(hThetayvsEAll);
        plotter_count.region(2).plot(hThetay);
        plotter_count.region(3).plot(hThetayAll);
        
        plotter_count_1 = af.createPlotterFactory().create();
        plotter_count_1.createRegions(2,2);
        plotter_count_1.setTitle("Acceptance vs Thetay");
        plotter_count_1.region(0).plot(hThetaySmallest);
        plotter_count_1.region(1).plot(hThetaySmallestEcut);
        plotter_count_1.region(2).plot(hTrigThetaySmallestEcut);
        plotter_count_1.region(3).style().statisticsBoxStyle().setVisible(false);
        
        plotter_count_11 = af.createPlotterFactory().create();
        plotter_count_11.createRegions(2,2);
        plotter_count_11.setTitle("Acceptance vs Thetay largest");
        //plotter_count.style().statisticsBoxStyle().setVisible(true);
        plotter_count_11.region(0).plot(hThetayLargest);
        plotter_count_11.region(1).plot(hThetayLargestEcut);
        plotter_count_11.region(2).plot(hTrigThetayLargestEcut);
        plotter_count_11.region(3).style().statisticsBoxStyle().setVisible(false);
        
        plotter_count_2 = af.createPlotterFactory().create();
        plotter_count_2.createRegions(2,2);
        plotter_count_2.setTitle("Trigger Count");
        //plotter_count.style().statisticsBoxStyle().setVisible(true);
        plotter_count_2.style().setParameter("hist2DStyle", "colorMap");
        plotter_count_2.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_count_2.region(0).plot(hThetaySmallestvsE);
        plotter_count_2.region(1).plot(hTrigThetaySmallestvsE);
        plotter_count_2.region(2).style().statisticsBoxStyle().setVisible(false);
        
        plotter_count_22 = af.createPlotterFactory().create();
        plotter_count_22.createRegions(2,2);
        plotter_count_22.setTitle("Trigger Count");
        //plotter_count.style().statisticsBoxStyle().setVisible(true);
        plotter_count_22.style().setParameter("hist2DStyle", "colorMap");
        plotter_count_22.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_count_22.region(0).plot(hThetayLargestvsE);
        plotter_count_22.region(1).plot(hTrigThetayLargestvsE);
        plotter_count_22.region(2).style().statisticsBoxStyle().setVisible(false);
        
        plotter_count_3 = af.createPlotterFactory().create();
        plotter_count_3.createRegions(1,2);
        plotter_count_3.setTitle("Trigger Count");
        //plotter_count.style().statisticsBoxStyle().setVisible(true);
        plotter_count_3.style().setParameter("hist2DStyle", "colorMap");
        plotter_count_3.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_count_3.region(0).plot(hThetayvsThetay);
        plotter_count_3.region(1).plot(hTrigThetayvsThetay);
        
        
        plotter_count_4 = af.createPlotterFactory().create();
        plotter_count_4.createRegions(1,1);
        plotter_count_4.setTitle("e- vs e-");
        //plotter_count.style().statisticsBoxStyle().setVisible(true);
        plotter_count_4.style().setParameter("hist2DStyle", "colorMap");
        plotter_count_4.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_count_4.region(0).style().statisticsBoxStyle().setVisible(false);
        plotter_count_4.region(0).plot(hele1vsele2);
        
        if(!this._hideFrame) {
            plotter_count.show();
            plotter_count_1.show();
            plotter_count_11.show();
            plotter_count_2.show();
            plotter_count_22.show();
            plotter_count_3.show();
            plotter_count_4.show();
        }
        
        
   }
    
   private void updatePlots() {
       
       //hTrigEffThetaySmallest = aida.histogram1D("hTrigEffThetaySmallest",50,0,0.1);
       IHistogram1D hTrigEffThetaySmallest = hf.divide("Trigger efficiency vs Theta_y smallest", hTrigThetaySmallestEcut, hThetaySmallestEcut);
       hTrigEffThetaySmallest.annotation().addItem("xAxisLabel", "Theta_y [rad]");
       hTrigEffThetaySmallest.annotation().addItem("yAxisLabel", "Trigger Efficiency");
       plotter_count_1.region(3).clear();
       plotter_count_1.region(3).style().statisticsBoxStyle().setVisible(false);
       plotter_count_1.region(3).plot(hTrigEffThetaySmallest);
       IHistogram2D hTrigEffThetaySmallestvsE = hf.divide("Trigger efficiency Theta_y vs E smallest", hTrigThetaySmallestvsE,hThetaySmallestvsE);
       hTrigEffThetaySmallestvsE.annotation().addItem("xAxisLabel", "Theta_y [rad]");
       hTrigEffThetaySmallestvsE.annotation().addItem("yAxisLabel", "Energy [GeV]");
       plotter_count_2.region(2).clear();
       plotter_count_2.region(2).style().statisticsBoxStyle().setVisible(false);
       plotter_count_2.region(2).plot(hTrigEffThetaySmallestvsE);
       
       IHistogram1D hTrigEffThetayLargest = hf.divide("Trigger efficiency vs Theta_y largest", hTrigThetayLargestEcut, hThetayLargestEcut);
       hTrigEffThetayLargest.annotation().addItem("xAxisLabel", "Theta_y [rad]");
       hTrigEffThetayLargest.annotation().addItem("yAxisLabel", "Trigger Efficiency");
       plotter_count_11.region(3).clear();
       plotter_count_11.region(3).style().statisticsBoxStyle().setVisible(false);
       plotter_count_11.region(3).plot(hTrigEffThetayLargest);   
       IHistogram2D hTrigEffThetayLargestvsE = hf.divide("Trigger efficiency Theta_y vs E largest", hTrigThetayLargestvsE,hThetayLargestvsE);
       hTrigEffThetayLargestvsE.annotation().addItem("xAxisLabel", "Theta_y [rad]");
       hTrigEffThetayLargestvsE.annotation().addItem("yAxisLabel", "Energy [GeV]");
       plotter_count_22.region(2).clear();
       plotter_count_22.region(2).style().statisticsBoxStyle().setVisible(false);
       plotter_count_22.region(2).plot(hTrigEffThetayLargestvsE);
       
   }
    
}
