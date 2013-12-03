package org.lcsim.hps.alignment;

import hep.aida.IAnalysisFactory;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class RunAlignment extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String[] detNames = {"Tracker"};
    Integer _minLayers = 8;
    Integer[] nlayers = {8};
    int nevt = 0;
    double[] beamsize = {0.001, 0.02, 0.02};
    String _config = "1pt8";
    AlignmentParameters ap;
    int totalTracks=0;
// flipSign is a kludge...
//  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
//  so we set the B-field in +iveZ and flip signs of fitted tracks
//  note:  this should be -1 for Test configurations and +1 for Full (v3.X and lower) configurations
//  this is set by the _config variable (detType in HeavyPhotonDriver)
    int flipSign = 1;

    public RunAlignment(int trackerLayers, int mintrkLayers, String config) {
        nlayers[0] = trackerLayers;
        _minLayers = mintrkLayers;
        _config = config;
        if (_config.contains("Test"))
            flipSign = -1;
        ap = new AlignmentParameters("/Users/mgraham/HPS/align.txt");

    }

    public void process(
            EventHeader event) {


        //  Create a map between tracks and the associated MCParticle
        List<Track> tracklist = event.get(Track.class, "MatchedTracks");
//        System.out.println("Number of Tracks = " + tracklist.size());
        double duRange=0.1;
         for (Track trk : tracklist) {
            totalTracks++;
            ap.PrintResidualsAndDerivatives(trk);

            if(1==1){
                aida.histogram1D("Track d0",50,-0.5,0.5).fill(trk.getTrackParameter(0));
                aida.histogram1D("Track sin(phi0)",50,-0.5,0.5).fill(Math.sin(trk.getTrackParameter(1)));
                aida.histogram1D("Track z0",50,-0.1,0.1).fill(Math.sin(trk.getTrackParameter(3)));
                aida.histogram1D("Track chi^2",50,0,25).fill(trk.getChi2());
                for (int i = 1; i < 11; i++) {
                double[] res = ap.getResidual(trk, i);
                int mylayer=(int)res[6];
                if(mylayer<11){
                     aida.histogram1D("Track chi^2 Positive Side",50,0,25).fill(trk.getChi2());
                }else{
                     aida.histogram1D("Track chi^2 Negative Side",50,0,25).fill(trk.getChi2());
                }

                aida.histogram1D("deltaU -- Layer " + mylayer,50,-duRange,duRange).fill(res[0]);
                aida.histogram1D("deltaU Pull-- Layer " + mylayer,50,-3,3).fill(res[0]/res[3]);
                if(i==3&&Math.sin(trk.getTrackParameter(1))>0){
                    aida.histogram1D("Positive phi0  deltaU -- Layer " + mylayer,50,-duRange,duRange).fill(res[0]);
                aida.histogram1D("Positive phi0 deltaU Pull-- Layer " + mylayer,50,-3,3).fill(res[0]/res[3]);
                }
                if(i==3&&Math.sin(trk.getTrackParameter(1))<0){
                    aida.histogram1D("Negative phi0  deltaU -- Layer " + mylayer,50,-duRange,duRange).fill(res[0]);
                aida.histogram1D("Negative phi0 deltaU Pull-- Layer " + mylayer,50,-3,3).fill(res[0]/res[3]);
                }
 
             }
            }
 }

    }

    public void endOfData() {
        try {
            System.out.println("Total Number of Tracks Found = "+totalTracks);
            ap.closeFile();
        } catch (IOException ex) {
            Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
