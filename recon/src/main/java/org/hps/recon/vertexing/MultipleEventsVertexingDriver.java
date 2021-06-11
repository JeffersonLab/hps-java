package org.hps.recon.vertexing;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;

//import java.io.IOException;
import java.util.List;
import java.util.Collections;
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

    private List<Track> accumulatedTracksTop            = new ArrayList<Track>();
    private List<BilliorTrack> accumulatedBTracksTop    = new ArrayList<BilliorTrack>(); 

    private List<Track> accumulatedTracksBot            = new ArrayList<Track>();
    private List<BilliorTrack> accumulatedBTracksBot    = new ArrayList<BilliorTrack>(); 


    private String trackCollectionName = "GBLTracks";
    protected double[] beamSize = {0.001, 0.130, 0.050}; // rough estimate from harp scans during engineering run
    private double[] beamPositionToUse = new double[3];
    private boolean _storeCovTrkMomList = false;
    private boolean debug = false;
    protected double bField;
    private AIDA aida;
    private String vtxFold="MultiEventVtx/";
    private int _ntrks = 100;
    private int _nhits = -1;
    private String outputPlots = "multiEvt.root";
    
    public void setNhits(int nhits) {
        _nhits = nhits;
    }

    public void setTrackCollectionName(String val) {
        trackCollectionName = val;
    }
    
    public void setNtrks( int ntrks) {
        _ntrks = ntrks;
    }

    public void setVtxFold( String val) {
        vtxFold = val;
    }


    @Override
    protected void detectorChanged(Detector detector) {
        
        if (aida == null)
            aida = AIDA.defaultInstance();
        
        aida.tree().cd("/");
        
        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
        bField = detector.getFieldMap().getField(ip).y();
        
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
        volumes.add("");
        
        for (String vol : volumes) {
            aida.histogram1D(vtxFold+"vtx_x"+vol,200,-3,3);
            aida.histogram1D(vtxFold+"vtx_y"+vol,200,-1,1);
            aida.histogram1D(vtxFold+"vtx_z"+vol,400,-20,20);
            aida.histogram2D(vtxFold+"vtx_x_y"+vol,200,-3,3,200,-1,1);
            aida.histogram1D(vtxFold+"vtx_chi2"+vol,200,0,8000);
            aida.histogram1D(vtxFold+"vtx_ntrks"+vol,200,0,200);
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
        else {
            return;
        }
        
        for (Track track : trackCollection) {

            if (_nhits < 0 || track.getTrackerHits().size() == _nhits) {
                
                //accumulatedTracks.add(track);
                accumulatedBTracks.add(new BilliorTrack(track));
                
                if (track.getTrackStates().get(0).getTanLambda() > 0) {
                    //accumulatedTracksTop.add(track);
                    accumulatedBTracksTop.add(new BilliorTrack(track));
                }
                
                else {
                    //accumulatedTracksBot.add(track);
                    accumulatedBTracksBot.add(new BilliorTrack(track));
                }
                
            }
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
        
        //Randomize the tracks ? Better to do something more reproducible
        
        Collections.shuffle(accumulatedBTracks);
        FitMultiVtx(accumulatedBTracks,vtxFitter,""); 
        
        FitMultiVtx(accumulatedBTracksTop,vtxFitter,"_top");
        FitMultiVtx(accumulatedBTracksBot,vtxFitter,"_bottom");
        
        


    }
    
    private void FitMultiVtx(List<BilliorTrack> accTrks, BilliorVertexer vtxFitter, String vol) {
        
        int n_chunks = accTrks.size() / _ntrks;
        int n_rest   = accTrks.size() % _ntrks;
        System.out.println("n_chunks = " + n_chunks);
        System.out.println("n_rest = "   + n_rest);
        System.out.printf("size  = %d \n", (n_chunks*_ntrks + n_rest));

        if (accTrks.size() < 2)
            return;
        
        for (int i_chunk = 0; i_chunk < n_chunks; i_chunk++) {
            List<BilliorTrack> tracksForFit = accTrks.subList(i_chunk*_ntrks, (i_chunk+1)*_ntrks);
            BilliorVertex vtx = vtxFitter.fitVertex(tracksForFit);
            aida.histogram1D(vtxFold+"vtx_x"+vol).fill(vtx.getPosition().x());
            aida.histogram1D(vtxFold+"vtx_y"+vol).fill(vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_z"+vol).fill(vtx.getPosition().z());
            aida.histogram2D(vtxFold+"vtx_x_y"+vol).fill(vtx.getPosition().x(),vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_chi2"+vol).fill(vtx.getChi2());
            aida.histogram1D(vtxFold+"vtx_ntrks"+vol).fill(_ntrks);
            //System.out.printf("vtx  %.5f %.5f %.5f\n",vtx.getPosition().x(), vtx.getPosition().y(),vtx.getPosition().z());
        }
        
        if (n_rest > 1) {
            
            List<BilliorTrack> tracksForFit = accTrks.subList(n_chunks*_ntrks, n_chunks*_ntrks + n_rest);
            BilliorVertex vtx = vtxFitter.fitVertex(tracksForFit);
            aida.histogram1D(vtxFold+"vtx_x"+vol).fill(vtx.getPosition().x());
            aida.histogram1D(vtxFold+"vtx_y"+vol).fill(vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_z"+vol).fill(vtx.getPosition().z());
            aida.histogram2D(vtxFold+"vtx_x_y"+vol).fill(vtx.getPosition().x(),vtx.getPosition().y());
            aida.histogram1D(vtxFold+"vtx_ntrks"+vol).fill(n_rest);
            aida.histogram1D(vtxFold+"vtx_chi2"+vol).fill(vtx.getChi2());
        }
     
        return ;
    }
    
}
