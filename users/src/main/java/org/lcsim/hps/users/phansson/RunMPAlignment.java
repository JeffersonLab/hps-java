package org.lcsim.hps.users.phansson;

import hep.physics.vec.BasicHep3Vector;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class RunMPAlignment extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String[] detNames = {"Tracker"};
    int nevt = 0;
    double[] beamsize = {0.001, 0.02, 0.02};
    String _config = "";
    StripMPAlignmentInput ap;
    int totalTracks=0;
    int totalTracksProcessed=0;
    private String _resLimitFileName="";
    private String outputPlotFileName="";
    private boolean hideFrame = false;
// flipSign is a kludge...
//  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
//  so we set the B-field in +iveZ and flip signs of fitted tracks
//  note:  this should be -1 for Test configurations and +1 for Full (v3.X and lower) configurations
//  this is set by the _config variable (detType in HeavyPhotonDriver)
    int flipSign = 1;
    private boolean _debug = false;
    private String _type = "LOCAL"; //GLOBAL OR LOCAL RESIDUALS
     
    private String simTrackerHitCollectionName = "TrackerHits";
    private String milleFile = "alignMP.txt";
    
    public void setDebug(boolean v) {
        this._debug = v;
    }
    public void setType(String type) {
        this._type = type;
    }
    public void setMilleFile(String filename) {
        milleFile = filename;
    }
    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }
    
    public void setHideFrame(boolean hide) {
        hideFrame = hide;
    }
    
    public void setResidualLimitFileName(String fileName) {
        this._resLimitFileName = fileName;
    }

    
    public RunMPAlignment() {
    }

    
    @Override
    public void detectorChanged(Detector detector) {
        
        ap = new StripMPAlignmentInput(milleFile,_type);
        ap.setDebug(_debug);
        ap.setHideFrame(hideFrame);
        double bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.)).y();
        System.out.printf("%s: B-field in z %.3f\n",this.getClass().getSimpleName(),bfield);
        ap.setUniformZFieldStrength(bfield);
        loadResidualLimits();
        
    }
    
    
    
    @Override
    public void process(EventHeader event) {

        
        List<Track> tracklist = null;
        if(event.hasCollection(Track.class,"MatchedTracks")) {        
            tracklist = event.get(Track.class, "MatchedTracks");
             if(_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Number of Tracks = " + tracklist.size());
             }
        }
        
        for (Track trk : tracklist) {
            
            //if(trk.getCharge()>0) continue;
            //if(trk.getTrackStates().get(0).getMomentum()[0]>0.8) continue;
            
            totalTracks++;
            
            if(hitsOnBothSides(trk)) continue;
            
            double Px =  trk.getTrackStates().get(0).getMomentum()[0];
            if(Px < 0.2) {
                System.out.printf("%s: Trk p = [%.3f,%.3f,%.3f] is low skip!?\n",this.getClass().getSimpleName(),
                        trk.getTrackStates().get(0).getMomentum()[0],trk.getTrackStates().get(0).getMomentum()[1],trk.getTrackStates().get(0).getMomentum()[2]);
                continue;
            }
            
            totalTracksProcessed++;
            
            ap.PrintResidualsAndDerivatives(trk, totalTracks);
            
            
            

        }
        
        
        
        
    }

    @Override
    public void endOfData() {
        ap.updatePlots();
        ap.closeFile();
        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(TrigRateDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Found = "+totalTracks);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Processed = "+totalTracksProcessed);
        
        
    }
    
    
    public boolean hitsOnBothSides(Track track) {
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        int ihalf = hitsOnTrack.get(0).getPosition()[2]>0 ? 1 : 0;
        boolean hitsOnWrongSide = false;
        for (TrackerHit hit : hitsOnTrack) {
            double[] pos = hit.getPosition();
            if((ihalf==0 && pos[2]>0) || (ihalf==1 && pos[2]<0)) {
                hitsOnWrongSide = true;
                break;
            }
        }   
        if(hitsOnWrongSide) {
            System.out.println(this.getClass().getSimpleName() + ": TRACK w/ both halves hit (: chi2 "+track.getChi2()+", pX "+track.getPX()+", pY "+track.getPY()+", pZ "+track.getPZ()+")");
            System.out.printf(this.getClass().getSimpleName() + ": Hits: ");
            for (TrackerHit hit : hitsOnTrack) {
                double[] pos = hit.getPosition();
                System.out.printf(this.getClass().getSimpleName() + ": (%.2f,%.2f,%.2f)", pos[0],pos[1],pos[2]);
            }   
            System.out.println("");
        }
        return hitsOnWrongSide;
    }
    
    private void loadResidualLimits() {
        
        //Initialize the res limits
        for(int i=1;i<=10;++i) {
            for(int j=0;j<3;++j) {
                double xmin = -50;
                double xmax = 50;
                for(int side=0;side<2;++side) {
                   ap.setResLimits(side,i,j, xmin, xmax);        
                }
            }
        }
        
        if(!"".equals(this._resLimitFileName)) {
            FileReader fReader;
            BufferedReader bReader;
            try {
                fReader = new FileReader(this._resLimitFileName);
                bReader = new BufferedReader(fReader);

                String line;
                while( (line = bReader.readLine()) != null) {
                    if (line.contains("#")) continue;
                    String[] vec = line.split("\\s+");
                    if(vec.length!=5) {
                        System.out.println(this.getClass().getSimpleName() + ": Error: residual limits line has wrong format -> " + line);
                        System.exit(1);
                    }
                    try {
                        int side = Integer.parseInt(vec[0]);
                        int layer = Integer.parseInt(vec[1]);
                        int direction = Integer.parseInt(vec[2]);
                        double min = Double.parseDouble(vec[3]);
                        double max = Double.parseDouble(vec[4]);
                        ap.setResLimits(side, layer, direction, min, max);
                    } catch(NumberFormatException e) {
                        Logger.getLogger(RunMPAlignment.class.getName()).log(Level.SEVERE,null,e);
                    }
                }
                bReader.close();
                fReader.close();
            }
            catch (FileNotFoundException ex) {
                Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException e) {
                Logger.getLogger(TrigRateAna.class.getName()).log(Level.SEVERE,null,e);
            } 
        }
    
        

    }
    
    
    
    
    
    
}
