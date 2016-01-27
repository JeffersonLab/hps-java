/**
 * 
 */
package org.hps.users.phansson;


import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * 
 * Compare residuals using updated track parameters from GBL.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class GblResidualDriver extends Driver {
    
    private static Logger logger = Logger.getLogger(GblResidualDriver.class.getName());

    private double bfield; 
    
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D res_Seed_u;
    IHistogram1D res_IP_u;
    IHistogram1D res_Last_u;

    IHistogram1D res_Seed_Y;
    IHistogram1D res_Seed_X;
    IHistogram1D res_IP_Y;
    IHistogram1D res_IP_X;
    IHistogram1D res_Last_Y;
    IHistogram1D res_Last_X;
    IHistogram1D res_IP_Last_diff_Y;
    IHistogram1D res_IP_Last_diff_X;
    IHistogram1D res_Seed_Last_diff_Y;
    IHistogram1D res_Seed_Last_diff_X;
    IHistogram1D res_IP_Last_diff_u;
    IHistogram1D res_Seed_Last_diff_u;
    
    
    /**
     * 
     */
    public GblResidualDriver() {
        logger.setLevel(Level.INFO);
    }
    
    @Override
    protected void detectorChanged(Detector arg0) {
        
        Hep3Vector bfieldVec = TrackUtils.getBField(arg0);
        bfield = bfieldVec.y();
        logger.info("bfieldVec " + bfieldVec.toString());

        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();
        IPlotter plotter;
        plotter = fac.createPlotterFactory().create("Residual stereo hit");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 3);
        res_Seed_Y = aida.histogram1D("res_Seed_Y", 50, -5, 5);
        res_Seed_X = aida.histogram1D("res_Seed_X", 50, -5, 5);
        res_IP_Y = aida.histogram1D("res_IP_Y", 50, -5, 5);
        res_IP_X = aida.histogram1D("res_IP_X", 50, -5, 5);
        res_Last_Y = aida.histogram1D("res_Last_Y", 50, -5, 5);
        res_Last_X = aida.histogram1D("res_Last_X", 50, -5, 5);
        plotter.region(0).plot(res_IP_Y);
        plotter.region(3).plot(res_IP_X);
        plotter.region(1).plot(res_Last_Y);
        plotter.region(4).plot(res_Last_X);
        plotter.region(2).plot(res_Seed_Y);
        plotter.region(5).plot(res_Seed_X);
        plotter.show();

        
        IPlotter plotter1;
        plotter1 = fac.createPlotterFactory().create("Residual local");
        plotter1.setStyle(style);
        plotter1.createRegions(1, 3);
        res_Seed_u = aida.histogram1D("res_Seed_u", 50, -5, 5);
        res_IP_u = aida.histogram1D("res_IP_u", 50, -5, 5);
        res_Last_u = aida.histogram1D("res_Last_u", 50, -5, 5);
        plotter1.region(0).plot(res_IP_u);
        plotter1.region(1).plot(res_Last_u);
        plotter1.region(2).plot(res_Seed_u);
        plotter1.show();
        
        IPlotter plotter2;
        plotter2 = fac.createPlotterFactory().create("Residual diffs");
        plotter2.setStyle(style);
        plotter2.createRegions(3, 2);
        res_IP_Last_diff_Y = aida.histogram1D("res_IP_Last_diff_Y", 50, -5, 5);
        res_IP_Last_diff_X = aida.histogram1D("res_IP_Last_diff_X", 50, -5, 5);
        res_Seed_Last_diff_Y = aida.histogram1D("res_Seed_Last_diff_Y", 50, -5, 5);
        res_Seed_Last_diff_X = aida.histogram1D("res_Seed_Last_diff_X", 50, -5, 5);
        res_IP_Last_diff_u = aida.histogram1D("res_IP_Last_diff_u", 50, -5, 5);
        res_Seed_Last_diff_u = aida.histogram1D("res_Seed_Last_diff_u", 50, -5, 5);
        plotter2.region(0).plot(res_Seed_Last_diff_Y);
        plotter2.region(2).plot(res_Seed_Last_diff_X);
        plotter2.region(1).plot(res_IP_Last_diff_Y);
        plotter2.region(3).plot(res_IP_Last_diff_X);
        plotter2.region(4).plot(res_Seed_Last_diff_u);
        plotter2.region(5).plot(res_IP_Last_diff_u);
        plotter2.show();
    }
    
    protected void process(EventHeader event) {
    
    if(!event.hasCollection(Track.class))
        return;
    
    List<List<Track>> trackCollections = event.get(Track.class);
    
    
    logger.fine("Found " + trackCollections.size() + " track collections");
    
    // loop over all track collections
    for(List<Track> tracks : trackCollections) {

        logger.fine("Found " + tracks.size() + " tracks in this collection");

        
        // loop over all tracks
        for (Track track : tracks) {
            
            logger.fine("Process track with " + track.getTrackerHits().size() + " hits and of type " + track.getType() + " GBL ? " + TrackType.isGBL(track.getType()));
            
            // require six stereo hits on the track
            if(track.getTrackerHits().size() != 6) 
                continue;
            
            // select GBL tracks
            if(TrackType.isGBL(track.getType())) {
            	
                TrackState stateIP = null;
                TrackState stateLast = null;
                for(TrackState state : track.getTrackStates()) {
                    if (state.getLocation() == TrackState.AtLastHit) stateLast = state;
                    if (state.getLocation() == TrackState.AtIP) stateIP = state;
                }

            	System.out.println("Event " + event.getEventNumber() 
            			+ " gbl track parameters " + 
            			stateIP.getD0() + " " +
            			stateIP.getZ0() + " " +
            			stateIP.getPhi() + " " +
            			stateIP.getOmega() + " " +
            			stateIP.getTanLambda()
            			);


                // find seed track
                Track seedTrack = null;
                List<LCRelation> seedToGblRelation = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
                
                if(seedToGblRelation == null)
                    logger.warning("no LCRelation found!?");
                else {
                    for(LCRelation relation : seedToGblRelation) {
                        if(relation.getTo().equals(track)) {
                            seedTrack = (Track)relation.getFrom();
                            break;
                        }
                    }
                }
                
                // find last 3D hit
                HelicalTrackHit lastHelicalTrackHit = null;
                //HelicalTrackStrip
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    HelicalTrackHit hit = (HelicalTrackHit) rotatedStereoHit;
                    if(lastHelicalTrackHit != null) {
                        if(lastHelicalTrackHit.getCorrectedPosition().x() < hit.getCorrectedPosition().x() ) {
                            lastHelicalTrackHit = hit;
                        }
                    } else {
                        lastHelicalTrackHit = hit;
                    }
                }
                
                if( lastHelicalTrackHit == null) 
                    throw new RuntimeException("No last hit found!");
                
                Hep3Vector lastStereoHitPosition = lastHelicalTrackHit.getCorrectedPosition();
                

                // find the last strip cluster
                // OK, I could be more rigorous but this should work 
                List<HelicalTrackStrip> strips = ((HelicalTrackCross) lastHelicalTrackHit).getStrips();
                HelicalTrackStrip lastStrip = strips.get(0).origin().x() > strips.get(1).origin().x() ? strips.get(0) : strips.get(1);
                
                Map<String,Double> localResidualsIP = TrackUtils.calculateLocalTrackHitResiduals(TrackUtils.getHTF(stateIP), lastStrip, bfield);
                Map<String,Double> localResidualsLast = TrackUtils.calculateLocalTrackHitResiduals(TrackUtils.getHTF(stateLast), lastStrip, bfield);
                
                Hep3Vector trackPositionIP = TrackUtils.extrapolateTrack(stateIP, lastStereoHitPosition.x());
                Hep3Vector trackPositionLast = TrackUtils.extrapolateTrack(stateLast, lastStereoHitPosition.x());

                double xResidualIP = trackPositionIP.x() - lastStereoHitPosition.y();
                double yResidualIP = trackPositionIP.y() - lastStereoHitPosition.z();

                double xResidualLast = trackPositionLast.x() - lastStereoHitPosition.y();
                double yResidualLast = trackPositionLast.y() - lastStereoHitPosition.z();

                                
                res_IP_u.fill(localResidualsIP.get("ures"));
                res_IP_Y.fill(yResidualIP);
                res_IP_X.fill(xResidualIP);
                res_Last_u.fill(localResidualsLast.get("ures"));
                res_Last_Y.fill(yResidualLast);
                res_Last_X.fill(xResidualLast);
                res_IP_Last_diff_Y.fill(Math.abs(yResidualLast) - Math.abs(yResidualIP));
                res_IP_Last_diff_X.fill(Math.abs(xResidualLast) - Math.abs(xResidualIP));
                res_IP_Last_diff_u.fill(Math.abs(localResidualsLast.get("ures")) - Math.abs(localResidualsIP.get("ures")));

                
                if(seedTrack != null) {
                    Hep3Vector trackPositionSeed = TrackUtils.extrapolateTrack(seedTrack, lastStereoHitPosition.x());
                    double xResidualSeed = trackPositionSeed.x() - lastStereoHitPosition.y();
                    double yResidualSeed = trackPositionSeed.y() - lastStereoHitPosition.z();
                    res_Seed_Y.fill(yResidualSeed);
                    res_Seed_X.fill(xResidualSeed);
                    res_Seed_Last_diff_Y.fill(Math.abs(yResidualLast) - Math.abs(yResidualSeed));
                    res_Seed_Last_diff_X.fill(Math.abs(xResidualLast) - Math.abs(xResidualSeed));
                    
                    Map<String,Double> localResidualsSeed = TrackUtils.calculateLocalTrackHitResiduals(seedTrack, lastHelicalTrackHit, lastStrip, bfield);
                    res_Seed_u.fill(localResidualsSeed.get("ures"));
                    res_Seed_Last_diff_u.fill(Math.abs(localResidualsLast.get("ures")) - Math.abs(localResidualsSeed.get("ures")));


                    
                } else {
                    logger.warning("No seed track found");
                }
                    

                
                

                
                
            }
        
            
        }
        
    }
    
    
    }

}
