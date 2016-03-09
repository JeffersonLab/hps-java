/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking.gbl;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.util.aida.AIDA;

/**
 * Calculates and plots truth residuals for track 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: TruthResiduals.java,v 1.10 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
 */
public class TruthResiduals {
    
    private int _debug;
    private boolean _hideFrame = true;
    private Hep3Vector _B;
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private Map<Integer, List<IHistogram1D> > res_truthsimhit = null;
    private Map<Integer, List<IHistogram1D> > res_truthsimhit_top_plus = null;
    private Map<Integer, List<IHistogram1D> > res_truthsimhit_bot_plus = null;
    private Map<Integer, List<IHistogram1D> > res_truthsimhit_top_minus = null;
    private Map<Integer, List<IHistogram1D> > res_truthsimhit_bot_minus = null;
    private IHistogram2D h_mcp_org;
    private IHistogram2D trkpos_y_vs_x;
    private boolean firstWeirdTrack = true;
    
    

    /*
     * file name
     * Bz in Tesla
     */
    public TruthResiduals(Hep3Vector bfield) {
        _B = CoordinateTransformations.transformVectorToTracking(bfield);
        System.out.printf("%s: B field %s\n",this.getClass().getSimpleName(),_B.toString());
    }
    public void setDebug(int debug) {
        _debug = debug;
    }
    public void setHideFrame(boolean hide) {
        _hideFrame = hide;
    }
    
    
    public void processSim(List<MCParticle> mcParticles, List<SimTrackerHit> simTrackerHits) {
        
        if(res_truthsimhit == null) makePlots();
        
        
        // map layer, mcparticles and sim hits
        Map<Integer, List<SimTrackerHit>> simHitsLayerMap = new HashMap<Integer, List<SimTrackerHit> >();
        Map<MCParticle, List<SimTrackerHit> > mcPartSimHitsMap = new HashMap<MCParticle, List<SimTrackerHit > >();
        for(SimTrackerHit sh : simTrackerHits) {
             Hep3Vector shpos = CoordinateTransformations.transformVectorToTracking(sh.getPositionVec());
            if(Math.abs(shpos.x()) < 50.0) {
                 System.out.printf("%s: Weird hit at %s (%s) in layer %d for MC part %d org %s p %s\n",
                         this.getClass().getSimpleName(),sh.getPositionVec().toString(),shpos.toString(),sh.getIdentifierFieldValue("layer"),
                         sh.getMCParticle().getPDGID(),sh.getMCParticle().getOrigin().toString(),sh.getMCParticle().getMomentum().toString());
                 System.exit(1);
            }
            
            int layer  = sh.getIdentifierFieldValue("layer");
            if(!simHitsLayerMap.containsKey(layer)) {
                simHitsLayerMap.put(layer, new ArrayList<SimTrackerHit>());
            }
            simHitsLayerMap.get(layer).add(sh);
            
            MCParticle part = sh.getMCParticle();
            if(!mcPartSimHitsMap.containsKey(part)) {
                mcPartSimHitsMap.put(part, new ArrayList<SimTrackerHit>());
            }
            mcPartSimHitsMap.get(part).add(sh);
        }

        
        for(MCParticle mcp : mcPartSimHitsMap.keySet()) {
            this.h_mcp_org.fill(mcp.getOriginX(), mcp.getOriginY());
        }

        // Find the particle responsible for the hit in each layer and compute the residual
        
        for(int layer=1;layer<13;++layer) {
            //System.out.printf("layer %d: \n",layer);
            
            List<SimTrackerHit> simHitsLayer = simHitsLayerMap.get(layer);
            
            
            if(simHitsLayer != null ) {
                
                if(simHitsLayer.size()==2) continue;
                
                for(SimTrackerHit simHit : simHitsLayer) {
                    
                     // Find the MC particle
                    MCParticle mcp = simHit.getMCParticle();
                    
                    if(mcp.getMomentum().magnitude()<0.5) continue;
                    
                    // Position in tracking coord
                    Hep3Vector simHitPosTracking = CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec());
                    
                    if(_debug>0) {
                        System.out.printf("%s: simHit for layer %d at %s (%s) from MC part %d org %s p %s\n",
                                        this.getClass().getSimpleName(),layer,simHit.getPositionVec().toString(),simHitPosTracking.toString(),
                                        simHit.getMCParticle().getPDGID(),simHit.getMCParticle().getOrigin().toString(),simHit.getMCParticle().getMomentum().toString());
                    
                        if(simHitPosTracking.x()<50.) System.exit(1);
                    
                    }    
                    
                   
                    
                    // Get track parameters from MC particle 
                    //HelicalTrackFit htfTruth = TrackUtils.getHTF(mcp, -1*this._B.z());
                    HelicalTrackFit htfTruth = TrackUtils.getHTF(mcp, -1*this._B.z());
                    
                    Hep3Vector trkposExtraPolator = TrackUtils.extrapolateTrack(htfTruth,simHitPosTracking.x());
                    //System.out.printf("trkposextrapol (det) %s\n",trkposExtraPolator.toString());
                    
                    trkposExtraPolator = CoordinateTransformations.transformVectorToTracking(trkposExtraPolator);
                    
                    // Calculate residuals
                    Hep3Vector res = VecOp.sub(simHitPosTracking, trkposExtraPolator);
                
                    //System.out.printf("trkpos %s trkposextrapol %s res %s\n",trkpos.toString(),trkposExtraPolator.toString(),res.toString());
                    
                    // Fill residuals
                    this.res_truthsimhit.get(layer).get(0).fill(res.y());
                    this.res_truthsimhit.get(layer).get(1).fill(res.z());
                    if(simHit.getPositionVec().y()>0) {
                        if(simHit.getMCParticle().getPDGID()<0) {
                            this.res_truthsimhit_top_plus.get(layer).get(0).fill(res.y());
                            this.res_truthsimhit_top_plus.get(layer).get(1).fill(res.z());
                        }
                        else {
                            this.res_truthsimhit_top_minus.get(layer).get(0).fill(res.y());
                            this.res_truthsimhit_top_minus.get(layer).get(1).fill(res.z());
                        }
                    }
                    else {
                        if(simHit.getMCParticle().getPDGID()<0) {
                            this.res_truthsimhit_bot_plus.get(layer).get(0).fill(res.y());
                            this.res_truthsimhit_bot_plus.get(layer).get(1).fill(res.z());
                        }
                        else {
                            this.res_truthsimhit_bot_minus.get(layer).get(0).fill(res.y());
                            this.res_truthsimhit_bot_minus.get(layer).get(1).fill(res.z());
                        }
                    }
                    
                    if(layer == 1 && res.y() > 0.1 && this.firstWeirdTrack) {
                        double dx = 1.0;
                        double xpos = mcp.getOriginZ();
                        while(xpos< 100.) {
                            xpos += dx;
                            trkposExtraPolator = CoordinateTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(htfTruth,xpos));
                            double ypos = trkposExtraPolator.y();
                            trkpos_y_vs_x.fill(xpos,ypos);
                        }
                        
                        int idummy = 0;
                        while(idummy<2) {
                            trkpos_y_vs_x.fill(simHitPosTracking.x(),simHitPosTracking.y());
                            idummy++;
                            //System.out.printf("weird simhit res pos %s \n", simHitPosTracking.toString());
                        }
                        
                        this.firstWeirdTrack = false;
                        
                    }
                    
                
                }
            }
        }
        
    }

  
    public IHistogram getResidual(int layer,String coord) {
        if( !this.res_truthsimhit.containsKey(layer) ) 
            throw new RuntimeException("Error the layer number is not valid");
        if( coord!="x" || coord!="y")
            throw new RuntimeException("Error the coord is not valid");
        IHistogram1D h = this.res_truthsimhit.get(layer).get(coord=="x"?0:1);
        return h;
    }
    

    
    private void makePlots() {
        
        
        res_truthsimhit = new HashMap<Integer, List<IHistogram1D> >();
        res_truthsimhit_top_plus = new HashMap<Integer, List<IHistogram1D> >();
        res_truthsimhit_bot_plus = new HashMap<Integer, List<IHistogram1D> >();
        res_truthsimhit_top_minus = new HashMap<Integer, List<IHistogram1D> >();
        res_truthsimhit_bot_minus = new HashMap<Integer, List<IHistogram1D> >();

        
        
        
        
        IHistogramFactory hf = aida.histogramFactory();
        double min=-1.;
        double max=1.;
        
        List<IPlotter> plotter1 = new ArrayList<IPlotter>();
        List<IPlotter> plotter2 = new ArrayList<IPlotter>();
        List<IPlotter> plotter3 = new ArrayList<IPlotter>();
        List<IPlotter> plotter4 = new ArrayList<IPlotter>();
        List<IPlotter> plotter5 = new ArrayList<IPlotter>();


        for(int idir=0;idir<2;++idir) {
           String dir = idir==0?"x":"y";
           IPlotter pl1 =  af.createPlotterFactory().create(String.format("SimHit-Truth Track Residual %s",dir));
           pl1.createRegions(3, 4);
           IPlotter pl2 =  af.createPlotterFactory().create(String.format("SimHit-Truth Track Residual %s",dir));
           pl2.createRegions(3, 4);
           IPlotter pl3 =  af.createPlotterFactory().create(String.format("SimHit-Truth Track Residual %s",dir));
           pl3.createRegions(3, 4);
           IPlotter pl4 =  af.createPlotterFactory().create(String.format("SimHit-Truth Track Residual %s",dir));
           pl4.createRegions(3, 4);
           IPlotter pl5 =  af.createPlotterFactory().create(String.format("SimHit-Truth Track Residual %s",dir));
           pl5.createRegions(3, 4);
           
           for(int layer=1;layer<13;++layer) {
                
               if(!res_truthsimhit.containsKey(layer)) {
                   res_truthsimhit.put(layer, new ArrayList<IHistogram1D>());
                   res_truthsimhit_top_plus.put(layer, new ArrayList<IHistogram1D>());
                   res_truthsimhit_bot_plus.put(layer, new ArrayList<IHistogram1D>());
                   res_truthsimhit_top_minus.put(layer, new ArrayList<IHistogram1D>());
                   res_truthsimhit_bot_minus.put(layer, new ArrayList<IHistogram1D>());
               } 
     
               if(layer<2) {
                   max = 0.05;//0.07;
                   min = -0.05;//-0.07;
               } else if(layer<3) {
                       max = 0.3;//0.07;
                       min = -0.3;//-0.07;
               } else {
                   max = 0.5 * layer;
                   min = -1.0*max;
               }
               
               IHistogram1D h = hf.createHistogram1D(String.format("dres_truthsimhit_layer%d_%s",layer,dir),50, min, max);
               h.setTitle(String.format("L%d SimHit-Truth Track Residual in %s",layer , dir));
               res_truthsimhit.get(layer).add(h);
               pl1.region(layer-1).plot(h);
               
               h = hf.createHistogram1D(String.format("res_truthsimhit_top_plus_layer%d_%s",layer,dir),50, min, max);
               h.setTitle(String.format("L%d SimHit-Truth Track (top,q=+1) Residual in %s",layer , dir));
               res_truthsimhit_top_plus.get(layer).add(h);
               pl2.region(layer-1).plot(h);
               
               h = hf.createHistogram1D(String.format("res_truthsimhit_top_minus_layer%d_%s",layer,dir),50, min, max);
               h.setTitle(String.format("L%d SimHit-Truth Track (top,q=-1) Residual in %s",layer , dir));
               res_truthsimhit_top_minus.get(layer).add(h);
               pl3.region(layer-1).plot(h);

               h = hf.createHistogram1D(String.format("res_truthsimhit_bot_minus_layer%d_%s",layer,dir),50, min, max);
               h.setTitle(String.format("L%d SimHit-Truth Track (bot,q=-1) Residual in %s",layer , dir));
               res_truthsimhit_bot_minus.get(layer).add(h);
               pl4.region(layer-1).plot(h);
               
               h = hf.createHistogram1D(String.format("res_truthsimhit_bot_plus_layer%d_%s",layer,dir),50, min, max);
               h.setTitle(String.format("L%d SimHit-Truth Track (bot,q=+1) Residual in %s",layer , dir));
               res_truthsimhit_bot_plus.get(layer).add(h);
               pl5.region(layer-1).plot(h);
               
               
            }
            plotter1.add(pl1);
            plotter2.add(pl2);
            plotter3.add(pl3);
            plotter4.add(pl4);
            plotter5.add(pl5);

            if(!this._hideFrame) {
                pl1.show();
                pl2.show();
                pl3.show();
                pl4.show();
                pl5.show();
            }
            else {
                pl1.hide();
                pl2.hide();
                pl3.hide();
                pl4.hide();
                pl5.hide();
            }
        
        }
        
        
        this.h_mcp_org = hf.createHistogram2D("MC particle origin", 50, -0.2,0.2,50,-0.2,0.2);
        IPlotter pl_org = af.createPlotterFactory().create("MC particle origin");
        pl_org.createRegions(1, 1);
        pl_org.region(0).plot(h_mcp_org);
        pl_org.region(0).style().setParameter("hist2DStyle", "colorMap");
        pl_org.region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        if(this._hideFrame) pl_org.hide();
        else pl_org.show();
        
        
        trkpos_y_vs_x = hf.createHistogram2D("Track pos y vs x", 300, -150,150,100,-4,4);
        IPlotter pl_pos_y_vs_x = af.createPlotterFactory().create("Track pos y vs x");
        pl_pos_y_vs_x.createRegions(1, 1);
        pl_pos_y_vs_x.region(0).plot(trkpos_y_vs_x);
        pl_pos_y_vs_x.region(0).style().setParameter("hist2DStyle", "colorMap");
        pl_pos_y_vs_x.region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        if(this._hideFrame) pl_pos_y_vs_x.hide();
        else pl_pos_y_vs_x.show();
        
        
        
        
    }

   


}
