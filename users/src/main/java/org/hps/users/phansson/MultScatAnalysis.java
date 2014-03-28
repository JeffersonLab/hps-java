package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.deprecated.EcalConditions;
import org.hps.readout.ecal.TriggerData;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.EcalTrackMatch;
import org.hps.recon.tracking.TrackUtils;
import org.hps.util.AIDAFrame;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson+
 */
public class MultScatAnalysis extends Driver {
    
    int nevents = 0;
    boolean debug = false;
    boolean saveFile = false;
    protected IDDecoder dec = null;
    protected Subdetector ecal;
    private String ecalName = "Ecal";
    String sides[] = {"top","bottom","all"};
    double crystalX;
    double crystalY;
    double beamGap;
    double EcalZPosition;
    public static final double TARGETZ = -67.4; //mm
    private static int crystalCols;
    private static int crystalRows;
    int ecalClusterSel;
    boolean hide = false;
    
    private String outputPlotFileName = "test.aida";
    private String trackCollectionName = "MatchedTracks";
    private String ecalClusterCollectionName = "EcalClusters";
    private String inputEcalHitCollection = "EcalCalHits";
    private boolean doTriggerPart = false;
    private boolean doTrackingPart = false;
    

    
    EcalTrackMatch trkMatchTool;
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    IHistogramFactory hf = aida.histogramFactory();
    private AIDAFrame plotterFrame;
    private AIDAFrame plotterFrameTrig;
    
    IPlotter plotter_trig_tag;
    IPlotter plotter_cltrkmatchE;
    IHistogram1D clusterEnergy[][] = new IHistogram1D[47][11];
            
    private int trigger[] = {0,0};
    
    
    private int refreshRate = 1000;
    
    
    
    public void startOfData() {
    }
    
    public void detectorChanged(Detector detector) {
	// Get the Subdetector.
	ecal = detector.getSubdetector(ecalName);

	// Cache ref to decoder.
	dec = ecal.getIDDecoder();
        
        //Ecal geometry
        crystalX = (13.3 + 16.0) / 2;
        crystalY = (13.3 + 16.0) / 2;
        beamGap = 37.0;//20.0;
        crystalCols = 46;
        crystalRows = 5;
        
        ecalClusterSel = 1;
        
        
        if(doTriggerPart) {

            plotterFrameTrig = new AIDAFrame();
            plotterFrameTrig.setTitle("Trigger");

            IPlotter plotter_trig_other = af.createPlotterFactory().create();
            plotter_trig_other.createRegions(3,1,0);
            plotter_trig_other.setTitle("Other");
            plotterFrameTrig.addPlotter(plotter_trig_other);


            IHistogram ht1 = aida.histogram1D("trigger_count" , 7, 0, 7); 
            IHistogram ht2 = aida.histogram1D("trigger_bank" , 2, 0, 2); 
            IHistogram ht3 = aida.histogram1D("trigger_list" , 2, 0, 2);         
            plotter_trig_other.region(0).plot(ht1);
            plotter_trig_other.region(1).plot(ht2);
            plotter_trig_other.region(2).plot(ht3);
            for(int i=0;i<3;++i) {
                IPlotterStyle style = plotter_trig_other.region(i).style();
                //style.setParameter("hist2DStyle", "colorMap");
                //style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_trig_other.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_trig_other.region(i)).getPlot().setAllowPopupMenus(true);
            }




            plotter_trig_tag = af.createPlotterFactory().create();
            plotter_trig_tag.createRegions(3,3,0);
            plotter_trig_tag.setTitle("Tag&Probe Top");
            plotterFrameTrig.addPlotter(plotter_trig_tag);
            IHistogram htag1 = aida.histogram1D("toptrig_cl_ecal_n_top" , 7, 0, 7); 
            IHistogram htag2 = aida.histogram1D("toptrig_cl_ecal_e_top" , 100, 0, 1500); 
            IHistogram htag3 = aida.histogram1D("toptrig_cl_ecal_emax_top" , 100, 0, 1500); 
            IHistogram htag4 = aida.histogram1D("toptrig_cl_ecal_e_bottom" , 100, 0, 1500); 
            IHistogram htag5 = aida.histogram1D("toptrig_cl_ecal_e_bottom_trig" , 100, 0, 1500); 
            IHistogram htag6 = aida.histogram1D("toptrigtag_cl_ecal_e_bottom" , 100, 0, 1500); 
            IHistogram htag7 = aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trig" , 100, 0, 1500); 
            IHistogram htag8 = aida.histogram1D("toptrig_cl_ecal_e_bottom_trigeff" , 100, 0, 1500); 
            IHistogram htag9 = aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trigeff" , 100, 0, 1500); 

            plotter_trig_tag.region(0).plot(htag1);
            plotter_trig_tag.region(3).plot(htag2);
            plotter_trig_tag.region(6).plot(htag3);
            plotter_trig_tag.region(1).plot(htag4);
            plotter_trig_tag.region(2).plot(htag6);
            plotter_trig_tag.region(4).plot(htag5);
            plotter_trig_tag.region(5).plot(htag7);
            plotter_trig_tag.region(7).plot(htag9);
            plotter_trig_tag.region(8).plot(htag9);

            for(int i=0;i<9;++i) {
                IPlotterStyle style = plotter_trig_tag.region(i).style();
                //style.setParameter("hist2DStyle", "colorMap");
                //style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_trig_tag.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_trig_tag.region(i)).getPlot().setAllowPopupMenus(true);
            }


            IPlotter plotter_trig_tag2 = af.createPlotterFactory().create();
            plotter_trig_tag2.setTitle("Tag&Probe Top Counts");
            plotterFrameTrig.addPlotter(plotter_trig_tag2);
            IHistogram htag10 = aida.histogram1D("toptrig_cl_ecal_n_bottom" , 7, 0, 7); 
            plotter_trig_tag2.createRegion().plot(htag10);

            if(!hide) {
                plotterFrameTrig.pack();
                plotterFrameTrig.setVisible(true);
            }

        }

        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("General");
        
        
        
        IPlotter plotter_ecal_pos = af.createPlotterFactory().create();
        IPlotter plotter_ecal_e = af.createPlotterFactory().create();
        IPlotter plotter_ecal_hitmap = af.createPlotterFactory().create();
        IPlotter plotter_ecal_crhitmap = af.createPlotterFactory().create();
        IPlotter plotter_ecal_cramp = af.createPlotterFactory().create();
        IPlotter plotter_ecal_cls = af.createPlotterFactory().create();
        
        plotter_ecal_e.createRegions(2,3,0);
        plotter_ecal_e.setTitle("Ecal Cluster Energy");
        plotter_ecal_cramp.createRegions(2,3,0);
        plotter_ecal_cramp.setTitle("Ecal Crystal Hit Map");
        //plotter_ecal_crhitmap.style().statisticsBoxStyle().setVisible(false);
        plotter_ecal_crhitmap.createRegions(2,3,0);
        plotter_ecal_crhitmap.setTitle("Ecal Crystal Hit Map");
        plotter_ecal_crhitmap.style().statisticsBoxStyle().setVisible(false);
        plotter_ecal_hitmap.createRegions(2,3,0);
        plotter_ecal_hitmap.setTitle("Ecal Cluster Hit Map");
        plotter_ecal_hitmap.style().statisticsBoxStyle().setVisible(false);
        plotter_ecal_pos.createRegions(2,3,0);
        plotter_ecal_pos.setTitle("Ecal Cluster Position");
        plotter_ecal_pos.style().statisticsBoxStyle().setVisible(false);
        plotter_ecal_cls.createRegions(2,3,0);
        plotter_ecal_cls.setTitle("Ecal Cluster size");

        plotterFrame.addPlotter(plotter_ecal_e);
        plotterFrame.addPlotter(plotter_ecal_crhitmap);
        plotterFrame.addPlotter(plotter_ecal_cramp);
        plotterFrame.addPlotter(plotter_ecal_hitmap);
        plotterFrame.addPlotter(plotter_ecal_pos);
        plotterFrame.addPlotter(plotter_ecal_cls);
        
        

            IPlotter plotter_track_mom_pos = af.createPlotterFactory().create();
            plotter_track_mom_pos.createRegions(3,3,0);
            plotter_track_mom_pos.setTitle("Track Momentum vs Position");
            plotter_track_mom_pos.style().statisticsBoxStyle().setVisible(false);
            plotterFrame.addPlotter(plotter_track_mom_pos);
            IHistogram hPzVsX_t = aida.histogram2D("Top track Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsX_b = aida.histogram2D("Bottom track Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsX_a = aida.histogram2D("Track Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqp_t = aida.histogram2D("Top track q>0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqp_b = aida.histogram2D("Bottom track q>0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqp_a = aida.histogram2D("Track q>0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqm_t = aida.histogram2D("Top track q<0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqm_b = aida.histogram2D("Bottom track q<0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            IHistogram hPzVsXqm_a = aida.histogram2D("Track q<0 Pz vs X" , 25, -500,500, 50, 0, 3500);
            plotter_track_mom_pos.region(0).plot(hPzVsX_t);
            plotter_track_mom_pos.region(1).plot(hPzVsX_b);
            plotter_track_mom_pos.region(2).plot(hPzVsX_a);
            plotter_track_mom_pos.region(3).plot(hPzVsXqp_t);
            plotter_track_mom_pos.region(4).plot(hPzVsXqp_b);
            plotter_track_mom_pos.region(5).plot(hPzVsXqp_a);
            plotter_track_mom_pos.region(6).plot(hPzVsXqm_t);
            plotter_track_mom_pos.region(7).plot(hPzVsXqm_b);
            plotter_track_mom_pos.region(8).plot(hPzVsXqm_a);
            for(int i=0;i<9;++i) {
                plotter_track_mom_pos.region(i).style().setParameter("hist2DStyle", "colorMap");
                plotter_track_mom_pos.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_track_mom_pos.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_track_mom_pos.region(i)).getPlot().setAllowPopupMenus(true);

            }


            IPlotter plotter_track_mom = af.createPlotterFactory().create();
            plotter_track_mom.createRegions(3,3,0);
            plotter_track_mom.setTitle("Track Momentum");
            plotterFrame.addPlotter(plotter_track_mom);
            IHistogram hPz_t = aida.histogram1D("Top track Pz" , 50, 0, 3500);
            IHistogram hPz_b = aida.histogram1D("Bottom track Pz" , 50, 0, 3500);
            IHistogram hPz_a = aida.histogram1D("Track Pz" , 50, 0, 3500);
            IHistogram hmPz_t = aida.histogram1D("Matched top track Pz" , 50, 0, 3500);
            IHistogram hmPz_b = aida.histogram1D("Matched bottom track Pz" , 50, 0, 3500);
            IHistogram hmPz_a = aida.histogram1D("Matched track Pz" , 50, 0, 3500);
            IHistogram hmsPz_t = aida.histogram1D("Matched sel top track Pz" , 50, 0, 3500);
            IHistogram hmsPz_b = aida.histogram1D("Matched sel bottom track Pz" , 50, 0, 3500);
            IHistogram hmsPz_a = aida.histogram1D("Matched sel track Pz" , 50, 0, 3500);


            plotter_track_mom.region(0).plot(hPz_t);
            plotter_track_mom.region(1).plot(hPz_b);
            plotter_track_mom.region(2).plot(hPz_a);
            plotter_track_mom.region(3).plot(hmPz_t);
            plotter_track_mom.region(4).plot(hmPz_b);
            plotter_track_mom.region(5).plot(hmPz_a);
            plotter_track_mom.region(6).plot(hmsPz_t);
            plotter_track_mom.region(7).plot(hmsPz_b);
            plotter_track_mom.region(8).plot(hmsPz_a);

            IPlotter plotter_track_mom2 = af.createPlotterFactory().create();
            plotter_track_mom2.createRegions(3,3,0);
            plotter_track_mom2.setTitle("Track Momentum Target Outliers X");
            plotterFrame.addPlotter(plotter_track_mom2);
            IHistogram hPzTX_t = aida.histogram1D("Top track !target in X Pz" , 50, 0, 3500);
            IHistogram hPzTX_b = aida.histogram1D("Bottom track !target in X Pz" , 50, 0, 3500);
            IHistogram hPzTX_a = aida.histogram1D("Track !target in X Pz" , 50, 0, 3500);
            IHistogram hmPzTX_t = aida.histogram1D("Matched top track !target in X Pz" , 50, 0, 3500);
            IHistogram hmPzTX_b = aida.histogram1D("Matched bottom track !target in X Pz" , 50, 0, 3500);
            IHistogram hmPzTX_a = aida.histogram1D("Matched track !target in X Pz" , 50, 0, 3500);
            IHistogram hmsPzTX_t = aida.histogram1D("Matched sel top track !target in X Pz" , 50, 0, 3500);
            IHistogram hmsPzTX_b = aida.histogram1D("Matched sel bottom track !target in X Pz" , 50, 0, 3500);
            IHistogram hmsPzTX_a = aida.histogram1D("Matched sel track !target in X Pz" , 50, 0, 3500);


            plotter_track_mom2.region(0).plot(hPzTX_t);
            plotter_track_mom2.region(1).plot(hPzTX_b);
            plotter_track_mom2.region(2).plot(hPzTX_a);
            plotter_track_mom2.region(3).plot(hmPzTX_t);
            plotter_track_mom2.region(4).plot(hmPzTX_b);
            plotter_track_mom2.region(5).plot(hmPzTX_a);
            plotter_track_mom2.region(6).plot(hmsPzTX_t);
            plotter_track_mom2.region(7).plot(hmsPzTX_b);
            plotter_track_mom2.region(8).plot(hmsPzTX_a);


            IPlotter plotter_track_ext = af.createPlotterFactory().create();
            plotter_track_ext.createRegions(2,3,0);
            plotter_track_ext.setTitle("Track @ Target");
            plotterFrame.addPlotter(plotter_track_ext);
            IHistogram hTrkXAtConv_t = aida.histogram1D("Top track X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConv_t = aida.histogram1D("Top track Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConv_b = aida.histogram1D("Bottom track X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConv_b = aida.histogram1D("Bottom track Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConv_a = aida.histogram1D("Track X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConv_a = aida.histogram1D("Track Y @ -67cm" , 50, -20, 20);
            plotter_track_ext.region(0).plot(hTrkXAtConv_t);
            plotter_track_ext.region(1).plot(hTrkXAtConv_b);
            plotter_track_ext.region(2).plot(hTrkXAtConv_a);
            plotter_track_ext.region(3).plot(hTrkYAtConv_t);
            plotter_track_ext.region(4).plot(hTrkYAtConv_b);
            plotter_track_ext.region(5).plot(hTrkYAtConv_a);

            IHistogram hTrkHMXAtConv_t = aida.histogram1D("Top track Pz>1GeV X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConv_t = aida.histogram1D("Top track Pz>1GeV Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConv_b = aida.histogram1D("Bottom track Pz>1GeV X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConv_b = aida.histogram1D("Bottom track Pz>1GeV Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConv_a = aida.histogram1D("Track Pz>1GeV X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConv_a = aida.histogram1D("Track Pz>1GeV Y @ -67cm" , 50, -20, 20);
            plotter_track_ext.region(0).plot(hTrkHMXAtConv_t,"mode=overlay");
            plotter_track_ext.region(1).plot(hTrkHMXAtConv_b,"mode=overlay");
            plotter_track_ext.region(2).plot(hTrkHMXAtConv_a,"mode=overlay");
            plotter_track_ext.region(3).plot(hTrkHMYAtConv_t,"mode=overlay");
            plotter_track_ext.region(4).plot(hTrkHMYAtConv_b,"mode=overlay");
            plotter_track_ext.region(5).plot(hTrkHMYAtConv_a,"mode=overlay");

            IPlotter plotter_track_ext_entr = af.createPlotterFactory().create();
            plotter_track_ext_entr.createRegions(2,3,0);
            plotter_track_ext_entr.setTitle("Track @ 0cm"); 
            plotterFrame.addPlotter(plotter_track_ext_entr);
            IHistogram hTrkXAtEntr_t = aida.histogram1D("Top track X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntr_t = aida.histogram1D("Top track Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntr_b = aida.histogram1D("Bottom track X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntr_b = aida.histogram1D("Bottom track Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntr_a = aida.histogram1D("Track X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntr_a = aida.histogram1D("Track Y @ 0cm" , 50, -40, 40);
            plotter_track_ext_entr.region(0).plot(hTrkXAtEntr_t);
            plotter_track_ext_entr.region(1).plot(hTrkXAtEntr_b);
            plotter_track_ext_entr.region(2).plot(hTrkXAtEntr_a);
            plotter_track_ext_entr.region(3).plot(hTrkYAtEntr_t);
            plotter_track_ext_entr.region(4).plot(hTrkYAtEntr_b);
            plotter_track_ext_entr.region(5).plot(hTrkYAtEntr_a);


            IHistogram hTrkHMXAtEntr_t = aida.histogram1D("Top track Pz>1GeV X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntr_t = aida.histogram1D("Top track Pz>1GeV Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntr_b = aida.histogram1D("Bottom track Pz>1GeV X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntr_b = aida.histogram1D("Bottom track Pz>1GeV Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntr_a = aida.histogram1D("Track Pz>1GeV X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntr_a = aida.histogram1D("Track Pz>1GeV Y @ 0cm" , 50, -40, 40);
            plotter_track_ext_entr.region(0).plot(hTrkHMXAtEntr_t,"mode=overlay");
            plotter_track_ext_entr.region(1).plot(hTrkHMXAtEntr_b,"mode=overlay");
            plotter_track_ext_entr.region(2).plot(hTrkHMXAtEntr_a,"mode=overlay");
            plotter_track_ext_entr.region(3).plot(hTrkHMYAtEntr_t,"mode=overlay");
            plotter_track_ext_entr.region(4).plot(hTrkHMYAtEntr_b,"mode=overlay");
            plotter_track_ext_entr.region(5).plot(hTrkHMYAtEntr_a,"mode=overlay");


            IPlotter plotter_track_ext_coll = af.createPlotterFactory().create();
            plotter_track_ext_coll.createRegions(2,3,0);
            plotter_track_ext_coll.setTitle("Track @ -150cm"); 
            plotterFrame.addPlotter(plotter_track_ext_coll);
            IHistogram hTrkXAtColl_t = aida.histogram1D("Top track X @ -150cm" , 50, -100, 100);
            IHistogram hTrkYAtColl_t = aida.histogram1D("Top track Y @ -150cm" , 50, -40, 40);
            IHistogram hTrkXAtColl_b = aida.histogram1D("Bottom track X @ -150cm" , 50, -100, 100);
            IHistogram hTrkYAtColl_b = aida.histogram1D("Bottom track Y @ -150cm" , 50, -40, 40);
            IHistogram hTrkXAtColl_a = aida.histogram1D("Track X @ -150cm" , 50, -100, 100);
            IHistogram hTrkYAtColl_a = aida.histogram1D("Track Y @ -150cm" , 50, -40, 40);
            plotter_track_ext_coll.region(0).plot(hTrkXAtColl_t);
            plotter_track_ext_coll.region(1).plot(hTrkXAtColl_b);
            plotter_track_ext_coll.region(2).plot(hTrkXAtColl_a);
            plotter_track_ext_coll.region(3).plot(hTrkYAtColl_t);
            plotter_track_ext_coll.region(4).plot(hTrkYAtColl_b);
            plotter_track_ext_coll.region(5).plot(hTrkYAtColl_a);


            IHistogram hTrkHMXAtColl_t = aida.histogram1D("Top track Pz>1GeV X @ -150cm" , 50, -100, 100);
            IHistogram hTrkHMYAtColl_t = aida.histogram1D("Top track Pz>1GeV Y @ -150cm" , 50, -40, 40);
            IHistogram hTrkHMXAtColl_b = aida.histogram1D("Bottom track Pz>1GeV X @ -150cm" , 50, -100, 100);
            IHistogram hTrkHMYAtColl_b = aida.histogram1D("Bottom track Pz>1GeV Y @ -150cm" , 50, -40, 40);
            IHistogram hTrkHMXAtColl_a = aida.histogram1D("Track Pz>1GeV X @ -150cm" , 50, -100, 100);
            IHistogram hTrkHMYAtColl_a = aida.histogram1D("Track Pz>1GeV Y @ -150cm" , 50, -40, 40);
            plotter_track_ext_coll.region(0).plot(hTrkHMXAtColl_t,"mode=overlay");
            plotter_track_ext_coll.region(1).plot(hTrkHMXAtColl_b,"mode=overlay");
            plotter_track_ext_coll.region(2).plot(hTrkHMXAtColl_a,"mode=overlay");
            plotter_track_ext_coll.region(3).plot(hTrkHMYAtColl_t,"mode=overlay");
            plotter_track_ext_coll.region(4).plot(hTrkHMYAtColl_b,"mode=overlay");
            plotter_track_ext_coll.region(5).plot(hTrkHMYAtColl_a,"mode=overlay");





            IPlotter plotter_track_ext2 = af.createPlotterFactory().create();
            plotter_track_ext2.createRegions(2,3,0);
            plotter_track_ext2.setTitle("Matched track @ Target ");
            plotterFrame.addPlotter(plotter_track_ext2);
            IHistogram hmTrkXAtConv_t = aida.histogram1D("Top matched track X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConv_t = aida.histogram1D("Top matched track Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConv_b = aida.histogram1D("Bottom matched track X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConv_b = aida.histogram1D("Bottom matched track Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConv_a = aida.histogram1D("Matched track X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConv_a = aida.histogram1D("Matched track Y @ -67cm" , 50, -20, 20);
            plotter_track_ext2.region(0).plot(hmTrkXAtConv_t);
            plotter_track_ext2.region(1).plot(hmTrkXAtConv_b);
            plotter_track_ext2.region(2).plot(hmTrkXAtConv_a);
            plotter_track_ext2.region(3).plot(hmTrkYAtConv_t);
            plotter_track_ext2.region(4).plot(hmTrkYAtConv_b);
            plotter_track_ext2.region(5).plot(hmTrkYAtConv_a);






            IPlotter plotter_track_ext3 = af.createPlotterFactory().create();
            plotter_track_ext3.createRegions(2,3,0);
            plotter_track_ext3.setTitle("Matched sel track @ Target");
            plotterFrame.addPlotter(plotter_track_ext3);
            IHistogram hmsTrkXAtConv_t = aida.histogram1D("Top sel matched track X @ -67cm" , 50, -100, 100);
            IHistogram hmsTrkYAtConv_t = aida.histogram1D("Top sel matched track Y @ -67cm" , 50, -100, 100);
            IHistogram hmsTrkXAtConv_b = aida.histogram1D("Bottom sel matched track X @ -67cm" , 50, -100, 100);
            IHistogram hmsTrkYAtConv_b = aida.histogram1D("Bottom sel matched track Y @ -67cm" , 50, -100, 100);
            IHistogram hmsTrkXAtConv_a = aida.histogram1D("Matched sel track X @ -67cm" , 50, -100, 100);
            IHistogram hmsTrkYAtConv_a = aida.histogram1D("Matched sel track Y @ -67cm" , 50, -100, 100);
            plotter_track_ext3.region(0).plot(hmsTrkXAtConv_t);
            plotter_track_ext3.region(1).plot(hmsTrkXAtConv_b);
            plotter_track_ext3.region(2).plot(hmsTrkXAtConv_a);
            plotter_track_ext3.region(3).plot(hmsTrkYAtConv_t);
            plotter_track_ext3.region(4).plot(hmsTrkYAtConv_b);
            plotter_track_ext3.region(5).plot(hmsTrkYAtConv_a);




            IPlotter plotter_track_ext4 = af.createPlotterFactory().create();
            plotter_track_ext4.createRegions(2,3,0);
            plotter_track_ext4.setTitle("Track @ Target Pos. Charge");
            plotterFrame.addPlotter(plotter_track_ext4);
            IHistogram hTrkXAtConvqp_t = aida.histogram1D("Top track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqp_t = aida.histogram1D("Top track q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConvqp_b = aida.histogram1D("Bottom track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqp_b = aida.histogram1D("Bottom track q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConvqp_a = aida.histogram1D("Track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqp_a = aida.histogram1D("Track q>0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext4.region(0).plot(hTrkXAtConvqp_t);
            plotter_track_ext4.region(1).plot(hTrkXAtConvqp_b);
            plotter_track_ext4.region(2).plot(hTrkXAtConvqp_a);
            plotter_track_ext4.region(3).plot(hTrkYAtConvqp_t);
            plotter_track_ext4.region(4).plot(hTrkYAtConvqp_b);
            plotter_track_ext4.region(5).plot(hTrkYAtConvqp_a);


            IHistogram hTrkHMXAtConvqp_t = aida.histogram1D("Top track Pz>1GeV q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqp_t = aida.histogram1D("Top track Pz>1GeV q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConvqp_b = aida.histogram1D("Bottom track Pz>1GeV q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqp_b = aida.histogram1D("Bottom track Pz>1GeV q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConvqp_a = aida.histogram1D("Track Pz>1GeV q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqp_a = aida.histogram1D("Track Pz>1GeV q>0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext4.region(0).plot(hTrkHMXAtConvqp_t,"mode=overlay");
            plotter_track_ext4.region(1).plot(hTrkHMXAtConvqp_b,"mode=overlay");
            plotter_track_ext4.region(2).plot(hTrkHMXAtConvqp_a,"mode=overlay");
            plotter_track_ext4.region(3).plot(hTrkHMYAtConvqp_t,"mode=overlay");
            plotter_track_ext4.region(4).plot(hTrkHMYAtConvqp_b,"mode=overlay");
            plotter_track_ext4.region(5).plot(hTrkHMYAtConvqp_a,"mode=overlay");



            IPlotter plotter_track_ext4_entr = af.createPlotterFactory().create();
            plotter_track_ext4_entr.createRegions(2,3,0);
            plotter_track_ext4_entr.setTitle("Track @ 0cm Pos. Charge");
            plotterFrame.addPlotter(plotter_track_ext4_entr);
            IHistogram hTrkXAtEntrqp_t = aida.histogram1D("Top track q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqp_t = aida.histogram1D("Top track q>0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntrqp_b = aida.histogram1D("Bottom track q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqp_b = aida.histogram1D("Bottom track q>0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntrqp_a = aida.histogram1D("Track q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqp_a = aida.histogram1D("Track q>0 Y @ 0cm" , 50, -40, 40);
            plotter_track_ext4_entr.region(0).plot(hTrkXAtEntrqp_t);
            plotter_track_ext4_entr.region(1).plot(hTrkXAtEntrqp_b);
            plotter_track_ext4_entr.region(2).plot(hTrkXAtEntrqp_a);
            plotter_track_ext4_entr.region(3).plot(hTrkYAtEntrqp_t);
            plotter_track_ext4_entr.region(4).plot(hTrkYAtEntrqp_b);
            plotter_track_ext4_entr.region(5).plot(hTrkYAtEntrqp_a);


            IHistogram hTrkHMXAtEntrqp_t = aida.histogram1D("Top track Pz>1GeV q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqp_t = aida.histogram1D("Top track Pz>1GeV q>0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntrqp_b = aida.histogram1D("Bottom track Pz>1GeV q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqp_b = aida.histogram1D("Bottom track Pz>1GeV q>0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntrqp_a = aida.histogram1D("Track Pz>1GeV q>0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqp_a = aida.histogram1D("Track Pz>1GeV q>0 Y @ 0cm" , 50, -40, 40);
            plotter_track_ext4_entr.region(0).plot(hTrkHMXAtEntrqp_t,"mode=overlay");
            plotter_track_ext4_entr.region(1).plot(hTrkHMXAtEntrqp_b,"mode=overlay");
            plotter_track_ext4_entr.region(2).plot(hTrkHMXAtEntrqp_a,"mode=overlay");
            plotter_track_ext4_entr.region(3).plot(hTrkHMYAtEntrqp_t,"mode=overlay");
            plotter_track_ext4_entr.region(4).plot(hTrkHMYAtEntrqp_b,"mode=overlay");
            plotter_track_ext4_entr.region(5).plot(hTrkHMYAtEntrqp_a,"mode=overlay"); 



            IPlotter plotter_track_ext5 = af.createPlotterFactory().create();
            plotter_track_ext5.createRegions(2,3,0);
            plotter_track_ext5.setTitle("Track @ Target Neg. Charge");
            plotterFrame.addPlotter(plotter_track_ext5);
            IHistogram hTrkXAtConvqm_t = aida.histogram1D("Top track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqm_t = aida.histogram1D("Top track q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConvqm_b = aida.histogram1D("Bottom track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqm_b = aida.histogram1D("Bottom track q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkXAtConvqm_a = aida.histogram1D("Track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkYAtConvqm_a = aida.histogram1D("Track q<0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext5.region(0).plot(hTrkXAtConvqm_t);
            plotter_track_ext5.region(1).plot(hTrkXAtConvqm_b);
            plotter_track_ext5.region(2).plot(hTrkXAtConvqm_a);
            plotter_track_ext5.region(3).plot(hTrkYAtConvqm_t);
            plotter_track_ext5.region(4).plot(hTrkYAtConvqm_b);
            plotter_track_ext5.region(5).plot(hTrkYAtConvqm_a);

            IHistogram hTrkHMXAtConvqm_t = aida.histogram1D("Top track Pz>1GeV q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqm_t = aida.histogram1D("Top track Pz>1GeV q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConvqm_b = aida.histogram1D("Bottom track Pz>1GeV q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqm_b = aida.histogram1D("Bottom track Pz>1GeV q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hTrkHMXAtConvqm_a = aida.histogram1D("Track Pz>1GeV q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hTrkHMYAtConvqm_a = aida.histogram1D("Track Pz>1GeV q<0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext5.region(0).plot(hTrkHMXAtConvqm_t,"mode=overlay");
            plotter_track_ext5.region(1).plot(hTrkHMXAtConvqm_b,"mode=overlay");
            plotter_track_ext5.region(2).plot(hTrkHMXAtConvqm_a,"mode=overlay");
            plotter_track_ext5.region(3).plot(hTrkHMYAtConvqm_t,"mode=overlay");
            plotter_track_ext5.region(4).plot(hTrkHMYAtConvqm_b,"mode=overlay");
            plotter_track_ext5.region(5).plot(hTrkHMYAtConvqm_a,"mode=overlay");


            IPlotter plotter_track_ext5_entr = af.createPlotterFactory().create();
            plotter_track_ext5_entr.createRegions(2,3,0);
            plotter_track_ext5_entr.setTitle("Track @ 0cm Neg. Charge");
            plotterFrame.addPlotter(plotter_track_ext5_entr);
            IHistogram hTrkXAtEntrqm_t = aida.histogram1D("Top track q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqm_t = aida.histogram1D("Top track q<0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntrqm_b = aida.histogram1D("Bottom track q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqm_b = aida.histogram1D("Bottom track q<0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkXAtEntrqm_a = aida.histogram1D("Track q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkYAtEntrqm_a = aida.histogram1D("Track q<0 Y @ 0cm" , 50, -40, 40);
            plotter_track_ext5_entr.region(0).plot(hTrkXAtEntrqm_t);
            plotter_track_ext5_entr.region(1).plot(hTrkXAtEntrqm_b);
            plotter_track_ext5_entr.region(2).plot(hTrkXAtEntrqm_a);
            plotter_track_ext5_entr.region(3).plot(hTrkYAtEntrqm_t);
            plotter_track_ext5_entr.region(4).plot(hTrkYAtEntrqm_b);
            plotter_track_ext5_entr.region(5).plot(hTrkYAtEntrqm_a);




            IHistogram hTrkHMXAtEntrqm_t = aida.histogram1D("Top track Pz>1GeV q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqm_t = aida.histogram1D("Top track Pz>1GeV q<0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntrqm_b = aida.histogram1D("Bottom track Pz>1GeV q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqm_b = aida.histogram1D("Bottom track Pz>1GeV q<0 Y @ 0cm" , 50, -40, 40);
            IHistogram hTrkHMXAtEntrqm_a = aida.histogram1D("Track Pz>1GeV q<0 X @ 0cm" , 50, -100, 100);
            IHistogram hTrkHMYAtEntrqm_a = aida.histogram1D("Track Pz>1GeV q<0 Y @ 0cm" , 50, -40, 40);
            plotter_track_ext5_entr.region(0).plot(hTrkHMXAtEntrqm_t,"mode=overlay");
            plotter_track_ext5_entr.region(1).plot(hTrkHMXAtEntrqm_b,"mode=overlay");
            plotter_track_ext5_entr.region(2).plot(hTrkHMXAtEntrqm_a,"mode=overlay");
            plotter_track_ext5_entr.region(3).plot(hTrkHMYAtEntrqm_t,"mode=overlay");
            plotter_track_ext5_entr.region(4).plot(hTrkHMYAtEntrqm_b,"mode=overlay");
            plotter_track_ext5_entr.region(5).plot(hTrkHMYAtEntrqm_a,"mode=overlay");






            IPlotter plotter_track_ext6 = af.createPlotterFactory().create();
            plotter_track_ext6.createRegions(2,3,0);
            plotter_track_ext6.setTitle("Matched Track @ Target Pos. Charge");
            plotterFrame.addPlotter(plotter_track_ext6);
            IHistogram hmTrkXAtConvqp_t = aida.histogram1D("Top matched track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqp_t = aida.histogram1D("Top matched track q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConvqp_b = aida.histogram1D("Bottom matched track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqp_b = aida.histogram1D("Bottom matched track q>0 Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConvqp_a = aida.histogram1D("Matched track q>0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqp_a = aida.histogram1D("Matched track q>0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext6.region(0).plot(hmTrkXAtConvqp_t);
            plotter_track_ext6.region(1).plot(hmTrkXAtConvqp_b);
            plotter_track_ext6.region(2).plot(hmTrkXAtConvqp_a);
            plotter_track_ext6.region(3).plot(hmTrkYAtConvqp_t);
            plotter_track_ext6.region(4).plot(hmTrkYAtConvqp_b);
            plotter_track_ext6.region(5).plot(hmTrkYAtConvqp_a);

            IPlotter plotter_track_ext7 = af.createPlotterFactory().create();
            plotter_track_ext7.createRegions(2,3,0);
            plotter_track_ext7.setTitle("Matched Track @ Target Neg. Charge");
            plotterFrame.addPlotter(plotter_track_ext7);
            IHistogram hmTrkXAtConvqm_t = aida.histogram1D("Top matched track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqm_t = aida.histogram1D("Top matched track q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConvqm_b = aida.histogram1D("Bottom matched track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqm_b = aida.histogram1D("Bottom matched track q<0 Y @ -67cm" , 50, -20, 20);
            IHistogram hmTrkXAtConvqm_a = aida.histogram1D("Matched track q<0 X @ -67cm" , 50, -100, 100);
            IHistogram hmTrkYAtConvqm_a = aida.histogram1D("Matched track q<0 Y @ -67cm" , 50, -20, 20);
            plotter_track_ext7.region(0).plot(hmTrkXAtConvqm_t);
            plotter_track_ext7.region(1).plot(hmTrkXAtConvqm_b);
            plotter_track_ext7.region(2).plot(hmTrkXAtConvqm_a);
            plotter_track_ext7.region(3).plot(hmTrkYAtConvqm_t);
            plotter_track_ext7.region(4).plot(hmTrkYAtConvqm_b);
            plotter_track_ext7.region(5).plot(hmTrkYAtConvqm_a);





            IPlotter plotter_track_chi2 = af.createPlotterFactory().create();
            plotter_track_chi2.createRegions(3,3,0);
            plotter_track_chi2.setTitle("Track Chi2");
            plotterFrame.addPlotter(plotter_track_chi2);
            IHistogram hChi2_t = aida.histogram1D("Top track Chi2" , 50, 0, 15);
            IHistogram hChi2_b = aida.histogram1D("Bottom track Chi2" , 50, 0, 15);
            IHistogram hChi2_a = aida.histogram1D("Track Chi2" , 50, 0, 15);
            IHistogram hmChi2_t = aida.histogram1D("Matched top track Chi2" , 50, 0, 15);
            IHistogram hmChi2_b = aida.histogram1D("Matched bottom track Chi2" , 50, 0, 15);
            IHistogram hmChi2_a = aida.histogram1D("Matched track Chi2" , 50, 0, 15);
            IHistogram hmsChi2_t = aida.histogram1D("Matched sel top track Chi2" , 50, 0, 15);
            IHistogram hmsChi2_b = aida.histogram1D("Matched sel bottom track Chi2" , 50, 0, 15);
            IHistogram hmsChi2_a = aida.histogram1D("Matched sel track Chi2" , 50, 0, 15);

            plotter_track_chi2.region(0).plot(hChi2_t);
            plotter_track_chi2.region(1).plot(hChi2_b);
            plotter_track_chi2.region(2).plot(hChi2_a);    
            plotter_track_chi2.region(3).plot(hmChi2_t);
            plotter_track_chi2.region(4).plot(hmChi2_b);
            plotter_track_chi2.region(5).plot(hmChi2_a);    
            plotter_track_chi2.region(6).plot(hmsChi2_t);
            plotter_track_chi2.region(7).plot(hmsChi2_b);
            plotter_track_chi2.region(8).plot(hmsChi2_a);    



            IPlotter plotter_track_chi2_2 = af.createPlotterFactory().create();
            plotter_track_chi2_2.createRegions(3,3,0);
            plotter_track_chi2_2.setTitle("Track Chi2 Target Outliers X");
            plotterFrame.addPlotter(plotter_track_chi2_2);
            IHistogram hChi2TX_t = aida.histogram1D("Top track !target in X Chi2" , 50, 0, 15);
            IHistogram hChi2TX_b = aida.histogram1D("Bottom track !target in X Chi2" , 50, 0, 15);
            IHistogram hChi2TX_a = aida.histogram1D("Track !target in X Chi2" , 50, 0, 15);
            IHistogram hmChi2TX_t = aida.histogram1D("Matched top track !target in X Chi2" , 50, 0, 15);
            IHistogram hmChi2TX_b = aida.histogram1D("Matched bottom track !target in X Chi2" , 50, 0, 15);
            IHistogram hmChi2TX_a = aida.histogram1D("Matched track !target in X Chi2" , 50, 0, 15);
            IHistogram hmsChi2TX_t = aida.histogram1D("Matched sel top track !target in X Chi2" , 50, 0, 15);
            IHistogram hmsChi2TX_b = aida.histogram1D("Matched sel bottom track !target in X Chi2" , 50, 0, 15);
            IHistogram hmsChi2TX_a = aida.histogram1D("Matched sel track !target in X Chi2" , 50, 0, 15);

            plotter_track_chi2_2.region(0).plot(hChi2TX_t);
            plotter_track_chi2_2.region(1).plot(hChi2TX_b);
            plotter_track_chi2_2.region(2).plot(hChi2TX_a);    
            plotter_track_chi2_2.region(3).plot(hmChi2TX_t);
            plotter_track_chi2_2.region(4).plot(hmChi2TX_b);
            plotter_track_chi2_2.region(5).plot(hmChi2TX_a);    
            plotter_track_chi2_2.region(6).plot(hmsChi2TX_t);
            plotter_track_chi2_2.region(7).plot(hmsChi2TX_b);
            plotter_track_chi2_2.region(8).plot(hmsChi2TX_a);    



            
            
            IHistogram hmcls_t = aida.histogram1D("Cluster size matched top", 6, 0.0, 6.0);
            IHistogram hmcls_b = aida.histogram1D("Cluster size matched bottom", 6, 0.0, 6.0);
            IHistogram hmcls_a = aida.histogram1D("Cluster size matched", 6, 0.0, 6.0);
            IHistogram hmscls_t = aida.histogram1D("Cluster size matched sel top", 6, 0.0, 6.0);
            IHistogram hmscls_b = aida.histogram1D("Cluster size matched sel bottom", 6, 0.0, 6.0);
            IHistogram hmscls_a = aida.histogram1D("Cluster size matched sel", 6, 0.0, 6.0);
        
        
            
            
            

            IPlotter plotter_ecal_clsxm = af.createPlotterFactory().create();
            plotter_ecal_clsxm.createRegions(2,3,0);
            plotter_ecal_clsxm.setTitle("Ecal Cluster size matched");
            plotterFrame.addPlotter(plotter_ecal_clsxm);

            plotter_ecal_clsxm.region(0).plot(hmcls_t);
            plotter_ecal_clsxm.region(1).plot(hmcls_b);
            plotter_ecal_clsxm.region(2).plot(hmcls_a);
            plotter_ecal_clsxm.region(3).plot(hmscls_t);
            plotter_ecal_clsxm.region(4).plot(hmscls_b);
            plotter_ecal_clsxm.region(5).plot(hmscls_a);
            




            IHistogram hmHMcls_t = aida.histogram1D("Cluster size matched trk Pz>600MeV top", 6, 0.0, 6.0);
            IHistogram hmHMcls_b = aida.histogram1D("Cluster size matched trk Pz>600MeV bottom", 6, 0.0, 6.0);
            IHistogram hmHMcls_a = aida.histogram1D("Cluster size matched trk Pz>600MeV", 6, 0.0, 6.0);
            IHistogram hmHM2cls_t = aida.histogram1D("Cluster size matched trk Pz>1000MeV top", 6, 0.0, 6.0);
            IHistogram hmHM2cls_b = aida.histogram1D("Cluster size matched trk Pz>1000MeV bottom", 6, 0.0, 6.0);
            IHistogram hmHM2cls_a = aida.histogram1D("Cluster size matched trk Pz>1000MeV", 6, 0.0, 6.0);




            IPlotter plotter_ecal_clsxm2 = af.createPlotterFactory().create();
            plotter_ecal_clsxm2.createRegions(2,3,0);
            plotter_ecal_clsxm2.setTitle("Ecal Cluster size matched Trk Pz");
            plotterFrame.addPlotter(plotter_ecal_clsxm2);

            plotter_ecal_clsxm2.region(0).plot(hmHMcls_t);
            plotter_ecal_clsxm2.region(1).plot(hmHMcls_b);
            plotter_ecal_clsxm2.region(2).plot(hmHMcls_a);
            plotter_ecal_clsxm2.region(3).plot(hmHM2cls_t);
            plotter_ecal_clsxm2.region(4).plot(hmHM2cls_b);
            plotter_ecal_clsxm2.region(5).plot(hmHM2cls_a);





            IHistogram hnclmatch_a = aida.histogram1D("Nr of track matched clusters dR<20.0",3,-0.5,2.5);
            IHistogram hnclmatch_t = aida.histogram1D("Nr of track matched top clusters dR<20.0" ,3,-0.5,2.5);
            IHistogram hnclmatch_b = aida.histogram1D("Nr of track matched bottom clusters dR<20.0" ,3,-0.5,2.5);
            IHistogram hnsclmatch_a = aida.histogram1D("Nr of sel track matched clusters dR<20.0",3,-0.5,2.5);
            IHistogram hnsclmatch_t = aida.histogram1D("Nr of sel track matched top clusters dR<20.0" ,3,-0.5,2.5);
            IHistogram hnsclmatch_b = aida.histogram1D("Nr of sel track matched bottom clusters dR<20.0" ,3,-0.5,2.5);



            IPlotter plotter_nclmatch = af.createPlotterFactory().create();
            plotter_nclmatch.createRegions(2,3,0);
            plotter_nclmatch.setTitle("Ecal track match # dR<20.0");
            plotterFrame.addPlotter(plotter_nclmatch);
            plotter_nclmatch.region(0).plot(hnclmatch_t);
            plotter_nclmatch.region(1).plot(hnclmatch_b);
            plotter_nclmatch.region(2).plot(hnclmatch_a);
            plotter_nclmatch.region(3).plot(hnsclmatch_t);
            plotter_nclmatch.region(4).plot(hnsclmatch_b);
            plotter_nclmatch.region(5).plot(hnsclmatch_a);



            IHistogram hnclmatchY_a = aida.histogram1D("Nr of track matched clusters dY<20.0",3,-0.5,2.5);
            IHistogram hnclmatchY_t = aida.histogram1D("Nr of track matched top clusters dY<20.0" ,3,-0.5,2.5);
            IHistogram hnclmatchY_b = aida.histogram1D("Nr of track matched bottom clusters dY<20.0" ,3,-0.5,2.5);
            IHistogram hnsclmatchY_a = aida.histogram1D("Nr of sel track matched clusters dY<20.0",3,-0.5,2.5);
            IHistogram hnsclmatchY_t = aida.histogram1D("Nr of sel track matched top clusters dY<20.0" ,3,-0.5,2.5);
            IHistogram hnsclmatchY_b = aida.histogram1D("Nr of sel track matched bottom clusters dY<20.0" ,3,-0.5,2.5);

            IPlotter plotter_nclmatchY = af.createPlotterFactory().create();
            plotter_nclmatchY.createRegions(2,3,0);
            plotter_nclmatchY.setTitle("Ecal track match # dY<20.0");
            plotterFrame.addPlotter(plotter_nclmatchY);
            plotter_nclmatchY.region(0).plot(hnclmatchY_t);
            plotter_nclmatchY.region(1).plot(hnclmatchY_b);
            plotter_nclmatchY.region(2).plot(hnclmatchY_a);
            plotter_nclmatchY.region(3).plot(hnsclmatchY_t);
            plotter_nclmatchY.region(4).plot(hnsclmatchY_b);
            plotter_nclmatchY.region(5).plot(hnsclmatchY_a);



            IPlotter plotter_clmatchpos = af.createPlotterFactory().create();
            plotter_clmatchpos.createRegions(2,3,0);
            plotter_clmatchpos.setTitle("Cluster track match pos");
            plotter_clmatchpos.style().statisticsBoxStyle().setVisible(false);
            plotterFrame.addPlotter(plotter_clmatchpos);

            IHistogram hclmatchpos_a = aida.histogram2D("Track matched cluster pos",51,-25.5,25.5,11,-5.5,5.5);
            IHistogram hclmatchpos_t = aida.histogram2D("Track matched top cluster pos",51,-25.5,25.5,6,-0.5,5.5);
            IHistogram hclmatchpos_b = aida.histogram2D("Track matched bottom cluster pos",51,-25.5,25.5,6,-5.5,0.5);
            IHistogram hsclmatchpos_a = aida.histogram2D("Track matched sel cluster pos",51,-25.5,25.5,11,-5.5,5.5);
            IHistogram hsclmatchpos_t = aida.histogram2D("Track matched sel top cluster pos",51,-25.5,25.5,6,-0.5,5.5);
            IHistogram hsclmatchpos_b = aida.histogram2D("Track matched sel bottom cluster pos",51,-25.5,25.5,6,-5.5,0.5);



            plotter_clmatchpos.region(0).plot(hclmatchpos_t);
            plotter_clmatchpos.region(1).plot(hclmatchpos_b);
            plotter_clmatchpos.region(2).plot(hclmatchpos_a);
            plotter_clmatchpos.region(3).plot(hsclmatchpos_t);
            plotter_clmatchpos.region(4).plot(hsclmatchpos_b);
            plotter_clmatchpos.region(5).plot(hsclmatchpos_a);

            for(int i=0;i<6;++i) {
                plotter_clmatchpos.region(i).style().setParameter("hist2DStyle", "colorMap");
                plotter_clmatchpos.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_clmatchpos.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_clmatchpos.region(i)).getPlot().setAllowPopupMenus(true);
            }

            IPlotter plotter_clunmatchpos = af.createPlotterFactory().create();
            plotter_clunmatchpos.createRegions(2,3,0);
            plotter_clunmatchpos.setTitle("Cluster track unmatched pos");
            plotterFrame.addPlotter(plotter_clunmatchpos);

            IHistogram hsclunmatchpos_a = aida.histogram2D("Track unmatched sel cluster pos",50,-500,500,50,-100,100);
            IHistogram hsclunmatchpos_t = aida.histogram2D("Track unmatched sel top cluster pos",50,-500,500,50,-100,100);
            IHistogram hsclunmatchpos_b = aida.histogram2D("Track unmatched sel bottom cluster pos",50,-500,500,50,-100,100);
            plotter_clunmatchpos.region(0).plot(hsclunmatchpos_t);
            plotter_clunmatchpos.region(1).plot(hsclunmatchpos_b);
            plotter_clunmatchpos.region(2).plot(hsclunmatchpos_a);









        
        
        
        
        
        
        
        IPlotter plotter_cln = af.createPlotterFactory().create();
        plotter_cln.createRegions(2,3,0);
        plotter_cln.setTitle("Cluster multiplicity");
        plotterFrame.addPlotter(plotter_cln);
        
        IHistogram hsCln_a = aida.histogram1D("Cluster sel multiplicity",5,0,5);
        IHistogram hsCln_t = aida.histogram1D("Top cluster sel multiplicity",5,0,5);
        IHistogram hsCln_b = aida.histogram1D("Bottom cluster sel multiplicity",5,0,5);
        IHistogram hCln_a = aida.histogram1D("Cluster multiplicity",5,0,5);
        IHistogram hCln_t = aida.histogram1D("Top cluster multiplicity",5,0,5);
        IHistogram hCln_b = aida.histogram1D("Bottom cluster multiplicity",5,0,5);
        
        plotter_cln.region(2).plot(hCln_a);
        plotter_cln.region(0).plot(hCln_t);//,"mode=overlay");
        plotter_cln.region(1).plot(hCln_b);//,"mode=overlay");
        
        plotter_cln.region(5).plot(hsCln_a);
        plotter_cln.region(3).plot(hsCln_t);//,"mode=overlay");
        plotter_cln.region(4).plot(hsCln_b);//,"mode=overlay");
        
        
        
        
        
      
        
        
        
        
        IPlotter plotter_ecal_clsx = af.createPlotterFactory().create();
        plotter_ecal_clsx.createRegions(2,3,0);
        plotter_ecal_clsx.setTitle("Ecal Cluster size");
        plotterFrame.addPlotter(plotter_ecal_clsx);
        
        
         
        IHistogram hcls_t = aida.histogram1D("Cluster size top", 6, 0.0, 6.0);
        IHistogram hcls_b = aida.histogram1D("Cluster size bottom", 6, 0.0, 6.0);
        IHistogram hcls_a = aida.histogram1D("Cluster size", 6, 0.0, 6.0);
        IHistogram hscls_t = aida.histogram1D("Cluster size sel top", 6, 0.0, 6.0);
        IHistogram hscls_b = aida.histogram1D("Cluster size sel bottom", 6, 0.0, 6.0);
        IHistogram hscls_a = aida.histogram1D("Cluster size sel", 6, 0.0, 6.0);
        
        plotter_ecal_clsx.region(0).plot(hcls_t);
        plotter_ecal_clsx.region(1).plot(hcls_b);
        plotter_ecal_clsx.region(2).plot(hcls_a);
        plotter_ecal_clsx.region(3).plot(hcls_t);
        plotter_ecal_clsx.region(4).plot(hcls_b);
        plotter_ecal_clsx.region(5).plot(hcls_a);
        
        
        
        
        
        
        
      
        
        
        IHistogram hth = aida.histogram1D("Cluster theta", 100, 0.02, 0.07);
        IHistogram hth_t = aida.histogram1D("Top cluster theta", 100, 0.02, 0.07);
        IHistogram hth_b = aida.histogram1D("Bottom cluster theta", 100, 0.02, 0.07);
        IHistogram hth_ecl = aida.histogram1D("Cluster theta sel", 100, 0.02, 0.07);
        IHistogram hth_ecl_t = aida.histogram1D("Top cluster theta sel", 100, 0.02, 0.07);
        IHistogram hth_ecl_b = aida.histogram1D("Bottom cluster theta sel", 100, 0.02, 0.07);
        IHistogram hth_m = aida.histogram1D("Cluster theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_m_t = aida.histogram1D("Top cluster theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_m_b = aida.histogram1D("Bottom cluster theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_eclm = aida.histogram1D("Cluster sel theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_eclm_t = aida.histogram1D("Top cluster sel theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_eclm_b = aida.histogram1D("Bottom cluster sel theta matched trk", 100, 0.02, 0.07);
        IHistogram hth_mp = aida.histogram1D("Cluster theta matched trk Pz>1000MeV", 100, 0.02, 0.07);
        IHistogram hth_mp_t = aida.histogram1D("Top cluster theta matched trk Pz>1000MeV", 100, 0.02, 0.07);
        IHistogram hth_mp_b = aida.histogram1D("Bottom cluster theta matched trk Pz>1000MeV", 100, 0.02, 0.07);
        
        
        IPlotter plotter_ecal_cltheta = af.createPlotterFactory().create();
        plotter_ecal_cltheta.createRegions(5,3,0);
        plotter_ecal_cltheta.setTitle("Ecal cl theta");
        plotterFrame.addPlotter(plotter_ecal_cltheta);
        
        plotter_ecal_cltheta.region(0).plot(hth);
        plotter_ecal_cltheta.region(1).plot(hth_t);
        plotter_ecal_cltheta.region(2).plot(hth_b);
        plotter_ecal_cltheta.region(3).plot(hth_ecl);
        plotter_ecal_cltheta.region(4).plot(hth_ecl_t);
        plotter_ecal_cltheta.region(5).plot(hth_ecl_b);
        plotter_ecal_cltheta.region(6).plot(hth_m);
        plotter_ecal_cltheta.region(7).plot(hth_m_t);
        plotter_ecal_cltheta.region(8).plot(hth_m_b);
        plotter_ecal_cltheta.region(9).plot(hth_eclm);
        plotter_ecal_cltheta.region(10).plot(hth_eclm_t);
        plotter_ecal_cltheta.region(11).plot(hth_eclm_b);
        plotter_ecal_cltheta.region(12).plot(hth_mp);
        plotter_ecal_cltheta.region(13).plot(hth_mp_t);
        plotter_ecal_cltheta.region(14).plot(hth_mp_b);


        
        IHistogram hEoverPX_t = aida.histogram2D("EoverP vs cl X top",51,-25.5,25.5,50,0,2);
        IHistogram hEoverPX_b = aida.histogram2D("EoverP vs cl X bottom",51,-25.5,25.5,50,0,2);
        
        IHistogram hsEoverPX_t = aida.histogram2D("EoverP vs cl X sel top",51,-25.5,25.5,50,0,2);
        IHistogram hsEoverPX_b = aida.histogram2D("EoverP vs cl X sel bottom",51,-25.5,25.5,50,0,2);

        IHistogram htsEoverPX_t = aida.histogram2D("EoverP vs cl X Pz>1GeV top",51,-25.5,25.5,50,0,2);
        IHistogram htsEoverPX_b = aida.histogram2D("EoverP vs cl X Pz>1GeV bottom",51,-25.5,25.5,50,0,2);

        
        IPlotter plotter_ep_x = af.createPlotterFactory().create();
        plotter_ep_x.createRegions(3,2,0);
        plotter_ep_x.setTitle("E over P vs X ");
        plotter_ep_x.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_x);

        plotter_ep_x.region(0).plot(hEoverPX_t);
        plotter_ep_x.region(1).plot(hEoverPX_b);
        plotter_ep_x.region(2).plot(hsEoverPX_t);
        plotter_ep_x.region(3).plot(hsEoverPX_b);
        plotter_ep_x.region(4).plot(htsEoverPX_t);
        plotter_ep_x.region(5).plot(htsEoverPX_b);
        
        for(int i=0;i<6;++i) {
            plotter_ep_x.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter_ep_x.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }
        
        
        

        IHistogram hnEoverP_t = aida.histogram1D("EoverP cl X<0 Pz>0.6GeV top",50,0,2);
        IHistogram hnEoverP_b = aida.histogram1D("EoverP cl X<0 Pz>0.6GeV bottom",50,0,2);
        IHistogram hnEoverPX_t = aida.histogram2D("EoverP vs cl X Pz>0.6GeV top",51,-25.5,25.5,50,0,2);
        IHistogram hnEoverPX_b = aida.histogram2D("EoverP vs cl X Pz>0.6GeV bottom",51,-25.5,25.5,50,0,2);

        
        IPlotter plotter_ep_x2 = af.createPlotterFactory().create();
        plotter_ep_x2.createRegions(2,2,0);
        plotter_ep_x2.setTitle("E over P other");
        plotter_ep_x2.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_x2);

        plotter_ep_x2.region(0).plot(hnEoverP_t);
        plotter_ep_x2.region(1).plot(hnEoverP_b);
        plotter_ep_x2.region(2).plot(hnEoverPX_t);
        plotter_ep_x2.region(3).plot(hnEoverPX_b);
        
        for(int i=2;i<4;++i) {
            plotter_ep_x2.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter_ep_x2.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }
        
        
        
        IHistogram hnEoverP2_t = aida.histogram1D("EoverP cl X<0 Pz>0.6GeV Y>1 top",50,0,2);
        IHistogram hnEoverP2_b = aida.histogram1D("EoverP cl X<0 Pz>0.6GeV Y<-1 bottom",50,0,2);
        IHistogram hnEoverPX2_t = aida.histogram2D("EoverP cl X<0 vs cl X Pz>0.6GeV Y>1 top",26,-25.5,0.5,50,0,2);
        IHistogram hnEoverPX2_b = aida.histogram2D("EoverP cl X<0 vs cl X Pz>0.6GeV Y<-1 bottom",26,-25.5,0.5,50,0,2);

        
        IPlotter plotter_ep_xn = af.createPlotterFactory().create();
        plotter_ep_xn.createRegions(2,2,0);
        plotter_ep_xn.setTitle("E over P other 2");
        plotter_ep_xn.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_xn);

        plotter_ep_xn.region(0).plot(hnEoverP2_t);
        plotter_ep_xn.region(1).plot(hnEoverP2_b);
        plotter_ep_xn.region(2).plot(hnEoverPX2_t);
        plotter_ep_xn.region(3).plot(hnEoverPX2_b);
        
        for(int i=2;i<4;++i) {
            plotter_ep_xn.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter_ep_xn.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }

        
        
        
        
        IHistogram hnlEoverPX_t = aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 top",51,-25.5,25.5,50,0,2);
        IHistogram hnlEoverPX_b = aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 bottom",51,-25.5,25.5,50,0,2);
        IHistogram hnylEoverPX_t = aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 Y>1 top",51,-25.5,25.5,50,0,2);
        IHistogram hnylEoverPX_b = aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 Y<-1 bottom",51,-25.5,25.5,50,0,2);

        
        IPlotter plotter_ep_x3 = af.createPlotterFactory().create();
        plotter_ep_x3.createRegions(2,2,0);
        plotter_ep_x3.setTitle("E over P other Size>2");
        plotter_ep_x3.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_x3);

        plotter_ep_x3.region(0).plot(hnlEoverPX_t);
        plotter_ep_x3.region(1).plot(hnlEoverPX_b);
        plotter_ep_x3.region(2).plot(hnylEoverPX_t);
        plotter_ep_x3.region(3).plot(hnylEoverPX_b);
        
        for(int i=0;i<4;++i) {
            plotter_ep_x3.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter_ep_x3.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }

        
         
        IHistogram hnlEoverPX2_t = aida.histogram2D("EoverP vs cl X Pz>1GeV size>2 top",51,-25.5,25.5,50,0,2);
        IHistogram hnlEoverPX2_b = aida.histogram2D("EoverP vs cl X Pz>1GeV size>2 bottom",51,-25.5,25.5,50,0,2);
        
        
        IPlotter plotter_ep_x4 = af.createPlotterFactory().create();
        plotter_ep_x4.createRegions(1,2,0);
        plotter_ep_x4.setTitle("E over P other Pz>1GeV Size>2");
        plotter_ep_x4.style().statisticsBoxStyle().setVisible(false);
        plotterFrame.addPlotter(plotter_ep_x4);

        plotter_ep_x4.region(0).plot(hnlEoverPX2_t);
        plotter_ep_x4.region(1).plot(hnlEoverPX2_b);
        
        
        for(int i=0;i<2;++i) {
            plotter_ep_x4.region(i).style().setParameter("hist2DStyle", "colorMap");
            plotter_ep_x4.region(i).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }

        
        
        
        
        
        IHistogram hcramp_t = aida.histogram1D("Crystal amplitude top", 100, 0, 2000);
        IHistogram hcramp_b = aida.histogram1D("Crystal amplitude bottom", 100, 0, 2000);
        IHistogram hcramp_a = aida.histogram1D("Crystal amplitude all", 100, 0, 2000);

                

        IPlotterStyle style;

            
        plotter_ecal_cramp.createRegions(1,3,0);
        plotter_ecal_cramp.region(0).plot(hcramp_t);
        plotter_ecal_cramp.region(1).plot(hcramp_b);
        plotter_ecal_cramp.region(2).plot(hcramp_a);
        plotter_ecal_cramp.style().dataStyle().fillStyle().setColor("yellow");
        
           
                    
        
        for(int irow=-5;irow<=5;++irow) {
            for(int icol=-23;icol<=23;++icol) {
                clusterEnergy[icol+23][irow+5] = aida.histogram1D("Cluster energy x=" + icol + " y=" + irow, 50, 0,6000);  
            }
        }
        
                    
        
          IHistogram hcrhm_t = aida.histogram2D("Crystal hit map top", 47, -23.5, 23.5, 6, -0.5, 5.5);
             IHistogram hcrhm_b = aida.histogram2D("Crystal hit map bottom", 47, -23.5, 23.5, 6, -5.5, 0.5);
            IHistogram hcrhm_a = aida.histogram2D("Crystal hit map all", 47, -23.5, 23.5, 11, -5.5, 5.5);
            
            
            plotter_ecal_crhitmap.createRegions(1,3,0);
            plotter_ecal_crhitmap.region(0).plot(hcrhm_t);
            plotter_ecal_crhitmap.region(1).plot(hcrhm_b);
            plotter_ecal_crhitmap.region(2).plot(hcrhm_a);
            

           

            for(int i=0;i<3;++i) {
                style = plotter_ecal_crhitmap.region(i).style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_ecal_crhitmap.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_ecal_crhitmap.region(i)).getPlot().setAllowPopupMenus(true);
            }
           
       
            IHistogram hhm_t = aida.histogram2D("Cluster hit map top", 51, -25.5, 25.5, 6, -0.5, 5.5);
            IHistogram hhm_b = aida.histogram2D("Cluster hit map bottom", 51, -25.5, 25.5, 6, -5.5, 0.5);
            IHistogram hhm_a = aida.histogram2D("Cluster hit map all", 51, -25.5, 25.5, 11, -5.5, 5.5);
            IHistogram hshm_t = aida.histogram2D("Cluster hit map sel top", 51, -25.5, 25.5, 6, -0.5, 5.5);
            IHistogram hshm_b = aida.histogram2D("Cluster hit map sel bottom", 51, -25.5, 25.5, 6, -5.5, 0.5);
            IHistogram hshm_a = aida.histogram2D("Cluster hit map sel all", 51, -25.5, 25.5, 11, -5.5, 5.5);
     
            
            plotter_ecal_hitmap.region(0).plot(hhm_t);
            plotter_ecal_hitmap.region(1).plot(hhm_b);
            plotter_ecal_hitmap.region(2).plot(hhm_a);
            plotter_ecal_hitmap.region(3).plot(hshm_t);
            plotter_ecal_hitmap.region(4).plot(hshm_b);
            plotter_ecal_hitmap.region(5).plot(hshm_a);

            for(int i=0;i<6;++i) {
                style = plotter_ecal_hitmap.region(i).style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_ecal_hitmap.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_ecal_hitmap.region(i)).getPlot().setAllowPopupMenus(true);
            }
            
            
            IPlotter plotter_ecal_hitmap_corr = af.createPlotterFactory().create();
            plotter_ecal_hitmap_corr.createRegions(2,3,0);
            plotter_ecal_hitmap_corr.setTitle("Cluster hit map Corr");
            plotter_ecal_hitmap_corr.style().statisticsBoxStyle().setVisible(false);
            plotterFrame.addPlotter(plotter_ecal_hitmap_corr);
            
                   
            IHistogram hchm_gr = aida.histogram2D("Cluster hit map good region", 26, -25.5, 0.5, 6, -0.5, 5.5);
            IHistogram hchm2_gr = aida.histogram2D("Cluster E>0.6GeV hit map good region", 26, -25.5, 0.5, 6, -0.5, 5.5);
            IHistogram hchm3_gr = aida.histogram2D("Cluster E>1GeV hit map good region", 26, -25.5, 0.5, 6, -0.5, 5.5);
            IHistogram hchm2c_gr = aida.histogram2D("Cluster E_corr>0.6GeV hit map good region", 26, -25.5, 0.5, 6, -0.5, 5.5);
            IHistogram hchm3c_gr = aida.histogram2D("Cluster E_corr>1GeV hit map good region", 26, -25.5, 0.5, 6, -0.5, 5.5);
            
            plotter_ecal_hitmap_corr.region(0).plot(hchm_gr);
            plotter_ecal_hitmap_corr.region(1).plot(hchm2_gr);
            plotter_ecal_hitmap_corr.region(2).plot(hchm3_gr);
            plotter_ecal_hitmap_corr.region(4).plot(hchm2c_gr);
            plotter_ecal_hitmap_corr.region(5).plot(hchm3c_gr);
            
            for(int i=0;i<6;++i) {
                if(i==3) continue;
                style = plotter_ecal_hitmap_corr.region(i).style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_ecal_hitmap_corr.region(i)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter_ecal_hitmap_corr.region(i)).getPlot().setAllowPopupMenus(true);
            }
            
    
            
            

            
            IHistogram hhp_t = aida.histogram2D("Cluster hit pos top", 25, -400, 400, 25, 0, 100);
            IHistogram hhp_b = aida.histogram2D("Cluster hit pos bottom", 25, -400, 400, 25, -100, 0);
            IHistogram hhp_a = aida.histogram2D("Cluster hit pos all", 25, -400, 400, 25, -100, 100);
            IHistogram hshp_t = aida.histogram2D("Cluster hit pos sel top", 25, -400, 400, 25, 0, 100);
            IHistogram hshp_b = aida.histogram2D("Cluster hit pos sel bottom", 25, -400, 400, 25, -100, 0);
            IHistogram hshp_a = aida.histogram2D("Cluster hit pos sel all", 25, -400, 400, 25, -100, 100);

            
            plotter_ecal_pos.region(0).plot(hhp_t);
            plotter_ecal_pos.region(1).plot(hhp_b);
            plotter_ecal_pos.region(2).plot(hhp_a);
            plotter_ecal_pos.region(3).plot(hshp_t);
            plotter_ecal_pos.region(4).plot(hshp_b);
            plotter_ecal_pos.region(5).plot(hshp_a);

            

            for(int i=0;i<6;++i) {
            style = plotter_ecal_pos.region(i).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_pos.region(i)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_pos.region(i)).getPlot().setAllowPopupMenus(true);
            }
            
            
            IPlotter plotter_ecal_tmax = af.createPlotterFactory().create();
            plotter_ecal_tmax.createRegions(4,2,0);
            plotter_ecal_tmax.setTitle("Cluster hit pos tmax");
            plotter_ecal_tmax.style().statisticsBoxStyle().setVisible(false);
            plotterFrame.addPlotter(plotter_ecal_tmax);
            
            IHistogram hposTMaxX_t = aida.histogram1D("Cluster hit X-Xcenter top", 50, -25, 25);
            IHistogram hposTMaxY_t = aida.histogram1D("Cluster hit Y-Ycenter top", 50, -10, 10);
            IHistogram hposTMaxX_b = aida.histogram1D("Cluster hit X-Xcenter bottom", 50, -25, 25);
            IHistogram hposTMaxY_b = aida.histogram1D("Cluster hit Y-Ycenter bottom", 50, -10, 10);
            
            IHistogram hposTMaxXvsX_t = aida.histogram2D("Cluster hit X vs X-Xcenter top",25, -400,400, 50, -25, 25);
            IHistogram hposTMaxYvsY_t = aida.histogram2D("Cluster hit Y vs Y-Ycenter top",25, 0,100, 50, -10, 10);
            
            IHistogram hposTMaxXvsX_b = aida.histogram2D("Cluster hit X vs X-Xcenter bottom",25, -400,400, 50, -25, 25);
            IHistogram hposTMaxYvsY_b = aida.histogram2D("Cluster hit Y vs Y-Ycenter bottom",25, -100,00, 50, -10, 10);
            
            
            plotter_ecal_tmax.region(0).plot(hposTMaxX_t);
            plotter_ecal_tmax.region(1).plot(hposTMaxX_b);
            plotter_ecal_tmax.region(2).plot(hposTMaxXvsX_t);
            plotter_ecal_tmax.region(3).plot(hposTMaxXvsX_b);
            plotter_ecal_tmax.region(4).plot(hposTMaxY_t);
            plotter_ecal_tmax.region(5).plot(hposTMaxY_b);
            plotter_ecal_tmax.region(6).plot(hposTMaxYvsY_t);
            plotter_ecal_tmax.region(7).plot(hposTMaxYvsY_b);
            
            style = plotter_ecal_tmax.region(2).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter_ecal_tmax.region(3).setStyle(style);
            plotter_ecal_tmax.region(6).setStyle(style);
            plotter_ecal_tmax.region(7).setStyle(style);
            
        
        
              
        
        
        
        
            
        IPlotter plotter_clEoverP = af.createPlotterFactory().create();
        plotter_clEoverP.createRegions(3,3,0);
        plotter_clEoverP.setTitle("EoverP");
 
        plotterFrame.addPlotter(plotter_clEoverP);
        
        
        
        IPlotter plotter_cltrkmatch = af.createPlotterFactory().create();
        plotter_cltrkmatch.createRegions(2,3,0);
        plotter_cltrkmatch.setTitle("Ecal track match");
        plotterFrame.addPlotter(plotter_cltrkmatch);
        
        IHistogram hdx_t = aida.histogram1D("Cluster X - track X top" , 25, -1, 500);                        
        IHistogram hdy_t = aida.histogram1D("Cluster Y - track Y top" , 25, -1, 50);                        
        IHistogram hdx_b = aida.histogram1D("Cluster X - track X bottom" , 25, -1, 500);                        
        IHistogram hdy_b = aida.histogram1D("Cluster Y - track Y bottom" , 25, -1, 50);                        
        
        plotter_cltrkmatch.region(0).plot(hdx_t);
        plotter_cltrkmatch.region(1).plot(hdx_b);
        plotter_cltrkmatch.region(2).plot(hdy_t);
        plotter_cltrkmatch.region(3).plot(hdy_b);
        
        
        
        plotter_cltrkmatchE = af.createPlotterFactory().create();
        plotter_cltrkmatchE.createRegions(2,3,0);
        plotter_cltrkmatchE.setTitle("Ecal track match efficiency");
        plotterFrame.addPlotter(plotter_cltrkmatchE);
        
        
        plotter_cltrkmatchE.region(0).plot(hPz_t);
        plotter_cltrkmatchE.region(1).plot(hmPz_t);
        //plotter_cltrkmatchE.region(2).plot(hPz_meff_t);
        plotter_cltrkmatchE.region(3).plot(hPz_b);
        plotter_cltrkmatchE.region(4).plot(hmPz_b);
        //plotter_cltrkmatchE.region(5).plot(hPz_meff_b);
        
        
           

            
            
            
            
            
        
        String side;
        for (int iSide=0;iSide<3;++iSide) {
            side = sides[iSide];
            IHistogram h1 = aida.histogram1D("Cluster energy sel " + side , 100, 0, 2500);   
            
            IHistogram h11 = aida.histogram1D("Cluster energy " + side , 100, 0, 2500);                        

         

            
            
            
            
            IHistogram h4 = aida.histogram2D("selcl_ecal_cls_" + side, 100, -500.0, 500.0,8,0,8);
            
            
            IHistogram h44 = aida.histogram2D("allcl_ecal_cls_" + side, 50, -400.0, 400.0,8,0,8);
            
                                    
            
            IHistogram h1111 = aida.histogram1D("allcl_cltrkdr_" + side , 50, -1, 500);                        
            IHistogram h3333 = aida.histogram1D("allcl_cltrkdx_" + side , 50, -500, 500);                        
            IHistogram h4444 = aida.histogram1D("allcl_cltrkdy_" + side , 50, -50, 50);                        
            
            
            
        
            
            IHistogram hEvsP = aida.histogram2D("allcl_clEvsP_" + side ,25,0,2500,25,0,2500);                        
            IHistogram hEoverP = aida.histogram1D("allcl_clEoverP_" + side ,25,0,2);                        
            IHistogram hEoverPsel = aida.histogram1D("selcl_clEoverP_" + side ,25,0,2);

            plotter_ecal_e.region(iSide+3).plot(h1);
            plotter_ecal_cls.region(iSide).plot(h4);
            
            plotter_ecal_e.region(iSide).plot(h11);
            
            plotter_ecal_cls.region(iSide+3).plot(h44);
            
            
           

           
            plotter_clEoverP.region(iSide).plot(hEvsP);
            plotter_clEoverP.region(iSide+3).plot(hEoverP);
            plotter_clEoverP.region(iSide+6).plot(hEoverPsel);
            plotter_clEoverP.region(iSide).style().setParameter("hist2DStyle", "colorMap");
            plotter_clEoverP.region(iSide).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter_clEoverP.region(iSide+3).style().setParameter("hist2DStyle", "colorMap");
            plotter_clEoverP.region(iSide+3).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter_clEoverP.region(iSide+6).style().setParameter("hist2DStyle", "colorMap");
            plotter_clEoverP.region(iSide+6).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_clEoverP.region(iSide)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_clEoverP.region(iSide)).getPlot().setAllowPopupMenus(true);
            ((PlotterRegion) plotter_clEoverP.region(iSide+3)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_clEoverP.region(iSide+3)).getPlot().setAllowPopupMenus(true);
            ((PlotterRegion) plotter_clEoverP.region(iSide+6)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_clEoverP.region(iSide+6)).getPlot().setAllowPopupMenus(true);
            
            

            
            style = plotter_ecal_e.region(iSide).style();
            //style.setParameter("hist2DStyle", "colorMap");
            //style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_e.region(iSide)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_e.region(iSide)).getPlot().setAllowPopupMenus(true);

            
            style = plotter_ecal_cls.region(iSide).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_cls.region(iSide)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_cls.region(iSide)).getPlot().setAllowPopupMenus(true);

            
            style = plotter_ecal_e.region(iSide+3).style();
            //style.setParameter("hist2DStyle", "colorMap");
            //style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_e.region(iSide+3)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_e.region(iSide+3)).getPlot().setAllowPopupMenus(true);

           
           
            style = plotter_ecal_cls.region(iSide+3).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_cls.region(iSide+3)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_cls.region(iSide+3)).getPlot().setAllowPopupMenus(true);

            style = plotter_ecal_clsx.region(iSide+3).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            ((PlotterRegion) plotter_ecal_clsx.region(iSide+3)).getPlot().setAllowUserInteraction(true);
            ((PlotterRegion) plotter_ecal_clsx.region(iSide+3)).getPlot().setAllowPopupMenus(true);

            
          }
            
            
        if(!hide) {

            plotterFrame.pack();
            plotterFrame.setVisible(true);
        }
               
        
        
      
        
        
        
    }
    
    public MultScatAnalysis() {
    
        trkMatchTool = new EcalTrackMatch(debug);
    
    }
    

    

    public void setDebug(boolean flag) {
        this.debug = flag;
    }
    
    public void setOutputPlotFileName( String name ) {
        this.outputPlotFileName = name;
    }
    
    public void setEcalClusterSel(int id) {
        this.ecalClusterSel = id;
        System.out.println("ecalClusterSel set: "  + ecalClusterSel);
    }
    
    
    public void process(EventHeader event) {
        ++nevents;
        if( debug ) {
            System.out.println("Processing event " + nevents);    
        }

        
        if(doTriggerPart) {
        
            getTrigger(event);
    
        }
        
        //fastTracking(event);
        
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClusterCollectionName); 
        
        
        if(debug) System.out.println( clusters.size() + " ECal clusters in the event");
        
        //plotClusterDistr(clusters,"");
        
        if(doTriggerPart) {
        
            tagAndProbe(clusters);
        
        }
      

        List<Track> tracks;
        if(event.hasCollection(Track.class, trackCollectionName)) {
            tracks = event.get(Track.class, trackCollectionName);
        } else {
            tracks = new ArrayList<Track>();
        }

        if(debug) System.out.println( tracks.size() + " tracks in this event");

        
        
        if(1==0) {

            for(Track trk: tracks) {
                int side = 1;
                if (trk.getTrackerHits().get(0).getPosition()[2] > 0)
                side = 0;//make plot look pretty
                aida.histogram1D("Track Pz").fill(trk.getPX()*1000);
                if(side==0) aida.histogram1D("Top track Pz").fill(trk.getPX()*1000);
                else aida.histogram1D("Bottom track Pz").fill(trk.getPX()*1000); 
                aida.histogram1D("Track Chi2").fill(trk.getChi2());
                if(side==0) aida.histogram1D("Top track Chi2").fill(trk.getChi2());
                else aida.histogram1D("Bottom track Chi2").fill(trk.getChi2()); 

                Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trk);


                aida.histogram2D("Track Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                if(side==0) aida.histogram2D("Top track Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                else aida.histogram2D("Bottom track Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000); 

                if(trk.getCharge()>0) {
                    aida.histogram2D("Track q>0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                    if(side==0) aida.histogram2D("Top track q>0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                    else aida.histogram2D("Bottom track q>0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000); 
                } else {
                    aida.histogram2D("Track q<0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                    if(side==0) aida.histogram2D("Top track q<0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000);
                    else aida.histogram2D("Bottom track q<0 Pz vs X").fill(posAtEcal.y(),trk.getPX()*1000); 

                }


                Hep3Vector posAtTarget = TrackUtils.extrapolateTrack(trk,-670.);

                for(int imom=0;imom!=2;++imom) {

                    String str = imom ==0 ? "" : "Pz>1GeV ";
                    if(imom==1) {
                        if(trk.getPX()*1000<=1000) continue;
                    }

                    aida.histogram1D("Track " + str + "X @ -67cm").fill(posAtTarget.y());
                    aida.histogram1D("Track " + str + "Y @ -67cm").fill(posAtTarget.z());
                    Hep3Vector extPosEntr = TrackUtils.extrapolateTrack(trk,-0.0001);
                    aida.histogram1D("Track " + str + "X @ 0cm").fill(extPosEntr.y());
                    aida.histogram1D("Track " + str + "Y @ 0cm").fill(extPosEntr.z());
                    Hep3Vector extPosColl = TrackUtils.extrapolateTrack(trk,-1500.0);
                    aida.histogram1D("Track " + str + "X @ -150cm").fill(extPosColl.y());
                    aida.histogram1D("Track " + str + "Y @ -150cm").fill(extPosColl.z());
    //              
    //                double[] extTrkPoints = {-150.0,-67.0,ExtendTrack.DIPOLE_EDGE,ExtendTrack.ECAL_FACE};
    //                for(int iext=0;iext<5;++iext) {
    //                    Hep3Vector extPosEntr = ext.extrapolateTrack(extTrkPoints[iext]);
    //                    aida.cloud2D("Track ext " + str + "X @ " + ((int)extTrkPoints[iext])).fill(extPosEntr.x(),extPosEntr.y());
    //                }
    //               
                    if(trk.getCharge()>0) {
                        aida.histogram1D("Track " + str + "q>0 X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Track " + str + "q>0 Y @ -67cm").fill(posAtTarget.z());
                        aida.histogram1D("Track " + str + "q>0 X @ 0cm").fill(extPosEntr.y());
                        aida.histogram1D("Track " + str + "q>0 Y @ 0cm").fill(extPosEntr.z());
                    } else {
                        aida.histogram1D("Track " + str + "q<0 X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Track " + str + "q<0 Y @ -67cm").fill(posAtTarget.z());
                        aida.histogram1D("Track " + str + "q<0 X @ 0cm").fill(extPosEntr.y());
                        aida.histogram1D("Track " + str + "q<0 Y @ 0cm").fill(extPosEntr.z());
                    }

                    if(side==0) {
                        aida.histogram1D("Top track " + str + "X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Top track " + str + "Y @ -67cm").fill(posAtTarget.z());
                        aida.histogram1D("Top track " + str + "X @ 0cm").fill(extPosEntr.y());
                        aida.histogram1D("Top track " + str + "Y @ 0cm").fill(extPosEntr.z());
                        aida.histogram1D("Top track " + str + "X @ -150cm").fill(extPosColl.y());
                        aida.histogram1D("Top track " + str + "Y @ -150cm").fill(extPosColl.z());
                        if(trk.getCharge()>0) {
                            aida.histogram1D("Top track " + str + "q>0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Top track " + str + "q>0 Y @ -67cm").fill(posAtTarget.z());
                            aida.histogram1D("Top track " + str + "q>0 X @ 0cm").fill(extPosEntr.y());
                            aida.histogram1D("Top track " + str + "q>0 Y @ 0cm").fill(extPosEntr.z());
                        } else {
                            aida.histogram1D("Top track " + str + "q<0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Top track " + str + "q<0 Y @ -67cm").fill(posAtTarget.z());
                            aida.histogram1D("Top track " + str + "q<0 X @ 0cm").fill(extPosEntr.y());
                            aida.histogram1D("Top track " + str + "q<0 Y @ 0cm").fill(extPosEntr.z());

                        }
                    } else {
                        aida.histogram1D("Bottom track " + str + "X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Bottom track " + str + "Y @ -67cm").fill(posAtTarget.z());
                        aida.histogram1D("Bottom track " + str + "X @ 0cm").fill(extPosEntr.y());
                        aida.histogram1D("Bottom track " + str + "Y @ 0cm").fill(extPosEntr.z());
                        aida.histogram1D("Bottom track " + str + "X @ -150cm").fill(extPosColl.y());
                        aida.histogram1D("Bottom track " + str + "Y @ -150cm").fill(extPosColl.z());
                        if(trk.getCharge()>0) {
                            aida.histogram1D("Bottom track " + str + "q>0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Bottom track " + str + "q>0 Y @ -67cm").fill(posAtTarget.z());
                            aida.histogram1D("Bottom track " + str + "q>0 X @ 0cm").fill(extPosEntr.y());
                            aida.histogram1D("Bottom track " + str + "q>0 Y @ 0cm").fill(extPosEntr.z());
                        } else {
                            aida.histogram1D("Bottom track " + str + "q<0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Bottom track " + str + "q<0 Y @ -67cm").fill(posAtTarget.z());
                            aida.histogram1D("Bottom track " + str + "q<0 X @ 0cm").fill(extPosEntr.y());
                            aida.histogram1D("Bottom track " + str + "q<0 Y @ 0cm").fill(extPosEntr.z());

                        }
                    }
                }

                if(posAtTarget.y()>25) {
                    aida.histogram1D("Track !target in X Pz").fill(trk.getPX()*1000);
                    if(side==0) aida.histogram1D("Top track !target in X Pz").fill(trk.getPX()*1000);
                    else aida.histogram1D("Bottom track !target in X Pz").fill(trk.getPX()*1000); 
                    aida.histogram1D("Track !target in X Chi2").fill(trk.getChi2());
                    if(side==0) aida.histogram1D("Top track !target in X Chi2").fill(trk.getChi2());
                    else aida.histogram1D("Bottom track !target in X Chi2").fill(trk.getChi2());     
                }



            }
        }
        
        
        
        
        
        
        if (event.hasCollection(CalorimeterHit.class, inputEcalHitCollection)) {
                List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputEcalHitCollection);
                for (CalorimeterHit hit : hits) {
                    int x = hit.getIdentifierFieldValue("ix");
                    int y = hit.getIdentifierFieldValue("iy");

                    if(y>0) aida.histogram2D("Crystal hit map top").fill(x,y);
                    else aida.histogram2D("Crystal hit map bottom").fill(x,y);
                    aida.histogram2D("Crystal hit map all").fill(x,y);
                    
                    aida.histogram1D("Crystal amplitude all").fill(hit.getRawEnergy());
                    if(y>0) aida.histogram1D("Crystal amplitude top").fill(hit.getRawEnergy());
                    else aida.histogram1D("Crystal amplitude bottom").fill(hit.getRawEnergy());
                    
                    //crystalAmp[x+23][y+5].fill(hit.getRawEnergy());
                    
                }
        }
        
        
        
        
        
        int[] cl_count = {0,0,0}; 
        int[] cl_sel_count = {0,0,0}; 
        
        
        for(HPSEcalCluster cl : clusters) {
            
            
            int side;
            if(cl.getPosition()[1] > 0 ) side = 0; //top
            else side = 1;

            cl_count[2] = cl_count[2] + 1;
            cl_count[side] = cl_count[side] + 1;
            
            int[] crystalPair = getCrystalPair(cl);
            double theta = Math.atan(cl.getPosition()[1]/cl.getPosition()[2]);
            double py = cl.getEnergy()*Math.sin(theta);
            
            BaseCluster bcl = (BaseCluster)cl;
            double[] posAtCenter = bcl.getPosition();
            
            //double px = 
            //HepLorentzVector clv = new BasicHepLorentzVector(cl.getEnergy());
            //clv.
            //        )

            aida.histogram1D("Cluster energy " + sides[side]).fill(cl.getEnergy()); 
            aida.histogram1D("Cluster energy all").fill(cl.getEnergy()); 
            
            
            clusterEnergy[crystalPair[0]+23][crystalPair[1]+5].fill(cl.getEnergy());
        
            
            
            aida.histogram2D("Cluster hit map " + sides[side]).fill(crystalPair[0], crystalPair[1]); 
            aida.histogram2D("Cluster hit map all").fill(crystalPair[0], crystalPair[1]); 

            aida.histogram2D("Cluster hit pos " + sides[side]).fill(cl.getPosition()[0],cl.getPosition()[1]);
            aida.histogram2D("Cluster hit pos all").fill(cl.getPosition()[0],cl.getPosition()[1]);

            aida.histogram2D("allcl_ecal_cls_" + sides[side]).fill(cl.getPosition()[0],cl.getSize());
            aida.histogram2D("allcl_ecal_cls_" + "all").fill(cl.getPosition()[0],cl.getSize());

            aida.histogram1D("Cluster size " + sides[side]).fill(cl.getSize());
            aida.histogram1D("Cluster size").fill(cl.getSize());

            aida.histogram1D("Cluster hit X-Xcenter " + sides[side]).fill(cl.getPosition()[0]-posAtCenter[0]);
            aida.histogram1D("Cluster hit Y-Ycenter " + sides[side]).fill(cl.getPosition()[1]-posAtCenter[1]);

            aida.histogram2D("Cluster hit X vs X-Xcenter " + sides[side]).fill(cl.getPosition()[0],cl.getPosition()[0]-posAtCenter[0]);
            aida.histogram2D("Cluster hit Y vs Y-Ycenter " + sides[side]).fill(cl.getPosition()[1],cl.getPosition()[1]-posAtCenter[1]);

            
              
            double cl_theta = getSimpleClusterTheta(cl,TARGETZ,0.0);
            
            aida.histogram1D("Cluster theta").fill(cl_theta);
            if(cl.getPosition()[1]>0) aida.histogram1D("Top cluster theta").fill(cl_theta);
            else aida.histogram1D("Bottom cluster theta").fill(-1*cl_theta);

            boolean clusterGoodRegion = false;
            if(crystalPair[0]<0 && crystalPair[1]>1) clusterGoodRegion = true;
            
            // Get corrected E/p energy
            double C_ep = 1.0/0.3;
            
            double clEnergyCorr = cl.getEnergy()*C_ep;
            
            if(clusterGoodRegion) {
                aida.histogram2D("Cluster hit map good region").fill(crystalPair[0], crystalPair[1]);
                if(cl.getEnergy()>600) {
                    aida.histogram2D("Cluster E>0.6GeV hit map good region").fill(crystalPair[0], crystalPair[1]);
                }
                if(cl.getEnergy()>1000) {
                    aida.histogram2D("Cluster E>1GeV hit map good region").fill(crystalPair[0], crystalPair[1]);
                }
                
                if(clEnergyCorr>600) {
                    aida.histogram2D("Cluster E_corr>0.6GeV hit map good region").fill(crystalPair[0], crystalPair[1]);
                }
                if(clEnergyCorr>1000) {
                    aida.histogram2D("Cluster E_corr>1GeV hit map good region").fill(crystalPair[0], crystalPair[1]);
                }
            }
              
            
            
            int cl_cuts = isBadCluster(cl);
            
            
            
            if(cl_cuts == 0) {

                cl_sel_count[2] = cl_sel_count[2] + 1;
                cl_sel_count[side] = cl_sel_count[side] + 1;
                
                aida.histogram1D("Cluster energy sel " + sides[side]).fill(cl.getEnergy()); 
                aida.histogram1D("Cluster energy sel all").fill(cl.getEnergy()); 

                aida.histogram2D("Cluster hit map sel " + sides[side]).fill(crystalPair[0], crystalPair[1]); 
                aida.histogram2D("Cluster hit map sel all").fill(crystalPair[0], crystalPair[1]); 

                aida.histogram2D("Cluster hit pos sel " + sides[side]).fill(cl.getPosition()[0],cl.getPosition()[1]);
                aida.histogram2D("Cluster hit pos sel all").fill(cl.getPosition()[0],cl.getPosition()[1]);
                
                aida.histogram2D("selcl_ecal_cls_" + sides[side]).fill(cl.getPosition()[0],cl.getSize());
                aida.histogram2D("selcl_ecal_cls_" + "all").fill(cl.getPosition()[0],cl.getSize());

                aida.histogram1D("Cluster size sel " + sides[side]).fill(cl.getSize());
                aida.histogram1D("Cluster size sel").fill(cl.getSize());

           
                aida.histogram1D("Cluster theta sel").fill(cl_theta);
                if(cl.getPosition()[1]>0) aida.histogram1D("Top cluster theta sel").fill(cl_theta);
                else aida.histogram1D("Bottom cluster theta sel").fill(-1*cl_theta);
            }

                trkMatchTool.setCluster(cl);
                trkMatchTool.match(tracks);

                
                



                //IHistogram2D heff = hf.divide(aida.histogram2D("allcl_clEtrkdr_all").title(), aida.histogram2D("allcl_clEtrkdr_all"), aida.histogram2D("allcl_clEtrkdr_all"))

               

                if(trkMatchTool.isMatched(9999.9)) { //just make sure something is in the tracker for these plots
                    

                    if(cl.getPosition()[1]>0) {
                        aida.histogram1D("Cluster X - track X top").fill(trkMatchTool.getDistanceToTrackInX());
                        aida.histogram1D("Cluster Y - track Y top").fill(trkMatchTool.getDistanceToTrackInY());
                    }
                    else {
                        aida.histogram1D("Cluster X - track X bottom").fill(trkMatchTool.getDistanceToTrackInX());
                        aida.histogram1D("Cluster Y - track Y bottom").fill(trkMatchTool.getDistanceToTrackInY());              
                    }

                   
                }

                
                


                if(trkMatchTool.isMatched(20.0)) { 

                    aida.histogram1D("Nr of track matched clusters dR<20.0").fill(1);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Nr of track matched top clusters dR<20.0").fill(1);
                    else aida.histogram1D("Nr of track matched bottom clusters dR<20.0").fill(1);

                    if(cl_cuts==0) {
                        aida.histogram1D("Nr of sel track matched clusters dR<20.0").fill(1);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Nr of sel track matched top clusters dR<20.0").fill(1);
                        else aida.histogram1D("Nr of sel track matched bottom clusters dR<20.0").fill(1);
                    }

                } else {

                    aida.histogram1D("Nr of track matched clusters dR<20.0").fill(0);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Nr of track matched top clusters dR<20.0").fill(0);
                    else aida.histogram1D("Nr of track matched bottom clusters dR<20.0").fill(0);

                    if(cl_cuts==0) {
                        aida.histogram1D("Nr of sel track matched clusters dR<20.0").fill(0);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Nr of sel track matched top clusters dR<20.0").fill(0);
                        else aida.histogram1D("Nr of sel track matched bottom clusters dR<20.0").fill(0);
                    }
                }







                if(trkMatchTool.isMatchedY(20.0)) { 

                    aida.histogram1D("Nr of track matched clusters dY<20.0").fill(1);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Nr of track matched top clusters dY<20.0").fill(1);
                    else aida.histogram1D("Nr of track matched bottom clusters dY<20.0").fill(1);

                    if(cl_cuts==0) {
                        aida.histogram1D("Nr of sel track matched clusters dY<20.0").fill(1);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Nr of sel track matched top clusters dY<20.0").fill(1);
                        else aida.histogram1D("Nr of sel track matched bottom clusters dY<20.0").fill(1);
                    }

                } else {

                    aida.histogram1D("Nr of track matched clusters dY<20.0").fill(0);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Nr of track matched top clusters dY<20.0").fill(0);
                    else aida.histogram1D("Nr of track matched bottom clusters dY<20.0").fill(0);

                    if(cl_cuts==0) {
                        aida.histogram1D("Nr of sel track matched clusters dY<20.0").fill(0);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Nr of sel track matched top clusters dY<20.0").fill(0);
                        else aida.histogram1D("Nr of sel track matched bottom clusters dY<20.0").fill(0);
                    }
                }





                if(trkMatchTool.isMatchedY(20.0)) { 

                    aida.histogram1D("Cluster size matched " + sides[side]).fill(cl.getSize());
                    aida.histogram1D("Cluster size matched").fill(cl.getSize());



                    aida.histogram1D("Cluster theta matched trk").fill(cl_theta);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Top cluster theta matched trk").fill(cl_theta);
                    else aida.histogram1D("Bottom cluster theta matched trk").fill(-1*cl_theta);


                    if(cl_cuts == 0 ) {
                        aida.histogram1D("Cluster sel theta matched trk").fill(cl_theta);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Top cluster sel theta matched trk").fill(cl_theta);
                        else aida.histogram1D("Bottom cluster sel theta matched trk").fill(-1*cl_theta);
                    }

                    if (trkMatchTool.getMatchedTrack().getPX()*1000>1000) {
                        aida.histogram1D("Cluster theta matched trk Pz>1000MeV").fill(cl_theta);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Top cluster theta matched trk Pz>1000MeV").fill(cl_theta);
                        else aida.histogram1D("Bottom cluster theta matched trk Pz>1000MeV").fill(-1*cl_theta);

                        aida.histogram1D("Cluster size matched trk Pz>1000MeV " + sides[side]).fill(cl.getSize());
                        aida.histogram1D("Cluster size matched trk Pz>1000MeV").fill(cl.getSize());

                    }




                    aida.histogram2D("allcl_clEvsP_all").fill(cl.getEnergy(),trkMatchTool.getMatchedTrack().getPX()*1000);
                    if(cl.getPosition()[1]>0) aida.histogram2D("allcl_clEvsP_top").fill(cl.getEnergy(),trkMatchTool.getMatchedTrack().getPX()*1000);
                    else aida.histogram2D("allcl_clEvsP_bottom").fill(cl.getEnergy(),trkMatchTool.getMatchedTrack().getPX()*1000);

                    aida.histogram1D("allcl_clEoverP_all").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                    if(cl.getPosition()[1]>0) aida.histogram1D("allcl_clEoverP_top").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                    else aida.histogram1D("allcl_clEoverP_bottom").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                    if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                    else aida.histogram2D("EoverP vs cl X bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                    if(trkMatchTool.getMatchedTrack().getPX()*1000 > 1000) {


                        if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X Pz>1GeV top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                        else aida.histogram2D("EoverP vs cl X Pz>1GeV bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));


                        if(cl.getSize()>2) {
                            if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X Pz>1GeV size>2 top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            else aida.histogram2D("EoverP vs cl X Pz>1GeV size>2 bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));                        


                        }
                    }


                    if(trkMatchTool.getMatchedTrack().getPX()*1000 > 600) {

                        aida.histogram1D("Cluster size matched trk Pz>600MeV " + sides[side]).fill(cl.getSize());
                        aida.histogram1D("Cluster size matched trk Pz>600MeV").fill(cl.getSize());


                        if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X Pz>0.6GeV top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                        else aida.histogram2D("EoverP vs cl X Pz>0.6GeV bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                        if(crystalPair[0]<0) {
                            if(cl.getPosition()[1]>0) aida.histogram1D("EoverP cl X<0 Pz>0.6GeV top").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            else aida.histogram1D("EoverP cl X<0 Pz>0.6GeV bottom").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                            if(crystalPair[1]>1) {
                                aida.histogram1D("EoverP cl X<0 Pz>0.6GeV Y>1 top").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                                aida.histogram2D("EoverP cl X<0 vs cl X Pz>0.6GeV Y>1 top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            }else if(crystalPair[1]<-1) {
                                aida.histogram1D("EoverP cl X<0 Pz>0.6GeV Y<-1 bottom").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                                aida.histogram2D("EoverP cl X<0 vs cl X Pz>0.6GeV Y<-1 bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            }
                        }

                        if(cl.getSize()>2) {
                            if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            else aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                            if(crystalPair[1]>1) {
                                aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 Y>1 top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            } else if(crystalPair[1]<-1) {
                                aida.histogram2D("EoverP vs cl X Pz>0.6GeV size>2 Y<-1 bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                            }


                        }


                    }



                    aida.histogram1D("Matched track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                    if(cl.getPosition()[1]>0) aida.histogram1D("Matched top track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                    else aida.histogram1D("Matched bottom track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000); 

                    aida.histogram1D("Matched track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                    if(cl.getPosition()[1]>0) aida.histogram1D("Matched top track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                    else aida.histogram1D("Matched bottom track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2()); 


                    
                    if((nevents % refreshRate) == 0) {
                        IHistogram1D heff = hf.divide(aida.histogram1D("Matched top track Pz").title(), aida.histogram1D("Matched top track Pz"), aida.histogram1D("Top track Pz"));
                        plotter_cltrkmatchE.region(2).clear();
                        plotter_cltrkmatchE.region(2).plot(heff);
                        IHistogram1D heff2 = hf.divide(aida.histogram1D("Matched bottom track Pz").title(), aida.histogram1D("Matched bottom track Pz"), aida.histogram1D("Bottom track Pz"));
                        plotter_cltrkmatchE.region(5).clear();
                        plotter_cltrkmatchE.region(5).plot(heff2);
        
                    }
                    


                    Hep3Vector posAtTarget = TrackUtils.extrapolateTrack(trkMatchTool.getMatchedTrack(),-670);    



                    aida.histogram1D("Matched track X @ -67cm").fill(posAtTarget.y());
                    aida.histogram1D("Matched track Y @ -67cm").fill(posAtTarget.z());
                    if(trkMatchTool.getMatchedTrack().getCharge()>0) {
                        aida.histogram1D("Matched track q>0 X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Matched track q>0 Y @ -67cm").fill(posAtTarget.z());
                    } else {
                        aida.histogram1D("Matched track q<0 X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Matched track q<0 Y @ -67cm").fill(posAtTarget.z());
                    }

                    if(cl.getPosition()[1]>0) {
                        aida.histogram1D("Top matched track X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Top matched track Y @ -67cm").fill(posAtTarget.z());
                        if(trkMatchTool.getMatchedTrack().getCharge()>0) {
                            aida.histogram1D("Top matched track q>0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Top matched track q>0 Y @ -67cm").fill(posAtTarget.z());
                        } else {
                            aida.histogram1D("Top matched track q<0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Top matched track q<0 Y @ -67cm").fill(posAtTarget.z());

                        }
                    } else {
                        aida.histogram1D("Bottom matched track X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Bottom matched track Y @ -67cm").fill(posAtTarget.z());
                        if(trkMatchTool.getMatchedTrack().getCharge()>0) {
                            aida.histogram1D("Bottom matched track q>0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Bottom matched track q>0 Y @ -67cm").fill(posAtTarget.z());
                        } else {
                            aida.histogram1D("Bottom matched track q<0 X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Bottom matched track q<0 Y @ -67cm").fill(posAtTarget.z());

                        }
                    }


                    if(posAtTarget.y()>25) {
                        aida.histogram1D("Matched track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Matched top track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                        else aida.histogram1D("Matched bottom track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000); 

                        aida.histogram1D("Matched track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                        if(cl.getPosition()[1]>0) aida.histogram1D("Matched top track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                        else aida.histogram1D("Matched bottom track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2()); 
                    }




                    int [] pos = getCrystalPair(cl);
                    aida.histogram2D("Track matched cluster pos").fill(pos[0],pos[1]);
                    if(cl.getPosition()[1]>0) aida.histogram2D("Track matched top cluster pos").fill(pos[0],pos[1]);
                    else aida.histogram2D("Track matched bottom cluster pos").fill(pos[0],pos[1]);


                    if(cl_cuts == 0) {

                        aida.histogram2D("Track matched sel cluster pos").fill(pos[0],pos[1]);
                        if(cl.getPosition()[1]>0) aida.histogram2D("Track matched sel top cluster pos").fill(pos[0],pos[1]);
                        else aida.histogram2D("Track matched sel bottom cluster pos").fill(pos[0],pos[1]);



                        aida.histogram1D("selcl_clEoverP_all").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                        if(cl.getPosition()[1]>0) aida.histogram1D("selcl_clEoverP_top").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                        else aida.histogram1D("selcl_clEoverP_bottom").fill(cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));

                        if(cl.getPosition()[1]>0) aida.histogram2D("EoverP vs cl X sel top").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));
                        else aida.histogram2D("EoverP vs cl X sel bottom").fill(crystalPair[0],cl.getEnergy()/(trkMatchTool.getMatchedTrack().getPX()*1000));



                        aida.histogram1D("Matched sel track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                        if(cl.getPosition()[1]>0) aida.histogram1D("Matched sel top track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                        else aida.histogram1D("Matched sel bottom track Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);

                        aida.histogram1D("Matched sel track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                        if(cl.getPosition()[1]>0) aida.histogram1D("Matched sel top track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                        else aida.histogram1D("Matched sel bottom track Chi2").fill(trkMatchTool.getMatchedTrack().getChi2()); 


                        if(posAtTarget.y()>25) {
                            aida.histogram1D("Matched sel track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                            if(cl.getPosition()[1]>0) aida.histogram1D("Matched sel top track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);
                            else aida.histogram1D("Matched sel bottom track !target in X Pz").fill(trkMatchTool.getMatchedTrack().getPX()*1000);

                            aida.histogram1D("Matched sel track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                            if(cl.getPosition()[1]>0) aida.histogram1D("Matched sel top track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2());
                            else aida.histogram1D("Matched sel bottom track !target in X Chi2").fill(trkMatchTool.getMatchedTrack().getChi2()); 

                        }



                        aida.histogram1D("Matched sel track X @ -67cm").fill(posAtTarget.y());
                        aida.histogram1D("Matched sel track Y @ -67cm").fill(posAtTarget.z());
                        if(cl.getPosition()[1]>0) {
                            aida.histogram1D("Top sel matched track X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Top sel matched track Y @ -67cm").fill(posAtTarget.z());
                        } else {
                            aida.histogram1D("Bottom sel matched track X @ -67cm").fill(posAtTarget.y());
                            aida.histogram1D("Bottom sel matched track Y @ -67cm").fill(posAtTarget.z());

                        }
                    }

                } else {


                    if(cl_cuts == 0) {   


                        aida.histogram2D("Track unmatched sel cluster pos").fill(cl.getPosition()[0],cl.getPosition()[1]);
                        if(cl.getPosition()[1]>0) aida.histogram2D("Track unmatched sel top cluster pos").fill(cl.getPosition()[0],cl.getPosition()[1]);
                        else aida.histogram2D("Track unmatched sel bottom cluster pos").fill(cl.getPosition()[0],cl.getPosition()[1]);


                    }

                }





            
        }
        
        aida.histogram1D("Cluster sel multiplicity").fill(cl_sel_count[2]);
        aida.histogram1D("Top cluster sel multiplicity").fill(cl_sel_count[0]);
        aida.histogram1D("Bottom cluster sel multiplicity").fill(cl_sel_count[1]);

        aida.histogram1D("Cluster multiplicity").fill(cl_count[2]);
        aida.histogram1D("Top cluster multiplicity").fill(cl_count[0]);
        aida.histogram1D("Bottom cluster multiplicity").fill(cl_count[1]);
        
        
        
        
        
        
    }
       
    
    
    
    private double getSimpleClusterTheta(HPSEcalCluster cluster, double prodZ,double cluster_corr_z) {
        //Get the angle between the cluster position and the target in the non-bend plane (JLab frame: Y)
        
        //Assumption on production position
        double theta = Math.atan((cluster.getPosition()[1]+cluster_corr_z)/(cluster.getPosition()[2]-prodZ));
        return theta;
        
        
    }
    
    
  
    
    
    
    private int getClusterSizeX(HPSEcalCluster cluster) {
        //List<Cluster> clusters = cluster.getClusters();
        //Set<CalorimeterHit> hitSet = new HashSet<CalorimeterHit>(cluster.getCalorimeterHits());
        int min = 99999;
        int max = -99999;
        //for( Cluster clus : clusters )
        //{
            List<CalorimeterHit> hits = cluster.getCalorimeterHits();
            for (CalorimeterHit hit : hits) {
                int[] pos = getCrystalPair(hit.getPosition());
                if(pos[0]<min) min = pos[0];
                if(pos[0]>max) max = pos[0];
                //System.out.println("min " + min + " max " + max + "   pos " + pos[0] + " , " + pos[1]);
                //hit.getPosition()
                //hitSet.addAll( clus.getCalorimeterHits() );
            }
        //}
        
	// cleanup and return
	int size;
        if(min>9999) size =0;
        else size = 1+ (max - min);
	//hitSet.clear();
        //System.out.println("size " + size);
        return size;
    
    }
      
     private List<HPSEcalCluster> selectCluster(List<HPSEcalCluster> clusters) {
        //need to decide which cluster to take
         List<HPSEcalCluster> selected = new ArrayList<HPSEcalCluster>();
        
        
        for (HPSEcalCluster cl: clusters) {
        
            if(isBadCluster(cl)==0) {
                selected.add(cl);
            }
            
//        switch (ecalClusterSel) {
//            case 1: 
//                //Require at least 1000MeV cluster
//                double E = -1.0;
//                if (cl.getEnergy()>1000.0 ) {
//                   selected.add(cl); 
//                }
//                break;
//            default:
//                // accept all
//                selected.add(cl);
//                break;
//        }
        }
        
        return selected;
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
     
    private int isBadCluster(HPSEcalCluster cluster) {
        int cuts = 0;
        int cl_E_bit = 1 << 0;
        int cl_size_bit = 1 << 1;
        int cl_badneigh_bit = 1 << 2;
        if(cluster.getEnergy()<1000.0) {
            cuts = cuts | cl_E_bit;
        }
        if(cluster.getSize()<2) {
            cuts = cuts | cl_size_bit;
        }
        //if(hasBadNeighbours(cluster)) {
//            cuts = cuts | cl_badneigh_bit;
        //}
//        
//        System.out.println("isBadCluster E " + cluster.getEnergy() 
//                + " size " + cluster.getSize() + "cuts " + Integer.toBinaryString(cuts)  
//                + " (" + cuts +  ") E bit " + Integer.toBinaryString(cl_E_bit) 
//                + " (" + cl_E_bit + ") size bit " + Integer.toBinaryString(cl_size_bit) + " (" +cl_size_bit + ")"
//                + " badneighbour " + Integer.toBinaryString(cl_badneigh_bit) + " (" + cl_badneigh_bit + ")");

        return cuts;
    }
    
    public int[] getCrystalPair(HPSEcalCluster cluster) {
        int[] pos = new int[2];
        pos[0] = cluster.getSeedHit().getIdentifierFieldValue("ix");
        pos[1] = cluster.getSeedHit().getIdentifierFieldValue("iy");
        return pos;
        //getCrystalPair(cluster.getPosition());
    }
    
    
    private int[] getCrystalPair(double[] pos) {
        double x = pos[0];
        double y = pos[1];
        int position[] = new int[2];
        position[0] = (int)Math.floor(x/crystalX);
        //Subtract the beam gap distance and normalize to Y size
        position[1] = (int) Math.floor( (y - beamGap * Math.signum(y) ) / crystalY);
        // For positive crystals I need to add one after floor as we start crystal index from 1
        if(y>0) position[1] = position[1] + 1;
        //System.out.println("x " + x + " crystalX " + crystalX + " -> " + position[0]);
        //System.out.println("y " + y + " crystalY " + crystalY + " -> " + position[1]);
        return position; 
        
    }

//    private double[] getClusterPosition(HPSEcalCluster cluster) {
//        CalorimeterHit hit = cluster.getSeedHit();
//        double pos[] = cluster.getPosition();
//        return pos; 
//    }
   

    
    
    public boolean isTopTrigger() {
        return (trigger[0]>0 ? true : false);
    }

    public boolean isBotTrigger() {
        return (trigger[1]>0 ? true : false);
    }

    private void clearTrigger() {
        trigger[0] = 0;
        trigger[1] = 0;
    }
    
    private void getTrigger(EventHeader event) {
        
        //Clear the trigger bits for this event
        clearTrigger();
        
        if(!event.hasCollection(TriggerData.class, "TriggerBank")) {
            if(debug) System.out.println( "Event has NO trigger bank");
            aida.histogram1D("trigger_bank").fill(0);
            aida.histogram1D("trigger_list").fill(0);
            aida.histogram1D("trigger_count").fill(0);
        } else {
            //if(debug) System.out.println( "Event has trigger bank");
            aida.histogram1D("trigger_bank").fill(1);
            List<TriggerData> triggerDataList = event.get(TriggerData.class, "TriggerBank");
            if(triggerDataList.isEmpty()) {
                aida.histogram1D("trigger_list").fill(0);
                aida.histogram1D("trigger_count").fill(0);
            } else {
                aida.histogram1D("trigger_list").fill(1);
                TriggerData triggerData = triggerDataList.get(0);
                int orTrig = triggerData.getOrTrig();
                int topTrig = triggerData.getTopTrig();
                int botTrig = triggerData.getBotTrig();
                int andTrig = triggerData.getAndTrig();
                //if(debug) System.out.println("top " + topTrig + " bottom " + botTrig + " or " + orTrig);
                if(topTrig>0) {
                    aida.histogram1D("trigger_count").fill(1);
                    trigger[0] = 1;
                }
                if(botTrig>0) {
                    aida.histogram1D("trigger_count").fill(2);
                    trigger[1] = 1;
                }
                if(orTrig>0) aida.histogram1D("trigger_count").fill(3);
                if(topTrig>0 || botTrig>0) aida.histogram1D("trigger_count").fill(4);
                if(andTrig>0) aida.histogram1D("trigger_count").fill(5);
                if(topTrig>0 && botTrig>0) aida.histogram1D("trigger_count").fill(6);
                
                
            }
            
        }
    }

    private void fillResetEff(IHistogram1D hnum,IHistogram1D hden,IHistogram1D heff) {
        heff.reset();
        
        heff = hf.divide(heff.title(), hnum, hden);
/*        
        heff.reset();
        for(int bin=1;bin<heff.axis().bins();++bin) {
            double n = (double)hnum.binEntries(bin);
            double d = (double)hden.binEntries(bin);
            double eff;
            if(d==0) eff=0;
            else eff = n/d;
            heff.fill(heff.axis().binCenter(bin), eff);
        }
  */
    }

    
      
        private void tagAndProbe(List<HPSEcalCluster> clusters) {
        
        
        if(isTopTrigger()) {
            
            //Find the tag
            double Emax=-999999.9;
            HPSEcalCluster cl_tag = null;
            int n = 0;
            for(HPSEcalCluster cl: clusters) {
                if(cl.getPosition()[1]>0) {
                    ++n;
                    aida.histogram1D("toptrig_cl_ecal_e_top").fill(cl.getEnergy());
                    if(cl.getEnergy()>Emax) {
                        Emax = cl.getEnergy();
                        cl_tag = cl;
                    }
                }
            }
            if(Emax>-9999) aida.histogram1D("toptrig_cl_ecal_emax_top").fill(Emax);
            aida.histogram1D("toptrig_cl_ecal_n_top").fill(n);

            if(cl_tag!=null) {
                //Find a probe
                int nb = 0;
                HPSEcalCluster cl_probe = null;
                double Emaxb=-999999.9;
                for(HPSEcalCluster cl: clusters) {
                    if(cl.getPosition()[1]<=0) {
                        ++nb;
                        if(cl.getEnergy()>Emaxb) {
                            Emaxb=cl.getEnergy();
                            cl_probe = cl;
                        }
                    }
                }
                
                //use only cases where the is a single probe candidate
                if(nb==0) {
                    aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(0);
            
                } else if(nb>1) {
                    aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(1);
                }else if(nb==1) {
                    
                    
                    aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(2);
            
                    aida.histogram1D("toptrig_cl_ecal_e_bottom").fill(cl_probe.getEnergy());
                        
                    if(isBotTrigger()) {
                        aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(3);
                        aida.histogram1D("toptrig_cl_ecal_e_bottom_trig").fill(cl_probe.getEnergy());
                    }
                 
                    if(cl_tag.getEnergy()>500) {
                        aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(4);
                        aida.histogram1D("toptrigtag_cl_ecal_e_bottom").fill(cl_probe.getEnergy());
                        
                        if(isBotTrigger()) {
                            aida.histogram1D("toptrig_cl_ecal_n_bottom").fill(5);
                            aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trig").fill(cl_probe.getEnergy());
                        }
                        
                    }
                    
                } 
            } //tag found
        }//topTrigger
        
        
        if((nevents % refreshRate) == 0) {
            IHistogram1D heff = hf.divide(aida.histogram1D("toptrig_cl_ecal_e_bottom_trig").title(), aida.histogram1D("toptrig_cl_ecal_e_bottom_trig"), aida.histogram1D("toptrig_cl_ecal_e_bottom"));
            plotter_trig_tag.region(7).clear();
            plotter_trig_tag.region(7).plot(heff);
            IHistogram heff2 = hf.divide(aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trig").title(), aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trig"), aida.histogram1D("toptrig_cl_ecal_e_bottom"));
            plotter_trig_tag.region(8).clear();
            plotter_trig_tag.region(8).plot(heff2);
        
        }
        
        //fillResetEff(aida.histogram1D("toptrig_cl_ecal_e_bottom_trig"),aida.histogram1D("toptrig_cl_ecal_e_bottom"),aida.histogram1D("toptrig_cl_ecal_e_bottom_trigeff"));
        //fillResetEff(aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trig"),aida.histogram1D("toptrigtag_cl_ecal_e_bottom"),aida.histogram1D("toptrigtag_cl_ecal_e_bottom_trigeff"));
        
        }
        
    
    
    public void endOfData() {
        
        //Print out ecal channels with no hits
        IHistogram2D h = aida.histogram2D("Crystal hit map all");
        System.out.println("Crystals with no hits:");
        for(int iy=-5;iy<=5;++iy) {
            for(int ix=-23;ix<=23;++ix) {
            
                int n = h.binEntries(h.coordToIndexX(ix), h.coordToIndexY(iy));
                //System.out.println(ix + " " + iy + "  (" + h.coordToIndexX(ix) + "," + h.coordToIndexY(iy) + ")  n=" + n);
                if(n==0) System.out.println(ix + " " + iy);
            }
        }
        
        
        if (outputPlotFileName != "")
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(MultScatAnalysis.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        //displayFastTrackingPlots();
        
    }

    
}
