/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import hep.aida.*;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.ITree;
import hep.aida.ref.plotter.PlotterRegion;
import java.io.IOException;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public class ECalHitMapPlots {
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    IHistogramFactory hf = aida.histogramFactory();
//    ITree tree = aida.tree();//(ITreeFactory) af.createTreeFactory().create();

    private boolean hide = false;
    
    
    public ECalHitMapPlots() {
        
    }
    
    public ECalHitMapPlots(boolean h) {
        this.hide = h;
        //System.out.println("Constructor ECalHitMapPlots");
        
    }
    
    public void hidePlots(boolean h) {
        this.hide = h;
    }
    
    public void default2DStyle(IPlotterStyle s) {
        s.statisticsBoxStyle().setVisible(false);
        s.setParameter("hist2DStyle","colorMap");
        s.dataStyle().fillStyle().setParameter("colorMapScheme","rainbow");
        
    }

    public void plotBasic2DMap(IHistogram2D h, String title, String xTitle, String yTitle,boolean writeToFile) {
        plotBasic2D(h, title, xTitle, yTitle, 1500, 300,writeToFile);
        
    }
    public void plotBasic2D(IHistogram2D h, String title, String xTitle, String yTitle, int width, int height,boolean writeToFile) {
        IPlotter plotter_hm = af.createPlotterFactory().create();
        plotter_hm.setTitle(title);
        h.setTitle(title);
        plotter_hm.setParameter("plotterWidth","1500");
        plotter_hm.setParameter("plotterHeight","300");
        
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
        //plotter_hm.createRegions(1,1);//.plot(hm);
        plotter_hm.region(0).plot(h);
        //default2DStyle(plotter_hm.region(0).style());
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        plotter_hm.style().setParameter("hist2DStyle","colorMap");
        plotter_hm.style().dataStyle().fillStyle().setParameter("colorMapScheme","rainbow");
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if (!hide) plotter_hm.show();
        if(writeToFile) {
        try {
            plotter_hm.writeToFile(title+".png", "png");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        }
    }
    
     public void plotBasic1D(IHistogram1D h, String title, String xTitle, String yTitle, String fillColor,boolean writeToFile) {
        IPlotter plotter_hm = af.createPlotterFactory().create();
        plotter_hm.setTitle(title);
        if("".equals(fillColor)) fillColor="yellow";
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
        plotter_hm.createRegions(1,1);//.plot(hm);
        plotter_hm.region(0).plot(h);
        plotter_hm.style().statisticsBoxStyle().setVisible(true);
        plotter_hm.style().dataStyle().fillStyle().setColor(fillColor);
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        ((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if(!hide) plotter_hm.show();
        if(writeToFile) {
        try {
            plotter_hm.writeToFile(title+".png", "png");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        }
        
    }

     
     
    public void plotBasic1D(IHistogram1D h, IHistogram1D h2, String title, String xTitle, String yTitle, String leg1,String leg2,boolean writeToFile) {
        //IPlotter plotter_hm1 = af.createPlotterFactory().create();
        //IPlotter plotter_hm2 = af.createPlotterFactory().create();
        IPlotter plotter_hm = af.createPlotterFactory().create();
        //if(!"".equals(title)) {
        //    plotter_hm.setTitle(title);
        //    h.setTitle(title);
        //}
        
        //if(fillColor=="") fillColor="yellow";
        //plotter_hm.createRegion(10.0,20.0, 460.0,100.0);
        //plotter_hm.createRegion(d, d1, d2, d3)
        plotter_hm.createRegions(1,1);//.plot(hm);
        //plotter_hm1.createRegions(1,1);
        //plotter_hm2.createRegions(1,1);
        plotter_hm.region(0).style().dataStyle().fillStyle().setVisible(false);
        plotter_hm.region(0).style().statisticsBoxStyle().setVisible(false);
        //plotter_hm.region(0).style().dataStyle().lineStyle().setColor("green");
        plotter_hm.style().xAxisStyle().setLabel(xTitle);
        plotter_hm.style().yAxisStyle().setLabel(yTitle);
//        plotter_hm1.region(1).style().dataStyle().fillStyle().setVisible(false);
//        plotter_hm1.region(1).style().statisticsBoxStyle().setVisible(false);
//        plotter_hm1.region(1).style().dataStyle().lineStyle().setColor("green");
//        plotter_hm1.style().xAxisStyle().setLabel(xTitle);
//        plotter_hm1.style().yAxisStyle().setLabel(yTitle);
//        plotter_hm2.region(1).style().dataStyle().fillStyle().setVisible(false);
//        plotter_hm2.region(1).style().statisticsBoxStyle().setVisible(false);
//        plotter_hm2.region(1).style().dataStyle().lineStyle().setColor("green");
//        plotter_hm2.style().xAxisStyle().setLabel(xTitle);
//        plotter_hm2.style().yAxisStyle().setLabel(yTitle);
//        
        //IPlotterStyle dataStyle = plotter_hm.region(0).style();
        //dataStyle.dataStyle().lineStyle().setColor("green");
        plotter_hm.region(0).plot(h);
        //IPlotterStyle dataStyle2 = plotter_hm.region(0).style().;
        //dataStyle2.dataStyle().lineStyle().setColor("blue");
        //plotter_hm.region(1).plot(h2,dataStyle);
        //plotter_hm.region(2).plot(h);
        plotter_hm.region(0).plot(h2,"mode=overlay");
        
        //System.out.println("av Params: " + plotter_hm.style().dataStyle().fillStyle().availableParameters().toString());
        //for(String str : plotter_hm.style().dataStyle().fillStyle().availableParameters()) {
        //    System.out.println(str);
        //}
        
        if(!"".equals(xTitle)) ((PlotterRegion) plotter_hm.region(0)).getPlot().getXAxis().setLabel(xTitle);
        if(!"".equals(yTitle)) ((PlotterRegion) plotter_hm.region(0)).getPlot().getYAxis().setLabel(yTitle);          
        if(!hide) plotter_hm.show();
        if(writeToFile) {
            try {
                plotter_hm.writeToFile(title+".png", "png");
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        
    }


    
    
    public void plotMultScatAna(ITree tree) {

        boolean save =true;
        
        IHistogram2D hm = (IHistogram2D)tree.find("Cluster hit map all");        
        plotBasic2DMap(hm,"Cluster Hit Map", "Horizontal Crystal Index","Vertical Crystal Index",save);
        
        IHistogram1D hAmp = (IHistogram1D)tree.find("Cluster size all");
        plotBasic1D(hAmp,"Cluster Size", "Cluster Size","Arbitrary Units","green",save);

        IHistogram1D hAmp2 = (IHistogram1D)tree.find("Cluster energy all");
        plotBasic1D(hAmp2,"Cluster Energy", "Cluster Energy","Arbitrary Units","green",save);

        IHistogram1D hAmp3 = (IHistogram1D)tree.find("toptrig_cl_ecal_e_bottom");
        plotBasic1D(hAmp3,"Cluster Energy Bottom Unbiased", "Cluster Energy","Arbitrary Units","green",save);

        IHistogram2D hcrhm = (IHistogram2D)tree.find("Crystal hit map all");        
        plotBasic2DMap(hcrhm,"Crystal Hit Map", "Horizontal Crystal Index","Vertical Crystal Index",save);

        IHistogram1D hEP_t = (IHistogram1D)tree.find("allcl_clEoverP_top");
        plotBasic1D(hEP_t,"E over P Top", "E over P","Arbitrary Units","green",save);

        IHistogram1D hdx_t = (IHistogram1D)tree.find("allcl_cltrkdx_all");
        plotBasic1D(hdx_t,"Track Matching", "Cluster X - Track X [mm]","Arbitrary Units","blue",save);

        IHistogram1D hdy_t = (IHistogram1D)tree.find("allcl_cltrkdy_all");
        plotBasic1D(hdy_t,"Track Matching", "Cluster Y - Track Y [mm]","Arbitrary Units","green",save);

        
        


    
        
    }
    
    
    
    public void plotEcalFilter(ITree tree) {

        boolean save = true;
       
        IHistogram2D hAmpMap = (IHistogram2D)tree.find("HPS-TestRun-v2 : EcalCalHits : Mean (Amplitude)");        
        plotBasic2DMap(hAmpMap,"Average Crystal Amplitude", "Horizontal Crystal Index","Vertical Crystal Index",save);

        
        IHistogram1D hAmp = (IHistogram1D)tree.find("HPS-TestRun-v2 : EcalCalHits : <Mean> (Amplitude) Filter");        
        plotBasic1D(hAmp,"Average Crystal Amplitude", "Average Crystal Amplitude","Arbitrary Units","",save);

        IHistogram1D hAmp2 = (IHistogram1D)tree.find("/HPS-TestRun-v2 : EcalCalHits : <Mean> (Amplitude) Bottom Trig Filter");
        plotBasic1D(hAmp2,"Average Crystal Amplitude unbiased", "Average Crystal Amplitude","Arbitrary Units","",save);

        
        IHistogram1D hAmpSingle = (IHistogram1D)tree.find("ECAL Amplitudes: x=21; y=-1");        
        plotBasic1D(hAmpSingle,"Single Crystal Amplitude (21,-1)", "Single Crystal Amplitude","Arbitrary Units","",save);
        
        IHistogram1D hAmpSingle2 = (IHistogram1D)tree.find("Top ECAL Amplitudes: x=21; y=-1");        
        plotBasic1D(hAmpSingle2,"Single Crystal Amplitude unbiased (21,-1)", "Single Crystal Amplitude","Arbitrary Units","",save);
        
        
    }

   
    
    public void overLayUpStrBkg(ITree tree_empty, ITree tree) {
        
        boolean save = true;
        
        IHistogram1D hYp_t = (IHistogram1D)tree.find("Top track q>0 Y @ -67cm");        
        IHistogram1D hYn_t = (IHistogram1D)tree.find("Top track q<0 Y @ -67cm");                
        IHistogram1D heYp_t = (IHistogram1D)tree_empty.find("Top track q>0 Y @ -67cm");
        IHistogram1D heYn_t = (IHistogram1D)tree_empty.find("Top track q<0 Y @ -67cm");        

        IHistogram1D hYp_b = (IHistogram1D)tree.find("Bottom track q>0 Y @ -67cm");        
        IHistogram1D hYn_b = (IHistogram1D)tree.find("Bottom track q<0 Y @ -67cm");                
        IHistogram1D heYp_b = (IHistogram1D)tree_empty.find("Bottom track q>0 Y @ -67cm");        
        IHistogram1D heYn_b = (IHistogram1D)tree_empty.find("Bottom track q<0 Y @ -67cm");        

        
        double r_t = 1933.479;
        double r_b = 1933.479;
        double re_t = 309.785;
        double re_b = 309.785;
        
        double c_t = re_t/r_t;
        double c_b = re_b/r_b;

        //plotBasic1D(hYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(heYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","red");
        //plotBasic1D(hYp_t,heYp_t,"Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(hYn_t,heYn_t,"Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        //plotBasic1D(hYp_b,heYp_b,"Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");
        //plotBasic1D(hYn_b,heYn_b,"Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue");

        
        heYp_t.scale(hYp_t.entries()/heYp_t.entries()*c_t);
        heYn_t.scale(hYn_t.entries()/heYn_t.entries()*c_t);

        heYp_b.scale(hYp_b.entries()/heYp_b.entries()*c_b);
        heYn_b.scale(hYn_b.entries()/heYn_b.entries()*c_b);

        plotBasic1D(hYp_t,heYp_t,"(norm) Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","","",save);
        plotBasic1D(hYn_t,heYn_t,"(norm) Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","","",save);

        plotBasic1D(hYp_b,heYp_b,"(norm) Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","","",save);
        plotBasic1D(hYn_b,heYn_b,"(norm) Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","","",save);

        
        IHistogram1D hsYn_t = hf.subtract("(subtr) Top track q<0 Y @ -67cm", hYn_t, heYn_t);
        IHistogram1D hsYp_t = hf.subtract("(subtr) Top track q>0 Y @ -67cm", hYp_t, heYp_t);

        IHistogram1D hsYn_b = hf.subtract("(subtr) Bottom track q<0 Y @ -67cm", hYn_b, heYn_b);
        IHistogram1D hsYp_b = hf.subtract("(subtr) Bottom track q>0 Y @ -67cm", hYp_b, heYp_b);

        plotBasic1D(hsYp_t,"(subtr) Top track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue",save);
        plotBasic1D(hsYn_t,"(subtr) Top track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue",save);
        plotBasic1D(hsYp_b,"(subtr) Bottom track q>0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue",save);
        plotBasic1D(hsYn_b,"(subtr) Bottom track q<0 Y @ -67cm", "Track Y @ -67cm [mm]","Arbitrary Units","blue",save);
        
        
        
        
    }
    
    

/*    
    public void plotAveAmpMap(ITree tree) {

        System.out.println("plotAveAmp with tree " + tree.name());

        IHistogram2D hm = (IHistogram2D)tree.find("Cluster hit map all");
        
        
        IPlotter plotter_hm = af.createPlotterFactory().create();
        plotter_hm.setTitle("Cluster Hit Map");
        plotter_hm.createRegion().plot(hm);
        plotter_hm.show();
        default2DStyle(plotter_hm.region(0).style());
        plotter_hm.style().statisticsBoxStyle().setVisible(false);
        plotter_hm.style().setParameter("hist2DStyle","colorMap");
        plotter_hm.style().dataStyle().fillStyle().setParameter("colorMapScheme","rainbow");
              //((PlotterRegion) plotter_trig_other.region(i)).getPlot().setAllowPopupMenus(true);
        
        
        
        
    }

    */
    
    
}
