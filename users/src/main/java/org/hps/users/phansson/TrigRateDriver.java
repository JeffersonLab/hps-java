package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.deprecated.BeamlineConstants;
import org.hps.conditions.deprecated.EcalConditions;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.util.AIDAFrame;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson+
 */
public class TrigRateDriver extends Driver {
    
    int nevents = 0;
    boolean debug = true;
    protected IDDecoder dec = null;
    protected Subdetector ecal;
    private String ecalName = "Ecal";
    
    
    private boolean hideFrame = true;
    private boolean simTrigger = false;
    
    private String outputPlotFileName = "trigRate.aida";
    private String trackCollectionName = "MatchedTracks";
    private String ecalClusterCollectionName = "EcalClusters";
    private String triggerClusterCollectionName = "EcalTriggerClusters";
    private double triggerThreshold = 10.0;
    
    EcalTrackMatch trkMatchTool;
    
    private boolean doTracking = false;
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    IHistogramFactory hf = aida.histogramFactory();
    private AIDAFrame plotterFrame;
    private AIDAFrame plotterFrameTrig;

    private static int nThr = 3;
    private int eThr[] = {0,600,800};
    

    IPlotter plotter_trig_tag;
    IHistogram1D clusterEnergy[][] = new IHistogram1D[47][11];
    IHistogram2D meanClusterEnergy;
    IHistogram1D hityCol[][] = new IHistogram1D[nThr][47];

    
    private int trigger[] = {0,0};

    
    private int refreshRate = 1000;
    
    // Cluster energy correction
    //double C_ep = 1.0; //1.0/0.3;

    
    
    public void startOfData() {
    }
    
    public void detectorChanged(Detector detector) {
	// Get the Subdetector.
	ecal = detector.getSubdetector(ecalName);

	// Cache ref to decoder.
	dec = ecal.getIDDecoder();
        
        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("TrigRateFrame");
        
        IPlotterStyle style;
        
        IPlotter plotter_hitmap_gr = af.createPlotterFactory().create();
        plotter_hitmap_gr.createRegions(2,nThr+1,0);
        plotter_hitmap_gr.setTitle("Cluster hit map");
        plotter_hitmap_gr.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_hitmap_gr);

        IPlotter plotter_hitmap_1 = af.createPlotterFactory().create();
        plotter_hitmap_1.createRegions(1);
        plotter_hitmap_1.setTitle("Cluster hit map");
        plotter_hitmap_1.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_hitmap_1);

        IPlotter plotter_hitY_gr = af.createPlotterFactory().create();
        plotter_hitY_gr.createRegions(2,nThr+1,0);
        plotter_hitY_gr.setTitle("Cluster hit Y");
        plotter_hitY_gr.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_hitY_gr);

        IPlotter plotter_hitTheta_gr = af.createPlotterFactory().create();
        plotter_hitTheta_gr.createRegions(2,nThr+1,0);
        plotter_hitTheta_gr.setTitle("Cluster hit theta");
        plotter_hitTheta_gr.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_hitTheta_gr);

        
        IPlotter plotter_ep_gr = af.createPlotterFactory().create();
        plotter_ep_gr.createRegions(2,nThr+1,0);
        plotter_ep_gr.setTitle("Cluster E over p");
        plotter_ep_gr.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_gr);

        
        IHistogram hm = aida.histogram2D("Cluster hit map", 52, -25.5, 25.5, 6, -5.5, 5.5);
        plotter_hitmap_1.region(0).plot(hm);            
        style = plotter_hitmap_1.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        ((PlotterRegion) plotter_hitmap_1.region(0)).getPlot().setAllowUserInteraction(true);
        ((PlotterRegion) plotter_hitmap_1.region(0)).getPlot().setAllowPopupMenus(true);
        
        
        for(int iside=0;iside<2;++iside) {
            String side = iside==0 ? "top" : "bottom";
            double ymin = iside == 0 ? -0.5 : -5.5;
            double ymax = iside == 0 ? 5.5 : 0.5;
            
            IHistogram h = aida.histogram2D("Hit map " + side, 26, -25.5, 0.5, 6, ymin, ymax);
            plotter_hitmap_gr.region((nThr+1)*iside).plot(h);

            
            IHistogram hy = aida.histogram1D("Hit Y " + side, 7, -0.5, 6.5);
            plotter_hitY_gr.region((nThr+1)*iside).plot(hy);
            
            IHistogram hth = aida.histogram1D("Hit theta " + side, 10, 0, 80);
            plotter_hitTheta_gr.region((nThr+1)*iside).plot(hth);
            
            IHistogram hep = aida.histogram1D("Cluster Eoverp " + side, 50, 0, 2);
            plotter_ep_gr.region((nThr+1)*iside).plot(hep);
            
            
            for(int i=0;i<nThr;++i) {            
                int reg = ((nThr+1)*iside)+(i+1);
                if(debug) System.out.println("reg " + reg);
                h = aida.histogram2D("Hit map E>" + eThr[i] + "MeV "  + side, 26, -25.5, 0.5, 6, ymin, ymax);
                plotter_hitmap_gr.region(reg).plot(h);
                
                hy = aida.histogram1D("Hit Y E>" + eThr[i] + "MeV " + side, 7, -0.5,6.5);
                plotter_hitY_gr.region(reg).plot(hy);

                hth = aida.histogram1D("Hit theta E>" + eThr[i] + "MeV " + side, 20, 0,80);
                plotter_hitTheta_gr.region(reg).plot(hth);

                
                for(int icol=-23;icol<=23;++icol) {
                    hityCol[i][icol+23] = aida.histogram1D("Hit Y E>" + eThr[i] + "MeV " + side + " col"+icol, 7, -0.5,6.5);
                }
                
                hep = aida.histogram1D("Eoverp E>" + eThr[i] + "MeV " + side, 50, 0,2);
                plotter_ep_gr.region(reg).plot(hep);
                
            
            }
        }
        

        for(int i=0;i<2*(nThr+1);++i) {
                            
            if(debug) System.out.println("i " + i);

            style = plotter_hitmap_gr.region(i).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_hitmap_gr.region(i)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_hitmap_gr.region(i)).getPlot().setAllowPopupMenus(true);
        }

        
        
        for(int irow=-5;irow<=5;++irow) {
            for(int icol=-23;icol<=23;++icol) {
                clusterEnergy[icol+23][irow+5] = aida.histogram1D("Cluster energy x=" + icol + " y=" + irow, 50, 0,6000);  
            }
        }
        
        meanClusterEnergy = aida.histogram2D("Mean cluster energy", 47, -23, 23, 11, -5, 5);
        
        IPlotter plotter_clE = af.createPlotterFactory().create();
        //plotter_clE.createRegions(2,6,0);
        plotter_clE.setTitle("Cluster Energy");
        plotter_clE.style().statisticsBoxStyle().setVisible(false);
        plotter_clE.style().setParameter("hist2DStyle", "colorMap");
        plotter_clE.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_clE.currentRegion().plot(meanClusterEnergy);
        
        plotterFrame.addPlotter(plotter_clE);
        
        
            
        if(!hideFrame) {

            plotterFrame.pack();
            plotterFrame.setVisible(true);
        }
               
        
        
      
        
        
        
    }
    
    public TrigRateDriver() {
    
        trkMatchTool = new EcalTrackMatch(false);
    
    }
    

    

    public void setDebug(boolean flag) {
        this.debug = flag;
    }
    
    public void setOutputPlotFileName( String name ) {
        this.outputPlotFileName = name;
    }
    
    public void setHideFrame( boolean val ) {
        this.hideFrame = val;
    }
    
    public void setSimTrigger(boolean simTrigger) {
        this.simTrigger = simTrigger;
    }
    
    
    public void process(EventHeader event) {
        ++nevents;
        if( debug ) {
            System.out.println("Processing event " + nevents);    
        }
        
        if( refreshRate > 0 && nevents % refreshRate == 0 ) {
            redraw();
        }
        
        
        if(simTrigger) {
            boolean trigger = false;
            if(event.hasCollection(HPSEcalCluster.class, triggerClusterCollectionName)) {
                for(HPSEcalCluster cluster : event.get(HPSEcalCluster.class, triggerClusterCollectionName)) {
                    if(cluster.getEnergy() > triggerThreshold) {
                        trigger = true;
                    }
                }
            }
            if(!trigger) {
                if(debug) {
                    System.out.println("event " + nevents + " did NOT trigger using simTrigger");
                }
                return;
            }
            if(debug) {
                System.out.println("event " + nevents + " triggered using simTrigger");
            }
        }
        
        
        //fastTracking(event);
        
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClusterCollectionName); 
        
        
        if(debug) System.out.println( clusters.size() + " ECal clusters in the event");
        
        //plotClusterDistr(clusters,"");
        
        
        List<Track> tracks = null;
        if(doTracking) {
            if(event.hasCollection(Track.class, trackCollectionName)) {
                tracks = event.get(Track.class, trackCollectionName);
            } else {
                tracks = new ArrayList<Track>();
            }
            if(debug) System.out.println( tracks.size() + " tracks in this event");
        }
        
        
        for(HPSEcalCluster cl : clusters) {
            
            int[] clusterPosIdx = new int[2];
            clusterPosIdx[0] = cl.getSeedHit().getIdentifierFieldValue("ix");
            clusterPosIdx[1] = cl.getSeedHit().getIdentifierFieldValue("iy");
            //Uses shower max position -> update ix,iy above? --> FIX THIS!
            double clusterPosY = cl.getPosition()[1];
            double clusterPosZ = cl.getPosition()[2];         
            String side = clusterPosIdx[1]>0 ? "top" : "bottom";
            int iside = clusterPosIdx[1]>0 ? 0 : 1;
            
            int hitY = clusterPosIdx[1]>0 ? clusterPosIdx[1] : (-1*clusterPosIdx[1]);
            double hitTheta = Math.atan(clusterPosY/(clusterPosZ-BeamlineConstants.HARP_POSITION_TESTRUN));
            //x-check
            if(hitTheta<0) {
                if(!"bottom".equals(side)) {
                    throw new RuntimeException("Hit theta was inconsistent with side!");
                }
                hitTheta *= -1;
            }
            
            clusterEnergy[clusterPosIdx[0]+23][clusterPosIdx[1]+5].fill(cl.getEnergy());
            aida.histogram2D("Cluster hit map").fill(clusterPosIdx[0], clusterPosIdx[1]);
            aida.histogram2D("Hit map " + side).fill(clusterPosIdx[0], clusterPosIdx[1]);
            aida.histogram1D("Hit Y " + side).fill(hitY);
            aida.histogram1D("Hit theta " + side).fill(hitTheta*1000.0); //mrad
                    
            double eoverp = -1;
            if(doTracking) {
                trkMatchTool.setCluster(cl);
                trkMatchTool.match(tracks);
                if(trkMatchTool.isMatchedY(20)) {
                    eoverp = cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000);
                }   
            }
            if(eoverp>0) aida.histogram1D("Eoverp " + side).fill(eoverp);

            if(debug) System.out.println("Ep = " + eoverp + " doTracking " + doTracking);

            for(int i=0;i<nThr;++i){
                if(cl.getEnergy()>eThr[i]) {
                    aida.histogram2D("Hit map E>" + eThr[i] + "MeV " + side).fill(clusterPosIdx[0], clusterPosIdx[1]);
                    aida.histogram1D("Hit Y E>" + eThr[i] + "MeV " + side).fill(hitY);
                    aida.histogram1D("Hit theta E>" + eThr[i] + "MeV " + side).fill(hitTheta*1000.0); //mrad
                    if(eoverp>0) aida.histogram1D("Eoverp E>" + eThr[i] + "MeV " + side).fill(eoverp);
                    hityCol[i][clusterPosIdx[0]+23].fill(clusterPosIdx[1]);
                }

            }


        }
        

    
    
    
    
    }
       
    
    
    
  
    
    
 
      
    
    
     
    private boolean hasBadNeighbours(HPSEcalCluster cluster) {
        //check if this cluster has a neighbour that is dead or bad
        if(!EcalConditions.badChannelsLoaded()) return false;
        List<CalorimeterHit> hits = cluster.getCalorimeterHits();
        IIdentifierHelper helper = EcalConditions.getHelper();
        IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
        expId.setValue(helper.getFieldIndex("system"), ecal.getSystemID());
        
        boolean bad = false;
        
        System.out.println("Checking cluster with " + cluster.getSize() + " hits for bad neighbours");
        
        for(CalorimeterHit hit : hits) {
            IIdentifier compactId = hit.getIdentifier();   
            //x-check
            if(EcalConditions.isBadChannel(hit.getCellID())) {
                System.out.println("This cluster has a bad channel hit included!? ");
                int x = helper.getValue(compactId, "ix");
                int y = helper.getValue(compactId, "iy");
                System.out.println(x + "," + y + " id " + hit.getCellID());
                System.exit(1);
            }
            //Find crystal pair
            
              
                        
            int x = helper.getValue(compactId, "ix");
            int y = helper.getValue(compactId, "iy");
            System.out.println("Hit at " + x + "," + y);
            for(int ix=x-1;ix!=x+2;++ix) {
                for(int iy=y-1;iy!=y+2;++iy) {
                    expId.setValue(helper.getFieldIndex("ix"), ix);
                    expId.setValue(helper.getFieldIndex("iy"), iy);
                    IIdentifier compactId_t = helper.pack(expId);
                    System.out.println("Check" + ix + "," + iy + " id " + compactId_t.getValue());
                    if(EcalConditions.isBadChannel(compactId_t.getValue())) {
                        System.out.println("This cell was BAD!");
                        return true;
                    }
                }    
            }
            
            
        }
        return false;
        
    }
     
    
    
    
 
   

    
    private void redraw() {
         //System.out.println("redraw");
         if(meanClusterEnergy.entries()>0) {
         meanClusterEnergy.reset();
         //System.out.println("redraw cluster energy");
         for(int irow=-5;irow<=5;++irow) {
            for(int icol=-23;icol<=23;++icol) {
                //System.out.println(irow+" "+icol);
                if(clusterEnergy[icol+23][irow+5].entries()>5) {
                    meanClusterEnergy.fill(icol,irow,clusterEnergy[icol+23][irow+5].mean());
                }
            }
        }
         }
    }
        
    
    
    public void endOfData() {
        if(nevents>0){
            redraw();
        }
        
        
        
        
        
        if (outputPlotFileName != "")
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        //displayFastTrackingPlots();
        
    }

    
}
