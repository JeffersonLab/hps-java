package org.hps.analysis.muon;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.units.clhep.SystemOfUnits;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SimpleMuonAnalysis extends Driver 
{
    AIDA aida = AIDA.defaultInstance();
    ICloud1D hite = aida.cloud1D("Hit Energy [MeV]");
    ICloud1D nhits = aida.cloud1D("Number Of Hits Per Event");    
    ICloud2D xy = aida.cloud2D("Hit Position XY");
    IHistogram1D layer = aida.histogram1D("Hit Layer Number", 4, 0, 4);
    
    public void startOfData() 
    {
    }
    
    public void endOfData() 
    {        
    }
    
    public void detectorChanged(Detector det) 
    {        
    }
    
    public void process(EventHeader event) 
    {
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "MUON_HITS");
        nhits.fill(hits.size());
        for (CalorimeterHit hit : hits) {
            xy.fill(hit.getPosition()[0], hit.getPosition()[1]);
            hite.fill(hit.getRawEnergy() * SystemOfUnits.MeV);
            layer.fill(hit.getLayerNumber());
        }
    }
}