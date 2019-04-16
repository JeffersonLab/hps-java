package org.hps.analysis.hodoscope;

import java.util.List;

import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.physics.vec.Hep3Vector;

public class SimpleHodoscopeAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String hitCollName = "HodoscopeHits";

    private IHistogram1D simHitCountPlot = aida.histogram1D("Sim Hit Count", 100, 0., 100.);
    private IHistogram1D simHitXPlot = aida.histogram1D("Sim Hit X", 200, 60., 260.);    
    private ICloud1D simHitYPlot = aida.cloud1D("Sim Hit Y");
    private ICloud1D simHitZPlot = aida.cloud1D("Sim Hit Z");
    private ICloud2D simHitXYPlot = aida.cloud2D("Sim Hit X vs Y");
    private ICloud1D simHitLenPlot = aida.cloud1D("Sim Hit Len");
    private ICloud1D simHitLayerPlot = aida.cloud1D("Sim Hit Layer");
    private ICloud1D simHitIdXPlot = aida.cloud1D("Sim Hit X ID");
    private ICloud1D simHitIdYPlot = aida.cloud1D("Sim Hit Y ID");
    private ICloud2D simHitIdXYPlot = aida.cloud2D("Sim Hit XY ID");
    private ICloud1D simHitEnergyPlot = aida.cloud1D("Sim Hit Energy");
    private ICloud1D simHitPathLenPlot = aida.cloud1D("Sim Hit Path Len");
    private IHistogram1D simHitTimePlot = aida.histogram1D("Sim Hit Time", 1000, 0., 250.);
    private ICloud2D simHitLenEnergyPlot = aida.cloud2D("Sim Hit Len vs Energy");
    
    private ICloud2D simHitPosPixelX = aida.cloud2D("Sim Hit Position X vs Pixel X");
    private ICloud2D simHitPosPixelY = aida.cloud2D("Sim Hit Position Y vs Pixel Y");
    private ICloud2D simHitPosPixelZ = aida.cloud2D("Sim Hit Position Z vs Pixel Z");
    
    private ICloud2D simHitPixelXY = aida.cloud2D("Sim Hit Pixel X vs Y");
    
    private HodoscopeDetectorElement hodoDetElem;
    private IIdentifierHelper helper;
    
    protected void detectorChanged(Detector det) {
        hodoDetElem = (HodoscopeDetectorElement) det.getSubdetector("Hodoscope").getDetectorElement();
        helper = hodoDetElem.getIdentifierHelper();
    }
    
    protected void startOfData() {
    }    

    protected void endOfData() {
    }

    protected void process(EventHeader event) {
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, hitCollName);
        simHitCountPlot.fill(simHits.size());
        LCMetaData meta = event.getMetaData(simHits);
        IDDecoder dec = meta.getIDDecoder();       
        for (SimTrackerHit simHit : simHits) {
            double[] pos = simHit.getPosition();
            simHitXPlot.fill(pos[0]);
            simHitYPlot.fill(pos[1]);
            simHitZPlot.fill(pos[2]);
            simHitXYPlot.fill(pos[0], pos[1]);
            Hep3Vector posVec = simHit.getPositionVec();
            simHitLenPlot.fill(posVec.magnitude());
            
            dec.setID(simHit.getCellID64());
            int layer = dec.getValue("layer");
            int ix = dec.getValue("ix");
            int iy = dec.getValue("iy");
            simHitLayerPlot.fill(layer);
            simHitIdXPlot.fill(ix);
            simHitIdYPlot.fill(iy);
            simHitIdXYPlot.fill(ix, iy);
            
            simHitEnergyPlot.fill(simHit.getdEdx());
            
            simHitPathLenPlot.fill(simHit.getPathLength());
            
            simHitTimePlot.fill(simHit.getTime());
            
            simHitLenEnergyPlot.fill(simHit.getPathLength(), simHit.getdEdx());
            
            IIdentifier hitId = simHit.getIdentifier();
            IDetectorElement idDetElem = hodoDetElem.findDetectorElement(hitId).get(0);
                        
            IGeometryInfo geom = idDetElem.getGeometry();
            simHitPosPixelX.fill(pos[0], geom.getPosition().x());
            simHitPosPixelY.fill(pos[1], geom.getPosition().y());
            simHitPosPixelZ.fill(pos[2], geom.getPosition().z());
            
            simHitPixelXY.fill(geom.getPosition().x(), geom.getPosition().y());            
        }
    }
}
