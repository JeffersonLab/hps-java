package org.hps.recon.vertexing;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;

//import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
//import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public final class MultipleEventsVertexingDriver extends Driver {
    
    private List<Track> accumulatedTracks            = new ArrayList<Track>();
    private List<BilliorTrack> accumulatedBTracks    = new ArrayList<BilliorTrack>(); 
    private String trackCollectionName = "GBLTracks";
    protected double[] beamSize = {0.001, 0.130, 0.050}; // rough estimate from harp scans during engineering run
    private double[] beamPositionToUse = new double[3];
    private boolean _storeCovTrkMomList = false;
    private boolean debug = false;
    protected double bField;
    private AIDA aida;
    private String vtxFold="MultiEventVtx/";
    private int _ntrks = 100;
    private String outputPlots = "multiEvt.root";
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        if (aida == null)
            aida = AIDA.defaultInstance();
        
        aida.tree().cd("/");
        
        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
        bField = detector.getFieldMap().getField(ip).y();
        
        List<String> volumes = new ArrayList<String>();
        //volumes.add("_top");
        //volumes.add("_bottom");
        volumes.add("");
        
        for (String vol : volumes) {
            aida.histogram1D(vtxFold+"vtx_x"+vol,100,-3,3);
            aida.histogram1D(vtxFold+"vtx_y"+vol,100,-1,1);
            aida.histogram1D(vtxFold+"vtx_z"+vol,100,-12,-5);
        }
    }

    @Override
    protected void startOfData() {
    }
    
    @Override
    protected void process(EventHeader event) {
        List<Track> trackCollection = null;
        
        if (event.hasCollection(Track.class, trackCollectionName)) {
            trackCollection = event.get(Track.class,trackCollectionName);
        }
        
        for (Track track : trackCollection) {
            accumulatedTracks.add(track);
            accumulatedBTracks.add(new BilliorTrack(track));
        }
    }
    

    @Override
    protected void endOfData() {
       
        System.out.println("Size of accumulated Billior tracks: " + accumulatedBTracks.size());
        System.out.println("Fitting Vertex");
        
        beamPositionToUse[0] = 0.;
        beamPositionToUse[0] = 0.;
        beamPositionToUse[0] = -10;
   
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPositionToUse);
        vtxFitter.setStoreCovTrkMomList(_storeCovTrkMomList);
        vtxFitter.setDebug(debug);
        vtxFitter.doBeamSpotConstraint(false);
        
        int n_chunks = accumulatedBTracks.size() / _ntrks;
        int n_rest   = accumulatedBTracks.size() % _ntrks;
        System.out.println("n_chunks = " + n_chunks);
        System.out.println("n_rest = "   + n_rest);
        System.out.printf("size  = %d \n", (n_chunks*_ntrks + n_rest));
        
        for (int i_chunk = 0; i_chunk < n_chunks; i_chunk++) {
            List<BilliorTrack> tracksForFit = accumulatedBTracks.subList(i_chunk*_ntrks, (i_chunk+1)*_ntrks);
            BilliorVertex vtx = vtxFitter.fitVertex(tracksForFit);
            aida.histogram1D(vtxFold+"vtx_x").fill(vtx.getPosition().x());
            aida.histogram1D(vtxFold+"vtx_y").fill(vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_z").fill(vtx.getPosition().z());
            System.out.printf("vtx  %.5f %.5f %.5f\n",vtx.getPosition().x(), vtx.getPosition().y(),vtx.getPosition().z());
        }
        
        if (n_rest > 1) {
            
            List<BilliorTrack> tracksForFit = accumulatedBTracks.subList(n_chunks*_ntrks, n_chunks*_ntrks + n_rest);
            BilliorVertex vtx = vtxFitter.fitVertex(tracksForFit);
            aida.histogram1D(vtxFold+"vtx_x").fill(vtx.getPosition().x());
            aida.histogram1D(vtxFold+"vtx_y").fill(vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_z").fill(vtx.getPosition().z());
        }
        
        
        //save the Output
        //try {
        //  aida.saveAs(outputPlots);
        //}
        //catch (IOException ex) {
        //}
    }
    
}
