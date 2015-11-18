package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.EventQuality;
import org.hps.recon.tracking.StrategyType;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This driver class is used to 1) write lcio collection of GBL info objects OR
 * 2) write GBL info into a unstructures text-based output
 *
 * It uses a helper class that does the actual work. We will port GBL to java
 * and that will replace this driver.
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: GBLOutputDriver.java,v 1.9 2013/11/07 03:54:58 phansson Exp $
 * $Date: 2013/11/07 03:54:58 $ $Author: phansson $
 *
 */
public class GBLOutputDriver extends Driver {

    private final AIDA aida = AIDA.defaultInstance();
    int nevt = 0;
    GBLOutput gbl = null;
    TruthResiduals truthRes = null;
    private String gblFileName = "";
    private String outputPlotFileName = "";
    private final String MCParticleCollectionName = "MCParticle";
    private String trackCollectionName = "MatchedTracks";
    private int _debug = 0;
    private boolean isMC = false;
    private int totalTracks = 0;
    private int totalTracksProcessed = 0;
    private int iTrack = 0;
    private int iEvent = 0;
    private boolean addBeamspot=false;
    private double beamspotScatAngle = 0.000001;
    private double beamspotWidthZ = 0.05;
    private double beamspotWidthY = 0.15;
    private double beamspotTiltZOverY = 15.0*180.0/Math.PI;
    private double beamspotPosition[] = {0,0,0};

    
    

    public GBLOutputDriver() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        Hep3Vector bfield = TrackUtils.getBField(detector);

        // Create the class that handles all the GBL output
        gbl = new GBLOutput(gblFileName, bfield); // if filename is empty no text file is written
        gbl.setDebug(_debug);
        gbl.buildModel(detector);
        gbl.setXPlaneFlag(false);
        gbl.setAddBeamspot(addBeamspot);
        gbl.setBeamspotScatAngle(beamspotScatAngle);
        gbl.setBeamspotWidthY(beamspotWidthY);
        gbl.setBeamspotWidthZ(beamspotWidthZ);
        gbl.setBeamspotTiltZOverY(beamspotTiltZOverY);
        gbl.setBeamspotPosition(beamspotPosition);

        //Create the class that makes residual plots for cross-checking
        //truthRes = new TruthResiduals(bfield);
        //truthRes.setDebug(_debug);
        //truthRes.setHideFrame(hideFrame);
    }

    @Override
    public void process(EventHeader event) {
        List<Track> tracklist = null;
        if (event.hasCollection(Track.class, trackCollectionName)) {
            tracklist = event.get(Track.class, trackCollectionName);
            if (_debug > 0) {
                System.out.printf("%s: Event %d has %d tracks\n", this.getClass().getSimpleName(), event.getEventNumber(), tracklist.size());
            }
        } else {
            return;
        }

        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        if (_debug > 0) {
            System.out.printf("%s: Got %d SiTrackerHitStrip1D in this event\n",this.getClass().getSimpleName(), stripHits.size());
        }

        List<MCParticle> mcParticles = new ArrayList<MCParticle>();
        if (event.hasCollection(MCParticle.class, this.MCParticleCollectionName)) {
            mcParticles = event.get(MCParticle.class, this.MCParticleCollectionName);
        }

        List<SimTrackerHit> simTrackerHits = new ArrayList<SimTrackerHit>();
        if (event.hasCollection(SimTrackerHit.class, "TrackerHits")) {
            simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        }

        if (isMC) {
            if (truthRes != null) {
                truthRes.processSim(mcParticles, simTrackerHits);
            }
        }

        // GBLData 
        // containers and data
        List<GBLEventData> gblEventData = new ArrayList<GBLEventData>();
        gblEventData.add(new GBLEventData(event.getEventNumber(), gbl.get_B().z()));
        List<GBLTrackData> gblTrackDataList = new ArrayList<GBLTrackData>();
        List<GBLStripClusterData> gblStripDataListAll = new ArrayList<GBLStripClusterData>();
        List<GBLStripClusterData> gblStripDataList = new ArrayList<GBLStripClusterData>();
        List<LCRelation> gblTrackToStripClusterRelationListAll = new ArrayList<LCRelation>();
        List<LCRelation> trackToGBLTrackRelationListAll = new ArrayList<LCRelation>();

        gbl.printNewEvent(iEvent, gbl.get_B().z());

        iTrack = 0;

        // Loop over each of the track collections retrieved from the event
        for (Track trk : tracklist) {
            totalTracks++;
            
            if (_debug > 0) System.out.printf("%s: PX %f bottom %d\n", this.getClass().getSimpleName(), trk.getPX(), TrackUtils.isBottomTrack(trk, 4)?1:0) ;
            
            //if( trk.getPX() < 0.9) continue;
            
            //if( TrackUtils.isBottomTrack(trk, 4)) continue;
            
            if (TrackUtils.isGoodTrack(trk, tracklist, EventQuality.Quality.NONE)) {
                if (_debug > 0) {
                    System.out.printf("%s: Print GBL output for this track\n", this.getClass().getSimpleName());
                }

                //GBLDATA
                GBLTrackData gblTrackData = new GBLTrackData(iTrack);
                gblTrackDataList.add(gblTrackData);

                //print to text file
                gbl.printTrackID(iTrack);
                gbl.printGBL(trk, stripHits, gblTrackData, gblStripDataList, mcParticles, simTrackerHits, this.isMC);

                //GBLDATA
                //add relation to normal track object
                trackToGBLTrackRelationListAll.add(new MyLCRelation(trk, gblTrackData));
                // add strip clusters to lists
                for (GBLStripClusterData gblStripClusterData : gblStripDataList) {
                    // add all strip clusters from this track to output list
                    gblStripDataListAll.add(gblStripClusterData);
                    // add LC relations between cluster and track
                    gblTrackToStripClusterRelationListAll.add(new MyLCRelation(gblTrackData, gblStripClusterData));
                }
                // clear list of strips for next track
                gblStripDataList.clear();

                totalTracksProcessed++;
                ++iTrack;
            } else if (_debug > 0) {
                System.out.printf("%s: Track failed selection\n", this.getClass().getSimpleName());
            }
        }

        event.put("GBLEventData", gblEventData, GBLEventData.class, 0);
        event.put("GBLTrackData", gblTrackDataList, GBLTrackData.class, 0);
        event.put("GBLStripClusterData", gblStripDataListAll, GBLStripClusterData.class, 0);
        event.put("GBLTrackToStripData", gblTrackToStripClusterRelationListAll, LCRelation.class, 0);
        event.put("TrackToGBLTrack", trackToGBLTrackRelationListAll, LCRelation.class, 0);

        ++iEvent;

    }

    @Override
    public void endOfData() {
        if (gbl != null) {
            gbl.close();
        }

        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Events           = " + iEvent);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks           = " + totalTracks);
        System.out.println(this.getClass().getSimpleName() + ": Total Number of Tracks Processed = " + totalTracksProcessed);

    }

    public void setDebug(int v) {
        this._debug = v;
    }

    public void setGblFileName(String filename) {
        gblFileName = filename;
    }

    public void setOutputPlotFileName(String filename) {
        outputPlotFileName = filename;
    }

    public void setIsMC(boolean isMC) {
        this.isMC = isMC;
    }

    public void setAddBeamspot(boolean add){
        this.addBeamspot=add;
    }

    public double getBeamspotScatAngle() {
        return beamspotScatAngle;
    }

    public void setBeamspotScatAngle(double beamspotScatAngle) {
        this.beamspotScatAngle = beamspotScatAngle;
    }

    public double getBeamspotWidthZ() {
        return beamspotWidthZ;
    }

    public void setBeamspotWidthZ(double beamspotWidthZ) {
        this.beamspotWidthZ = beamspotWidthZ;
    }

    public double getBeamspotWidthY() {
        return beamspotWidthY;
    }

    public void setBeamspotWidthY(double beamspotWidthY) {
        this.beamspotWidthY = beamspotWidthY;
    }

    public double getBeamspotTiltZOverY() {
        return beamspotTiltZOverY;
    }

    public void setBeamspotTiltZOverY(double beamspotTiltZOverY) {
        this.beamspotTiltZOverY = beamspotTiltZOverY;
    }

    public double[] getBeamspotPosition() {
        return beamspotPosition;
    }

    public void setBeamspotPosition(double beamspotPosition[]) {
        this.beamspotPosition = beamspotPosition;
    }
    
}
