package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.EventQuality;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This driver class is used to
 * 1) write lcio collection of GBL info objects OR
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

    private AIDA aida = AIDA.defaultInstance();
    int nevt = 0;
    GBLOutput gbl = null;
    TruthResiduals truthRes = null;
    private String gblFileName = "";
    private String outputPlotFileName = "";
    private String MCParticleCollectionName = "MCParticle";   
    private int _debug = 0;
    private boolean isMC = false;
    private int totalTracks = 0;
    private int totalTracksProcessed = 0;
    private int iTrack = 0;
    private int iEvent = 0;

    public GBLOutputDriver() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        Hep3Vector bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));

        // Create the class that handles all the GBL output
        gbl = new GBLOutput(gblFileName, bfield); // if filename is empty no text file is written
        gbl.setDebug(_debug);
        gbl.buildModel(detector);
        gbl.setAPrimeEventFlag(false);
        gbl.setXPlaneFlag(false);

        //Create the class that makes residual plots for cross-checking
        //truthRes = new TruthResiduals(bfield);
        //truthRes.setDebug(_debug);
        //truthRes.setHideFrame(hideFrame);
    }

    @Override
    public void process(EventHeader event) {      
        // Check if the event contains a collection of the type Track. If it
        // doesn't skip the event.
        if (!event.hasCollection(Track.class))
            return;

        // Get all collections of the type Track from the event. This is
        // required since the event contains a track collection for each of the
        // different tracking strategies.
        List<List<Track>> trackCollections = event.get(Track.class);
        if (_debug > 0)
            System.out.printf("%s: Event %d has %d tracks\n", this.getClass().getSimpleName(), event.getEventNumber(), trackCollections.size());

        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        if (_debug > 0)
            System.out.printf("%s: Got %d SiTrackerHitStrip1D in this event\n", stripHits.size());

        List<MCParticle> mcParticles = new ArrayList<MCParticle>();
        if (event.hasCollection(MCParticle.class, this.MCParticleCollectionName))
            mcParticles = event.get(MCParticle.class, this.MCParticleCollectionName);

        List<SimTrackerHit> simTrackerHits = new ArrayList<SimTrackerHit>();
        if (event.hasCollection(SimTrackerHit.class, "TrackerHits"))
            simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");

        if (isMC)
            if (truthRes != null)
                truthRes.processSim(mcParticles, simTrackerHits);
        // Loop over each of the track collections retrieved from the event
         List<Track> selected_tracks = new ArrayList<Track>();
        for (List<Track> tracklist : trackCollections) {           
            for (Track trk : tracklist) {
                totalTracks++;
                if (TrackUtils.isGoodTrack(trk, tracklist, EventQuality.Quality.NONE)) {
                    if (_debug > 0)
                        System.out.printf("%s: Track failed selection\n", this.getClass().getSimpleName());
                    selected_tracks.add(trk);
                }
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
        for (Track trk : selected_tracks) {
            if (_debug > 0)
                System.out.printf("%s: Print GBL output for this track\n", this.getClass().getSimpleName());

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
        }
    
        event.put("GBLEventData" , gblEventData, GBLEventData.class, 0);
        event.put("GBLTrackData" , gblTrackDataList, GBLTrackData.class, 0);
        event.put("GBLStripClusterData" , gblStripDataListAll, GBLStripClusterData.class, 0);
        event.put("GBLTrackToStripData" , gblTrackToStripClusterRelationListAll, LCRelation.class, 0);
        event.put("TrackToGBLTrack" , trackToGBLTrackRelationListAll, LCRelation.class, 0);

        ++iEvent;

    }

    @Override
    public void endOfData() {
        if (gbl != null)
            gbl.close();

        if (!"".equals(outputPlotFileName))
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(GBLOutputDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
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

}
