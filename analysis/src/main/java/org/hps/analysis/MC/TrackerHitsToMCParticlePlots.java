package org.hps.analysis.MC;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mrsolt
 * This driver plots info related to Tracker Hits to MC Particle Truth Info
 */
public class TrackerHitsToMCParticlePlots extends Driver {

    //Collection Names
    private String mcParticleCollectionName = "MCParticle";
    private String trackerHitCollectionName = "TrackerHits";
    
    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 
        
    //List of Histograms
    IHistogram1D hasMCParticle;
    
    public void detectorChanged(Detector detector){
        
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        //Setup Plots
        hasMCParticle = histogramFactory.createHistogram1D("Tracker Hit has MCParticle", 2, 0, 2);
    }
    
    public void setMcParticleCollectionName(String mcParticleCollectionName) {
        this.mcParticleCollectionName = mcParticleCollectionName;
    }

    public void settrackerHitCollectionName(String trackerHitCollectionName) {
        this.trackerHitCollectionName = trackerHitCollectionName;
    }
    
    protected void process(EventHeader event) {
        
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, trackerHitCollectionName);
        
        for(SimTrackerHit hit:trackerHits){
            MCParticle p = hit.getMCParticle();
            int hasMCPart = 0;
            if(p != null)
                hasMCPart = 1;
            hasMCParticle.fill(hasMCPart);
        }
        
    }

}