package org.hps.users.spaul;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.util.IDDecoder;
import org.lcsim.geometry.util.IDDescriptor;
import org.lcsim.geometry.util.IDDescriptor.IDException;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.SIOSimCalorimeterHit;
import org.lcsim.util.Driver;

public class HitrateHistograms extends Driver{
    HashMap<String, IHistogram2D[]> hist2d = new HashMap<String, IHistogram2D[]>();

    IAnalysisFactory af = IAnalysisFactory.create();
    IHistogramFactory hf = af.createHistogramFactory(af.createTreeFactory().create());
    IPlotterFactory pf = af.createPlotterFactory();
    public HitrateHistograms(){
        addHistograms2D("Ecal", 1, -30, 30, -8, 16);
        addHistograms2D("muon", 8, -30, 30, -8, 16);

        try {
            ecalDecoder = new IDDecoder(new IDDescriptor("system:0:6,layer:6:2,ix:8:-8,iy:16:-6"));
        } catch (IDException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }
    private void addHistograms2D(String detector, int nLayers, int ixMin, int ixMax,  int iyMin, int iyMax){
        IPlotter plotter1 = pf.create("detector");
        int nx = (int)Math.ceil(Math.sqrt(nLayers));
        int ny = (int)Math.ceil(nLayers/(double)nx);
        plotter1.createRegions(nx, ny);

        hist2d.put(detector,new IHistogram2D[nLayers]);
        for(int i = 0; i< nLayers; i++){
            hist2d.get(detector)[i] 
                                 = hf.createHistogram2D(detector + " layer " + (i+1), ixMax-ixMin, ixMin, ixMax, iyMax-iyMin, iyMin, iyMax);
            plotter1.region(i).plot(hist2d.get(detector)[i]);

        }
    }


    IDDecoder ecalDecoder;
    private float recency;

    public void process(EventHeader header){


        //System.out.println(header.keys());


        for(SIOSimCalorimeterHit hit: header.get(SIOSimCalorimeterHit.class,"EcalHits")){
            int fieldCount = hit.getIDDecoder().getFieldCount();
            ecalDecoder.setID(hit.getCellID());
            int ix = ecalDecoder.getValue("ix");
            int iy = ecalDecoder.getValue("iy");
            int layer = ecalDecoder.getValue("layer");
            hist2d.get("Ecal")[layer].fill(ix, iy);
        }
        
        
        /*if(recency != 0)
        for(IHistogram2D[] hists : hist2d.values()){
            for(IHistogram2D hist : hists){
                hf.
            }
        }*/
        //}
    }
    /**
     * "recency" is a parameter used to determine how to time-weight the 
     * histogram so that the more recent events are more highly weighted.
     * A recency of zero means that there is no time weightedness.
     * A recency of R means that each event is weighted by a factor of (1-R)^n,
     * where n is the number of events that have taken place since the event shown.
     * @param recency
     */
    //public void setRecency(float recency){
    //this.recency = recency;
    //}

    public void startOfData(){

    }
    public void endOfData(){

        for(String name : hist2d.keySet()){
            IHistogram2D[] hists = hist2d.get(name);
            System.out.println(name);
            for(int i = 0; i< hists.length; i++){
                double max = hists[i].maxBinHeight();
                double total = hists[i].sumAllBinHeights();
                double ratioPercent = 100.*max/(double)total;
                System.out.printf("  layer %d: %.2f %% of hits were in the most populated bin\n",i, ratioPercent);
            }
        }
    }
    public static void main(String arg[]) throws IOException{
        LCIOReader lcReader = new LCIOReader(new File(arg[0]));
        HitrateHistograms driver = new HitrateHistograms();
        driver.startOfData();
        for(int i=0;i<1000;i++){
            EventHeader event = lcReader.read();

            if(event == null)
                break;
            driver.process(event);

        }
        driver.endOfData();
    }

}
