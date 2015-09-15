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

import javax.swing.text.DefaultEditorKit.PasteAction;

import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
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
            
            if(track.getTrackerHits().size() != 6) 
                continue;
            

            if(TrackType.isGBL(track.getType())) {

                TrackState stateIP = null;
                TrackState stateLast = null;
                for(TrackState state : track.getTrackStates()) {
                    if (state.getLocation() == TrackState.AtLastHit) stateLast = state;
                    if (state.getLocation() == TrackState.AtIP) stateIP = state;
                }

                
                // find last 3D hit
                Hep3Vector lastStereoHitPosition = null;
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    Hep3Vector stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();
                    if(lastStereoHitPosition != null) {
                        if(lastStereoHitPosition.x() < stereoHitPosition.x() )
                            lastStereoHitPosition = stereoHitPosition;
                    } else {
                        lastStereoHitPosition = stereoHitPosition;
                    }
                }
                
                /*
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    Hep3Vector stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();
                    RawTrackerHit rawhit = (RawTrackerHit) rotatedStereoHit.getRawHits().get(0);
                    HpsSiSensor sensor = (HpsSiSensor) rawhit.getDetectorElement();
                    int layer = HPSTrackerBuilder.getLayerFromVolumeName(sensor.getName());
                    if(layer == 4) {
                        if(HPSTrackerBuilder.isTopFromName(sensor.getName())) {
                            if(HPSTrackerBuilder.isAxialFromName(sensor.getName())) {
                                lastStereoHitPosition = stereoHitPosition;
                                System.out.printf("\"Last hit\": %s %s: layer %d %s\n", stereoHitPosition.toString(), sensor.getName(), sensor.getLayerNumber(), sensor.isAxial()?"axial":"stereo");
                                
                                break;
                            }
                        } else {
                            if(!HPSTrackerBuilder.isAxialFromName(sensor.getName())) {
                                lastStereoHitPosition = stereoHitPosition;
                                System.out.printf("\"Last hit\": %s %s: layer %d %s\n", stereoHitPosition.toString(), sensor.getName(), sensor.getLayerNumber(), sensor.isAxial()?"axial":"stereo");
                                break;
                            }
                        }
                    }
                }
                */
                
                if( lastStereoHitPosition == null) 
                    throw new RuntimeException("No last hit found!");

                //System.out.printf("\"Last hit position found\": %s \n",lastStereoHitPosition.toString());
                
                Hep3Vector trackPositionIP = TrackUtils.extrapolateTrack(stateIP, lastStereoHitPosition.x());
                Hep3Vector trackPositionLast = TrackUtils.extrapolateTrack(stateLast, lastStereoHitPosition.x());

                double xResidualIP = trackPositionIP.x() - lastStereoHitPosition.y();
                double yResidualIP = trackPositionIP.y() - lastStereoHitPosition.z();

                double xResidualLast = trackPositionLast.x() - lastStereoHitPosition.y();
                double yResidualLast = trackPositionLast.y() - lastStereoHitPosition.z();

                //System.out.printf("Found last stereo hit at %s\n",lastStereoHitPosition.toString());
                //System.out.printf("trackPositionIP   %s\n",trackPositionIP.toString());
                //System.out.printf("trackPositionLast %s\n",trackPositionLast.toString());

                
                res_IP_Y.fill(yResidualIP);
                res_IP_X.fill(xResidualIP);
                res_Last_Y.fill(yResidualLast);
                res_Last_X.fill(xResidualLast);
                
            }
        
            
        }
        
    }
    
    
    }

}
