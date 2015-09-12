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

import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class ECalExtrapolationDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D res_IP_Y;
    IHistogram1D res_IP_X;
    IHistogram1D res_Last_Y;
    IHistogram1D res_Last_X;
    
    
    /**
     * 
     */
    public ECalExtrapolationDriver() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void detectorChanged(Detector arg0) {
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();
        IPlotter plotter;
        plotter = fac.createPlotterFactory().create("Residual");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        res_IP_Y = aida.histogram1D("res_IP_Y", 50, -5, 5);
        res_IP_X = aida.histogram1D("res_IP_X", 50, -5, 5);
        res_Last_Y = aida.histogram1D("res_Last_Y", 50, -5, 5);
        res_Last_X = aida.histogram1D("res_Last_X", 50, -5, 5);
        plotter.region(0).plot(res_IP_Y);
        plotter.region(1).plot(res_IP_X);
        plotter.region(2).plot(res_Last_Y);
        plotter.region(3).plot(res_Last_X);
        plotter.show();
    }
    
    protected void process(EventHeader event) {
    
    if(!event.hasCollection(Track.class))
        return;
    
    List<List<Track>> trackCollections = event.get(Track.class);
    
    for(List<Track> tracks : trackCollections) {
        
        for (Track track : tracks) {

            if(TrackType.isGBL(track.getType())) {

                TrackState stateIP = null;
                TrackState stateLast = null;
                for(TrackState state : track.getTrackStates()) {
                    if (state.getLocation() == TrackState.AtLastHit) stateLast = state;
                    if (state.getLocation() == TrackState.AtIP) stateIP = state;
                }

                // find last 3D hit
                Hep3Vector lastStereoHitPosition = null;
                Hep3Vector stereoHitPosition = null;
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();
                    if(lastStereoHitPosition != null) {
                        if(lastStereoHitPosition.x() < stereoHitPosition.x() )
                            lastStereoHitPosition = stereoHitPosition;
                    } else {
                        lastStereoHitPosition = stereoHitPosition;
                    }
                }

                //System.out.printf("Found last stereo hit at %s\n",lastStereoHitPosition.toString());

                Hep3Vector trackPositionIP = TrackUtils.extrapolateTrack(stateIP, stereoHitPosition.x());
                Hep3Vector trackPositionLast = TrackUtils.extrapolateTrack(stateLast, stereoHitPosition.x());

                double xResidualIP = trackPositionIP.x() - stereoHitPosition.y();
                double yResidualIP = trackPositionIP.y() - stereoHitPosition.z();

                double xResidualLast = trackPositionLast.x() - stereoHitPosition.y();
                double yResidualLast = trackPositionLast.y() - stereoHitPosition.z();

                res_IP_Y.fill(yResidualIP);
                res_IP_X.fill(xResidualIP);
                res_Last_Y.fill(yResidualLast);
                res_Last_X.fill(xResidualLast);
                
            }
        
            
        }
        
    }
    
    
    }

}
