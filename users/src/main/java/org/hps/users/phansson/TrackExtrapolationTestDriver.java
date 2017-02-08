package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TrackExtrapolationTestDriver extends Driver {
    
    private static Logger LOGGER = Logger.getLogger(TrackExtrapolationTestDriver.class.getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D res_trackPos_Y;
    IHistogram1D res_trackPos_X;
    IHistogram2D res_trackPos_Y_vs_p;
    IHistogram2D res_trackPos_X_vs_p;
    private FieldMap bFieldMap;
    private double stepSize = 1.0; //mm
    
    /**
     * 
     */
    public TrackExtrapolationTestDriver() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void detectorChanged(Detector arg0) {
        
        // Get the field map from the detector object
        bFieldMap = arg0.getFieldMap(); 
        
        
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();
        IPlotter plotter;
        plotter = fac.createPlotterFactory().create("Residual");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        res_trackPos_Y = aida.histogram1D("res_trackPos_Y", 50, -0.1, 0.1);
        res_trackPos_X = aida.histogram1D("res_trackPos_X", 50, -2.5, 2.5);
        res_trackPos_Y_vs_p = aida.histogram2D("res_trackPos_Y_vs_p", 50,0.1, 1.4, 50, -0.1, 0.1);
        res_trackPos_X_vs_p = aida.histogram2D("res_trackPos_X_vs_p", 50,0.1, 1.4, 50, -2.5, 2.5);
        plotter.region(0).plot(res_trackPos_Y);
        plotter.region(1).plot(res_trackPos_X);
        plotter.region(2).plot(res_trackPos_Y_vs_p);
        plotter.region(3).plot(res_trackPos_X_vs_p);
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




                // find hit on each layer
                Map<Integer, TrackerHit> hits = new HashMap<Integer, TrackerHit>();
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    RawTrackerHit rawhit = (RawTrackerHit) rotatedStereoHit.getRawHits().get(0);
                    HpsSiSensor sensor = (HpsSiSensor) rawhit.getDetectorElement();
                    int layer = HPSTrackerBuilder.getLayerFromVolumeName(sensor.getName());
                    hits.put(layer, rotatedStereoHit);
                }

                Map<Integer, Hep3Vector> layerTrackPos = new HashMap<Integer, Hep3Vector>();
                Map<Integer, Hep3Vector> layerTrackPos3DField = new HashMap<Integer, Hep3Vector>();
                
                for(Map.Entry<Integer, TrackerHit> entry : hits.entrySet()) {
                    int layer = entry.getKey();
                    Hep3Vector stereoHitPosition = ((HelicalTrackHit) entry.getValue()).getCorrectedPosition();
                    Hep3Vector prevStereoHitPosition = null;
                    if( hits.containsKey(layer-1)) {
                        prevStereoHitPosition = ((HelicalTrackHit) hits.get(layer -1)).getCorrectedPosition();
                    }
                    Hep3Vector trackPosition = TrackUtils.extrapolateHelixToXPlane(track, stereoHitPosition.x());
                    Hep3Vector trackPosition3DField = null;
                    TrackState trackState3DField = null;
                    if( prevStereoHitPosition != null) {
                        trackState3DField = TrackUtils.extrapolateTrackUsingFieldMap(track, prevStereoHitPosition.x(), stereoHitPosition.x(), stepSize, bFieldMap);
                        trackPosition3DField = new BasicHep3Vector(trackState3DField.getReferencePoint());
                    }                        
                    layerTrackPos.put(layer, trackPosition);
                    layerTrackPos3DField.put(layer, trackPosition3DField);
                    
                    LOGGER.fine("layer " + layer + " stereohitposition " + stereoHitPosition.toString());
                    if( prevStereoHitPosition != null)
                        LOGGER.fine("prevStereoHitPosition " + prevStereoHitPosition.toString());
                    LOGGER.fine("trackPos " + layerTrackPos.get(layer).toString());
                    if( trackPosition3DField != null) {
                        LOGGER.fine("trackPosition3DField " + trackPosition3DField.toString());
                    } else {
                        LOGGER.fine("trackPosition3DField  no prev layer ");
                    }
                    
                    if(layer == 6 ) {
                        res_trackPos_Y.fill(VecOp.sub(trackPosition, trackPosition3DField).z());
                        res_trackPos_X.fill(VecOp.sub(trackPosition, trackPosition3DField).y());
                        res_trackPos_Y_vs_p.fill(track.getTrackStates().get(0).getMomentum()[0], VecOp.sub(trackPosition, trackPosition3DField).z());
                        res_trackPos_X_vs_p.fill(track.getTrackStates().get(0).getMomentum()[0], VecOp.sub(trackPosition, trackPosition3DField).y());
                    }                    
                }
                
                
               


            }




        }


    }

}
