package org.hps.analysis.hodoscope;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.ICloud1D;
//import hep.aida.ICloud2D;

public class SimpleHodoscopeAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String hitCollName = "HodoscopeHits";

    private ICloud1D hitXPlot = aida.cloud1D("X");
    private ICloud1D hitYPlot = aida.cloud1D("Y");
    private ICloud1D hitZPlot = aida.cloud1D("Z");
    //private ICloud2D xVsYPlot = aida.cloud2D("X vs Y");
    private ICloud1D layerPlot = aida.cloud1D("Layer");
    private ICloud1D rawEPlot = aida.cloud1D("Raw Energy");
    private ICloud1D ixPlot = aida.cloud1D("ix");
    private ICloud1D iyPlot = aida.cloud1D("iy");
    private ICloud1D xIdPlot = aida.cloud1D("x ID");
    private ICloud1D yIdPlot = aida.cloud1D("y ID");
    //private ICloud2D xVsYIdPlot = aida.cloud2D("x vs y ID");
    //private ICloud2D ixVsiyPlot = aida.cloud2D("ix vs iy");
    
    protected void detectorChanged(Detector det) {
    }
    
    protected void startOfData() {
    }    

    protected void endOfData() {
    }

    protected void process(EventHeader event) {
        List<SimCalorimeterHit> hodoHits = event.get(SimCalorimeterHit.class, hitCollName);
        LCMetaData meta = event.getMetaData(hodoHits);
        IDDecoder dec = meta.getIDDecoder();
        for (SimCalorimeterHit hit : hodoHits) {
            double[] pos = hit.getPosition();
            hitXPlot.fill(pos[0]);
            //hitYPlot.fill(pos[1]);
            //hitZPlot.fill(pos[2]);
            //xVsYPlot.fill(pos[0], pos[1]);
            
            double e = hit.getRawEnergy() * 1000; // GeV to MeV
            rawEPlot.fill(e);
            
            dec.setID(hit.getCellID());
            int layer = dec.getValue("layer");
            int ix = dec.getValue("ix");
            int iy = dec.getValue("iy");
            int x = dec.getValue("x");
            int y = dec.getValue("y");
            
            //layerPlot.fill(layer);
            //ixPlot.fill(ix);
            //iyPlot.fill(iy);
            //xIdPlot.fill(x);
            //yIdPlot.fill(y);
            
            //xVsYIdPlot.fill(x, y);
            //ixVsiyPlot.fill(ix, iy);
        }
    }
}
